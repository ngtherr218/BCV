using System.Text.Json;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Discovery.Domain;
using Npgsql;
using NpgsqlTypes;

namespace Bcv.Api.Modules.Admin;

public sealed record ModelPlace(long PlaceId, int Cluster, Dictionary<string,int> Features);
public sealed record ModelImport(string Version, int ClusterCount, decimal DistanceLimit,
    RecommendationSettings Settings, Dictionary<string,decimal> Weights, ModelPlace[] Places);

/// <summary>Offline model exchange through the authenticated admin boundary; never accepts executable artifacts.</summary>
public sealed class RecommendationAdmin(NpgsqlDataSource db)
{
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);

    public static void Validate(ModelImport input)
    {
        if (input.Version is null || input.Version.Length is <1 or >50 || !input.Version.All(c=>char.IsAsciiLetterOrDigit(c)||c is '-' or '_' or '.')
            || input.ClusterCount is <2 or >100 || input.DistanceLimit is <=0 or >10000000
            || input.Settings is null || !input.Settings.IsValid()
            || input.Weights is null || !RecommendationScoring.ValidWeights(input.Weights)
            || input.Weights.Values.Any(w=>decimal.Round(w,4)!=w)
            || input.Places is null || input.Places.Length is <2 or >10000
            || input.Places.Any(p=>p is null || p.PlaceId<=0 || p.Cluster<0 || p.Cluster>=input.ClusterCount
                || p.Features is null || p.Features.Count!=35
                || !p.Features.Keys.ToHashSet().SetEquals(RecommendationScoring.FeatureCodes)
                || p.Features.Values.Any(v=>v is not (0 or 1)) || !p.Features.Values.Any(v=>v==1))
            || input.Places.Select(p=>p.PlaceId).Distinct().Count()!=input.Places.Length
            || input.Places.Select(p=>p.Cluster).Distinct().Count()!=input.ClusterCount)
            throw new AdminValidation("Dữ liệu mô hình, đặc trưng hoặc cấu hình không hợp lệ.");
    }

    public async Task<object> Import(ModelImport input, CancellationToken ct)
    {
        Validate(input);
        await using var conn=await db.OpenConnectionAsync(ct);
        await using var tx=await conn.BeginTransactionAsync(ct);
        await using(var check=new NpgsqlCommand("SELECT count(*) FROM place WHERE place_id=ANY($1)",conn,tx))
        {
            check.Parameters.AddWithValue(input.Places.Select(p=>p.PlaceId).ToArray());
            if ((long)(await check.ExecuteScalarAsync(ct))! != input.Places.Length)
                throw new AdminValidation("Có mã địa danh không tồn tại.");
        }
        long id;
        await using(var insert=new NpgsqlCommand("""
            INSERT INTO recommendation_config(version,distance_limit,cluster_count,status,settings)
            VALUES($1,$2,$3,'DRAFT',$4) RETURNING recommendation_config_id
            """,conn,tx))
        {
            insert.Parameters.AddWithValue(input.Version);insert.Parameters.AddWithValue(input.DistanceLimit);
            insert.Parameters.AddWithValue(input.ClusterCount);
            insert.Parameters.AddWithValue(NpgsqlDbType.Jsonb,JsonSerializer.Serialize(input.Settings,Json));
            id=(long)(await insert.ExecuteScalarAsync(ct))!;
        }
        const string statements="""
            INSERT INTO recommendation_weight
            SELECT $1,c.criterion_id,v.value::numeric FROM jsonb_each_text($2) v
            JOIN recommendation_criterion c ON c.code=v.key;
            INSERT INTO place_feature(place_id,recommendation_config_id,cluster_number)
            SELECT (p->>'placeId')::bigint,$1,(p->>'cluster')::int FROM jsonb_array_elements($3) p;
            INSERT INTO place_feature_value
            SELECT pf.place_feature_id,c.criterion_id,v.value::float8
            FROM jsonb_array_elements($3) p
            JOIN place_feature pf ON pf.place_id=(p->>'placeId')::bigint AND pf.recommendation_config_id=$1
            CROSS JOIN LATERAL jsonb_each_text(p->'features') v
            JOIN recommendation_criterion c ON c.code=v.key;
            INSERT INTO recommendation_rating_snapshot
            SELECT $1,p.place_id,p.rating,p.rating_count FROM place p
            JOIN place_feature pf ON pf.place_id=p.place_id AND pf.recommendation_config_id=$1;
            """;
        foreach(var statement in statements.Split(';',StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            await using var command=new NpgsqlCommand(statement.Replace("$1","@id").Replace("$2","@weights").Replace("$3","@places"),conn,tx);
            command.Parameters.AddWithValue("id",id);
            command.Parameters.AddWithValue("weights",NpgsqlDbType.Jsonb,JsonSerializer.Serialize(input.Weights,Json));
            command.Parameters.AddWithValue("places",NpgsqlDbType.Jsonb,JsonSerializer.Serialize(input.Places,Json));
            await command.ExecuteNonQueryAsync(ct);
        }
        await using(var count=new NpgsqlCommand("""
            SELECT (SELECT count(*) FROM recommendation_weight WHERE recommendation_config_id=$1)=5
               AND (SELECT count(*) FROM place_feature_value v JOIN place_feature f USING(place_feature_id)
                    WHERE f.recommendation_config_id=$1)=$2
            """,conn,tx))
        {
            count.Parameters.AddWithValue(id);count.Parameters.AddWithValue(input.Places.Length*35);
            if(!(bool)(await count.ExecuteScalarAsync(ct))!) throw new AdminValidation("Thiếu tiêu chí trong từ điển dữ liệu.");
        }
        await tx.CommitAsync(ct);
        return new {configId=id,input.Version,status="DRAFT",places=input.Places.Length};
    }

    public async Task<object> Activate(long id,CancellationToken ct)
    {
        await using var conn=await db.OpenConnectionAsync(ct);
        await using var tx=await conn.BeginTransactionAsync(ct);
        await using(var gate=new NpgsqlCommand("SELECT pg_advisory_xact_lock(7246318)",conn,tx))
            await gate.ExecuteNonQueryAsync(ct);
        await using(var check=new NpgsqlCommand("""
            SELECT EXISTS(SELECT 1 FROM recommendation_config c
              WHERE c.recommendation_config_id=$1
                AND (SELECT count(*) FROM recommendation_weight w WHERE w.recommendation_config_id=$1)=5
                AND (SELECT sum(weight_value) FROM recommendation_weight w WHERE w.recommendation_config_id=$1)=1
                AND (SELECT count(DISTINCT cluster_number) FROM place_feature WHERE recommendation_config_id=$1)=c.cluster_count
                AND NOT EXISTS(SELECT 1 FROM place p JOIN region r USING(region_id)
                   WHERE p.status='ACTIVE' AND r.status='ACTIVE' AND NOT EXISTS
                     (SELECT 1 FROM place_feature f WHERE f.place_id=p.place_id AND f.recommendation_config_id=$1))
                AND NOT EXISTS(SELECT 1 FROM place_feature f WHERE f.recommendation_config_id=$1 AND
                    ((SELECT count(*) FROM place_feature_value v WHERE v.place_feature_id=f.place_feature_id)<>35
                     OR NOT EXISTS(SELECT 1 FROM recommendation_rating_snapshot s
                        WHERE s.place_id=f.place_id AND s.recommendation_config_id=$1))))
            """,conn,tx))
        {
            check.Parameters.AddWithValue(id);
            if(!(bool)(await check.ExecuteScalarAsync(ct))!) throw new AdminValidation("Phiên bản không tồn tại hoặc chưa phủ đủ địa danh/đặc trưng/trọng số.");
        }
        await using(var deactivate=new NpgsqlCommand("UPDATE recommendation_config SET status='INACTIVE' WHERE status='ACTIVE'",conn,tx))
            await deactivate.ExecuteNonQueryAsync(ct);
        await using(var update=new NpgsqlCommand("UPDATE recommendation_config SET status='ACTIVE',updated_at=CURRENT_TIMESTAMP WHERE recommendation_config_id=$1",conn,tx))
        {update.Parameters.AddWithValue(id);await update.ExecuteNonQueryAsync(ct);}
        await tx.CommitAsync(ct);
        return new {configId=id,status="ACTIVE"};
    }

    public async Task<JsonElement> Export(CancellationToken ct)
    {
        await using var command=db.CreateCommand("""
            SELECT jsonb_build_object('config',to_jsonb(rc),'weights',
              (SELECT jsonb_object_agg(c.code,w.weight_value) FROM recommendation_weight w JOIN recommendation_criterion c USING(criterion_id) WHERE w.recommendation_config_id=rc.recommendation_config_id),
              'places',(SELECT jsonb_agg(jsonb_build_object('placeId',p.place_id,'name',p.name,'regionName',r.name,
                'latitude',p.latitude,'longitude',p.longitude,'rating',s.rating,'ratingCount',s.rating_count,'cluster',pf.cluster_number,
                'features',(SELECT jsonb_object_agg(c.code,v.feature_value) FROM place_feature_value v JOIN recommendation_criterion c USING(criterion_id) WHERE v.place_feature_id=pf.place_feature_id)) ORDER BY p.place_id)
              FROM place_feature pf JOIN place p USING(place_id) JOIN region r USING(region_id)
              JOIN recommendation_rating_snapshot s ON s.place_id=p.place_id AND s.recommendation_config_id=pf.recommendation_config_id
              WHERE pf.recommendation_config_id=rc.recommendation_config_id AND p.status='ACTIVE' AND r.status='ACTIVE'))::text
            FROM recommendation_config rc WHERE status='ACTIVE'
            """);
        var text=await command.ExecuteScalarAsync(ct) as string ?? throw new AdminValidation("Chưa có cấu hình hoạt động.");
        return JsonSerializer.Deserialize<JsonElement>(text);
    }
}
