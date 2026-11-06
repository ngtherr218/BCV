using System.Buffers.Binary;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.CommunityMedia;
using Npgsql;

static class PlaceVideoChecks
{
    static byte[] Box(string type,params byte[][] children)
    {
        var payload=children.SelectMany(x=>x).ToArray();var result=new byte[payload.Length+8];BinaryPrimitives.WriteUInt32BigEndian(result,(uint)result.Length);Encoding.ASCII.GetBytes(type).CopyTo(result,4);payload.CopyTo(result,8);return result;
    }
    public static byte[] Fixture(int seconds=10,int width=1280,int height=720)
    {
        var mvhd=new byte[20];BinaryPrimitives.WriteUInt32BigEndian(mvhd.AsSpan(12),1000);BinaryPrimitives.WriteUInt32BigEndian(mvhd.AsSpan(16),(uint)seconds*1000);
        var tkhd=new byte[84];BinaryPrimitives.WriteUInt32BigEndian(tkhd.AsSpan(76),(uint)width<<16);BinaryPrimitives.WriteUInt32BigEndian(tkhd.AsSpan(80),(uint)height<<16);
        var handler=new byte[12];Encoding.ASCII.GetBytes("vide").CopyTo(handler,8);
        return new[]{Box("ftyp",Encoding.ASCII.GetBytes("isom0000")),Box("moov",Box("mvhd",mvhd),Box("trak",Box("tkhd",tkhd),Box("mdia",Box("hdlr",handler)))),Box("mdat",new byte[]{0})}.SelectMany(x=>x).ToArray();
    }
    static byte[] Ebml(uint id,params byte[][] children)
    {
        var key=new byte[4];BinaryPrimitives.WriteUInt32BigEndian(key,id);
        var payload=children.SelectMany(x=>x).ToArray();
        return key.SkipWhile(x=>x==0).Concat(new[]{(byte)(0x80|payload.Length)}).Concat(payload).ToArray();
    }
    static byte[] Webm(int seconds=60,int width=1920,int height=1080)
    {
        var duration=new byte[8];BinaryPrimitives.WriteInt64BigEndian(duration,BitConverter.DoubleToInt64Bits(seconds*1000d));
        var w=new byte[2];var h=new byte[2];BinaryPrimitives.WriteUInt16BigEndian(w,(ushort)width);BinaryPrimitives.WriteUInt16BigEndian(h,(ushort)height);
        return Ebml(0x1A45DFA3).Concat(Ebml(0x18538067,Ebml(0x1549A966,Ebml(0x4489,duration)),Ebml(0x1654AE6B,Ebml(0xAE,Ebml(0x83,new byte[]{1}),Ebml(0xE0,Ebml(0xB0,w),Ebml(0xBA,h)))))).ToArray();
    }
    public static async Task Run(NpgsqlDataSource db,HttpClient client)
    {
        void Check(bool ok,string message){if(!ok)throw new Exception(message);}
        async Task Exec(string sql){await using var cmd=db.CreateCommand(sql);await cmd.ExecuteNonQueryAsync();}
        async Task<long> Count(string sql){await using var cmd=db.CreateCommand(sql);return Convert.ToInt64(await cmd.ExecuteScalarAsync());}
        foreach(var invalid in new[]{new byte[24],Fixture(61),Fixture(10,3840,2160),Fixture(0),Fixture()[..30]})
        {try{PlaceVideos.Inspect(invalid);throw new Exception("Invalid MP4 accepted");}catch(AdminValidation){}}
        Check(PlaceVideos.Inspect(Fixture(60,1920,1080)).Duration==60,"60s 1080p rejected");
        Check(PlaceVideos.Inspect(Fixture(60,1080,1920)).Height==1920,"Portrait 1080p rejected");
        Check(PlaceVideos.Inspect(Fixture()).Width==1280,"Valid MP4 metadata rejected");
        Check(PlaceVideos.MaxBytes==100*1024*1024,"Wrong video byte limit");
        try{PlaceVideos.Inspect(new byte[PlaceVideos.MaxBytes+1]);throw new Exception("Video above 100 MB accepted");}catch(AdminValidation){}
        Check(WebmMetadata.Inspect(Webm()).Duration==60,"60s 1080p WebM rejected");
        foreach(var invalid in new[]{Webm(61),Webm(60,3840,2160),Webm()[..12]})
        {try{WebmMetadata.Inspect(invalid);throw new Exception("Invalid WebM accepted");}catch(AdminValidation){}}
        client.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer","950005");
        var ids=new List<long>();
        for(var i=0;i<5;i++)
        {
            using var content=new ByteArrayContent(i==0?Fixture(60,1920,1080):Fixture());content.Headers.ContentType=new MediaTypeHeaderValue("video/mp4");
            using var r=await client.PostAsync("/api/admin/place-videos",content);Check(r.IsSuccessStatusCode,"Official video upload failed: "+await r.Content.ReadAsStringAsync());
            ids.Add((await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("videoId").GetInt64());
        }
        async Task Attach(long actor,long[] selected)
        {
            await using var conn=await db.OpenConnectionAsync();await using var tx=await conn.BeginTransactionAsync();
            await using(var cmd=new NpgsqlCommand("SELECT place_id FROM place WHERE place_id=910001 FOR UPDATE",conn,tx))await cmd.ExecuteScalarAsync();
            await PlaceVideos.SaveAsync(conn,tx,910001,actor,JsonSerializer.SerializeToElement(new{video_ids=selected}),default);await tx.CommitAsync();
        }
        try
        {
            await CommunityMediaEndpoints.EnsureAsync(db); // A second startup must accept existing 60-second rows.
            var large=Fixture(60,1920,1080);var mediaOffset=large.Length-9;
            Array.Resize(ref large,31*1024*1024);BinaryPrimitives.WriteUInt32BigEndian(large.AsSpan(mediaOffset),(uint)(large.Length-mediaOffset));
            using(var content=new ByteArrayContent(large))
            {
                content.Headers.ContentType=new MediaTypeHeaderValue("video/mp4");
                using var response=await client.PostAsync("/api/admin/place-videos",content);
                Check(response.IsSuccessStatusCode,"Upload above Kestrel's default 30 MB limit failed: "+await response.Content.ReadAsStringAsync());
            }
            foreach(var pair in new[]{("video/mp4",Fixture(60,1920,1080),HttpStatusCode.OK),("video/mp4",Fixture(61),HttpStatusCode.BadRequest),("video/webm",Webm(),HttpStatusCode.OK),("video/webm",Webm(61),HttpStatusCode.BadRequest)})
            {
                using var content=new ByteArrayContent(pair.Item2);content.Headers.ContentType=new MediaTypeHeaderValue(pair.Item1);
                using var response=await client.PostAsync("/api/places/910001/media",content);
                Check(response.StatusCode==pair.Item3,"Community video limits inconsistent: "+await response.Content.ReadAsStringAsync());
            }
            Console.WriteLine("PASS 100 MB cap, actual 31 MB HTTP upload, MP4/WebM 60s 1080p limits and repeated migration with 60s data");
            try{await Attach(950004,ids.ToArray());throw new Exception("Other admin's staging videos were attached");}catch(AdminValidation){}
            await Attach(950005,ids.ToArray());
            Check(await Count("SELECT count(*) FROM place_video WHERE place_id=910001")==5,"Official videos not attached");
            await Exec("INSERT INTO community_media(media_id,user_id,place_id,mime_type,media_data) OVERRIDING SYSTEM VALUE VALUES(950010,950001,910001,'video/mp4',decode('303030306674797069736f6d','hex'))");
            using(var r=await client.PostAsJsonAsync("/api/admin/community-media/950010/review",new{status="APPROVED",expectedStatus="PENDING"}))Check(r.StatusCode==HttpStatusCode.BadRequest,"Sixth public video accepted");
            await Attach(950005,ids.Take(4).ToArray());
            using(var r=await client.PostAsJsonAsync("/api/admin/community-media/950010/review",new{status="APPROVED",expectedStatus="PENDING"}))Check(r.IsSuccessStatusCode,"Fifth combined video rejected");
            client.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer","950001");
            using(var r=await client.GetAsync($"/api/explore/places/910001/videos/{ids[0]}"))Check(r.StatusCode==HttpStatusCode.NotFound,"Locked place video leaked");
            await Exec("UPDATE region SET status='ACTIVE' WHERE region_id=910001; INSERT INTO user_discovered_place(user_id,place_id) VALUES(950001,910001)");
            using(var r=await client.GetAsync($"/api/explore/places/910001/videos/{ids[0]}"))Check(r.IsSuccessStatusCode,"Unlocked video inaccessible");
            client.DefaultRequestHeaders.Range=new RangeHeaderValue(0,9);
            using(var r=await client.GetAsync($"/api/explore/places/910001/videos/{ids[0]}"))Check(r.StatusCode==HttpStatusCode.PartialContent,"Video seek/range unavailable");
            client.DefaultRequestHeaders.Range=null;
            client.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer","950005");
            await Exec("INSERT INTO place(place_id,region_id,place_type_id,name,latitude,longitude,status) OVERRIDING SYSTEM VALUE VALUES(950011,910001,910001,'Card filter fixture',21,105,'DRAFT')");
            var repo=new AdminRepository(db,new NoSessions());
            object Card(long place)=>new{name="Card filter",place_id=place,status="DRAFT"};
            try{await repo.Save("cards",null,JsonSerializer.SerializeToElement(Card(910001)),950005,default);throw new Exception("Duplicate card place accepted");}catch(AdminValidation){}
            var created=JsonSerializer.SerializeToElement(await repo.Save("cards",null,JsonSerializer.SerializeToElement(Card(950011)),950005,default)).GetProperty("id").GetInt64();
            var lookups=JsonSerializer.SerializeToElement(await repo.Lookups(default));
            Check(lookups.GetProperty("places").EnumerateArray().Single(p=>p.GetProperty("id").GetInt64()==950011).GetProperty("card_id").GetInt64()==created,"Card availability missing in lookups");
            await repo.Delete("cards",created,default);await repo.Delete("places",950011,default);
            Console.WriteLine("PASS MP4 size/duration/resolution validation, staging ownership, attach, combined 5-video quota, discovery access, Range playback and card-place availability");
        }
        finally
        {
            client.DefaultRequestHeaders.Range=null;client.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer","950005");
            await Exec("DELETE FROM place_video WHERE created_by=950005; DELETE FROM user_discovered_place WHERE user_id=950001; UPDATE region SET status='DRAFT' WHERE region_id=910001");
        }
    }
    sealed class NoSessions:Bcv.Api.Modules.Account.Application.ISessionStore
    {
        public Bcv.Api.Modules.Account.Domain.PublicAccount? Get(string? token)=>null;
        public Bcv.Api.Modules.Account.Application.AccountSession Create(Bcv.Api.Modules.Account.Domain.AccountUser user)=>throw new NotSupportedException();
        public void Revoke(string? token){}public void RevokeAll(long id){}
    }
}
