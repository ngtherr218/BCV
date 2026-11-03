using Npgsql;
namespace Bcv.Api.Modules.PlaceCollection.Infrastructure;
public static class CollectionSchema
{
    public static async Task EnsureAsync(NpgsqlDataSource db)
    {
        await using var command = db.CreateCommand("""
            BEGIN;
            ALTER TABLE public.user_card ADD COLUMN IF NOT EXISTS seen_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;
            ALTER TABLE public.user_card ALTER COLUMN seen_at DROP DEFAULT;
            COMMIT;
            """);
        await command.ExecuteNonQueryAsync();
    }
}
