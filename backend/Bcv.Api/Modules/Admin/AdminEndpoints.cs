using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Npgsql;
using Bcv.Api.Modules.Shared.Infrastructure;

namespace Bcv.Api.Modules.Admin;

public static class AdminEndpoints
{
    public static void MapAdminEndpoints(this WebApplication app)
    {
        app.MapPost("/api/admin/login", async (LoginCommand body, AccountService accounts, ISessionStore sessions, CancellationToken ct) =>
        {
            try
            {
                var session = await accounts.Login(body, ct, requireAdmin: true);
                if (session.User.Role != "ADMIN")
                {
                    sessions.Revoke(session.Token);
                    return Results.Json(new { code = "FORBIDDEN", message = "Tài khoản không có quyền quản trị." }, statusCode: 403);
                }
                return Results.Ok(session);
            }
            catch (AccountException ex) { return Results.Json(new { code = ex.Code, message = ex.Message }, statusCode: ex.Code == "FORBIDDEN" ? 403 : 401); }
            catch (NpgsqlException) { return Results.Json(new { code = "DATABASE_UNAVAILABLE", message = "Chưa kết nối được cơ sở dữ liệu. Kiểm tra cấu hình backend." }, statusCode: 503); }
        });

        var group = app.MapGroup("/api/admin");
        group.AddEndpointFilter(async (context, next) =>
        {
            var services = context.HttpContext.RequestServices;
            var sessions = services.GetRequiredService<ISessionStore>();
            var user = CurrentUser.Get(context.HttpContext.Request, sessions);
            if (user is null) return Results.Json(new { code = "UNAUTHORIZED", message = "Phiên đăng nhập đã hết hạn." }, statusCode: 401);
            try
            {
                var repository = services.GetRequiredService<AdminRepository>();
                if (!await repository.IsAdmin(user.UserId, context.HttpContext.RequestAborted))
                    return Results.Json(new { code = "FORBIDDEN", message = "Bạn không có quyền quản trị hoặc tài khoản đã bị khóa." }, statusCode: 403);
                context.HttpContext.Items["AdminId"] = user.UserId;
                return await next(context);
            }
            catch (AdminValidation ex) { return Results.Json(new { code = "VALIDATION_ERROR", message = ex.Message }, statusCode: 400); }
            catch (KeyNotFoundException ex) { return Results.Json(new { code = "NOT_FOUND", message = ex.Message }, statusCode: 404); }
            catch (PostgresException ex) when (ex.SqlState is "23505" or "23503" or "23514" or "22001" or "22003")
            {
                var message = ex.SqlState switch
                {
                    "23505" => "Dữ liệu bị trùng. Mỗi địa danh chỉ có một thẻ; tên loại địa danh phải duy nhất.",
                    "23503" => "Dữ liệu liên kết không tồn tại hoặc đang được sử dụng. Hãy tải lại danh sách.",
                    _ => "Dữ liệu không đáp ứng ràng buộc. Kiểm tra các trường số và độ dài."
                };
                return Results.Json(new { code = "DATA_CONFLICT", message }, statusCode: 409);
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                services.GetRequiredService<ILoggerFactory>().CreateLogger("Admin").LogError(ex, "Admin request failed");
                return Results.Json(new { code = "SERVER_ERROR", message = "Không thể xử lý yêu cầu. Vui lòng thử lại." }, statusCode: 500);
            }
        });

        // These internal/offline routes inherit the ACTIVE ADMIN filter above.
        group.MapGet("/recommendations/dataset", (RecommendationAdmin service, CancellationToken ct) => service.Export(ct));
        group.MapPost("/recommendations/models", (ModelImport body, RecommendationAdmin service, CancellationToken ct) => service.Import(body, ct));
        group.MapPost("/recommendations/models/{id:long}/activate", (long id, RecommendationAdmin service, CancellationToken ct) => service.Activate(id, ct));
        group.MapGet("/me", (HttpRequest request, ISessionStore sessions) => Results.Ok(CurrentUser.Get(request, sessions)));
        group.MapGet("/dashboard", (int? days, AdminRepository repo, CancellationToken ct) => repo.Dashboard(days ?? 30, ct));
        group.MapGet("/lookups", (AdminRepository repo, CancellationToken ct) => repo.Lookups(ct));
        group.MapGet("/map/region", async (double latitude, double longitude, GoongGeocoder geocoder, CancellationToken ct) =>
        {
            try { return Results.Ok(await geocoder.Reverse(latitude, longitude, ct)); }
            catch (HttpRequestException) { return Results.Json(new { message = "Không kết nối được Goong để xác định khu vực. Bạn có thể chọn khu vực trong biểu mẫu." }, statusCode: 503); }
            catch (OperationCanceledException) when (!ct.IsCancellationRequested)
            { return Results.Json(new { message = "Goong phản hồi quá lâu. Bạn có thể chọn khu vực trong biểu mẫu." }, statusCode: 503); }
        });
        group.MapGet("/data/{resource}", (string resource, string? q, string? status, long? regionId, long? typeId, int? page, int? pageSize, AdminRepository repo, CancellationToken ct) =>
            repo.List(resource, q, status, regionId, typeId, page ?? 1, pageSize ?? 12, ct));
        group.MapGet("/data/{resource}/{id:long}", async (string resource, long id, AdminRepository repo, CancellationToken ct) =>
            await repo.Detail(resource, id, ct) is { } data ? Results.Ok(data) : Results.NotFound(new { message = "Không tìm thấy bản ghi." }));
        group.MapPost("/data/{resource}", (string resource, System.Text.Json.JsonElement body, HttpContext http, AdminRepository repo, CancellationToken ct) =>
            repo.Save(resource, null, body, (long)http.Items["AdminId"]!, ct));
        group.MapPut("/data/{resource}/{id:long}", (string resource, long id, System.Text.Json.JsonElement body, HttpContext http, AdminRepository repo, CancellationToken ct) =>
            repo.Save(resource, id, body, (long)http.Items["AdminId"]!, ct));
        group.MapDelete("/data/{resource}/{id:long}", async (string resource, long id, AdminRepository repo, CancellationToken ct) =>
        {
            await repo.Delete(resource, id, ct);
            return Results.NoContent();
        });
        // ?purpose=place|card picks the storage folder (places/, cards/), never the users' avatars/.
        group.MapPost("/images", async (HttpContext http, string? purpose, IImageStorage storage, CancellationToken ct) =>
        {
            var folder = ImageFolder.ForAdminPurpose(purpose)
                ?? throw new AdminValidation("Thiếu mục đích ảnh: place (địa danh) hoặc card (thẻ).");
            const int maxBytes = 5 * 1024 * 1024;
            if (http.Request.ContentType != "image/jpeg") throw new AdminValidation("Chỉ chấp nhận ảnh JPEG.");
            if (http.Request.ContentLength is > maxBytes) throw new AdminValidation("Ảnh không được vượt quá 5 MB.");
            using var stream = new MemoryStream();
            var buffer = new byte[8192]; int read;
            while ((read = await http.Request.Body.ReadAsync(buffer, ct)) > 0)
            {
                if (stream.Length + read > maxBytes) throw new AdminValidation("Ảnh không được vượt quá 5 MB.");
                await stream.WriteAsync(buffer.AsMemory(0, read), ct);
            }
            var bytes = stream.ToArray();
            if (bytes.Length < 4 || bytes[0] != 0xff || bytes[1] != 0xd8 || bytes[2] != 0xff)
                throw new AdminValidation("Tệp ảnh JPEG không hợp lệ.");
            var url = await storage.SaveAsync(folder, bytes, ct);
            return Results.Ok(new { url });
        });
    }
}
