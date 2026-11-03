using Npgsql;

namespace Bcv.Api.Modules.Discovery.Infrastructure;

public static class RecommendationSchema
{
    public static async Task EnsureAsync(NpgsqlDataSource db)
    {
        await using var connection = await db.OpenConnectionAsync();
        await using var transaction = await connection.BeginTransactionAsync();
        await using (var gate = new NpgsqlCommand("SELECT pg_advisory_xact_lock(7246318)", connection, transaction))
            await gate.ExecuteNonQueryAsync();
        await using var check = new NpgsqlCommand("""
            SELECT to_regclass('public.interest_group') IS NOT NULL
               AND to_regclass('public.user_preference_interest') IS NOT NULL
               AND to_regclass('public.recommendation_rating_snapshot') IS NOT NULL
               AND EXISTS(SELECT 1 FROM information_schema.columns
                          WHERE table_schema='public' AND table_name='recommendation_config' AND column_name='settings')
            """, connection, transaction);
        if (!(bool)(await check.ExecuteScalarAsync())!)
        {
            using var stream = typeof(RecommendationSchema).Assembly.GetManifestResourceStream("Bcv.RecommendationMigration.sql")
                ?? throw new InvalidOperationException("Missing recommendation migration resource.");
            using var reader = new StreamReader(stream);
            var sql = (await reader.ReadToEndAsync()).Replace("BEGIN;", "").Replace("COMMIT;", "");
            await using var migration = new NpgsqlCommand(sql, connection, transaction) { CommandTimeout = 120 };
            await migration.ExecuteNonQueryAsync();
        }
        await transaction.CommitAsync();
    }
}
