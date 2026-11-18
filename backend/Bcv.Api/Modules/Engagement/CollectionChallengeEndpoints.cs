using Npgsql;
namespace Bcv.Api.Modules.Engagement;

public static partial class EngagementEndpoints
{
    public sealed record DailySteps(string Date, int Steps);
    public sealed record PauseRequest(int Days);
    public sealed record NoteRequest(string? Note);
    private const int MaxJournalPhotosPerPlace = 12;
    private const string PlaceImageSql = """
        (SELECT CASE WHEN i.image_key LIKE 'data:%' THEN '/api/places/' || p.place_id || '/thumbnail' ELSE i.image_key END
         FROM place_image i WHERE i.place_id=p.place_id ORDER BY i.place_image_id LIMIT 1)
        """;
    private static long CollectionUser(HttpContext http) => (long)http.Items["UserId"]!;

    private static void MapCollectionChallengeEndpoints(RouteGroupBuilder group)
    {
        group.MapGet("/engagement/summary", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var userId = CollectionUser(http);
            return Results.Ok(new { streak=await StreakJsonAsync(db,userId,ct),
                passport=await CollectionPassportAsync(db,userId,ct),badges=await CollectionBadgesAsync(db,userId,ct) });
        });
        group.MapPost("/activity/daily", async (DailySteps body,HttpContext http,NpgsqlDataSource db,CancellationToken ct) =>
        {
            var today=EngagementCore.Today();
            if(!DateOnly.TryParseExact(body.Date,"yyyy-MM-dd",System.Globalization.CultureInfo.InvariantCulture,System.Globalization.DateTimeStyles.None,out var date)
                || date>today || date<today.AddDays(-1) || body.Steps<0) return Results.BadRequest(new {message="Ngày hoặc số bước không hợp lệ."});
            var userId=CollectionUser(http);var (_,goal)=await EngagementCore.DailyGoalAsync(db,userId,ct);
            await using var command=db.CreateCommand("""
                INSERT INTO daily_activity(user_id,activity_date,steps,health_steps,goal_steps) VALUES($1,$2,$3,$3,$4)
                ON CONFLICT(user_id,activity_date) DO UPDATE SET
                  health_steps=GREATEST(daily_activity.health_steps,EXCLUDED.health_steps),
                  steps=LEAST(GREATEST(daily_activity.health_steps,EXCLUDED.health_steps,daily_activity.journey_steps),40000),updated_at=CURRENT_TIMESTAMP
                """);
            command.Parameters.AddWithValue(userId);command.Parameters.AddWithValue(date);command.Parameters.AddWithValue(Math.Min(body.Steps,40000));command.Parameters.AddWithValue(goal);
            await command.ExecuteNonQueryAsync(ct);return Results.Ok(await StreakJsonAsync(db,userId,ct));
        });
        group.MapPost("/activity/pause",async(PauseRequest body,HttpContext http,NpgsqlDataSource db,CancellationToken ct)=>
        {
            if(body.Days is <1 or >7)return Results.BadRequest(new{message="Tạm nghỉ từ 1 đến 7 ngày."});
            var userId=CollectionUser(http);var today=EngagementCore.Today();
            await using var connection=await db.OpenConnectionAsync(ct);await using var transaction=await connection.BeginTransactionAsync(ct);
            await LockCollectionUser(connection,transaction,userId,ct);
            await using(var check=new NpgsqlCommand("SELECT count(*) FILTER(WHERE end_date >= $2),count(*) FILTER(WHERE start_date > $3) FROM activity_pause WHERE user_id=$1",connection,transaction))
            {
                check.Parameters.AddWithValue(userId);check.Parameters.AddWithValue(today);check.Parameters.AddWithValue(today.AddDays(-30));
                await using var reader=await check.ExecuteReaderAsync(ct);await reader.ReadAsync(ct);
                if(reader.GetInt64(0)>0 || reader.GetInt64(1)>=2)return Results.Conflict(new{message="Bạn đang tạm nghỉ hoặc đã dùng đủ 2 lần trong 30 ngày."});
            }
            await using(var command=new NpgsqlCommand("INSERT INTO activity_pause(user_id,start_date,end_date) VALUES($1,$2,$3)",connection,transaction))
            {command.Parameters.AddWithValue(userId);command.Parameters.AddWithValue(today);command.Parameters.AddWithValue(today.AddDays(body.Days-1));await command.ExecuteNonQueryAsync(ct);}
            await transaction.CommitAsync(ct);return Results.Ok(await StreakJsonAsync(db,userId,ct));
        });
        group.MapDelete("/activity/pause",async(HttpContext http,NpgsqlDataSource db,CancellationToken ct)=>
        {
            var userId=CollectionUser(http);var today=EngagementCore.Today();
            await using var connection=await db.OpenConnectionAsync(ct);await using var transaction=await connection.BeginTransactionAsync(ct);
            await LockCollectionUser(connection,transaction,userId,ct);
            await using(var remove=new NpgsqlCommand("DELETE FROM activity_pause WHERE user_id=$1 AND start_date=$2",connection,transaction))
            {remove.Parameters.AddWithValue(userId);remove.Parameters.AddWithValue(today);await remove.ExecuteNonQueryAsync(ct);}
            await using(var end=new NpgsqlCommand("UPDATE activity_pause SET end_date=$2 WHERE user_id=$1 AND start_date<$3 AND end_date>=$3",connection,transaction))
            {end.Parameters.AddWithValue(userId);end.Parameters.AddWithValue(today.AddDays(-1));end.Parameters.AddWithValue(today);await end.ExecuteNonQueryAsync(ct);}
            await transaction.CommitAsync(ct);return Results.Ok(await StreakJsonAsync(db,userId,ct));
        });
        group.MapGet("/journal", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand($"""
                SELECT p.place_id, p.name, r.name, 'V2'::text, d.visited_at,
                  (SELECT count(*)::int FROM journal_photo jp WHERE jp.user_id=$1 AND jp.place_id=p.place_id),
                  COALESCE((SELECT note FROM journal_note n WHERE n.user_id=$1 AND n.place_id=p.place_id), ''),
                  EXISTS(SELECT 1 FROM user_discovered_place c WHERE c.user_id=$1 AND c.place_id=p.place_id),
                  (SELECT jp.photo_id FROM journal_photo jp WHERE jp.user_id=$1 AND jp.place_id=p.place_id ORDER BY jp.photo_id DESC LIMIT 1),
                  {PlaceImageSql}
                FROM (SELECT user_id, place_id, discovered_at AS visited_at FROM user_discovered_place) d JOIN place p ON p.place_id=d.place_id JOIN region r ON r.region_id=p.region_id
                WHERE d.user_id=$1 ORDER BY d.visited_at DESC
                """);
            command.Parameters.AddWithValue(CollectionUser(http));
            var items = new List<object>();
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                items.Add(new {
                    placeId = reader.GetInt64(0), name = reader.GetString(1), regionName = reader.GetString(2),
                    zone = reader.IsDBNull(3) ? null : EngagementCore.ZoneNames.GetValueOrDefault(reader.GetString(3)),
                    discoveredAt = reader.GetDateTime(4), photoCount = reader.GetInt32(5), note = reader.GetString(6),
                    visited = reader.GetBoolean(7),
                    coverUrl = reader.IsDBNull(8) ? null : $"/api/journal/photos/{reader.GetInt64(8)}/content",
                    placeImageUrl = reader.IsDBNull(9) ? null : reader.GetString(9) });
            return Results.Ok(new { items });
        });
        group.MapGet("/journal/places/{placeId:long}", async (long placeId, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var userId = CollectionUser(http);
            object? entry = null;
            await using (var command = db.CreateCommand($"""
                SELECT p.name, r.name, d.visited_at,
                  COALESCE((SELECT note FROM journal_note n WHERE n.user_id=$1 AND n.place_id=p.place_id), ''),
                  (SELECT count(*)::int FROM user_discovered_place c WHERE c.user_id=$1 AND c.place_id=p.place_id),
                  {PlaceImageSql}
                FROM (SELECT user_id, place_id, discovered_at AS visited_at FROM user_discovered_place) d JOIN place p ON p.place_id=d.place_id JOIN region r ON r.region_id=p.region_id
                WHERE d.user_id=$1 AND d.place_id=$2
                """))
            {
                command.Parameters.AddWithValue(userId); command.Parameters.AddWithValue(placeId);
                await using var reader = await command.ExecuteReaderAsync(ct);
                if (await reader.ReadAsync(ct))
                {
                    // discoveredAt keeps its name for older clients; it is now the first real visit.
                    entry = new {
                        placeId, name = reader.GetString(0), regionName = reader.GetString(1), discoveredAt = reader.GetDateTime(2),
                        note = reader.GetString(3), visited = true, checkins = reader.GetInt32(4),
                        placeImageUrl = reader.IsDBNull(5) ? null : reader.GetString(5) };
                }
            }
            if (entry is null) return Results.NotFound(new { message = "Nhật ký chỉ mở ở địa danh bạn đã khám phá." });
            var photos = new List<object>();
            await using (var command = db.CreateCommand("""
                SELECT photo_id, caption, created_at FROM journal_photo WHERE user_id=$1 AND place_id=$2 ORDER BY photo_id DESC
                """))
            {
                command.Parameters.AddWithValue(userId); command.Parameters.AddWithValue(placeId);
                await using var reader = await command.ExecuteReaderAsync(ct);
                while (await reader.ReadAsync(ct))
                    photos.Add(new { photoId = reader.GetInt64(0), caption = reader.GetString(1), createdAt = reader.GetDateTime(2),
                        contentUrl = $"/api/journal/photos/{reader.GetInt64(0)}/content" });
            }
            return Results.Ok(new { entry, photos });
        });
        group.MapPut("/journal/places/{placeId:long}/note", async (long placeId, NoteRequest body, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var note = (body.Note ?? "").Trim();
            if (note.Length > 1000) return Results.BadRequest(new { message = "Ghi chú tối đa 1.000 ký tự." });
            await using var command = db.CreateCommand("""
                INSERT INTO journal_note(user_id, place_id, note)
                SELECT DISTINCT $1::bigint, place_id, $3::varchar FROM user_discovered_place WHERE user_id=$1 AND place_id=$2
                ON CONFLICT (user_id, place_id) DO UPDATE SET note=EXCLUDED.note, updated_at=CURRENT_TIMESTAMP
                """);
            command.Parameters.AddWithValue(CollectionUser(http)); command.Parameters.AddWithValue(placeId); command.Parameters.AddWithValue(note);
            return await command.ExecuteNonQueryAsync(ct) > 0 ? Results.Ok(new { success = true })
                : Results.NotFound(new { message = "Nhật ký chỉ mở ở địa danh bạn đã khám phá." });
        });
        group.MapPost("/journal/places/{placeId:long}/photos", async (long placeId, string? caption, HttpRequest request,
            HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            if ((caption?.Length ?? 0) > 300) return Results.BadRequest(new { message = "Chú thích tối đa 300 ký tự." });
            var mime = request.ContentType?.Split(';')[0].Trim().ToLowerInvariant();
            if (mime is not ("image/jpeg" or "image/png" or "image/webp"))
                return Results.BadRequest(new { message = "Nhật ký chỉ nhận ảnh JPEG, PNG hoặc WebP." });
            const int limit = 10 * 1024 * 1024;
            if (request.ContentLength > limit) return Results.BadRequest(new { message = "Ảnh tối đa 10 MB." });
            await using var memory = new MemoryStream();
            var buffer = new byte[81920];
            int count;
            while ((count = await request.Body.ReadAsync(buffer, ct)) > 0)
            {
                if (memory.Length + count > limit) return Results.BadRequest(new { message = "Ảnh tối đa 10 MB." });
                await memory.WriteAsync(buffer.AsMemory(0, count), ct);
            }
            var bytes = memory.ToArray();
            if (!Bcv.Api.Modules.CommunityMedia.CommunityMediaEndpoints.ValidHeader(mime, bytes)) return Results.BadRequest(new { message = "Nội dung tệp không phải ảnh hợp lệ." });
            var userId = CollectionUser(http);
            await using var connection = await db.OpenConnectionAsync(ct);
            await using var transaction = await connection.BeginTransactionAsync(ct);
            await LockCollectionUser(connection, transaction, userId, ct);
            await using var command = new NpgsqlCommand("""
                INSERT INTO journal_photo(user_id, place_id, caption, mime_type, photo_data)
                SELECT $1, d.place_id, $3, $4, $5 FROM (SELECT DISTINCT user_id, place_id FROM user_discovered_place) d
                WHERE d.user_id=$1 AND d.place_id=$2
                  AND (SELECT count(*) FROM journal_photo WHERE user_id=$1 AND place_id=$2) < $6
                RETURNING photo_id
                """, connection, transaction);
            command.Parameters.AddWithValue(userId); command.Parameters.AddWithValue(placeId);
            command.Parameters.AddWithValue(caption?.Trim() ?? ""); command.Parameters.AddWithValue(mime);
            command.Parameters.AddWithValue(bytes); command.Parameters.AddWithValue(MaxJournalPhotosPerPlace);
            var id = await command.ExecuteScalarAsync(ct);
            await transaction.CommitAsync(ct);
            return id is long photoId ? Results.Ok(new { photoId, message = "Đã lưu ảnh vào nhật ký (chỉ bạn xem được)." })
                : Results.BadRequest(new { message = $"Chỉ lưu ảnh ở địa danh đã khám phá, tối đa {MaxJournalPhotosPerPlace} ảnh mỗi nơi." });
        }).WithMetadata(new Microsoft.AspNetCore.Mvc.RequestSizeLimitAttribute(10 * 1024 * 1024 + 1024));
        group.MapGet("/journal/photos/{photoId:long}/content", async (long photoId, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("SELECT mime_type, photo_data FROM journal_photo WHERE photo_id=$1 AND user_id=$2");
            command.Parameters.AddWithValue(photoId); command.Parameters.AddWithValue(CollectionUser(http));
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct)) return Results.NotFound();
            http.Response.Headers.CacheControl = "private, no-store";
            http.Response.Headers["X-Content-Type-Options"] = "nosniff";
            return Results.File(reader.GetFieldValue<byte[]>(1), reader.GetString(0));
        });
        group.MapDelete("/journal/photos/{photoId:long}", async (long photoId, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("DELETE FROM journal_photo WHERE photo_id=$1 AND user_id=$2");
            command.Parameters.AddWithValue(photoId); command.Parameters.AddWithValue(CollectionUser(http));
            return await command.ExecuteNonQueryAsync(ct) > 0 ? Results.Ok(new { success = true }) : Results.NotFound();
        });
    }
    private static async Task LockCollectionUser(NpgsqlConnection connection,NpgsqlTransaction transaction,long userId,CancellationToken ct)
    {
        await using var command=new NpgsqlCommand("SELECT user_id FROM app_user WHERE user_id=$1 FOR UPDATE",connection,transaction);
        command.Parameters.AddWithValue(userId);await command.ExecuteScalarAsync(ct);
    }
    private static async Task<object> StreakJsonAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        var today = EngagementCore.Today();
        var (days, pauses) = await EngagementCore.HistoryAsync(db, userId, ct);
        var (level, goal) = await EngagementCore.DailyGoalAsync(db, userId, ct);
        var streak = EngagementCore.ComputeStreak(days, pauses, today);
        var todaySteps = days.FirstOrDefault(d => d.Date == today)?.Steps ?? 0;
        var week = Enumerable.Range(0, 7).Select(i => today.AddDays(i - 6)).Select(d =>
        {
            var row = days.FirstOrDefault(x => x.Date == d);
            return new { date = d.ToString("yyyy-MM-dd"), steps = row?.Steps ?? 0, goal = row?.Goal ?? goal,
                met = row is not null && row.Steps >= row.Goal, paused = pauses.Any(p => d >= p.Start && d <= p.End) };
        }).ToList();
        return new {
            current = streak.Current, best = streak.Best, todayDone = streak.TodayDone,
            todaySteps, dailyGoal = goal, activityLevel = level,
            pause = streak.ActivePause is { } p ? new { start = p.Start.ToString("yyyy-MM-dd"), end = p.End.ToString("yyyy-MM-dd") } : null,
            week
        };
    }


}
