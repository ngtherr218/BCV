using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.Account.Application;

namespace Bcv.Api.Modules.Account.Infrastructure;

public sealed class PostgresInterestRepository(NpgsqlDataSource db) : IInterestRepository
{
    public async Task<InterestSnapshot> LoadAsync(long userId, CancellationToken ct)
    {
        var options = new List<PlaceTypeOption>();
        await using (var command = db.CreateCommand("SELECT interest_group_id, name FROM public.interest_group ORDER BY interest_group_id"))
        await using (var reader = await command.ExecuteReaderAsync(ct))
            while (await reader.ReadAsync(ct)) options.Add(new(reader.GetInt64(0), reader.GetString(1)));

        var selected = new List<long>();
        await using (var command = db.CreateCommand("""
            SELECT uppt.interest_group_id FROM public.user_preference up
            JOIN public.user_preference_interest uppt ON uppt.user_preference_id = up.user_preference_id
            WHERE up.user_id = $1 ORDER BY uppt.interest_group_id
            """))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct)) selected.Add(reader.GetInt64(0));
        }
        return new(options, selected);
    }

    public async Task<IReadOnlyList<long>> SaveAsync(long userId, IReadOnlyList<long> placeTypeIds, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(ct);
        var ids = placeTypeIds.ToArray();
        await using (var check = new NpgsqlCommand("SELECT count(*) FROM public.interest_group WHERE interest_group_id = ANY($1)", connection, transaction))
        {
            check.Parameters.Add(new NpgsqlParameter<long[]> { TypedValue = ids, NpgsqlDbType = NpgsqlDbType.Array | NpgsqlDbType.Bigint });
            var count = (long)(await check.ExecuteScalarAsync(ct) ?? 0L);
            if (count != ids.Length)
                throw new InterestException("UNKNOWN_PLACE_TYPE", "Có nhóm địa danh không còn tồn tại. Hãy tải lại danh sách.");
        }

        long preferenceId;
        await using (var upsert = new NpgsqlCommand("""
            INSERT INTO public.user_preference (user_id, activity_level)
            VALUES ($1, 'PENDING')
            ON CONFLICT (user_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP
            RETURNING user_preference_id
            """, connection, transaction))
        {
            upsert.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            preferenceId = (long)(await upsert.ExecuteScalarAsync(ct) ?? throw new InvalidOperationException("Missing preference id"));
        }
        await using (var clear = new NpgsqlCommand("DELETE FROM public.user_preference_interest WHERE user_preference_id = $1", connection, transaction))
        {
            clear.Parameters.Add(new NpgsqlParameter<long> { TypedValue = preferenceId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await clear.ExecuteNonQueryAsync(ct);
        }
        await using (var insert = new NpgsqlCommand("""
            INSERT INTO public.user_preference_interest (user_preference_id, interest_group_id)
            SELECT $1, unnest($2::bigint[])
            """, connection, transaction))
        {
            insert.Parameters.Add(new NpgsqlParameter<long> { TypedValue = preferenceId, NpgsqlDbType = NpgsqlDbType.Bigint });
            insert.Parameters.Add(new NpgsqlParameter<long[]> { TypedValue = ids, NpgsqlDbType = NpgsqlDbType.Array | NpgsqlDbType.Bigint });
            await insert.ExecuteNonQueryAsync(ct);
        }
        await transaction.CommitAsync(ct);
        return ids;
    }
}
