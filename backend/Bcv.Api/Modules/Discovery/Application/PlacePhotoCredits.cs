using System.Text.Json;

namespace Bcv.Api.Modules.Discovery.Application;

public sealed record PlaceImageCredit(string ImageUrl,string Author,string License,string SourceUrl,string LicenseUrl);

public static class PlacePhotoCredits
{
    private static readonly IReadOnlyList<PlaceImageCredit> Credits=Read();
    private static IReadOnlyList<PlaceImageCredit> Read() {
        using var source=typeof(PlacePhotoCredits).Assembly.GetManifestResourceStream("Bcv.PlacePhotoCredits.json")!;
        return JsonSerializer.Deserialize<List<PlaceImageCredit>>(source,new JsonSerializerOptions(JsonSerializerDefaults.Web))!;
    }
    public static IReadOnlyList<PlaceImageCredit> For(IEnumerable<string?> urls) {
        var set=urls.Where(x=>x is not null).ToHashSet();
        return Credits.Where(x=>set.Contains(x.ImageUrl)).ToList();
    }
}
