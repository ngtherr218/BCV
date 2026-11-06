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
            await using var uploadConnection=await db.OpenConnectionAsync(ct);
            await using var uploadTransaction=await uploadConnection.BeginTransactionAsync(ct);
            await using(var permission=new NpgsqlCommand("SELECT media_upload_blocked FROM app_user WHERE user_id=$1 AND status='ACTIVE' FOR SHARE",uploadConnection,uploadTransaction))
            {
                permission.Parameters.AddWithValue(user.UserId);
                var blocked=await permission.ExecuteScalarAsync(ct);
                if(blocked is not false)return Results.Json(new {message="Quyền gửi nội dung đã bị khóa hoặc tài khoản không còn hoạt động."},statusCode:403);
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
        }).WithMetadata(new Microsoft.AspNetCore.Mvc.RequestSizeLimitAttribute(32 * 1024 * 1024));

        app.MapGet("/api/community-media/{id:long}/content", async (long id, HttpRequest request,
            ISessionStore sessions, NpgsqlDataSource db, AdminRepository admins, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            await using var command = db.CreateCommand("SELECT user_id,mime_type,media_data,status FROM community_media WHERE media_id=$1");
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
            try { return await next(context); }
            catch(AdminValidation ex) {return Results.BadRequest(new {message=ex.Message});}
            catch(KeyNotFoundException ex) {return Results.NotFound(new {message=ex.Message});}
        });
        admin.MapGet("", (string? status,string? q,string? kind,bool? reported,int? page,int? pageSize,NpgsqlDataSource db,CancellationToken ct) =>
            MediaModeration.ListAsync(db,status,q,kind,reported??false,page??1,pageSize??12,ct));
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

    private static async Task<List<object>> List(NpgsqlDataSource db, long? placeId, long userId, bool admin, string status, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT m.media_id,m.place_id,p.name,u.full_name,m.caption,m.mime_type,m.status,m.review_note,m.created_at
            FROM community_media m JOIN place p ON p.place_id=m.place_id JOIN app_user u ON u.user_id=m.user_id
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
