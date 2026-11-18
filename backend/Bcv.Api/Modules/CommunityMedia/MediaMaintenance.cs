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
            """,conn,tx);
        await cmd.ExecuteNonQueryAsync(ct);await tx.CommitAsync(ct);
    }
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer=new PeriodicTimer(TimeSpan.FromHours(1));
        do {
            try{await SweepAsync(db,stoppingToken);}
            catch(OperationCanceledException) when(stoppingToken.IsCancellationRequested){return;}
            catch(PostgresException error) when(error.SqlState is PostgresErrorCodes.UndefinedColumn or PostgresErrorCodes.UndefinedTable)
            {
                // SweepAsync rolls back its transaction before this handler runs.
                // Never repair schema or delete files against an incompatible database.
                log.LogWarning("Media cleanup stopped because the database schema is incompatible ({SqlState}: {DatabaseError}). " +
                    "Review sql/README.md and the photo lifecycle migration sql/migrations/030_photo_lifecycle.sql, " +
                    "apply the approved upgrade, then restart the API. For local development, use backend/run-dev-v2.ps1 " +
                    "or set Media__MaintenanceEnabled=false. No schema changes were applied by this worker.",
                    error.SqlState,error.MessageText);
                return;
            }
            catch(Exception error){log.LogError(error,"Failed to clean expired media files");}
        } while(await timer.WaitForNextTickAsync(stoppingToken));
    }
}
