using System.Text.Json;
using Bcv.Api.Modules.Account.Application;
using Npgsql;
using NpgsqlTypes;

namespace Bcv.Api.Modules.Admin;

public sealed class AdminValidation(string message) : Exception(message);

public sealed class AdminRepository(NpgsqlDataSource db, ISessionStore sessions)
{
    public async Task<bool> IsAdmin(long id, CancellationToken ct)
    {
        await using var cmd = db.CreateCommand("SELECT EXISTS(SELECT 1 FROM app_user WHERE user_id=$1 AND role='ADMIN' AND status='ACTIVE')");
        cmd.Parameters.AddWithValue(id);
        return (bool)(await cmd.ExecuteScalarAsync(ct))!;
    }

    private static AdminResource Resource(string key) => AdminCatalog.Resources.TryGetValue(key, out var r) ? r : throw new AdminValidation("Chức năng không tồn tại.");
    private static JsonElement Json(string json) => JsonSerializer.Deserialize<JsonElement>(json);

    public async Task Delete(string key, long id, CancellationToken ct)
    {
        var r = Resource(key);
        await using var conn = await db.OpenConnectionAsync(ct);
        await using var tx = await conn.BeginTransactionAsync(ct);
        await using var check = new NpgsqlCommand($"SELECT {(key == "users" ? "role" : r.Id)} FROM {r.Table} WHERE {r.Id}=$1 FOR UPDATE", conn, tx);
        check.Parameters.AddWithValue(id);
        var existing = await check.ExecuteScalarAsync(ct);
        if (existing is null) throw new KeyNotFoundException("Bản ghi đã bị xóa hoặc không tồn tại.");
        if (key == "users" && existing is "ADMIN") throw new AdminValidation("Không được xóa tài khoản quản trị.");
        // Only remove owned media/membership rows. Other references preserve business history.
        var ownedSql = key switch
        {
            "places" => "DELETE FROM place_feature_value WHERE place_feature_id IN (SELECT place_feature_id FROM place_feature WHERE place_id=$1); DELETE FROM place_feature WHERE place_id=$1; DELETE FROM place_image WHERE place_id=$1",
            "collections" => "DELETE FROM collection_card WHERE collection_id=$1",
            _ => null
        };
        if (ownedSql is not null)
        {
            foreach (var statement in ownedSql.Split(';', StringSplitOptions.RemoveEmptyEntries))
            {
                await using var owned = new NpgsqlCommand(statement, conn, tx);
                owned.Parameters.AddWithValue(id);
                await owned.ExecuteNonQueryAsync(ct);
            }
        }
        try
        {
            await using var command = new NpgsqlCommand($"DELETE FROM {r.Table} WHERE {r.Id}=$1", conn, tx);
            command.Parameters.AddWithValue(id);
            await command.ExecuteNonQueryAsync(ct);
            await tx.CommitAsync(ct);
        }
        catch (PostgresException ex) when (ex.SqlState == "23503")
        {
            throw new AdminValidation("Không thể xóa vì bản ghi đang được sử dụng hoặc có lịch sử liên quan. Hãy gỡ liên kết trước hoặc dùng trạng thái Tạm ngưng.");
        }
        if (key == "users") sessions.RevokeAll(id);
    }

    public async Task<object> List(string key, string? q, string? status, long? regionId, long? typeId, int page, int pageSize, CancellationToken ct)
    {
        var r = Resource(key);
        page = Math.Clamp(page, 1, 100000); pageSize = Math.Clamp(pageSize, 1, 100);
        var direction = key == "regions" ? "ASC" : "DESC";
        var where = "WHERE ($1='' OR " + string.Join(" OR ", r.Search.Select(c => $"t.{c} ILIKE '%' || $1 || '%'")) + ")";
        if (r.Fields.Any(f => f.Name == "status")) where += " AND ($2='' OR t.status=$2)";
        else where += " AND $2=''";
        where += key == "places" ? " AND ($3=0 OR t.region_id=$3) AND ($4=0 OR t.place_type_id=$4)" : " AND $3=0 AND $4=0";
        var json = "to_jsonb(t)";
        if (key == "users") json = "to_jsonb(t)-'password_hash'";
        if (key == "places") json += " || jsonb_build_object('region_name',(SELECT name FROM region WHERE region_id=t.region_id),'type_name',(SELECT name FROM place_type WHERE place_type_id=t.place_type_id),'image_count',(SELECT count(*) FROM place_image WHERE place_id=t.place_id),'image_key',(SELECT image_key FROM place_image WHERE place_id=t.place_id ORDER BY place_image_id LIMIT 1))";
        if (key == "cards") json += " || jsonb_build_object('place_name',(SELECT name FROM place WHERE place_id=t.place_id),'collected_count',(SELECT count(*) FROM (SELECT user_id FROM public.user_card WHERE card_id=t.card_id UNION SELECT user_id FROM public.user_discovered_place WHERE place_id=t.place_id) owners))";
        if (key == "collections") json += " || jsonb_build_object('card_count',(SELECT count(*) FROM collection_card WHERE collection_id=t.collection_id))";
        if (key == "quests") json += " || jsonb_build_object('reward_name',(SELECT name FROM item WHERE item_id=t.reward_item_id),'completed_count',(SELECT count(*) FROM quest_progress WHERE quest_id=t.quest_id AND completion_status))";
        if (key == "regions") json += " || jsonb_build_object('place_count',(SELECT count(*) FROM place WHERE region_id=t.region_id))";
        await using var cmd = db.CreateCommand($"""
            SELECT jsonb_build_object('total',(SELECT count(*) FROM {r.Table} t {where}),
            'items',COALESCE((SELECT jsonb_agg(data) FROM (SELECT {json} AS data FROM {r.Table} t {where} ORDER BY t.{r.Id} {direction} LIMIT $5 OFFSET $6) s),'[]'::jsonb))::text
            """);
        cmd.Parameters.AddWithValue((q ?? "").Trim()[..Math.Min((q ?? "").Trim().Length, 200)]);
        cmd.Parameters.AddWithValue(status ?? ""); cmd.Parameters.AddWithValue(regionId ?? 0); cmd.Parameters.AddWithValue(typeId ?? 0);
        cmd.Parameters.AddWithValue(pageSize); cmd.Parameters.AddWithValue((page - 1) * pageSize);
        return Json((string)(await cmd.ExecuteScalarAsync(ct))!);
    }

    public async Task<JsonElement?> Detail(string key, long id, CancellationToken ct)
    {
        var r = Resource(key);
        var extra = key switch
        {
            "places" => " || jsonb_build_object('images',COALESCE((SELECT jsonb_agg(image_key ORDER BY place_image_id) FROM place_image WHERE place_id=t.place_id),'[]'::jsonb),'feature_codes',COALESCE((SELECT jsonb_agg(c.code ORDER BY c.criterion_id) FROM place_feature pf JOIN place_feature_value fv ON fv.place_feature_id=pf.place_feature_id JOIN recommendation_criterion c ON c.criterion_id=fv.criterion_id WHERE pf.place_id=t.place_id AND fv.feature_value=1 AND pf.recommendation_config_id=(SELECT recommendation_config_id FROM recommendation_config ORDER BY (status='ACTIVE') DESC,updated_at DESC,recommendation_config_id DESC LIMIT 1)),'[]'::jsonb))",
            "collections" => " || jsonb_build_object('card_ids',COALESCE((SELECT jsonb_agg(card_id) FROM collection_card WHERE collection_id=t.collection_id),'[]'::jsonb))",
            "users" => " || jsonb_build_object('journey_count',(SELECT count(*) FROM journey WHERE user_id=t.user_id),'discovered_count',(SELECT count(*) FROM user_discovered_place WHERE user_id=t.user_id),'card_count',(SELECT count(*) FROM public.card c WHERE c.status='ACTIVE' AND (EXISTS(SELECT 1 FROM public.user_card uc WHERE uc.user_id=t.user_id AND uc.card_id=c.card_id) OR EXISTS(SELECT 1 FROM public.user_discovered_place d WHERE d.user_id=t.user_id AND d.place_id=c.place_id))),'total_steps',(SELECT COALESCE(sum(step_count),0) FROM step_data WHERE user_id=t.user_id))",
            _ => ""
        };
        var row = key == "users" ? "(to_jsonb(t)-'password_hash')" : "to_jsonb(t)";
        await using var cmd = db.CreateCommand($"SELECT ({row}{extra})::text FROM {r.Table} t WHERE t.{r.Id}=$1");
        cmd.Parameters.AddWithValue(id);
        return await cmd.ExecuteScalarAsync(ct) is string json ? Json(json) : null;
    }

    private static Dictionary<string, object?> Validate(AdminResource r, JsonElement body)
    {
        if (body.ValueKind != JsonValueKind.Object) throw new AdminValidation("Dữ liệu phải là một đối tượng.");
        var result = new Dictionary<string, object?>();
        foreach (var f in r.Fields)
        {
            var present = body.TryGetProperty(f.Name, out var v) && v.ValueKind != JsonValueKind.Null && v.ToString().Trim() != "";
            if (!present)
            {
                if (f.Required) throw new AdminValidation($"Thiếu trường bắt buộc: {f.Name}.");
                result[f.Name] = null; continue;
            }
            if (f.Type == NpgsqlDbType.Text)
            {
                if (v.ValueKind != JsonValueKind.String) throw new AdminValidation($"Trường {f.Name} phải là văn bản.");
                var value = v.GetString()!.Trim();
                if (f.MaxLength > 0 && value.Length > f.MaxLength) throw new AdminValidation($"Trường {f.Name} quá dài (tối đa {f.MaxLength} ký tự).");
                if (f.Options is not null && !f.Options.Contains(value)) throw new AdminValidation($"Giá trị {f.Name} không hợp lệ.");
                result[f.Name] = value;
            }
            else
            {
                if (v.ValueKind != JsonValueKind.Number || !v.TryGetDecimal(out var number) || (f.Min.HasValue && number < f.Min) || (f.Max.HasValue && number > f.Max))
                    throw new AdminValidation($"Giá trị {f.Name} ngoài phạm vi cho phép.");
                if (f.Type != NpgsqlDbType.Numeric && decimal.Truncate(number) != number) throw new AdminValidation($"Trường {f.Name} phải là số nguyên.");
                if (f.Type == NpgsqlDbType.Integer && (number > int.MaxValue || number < int.MinValue)) throw new AdminValidation($"Trường {f.Name} quá lớn.");
                if (f.Type == NpgsqlDbType.Bigint && (number > long.MaxValue || number < 1)) throw new AdminValidation($"Mã {f.Name} không hợp lệ.");
                result[f.Name] = f.Type switch { NpgsqlDbType.Bigint => (object)(long)number, NpgsqlDbType.Integer => (int)number, _ => number };
            }
        }
        return result;
    }

    private static bool IsImage(string value) => value.StartsWith("data:image/jpeg;base64,", StringComparison.Ordinal) ||
        (Uri.TryCreate(value, UriKind.Absolute, out var uri) && uri.Scheme == "https" && !string.IsNullOrEmpty(uri.Host));

    public async Task<object> Save(string key, long? id, JsonElement body, long actorId, CancellationToken ct)
    {
        var r = Resource(key);
        if (id is null && !r.CanCreate) throw new AdminValidation("Chức năng này chỉ cho phép cập nhật.");
        var values = Validate(r, body);
        if (values.GetValueOrDefault("image_key") is string img && !IsImage(img)) throw new AdminValidation("Ảnh phải là URL HTTPS hoặc ảnh JPEG đã tải lên.");
        await using var conn = await db.OpenConnectionAsync(ct);
        await using var tx = await conn.BeginTransactionAsync(ct);
        async Task<object?> Scalar(string sql, params object[] args)
        {
            await using var cmd = new NpgsqlCommand(sql, conn, tx);
            foreach (var arg in args) cmd.Parameters.AddWithValue(arg);
            return await cmd.ExecuteScalarAsync(ct);
        }
        if (id is not null && await Scalar($"SELECT {r.Id} FROM {r.Table} WHERE {r.Id}=$1 FOR UPDATE", id.Value) is null)
            throw new KeyNotFoundException("Không tìm thấy bản ghi.");
        if (key == "users")
        {
            if (id == actorId) throw new AdminValidation("Không thể thay đổi trạng thái tài khoản đang đăng nhập.");
            if (await Scalar("SELECT status FROM app_user WHERE user_id=$1", id!.Value) is "DELETED")
                throw new AdminValidation("Tài khoản đã được người dùng đóng; không thể kích hoạt lại qua quản trị.");
            if (await Scalar("SELECT role FROM app_user WHERE user_id=$1", id!.Value) is string role && role == "ADMIN")
                throw new AdminValidation("Chức năng này chỉ thay đổi trạng thái người dùng thông thường.");
        }
        if (key == "quests")
        {
            if (!Equals(values["quest_type"], values["cycle_type"])) throw new AdminValidation("Loại nhiệm vụ phải khớp chu kỳ ngày/tuần.");
            if ((values["reward_item_id"] is null) != (values["reward_quantity"] is null)) throw new AdminValidation("Hãy chọn vật phẩm và số lượng thưởng cùng nhau.");
            if (id is not null && await Scalar("SELECT EXISTS(SELECT 1 FROM quest_progress WHERE quest_id=$1)", id.Value) is true)
            {
                if (await Scalar("SELECT quest_type=$2 AND condition_type=$3 AND target_value=$4 AND cycle_type=$5 AND reward_item_id IS NOT DISTINCT FROM $6::bigint AND reward_quantity IS NOT DISTINCT FROM $7::integer FROM quest WHERE quest_id=$1", id.Value, values["quest_type"]!, values["condition_type"]!, values["target_value"]!, values["cycle_type"]!, values["reward_item_id"] ?? DBNull.Value, values["reward_quantity"] ?? DBNull.Value) is false)
                    throw new AdminValidation("Nhiệm vụ đã có tiến độ: chỉ sửa tên/trạng thái. Hãy tạo nhiệm vụ mới để thay đổi điều kiện hoặc phần thưởng.");
            }
        }
        if (key == "items")
        {
            var type = (string)values["item_type"]!;
            if (id is not null && !Equals(await Scalar("SELECT item_type FROM item WHERE item_id=$1", id.Value), type))
                throw new AdminValidation("Không thể đổi loại vật phẩm đã tạo. Hãy tạo vật phẩm mới.");
            if (type == "SPEED_SHOES" && (values["effect_value"] is not decimal effect || effect <= 1 || values["effective_range"] is not int range || range < 1))
                throw new AdminValidation("Giày tăng tốc cần hệ số lớn hơn 1 và số bước hiệu lực lớn hơn 0.");
            if (type == "FLIGHT_TICKET") { values["effect_value"] = null; values["effective_range"] = null; }
        }
        if (key == "places" && Equals(values["status"], "ACTIVE") &&
            await Scalar("SELECT region_id FROM region WHERE region_id=$1 AND status='ACTIVE' FOR SHARE", values["region_id"]!) is null)
            throw new AdminValidation("Chỉ kích hoạt địa danh thuộc khu vực đang hoạt động.");
        if (key == "cards" && Equals(values["status"], "ACTIVE"))
        {
            if (values["image_key"] is null) throw new AdminValidation("Thẻ đang hoạt động phải có hình ảnh.");
            if (await Scalar("SELECT place_id FROM place WHERE place_id=$1 AND status='ACTIVE' FOR SHARE", values["place_id"]!) is null)
                throw new AdminValidation("Địa danh của thẻ phải đang hoạt động.");
        }
        var columns = r.Fields.Select(f => f.Name).ToArray();
        var sql = id is null
            ? $"INSERT INTO {r.Table} ({string.Join(',', columns)}) VALUES ({string.Join(',', columns.Select((_, i) => "$" + (i + 1)))}) RETURNING {r.Id}"
            : $"UPDATE {r.Table} SET {string.Join(',', columns.Select((c, i) => c + "=$" + (i + 1)))} WHERE {r.Id}=${columns.Length + 1} RETURNING {r.Id}";
        long savedId;
        await using (var cmd = new NpgsqlCommand(sql, conn, tx))
        {
            foreach (var f in r.Fields) cmd.Parameters.Add(new NpgsqlParameter { NpgsqlDbType = f.Type, Value = values[f.Name] ?? DBNull.Value });
            if (id is not null) cmd.Parameters.AddWithValue(id.Value);
            savedId = (long)(await cmd.ExecuteScalarAsync(ct))!;
        }
        if (key == "places")
        {
            if (!body.TryGetProperty("images", out var array) || array.ValueKind != JsonValueKind.Array || array.GetArrayLength() > 30)
                throw new AdminValidation("Danh sách ảnh phải là mảng, tối đa 30 ảnh.");
            var images = array.EnumerateArray().Select(e => e.ValueKind == JsonValueKind.String ? e.GetString()!.Trim() : "").Distinct().ToArray();
            if (images.Any(s => s.Length > 8000000 || !IsImage(s))) throw new AdminValidation("Danh sách có URL ảnh không hợp lệ; sử dụng HTTPS.");
            await Scalar("DELETE FROM place_image WHERE place_id=$1", savedId);
            foreach (var image in images) await Scalar("INSERT INTO place_image(place_id,image_key) VALUES($1,$2) RETURNING place_image_id", savedId, image);
            await AdminPlaceFeatures.Save(conn, tx, savedId, body, ct);
        }
        if (key == "collections")
        {
            if (!body.TryGetProperty("card_ids", out var array) || array.ValueKind != JsonValueKind.Array || array.GetArrayLength() > 5000)
                throw new AdminValidation("Danh sách thẻ không hợp lệ.");
            var ids = array.EnumerateArray().Select(e => e.ValueKind == JsonValueKind.Number && e.TryGetInt64(out var value) && value > 0 ? value : throw new AdminValidation("Mã thẻ không hợp lệ.")).Distinct().ToArray();
            await Scalar("DELETE FROM collection_card WHERE collection_id=$1", savedId);
            foreach (var cardId in ids)
            {
                if (Equals(values["status"], "ACTIVE") && await Scalar("SELECT card_id FROM card WHERE card_id=$1 AND status='ACTIVE' FOR SHARE", cardId) is null)
                    throw new AdminValidation("Bộ sưu tập đang hoạt động chỉ được chứa thẻ đang hoạt động. Hãy kích hoạt thẻ hoặc lưu bộ sưu tập dưới dạng bản nháp.");
                await Scalar("INSERT INTO collection_card(collection_id,card_id) VALUES($1,$2) RETURNING collection_card_id", savedId, cardId);
            }
        }
        await tx.CommitAsync(ct);
        if (key == "users" && !Equals(values["status"], "ACTIVE")) sessions.RevokeAll(savedId);
        return new { id = savedId };
    }

    public async Task<object> Lookups(CancellationToken ct)
    {
        await using var cmd = db.CreateCommand("""
            SELECT jsonb_build_object(
              'regions',COALESCE((SELECT jsonb_agg(jsonb_build_object('id',region_id,'name',name,'status',status) ORDER BY region_id ASC) FROM region),'[]'),
              'types',COALESCE((SELECT jsonb_agg(jsonb_build_object('id',place_type_id,'name',name) ORDER BY name) FROM place_type),'[]'),
              'features',COALESCE((SELECT jsonb_agg(jsonb_build_object('code',code,'name',name,'description',description) ORDER BY criterion_id) FROM recommendation_criterion WHERE code LIKE 'kind\_\_%' ESCAPE '\' OR code LIKE 'landform\_\_%' ESCAPE '\' OR code LIKE 'ecosystem\_\_%' ESCAPE '\' OR code LIKE 'theme\_\_%' ESCAPE '\'),'[]'),
              'places',COALESCE((SELECT jsonb_agg(jsonb_build_object('id',place_id,'name',name,'latitude',latitude,'longitude',longitude,'status',status,'region_id',region_id,'image_key',(SELECT image_key FROM place_image i WHERE i.place_id=p.place_id ORDER BY place_image_id LIMIT 1)) ORDER BY name) FROM place p),'[]'),
              'cards',COALESCE((SELECT jsonb_agg(jsonb_build_object('id',card_id,'name',name,'status',status) ORDER BY name) FROM card),'[]'),
              'items',COALESCE((SELECT jsonb_agg(jsonb_build_object('id',item_id,'name',name,'status',status) ORDER BY name) FROM item),'[]')
            )::text
            """);
        return Json((string)(await cmd.ExecuteScalarAsync(ct))!);
    }

    public async Task<object> Dashboard(int days, CancellationToken ct)
    {
        days = new[] { 7, 30, 90 }.Contains(days) ? days : 30;
        await using var cmd = db.CreateCommand("""
            SELECT jsonb_build_object(
              'users',(SELECT count(*) FROM app_user WHERE role='USER'),
              'journeys',(SELECT count(*) FROM journey),
              'completed',(SELECT count(*) FROM journey WHERE status='COMPLETED'),
              'discoveries',(SELECT count(*) FROM user_discovered_place),
              'collected',(SELECT count(*) FROM user_card),
              'steps',(SELECT COALESCE(sum(step_count),0) FROM step_data),
              'places',(SELECT count(*) FROM place),
              'activePlaces',(SELECT count(*) FROM place WHERE status='ACTIVE'),
              'regions',(SELECT count(*) FROM region),
              'cards',(SELECT count(*) FROM card),
              'collections',(SELECT count(*) FROM collection),
              'quests',(SELECT count(*) FROM quest WHERE status='ACTIVE'),
              'missingImages',(SELECT count(*) FROM place p WHERE NOT EXISTS(SELECT 1 FROM place_image i WHERE i.place_id=p.place_id)),
              'draftCards',(SELECT count(*) FROM card WHERE status='DRAFT'),
              'trend', (SELECT jsonb_agg(jsonb_build_object('date',d::date,'journeys',(SELECT count(*) FROM journey WHERE started_at>=d AND started_at<d+interval '1 day'),'users',(SELECT count(*) FROM app_user WHERE created_at>=d AND created_at<d+interval '1 day')) ORDER BY d) FROM generate_series(CURRENT_DATE-($1-1),CURRENT_DATE,interval '1 day') d),
              'topRegions',COALESCE((SELECT jsonb_agg(to_jsonb(s) ORDER BY s.region_id ASC) FROM (SELECT r.region_id,r.name,count(p.place_id) AS places,(SELECT count(*) FROM user_discovered_place u JOIN place p2 ON p2.place_id=u.place_id WHERE p2.region_id=r.region_id) AS discoveries FROM region r LEFT JOIN place p ON p.region_id=r.region_id GROUP BY r.region_id ORDER BY r.region_id ASC LIMIT 5) s),'[]'),
              'recentJourneys',COALESCE((SELECT jsonb_agg(to_jsonb(s)) FROM (SELECT j.journey_id,u.full_name,p.name AS destination,j.status,j.recorded_steps,j.required_steps,j.started_at FROM journey j JOIN app_user u ON u.user_id=j.user_id JOIN place p ON p.place_id=j.destination_place_id ORDER BY j.started_at DESC LIMIT 5) s),'[]')
            )::text
            """);
        cmd.Parameters.AddWithValue(days);
        return Json((string)(await cmd.ExecuteScalarAsync(ct))!);
    }
}
