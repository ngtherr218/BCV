using Bcv.Api.Modules.Discovery.Domain;
using Npgsql;

namespace Bcv.Api.Modules.Engagement;

/// <summary>Shared rules for daily goals, streaks and travel zones.</summary>
public static class EngagementCore
{
    /// <summary>Days follow Vietnam time so "today" matches what the user sees.</summary>
    public static DateOnly Today() => DateOnly.FromDateTime(DateTime.UtcNow.AddHours(7));

    public const int MaxDailySteps = 40_000;

    public static readonly IReadOnlyDictionary<string, string> ZoneNames = new Dictionary<string, string>
    {
        ["MIEN_NUI_BAC"] = "Trung du và miền núi phía Bắc",
        ["SONG_HONG"] = "Đồng bằng sông Hồng",
        ["MIEN_TRUNG"] = "Bắc Trung Bộ và duyên hải miền Trung",
        ["TAY_NGUYEN"] = "Tây Nguyên",
        ["DONG_NAM_BO"] = "Đông Nam Bộ",
        ["TAY_NAM_BO"] = "Đồng bằng sông Cửu Long",
    };

    /// <summary>Personal daily goal: the same T(u) the recommender uses for the user's activity level.</summary>
    public static async Task<(string Level, int Goal)> DailyGoalAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("SELECT activity_level FROM user_preference WHERE user_id=$1");
        command.Parameters.AddWithValue(userId);
        var level = await command.ExecuteScalarAsync(ct) as string ?? "MEDIUM";
        return (level, RecommendationScoring.ActivityThresholds.TryGetValue(level, out var goal) ? goal : 6000);
    }

    public sealed record DayRecord(DateOnly Date, int Steps, int Goal);
    public sealed record Streak(int Current, int Best, bool TodayDone);

    public static async Task<List<DayRecord>> HistoryAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        var days = new List<DayRecord>();
        await using (var command = db.CreateCommand("""
            SELECT activity_date, steps, goal_steps FROM daily_activity
            WHERE user_id=$1 AND activity_date >= $2 ORDER BY activity_date
            """))
        {
            command.Parameters.AddWithValue(userId);
            command.Parameters.AddWithValue(Today().AddDays(-400));
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                days.Add(new DayRecord(reader.GetFieldValue<DateOnly>(0), reader.GetInt32(1), reader.GetInt32(2)));
        }
        return days;
    }

    /// <summary>
    /// A day keeps the streak when its steps reach that day's goal; a missed day breaks it.
    /// Today only extends the streak once done, so an unfinished today never resets it.
    /// </summary>
    public static Streak ComputeStreak(IReadOnlyList<DayRecord> days, DateOnly today)
    {
        var met = days.Where(d => d.Steps >= d.Goal).Select(d => d.Date).ToHashSet();
        var todayDone = met.Contains(today);
        var current = 0;
        for (var d = todayDone ? today : today.AddDays(-1); d >= today.AddDays(-400); d = d.AddDays(-1))
        {
            if (met.Contains(d)) current++;
            else break;
        }
        var best = 0;
        var running = 0;
        if (days.Count > 0)
            for (var d = days[0].Date; d <= today; d = d.AddDays(1))
            {
                if (met.Contains(d)) running++;
                else if (d != today) running = 0;
                best = Math.Max(best, running);
            }
        return new Streak(current, Math.Max(best, current), todayDone);
    }

    public static double DistanceMeters(double lat1, double lon1, double lat2, double lon2)
    {
        const double r = 6_371_000;
        double Rad(double v) => v * Math.PI / 180;
        var dLat = Rad(lat2 - lat1);
        var dLon = Rad(lon2 - lon1);
        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                Math.Cos(Rad(lat1)) * Math.Cos(Rad(lat2)) * Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        return r * 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
    }
}
