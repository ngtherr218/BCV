using Npgsql;

namespace Bcv.Api.Modules.Admin;

public static class PlaceTypeFeatureSchema
{
    public static async Task EnsureAsync(NpgsqlDataSource db)
    {
        await using var conn = await db.OpenConnectionAsync();
        await using var tx = await conn.BeginTransactionAsync();
        await using (var gate = new NpgsqlCommand("SELECT pg_advisory_xact_lock(7246318)", conn, tx))
            await gate.ExecuteNonQueryAsync();
        using var stream = typeof(PlaceTypeFeatureSchema).Assembly.GetManifestResourceStream("Bcv.PlaceTypeFeatureMigration.sql")!;
        using var reader = new StreamReader(stream);
        await using var migration = new NpgsqlCommand(await reader.ReadToEndAsync(), conn, tx);
        await migration.ExecuteNonQueryAsync();
        await tx.CommitAsync();
    }
}
