using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.QuestItem.Application;
using Bcv.Api.Modules.QuestItem.Domain;

namespace Bcv.Api.Modules.QuestItem.Infrastructure;

public sealed class QuestRepository(NpgsqlDataSource db)
{
    /// <summary>
    /// Current daily and weekly quests with progress. Completed quests whose reward is still unpaid
    /// are paid here (item added to USER_ITEM) in the same transaction, so a reward is granted once.
    /// </summary>
    public async Task<QuestOverview> LoadAsync(long userId, CancellationToken ct)
    {
        var (daily, weekly) = QuestCycles.Current();
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(ct);

        await using (var grant = new NpgsqlCommand("""
            WITH due AS (
                UPDATE public.quest_progress qp SET reward_status = TRUE
                FROM public.quest q
                WHERE qp.quest_id = q.quest_id AND qp.user_id = $1
                  AND qp.cycle_key IN ($2, $3, 'ONCE')
                  AND qp.completion_status AND NOT qp.reward_status

                RETURNING q.reward_item_id AS item_id, q.reward_quantity AS quantity
            ), totals AS (
                SELECT item_id, SUM(quantity)::int AS quantity FROM due WHERE item_id IS NOT NULL GROUP BY item_id
            ), granted AS (
                INSERT INTO public.user_item (user_id, item_id, quantity)
                SELECT $1, item_id, quantity FROM totals
                ON CONFLICT (user_id, item_id) DO UPDATE
                SET quantity = user_item.quantity + EXCLUDED.quantity, updated_at = CURRENT_TIMESTAMP
            )
            INSERT INTO public.notification (user_id, notification_type, title, content)
            SELECT $1, 'ITEM_RECEIVED', 'Đã nhận vật phẩm',
                   t.quantity || ' ' || lower(i.name) || ' đã được thêm vào kho.'
            FROM totals t JOIN public.item i ON i.item_id = t.item_id
            """, connection, transaction))
        {
            grant.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            grant.Parameters.Add(new NpgsqlParameter<string> { TypedValue = daily, NpgsqlDbType = NpgsqlDbType.Varchar });
            grant.Parameters.Add(new NpgsqlParameter<string> { TypedValue = weekly, NpgsqlDbType = NpgsqlDbType.Varchar });
            await grant.ExecuteNonQueryAsync(ct);
        }

        var dailyQuests = new List<QuestView>();

        var weeklyQuests = new List<QuestView>();
        await using (var command = new NpgsqlCommand("""
            SELECT q.quest_id, q.name, q.cycle_type, q.condition_type, q.target_value,
                   COALESCE(qp.progress_value, 0), COALESCE(qp.completion_status, FALSE),
                   COALESCE(qp.reward_status, FALSE), i.name, i.item_type, q.reward_quantity,q.reward_points,COALESCE(qp.awarded_points,0)
            FROM public.quest q
            LEFT JOIN public.quest_progress qp
              ON qp.quest_id = q.quest_id AND qp.user_id = $1
             AND qp.cycle_key = CASE q.cycle_type WHEN 'DAILY' THEN $2 WHEN 'WEEKLY' THEN $3 ELSE 'ONCE' END
            LEFT JOIN public.item i ON i.item_id = q.reward_item_id
            WHERE q.status = 'ACTIVE' AND q.cycle_type IN ('DAILY', 'WEEKLY')
            ORDER BY q.target_value, q.quest_id
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = daily, NpgsqlDbType = NpgsqlDbType.Varchar });
            command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = weekly, NpgsqlDbType = NpgsqlDbType.Varchar });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
            {
                var quest = new QuestView(reader.GetInt64(0), reader.GetString(1), reader.GetString(2),
                    reader.GetString(3), reader.GetInt32(4), reader.GetInt32(5), reader.GetBoolean(6),
                    reader.GetBoolean(7), reader.IsDBNull(8) ? null : reader.GetString(8),
                    reader.IsDBNull(9) ? null : reader.GetString(9), reader.IsDBNull(10) ? null : reader.GetInt32(10),reader.GetInt32(11),reader.GetInt32(12));
                (quest.CycleType == "DAILY" ? dailyQuests : weeklyQuests).Add(quest);
            }
        }
        await transaction.CommitAsync(ct);
        return new QuestOverview(daily, weekly, DateTimeOffset.UtcNow, dailyQuests, weeklyQuests);
    }
}
