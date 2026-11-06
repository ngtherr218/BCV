using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.QuestItem.Domain;

namespace Bcv.Api.Modules.QuestItem.Infrastructure;

/// <summary>
/// Adds progress to every active quest of a condition for the current cycle. Called inside the
/// transaction that records the event (step sync, journey completion), so progress and event agree.
/// </summary>
public static class QuestProgress
{
    public const string StepCount = "STEP_COUNT";
    public const string DiscoverPlace = "DISCOVER_PLACE";
    public const string CompleteJourney = "COMPLETE_JOURNEY";

    public static async Task RecordAsync(NpgsqlConnection connection, NpgsqlTransaction transaction,
        long userId, string conditionType, int amount, CancellationToken ct)
    {
        if (amount <= 0) return;
        var (daily, weekly) = QuestCycles.Current();
        await using var command = new NpgsqlCommand("""
            INSERT INTO quest_progress
                (user_id, quest_id, cycle_key, progress_value, completion_status, completed_at)
            SELECT $1, q.quest_id,
                   CASE q.cycle_type WHEN 'DAILY' THEN $3 WHEN 'WEEKLY' THEN $4 ELSE 'ONCE' END,
                   $2, $2 >= q.target_value,
                   CASE WHEN $2 >= q.target_value THEN CURRENT_TIMESTAMP END
            FROM quest q
            WHERE q.condition_type = $5 AND q.status = 'ACTIVE'
            ON CONFLICT (user_id, quest_id, cycle_key) DO UPDATE
            SET progress_value = quest_progress.progress_value + EXCLUDED.progress_value,
                completion_status = quest_progress.completion_status
                    OR quest_progress.progress_value + EXCLUDED.progress_value >=
                       (SELECT target_value FROM quest WHERE quest_id = quest_progress.quest_id),
                completed_at = COALESCE(quest_progress.completed_at,
                    CASE WHEN quest_progress.progress_value + EXCLUDED.progress_value >=
                              (SELECT target_value FROM quest WHERE quest_id = quest_progress.quest_id)
                         THEN CURRENT_TIMESTAMP END)
            """, connection, transaction);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        command.Parameters.Add(new NpgsqlParameter<int> { TypedValue = amount, NpgsqlDbType = NpgsqlDbType.Integer });
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = daily, NpgsqlDbType = NpgsqlDbType.Varchar });
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = weekly, NpgsqlDbType = NpgsqlDbType.Varchar });
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = conditionType, NpgsqlDbType = NpgsqlDbType.Varchar });
        await command.ExecuteNonQueryAsync(ct);

        // completed_at = CURRENT_TIMESTAMP only for quests this transaction completed (it is the
        // transaction start time); NOT EXISTS keeps a later call in the same transaction from repeating it.
        await using var notify = new NpgsqlCommand("""
            INSERT INTO notification (user_id, notification_type, title, content, created_at)
            SELECT $1, 'QUEST_COMPLETED', 'Nhiệm vụ hoàn thành', q.name || ' đã đạt mục tiêu.', CURRENT_TIMESTAMP
            FROM quest_progress qp JOIN quest q ON q.quest_id = qp.quest_id
            WHERE qp.user_id = $1 AND q.condition_type = $2 AND qp.completed_at = CURRENT_TIMESTAMP
              AND NOT EXISTS (SELECT 1 FROM notification n
                              WHERE n.user_id = $1 AND n.notification_type = 'QUEST_COMPLETED'
                                AND n.created_at = CURRENT_TIMESTAMP AND n.content = q.name || ' đã đạt mục tiêu.')
            """, connection, transaction);
        notify.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        notify.Parameters.Add(new NpgsqlParameter<string> { TypedValue = conditionType, NpgsqlDbType = NpgsqlDbType.Varchar });
        await notify.ExecuteNonQueryAsync(ct);
    }
}
