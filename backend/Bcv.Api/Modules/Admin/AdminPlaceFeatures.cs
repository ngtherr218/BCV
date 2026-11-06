using System.Text.Json;
using Bcv.Api.Modules.Discovery.Domain;
using Npgsql;

namespace Bcv.Api.Modules.Admin;

internal static class AdminPlaceFeatures
{
    internal static HashSet<string> ReadCodes(JsonElement body)
    {
        if (!body.TryGetProperty("feature_codes", out var array) || array.ValueKind != JsonValueKind.Array || array.GetArrayLength() > 35)
            throw new AdminValidation("Hãy chọn đặc trưng nội dung, tối đa 35 mục.");
        var codes = new HashSet<string>(StringComparer.Ordinal);
        foreach (var item in array.EnumerateArray())
        {
            if (item.ValueKind != JsonValueKind.String || !PlaceFeatureClassifier.Features.Contains(item.GetString()!))
                throw new AdminValidation("Đặc trưng nội dung không được hỗ trợ.");
            codes.Add(item.GetString()!);
        }
        if (codes.Count == 0) throw new AdminValidation("Cần ít nhất một đặc trưng nội dung.");
        return codes;
    }

    public static async Task Save(NpgsqlConnection connection, NpgsqlTransaction transaction, long placeId, JsonElement body, CancellationToken ct)
    {
        var source = body.TryGetProperty("feature_source", out var mode)
            ? mode.ValueKind == JsonValueKind.String ? mode.GetString() : null
            : body.TryGetProperty("feature_codes", out _) ? "CUSTOM" : "TYPE";
        if (source is not ("TYPE" or "CUSTOM")) throw new AdminValidation("Chế độ đặc trưng không hợp lệ.");
        HashSet<string> selected;
        if (source == "CUSTOM") selected = ReadCodes(body);
        else
        {
            selected = new(StringComparer.Ordinal);
            await using var defaults = new NpgsqlCommand("SELECT f.code FROM place_type_feature f JOIN place p ON p.place_type_id=f.place_type_id WHERE p.place_id=$1", connection, transaction);
            defaults.Parameters.AddWithValue(placeId);
            await using var reader = await defaults.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct)) selected.Add(reader.GetString(0));
            if (selected.Count == 0) throw new AdminValidation("Loại địa danh chưa cấu hình đặc trưng. Hãy cấu hình tại Loại địa danh hoặc bật tùy chỉnh.");
        }
        await using (var update = new NpgsqlCommand("UPDATE place SET feature_source=$2 WHERE place_id=$1", connection, transaction))
        {
            update.Parameters.AddWithValue(placeId); update.Parameters.AddWithValue(source!);
            await update.ExecuteNonQueryAsync(ct);
        }
        await SaveBatch(connection, transaction, new Dictionary<long, HashSet<string>> { [placeId] = selected }, ct);
    }

    public static async Task SaveType(NpgsqlConnection connection, NpgsqlTransaction transaction, long typeId, JsonElement body, CancellationToken ct)
    {
        var selected = ReadCodes(body);
        var previous = new HashSet<string>(StringComparer.Ordinal);
        await using (var current = new NpgsqlCommand("SELECT code FROM place_type_feature WHERE place_type_id=$1", connection, transaction))
        {
            current.Parameters.AddWithValue(typeId);
            await using var reader = await current.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct)) previous.Add(reader.GetString(0));
        }
        if (previous.SetEquals(selected)) return;
        await using (var clear = new NpgsqlCommand("DELETE FROM place_type_feature WHERE place_type_id=$1", connection, transaction))
        {
            clear.Parameters.AddWithValue(typeId); await clear.ExecuteNonQueryAsync(ct);
        }
        await using (var insert = new NpgsqlCommand("INSERT INTO place_type_feature SELECT $1,unnest($2::text[])", connection, transaction))
        {
            insert.Parameters.AddWithValue(typeId); insert.Parameters.AddWithValue(selected.ToArray());
            await insert.ExecuteNonQueryAsync(ct);
        }
        var updates = new Dictionary<long, HashSet<string>>();
        await using (var places = new NpgsqlCommand("SELECT place_id FROM place WHERE place_type_id=$1 AND feature_source='TYPE' ORDER BY place_id FOR UPDATE", connection, transaction))
        {
            places.Parameters.AddWithValue(typeId);
            await using var reader = await places.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct)) updates.Add(reader.GetInt64(0), selected);
        }
        if (updates.Count > 0) await SaveBatch(connection, transaction, updates, ct);
    }

    private static async Task SaveBatch(NpgsqlConnection connection, NpgsqlTransaction transaction, IReadOnlyDictionary<long, HashSet<string>> updates, CancellationToken ct)
    {
        await using (var gate = new NpgsqlCommand("SELECT pg_advisory_xact_lock(7246318)", connection, transaction))
            await gate.ExecuteNonQueryAsync(ct);
        long configId;
        int clusterCount;
        await using (var config = new NpgsqlCommand("""
            SELECT recommendation_config_id, cluster_count FROM recommendation_config
            WHERE status = 'ACTIVE'
            ORDER BY updated_at DESC, recommendation_config_id DESC
            LIMIT 1 FOR SHARE
            """, connection, transaction))
        await using (var reader = await config.ExecuteReaderAsync(ct))
        {
            if (!await reader.ReadAsync(ct)) throw new AdminValidation("Chưa có cấu hình gợi ý. Hãy tạo cấu hình trước khi lưu địa danh.");
            configId = reader.GetInt64(0);
            clusterCount = reader.GetInt32(1);
        }
        if (clusterCount != 15) throw new AdminValidation("Mô hình hiện tại hỗ trợ 15 cụm. Cần cập nhật mô hình cho cấu hình này.");
        // Do not silently create a partial feature vector if reference data is missing.
        await using (var criteria = new NpgsqlCommand("SELECT count(*) FROM recommendation_criterion WHERE code=ANY($1)", connection, transaction))
        {
            criteria.Parameters.AddWithValue(PlaceFeatureClassifier.Features.ToArray());
            if ((long)(await criteria.ExecuteScalarAsync(ct))! != PlaceFeatureClassifier.Features.Count)
                throw new AdminValidation("Thiếu dữ liệu 35 đặc trưng mô hình trong cơ sở dữ liệu.");
        }
        // Admin edits get their own revision, preserving the prior model snapshot for rollback.
        var previousId = configId;
        var version = "admin-" + Guid.NewGuid().ToString("N");
        await using (var revision = new NpgsqlCommand("""
            INSERT INTO recommendation_config(version,distance_limit,cluster_count,status,settings)
            SELECT $2,distance_limit,cluster_count,'DRAFT',
              jsonb_set(settings,'{datasetVersion}',to_jsonb($2::text))
            FROM recommendation_config WHERE recommendation_config_id=$1
            RETURNING recommendation_config_id
            """, connection, transaction))
        {
            revision.Parameters.AddWithValue(previousId); revision.Parameters.AddWithValue(version);
            configId = (long)(await revision.ExecuteScalarAsync(ct))!;
        }
        var copies = new[] {
            "INSERT INTO recommendation_weight SELECT $1,criterion_id,weight_value FROM recommendation_weight WHERE recommendation_config_id=$2",
            "INSERT INTO place_feature(place_id,recommendation_config_id,cluster_number) SELECT place_id,$1,cluster_number FROM place_feature WHERE recommendation_config_id=$2",
            """
            INSERT INTO place_feature_value
            SELECT n.place_feature_id,v.criterion_id,v.feature_value FROM place_feature old
            JOIN place_feature_value v ON v.place_feature_id=old.place_feature_id
            JOIN place_feature n ON n.place_id=old.place_id AND n.recommendation_config_id=$1
            WHERE old.recommendation_config_id=$2
            """,
            """
            INSERT INTO recommendation_rating_snapshot
            SELECT $1,place_id,rating,rating_count FROM recommendation_rating_snapshot WHERE recommendation_config_id=$2
            """
        };
        foreach (var sql in copies)
        {
            await using var copy = new NpgsqlCommand(sql, connection, transaction);
            copy.Parameters.AddWithValue(configId); copy.Parameters.AddWithValue(previousId);
            await copy.ExecuteNonQueryAsync(ct);
        }
        foreach (var (placeId, selected) in updates)
        {
            long featureId;
            await using (var upsert = new NpgsqlCommand("""
                INSERT INTO place_feature(place_id,recommendation_config_id,cluster_number)
                VALUES($1,$2,$3) ON CONFLICT(place_id,recommendation_config_id)
                DO UPDATE SET cluster_number=EXCLUDED.cluster_number,updated_at=CURRENT_TIMESTAMP
                RETURNING place_feature_id
                """, connection, transaction))
            {
                upsert.Parameters.AddWithValue(placeId); upsert.Parameters.AddWithValue(configId);
                upsert.Parameters.AddWithValue(PlaceFeatureClassifier.Classify(selected));
                featureId = (long)(await upsert.ExecuteScalarAsync(ct))!;
            }
            await using var values = new NpgsqlCommand("""
                INSERT INTO place_feature_value(place_feature_id,criterion_id,feature_value)
                SELECT $1,criterion_id,CASE WHEN code=ANY($2) THEN 1.0 ELSE 0.0 END
                FROM recommendation_criterion WHERE code=ANY($3)
                ON CONFLICT(place_feature_id,criterion_id) DO UPDATE SET feature_value=EXCLUDED.feature_value
                """, connection, transaction);
            values.Parameters.AddWithValue(featureId); values.Parameters.AddWithValue(selected.ToArray());
            values.Parameters.AddWithValue(PlaceFeatureClassifier.Features.ToArray());
            await values.ExecuteNonQueryAsync(ct);
            await using (var snapshot = new NpgsqlCommand("""
                INSERT INTO recommendation_rating_snapshot
                SELECT $1,place_id,rating,rating_count FROM place WHERE place_id=$2
                ON CONFLICT(recommendation_config_id,place_id)
                DO UPDATE SET rating=EXCLUDED.rating,rating_count=EXCLUDED.rating_count
                """, connection, transaction))
            {
                snapshot.Parameters.AddWithValue(configId); snapshot.Parameters.AddWithValue(placeId);
                await snapshot.ExecuteNonQueryAsync(ct);
            }
        }
        await using (var deactivate = new NpgsqlCommand("UPDATE recommendation_config SET status='INACTIVE' WHERE recommendation_config_id=$1", connection, transaction))
        {
            deactivate.Parameters.AddWithValue(previousId); await deactivate.ExecuteNonQueryAsync(ct);
        }
        await using (var activate = new NpgsqlCommand("UPDATE recommendation_config SET status='ACTIVE' WHERE recommendation_config_id=$1", connection, transaction))
        {
            activate.Parameters.AddWithValue(configId); await activate.ExecuteNonQueryAsync(ct);
        }
    }
}
