using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.PlaceCollection.Application;

namespace Bcv.Api.Modules.PlaceCollection.Infrastructure;

/// <summary>
/// "Bộ sưu tập" tab. A card counts as owned when the user holds it in USER_CARD or has discovered
/// its place, so places unlocked before cards were granted still show up.
/// </summary>
public sealed class CollectionRepository(NpgsqlDataSource db)
{
    private const string Owned = """
        (EXISTS (SELECT 1 FROM public.user_card uc WHERE uc.user_id = $1 AND uc.card_id = c.card_id)
         OR EXISTS (SELECT 1 FROM public.user_discovered_place d
                    WHERE d.user_id = $1 AND d.place_id = c.place_id))
        """;

    private const string NewCard = "EXISTS (SELECT 1 FROM public.user_card uc WHERE uc.user_id = $1 AND uc.card_id = c.card_id AND uc.seen_at IS NULL)";

    public async Task MarkSeenAsync(long userId, long cardId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("UPDATE public.user_card SET seen_at = CURRENT_TIMESTAMP WHERE user_id = $1 AND card_id = $2 AND seen_at IS NULL");
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = cardId, NpgsqlDbType = NpgsqlDbType.Bigint });
        await command.ExecuteNonQueryAsync(ct);
    }

    public async Task<CollectionOverview> LoadAsync(long userId, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        int ownedCards, totalCards, newCards;
        await using (var command = new NpgsqlCommand($"""
            SELECT count(*) FILTER (WHERE {Owned}), count(*), count(*) FILTER (WHERE {NewCard})
            FROM public.card c WHERE c.status = 'ACTIVE'
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            await reader.ReadAsync(ct);
            ownedCards = (int)reader.GetInt64(0);
            totalCards = (int)reader.GetInt64(1);
            newCards = (int)reader.GetInt64(2);
        }

        var collections = new List<CollectionSummary>();
        await using (var command = new NpgsqlCommand($"""
            SELECT col.collection_id, col.name, col.description,
                   count(c.card_id) FILTER (WHERE {Owned}),
                   count(c.card_id),
                   (array_agg(c.image_key ORDER BY cc.collection_card_id)
                        FILTER (WHERE {Owned} AND c.image_key IS NOT NULL))[1:4],
                   count(DISTINCT c.card_id) FILTER (WHERE {NewCard})
            FROM public.collection col
            JOIN public.collection_card cc ON cc.collection_id = col.collection_id
            JOIN public.card c ON c.card_id = cc.card_id AND c.status = 'ACTIVE'
            WHERE col.status = 'ACTIVE'
            GROUP BY col.collection_id, col.name, col.description
            ORDER BY col.collection_id
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
            {
                collections.Add(new CollectionSummary(reader.GetInt64(0), reader.GetString(1),
                    reader.IsDBNull(2) ? null : reader.GetString(2),
                    (int)reader.GetInt64(3), (int)reader.GetInt64(4),
                    reader.IsDBNull(5) ? [] : reader.GetFieldValue<string[]>(5), (int)reader.GetInt64(6)));
            }
        }
        return new CollectionOverview(ownedCards, totalCards, collections, newCards);
    }

    /// <summary>One collection with all its cards, owned ones first; null when it does not exist.</summary>
    public async Task<CollectionDetail?> LoadDetailAsync(long userId, long collectionId, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        string name;
        string? description;
        await using (var command = new NpgsqlCommand("""
            SELECT name, description FROM public.collection
            WHERE collection_id = $1 AND status = 'ACTIVE'
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = collectionId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct)) return null;
            name = reader.GetString(0);
            description = reader.IsDBNull(1) ? null : reader.GetString(1);
        }

        var cards = new List<CollectionCard>();
        await using (var command = new NpgsqlCommand($"""
            SELECT c.card_id, c.place_id, c.name, c.image_key, {Owned} AS owned, {NewCard} AS is_new
            FROM public.collection_card cc
            JOIN public.card c ON c.card_id = cc.card_id AND c.status = 'ACTIVE'
            WHERE cc.collection_id = $2
            ORDER BY is_new DESC, owned DESC, cc.collection_card_id
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = collectionId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
            {
                var owned = reader.GetBoolean(4);
                cards.Add(new CollectionCard(reader.GetInt64(0), reader.GetInt64(1), reader.GetString(2),
                    owned && !reader.IsDBNull(3) ? reader.GetString(3) : null, owned, reader.GetBoolean(5)));
            }
        }
        return new CollectionDetail(collectionId, name, description, cards.Count(c => c.Owned), cards.Count, cards);
    }
}
