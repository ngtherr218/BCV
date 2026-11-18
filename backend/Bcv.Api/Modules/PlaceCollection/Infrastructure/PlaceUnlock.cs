using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.QuestItem.Infrastructure;

namespace Bcv.Api.Modules.PlaceCollection.Infrastructure;

/// <summary>
/// Arriving at a place. Two kinds of arrival, each its own operation:
/// <list type="bullet">
/// <item><see cref="DiscoverAsync"/>: a real check-in unlocks the place, but its card still has to be walked for.</item>
/// <item><see cref="ArriveWithCardAsync"/>: finishing a journey there or landing by flight ticket unlocks the
/// place and earns its collectible card.</item>
/// </list>
/// </summary>
public static class PlaceUnlock
{
    /// <summary>
    /// Marks the place discovered (counting toward DISCOVER_PLACE quests the first time).
    /// Returns true when it was not discovered before.
    /// </summary>
    public static async Task<bool> DiscoverAsync(NpgsqlConnection connection, NpgsqlTransaction transaction,
        long userId, long placeId, CancellationToken ct)
    {
        await using var discovery = new NpgsqlCommand("""
            INSERT INTO public.user_discovered_place (user_id, place_id)
            VALUES ($1, $2)
            ON CONFLICT (user_id, place_id) DO NOTHING
            """, connection, transaction);
        AddIds(discovery, userId, placeId);
        if (await discovery.ExecuteNonQueryAsync(ct) == 0) return false;
        await QuestProgress.RecordAsync(connection, transaction, userId, QuestProgress.DiscoverPlace, 1, ct);
        return true;
    }

    /// <summary>Discovers the place and gives the user its active card, with a notification when it is new.</summary>
    public static async Task ArriveWithCardAsync(NpgsqlConnection connection, NpgsqlTransaction transaction,
        long userId, long placeId, CancellationToken ct)
    {
        await DiscoverAsync(connection, transaction, userId, placeId, ct);
        await using var card = new NpgsqlCommand("""
            INSERT INTO public.user_card (user_id, card_id)
            SELECT $1, c.card_id FROM public.card c
            WHERE c.place_id = $2 AND c.status = 'ACTIVE'
            ON CONFLICT (user_id, card_id) DO NOTHING
            """, connection, transaction);
        AddIds(card, userId, placeId);
        if (await card.ExecuteNonQueryAsync(ct) == 0) return;
        await using var name = new NpgsqlCommand("SELECT name FROM public.place WHERE place_id = $1", connection, transaction);
        name.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
        var placeName = (string?)await name.ExecuteScalarAsync(ct) ?? "địa danh";
        await Bcv.Api.Modules.StatsNotify.Infrastructure.Notifications.AddAsync(connection, transaction, userId,
            "CARD_RECEIVED", "Đã nhận thẻ mới", $"Thẻ {placeName} đã được thêm vào bộ sưu tập của bạn.", ct);
    }

    private static void AddIds(NpgsqlCommand command, long userId, long placeId)
    {
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = placeId, NpgsqlDbType = NpgsqlDbType.Bigint });
    }
}
