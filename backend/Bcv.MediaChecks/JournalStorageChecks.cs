using System.Net;
using Bcv.Api.Modules.Shared.Infrastructure;
using Microsoft.Extensions.Logging.Abstractions;

static class JournalStorageChecks
{
    public static async Task RunAsync()
    {
        var handler = new StorageHandler();
        using var client = new HttpClient(handler);
        var r2 = new R2ImageStorage(new R2Settings("fixture", "private-journal", "access", "secret", "https://unused.invalid"),
            client, NullLogger<R2ImageStorage>.Instance);
        var storage = new JournalPhotoStorage(r2, NullLogger<JournalPhotoStorage>.Instance);
        foreach (var (mime, extension) in new[] { ("image/jpeg", "jpg"), ("image/png", "png"), ("image/webp", "webp") })
        {
            var key = await storage.SaveAsync(42, handler.Bytes, mime, default);
            Require(key!.StartsWith("journals/42/") && key.EndsWith("." + extension), "Invalid journal key");
            Require(handler.Mime == mime && handler.Stored!.SequenceEqual(handler.Bytes), "Upload changed MIME or bytes");
            Require((await storage.ReadAsync(key, default)).SequenceEqual(handler.Bytes), "Read changed bytes");
            await storage.DeleteAsync(key);
            Require(handler.Deleted, "Object not deleted");
        }
        handler.Fail = true;
        try { await storage.SaveAsync(42, handler.Bytes, "image/png", default); throw new Exception("Upload failure was hidden"); }
        catch (InvalidOperationException ex) when (ex.Message.StartsWith("R2 upload failed:")) { }
        var inline = new JournalPhotoStorage(null, NullLogger<JournalPhotoStorage>.Instance);
        Require(await inline.SaveAsync(42, handler.Bytes, "image/png", default) is null, "Missing R2 must preserve DB fallback");
        Console.WriteLine("PASS: journal R2 upload/read/delete, MIME preservation, upload failure and database fallback.");
    }

    static void Require(bool ok, string message) { if (!ok) throw new Exception(message); }

    sealed class StorageHandler : HttpMessageHandler
    {
        public byte[] Bytes = [137, 80, 78, 71, 1, 2, 3];
        public byte[]? Stored;
        public string? Mime;
        public bool Deleted, Fail;
        protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
        {
            Require(request.RequestUri!.Host == "fixture.r2.cloudflarestorage.com" &&
                request.RequestUri.AbsolutePath.StartsWith("/private-journal/journals/42/"), "Wrong bucket or owner");
            Require(request.Headers.Contains("Authorization"), "Missing S3 signature");
            if (Fail) return new(HttpStatusCode.Forbidden) { Content = new StringContent("fixture failure") };
            if (request.Method == HttpMethod.Put)
            {
                Mime = request.Content!.Headers.ContentType!.MediaType;
                Stored = await request.Content.ReadAsByteArrayAsync(ct);
            }
            if (request.Method == HttpMethod.Delete) Deleted = true;
            return new(HttpStatusCode.OK) { Content = new ByteArrayContent(Bytes) };
        }
    }
}
