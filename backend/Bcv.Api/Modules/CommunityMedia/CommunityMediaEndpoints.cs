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
        await using var conn=await db.OpenConnectionAsync();
        await using var tx=await conn.BeginTransactionAsync();
        await using(var gate=new NpgsqlCommand("SELECT pg_advisory_xact_lock(7246319)",conn,tx)) await gate.ExecuteNonQueryAsync();
        await using(var command=new NpgsqlCommand((await reader.ReadToEndAsync()).Replace("public.", ""),conn,tx)) await command.ExecuteNonQueryAsync();
        using var moderationStream=typeof(CommunityMediaEndpoints).Assembly.GetManifestResourceStream("Bcv.MediaModerationMigration.sql")!;
        using var moderationReader=new StreamReader(moderationStream);
        await using(var migration=new NpgsqlCommand(await moderationReader.ReadToEndAsync(),conn,tx)) await migration.ExecuteNonQueryAsync();
        using var videoStream=typeof(CommunityMediaEndpoints).Assembly.GetManifestResourceStream("Bcv.PlaceVideoMigration.sql")!;
        using var videoReader=new StreamReader(videoStream);
        await using(var migration=new NpgsqlCommand(await videoReader.ReadToEndAsync(),conn,tx))await migration.ExecuteNonQueryAsync();
        using var limitsStream=typeof(CommunityMediaEndpoints).Assembly.GetManifestResourceStream("Bcv.VideoLimitsMigration.sql")!;
        using var limitsReader=new StreamReader(limitsStream);
        await using(var migration=new NpgsqlCommand(await limitsReader.ReadToEndAsync(),conn,tx))await migration.ExecuteNonQueryAsync();
        await tx.CommitAsync();
    }

    public sealed record Review(string Status, string? Note, string? ExpectedStatus = null);
    public sealed record UploadAccess(bool Blocked, string? Note);
    public sealed record ReportBody(string? Reason);

    public static void MapCommunityMedia(this WebApplication app)
    {
        // Keep base64 images out of the map JSON; load each visible image separately.
        app.MapGet("/api/places/{placeId:long}/thumbnail", async (long placeId, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("""
                SELECT i.image_key FROM place_image i JOIN place p ON p.place_id=i.place_id
                JOIN region r ON r.region_id=p.region_id
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

        app.MapGet("/api/places/{placeId:long}/media", async (long placeId, bool? approvedOnly, HttpRequest request,
            ISessionStore sessions, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            request.HttpContext.Response.Headers.CacheControl = "no-store";
            if (!await CanUsePlace(db, placeId, user.UserId, ct)) return PlaceUnavailable();
            return Results.Ok(new { items = await List(db, placeId, user.UserId, approvedOnly == true, ct) });
        });

        app.MapPost("/api/places/{placeId:long}/media", async (long placeId, string? caption,
            HttpRequest request, ISessionStore sessions, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            if ((caption?.Length ?? 0) > 500) return Results.BadRequest(new { message = "Mô tả tối đa 500 ký tự." });
            var mime = request.ContentType?.Split(';')[0].Trim().ToLowerInvariant();
            var bodyLimit=request.HttpContext.Features.Get<Microsoft.AspNetCore.Http.Features.IHttpMaxRequestBodySizeFeature>();
            if(bodyLimit is {IsReadOnly:false})bodyLimit.MaxRequestBodySize=PlaceVideos.MaxBytes;
            var image = mime is "image/jpeg" or "image/png" or "image/webp";
            if (!image && mime is not ("video/mp4" or "video/webm"))
                return Results.BadRequest(new { message = "Chọn ảnh JPEG/PNG/WebP hoặc video MP4/WebM." });
            var limit = image ? 10 * 1024 * 1024 : PlaceVideos.MaxBytes;
            if (request.ContentLength > limit) return Results.BadRequest(new { message = "Ảnh tối đa 10 MB; video tối đa 100 MB." });
            await using var memory = new MemoryStream(request.ContentLength is >0?(int)request.ContentLength.Value:0);
            var buffer = new byte[81920];
            int count;
            while ((count = await request.Body.ReadAsync(buffer, ct)) > 0)
            {
                if (memory.Length + count > limit) return Results.BadRequest(new { message = "Tệp vượt quá kích thước cho phép." });
                await memory.WriteAsync(buffer.AsMemory(0, count), ct);
            }
            var bytes = memory.Length==memory.Capacity?memory.GetBuffer():memory.ToArray();
            if (!ValidHeader(mime!, bytes)) return Results.BadRequest(new { message = "Nội dung tệp không đúng định dạng." });
            if (!image)
            {
                try { _ = mime == "video/webm" ? WebmMetadata.Inspect(bytes) : PlaceVideos.Inspect(bytes); }
                catch (AdminValidation error) { return Results.BadRequest(new { message = error.Message }); }
            }
            await using var uploadConnection=await db.OpenConnectionAsync(ct);
            await using var uploadTransaction=await uploadConnection.BeginTransactionAsync(ct);
            await using(var permission=new NpgsqlCommand("SELECT media_upload_blocked FROM app_user WHERE user_id=$1 AND status='ACTIVE' FOR SHARE",uploadConnection,uploadTransaction))
            {
                permission.Parameters.AddWithValue(user.UserId);
                var blocked=await permission.ExecuteScalarAsync(ct);
                if(blocked is not false)return Results.Json(new {message="Quyền gửi nội dung đã bị khóa hoặc tài khoản không còn hoạt động."},statusCode:403);
            }
            // Keep the discovery and visibility decision valid until the upload commits.
            await using (var place = new NpgsqlCommand("""
                SELECT p.place_id FROM place p JOIN region r USING(region_id)
                JOIN user_discovered_place d ON d.place_id=p.place_id AND d.user_id=$2
                WHERE p.place_id=$1 AND p.status='ACTIVE' AND r.status='ACTIVE'
                FOR SHARE OF p,r,d
                """, uploadConnection, uploadTransaction))
            {
                place.Parameters.AddWithValue(placeId); place.Parameters.AddWithValue(user.UserId);
                if (await place.ExecuteScalarAsync(ct) is null) return PlaceUnavailable();
            }
            await using var command = new NpgsqlCommand("""
                INSERT INTO community_media(user_id,place_id,caption,mime_type,media_data)
                SELECT $1,p.place_id,$3,$4,$5 FROM place p WHERE p.place_id=$2 AND p.status='ACTIVE'
                RETURNING media_id
                """,uploadConnection,uploadTransaction);
            command.Parameters.AddWithValue(user.UserId);
            command.Parameters.AddWithValue(placeId);
            command.Parameters.AddWithValue(caption?.Trim() ?? "");
            command.Parameters.AddWithValue(mime!);
            command.Parameters.AddWithValue(bytes);
            var id = await command.ExecuteScalarAsync(ct);
            await uploadTransaction.CommitAsync(ct);
            return id is null ? Results.NotFound(new { message = "Không tìm thấy địa danh đang hoạt động." })
                : Results.Ok(new { mediaId = (long)id, status = "PENDING", message = "Đã gửi. Nội dung đang chờ quản trị viên duyệt." });
        }).WithMetadata(new Microsoft.AspNetCore.Mvc.RequestSizeLimitAttribute(PlaceVideos.MaxBytes));

        app.MapGet("/api/community-media/{id:long}/content", async (long id, HttpRequest request,
            ISessionStore sessions, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            // Administrators must still be able to moderate files after a place is hidden.
            // All other viewers, including the owner, need an active, discovered place.
            await using var command = db.CreateCommand("""
                SELECT m.mime_type,m.media_data FROM community_media m
                JOIN place p ON p.place_id=m.place_id JOIN region r USING(region_id)
                JOIN app_user viewer ON viewer.user_id=$2 AND viewer.status='ACTIVE'
                WHERE m.media_id=$1 AND m.media_data IS NOT NULL AND
                  (viewer.role='ADMIN' OR (p.status='ACTIVE' AND r.status='ACTIVE'
                    AND EXISTS(SELECT 1 FROM user_discovered_place d WHERE d.place_id=p.place_id AND d.user_id=$2)
                    AND (m.status='APPROVED' OR m.user_id=$2)))
                """);
            command.Parameters.AddWithValue(id);
            command.Parameters.AddWithValue(user.UserId);
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct)) return Results.NotFound();
            request.HttpContext.Response.Headers.CacheControl = "private, no-store";
            request.HttpContext.Response.Headers["X-Content-Type-Options"] = "nosniff";
            return Results.File(reader.GetFieldValue<byte[]>(1), reader.GetString(0), enableRangeProcessing: true);
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
            try { return await next(context); }
            catch(AdminValidation ex) {return Results.BadRequest(new {message=ex.Message});}
            catch(KeyNotFoundException ex) {return Results.NotFound(new {message=ex.Message});}
        });
        admin.MapGet("", (string? status,string? q,string? kind,bool? reported,int? page,int? pageSize,long? placeId,NpgsqlDataSource db,CancellationToken ct) =>
            MediaModeration.ListAsync(db,status,q,kind,reported??false,page??1,pageSize??12,ct,placeId));
        admin.MapGet("/{id:long}/reports",(long id,NpgsqlDataSource db,CancellationToken ct)=>MediaModeration.ReportsAsync(db,id,ct));
        admin.MapPost("/{id:long}/review", async (long id, Review body,HttpContext http,NpgsqlDataSource db,CancellationToken ct) =>
        {
            await MediaModeration.ReviewAsync(db,id,body.Status,body.Note,body.ExpectedStatus,(long)http.Items["MediaAdmin"]!,ct);
            return Results.Ok(new {success=true});
        });
        admin.MapPost("/users/{userId:long}/upload-access",async(long userId,UploadAccess body,HttpContext http,NpgsqlDataSource db,CancellationToken ct)=>
        {
            await MediaModeration.SetUploadAccessAsync(db,userId,body.Blocked,body.Note,(long)http.Items["MediaAdmin"]!,ct);
            return Results.Ok(new {success=true});
        });
        app.MapPost("/api/community-media/{id:long}/reports",async(long id,ReportBody body,HttpRequest request,ISessionStore sessions,NpgsqlDataSource db,IConfiguration config,CancellationToken ct)=>
        {
            var user=CurrentUser.Get(request,sessions);if(user is null)return Results.Unauthorized();
            try {
                await MediaModeration.ReportAsync(db,id,user.UserId,body.Reason,config.GetValue<int?>("CommunityMedia:ReportThreshold")??3,ct);
                return Results.Ok(new {success=true});
            } catch(AdminValidation ex){return Results.BadRequest(new {message=ex.Message});}
              catch(KeyNotFoundException ex){return Results.NotFound(new {message=ex.Message});}
        });
    }

    private static IResult PlaceUnavailable() => Results.NotFound(new {
        code = "PLACE_NOT_AVAILABLE", message = "Chỉ xem và gửi nội dung tại địa danh đã khám phá, còn hoạt động." });

    private static async Task<bool> CanUsePlace(NpgsqlDataSource db, long placeId, long userId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT EXISTS(SELECT 1 FROM place p JOIN region r USING(region_id)
              JOIN user_discovered_place d ON d.place_id=p.place_id AND d.user_id=$2
              JOIN app_user u ON u.user_id=d.user_id AND u.status='ACTIVE'
              WHERE p.place_id=$1 AND p.status='ACTIVE' AND r.status='ACTIVE')
            """);
        command.Parameters.AddWithValue(placeId); command.Parameters.AddWithValue(userId);
        return (bool)(await command.ExecuteScalarAsync(ct))!;
    }

    private static async Task<List<object>> List(NpgsqlDataSource db, long placeId, long userId, bool approvedOnly, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT m.media_id,m.place_id,p.name,u.full_name,m.caption,m.mime_type,m.status,m.review_note,m.created_at,
                m.user_id=$2 AS is_owner,m.media_data IS NOT NULL AS content_available,
                m.status='APPROVED' AND m.user_id<>$2 AND m.media_data IS NOT NULL
                  AND NOT EXISTS(SELECT 1 FROM media_report report WHERE report.media_id=m.media_id AND report.reporter_id=$2) AS can_report
            FROM community_media m JOIN place p ON p.place_id=m.place_id JOIN app_user u ON u.user_id=m.user_id
            JOIN region r ON r.region_id=p.region_id
            WHERE m.place_id=$1 AND (m.status='APPROVED' OR m.user_id=$2)
                AND (NOT $3 OR m.status='APPROVED')
                AND p.status='ACTIVE' AND r.status='ACTIVE'
                AND EXISTS(SELECT 1 FROM user_discovered_place d JOIN app_user viewer ON viewer.user_id=d.user_id
                    WHERE d.place_id=p.place_id AND d.user_id=$2 AND viewer.status='ACTIVE')
            ORDER BY COALESCE(m.reviewed_at,m.created_at) DESC,m.media_id DESC LIMIT 100
            """);
        command.Parameters.AddWithValue(placeId);
        command.Parameters.AddWithValue(userId);
        command.Parameters.AddWithValue(approvedOnly);
        var items = new List<object>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct)) items.Add(new {
            mediaId = reader.GetInt64(0), placeId = reader.GetInt64(1), placeName = reader.GetString(2),
            author = reader.GetString(3), caption = reader.GetString(4), mimeType = reader.GetString(5),
            status = reader.GetString(6), reviewNote = reader.IsDBNull(7) ? null : reader.GetString(7),
            createdAt = reader.GetDateTime(8), contentUrl = $"/api/community-media/{reader.GetInt64(0)}/content",
            isOwner = reader.GetBoolean(9), contentAvailable = reader.GetBoolean(10), canReport = reader.GetBoolean(11)
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
