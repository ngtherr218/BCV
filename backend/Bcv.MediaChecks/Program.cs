using System.Net;
using System.Net.Http.Headers;
using Bcv.Api.Modules.CommunityMedia;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Bcv.Api.Modules.Admin;
using Npgsql;

void Check(bool condition, string message) { if (!condition) throw new Exception(message); }
Check(!CommunityMediaEndpoints.ValidHeader("image/jpeg", new byte[16]), "Accepted fake JPEG");
Check(!CommunityMediaEndpoints.ValidHeader("video/mp4", Array.Empty<byte>()), "Accepted empty video");
Check(!CommunityMediaEndpoints.ValidHeader("text/html", System.Text.Encoding.UTF8.GetBytes("<html>hello world</html>")), "Accepted active HTML");
Check(CommunityMediaEndpoints.ValidHeader("image/png", new byte[]{137,80,78,71,13,10,26,10,0,0,0,0}), "Rejected PNG header");
Check(CommunityMediaEndpoints.ValidHeader("video/mp4", System.Text.Encoding.ASCII.GetBytes("0000ftypisom")), "Rejected MP4 header");

var builder = WebApplication.CreateBuilder();
builder.Logging.ClearProviders();
builder.WebHost.UseUrls("http://127.0.0.1:0");
builder.Services.AddSingleton(NpgsqlDataSource.Create("Host=127.0.0.1;Port=1;Database=not_used;Username=fixture;Timeout=1"));
builder.Services.AddSingleton<ISessionStore, TestSessions>();
builder.Services.AddScoped<AdminRepository>();
await using var app = builder.Build();
app.MapCommunityMedia();
using (var scope = app.Services.CreateScope()) {
    var repository = scope.ServiceProvider.GetRequiredService<AdminRepository>();
    foreach (var resource in new[] { "users", "items" }) {
        try {
            await repository.Delete(resource, 1, default);
            throw new Exception("Deletion must be blocked for " + resource);
        } catch (AdminValidation error) {
            Check(error.Message.Contains("Không hỗ trợ xóa"), "Wrong deletion guard");
        }
    }
}
Console.WriteLine("PASS: users/items deletion rejected before any database operation.");
await app.StartAsync();
using var client = new HttpClient { BaseAddress = new Uri(app.Urls.Single()) };
foreach (var route in new[]{"/api/places/1/media","/api/community-media/1/content","/api/admin/community-media"}) {
    using var response = await client.GetAsync(route);
    Check(response.StatusCode == HttpStatusCode.Unauthorized, "Anonymous media access was not rejected");
}
using (var response = await client.PostAsync("/api/places/1/media", new ByteArrayContent(new byte[12])))
    Check(response.StatusCode == HttpStatusCode.Unauthorized, "Anonymous upload was not rejected");
using (var response = await client.PostAsJsonAsync("/api/admin/community-media/1/review", new { status="APPROVED" }))
    Check(response.StatusCode == HttpStatusCode.Unauthorized, "Anonymous approval was not rejected");
using (var response = await client.PostAsJsonAsync("/api/community-media/1/reports", new { reason="Report" }))
    Check(response.StatusCode == HttpStatusCode.Unauthorized, "Anonymous report was not rejected");
client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", "fixture");
using (var response = await client.PostAsync("/api/places/1/media", new StringContent("<html>bad</html>")))
    Check(response.StatusCode == HttpStatusCode.BadRequest, "Unsupported upload MIME accepted");
using (var bytes = new ByteArrayContent(new byte[16])) {
    bytes.Headers.ContentType = new MediaTypeHeaderValue("image/jpeg");
    using var response = await client.PostAsync("/api/places/1/media", bytes);
    Check(response.StatusCode == HttpStatusCode.BadRequest, "Spoofed JPEG accepted");
}
using (var bytes = new ByteArrayContent(System.Text.Encoding.ASCII.GetBytes("0000ftypisom0"))) {
    bytes.Headers.ContentType = new MediaTypeHeaderValue("video/mp4");
    using var response = await client.PostAsync("/api/places/1/media", bytes);
    Check(response.StatusCode == HttpStatusCode.BadRequest, "Header-only MP4 accepted");
}
using (var bytes = new ByteArrayContent(new byte[]{26,69,223,163,0,0,0,0,0,0,0,0})) {
    bytes.Headers.ContentType = new MediaTypeHeaderValue("video/webm");
    using var response = await client.PostAsync("/api/places/1/media", bytes);
    Check(response.StatusCode == HttpStatusCode.BadRequest, "Malformed WebM upload accepted");
}
using (var bytes = new ByteArrayContent(new byte[16])) {
    bytes.Headers.ContentType = new MediaTypeHeaderValue("video/mp4");
    using var response = await client.PostAsync("/api/places/1/media?caption="+new string('x',501),bytes);
    Check(response.StatusCode == HttpStatusCode.BadRequest, "Oversized caption accepted");
}
await app.StopAsync();
Console.WriteLine("PASS: file signatures, anonymous list/upload/review protection, invalid MIME/content and caption limits. No live database used.");

sealed class TestSessions : ISessionStore {
    public PublicAccount? Get(string? token) => token == "fixture" ? new PublicAccount(1,"Fixture","fixture@example.invalid","USER") : null;
    public AccountSession Create(AccountUser user) => throw new NotSupportedException();
    public void Revoke(string? token) { }
    public void RevokeAll(long id) { }
}
