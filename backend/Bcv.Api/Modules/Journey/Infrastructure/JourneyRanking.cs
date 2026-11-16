using Npgsql;

namespace Bcv.Api.Modules.Journey.Infrastructure;

/// <summary>Weekly national rank, using the leaderboard's active-user and dense-rank rules.</summary>
public static class JourneyRanking
{
    public static async Task<long?> LoadAsync(NpgsqlConnection connection, NpgsqlTransaction transaction,
        long userId, DateTimeOffset start, DateTimeOffset end, CancellationToken ct)
    {
        await using var command = new NpgsqlCommand("""
            WITH totals AS (
                SELECT e.user_id,sum(e.points)::bigint AS points
                FROM explorer_score_event e JOIN app_user u ON u.user_id=e.user_id AND u.status='ACTIVE'
                WHERE e.occurred_at >= $1 AND e.occurred_at < $2 GROUP BY e.user_id
            ), ranked AS (SELECT user_id,dense_rank() OVER(ORDER BY points DESC) AS rank FROM totals)
            SELECT rank FROM ranked WHERE user_id=$3
            """, connection, transaction);
        command.Parameters.AddWithValue(start.ToUniversalTime());
        command.Parameters.AddWithValue(end.ToUniversalTime());
        command.Parameters.AddWithValue(userId);
        return await command.ExecuteScalarAsync(ct) is long rank ? rank : null;
    }

    public static string Describe(long? before, long? after) => (before, after) switch
    {
        (_, null) => "Bạn chưa có thứ hạng trên bảng xếp hạng toàn quốc tuần này.",
        (null, _) => $"Bạn đã vào bảng xếp hạng toàn quốc tuần này ở vị trí #{after}.",
        _ when after < before => $"Xếp hạng toàn quốc tuần này: #{before} → #{after} (tăng {before - after} bậc).",
        _ when after > before => $"Xếp hạng toàn quốc tuần này: #{before} → #{after} (giảm {after - before} bậc).",
        _ => $"Xếp hạng toàn quốc tuần này: giữ vị trí #{after}."
    };
}
