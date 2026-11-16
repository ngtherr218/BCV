using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Shared.Api;
using Bcv.Api.Modules.Discovery.Infrastructure;

namespace Bcv.Api.Modules.Discovery.Api;

public static class ExploreEndpoints
{
    public static void MapExploreEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/explore");
        group.AddEndpointFilter(async (context,next) =>
        {
            context.HttpContext.Response.Headers.CacheControl="no-store";
            return await next(context);
        });
        app.MapGet("/api/leaderboard", async ([AsParameters] Bcv.Api.Modules.StatsNotify.Infrastructure.LeaderboardQuery query,
            HttpRequest request, ISessionStore sessions, Bcv.Api.Modules.StatsNotify.Infrastructure.LeaderboardRepository repository, ILoggerFactory logs, CancellationToken ct) =>
        {
            request.HttpContext.Response.Headers.CacheControl="no-store";
            var user=CurrentUser.Get(request,sessions);
            return user is null ? Results.Unauthorized() : await Execute(()=>repository.LoadAsync(user.UserId,query,ct),logs);
        });
        group.MapGet("/filters", async (HttpRequest request, ISessionStore sessions,
            PlaceSearchRepository repository, ILoggerFactory logs, CancellationToken ct) =>
            CurrentUser.Get(request,sessions) is null ? Results.Unauthorized() : await Execute(() => repository.OptionsAsync(ct),logs));
        app.MapGet("/api/passport", async (HttpRequest request, ISessionStore sessions,
            Bcv.Api.Modules.PlaceCollection.Infrastructure.PassportRepository repository, ILoggerFactory logs, CancellationToken ct) =>
        {
            request.HttpContext.Response.Headers.CacheControl="no-store";
            var user = CurrentUser.Get(request, sessions);
            return user is null ? Results.Unauthorized() : await Execute(() => repository.LoadAsync(user.UserId, ct), logs);
        });
        group.MapGet("/places", async ([AsParameters] PlaceSearchQuery query, HttpRequest request, ISessionStore sessions,
            PlaceSearchRepository repository, ILoggerFactory logs, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            return user is null ? Results.Unauthorized() : await Execute(() => repository.SearchAsync(user.UserId, query, ct), logs);
        });
        group.MapGet("", async (HttpRequest request, ISessionStore sessions,
            ExploreService service, ILoggerFactory logs, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            return user is null ? Results.Unauthorized() :
                await Execute(() => service.LoadAsync(user.UserId, ct), logs);
        });
        group.MapGet("/places/{placeId:long}", async (long placeId, HttpRequest request, ISessionStore sessions,
            ExploreService service, ILoggerFactory logs, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            return user is null ? Results.Unauthorized() :
                await Execute(() => service.LoadPlaceAsync(user.UserId, placeId, ct), logs);
        });
        group.MapGet("/recommendations", async (HttpRequest request, ISessionStore sessions,
            RecommendationService service, ILoggerFactory logs, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            return user is null ? Results.Unauthorized() :
                await Execute(() => service.RecommendAsync(user.UserId, ct), logs);
        });
    }

    private static async Task<IResult> Execute<T>(Func<Task<T>> action, ILoggerFactory logs)
    {
        try { return Results.Ok(await action()); }
        catch (ExploreException ex)
        {
            return Results.Json(new ApiError(ex.Code, ex.Message), statusCode: 400);
        }
        catch (Exception ex)
        {
            logs.CreateLogger("Explore").LogError(ex, "Explore request failed");
            return Results.Json(new ApiError("SERVER_ERROR", "Máy chủ đang gặp lỗi. Vui lòng thử lại."),
                statusCode: 500);
        }
    }
}
