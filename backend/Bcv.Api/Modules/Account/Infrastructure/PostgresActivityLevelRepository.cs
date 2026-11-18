using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.Account.Application;

namespace Bcv.Api.Modules.Account.Infrastructure;

public sealed class PostgresActivityLevelRepository(NpgsqlDataSource db) : IActivityLevelRepository
{
    public async Task<ActivityLevelSnapshot> LoadAsync(long userId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT activity_level FROM public.user_preference WHERE user_id = $1
            """);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        var value = (string?)await command.ExecuteScalarAsync(ct);
        return new ActivityLevelSnapshot(value is "LOW" or "MEDIUM" or "HIGH" ? value : null);
    }

    public async Task SaveAsync(long userId, string level, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            UPDATE public.user_preference AS preference
            SET activity_level = $2, updated_at = CURRENT_TIMESTAMP
            WHERE preference.user_id = $1
              AND EXISTS (
                SELECT 1 FROM public.user_preference_place_type AS selected
                WHERE selected.user_preference_id = preference.user_preference_id
              )
            """);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = level, NpgsqlDbType = NpgsqlDbType.Varchar });
        if (await command.ExecuteNonQueryAsync(ct) == 0)
            throw new ActivityLevelException("INTERESTS_REQUIRED", "Hãy chọn nhóm địa danh trước khi lưu mức vận động.");
    }
}
