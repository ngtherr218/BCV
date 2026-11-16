using System.Globalization;

namespace Bcv.Api.Modules.QuestItem.Domain;

/// <summary>Daily ("2026-10-25") and ISO-weekly ("2026-W43") cycle keys in Vietnam time, as seeded in QUEST_PROGRESS.</summary>
public static class QuestCycles
{
    private static readonly TimeZoneInfo Vietnam = FindVietnam();

    public static (string Daily, string Weekly) Current() => At(DateTimeOffset.UtcNow);

    public static (string Daily, string Weekly) At(DateTimeOffset instant)
    {
        var local = TimeZoneInfo.ConvertTime(instant, Vietnam).Date;
        return (local.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture),
            $"{ISOWeek.GetYear(local)}-W{ISOWeek.GetWeekOfYear(local):00}");
    }

    private static TimeZoneInfo FindVietnam()
    {
        foreach (var id in new[] { "Asia/Ho_Chi_Minh", "SE Asia Standard Time" })
        {
            try { return TimeZoneInfo.FindSystemTimeZoneById(id); }
            catch (TimeZoneNotFoundException) { }
        }
        return TimeZoneInfo.CreateCustomTimeZone("ICT", TimeSpan.FromHours(7), "ICT", "ICT");
    }
}
