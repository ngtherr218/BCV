using System.Text.Json;
using System.Text.Json.Nodes;
using Npgsql;
using Bcv.Api.Modules.Journey.Application;

namespace Bcv.Api.Modules.Journey.Infrastructure;

/// <summary>Read-only receipt; opening/retrying a completion screen never issues rewards.</summary>
public static class JourneyCompletion
{
    public static async Task<JsonElement> LoadAsync(NpgsqlDataSource db,long userId,long journeyId,CancellationToken ct)
    {
        await using var command=db.CreateCommand("""
            SELECT jsonb_build_object(
              'journeyId',j.journey_id,'placeId',p.place_id,'placeName',p.name,'regionName',r.name,
              'status',j.status,'recordedSteps',j.recorded_steps,'requiredSteps',j.required_steps,
              'completedAt',j.completed_at,'isFirstDiscovery',COALESCE(d.discovered_at=j.completed_at,false),
              'imageUrl',(SELECT image_key FROM place_image WHERE place_id=p.place_id ORDER BY place_image_id LIMIT 1),
              'rankingPoints',COALESCE((SELECT sum(e.points) FROM explorer_score_event e WHERE e.user_id=j.user_id AND (
                (e.event_type='JOURNEY' AND e.source_key='journey:'||j.journey_id::text) OR
                (d.discovered_at=j.completed_at AND e.place_id=p.place_id AND
                  (e.event_type='DISCOVERY' OR (e.event_type IN ('JOURNEY','FIRST_PLACE') AND e.source_key=p.place_id::text)))
              )),0),
              'card',(SELECT jsonb_build_object('cardId',c.card_id,'name',c.name,'imageUrl',c.image_key,
                'newlyAwarded',uc.received_at=j.completed_at) FROM user_card uc JOIN card c USING(card_id)
                WHERE uc.user_id=j.user_id AND c.place_id=p.place_id AND c.status='ACTIVE'
                AND uc.received_at<=j.completed_at ORDER BY uc.received_at,c.card_id LIMIT 1)
            )::text
            FROM journey j JOIN place p ON p.place_id=j.destination_place_id JOIN region r ON r.region_id=p.region_id
            LEFT JOIN user_discovered_place d ON d.user_id=j.user_id AND d.place_id=p.place_id
            WHERE j.user_id=$1 AND j.journey_id=$2
            """);
        command.Parameters.AddWithValue(userId);command.Parameters.AddWithValue(journeyId);
        if(await command.ExecuteScalarAsync(ct) is not string json)
            throw new JourneyException("JOURNEY_NOT_FOUND","Không tìm thấy kết quả hành trình của bạn.");
        var result=JsonSerializer.Deserialize<JsonElement>(json);
        if(result.GetProperty("status").GetString()!="COMPLETED" || result.GetProperty("completedAt").ValueKind==JsonValueKind.Null
            || result.GetProperty("recordedSteps").GetInt32()<result.GetProperty("requiredSteps").GetInt32())
            throw new JourneyException("JOURNEY_NOT_COMPLETED","Hành trình chưa được máy chủ xác nhận hoàn thành.");
        await using var details = db.CreateCommand("""
            WITH trip AS (SELECT * FROM journey WHERE journey_id=$1 AND user_id=$2),
            totals AS (
              SELECT e.user_id,
                sum(e.points) FILTER(WHERE e.occurred_at < j.completed_at::timestamptz) AS before_points,
                sum(e.points) AS after_points
              FROM trip j JOIN explorer_score_event e ON
                e.occurred_at >= (date_trunc('week',j.completed_at::timestamptz AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'Asia/Ho_Chi_Minh')
                AND e.occurred_at <= j.completed_at::timestamptz
              JOIN app_user u ON u.user_id=e.user_id AND u.status='ACTIVE' GROUP BY e.user_id
            ), before_ranks AS (SELECT user_id,dense_rank() OVER(ORDER BY before_points DESC) AS rank FROM totals WHERE before_points IS NOT NULL),
            after_ranks AS (SELECT user_id,dense_rank() OVER(ORDER BY after_points DESC) AS rank FROM totals)
            SELECT jsonb_build_object(
              'quizAnswered',(SELECT count(*) FROM journey_quiz_answer WHERE journey_id=$1),
              'quizCorrect',(SELECT count(*) FROM journey_quiz_answer WHERE journey_id=$1 AND is_correct),
              'quizPoints',COALESCE((SELECT sum(points) FROM explorer_score_event WHERE user_id=$2 AND event_type='QUIZ' AND source_key LIKE $1::text||':%'),0),
              'masteryPoints',COALESCE((SELECT sum(e.points) FROM explorer_score_event e JOIN trip j ON e.user_id=j.user_id
                AND e.place_id=j.destination_place_id AND e.event_type='MASTERY'
                AND e.occurred_at >= j.started_at::timestamptz AND e.occurred_at <= j.completed_at::timestamptz),0),
              'rankBefore',(SELECT rank FROM before_ranks WHERE user_id=$2),
              'rankAfter',(SELECT rank FROM after_ranks WHERE user_id=$2)
            )::text
            """);
        details.Parameters.AddWithValue(journeyId); details.Parameters.AddWithValue(userId);
        var receipt=JsonNode.Parse(json)!.AsObject();
        var extra=JsonNode.Parse((string)(await details.ExecuteScalarAsync(ct))!)!.AsObject();
        foreach(var field in extra) receipt[field.Key]=field.Value?.DeepClone();
        receipt["totalPoints"]=receipt["rankingPoints"]!.GetValue<long>()+receipt["quizPoints"]!.GetValue<long>()+receipt["masteryPoints"]!.GetValue<long>();
        return JsonSerializer.SerializeToElement(receipt);
    }
}
