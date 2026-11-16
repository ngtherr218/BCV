using System.Data;
using System.Text.Json;
using Npgsql;
using NpgsqlTypes;

// Local, data-only merge. Source connection is always READ ONLY; no DDL, DELETE or TRUNCATE.
var tables = new[] {
    new Spec("region","region_id",["region_id"]),
    new Spec("place_type","place_type_id",["name"]),
    new Spec("recommendation_criterion","criterion_id",["code"]),
    new Spec("recommendation_config","recommendation_config_id",["recommendation_config_id"]),
    new Spec("place","place_id",["place_id"],("region_id","region"),("place_type_id","place_type")),
    new Spec("place_image","place_image_id",["place_id","image_key"],("place_id","place")),
    new Spec("place_feature","place_feature_id",["place_id","recommendation_config_id"],("place_id","place"),("recommendation_config_id","recommendation_config")),
    new Spec("place_feature_value",null,["place_feature_id","criterion_id"],("place_feature_id","place_feature"),("criterion_id","recommendation_criterion")),
    new Spec("recommendation_weight",null,["recommendation_config_id","criterion_id"],("recommendation_config_id","recommendation_config"),("criterion_id","recommendation_criterion")),
    new Spec("app_user","user_id",["email"]),
    new Spec("user_preference","user_preference_id",["user_id"],("user_id","app_user")),
    new Spec("user_preference_place_type",null,["user_preference_id","place_type_id"],("user_preference_id","user_preference"),("place_type_id","place_type")),
    new Spec("virtual_location","virtual_location_id",["user_id"],("user_id","app_user"),("region_id","region"),("nearest_place_id","place")),
    new Spec("journey","journey_id",["user_id","destination_place_id","started_at"],("user_id","app_user"),("start_place_id","place"),("destination_place_id","place")),
    new Spec("step_data","step_data_id",["user_id","source_data_id"],("user_id","app_user")),
    new Spec("user_discovered_place","user_discovered_place_id",["user_id","place_id"],("user_id","app_user"),("place_id","place")),
    new Spec("card","card_id",["place_id"],("place_id","place")),
    new Spec("collection","collection_id",["name"]),
    new Spec("collection_card","collection_card_id",["collection_id","card_id"],("collection_id","collection"),("card_id","card")),
    new Spec("user_card","user_card_id",["user_id","card_id"],("user_id","app_user"),("card_id","card")),
    new Spec("item","item_id",["item_id"]),
    new Spec("user_item","user_item_id",["user_id","item_id"],("user_id","app_user"),("item_id","item")),
    new Spec("quest","quest_id",["quest_id"],("reward_item_id","item")),
    new Spec("quest_progress","quest_progress_id",["user_id","quest_id","cycle_key"],("user_id","app_user"),("quest_id","quest")),
    new Spec("notification","notification_id",["user_id","notification_type","title","content","created_at"],("user_id","app_user"))
};
var report = new List<object>();
try
{
    var root=Directory.GetCurrentDirectory();
    var env=File.ReadAllLines(Path.Combine(root,".env.local")).Where(l=>!string.IsNullOrWhiteSpace(l)&&!l.TrimStart().StartsWith('#'))
        .Select(l=>l.Split('=',2)).Where(p=>p.Length==2).ToDictionary(p=>p[0].Trim(),p=>p[1]);
    if(env["DB_HOST"] is not ("localhost" or "127.0.0.1" or "::1") || env["DB_SOURCE_NAME"]!="bcv" || env["DB_NAME"]!="vietnam_explorer_dev_v2")
        throw new ImportError("Only localhost bcv -> vietnam_explorer_dev_v2 is allowed.");
    var apply=args.Contains("--apply");
    if(apply && File.Exists(Path.Combine(root,"output","data-import","applied.json")))
        throw new ImportError("This one-time import already committed. Use the default read-only plan; review the saved mapping and a fresh backup before any later import.");
    var backupIndex=Array.IndexOf(args,"--backup-file");
    if(apply && (backupIndex<0 || backupIndex+1>=args.Length || !File.Exists(args[backupIndex+1]) || new FileInfo(args[backupIndex+1]).Length==0))
        throw new ImportError("Apply requires a verified existing target backup: --backup-file <path>.");
    NpgsqlConnection Connection(string database) => new(new NpgsqlConnectionStringBuilder {
        Host=env["DB_HOST"],Port=int.Parse(env["DB_PORT"]),Username=env["DB_USER"],Password=env["DB_PASSWORD"],
        Database=database,Timeout=10,CommandTimeout=120,ApplicationName="BCV explicit local data merge"
    }.ConnectionString);
    await using var source=Connection("bcv");await using var target=Connection("vietnam_explorer_dev_v2");
    await source.OpenAsync();await target.OpenAsync();
    await using var sourceTx=await source.BeginTransactionAsync(IsolationLevel.RepeatableRead);
    await Sql(source,sourceTx,"SET TRANSACTION READ ONLY");
    await using var targetTx=await target.BeginTransactionAsync(IsolationLevel.Serializable);
    if(!apply)await Sql(target,targetTx,"SET TRANSACTION READ ONLY");
    if((string)(await Scalar(source,sourceTx,"SELECT current_database()"))! != "bcv"
        || (string)(await Scalar(target,targetTx,"SELECT current_database()"))! != "vietnam_explorer_dev_v2")throw new ImportError("Database identity mismatch.");
    await using(var inventory=new NpgsqlCommand("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename<>'spatial_ref_sys'",source,sourceTx))
    {
        var actual=new HashSet<string>();
        await using(var reader=await inventory.ExecuteReaderAsync())while(await reader.ReadAsync())actual.Add(reader.GetString(0));
        if(!actual.SetEquals(tables.Select(t=>t.Table)))throw new ImportError("Source tables differ from the audited 25-table schema; review import coverage first.");
    }
    if(apply)
    {
        await Sql(target,targetTx,"SET LOCAL lock_timeout='10s'");
        await Sql(target,targetTx,"LOCK TABLE "+string.Join(',',tables.Select(s=>"public."+Q(s.Table)))+" IN SHARE ROW EXCLUSIVE MODE");
    }
    var mappings=new Dictionary<string,Dictionary<long,long>>();
    var live=new Dictionary<string,List<Dictionary<string,object?>>>();
    var jsonOptions=new JsonSerializerOptions{WriteIndented=true};
    var beforeSource=new Dictionary<string,int>();
    var beforeTarget=new Dictionary<string,int>();
    var coverage=new Dictionary<string,int>();
    foreach(var spec in tables)
    {
        var src=await Load(source,sourceTx,spec.Table);
        var dst=await Load(target,targetTx,spec.Table);
        beforeSource[spec.Table]=src.Rows.Count;beforeTarget[spec.Table]=dst.Rows.Count;
        live[spec.Table]=dst.Rows;
        var map=new Dictionary<long,long>();mappings[spec.Table]=map;
        var natural=new Dictionary<string,Dictionary<string,object?>>();
        foreach(var row in dst.Rows)
            if(!natural.TryAdd(Key(row,spec.Keys),row))throw new ImportError($"Ambiguous target natural keys in {spec.Table}; no import performed.");
        var used=spec.Id is null?new HashSet<long>():dst.Rows.Select(r=>Convert.ToInt64(r[spec.Id])).ToHashSet();
        var counter=spec.Id is null?0:src.Rows.Concat(dst.Rows).Select(r=>Convert.ToInt64(r[spec.Id])).DefaultIfEmpty(0).Max();
        int inserted=0,updated=0,unchanged=0,protectedRows=0,remapped=0;
        foreach(var original in src.Rows)
        {
            var row=new Dictionary<string,object?>(original);
            foreach(var (column,parent) in spec.Refs)
                if(row[column] is not null)
                {
                    var old=Convert.ToInt64(row[column]);
                    if(!mappings[parent].TryGetValue(old,out var mapped))throw new ImportError($"Unresolved source FK in {spec.Table}.{column}.");
                    row[column]=mapped;
                }
            if(spec.Table=="step_data" && row["source_data_id"] is string sourceId && sourceId.StartsWith("health-connect:journey:"))
            {
                if(!long.TryParse(sourceId["health-connect:journey:".Length..],out var journeyId) || !mappings["journey"].TryGetValue(journeyId,out var mapped))
                    throw new ImportError("Cannot safely remap a journey step source ID.");
                row["source_data_id"]=$"health-connect:journey:{mapped}";
            }
            var key=Key(row,spec.Keys);
            var columns=src.Columns.Select(c=>c.Name).ToArray();
            if(natural.TryGetValue(key,out var existing))
            {
                if(spec.Id is not null)row[spec.Id]=existing[spec.Id];
                bool protectedRow=false;
                void Keep(params string[] fields){foreach(var field in fields)row[field]=existing[field];protectedRow=true;}
                if(spec.Table=="app_user" && string.Equals((string?)existing["email"],"admin@gmail.com",StringComparison.OrdinalIgnoreCase))Keep("password_hash","role","status");
                if(spec.Table=="recommendation_config")
                {
                    if(!Equals(row["cluster_count"],existing["cluster_count"]))throw new ImportError("Source model cluster count differs; review model import first.");
                    Keep("version","status"); // Keep v2 version metadata and the active model contract.
                }
                if(spec.Table=="card")
                {
                    var place=live["place"].Single(p=>Equals(p["place_id"],row["place_id"]));
                    if(!Equals(existing["name"],place["name"]))Keep("name","description","status");
                    if(row["image_key"] is null && existing["image_key"] is not null)Keep("image_key");
                }
                if(spec.Table=="user_item")row["quantity"]=Math.Max(Convert.ToInt32(row["quantity"]),Convert.ToInt32(existing["quantity"]));
                if(spec.Table=="item" && Equals(existing["status"],"ACTIVE"))Keep("status");
                if(spec.Table=="quest" && Equals(existing["cycle_type"],"WEEKLY") && Equals(existing["status"],"ACTIVE"))Keep("status");
                if(spec.Table=="quest_progress")
                {
                    row["progress_value"]=Math.Max(Convert.ToInt32(row["progress_value"]),Convert.ToInt32(existing["progress_value"]));
                    row["completion_status"]=(bool)row["completion_status"]! || (bool)existing["completion_status"]!;
                    row["reward_status"]=(bool)row["reward_status"]! || (bool)existing["reward_status"]!;
                    if(existing["completed_at"] is DateTime previous && (row["completed_at"] is not DateTime current || previous<current))row["completed_at"]=previous;
                }
                if(spec.Table is "user_card" or "user_discovered_place")
                {
                    var date=spec.Table=="user_card"?"received_at":"discovered_at";
                    if((DateTime)existing[date]!<(DateTime)row[date]!)row[date]=existing[date];
                }
                if(spec.Table=="virtual_location" && (DateTime)existing["updated_at"]!>(DateTime)row["updated_at"]!)Keep(columns);
                var changes=columns.Where(c=>c!=spec.Id && !Equals(row[c],existing[c])).ToArray();
                if(changes.Length>0)
                {
                    if(apply)await Update(target,targetTx,spec,src.Columns,row,changes);
                    foreach(var name in changes)existing[name]=row[name];
                    updated++;
                }
                else unchanged++;
                if(protectedRow)protectedRows++;
            }
            else
            {
                if(spec.Table=="recommendation_config")throw new ImportError("An unknown source model requires explicit v2 metadata; no invented settings inserted.");
                if(spec.Id is not null)
                {
                    var requested=Convert.ToInt64(row[spec.Id]);
                    if(used.Contains(requested))row[spec.Id]=++counter;
                    used.Add(Convert.ToInt64(row[spec.Id]));
                }
                if(spec.Table=="journey" && Equals(row["status"],"IN_PROGRESS") && dst.Rows.Any(r=>Equals(r["user_id"],row["user_id"])&&Equals(r["status"],"IN_PROGRESS")))
                    throw new ImportError("Two active journeys would belong to the same merged account; no import committed.");
                if(apply)await Insert(target,targetTx,spec.Table,src.Columns,row);
                dst.Rows.Add(row);natural.Add(key,row);inserted++;
            }
            if(spec.Id is not null)
            {
                var old=Convert.ToInt64(original[spec.Id]);var current=Convert.ToInt64(row[spec.Id]);
                map[old]=current;if(old!=current)remapped++;
            }
        }
        coverage[spec.Table]=src.Rows.Count;
        report.Add(new{table=spec.Table,source=src.Rows.Count,targetBefore=beforeTarget[spec.Table],inserted,updated,unchanged,protectedRows,remapped,targetAfter=dst.Rows.Count});
    }
    if(apply)
    {
        // Imported custom places also need a genuine per-model rating snapshot. Existing frozen snapshots stay unchanged.
        await Sql(target,targetTx,"""
            INSERT INTO recommendation_rating_snapshot(recommendation_config_id,place_id,rating,rating_count)
            SELECT pf.recommendation_config_id,p.place_id,p.rating,p.rating_count
            FROM place_feature pf JOIN place p USING(place_id)
            ON CONFLICT(recommendation_config_id,place_id) DO NOTHING
            """);
        // Sequence state only; no new columns/tables/constraints. Keep generated IDs collision-free.
        foreach(var spec in tables.Where(s=>s.Id is not null))
        {
            await using var sequence=new NpgsqlCommand("SELECT pg_get_serial_sequence($1,$2)",target,targetTx);
            sequence.Parameters.AddWithValue("public."+spec.Table);sequence.Parameters.AddWithValue(spec.Id!);
            if(await sequence.ExecuteScalarAsync() is string name)
            {
                // setval cannot roll back: never move an existing sequence backwards, even on a failed import.
                var maximum=live[spec.Table].Select(r=>Convert.ToInt64(r[spec.Id!])).DefaultIfEmpty(0).Max();
                var parts=name.Split('.');
                var sequenceName=string.Join('.',parts.Select(p=>Q(p.Trim('"'))));
                var last=Convert.ToInt64(await Scalar(target,targetTx,$"SELECT last_value FROM {sequenceName}"));
                await using var set=new NpgsqlCommand("SELECT setval($1::regclass,$2,true)",target,targetTx);
                set.Parameters.AddWithValue(name);set.Parameters.AddWithValue(Math.Max(1,Math.Max(last,maximum)));
                await set.ExecuteScalarAsync();
            }
        }
        await targetTx.CommitAsync();
    }
    else await targetTx.RollbackAsync();
    await sourceTx.RollbackAsync();
    var destination=Path.Combine(root,"output","data-import");Directory.CreateDirectory(destination);
    var mode=apply?"applied":"plan";
    File.WriteAllText(Path.Combine(destination,$"{mode}.json"),JsonSerializer.Serialize(new{mode,sourceDatabase="bcv",targetDatabase="vietnam_explorer_dev_v2",tables=report,sourceCoverage=coverage},jsonOptions));
    if(apply)File.WriteAllText(Path.Combine(destination,"id-mapping.json"),JsonSerializer.Serialize(mappings,jsonOptions));
    Console.WriteLine(JsonSerializer.Serialize(new{mode,tables=report},jsonOptions));
}
catch(Exception error)
{
    Console.Error.WriteLine(error is ImportError ? error.Message : error is PostgresException pg ? $"Import stopped: SQLSTATE {pg.SqlState} ({pg.TableName ?? "database"}/{pg.ConstraintName ?? "constraint"}). Target transaction was not committed." : $"Import stopped ({error.GetType().Name}); no target commit confirmed. Inspect local configuration.");
    Environment.ExitCode=1;
}

static string Q(string name)=>'"'+name.Replace("\"","\"\"")+'"';
static string Key(Dictionary<string,object?> row,string[] keys)=>JsonSerializer.Serialize(keys.Select(k=>row[k]));
static async Task Sql(NpgsqlConnection c,NpgsqlTransaction t,string sql){await using var cmd=new NpgsqlCommand(sql,c,t);await cmd.ExecuteNonQueryAsync();}
static async Task<object?> Scalar(NpgsqlConnection c,NpgsqlTransaction t,string sql){await using var cmd=new NpgsqlCommand(sql,c,t);return await cmd.ExecuteScalarAsync();}
static async Task<Data> Load(NpgsqlConnection c,NpgsqlTransaction t,string name)
{
    await using var cmd=new NpgsqlCommand("SELECT * FROM public."+Q(name),c,t);
    await using var reader=await cmd.ExecuteReaderAsync();
    var columns=Enumerable.Range(0,reader.FieldCount).Select(i=>new Col(reader.GetName(i),reader.GetPostgresType(i).Name switch {
        "bigint" or "int8"=>NpgsqlDbType.Bigint,"integer" or "int4"=>NpgsqlDbType.Integer,"numeric"=>NpgsqlDbType.Numeric,"double precision" or "float8"=>NpgsqlDbType.Double,
        "boolean" or "bool"=>NpgsqlDbType.Boolean,"timestamp without time zone" or "timestamp"=>NpgsqlDbType.Timestamp,"timestamp with time zone" or "timestamptz"=>NpgsqlDbType.TimestampTz,
        "text"=>NpgsqlDbType.Text,"character varying" or "varchar"=>NpgsqlDbType.Varchar,"jsonb"=>NpgsqlDbType.Jsonb,
        _=>throw new ImportError("Unsupported column type "+reader.GetPostgresType(i).Name+" in "+name+"."+reader.GetName(i))
    })).ToArray();
    var rows=new List<Dictionary<string,object?>>();
    while(await reader.ReadAsync())rows.Add(Enumerable.Range(0,reader.FieldCount).ToDictionary(i=>columns[i].Name,i=>reader.IsDBNull(i)?null:reader.GetValue(i)));
    return new(columns,rows);
}
static async Task Insert(NpgsqlConnection c,NpgsqlTransaction t,string table,Col[] columns,Dictionary<string,object?> row)
{
    await using var cmd=new NpgsqlCommand($"INSERT INTO public.{Q(table)} ({string.Join(',',columns.Select(col=>Q(col.Name)))}) VALUES ({string.Join(',',columns.Select((_,i)=>"$"+(i+1)))})",c,t);
    foreach(var col in columns)cmd.Parameters.Add(new NpgsqlParameter{NpgsqlDbType=col.Type,Value=row[col.Name]??DBNull.Value});
    await cmd.ExecuteNonQueryAsync();
}
static async Task Update(NpgsqlConnection c,NpgsqlTransaction t,Spec spec,Col[] columns,Dictionary<string,object?> row,string[] changed)
{
    var key=spec.Id is null?spec.Keys:[spec.Id];
    var fields=changed.Concat(key).ToArray();
    var sql=$"UPDATE public.{Q(spec.Table)} SET {string.Join(',',changed.Select((f,i)=>Q(f)+"=$"+(i+1)))} WHERE {string.Join(" AND ",key.Select((f,i)=>Q(f)+"=$"+(changed.Length+i+1)))}";
    await using var cmd=new NpgsqlCommand(sql,c,t);
    foreach(var field in fields)cmd.Parameters.Add(new NpgsqlParameter{NpgsqlDbType=columns.Single(col=>col.Name==field).Type,Value=row[field]??DBNull.Value});
    if(await cmd.ExecuteNonQueryAsync()!=1)throw new ImportError("Unexpected update count in "+spec.Table);
}
sealed record Spec(string Table,string? Id,string[] Keys,params (string Column,string Parent)[] Refs);
sealed record Col(string Name,NpgsqlDbType Type);
sealed record Data(Col[] Columns,List<Dictionary<string,object?>> Rows);
sealed class ImportError(string message):Exception(message);
