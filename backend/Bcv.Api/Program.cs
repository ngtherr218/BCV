using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Infrastructure;
using Bcv.Api.Modules.Discovery.Api;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Discovery.Infrastructure;
using Bcv.Api.Modules.Journey.Api;
using Bcv.Api.Modules.Journey.Application;
using Bcv.Api.Modules.Journey.Infrastructure;
using Bcv.Api.Modules.PlaceCollection.Api;
using Bcv.Api.Modules.PlaceCollection.Infrastructure;
using Bcv.Api.Modules.QuestItem.Api;
using Bcv.Api.Modules.QuestItem.Infrastructure;
using Bcv.Api.Modules.StatsNotify.Api;
using Bcv.Api.Modules.StatsNotify.Infrastructure;
using Bcv.Api.Modules.StepLocation.Api;
using Bcv.Api.Modules.StepLocation.Application;
using Bcv.Api.Modules.StepLocation.Infrastructure;
using Npgsql;
using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.Shared.Infrastructure;

var builder = WebApplication.CreateBuilder(args);
// Cloud hosts such as Render assign the public listening port at runtime.
if (builder.Configuration["PORT"] is { Length: > 0 } publicPort)
{
    if (!int.TryParse(publicPort, out var port) || port is < 1 or > 65535)
        throw new InvalidOperationException("PORT must be an integer from 1 to 65535.");
    builder.WebHost.UseUrls($"http://0.0.0.0:{port}");
}
var connectionString = builder.Configuration.GetConnectionString("Bcv")
    ?? throw new InvalidOperationException("Set ConnectionStrings__Bcv before starting the API.");

builder.Services.AddSingleton(NpgsqlDataSource.Create(connectionString));
builder.Services.AddScoped<AdminRepository>();
builder.Services.AddScoped<RecommendationAdmin>();
builder.Services.AddHttpClient<GoongGeocoder>(client => client.Timeout = TimeSpan.FromSeconds(10))
    .RemoveAllLoggers();
builder.Services.AddScoped<IAccountRepository, PostgresAccountRepository>();
builder.Services.AddScoped<AccountService>();
// Sign-ins survive restarts. The file holds only token hashes and lives outside the repository.
var sessionFile = builder.Configuration["Sessions:FilePath"] ?? Path.Combine(
    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "BCV", "sessions.json");
builder.Services.AddSingleton<ISessionStore>(services =>
    new FileSessionStore(sessionFile, services.GetRequiredService<ILogger<FileSessionStore>>()));
builder.Services.AddSingleton<IPasswordResetStore, InMemoryPasswordResetStore>();
builder.Services.AddScoped<PasswordResetService>();
var smtp = builder.Configuration.GetSection("Smtp");
if (!string.IsNullOrWhiteSpace(smtp["Host"]))
{
    builder.Services.AddSingleton(new SmtpSettings(smtp["Host"]!, smtp.GetValue("Port", 587), smtp["User"],
        smtp["Password"], smtp["From"] ?? smtp["User"] ?? "no-reply@buocchanviet.local", smtp.GetValue("EnableSsl", true)));
    builder.Services.AddSingleton<IPasswordResetMailer, SmtpPasswordResetMailer>();
}
else
{
    builder.Services.AddSingleton<IPasswordResetMailer, LogPasswordResetMailer>();
}
builder.Services.AddScoped<IInterestRepository, PostgresInterestRepository>();
builder.Services.AddScoped<InterestService>();
builder.Services.AddScoped<IActivityLevelRepository, PostgresActivityLevelRepository>();
builder.Services.AddScoped<ActivityLevelService>();
builder.Services.AddScoped<IStartingPointRepository, PostgresStartingPointRepository>();
builder.Services.AddScoped<StartingPointService>();
builder.Services.AddScoped<IProfileRepository, PostgresProfileRepository>();
builder.Services.AddScoped<ProfileService>();
builder.Services.AddScoped<IExploreRepository, PostgresExploreRepository>();
builder.Services.AddScoped<ExploreService>();
builder.Services.AddScoped<IRecommendationRepository, PostgresRecommendationRepository>();
builder.Services.AddScoped<RecommendationService>();
builder.Services.AddScoped<IJourneyRepository, PostgresJourneyRepository>();
builder.Services.AddScoped<JourneyService>();
builder.Services.AddScoped<CollectionRepository>();
builder.Services.AddScoped<QuestRepository>();
builder.Services.AddScoped<InventoryRepository>();
builder.Services.AddScoped<ItemUseRepository>();
builder.Services.AddScoped<StatisticsRepository>();
builder.Services.AddScoped<NotificationRepository>();
// Avatars, place photos and card art go to Cloudflare R2 (one folder per purpose, see ImageFolder) when
// R2__* is set (see run-local.ps1); otherwise they stay inline in the database.
var r2 = builder.Configuration.GetSection("R2");
if (new[] { "AccountId", "Bucket", "AccessKeyId", "SecretAccessKey", "PublicBaseUrl" }.All(k => !string.IsNullOrWhiteSpace(r2[k])))
{
    builder.Services.AddSingleton(new R2Settings(r2["AccountId"]!, r2["Bucket"]!, r2["AccessKeyId"]!,
        r2["SecretAccessKey"]!, r2["PublicBaseUrl"]!));
    builder.Services.AddHttpClient<IImageStorage, R2ImageStorage>();
}
else
{
    builder.Services.AddSingleton<IImageStorage, InlineImageStorage>();
}

var app = builder.Build();
app.UseDefaultFiles();
app.UseStaticFiles();
// Explicit CLI operation; the public registration endpoint never grants ADMIN permissions.
if (Array.IndexOf(args, "--create-admin") is var adminIndex && adminIndex >= 0)
{
    if (adminIndex + 1 >= args.Length) throw new InvalidOperationException("Provide an email after --create-admin.");
    var email = args[adminIndex + 1].Trim().ToLowerInvariant();
    var password = Environment.GetEnvironmentVariable("BCV_ADMIN_PASSWORD") ?? "";
    if (!System.Net.Mail.MailAddress.TryCreate(email, out var address) || address.Address != email || email.Length > 255)
        throw new InvalidOperationException("Invalid administrator email.");
    if (password.Length < 12 || password.Length > 128 || !password.Any(char.IsLetter) || !password.Any(char.IsDigit))
        throw new InvalidOperationException("BCV_ADMIN_PASSWORD must contain 12–128 characters including letters and digits.");
    await using var command = app.Services.GetRequiredService<NpgsqlDataSource>().CreateCommand("""
        INSERT INTO app_user(full_name,email,password_hash,role,status)
        VALUES('Quản trị viên',$1,$2,'ADMIN','ACTIVE')
        ON CONFLICT(email) DO NOTHING RETURNING user_id
        """);
    command.Parameters.AddWithValue(email);
    command.Parameters.AddWithValue(Bcv.Api.Modules.Account.Domain.PasswordHasher.Hash(password));
    var created = await command.ExecuteScalarAsync();
    if (created is null) throw new InvalidOperationException("Email already exists; no account or permissions were changed.");
    Console.WriteLine("Administrator created successfully.");
    return;
}
// The deployment image includes the Vite build. Local development still uses Vite's proxy.
if (Directory.Exists(Path.Combine(app.Environment.ContentRootPath, "wwwroot")))
{
    app.UseDefaultFiles();
    app.UseStaticFiles();
}
app.MapGet("/health", async (NpgsqlDataSource db, CancellationToken cancellationToken) =>
{
    try
    {
        await using var command = db.CreateCommand("SELECT 1");
        await command.ExecuteScalarAsync(cancellationToken);
        return Results.Ok(new { status = "ok" });
    }
    catch
    {
        return Results.Problem("Database unavailable", statusCode: 503);
    }
});
// Modules as in the architecture: Account & User, Step & Virtual Location, Discovery, Journey,
// Place & Collection, Quest & Item, Statistics & Notification.
app.MapAccountEndpoints();
app.MapInterestEndpoints();
app.MapActivityLevelEndpoints();
app.MapProfileEndpoints();
app.MapStartingPointEndpoints();
app.MapExploreEndpoints();
app.MapJourneyEndpoints();
app.MapCollectionEndpoints();
app.MapQuestEndpoints();
app.MapInventoryEndpoints();
app.MapItemUseEndpoints();
app.MapStatsNotifyEndpoints();
app.MapAdminEndpoints();
Bcv.Api.Modules.CommunityMedia.CommunityMediaEndpoints.MapCommunityMedia(app);
await AccountSchema.EnsureAsync(app.Services.GetRequiredService<NpgsqlDataSource>());
await Bcv.Api.Modules.PlaceCollection.Infrastructure.CollectionSchema.EnsureAsync(app.Services.GetRequiredService<NpgsqlDataSource>());
await Bcv.Api.Modules.StatsNotify.Infrastructure.NotificationSchema.EnsureAsync(app.Services.GetRequiredService<NpgsqlDataSource>());
await RecommendationSchema.EnsureAsync(app.Services.GetRequiredService<NpgsqlDataSource>());
await Bcv.Api.Modules.CommunityMedia.CommunityMediaEndpoints.EnsureAsync(app.Services.GetRequiredService<NpgsqlDataSource>());
await Bcv.Api.Modules.Admin.PlaceTypeFeatureSchema.EnsureAsync(app.Services.GetRequiredService<NpgsqlDataSource>());
app.Run();
