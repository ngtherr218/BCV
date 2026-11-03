using Npgsql;
namespace Bcv.Api.Modules.StatsNotify.Infrastructure;
public static class NotificationSchema
{
    public static async Task EnsureAsync(NpgsqlDataSource db)
    {
        await using var command = db.CreateCommand("ALTER TABLE public.notification ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMPTZ NULL;");
        await command.ExecuteNonQueryAsync();
    }
}
