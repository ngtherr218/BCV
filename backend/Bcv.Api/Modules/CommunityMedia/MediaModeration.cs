using System.Text.Json;
using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.QuestItem.Infrastructure;
using Npgsql;

namespace Bcv.Api.Modules.CommunityMedia;

public static class MediaModeration
{
    public static readonly string[] Statuses = ["PENDING", "APPROVED", "REJECTED", "FLAGGED", "REMOVED", "EXPIRED"];
    public static string Note(string? note, bool required)
    {
        var value = note?.Trim() ?? "";
        if (value.Length > 500 || required && value.Length == 0)
            throw new AdminValidation("Hãy nhập lý do từ 1 đến 500 ký tự.");
        return value;
    }

    public static async Task ReviewAsync(NpgsqlDataSource db, long id, string status, string? note, string? expectedStatus, long actor, CancellationToken ct)
    {
        if (status is not ("APPROVED" or "REJECTED" or "REMOVED")) throw new AdminValidation("Thao tác kiểm duyệt không hợp lệ.");
        var reason = Note(note, status != "APPROVED");
        await using var conn = await db.OpenConnectionAsync(ct);
        await using var tx = await conn.BeginTransactionAsync(ct);
        // Lock the parent first to serialize the video quota with official-video edits.
        await using(var gate=new NpgsqlCommand("SELECT place_id FROM place WHERE place_id=(SELECT place_id FROM community_media WHERE media_id=$1) FOR UPDATE",conn,tx))
        {gate.Parameters.AddWithValue(id);await gate.ExecuteScalarAsync(ct);}
        long owner; string previous, place; bool first, video, photo;long placeId;
        await using (var cmd = new NpgsqlCommand("SELECT m.user_id,m.status,m.first_approved_at IS NULL,p.name,m.mime_type LIKE 'video/%',m.place_id,m.mime_type LIKE 'image/%' FROM community_media m JOIN place p USING(place_id) WHERE m.media_id=$1 FOR UPDATE OF m", conn, tx))
        {
            cmd.Parameters.AddWithValue(id);
            await using var r = await cmd.ExecuteReaderAsync(ct);
            if (!await r.ReadAsync(ct)) throw new KeyNotFoundException("Không tìm thấy nội dung.");
            owner=r.GetInt64(0); previous=r.GetString(1); first=r.GetBoolean(2); place=r.GetString(3);video=r.GetBoolean(4);placeId=r.GetInt64(5);photo=r.GetBoolean(6);
        }
        if (expectedStatus is not null && previous != expectedStatus) throw new AdminValidation("Trạng thái đã thay đổi. Hãy tải lại trước khi xử lý.");
        var valid = status switch {
            "APPROVED" => previous is "PENDING" or "FLAGGED",
            "REJECTED" => previous == "PENDING",
            "REMOVED" => previous is "APPROVED" or "FLAGGED",
            _ => false
        };
        if (status == "APPROVED" && previous == "APPROVED")
        {
            await using var reports = new NpgsqlCommand("SELECT EXISTS(SELECT 1 FROM media_report WHERE media_id=$1 AND status='OPEN')",conn,tx);
            reports.Parameters.AddWithValue(id);
            valid = (bool)(await reports.ExecuteScalarAsync(ct))!;
        }
        if (!valid) throw new AdminValidation("Nội dung đã được xử lý hoặc không thể thực hiện thao tác này. Hãy tải lại.");
        if (status == "APPROVED" && previous != "PENDING") reason = Note(note, true);
        if(status=="APPROVED" && previous!="APPROVED" && video)
        {
            await using var quota=new NpgsqlCommand("SELECT (SELECT count(*) FROM place_video WHERE place_id=$1)+(SELECT count(*) FROM community_media WHERE place_id=$1 AND status='APPROVED' AND mime_type LIKE 'video/%')",conn,tx);
            quota.Parameters.AddWithValue(placeId);
            if((long)(await quota.ExecuteScalarAsync(ct))!>=5)throw new AdminValidation("Địa danh đã đủ 5 video công khai. Không thể duyệt thêm video.");
        }
        await using (var cmd = new NpgsqlCommand("UPDATE community_media SET status=$2,review_note=$3,reviewed_by=$4,reviewed_at=CURRENT_TIMESTAMP,media_data=CASE WHEN $2='REMOVED' THEN NULL ELSE media_data END,file_deleted_at=CASE WHEN $2='REMOVED' THEN CURRENT_TIMESTAMP ELSE file_deleted_at END,delete_after=CASE WHEN $2='REJECTED' THEN CURRENT_TIMESTAMP+INTERVAL '7 days' ELSE NULL END,first_approved_at=CASE WHEN $2='APPROVED' THEN COALESCE(first_approved_at,CURRENT_TIMESTAMP) ELSE first_approved_at END WHERE media_id=$1",conn,tx))
        {
            cmd.Parameters.AddWithValue(id); cmd.Parameters.AddWithValue(status); cmd.Parameters.AddWithValue(reason); cmd.Parameters.AddWithValue(actor); await cmd.ExecuteNonQueryAsync(ct);
        }
        await using (var cmd = new NpgsqlCommand("UPDATE media_report SET status=CASE WHEN $3='REMOVED' THEN 'ACCEPTED' ELSE 'DISMISSED' END,resolved_at=CURRENT_TIMESTAMP,resolved_by=$2 WHERE media_id=$1 AND status='OPEN'",conn,tx))
        { cmd.Parameters.AddWithValue(id); cmd.Parameters.AddWithValue(actor); cmd.Parameters.AddWithValue(status); await cmd.ExecuteNonQueryAsync(ct); }
        var title = status switch {"APPROVED"=>previous=="PENDING"?"Nội dung đã được duyệt":previous=="APPROVED"?"Báo cáo nội dung đã được xử lý":"Nội dung đã được khôi phục", "REJECTED"=>"Nội dung bị từ chối", _=>"Nội dung đã bị gỡ"};
        await Notify(conn,tx,owner,"MEDIA_"+status,title,$"Ảnh/video tại {place}. {reason}",ct);
        if (status == "APPROVED" && first)
        {
            // Keep existing mixed-media quests working; photo-only quests must never count a video.
            await QuestProgress.RecordAsync(conn,tx,owner,QuestProgress.MediaApproved,1,ct);
            if (photo) await QuestProgress.RecordAsync(conn,tx,owner,QuestProgress.PhotoApproved,1,ct);
        }
        await tx.CommitAsync(ct);
    }

    public static async Task SetUploadAccessAsync(NpgsqlDataSource db,long userId,bool blocked,string? note,long actor,CancellationToken ct)
    {
        var reason=Note(note,true);
        await using var conn=await db.OpenConnectionAsync(ct);
        await using var tx=await conn.BeginTransactionAsync(ct);
        await using(var role=new NpgsqlCommand("SELECT role FROM app_user WHERE user_id=$1 FOR UPDATE",conn,tx))
        {role.Parameters.AddWithValue(userId);if(await role.ExecuteScalarAsync(ct) is "ADMIN")throw new AdminValidation("Không thay đổi quyền gửi của tài khoản quản trị.");}
        await using(var cmd=new NpgsqlCommand("SELECT media_upload_blocked FROM app_user WHERE user_id=$1 FOR UPDATE",conn,tx))
        {
            cmd.Parameters.AddWithValue(userId);
            var current=await cmd.ExecuteScalarAsync(ct);
            if(current is null) throw new KeyNotFoundException("Không tìm thấy người gửi.");
            if((bool)current==blocked) throw new AdminValidation("Quyền gửi đã thay đổi. Hãy tải lại.");
        }
        await using(var cmd=new NpgsqlCommand("UPDATE app_user SET media_upload_blocked=$2 WHERE user_id=$1",conn,tx))
        {cmd.Parameters.AddWithValue(userId);cmd.Parameters.AddWithValue(blocked);await cmd.ExecuteNonQueryAsync(ct);}
        await Notify(conn,tx,userId,"MEDIA_UPLOAD_ACCESS",blocked?"Quyền gửi nội dung đã bị khóa":"Quyền gửi nội dung đã được mở",$"Quản trị viên #{actor}: {reason}",ct);
        await tx.CommitAsync(ct);
    }

    public static async Task ReportAsync(NpgsqlDataSource db,long id,long reporter,string? note,int threshold,CancellationToken ct)
    {
        var reason=Note(note,true);
        await using var conn=await db.OpenConnectionAsync(ct);
        await using var tx=await conn.BeginTransactionAsync(ct);
        long owner; string state;
        await using(var cmd=new NpgsqlCommand("""
            SELECT m.user_id,m.status FROM community_media m
            JOIN place p ON p.place_id=m.place_id JOIN region r USING(region_id)
            JOIN app_user viewer ON viewer.user_id=$2 AND viewer.status='ACTIVE'
            WHERE m.media_id=$1 AND m.media_data IS NOT NULL AND
              (viewer.role='ADMIN' OR (p.status='ACTIVE' AND r.status='ACTIVE'
                AND EXISTS(SELECT 1 FROM user_discovered_place d WHERE d.place_id=p.place_id AND d.user_id=$2)
                AND (m.status='APPROVED' OR m.user_id=$2)))
            FOR UPDATE OF m
            """,conn,tx))
        {
            cmd.Parameters.AddWithValue(id);cmd.Parameters.AddWithValue(reporter);await using var r=await cmd.ExecuteReaderAsync(ct);
            if(!await r.ReadAsync(ct))throw new KeyNotFoundException("Không tìm thấy nội dung.");
            owner=r.GetInt64(0);state=r.GetString(1);
        }
        if(owner==reporter)throw new AdminValidation("Không thể báo cáo nội dung của chính bạn.");
        if(state!="APPROVED")throw new AdminValidation("Nội dung này không còn công khai.");
        await using(var cmd=new NpgsqlCommand("INSERT INTO media_report(media_id,reporter_id,reason) VALUES($1,$2,$3) ON CONFLICT(media_id,reporter_id) DO NOTHING",conn,tx))
        {
            cmd.Parameters.AddWithValue(id);cmd.Parameters.AddWithValue(reporter);cmd.Parameters.AddWithValue(reason);
            if(await cmd.ExecuteNonQueryAsync(ct)==0)throw new AdminValidation("Bạn đã báo cáo nội dung này.");
        }
        await using(var cmd=new NpgsqlCommand("SELECT count(*) FROM media_report WHERE media_id=$1 AND status='OPEN'",conn,tx))
        {
            cmd.Parameters.AddWithValue(id);
            if((long)(await cmd.ExecuteScalarAsync(ct))! >= Math.Clamp(threshold,1,100))
            {
                await using var hide=new NpgsqlCommand("UPDATE community_media SET status='FLAGGED' WHERE media_id=$1",conn,tx);
                hide.Parameters.AddWithValue(id);await hide.ExecuteNonQueryAsync(ct);
                await Notify(conn,tx,owner,"MEDIA_FLAGGED","Nội dung tạm ẩn để kiểm tra","Ảnh/video của bạn đang được quản trị viên xem xét sau khi có báo cáo.",ct);
            }
        }
        await tx.CommitAsync(ct);
    }

    private static async Task Notify(NpgsqlConnection conn,NpgsqlTransaction tx,long owner,string type,string title,string text,CancellationToken ct)
    {
        await using var cmd=new NpgsqlCommand("INSERT INTO notification(user_id,notification_type,title,content) VALUES($1,$2,$3,$4)",conn,tx);
        cmd.Parameters.AddWithValue(owner);cmd.Parameters.AddWithValue(type);cmd.Parameters.AddWithValue(title);cmd.Parameters.AddWithValue(text);await cmd.ExecuteNonQueryAsync(ct);
    }

    public static async Task<JsonElement> ListAsync(NpgsqlDataSource db,string? status,string? q,string? kind,bool reported,int page,int pageSize,CancellationToken ct,long? placeId=null)
    {
        status??="PENDING";q=(q??"").Trim();kind??="";
        if(status!=""&&!Statuses.Contains(status)||kind is not ("" or "image" or "video")||q.Length>200)throw new AdminValidation("Bộ lọc không hợp lệ.");
        if(placeId is <=0)throw new AdminValidation("Địa danh không hợp lệ.");
        page=Math.Clamp(page,1,100000);pageSize=Math.Clamp(pageSize,1,50);
        await using var cmd=db.CreateCommand("""
            WITH rows AS (
              SELECT m.media_id,m.place_id,m.user_id,p.name place_name,u.full_name author,u.media_upload_blocked,
                m.caption,m.mime_type,m.status,m.review_note,m.created_at,m.reviewed_at,a.full_name reviewer,
                COALESCE(octet_length(m.media_data),0)::bigint bytes,m.media_data IS NOT NULL content_available,
                (SELECT count(*) FROM media_report r WHERE r.media_id=m.media_id AND r.status='OPEN') report_count,
                (SELECT count(*) FROM media_report r WHERE r.media_id=m.media_id) total_reports
              FROM community_media m JOIN place p USING(place_id) JOIN app_user u ON u.user_id=m.user_id
              LEFT JOIN app_user a ON a.user_id=m.reviewed_by
            ), filtered AS (
              SELECT * FROM rows WHERE ($1='' OR status=$1) AND ($2='' OR place_name ILIKE '%'||$2||'%' OR author ILIKE '%'||$2||'%' OR caption ILIKE '%'||$2||'%')
                AND ($3='' OR mime_type LIKE $3||'/%') AND (NOT $4 OR report_count>0) AND ($7::bigint IS NULL OR place_id=$7)
            )
            SELECT jsonb_build_object('total',(SELECT count(*) FROM filtered),'page',$5,'pageSize',$6,
              'stats',jsonb_build_object('pending',(SELECT count(*) FROM rows WHERE status='PENDING'),'approved',(SELECT count(*) FROM rows WHERE status='APPROVED'),
                'flagged',(SELECT count(*) FROM rows WHERE status='FLAGGED'),'reported',(SELECT count(*) FROM rows WHERE report_count>0),'bytes',(SELECT COALESCE(sum(bytes),0) FROM rows)),
              'items',COALESCE((SELECT jsonb_agg(jsonb_build_object('mediaId',media_id,'placeId',place_id,'userId',user_id,'placeName',place_name,'author',author,
                'uploadBlocked',media_upload_blocked,'caption',caption,'mimeType',mime_type,'status',status,'reviewNote',review_note,'createdAt',created_at,'reviewedAt',reviewed_at,
                'reviewer',reviewer,'contentAvailable',content_available,'bytes',bytes,'reportCount',report_count,'totalReports',total_reports,'contentUrl','/api/community-media/'||media_id||'/content'))
                FROM (SELECT * FROM filtered ORDER BY CASE WHEN status='PENDING' THEN created_at END ASC,CASE WHEN status='APPROVED' THEN reviewed_at END DESC NULLS LAST,created_at DESC,media_id DESC LIMIT $6 OFFSET (($5-1)*$6)) s),'[]'::jsonb))::text
            """);
        cmd.Parameters.AddWithValue(status);cmd.Parameters.AddWithValue(q);cmd.Parameters.AddWithValue(kind);cmd.Parameters.AddWithValue(reported);cmd.Parameters.AddWithValue(page);cmd.Parameters.AddWithValue(pageSize);cmd.Parameters.AddWithValue(NpgsqlTypes.NpgsqlDbType.Bigint,(object?)placeId??DBNull.Value);
        return JsonSerializer.Deserialize<JsonElement>((string)(await cmd.ExecuteScalarAsync(ct))!);
    }

    public static async Task<JsonElement> ReportsAsync(NpgsqlDataSource db,long id,CancellationToken ct)
    {
        await using var cmd=db.CreateCommand("SELECT COALESCE(jsonb_agg(to_jsonb(s)),'[]'::jsonb)::text FROM (SELECT r.report_id,r.reason,r.status,r.created_at,u.full_name reporter FROM media_report r JOIN app_user u ON u.user_id=r.reporter_id WHERE r.media_id=$1 ORDER BY r.created_at DESC LIMIT 100) s");
        cmd.Parameters.AddWithValue(id);return JsonSerializer.Deserialize<JsonElement>((string)(await cmd.ExecuteScalarAsync(ct))!);
    }
}
