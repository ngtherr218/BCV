using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Npgsql;

var raw = Environment.GetEnvironmentVariable("BCV_CHECK_CONNECTION") ?? throw new Exception("Set BCV_CHECK_CONNECTION to local PostgreSQL.");
var settings = new NpgsqlConnectionStringBuilder(raw);
if (settings.Host is not ("localhost" or "127.0.0.1" or "::1")) throw new Exception("Local PostgreSQL only.");
var schema = "bcv_delete_check_" + Guid.NewGuid().ToString("N");
await using var source = NpgsqlDataSource.Create(raw);
async Task Exec(string sql) { await using var command = source.CreateCommand(sql); await command.ExecuteNonQueryAsync(); }
var tables = new List<string>();
var foreignKeys = new List<(string Table, string Name, string Definition)>();
await using (var command = source.CreateCommand("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename"))
await using (var reader = await command.ExecuteReaderAsync())
    while (await reader.ReadAsync()) tables.Add(reader.GetString(0));
await using (var command = source.CreateCommand("SELECT c.relname, con.conname, pg_get_constraintdef(con.oid) FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND con.contype='f'"))
await using (var reader = await command.ExecuteReaderAsync())
    while (await reader.ReadAsync()) foreignKeys.Add((reader.GetString(0),reader.GetString(1),reader.GetString(2)));
string Q(string value) => "\"" + value.Replace("\"", "\"\"") + "\"";
await Exec($"CREATE SCHEMA {schema}");
try
{
    await using (var conn = await source.OpenConnectionAsync())
    await using (var tx = await conn.BeginTransactionAsync())
    {
        async Task Setup(string sql) { await using var cmd = new NpgsqlCommand(sql, conn, tx); await cmd.ExecuteNonQueryAsync(); }
        await Setup($"SET LOCAL search_path TO {schema}");
        foreach (var table in tables) await Setup($"CREATE TABLE {schema}.{Q(table)} (LIKE public.{Q(table)} INCLUDING ALL)");
        foreach (var fk in foreignKeys)
            await Setup($"ALTER TABLE {schema}.{Q(fk.Table)} ADD CONSTRAINT {Q(fk.Name)} {fk.Definition.Replace("REFERENCES public.", $"REFERENCES {schema}.")}");
        // Older local databases may not yet have migration 021. Upgrade only the disposable schema.
        await Setup("ALTER TABLE recommendation_config ADD COLUMN IF NOT EXISTS settings jsonb; CREATE TABLE IF NOT EXISTS recommendation_rating_snapshot(recommendation_config_id bigint REFERENCES recommendation_config,place_id bigint REFERENCES place,rating numeric(3,2),rating_count integer,PRIMARY KEY(recommendation_config_id,place_id))");
        await Setup("""
            INSERT INTO recommendation_config(recommendation_config_id,version,distance_limit,cluster_count,status,settings) OVERRIDING SYSTEM VALUE
              VALUES(910001,'delete-check',10000,15,'DRAFT','{}');
            INSERT INTO recommendation_criterion(criterion_id,code,name,status) OVERRIDING SYSTEM VALUE VALUES(910001,'delete-check','Delete check','ACTIVE');
            INSERT INTO region(region_id,name,status) OVERRIDING SYSTEM VALUE VALUES(910001,'Delete check region','DRAFT');
            INSERT INTO place_type(place_type_id,name) OVERRIDING SYSTEM VALUE VALUES(910001,'Delete check type');
            INSERT INTO place(place_id,region_id,place_type_id,name,latitude,longitude,status) OVERRIDING SYSTEM VALUE VALUES(910001,910001,910001,'Delete check place',21,105,'DRAFT');
            INSERT INTO card(card_id,place_id,name,status) OVERRIDING SYSTEM VALUE VALUES(910001,910001,'Delete check card','DRAFT');
            INSERT INTO place_image(place_image_id,place_id,image_key) OVERRIDING SYSTEM VALUE VALUES(910001,910001,'https://example.invalid/check.jpg');
            INSERT INTO place_feature(place_feature_id,place_id,recommendation_config_id,cluster_number) OVERRIDING SYSTEM VALUE VALUES(910001,910001,910001,0);
            INSERT INTO place_feature_value VALUES(910001,910001,1);
            INSERT INTO recommendation_rating_snapshot VALUES(910001,910001,4,1);
            INSERT INTO collection(collection_id,name,status) OVERRIDING SYSTEM VALUE VALUES(910001,'Delete check collection','DRAFT');
            INSERT INTO collection_card(collection_card_id,collection_id,card_id) OVERRIDING SYSTEM VALUE VALUES(910001,910001,910001);
            INSERT INTO quest(quest_id,name,quest_type,condition_type,target_value,cycle_type,status) OVERRIDING SYSTEM VALUE VALUES(910001,'Delete check quest','DAILY','STEP_COUNT',1,'DAILY','DRAFT');
            """);
        await tx.CommitAsync();
    }
    settings.SearchPath = schema;
    await using var isolated = NpgsqlDataSource.Create(settings.ConnectionString);
    var repo = new AdminRepository(isolated, new TestSessions());
    await PlaceTypeFeatureSchema.EnsureAsync(isolated);
    await FeatureInheritanceChecks.Run(isolated, repo);
    void Assert(bool condition, string message) { if (!condition) throw new Exception(message); }
    async Task<long> Count(string table) { await using var cmd = isolated.CreateCommand($"SELECT count(*) FROM {table}"); return (long)(await cmd.ExecuteScalarAsync())!; }
    async Task Blocked(string resource, bool hasStatus)
    {
        var result = await repo.CheckDelete(resource,910001,default);
        Assert(!result.CanDelete && result.CanChangeStatus == hasStatus, "Incorrect linked eligibility: " + resource);
        try { await repo.Delete(resource,910001,default); throw new Exception("Linked deletion accepted: " + resource); }
        catch(AdminValidation) { }
        Assert(await Count(AdminCatalog.Resources[resource].Table) == 1, "Parent lost: " + resource);
        Console.WriteLine("PASS linked check + DELETE blocked: " + resource);
    }
    foreach(var resource in new[]{"regions","places","cards","collections"}) await Blocked(resource,true);
    await Blocked("place-types",false);
    Assert(await Count("place_image")==1 && await Count("place_feature")==1 && await Count("place_feature_value")==1 && await Count("recommendation_rating_snapshot")==1,"Blocked deletion removed owned data");
    await using(var cmd=isolated.CreateCommand("DROP TABLE recommendation_rating_snapshot")) await cmd.ExecuteNonQueryAsync();
    await Blocked("places",true);
    Console.WriteLine("PASS missing optional snapshot table does not bypass business references");
    await using(var cmd=isolated.CreateCommand("CREATE TABLE recommendation_rating_snapshot(recommendation_config_id bigint REFERENCES recommendation_config,place_id bigint REFERENCES place,rating numeric(3,2),rating_count integer,PRIMARY KEY(recommendation_config_id,place_id)); INSERT INTO recommendation_rating_snapshot VALUES(910001,910001,4,1)")) await cmd.ExecuteNonQueryAsync();
    Assert(await Count("collection_card")==1,"Membership cascaded away");
    // Test ON DELETE SET NULL and CASCADE explicitly, including references outside public.
    await using(var cmd=isolated.CreateCommand("CREATE TABLE quest_link(id bigint PRIMARY KEY,quest_id bigint REFERENCES quest(quest_id) ON DELETE SET NULL); INSERT INTO quest_link VALUES(1,910001); CREATE TABLE region_link(id bigint PRIMARY KEY,region_id bigint REFERENCES region(region_id) ON DELETE CASCADE); INSERT INTO region_link VALUES(1,910001)")) await cmd.ExecuteNonQueryAsync();
    await Blocked("quests",true);
    await using(var cmd=isolated.CreateCommand("DELETE FROM collection_card; DELETE FROM quest_link;")) await cmd.ExecuteNonQueryAsync();
    // Change since preflight: eligibility must be checked again by DELETE.
    Assert((await repo.CheckDelete("collections",910001,default)).CanDelete,"Empty collection not eligible");
    await using(var cmd=isolated.CreateCommand("INSERT INTO collection_card(collection_card_id,collection_id,card_id) OVERRIDING SYSTEM VALUE VALUES(910002,910001,910001)")) await cmd.ExecuteNonQueryAsync();
    await Blocked("collections",true);
    await using(var cmd=isolated.CreateCommand("DELETE FROM collection_card")) await cmd.ExecuteNonQueryAsync();
    Console.WriteLine("PASS link added after preflight is blocked at DELETE");
    // Actual state change leaves the linked parent and its child untouched.
    await repo.Save("regions", 910001, System.Text.Json.JsonSerializer.SerializeToElement(new { name="Delete check region", status="INACTIVE" }), 1, default);
    await using(var cmd=isolated.CreateCommand("SELECT status FROM region WHERE region_id=910001"))
        Assert((string?)(await cmd.ExecuteScalarAsync()) == "INACTIVE", "Status update failed");
    Assert(await Count("place")==1,"Status update removed child");
    Console.WriteLine("PASS linked status update preserves child");
    foreach(var resource in new[]{"collections","cards","places","place-types","quests"})
    {
        Assert((await repo.CheckDelete(resource,910001,default)).CanDelete,"Unlinked record blocked: " + resource);
        await repo.Delete(resource,910001,default);
        Assert(await Count(AdminCatalog.Resources[resource].Table)==0,"Unlinked record not deleted: " + resource);
        Console.WriteLine("PASS unlinked eligibility + actual DELETE: " + resource);
        if(resource=="places")
        {
            foreach(var table in new[]{"place_image","place_feature","place_feature_value","recommendation_rating_snapshot"})
                Assert(await Count(table)==0,"Owned data not cleaned: " + table);
            Console.WriteLine("PASS place with only images/features/snapshots is deletable; owned rows cleaned atomically");
        }
    }
    // Region now only has the CASCADE link. It must still be protected.
    await Blocked("regions",true);
    Assert(await Count("region_link")==1,"CASCADE link was lost");
    await using(var cmd=isolated.CreateCommand("DELETE FROM region_link")) await cmd.ExecuteNonQueryAsync();
    await repo.Delete("regions",910001,default);
    Assert(await Count("region")==0,"Region not deleted after unlink");
    foreach(var resource in new[]{"users","items"})
    {
        try { await repo.Delete(resource,910001,default); throw new Exception("Protected resource accepted"); } catch(AdminValidation) { }
    }
    try { await repo.CheckDelete("regions",910001,default); throw new Exception("Missing record accepted"); } catch(KeyNotFoundException) { }
    Console.WriteLine("PASS CASCADE protection, unlinked region deletion, protected users/items, missing record");
}
finally
{
    await Exec($"DROP SCHEMA {schema} CASCADE");
    Console.WriteLine("CLEANUP: isolated fixture schema removed; public application rows were not modified.");
}
sealed class TestSessions : ISessionStore
{
    public PublicAccount? Get(string? token) => null;
    public AccountSession Create(AccountUser user) => throw new NotSupportedException();
    public void Revoke(string? token) { }
    public void RevokeAll(long id) { }
}
