using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json.Nodes;
using Bcv.Api.Modules.Engagement;
using Npgsql;

internal static class CollectionChallengeChecks
{
    public static async Task Run(HttpClient http,NpgsqlDataSource db,long userId,string fixtureEmail)
    {
        void Check(bool pass,string message){if(!pass)throw new Exception(message);Console.WriteLine("PASS: "+message);}
        async Task<JsonNode> Get(string path)=>JsonNode.Parse(await http.GetStringAsync(path))!;
        async Task<object?> Sql(string sql){await using var command=db.CreateCommand(sql);return await command.ExecuteScalarAsync();}
        var summary=await Get("/api/engagement/summary");
        var goal=summary["streak"]!["dailyGoal"]!.GetValue<int>();
        Check(summary["streak"]!["week"]!.AsArray().Count==7 && summary["badges"]!.AsArray().Count==13,"Challenge summary provides seven days and thirteen real-progress badges");
        var today=EngagementCore.Today();
        async Task<JsonNode> Daily(DateOnly date,int steps){var r=await http.PostAsJsonAsync("/api/activity/daily",new{date=date.ToString("yyyy-MM-dd"),steps});r.EnsureSuccessStatusCode();return JsonNode.Parse(await r.Content.ReadAsStringAsync())!;}
        await Daily(today.AddDays(-1),goal);var streak=await Daily(today,goal);
        Check(streak["current"]!.GetValue<int>()==2 && streak["todayDone"]!.GetValue<bool>(),"Daily steps create a two-day streak in Vietnam time");
        Check((await Daily(today,1))["todaySteps"]!.GetValue<int>()==goal,"Repeated or lower Health Connect reports never decrease daily steps");
        await using(var connection=await db.OpenConnectionAsync())await using(var tx=await connection.BeginTransactionAsync())
        {await CollectionActivityData.RecordJourneyStepsAsync(connection,tx,userId,100,default);await tx.CommitAsync();}
        Check((await Get("/api/engagement/summary"))["streak"]!["todaySteps"]!.GetValue<int>()==goal,"Health totals and physical journey steps are not added twice");
        Check((await http.PostAsJsonAsync("/api/activity/daily",new{date=today.AddDays(1).ToString("yyyy-MM-dd"),steps=1})).StatusCode==HttpStatusCode.BadRequest,"Future daily activity is rejected");
        var pauses=await Task.WhenAll(http.PostAsJsonAsync("/api/activity/pause",new{days=3}),http.PostAsJsonAsync("/api/activity/pause",new{days=3}));
        Check(pauses.Count(p=>p.IsSuccessStatusCode)==1 && pauses.Count(p=>p.StatusCode==HttpStatusCode.Conflict)==1,"Concurrent pause requests cannot create overlapping rest periods");
        (await http.DeleteAsync("/api/activity/pause")).EnsureSuccessStatusCode();
        Check((await Get("/api/engagement/summary"))["streak"]!["pause"] is null,"Ending a pause restores the normal streak card");
        Check((await http.GetAsync("/api/journal/places/1")).StatusCode==HttpStatusCode.NotFound,"Journal entry requires the user's own discovered place");
        Check((await http.PutAsJsonAsync("/api/journal/places/1/note",new{note="No discovery"})).StatusCode==HttpStatusCode.NotFound,"Unowned destinations cannot receive journal notes");
        await Sql($"INSERT INTO user_discovered_place(user_id,place_id,discovered_at) VALUES({userId},1,CURRENT_TIMESTAMP)");
        summary=await Get("/api/engagement/summary");
        Check(summary["passport"]!["stamped"]!.GetValue<int>()==1 && summary["passport"]!["gold"]!.GetValue<int>()==1,"Passport tiers are computed from v2 discoveries");
        Check(summary["badges"]!.AsArray().First(b=>b!["code"]!.GetValue<string>()=="FIRST_STEP")!["earned"]!.GetValue<bool>(),"Discovery unlocks its badge without issuing a second reward");
        (await http.PutAsJsonAsync("/api/journal/places/1/note",new{note="Một ngày khám phá văn hóa Việt Nam."})).EnsureSuccessStatusCode();
        var entry=await Get("/api/journal/places/1");
        Check(entry["entry"]!["note"]!.GetValue<string>().Contains("văn hóa") && (await Get("/api/journal"))["items"]!.AsArray().Count==1,"Private notes reload in the journal timeline");
        using var other=new HttpClient{BaseAddress=http.BaseAddress};
        var password="Quiz-test-123456!";
        var account=await other.PostAsJsonAsync("/api/account/register",new{email=fixtureEmail.Replace("@","_other@"),password,confirmPassword=password});account.EnsureSuccessStatusCode();
        var token=JsonNode.Parse(await account.Content.ReadAsStringAsync())!["token"]!.GetValue<string>();
        other.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer",token);
        await using(var copy=db.CreateCommand("INSERT INTO app_user SELECT * FROM public.app_user WHERE email=$1")){copy.Parameters.AddWithValue(fixtureEmail.Replace("@","_other@"));await copy.ExecuteNonQueryAsync();}
        Check((await other.GetAsync("/api/journal/places/1")).StatusCode==HttpStatusCode.NotFound && JsonNode.Parse(await other.GetStringAsync("/api/journal"))!["items"]!.AsArray().Count==0,"Another user cannot read private journal notes or timeline");
        var bytes=Convert.FromBase64String("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1sAAAAASUVORK5CYII=");
        async Task<HttpResponseMessage> Upload(HttpClient client){var content=new ByteArrayContent(bytes);content.Headers.ContentType=new MediaTypeHeaderValue("image/png");return await client.PostAsync("/api/journal/places/1/photos",content);}
        var photo=await Upload(http);photo.EnsureSuccessStatusCode();var photoId=JsonNode.Parse(await photo.Content.ReadAsStringAsync())!["photoId"]!.GetValue<long>();
        Check((await http.GetByteArrayAsync($"/api/journal/photos/{photoId}/content")).SequenceEqual(bytes),"Journal photo content is served to its owner");
        Check((await other.GetAsync($"/api/journal/photos/{photoId}/content")).StatusCode==HttpStatusCode.NotFound && (await other.DeleteAsync($"/api/journal/photos/{photoId}")).StatusCode==HttpStatusCode.NotFound,"Other users cannot read or delete journal photos");
        var concurrent=await Task.WhenAll(Enumerable.Range(0,12).Select(_=>Upload(http)));
        Check(concurrent.Count(p=>p.IsSuccessStatusCode)==11 && Convert.ToInt32(await Sql("SELECT count(*) FROM journal_photo"))==12,"Concurrent photo uploads respect the twelve-photo cap");
        (await http.DeleteAsync($"/api/journal/photos/{photoId}")).EnsureSuccessStatusCode();
        Check((await http.GetAsync($"/api/journal/photos/{photoId}/content")).StatusCode==HttpStatusCode.NotFound,"Deleted private photos are no longer served");
        await Sql("UPDATE place SET status='INACTIVE' WHERE place_id=1");
        Check((await Get("/api/journal"))["items"]!.AsArray().Count==1 && (await Get("/api/engagement/summary"))["passport"]!["stamped"]!.GetValue<int>()==1,"Retired destinations keep private journal and historical passport stamps");
        Check(Convert.ToInt64(await Sql("SELECT sum(points) FROM explorer_score_event"))==100,"Journal, daily steps and badge views do not change the v2 score ledger");
    }
}
