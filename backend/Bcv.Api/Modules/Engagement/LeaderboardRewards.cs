using Npgsql;

namespace Bcv.Api.Modules.Engagement;

/// <summary>
/// Weekly prizes for all three boards: the top 3 of the Khám phá board and of each level's Vận động and
/// Giữ chuỗi boards get the prize set for their rank on the admin web (leaderboard_prize). Khám phá and Vận động count that week only; Giữ chuỗi is the
/// longest streak kept up to that Sunday (it never resets). A week is settled once, by the first board or
/// summary request after Monday 0:00 (Vietnam time); the ranking is the board's (value, then the earlier account).
/// </summary>
public static class LeaderboardRewards
{
    /// <summary>The prize for rank 1, 2 and 3 (admin web › Giải thưởng tuần); quantity 0 when there is none.</summary>
    public static async Task<List<object>> PrizesAsync(NpgsqlDataSource db, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT p.rank::int, i.name, i.item_type, CASE WHEN i.item_id IS NULL THEN 0 ELSE p.quantity END
            FROM leaderboard_prize p LEFT JOIN item i ON i.item_id=p.item_id
            ORDER BY p.rank
            """);
        var prizes = new List<object>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
            prizes.Add(new
            {
                rank = reader.GetInt32(0),
                itemName = reader.IsDBNull(1) ? null : reader.GetString(1),
                itemType = reader.IsDBNull(2) ? null : reader.GetString(2),
                quantity = reader.GetInt32(3),
            });
        return prizes;
    }

    private const long LockKey = 7246330;

    public static DateOnly Monday(DateOnly day) => day.AddDays(-(((int)day.DayOfWeek + 6) % 7));

    /// <summary>The week before this one: rewards are paid for it.</summary>
    public static DateOnly LastWeek() => Monday(EngagementCore.Today()).AddDays(-7);

    public static async Task SettleAsync(NpgsqlDataSource db, CancellationToken ct)
    {
        var week = LastWeek();
        await using (var done = db.CreateCommand("SELECT EXISTS (SELECT 1 FROM leaderboard_week WHERE week_start=$1)"))
        {
            done.Parameters.AddWithValue(week);
            if ((bool)(await done.ExecuteScalarAsync(ct))!) return;
        }
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(ct);
        await using (var gate = new NpgsqlCommand($"SELECT pg_advisory_xact_lock({LockKey})", connection, transaction))
            await gate.ExecuteNonQueryAsync(ct);
        await using (var mark = new NpgsqlCommand(
            "INSERT INTO leaderboard_week (week_start) VALUES ($1) ON CONFLICT DO NOTHING", connection, transaction))
        {
            mark.Parameters.AddWithValue(week);
            if (await mark.ExecuteNonQueryAsync(ct) == 0) return; // another request settled it meanwhile
        }

        // Same rules as the boards: active non-admin users; Vận động excludes anyone with refused steps that week.
        await using (var winners = new NpgsqlCommand("""
            WITH knowledge AS (
                SELECT 'knowledge'::varchar AS scope, ''::varchar AS level, p.user_id, SUM(p.points)::int AS value
                FROM explorer_point p
                JOIN app_user u ON u.user_id=p.user_id AND u.status='ACTIVE' AND u.role <> 'ADMIN'
                WHERE p.created_at >= $1::timestamp AND p.created_at < $2::timestamp
                GROUP BY p.user_id HAVING SUM(p.points) > 0
            ), activity AS (
                SELECT 'activity'::varchar, pref.activity_level::varchar, a.user_id, SUM(a.steps)::int
                FROM daily_activity a
                JOIN app_user u ON u.user_id=a.user_id AND u.status='ACTIVE' AND u.role <> 'ADMIN'
                JOIN user_preference pref ON pref.user_id=u.user_id
                WHERE a.activity_date >= $1 AND a.activity_date < $2
                  AND NOT EXISTS (SELECT 1 FROM step_flag f WHERE f.user_id=u.user_id
                                  AND f.created_at >= $1::timestamp AND f.created_at < $2::timestamp)
                GROUP BY pref.activity_level, a.user_id HAVING SUM(a.steps) > 0
            ), ranked AS (
                SELECT x.*, row_number() OVER (PARTITION BY scope, level ORDER BY value DESC, user_id) AS rank
                FROM (SELECT * FROM knowledge UNION ALL SELECT * FROM activity) x
            )
            INSERT INTO leaderboard_reward (week_start, scope, activity_level, rank, user_id, value, item_id, quantity)
            SELECT $1, w.scope, w.level, w.rank, w.user_id, w.value,
                   p.item_id, CASE WHEN p.item_id IS NULL THEN 0 ELSE p.quantity END
            FROM ranked w LEFT JOIN leaderboard_prize p ON p.rank = w.rank
            WHERE w.rank <= 3
            """, connection, transaction))
        {
            winners.Parameters.AddWithValue(week);
            winners.Parameters.AddWithValue(week.AddDays(7));
            await winners.ExecuteNonQueryAsync(ct);
        }
        await AddStreakWinnersAsync(connection, transaction, week, ct);
        await using (var grant = new NpgsqlCommand("""
            INSERT INTO user_item (user_id, item_id, quantity)
            SELECT user_id, item_id, SUM(quantity) FROM leaderboard_reward
            WHERE week_start=$1 AND item_id IS NOT NULL AND quantity > 0
            GROUP BY user_id, item_id
            ON CONFLICT (user_id, item_id) DO UPDATE
            SET quantity = user_item.quantity + EXCLUDED.quantity, updated_at = CURRENT_TIMESTAMP
            """, connection, transaction))
        {
            grant.Parameters.AddWithValue(week);
            await grant.ExecuteNonQueryAsync(ct);
        }
        await using (var notify = new NpgsqlCommand("""
            INSERT INTO notification (user_id, notification_type, title, content)
            SELECT r.user_id, 'LEADERBOARD_REWARD', 'Bạn lọt top 3 tuần trước!',
                   'Hạng ' || r.rank || ' bảng ' ||
                   CASE r.scope WHEN 'knowledge' THEN 'Khám phá' WHEN 'streak' THEN 'Giữ chuỗi' ELSE 'Vận động' END ||
                   CASE WHEN r.item_id IS NOT NULL AND r.quantity > 0
                        THEN ': +' || r.quantity || ' ' || lower(i.name) || ' đã vào kho.' ELSE '.' END
            FROM leaderboard_reward r LEFT JOIN item i ON i.item_id = r.item_id
            WHERE r.week_start=$1
            """, connection, transaction))
        {
            notify.Parameters.AddWithValue(week);
            await notify.ExecuteNonQueryAsync(ct);
        }
        await transaction.CommitAsync(ct);
    }

    /// <summary>Giữ chuỗi: each level's top 3 by the longest streak kept up to the week's Sunday.</summary>
    private static async Task AddStreakWinnersAsync(NpgsqlConnection connection, NpgsqlTransaction transaction,
        DateOnly week, CancellationToken ct)
    {
        var sunday = week.AddDays(6);
        var people = new Dictionary<long, (string Level, List<EngagementCore.DayRecord> Days)>();
        await using (var history = new NpgsqlCommand("""
            SELECT pref.activity_level, a.user_id, a.activity_date, a.steps, a.goal_steps
            FROM daily_activity a
            JOIN app_user u ON u.user_id=a.user_id AND u.status='ACTIVE' AND u.role <> 'ADMIN'
            JOIN user_preference pref ON pref.user_id=u.user_id
            WHERE a.activity_date <= $1
            ORDER BY a.user_id, a.activity_date
            """, connection, transaction))
        {
            history.Parameters.AddWithValue(sunday);
            await using var reader = await history.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
            {
                var id = reader.GetInt64(1);
                if (!people.TryGetValue(id, out var person)) people[id] = person = (reader.GetString(0), []);
                person.Days.Add(new EngagementCore.DayRecord(reader.GetFieldValue<DateOnly>(2), reader.GetInt32(3), reader.GetInt32(4)));
            }
        }
        var winners = people
            .Select(p => (p.Value.Level, UserId: p.Key, Best: EngagementCore.ComputeStreak(p.Value.Days, sunday).Best))
            .Where(p => p.Best > 0)
            .GroupBy(p => p.Level)
            .SelectMany(g => g.OrderByDescending(p => p.Best).ThenBy(p => p.UserId).Take(3)
                .Select((p, index) => (p.Level, p.UserId, p.Best, Rank: index + 1)));
        foreach (var (level, userId, best, rank) in winners)
        {
            await using var insert = new NpgsqlCommand("""
                INSERT INTO leaderboard_reward (week_start, scope, activity_level, rank, user_id, value, item_id, quantity)
                SELECT $1, 'streak', $2, $3, $4, $5,
                       p.item_id, CASE WHEN p.item_id IS NULL THEN 0 ELSE p.quantity END
                FROM (SELECT 1) one LEFT JOIN leaderboard_prize p ON p.rank = $3
                """, connection, transaction);
            insert.Parameters.AddWithValue(week);
            insert.Parameters.AddWithValue(level);
            insert.Parameters.AddWithValue((short)rank);
            insert.Parameters.AddWithValue(userId);
            insert.Parameters.AddWithValue(best);
            await insert.ExecuteNonQueryAsync(ct);
        }
    }

    public static string Unit(string scope) => scope switch { "knowledge" => "điểm", "streak" => "ngày", _ => "bước" };

    public static string BoardName(string scope, string level) => scope switch
    {
        "knowledge" => "Khám phá",
        "streak" => "Giữ chuỗi · mức " + LevelName(level),
        _ => "Vận động · mức " + LevelName(level),
    };

    /// <summary>Last week's top 3 of one board, for "Bảng vàng tuần trước" (empty when nobody placed).</summary>
    public static async Task<List<(int Rank, long UserId, string Name, int Value, string? Avatar)>> PodiumAsync(
        NpgsqlDataSource db, string scope, string level, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT r.rank, r.user_id, u.full_name, r.value, CASE WHEN u.avatar LIKE 'https://%' THEN u.avatar END
            FROM leaderboard_reward r JOIN app_user u ON u.user_id=r.user_id
            WHERE r.week_start=$1 AND r.scope=$2 AND r.activity_level=$3
            ORDER BY r.rank
            """);
        command.Parameters.AddWithValue(LastWeek());
        command.Parameters.AddWithValue(scope);
        command.Parameters.AddWithValue(level);
        var rows = new List<(int, long, string, int, string?)>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
            rows.Add((reader.GetInt16(0), reader.GetInt64(1), reader.GetString(2), reader.GetInt32(3),
                reader.IsDBNull(4) ? null : reader.GetString(4)));
        return rows;
    }

    /// <summary>
    /// Bảng vàng history of one board: the top 3 of each settled week, newest first (weeks nobody
    /// placed in are skipped), and how many times the user has been on any podium.
    /// </summary>
    public static async Task<object> HistoryAsync(NpgsqlDataSource db, long userId, string scope, string level,
        int weeks, CancellationToken ct)
    {
        var unit = Unit(scope);
        var rows = new List<(DateOnly Week, int Rank, long UserId, string Name, int Value, string? Avatar, int Quantity,
            string? ItemName, string? ItemType)>();
        await using (var command = db.CreateCommand("""
            SELECT r.week_start, r.rank, r.user_id, u.full_name, r.value,
                   CASE WHEN u.avatar LIKE 'https://%' THEN u.avatar END, r.quantity, i.name, i.item_type
            FROM leaderboard_reward r JOIN app_user u ON u.user_id=r.user_id
            LEFT JOIN item i ON i.item_id=r.item_id
            WHERE r.scope=$1 AND r.activity_level=$2
              AND r.week_start IN (SELECT DISTINCT week_start FROM leaderboard_reward
                                   WHERE scope=$1 AND activity_level=$2 ORDER BY week_start DESC LIMIT $3)
            ORDER BY r.week_start DESC, r.rank
            """))
        {
            command.Parameters.AddWithValue(scope);
            command.Parameters.AddWithValue(level);
            command.Parameters.AddWithValue(weeks);
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                rows.Add((reader.GetFieldValue<DateOnly>(0), reader.GetInt16(1), reader.GetInt64(2), reader.GetString(3),
                    reader.GetInt32(4), reader.IsDBNull(5) ? null : reader.GetString(5), reader.GetInt32(6),
                    reader.IsDBNull(7) ? null : reader.GetString(7), reader.IsDBNull(8) ? null : reader.GetString(8)));
        }
        int podiums;
        await using (var mine = db.CreateCommand("SELECT count(*)::int FROM leaderboard_reward WHERE user_id=$1"))
        {
            mine.Parameters.AddWithValue(userId);
            podiums = (int)(await mine.ExecuteScalarAsync(ct))!;
        }
        return new
        {
            scope, unit,
            boardName = BoardName(scope, level),
            myPodiums = podiums,
            weeks = rows.GroupBy(r => r.Week).Select(g => new
            {
                weekStart = g.Key.ToString("yyyy-MM-dd"),
                weekEnd = g.Key.AddDays(6).ToString("yyyy-MM-dd"),
                top = g.Select(r => new
                {
                    rank = r.Rank, name = Shorten(r.Name), value = r.Value, unit, avatar = r.Avatar,
                    isMe = r.UserId == userId, quantity = r.Quantity, itemName = r.ItemName, itemType = r.ItemType
                }).ToList()
            }).ToList()
        };
    }

    private static string Shorten(string fullName)
    {
        var name = fullName.Trim();
        return name.Length <= 24 ? name : name[..23].TrimEnd() + "…";
    }

    /// <summary>Rewards the user has not seen yet, for the popup on the ranking tab.</summary>
    public static async Task<List<object>> UnseenAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT r.week_start, r.scope, r.activity_level, r.rank, r.quantity, i.name, i.item_type
            FROM leaderboard_reward r LEFT JOIN item i ON i.item_id=r.item_id
            WHERE r.user_id=$1 AND r.seen_at IS NULL
            ORDER BY r.week_start DESC, r.scope
            """);
        command.Parameters.AddWithValue(userId);
        var rows = new List<object>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            var scope = reader.GetString(1);
            rows.Add(new
            {
                weekStart = reader.GetFieldValue<DateOnly>(0).ToString("yyyy-MM-dd"),
                scope,
                boardName = BoardName(scope, reader.GetString(2)),
                rank = (int)reader.GetInt16(3),
                quantity = reader.GetInt32(4),
                itemName = reader.IsDBNull(5) ? null : reader.GetString(5),
                itemType = reader.IsDBNull(6) ? null : reader.GetString(6),
            });
        }
        return rows;
    }

    public static async Task MarkSeenAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        await using var command = db.CreateCommand(
            "UPDATE leaderboard_reward SET seen_at=now() WHERE user_id=$1 AND seen_at IS NULL");
        command.Parameters.AddWithValue(userId);
        await command.ExecuteNonQueryAsync(ct);
    }

    public static string LevelName(string level) => level switch { "LOW" => "nhẹ", "HIGH" => "cao", _ => "vừa" };
}
