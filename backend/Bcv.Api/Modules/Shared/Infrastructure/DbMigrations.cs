using System.Security.Cryptography;
using System.Text;
using Npgsql;

namespace Bcv.Api.Modules.Shared.Infrastructure;

/// <summary>
/// Applies the numbered db/*.sql files embedded in the API, in order, each once. What ran is kept in
/// schema_migration (version, checksum), so a start-up only runs files that are new. A repeatable file
/// (a seed) runs again when its content changes. Each file runs in its own transaction under an advisory
/// lock, so two instances starting together don't both apply it.
/// To change the schema, add a new numbered file (and list it here); never edit one that has shipped.
/// </summary>
public static class DbMigrations
{
    private sealed record Migration(string Version, string File, bool Repeatable = false);

    private static readonly Migration[] All =
    [
        new("023", "023_place_type_features.sql"),
        new("027", "027_engagement.sql"),
        new("028", "028_seed_place_questions.sql", Repeatable: true),
        new("029", "029_drop_community_media.sql"),
        new("030", "030_place_media.sql"),
        new("031", "031_region_zones_provinces.sql"),
        new("032", "032_badges.sql"),
        new("033", "033_drop_rating_snapshot.sql"),
        new("034", "034_preferences_place_types.sql"),
        new("035", "035_account_status_blocked.sql"),
        new("036", "036_leaderboard_rewards.sql"),
        new("037", "037_leaderboard_prizes.sql"),
        new("038", "038_drop_unused_quiz_bonus_and_media_quests.sql"),
    ];

    private const long LockKey = 7246321;

    /// <summary>Applies every file not applied yet (and repeatable files whose content changed).</summary>
    public static async Task ApplyAsync(NpgsqlDataSource db, CancellationToken ct = default)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using (var table = new NpgsqlCommand("""
            CREATE TABLE IF NOT EXISTS schema_migration (
                version    VARCHAR(20) PRIMARY KEY,
                checksum   CHAR(64)    NOT NULL,
                applied_at TIMESTAMPTZ NOT NULL DEFAULT now())
            """, connection))
            await table.ExecuteNonQueryAsync(ct);

        foreach (var migration in All)
        {
            var sql = Read(migration.File);
            var checksum = Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(sql)));
            await using var transaction = await connection.BeginTransactionAsync(ct);
            await using (var gate = new NpgsqlCommand($"SELECT pg_advisory_xact_lock({LockKey})", connection, transaction))
                await gate.ExecuteNonQueryAsync(ct);
            string? applied;
            await using (var check = new NpgsqlCommand("SELECT checksum FROM schema_migration WHERE version=$1", connection, transaction))
            {
                check.Parameters.AddWithValue(migration.Version);
                applied = await check.ExecuteScalarAsync(ct) as string;
            }
            if (applied is not null && (!migration.Repeatable || applied == checksum))
            {
                await transaction.RollbackAsync(ct);
                continue;
            }
            await using (var run = new NpgsqlCommand(sql, connection, transaction) { CommandTimeout = 300 })
                await run.ExecuteNonQueryAsync(ct);
            await using (var record = new NpgsqlCommand("""
                INSERT INTO schema_migration (version, checksum) VALUES ($1, $2)
                ON CONFLICT (version) DO UPDATE SET checksum = EXCLUDED.checksum, applied_at = now()
                """, connection, transaction))
            {
                record.Parameters.AddWithValue(migration.Version);
                record.Parameters.AddWithValue(checksum);
                await record.ExecuteNonQueryAsync(ct);
            }
            await transaction.CommitAsync(ct);
        }
    }

    /// <summary>
    /// Runs the given files again whatever schema_migration says. For checks that a migration is
    /// re-runnable (it must leave existing data and admin edits alone).
    /// </summary>
    public static async Task RunAsync(NpgsqlDataSource db, params string[] versions)
    {
        await using var connection = await db.OpenConnectionAsync();
        foreach (var migration in All.Where(m => versions.Contains(m.Version)))
        {
            await using var transaction = await connection.BeginTransactionAsync();
            await using (var gate = new NpgsqlCommand($"SELECT pg_advisory_xact_lock({LockKey})", connection, transaction))
                await gate.ExecuteNonQueryAsync();
            await using (var run = new NpgsqlCommand(Read(migration.File), connection, transaction) { CommandTimeout = 300 })
                await run.ExecuteNonQueryAsync();
            await transaction.CommitAsync();
        }
    }

    private static string Read(string file)
    {
        using var stream = typeof(DbMigrations).Assembly.GetManifestResourceStream("Bcv.Migrations." + file)
            ?? throw new InvalidOperationException($"Missing embedded migration {file}.");
        using var reader = new StreamReader(stream);
        return reader.ReadToEnd();
    }
}
