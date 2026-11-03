using NpgsqlTypes;

namespace Bcv.Api.Modules.Admin;

public sealed record AdminField(string Name, NpgsqlDbType Type, bool Required = false, int MaxLength = 0,
    decimal? Min = null, decimal? Max = null, string[]? Options = null);
public sealed record AdminResource(string Table, string Id, string[] Search, AdminField[] Fields, bool CanCreate = true);

// SQL identifiers come exclusively from this catalog, never from request bodies or query strings.
public static class AdminCatalog
{
    private static AdminField Text(string name, int max = 0, bool required = false) => new(name, NpgsqlDbType.Text, required, max);
    private static AdminField Number(string name, NpgsqlDbType type, bool required = false, decimal? min = null, decimal? max = null) => new(name, type, required, Min: min, Max: max);
    private static AdminField Choice(string name, params string[] options) => new(name, NpgsqlDbType.Text, true, Options: options);
    private static AdminField Status() => Choice("status", "DRAFT", "ACTIVE", "INACTIVE");
    public static readonly IReadOnlyDictionary<string, AdminResource> Resources = new Dictionary<string, AdminResource>
    {
        ["users"] = new("app_user", "user_id", ["full_name", "email"], [Choice("status", "ACTIVE", "BLOCKED", "INACTIVE")], false),
        ["regions"] = new("region", "region_id", ["name", "description"], [Text("name", 150, true), Text("description", 20000), Status()]),
        ["place-types"] = new("place_type", "place_type_id", ["name"], [Text("name", 100, true)]),
        ["places"] = new("place", "place_id", ["name", "introduction"], [Text("name", 200, true), Number("region_id", NpgsqlDbType.Bigint, true, 1), Number("place_type_id", NpgsqlDbType.Bigint, true, 1), Number("latitude", NpgsqlDbType.Numeric, true, -90, 90), Number("longitude", NpgsqlDbType.Numeric, true, -180, 180), Text("introduction", 50000), Text("highlights", 20000), Number("rating", NpgsqlDbType.Numeric, false, 0, 5), Number("rating_count", NpgsqlDbType.Integer, false, 0), Status()]),
        ["cards"] = new("card", "card_id", ["name", "description"], [Text("name", 150, true), Number("place_id", NpgsqlDbType.Bigint, true, 1), Text("image_key", 8000000), Text("description", 20000), Status()]),
        ["collections"] = new("collection", "collection_id", ["name", "description"], [Text("name", 150, true), Text("description", 20000), Status()]),
        ["quests"] = new("quest", "quest_id", ["name"], [Text("name", 200, true), Choice("quest_type", "DAILY", "WEEKLY"), Choice("condition_type", "STEP_COUNT", "DISCOVER_PLACE", "COMPLETE_JOURNEY"), Number("target_value", NpgsqlDbType.Integer, true, 1), Choice("cycle_type", "DAILY", "WEEKLY"), Number("reward_item_id", NpgsqlDbType.Bigint, false, 1), Number("reward_quantity", NpgsqlDbType.Integer, false, 1), Status()]),
        ["items"] = new("item", "item_id", ["name", "description"], [Text("name", 150, true), Choice("item_type", "SPEED_SHOES", "FLIGHT_TICKET"), Text("description", 20000), Number("effect_value", NpgsqlDbType.Numeric, false, 1, 100), Number("effective_range", NpgsqlDbType.Integer, false, 1), Number("usage_limit", NpgsqlDbType.Integer, false, 0), Status()])
    };
}
