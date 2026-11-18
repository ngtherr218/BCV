using Bcv.Api.Modules.Discovery.Domain;

namespace Bcv.Api.Modules.Discovery.Application;

public sealed record RecommendationSettings(string DatasetVersion, string ModelVersion,
    string ModelSha256, string MappingVersion, Dictionary<string,int> ActivityThresholds,
    Dictionary<long,string[]> InterestFeatures)
{
    public bool IsValid() => !string.IsNullOrWhiteSpace(DatasetVersion) && DatasetVersion.Length <= 100
        && !string.IsNullOrWhiteSpace(ModelVersion) && ModelVersion.Length <= 100
        && !string.IsNullOrWhiteSpace(MappingVersion) && MappingVersion.Length <= 100
        && ModelSha256 is { Length: 64 } && ModelSha256.All(Uri.IsHexDigit)
        && ActivityThresholds is { Count: 3 }
        && new[]{"LOW","MEDIUM","HIGH"}.All(k => ActivityThresholds.TryGetValue(k,out var v) && v>0 && v<=100000);
    // InterestFeatures is no longer used: users pick place types, matched through place_type_feature.
}
