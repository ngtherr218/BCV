using System.Text.Json;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Discovery.Infrastructure;
using Bcv.Api.Modules.PlaceCollection.Infrastructure;
using Npgsql;

static class CollectionAvailabilityChecks
{
    public static async Task Run(string connectionString)
    {
        var settings = new NpgsqlConnectionStringBuilder(connectionString);
        if (settings.Host is not ("localhost" or "127.0.0.1" or "::1"))
            throw new Exception("Collection checks require local PostgreSQL.");
        var schema = "bcv_collection_check_" + Guid.NewGuid().ToString("N");
        await using var source = NpgsqlDataSource.Create(connectionString);
        async Task SourceSql(string sql) { await using var cmd = source.CreateCommand(sql); await cmd.ExecuteNonQueryAsync(); }
        await SourceSql($"CREATE SCHEMA {schema}");
        try
        {
            // Every queried application table is local to this disposable schema. Public only supplies PostGIS.
            settings.SearchPath = schema + ",public";
            await using var isolated = NpgsqlDataSource.Create(settings.ConnectionString);
            async Task Exec(string sql) { await using var cmd = isolated.CreateCommand(sql); await cmd.ExecuteNonQueryAsync(); }
            void Check(bool value, string message) { if (!value) throw new Exception(message); }
            await Exec("""
                CREATE TABLE region(region_id bigint PRIMARY KEY,name text NOT NULL,status text NOT NULL);
                CREATE TABLE place_type(place_type_id bigint PRIMARY KEY,name text NOT NULL);
                CREATE TABLE place(place_id bigint PRIMARY KEY,region_id bigint,place_type_id bigint,name text NOT NULL,
                    latitude numeric,longitude numeric,status text,introduction text,highlights text,rating numeric,rating_count integer);
                CREATE TABLE virtual_location(user_id bigint PRIMARY KEY,latitude numeric,longitude numeric);
                CREATE TABLE card(card_id bigint PRIMARY KEY,place_id bigint NOT NULL,name text NOT NULL,image_key text,status text);
                CREATE TABLE collection(collection_id bigint PRIMARY KEY,name text NOT NULL,description text,status text);
                CREATE TABLE collection_card(collection_card_id bigint PRIMARY KEY,collection_id bigint,card_id bigint);
                CREATE TABLE user_card(user_id bigint,card_id bigint,seen_at timestamptz);
                CREATE TABLE user_discovered_place(user_id bigint,place_id bigint);
                CREATE TABLE place_image(place_image_id bigint,place_id bigint,image_key text);
                CREATE TABLE place_video(video_id bigint,place_id bigint,duration_seconds double precision,width integer,height integer);

                INSERT INTO region VALUES(1,'Active region','ACTIVE'),(2,'Retired region','INACTIVE');
                INSERT INTO place_type VALUES(1,'Heritage');
                INSERT INTO place(place_id,region_id,place_type_id,name,latitude,longitude,status)
                    VALUES(1,1,1,'Available',21,105,'ACTIVE'),(2,1,1,'Retired unowned',21,105,'INACTIVE'),
                    (3,2,1,'Inactive region',21,105,'ACTIVE'),(4,1,1,'Owned active',21,105,'ACTIVE'),
                    (5,1,1,'Inactive card',21,105,'ACTIVE'),(6,1,1,'Owned retired',21,105,'INACTIVE'),
                    (7,1,1,'Legacy discovered retired',21,105,'INACTIVE'),(8,999,1,'Missing region',21,105,'ACTIVE'),
                    (9,1,1,'Status transition',21,105,'ACTIVE');
                INSERT INTO virtual_location VALUES(42,21,105);
                INSERT INTO card VALUES
                    (1,1,'Available','https://example.invalid/1.jpg','ACTIVE'),
                    (2,2,'Retired unowned','https://example.invalid/2.jpg','ACTIVE'),
                    (3,3,'Inactive region','https://example.invalid/3.jpg','ACTIVE'),
                    (4,4,'Owned active','https://example.invalid/4.jpg','ACTIVE'),
                    (5,5,'Owned inactive card','https://example.invalid/5.jpg','INACTIVE'),
                    (6,6,'Owned retired place','https://example.invalid/6.jpg','ACTIVE'),
                    (7,7,'Legacy discovered retired','https://example.invalid/7.jpg','ACTIVE'),
                    (8,99,'Legacy missing unowned','https://example.invalid/8.jpg','ACTIVE'),
                    (9,100,'Legacy missing owned','https://example.invalid/9.jpg','ACTIVE'),
                    (10,8,'Missing region','https://example.invalid/10.jpg','ACTIVE'),
                    (11,9,'Status transition','https://example.invalid/11.jpg','ACTIVE');
                INSERT INTO user_card VALUES(42,4,NULL),(42,5,NULL),(42,6,CURRENT_TIMESTAMP),(42,9,NULL);
                INSERT INTO user_discovered_place VALUES(42,7);
                INSERT INTO collection VALUES(1,'Mixed collection',NULL,'ACTIVE'),(2,'Unavailable only',NULL,'ACTIVE');
                INSERT INTO collection_card SELECT card_id,1,card_id FROM card;
                INSERT INTO collection_card VALUES(20,2,2),(21,2,3);
                """);

            var cards = new CollectionRepository(isolated);
            var overview = await cards.LoadAsync(42, default);
            var detail = (await cards.LoadDetailAsync(42, 1, default))!;
            Check(overview.OwnedCards == 4 && overview.TotalCards == 6 && overview.NewCards == 2,
                "Overview must retain owned history and exclude unowned unavailable destinations.");
            Check(overview.Collections.Count == 1 && overview.Collections[0].OwnedCards == 4 &&
                overview.Collections[0].TotalCards == 6 && overview.Collections[0].NewCards == 2,
                "Collection summaries must use the same eligibility/count rules as the overview.");
            Check(overview.Collections[0].PreviewImageUrls.Count == 4,
                "Owned historical card artwork must remain in collection previews.");
            Check(detail.OwnedCards == 4 && detail.TotalCards == 6 &&
                detail.Cards.Select(c => c.CardId).Order().SequenceEqual(new long[] { 1, 4, 6, 7, 9, 11 }),
                "Detail must not retain unattainable unowned cards or discard earned history.");
            Check(detail.Cards.Where(c => !c.PlaceAvailable).Select(c => c.CardId).Order()
                .SequenceEqual(new long[] { 6, 7, 9 }), "Retired/missing owned destinations must disable place navigation.");
            Check(!detail.Cards.Any(c => c.CardId == 5), "An inactive card must stay hidden under the existing card policy.");
            await using (var storedOwnership = isolated.CreateCommand("SELECT EXISTS(SELECT 1 FROM user_card WHERE user_id=42 AND card_id=5)"))
                Check((bool)(await storedOwnership.ExecuteScalarAsync())!, "Hiding an inactive card must not delete its ownership.");
            Check(detail.Cards.Where(c => c.Owned).All(c => c.ImageUrl != null) &&
                detail.Cards.Where(c => !c.Owned).All(c => c.ImageUrl == null), "Card artwork ownership gating changed.");
            var retiredJson = JsonSerializer.SerializeToElement(detail.Cards.Single(c => c.CardId == 6),
                new JsonSerializerOptions(JsonSerializerDefaults.Web));
            Check(!retiredJson.GetProperty("placeAvailable").GetBoolean(), "Availability flag is missing from the API contract.");
            var other = await cards.LoadAsync(43, default);
            Check(other.OwnedCards == 0 && other.TotalCards == 3 && other.NewCards == 0,
                "Another user's ownership must not retain retired cards or show new-card badges.");
            Console.WriteLine("PASS collection eligibility/counts/preview/navigation agree; active-card history retained and inactive-card policy preserved.");

            await Exec("UPDATE place SET status='INACTIVE' WHERE place_id=9");
            overview = await cards.LoadAsync(42, default);
            detail = (await cards.LoadDetailAsync(42, 1, default))!;
            Check(overview.TotalCards == 5 && overview.Collections[0].TotalCards == 5 && detail.TotalCards == 5,
                "Retiring a destination must update all collection totals consistently.");
            await Exec("UPDATE region SET status='INACTIVE' WHERE region_id=1");
            overview = await cards.LoadAsync(42, default);
            detail = (await cards.LoadDetailAsync(42, 1, default))!;
            Check(overview.OwnedCards == 4 && overview.TotalCards == 4 && detail.Cards.All(c => c.Owned && !c.PlaceAvailable),
                "Retiring a region must retain only earned history with navigation disabled.");
            await Exec("UPDATE region SET status='ACTIVE' WHERE region_id=1; UPDATE place SET status='ACTIVE' WHERE place_id=9");
            await cards.MarkSeenAsync(42, 4, default);
            overview = await cards.LoadAsync(42, default);
            Check(overview.TotalCards == 6 && overview.OwnedCards == 4 && overview.NewCards == 1,
                "Restoring availability or marking seen must not delete ownership.");
            Console.WriteLine("PASS place/region retirement and reactivation keep summary/detail totals and ownership consistent.");

            var explore = new PostgresExploreRepository(isolated);
            async Task ExpectError(long user, long place, string code)
            {
                try { await explore.LoadPlaceAsync(user, place, default); throw new Exception("Unexpected place access accepted."); }
                catch (ExploreException error) { Check(error.Code == code, "Wrong place failure: " + error.Code); }
            }
            await ExpectError(43, 1, "NO_STARTING_POINT");
            await ExpectError(43, 2, "PLACE_NOT_FOUND");
            await ExpectError(42, 2, "PLACE_NOT_FOUND");
            await ExpectError(42, 3, "PLACE_NOT_FOUND");
            await ExpectError(42, 8, "PLACE_NOT_FOUND");
            await ExpectError(42, 100, "PLACE_NOT_FOUND");
            Check((await explore.LoadPlaceAsync(42, 1, default)).PlaceId == 1, "Available place detail should remain accessible.");
            Console.WriteLine("PASS inactive/deleted destinations are distinct from missing virtual starting point.");
        }
        finally
        {
            await SourceSql($"DROP SCHEMA {schema} CASCADE");
            Console.WriteLine("CLEANUP: disposable collection fixtures removed; public application rows untouched.");
        }
    }
}
