using System.Buffers.Binary;
using Bcv.Api.Modules.Admin;

namespace Bcv.Api.Modules.CommunityMedia;

public static class WebmMetadata
{
    private sealed record Element(ulong Id,int Start,int End);
    private static IEnumerable<Element> Elements(byte[] bytes,int start,int end)
    {
        while(start<end)
        {
            var id=Read(bytes,ref start,end,true,out _);var size=Read(bytes,ref start,end,false,out var unknown);
            if(!unknown&&size>(ulong)(end-start))throw new AdminValidation("Cấu trúc video WebM không hợp lệ.");
            var next=unknown?end:start+(int)size;yield return new(id,start,next);start=next;
        }
    }
    private static ulong Read(byte[] bytes,ref int offset,int end,bool id,out bool unknown)
    {
        if(offset>=end||bytes[offset]==0)throw new AdminValidation("WebM bị cắt hoặc sai cấu trúc.");
        var first=bytes[offset++];int length=1,mask=128;while((first&mask)==0){length++;mask>>=1;}
        if(length>(id?4:8)||offset+length-1>end)throw new AdminValidation("WebM không hợp lệ.");
        ulong value=id?first:(ulong)(first&(mask-1));for(var i=1;i<length;i++)value=(value<<8)|bytes[offset++];
        unknown=!id&&value==((1UL<<(7*length))-1);return value;
    }
    private static ulong Unsigned(byte[] bytes,Element e)
    {
        if(e.End-e.Start is <1 or >8)throw new AdminValidation("Thông tin WebM không hợp lệ.");
        ulong result=0;for(var i=e.Start;i<e.End;i++)result=(result<<8)|bytes[i];return result;
    }
    private static bool HasVideoBlock(byte[] bytes,Element[] children,ulong trackNumber)
    {
        foreach(var cluster in children.Where(e=>e.Id==0x1F43B675))
        foreach(var child in Elements(bytes,cluster.Start,cluster.End))
        {
            var blocks=child.Id==0xA3?new[]{child}:child.Id==0xA0
                ?Elements(bytes,child.Start,child.End).Where(e=>e.Id==0xA1):Array.Empty<Element>();
            foreach(var block in blocks)
            {
                var offset=block.Start;
                var track=Read(bytes,ref offset,block.End,false,out _);
                // A block contains a track VINT, timestamp (2 bytes), flags and frame data.
                // Match the video track so an audio-only cluster cannot validate a video upload.
                if(track!=trackNumber||block.End-offset<=3)continue;
                offset+=2;
                var lacing=bytes[offset++]&6;
                if(lacing==0)return true;
                var frames=bytes[offset++]+1;
                if(frames<2)continue;
                if(lacing==4)
                {
                    if(block.End>offset&&(block.End-offset)%frames==0)return true;
                    continue;
                }
                long sizes=0,previous=0;
                for(var frame=0;frame<frames-1;frame++)
                {
                    long size;
                    if(lacing==2)
                    {
                        size=0;byte part;
                        do {if(offset>=block.End)throw new AdminValidation("Dữ liệu khung hình WebM bị cắt.");part=bytes[offset++];size+=part;}while(part==255);
                    }
                    else
                    {
                        var start=offset;
                        var encoded=Read(bytes,ref offset,block.End,false,out _);
                        size=frame==0?(long)encoded:previous+(long)encoded-((1L<<(7*(offset-start)-1))-1);
                    }
                    if(size<=0)throw new AdminValidation("WebM có khung hình rỗng.");
                    sizes+=size;previous=size;
                }
                if(sizes<block.End-offset)return true; // The final frame takes the remaining bytes.
            }
        }
        return false;
    }
    public static PlaceVideos.Metadata Inspect(byte[] bytes)
    {
        if(bytes.Length>PlaceVideos.MaxBytes)throw new AdminValidation("Video tối đa 100 MB.");
        var root=Elements(bytes,0,bytes.Length).ToArray();
        if(!root.Any(e=>e.Id==0x1A45DFA3))throw new AdminValidation("Không phải video WebM.");
        var segment=root.FirstOrDefault(e=>e.Id==0x18538067)??throw new AdminValidation("WebM thiếu dữ liệu video.");
        var children=Elements(bytes,segment.Start,segment.End).ToArray();
        var info=children.FirstOrDefault(e=>e.Id==0x1549A966)??throw new AdminValidation("WebM thiếu thông tin thời lượng.");
        var settings=Elements(bytes,info.Start,info.End).ToArray();var time=settings.FirstOrDefault(e=>e.Id==0x2AD7B1);var scale=time is null?1000000UL:Unsigned(bytes,time);
        var duration=settings.FirstOrDefault(e=>e.Id==0x4489)??throw new AdminValidation("Không đọc được thời lượng WebM. Hãy xuất lại video.");
        double units=(duration.End-duration.Start) switch {4=>BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(bytes.AsSpan(duration.Start,4))),8=>BitConverter.Int64BitsToDouble(BinaryPrimitives.ReadInt64BigEndian(bytes.AsSpan(duration.Start,8))),_=>0};
        var seconds=units*scale/1_000_000_000d;
        if(!double.IsFinite(seconds)||seconds<=0||seconds>60)throw new AdminValidation("Video tối đa 60 giây.");
        var tracks=children.FirstOrDefault(e=>e.Id==0x1654AE6B)??throw new AdminValidation("WebM thiếu luồng video.");
        foreach(var entry in Elements(bytes,tracks.Start,tracks.End).Where(e=>e.Id==0xAE))
        {
            var fields=Elements(bytes,entry.Start,entry.End).ToArray();var type=fields.FirstOrDefault(e=>e.Id==0x83);
            if(type is null||Unsigned(bytes,type)!=1)continue;
            var number=fields.FirstOrDefault(e=>e.Id==0xD7);
            if(number is null||Unsigned(bytes,number)==0)throw new AdminValidation("WebM thiếu mã luồng video.");
            var video=fields.FirstOrDefault(e=>e.Id==0xE0);if(video is null)continue;
            var dimensions=Elements(bytes,video.Start,video.End).ToArray();var w=dimensions.FirstOrDefault(e=>e.Id==0xB0);var h=dimensions.FirstOrDefault(e=>e.Id==0xBA);
            if(w is null||h is null)continue;var width=Unsigned(bytes,w);var height=Unsigned(bytes,h);
            if(width==0||height==0||Math.Min(width,height)>1080||Math.Max(width,height)>1920)throw new AdminValidation("Video tối đa 1080p (1920×1080 hoặc 1080×1920).");
            if(!HasVideoBlock(bytes,children,Unsigned(bytes,number)))throw new AdminValidation("WebM không có dữ liệu khung hình video.");
            return new(seconds,(int)width,(int)height);
        }
        throw new AdminValidation("WebM không có luồng video hợp lệ.");
    }
}
