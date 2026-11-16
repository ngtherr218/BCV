using System.Net;
using Bcv.Api.Modules.PlaceMedia;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Bcv.Api.Modules.Admin;
using Npgsql;

void Check(bool condition, string message) { if (!condition) throw new Exception(message); }
Check(!PlaceMediaEndpoints.ValidHeader("image/jpeg", new byte[16]), "Accepted fake JPEG");
Check(!PlaceMediaEndpoints.ValidHeader("video/mp4", Array.Empty<byte>()), "Accepted empty video");
Check(!PlaceMediaEndpoints.ValidHeader("text/html", System.Text.Encoding.UTF8.GetBytes("<html>hello world</html>")), "Accepted active HTML");
Check(PlaceMediaEndpoints.ValidHeader("image/png", new byte[]{137,80,78,71,13,10,26,10,0,0,0,0}), "Rejected PNG header");
Check(PlaceMediaEndpoints.ValidHeader("video/mp4", System.Text.Encoding.ASCII.GetBytes("0000ftypisom")), "Rejected MP4 header");

var builder = WebApplication.CreateBuilder();
builder.Logging.ClearProviders();
builder.WebHost.UseUrls("http://127.0.0.1:0");
builder.Services.AddSingleton(NpgsqlDataSource.Create("Host=127.0.0.1;Port=1;Database=not_used;Username=fixture;Timeout=1"));
builder.Services.AddSingleton<ISessionStore, TestSessions>();
builder.Services.AddScoped<AdminRepository>();
await using var app = builder.Build();
app.MapPlaceMedia();
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
// Community uploads were retired: their routes no longer exist.
foreach (var route in new[]{"/api/places/1/media","/api/community-media/1/content","/api/admin/community-media"}) {
    using var response = await client.GetAsync(route);
    Check(response.StatusCode == HttpStatusCode.NotFound, "Retired community route still answers: " + route);
}
await app.StopAsync();
Console.WriteLine("PASS: file signatures and retired community routes. No live database used.");

sealed class TestSessions : ISessionStore {
    public PublicAccount? Get(string? token) => token == "fixture" ? new PublicAccount(1,"Fixture","fixture@example.invalid","USER") : null;
    public AccountSession Create(AccountUser user) => throw new NotSupportedException();
    public void Revoke(string? token) { }
    public void RevokeAll(long id) { }
}
