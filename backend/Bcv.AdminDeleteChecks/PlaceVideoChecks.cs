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
    public static async Task Run(NpgsqlDataSource db,HttpClient client)
    {
        void Check(bool ok,string message){if(!ok)throw new Exception(message);}
        async Task Exec(string sql){await using var cmd=db.CreateCommand(sql);await cmd.ExecuteNonQueryAsync();}
        async Task<long> Count(string sql){await using var cmd=db.CreateCommand(sql);return Convert.ToInt64(await cmd.ExecuteScalarAsync());}
        foreach(var invalid in new[]{new byte[24],Fixture(31),Fixture(10,1920,1080),Fixture(0),Fixture()[..30]})
        {try{PlaceVideos.Inspect(invalid);throw new Exception("Invalid MP4 accepted");}catch(AdminValidation){}}
        Check(PlaceVideos.Inspect(Fixture()).Width==1280,"Valid MP4 metadata rejected");
        client.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer","950005");
        var ids=new List<long>();
        for(var i=0;i<5;i++)
        {
            using var content=new ByteArrayContent(Fixture());content.Headers.ContentType=new MediaTypeHeaderValue("video/mp4");
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
