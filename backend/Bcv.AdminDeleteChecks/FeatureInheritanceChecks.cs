using System.Text.Json;
using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.Discovery.Domain;
using Npgsql;

static class FeatureInheritanceChecks
{
    public static async Task Run(NpgsqlDataSource db, AdminRepository repo)
    {
        void Check(bool value, string error) { if (!value) throw new Exception(error); }
        async Task Exec(string sql) { await using var cmd=db.CreateCommand(sql); await cmd.ExecuteNonQueryAsync(); }
        async Task<long> Save(string resource, long? id, object data)
        {
            var result=await repo.Save(resource,id,JsonSerializer.SerializeToElement(data),1,default);
            return JsonSerializer.SerializeToElement(result).GetProperty("id").GetInt64();
        }
        async Task<string[]> Codes(long id)
        {
            await using var cmd=db.CreateCommand("SELECT c.code FROM place_feature pf JOIN place_feature_value v USING(place_feature_id) JOIN recommendation_criterion c USING(criterion_id) JOIN recommendation_config cfg USING(recommendation_config_id) WHERE pf.place_id=$1 AND cfg.status='ACTIVE' AND v.feature_value=1 ORDER BY c.code");
            cmd.Parameters.AddWithValue(id); var codes=new List<string>();
            await using var r=await cmd.ExecuteReaderAsync(); while(await r.ReadAsync()) codes.Add(r.GetString(0));
            return codes.ToArray();
        }
        var index=0;
        foreach(var code in PlaceFeatureClassifier.Features)
        {
            await using var cmd=db.CreateCommand("INSERT INTO recommendation_criterion(criterion_id,code,name,status) OVERRIDING SYSTEM VALUE VALUES($1,$2,$2,'ACTIVE') ON CONFLICT(code) DO NOTHING");
            cmd.Parameters.AddWithValue(920000L+index++); cmd.Parameters.AddWithValue(code); await cmd.ExecuteNonQueryAsync();
        }
        await Exec("UPDATE recommendation_config SET status='ACTIVE' WHERE recommendation_config_id=910001");
        var type=await Save("place-types",null,new{name="Inheritance test",feature_codes=new[]{"kind__religious","theme__spirituality"}});
        // Idempotent migration must not restore old defaults over administrator choices.
        await PlaceTypeFeatureSchema.EnsureAsync(db);
        var detail=await repo.Detail("place-types",type,default);
        Check(detail!.Value.GetProperty("feature_codes").GetArrayLength()==2,"Type feature detail/migration lost mapping");
        var lookups=JsonSerializer.SerializeToElement(await repo.Lookups(default));
        Check(lookups.GetProperty("types").EnumerateArray().Single(t=>t.GetProperty("id").GetInt64()==type).GetProperty("feature_codes").GetArrayLength()==2,"Lookup mapping missing");
        object Place(string name, string source, string[]? codes=null)=>new{name,feature_source=source,feature_codes=codes,region_id=910001,place_type_id=type,latitude=21,longitude=105,rating_count=0,status="DRAFT",images=Array.Empty<string>()};
        var inherited=await Save("places",null,Place("Inherited","TYPE"));
        var secondInherited=await Save("places",null,Place("Second inherited","TYPE"));
        var custom=await Save("places",null,Place("Custom","CUSTOM",new[]{"kind__museum"}));
        Check((await Codes(inherited)).SequenceEqual(new[]{"kind__religious","theme__spirituality"}),"Backend did not inherit type without feature selections");
        await using(var cmd=db.CreateCommand("SELECT count(*) FROM recommendation_config"))
        {
            var before=(long)(await cmd.ExecuteScalarAsync())!;
            await Save("place-types",type,new{name="Inheritance test",feature_codes=new[]{"kind__religious","theme__spirituality"}});
            Check((long)(await cmd.ExecuteScalarAsync())! == before,"Unchanged mapping created a redundant model revision");
        }
        Check((await Codes(custom)).SequenceEqual(new[]{"kind__museum"}),"Custom features ignored");
        await using(var cmd=db.CreateCommand("SELECT count(*) FROM place_feature pf JOIN recommendation_config cfg USING(recommendation_config_id) JOIN recommendation_rating_snapshot s USING(place_id,recommendation_config_id) JOIN place_feature_value v USING(place_feature_id) WHERE pf.place_id=$1 AND cfg.status='ACTIVE' AND pf.cluster_number IS NOT NULL"))
        {
            cmd.Parameters.AddWithValue(inherited); Check((long)(await cmd.ExecuteScalarAsync())! == 35,"Inherited place lacks recommendation cluster/vector/snapshot");
        }
        Console.WriteLine("PASS create type + create place without feature selections; 35-value vector, cluster and recommendation snapshot saved");
        await Save("place-types",type,new{name="Updated inheritance",feature_codes=new[]{"kind__heritage","theme__history"}});
        Check((await Codes(inherited)).SequenceEqual(new[]{"kind__heritage","theme__history"}),"Inherited place not propagated");
        Check((await Codes(secondInherited)).SequenceEqual(new[]{"kind__heritage","theme__history"}),"Batch did not update second inherited place");
        Check((await Codes(custom)).SequenceEqual(new[]{"kind__museum"}),"Type change overwrote custom place");
        var inheritedDetail=await repo.Detail("places",inherited,default);
        Check(inheritedDetail!.Value.GetProperty("feature_source").GetString()=="TYPE","Inheritance mode not persisted");
        await Save("places",custom,Place("Custom now inherits","TYPE"));
        Check((await Codes(custom)).SequenceEqual(new[]{"kind__heritage","theme__history"}),"Switch to inheritance failed");
        Console.WriteLine("PASS type edits propagate only to inherited places; custom preserved; switching back to inheritance works");
        try { await Save("place-types",type,new{name="Must rollback",feature_codes=Array.Empty<string>()}); throw new Exception("Empty mapping accepted"); } catch(AdminValidation) { }
        Check((await repo.Detail("place-types",type,default))!.Value.GetProperty("name").GetString()=="Updated inheritance","Invalid type save was not rolled back");
        try { await Save("places",inherited,Place("Must rollback","CUSTOM",new[]{"invalid-code"})); throw new Exception("Invalid feature accepted"); } catch(AdminValidation) { }
        Check((await Codes(inherited)).SequenceEqual(new[]{"kind__heritage","theme__history"}),"Invalid place save changed features");
        await repo.Delete("places",inherited,default); await repo.Delete("places",secondInherited,default); await repo.Delete("places",custom,default); await repo.Delete("place-types",type,default);
        // Restore the deletion suite's single model fixture after testing revision copies.
        await Exec("DELETE FROM place_feature_value WHERE place_feature_id IN (SELECT place_feature_id FROM place_feature WHERE recommendation_config_id<>910001); DELETE FROM place_feature WHERE recommendation_config_id<>910001; DELETE FROM recommendation_rating_snapshot WHERE recommendation_config_id<>910001; DELETE FROM recommendation_config WHERE recommendation_config_id<>910001; UPDATE recommendation_config SET status='DRAFT' WHERE recommendation_config_id=910001");
        Console.WriteLine("PASS empty/invalid mappings rejected atomically; inherited places and unused type mapping can be deleted");
    }
}
