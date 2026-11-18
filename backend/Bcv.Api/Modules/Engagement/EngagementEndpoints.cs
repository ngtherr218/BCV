using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.PlaceCollection.Infrastructure;
using Bcv.Api.Modules.QuestItem.Infrastructure;
using Bcv.Api.Modules.StatsNotify.Infrastructure;
using Npgsql;

namespace Bcv.Api.Modules.Engagement;

public static partial class EngagementEndpoints
{
    public sealed record QuizAnswer(string Key, string Answer);
    public static void MapEngagementEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api");
        group.AddEndpointFilter(async (context, next) =>
        {
            var user = CurrentUser.Get(context.HttpContext.Request, context.HttpContext.RequestServices.GetRequiredService<ISessionStore>());
            if (user is null) return Results.Unauthorized();
            var accounts = context.HttpContext.RequestServices.GetRequiredService<AccountService>();
            if (await accounts.GetActiveUser(user.UserId, context.HttpContext.RequestAborted) is null) return Results.Unauthorized();
            context.HttpContext.Items["UserId"] = user.UserId;
            context.HttpContext.Response.Headers.CacheControl = "no-store";
            return await next(context);
        });
        MapCollectionChallengeEndpoints(group);
        static long Me(HttpContext http) => (long)http.Items["UserId"]!;

        group.MapGet("/engagement/mastered-places", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            await using var command = db.CreateCommand("SELECT place_id FROM user_place_mastery WHERE user_id=$1 ORDER BY place_id");
            command.Parameters.AddWithValue(Me(http));
            var placeIds = new List<long>();
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct)) placeIds.Add(reader.GetInt64(0));
            return Results.Ok(new { placeIds });
        });
        group.MapGet("/journeys/current/quiz", async (HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            var journey = await ActiveJourneyAsync(db, Me(http), ct);
            if (journey is null) return Results.Ok(new { available = false, questions = Array.Empty<object>() });
            return Results.Ok(await QuizStateAsync(db, journey, ct));
        });
        group.MapPost("/journeys/current/quiz", async (QuizAnswer body, HttpContext http, NpgsqlDataSource db, CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(body.Key) || string.IsNullOrWhiteSpace(body.Answer) || body.Answer.Length > 150)
                return Results.BadRequest(new { message = "Câu trả lời không hợp lệ." });
            var userId = Me(http);
            var journey = await ActiveJourneyAsync(db, userId, ct);
            if (journey is null) return Results.BadRequest(new { message = "Bạn chưa có hành trình đang thực hiện." });
            var questions = await CultureQuiz.BuildAsync(db, journey.JourneyId, journey.PlaceId, ct);
            var stopIndex = questions.ToList().FindIndex(q => q.Key == body.Key);
            if (stopIndex < 0) return Results.NotFound(new { message = "Không tìm thấy câu hỏi." });
            var question = questions[stopIndex];
            var today = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeBySystemTimeZoneId(DateTime.UtcNow, "Asia/Ho_Chi_Minh"));
            await using var connection = await db.OpenConnectionAsync(ct);
            await using var transaction = await connection.BeginTransactionAsync(ct);
            int required, recorded, bonusSoFar;
            bool answered;
            await using (var command = new NpgsqlCommand("""
                SELECT required_steps, recorded_steps, bonus_steps
                FROM journey j WHERE journey_id=$1 AND status='IN_PROGRESS' FOR UPDATE
                """, connection, transaction))
            {
                command.Parameters.AddWithValue(journey.JourneyId);
                await using var reader = await command.ExecuteReaderAsync(ct);
                if (!await reader.ReadAsync(ct)) return Results.BadRequest(new { message = "Hành trình đã kết thúc." });
                required = reader.GetInt32(0); recorded = reader.GetInt32(1); bonusSoFar = reader.GetInt32(2);

            }
            // Re-read after obtaining the journey lock: a concurrent answer may have committed
            // while this request waited. A separate statement sees that committed answer.
            await using (var check = new NpgsqlCommand("SELECT EXISTS(SELECT 1 FROM journey_quiz_answer WHERE journey_id=$1 AND question_key=$2)", connection, transaction))
            {
                check.Parameters.AddWithValue(journey.JourneyId); check.Parameters.AddWithValue(body.Key);
                answered = (bool)(await check.ExecuteScalarAsync(ct))!;
            }
            if (answered) return Results.Conflict(new { message = "Bạn đã trả lời câu này rồi." });
            var unlockSteps = CultureQuiz.UnlockSteps(required, stopIndex, questions.Count);
            if (recorded < unlockSteps)
                return Results.Conflict(new { message = $"Trạm này mở khi bạn đi tới {CultureQuiz.UnlockPercent(stopIndex, questions.Count)}% hành trình (còn {unlockSteps - recorded:N0} bước)." });
            if (!question.Options.Contains(body.Answer.Trim())) return Results.BadRequest(new { message = "Hãy chọn một đáp án trong câu hỏi." });
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
                    CultureQuiz.PointsPerCorrect, $"Trả lời đúng câu hỏi về {journey.PlaceName}", journey.PlaceId, ct);
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
                            CultureQuiz.MasteryPoints, $"Am hiểu {journey.PlaceName}", journey.PlaceId, ct);
                        await Notifications.AddAsync(connection, transaction, userId, "PLACE_MASTERED", "Thẻ Am hiểu",
                            $"Bạn đã trả lời đúng mọi trạm câu hỏi về {journey.PlaceName}. Thẻ địa danh này được nâng lên bản Am hiểu viền vàng!", ct);
                    }
                }
            }
            await transaction.CommitAsync(ct);
            var message = !correct ? $"Chưa đúng. Đáp án: {question.Answer}." : $"Chính xác! +{points} điểm khám phá.";
            return Results.Ok(new { correct, points, mastered, answer = question.Answer, explanation = question.Explanation, message });
        });

    }
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
        return new {
            available = true, journeyId = journey.JourneyId, placeName = journey.PlaceName,
            requiredSteps = required, recordedSteps = recorded,
            stops, openCount = stops.Count(s => s.state == "OPEN"),
            answeredCount = stops.Count(s => s.state is "CORRECT" or "WRONG"), totalQuestions = stops.Count,
            // Card pieces: one per correct stop; all of them = "Am hiểu" edition of the place's card.
            correctCount, mastered, pointsPerCorrect = CultureQuiz.PointsPerCorrect, masteryPoints = CultureQuiz.MasteryPoints,
            earnedPoints = correctCount * CultureQuiz.PointsPerCorrect + (mastered ? CultureQuiz.MasteryPoints : 0)
        };
    }

    private static async Task<int> AddPointsAsync(NpgsqlConnection connection, NpgsqlTransaction transaction,
        long userId, string source, string refKey, int points, string note, long placeId, CancellationToken ct)
    {
        await using var command = new NpgsqlCommand("""
            INSERT INTO explorer_score_event(user_id,event_type,source_key,points,rule_version,occurred_at,place_id,region_id)
            SELECT $1,$2,$3,$4,'v4:quiz',CURRENT_TIMESTAMP,p.place_id,p.region_id
            FROM place p WHERE p.place_id=$5
            ON CONFLICT(user_id,event_type,source_key) DO NOTHING RETURNING points
            """, connection, transaction);
        command.Parameters.AddWithValue(userId); command.Parameters.AddWithValue(source);
        command.Parameters.AddWithValue(refKey); command.Parameters.AddWithValue(points); command.Parameters.AddWithValue(placeId);
        return await command.ExecuteScalarAsync(ct) is int awarded ? awarded : 0;
    }
}
