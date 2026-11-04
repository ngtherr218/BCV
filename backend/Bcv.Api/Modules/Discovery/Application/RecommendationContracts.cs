using Bcv.Api.Modules.Discovery.Domain;

namespace Bcv.Api.Modules.Discovery.Application;

public sealed record RecommendationConfig(long ConfigId, string Version, decimal DistanceLimitMeters,
    IReadOnlyDictionary<string, decimal> Weights, RecommendationSettings? Settings = null);

public sealed record RecommendationInput(string RegionName, decimal Latitude, decimal Longitude, string? ActivityLevel,
    IReadOnlyList<PreferenceGroup> Preferences, RecommendationConfig Config,
    IReadOnlyList<RecommendationPlace> Places);

public sealed record RecommendedPlace(int Rank, long PlaceId, string Name, string RegionName, string? ImageUrl,
    bool IsDiscovered, decimal DistanceKm, int RequiredSteps, double Score,
    IReadOnlyList<string> MatchedPreferences, CriterionScores Criteria,
    IReadOnlyList<string> UsedCriteria, IReadOnlyList<string> MissingCriteria,
    IReadOnlyDictionary<string, double> EffectiveWeights, IReadOnlyList<string> Reasons);

/// <summary>Latitude/Longitude are the virtual location, so the app can show its street address.</summary>
public sealed record RecommendationResult(string RegionName, decimal Latitude, decimal Longitude,
    string ConfigVersion, IReadOnlyList<RecommendedPlace> Places, RecommendationSettings? Configuration = null);

public interface IRecommendationRepository
{
    /// <summary>Null when the user has no virtual location yet.</summary>
    Task<RecommendationInput?> LoadAsync(long userId, CancellationToken ct);
}

public sealed class RecommendationService(IRecommendationRepository repository)
{
    public async Task<RecommendationResult> RecommendAsync(long userId, CancellationToken ct)
    {
        var input = await repository.LoadAsync(userId, ct)
            ?? throw new ExploreException("NO_VIRTUAL_LOCATION", "Hãy chọn điểm xuất phát trước khi tìm điểm đến.");
        if (input.Preferences.Count == 0)
            throw new ExploreException("NO_RECOMMENDATION_PREFERENCES", "Hãy chọn ít nhất một sở thích trước khi tìm điểm đến.");
        if (input.ActivityLevel is null || !(input.Config.Settings?.ActivityThresholds ?? RecommendationScoring.ActivityThresholds).ContainsKey(input.ActivityLevel))
            throw new ExploreException("NO_ACTIVITY_LEVEL", "Hãy thiết lập mức vận động trước khi tìm điểm đến.");
        if (input.Config.DistanceLimitMeters <= 0 || !RecommendationScoring.ValidWeights(input.Config.Weights))
            throw new ExploreException("INVALID_RECOMMENDATION_CONFIG", "Cấu hình gợi ý chưa hợp lệ. Vui lòng thử lại sau.");
        var ranked = RecommendationScoring.Rank(input.Places, input.Preferences, input.ActivityLevel,
            input.Config.Weights, input.Config.DistanceLimitMeters, input.Config.Settings?.ActivityThresholds);
        var places = ranked.Select((p, index) =>
        {
            var used = p.EffectiveWeights.Keys.ToList();
            var missing = RecommendationScoring.Criteria(p.Criteria)
                .Where(c => !RecommendationScoring.HasScore(c.Value) && input.Config.Weights[c.Code] > 0).Select(c => c.Code).ToList();
            return new RecommendedPlace(index + 1, p.Place.PlaceId, p.Place.Name, p.Place.RegionName,
                p.Place.ImageUrl, p.Place.Discovered, Math.Round(p.Place.DistanceMeters / 1000m, 2),
                p.RequiredSteps, Math.Round(p.Score, 4), p.MatchedPreferences, p.Criteria, used, missing,
                p.EffectiveWeights, Reasons(p));
        }).ToList();
        return new RecommendationResult(input.RegionName, input.Latitude, input.Longitude,
            input.Config.Version, places, input.Config.Settings);
    }

    private static IReadOnlyList<string> Reasons(ScoredPlace place)
    {
        var reasons = new List<string>();
        bool Used(string code) => place.EffectiveWeights.ContainsKey(code);
        if (Used(RecommendationScoring.Preference) && place.MatchedPreferences.Count > 0)
            reasons.Add($"Hợp sở thích: {string.Join(", ", place.MatchedPreferences)}");
        if (Used(RecommendationScoring.Cluster) && place.Criteria.Cluster >= 0.5)
            reasons.Add("Thuộc nhóm địa danh phù hợp sở thích");
        if (Used(RecommendationScoring.Activity) && place.Criteria.Activity == 1)
            reasons.Add("Số bước phù hợp mức vận động đã chọn");
        if (Used(RecommendationScoring.Rating) && place.Criteria.Rating >= 0.8)
            reasons.Add($"Điểm đánh giá {place.Place.Rating?.ToString("0.#", System.Globalization.CultureInfo.GetCultureInfo("vi-VN"))}/5");
        if (Used(RecommendationScoring.Popularity) && place.Criteria.Popularity >= 0.7)
            reasons.Add("Có nhiều lượt đánh giá");
        if (reasons.Count == 0)
            reasons.Add($"Cần khoảng {place.RequiredSteps.ToString("N0", System.Globalization.CultureInfo.GetCultureInfo("vi-VN"))} bước từ vị trí ảo");
        return reasons;
    }
}
