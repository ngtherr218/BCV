using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Shared.Api;
using Bcv.Api.Modules.StatsNotify.Infrastructure;

namespace Bcv.Api.Modules.StatsNotify.Api;

public static class StatsNotifyEndpoints
{
    public static void MapStatsNotifyEndpoints(this WebApplication app)
    {
        app.MapGet("/api/profile/stats", (HttpRequest request, ISessionStore sessions, StatisticsRepository stats,
            ILoggerFactory logs, CancellationToken ct) => Run(request, sessions, logs, user => stats.StatsAsync(user, ct)));
        app.MapGet("/api/notifications", (HttpRequest request, ISessionStore sessions, NotificationRepository notifications,
            ILoggerFactory logs, CancellationToken ct) => Run(request, sessions, logs, user => notifications.NotificationsAsync(user, ct)));
        app.MapPost("/api/notifications/read-all", (HttpRequest request, ISessionStore sessions,
            NotificationRepository notifications, ILoggerFactory logs, CancellationToken ct) =>
            Run(request, sessions, logs, user => notifications.MarkAllReadAsync(user, ct)));
        app.MapPost("/api/notifications/delete-all", (HttpRequest request,
            ISessionStore sessions, NotificationRepository notifications, ILoggerFactory logs, CancellationToken ct) =>
            Run(request, sessions, logs, user => notifications.DeleteAllAsync(user, ct)));
        app.MapPost("/api/notifications/{notificationId:long}/read", (long notificationId, HttpRequest request,
            ISessionStore sessions, NotificationRepository notifications, ILoggerFactory logs, CancellationToken ct) =>
            Run(request, sessions, logs, user => notifications.MarkReadAsync(user, notificationId, ct)));
    }

    private static async Task<IResult> Run<T>(HttpRequest request, ISessionStore sessions, ILoggerFactory logs,
        Func<long, Task<T>> action)
    {
        var user = CurrentUser.Get(request, sessions);
        if (user is null) return Results.Unauthorized();
        request.HttpContext.Response.Headers.CacheControl = "no-store";
        try { return Results.Ok(await action(user.UserId)); }
        catch (Exception ex)
        {
            logs.CreateLogger("StatsNotify").LogError(ex, "Statistics or notification request failed");
            return Results.Json(new ApiError("SERVER_ERROR", "Máy chủ đang gặp lỗi. Vui lòng thử lại."), statusCode: 500);
        }
    }
}
