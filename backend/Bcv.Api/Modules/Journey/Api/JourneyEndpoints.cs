using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Journey.Application;
using Bcv.Api.Modules.Shared.Api;

namespace Bcv.Api.Modules.Journey.Api;

public static class JourneyEndpoints
{
    public static void MapJourneyEndpoints(this WebApplication app)
    {
        app.MapGet("/api/journeys/{journeyId:long}/completion",(long journeyId,HttpRequest request,ISessionStore sessions,
            Npgsql.NpgsqlDataSource db,ILoggerFactory logs,CancellationToken ct) =>
            Run(request,sessions,logs,user=>Bcv.Api.Modules.Journey.Infrastructure.JourneyCompletion.LoadAsync(db,user,journeyId,ct)));
        // Kept under /api/explore: the app's explore screen starts journeys and syncs steps.
        app.MapPost("/api/explore/journeys", (HttpRequest request, CreateJourneyCommand command, ISessionStore sessions,
            JourneyService service, ILoggerFactory logs, CancellationToken ct) =>
            Run(request, sessions, logs, user => service.CreateJourneyAsync(user, command, ct)));
        app.MapPost("/api/explore/sync-steps", (HttpRequest request, SyncStepsCommand command, ISessionStore sessions,
            JourneyService service, ILoggerFactory logs, CancellationToken ct) =>
            Run(request, sessions, logs, user => service.SyncStepsAsync(user, command, ct)));
        app.MapGet("/api/journeys/history", (HttpRequest request, ISessionStore sessions, JourneyService service,
            ILoggerFactory logs, CancellationToken ct) =>
            Run(request, sessions, logs, async user => new { journeys = await service.HistoryAsync(user, ct) }));
    }

    private static async Task<IResult> Run<T>(HttpRequest request, ISessionStore sessions, ILoggerFactory logs,
        Func<long, Task<T>> action)
    {
        var user = CurrentUser.Get(request, sessions);
        if (user is null) return Results.Unauthorized();
        request.HttpContext.Response.Headers.CacheControl = "no-store";
        try { return Results.Ok(await action(user.UserId)); }
        catch (JourneyException ex)
        {
            return Results.Json(new ApiError(ex.Code, ex.Message), statusCode: 400);
        }
        catch (Exception ex)
        {
            logs.CreateLogger("Journey").LogError(ex, "Journey request failed");
            return Results.Json(new ApiError("SERVER_ERROR", "Máy chủ đang gặp lỗi. Vui lòng thử lại."), statusCode: 500);
        }
    }
}
