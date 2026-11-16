using Npgsql;
namespace Bcv.Api.Modules.StatsNotify.Infrastructure;

/// <summary>Append-only, transaction-scoped awards. Legacy events remain readable, never reissued.</summary>
public static class ExplorerScoreEvents
{
    public static async Task<int> WalkingAsync(NpgsqlConnection connection,NpgsqlTransaction transaction,
        long userId,long placeId,bool firstDiscovery,long journeyId,CancellationToken ct)
    {
        if(!firstDiscovery)return 0;
        var rules=await ExplorerRewardConfig.LoadAsync(connection,transaction,ct);
        if(rules.DiscoveryPoints==0)return 0;
        await using var command=new NpgsqlCommand("""
            INSERT INTO explorer_score_event(user_id,event_type,source_key,place_id,region_id,points,rule_version,occurred_at)
            SELECT $1,'DISCOVERY',p.place_id::text,p.place_id,p.region_id,$4,$5,CURRENT_TIMESTAMP
            FROM place p WHERE p.place_id=$2 AND EXISTS(SELECT 1 FROM journey j
              WHERE j.journey_id=$3 AND j.user_id=$1 AND j.destination_place_id=p.place_id AND j.status='COMPLETED')
            ON CONFLICT(user_id,event_type,source_key) DO NOTHING RETURNING points
            """,connection,transaction);
        command.Parameters.AddWithValue(userId);command.Parameters.AddWithValue(placeId);command.Parameters.AddWithValue(journeyId);
        command.Parameters.AddWithValue(rules.DiscoveryPoints);command.Parameters.AddWithValue(rules.RuleVersion);
        return await command.ExecuteScalarAsync(ct) is int points?points:0;
    }

    public static async Task QuestAsync(NpgsqlConnection connection,NpgsqlTransaction transaction,long userId,long progressId,CancellationToken ct,bool eligible=true)
    {
        await using var command=new NpgsqlCommand("""
            WITH settled AS (
              UPDATE quest_progress qp SET awarded_points=CASE WHEN $3 THEN q.reward_points ELSE 0 END,points_settled=true
              FROM quest q WHERE qp.quest_id=q.quest_id AND qp.user_id=$1 AND qp.quest_progress_id=$2
                AND qp.completion_status AND qp.completed_at=CURRENT_TIMESTAMP AND NOT qp.points_settled
              RETURNING qp.user_id,qp.quest_progress_id,qp.awarded_points
            )
            INSERT INTO explorer_score_event(user_id,event_type,source_key,points,rule_version,occurred_at)
            SELECT user_id,'QUEST',quest_progress_id::text,awarded_points,'v3:quest',CURRENT_TIMESTAMP
            FROM settled WHERE awarded_points>0
            ON CONFLICT(user_id,event_type,source_key) DO NOTHING
            """,connection,transaction);
        command.Parameters.AddWithValue(userId);command.Parameters.AddWithValue(progressId);command.Parameters.AddWithValue(eligible);
        await command.ExecuteNonQueryAsync(ct);
    }
}
