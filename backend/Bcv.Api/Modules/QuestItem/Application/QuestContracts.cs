namespace Bcv.Api.Modules.QuestItem.Application;

public sealed record QuestView(long QuestId, string Name, string CycleType, string ConditionType,
    int TargetValue, int ProgressValue, bool Completed, bool RewardReceived,
    string? RewardName, string? RewardType, int? RewardQuantity,
    int RankingPoints = 0,
    int AwardedRankingPoints = 0);

public sealed record QuestOverview(string DailyCycle, string WeeklyCycle, DateTimeOffset UpdatedAt,
    IReadOnlyList<QuestView> Daily, IReadOnlyList<QuestView> Weekly);
