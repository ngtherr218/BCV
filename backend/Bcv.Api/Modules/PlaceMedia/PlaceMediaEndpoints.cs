using Npgsql;

namespace Bcv.Api.Modules.PlaceMedia;

/// <summary>
/// Official place media (place_media: photos and videos, admin-managed): the map thumbnail and stored images.
/// The schema is db/030_place_media.sql (and 029, which drops the retired community uploads).
/// </summary>
public static class PlaceMediaEndpoints
{
    public static void MapPlaceMedia(this WebApplication app)
    {
        // Keep base64 images out of the map JSON; load each visible image separately.
        app.MapGet("/api/places/{placeId:long}/thumbnail", async (long placeId, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("""
                SELECT i.image_key FROM place_media i JOIN place p ON p.place_id=i.place_id
                JOIN region r ON r.region_id=p.region_id
                WHERE i.media_type='IMAGE' AND p.place_id=$1 AND p.status='ACTIVE' AND r.status='ACTIVE'
                ORDER BY i.media_id LIMIT 1
                """);
            command.Parameters.AddWithValue(placeId);
            return StoredImage(await command.ExecuteScalarAsync(ct) as string);
        });
    }

    /// <summary>
    /// Serves a stored image value: an https URL redirects to cloud storage; an inline data URI
    /// (jpeg/png/webp, at most 8 MB) is decoded and served as a file once its bytes match the type.
    /// </summary>
    public static IResult StoredImage(string? value)
    {
        if (string.IsNullOrEmpty(value)) return Results.NotFound();
        if (value.StartsWith("https://", StringComparison.OrdinalIgnoreCase)) return Results.Redirect(value);
        var split = value.IndexOf(",", StringComparison.Ordinal);
        if (split < 0 || !value.StartsWith("data:", StringComparison.Ordinal) || value.Length > 8 * 1024 * 1024)
            return Results.NotFound();
        var mime = value[5..split].Replace(";base64", "");
        if (mime is not ("image/jpeg" or "image/png" or "image/webp")) return Results.NotFound();
        try
        {
            var bytes = Convert.FromBase64String(value[(split + 1)..]);
            return ValidHeader(mime, bytes) ? Results.File(bytes, mime) : Results.NotFound();
        }
        catch (FormatException) { return Results.NotFound(); }
    }

    /// <summary>Checks the file's magic bytes match the declared type.</summary>
    public static bool ValidHeader(string mime, byte[] bytes) => bytes.Length >= 12 && mime switch
    {
        "image/jpeg" => bytes[0] == 255 && bytes[1] == 216 && bytes[2] == 255,
        "image/png" => bytes.AsSpan(0, 8).SequenceEqual(new byte[] { 137, 80, 78, 71, 13, 10, 26, 10 }),
        "image/webp" => System.Text.Encoding.ASCII.GetString(bytes, 0, 4) == "RIFF" && System.Text.Encoding.ASCII.GetString(bytes, 8, 4) == "WEBP",
        "video/mp4" => System.Text.Encoding.ASCII.GetString(bytes, 4, 4) == "ftyp",
        "video/webm" => bytes[0] == 26 && bytes[1] == 69 && bytes[2] == 223 && bytes[3] == 163,
        _ => false
    };
}

/// <summary>Hourly: removes official videos an administrator uploaded but never attached to a place.</summary>
public sealed class PlaceVideoMaintenance(NpgsqlDataSource db, ILogger<PlaceVideoMaintenance> log) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer = new PeriodicTimer(TimeSpan.FromHours(1));
        do
        {
            try
            {
                await using var command = db.CreateCommand(
                    "DELETE FROM place_media WHERE media_type='VIDEO' AND place_id IS NULL AND created_at < CURRENT_TIMESTAMP - INTERVAL '24 hours'");
                await command.ExecuteNonQueryAsync(stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested) { return; }
            catch (Exception error) { log.LogError(error, "Failed to clean unattached place videos"); }
        } while (await timer.WaitForNextTickAsync(stoppingToken));
    }
}
