using Npgsql;
using System.Text.Json;

namespace Bcv.Api.Modules.PlaceCollection.Infrastructure;

public sealed record PassportStamp(long PlaceId, string Name, DateTime DiscoveredAt);
public sealed record PassportRegion(long RegionId, string Name, long Discovered, long Total, int ProgressPercent, bool HasBadge, IReadOnlyList<PassportStamp> Stamps);
public sealed record PassportOverview(IReadOnlyList<PassportRegion> Regions);

public sealed class PassportRepository(NpgsqlDataSource db)
{
    public async Task<PassportOverview> LoadAsync(long userId, CancellationToken ct)
    {
        await using var command = db.CreateCommand("""
            SELECT r.region_id,r.name,count(d.place_id),count(p.place_id),
              COALESCE(jsonb_agg(jsonb_build_object('placeId',p.place_id,'name',p.name,'discoveredAt',d.discovered_at)
                ORDER BY d.discovered_at,p.place_id) FILTER(WHERE d.place_id IS NOT NULL),'[]'::jsonb)::text
            FROM region r LEFT JOIN place p ON p.region_id=r.region_id AND p.status='ACTIVE'
            LEFT JOIN user_discovered_place d ON d.place_id=p.place_id AND d.user_id=$1
            WHERE r.status='ACTIVE' GROUP BY r.region_id,r.name ORDER BY r.name,r.region_id
            """);
        command.Parameters.AddWithValue(userId);
        var regions = new List<PassportRegion>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            var owned=reader.GetInt64(2); var total=reader.GetInt64(3);
            regions.Add(new(reader.GetInt64(0),reader.GetString(1),owned,total,total==0?0:(int)(100*owned/total),
                total>0 && owned==total,JsonSerializer.Deserialize<List<PassportStamp>>(reader.GetString(4),new JsonSerializerOptions(JsonSerializerDefaults.Web))!));
        }
        return new(regions);
    }
}
