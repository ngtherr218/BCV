using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Admin;
using Npgsql;

namespace Bcv.Api.Modules.CommunityMedia;

public static class CommunityMediaEndpoints
{
    public static async Task EnsureAsync(NpgsqlDataSource db)
    {
        using var stream = typeof(CommunityMediaEndpoints).Assembly.GetManifestResourceStream("Bcv.CommunityMediaMigration.sql")!;
        using var reader = new StreamReader(stream);
        await using var command = db.CreateCommand(await reader.ReadToEndAsync());
        await command.ExecuteNonQueryAsync();
    }

    public sealed record Review(string Status, string? Note);

    public static void MapCommunityMedia(this WebApplication app)
    {
        // Keep base64 images out of the map JSON; load each visible image separately.
        app.MapGet("/api/places/{placeId:long}/thumbnail", async (long placeId, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("""
                SELECT i.image_key FROM public.place_image i JOIN public.place p ON p.place_id=i.place_id
                JOIN public.region r ON r.region_id=p.region_id
                WHERE p.place_id=$1 AND p.status='ACTIVE' AND r.status='ACTIVE'
                ORDER BY i.place_image_id LIMIT 1
                """);
            command.Parameters.AddWithValue(placeId);
            var value = await command.ExecuteScalarAsync(ct) as string;
            if (value is null) return Results.NotFound();
            if (value.StartsWith("https://", StringComparison.OrdinalIgnoreCase)) return Results.Redirect(value);
            var split = value.IndexOf(",", StringComparison.Ordinal);
            if (split < 0 || !value.StartsWith("data:", StringComparison.Ordinal) || value.Length > 8 * 1024 * 1024)
                return Results.NotFound();
            var mime = value[5..split].Replace(";base64", "");
            if (mime is not ("image/jpeg" or "image/png" or "image/webp")) return Results.NotFound();
            try
            {
                var bytes = Convert.FromBase64String(value[(split + 1)..]);
                return ValidHeader(mime, bytes) ? Results.File(bytes, mime) : Results.NotFound();
            }
            catch (FormatException) { return Results.NotFound(); }
        });

        app.MapGet("/api/places/{placeId:long}/media", async (long placeId, HttpRequest request,
            ISessionStore sessions, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            request.HttpContext.Response.Headers.CacheControl = "no-store";
            return Results.Ok(new { items = await List(db, placeId, user.UserId, false, "", ct) });
        });

        app.MapPost("/api/places/{placeId:long}/media", async (long placeId, string? caption,
            HttpRequest request, ISessionStore sessions, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            if ((caption?.Length ?? 0) > 500) return Results.BadRequest(new { message = "Mô tả tối đa 500 ký tự." });
            var mime = request.ContentType?.Split(';')[0].Trim().ToLowerInvariant();
            var image = mime is "image/jpeg" or "image/png" or "image/webp";
            if (!image && mime is not ("video/mp4" or "video/webm"))
                return Results.BadRequest(new { message = "Chọn ảnh JPEG/PNG/WebP hoặc video MP4/WebM." });
            var limit = (image ? 10 : 30) * 1024 * 1024;
            if (request.ContentLength > limit) return Results.BadRequest(new { message = "Ảnh tối đa 10 MB; video tối đa 30 MB." });
            await using var memory = new MemoryStream();
            var buffer = new byte[81920];
            int count;
            while ((count = await request.Body.ReadAsync(buffer, ct)) > 0)
            {
                if (memory.Length + count > limit) return Results.BadRequest(new { message = "Tệp vượt quá kích thước cho phép." });
                await memory.WriteAsync(buffer.AsMemory(0, count), ct);
            }
            var bytes = memory.ToArray();
            if (!ValidHeader(mime!, bytes)) return Results.BadRequest(new { message = "Nội dung tệp không đúng định dạng." });
            await using var command = db.CreateCommand("""
                INSERT INTO public.community_media(user_id,place_id,caption,mime_type,media_data)
                SELECT $1,p.place_id,$3,$4,$5 FROM public.place p WHERE p.place_id=$2 AND p.status='ACTIVE'
                RETURNING media_id
                """);
            command.Parameters.AddWithValue(user.UserId);
            command.Parameters.AddWithValue(placeId);
            command.Parameters.AddWithValue(caption?.Trim() ?? "");
            command.Parameters.AddWithValue(mime!);
            command.Parameters.AddWithValue(bytes);
            var id = await command.ExecuteScalarAsync(ct);
            return id is null ? Results.NotFound(new { message = "Không tìm thấy địa danh đang hoạt động." })
                : Results.Ok(new { mediaId = (long)id, status = "PENDING", message = "Đã gửi. Nội dung đang chờ quản trị viên duyệt." });
        }).WithMetadata(new Microsoft.AspNetCore.Mvc.RequestSizeLimitAttribute(32 * 1024 * 1024));

        app.MapGet("/api/community-media/{id:long}/content", async (long id, HttpRequest request,
            ISessionStore sessions, NpgsqlDataSource db, AdminRepository admins, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            await using var command = db.CreateCommand("SELECT user_id,mime_type,media_data,status FROM public.community_media WHERE media_id=$1");
            command.Parameters.AddWithValue(id);
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct)) return Results.NotFound();
            if (reader.GetString(3) != "APPROVED" && (user is null ||
                user.UserId != reader.GetInt64(0) && !await admins.IsAdmin(user.UserId, ct))) return Results.NotFound();
            request.HttpContext.Response.Headers.CacheControl = "private, no-store";
            request.HttpContext.Response.Headers["X-Content-Type-Options"] = "nosniff";
            return Results.File(reader.GetFieldValue<byte[]>(2), reader.GetString(1), enableRangeProcessing: true);
        });

        var admin = app.MapGroup("/api/admin/community-media");
        admin.AddEndpointFilter(async (context, next) =>
        {
            var user = CurrentUser.Get(context.HttpContext.Request, context.HttpContext.RequestServices.GetRequiredService<ISessionStore>());
            if (user is null) return Results.Unauthorized();
            var repository = context.HttpContext.RequestServices.GetRequiredService<AdminRepository>();
            if (!await repository.IsAdmin(user.UserId, context.HttpContext.RequestAborted)) return Results.StatusCode(403);
            context.HttpContext.Items["MediaAdmin"] = user.UserId;
            context.HttpContext.Response.Headers.CacheControl = "no-store";
            return await next(context);
        });
        admin.MapGet("", async (string? status, NpgsqlDataSource db, CancellationToken ct) =>
            Results.Ok(new { items = await List(db, null, 0, true, status ?? "PENDING", ct) }));
        admin.MapPost("/{id:long}/review", async (long id, Review body, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            if (body.Status is not ("APPROVED" or "REJECTED") || (body.Note?.Length ?? 0) > 500)
                return Results.BadRequest(new { message = "Trạng thái hoặc lý do duyệt không hợp lệ." });
            await using var command = db.CreateCommand("""
                UPDATE public.community_media SET status=$2,review_note=$3,reviewed_by=$4,reviewed_at=CURRENT_TIMESTAMP
                WHERE media_id=$1 AND status='PENDING' RETURNING media_id
                """);
            command.Parameters.AddWithValue(id);
            command.Parameters.AddWithValue(body.Status);
            command.Parameters.AddWithValue(body.Note?.Trim() ?? "");
            command.Parameters.AddWithValue((long)http.Items["MediaAdmin"]!);
            return await command.ExecuteScalarAsync(ct) is null
                ? Results.Conflict(new { message = "Nội dung không tồn tại hoặc đã được duyệt. Hãy tải lại." })
                : Results.Ok(new { success = true });
        });
    }

    private static async Task<List<object>> List(NpgsqlDataSource db, long? placeId, long userId, bool admin, string status, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT m.media_id,m.place_id,p.name,u.full_name,m.caption,m.mime_type,m.status,m.review_note,m.created_at
            FROM public.community_media m JOIN public.place p ON p.place_id=m.place_id JOIN public.app_user u ON u.user_id=m.user_id
            WHERE ($1::bigint IS NULL OR m.place_id=$1) AND ($3 OR m.status='APPROVED' OR m.user_id=$2)
                AND ($4='' OR m.status=$4)
            ORDER BY m.created_at DESC,m.media_id DESC LIMIT 100
            """);
        command.Parameters.AddWithValue(NpgsqlTypes.NpgsqlDbType.Bigint, (object?)placeId ?? DBNull.Value);
        command.Parameters.AddWithValue(userId);
        command.Parameters.AddWithValue(admin);
        command.Parameters.AddWithValue(status);
        var items = new List<object>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct)) items.Add(new {
            mediaId = reader.GetInt64(0), placeId = reader.GetInt64(1), placeName = reader.GetString(2),
            author = reader.GetString(3), caption = reader.GetString(4), mimeType = reader.GetString(5),
            status = reader.GetString(6), reviewNote = reader.IsDBNull(7) ? null : reader.GetString(7),
            createdAt = reader.GetDateTime(8), contentUrl = $"/api/community-media/{reader.GetInt64(0)}/content"
        });
        return items;
    }

    public static bool ValidHeader(string mime, byte[] bytes) => bytes.Length >= 12 && mime switch
    {
        "image/jpeg" => bytes[0] == 255 && bytes[1] == 216 && bytes[2] == 255,
        "image/png" => bytes.AsSpan(0, 8).SequenceEqual(new byte[] {137,80,78,71,13,10,26,10}),
        "image/webp" => System.Text.Encoding.ASCII.GetString(bytes,0,4)=="RIFF" && System.Text.Encoding.ASCII.GetString(bytes,8,4)=="WEBP",
        "video/mp4" => System.Text.Encoding.ASCII.GetString(bytes,4,4)=="ftyp",
        "video/webm" => bytes[0]==26 && bytes[1]==69 && bytes[2]==223 && bytes[3]==163,
        _ => false
    };
}
