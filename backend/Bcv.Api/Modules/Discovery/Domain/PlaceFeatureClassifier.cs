using System.Text.Json;

namespace Bcv.Api.Modules.Discovery.Domain;

/// <summary>Fixed 35-feature Lloyd model; never refit clusters when editing a place.</summary>
public static class PlaceFeatureClassifier
{
    private sealed record Snapshot(string[] Features, int[] Clusters, double[][] Centers);
    private static readonly Snapshot Model = Load();
    public static IReadOnlyList<string> Features => Model.Features;

    private static Snapshot Load()
    {
        using var stream = typeof(PlaceFeatureClassifier).Assembly.GetManifestResourceStream(
            "Bcv.Api.Modules.Discovery.Domain.recommendation-centroids.json")!;
        return JsonSerializer.Deserialize<Snapshot>(stream)!;
    }

    public static int Classify(IReadOnlySet<string> features)
    {
        if (features.Count == 0 || features.Any(code => !Model.Features.Contains(code)))
            throw new ArgumentException("Choose at least one supported content feature.");
        var vector = Model.Features.Select(code => features.Contains(code) ? 1.0 : 0.0).ToArray();
        foreach (var prefix in new[] { "kind__", "landform__", "ecosystem__", "theme__" })
        {
            var indices = Enumerable.Range(0, vector.Length).Where(i => Model.Features[i].StartsWith(prefix, StringComparison.Ordinal)).ToArray();
            var norm = Math.Sqrt(indices.Sum(i => vector[i] * vector[i]));
            if (norm > 0) foreach (var i in indices) vector[i] = vector[i] / norm * 0.5;
        }
        return Enumerable.Range(0, Model.Centers.Length)
            .OrderBy(c => vector.Select((v, i) => Math.Pow(v - Model.Centers[c][i], 2)).Sum())
            .ThenBy(c => Model.Clusters[c]).Select(c => Model.Clusters[c]).First();
    }
}
