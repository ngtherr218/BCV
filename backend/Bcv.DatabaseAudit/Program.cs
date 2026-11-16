using System.Text;
using System.Text.Json;
using Npgsql;

// Standalone: never starts API services or their automatic schema migrations.
try
{
    var root = Directory.GetCurrentDirectory();
    var settings = new Dictionary<string, string>();
    var envFile = Path.Combine(root, ".env.local");
    if (File.Exists(envFile))
        foreach (var line in File.ReadAllLines(envFile))
        {
            var text = line.Trim();
            if (text.Length == 0 || text.StartsWith('#')) continue;
            var split = text.IndexOf('=');
            if (split > 0) settings[text[..split].Trim()] = text[(split + 1)..].Trim().Trim('"', '\'');
        }
    string Value(string key, string fallback = "") => Environment.GetEnvironmentVariable(key)
        ?? settings.GetValueOrDefault(key, fallback);
    var host = Value("DB_HOST", "localhost");
    if (host is not ("localhost" or "127.0.0.1" or "::1"))
        throw new InvalidOperationException("Only a loopback database is permitted.");
    var target = args.Contains("--target");
    var database = Value(target ? "DB_NAME" : "DB_SOURCE_NAME");
    if (target && database != "vietnam_explorer_dev_v2")
        throw new InvalidOperationException("Target must be vietnam_explorer_dev_v2.");
    if (string.IsNullOrWhiteSpace(database))
        throw new InvalidOperationException("Set DB_SOURCE_NAME to the existing LOCAL database in .env.local.");
    var password = Value("DB_PASSWORD");
    if (string.IsNullOrEmpty(password)) throw new InvalidOperationException("Set DB_PASSWORD in .env.local.");
    var cs = new NpgsqlConnectionStringBuilder
    {
        Host = host, Port = int.Parse(Value("DB_PORT", "5432")), Database = database,
        Username = Value("DB_USER", "postgres"), Password = password,
        Timeout = 5, CommandTimeout = 60, ApplicationName = "BCV read-only schema audit"
    };
    await using var connection = new NpgsqlConnection(cs.ConnectionString);
    await connection.OpenAsync();
    await using var transaction = await connection.BeginTransactionAsync(System.Data.IsolationLevel.RepeatableRead);
    await using (var readOnly = new NpgsqlCommand("SET TRANSACTION READ ONLY", connection, transaction))
        await readOnly.ExecuteNonQueryAsync();
    async Task<List<string[]>> Query(string sql)
    {
        await using var cmd = new NpgsqlCommand(sql, connection, transaction);
        await using var reader = await cmd.ExecuteReaderAsync();
        var rows = new List<string[]>();
        while (await reader.ReadAsync())
            rows.Add(Enumerable.Range(0, reader.FieldCount).Select(i => reader.IsDBNull(i) ? "NULL" : Convert.ToString(reader.GetValue(i))!).ToArray());
        return rows;
    }
    var identity = await Query("SELECT current_database(), inet_server_addr()::text, inet_server_port()::text, current_setting('transaction_read_only')");
    if (identity[0][0] != database || identity[0][3] != "on") throw new InvalidOperationException("Database identity/read-only verification failed.");
    var tables = await Query("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename");
    var snapshot = new List<object>();
    var md = new StringBuilder("# Database schema before — live read-only snapshot\n\n");
    md.AppendLine($"Captured UTC: {DateTimeOffset.UtcNow:O}. Source: local `{database}`. Isolation: REPEATABLE READ, READ ONLY. No user rows exported.\n");
    md.AppendLine("| STT | Bảng | PK | FK | Số bản ghi | Chức năng |\n|---|---|---|---|---|---|");
    foreach (var (row, index) in tables.Select((r, i) => (r, i)))
    {
        var name = row[0];
        var literal = "'" + name.Replace("'", "''") + "'";
        var quoted = "\"" + name.Replace("\"", "\"\"") + "\"";
        var count = (await Query($"SELECT count(*)::text FROM public.{quoted}"))[0][0];
        var columns = await Query($"SELECT a.attname,format_type(a.atttypid,a.atttypmod),a.attnotnull::text,coalesce(pg_get_expr(d.adbin,d.adrelid),'NULL'),a.attidentity::text,a.attgenerated::text,coalesce(coll.collname,'NULL') FROM pg_attribute a JOIN pg_class t ON t.oid=a.attrelid JOIN pg_namespace n ON n.oid=t.relnamespace LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum LEFT JOIN pg_collation coll ON coll.oid=a.attcollation WHERE n.nspname='public' AND t.relname={literal} AND a.attnum>0 AND NOT a.attisdropped ORDER BY a.attnum");
        var constraints = await Query($"SELECT c.conname,c.contype::text,pg_get_constraintdef(c.oid) FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace WHERE n.nspname='public' AND t.relname={literal} ORDER BY c.conname");
        var indexes = await Query($"SELECT indexname,indexdef FROM pg_indexes WHERE schemaname='public' AND tablename={literal} ORDER BY indexname");
        snapshot.Add(new { table = name, count, columns, constraints, indexes });
        var pk=string.Join("; ",constraints.Where(c=>c[1]=="p").Select(c=>c[2]));
        var fk=string.Join("; ",constraints.Where(c=>c[1]=="f").Select(c=>c[2]));
        var purpose=name switch {
            "spatial_ref_sys"=>"Hệ tọa độ PostGIS", "app_user"=>"Tài khoản", "step_data"=>"Dữ liệu bước đồng bộ",
            "region"=>"Vùng địa danh", "place"=>"Địa danh", "place_type"=>"Loại địa danh", "place_image"=>"Ảnh chính thức", "virtual_location"=>"Vị trí ảo",
            "journey"=>"Hành trình", "user_discovered_place"=>"Lịch sử khám phá đầu tiên", "card"=>"Danh mục thẻ", "collection"=>"Bộ sưu tập", "collection_card"=>"Thẻ thuộc bộ", "user_card"=>"Thẻ sở hữu",
            "item"=>"Vật phẩm", "user_item"=>"Kho vật phẩm", "quest"=>"Nhiệm vụ", "quest_progress"=>"Tiến độ và chi thưởng theo chu kỳ", "notification"=>"Thông báo",
            "user_preference"=>"Hồ sơ vận động", "user_preference_place_type"=>"Sở thích loại cũ", "interest_group"=>"Nhóm sở thích", "user_preference_interest"=>"Nhóm sở thích cá nhân",
            "place_type_feature"=>"Đặc trưng mặc định theo loại", "place_feature"=>"Cụm địa danh theo model", "place_feature_value"=>"Vector đặc trưng",
            "recommendation_config"=>"Phiên bản mô hình", "recommendation_criterion"=>"Tiêu chí và mã đặc trưng", "recommendation_weight"=>"Trọng số gợi ý", "recommendation_rating_snapshot"=>"Đánh giá theo phiên bản",
            "community_media"=>"Media cộng đồng có kiểm duyệt", "media_report"=>"Báo cáo nội dung", "place_video"=>"Video chính thức", "explorer_score_event"=>"Sổ điểm thành tích", _=>"Cần đối chiếu module"
        };
        md.AppendLine($"| {index + 1} | {name} | {pk} | {fk} | {count} | {purpose} |");
    }
    await transaction.RollbackAsync();
    var output = Path.Combine(root, "docs", "technical");
    Directory.CreateDirectory(output);
    var json = JsonSerializer.Serialize(snapshot, new JsonSerializerOptions { WriteIndented = true });
    // Definitions retain exact types, precision, FK actions, defaults and indexes from PostgreSQL.
    md.AppendLine("\n## Chi tiết cột, PK/FK, UNIQUE, CHECK và index\n\ncolumns: name, exact SQL type (including precision/length), NOT NULL, default, identity kind, generated kind, collation. constraints: name, kind, PostgreSQL definition (including FK actions). indexes: name, full definition.\n\n```json\n" + json + "\n```");
    var stage = target ? "after" : args.Contains("--recheck-source") ? "source_recheck" : "before";
    File.WriteAllText(Path.Combine(output, $"database_schema_{stage}.md"), md.ToString().Replace("schema before", $"schema {stage}"));
    File.WriteAllText(Path.Combine(output, $"database_schema_{stage}.snapshot.json"), json);
    Console.WriteLine($"Audited {tables.Count} public tables read-only. No schema/data writes executed.");
}
catch (Exception ex)
{
    // Never log the connection string or arbitrary server diagnostics containing secrets.
    Console.Error.WriteLine(ex is InvalidOperationException ? ex.Message : $"Audit failed ({ex.GetType().Name}). Check local settings and database availability.");
    Environment.ExitCode = 1;
}
