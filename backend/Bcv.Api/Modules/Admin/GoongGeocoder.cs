using System.Globalization;
using System.Text.Json;

namespace Bcv.Api.Modules.Admin;

public sealed record PinAddress(string? Province, string? LegacyProvince, string? Address);

public sealed class GoongGeocoder(HttpClient client, IConfiguration configuration)
{
    public async Task<PinAddress> Reverse(double latitude, double longitude, CancellationToken ct)
    {
        if (!double.IsFinite(latitude) || !double.IsFinite(longitude) || Math.Abs(latitude) > 90 || Math.Abs(longitude) > 180)
            throw new AdminValidation("Tọa độ không hợp lệ.");
        var key = configuration["Goong:ApiKey"];
        if (string.IsNullOrWhiteSpace(key))
            throw new AdminValidation("Chưa cấu hình Goong REST API Key trên backend để xác định khu vực. Bạn có thể chọn khu vực trong biểu mẫu.");
        var coordinates = latitude.ToString(CultureInfo.InvariantCulture) + "," + longitude.ToString(CultureInfo.InvariantCulture);
        // Keep the key server-side. Do not log the request URL or provider error body.
        using var response = await client.GetAsync("https://rsapi.goong.io/v2/geocode?latlng=" + coordinates +
            "&limit=1&has_deprecated_administrative_unit=true&api_key=" + Uri.EscapeDataString(key), ct);
        if (!response.IsSuccessStatusCode) throw new AdminValidation("Không tra được khu vực từ Goong. Bạn có thể thử lại hoặc chọn khu vực trong biểu mẫu.");
        using var document = JsonDocument.Parse(await response.Content.ReadAsStringAsync(ct));
        var root = document.RootElement;
        if (!root.TryGetProperty("status", out var status) || status.GetString() != "OK" ||
            !root.TryGetProperty("results", out var results) || results.GetArrayLength() == 0)
            return new(null, null, null);
        var result = results[0];
        return new(Province(result, "compound"), Province(result, "deprecated_compound"),
            result.TryGetProperty("formatted_address", out var address) ? address.GetString() : null);
    }

    private static string? Province(JsonElement result, string property) =>
        result.TryGetProperty(property, out var compound) && compound.ValueKind == JsonValueKind.Object &&
        compound.TryGetProperty("province", out var province) ? province.GetString() : null;
}
