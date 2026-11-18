using System.Diagnostics;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json.Nodes;
using Bcv.Api.Modules.Engagement;
using Npgsql;

var raw = Environment.GetEnvironmentVariable("BCV_CHECK_CONNECTION") ?? throw new Exception("Set BCV_CHECK_CONNECTION.");
var settings = new NpgsqlConnectionStringBuilder(raw);
if (settings.Host is not ("localhost" or "127.0.0.1" or "::1") || settings.Database != "vietnam_explorer_dev_v2")
    throw new Exception("Only local vietnam_explorer_dev_v2 is permitted; never bcv.");
// Remove only accounts owned by this fixture, including remnants of interrupted runs.
var schema = "bcv_quiz_check_" + Guid.NewGuid().ToString("N");
await using var root = NpgsqlDataSource.Create(raw);
if (args.Contains("--live-question-filters"))
{
    var sessions = new Bcv.Api.Modules.Account.Infrastructure.FileSessionStore(Path.GetFullPath("output/readonly-filter-sessions.json"),Microsoft.Extensions.Logging.Abstractions.NullLogger<Bcv.Api.Modules.Account.Infrastructure.FileSessionStore>.Instance);
    var repository = new Bcv.Api.Modules.Admin.AdminRepository(root,sessions);
    long regionId,placeId; int regionCount,placeCount;
    await using (var command=root.CreateCommand("SELECT p.region_id,p.place_id,(SELECT count(*)::int FROM place_question q JOIN place p2 USING(place_id) WHERE p2.region_id=p.region_id),(SELECT count(*)::int FROM place_question q WHERE q.place_id=p.place_id) FROM place p WHERE EXISTS(SELECT 1 FROM place_question q WHERE q.place_id=p.place_id) ORDER BY p.place_id LIMIT 1"))
    await using (var reader=await command.ExecuteReaderAsync())
    { if(!await reader.ReadAsync()) throw new Exception("No question fixtures in v2.");regionId=reader.GetInt64(0);placeId=reader.GetInt64(1);regionCount=reader.GetInt32(2);placeCount=reader.GetInt32(3); }
    var region=(System.Text.Json.JsonElement)await repository.List("questions",null,null,regionId,null,1,100,default);
    var place=(System.Text.Json.JsonElement)await repository.List("questions",null,null,regionId,null,1,100,default,placeId:placeId);
    if(region.GetProperty("total").GetInt32()!=regionCount || regionCount==0)throw new Exception("Live region filter mismatch.");
    if(place.GetProperty("total").GetInt32()!=placeCount || placeCount==0)throw new Exception("Live place filter mismatch.");
    Console.WriteLine($"PASS: live v2 region {regionId}: {regionCount} questions; region + place {placeId}: {placeCount} questions. Read-only verification.");
    return;
}

async Task Exec(string sql) { await using var c = root.CreateCommand(sql); await c.ExecuteNonQueryAsync(); }
async Task CleanupFixtures(string fixtureEmail)
{
    await using var connection=await root.OpenConnectionAsync();await using var transaction=await connection.BeginTransactionAsync();
    await using(var notifications=new NpgsqlCommand("DELETE FROM public.notification WHERE user_id IN(SELECT user_id FROM public.app_user WHERE email=$1 OR email=$2)",connection,transaction))
    {notifications.Parameters.AddWithValue(fixtureEmail);notifications.Parameters.AddWithValue(fixtureEmail.Replace("@","_other@"));await notifications.ExecuteNonQueryAsync();}
    await using(var account=new NpgsqlCommand("DELETE FROM public.app_user WHERE email=$1 OR email=$2",connection,transaction))
    {account.Parameters.AddWithValue(fixtureEmail);account.Parameters.AddWithValue(fixtureEmail.Replace("@","_other@"));await account.ExecuteNonQueryAsync();}
    await transaction.CommitAsync();
}
void Check(bool value, string message) { if (!value) throw new Exception(message); Console.WriteLine("PASS: " + message); }
Check(CultureQuiz.UnlockSteps(101,0,4)==21 && CultureQuiz.UnlockPercent(3,4)==80, "Quiz thresholds round upward and spread along the journey");
var fixtureEmail = schema + "@example.test";
Process? server = null;
try
{
    await CleanupFixtures(fixtureEmail);
    await Exec($"CREATE SCHEMA {schema}");
    var tables = new List<string>();
    await using (var command = root.CreateCommand("SELECT tablename FROM pg_tables WHERE schemaname='public'"))
    await using (var reader = await command.ExecuteReaderAsync()) while(await reader.ReadAsync()) tables.Add(reader.GetString(0));
    foreach(var table in tables) await Exec($"CREATE TABLE {schema}.\"{table}\" (LIKE public.\"{table}\" INCLUDING ALL)");
    settings.SearchPath = schema + ",public";
    await using var db = NpgsqlDataSource.Create(settings.ConnectionString);
    async Task<object?> Scalar(string sql) { await using var c=db.CreateCommand(sql);return await c.ExecuteScalarAsync(); }
    await Scalar("INSERT INTO region(region_id,name,status) VALUES(1,'Quiz fixture','ACTIVE'); INSERT INTO place(place_id,region_id,place_type_id,name,latitude,longitude,status) VALUES(1,1,101,'Quiz fixture',21,105,'ACTIVE'); INSERT INTO quest(name,quest_type,condition_type,target_value,cycle_type,status,reward_points) VALUES('Quiz fixture','WEEKLY','QUIZ_CORRECT',2,'WEEKLY','ACTIVE',10); INSERT INTO explorer_reward_config SELECT * FROM public.explorer_reward_config;");
    var apiDll=Path.Combine(AppContext.BaseDirectory,"Bcv.Api.dll");
    var start=new ProcessStartInfo("dotnet") { UseShellExecute=false, RedirectStandardOutput=true, RedirectStandardError=true };
    start.ArgumentList.Add(apiDll);
    start.Environment["ConnectionStrings__Bcv"]=settings.ConnectionString;
    start.Environment["ASPNETCORE_URLS"]="http://127.0.0.1:5085";
    start.Environment["PORT"]="";
    start.Environment["Database__ApplyStartupMigrations"]="false";
    start.Environment["Media__MaintenanceEnabled"]="false";
    start.Environment["Sessions__FilePath"]=Path.GetFullPath("output/quiz-check-sessions.json");
    server=Process.Start(start)!;server.BeginOutputReadLine();server.BeginErrorReadLine();
    using var http=new HttpClient { BaseAddress=new Uri("http://127.0.0.1:5085"), Timeout=TimeSpan.FromSeconds(20) };
    for(int i=0;i<60;i++) { try { var health=await http.GetAsync("/health"); if(health.IsSuccessStatusCode) break; } catch(HttpRequestException) { } await Task.Delay(250); }
    Check((await http.GetAsync("/api/journeys/current/quiz")).StatusCode==HttpStatusCode.Unauthorized,"Quiz requires authentication");
    var registration=await http.PostAsJsonAsync("/api/account/register",new {email=fixtureEmail,password="Quiz-test-123456!",confirmPassword="Quiz-test-123456!"});
    registration.EnsureSuccessStatusCode();var account=JsonNode.Parse(await registration.Content.ReadAsStringAsync())!;
    http.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer",account["token"]!.GetValue<string>());
    await using (var copy = db.CreateCommand("INSERT INTO app_user SELECT * FROM public.app_user WHERE email=$1")) { copy.Parameters.AddWithValue(fixtureEmail); await copy.ExecuteNonQueryAsync(); }
    var userId=Convert.ToInt64(await Scalar("SELECT user_id FROM app_user LIMIT 1"));
    async Task<JsonNode> State() => JsonNode.Parse(await http.GetStringAsync("/api/journeys/current/quiz"))!;
    Check(!(await State())["available"]!.GetValue<bool>(),"No active journey returns an empty state");
    await Scalar($"UPDATE app_user SET role='ADMIN' WHERE user_id={userId}; INSERT INTO journey(user_id,start_latitude,start_longitude,destination_place_id,distance,required_steps,recorded_steps,status) VALUES({userId},21,105,1,1,101,0,'IN_PROGRESS')");
    var body=new {place_id=1,question="Địa danh văn hóa?",option_a="A",option_b="B",option_c="C",option_d="D",answer_index=0,difficulty=1,explanation="Fixture explanation",status="ACTIVE"};
    var create=await http.PostAsJsonAsync("/api/admin/data/questions",body);
    if (!create.IsSuccessStatusCode) Console.WriteLine(await create.Content.ReadAsStringAsync()); Check(create.IsSuccessStatusCode,"Admin can create a question using numeric answer and difficulty");
    await Scalar("INSERT INTO place_question(place_id,question,option_a,option_b,option_c,option_d,answer_index,status) VALUES(1,'Second fixture?','A','B','C','D',0,'ACTIVE'),(1,'Hidden fixture?','A','B','C','D',0,'DRAFT')");
    async Task<JsonNode> Search(string query) => JsonNode.Parse(await http.GetStringAsync("/api/admin/data/questions?"+query))!;
    Check((await Search("q=dia%20danh"))["total"]!.GetValue<int>()==1,"Search ignores Vietnamese accents including đ");
    Check((await Search("q=HOA%20DIA"))["total"]!.GetValue<int>()==1,"Search matches multiple words regardless of order or case");
    Check((await Search("q=quiz%20fixture"))["total"]!.GetValue<int>()==3,"Search includes destination names");
    Check((await Search("q=explanation"))["total"]!.GetValue<int>()==1,"Search includes explanations");
    Check((await Search("placeId=1&regionId=1&difficulty=1&status=ACTIVE"))["total"]!.GetValue<int>()==2,"Place, region, difficulty and status filters combine");
    Check((await Search("placeId=2"))["total"]!.GetValue<int>()==0,"Place filter excludes other destinations");
    Check((await Search("q=%25"))["total"]!.GetValue<int>()==0,"Percent is literal rather than an SQL wildcard");
    var firstPage=await Search("pageSize=1&sort=oldest");var secondPage=await Search("pageSize=1&page=2&sort=oldest");
    Check(firstPage["total"]!.GetValue<int>()==3 && firstPage["items"]![0]!["question_id"]!.GetValue<long>() < secondPage["items"]![0]!["question_id"]!.GetValue<long>(),"Sorting and pagination retain the full filtered count");
    Check((await http.GetAsync("/api/admin/data/questions?difficulty=4")).StatusCode==HttpStatusCode.BadRequest,"Invalid question filters return validation errors");
    var state=await State();var stops=state["stops"]!.AsArray();
    Check(stops.Count==2 && stops.All(q=>q!["text"]==null && q["options"]==null && q["answer"]==null),"Draft questions excluded and locked questions reveal no content or answers");
    var key=stops[0]!["key"]!.GetValue<string>();
    async Task<HttpResponseMessage> Answer(string k,string a) => await http.PostAsJsonAsync("/api/journeys/current/quiz",new {key=k,answer=a});
    Check((await Answer(key,"A")).StatusCode==HttpStatusCode.Conflict,"Answer before threshold is rejected");
    await Scalar("UPDATE journey SET recorded_steps=34");
    Check((await Answer(key,"invalid")).StatusCode==HttpStatusCode.BadRequest,"Unknown answer rejected");
    var concurrent=await Task.WhenAll(Answer(key,"A"),Answer(key,"A"));
    foreach (var response in concurrent) if (!response.IsSuccessStatusCode && response.StatusCode != HttpStatusCode.Conflict) Console.WriteLine(await response.Content.ReadAsStringAsync());
    Check(concurrent.Count(r=>r.IsSuccessStatusCode)==1 && concurrent.Count(r=>r.StatusCode==HttpStatusCode.Conflict)==1,"Concurrent submissions award once");
    Check(Convert.ToInt64(await Scalar("SELECT sum(points) FROM explorer_score_event"))==20,"Correct answer awards 20 points in the v2 ledger");
    await Scalar("UPDATE journey SET recorded_steps=68");
    var second=stops[1]!["key"]!.GetValue<string>();var answer=await Answer(second,"A");answer.EnsureSuccessStatusCode();
    Check(Convert.ToInt64(await Scalar("SELECT sum(points) FROM explorer_score_event"))==100,"Two correct answers award 40 + 50 mastery + 10 quest points");
    Check(Convert.ToInt64(await Scalar("SELECT count(*) FROM user_place_mastery"))==1 && Convert.ToInt64(await Scalar("SELECT recorded_steps FROM journey"))==68,"Mastery recorded without adding walking steps");
    Check(Convert.ToInt64(await Scalar("SELECT count(*) FROM explorer_score_event WHERE event_type IN ('QUIZ','MASTERY') AND region_id=1"))==3,"Quiz rewards count toward the destination region");
    Check((await State())["mastered"]!.GetValue<bool>(),"Mastery is available to the app after reload");
    await Scalar("UPDATE journey SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP");
    Check((await Answer(second,"A")).StatusCode==HttpStatusCode.BadRequest,"Ended journey cannot accept answers");
    await Scalar($"INSERT INTO journey(user_id,start_latitude,start_longitude,destination_place_id,distance,required_steps,recorded_steps,status) VALUES({userId},21,105,1,1,101,101,'IN_PROGRESS')");
    var wrong=await Answer(key,"B");wrong.EnsureSuccessStatusCode();
    var wrongResult=JsonNode.Parse(await wrong.Content.ReadAsStringAsync())!;
    Check(!wrongResult["correct"]!.GetValue<bool>() && wrongResult["answer"]!.GetValue<string>()=="A" && wrongResult["explanation"]!.GetValue<string>()=="Fixture explanation","Wrong answer reveals the correct answer and explanation");
    Check(Convert.ToInt64(await Scalar("SELECT sum(points) FROM explorer_score_event"))==100,"Wrong answer does not award points or repeat mastery");
    Check((await Answer(key,"A")).StatusCode==HttpStatusCode.Conflict,"Wrong answer cannot be retried to farm rewards");
    await CollectionChallengeChecks.Run(http,db,userId,fixtureEmail);
    Console.WriteLine("All quiz, collection and challenge integration checks passed in disposable v2 schema.");
    if (args.Contains("--ui-preview"))
    {
        await Scalar($"UPDATE place SET status='ACTIVE',name='Văn Miếu – Quốc Tử Giám' WHERE place_id=1; UPDATE region SET name='Hà Nội' WHERE region_id=1; INSERT INTO card(card_id,place_id,name,image_key,status) VALUES(900001,1,'Dấu ấn Văn Miếu',(SELECT image_key FROM public.card WHERE image_key IS NOT NULL LIMIT 1),'ACTIVE'); INSERT INTO collection(collection_id,name,description,status) VALUES(900001,'Di sản văn hóa Việt Nam','Những câu chuyện trên mỗi bước chân','ACTIVE'); INSERT INTO collection_card(collection_id,card_id) VALUES(900001,900001); INSERT INTO user_card(user_id,card_id) VALUES({userId},900001)");
        var file=Path.GetFullPath("output/dev-v2/collection-ui-fixture.json");
        var finish=Path.GetFullPath("output/dev-v2/collection-ui-finish.flag");
        if(File.Exists(finish))File.Delete(finish);
        await File.WriteAllTextAsync(file,new JsonObject{["token"]=account["token"]!.GetValue<string>(),["userId"]=userId}.ToJsonString());
        Console.WriteLine("UI_PREVIEW_READY: isolated API on 5085; waiting for UI verification.");
        try { var deadline=DateTime.UtcNow.AddMinutes(8);while(!File.Exists(finish)&&DateTime.UtcNow<deadline)await Task.Delay(500); }
        finally {File.Delete(file);if(File.Exists(finish))File.Delete(finish);}
    }

}
finally
{
    if(server is {HasExited:false}) { server.Kill(true);await server.WaitForExitAsync(); }
    await Exec($"DROP SCHEMA IF EXISTS {schema} CASCADE");
    await CleanupFixtures(fixtureEmail);
}
