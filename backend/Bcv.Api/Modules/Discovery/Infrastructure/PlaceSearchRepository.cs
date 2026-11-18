using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Shared.Infrastructure;
using Bcv.Api.Modules.StepLocation.Domain;
using Npgsql;
using NpgsqlTypes;
using System.Text.Json;

namespace Bcv.Api.Modules.Discovery.Infrastructure;

public sealed record PlaceSearchQuery(string? Q = null, decimal? MinDistanceKm = null, decimal? MaxDistanceKm = null,
    int? MaxEstimatedDays = null, long? PlaceTypeId = null, long? InterestGroupId = null,
    int Page = 1, int PageSize = 20, string Sort = "distance", string? InterestGroupIds = null, bool OnlyUndiscovered = false,
    int? MaxRequiredSteps = null)
{
    public long[] SelectedInterests()
    {
        if (string.IsNullOrWhiteSpace(InterestGroupIds)) return [];
        var parts = InterestGroupIds.Split(',');
        if (parts.Length > 3)
            throw new ExploreException("INVALID_FILTER", "Chỉ được chọn tối đa 3 loại địa danh.");
        if (parts.Any(value => !long.TryParse(value, out var id) || id < 101 || id > 112))
            throw new ExploreException("INVALID_FILTER", "Loại địa danh không hợp lệ.");
        return parts.Select(long.Parse).Distinct().ToArray();
    }
    public void Validate()
    {
        _ = SelectedInterests();
        if (Q?.Length > 120 || MinDistanceKm < 0 || MaxDistanceKm < 0 || MaxDistanceKm < MinDistanceKm
            || MaxEstimatedDays <= 0 || MaxRequiredSteps <= 0 || PlaceTypeId <= 0 || InterestGroupId <= 0
            || Page < 1 || Page > 100000 || PageSize < 1 || PageSize > 50
            || Sort is not ("distance" or "name" or "estimatedDays" or "interests"))
            throw new ExploreException("INVALID_FILTER", "Bộ lọc không hợp lệ. Kiểm tra khoảng cách, số bước, số ngày và phân trang.");
    }
}

public sealed record PlaceSearchResult(int Page, int PageSize, long Total, IReadOnlyList<PlaceSearchItem> Places,
    int? MaxRequiredSteps = null, decimal? MaxDistanceKm = null);
public sealed record PlaceSearchItem(long PlaceId, string Name, string PlaceType, string RegionName,
    decimal Latitude, decimal Longitude, string? ImageUrl, bool IsDiscovered,
    decimal DistanceKm, int RequiredSteps, int DailyStepTarget, int EstimatedDays, string RelativeDifficulty, bool MatchesInterests);

public static class JourneyEffort
{
    public static int Days(int steps, int dailyTarget) => steps <= 0 ? 0 : (int)Math.Ceiling((decimal)steps / dailyTarget);
    public static string Difficulty(int days) => days <= 1 ? "EASY" : days <= 3 ? "MODERATE" : "HARD";
    public static async Task<int> DailyTargetAsync(NpgsqlConnection connection, long userId, CancellationToken ct)
    {
        await using var command = new NpgsqlCommand("""
            SELECT up.activity_level, c.settings::text
            FROM user_preference up CROSS JOIN recommendation_config c
            WHERE up.user_id=$1 AND c.status='ACTIVE'
            ORDER BY c.recommendation_config_id DESC LIMIT 1
            """, connection);
        command.Parameters.AddWithValue(userId);
        await using var reader = await command.ExecuteReaderAsync(ct);
        if (!await reader.ReadAsync(ct)) throw new ExploreException("NO_ACTIVITY_CONFIG", "Hãy thiết lập mức vận động; hệ thống cần cấu hình gợi ý đang hoạt động.");
        var settings = JsonSerializer.Deserialize<RecommendationSettings>(reader.GetString(1), new JsonSerializerOptions(JsonSerializerDefaults.Web));
        if (settings is null || !settings.IsValid() || !settings.ActivityThresholds.TryGetValue(reader.GetString(0), out var target))
            throw new ExploreException("INVALID_RECOMMENDATION_CONFIG", "Cấu hình vận động chưa hợp lệ.");
        return target;
    }
}

public sealed class PlaceSearchRepository(NpgsqlDataSource db)
{
    public async Task<object> OptionsAsync(CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT pt.place_type_id,pt.name FROM place_type pt WHERE EXISTS(SELECT 1 FROM place p JOIN region r USING(region_id) WHERE p.place_type_id=pt.place_type_id AND p.status='ACTIVE' AND r.status='ACTIVE') ORDER BY pt.name;
            SELECT g.interest_group_id,p.name FROM interest_group g JOIN place_type p ON p.place_type_id=g.interest_group_id WHERE g.interest_group_id BETWEEN 101 AND 112 AND g.code='PLACE_TYPE_'||p.place_type_id ORDER BY g.interest_group_id;
            SELECT region_id,name FROM region WHERE status='ACTIVE' ORDER BY name,region_id;
            """);
        var types=new List<Bcv.Api.Modules.Account.Application.PlaceTypeOption>();
        var interests=new List<Bcv.Api.Modules.Account.Application.PlaceTypeOption>();
        var regions=new List<Bcv.Api.Modules.Account.Application.PlaceTypeOption>();
        await using var reader=await command.ExecuteReaderAsync(ct);
        while(await reader.ReadAsync(ct))types.Add(new(reader.GetInt64(0),reader.GetString(1)));
        await reader.NextResultAsync(ct);
        while(await reader.ReadAsync(ct))interests.Add(new(reader.GetInt64(0),reader.GetString(1)));
        await reader.NextResultAsync(ct);
        while(await reader.ReadAsync(ct))regions.Add(new(reader.GetInt64(0),reader.GetString(1)));
        return new { placeTypes=types, interests, regions };
    }
    public async Task<PlaceSearchResult> SearchAsync(long userId, PlaceSearchQuery query, CancellationToken ct)
    {
        query.Validate();
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var transaction = await connection.BeginTransactionAsync(System.Data.IsolationLevel.RepeatableRead, ct);
        // All search statements share one consistent snapshot; search never writes user progress.
        await using (var readOnly = new NpgsqlCommand("SET TRANSACTION READ ONLY", connection, transaction)) await readOnly.ExecuteNonQueryAsync(ct);
        var target = await JourneyEffort.DailyTargetAsync(connection, userId, ct);
        await using (var location = new NpgsqlCommand("SELECT EXISTS(SELECT 1 FROM virtual_location WHERE user_id=$1)", connection, transaction))
        {
            location.Parameters.AddWithValue(userId);
            if (!(bool)(await location.ExecuteScalarAsync(ct))!) throw new ExploreException("NO_VIRTUAL_LOCATION", "Hãy chọn điểm xuất phát trước khi tìm địa danh.");
        }
        var order = query.Sort switch { "name" => "name,place_id", "interests" => "matches_interests DESC,distance_km,place_id", "estimatedDays" => "estimated_days,distance_km,place_id", _ => "distance_km,place_id" };
        var sql = $"""
            WITH candidates AS (
              SELECT p.place_id,p.name,pt.name AS place_type,r.name AS region_name,p.latitude,p.longitude,
                (SELECT image_key FROM place_image i WHERE i.place_id=p.place_id ORDER BY place_image_id LIMIT 1) AS image_url,
                EXISTS(SELECT 1 FROM user_discovered_place d WHERE d.user_id=$1 AND d.place_id=p.place_id) AS discovered,
                ({GeoSql.VirtualLocationToPlaceMeters}/1000)::numeric AS distance_km,
                EXISTS(SELECT 1 FROM user_preference up
                  JOIN user_preference_interest ui ON ui.user_preference_id=up.user_preference_id
                  JOIN recommendation_config cfg ON cfg.status='ACTIVE'
                  JOIN place_feature pf ON pf.recommendation_config_id=cfg.recommendation_config_id AND pf.place_id=p.place_id
                  JOIN place_feature_value fv ON fv.place_feature_id=pf.place_feature_id AND fv.feature_value=1
                  JOIN recommendation_criterion rc ON rc.criterion_id=fv.criterion_id
                  WHERE up.user_id=$1 AND rc.code IN (SELECT jsonb_array_elements_text(COALESCE($13::jsonb->ui.interest_group_id::text,cfg.settings->'interestFeatures'->ui.interest_group_id::text)))) AS matches_interests
              FROM place p JOIN region r USING(region_id) JOIN place_type pt USING(place_type_id)
              JOIN virtual_location v ON v.user_id=$1
              WHERE p.status='ACTIVE' AND r.status='ACTIVE'
                AND ($2='' OR strpos(lower(p.name),lower($2))>0)
                AND ($3::bigint IS NULL OR p.place_type_id=$3)
                AND ($4::bigint IS NULL OR EXISTS(
                  SELECT 1 FROM recommendation_config cfg
                  JOIN place_feature pf ON pf.recommendation_config_id=cfg.recommendation_config_id AND pf.place_id=p.place_id
                  JOIN place_feature_value fv ON fv.place_feature_id=pf.place_feature_id AND fv.feature_value=1
                  JOIN recommendation_criterion rc ON rc.criterion_id=fv.criterion_id
                  WHERE cfg.status='ACTIVE' AND rc.code IN (SELECT jsonb_array_elements_text(COALESCE($13::jsonb->$4::text,cfg.settings->'interestFeatures'->$4::text)))))
                AND (cardinality($14::bigint[])=0 OR p.place_type_id=ANY($14::bigint[]))
            ), effort AS (
              SELECT *, CASE WHEN distance_km<=0 THEN 0 ELSE LEAST(2147483647,GREATEST(1,ceil(distance_km*1000*$5/$6)))::int END AS required_steps FROM candidates
            ), filtered AS (
              SELECT *,ceil(required_steps::numeric/$7)::int AS estimated_days FROM effort
              WHERE ($8::numeric IS NULL OR distance_km >= $8) AND ($9::numeric IS NULL OR distance_km <= $9)
                AND ($10::int IS NULL OR ceil(required_steps::numeric/$7) <= $10)
                AND (NOT $15 OR (NOT discovered AND required_steps>0))
                AND ($16::int IS NULL OR required_steps <= $16)
            ), page AS (SELECT * FROM filtered ORDER BY {order} LIMIT $11 OFFSET $12)
            SELECT (SELECT count(*) FROM filtered), page.* FROM (SELECT 1) anchor LEFT JOIN page ON true ORDER BY {order}
            """;
        await using var command = new NpgsqlCommand(sql, connection, transaction);
        command.Parameters.AddWithValue(userId);
        command.Parameters.AddWithValue(query.Q?.Trim() ?? "");
        command.Parameters.Add(new() { NpgsqlDbType=NpgsqlDbType.Bigint, Value=(object?)query.PlaceTypeId ?? DBNull.Value });
        command.Parameters.Add(new() { NpgsqlDbType=NpgsqlDbType.Bigint, Value=(object?)query.InterestGroupId ?? DBNull.Value });
        command.Parameters.AddWithValue(StepConversion.DistanceScale);
        command.Parameters.AddWithValue(StepConversion.StepLengthMeters);
        command.Parameters.AddWithValue(target);
        command.Parameters.Add(new() { NpgsqlDbType=NpgsqlDbType.Numeric, Value=(object?)query.MinDistanceKm ?? DBNull.Value });
        command.Parameters.Add(new() { NpgsqlDbType=NpgsqlDbType.Numeric, Value=(object?)query.MaxDistanceKm ?? DBNull.Value });
        command.Parameters.Add(new() { NpgsqlDbType=NpgsqlDbType.Integer, Value=(object?)query.MaxEstimatedDays ?? DBNull.Value });
        command.Parameters.AddWithValue(query.PageSize);
        command.Parameters.AddWithValue((query.Page-1)*query.PageSize);
        command.Parameters.AddWithValue(Bcv.Api.Modules.Account.Application.InterestCatalog.FeaturesJson);
        command.Parameters.AddWithValue(query.SelectedInterests());
        command.Parameters.AddWithValue(query.OnlyUndiscovered);
        command.Parameters.Add(new() { NpgsqlDbType=NpgsqlDbType.Integer, Value=(object?)query.MaxRequiredSteps ?? DBNull.Value });
        var items = new List<PlaceSearchItem>();
        long total = 0;
        await using (var reader = await command.ExecuteReaderAsync(ct))
        {
            while (await reader.ReadAsync(ct))
            {
                total = reader.GetInt64(0);
                if (reader.IsDBNull(1)) continue;
                var days = reader.GetInt32(12);
                items.Add(new(reader.GetInt64(1), reader.GetString(2), reader.GetString(3), reader.GetString(4),
                    reader.GetDecimal(5), reader.GetDecimal(6), reader.IsDBNull(7)?null:reader.GetString(7), reader.GetBoolean(8),
                    Math.Round(reader.GetDecimal(9),2), reader.GetInt32(11), target, days, JourneyEffort.Difficulty(days), reader.GetBoolean(10)));
            }
        }
        await transaction.CommitAsync(ct);
        return new(query.Page, query.PageSize, total, items, query.MaxRequiredSteps, query.MaxDistanceKm);
    }
}
