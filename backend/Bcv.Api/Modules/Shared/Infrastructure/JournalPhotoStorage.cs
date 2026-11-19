namespace Bcv.Api.Modules.Shared.Infrastructure;

// Never returns public URLs to clients. Use R2:JournalBucket with public access disabled
// for privacy at the storage layer, independently of the public artwork bucket.
public sealed class JournalPhotoStorage(R2ImageStorage? r2, ILogger<JournalPhotoStorage> log)
{
    public async Task<string?> SaveAsync(long userId, byte[] bytes, string mime, CancellationToken ct)
    {
        if (r2 is null) return null;
        var extension = mime switch { "image/png" => "png", "image/webp" => "webp", _ => "jpg" };
        var key = $"journals/{userId}/{Guid.NewGuid():N}.{extension}";
        await r2.PutObjectAsync(key, bytes, mime, ct);
        return key;
    }

    public Task<byte[]> ReadAsync(string key, CancellationToken ct) =>
        r2?.ReadObjectAsync(key, ct)
        ?? throw new InvalidOperationException("R2 must be configured to read journal photos stored in R2.");

    public async Task DeleteAsync(string? key)
    {
        if (key is null) return;
        try
        {
            if (r2 is null) throw new InvalidOperationException("R2 is not configured.");
            using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(20));
            await r2.DeleteObjectAsync(key, timeout.Token);
        }
        catch (Exception ex) { log.LogWarning(ex, "Journal R2 object cleanup failed: {Key}", key); }
    }
}
