using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.QuestItem.Application;
using Bcv.Api.Modules.QuestItem.Infrastructure;
using Bcv.Api.Modules.Shared.Api;

namespace Bcv.Api.Modules.QuestItem.Api;

public static class ItemUseEndpoints
{
    public static void MapItemUseEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/items");
        group.MapGet("/speed-shoes", (long? itemId, HttpRequest request, ISessionStore sessions, ItemUseRepository items,
            ILoggerFactory logs, CancellationToken ct) => Run(request, sessions, logs, user => items.ShoesAsync(user, ct, itemId)));
        group.MapPost("/speed-shoes/use", (long? itemId, HttpRequest request, ISessionStore sessions, ItemUseRepository items,
            ILoggerFactory logs, CancellationToken ct) => Run(request, sessions, logs, user => items.UseShoesAsync(user, ct, itemId)));
        group.MapGet("/flight-ticket", (long? itemId, HttpRequest request, ISessionStore sessions, ItemUseRepository items,
            ILoggerFactory logs, CancellationToken ct) => Run(request, sessions, logs, user => items.FlightAsync(user, ct, itemId)));
        group.MapGet("/flight-ticket/regions/{regionId:long}/places", (long regionId, HttpRequest request,
            ISessionStore sessions, ItemUseRepository items, ILoggerFactory logs, CancellationToken ct) =>
            Run(request, sessions, logs, async user => new { places = await items.RegionPlacesAsync(user, regionId, ct) }));
        group.MapPost("/flight-ticket/use", (UsePlaceRequest body, long? itemId, HttpRequest request, ISessionStore sessions,
            ItemUseRepository items, ILoggerFactory logs, CancellationToken ct) =>
            Run(request, sessions, logs, user => items.UseFlightAsync(user, body.PlaceId, ct, itemId)));
    }

    private static async Task<IResult> Run<T>(HttpRequest request, ISessionStore sessions, ILoggerFactory logs,
        Func<long, Task<T>> action)
    {
        var user = CurrentUser.Get(request, sessions);
        if (user is null) return Results.Unauthorized();
        request.HttpContext.Response.Headers.CacheControl = "no-store";
        try { return Results.Ok(await action(user.UserId)); }
        catch (ItemUseException ex)
        {
            return Results.Json(new ApiError(ex.Code, ex.Message), statusCode: 409);
        }
        catch (Exception ex)
        {
            logs.CreateLogger("ItemUse").LogError(ex, "Item request failed");
            return Results.Json(new ApiError("SERVER_ERROR", "Máy chủ đang gặp lỗi. Vui lòng thử lại."),
                statusCode: 500);
        }
    }
}
