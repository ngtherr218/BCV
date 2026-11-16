using Bcv.Api.Modules.Discovery.Application;
using Npgsql;
using NpgsqlTypes;
using System.Globalization;

namespace Bcv.Api.Modules.StatsNotify.Infrastructure;

public sealed record LeaderboardQuery(string? Week=null,long? RegionId=null,int Limit=20);
public sealed record LeaderboardEntry(long UserId,string DisplayName,long? Rank,long Points);
public sealed record LeaderboardResult(string Week,DateTimeOffset StartsAt,DateTimeOffset EndsAt,long? RegionId,IReadOnlyList<LeaderboardEntry> Top,LeaderboardEntry Me,ExplorerRewardConfig? RewardRules=null);

public static class ExplorerWeek
{
    public static (string Key,DateTimeOffset Start,DateTimeOffset End) Resolve(string? key,DateTimeOffset now)
    {
        var local=now.ToOffset(TimeSpan.FromHours(7)).Date;
        key ??= $"{ISOWeek.GetYear(local)}-W{ISOWeek.GetWeekOfYear(local):00}";
        if(key.Length!=8 || key[4..6]!="-W" || !int.TryParse(key[..4],out var year) || !int.TryParse(key[6..],out var week)
            || year<2 || year>9998 || week<1 || week>ISOWeek.GetWeeksInYear(year))
            throw new ExploreException("INVALID_WEEK","Tuần không hợp lệ. Dùng định dạng YYYY-Www.");
        var start=new DateTimeOffset(ISOWeek.ToDateTime(year,week,DayOfWeek.Monday),TimeSpan.FromHours(7));
        return(key,start,start.AddDays(7));
    }
}

public sealed class LeaderboardRepository(NpgsqlDataSource db)
{
    public async Task<LeaderboardResult> LoadAsync(long userId,LeaderboardQuery query,CancellationToken ct)
    {
        if(query.RegionId<=0 || query.Limit<1 || query.Limit>100)throw new ExploreException("INVALID_FILTER","Vùng hoặc giới hạn xếp hạng không hợp lệ.");
        var period=ExplorerWeek.Resolve(query.Week,DateTimeOffset.UtcNow);
        await using var connection=await db.OpenConnectionAsync(ct);
        var rules=await ExplorerRewardConfig.LoadAsync(connection,null,ct);
        await using var command=new NpgsqlCommand("""
            WITH totals AS (
              SELECT e.user_id,sum(e.points)::bigint AS points FROM explorer_score_event e
              JOIN app_user u ON u.user_id=e.user_id AND u.status='ACTIVE'
              WHERE e.occurred_at >= $1 AND e.occurred_at < $2 AND ($3::bigint IS NULL OR e.region_id=$3)
              GROUP BY e.user_id
            ), ranked AS (SELECT *,dense_rank() OVER(ORDER BY points DESC) AS rank FROM totals),
            top_rows AS (SELECT * FROM ranked ORDER BY points DESC,user_id LIMIT $4)
            SELECT user_id,points,rank,false AS is_me FROM top_rows
            UNION ALL SELECT user_id,points,rank,true FROM ranked WHERE user_id=$5
            """,connection);
        command.Parameters.AddWithValue(period.Start.ToUniversalTime());command.Parameters.AddWithValue(period.End.ToUniversalTime());
        command.Parameters.Add(new(){NpgsqlDbType=NpgsqlDbType.Bigint,Value=(object?)query.RegionId??DBNull.Value});
        command.Parameters.AddWithValue(query.Limit);command.Parameters.AddWithValue(userId);
        var top=new List<LeaderboardEntry>();
        var me=new LeaderboardEntry(userId,$"Nhà khám phá {userId}",null,0);
        await using var reader=await command.ExecuteReaderAsync(ct);
        while(await reader.ReadAsync(ct))
        {
            var id=reader.GetInt64(0);var entry=new LeaderboardEntry(id,$"Nhà khám phá {id}",reader.GetInt64(2),reader.GetInt64(1));
            if(reader.GetBoolean(3))me=entry;else top.Add(entry);
        }
        return new(period.Key,period.Start,period.End,query.RegionId,top.OrderBy(x=>x.Rank).ThenBy(x=>x.UserId).ToList(),me,rules);
    }
}
