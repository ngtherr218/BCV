using System.Text.Json;
using Bcv.Api.Modules.Account.Infrastructure;
using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.Discovery.Domain;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Configuration;
using Npgsql;

void Check(bool condition, string message) { if (!condition) throw new Exception(message); }
var geocoderConfiguration = new ConfigurationBuilder().AddInMemoryCollection(
    new Dictionary<string, string?> { ["Goong:ApiKey"] = "test-key" }).Build();
var handler = new GeocodeStub();
var geocoder = new GoongGeocoder(new HttpClient(handler), geocoderConfiguration);
var pinAddress = await geocoder.Reverse(20.8975369, 106.427671, default);
Check(pinAddress.Province == "Hải Phòng" && pinAddress.LegacyProvince == "Hải Dương", "Lost old/new province from Goong");
Check(handler.RequestUri!.Contains("latlng=20.8975369,106.427671") &&
    handler.RequestUri.Contains("has_deprecated_administrative_unit=true"), "Wrong reverse-geocode query");
try { await geocoder.Reverse(double.NaN, 106, default); throw new Exception("Accepted invalid pin"); }
catch (AdminValidation) { }
handler.Payload = """{"status":"ZERO_RESULTS","results":[]}""";
Check((await geocoder.Reverse(21, 105, default)).Province is null, "Invented a region for no results");
var unconfiguredGeocoder = new GoongGeocoder(new HttpClient(handler), new ConfigurationBuilder().Build());
try { await unconfiguredGeocoder.Reverse(21, 105, default); throw new Exception("Accepted missing REST key"); }
catch (AdminValidation) { }
Console.WriteLine("PASS: reverse geocoding preserves old/new provinces, validates coordinates, handles empty results and missing key.");
var lines = File.ReadAllLines("recommendation/service/data/features.csv");
var columns = lines[0].Split(',');
foreach (var line in lines.Skip(1))
{
    var cells = line.Split(',');
    var codes = Enumerable.Range(4, columns.Length - 4).Where(i => cells[i] == "1").Select(i => columns[i]).ToHashSet();
    Check(PlaceFeatureClassifier.Classify(codes) == int.Parse(cells[3]), $"Wrong cluster: {cells[0]}");
}
Console.WriteLine($"PASS: classifier matches all {lines.Length - 1} reviewed model labels.");
try { PlaceFeatureClassifier.Classify(new HashSet<string>()); throw new Exception("Accepted empty features"); }
catch (ArgumentException) { }
try { PlaceFeatureClassifier.Classify(new HashSet<string> { "unknown" }); throw new Exception("Accepted unknown feature"); }
catch (ArgumentException) { }
var connectionString = Environment.GetEnvironmentVariable("BCV_CHECK_CONNECTION");
if (string.IsNullOrEmpty(connectionString)) return;
await using var db = NpgsqlDataSource.Create(connectionString);
var sessions = new FileSessionStore(Path.Combine(Path.GetTempPath(), "bcv-feature-check-sessions.json"), NullLogger<FileSessionStore>.Instance);
var repo = new AdminRepository(db, sessions);
var name = "feature-check-" + Guid.NewGuid().ToString("N");
long? createdId = null;
var options = JsonSerializer.SerializeToElement(await repo.Lookups(default));
var regionId = options.GetProperty("regions")[0].GetProperty("id").GetInt64();
var typeId = options.GetProperty("types")[0].GetProperty("id").GetInt64();
JsonElement Body(string[] features) => JsonSerializer.SerializeToElement(new {
    name, region_id=regionId, place_type_id=typeId, latitude=21.0272, longitude=105.8355,
    introduction="", highlights="", rating_count=0, status="DRAFT", images=Array.Empty<string>(), feature_codes=features
});
try
{
    var result = JsonSerializer.SerializeToElement(await repo.Save("places", null, Body(["kind__heritage", "theme__history"]), 0, default));
    createdId = result.GetProperty("id").GetInt64();
    var detail = (await repo.Detail("places", createdId.Value, default))!.Value;
    Check(detail.GetProperty("feature_codes").GetArrayLength() == 2, "Feature round trip failed");
    await using (var command = db.CreateCommand("SELECT count(*),count(DISTINCT pf.cluster_number) FROM place_feature pf JOIN place_feature_value fv USING(place_feature_id) WHERE pf.place_id=$1"))
    {
        command.Parameters.AddWithValue(createdId.Value);
        await using var reader = await command.ExecuteReaderAsync(); await reader.ReadAsync();
        Check(reader.GetInt64(0) == 35 && reader.GetInt64(1) == 1, "Missing vector/cluster");
    }
    await repo.Save("places", createdId, Body(["kind__museum"]), 0, default);
    detail = (await repo.Detail("places", createdId.Value, default))!.Value;
    Check(detail.GetProperty("feature_codes")[0].GetString() == "kind__museum", "Update left stale features");
    try { await repo.Save("places", createdId, Body([]), 0, default); throw new Exception("Accepted empty vector"); }
    catch (AdminValidation) { }
    detail = (await repo.Detail("places", createdId.Value, default))!.Value;
    Check(detail.GetProperty("feature_codes")[0].GetString() == "kind__museum", "Invalid save did not roll back");
    var lookups = JsonSerializer.SerializeToElement(await repo.Lookups(default));
    Check(lookups.GetProperty("features").GetArrayLength() == 35, "Missing feature options");
    Console.WriteLine("PASS: database create, 35 values, cluster, detail, update, rollback, lookups.");
}
finally { if (createdId is long id) await repo.Delete("places", id, default); }
Console.WriteLine("PASS: temporary place and owned feature rows removed.");

sealed class GeocodeStub : HttpMessageHandler
{
    public string? RequestUri { get; private set; }
    public string Payload { get; set; } = """{"status":"OK","results":[{"compound":{"province":"Hải Phòng"},"deprecated_compound":{"province":"Hải Dương"},"formatted_address":"Thanh Hà, Hải Phòng"}]}""";
    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
    {
        RequestUri = request.RequestUri!.OriginalString;
        return Task.FromResult(new HttpResponseMessage(System.Net.HttpStatusCode.OK) { Content = new StringContent(Payload) });
    }
}
