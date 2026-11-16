using Npgsql;

namespace Bcv.Api.Modules.Engagement;

/// <summary>
/// Applies db/027_engagement.sql and the sample question bank db/028_seed_place_questions.sql at
/// start-up. Both are re-runnable: the seed only inserts questions that are not there yet.
/// </summary>
public static class EngagementSchema
{
    public static async Task EnsureAsync(NpgsqlDataSource db)
    {
        await using var connection = await db.OpenConnectionAsync();
        await using var transaction = await connection.BeginTransactionAsync();
        await using (var gate = new NpgsqlCommand("SELECT pg_advisory_xact_lock(7246320)", connection, transaction))
            await gate.ExecuteNonQueryAsync();
        foreach (var resource in new[] { "Bcv.EngagementMigration.sql", "Bcv.PlaceQuestionSeed.sql" })
        {
            await using var stream = typeof(EngagementSchema).Assembly.GetManifestResourceStream(resource);
            if (stream is null) continue;
            using var reader = new StreamReader(stream);
            await using var command = new NpgsqlCommand(await reader.ReadToEndAsync(), connection, transaction);
            await command.ExecuteNonQueryAsync();
        }
        await transaction.CommitAsync();
    }
}
