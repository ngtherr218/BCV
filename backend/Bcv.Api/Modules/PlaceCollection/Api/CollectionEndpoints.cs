using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.PlaceCollection.Infrastructure;
using Bcv.Api.Modules.Shared.Api;

namespace Bcv.Api.Modules.PlaceCollection.Api;

public static class CollectionEndpoints
{
    public static void MapCollectionEndpoints(this WebApplication app)
    {
        app.MapPost("/api/cards/{cardId:long}/seen", async (long cardId, HttpRequest request,
            ISessionStore sessions, CollectionRepository repository, ILoggerFactory logs, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            request.HttpContext.Response.Headers.CacheControl = "no-store";
            try
            {
                await repository.MarkSeenAsync(user.UserId, cardId, ct);
                return Results.Ok(new { success = true });
            }
            catch (Exception ex)
            {
                logs.CreateLogger("Collection").LogError(ex, "Mark card seen failed");
                return Results.Json(new ApiError("SERVER_ERROR", "Chưa lưu được trạng thái thẻ."), statusCode: 500);
            }
        });
        app.MapGet("/api/collections", async (HttpRequest request, ISessionStore sessions,
            CollectionRepository repository, ILoggerFactory logs, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            // Per-user ownership: never cached by intermediaries.
            request.HttpContext.Response.Headers.CacheControl = "no-store";
            try { return Results.Ok(await repository.LoadAsync(user.UserId, ct)); }
            catch (Exception ex)
            {
                logs.CreateLogger("Collection").LogError(ex, "Collection request failed");
                return Results.Json(new ApiError("SERVER_ERROR", "Máy chủ đang gặp lỗi. Vui lòng thử lại."),
                    statusCode: 500);
            }
        });
        app.MapGet("/api/collections/{collectionId:long}", async (long collectionId, HttpRequest request,
            ISessionStore sessions, CollectionRepository repository, ILoggerFactory logs, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            request.HttpContext.Response.Headers.CacheControl = "no-store";
            try
            {
                var detail = await repository.LoadDetailAsync(user.UserId, collectionId, ct);
                return detail is null
                    ? Results.Json(new ApiError("COLLECTION_NOT_FOUND", "Không tìm thấy bộ sưu tập."), statusCode: 404)
                    : Results.Ok(detail);
            }
            catch (Exception ex)
            {
                logs.CreateLogger("Collection").LogError(ex, "Collection detail request failed");
                return Results.Json(new ApiError("SERVER_ERROR", "Máy chủ đang gặp lỗi. Vui lòng thử lại."),
                    statusCode: 500);
            }
        });
    }
}
