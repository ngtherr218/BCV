using Bcv.Api.Modules.StepLocation.Domain;

namespace Bcv.Api.Modules.Discovery.Domain;

/// <summary>One active place with the model data the ranking needs (section 3.5.4.6 of the design).</summary>
public sealed record RecommendationPlace(
    long PlaceId, string Name, string RegionName, string? ImageUrl,
    IReadOnlySet<string> Features, int? Cluster, decimal? Rating, int? RatingCount,
    decimal DistanceMeters, bool Discovered);

/// <summary>A preference the user picked; a place matches it when it has any of [FeatureCodes].</summary>
public sealed record PreferenceGroup(string Name, IReadOnlySet<string> FeatureCodes)
{
    public bool Matches(RecommendationPlace place) => place.Features.Overlaps(FeatureCodes);
}

public sealed record CriterionScores(double? Preference, double? Cluster, double? Activity,
    double? Rating, double? Popularity);

public sealed record ScoredPlace(RecommendationPlace Place, int RequiredSteps, double Score,
    CriterionScores Criteria, IReadOnlyList<string> MatchedPreferences,
    IReadOnlyDictionary<string, double> EffectiveWeights);

/// <summary>
/// P/C/A/R/N ranking. Missing criteria are dropped and the remaining weights renormalised;
/// ties break on place id so the order is stable.
/// </summary>
public static class RecommendationScoring
{
    public const string Preference = "PREFERENCE";
    public const string Cluster = "CLUSTER";
    public const string Activity = "ACTIVITY";
    public const string Rating = "RATING";
    public const string Popularity = "POPULARITY";
    public const int MaxResults = 3;
    public static readonly IReadOnlySet<string> CriterionCodes = Set(Preference, Cluster, Activity, Rating, Popularity);

    /// <summary>The exact reviewed 35-column content vocabulary; unknown/empty vectors are not candidates.</summary>
    public static readonly IReadOnlySet<string> FeatureCodes = Set(
        "kind__nature", "kind__heritage", "kind__religious", "kind__museum", "kind__structure",
        "kind__market", "kind__craft", "kind__agriculture", "kind__community", "kind__recreation",
        "landform__coast", "landform__island", "landform__mountain", "landform__cave", "landform__river",
        "landform__lake", "landform__waterfall", "landform__stream", "landform__lagoon", "landform__dunes", "landform__valley",
        "ecosystem__forest", "ecosystem__flooded_habitat", "ecosystem__grassland",
        "theme__history", "theme__culture", "theme__architecture", "theme__spirituality", "theme__ecology",
        "theme__geology", "theme__craft", "theme__cuisine", "theme__games", "theme__relaxation", "theme__agriculture");

    /// <summary>T(u): conventional daily steps per activity level.</summary>
    public static readonly IReadOnlyDictionary<string, int> ActivityThresholds = new Dictionary<string, int>
    {
        ["LOW"] = 3000, ["MEDIUM"] = 6000, ["HIGH"] = 10000
    };

    private static IReadOnlySet<string> Set(params string[] codes) => codes.ToHashSet();

    /// <param name="places">Every active place with features: the reference set for C and N_max.</param>
    public static IReadOnlyList<ScoredPlace> Rank(IReadOnlyList<RecommendationPlace> places,
        IReadOnlyList<PreferenceGroup> preferences, string? activityLevel,
        IReadOnlyDictionary<string, decimal> weights, decimal distanceLimitMeters,
        IReadOnlyDictionary<string, int>? activityThresholds = null)
    {
        if (distanceLimitMeters <= 0) throw new ArgumentOutOfRangeException(nameof(distanceLimitMeters));
        if (!ValidWeights(weights)) throw new ArgumentException("Expected five nonnegative criterion weights summing to one", nameof(weights));
        // One canonical database ID per place. Invalid content never affects C or N_max.
        places = places.Where(p => p.PlaceId > 0 && p.Features.Count > 0 && p.Features.All(FeatureCodes.Contains))
            .DistinctBy(p => p.PlaceId).ToList();
        // P(u, p) for every place; C(u, c) is the mean P over the distinct places of a cluster.
        var preference = places.ToDictionary(p => p.PlaceId, p => PreferenceScore(p, preferences));
        var clusterScores = places.Where(p => p.Cluster is >= 0 && preference[p.PlaceId] is not null)
            .GroupBy(p => p.Cluster!.Value)
            .ToDictionary(g => g.Key, g => g.Average(p => preference[p.PlaceId]!.Value));

        // N_max comes from the reference set before the per-user distance and discovery filters.
        var maxCount = places.Where(HasRatingCount).Select(p => p.RatingCount!.Value).DefaultIfEmpty(0).Max();
        int? threshold = activityLevel is not null && (activityThresholds ?? ActivityThresholds).TryGetValue(activityLevel, out var t) ? t : null;

        var ranked = new List<ScoredPlace>();
        foreach (var place in places)
        {
            if (place.Discovered || place.DistanceMeters <= 0 || place.DistanceMeters > distanceLimitMeters) continue;
            var steps = StepConversion.RequiredSteps(place.DistanceMeters / 1000m);
            var criteria = new CriterionScores(
                preference[place.PlaceId],
                place.Cluster is int c && clusterScores.TryGetValue(c, out var cs) ? cs : null,
                threshold is int limit ? Math.Min(1.0, (double)limit / steps) : null,
                place.Rating is decimal r && r is >= 0 and <= 5 ? (double)(r / 5m) : null,
                HasRatingCount(place) && maxCount > 0
                    ? Math.Log(1 + place.RatingCount!.Value) / Math.Log(1 + maxCount) : null);
            var score = Combine(criteria, weights);
            if (score is null) continue;
            var matched = preferences.Where(g => g.Matches(place)).Select(g => g.Name).ToList();
            ranked.Add(new ScoredPlace(place, steps, score.Value, criteria, matched, EffectiveWeights(criteria, weights)));
        }
        return ranked.OrderByDescending(p => p.Score).ThenBy(p => p.Place.PlaceId).Take(MaxResults).ToList();
    }

    private static double? PreferenceScore(RecommendationPlace place, IReadOnlyList<PreferenceGroup> preferences) =>
        preferences.Count == 0 ? null
            : (double)preferences.Count(g => g.Matches(place)) / preferences.Count;

    // Migration 016 restores NULL for the reviewed missing counts. A verified zero remains real data.
    private static bool HasRatingCount(RecommendationPlace place) => place.RatingCount is >= 0;

    public static bool ValidWeights(IReadOnlyDictionary<string, decimal> weights) =>
        weights.Count == CriterionCodes.Count && weights.Keys.All(CriterionCodes.Contains)
        && weights.Values.All(w => w is >= 0 and <= 1) && weights.Values.Sum() == 1m;

    public static IEnumerable<(string Code, double? Value)> Criteria(CriterionScores scores) =>
    [
        (Preference, scores.Preference), (Cluster, scores.Cluster), (Activity, scores.Activity),
        (Rating, scores.Rating), (Popularity, scores.Popularity)
    ];

    public static bool HasScore(double? value) => value is double s && double.IsFinite(s) && s is >= 0 and <= 1;

    /// <summary>Only nonzero weights whose criterion has usable data take part in the score.</summary>
    public static IReadOnlyDictionary<string, double> EffectiveWeights(CriterionScores criteria,
        IReadOnlyDictionary<string, decimal> weights)
    {
        var available = Criteria(criteria).Where(c => HasScore(c.Value) && weights.TryGetValue(c.Code, out var w) && w > 0).ToList();
        var sum = available.Sum(c => weights[c.Code]);
        return sum > 0 ? available.ToDictionary(c => c.Code, c => (double)(weights[c.Code] / sum))
            : new Dictionary<string, double>();
    }

    /// <summary>S(u, p) = Σ w·s / Σ w over the criteria that have data.</summary>
    public static double? Combine(CriterionScores criteria, IReadOnlyDictionary<string, decimal> weights)
    {
        var effective = EffectiveWeights(criteria, weights);
        return effective.Count > 0
            ? Criteria(criteria).Where(c => effective.ContainsKey(c.Code)).Sum(c => effective[c.Code] * c.Value!.Value)
            : null;
    }
}
