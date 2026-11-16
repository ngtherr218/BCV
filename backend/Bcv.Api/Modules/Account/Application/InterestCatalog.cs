using System.Text.Json;

namespace Bcv.Api.Modules.Account.Application;

/// <summary>The twelve standard types from 014_standard_place_types.sql; legacy groups stay valid.</summary>
public static class InterestCatalog
{
    public static readonly IReadOnlyDictionary<long, string[]> Features = new Dictionary<long, string[]>
    {
        [101] = ["kind__heritage", "kind__museum"],
        [102] = ["kind__religious"],
        [103] = ["kind__structure"],
        [104] = ["kind__community", "kind__craft"],
        [105] = ["kind__market", "theme__cuisine"],
        [106] = ["kind__recreation"],
        [107] = ["kind__agriculture"],
        [108] = ["landform__coast", "landform__island", "landform__lagoon", "landform__dunes"],
        [109] = ["landform__river", "landform__lake", "landform__waterfall", "landform__stream"],
        [110] = ["landform__mountain", "landform__valley"],
        [111] = ["landform__cave"],
        [112] = ["ecosystem__forest", "ecosystem__flooded_habitat", "ecosystem__grassland"]
    };
    public static string FeaturesJson => JsonSerializer.Serialize(Features);
}
