using System.Buffers.Binary;
using System.Text.Json;
using Bcv.Api.Modules.Account.Api;
using Bcv.Api.Modules.Account.Application;
using Npgsql;

namespace Bcv.Api.Modules.Admin;

public static class PlaceVideos
{
    public const int MaxBytes=15*1024*1024;
    public sealed record Metadata(double Duration,int Width,int Height);
    private sealed record Box(string Type,int Start,int End);
    private static IEnumerable<Box> Boxes(byte[] bytes,int start,int end)
    {
        while(start<=end-8)
        {
            var size=(long)BinaryPrimitives.ReadUInt32BigEndian(bytes.AsSpan(start,4));
            var type=System.Text.Encoding.ASCII.GetString(bytes,start+4,4);var header=8;
            if(size==1){if(end-start<16)throw new AdminValidation("Video MP4 không hợp lệ.");var large=BinaryPrimitives.ReadUInt64BigEndian(bytes.AsSpan(start+8,8));if(large>int.MaxValue)throw new AdminValidation("Video quá lớn.");size=(long)large;header=16;}
            if(size==0)size=end-start;
            if(size<header||size>end-start)throw new AdminValidation("Cấu trúc video MP4 không hợp lệ.");
            yield return new(type,start+header,start+(int)size);start+=(int)size;
        }
        if(start!=end)throw new AdminValidation("Video MP4 bị cắt hoặc sai cấu trúc.");
    }
    public static Metadata Inspect(byte[] bytes)
    {
        if(bytes.Length<12||bytes.Length>MaxBytes)throw new AdminValidation("Video MP4 tối đa 15 MB.");
        var root=Boxes(bytes,0,bytes.Length).ToArray();
        if(!root.Any(b=>b.Type=="ftyp"))throw new AdminValidation("Chỉ hỗ trợ tệp MP4.");
        if(!root.Any(b=>b.Type=="mdat"&&b.End>b.Start))throw new AdminValidation("MP4 không có dữ liệu video.");
        var movie=root.FirstOrDefault(b=>b.Type=="moov")??throw new AdminValidation("Không đọc được thông tin video MP4.");
        var children=Boxes(bytes,movie.Start,movie.End).ToArray();
        var header=children.FirstOrDefault(b=>b.Type=="mvhd")??throw new AdminValidation("Video thiếu thời lượng.");
        if(header.End-header.Start<20)throw new AdminValidation("Thời lượng video không hợp lệ.");
        var version=bytes[header.Start];if(version>1)throw new AdminValidation("Phiên bản MP4 chưa được hỗ trợ.");
        var timeOffset=version==1?20:12;var durationOffset=version==1?24:16;var durationSize=version==1?8:4;
        if(header.End-header.Start<durationOffset+durationSize)throw new AdminValidation("Thời lượng video không hợp lệ.");
        var scale=BinaryPrimitives.ReadUInt32BigEndian(bytes.AsSpan(header.Start+timeOffset,4));
        var duration=version==1?BinaryPrimitives.ReadUInt64BigEndian(bytes.AsSpan(header.Start+durationOffset,8)):BinaryPrimitives.ReadUInt32BigEndian(bytes.AsSpan(header.Start+durationOffset,4));
        var seconds=scale==0?0:(double)duration/scale;
        if(seconds<=0||seconds>30)throw new AdminValidation("Video phải có thời lượng từ trên 0 đến 30 giây.");
        foreach(var track in children.Where(b=>b.Type=="trak"))
        {
            var parts=Boxes(bytes,track.Start,track.End).ToArray();var mdia=parts.FirstOrDefault(b=>b.Type=="mdia");var tkhd=parts.FirstOrDefault(b=>b.Type=="tkhd");
            if(mdia is null||tkhd is null)continue;
            var handler=Boxes(bytes,mdia.Start,mdia.End).FirstOrDefault(b=>b.Type=="hdlr");
            if(handler is null||handler.End-handler.Start<12||System.Text.Encoding.ASCII.GetString(bytes,handler.Start+8,4)!="vide")continue;
            if(tkhd.End-tkhd.Start<1||bytes[tkhd.Start]>1)throw new AdminValidation("Thông tin luồng video không hợp lệ.");
            var widthOffset=bytes[tkhd.Start]==1?88:76;
            if(tkhd.End-tkhd.Start<widthOffset+8)throw new AdminValidation("Không đọc được kích thước video.");
            var width=(int)(BinaryPrimitives.ReadUInt32BigEndian(bytes.AsSpan(tkhd.Start+widthOffset,4))>>16);var height=(int)(BinaryPrimitives.ReadUInt32BigEndian(bytes.AsSpan(tkhd.Start+widthOffset+4,4))>>16);
            if(width<=0||height<=0||Math.Min(width,height)>720||Math.Max(width,height)>1280)throw new AdminValidation("Video tối đa 720p (1280×720 hoặc 720×1280).");
            return new(seconds,width,height);
        }
        throw new AdminValidation("MP4 không có luồng video được hỗ trợ.");
    }

    public static async Task SaveAsync(NpgsqlConnection conn,NpgsqlTransaction tx,long placeId,long actor,JsonElement body,CancellationToken ct)
    {
        if(!body.TryGetProperty("video_ids",out var array))return; // Older clients keep existing videos.
        if(array.ValueKind!=JsonValueKind.Array||array.GetArrayLength()>5)throw new AdminValidation("Mỗi địa danh tối đa 5 video.");
        var ids=array.EnumerateArray().Select(x=>x.ValueKind==JsonValueKind.Number&&x.TryGetInt64(out var id)&&id>0?id:throw new AdminValidation("Video không hợp lệ.")).Distinct().ToArray();
        await using(var check=new NpgsqlCommand("SELECT video_id FROM place_video WHERE video_id=ANY($1) AND (place_id=$2 OR (place_id IS NULL AND created_by=$3)) FOR UPDATE",conn,tx))
        {
            check.Parameters.AddWithValue(ids);check.Parameters.AddWithValue(placeId);check.Parameters.AddWithValue(actor);
            var count=0;await using var r=await check.ExecuteReaderAsync(ct);while(await r.ReadAsync(ct))count++;
            if(count!=ids.Length)throw new AdminValidation("Video không tồn tại, đã hết hạn hoặc đang thuộc địa danh khác.");
        }
        await using(var count=new NpgsqlCommand("SELECT count(*) FROM community_media WHERE place_id=$1 AND status='APPROVED' AND mime_type LIKE 'video/%'",conn,tx))
        {count.Parameters.AddWithValue(placeId);if((long)(await count.ExecuteScalarAsync(ct))!+ids.Length>5)throw new AdminValidation("Địa danh đã đủ 5 video công khai, tính cả video cộng đồng đã duyệt.");}
        await using(var remove=new NpgsqlCommand("DELETE FROM place_video WHERE place_id=$1 AND NOT(video_id=ANY($2))",conn,tx))
        {remove.Parameters.AddWithValue(placeId);remove.Parameters.AddWithValue(ids);await remove.ExecuteNonQueryAsync(ct);}
        await using(var attach=new NpgsqlCommand("UPDATE place_video SET place_id=$1 WHERE video_id=ANY($2)",conn,tx))
        {attach.Parameters.AddWithValue(placeId);attach.Parameters.AddWithValue(ids);await attach.ExecuteNonQueryAsync(ct);}
    }

    public static void MapEndpoints(RouteGroupBuilder admin,WebApplication app)
    {
        admin.MapPost("/place-videos",async(HttpContext http,NpgsqlDataSource db,CancellationToken ct)=>
        {
            if(http.Request.ContentType?.Split(';')[0]!="video/mp4")throw new AdminValidation("Chỉ chấp nhận video MP4.");
            if(http.Request.ContentLength>MaxBytes)throw new AdminValidation("Video tối đa 15 MB.");
            using var memory=new MemoryStream();var buffer=new byte[81920];int read;
            while((read=await http.Request.Body.ReadAsync(buffer,ct))>0){if(memory.Length+read>MaxBytes)throw new AdminValidation("Video tối đa 15 MB.");await memory.WriteAsync(buffer.AsMemory(0,read),ct);}
            var bytes=memory.ToArray();var metadata=Inspect(bytes);
            await using var cmd=db.CreateCommand("INSERT INTO place_video(created_by,video_data,duration_seconds,width,height) VALUES($1,$2,$3,$4,$5) RETURNING video_id");
            cmd.Parameters.AddWithValue((long)http.Items["AdminId"]!);cmd.Parameters.AddWithValue(bytes);cmd.Parameters.AddWithValue(metadata.Duration);cmd.Parameters.AddWithValue(metadata.Width);cmd.Parameters.AddWithValue(metadata.Height);
            var id=(long)(await cmd.ExecuteScalarAsync(ct))!;
            return Results.Ok(new{videoId=id,url=$"/api/admin/place-videos/{id}",durationSeconds=metadata.Duration,width=metadata.Width,height=metadata.Height});
        }).WithMetadata(new Microsoft.AspNetCore.Mvc.RequestSizeLimitAttribute(MaxBytes));
        admin.MapGet("/place-videos/{id:long}",async(long id,NpgsqlDataSource db,CancellationToken ct)=>
        {
            await using var cmd=db.CreateCommand("SELECT video_data FROM place_video WHERE video_id=$1");cmd.Parameters.AddWithValue(id);
            return await cmd.ExecuteScalarAsync(ct) is byte[] bytes?Results.File(bytes,"video/mp4",enableRangeProcessing:true):Results.NotFound();
        });
        app.MapGet("/api/explore/places/{placeId:long}/videos/{id:long}",async(long placeId,long id,HttpRequest request,ISessionStore sessions,NpgsqlDataSource db,CancellationToken ct)=>
        {
            var user=CurrentUser.Get(request,sessions);if(user is null)return Results.Unauthorized();
            await using var cmd=db.CreateCommand("SELECT v.video_data FROM place_video v JOIN place p ON p.place_id=v.place_id JOIN region r USING(region_id) WHERE v.video_id=$1 AND p.place_id=$2 AND p.status='ACTIVE' AND r.status='ACTIVE' AND EXISTS(SELECT 1 FROM user_discovered_place WHERE user_id=$3 AND place_id=p.place_id)");
            cmd.Parameters.AddWithValue(id);cmd.Parameters.AddWithValue(placeId);cmd.Parameters.AddWithValue(user.UserId);
            request.HttpContext.Response.Headers.CacheControl="private, no-store";
            return await cmd.ExecuteScalarAsync(ct) is byte[] bytes?Results.File(bytes,"video/mp4",enableRangeProcessing:true):Results.NotFound();
        });
    }
}
