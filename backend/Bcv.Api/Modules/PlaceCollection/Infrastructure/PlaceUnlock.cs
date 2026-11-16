using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.QuestItem.Infrastructure;

namespace Bcv.Api.Modules.PlaceCollection.Infrastructure;

/// <summary>
/// Arriving at a place, by finishing a journey or by flight ticket: the place is discovered (counting
/// toward DISCOVER_PLACE quests the first time) and its collectible card, if any, is earned.
/// </summary>
public static class PlaceUnlock
{
    public static async Task<int> UnlockAsync(NpgsqlConnection connection, NpgsqlTransaction transaction,
        long userId, long placeId, CancellationToken ct, bool walking = false, long? journeyId = null)
    {
        var firstDiscovery=false;
        await using (var discovery = new NpgsqlCommand("""
            INSERT INTO public.user_discovered_place (user_id, place_id)
            VALUES ($1, $2)
            ON CONFLICT (user_id, place_id) DO NOTHING
            """, connection, transaction))
        {
            discovery.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            discovery.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
            firstDiscovery=await discovery.ExecuteNonQueryAsync(ct)>0;
            if (walking && !firstDiscovery)
                throw new Bcv.Api.Modules.Journey.Application.JourneyException("PLACE_ALREADY_DISCOVERED",
                    "Bạn đã khám phá địa danh này. Không thể hoàn thành thêm hành trình đến địa danh đã khám phá.");
            if (firstDiscovery)
                await QuestProgress.RecordAsync(connection, transaction, userId, QuestProgress.DiscoverPlace, 1, ct, awardScore:walking);
        }
        var rankingPoints=0;
        if(walking)
            rankingPoints=await Bcv.Api.Modules.StatsNotify.Infrastructure.ExplorerScoreEvents.WalkingAsync(connection,transaction,userId,placeId,firstDiscovery,
                journeyId ?? throw new InvalidOperationException("A walking score requires its completed journey ID."),ct);

        await using var card = new NpgsqlCommand("""
            INSERT INTO public.user_card (user_id, card_id)
            SELECT $1, c.card_id FROM public.card c
            WHERE c.place_id = $2 AND c.status = 'ACTIVE'
            ON CONFLICT (user_id, card_id) DO NOTHING
            """, connection, transaction);
        card.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        card.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
        if (await card.ExecuteNonQueryAsync(ct) > 0)
        {
            await using var name = new NpgsqlCommand("SELECT name FROM public.place WHERE place_id = $1", connection, transaction);
            name.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
            var placeName = (string?)await name.ExecuteScalarAsync(ct) ?? "địa danh";
            await Bcv.Api.Modules.StatsNotify.Infrastructure.Notifications.AddAsync(connection, transaction, userId,
                "CARD_RECEIVED", "Đã nhận thẻ mới", $"Thẻ {placeName} đã được thêm vào bộ sưu tập của bạn.", ct);
        }
        return rankingPoints;
    }
}
