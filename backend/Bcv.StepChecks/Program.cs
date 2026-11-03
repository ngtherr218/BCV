using Bcv.Api.Modules.Discovery.Infrastructure;
using Bcv.Api.Modules.Journey.Application;
using Bcv.Api.Modules.Journey.Infrastructure;
using Bcv.Api.Modules.QuestItem.Application;
using Bcv.Api.Modules.QuestItem.Infrastructure;
using Npgsql;
using System.Text.Json;
using Bcv.Api.Modules.Admin;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Bcv.Api.Modules.Account.Infrastructure;
using Bcv.Api.Modules.PlaceCollection.Infrastructure;

var settings = new NpgsqlConnectionStringBuilder(Environment.GetEnvironmentVariable("BCV_STEP_CHECK_CONNECTION")
    ?? throw new Exception("Set BCV_STEP_CHECK_CONNECTION to an isolated copy of the database."));
if (!settings.Database!.StartsWith("bcv_step_checks_", StringComparison.Ordinal))
    throw new Exception("Only isolated bcv_step_checks_* databases are allowed. Never point these fixtures at a live database.");
await using var db = NpgsqlDataSource.Create(settings.ConnectionString);
await AccountSchema.EnsureAsync(db);
await AccountSchema.EnsureAsync(db); // Upgrade is safe to repeat.
await CollectionSchema.EnsureAsync(db);
await CollectionSchema.EnsureAsync(db);
await Bcv.Api.Modules.StatsNotify.Infrastructure.NotificationSchema.EnsureAsync(db);
void Check(bool condition, string message) { if (!condition) throw new Exception(message); }
async Task<object?> Sql(string text, params object[] parameters)
{
    await using var command = db.CreateCommand(text);
    foreach (var parameter in parameters) command.Parameters.AddWithValue(parameter);
    return await command.ExecuteScalarAsync();
}
var user = (long)(await Sql("""
    INSERT INTO app_user(full_name,email,password_hash,role,status)
    VALUES('Step test fixture',$1,'not-a-login-hash','USER','ACTIVE') RETURNING user_id
    """, $"step-check-{Guid.NewGuid():N}@example.invalid"))!;
await Sql("UPDATE item SET status='ACTIVE' WHERE item_type IN ('SPEED_SHOES','FLIGHT_TICKET')");
var shoesId = (long)(await Sql("SELECT item_id FROM item WHERE item_type='SPEED_SHOES' ORDER BY item_id LIMIT 1"))!;
var ticketId = (long)(await Sql("SELECT item_id FROM item WHERE item_type='FLIGHT_TICKET' ORDER BY item_id LIMIT 1"))!;
await Sql("UPDATE item SET effect_value=2,effective_range=5000 WHERE item_id=$1", shoesId);
await Sql("INSERT INTO user_item(user_id,item_id,quantity) VALUES($1,$2,2),($1,$3,2)", user, shoesId, ticketId);
var from = (long)(await Sql("SELECT place_id FROM place WHERE status='ACTIVE' ORDER BY place_id LIMIT 1"))!;
var to = (long)(await Sql("SELECT place_id FROM place WHERE status='ACTIVE' AND place_id<>$1 ORDER BY place_id LIMIT 1", from))!;
await Sql("""
    INSERT INTO virtual_location(user_id,region_id,nearest_place_id,latitude,longitude)
    SELECT $1,region_id,place_id,latitude,longitude FROM place WHERE place_id=$2
    """, user, from);
var trip = (long)(await Sql("""
    INSERT INTO journey(user_id,start_place_id,start_latitude,start_longitude,destination_place_id,distance,required_steps,status)
    SELECT $1,place_id,latitude,longitude,$3,1,1000,'IN_PROGRESS' FROM place WHERE place_id=$2 RETURNING journey_id
    """, user, from, to))!;
var quest = (long)(await Sql("""
    INSERT INTO quest(reward_item_id,name,quest_type,condition_type,target_value,cycle_type,reward_quantity,status)
    VALUES($1,'Step check: 400 raw steps','DAILY','STEP_COUNT',400,'DAILY',2,'ACTIVE') RETURNING quest_id
    """, shoesId))!;
var explore = new PostgresExploreRepository(db);
var service = new JourneyService(new PostgresJourneyRepository(db), explore);
var items = new ItemUseRepository(db);
var quests = new QuestRepository(db);
async Task<int> Quantity(long item) => (int)(await Sql("SELECT quantity FROM user_item WHERE user_id=$1 AND item_id=$2", user, item))!;
async Task<int> QuestSteps() => (int)(await Sql("SELECT progress_value FROM quest_progress WHERE user_id=$1 AND quest_id=$2", user, quest))!;

var first = await service.SyncStepsAsync(user, new(trip, 200), default);
Check(first.Journey is { SourceSteps: 200, RecordedSteps: 200 }, "Raw steps did not advance journey");
var expectedStartName = (string)(await Sql("SELECT name FROM place WHERE place_id=$1", from))!;
Check(first.Journey?.StartPlaceName == expectedStartName, "Journey start place name is missing");
Check(first.Journey.ActiveItem is null, "An unused item appeared active");
Check(await QuestSteps() == 200, "Raw steps did not advance quest");
await items.UseShoesAsync(user, default);
Check(await Quantity(shoesId) == 1, "Shoes were not consumed once");
var equipped = await explore.LoadAsync(user, default);
Check(equipped.Journey?.ActiveItem is { ItemType: "SPEED_SHOES", Multiplier: 2, Range: 5000, RemainingSteps: 5000 }, "Equipped shoes indicator is missing");
var boosted = await service.SyncStepsAsync(user, new(trip, 400), default);
Check(boosted.Journey is { SourceSteps: 400, RecordedSteps: 600 }, "Shoes should boost only journey, not raw steps");
Check(boosted.Journey.ActiveItem is { RemainingSteps: 4800 }, "Item indicator must count raw steps remaining");
Check(await QuestSteps() == 400, "Quest counted boosted steps instead of raw steps");
await quests.LoadAsync(user, default);
var rewarded = await Quantity(shoesId);
Check(rewarded >= 3, "Completed quest did not award items");
await service.SyncStepsAsync(user, new(trip, 400), default);
await quests.LoadAsync(user, default);
Check(await QuestSteps() == 400 && await Quantity(shoesId) == rewarded, "Duplicate sync repeated progress/reward");
try { await items.UseFlightAsync(user, from, default); throw new Exception("Flight allowed during journey"); }
catch (ItemUseException error) when (error.Code == "JOURNEY_ACTIVE") { }
Check(await Quantity(ticketId) == 2, "Rejected flight consumed a ticket");
var completed = await service.SyncStepsAsync(user, new(trip, 800), default);
Check(completed.Journey is { SourceSteps: 800, RecordedSteps: 1000, Status: "COMPLETED" }, "Journey did not complete with raw steps and shoes bonus");
Check(completed.Journey?.StartPlaceName == expectedStartName, "Completed journey lost its original start place");
Check(completed.Journey.ActiveItem is null, "Completed journey still shows active shoes");
Check((long)(await Sql("SELECT nearest_place_id FROM virtual_location WHERE user_id=$1", user))! == to, "Completion did not move virtual location");
Check((long)(await Sql("SELECT count(*) FROM user_discovered_place WHERE user_id=$1 AND place_id=$2", user, to))! == 1, "Completion did not unlock place exactly once");
await service.SyncStepsAsync(user, new(trip, 800), default);
Check(await QuestSteps() == 800, "Completion retry counted steps again");
var landing = (long)(await Sql("SELECT place_id FROM place WHERE status='ACTIVE' AND region_id<>(SELECT region_id FROM place WHERE place_id=$1) ORDER BY place_id LIMIT 1", to))!;
await items.UseFlightAsync(user, landing, default);
Check(await Quantity(ticketId) == 1, "Flight did not consume exactly one ticket");
Check((long)(await Sql("SELECT nearest_place_id FROM virtual_location WHERE user_id=$1", user))! == landing, "Flight did not move virtual location");
try { await service.SyncStepsAsync(user + 10_000_000, new(trip, 900), default); throw new Exception("Another user could sync the journey"); }
catch (JourneyException error) when (error.Code == "JOURNEY_NOT_FOUND") { }
var admin = new AdminRepository(db, null!); // Item saves do not use the session store; HTTP endpoints enforce admin access.
var newItemBody = JsonSerializer.SerializeToElement(new { name = "Fixture new shoes", item_type = "SPEED_SHOES",
    description = "Isolated fixture", effect_value = 3, effective_range = 100, usage_limit = (int?)null, status = "ACTIVE" });
var createdItem = JsonSerializer.SerializeToElement(await admin.Save("items", null, newItemBody, user, default)).GetProperty("id").GetInt64();
Check((string)(await Sql("SELECT item_type FROM item WHERE item_id=$1", createdItem))! == "SPEED_SHOES", "Admin could not create item");
var changedType = JsonSerializer.SerializeToElement(new { name = "Invalid type change", item_type = "FLIGHT_TICKET", status = "ACTIVE" });
try { await admin.Save("items", createdItem, changedType, user, default); throw new Exception("Item type could be changed"); }
catch (AdminValidation) { }
await Sql("INSERT INTO user_item(user_id,item_id,quantity) VALUES($1,$2,1)", user, createdItem);
var secondTrip = (long)(await Sql("""
    INSERT INTO journey(user_id,start_place_id,start_latitude,start_longitude,destination_place_id,distance,required_steps,status)
    SELECT $1,place_id,latitude,longitude,$3,10,100000,'IN_PROGRESS' FROM place WHERE place_id=$2 RETURNING journey_id
    """, user, landing, to))!;
var originalQuantity = await Quantity(shoesId);
var selectedStatus = await items.UseShoesAsync(user, default, createdItem);
Check(selectedStatus.ItemId == createdItem && selectedStatus.Multiplier == 3, "Wrong shoe variant was used");
Check(await Quantity(createdItem) == 0 && await Quantity(shoesId) == originalQuantity, "Wrong inventory item was consumed");
var expiredBoost = await service.SyncStepsAsync(user, new(secondTrip, 200), default);
Check(expiredBoost.Journey is { Status: "IN_PROGRESS", RecordedSteps: 400, ActiveItem: null }, "Expired item is still active");
try { await items.ShoesAsync(user, default, ticketId); throw new Exception("Wrong item type accepted"); }
catch (ItemUseException error) when (error.Code == "ITEM_NOT_FOUND") { }
Console.WriteLine("PASS: cumulative steps, quest rewards, item effects/expiry, admin creation, selected item consumption, immutable type, ownership and flight restrictions.");

var loginEmail = $"login-check-{Guid.NewGuid():N}@example.invalid";
const string loginPassword = "Fixture-login-123!";
await Sql("UPDATE app_user SET email=$2,password_hash=$3 WHERE user_id=$1", user, loginEmail, PasswordHasher.Hash(loginPassword));
var loginSessions = new CheckSessions();
var accounts = new AccountService(new PostgresAccountRepository(db), loginSessions);
async Task<DateTime?> LoginTime() => await Sql("SELECT last_login_at FROM app_user WHERE user_id=$1", user) is DateTime value ? value : null;
Check(await LoginTime() is null, "Historical login time was invented");
try { await accounts.Login(new(loginEmail, "wrong"), default); throw new Exception("Wrong password accepted"); }
catch (AccountException error) when (error.Code == "INVALID_CREDENTIALS") { }
Check(await LoginTime() is null, "Failed login updated timestamp");
await accounts.Login(new(loginEmail, loginPassword), default);
var firstLoginTime = await LoginTime();
Check(firstLoginTime is not null && firstLoginTime.Value.Kind == DateTimeKind.Utc, "Successful login did not store UTC time");
await accounts.GetActiveUser(user, default);
Check(await LoginTime() == firstLoginTime, "Session restore updated last login");
try { await accounts.Login(new(loginEmail, loginPassword), default, requireAdmin: true); throw new Exception("Non-admin accepted"); }
catch (AccountException error) when (error.Code == "FORBIDDEN") { }
Check(await LoginTime() == firstLoginTime, "Rejected admin login updated timestamp");
await Sql("UPDATE app_user SET status='BLOCKED' WHERE user_id=$1", user);
try { await accounts.Login(new(loginEmail, loginPassword), default); throw new Exception("Blocked user accepted"); }
catch (AccountException error) when (error.Code == "ACCOUNT_INACTIVE") { }
Check(await LoginTime() == firstLoginTime, "Blocked login updated timestamp");
await Sql("UPDATE app_user SET status='ACTIVE',role='ADMIN' WHERE user_id=$1", user);
await accounts.Login(new(loginEmail, loginPassword), default, requireAdmin: true);
Check(await LoginTime() > firstLoginTime, "Latest successful admin login did not update timestamp");
var usersJson = JsonSerializer.SerializeToElement(await admin.List("users", loginEmail, null, null, null, 1, 12, default));
Check(usersJson.GetProperty("items")[0].GetProperty("last_login_at").ValueKind == JsonValueKind.String, "Admin list is missing last login time");
Check(!usersJson.GetProperty("items")[0].TryGetProperty("password_hash", out _), "Admin list leaked password hash");
Console.WriteLine("PASS: last-login migration, successful/failed/blocked/admin logins, session restoration and admin list.");

// Reproduce places discovered while their cards were DRAFT, then activated later.
await Sql("""
    INSERT INTO card(name,place_id,status)
    SELECT 'Fixture collectible',$1,'ACTIVE' WHERE NOT EXISTS(SELECT 1 FROM card WHERE place_id=$1)
    """, to);
await Sql("UPDATE card SET status='ACTIVE' WHERE place_id=$1", to);
await Sql("DELETE FROM user_card WHERE user_id=$1", user);
var collectibleId = (long)(await Sql("SELECT card_id FROM card WHERE place_id=$1 ORDER BY card_id LIMIT 1", to))!;
var appCollections = new CollectionRepository(db);
async Task CheckCardCounts()
{
    var appOwned = (await appCollections.LoadAsync(user, default)).OwnedCards;
    var adminUser = await admin.Detail("users", user, default);
    Check(appOwned > 0 && adminUser!.Value.GetProperty("card_count").GetInt32() == appOwned,
        "Admin and app disagree about cards from discovered places");
}
await CheckCardCounts();
var beforeExplicitGrant = (await appCollections.LoadAsync(user, default)).OwnedCards;
async Task<int> AdminCardOwners()
{
    var name = (string)(await Sql("SELECT name FROM card WHERE card_id=$1", collectibleId))!;
    var list = JsonSerializer.SerializeToElement(await admin.List("cards", name, null, null, null, 1, 100, default));
    return list.GetProperty("items").EnumerateArray().Single(c => c.GetProperty("card_id").GetInt64() == collectibleId)
        .GetProperty("collected_count").GetInt32();
}
var beforeOwners = await AdminCardOwners();
Check(beforeOwners >= 1, "Admin card owner count missed discovered ownership");
await Sql("INSERT INTO user_card(user_id,card_id) VALUES($1,$2)", user, collectibleId);
await CheckCardCounts();
Check(await AdminCardOwners() == beforeOwners, "Admin card owners double counted the same user");
Check((await appCollections.LoadAsync(user, default)).OwnedCards == beforeExplicitGrant, "Explicit and discovered ownership double counted");
Console.WriteLine("PASS: admin/app card counts agree after activation, with no double counting.");

// New-card badges count unique cards, survive notification changes and clear only on opening a card.
var badgeCollection = (long)(await Sql("INSERT INTO collection(name,status) VALUES('Badge fixture','ACTIVE') RETURNING collection_id"))!;
await Sql("INSERT INTO collection_card(collection_id,card_id) VALUES($1,$2)", badgeCollection, collectibleId);
var newOverview = await appCollections.LoadAsync(user, default);
Check(newOverview.NewCards == 1, "A newly granted card is missing from the global badge");
Check(newOverview.Collections.Single(c => c.CollectionId == badgeCollection).NewCards == 1, "Collection badge is missing its new card");
Check((await appCollections.LoadDetailAsync(user, badgeCollection, default))!.Cards.Single().IsNew, "Card tile is not marked new");
await CollectionSchema.EnsureAsync(db);
Check((await appCollections.LoadAsync(user, default)).NewCards == 1, "Restarting the API cleared unseen cards");
await Sql("DELETE FROM notification WHERE user_id=$1", user);
Check((await appCollections.LoadAsync(user, default)).NewCards == 1, "Deleting notifications cleared the card badge");
await appCollections.MarkSeenAsync(0, collectibleId, default);
Check((await appCollections.LoadAsync(user, default)).NewCards == 1, "Another account cleared the card badge");
await appCollections.MarkSeenAsync(user, collectibleId, default);
await appCollections.MarkSeenAsync(user, collectibleId, default);
var seenOverview = await appCollections.LoadAsync(user, default);
Check(seenOverview.NewCards == 0 && seenOverview.OwnedCards == newOverview.OwnedCards, "Opening a card did not clear only the new count");
Check(seenOverview.Collections.Single(c => c.CollectionId == badgeCollection).NewCards == 0, "Collection badge did not clear");
Check(!(await appCollections.LoadDetailAsync(user, badgeCollection, default))!.Cards.Single().IsNew, "Opened card is still marked new");
Console.WriteLine("PASS: global/collection/card badges, restart persistence, notification independence, account isolation and idempotent viewing.");

sealed class CheckSessions : ISessionStore
{
    public AccountSession Create(AccountUser account) => new(Guid.NewGuid().ToString(), DateTimeOffset.UtcNow.AddDays(1), account.ToPublic());
    public PublicAccount? Get(string? token) => null;
    public void Revoke(string? token) { }
    public void RevokeAll(long userId) { }
}
