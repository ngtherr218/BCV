using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.PlaceCollection.Infrastructure;
using Bcv.Api.Modules.QuestItem.Application;
using Bcv.Api.Modules.QuestItem.Domain;

namespace Bcv.Api.Modules.QuestItem.Infrastructure;

public sealed class ItemUseRepository(NpgsqlDataSource db)
{
    private const string FlightType = "FLIGHT_TICKET";

    private sealed record OwnedItem(long ItemId, string Name, bool Active, int Quantity, decimal? EffectValue, int? Range);

    private static async Task<OwnedItem> LoadItemAsync(NpgsqlConnection connection, NpgsqlTransaction? transaction,
        long userId, string type, bool forUpdate, CancellationToken ct, long? itemId = null)
    {
        await using var command = new NpgsqlCommand($"""
            SELECT i.item_id, i.name, i.status = 'ACTIVE', COALESCE(ui.quantity, 0), i.effect_value, i.effective_range
            FROM public.item i
            LEFT JOIN public.user_item ui ON ui.item_id = i.item_id AND ui.user_id = $1
            WHERE i.item_type = $2 AND ($3::bigint IS NULL OR i.item_id = $3)
            ORDER BY i.item_id LIMIT 1
            {(forUpdate ? "FOR UPDATE OF i" : "")}
            """, connection, transaction);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = type, NpgsqlDbType = NpgsqlDbType.Varchar });
        command.Parameters.Add(new NpgsqlParameter { Value = (object?)itemId ?? DBNull.Value, NpgsqlDbType = NpgsqlDbType.Bigint });
        await using var reader = await command.ExecuteReaderAsync(ct);
        if (!await reader.ReadAsync(ct)) throw new ItemUseException("ITEM_NOT_FOUND", "Không tìm thấy vật phẩm.");
        return new OwnedItem(reader.GetInt64(0), reader.GetString(1), reader.GetBoolean(2), reader.GetInt32(3),
            reader.IsDBNull(4) ? null : reader.GetDecimal(4), reader.IsDBNull(5) ? null : reader.GetInt32(5));
    }

    /// <summary>Takes one item; fails rather than going below zero when another request used it first.</summary>
    private static async Task ConsumeOneAsync(NpgsqlConnection connection, NpgsqlTransaction transaction,
        long userId, long itemId, CancellationToken ct)
    {
        await using var command = new NpgsqlCommand("""
            UPDATE public.user_item SET quantity = quantity - 1, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = $1 AND item_id = $2 AND quantity > 0
            """, connection, transaction);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = itemId, NpgsqlDbType = NpgsqlDbType.Bigint });
        if (await command.ExecuteNonQueryAsync(ct) == 0)
            throw new ItemUseException("NOT_OWNED", "Bạn không còn vật phẩm này.");
    }

    public async Task<SpeedShoesStatus> ShoesAsync(long userId, CancellationToken ct, long? itemId = null)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        return await ShoesAsync(connection, null, userId, ct, itemId);
    }

    private static async Task<SpeedShoesStatus> ShoesAsync(NpgsqlConnection connection, NpgsqlTransaction? transaction,
        long userId, CancellationToken ct, long? itemId = null)
    {
        var item = await LoadItemAsync(connection, transaction, userId, SpeedShoes.Type, false, ct, itemId);
        SpeedShoesJourney? journey = null;
        await using (var command = new NpgsqlCommand("""
            SELECT j.journey_id, p.name, j.recorded_steps, j.required_steps
            FROM public.journey j JOIN public.place p ON p.place_id = j.destination_place_id
            WHERE j.user_id = $1 AND j.status = 'IN_PROGRESS'
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (await reader.ReadAsync(ct))
                journey = new SpeedShoesJourney(reader.GetInt64(0), reader.GetString(1), reader.GetInt32(2), reader.GetInt32(3));
        }

        SpeedShoesEffect? effect = null;
        if (journey is not null)
        {
            var boosts = await SpeedShoesStore.LoadBoostsAsync(connection, transaction, userId, journey.JourneyId, ct);
            var latest = boosts.MaxBy(b => b.From);
            if (latest is not null)
            {
                var raw = await SpeedShoesStore.RawStepsAsync(connection, transaction, userId, journey.JourneyId, ct);
                effect = new SpeedShoesEffect(latest.Multiplier, latest.Range,
                    Math.Max(0, latest.Range - Math.Max(0, raw - latest.From)));
            }
        }
        return new SpeedShoesStatus(item.ItemId, item.Name, item.Active, item.Quantity,
            item.EffectValue ?? 1m, item.Range ?? 0, journey, effect);
    }

    public async Task<SpeedShoesStatus> UseShoesAsync(long userId, CancellationToken ct, long? itemId = null)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(ct);
        var item = await LoadItemAsync(connection, transaction, userId, SpeedShoes.Type, true, ct, itemId);
        if (!item.Active) throw new ItemUseException("ITEM_INACTIVE", "Vật phẩm đang tạm ngưng hoạt động.");
        if (item.Quantity <= 0) throw new ItemUseException("NOT_OWNED", "Bạn chưa sở hữu giày tăng tốc.");
        if (item.EffectValue is not > 1m || item.Range is not > 0)
            throw new ItemUseException("ITEM_MISCONFIGURED", "Vật phẩm chưa được cấu hình hiệu ứng.");

        var status = await ShoesAsync(connection, transaction, userId, ct, itemId);
        if (status.Journey is null)
            throw new ItemUseException("NO_ACTIVE_JOURNEY", "Cần có hành trình đang thực hiện để dùng giày.");
        if (status.Effect is { RemainingSteps: > 0 })
            throw new ItemUseException("EFFECT_ACTIVE", "Hiệu ứng tăng tốc trước vẫn đang áp dụng.");

        var raw = await SpeedShoesStore.RawStepsAsync(connection, transaction, userId, status.Journey.JourneyId, ct);
        await ConsumeOneAsync(connection, transaction, userId, item.ItemId, ct);
        await using (var command = new NpgsqlCommand("""
            INSERT INTO public.step_data (user_id, source_data_id, step_count, start_time, end_time)
            VALUES ($1, $2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, source_data_id) DO NOTHING
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<string>
            {
                TypedValue = SpeedShoes.SourceId(status.Journey.JourneyId, raw, item.EffectValue.Value, item.Range.Value),
                NpgsqlDbType = NpgsqlDbType.Varchar
            });
            if (await command.ExecuteNonQueryAsync(ct) == 0)
                throw new ItemUseException("EFFECT_ACTIVE", "Hiệu ứng tăng tốc trước vẫn đang áp dụng.");
        }
        var result = await ShoesAsync(connection, transaction, userId, ct, itemId);
        await transaction.CommitAsync(ct);
        return result;
    }

    public async Task<FlightTicketStatus> FlightAsync(long userId, CancellationToken ct, long? itemId = null)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        var item = await LoadItemAsync(connection, null, userId, FlightType, false, ct, itemId);
        bool hasJourney;
        await using (var command = new NpgsqlCommand("""
            SELECT EXISTS (SELECT 1 FROM public.journey WHERE user_id = $1 AND status = 'IN_PROGRESS')
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            hasJourney = (bool)(await command.ExecuteScalarAsync(ct))!;
        }
        string? currentName = null;
        long? currentId = null;
        await using (var command = new NpgsqlCommand("""
            SELECT r.region_id, r.name FROM public.virtual_location v
            JOIN public.region r ON r.region_id = v.region_id WHERE v.user_id = $1
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (await reader.ReadAsync(ct)) { currentId = reader.GetInt64(0); currentName = reader.GetString(1); }
        }
        var regions = new List<RegionOption>();
        await using (var command = new NpgsqlCommand("""
            SELECT r.region_id, r.name, COUNT(p.place_id)::int
            FROM public.region r
            JOIN public.place p ON p.region_id = r.region_id AND p.status = 'ACTIVE'
            WHERE r.status = 'ACTIVE' AND r.region_id IS DISTINCT FROM $1
            GROUP BY r.region_id, r.name
            ORDER BY r.name
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long?> { TypedValue = currentId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                regions.Add(new RegionOption(reader.GetInt64(0), reader.GetString(1), reader.GetInt32(2)));
        }
        return new FlightTicketStatus(item.ItemId, item.Name, item.Active, item.Quantity, hasJourney,
            currentName, currentId, regions);
    }

    public async Task<IReadOnlyList<FlightPlace>> RegionPlacesAsync(long userId, long regionId, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var command = new NpgsqlCommand("""
            SELECT p.place_id, p.name,
                   EXISTS (SELECT 1 FROM public.user_discovered_place d WHERE d.user_id = $1 AND d.place_id = p.place_id)
            FROM public.place p
            JOIN public.region r ON r.region_id = p.region_id AND r.status = 'ACTIVE'
            WHERE p.region_id = $2 AND p.status = 'ACTIVE'
            ORDER BY p.rating_count DESC NULLS LAST, p.name
            """, connection);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = regionId, NpgsqlDbType = NpgsqlDbType.Bigint });
        var places = new List<FlightPlace>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
            places.Add(new FlightPlace(reader.GetInt64(0), reader.GetString(1), reader.GetBoolean(2)));
        return places;
    }

    /// <summary>Moves the virtual location to the place and unlocks it (discovery, card, quest progress).</summary>
    public async Task<FlightResult> UseFlightAsync(long userId, long placeId, CancellationToken ct, long? itemId = null)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(ct);
        var item = await LoadItemAsync(connection, transaction, userId, FlightType, true, ct, itemId);
        if (!item.Active) throw new ItemUseException("ITEM_INACTIVE", "Vật phẩm đang tạm ngưng hoạt động.");
        if (item.Quantity <= 0) throw new ItemUseException("NOT_OWNED", "Bạn chưa sở hữu vé máy bay.");
        await using (var command = new NpgsqlCommand("""
            SELECT EXISTS (SELECT 1 FROM public.journey WHERE user_id = $1 AND status = 'IN_PROGRESS')
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            if ((bool)(await command.ExecuteScalarAsync(ct))!)
                throw new ItemUseException("JOURNEY_ACTIVE", "Hãy kết thúc hành trình hiện tại trước khi dùng vé.");
        }

        string region, place;
        await using (var command = new NpgsqlCommand("""
            UPDATE public.virtual_location v
            SET region_id = p.region_id, nearest_place_id = p.place_id,
                latitude = p.latitude, longitude = p.longitude, updated_at = CURRENT_TIMESTAMP
            FROM public.place p JOIN public.region r ON r.region_id = p.region_id
            WHERE v.user_id = $1 AND p.place_id = $2 AND p.status = 'ACTIVE' AND r.status = 'ACTIVE'
              AND p.region_id <> v.region_id
            RETURNING r.name, p.name
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct))
                throw new ItemUseException("INVALID_PLACE", "Địa danh này không hợp lệ cho vé máy bay.");
            region = reader.GetString(0);
            place = reader.GetString(1);
        }
        await ConsumeOneAsync(connection, transaction, userId, item.ItemId, ct);
        // Landing counts as arriving: the place is unlocked just as at the end of a journey.
        await PlaceUnlock.UnlockAsync(connection, transaction, userId, placeId, ct);
        await transaction.CommitAsync(ct);
        return new FlightResult(region, place, item.Quantity - 1);
    }
}
