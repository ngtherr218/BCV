# API inventory từ mã nguồn hiện tại

Ghép route group và route con theo module. Kiểm thử admin mới ở admin_management_report.md.

```text
130:app.MapGet("/health", async (NpgsqlDataSource db, CancellationToken cancellationToken) =>
14:        var group = app.MapGroup("/api/starting-point");
15:        group.MapGet("", async (HttpRequest request, ISessionStore sessions,
21:        group.MapPost("/resolve-current", async (HttpRequest request, CoordinatesRequest body,
27:        group.MapPut("", async (HttpRequest request, SaveStartingPointCommand body,
98:        group.MapGet("/exploration/overview", (AdminExploration service, CancellationToken ct) => Read(() => service.Overview(ct)));
99:        group.MapGet("/exploration/events", ([AsParameters] AdminScoreQuery query, AdminExploration service, CancellationToken ct) => Read(() => service.Events(query,ct)));
100:        group.MapGet("/exploration/leaderboard", ([AsParameters] LeaderboardQuery query, HttpContext http, LeaderboardRepository service, CancellationToken ct) =>
13:        app.MapPost("/api/admin/login", async (LoginCommand body, AccountService accounts, ISessionStore sessions, CancellationToken ct) =>
29:        var group = app.MapGroup("/api/admin");
66:        group.MapGet("/recommendations/dataset", (RecommendationAdmin service, CancellationToken ct) => service.Export(ct));
67:        group.MapPost("/recommendations/models", (ModelImport body, RecommendationAdmin service, CancellationToken ct) => service.Import(body, ct));
68:        group.MapPost("/recommendations/models/{id:long}/activate", (long id, RecommendationAdmin service, CancellationToken ct) => service.Activate(id, ct));
69:        group.MapGet("/me", (HttpRequest request, ISessionStore sessions) => Results.Ok(CurrentUser.Get(request, sessions)));
70:        group.MapGet("/dashboard", (int? days, AdminRepository repo, CancellationToken ct) => repo.Dashboard(days ?? 30, ct));
71:        group.MapGet("/lookups", (AdminRepository repo, CancellationToken ct) => repo.Lookups(ct));
72:        group.MapGet("/map/region", async (double latitude, double longitude, GoongGeocoder geocoder, CancellationToken ct) =>
79:        group.MapGet("/data/{resource}", (string resource, string? q, string? status, long? regionId, long? typeId, int? page, int? pageSize, AdminRepository repo, CancellationToken ct) =>
81:        group.MapGet("/data/{resource}/{id:long}", async (string resource, long id, AdminRepository repo, CancellationToken ct) =>
83:        group.MapPost("/data/{resource}", (string resource, System.Text.Json.JsonElement body, HttpContext http, AdminRepository repo, CancellationToken ct) =>
85:        group.MapPut("/data/{resource}/{id:long}", (string resource, long id, System.Text.Json.JsonElement body, HttpContext http, AdminRepository repo, CancellationToken ct) =>
87:        group.MapGet("/data/{resource}/{id:long}/delete-check", (string resource, long id, AdminRepository repo, CancellationToken ct) => repo.CheckDelete(resource, id, ct));
88:        group.MapDelete("/data/{resource}/{id:long}", async (string resource, long id, AdminRepository repo, CancellationToken ct) =>
95:        group.MapPost("/images", async (HttpContext http, string? purpose, IImageStorage storage, CancellationToken ct) =>
81:        admin.MapPost("/place-videos",async(HttpContext http,NpgsqlDataSource db,CancellationToken ct)=>
95:        admin.MapGet("/place-videos/{id:long}",async(long id,NpgsqlDataSource db,CancellationToken ct)=>
100:        app.MapGet("/api/explore/places/{placeId:long}/videos/{id:long}",async(long placeId,long id,HttpRequest request,ISessionStore sessions,NpgsqlDataSource db,CancellationToken ct)=>
37:        app.MapGet("/api/places/{placeId:long}/thumbnail", async (long placeId, NpgsqlDataSource db, CancellationToken ct) =>
62:        app.MapGet("/api/places/{placeId:long}/media", async (long placeId, bool? approvedOnly, HttpRequest request,
72:        app.MapPost("/api/places/{placeId:long}/media", async (long placeId, string? caption,
136:        app.MapGet("/api/community-media/{id:long}/content", async (long id, HttpRequest request,
161:        var admin = app.MapGroup("/api/admin/community-media");
174:        admin.MapGet("", (string? status,string? q,string? kind,bool? reported,int? page,int? pageSize,long? placeId,NpgsqlDataSource db,CancellationToken ct) =>
176:        admin.MapGet("/{id:long}/reports",(long id,NpgsqlDataSource db,CancellationToken ct)=>MediaModeration.ReportsAsync(db,id,ct));
177:        admin.MapPost("/{id:long}/review", async (long id, Review body,HttpContext http,NpgsqlDataSource db,CancellationToken ct) =>
182:        admin.MapPost("/users/{userId:long}/upload-access",async(long userId,UploadAccess body,HttpContext http,NpgsqlDataSource db,CancellationToken ct)=>
187:        app.MapPost("/api/community-media/{id:long}/reports",async(long id,ReportBody body,HttpRequest request,ISessionStore sessions,NpgsqlDataSource db,IConfiguration config,CancellationToken ct)=>
12:        app.MapPost("/api/cards/{cardId:long}/seen", async (long cardId, HttpRequest request,
29:        app.MapGet("/api/collections", async (HttpRequest request, ISessionStore sessions,
44:        app.MapGet("/api/collections/{collectionId:long}", async (long collectionId, HttpRequest request,
10:        app.MapGet("/api/account/profile", (HttpRequest request, ISessionStore sessions, ProfileService profile,
12:        app.MapPut("/api/account/profile", (UpdateProfileRequest body, HttpRequest request, ISessionStore sessions,
15:        app.MapPost("/api/account/delete", (HttpRequest request, ISessionStore sessions, ProfileService profile,
12:        var group = app.MapGroup("/api/interests");
13:        group.MapGet("", async (HttpRequest request, ISessionStore sessions, InterestService service, CancellationToken ct) =>
18:        group.MapPut("", async (HttpRequest request, SaveInterestsRequest body, ISessionStore sessions,
13:        app.MapPost("/api/explore/journeys", (HttpRequest request, CreateJourneyCommand command, ISessionStore sessions,
16:        app.MapPost("/api/explore/sync-steps", (HttpRequest request, SyncStepsCommand command, ISessionStore sessions,
19:        app.MapGet("/api/journeys/history", (HttpRequest request, ISessionStore sessions, JourneyService service,
12:        var group = app.MapGroup("/api/activity-level");
13:        group.MapGet("", async (HttpRequest request, ISessionStore sessions,
19:        group.MapPut("", async (HttpRequest request, SaveActivityLevelRequest body,
11:        var group = app.MapGroup("/api/account");
12:        group.MapPost("/register", async (RegisterRequest request, AccountService service, ILoggerFactory logs, CancellationToken ct) =>
14:        group.MapPost("/login", async (LoginRequest request, AccountService service, ILoggerFactory logs, CancellationToken ct) =>
16:        group.MapGet("/me", async (HttpRequest request, ISessionStore sessions, AccountService service, CancellationToken ct) =>
23:        group.MapPost("/password-reset/request", async (PasswordResetRequest request, PasswordResetService service,
26:        group.MapPost("/password-reset/verify", async (PasswordResetVerifyRequest request, PasswordResetService service,
29:        group.MapPost("/password-reset/complete", async (PasswordResetCompleteRequest request,
32:        group.MapPost("/change-password", async (ChangePasswordRequest body, HttpRequest request, ISessionStore sessions,
40:        group.MapPost("/logout", (HttpRequest request, ISessionStore sessions) =>
12:        app.MapGet("/api/profile/stats", (HttpRequest request, ISessionStore sessions, StatisticsRepository stats,
14:        app.MapGet("/api/notifications", (HttpRequest request, ISessionStore sessions, NotificationRepository notifications,
16:        app.MapPost("/api/notifications/read-all", (HttpRequest request, ISessionStore sessions,
19:        app.MapPost("/api/notifications/delete-all", (HttpRequest request,
22:        app.MapPost("/api/notifications/{notificationId:long}/read", (long notificationId, HttpRequest request,
13:        var group = app.MapGroup("/api/explore");
19:        app.MapGet("/api/leaderboard", async ([AsParameters] Bcv.Api.Modules.StatsNotify.Infrastructure.LeaderboardQuery query,
26:        group.MapGet("/filters", async (HttpRequest request, ISessionStore sessions,
29:        app.MapGet("/api/passport", async (HttpRequest request, ISessionStore sessions,
36:        group.MapGet("/places", async ([AsParameters] PlaceSearchQuery query, HttpRequest request, ISessionStore sessions,
42:        group.MapGet("", async (HttpRequest request, ISessionStore sessions,
49:        group.MapGet("/places/{placeId:long}", async (long placeId, HttpRequest request, ISessionStore sessions,
56:        group.MapGet("/recommendations", async (HttpRequest request, ISessionStore sessions,
12:        app.MapGet("/api/inventory", async (HttpRequest request, ISessionStore sessions,
13:        var group = app.MapGroup("/api/items");
14:        group.MapGet("/speed-shoes", (long? itemId, HttpRequest request, ISessionStore sessions, ItemUseRepository items,
16:        group.MapPost("/speed-shoes/use", (long? itemId, HttpRequest request, ISessionStore sessions, ItemUseRepository items,
18:        group.MapGet("/flight-ticket", (long? itemId, HttpRequest request, ISessionStore sessions, ItemUseRepository items,
20:        group.MapGet("/flight-ticket/regions/{regionId:long}/places", (long regionId, HttpRequest request,
23:        group.MapPost("/flight-ticket/use", (UsePlaceRequest body, long? itemId, HttpRequest request, ISessionStore sessions,
12:        app.MapGet("/api/quests", async (HttpRequest request, ISessionStore sessions,
```
