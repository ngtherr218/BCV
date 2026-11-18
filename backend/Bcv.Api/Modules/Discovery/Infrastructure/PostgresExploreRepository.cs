using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Shared.Infrastructure;
using Bcv.Api.Modules.StepLocation.Domain;
using Bcv.Api.Modules.QuestItem.Infrastructure;

namespace Bcv.Api.Modules.Discovery.Infrastructure;

public sealed class PostgresExploreRepository(NpgsqlDataSource db) : IExploreRepository
{

    public async Task<ExploreSnapshot> LoadAsync(long userId, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        return await LoadSnapshotAsync(connection, userId, ct);
    }

    public async Task<ExplorePlaceDetail> LoadPlaceAsync(long userId, long placeId, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        long id;
        string name, placeType, regionName;
        string? introduction, highlights;
        bool discovered;
        decimal distanceKm;
        decimal? rating;
        int? ratingCount;
        await using (var command = new NpgsqlCommand($"""
            SELECT p.place_id, p.name, pt.name,
                   EXISTS (SELECT 1 FROM user_discovered_place d
                           WHERE d.user_id = $1 AND d.place_id = p.place_id),
                   ({GeoSql.VirtualLocationToPlaceMeters} / 1000.0)::numeric, p.introduction, p.highlights, r.name, p.rating, p.rating_count,
                   v.user_id
            FROM place p
            JOIN place_type pt ON pt.place_type_id = p.place_type_id
            JOIN region r ON r.region_id = p.region_id
            LEFT JOIN virtual_location v ON v.user_id = $1
            WHERE p.place_id = $2 AND p.status = 'ACTIVE' AND r.status = 'ACTIVE'
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct))
                throw new ExploreException("PLACE_NOT_FOUND", "Địa danh này hiện không còn khả dụng.");
            if (reader.IsDBNull(10))
                throw new ExploreException("NO_STARTING_POINT", "Hãy chọn điểm xuất phát trước khi xem địa danh.");
            id = reader.GetInt64(0);
            name = reader.GetString(1);
            placeType = reader.GetString(2);
            discovered = reader.GetBoolean(3);
            distanceKm = reader.GetDecimal(4);
            introduction = !reader.IsDBNull(5) ? reader.GetString(5) : null;
            highlights = !reader.IsDBNull(6) ? reader.GetString(6) : null;
            regionName = reader.GetString(7);
            rating = reader.IsDBNull(8) ? null : reader.GetDecimal(8);
            ratingCount = reader.IsDBNull(9) ? null : reader.GetInt32(9);
        }

        var images = new List<string>();
        await using (var command = new NpgsqlCommand("""
            SELECT image_key FROM place_media
            WHERE media_type = 'IMAGE' AND place_id = $1 ORDER BY media_id
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct)) images.Add(reader.GetString(0));
        }

        // How many videos the place has, so a locked page can say what unlocking adds.
        int videoCount;
        await using (var count = new NpgsqlCommand("SELECT count(*)::int FROM place_media WHERE media_type='VIDEO' AND place_id=$1", connection))
        {
            count.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
            videoCount = (int)(await count.ExecuteScalarAsync(ct))!;
        }
        var videos=new List<ExplorePlaceVideo>();
        if(discovered)
        {
            await using var command=new NpgsqlCommand("SELECT media_id,duration_seconds,width,height FROM place_media WHERE media_type='VIDEO' AND place_id=$1 ORDER BY media_id",connection);
            command.Parameters.AddWithValue(placeId);
            await using var reader=await command.ExecuteReaderAsync(ct);
            while(await reader.ReadAsync(ct))videos.Add(new ExplorePlaceVideo(reader.GetInt64(0),$"/api/explore/places/{placeId}/videos/{reader.GetInt64(0)}",reader.GetDouble(1),reader.GetInt32(2),reader.GetInt32(3)));
        }
        // Check-in is offered at the destination of the journey being walked or where the virtual location
        // stands; a card not yet earned
        // (the place was unlocked by check-in or flight) can still be walked for.
        bool journeyDestination, cardPending, atVirtualLocation;
        await using (var command = new NpgsqlCommand(
            """
            SELECT EXISTS (SELECT 1 FROM journey WHERE user_id=$1 AND destination_place_id=$2 AND status='IN_PROGRESS'),
                   EXISTS (SELECT 1 FROM card c WHERE c.place_id=$2 AND c.status='ACTIVE'
                             AND NOT EXISTS (SELECT 1 FROM user_card uc WHERE uc.user_id=$1 AND uc.card_id=c.card_id)),
                   EXISTS (SELECT 1 FROM virtual_location v JOIN place p ON p.place_id=$2 WHERE v.user_id=$1
                             AND abs(v.latitude - p.latitude) < 0.0005 AND abs(v.longitude - p.longitude) < 0.0005)
                   -- The virtual location only moves when a journey ends: once 1,000 steps of a new journey are
                   -- walked the user has left this spot, so it no longer offers check-in (same rule in EngagementEndpoints).
                   AND NOT EXISTS (SELECT 1 FROM journey j WHERE j.user_id=$1 AND j.status='IN_PROGRESS' AND j.recorded_steps >= 1000)
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            await reader.ReadAsync(ct);
            journeyDestination = reader.GetBoolean(0);
            cardPending = reader.GetBoolean(1);
            atVirtualLocation = reader.GetBoolean(2);
        }
        // Preview before unlocking: cover photo, introduction and fun fact. The full gallery, videos,
        // the collectible card and every reward still need the walk (or a real check-in).
        return new ExplorePlaceDetail(id, name, placeType, images.FirstOrDefault(), discovered, distanceKm,
            distanceKm <= 0 ? 0 : StepConversion.RequiredSteps(distanceKm), introduction, highlights, regionName,
            discovered ? images : [], rating, ratingCount, videos, journeyDestination, cardPending, videoCount,
            journeyDestination || atVirtualLocation);
    }

    private static async Task<ExploreSnapshot> LoadSnapshotAsync(NpgsqlConnection connection,
        long userId, CancellationToken ct)
    {
        ExploreLocation? location = null;
        await using (var command = new NpgsqlCommand("""
            SELECT r.name, p.name, v.latitude, v.longitude
            FROM public.virtual_location v
            JOIN public.region r ON r.region_id = v.region_id
            LEFT JOIN public.place p ON p.place_id = v.nearest_place_id
            WHERE v.user_id = $1
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (await reader.ReadAsync(ct))
                location = new ExploreLocation(reader.GetString(0), reader.IsDBNull(1) ? null : reader.GetString(1),
                    reader.GetDecimal(2), reader.GetDecimal(3));
        }

        ExploreJourney? journey = null;
        await using (var command = new NpgsqlCommand("""
            SELECT j.journey_id, p.name, p.latitude, p.longitude,
                   j.required_steps, j.recorded_steps, j.status,
                   EXTRACT(EPOCH FROM j.started_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::bigint,
                   j.start_latitude, j.start_longitude, j.destination_place_id,
                   COALESCE((SELECT s.step_count FROM public.step_data s
                     WHERE s.user_id = j.user_id AND s.source_data_id = 'health-connect:journey:' || j.journey_id::text), 0),
                   start_place.name, c.card_id AS awarded_card_id, c.name AS awarded_card_name
            FROM public.journey j
            JOIN public.place p ON p.place_id = j.destination_place_id
            LEFT JOIN public.place start_place ON start_place.place_id = j.start_place_id
            LEFT JOIN public.card c ON c.place_id = p.place_id AND c.status = 'ACTIVE'
                AND j.status = 'COMPLETED'
                AND (EXISTS (SELECT 1 FROM public.user_card uc WHERE uc.user_id = j.user_id AND uc.card_id = c.card_id)
                     OR EXISTS (SELECT 1 FROM public.user_discovered_place d WHERE d.user_id = j.user_id AND d.place_id = p.place_id))
            WHERE j.user_id = $1 AND j.status IN ('IN_PROGRESS', 'COMPLETED')
            ORDER BY CASE WHEN j.status = 'IN_PROGRESS' THEN 0 ELSE 1 END,
                     j.started_at DESC, j.journey_id DESC
            LIMIT 1
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (await reader.ReadAsync(ct))
                journey = new ExploreJourney(reader.GetInt64(0), reader.GetString(1),
                    reader.GetDecimal(2), reader.GetDecimal(3), reader.GetInt32(4),
                    reader.GetInt32(5), reader.GetString(6), reader.GetInt64(7),
                    reader.GetDecimal(8), reader.GetDecimal(9), reader.GetInt64(10), reader.GetInt32(11),
                    StartPlaceName: reader.IsDBNull(12) ? null : reader.GetString(12),
                    AwardedCard: reader.IsDBNull(13) ? null : new ExploreCardReward(reader.GetInt64(13), reader.GetString(14)));
        }
        if (journey is { Status: "IN_PROGRESS" })
        {
            var boosts = await SpeedShoesStore.LoadBoostsAsync(connection, null, userId, journey.JourneyId, ct);
            var latest = boosts.MaxBy(b => b.From);
            if (latest is not null)
            {
                var remaining = Math.Max(0, latest.Range - Math.Max(0, journey.SourceSteps - latest.From));
                if (remaining > 0)
                    journey = journey with { ActiveItem = new ExploreItemEffect("SPEED_SHOES", "Giày tăng tốc",
                        latest.Multiplier, latest.Range, remaining) };
            }
        }
        var places = new List<ExploreMapPlace>();
        await using (var command = new NpgsqlCommand("""
            SELECT p.place_id, p.name, p.latitude, p.longitude,
                   (d.place_id IS NOT NULL) AS is_discovered,
                   COALESCE((SELECT name FROM public.place_type WHERE place_type_id=p.place_type_id),''),
                   (SELECT CASE WHEN image_key LIKE 'data:%' THEN '/api/places/' || p.place_id || '/thumbnail' ELSE image_key END
                        FROM public.place_media WHERE media_type='IMAGE' AND place_id=p.place_id ORDER BY media_id LIMIT 1)
            FROM public.place p
            LEFT JOIN public.user_discovered_place d
              ON d.place_id = p.place_id AND d.user_id = $1
            WHERE p.status = 'ACTIVE' AND EXISTS (SELECT 1 FROM public.region r WHERE r.region_id=p.region_id AND r.status='ACTIVE')
            ORDER BY p.place_id
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                places.Add(new ExploreMapPlace(reader.GetInt64(0), reader.GetString(1),
                    reader.GetDecimal(2), reader.GetDecimal(3), reader.GetBoolean(4), reader.GetString(5), reader.IsDBNull(6) ? null : reader.GetString(6)));
        }
        return new ExploreSnapshot(location, journey, places);
    }
}
