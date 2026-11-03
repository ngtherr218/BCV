using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.Journey.Application;
using Bcv.Api.Modules.PlaceCollection.Infrastructure;
using Bcv.Api.Modules.QuestItem.Domain;
using Bcv.Api.Modules.QuestItem.Infrastructure;
using Bcv.Api.Modules.Shared.Infrastructure;
using Bcv.Api.Modules.StatsNotify.Infrastructure;
using Bcv.Api.Modules.StepLocation.Domain;

namespace Bcv.Api.Modules.Journey.Infrastructure;

public sealed class PostgresJourneyRepository(NpgsqlDataSource db) : IJourneyRepository
{
    public async Task CreateJourneyAsync(long userId, CreateJourneyCommand request,
        CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(ct);
        // A user's virtual-location row serializes competing journey creation requests.
        long? startPlaceId;
        decimal startLatitude;
        decimal startLongitude;
        await using (var command = new NpgsqlCommand("""
            SELECT nearest_place_id, latitude, longitude FROM public.virtual_location
            WHERE user_id = $1 FOR UPDATE
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct))
                throw new JourneyException("NO_STARTING_POINT", "Hãy chọn điểm xuất phát trước.");
            startPlaceId = reader.IsDBNull(0) ? null : reader.GetInt64(0);
            startLatitude = reader.GetDecimal(1);
            startLongitude = reader.GetDecimal(2);
        }

        long? activeJourneyId = null;
        long? activePlaceId = null;
        await using (var command = new NpgsqlCommand("""
            SELECT journey_id, destination_place_id FROM public.journey
            WHERE user_id = $1 AND status = 'IN_PROGRESS' FOR UPDATE
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (await reader.ReadAsync(ct))
            {
                activeJourneyId = reader.GetInt64(0);
                activePlaceId = reader.GetInt64(1);
            }
        }
        if (activePlaceId == request.PlaceId)
        {
            await transaction.CommitAsync(ct);
            return;
        }
        if (activeJourneyId is not null && !request.ReplaceActive)
            throw new JourneyException("ACTIVE_JOURNEY_EXISTS", "Bạn đang có hành trình khác. Xác nhận để thay điểm đến.");

        decimal distanceKm;
        await using (var command = new NpgsqlCommand($"""
            SELECT ({GeoSql.VirtualLocationToPlaceMeters} / 1000.0)::numeric
            FROM public.place p JOIN public.virtual_location v ON v.user_id = $1
            WHERE p.place_id = $2 AND p.status = 'ACTIVE'
              AND EXISTS (SELECT 1 FROM public.region r WHERE r.region_id=p.region_id AND r.status='ACTIVE')
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = request.PlaceId, NpgsqlDbType = NpgsqlDbType.Bigint });
            var value = await command.ExecuteScalarAsync(ct);
            if (value is not decimal distance || distance <= 0)
                throw new JourneyException("INVALID_DESTINATION", "Điểm đến không khả dụng hoặc trùng vị trí ảo.");
            distanceKm = distance;
        }

        if (activeJourneyId is not null)
        {
            await using var abandon = new NpgsqlCommand("""
                UPDATE public.journey SET status = 'ABANDONED' WHERE journey_id = $1
                """, connection, transaction);
            abandon.Parameters.Add(new NpgsqlParameter<long> { TypedValue = activeJourneyId.Value, NpgsqlDbType = NpgsqlDbType.Bigint });
            await abandon.ExecuteNonQueryAsync(ct);
        }
        await using (var insert = new NpgsqlCommand("""
            INSERT INTO public.journey (user_id, start_place_id, start_latitude, start_longitude,
                destination_place_id, distance, required_steps, status)
            VALUES ($1, $2, $3, $4, $5, $6, $7, 'IN_PROGRESS')
            """, connection, transaction))
        {
            insert.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            insert.Parameters.Add(new NpgsqlParameter<long?> { TypedValue = startPlaceId, NpgsqlDbType = NpgsqlDbType.Bigint });
            insert.Parameters.Add(new NpgsqlParameter<decimal> { TypedValue = startLatitude, NpgsqlDbType = NpgsqlDbType.Numeric });
            insert.Parameters.Add(new NpgsqlParameter<decimal> { TypedValue = startLongitude, NpgsqlDbType = NpgsqlDbType.Numeric });
            insert.Parameters.Add(new NpgsqlParameter<long> { TypedValue = request.PlaceId, NpgsqlDbType = NpgsqlDbType.Bigint });
            insert.Parameters.Add(new NpgsqlParameter<decimal> { TypedValue = Math.Round(distanceKm, 2), NpgsqlDbType = NpgsqlDbType.Numeric });
            insert.Parameters.Add(new NpgsqlParameter<int> { TypedValue = StepConversion.RequiredSteps(distanceKm), NpgsqlDbType = NpgsqlDbType.Integer });
            await insert.ExecuteNonQueryAsync(ct);
        }
        await transaction.CommitAsync(ct);
    }

    public async Task SyncStepsAsync(long userId, SyncStepsCommand request, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(ct);
        DateTime startedAt;
        int recordedSteps;
        int requiredSteps;
        long destinationPlaceId;
        string status;
        await using (var command = new NpgsqlCommand("""
            SELECT started_at, recorded_steps, required_steps, destination_place_id, status
            FROM public.journey
            WHERE journey_id = $1 AND user_id = $2
            FOR UPDATE
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = request.JourneyId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct))
                throw new JourneyException("JOURNEY_NOT_FOUND", "Không tìm thấy hành trình của bạn.");
            startedAt = reader.GetDateTime(0);
            recordedSteps = reader.GetInt32(1);
            requiredSteps = reader.GetInt32(2);
            destinationPlaceId = reader.GetInt64(3);
            status = reader.GetString(4);
        }

        if (status != "IN_PROGRESS")
        {
            await transaction.CommitAsync(ct);
            return;
        }

        var sourceId = $"health-connect:journey:{request.JourneyId}";
        var previousSteps = 0;
        await using (var command = new NpgsqlCommand("""
            SELECT step_count FROM public.step_data
            WHERE user_id = $1 AND source_data_id = $2
            FOR UPDATE
            """, connection, transaction))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = sourceId, NpgsqlDbType = NpgsqlDbType.Varchar });
            var value = await command.ExecuteScalarAsync(ct);
            if (value is int count) previousSteps = count;
        }

        var cumulativeSteps = checked((int)request.CumulativeSteps);
        if (cumulativeSteps > previousSteps)
        {
            await using (var command = new NpgsqlCommand("""
                INSERT INTO public.step_data
                    (user_id, source_data_id, step_count, start_time, end_time)
                VALUES ($1, $2, $3, $4, GREATEST(CURRENT_TIMESTAMP, $4))
                ON CONFLICT (user_id, source_data_id) DO UPDATE
                    SET step_count = EXCLUDED.step_count,
                        end_time = EXCLUDED.end_time,
                        synced_at = CURRENT_TIMESTAMP
                """, connection, transaction))
            {
                command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
                command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = sourceId, NpgsqlDbType = NpgsqlDbType.Varchar });
                command.Parameters.Add(new NpgsqlParameter<int> { TypedValue = cumulativeSteps, NpgsqlDbType = NpgsqlDbType.Integer });
                command.Parameters.Add(new NpgsqlParameter<DateTime> { TypedValue = startedAt, NpgsqlDbType = NpgsqlDbType.Timestamp });
                await command.ExecuteNonQueryAsync(ct);
            }

            // Only the steps new since the last sync count toward today's and this week's quests.
            await QuestProgress.RecordAsync(connection, transaction, userId, QuestProgress.StepCount,
                cumulativeSteps - previousSteps, ct);

            // Raw steps count once; an active speed-shoe boost adds its extra share to this journey only.
            var boosts = await SpeedShoesStore.LoadBoostsAsync(connection, transaction, userId, request.JourneyId, ct);
            var bonus = SpeedShoes.Bonus(boosts, previousSteps, cumulativeSteps);
            var newRecorded = (int)Math.Min(requiredSteps, (long)recordedSteps + cumulativeSteps - previousSteps + bonus);
            var completed = newRecorded >= requiredSteps;
            await using (var command = new NpgsqlCommand("""
                UPDATE public.journey
                SET recorded_steps = $1,
                    status = CASE WHEN $2 THEN 'COMPLETED' ELSE 'IN_PROGRESS' END,
                    completed_at = CASE WHEN $2 THEN CURRENT_TIMESTAMP ELSE NULL END
                WHERE journey_id = $3
                """, connection, transaction))
            {
                command.Parameters.Add(new NpgsqlParameter<int> { TypedValue = newRecorded, NpgsqlDbType = NpgsqlDbType.Integer });
                command.Parameters.Add(new NpgsqlParameter<bool> { TypedValue = completed, NpgsqlDbType = NpgsqlDbType.Boolean });
                command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = request.JourneyId, NpgsqlDbType = NpgsqlDbType.Bigint });
                await command.ExecuteNonQueryAsync(ct);
            }

            if (completed)
            {
                await using (var command = new NpgsqlCommand("""
                    UPDATE public.virtual_location v
                    SET region_id = p.region_id, nearest_place_id = p.place_id,
                        latitude = p.latitude, longitude = p.longitude,
                        updated_at = CURRENT_TIMESTAMP
                    FROM public.place p
                    WHERE v.user_id = $1 AND p.place_id = $2
                    """, connection, transaction))
                {
                    command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
                    command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = destinationPlaceId, NpgsqlDbType = NpgsqlDbType.Bigint });
                    await command.ExecuteNonQueryAsync(ct);
                }
                await PlaceUnlock.UnlockAsync(connection, transaction, userId, destinationPlaceId, ct);

                await using (var name = new NpgsqlCommand("SELECT name FROM public.place WHERE place_id = $1",
                    connection, transaction))
                {
                    name.Parameters.Add(new NpgsqlParameter<long> { TypedValue = destinationPlaceId, NpgsqlDbType = NpgsqlDbType.Bigint });
                    var placeName = (string?)await name.ExecuteScalarAsync(ct) ?? "điểm đến";
                    await using var ownedCard = new NpgsqlCommand("""
                        SELECT EXISTS (SELECT 1 FROM public.user_card uc
                            JOIN public.card c ON c.card_id = uc.card_id
                            WHERE uc.user_id = $1 AND c.place_id = $2)
                        """, connection, transaction);
                    ownedCard.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
                    ownedCard.Parameters.Add(new NpgsqlParameter<long> { TypedValue = destinationPlaceId, NpgsqlDbType = NpgsqlDbType.Bigint });
                    var hasCard = (bool)(await ownedCard.ExecuteScalarAsync(ct))!;
                    var celebration = $"Bạn đã hoàn thành hành trình và khám phá địa điểm {placeName}!";
                    if (hasCard) celebration += $"\n\nThẻ địa danh {placeName} đã có trong bộ sưu tập của bạn.";
                    await Notifications.AddAsync(connection, transaction, userId, Notifications.JourneyCompleted,
                        "Hành trình hoàn thành", celebration, ct);
                }

                await QuestProgress.RecordAsync(connection, transaction, userId, QuestProgress.CompleteJourney, 1, ct);
            }
        }

        await transaction.CommitAsync(ct);
    }

    public async Task<IReadOnlyList<JourneyHistoryEntry>> HistoryAsync(long userId, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var command = new NpgsqlCommand("""
            SELECT j.journey_id, j.status, COALESCE(s.name, 'Vị trí ảo'), d.place_id, d.name, j.started_at, j.completed_at
            FROM public.journey j
            JOIN public.place d ON d.place_id = j.destination_place_id
            LEFT JOIN public.place s ON s.place_id = j.start_place_id
            WHERE j.user_id = $1
            ORDER BY j.started_at DESC, j.journey_id DESC
            """, connection);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        var entries = new List<JourneyHistoryEntry>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
            entries.Add(new JourneyHistoryEntry(reader.GetInt64(0), reader.GetString(1), reader.GetString(2),
                reader.GetInt64(3), reader.GetString(4), reader.GetDateTime(5), reader.IsDBNull(6) ? null : reader.GetDateTime(6)));
        return entries;
    }
}
