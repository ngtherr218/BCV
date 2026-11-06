using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Bcv.Api.Modules.CommunityMedia;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Npgsql;

static class ModerationChecks
{
    public static async Task Run(NpgsqlDataSource db)
    {
        void Check(bool ok,string message){if(!ok)throw new Exception(message);}
        async Task Exec(string sql){await using var c=db.CreateCommand(sql);await c.ExecuteNonQueryAsync();}
        async Task<long> Scalar(string sql){await using var c=db.CreateCommand(sql);return Convert.ToInt64(await c.ExecuteScalarAsync());}
        await CommunityMediaEndpoints.EnsureAsync(db);
        await CommunityMediaEndpoints.EnsureAsync(db);
        await Exec("""
            INSERT INTO app_user(user_id,full_name,email,password_hash,role,status) OVERRIDING SYSTEM VALUE
            SELECT n,'Moderation fixture '||n,'moderation-'||n||'@example.invalid','unused',CASE WHEN n=950005 THEN 'ADMIN' ELSE 'USER' END,'ACTIVE' FROM generate_series(950001,950005) n;
            INSERT INTO quest(quest_id,name,quest_type,condition_type,target_value,cycle_type,status) OVERRIDING SYSTEM VALUE VALUES(950001,'Media fixture','DAILY','MEDIA_APPROVED',1,'DAILY','ACTIVE');
            UPDATE place SET status='ACTIVE' WHERE place_id=910001;
            INSERT INTO community_media(media_id,user_id,place_id,mime_type,media_data) OVERRIDING SYSTEM VALUE VALUES(950001,950001,910001,'image/png',decode('89504e470d0a1a0a00000000','hex'));
            INSERT INTO community_media(media_id,user_id,place_id,mime_type,media_data) OVERRIDING SYSTEM VALUE VALUES(950002,950001,910001,'image/png',decode('89504e470d0a1a0a00000000','hex'));
            INSERT INTO place(place_id,region_id,place_type_id,name,latitude,longitude,status) OVERRIDING SYSTEM VALUE VALUES(950020,910001,910001,'Other place media fixture',21,105,'ACTIVE');
            INSERT INTO community_media(media_id,user_id,place_id,mime_type,media_data,status) OVERRIDING SYSTEM VALUE VALUES(950020,950002,950020,'image/png',decode('89504e470d0a1a0a00000000','hex'),'APPROVED');
            """);
        try
        {
            var builder=WebApplication.CreateBuilder();builder.Logging.ClearProviders();builder.WebHost.UseUrls("http://127.0.0.1:0");
            builder.Services.AddSingleton(db);builder.Services.AddSingleton<ISessionStore,ModerationSessions>();builder.Services.AddScoped<AdminRepository>();
            await using var app=builder.Build();app.MapCommunityMedia();
            var videoAdmin=app.MapGroup("/api/admin");
            videoAdmin.AddEndpointFilter(async(context,next)=>{
                var user=Bcv.Api.Modules.Account.Api.CurrentUser.Get(context.HttpContext.Request,context.HttpContext.RequestServices.GetRequiredService<ISessionStore>());
                if(user is null)return Microsoft.AspNetCore.Http.Results.Unauthorized();
                if(user.Role!="ADMIN")return Microsoft.AspNetCore.Http.Results.StatusCode(403);
                context.HttpContext.Items["AdminId"]=user.UserId;
                try{return await next(context);}catch(AdminValidation error){return Microsoft.AspNetCore.Http.Results.BadRequest(new{message=error.Message});}
            });
            PlaceVideos.MapEndpoints(videoAdmin,app);await app.StartAsync();
            using var client=new HttpClient{BaseAddress=new Uri(app.Urls.Single())};
            void Token(string? token){client.DefaultRequestHeaders.Authorization=token is null?null:new AuthenticationHeaderValue("Bearer",token);}
            async Task Send(string url,object body,HttpStatusCode expected){using var response=await client.PostAsJsonAsync(url,body);Check(response.StatusCode==expected,$"{url}: {response.StatusCode}, expected {expected}: {await response.Content.ReadAsStringAsync()}");}
            using(var r=await client.GetAsync("/api/admin/community-media"))Check(r.StatusCode==HttpStatusCode.Unauthorized,"Anonymous admin access");
            await Send("/api/admin/community-media/950001/review",new{status="APPROVED"},HttpStatusCode.Unauthorized);
            Token("950001");using(var r=await client.GetAsync("/api/admin/community-media"))Check(r.StatusCode==HttpStatusCode.Forbidden,"User admin access");
            Token("950005");
            await Send("/api/admin/community-media/950001/review",new{status="REJECTED",note="  ",expectedStatus="PENDING"},HttpStatusCode.BadRequest);
            Check(await Scalar("SELECT count(*) FROM notification WHERE user_id=950001")==0,"Invalid review emitted notification");
            await Send("/api/admin/community-media/950002/review",new{status="REJECTED",note="Ảnh không phù hợp",expectedStatus="PENDING"},HttpStatusCode.OK);
            await Send("/api/admin/community-media/950001/review",new{status="APPROVED",note="",expectedStatus="PENDING"},HttpStatusCode.OK);
            await Send("/api/admin/community-media/950001/review",new{status="APPROVED",note="",expectedStatus="PENDING"},HttpStatusCode.BadRequest);
            Check(await Scalar("SELECT progress_value FROM quest_progress WHERE user_id=950001 AND quest_id=950001")==1,"First approval not counted exactly once");
            Console.WriteLine("PASS moderation authentication, mandatory rejection reason, first approval task and duplicate review guard");
            var approvedForPlace=await client.GetFromJsonAsync<JsonElement>("/api/admin/community-media?status=APPROVED&placeId=910001");
            Check(approvedForPlace.GetProperty("total").GetInt32()==1&&approvedForPlace.GetProperty("items")[0].GetProperty("mediaId").GetInt64()==950001,"Place detail does not isolate its approved community content");
            var otherPlace=await client.GetFromJsonAsync<JsonElement>("/api/admin/community-media?status=APPROVED&placeId=950020");
            Check(otherPlace.GetProperty("total").GetInt32()==1&&otherPlace.GetProperty("items")[0].GetProperty("mediaId").GetInt64()==950020,"Other place approved content mixed into detail");
            Token("950001");
            var approvedForApp=await client.GetFromJsonAsync<JsonElement>("/api/places/910001/media?approvedOnly=true");
            Check(approvedForApp.GetProperty("items").GetArrayLength()==1&&approvedForApp.GetProperty("items")[0].GetProperty("mediaId").GetInt64()==950001,"App detail includes owner's rejected content or excludes approved image");
            Console.WriteLine("PASS web/app place detail lists approved user content only, scoped to the exact place");
            Token("950001");await Send("/api/community-media/950001/reports",new{reason="self"},HttpStatusCode.BadRequest);
            Token("950002");await Send("/api/community-media/950001/reports",new{reason="Nội dung vi phạm"},HttpStatusCode.OK);
            await Send("/api/community-media/950001/reports",new{reason="duplicate"},HttpStatusCode.BadRequest);
            Token("950003");await Send("/api/community-media/950001/reports",new{reason="Cần xem xét"},HttpStatusCode.OK);
            Token("950004");await Send("/api/community-media/950001/reports",new{reason="Cần kiểm duyệt lại"},HttpStatusCode.OK);
            using(var r=await client.GetAsync("/api/community-media/950001/content"))Check(r.StatusCode==HttpStatusCode.NotFound,"Flagged content leaked to other user");
            Token("950005");
            var listing=await client.GetFromJsonAsync<JsonElement>("/api/admin/community-media?status=&reported=true&pageSize=1");
            Check(listing.GetProperty("total").GetInt32()==1&&listing.GetProperty("items")[0].GetProperty("reportCount").GetInt32()==3,"Report filter/count/paging failed");
            var reports=await client.GetFromJsonAsync<JsonElement>("/api/admin/community-media/950001/reports");Check(reports.GetArrayLength()==3,"Report reasons missing");
            await Send("/api/admin/community-media/950001/review",new{status="APPROVED",note="Đã kiểm tra báo cáo",expectedStatus="FLAGGED"},HttpStatusCode.OK);
            Check(await Scalar("SELECT count(*) FROM media_report WHERE status='OPEN'")==0,"Restore did not resolve reports");
            Check(await Scalar("SELECT progress_value FROM quest_progress WHERE user_id=950001 AND quest_id=950001")==1,"Restore repeated task credit");
            await Send("/api/community-media/950001/reports",new{reason="Vi phạm được xác nhận"},HttpStatusCode.OK);
            await Send("/api/admin/community-media/950001/review",new{status="REMOVED",note="Cần gỡ",expectedStatus="APPROVED"},HttpStatusCode.OK);
            Check(await Scalar("SELECT count(*) FROM media_report WHERE media_id=950001 AND status='ACCEPTED'")==1,"Removing reported content must accept its report");
            Token(null);using(var r=await client.GetAsync("/api/community-media/950001/content"))Check(r.StatusCode==HttpStatusCode.NotFound,"Removed content still public");
            Token("950005");await Send("/api/admin/community-media/950001/review",new{status="APPROVED",note="Đã xử lý",expectedStatus="REMOVED"},HttpStatusCode.BadRequest);
            Check(await Scalar("SELECT progress_value FROM quest_progress WHERE user_id=950001 AND quest_id=950001")==1,"Removed restore repeated task credit");
            Console.WriteLine("PASS report uniqueness/self-report protection, threshold auto-hide, report queue, restore/remove visibility and no repeated task credit");
            await Send("/api/admin/community-media/users/950001/upload-access",new{blocked=true,note="Vi phạm quy định"},HttpStatusCode.OK);
            Token("950001");using(var bytes=new ByteArrayContent(new byte[]{137,80,78,71,13,10,26,10,0,0,0,0})){bytes.Headers.ContentType=new MediaTypeHeaderValue("image/png");using var r=await client.PostAsync("/api/places/910001/media",bytes);Check(r.StatusCode==HttpStatusCode.Forbidden,"Blocked upload accepted");}
            Token("950005");await Send("/api/admin/community-media/users/950001/upload-access",new{blocked=false,note="Được gửi trở lại"},HttpStatusCode.OK);
            Token("950001");using(var bytes=new ByteArrayContent(new byte[]{137,80,78,71,13,10,26,10,0,0,0,0})){bytes.Headers.ContentType=new MediaTypeHeaderValue("image/png");using var r=await client.PostAsync("/api/places/910001/media",bytes);Check(r.IsSuccessStatusCode,"Unblocked upload rejected");}
            Check(await Scalar("SELECT count(*) FROM notification WHERE user_id=950001 AND notification_type LIKE 'MEDIA_%'")==7,"Moderation notifications missing/duplicated");
            Console.WriteLine("PASS upload lock/unlock enforced by upload API and transactional owner notifications");
            await Exec("INSERT INTO community_media(media_id,user_id,place_id,mime_type,media_data) OVERRIDING SYSTEM VALUE VALUES(950003,950001,910001,'image/png',decode('89504e470d0a1a0a00000000','hex'))");
            Token("950005");await Send("/api/admin/community-media/950003/review",new{status="APPROVED",expectedStatus="PENDING"},HttpStatusCode.OK);
            Token("950002");await Send("/api/community-media/950003/reports",new{reason="Cần kiểm tra"},HttpStatusCode.OK);
            Token("950005");await Send("/api/admin/community-media/950003/review",new{status="APPROVED",note="Báo cáo không có căn cứ",expectedStatus="APPROVED"},HttpStatusCode.OK);
            Check(await Scalar("SELECT count(*) FROM media_report WHERE media_id=950003 AND status='OPEN'")==0,"Below-threshold report cannot be resolved while keeping content public");
            Check(await Scalar("SELECT progress_value FROM quest_progress WHERE user_id=950001 AND quest_id=950001")==2,"Keeping reported content public repeated task credit");
            await Send("/api/admin/community-media/950003/review",new{status="APPROVED",note="Lặp lại",expectedStatus="APPROVED"},HttpStatusCode.BadRequest);
            Console.WriteLine("PASS reports below auto-hide threshold can be dismissed while keeping content public without duplicate credit");
            await Send("/api/admin/community-media/users/950005/upload-access",new{blocked=true,note="Admin protected"},HttpStatusCode.BadRequest);
            await PlaceVideoChecks.Run(db,client);
            await Exec("UPDATE community_media SET delete_after=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE media_id=950002; UPDATE community_media SET created_at=CURRENT_TIMESTAMP-INTERVAL '31 days' WHERE status='PENDING' AND user_id=950001");
            await MediaMaintenance.SweepAsync(db,default);
            Check(await Scalar("SELECT count(*) FROM community_media WHERE media_id=950002 AND media_data IS NULL")==1,"Rejected file not cleaned");
            Check(await Scalar("SELECT count(*) FROM community_media WHERE status='EXPIRED' AND user_id=950001")>0,"Pending expiry not cleaned");
            Check(await Scalar("SELECT count(*) FROM media_report WHERE status='DISMISSED'")>0,"Reports not marked dismissed");
            Console.WriteLine("PASS protected admin upload access, rejected-file cleanup and pending expiry");
            await app.StopAsync();
        }
        finally
        {
            await Exec("DELETE FROM media_report; DELETE FROM community_media WHERE user_id BETWEEN 950001 AND 950005; DELETE FROM place WHERE place_id=950020; DELETE FROM notification WHERE user_id BETWEEN 950001 AND 950005; DELETE FROM quest_progress WHERE quest_id=950001; DELETE FROM quest WHERE quest_id=950001; DELETE FROM app_user WHERE user_id BETWEEN 950001 AND 950005; UPDATE place SET status='DRAFT' WHERE place_id=910001");
        }
    }
    sealed class ModerationSessions:ISessionStore
    {
        public PublicAccount? Get(string? token)=>long.TryParse(token,out var id)&&id>=950001&&id<=950005?new(id,"Fixture","fixture@example.invalid",id==950005?"ADMIN":"USER"):null;
        public AccountSession Create(AccountUser user)=>throw new NotSupportedException();
        public void Revoke(string? token){}public void RevokeAll(long id){}
    }
}
