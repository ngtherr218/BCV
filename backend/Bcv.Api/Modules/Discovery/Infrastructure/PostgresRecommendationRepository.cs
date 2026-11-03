using System.Text.Json;
using System.Data;
using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Discovery.Domain;
using Bcv.Api.Modules.Shared.Infrastructure;

namespace Bcv.Api.Modules.Discovery.Infrastructure;

public sealed class PostgresRecommendationRepository(NpgsqlDataSource db) : IRecommendationRepository
{
    public async Task<RecommendationInput?> LoadAsync(long userId, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(IsolationLevel.RepeatableRead, ct);

        string regionName;
        decimal latitude, longitude;
        await using (var command = new NpgsqlCommand("""
            SELECT r.name, v.latitude, v.longitude FROM public.virtual_location v
            JOIN public.region r ON r.region_id = v.region_id
            WHERE v.user_id = $1
            """, connection))
        {
            command.Parameters.Add(UserId(userId));
            await using var reader = await command.ExecuteReaderAsync(ct);
            if (!await reader.ReadAsync(ct)) return null;
            regionName = reader.GetString(0);
            latitude = reader.GetDecimal(1);
            longitude = reader.GetDecimal(2);
        }

        // Only an activated configuration may affect user-facing recommendations.
        RecommendationConfig? config = null;
        await using (var command = new NpgsqlCommand("""
            SELECT recommendation_config_id, version, distance_limit, settings::text
            FROM public.recommendation_config
            WHERE status = 'ACTIVE'
            ORDER BY updated_at DESC, recommendation_config_id DESC
            LIMIT 1
            """, connection))
        await using (var reader = await command.ExecuteReaderAsync(ct))
        {
            if (await reader.ReadAsync(ct))
                config = new RecommendationConfig(reader.GetInt64(0), reader.GetString(1), reader.GetDecimal(2),
                    new Dictionary<string, decimal>(), JsonSerializer.Deserialize<RecommendationSettings>(reader.GetString(3), new JsonSerializerOptions(JsonSerializerDefaults.Web)));
        }
        if (config is null) throw new ExploreException("NO_RECOMMENDATION_CONFIG", "Chưa có cấu hình gợi ý.");

        if (config.Settings is null || !config.Settings.IsValid()) throw new ExploreException("INVALID_RECOMMENDATION_CONFIG", "Cấu hình gợi ý chưa hợp lệ.");

        string? activityLevel = null;
        var preferences = new List<PreferenceGroup>();
        await using (var command = new NpgsqlCommand("""
            SELECT up.activity_level, pt.interest_group_id, pt.name
            FROM public.user_preference up
            LEFT JOIN public.user_preference_interest upt ON upt.user_preference_id = up.user_preference_id
            LEFT JOIN public.interest_group pt ON pt.interest_group_id = upt.interest_group_id
            WHERE up.user_id = $1
            ORDER BY pt.interest_group_id
            """, connection))
        {
            command.Parameters.Add(UserId(userId));
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
            {
                activityLevel = reader.GetString(0);
                if (!reader.IsDBNull(1) && config.Settings!.InterestFeatures.TryGetValue(reader.GetInt64(1), out var codes))
                    preferences.Add(new PreferenceGroup(reader.GetString(2), codes.ToHashSet()));
            }
        }

        var weights = new Dictionary<string, decimal>();
        await using (var command = new NpgsqlCommand("""
            SELECT c.code, w.weight_value
            FROM public.recommendation_weight w
            JOIN public.recommendation_criterion c ON c.criterion_id = w.criterion_id
            WHERE w.recommendation_config_id = $1
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = config.ConfigId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct)) weights[reader.GetString(0)] = reader.GetDecimal(1);
        }

        var places = new List<RecommendationPlace>();
        await using (var command = new NpgsqlCommand($"""
            SELECT p.place_id, p.name, r.name,
                   (SELECT i.image_key FROM public.place_image i WHERE i.place_id = p.place_id
                    ORDER BY i.place_image_id LIMIT 1),
                   ARRAY(SELECT c.code FROM public.place_feature_value fv
                         JOIN public.recommendation_criterion c ON c.criterion_id = fv.criterion_id
                         WHERE fv.place_feature_id = pf.place_feature_id AND fv.feature_value = 1),
                   pf.cluster_number, snapshot.rating, snapshot.rating_count,
                   ({GeoSql.VirtualLocationToPlaceMeters})::numeric,
                   EXISTS (SELECT 1 FROM public.user_discovered_place d
                           WHERE d.user_id = $1 AND d.place_id = p.place_id)
            FROM public.place p
            JOIN public.region r ON r.region_id = p.region_id
            JOIN public.place_feature pf ON pf.place_id = p.place_id AND pf.recommendation_config_id = $2
            JOIN public.recommendation_rating_snapshot snapshot ON snapshot.place_id=p.place_id AND snapshot.recommendation_config_id=pf.recommendation_config_id
            JOIN public.virtual_location v ON v.user_id = $1
            WHERE p.status = 'ACTIVE' AND r.status = 'ACTIVE'
            """, connection))
        {
            command.Parameters.Add(UserId(userId));
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = config.ConfigId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await using var reader = await command.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
                places.Add(new RecommendationPlace(reader.GetInt64(0), reader.GetString(1), reader.GetString(2),
                    reader.IsDBNull(3) ? null : reader.GetString(3),
                    reader.GetFieldValue<string[]>(4).ToHashSet(),
                    reader.IsDBNull(5) ? null : reader.GetInt32(5),
                    reader.IsDBNull(6) ? null : reader.GetDecimal(6),
                    reader.IsDBNull(7) ? null : reader.GetInt32(7),
                    reader.GetDecimal(8), reader.GetBoolean(9)));
        }

        return new RecommendationInput(regionName, latitude, longitude, activityLevel, preferences,
            config with { Weights = weights }, places);
    }

    private static NpgsqlParameter<long> UserId(long userId) =>
        new() { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint };
}
