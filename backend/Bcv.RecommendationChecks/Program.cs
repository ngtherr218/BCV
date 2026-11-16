using System.Text.Json;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Discovery.Domain;
using Bcv.Api.Modules.StepLocation.Domain;
using Bcv.Api.Modules.Admin;

using Scores = Bcv.Api.Modules.Discovery.Domain.RecommendationScoring;

Dictionary<string, decimal> Weights(string? only = null) => new()
{
    [Scores.Preference] = only is null ? .4m : only == Scores.Preference ? 1m : 0m,
    [Scores.Cluster] = only is null ? .2m : only == Scores.Cluster ? 1m : 0m,
    [Scores.Activity] = only is null ? .2m : only == Scores.Activity ? 1m : 0m,
    [Scores.Rating] = only is null ? .1m : only == Scores.Rating ? 1m : 0m,
    [Scores.Popularity] = only is null ? .1m : only == Scores.Popularity ? 1m : 0m
};
HashSet<string> Features(params string[] codes) => codes.ToHashSet();
PreferenceGroup Preference(string code = "kind__heritage") => new("Di tích", Features(code));
RecommendationPlace Place(long id, decimal meters = 1000, int? cluster = 0, decimal? rating = null,
    int? count = null, bool discovered = false, string feature = "kind__heritage") =>
    new(id, $"Địa danh {id}", "Khu vực", null, Features(feature), cluster, rating, count, meters, discovered);
IReadOnlyList<ScoredPlace> Rank(IReadOnlyList<RecommendationPlace> places,
    IReadOnlyDictionary<string, decimal>? weights = null, string? activity = "LOW",
    PreferenceGroup? preference = null, decimal limit = 500000) =>
    Scores.Rank(places, [preference ?? Preference()], activity, weights ?? Weights(), limit);
void Assert(bool condition, string message = "Unexpected result")
{
    if (!condition) throw new InvalidOperationException(message);
}
void Near(double actual, double expected) => Assert(Math.Abs(actual - expected) < 1e-10, $"{actual} != {expected}");
RecommendationInput Input(IReadOnlyList<RecommendationPlace>? places = null,
    IReadOnlyDictionary<string, decimal>? weights = null, string? activity = "LOW",
    IReadOnlyList<PreferenceGroup>? preferences = null) =>
    new("Khu vực", 21, 105, activity, preferences ?? [Preference()],
        new RecommendationConfig(1, "test", 500000, weights ?? Weights()), places ?? [Place(1)]);
RecommendationResult Recommend(RecommendationInput? input) =>
    new RecommendationService(new FixedRepository(input)).RecommendAsync(1, CancellationToken.None).GetAwaiter().GetResult();
void Error(string code, Action action)
{
    try { action(); }
    catch (ExploreException error) { Assert(error.Code == code, $"{error.Code} != {code}"); return; }
    throw new InvalidOperationException($"Expected {code}");
}

var checks = new (string Name, Action Run)[]
{
    ("Twelve standard interest types use valid model features including museums and craft", () => {
        var catalog=Bcv.Api.Modules.Account.Application.InterestCatalog.Features;
        Assert(catalog.Count==12 && Enumerable.Range(101,12).All(id=>catalog.ContainsKey(id)));
        Assert(catalog.Values.All(codes=>codes.Length>0 && codes.All(Scores.FeatureCodes.Contains)));
        Assert(catalog[101].Contains("kind__museum") && catalog[104].Contains("kind__craft"));
    }),
    ("Weekly quests and ranking agree at Vietnam midnight and ISO year boundaries", () => {
        var instants = new[] { "2026-11-08T16:59:59Z", "2026-11-08T17:00:00Z", "2026-12-31T17:00:00Z", "2027-01-03T16:59:59Z", "2027-01-03T17:00:00Z" };
        foreach (var value in instants) {
            var instant = DateTimeOffset.Parse(value);
            var cycle = Bcv.Api.Modules.QuestItem.Domain.QuestCycles.At(instant);
            var period = Bcv.Api.Modules.StatsNotify.Infrastructure.ExplorerWeek.Resolve(null, instant);
            Assert(cycle.Weekly == period.Key && instant >= period.Start && instant < period.End);
            Assert(cycle.Daily == instant.ToOffset(TimeSpan.FromHours(7)).ToString("yyyy-MM-dd"));
        }
        Assert(Bcv.Api.Modules.QuestItem.Domain.QuestCycles.At(DateTimeOffset.Parse("2027-01-01T00:00:00Z")).Weekly == "2026-W53");
    }),
    ("Vietnam ranking weeks use Monday boundaries and ISO years", () => {
        var sunday=Bcv.Api.Modules.StatsNotify.Infrastructure.ExplorerWeek.Resolve(null,DateTimeOffset.Parse("2026-11-08T16:59:59Z"));
        var monday=Bcv.Api.Modules.StatsNotify.Infrastructure.ExplorerWeek.Resolve(null,DateTimeOffset.Parse("2026-11-08T17:00:00Z"));
        Assert(sunday.End==monday.Start && monday.Key=="2026-W46");
        var january=Bcv.Api.Modules.StatsNotify.Infrastructure.ExplorerWeek.Resolve(null,DateTimeOffset.Parse("2027-01-01T00:00:00Z"));
        Assert(january.Key=="2026-W53" && january.End-january.Start==TimeSpan.FromDays(7));
        Error("INVALID_WEEK",()=>Bcv.Api.Modules.StatsNotify.Infrastructure.ExplorerWeek.Resolve("2026-W54",DateTimeOffset.UtcNow));
    }),
    ("Discovery mix reserves one valid daily-target destination outside interests", () => {
        var candidates = new[]{Place(1),Place(2),Place(3),Place(4,feature:"landform__coast"),Place(4,feature:"landform__coast")};
        var result=Scores.Rank(candidates,[Preference()],"LOW",Weights(),500000,discoveryMix:true);
        Assert(result.Select(p=>p.Place.PlaceId).SequenceEqual(new long[]{1,2,4}));
        Assert(result.Select(p=>p.Place.PlaceId).Distinct().Count()==3);
        Assert(result[2].MatchedPreferences.Count==0 && result[2].RequiredSteps<=3000);
    }),
    ("Discovery mix cannot promote discovered, distant or over-target surprise places", () => {
        var candidates = new[]{Place(1),Place(2),Place(3),Place(4,meters:100000,feature:"landform__coast"),Place(5,discovered:true,feature:"landform__coast"),Place(6,meters:500001,feature:"landform__coast")};
        var result=Scores.Rank(candidates,[Preference()],"LOW",Weights(),500000,discoveryMix:true);
        Assert(result.Select(p=>p.Place.PlaceId).SequenceEqual(new long[]{1,2,3}));
    }),
    ("Discovery mix uses versioned thresholds and preserves genuine model scores", () => {
        var candidates = new[]{Place(1),Place(2),Place(3),Place(4,meters:2000,feature:"landform__coast")};
        var thresholds=new Dictionary<string,int>{{"LOW",1000},{"MEDIUM",6000},{"HIGH",10000}};
        var result=Scores.Rank(candidates,[Preference()],"LOW",Weights(),500000,thresholds,discoveryMix:true);
        Assert(result.All(p=>p.Place.PlaceId!=4));
        var mixed=Scores.Rank(candidates,[Preference()],"LOW",Weights(),500000,discoveryMix:true);
        var surprise=mixed.Single(p=>p.Place.PlaceId==4);
        Near(surprise.Score,Scores.Combine(surprise.Criteria,Weights())!.Value);
        Assert(surprise.Score<mixed[0].Score);
    }),
    ("Discovery mix handles fewer candidates and no matching interests", () => {
        var result=Scores.Rank([Place(1,feature:"landform__coast")],[Preference()],"LOW",Weights(),500000,discoveryMix:true);
        Assert(result.Count==1 && result[0].Place.PlaceId==1);
        Assert(Scores.Rank([], [Preference()],"LOW",Weights(),500000,discoveryMix:true).Count==0);
    }),
    ("Activity thresholds are versioned rather than fixed at 3000", () => {
        var places = new[]{Place(1, meters:3000)};
        var custom = new Dictionary<string,int>{{"LOW",1000},{"MEDIUM",6000},{"HIGH",10000}};
        var result=Scores.Rank(places,[Preference()],"LOW",Weights(Scores.Activity),500000,custom);
        Assert(result[0].Criteria.Activity == .25);
    }),
    ("Model import rejects malformed settings before database access", () => {
        try { RecommendationAdmin.Validate(new ModelImport("bad",15,500000,null!,Weights(),[])); }
        catch(AdminValidation) { return; }
        throw new Exception("Invalid import accepted");
    }),
    ("Vocabulary matches the real 35-column dataset", () => {
        var columns = File.ReadLines(Path.Combine(AppContext.BaseDirectory, "features.csv")).First().Split(',').Skip(4).ToHashSet();
        Assert(columns.SetEquals(Scores.FeatureCodes));
    }),
    ("Top three are unique and ties have stable place-ID order", () => {
        var result = Rank([Place(5), Place(2), Place(2), Place(4), Place(1)], Weights(Scores.Preference));
        Assert(result.Select(p => p.Place.PlaceId).SequenceEqual(new long[] { 1, 2, 4 }));
    }),
    ("Discovered, zero-distance and out-of-range places are filtered", () => {
        var result = Rank([Place(1, discovered: true), Place(2, 0), Place(3, 500001), Place(4, 500000)]);
        Assert(result.Count == 1 && result[0].Place.PlaceId == 4);
    }),
    ("Empty or unknown feature vectors cannot become candidates", () => {
        var invalid = Place(1) with { Features = Features() };
        Assert(Rank([invalid, Place(2, feature: "unknown__feature")]).Count == 0);
    }),
    ("Changing interests changes ranking across different clusters", () => {
        var places = new[] { Place(1), Place(2, cluster: 1, feature: "landform__coast") };
        Assert(Rank(places, Weights(Scores.Preference))[0].Place.PlaceId == 1);
        Assert(Rank(places, Weights(Scores.Preference), preference: Preference("landform__coast"))[0].Place.PlaceId == 2);
    }),
    ("Changing activity level changes the effort ranking", () => {
        // Existing production code uses kappa=1: 6 km / 1.5 km require 8,000 / 2,000 steps.
        var places = new[] { Place(1, 6000), Place(2, 1500) };
        Assert(Rank(places, Weights(Scores.Activity), activity: "LOW")[0].Place.PlaceId == 2);
        Assert(Rank(places, Weights(Scores.Activity), activity: "HIGH")[0].Place.PlaceId == 1);
    }),
    ("Cluster affinity and popularity use the reference set before user filters", () => {
        var places = new[] { Place(1, count: 100), Place(2, count: 10000, discovered: true, feature: "landform__coast") };
        var result = Rank(places)[0];
        Near(result.Criteria.Cluster!.Value, .5);
        Near(result.Criteria.Popularity!.Value, Math.Log(101) / Math.Log(10001));
    }),
    ("Missing rating/count renormalizes P C A to .5 .25 .25", () => {
        var result = Rank([Place(1)])[0];
        Assert(result.Criteria.Rating is null && result.Criteria.Popularity is null);
        Near(result.EffectiveWeights[Scores.Preference], .5);
        Near(result.EffectiveWeights[Scores.Cluster], .25);
        Near(result.EffectiveWeights[Scores.Activity], .25);
        Near(result.Score, 1);
    }),
    ("Verified zero count is real zero; missing rating remains missing", () => {
        var result = Rank([Place(1, count: 0), Place(2, count: 10)]).Single(p => p.Place.PlaceId == 1);
        Assert(result.Criteria.Rating is null && result.Criteria.Popularity == 0);
        Assert(result.EffectiveWeights.ContainsKey(Scores.Popularity));
    }),
    ("Popularity is unavailable when no positive reference count exists", () => {
        Assert(Rank([Place(1, count: 0)])[0].Criteria.Popularity is null);
    }),
    ("Zero weights do not appear as used criteria or explanatory reasons", () => {
        var result = Recommend(Input([Place(1, rating: 5, count: 100)], Weights(Scores.Activity))).Places[0];
        Assert(result.UsedCriteria.SequenceEqual(new[] { Scores.Activity }));
        Assert(result.MissingCriteria.Count == 0);
        Assert(result.Reasons.All(reason => !reason.Contains("đánh giá") && !reason.Contains("sở thích")));
    }),
    ("No available weighted criterion returns an empty list", () => {
        Assert(Rank([Place(1)], Weights(Scores.Rating)).Count == 0);
    }),
    ("Absent clusters and invalid rating values are treated as missing", () => {
        var result = Rank([Place(1, cluster: null, rating: 9)])[0];
        Assert(result.Criteria.Cluster is null && result.Criteria.Rating is null);
        Near(result.EffectiveWeights.Values.Sum(), 1);
    }),
    ("API score is explained by effective weights and exposes genuine missing data", () => {
        var place = Recommend(Input()).Places[0];
        Assert(place.MissingCriteria.SequenceEqual(new[] { Scores.Rating, Scores.Popularity }));
        Assert(place.Reasons.Any(reason => reason.Contains("Di tích")));
        Near(place.EffectiveWeights.Values.Sum(), 1);
        var json = JsonSerializer.Serialize(place, new JsonSerializerOptions(JsonSerializerDefaults.Web));
        Assert(json.Contains("\"effectiveWeights\"") && json.Contains("\"reasons\""));
    }),
    ("Unusable numeric scores cannot produce NaN or infinity", () => {
        var value = Scores.Combine(new CriterionScores(double.NaN, null, 1, null, null), Weights());
        Assert(value == 1);
    }),
    ("Invalid weights or distance configuration fail explicitly", () => {
        var invalid = Weights(); invalid[Scores.Activity] = -.2m;
        Error("INVALID_RECOMMENDATION_CONFIG", () => Recommend(Input(weights: invalid)));
        invalid = Weights(); invalid.Remove(Scores.Activity);
        Error("INVALID_RECOMMENDATION_CONFIG", () => Recommend(Input(weights: invalid)));
        var input = Input();
        Error("INVALID_RECOMMENDATION_CONFIG", () => Recommend(input with { Config = input.Config with { DistanceLimitMeters = 0 } }));
    }),
    ("Missing onboarding and virtual location return actionable errors", () => {
        Error("NO_VIRTUAL_LOCATION", () => Recommend(null));
        Error("NO_RECOMMENDATION_PREFERENCES", () => Recommend(Input(preferences: [])));
        Error("NO_ACTIVITY_LEVEL", () => Recommend(Input(activity: null)));
    }),
    ("Required steps preserve current kappa=1 with a minimum of one", () => {
        Assert(StepConversion.RequiredSteps(50) == 66667);
        Assert(StepConversion.RequiredSteps(0) == 1);
    })
};

var failures = 0;
foreach (var check in checks)
{
    try { check.Run(); Console.WriteLine($"PASS {check.Name}"); }
    catch (Exception exception) { failures++; Console.Error.WriteLine($"FAIL {check.Name}: {exception.Message}"); }
}
Console.WriteLine($"{checks.Length - failures}/{checks.Length} recommendation checks passed");
return failures == 0 ? 0 : 1;

sealed class FixedRepository(RecommendationInput? input) : IRecommendationRepository
{
    public Task<RecommendationInput?> LoadAsync(long userId, CancellationToken ct) => Task.FromResult(input);
}
