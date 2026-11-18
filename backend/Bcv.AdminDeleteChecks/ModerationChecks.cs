using Bcv.Api.Modules.Shared.Infrastructure;
using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Bcv.Api.Modules.PlaceMedia;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Npgsql;

/// <summary>
/// Hosts the place-media endpoints and official place-video admin API against the isolated schema,
/// then runs <see cref="PlaceVideoChecks"/>. (Community uploads and their moderation were retired.)
/// </summary>
static class ModerationChecks
{
    public static async Task Run(NpgsqlDataSource db)
    {
        async Task Exec(string sql){await using var c=db.CreateCommand(sql);await c.ExecuteNonQueryAsync();}
        await DbMigrations.RunAsync(db, "030", "029");
        await DbMigrations.RunAsync(db, "030", "029"); // Re-runnable.
        await Exec("""
            INSERT INTO app_user(user_id,full_name,email,password_hash,role,status) OVERRIDING SYSTEM VALUE
            SELECT n,'Media fixture '||n,'media-'||n||'@example.invalid','unused',CASE WHEN n=950005 THEN 'ADMIN' ELSE 'USER' END,'ACTIVE' FROM generate_series(950001,950005) n;
            UPDATE place SET status='ACTIVE' WHERE place_id=910001;
            UPDATE region SET status='ACTIVE' WHERE region_id=910001;
            """);
        try
        {
            var builder=WebApplication.CreateBuilder();builder.Logging.ClearProviders();builder.WebHost.UseUrls("http://127.0.0.1:0");
            builder.Services.AddSingleton(db);builder.Services.AddSingleton<ISessionStore,ModerationSessions>();builder.Services.AddScoped<AdminRepository>();
            await using var app=builder.Build();app.MapPlaceMedia();
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
            await PlaceVideoChecks.Run(db,client);
            await app.StopAsync();
        }
        finally
        {
            await Exec("DELETE FROM notification WHERE user_id BETWEEN 950001 AND 950005; DELETE FROM user_discovered_place WHERE user_id BETWEEN 950001 AND 950005; DELETE FROM app_user WHERE user_id BETWEEN 950001 AND 950005; UPDATE place SET status='DRAFT' WHERE place_id=910001; UPDATE region SET status='DRAFT' WHERE region_id=910001");
        }
    }
    sealed class ModerationSessions:ISessionStore
    {
        public PublicAccount? Get(string? token)=>long.TryParse(token,out var id)&&id>=950001&&id<=950005?new(id,"Fixture","fixture@example.invalid",id==950005?"ADMIN":"USER"):null;
        public AccountSession Create(AccountUser user)=>throw new NotSupportedException();
        public void Revoke(string? token){}public void RevokeAll(long id){}
    }
}
