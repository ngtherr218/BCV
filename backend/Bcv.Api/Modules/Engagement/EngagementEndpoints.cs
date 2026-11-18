using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.PlaceMedia;
using Bcv.Api.Modules.PlaceCollection.Infrastructure;
using Bcv.Api.Modules.QuestItem.Infrastructure;
using Bcv.Api.Modules.Shared.Infrastructure;
using Bcv.Api.Modules.StatsNotify.Infrastructure;
using Npgsql;

namespace Bcv.Api.Modules.Engagement;

public static partial class EngagementEndpoints
{
    public sealed record DailySteps(string Date, int Steps);
    /// <summary>[Test]: a debug-build check-in that stands exactly at the place. Accepted only while the
    /// anti-cheat guard is off (local test servers), never in production.</summary>
    public sealed record CheckinRequest(double Latitude, double Longitude, bool Test = false);

    public sealed record QuizAnswer(string Key, string Answer);
    public sealed record NoteRequest(string? Note);

    private const int CheckinRadiusMeters = 750;
    private const int CheckinPoints = 200;

    /// <summary>The place's first official image: an https URL, or the thumbnail endpoint for inline images.</summary>
    private const string PlaceImageSql = """
        (SELECT CASE WHEN i.image_key LIKE 'data:%' THEN '/api/places/' || p.place_id || '/thumbnail' ELSE i.image_key END
         FROM place_media i WHERE i.media_type='IMAGE' AND i.place_id=p.place_id ORDER BY i.media_id LIMIT 1)
        """;
    private const int MaxJournalPhotosPerPlace = 12;

    public static void MapEngagementEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api");
        group.AddEndpointFilter(async (context, next) =>
        {
            var user = CurrentUser.Get(context.HttpContext.Request, context.HttpContext.RequestServices.GetRequiredService<ISessionStore>());
            if (user is null) return Results.Unauthorized();
            context.HttpContext.Items["UserId"] = user.UserId;
            context.HttpContext.Response.Headers.CacheControl = "no-store";
            return await next(context);
        });
        static long Me(HttpContext http) => (long)http.Items["UserId"]!;

        // ---------- F74: "Không quan tâm" ----------
        group.MapPost("/explore/places/{placeId:long}/dismiss", async (long placeId, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("""
                INSERT INTO place_dismissal(user_id, place_id) SELECT $1, place_id FROM place WHERE place_id=$2
                ON CONFLICT (user_id, place_id) DO UPDATE SET dismissed_at=CURRENT_TIMESTAMP
                """);
            command.Parameters.AddWithValue(Me(http)); command.Parameters.AddWithValue(placeId);
            return await command.ExecuteNonQueryAsync(ct) > 0 ? Results.Ok(new { success = true }) : Results.NotFound();
        });
        group.MapDelete("/explore/places/{placeId:long}/dismiss", async (long placeId, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("DELETE FROM place_dismissal WHERE user_id=$1 AND place_id=$2");
            command.Parameters.AddWithValue(Me(http)); command.Parameters.AddWithValue(placeId);
            await command.ExecuteNonQueryAsync(ct);
            return Results.Ok(new { success = true });
        });
        group.MapGet("/explore/dismissed", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("""
                SELECT p.place_id, p.name, r.name, d.dismissed_at FROM place_dismissal d
                JOIN place p ON p.place_id=d.place_id JOIN region r ON r.region_id=p.region_id
                WHERE d.user_id=$1 AND d.dismissed_at > CURRENT_TIMESTAMP - INTERVAL '30 days'
                ORDER BY d.dismissed_at DESC
                """);
            command.Parameters.AddWithValue(Me(http));
            var items = new List<object>();
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                items.Add(new { placeId = reader.GetInt64(0), name = reader.GetString(1), regionName = reader.GetString(2),
                    hiddenUntil = reader.GetDateTime(3).AddDays(30) });
            return Results.Ok(new { items });
        });

        // ---------- F83/F80: daily steps, streak, rest days ----------
        group.MapPost("/activity/daily", async (DailySteps body, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var today = EngagementCore.Today();
            if (!DateOnly.TryParse(body.Date, out var date) || date > today || date < today.AddDays(-1))
                return Results.BadRequest(new { message = "Chỉ ghi nhận số bước của hôm nay hoặc hôm qua." });
            if (body.Steps < 0) return Results.BadRequest(new { message = "Số bước không hợp lệ." });
            var userId = Me(http);
            var (_, goal) = await EngagementCore.DailyGoalAsync(db, userId, ct);
            var steps = Math.Min(body.Steps, EngagementCore.MaxDailySteps);
            await using var connection = await db.OpenConnectionAsync(ct);
            await using var transaction = await connection.BeginTransactionAsync(ct);
            if (body.Steps > EngagementCore.MaxDailySteps)
                await FlagAsync(connection, transaction, userId, null, $"Vượt {EngagementCore.MaxDailySteps:N0} bước/ngày ({date:dd/MM})",
                    body.Steps - EngagementCore.MaxDailySteps, ct);
            await using (var command = new NpgsqlCommand("""
                INSERT INTO daily_activity(user_id, activity_date, steps, health_steps, goal_steps) VALUES ($1,$2,$3,$3,$4)
                ON CONFLICT (user_id, activity_date) DO UPDATE
                  SET health_steps=GREATEST(daily_activity.health_steps, EXCLUDED.health_steps),
                      steps=LEAST(GREATEST(daily_activity.health_steps, EXCLUDED.health_steps, daily_activity.journey_steps), 40000),
                      updated_at=CURRENT_TIMESTAMP
                """, connection, transaction))
            {
                command.Parameters.AddWithValue(userId); command.Parameters.AddWithValue(date);
                command.Parameters.AddWithValue(steps); command.Parameters.AddWithValue(goal);
                await command.ExecuteNonQueryAsync(ct);
            }
            await transaction.CommitAsync(ct);
            return Results.Ok(await StreakJsonAsync(db, userId, ct));
        });

        // ---------- F81/F82/F83/F85: one summary for the "Thành tích" screen ----------
        group.MapGet("/engagement/summary", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var userId = Me(http);
            await LeaderboardRewards.SettleAsync(db, ct);
            var streak = await StreakJsonAsync(db, userId, ct);
            var passport = await PassportAsync(db, userId, ct);
            var badges = await BadgesAsync(db, userId, ct);
            var rewards = await LeaderboardRewards.UnseenAsync(db, userId, ct);
            await using var command = db.CreateCommand("""
                SELECT COALESCE((SELECT sum(points) FROM explorer_point WHERE user_id=$1),0)::int,
                       (SELECT count(*) FROM user_place_mastery WHERE user_id=$1)::int
                """);
            command.Parameters.AddWithValue(userId);
            await using var reader = await command.ExecuteReaderAsync(ct);
            await reader.ReadAsync(ct);
            return Results.Ok(new { streak, passport, badges, rewards, points = reader.GetInt32(0), masteredPlaces = reader.GetInt32(1) });
        });
        // Bảng vàng history: past weeks' top 3 of the Khám phá board or of the user's Vận động level.
        group.MapGet("/engagement/leaderboard/history", async (string? scope, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var userId = Me(http);
            await LeaderboardRewards.SettleAsync(db, ct);
            var board = scope is "activity" or "streak" ? scope : "knowledge";
            var level = board == "knowledge" ? "" : (await EngagementCore.DailyGoalAsync(db, userId, ct)).Item1;
            return Results.Ok(await LeaderboardRewards.HistoryAsync(db, userId, board, level, 12, ct));
        });
        // The weekly-prize popup was shown: don't show it again.
        group.MapPost("/engagement/leaderboard/rewards/seen", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await LeaderboardRewards.MarkSeenAsync(db, Me(http), ct);
            return Results.NoContent();
        });
        // Uploaded badge art kept inline (no cloud storage): served as an image file.
        group.MapGet("/engagement/badges/{id:long}/image", async (long id, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("SELECT image_key FROM badge WHERE badge_id=$1 AND status='ACTIVE'");
            command.Parameters.AddWithValue(id);
            return PlaceMediaEndpoints.StoredImage(await command.ExecuteScalarAsync(ct) as string);
        });
        // Places whose card is in its "Am hiểu" edition (every question stop answered correctly).
        group.MapGet("/engagement/mastery", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("SELECT place_id FROM user_place_mastery WHERE user_id=$1");
            command.Parameters.AddWithValue(Me(http));
            var ids = new List<long>();
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct)) ids.Add(reader.GetInt64(0));
            return Results.Ok(new { placeIds = ids });
        });
        group.MapGet("/engagement/leaderboard", async (string? scope, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await LeaderboardRewards.SettleAsync(db, ct);
            return Results.Ok(scope == "streak" ? await StreakLeaderboardAsync(db, Me(http), ct)
                : await LeaderboardAsync(db, Me(http), scope == "knowledge" ? "knowledge" : "activity", ct));
        });

        // ---------- F77: real-world check-in ----------
        group.MapPost("/explore/places/{placeId:long}/checkin", async (long placeId, CheckinRequest body, HttpContext http,
            NpgsqlDataSource db, CancellationToken ct) =>
        {
            if (body.Latitude is < -90 or > 90 || body.Longitude is < -180 or > 180)
                return Results.BadRequest(new { message = "Vị trí không hợp lệ." });
            var userId = Me(http);
            var today = EngagementCore.Today();
            await using var connection = await db.OpenConnectionAsync(ct);
            await using var transaction = await connection.BeginTransactionAsync(ct);
            string name; double lat, lon;
            await using (var command = new NpgsqlCommand("""
                SELECT p.name, p.latitude::float8, p.longitude::float8 FROM place p JOIN region r ON r.region_id=p.region_id
                WHERE p.place_id=$1 AND p.status='ACTIVE' AND r.status='ACTIVE'
                """, connection, transaction))
            {
                command.Parameters.AddWithValue(placeId);
                await using var reader = await command.ExecuteReaderAsync(ct);
                if (!await reader.ReadAsync(ct)) return Results.NotFound(new { message = "Địa danh không còn hoạt động." });
                name = reader.GetString(0); lat = reader.GetDouble(1); lon = reader.GetDouble(2);
            }
            // Check-in only where the game puts you: the destination of the journey being walked, or the
            // place your virtual location stands at.
            await using (var allowed = new NpgsqlCommand(
                $"SELECT {CheckinSql.IsJourneyDestination} OR {CheckinSql.IsAtVirtualLocation}", connection, transaction))
            {
                allowed.Parameters.AddWithValue(userId); allowed.Parameters.AddWithValue(placeId);
                if (!(bool)(await allowed.ExecuteScalarAsync(ct))!)
                    return Results.BadRequest(new { code = "NOT_JOURNEY_DESTINATION",
                        message = $"Chỉ check-in được tại điểm đến của hành trình đang đi hoặc nơi vị trí ảo của bạn đang đứng. Hãy chọn {name} làm điểm đến trước." });
            }
            var guard = Bcv.Api.Modules.Journey.Infrastructure.StepGuard.Enabled;
            if (body.Test)
            {
                if (guard) return Results.BadRequest(new { code = "TEST_DISABLED", message = "Check-in thử chỉ dùng trên máy chủ thử nghiệm." });
                body = body with { Latitude = lat, Longitude = lon };
            }
            var distance = EngagementCore.DistanceMeters(lat, lon, body.Latitude, body.Longitude);
            if (distance > CheckinRadiusMeters)
                return Results.BadRequest(new { code = "TOO_FAR", distanceMeters = (int)distance,
                    message = $"Bạn đang cách {name} khoảng {FormatDistance(distance)}. Hãy đến gần hơn (dưới {CheckinRadiusMeters} m) để check-in." });

            // Teleport check: compare with the user's previous check-in today.
            await using (var last = new NpgsqlCommand("""
                SELECT latitude::float8, longitude::float8,
                  (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - created_at)) / 3600.0)::float8
                FROM place_checkin WHERE user_id=$1 ORDER BY created_at DESC LIMIT 1
                """, connection, transaction))
            {
                last.Parameters.AddWithValue(userId);
                await using var reader = await last.ExecuteReaderAsync(ct);
                if (await reader.ReadAsync(ct))
                {
                    var km = EngagementCore.DistanceMeters(reader.GetDouble(0), reader.GetDouble(1), body.Latitude, body.Longitude) / 1000;
                    var hours = Math.Max(reader.GetDouble(2), 1.0 / 60);
                    if (guard && km > 5 && km / hours > 200)
                    {
                        await reader.DisposeAsync();
                        await FlagAsync(connection, transaction, userId, null, $"Check-in di chuyển {km:0} km trong {hours * 60:0} phút", 0, ct);
                        await transaction.CommitAsync(ct);
                        return Results.BadRequest(new { code = "IMPLAUSIBLE", message = "Vị trí thay đổi quá nhanh so với lần check-in trước." });
                    }
                }
            }
            int inserted;
            await using (var insert = new NpgsqlCommand("""
                INSERT INTO place_checkin(user_id, place_id, checkin_date, latitude, longitude, distance_meters)
                VALUES ($1,$2,$3,$4,$5,$6) ON CONFLICT (user_id, place_id, checkin_date) DO NOTHING
                """, connection, transaction))
            {
                insert.Parameters.AddWithValue(userId); insert.Parameters.AddWithValue(placeId); insert.Parameters.AddWithValue(today);
                insert.Parameters.AddWithValue((decimal)Math.Round(body.Latitude, 6)); insert.Parameters.AddWithValue((decimal)Math.Round(body.Longitude, 6));
                insert.Parameters.AddWithValue((int)distance);
                inserted = await insert.ExecuteNonQueryAsync(ct);
            }
            if (inserted == 0)
            {
                await transaction.CommitAsync(ct);
                return Results.Ok(new { success = true, alreadyToday = true, placeName = name,
                    message = $"Hôm nay bạn đã check-in tại {name}." });
            }
            // Unlocks the place without its card (cards come from finishing a journey or a flight ticket).
            // Checking in does not finish the journey either: the steps still have to be walked.
            var newDiscovery = await PlaceUnlock.DiscoverAsync(connection, transaction, userId, placeId, ct);
            // For the success dialog: the card still to earn by finishing the journey.
            string? pendingCard;
            await using (var card = new NpgsqlCommand($"SELECT c.name FROM {CheckinSql.PendingCard} LIMIT 1", connection, transaction))
            {
                card.Parameters.AddWithValue(userId); card.Parameters.AddWithValue(placeId);
                pendingCard = await card.ExecuteScalarAsync(ct) as string;
            }
            await AddPointsAsync(connection, transaction, userId, "CHECKIN", $"{placeId}:{today:yyyy-MM-dd}",
                CheckinPoints, $"Check-in tại {name}", ct);
            await Notifications.AddAsync(connection, transaction, userId, "PLACE_CHECKIN", "Check-in thành công",
                $"Bạn đã đến {name} ngoài đời thật! Hộ chiếu đã được đóng dấu, +{CheckinPoints} điểm khám phá.", ct);
            // The passport page this stamp lands on: places checked in within the place's province (region).
            string? province = null; int provinceStamped = 0, provinceTotal = 0, visits = 1;
            await using (var stamp = new NpgsqlCommand("""
                SELECT r.name,
                       count(DISTINCT p.place_id) FILTER (WHERE EXISTS
                         (SELECT 1 FROM place_checkin c WHERE c.user_id=$1 AND c.place_id=p.place_id))::int,
                       count(DISTINCT p.place_id)::int,
                       (SELECT count(*) FROM place_checkin c WHERE c.user_id=$1 AND c.place_id=$2)::int
                FROM place self JOIN region r ON r.region_id=self.region_id
                JOIN place p ON p.region_id=r.region_id AND p.status='ACTIVE'
                WHERE self.place_id=$2 GROUP BY r.name
                """, connection, transaction))
            {
                stamp.Parameters.AddWithValue(userId); stamp.Parameters.AddWithValue(placeId);
                await using var reader = await stamp.ExecuteReaderAsync(ct);
                if (await reader.ReadAsync(ct))
                {
                    province = reader.GetString(0); provinceStamped = reader.GetInt32(1);
                    provinceTotal = reader.GetInt32(2); visits = reader.GetInt32(3);
                }
            }
            await transaction.CommitAsync(ct);
            return Results.Ok(new { success = true, alreadyToday = false,
                placeName = name, points = CheckinPoints, newDiscovery, pendingCard, firstVisit = visits == 1, visits,
                province, provinceStamped, provinceTotal,
                message = $"Check-in tại {name}! Hộ chiếu đã đóng dấu, +{CheckinPoints} điểm khám phá." });
        });

        // ---------- F76: culture questions as extra "fuel" ----------
        group.MapGet("/journeys/current/quiz", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var journey = await ActiveJourneyAsync(db, Me(http), ct);
            if (journey is null) return Results.Ok(new { available = false, questions = Array.Empty<object>() });
            return Results.Ok(await QuizStateAsync(db, journey, ct));
        });
        group.MapPost("/journeys/current/quiz", async (QuizAnswer body, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var userId = Me(http);
            var journey = await ActiveJourneyAsync(db, userId, ct);
            if (journey is null) return Results.BadRequest(new { message = "Bạn chưa có hành trình đang thực hiện." });
            var questions = await CultureQuiz.BuildAsync(db, journey.JourneyId, journey.PlaceId, ct);
            var stopIndex = questions.ToList().FindIndex(q => q.Key == body.Key);
            if (stopIndex < 0) return Results.NotFound(new { message = "Không tìm thấy câu hỏi." });
            var question = questions[stopIndex];
            var today = EngagementCore.Today();
            await using var connection = await db.OpenConnectionAsync(ct);
            await using var transaction = await connection.BeginTransactionAsync(ct);
            int required, recorded, bonusSoFar;
            bool answered;
            await using (var command = new NpgsqlCommand("""
                SELECT required_steps, recorded_steps, bonus_steps,
                  EXISTS(SELECT 1 FROM journey_quiz_answer a WHERE a.journey_id=j.journey_id AND a.question_key=$2)
                FROM journey j WHERE journey_id=$1 AND status='IN_PROGRESS' FOR UPDATE
                """, connection, transaction))
            {
                command.Parameters.AddWithValue(journey.JourneyId); command.Parameters.AddWithValue(body.Key);
                await using var reader = await command.ExecuteReaderAsync(ct);
                if (!await reader.ReadAsync(ct)) return Results.BadRequest(new { message = "Hành trình đã kết thúc." });
                required = reader.GetInt32(0); recorded = reader.GetInt32(1); bonusSoFar = reader.GetInt32(2);
                answered = reader.GetBoolean(3);
            }
            if (answered) return Results.Conflict(new { message = "Bạn đã trả lời câu này rồi." });
            var unlockSteps = CultureQuiz.UnlockSteps(required, stopIndex, questions.Count);
            if (recorded < unlockSteps)
                return Results.Conflict(new { message = $"Trạm này mở khi bạn đi tới {CultureQuiz.UnlockPercent(stopIndex, questions.Count)}% hành trình (còn {unlockSteps - recorded:N0} bước)." });
            var correct = string.Equals(body.Answer.Trim(), question.Answer, StringComparison.Ordinal);
            await using (var insert = new NpgsqlCommand("""
                INSERT INTO journey_quiz_answer(journey_id, question_key, answered_on, is_correct, bonus_steps, chosen_answer)
                VALUES ($1,$2,$3,$4,0,$5)
                """, connection, transaction))
            {
                insert.Parameters.AddWithValue(journey.JourneyId); insert.Parameters.AddWithValue(body.Key);
                insert.Parameters.AddWithValue(today); insert.Parameters.AddWithValue(correct);
                insert.Parameters.AddWithValue(body.Answer.Trim()[..Math.Min(150, body.Answer.Trim().Length)]);
                await insert.ExecuteNonQueryAsync(ct);
            }
            // Rewards never touch the journey: walking stays the only way to the destination.
            var points = 0; var mastered = false;
            if (correct)
            {
                points += await AddPointsAsync(connection, transaction, userId, "QUIZ", $"{journey.JourneyId}:{body.Key}",
                    CultureQuiz.PointsFor(question.Difficulty),
                    $"Trả lời đúng câu hỏi ({CultureQuiz.DifficultyName(question.Difficulty).ToLowerInvariant()}) về {journey.PlaceName}", ct);
                await QuestProgress.RecordAsync(connection, transaction, userId, QuestProgress.QuizCorrect, 1, ct);
                int correctCount;
                await using (var count = new NpgsqlCommand("""
                    SELECT count(*)::int FROM journey_quiz_answer WHERE journey_id=$1 AND is_correct AND question_key = ANY($2)
                    """, connection, transaction))
                {
                    count.Parameters.AddWithValue(journey.JourneyId);
                    count.Parameters.AddWithValue(questions.Select(q => q.Key).ToArray());
                    correctCount = (int)(await count.ExecuteScalarAsync(ct))!;
                }
                if (correctCount == questions.Count)
                {
                    // Every stop right: the place's card is upgraded to its "Am hiểu" edition.
                    await using var master = new NpgsqlCommand("""
                        INSERT INTO user_place_mastery(user_id, place_id) VALUES ($1,$2) ON CONFLICT DO NOTHING
                        """, connection, transaction);
                    master.Parameters.AddWithValue(userId); master.Parameters.AddWithValue(journey.PlaceId);
                    mastered = await master.ExecuteNonQueryAsync(ct) > 0;
                    if (mastered)
                    {
                        points += await AddPointsAsync(connection, transaction, userId, "MASTERY", $"{journey.PlaceId}",
                            CultureQuiz.MasteryPoints, $"Am hiểu {journey.PlaceName}", ct);
                        await Notifications.AddAsync(connection, transaction, userId, "PLACE_MASTERED", "Thẻ Am hiểu",
                            $"Bạn đã trả lời đúng mọi trạm câu hỏi về {journey.PlaceName}. Thẻ địa danh này được nâng lên bản Am hiểu viền vàng!", ct);
                    }
                }
            }
            await transaction.CommitAsync(ct);
            var message = !correct ? $"Chưa đúng. Đáp án: {question.Answer}." : $"Chính xác! +{points} điểm khám phá.";
            return Results.Ok(new { correct, points, mastered, answer = question.Answer, explanation = question.Explanation, message });
        });

        // ---------- F88/F89: private journal ----------
        // Real visits only: a page opens with the first GPS check-in at the place. Virtual journeys
        // earn cards instead; there is nothing lived to write about.
        group.MapGet("/journal", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand($"""
                SELECT p.place_id, p.name, r.name, r.zone, d.visited_at,
                  (SELECT count(*)::int FROM journal_photo jp WHERE jp.user_id=$1 AND jp.place_id=p.place_id),
                  COALESCE((SELECT note FROM journal_note n WHERE n.user_id=$1 AND n.place_id=p.place_id), ''),
                  EXISTS(SELECT 1 FROM place_checkin c WHERE c.user_id=$1 AND c.place_id=p.place_id),
                  (SELECT jp.photo_id FROM journal_photo jp WHERE jp.user_id=$1 AND jp.place_id=p.place_id ORDER BY jp.photo_id DESC LIMIT 1),
                  {PlaceImageSql}
                FROM (SELECT user_id, place_id, min(created_at) AS visited_at FROM place_checkin GROUP BY user_id, place_id) d JOIN place p ON p.place_id=d.place_id JOIN region r ON r.region_id=p.region_id
                WHERE d.user_id=$1 ORDER BY d.visited_at DESC
                """);
            command.Parameters.AddWithValue(Me(http));
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
            var userId = Me(http);
            object? entry = null;
            await using (var command = db.CreateCommand($"""
                SELECT p.name, r.name, d.visited_at,
                  COALESCE((SELECT note FROM journal_note n WHERE n.user_id=$1 AND n.place_id=p.place_id), ''),
                  (SELECT count(*)::int FROM place_checkin c WHERE c.user_id=$1 AND c.place_id=p.place_id),
                  {PlaceImageSql}
                FROM (SELECT user_id, place_id, min(created_at) AS visited_at FROM place_checkin GROUP BY user_id, place_id) d JOIN place p ON p.place_id=d.place_id JOIN region r ON r.region_id=p.region_id
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
            if (entry is null) return Results.NotFound(new { message = "Nhật ký chỉ mở ở địa danh bạn đã check-in ngoài đời thật." });
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
                SELECT DISTINCT $1::bigint, place_id, $3::varchar FROM place_checkin WHERE user_id=$1 AND place_id=$2
                ON CONFLICT (user_id, place_id) DO UPDATE SET note=EXCLUDED.note, updated_at=CURRENT_TIMESTAMP
                """);
            command.Parameters.AddWithValue(Me(http)); command.Parameters.AddWithValue(placeId); command.Parameters.AddWithValue(note);
            return await command.ExecuteNonQueryAsync(ct) > 0 ? Results.Ok(new { success = true })
                : Results.NotFound(new { message = "Nhật ký chỉ mở ở địa danh bạn đã check-in ngoài đời thật." });
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
            if (!PlaceMediaEndpoints.ValidHeader(mime, bytes)) return Results.BadRequest(new { message = "Nội dung tệp không phải ảnh hợp lệ." });
            var userId = Me(http);
            await using var command = db.CreateCommand("""
                INSERT INTO journal_photo(user_id, place_id, caption, mime_type, photo_data)
                SELECT $1, d.place_id, $3, $4, $5 FROM (SELECT DISTINCT user_id, place_id FROM place_checkin) d
                WHERE d.user_id=$1 AND d.place_id=$2
                  AND (SELECT count(*) FROM journal_photo WHERE user_id=$1 AND place_id=$2) < $6
                RETURNING photo_id
                """);
            command.Parameters.AddWithValue(userId); command.Parameters.AddWithValue(placeId);
            command.Parameters.AddWithValue(caption?.Trim() ?? ""); command.Parameters.AddWithValue(mime);
            command.Parameters.AddWithValue(bytes); command.Parameters.AddWithValue(MaxJournalPhotosPerPlace);
            var id = await command.ExecuteScalarAsync(ct);
            return id is long photoId ? Results.Ok(new { photoId, message = "Đã lưu ảnh vào nhật ký (chỉ bạn xem được)." })
                : Results.BadRequest(new { message = $"Chỉ lưu ảnh ở địa danh đã check-in thật, tối đa {MaxJournalPhotosPerPlace} ảnh mỗi nơi." });
        }).WithMetadata(new Microsoft.AspNetCore.Mvc.RequestSizeLimitAttribute(10 * 1024 * 1024 + 1024));
        group.MapGet("/journal/photos/{photoId:long}/content", async (long photoId, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("SELECT mime_type, photo_data FROM journal_photo WHERE photo_id=$1 AND user_id=$2");
            command.Parameters.AddWithValue(photoId); command.Parameters.AddWithValue(Me(http));
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct)) return Results.NotFound();
            http.Response.Headers.CacheControl = "private, max-age=86400";
            http.Response.Headers["X-Content-Type-Options"] = "nosniff";
            return Results.File(reader.GetFieldValue<byte[]>(1), reader.GetString(0));
        });
        group.MapDelete("/journal/photos/{photoId:long}", async (long photoId, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("DELETE FROM journal_photo WHERE photo_id=$1 AND user_id=$2");
            command.Parameters.AddWithValue(photoId); command.Parameters.AddWithValue(Me(http));
            return await command.ExecuteNonQueryAsync(ct) > 0 ? Results.Ok(new { success = true }) : Results.NotFound();
        });
    }

    // ================= helpers =================

    private sealed record ActiveJourney(long JourneyId, long PlaceId, string PlaceName);

    private static async Task<ActiveJourney?> ActiveJourneyAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT j.journey_id, p.place_id, p.name FROM journey j JOIN place p ON p.place_id=j.destination_place_id
            WHERE j.user_id=$1 AND j.status='IN_PROGRESS'
            """);
        command.Parameters.AddWithValue(userId);
        await using var reader = await command.ExecuteReaderAsync(ct);
        return await reader.ReadAsync(ct) ? new ActiveJourney(reader.GetInt64(0), reader.GetInt64(1), reader.GetString(2)) : null;
    }

    private static async Task<object> QuizStateAsync(NpgsqlDataSource db, ActiveJourney journey, CancellationToken ct)
    {
        var questions = await CultureQuiz.BuildAsync(db, journey.JourneyId, journey.PlaceId, ct);
        if (questions.Count == 0)
            return new { available = false, placeName = journey.PlaceName,
                message = $"{journey.PlaceName} chưa có câu hỏi văn hóa. Bộ câu hỏi đang được bổ sung." };
        var answers = new Dictionary<string, (bool Correct, string? Chosen, int Bonus)>();
        int required = 0, recorded = 0;
        await using (var command = db.CreateCommand("""
            SELECT question_key, is_correct, chosen_answer, bonus_steps FROM journey_quiz_answer WHERE journey_id=$1
            """))
        {
            command.Parameters.AddWithValue(journey.JourneyId);
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                answers[reader.GetString(0)] = (reader.GetBoolean(1), reader.IsDBNull(2) ? null : reader.GetString(2), reader.GetInt32(3));
        }
        await using (var command = db.CreateCommand("SELECT required_steps, recorded_steps FROM journey WHERE journey_id=$1"))
        {
            command.Parameters.AddWithValue(journey.JourneyId);
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (await reader.ReadAsync(ct)) { required = reader.GetInt32(0); recorded = reader.GetInt32(1); }
        }
        // One stop per question, evenly spread. Locked stops never reveal their question.
        var stops = questions.Select((q, i) =>
        {
            var unlockSteps = CultureQuiz.UnlockSteps(required, i, questions.Count);
            var state = answers.TryGetValue(q.Key, out var a) ? (a.Correct ? "CORRECT" : "WRONG")
                : recorded >= unlockSteps ? "OPEN" : "LOCKED";
            var revealed = state != "LOCKED";
            return new {
                key = q.Key, number = i + 1, state,
                difficulty = q.Difficulty, difficultyName = CultureQuiz.DifficultyName(q.Difficulty),
                points = CultureQuiz.PointsFor(q.Difficulty),
                fraction = CultureQuiz.UnlockFraction(i, questions.Count),
                unlockPercent = CultureQuiz.UnlockPercent(i, questions.Count),
                unlockSteps, stepsToGo = Math.Max(0, unlockSteps - recorded),
                text = revealed ? q.Text : null,
                options = revealed ? q.Options : null,
                answer = state is "CORRECT" or "WRONG" ? q.Answer : null,
                chosen = state is "CORRECT" or "WRONG" ? a.Chosen : null,
                explanation = state is "CORRECT" or "WRONG" ? q.Explanation : null
            };
        }).ToList();
        bool mastered;
        await using (var command = db.CreateCommand("""
            SELECT EXISTS(SELECT 1 FROM user_place_mastery m JOIN journey j ON j.user_id=m.user_id AND j.destination_place_id=m.place_id
                          WHERE j.journey_id=$1)
            """))
        {
            command.Parameters.AddWithValue(journey.JourneyId);
            mastered = (bool)(await command.ExecuteScalarAsync(ct))!;
        }
        var correctCount = stops.Count(s => s.state == "CORRECT");
        var correctPoints = stops.Where(s => s.state == "CORRECT").Sum(s => s.points);
        return new {
            available = true, journeyId = journey.JourneyId, placeName = journey.PlaceName,
            requiredSteps = required, recordedSteps = recorded,
            stops, openCount = stops.Count(s => s.state == "OPEN"),
            answeredCount = stops.Count(s => s.state is "CORRECT" or "WRONG"), totalQuestions = stops.Count,
            // Card pieces: one per correct stop; all of them = "Am hiểu" edition of the place's card.
            correctCount, mastered, masteryPoints = CultureQuiz.MasteryPoints,
            pointsByDifficulty = new { easy = CultureQuiz.PointsFor(1), medium = CultureQuiz.PointsFor(2), hard = CultureQuiz.PointsFor(3) },
            earnedPoints = correctPoints + (mastered ? CultureQuiz.MasteryPoints : 0)
        };
    }

    private static async Task<object> StreakJsonAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        var today = EngagementCore.Today();
        var days = await EngagementCore.HistoryAsync(db, userId, ct);
        var (level, goal) = await EngagementCore.DailyGoalAsync(db, userId, ct);
        var streak = EngagementCore.ComputeStreak(days, today);
        var todaySteps = days.FirstOrDefault(d => d.Date == today)?.Steps ?? 0;
        // A missed day: a past day (not today) since the user started tracking whose goal was not reached.
        var firstDay = days.Count > 0 ? days.Min(x => x.Date) : today;
        var week = Enumerable.Range(0, 7).Select(i => today.AddDays(i - 6)).Select(d =>
        {
            var row = days.FirstOrDefault(x => x.Date == d);
            var met = row is not null && row.Steps >= row.Goal;
            return new { date = d.ToString("yyyy-MM-dd"), steps = row?.Steps ?? 0, goal = row?.Goal ?? goal,
                met, missed = !met && d < today && d >= firstDay };
        }).ToList();
        return new {
            current = streak.Current, best = streak.Best, todayDone = streak.TodayDone,
            todaySteps, dailyGoal = goal, activityLevel = level,
            week
        };
    }

    /// <summary>
    /// F81 passport: every real GPS check-in stamps one place. The 34 provinces (Khu vực, after the 2025 merger)
    /// show how many of their places carry a stamp and rise bronze → silver → gold
    /// (1 place → half of them → all of them). Virtual discovery earns cards instead, never stamps.
    /// </summary>
    private static async Task<object> PassportAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        // One passport page per Khu vực (the 34 provinces), grouped by its Vùng.
        var rows = new List<(long ProvinceId, string Province, string Area, long PlaceId, string Place, DateOnly? Visit)>();
        await using (var command = db.CreateCommand("""
            SELECT r.region_id, r.name, r.zone,
                   p.place_id, p.name,
                   (SELECT min(c.checkin_date) FROM place_checkin c WHERE c.user_id=$1 AND c.place_id=p.place_id)
            FROM region r
            LEFT JOIN place p ON p.region_id=r.region_id AND p.status='ACTIVE'
            WHERE r.status='ACTIVE' AND r.zone IS NOT NULL
            ORDER BY r.name, p.name
            """))
        {
            command.Parameters.AddWithValue(userId);
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                rows.Add((reader.GetInt64(0), reader.GetString(1), reader.GetString(2),
                    reader.IsDBNull(3) ? 0 : reader.GetInt64(3), reader.IsDBNull(4) ? "" : reader.GetString(4),
                    reader.IsDBNull(5) ? null : reader.GetFieldValue<DateOnly>(5)));
        }
        var provinces = rows.GroupBy(r => (r.ProvinceId, r.Province, r.Area)).Select(g =>
        {
            var places = g.Where(r => r.PlaceId != 0).ToList();
            int visited = places.Count(p => p.Visit != null), total = places.Count;
            var tier = visited == 0 ? "NONE" : visited >= total ? "GOLD" : visited * 2 >= total ? "SILVER" : "BRONZE";
            return new
            {
                area = g.Key.Area, provinceId = g.Key.ProvinceId, name = g.Key.Province, visited, total, tier,
                firstVisit = places.Where(p => p.Visit != null).Min(p => p.Visit)?.ToString("yyyy-MM-dd"),
                places = places.Select(p => new { placeId = p.PlaceId, name = p.Place, stamped = p.Visit != null,
                    date = p.Visit?.ToString("yyyy-MM-dd") }).ToList()
            };
        }).ToList();
        var areas = PassportAreas.Select(a => new
        {
            code = a.Code, name = a.Name,
            provinces = provinces.Where(p => p.area == a.Code).ToList()
        }).Where(a => a.provinces.Count > 0).ToList();
        return new
        {
            stamped = provinces.Count(p => p.visited > 0), total = provinces.Count,
            placesStamped = provinces.Sum(p => p.visited), placesTotal = provinces.Sum(p => p.total),
            gold = provinces.Count(p => p.tier == "GOLD"), zones = areas
        };
    }

    /// <summary>Passport pages: the 6 regions, in the order of <see cref="EngagementCore.ZoneNames"/>.</summary>
    private static readonly (string Code, string Name)[] PassportAreas =
        EngagementCore.ZoneNames.Select(z => (z.Key, z.Value)).ToArray();

    /// <summary>[ImageUrl]: https art as is, uploaded (inline) art through /api/engagement/badges/{id}/image.</summary>
    private sealed record Badge(string Name, string Description, bool Earned, string Progress, string? ImageUrl);

    private static async Task<IReadOnlyList<Badge>> BadgesAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        int discovered, zonesTouched, zonesComplete, checkins, correct, masteredCount, provincesVisited, podiums;
        await using (var command = db.CreateCommand("""
            WITH mine AS (SELECT p.place_id, r.zone FROM user_discovered_place d JOIN place p ON p.place_id=d.place_id
                          JOIN region r ON r.region_id=p.region_id WHERE d.user_id=$1),
                 totals AS (SELECT r.zone, count(*) AS total FROM place p JOIN region r ON r.region_id=p.region_id
                            WHERE p.status='ACTIVE' AND r.status='ACTIVE' AND r.zone IS NOT NULL GROUP BY r.zone)
            SELECT (SELECT count(*)::int FROM mine),
                   (SELECT count(DISTINCT zone)::int FROM mine WHERE zone IS NOT NULL),
                   (SELECT count(*)::int FROM totals t WHERE t.total <= (SELECT count(*) FROM mine m WHERE m.zone=t.zone)),
                   (SELECT count(DISTINCT place_id)::int FROM place_checkin WHERE user_id=$1),
                   (SELECT count(*)::int FROM journey_quiz_answer a JOIN journey j ON j.journey_id=a.journey_id
                      WHERE j.user_id=$1 AND a.is_correct),
                   (SELECT count(*)::int FROM user_place_mastery WHERE user_id=$1),
                   (SELECT count(DISTINCT p.region_id)::int FROM place_checkin c JOIN place p ON p.place_id=c.place_id WHERE c.user_id=$1),
                   (SELECT count(*)::int FROM leaderboard_reward WHERE user_id=$1)
            """))
        {
            command.Parameters.AddWithValue(userId);
            await using var reader = await command.ExecuteReaderAsync(ct);
            await reader.ReadAsync(ct);
            discovered = reader.GetInt32(0); zonesTouched = reader.GetInt32(1); zonesComplete = reader.GetInt32(2);
            checkins = reader.GetInt32(3); correct = reader.GetInt32(4); masteredCount = reader.GetInt32(5);
            provincesVisited = reader.GetInt32(6); podiums = reader.GetInt32(7);
        }
        var days = await EngagementCore.HistoryAsync(db, userId, ct);
        var best = EngagementCore.ComputeStreak(days, EngagementCore.Today()).Best;
        // The badges themselves live in the badge table (admin web); each names one of these metrics.
        var metrics = new Dictionary<string, int>
        {
            ["PLACES_DISCOVERED"] = discovered, ["ZONES_TOUCHED"] = zonesTouched, ["ZONES_COMPLETED"] = zonesComplete,
            ["BEST_STREAK"] = best, ["REAL_CHECKINS"] = checkins, ["PROVINCES_STAMPED"] = provincesVisited,
            ["QUIZ_CORRECT"] = correct, ["CARDS_MASTERED"] = masteredCount, ["PODIUM_FINISHES"] = podiums,
        };
        var badges = new List<Badge>();
        await using (var list = db.CreateCommand("""
            SELECT name, description, condition_type, target_value,
                   CASE WHEN image_key ILIKE 'https://%' THEN image_key END, coalesce(image_key, '') <> '', badge_id
            FROM badge WHERE status='ACTIVE' ORDER BY sort_order, badge_id
            """))
        await using (var reader = await list.ExecuteReaderAsync(ct))
            while (await reader.ReadAsync(ct))
            {
                // Only the URL is read: inline art (up to megabytes) stays in the database until requested.
                var value = metrics.GetValueOrDefault(reader.GetString(2));
                var target = reader.GetInt32(3);
                var imageUrl = !reader.IsDBNull(4) ? reader.GetString(4)
                    : reader.GetBoolean(5) ? $"/api/engagement/badges/{reader.GetInt64(6)}/image" : null;
                badges.Add(new Badge(reader.GetString(0), reader.GetString(1),
                    value >= target, $"{Math.Min(value, target)}/{target}", imageUrl));
            }
        return badges;
    }

    /// <summary>Every board lists its top 50; a user below that still gets their own rank in <c>me</c>.</summary>
    private const int TopSize = 50;

    /// <summary>Longest streak each person ever kept (their record), among users at the same activity level.</summary>
    private static async Task<object> StreakLeaderboardAsync(NpgsqlDataSource db, long userId, CancellationToken ct)
    {
        var today = EngagementCore.Today();
        var people = new Dictionary<long, (string Name, string? Avatar)>();
        var days = new Dictionary<long, List<EngagementCore.DayRecord>>();
        await using (var command = db.CreateCommand("""
            SELECT u.user_id, u.full_name, CASE WHEN u.avatar LIKE 'https://%' THEN u.avatar END,
                   a.activity_date, a.steps, a.goal_steps
            FROM daily_activity a
            JOIN app_user u ON u.user_id=a.user_id AND u.status='ACTIVE' AND u.role <> 'ADMIN'
            JOIN user_preference pref ON pref.user_id=u.user_id
            WHERE pref.activity_level = (SELECT activity_level FROM user_preference WHERE user_id=$1)
            ORDER BY u.user_id, a.activity_date
            """))
        {
            command.Parameters.AddWithValue(userId);
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
            {
                var id = reader.GetInt64(0);
                people[id] = (reader.GetString(1), reader.IsDBNull(2) ? null : reader.GetString(2));
                if (!days.TryGetValue(id, out var list)) days[id] = list = [];
                list.Add(new EngagementCore.DayRecord(reader.GetFieldValue<DateOnly>(3), reader.GetInt32(4), reader.GetInt32(5)));
            }
        }
        var rows = people.Select(p => (Id: p.Key, p.Value.Name, Value: EngagementCore.ComputeStreak(days[p.Key], today).Best, p.Value.Avatar))
            .Where(r => r.Value > 0).OrderByDescending(r => r.Value).ThenBy(r => r.Id).ToList();
        var myIndex = rows.FindIndex(r => r.Id == userId);
        var (level, _) = await EngagementCore.DailyGoalAsync(db, userId, ct);
        var podium = await LeaderboardRewards.PodiumAsync(db, "streak", level, ct);
        var championId = podium.FirstOrDefault(p => p.Rank == 1).UserId;
        object Row((long Id, string Name, int Value, string? Avatar) r, int index) => new {
            rank = index + 1, name = DisplayName(r.Name), value = r.Value, unit = "ngày", avatar = r.Avatar, isMe = r.Id == userId,
            champion = championId != 0 && r.Id == championId };
        return new {
            scope = "streak", unit = "ngày",
            scopeName = level switch { "LOW" => "Cùng mức vận động nhẹ", "HIGH" => "Cùng mức vận động cao", _ => "Cùng mức vận động vừa" },
            participants = rows.Count,
            prizes = await LeaderboardRewards.PrizesAsync(db, ct),
            top = rows.Take(TopSize).Select(Row).ToList(),
            me = myIndex >= 0 ? Row(rows[myIndex], myIndex) : null
        };
    }

    private static async Task<object> LeaderboardAsync(NpgsqlDataSource db, long userId, string scope, CancellationToken ct)
    {
        var today = EngagementCore.Today();
        var monday = today.AddDays(-(((int)today.DayOfWeek + 6) % 7));
        var knowledge = scope == "knowledge";
        var sql = knowledge ? """
            SELECT u.user_id, u.full_name, SUM(p.points)::int, CASE WHEN u.avatar LIKE 'https://%' THEN u.avatar END
            FROM explorer_point p
            JOIN app_user u ON u.user_id=p.user_id AND u.status='ACTIVE' AND u.role <> 'ADMIN'
            WHERE p.created_at >= $1::timestamp
            GROUP BY u.user_id, u.full_name, u.avatar
            HAVING SUM(p.points) > 0
            ORDER BY 3 DESC, u.user_id
            """ : """
            SELECT u.user_id, u.full_name, SUM(a.steps)::int, CASE WHEN u.avatar LIKE 'https://%' THEN u.avatar END
            FROM daily_activity a
            JOIN app_user u ON u.user_id=a.user_id AND u.status='ACTIVE' AND u.role <> 'ADMIN'
            JOIN user_preference pref ON pref.user_id=u.user_id
            WHERE a.activity_date BETWEEN $2 AND $3
              AND pref.activity_level = (SELECT activity_level FROM user_preference WHERE user_id=$1)
              AND NOT EXISTS(SELECT 1 FROM step_flag f WHERE f.user_id=u.user_id AND f.created_at >= $2::timestamp)
            GROUP BY u.user_id, u.full_name, u.avatar
            HAVING SUM(a.steps) > 0
            ORDER BY 3 DESC, u.user_id
            """;
        await using var command = db.CreateCommand(sql);
        if (knowledge) command.Parameters.AddWithValue(monday);
        else { command.Parameters.AddWithValue(userId); command.Parameters.AddWithValue(monday); command.Parameters.AddWithValue(today); }
        var rows = new List<(long Id, string Name, int Value, string? Avatar)>();
        await using (var reader = await command.ExecuteReaderAsync(ct))
            while (await reader.ReadAsync(ct))
                rows.Add((reader.GetInt64(0), reader.GetString(1), reader.GetInt32(2), reader.IsDBNull(3) ? null : reader.GetString(3)));
        var myIndex = rows.FindIndex(r => r.Id == userId);
        var unit = knowledge ? "điểm" : "bước";
        string? scopeName;
        var level = "";
        if (knowledge) scopeName = "Tất cả người khám phá";
        else
        {
            (level, _) = await EngagementCore.DailyGoalAsync(db, userId, ct);
            scopeName = level switch { "LOW" => "Cùng mức vận động nhẹ", "HIGH" => "Cùng mức vận động cao", _ => "Cùng mức vận động vừa" };
        }
        // Last week's #1 of this board is marked "Quán quân tuần".
        var podium = await LeaderboardRewards.PodiumAsync(db, knowledge ? "knowledge" : "activity", level, ct);
        var championId = podium.FirstOrDefault(p => p.Rank == 1).UserId;
        object Row((long Id, string Name, int Value, string? Avatar) r, int index) => new {
            rank = index + 1, name = DisplayName(r.Name), value = r.Value, unit, avatar = r.Avatar, isMe = r.Id == userId,
            champion = championId != 0 && r.Id == championId };
        return new {
            scope = knowledge ? "knowledge" : "activity", scopeName, unit,
            weekStart = monday.ToString("yyyy-MM-dd"), weekEnd = monday.AddDays(6).ToString("yyyy-MM-dd"),
            participants = rows.Count,
            prizes = await LeaderboardRewards.PrizesAsync(db, ct),
            top = rows.Take(TopSize).Select(Row).ToList(),
            me = myIndex >= 0 ? Row(rows[myIndex], myIndex) : null
        };
    }

    /// <summary>The display name the user chose in their profile, kept short for the board.</summary>
    private static string DisplayName(string fullName)
    {
        var name = fullName.Trim();
        return name.Length <= 24 ? name : name[..23].TrimEnd() + "…";
    }

    private static readonly System.Globalization.CultureInfo Vi = System.Globalization.CultureInfo.GetCultureInfo("vi-VN");

    private static string FormatDistance(double meters) =>
        meters >= 1000 ? (meters / 1000).ToString("#,##0.#", Vi) + " km" : meters.ToString("0", Vi) + " m";

    /// <summary>Adds explorer points once per (source, ref); returns the points actually added.</summary>
    private static async Task<int> AddPointsAsync(NpgsqlConnection connection, NpgsqlTransaction transaction, long userId,
        string source, string refKey, int points, string note, CancellationToken ct)
    {
        await using var command = new NpgsqlCommand("""
            INSERT INTO explorer_point(user_id, source, ref_key, points, note) VALUES ($1,$2,$3,$4,$5)
            ON CONFLICT (user_id, source, ref_key) DO NOTHING
            """, connection, transaction);
        command.Parameters.AddWithValue(userId); command.Parameters.AddWithValue(source);
        command.Parameters.AddWithValue(refKey); command.Parameters.AddWithValue(points); command.Parameters.AddWithValue(note);
        return await command.ExecuteNonQueryAsync(ct) > 0 ? points : 0;
    }

    public static async Task FlagAsync(NpgsqlConnection connection, NpgsqlTransaction transaction, long userId, long? journeyId,
        string reason, int rejectedSteps, CancellationToken ct)
    {
        await using var command = new NpgsqlCommand("""
            INSERT INTO step_flag(user_id, journey_id, reason, rejected_steps) VALUES ($1,$2,$3,$4)
            """, connection, transaction);
        command.Parameters.AddWithValue(userId);
        command.Parameters.Add(new NpgsqlParameter<long?> { TypedValue = journeyId, NpgsqlDbType = NpgsqlTypes.NpgsqlDbType.Bigint });
        command.Parameters.AddWithValue(reason[..Math.Min(200, reason.Length)]);
        command.Parameters.AddWithValue(rejectedSteps);
        await command.ExecuteNonQueryAsync(ct);
    }
}
