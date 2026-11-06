using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.CommunityMedia;
using Bcv.Api.Modules.QuestItem.Domain;
using Npgsql;

static class PhotoQuestChecks
{
    public static async Task Run(NpgsqlDataSource db)
    {
        void Check(bool ok, string message) { if (!ok) throw new Exception(message); }
        async Task Exec(string sql) { await using var c = db.CreateCommand(sql); await c.ExecuteNonQueryAsync(); }
        async Task<long> Scalar(string sql) { await using var c = db.CreateCommand(sql); return Convert.ToInt64(await c.ExecuteScalarAsync()); }
        async Task<long> Progress(long id, string cycle)
        {
            await using var c = db.CreateCommand("SELECT COALESCE((SELECT progress_value FROM quest_progress WHERE user_id=950004 AND quest_id=$1 AND cycle_key=$2),0)");
            c.Parameters.AddWithValue(id); c.Parameters.AddWithValue(cycle); return Convert.ToInt64(await c.ExecuteScalarAsync());
        }
        var (daily, weekly) = QuestCycles.Current();
        await Exec("""
            INSERT INTO quest(quest_id,name,quest_type,condition_type,target_value,cycle_type,status) OVERRIDING SYSTEM VALUE VALUES
              (951201,'Media photo fixture daily','DAILY','PHOTO_APPROVED',2,'DAILY','ACTIVE'),
              (951202,'Media photo fixture weekly','WEEKLY','PHOTO_APPROVED',2,'WEEKLY','ACTIVE'),
              (951203,'Media photo fixture compatibility','DAILY','MEDIA_APPROVED',2,'DAILY','ACTIVE'),
              (951204,'Media photo fixture inactive','DAILY','PHOTO_APPROVED',1,'DAILY','INACTIVE');
            INSERT INTO quest_progress(user_id,quest_id,cycle_key,progress_value,completion_status,completed_at) VALUES
              (950004,951201,'2000-01-01',7,true,'2000-01-01'),
              (950004,951202,'2000-W01',9,true,'2000-01-01');
            INSERT INTO community_media(media_id,user_id,place_id,mime_type,media_data,status,first_approved_at) OVERRIDING SYSTEM VALUE VALUES
              (951201,950004,910001,'image/png',decode('89504e470d0a1a0a00000000','hex'),'PENDING',NULL),
              (951202,950004,910001,'image/jpeg',decode('ffd8ff000000000000000000','hex'),'PENDING',NULL),
              (951203,950004,910001,'video/webm',decode('1a45dfa30000000000000000','hex'),'PENDING',NULL),
              (951204,950004,910001,'image/png',decode('89504e470d0a1a0a00000000','hex'),'FLAGGED','2000-01-01');
            """);
        try
        {
            Check(AdminCatalog.Resources["quests"].Fields.Single(f => f.Name == "condition_type").Options!.Contains("PHOTO_APPROVED"), "Admin cannot configure photo quest");
            await MediaModeration.ReviewAsync(db, 951201, "APPROVED", null, "PENDING", 950005, default);
            Check(await Progress(951201, daily) == 1 && await Progress(951202, weekly) == 1, "First photo did not increment daily/weekly photo quests");
            Check(await Progress(951203, daily) == 1, "Photo did not preserve mixed-media quest compatibility");
            await MediaModeration.ReviewAsync(db, 951203, "APPROVED", null, "PENDING", 950005, default);
            Check(await Progress(951201, daily) == 1 && await Progress(951202, weekly) == 1 && await Progress(951203, daily) == 2, "Video incorrectly incremented photo quest or missed legacy media quest");
            try
            {
                await MediaModeration.ReviewAsync(db, 951201, "APPROVED", null, "PENDING", 950005, default);
                throw new Exception("Repeated approval accepted");
            }
            catch (AdminValidation) { }
            await Exec("UPDATE community_media SET status='FLAGGED' WHERE media_id=951201");
            await MediaModeration.ReviewAsync(db, 951201, "APPROVED", "Đã xem lại", "FLAGGED", 950005, default);
            await MediaModeration.ReviewAsync(db, 951204, "APPROVED", "Khôi phục ảnh của chu kỳ cũ", "FLAGGED", 950005, default);
            Check(await Progress(951201, daily) == 1 && await Progress(951202, weekly) == 1 && await Progress(951203, daily) == 2, "Restore or a previous cycle approval was counted twice");
            await MediaModeration.ReviewAsync(db, 951202, "APPROVED", null, "PENDING", 950005, default);
            Check(await Progress(951201, daily) == 2 && await Progress(951202, weekly) == 2 && await Progress(951203, daily) == 3, "Second photo progress incorrect");
            Check(await Progress(951201, "2000-01-01") == 7 && await Progress(951202, "2000-W01") == 9, "Current approval changed a prior cycle");
            Check(await Scalar("SELECT count(*) FROM quest_progress WHERE user_id=950004 AND quest_id=951204") == 0, "Inactive photo quest received progress");
            Check(await Scalar("SELECT count(*) FROM notification WHERE user_id=950004 AND notification_type='QUEST_COMPLETED' AND content LIKE 'Media photo fixture%'") == 3, "Daily/weekly/compatibility completion notifications missing or duplicated");
            Console.WriteLine("PASS photo-only approval, mixed-media compatibility, first-approval idempotency, daily/weekly cycle separation and inactive quest exclusion");
        }
        finally
        {
            await Exec("""
                DELETE FROM media_report WHERE media_id BETWEEN 951201 AND 951204;
                DELETE FROM community_media WHERE media_id BETWEEN 951201 AND 951204;
                DELETE FROM quest_progress WHERE quest_id BETWEEN 951201 AND 951204 OR (user_id=950004 AND quest_id=950001);
                DELETE FROM quest WHERE quest_id BETWEEN 951201 AND 951204;
                DELETE FROM notification WHERE user_id=950004;
                """);
        }
    }
}
