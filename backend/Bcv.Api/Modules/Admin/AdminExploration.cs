using System.Text.Json;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.StatsNotify.Infrastructure;
using Npgsql;
using NpgsqlTypes;

namespace Bcv.Api.Modules.Admin;

public sealed record AdminScoreQuery(string? Week = null, long? RegionId = null, string? EventType = null, int Page = 1, int PageSize = 20);

/// <summary>Read-only admin views over the existing catalogs, passport data and approved ledger.</summary>
public sealed class AdminExploration(NpgsqlDataSource db)
{
    public async Task<JsonElement> Overview(CancellationToken ct)
    {
        var period = ExplorerWeek.Resolve(null, DateTimeOffset.UtcNow);
        await using var command = db.CreateCommand("""
            WITH active_places AS (
              SELECT p.place_id,p.region_id FROM place p JOIN region r USING(region_id)
              WHERE p.status='ACTIVE' AND r.status='ACTIVE'
            ), region_totals AS (
              SELECT r.region_id,r.name,count(p.place_id) AS total
              FROM region r LEFT JOIN active_places p USING(region_id)
              WHERE r.status='ACTIVE' GROUP BY r.region_id,r.name
            ), user_regions AS (
              SELECT p.region_id,d.user_id,count(*) AS discovered FROM user_discovered_place d
              JOIN active_places p USING(place_id) GROUP BY p.region_id,d.user_id
            )
            SELECT jsonb_build_object(
              'database',current_database(),'currentWeek',$1::text,
              'counts',jsonb_build_object(
                'activePlaces',(SELECT count(*) FROM active_places),
                'activeCards',(SELECT count(*) FROM card WHERE status='ACTIVE'),
                'distinctiveCards',(SELECT count(*) FROM card c JOIN place p USING(place_id) WHERE c.name<>p.name),
                'weeklyQuests',(SELECT count(*) FROM quest WHERE cycle_type='WEEKLY' AND status='ACTIVE'),
                'passportStamps',(SELECT count(*) FROM user_discovered_place d JOIN active_places p USING(place_id)),
                'weeklyParticipants',(SELECT count(DISTINCT e.user_id) FROM explorer_score_event e JOIN app_user u USING(user_id)
                  WHERE e.occurred_at >= $2 AND e.occurred_at < $3 AND u.status='ACTIVE')
              ),
              'cards',COALESCE((SELECT jsonb_agg(to_jsonb(catalog) ORDER BY catalog.name) FROM (
                SELECT c.card_id AS "cardId",c.name,c.description,c.status,p.place_id AS "placeId",p.name AS "placeName",
                  r.name AS "regionName",c.image_key IS NOT NULL AS "hasImage",
                  (SELECT count(*) FROM user_card uc WHERE uc.card_id=c.card_id) AS "collectedCount"
                FROM card c JOIN place p USING(place_id) JOIN region r USING(region_id)
                WHERE c.name<>p.name ORDER BY c.name,c.card_id LIMIT 50
              ) catalog),'[]'::jsonb),
              'quests',COALESCE((SELECT jsonb_agg(to_jsonb(challenge) ORDER BY challenge."questId") FROM (
                SELECT q.quest_id AS "questId",q.name,q.status,q.condition_type AS "conditionType",q.target_value AS "targetValue",
                  i.name AS "rewardName",q.reward_quantity AS "rewardQuantity",
                  (SELECT count(*) FROM quest_progress qp WHERE qp.quest_id=q.quest_id AND qp.cycle_key=$1) AS "participants",
                  (SELECT count(*) FROM quest_progress qp WHERE qp.quest_id=q.quest_id AND qp.cycle_key=$1 AND qp.completion_status) AS "completed"
                FROM quest q LEFT JOIN item i ON i.item_id=q.reward_item_id WHERE q.cycle_type='WEEKLY'
                ORDER BY q.quest_id
              ) challenge),'[]'::jsonb),
              'regions',COALESCE((SELECT jsonb_agg(to_jsonb(passport) ORDER BY passport.name) FROM (
                SELECT rt.region_id AS "regionId",rt.name,rt.total,
                  COALESCE((SELECT sum(ur.discovered) FROM user_regions ur WHERE ur.region_id=rt.region_id),0) AS "stamps",
                  (SELECT count(*) FROM user_regions ur WHERE ur.region_id=rt.region_id AND rt.total>0 AND ur.discovered=rt.total) AS "completedPassports"
                FROM region_totals rt
              ) passport),'[]'::jsonb)
            )::text
            """);
        command.Parameters.AddWithValue(period.Key);
        command.Parameters.AddWithValue(period.Start.ToUniversalTime());
        command.Parameters.AddWithValue(period.End.ToUniversalTime());
        var json = (string)(await command.ExecuteScalarAsync(ct))!;
        return JsonSerializer.Deserialize<JsonElement>(json);
    }

    public async Task<JsonElement> Events(AdminScoreQuery query, CancellationToken ct)
    {
        if (query.Page < 1 || query.Page > 100000 || query.PageSize < 1 || query.PageSize > 50 || query.RegionId <= 0
            || query.EventType is not (null or "FIRST_PLACE" or "JOURNEY" or "QUEST" or "DISCOVERY"))
            throw new AdminValidation("Bộ lọc điểm không hợp lệ.");
        var period = ExplorerWeek.Resolve(query.Week, DateTimeOffset.UtcNow);
        await using var command = db.CreateCommand("""
            WITH filtered AS (
              SELECT e.score_event_id AS "eventId",e.user_id AS "userId",e.event_type AS "eventType",e.points,
                e.occurred_at AS "occurredAt",p.name AS "placeName",r.name AS "regionName",
                e.source_key AS "sourceKey",e.rule_version AS "ruleVersion",q.name AS "questName"
              FROM explorer_score_event e LEFT JOIN place p USING(place_id) LEFT JOIN region r ON r.region_id=e.region_id
              LEFT JOIN quest_progress qp ON e.event_type='QUEST' AND e.source_key=qp.quest_progress_id::text
              LEFT JOIN quest q ON q.quest_id=qp.quest_id
              WHERE e.occurred_at >= $1 AND e.occurred_at < $2
                AND ($3::bigint IS NULL OR e.region_id=$3) AND ($4::text IS NULL OR e.event_type=$4)
            ), paged AS (SELECT * FROM filtered ORDER BY "occurredAt" DESC,"eventId" DESC LIMIT $5 OFFSET $6)
            SELECT jsonb_build_object('total',(SELECT count(*) FROM filtered),
              'items',COALESCE((SELECT jsonb_agg(to_jsonb(paged) ORDER BY "occurredAt" DESC,"eventId" DESC) FROM paged),'[]'::jsonb))::text
            """);
        command.Parameters.AddWithValue(period.Start.ToUniversalTime());
        command.Parameters.AddWithValue(period.End.ToUniversalTime());
        command.Parameters.Add(new() { NpgsqlDbType=NpgsqlDbType.Bigint, Value=(object?)query.RegionId ?? DBNull.Value });
        command.Parameters.Add(new() { NpgsqlDbType=NpgsqlDbType.Text, Value=(object?)query.EventType ?? DBNull.Value });
        command.Parameters.AddWithValue(query.PageSize);
        command.Parameters.AddWithValue((query.Page-1)*query.PageSize);
        return JsonSerializer.Deserialize<JsonElement>((string)(await command.ExecuteScalarAsync(ct))!);
    }

    public static void Map(RouteGroupBuilder group)
    {
        group.MapGet("/exploration/reward-settings",async (NpgsqlDataSource db,CancellationToken ct) => {
            await using var connection=await db.OpenConnectionAsync(ct);
            return await ExplorerRewardConfig.LoadAsync(connection,null,ct);
        });
        group.MapPut("/exploration/reward-settings",async (ExplorerRewardConfig input,HttpContext http,NpgsqlDataSource db,CancellationToken ct) => {
            if(input.DiscoveryPoints is <0 or >200000)
                throw new AdminValidation("Điểm thưởng phải là số nguyên từ 0 đến 200.000.");
            await using var connection=await db.OpenConnectionAsync(ct);
            await using var command=new NpgsqlCommand("""
                UPDATE explorer_reward_config SET discovery_points=$1,
                  revision=revision+1,updated_at=CURRENT_TIMESTAMP,updated_by=$2 WHERE singleton AND revision=$3
                """,connection);
            command.Parameters.AddWithValue(input.DiscoveryPoints);
            command.Parameters.AddWithValue((long)http.Items["AdminId"]!);command.Parameters.AddWithValue(input.Revision);
            if(await command.ExecuteNonQueryAsync(ct)!=1)throw new AdminValidation("Cấu hình đã thay đổi. Hãy tải lại trước khi lưu.");
            return await ExplorerRewardConfig.LoadAsync(connection,null,ct);
        });
        group.MapGet("/exploration/overview", (AdminExploration service, CancellationToken ct) => Read(() => service.Overview(ct)));
        group.MapGet("/exploration/events", ([AsParameters] AdminScoreQuery query, AdminExploration service, CancellationToken ct) => Read(() => service.Events(query,ct)));
        group.MapGet("/exploration/leaderboard", ([AsParameters] LeaderboardQuery query, HttpContext http, LeaderboardRepository service, CancellationToken ct) =>
            Read(() => service.LoadAsync((long)http.Items["AdminId"]!,query,ct)));
    }

    private static async Task<T> Read<T>(Func<Task<T>> action)
    {
        try { return await action(); }
        catch(ExploreException ex) { throw new AdminValidation(ex.Message); }
    }
}
