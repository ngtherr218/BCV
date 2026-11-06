using Npgsql;
namespace Bcv.Api.Modules.CommunityMedia;

public sealed class MediaMaintenance(NpgsqlDataSource db,ILogger<MediaMaintenance> log):BackgroundService
{
    public static async Task SweepAsync(NpgsqlDataSource db,CancellationToken ct)
    {
        await using var conn=await db.OpenConnectionAsync(ct);
        await using var tx=await conn.BeginTransactionAsync(ct);
        await using var cmd=new NpgsqlCommand("""
            UPDATE community_media SET media_data=NULL,file_deleted_at=CURRENT_TIMESTAMP
              WHERE status='REJECTED' AND delete_after<=CURRENT_TIMESTAMP AND media_data IS NOT NULL;
            UPDATE community_media SET status='EXPIRED',media_data=NULL,file_deleted_at=CURRENT_TIMESTAMP
              WHERE status='PENDING' AND created_at<CURRENT_TIMESTAMP-INTERVAL '30 days';
            DELETE FROM place_video WHERE place_id IS NULL AND created_at<CURRENT_TIMESTAMP-INTERVAL '24 hours';
            """,conn,tx);
        await cmd.ExecuteNonQueryAsync(ct);await tx.CommitAsync(ct);
    }
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer=new PeriodicTimer(TimeSpan.FromHours(1));
        do {
            try{await SweepAsync(db,stoppingToken);}
            catch(OperationCanceledException) when(stoppingToken.IsCancellationRequested){return;}
            catch(Exception error){log.LogError(error,"Failed to clean expired media files");}
        } while(await timer.WaitForNextTickAsync(stoppingToken));
    }
}
