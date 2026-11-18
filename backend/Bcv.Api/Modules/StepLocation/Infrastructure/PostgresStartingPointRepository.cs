using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.StepLocation.Application;

namespace Bcv.Api.Modules.StepLocation.Infrastructure;

public sealed class PostgresStartingPointRepository(NpgsqlDataSource db) : IStartingPointRepository
{
    private const string Coordinates = "ST_SetSRID(ST_MakePoint(p.longitude::double precision, p.latitude::double precision), 4326)::geography";

    public async Task<StartingPointSnapshot?> LoadSavedAsync(long userId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT v.virtual_location_id, v.nearest_place_id, r.name, p.name,
                   v.latitude, v.longitude
            FROM public.virtual_location v
            JOIN public.region r ON r.region_id = v.region_id
            LEFT JOIN public.place p ON p.place_id = v.nearest_place_id
            WHERE v.user_id = $1
            """);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        await using var reader = await command.ExecuteReaderAsync(ct);
        return await reader.ReadAsync(ct) ? new StartingPointSnapshot(
            reader.GetInt64(0), reader.IsDBNull(1) ? null : reader.GetInt64(1),
            reader.GetString(2), reader.IsDBNull(3) ? null : reader.GetString(3),
            reader.GetDecimal(4), reader.GetDecimal(5)) : null;
    }

    public async Task<StartingPointCandidate?> LoadSuggestedAsync(CancellationToken ct)
    {
        await using var command = db.CreateCommand($"""
            SELECT p.place_id, r.region_id, r.name, p.name, p.latitude, p.longitude
            FROM public.place p
            JOIN public.region r ON r.region_id = p.region_id
            WHERE r.status = 'ACTIVE' AND p.status = 'ACTIVE'
            ORDER BY CASE WHEN r.name = 'Hà Nội' THEN 0 ELSE 1 END,
                     ST_Distance({Coordinates},
                         ST_SetSRID(ST_MakePoint(105.8525, 21.028889), 4326)::geography),
                     p.place_id
            LIMIT 1
            """);
        await using var reader = await command.ExecuteReaderAsync(ct);
        return await reader.ReadAsync(ct) ? ReadCandidate(reader) : null;
    }

    public async Task<StartingPointCandidate?> ResolveCurrentAsync(decimal latitude, decimal longitude,
        CancellationToken ct)
    {
        await using var command = db.CreateCommand($"""
            SELECT p.place_id, r.region_id, r.name, p.name, p.latitude, p.longitude
            FROM public.place p
            JOIN public.region r ON r.region_id = p.region_id
            WHERE r.status = 'ACTIVE' AND p.status = 'ACTIVE'
              -- Anywhere in Vietnam (mainland, islands and the sea around them): the nearest place
              -- gives the region, however far it is. Only points clearly abroad are refused.
              AND $1::double precision BETWEEN 7.5 AND 23.5 AND $2::double precision BETWEEN 102.0 AND 117.5
            ORDER BY ST_Distance({Coordinates},
                ST_SetSRID(ST_MakePoint($2::double precision, $1::double precision), 4326)::geography),
                p.place_id
            LIMIT 1
            """);
        command.Parameters.Add(new NpgsqlParameter<decimal> { TypedValue = latitude, NpgsqlDbType = NpgsqlDbType.Numeric });
        command.Parameters.Add(new NpgsqlParameter<decimal> { TypedValue = longitude, NpgsqlDbType = NpgsqlDbType.Numeric });
        await using var reader = await command.ExecuteReaderAsync(ct);
        return await reader.ReadAsync(ct) ? ReadCandidate(reader) : null;
    }

    public async Task<StartingPointSnapshot> SaveIfMissingAsync(long userId, StartingPointCandidate point,
        decimal latitude, decimal longitude, CancellationToken ct)
    {
        await using (var command = db.CreateCommand("""
            INSERT INTO public.virtual_location
                (user_id, region_id, nearest_place_id, latitude, longitude)
            VALUES ($1, $2, $3, $4, $5)
            ON CONFLICT (user_id) DO NOTHING
            """))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = point.RegionId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = point.PlaceId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<decimal> { TypedValue = latitude, NpgsqlDbType = NpgsqlDbType.Numeric });
            command.Parameters.Add(new NpgsqlParameter<decimal> { TypedValue = longitude, NpgsqlDbType = NpgsqlDbType.Numeric });
            await command.ExecuteNonQueryAsync(ct);
        }

        return await LoadSavedAsync(userId, ct)
            ?? throw new InvalidOperationException("Saving the starting point did not create a row.");
    }

    private static StartingPointCandidate ReadCandidate(NpgsqlDataReader reader) => new(
        reader.GetInt64(0), reader.GetInt64(1), reader.GetString(2), reader.GetString(3),
        reader.GetDecimal(4), reader.GetDecimal(5));
}
