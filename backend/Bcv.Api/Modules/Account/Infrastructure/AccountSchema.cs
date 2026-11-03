using Npgsql;

namespace Bcv.Api.Modules.Account.Infrastructure;

public static class AccountSchema
{
    // Additive, idempotent upgrade for existing installations. Historical login times are unknown.
    public static async Task EnsureAsync(NpgsqlDataSource db, CancellationToken ct = default)
    {
        await using var command = db.CreateCommand("""
            ALTER TABLE public.app_user ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ NULL
            """);
        await command.ExecuteNonQueryAsync(ct);
    }
}
