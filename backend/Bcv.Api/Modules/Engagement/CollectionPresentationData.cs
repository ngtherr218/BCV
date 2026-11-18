using Npgsql;
using Bcv.Api.Modules.Discovery.Domain;
namespace Bcv.Api.Modules.Engagement;

public static partial class EngagementEndpoints
{
    private sealed record PassportPlace(long PlaceId,string Name,bool Stamped,string? Date);
    private static async Task<object> CollectionPassportAsync(NpgsqlDataSource db,long userId,CancellationToken ct)
    {
        var rows=new List<(long RegionId,string Region,long? PlaceId,string? Place,DateOnly? Date)>();
        await using(var command=db.CreateCommand("""
            SELECT r.region_id,r.name,p.place_id,p.name,d.discovered_at::date
            FROM region r LEFT JOIN place p ON p.region_id=r.region_id AND
              ((p.status='ACTIVE' AND r.status='ACTIVE') OR EXISTS(SELECT 1 FROM user_discovered_place own WHERE own.user_id=$1 AND own.place_id=p.place_id))
            LEFT JOIN user_discovered_place d ON d.user_id=$1 AND d.place_id=p.place_id
            WHERE r.status='ACTIVE' OR EXISTS(SELECT 1 FROM user_discovered_place own JOIN place p2 USING(place_id) WHERE own.user_id=$1 AND p2.region_id=r.region_id)
            ORDER BY r.name,p.name,p.place_id
            """))
        {
            command.Parameters.AddWithValue(userId);await using var reader=await command.ExecuteReaderAsync(ct);
            while(await reader.ReadAsync(ct))rows.Add((reader.GetInt64(0),reader.GetString(1),reader.IsDBNull(2)?null:reader.GetInt64(2),reader.IsDBNull(3)?null:reader.GetString(3),reader.IsDBNull(4)?null:reader.GetFieldValue<DateOnly>(4)));
        }
        var regions=rows.GroupBy(r=>(r.RegionId,r.Region)).Select(g=>{
            var places=g.Where(r=>r.PlaceId.HasValue).Select(r=>new PassportPlace(r.PlaceId!.Value,r.Place!,r.Date.HasValue,r.Date?.ToString("yyyy-MM-dd"))).ToList();
            var visited=places.Count(p=>p.Stamped);var total=places.Count;
            return new{provinceId=g.Key.RegionId,name=g.Key.Region,visited,total,tier=visited==0?"NONE":visited==total?"GOLD":visited*2>=total?"SILVER":"BRONZE",places};
        }).ToList();
        return new{stamped=regions.Count(r=>r.visited>0),total=regions.Count,placesStamped=regions.Sum(r=>r.visited),placesTotal=regions.Sum(r=>r.total),gold=regions.Count(r=>r.tier=="GOLD"),zones=new[]{new{name="Các khu vực Việt Nam",provinces=regions}}};
    }
    private sealed record CollectionBadge(string Code,string Name,string Description,bool Earned,string Progress);
    private static async Task<object> CollectionBadgesAsync(NpgsqlDataSource db,long userId,CancellationToken ct)
    {
        int discovered,regions,complete,journeys,correct,mastered;
        await using(var command=db.CreateCommand("""
            SELECT (SELECT count(*)::int FROM user_discovered_place WHERE user_id=$1),
              (SELECT count(DISTINCT p.region_id)::int FROM user_discovered_place d JOIN place p USING(place_id) WHERE d.user_id=$1),
              (SELECT count(*)::int FROM region r WHERE r.status='ACTIVE' AND EXISTS(SELECT 1 FROM place p WHERE p.region_id=r.region_id AND p.status='ACTIVE')
                AND NOT EXISTS(SELECT 1 FROM place p WHERE p.region_id=r.region_id AND p.status='ACTIVE' AND NOT EXISTS(SELECT 1 FROM user_discovered_place d WHERE d.user_id=$1 AND d.place_id=p.place_id))),
              (SELECT count(*)::int FROM journey WHERE user_id=$1 AND status='COMPLETED'),
              (SELECT count(*)::int FROM journey_quiz_answer a JOIN journey j USING(journey_id) WHERE j.user_id=$1 AND a.is_correct),
              (SELECT count(*)::int FROM user_place_mastery WHERE user_id=$1)
            """))
        {
            command.Parameters.AddWithValue(userId);await using var reader=await command.ExecuteReaderAsync(ct);await reader.ReadAsync(ct);
            discovered=reader.GetInt32(0);regions=reader.GetInt32(1);complete=reader.GetInt32(2);journeys=reader.GetInt32(3);correct=reader.GetInt32(4);mastered=reader.GetInt32(5);
        }
        var (days,pauses)=await EngagementCore.HistoryAsync(db,userId,ct);var best=EngagementCore.ComputeStreak(days,pauses,EngagementCore.Today()).Best;
        CollectionBadge Badge(string code,string name,string desc,int value,int target)=>new(code,name,desc,value>=target,$"{Math.Min(value,target)}/{target}");
        return new[]{Badge("FIRST_STEP","Bước chân đầu tiên","Khám phá địa danh đầu tiên",discovered,1),
            Badge("EXPLORER_10","Lữ khách","Khám phá 10 địa danh",discovered,10),Badge("EXPLORER_50","Nhà thám hiểm","Khám phá 50 địa danh",discovered,50),Badge("EXPLORER_100","Huyền thoại","Khám phá 100 địa danh",discovered,100),
            Badge("THREE_ZONES","Xuyên vùng","Khám phá địa danh ở 3 khu vực",regions,3),Badge("ZONE_COMPLETE","Chinh phục một vùng","Khám phá mọi địa danh đang hoạt động trong một khu vực",complete,1),
            Badge("STREAK_7","Bền bỉ 7 ngày","Giữ chuỗi 7 ngày liên tiếp",best,7),Badge("STREAK_30","Bền bỉ 30 ngày","Giữ chuỗi 30 ngày liên tiếp",best,30),Badge("STREAK_100","Bền bỉ 100 ngày","Giữ chuỗi 100 ngày liên tiếp",best,100),
            Badge("JOURNEY_FIRST","Hành trình đầu tiên","Hoàn thành một hành trình",journeys,1),Badge("REGIONS_5","Dấu chân 5 vùng","Đóng dấu hộ chiếu ở 5 khu vực",regions,5),
            Badge("QUIZ_10","Am hiểu văn hóa","Trả lời đúng 10 câu hỏi văn hóa",correct,10),Badge("MASTER_3","Nhà thông thái","Có 3 thẻ bản Am hiểu",mastered,3)};
    }
}

public static class CollectionActivityData
{
    /// <summary>Physical step delta only. Health totals and journey totals use max, never sum twice.</summary>
    public static async Task RecordJourneyStepsAsync(NpgsqlConnection connection,NpgsqlTransaction transaction,long userId,int delta,CancellationToken ct)
    {
        if(delta<=0)return;
        string level;
        await using(var goal=new NpgsqlCommand("SELECT activity_level FROM user_preference WHERE user_id=$1",connection,transaction))
        {goal.Parameters.AddWithValue(userId);level=await goal.ExecuteScalarAsync(ct) as string??"MEDIUM";}
        var threshold=RecommendationScoring.ActivityThresholds.GetValueOrDefault(level,6000);
        await using var command=new NpgsqlCommand("""
            INSERT INTO daily_activity(user_id,activity_date,steps,journey_steps,goal_steps) VALUES($1,$2,$3,$3,$4)
            ON CONFLICT(user_id,activity_date) DO UPDATE SET
              journey_steps=LEAST(daily_activity.journey_steps+EXCLUDED.journey_steps,40000),
              steps=LEAST(GREATEST(daily_activity.health_steps,daily_activity.journey_steps+EXCLUDED.journey_steps),40000),updated_at=CURRENT_TIMESTAMP
            """,connection,transaction);
        command.Parameters.AddWithValue(userId);command.Parameters.AddWithValue(EngagementCore.Today());command.Parameters.AddWithValue(Math.Min(delta,40000));command.Parameters.AddWithValue(threshold);
        await command.ExecuteNonQueryAsync(ct);
    }
}
