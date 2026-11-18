using Npgsql;

namespace Bcv.Api.Modules.Engagement;

/// <summary>
/// F76: culture questions about the active journey's destination, read from the curated
/// <c>place_question</c> bank (ACTIVE rows only). A place without approved questions offers none.
/// </summary>
public static class CultureQuiz
{
    /// <summary>Explorer points for a correct answer, by the question's difficulty (1 Dễ, 2 Vừa, 3 Khó).</summary>
    public static int PointsFor(int difficulty) => difficulty switch { <= 1 => 10, 2 => 20, _ => 30 };
    public static string DifficultyName(int difficulty) => difficulty switch { <= 1 => "Dễ", 2 => "Vừa", _ => "Khó" };
    /// <summary>The extra for answering every stop of a place correctly.</summary>
    public const int MasteryPoints = 50;

    /// <summary>
    /// Stops are spread evenly along the journey: with N questions, stop i (0-based) opens at
    /// (i+1)/(N+1) of the required steps — 4 questions open at 20 %, 40 %, 60 %, 80 %.
    /// </summary>
    public static double UnlockFraction(int index, int count) => (index + 1.0) / (count + 1.0);
    public static int UnlockPercent(int index, int count) => (int)Math.Round(UnlockFraction(index, count) * 100);
    public static int UnlockSteps(int requiredSteps, int index, int count) =>
        (int)Math.Ceiling(requiredSteps * UnlockFraction(index, count));

    public sealed record Question(string Key, string Text, IReadOnlyList<string> Options, string Answer, string Explanation, int Difficulty);

    /// <summary>Easiest first, then in the order they were written. Key "Q{id}" is what answers are stored under.</summary>
    public static async Task<IReadOnlyList<Question>> BuildAsync(NpgsqlDataSource db, long journeyId, long placeId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT question_id, question, option_a, option_b, option_c, option_d, answer_index, explanation, difficulty
            FROM place_question WHERE place_id=$1 AND status='ACTIVE'
            ORDER BY difficulty, question_id
            """);
        command.Parameters.AddWithValue(placeId);
        var questions = new List<Question>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            var options = new[] { reader.GetString(2), reader.GetString(3), reader.GetString(4), reader.GetString(5) };
            var answer = reader.GetInt16(6);
            if (answer is < 0 or > 3) continue;
            questions.Add(new Question($"Q{reader.GetInt64(0)}", reader.GetString(1), options, options[answer], reader.GetString(7),
                Convert.ToInt32(reader.GetValue(8))));
        }
        return questions;
    }
}
