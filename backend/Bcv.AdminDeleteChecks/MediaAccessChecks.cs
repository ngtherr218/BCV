using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using Npgsql;

static class MediaAccessChecks
{
    public static async Task Run(NpgsqlDataSource db, HttpClient client)
    {
        void Check(bool ok, string message) { if (!ok) throw new Exception(message); }
        async Task Exec(string sql) { await using var cmd = db.CreateCommand(sql); await cmd.ExecuteNonQueryAsync(); }
        void Token(string? token) => client.DefaultRequestHeaders.Authorization = token is null ? null : new AuthenticationHeaderValue("Bearer", token);
        async Task Get(string path, HttpStatusCode expected)
        {
            using var response = await client.GetAsync(path);
            Check(response.StatusCode == expected, $"GET {path}: {response.StatusCode}, expected {expected}");
        }
        async Task Upload(byte[] bytes, string mime, HttpStatusCode expected)
        {
            using var content = new ByteArrayContent(bytes); content.Headers.ContentType = new MediaTypeHeaderValue(mime);
            using var response = await client.PostAsync("/api/places/951100/media", content);
            Check(response.StatusCode == expected, $"Upload {mime}: {response.StatusCode}, expected {expected}: {await response.Content.ReadAsStringAsync()}");
        }
        async Task Report(long id, HttpStatusCode expected)
        {
            using var response = await client.PostAsJsonAsync($"/api/community-media/{id}/reports", new { reason = "Nội dung cần kiểm tra" });
            Check(response.StatusCode == expected, $"Report {id}: {response.StatusCode}, expected {expected}");
        }
        async Task<JsonElement[]> Items(bool approvedOnly = false) => (await client.GetFromJsonAsync<JsonElement>("/api/places/951100/media?approvedOnly=" + approvedOnly.ToString().ToLowerInvariant()))
            .GetProperty("items").EnumerateArray().ToArray();
        var png = new byte[] { 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0 };
        await Exec("""
            INSERT INTO region(region_id,name,status) OVERRIDING SYSTEM VALUE VALUES(951100,'Media access fixture','ACTIVE');
            INSERT INTO place(place_id,region_id,place_type_id,name,latitude,longitude,status) OVERRIDING SYSTEM VALUE
              VALUES(951100,951100,910001,'Media access fixture',21,105,'ACTIVE');
            INSERT INTO user_discovered_place(user_id,place_id) VALUES(950001,951100),(950002,951100);
            INSERT INTO community_media(media_id,user_id,place_id,mime_type,media_data,status) OVERRIDING SYSTEM VALUE VALUES
              (951101,950001,951100,'image/png',decode('89504e470d0a1a0a00000000','hex'),'PENDING'),
              (951102,950001,951100,'image/png',decode('89504e470d0a1a0a00000000','hex'),'APPROVED'),
              (951103,950001,951100,'image/png',NULL,'REJECTED'),
              (951104,950001,951100,'video/webm',decode('1a45dfa30000000000000000','hex'),'APPROVED');
            """);
        try
        {
            Token(null);
            await Get("/api/places/951100/media", HttpStatusCode.Unauthorized);
            await Get("/api/places/951100/media?approvedOnly=true", HttpStatusCode.Unauthorized);
            await Get("/api/community-media/951101/content", HttpStatusCode.Unauthorized);
            await Get("/api/community-media/951102/content", HttpStatusCode.Unauthorized);
            await Upload(png, "image/png", HttpStatusCode.Unauthorized);
            await Report(951102, HttpStatusCode.Unauthorized);

            Token("950003"); // Signed in, but has not discovered this place.
            await Get("/api/places/951100/media", HttpStatusCode.NotFound);
            await Get("/api/places/951100/media?approvedOnly=true", HttpStatusCode.NotFound);
            await Get("/api/community-media/951102/content", HttpStatusCode.NotFound);
            await Upload(png, "image/png", HttpStatusCode.NotFound);
            await Report(951102, HttpStatusCode.NotFound);

            Token("950001");
            var owned = await Items();
            Check(owned.Length == 4 && owned.All(x => x.GetProperty("isOwner").GetBoolean() && !x.GetProperty("canReport").GetBoolean()), "Owner list/flags incorrect");
            Check(!owned.Single(x => x.GetProperty("mediaId").GetInt64() == 951103).GetProperty("contentAvailable").GetBoolean(), "Deleted file marked available");
            var approvedOwned = await Items(true);
            Check(approvedOwned.Length == 2 && approvedOwned.All(x => x.GetProperty("status").GetString() == "APPROVED"
                && x.GetProperty("isOwner").GetBoolean() && !x.GetProperty("canReport").GetBoolean()
                && x.GetProperty("contentAvailable").GetBoolean()), "Approved-only owner list leaked pending/rejected media or lost permission flags");
            await Get("/api/community-media/951101/content", HttpStatusCode.OK);
            await Get("/api/community-media/951103/content", HttpStatusCode.NotFound);

            Token("950002");
            var visible = await Items();
            Check(visible.Length == 2 && visible.All(x => x.GetProperty("status").GetString() == "APPROVED" && !x.GetProperty("isOwner").GetBoolean() && x.GetProperty("canReport").GetBoolean()), "Another user can see unpublished media or has wrong flags");
            Check((await Items(true)).Select(x => x.GetProperty("mediaId").GetInt64()).SequenceEqual(visible.Select(x => x.GetProperty("mediaId").GetInt64())), "Approved-only filter changed another user's authorized content");
            await Get("/api/community-media/951101/content", HttpStatusCode.NotFound);
            await Report(951101, HttpStatusCode.NotFound);
            await Get("/api/community-media/951104/content", HttpStatusCode.OK); // Legacy WebM remains readable.
            await Report(951102, HttpStatusCode.OK);
            Check(!(await Items()).Single(x => x.GetProperty("mediaId").GetInt64() == 951102).GetProperty("canReport").GetBoolean(), "Already-reported item still offers report action");

            foreach (var table in new[] { "place", "region" })
            {
                await Exec($"UPDATE {table} SET status='INACTIVE' WHERE {table}_id=951100");
                Token("950001");
                await Get("/api/places/951100/media", HttpStatusCode.NotFound);
                await Get("/api/places/951100/media?approvedOnly=true", HttpStatusCode.NotFound);
                await Get("/api/community-media/951101/content", HttpStatusCode.NotFound);
                await Upload(png, "image/png", HttpStatusCode.NotFound);
                Token("950002");
                await Get("/api/community-media/951102/content", HttpStatusCode.NotFound);
                await Report(951104, HttpStatusCode.NotFound);
                Token("950005");
                await Get("/api/community-media/951101/content", HttpStatusCode.OK); // Moderation remains possible.
                await Exec($"UPDATE {table} SET status='ACTIVE' WHERE {table}_id=951100");
            }
            await Exec("UPDATE app_user SET status='BLOCKED' WHERE user_id=950001");
            Token("950001");
            await Get("/api/places/951100/media", HttpStatusCode.NotFound);
            await Get("/api/places/951100/media?approvedOnly=true", HttpStatusCode.NotFound);
            await Get("/api/community-media/951101/content", HttpStatusCode.NotFound);
            await Upload(png, "image/png", HttpStatusCode.Forbidden);
            await Exec("UPDATE app_user SET status='ACTIVE' WHERE user_id=950001");

            await Upload(PlaceVideoChecks.Fixture(), "video/mp4", HttpStatusCode.OK);
            await Upload(PlaceVideoChecks.Fixture(60, 1920, 1080), "video/mp4", HttpStatusCode.OK);
            await Upload(PlaceVideoChecks.Fixture(61), "video/mp4", HttpStatusCode.BadRequest);
            await Upload(PlaceVideoChecks.Fixture(10, 3840, 2160), "video/mp4", HttpStatusCode.BadRequest);
            await Upload(System.Text.Encoding.ASCII.GetBytes("0000ftypisom0"), "video/mp4", HttpStatusCode.BadRequest);
            await Upload(new byte[] { 26, 69, 223, 163, 0, 0, 0, 0, 0, 0, 0, 0 }, "video/webm", HttpStatusCode.BadRequest);
            await Upload(new byte[10 * 1024 * 1024 + 1], "image/png", HttpStatusCode.BadRequest);
            using (var oversized = new ByteArrayContent(new byte[Bcv.Api.Modules.Admin.PlaceVideos.MaxBytes + 1]))
            {
                oversized.Headers.ContentType = new MediaTypeHeaderValue("video/mp4");
                using var response = await client.PostAsync("/api/places/951100/media", oversized);
                Check(response.StatusCode is HttpStatusCode.BadRequest or HttpStatusCode.RequestEntityTooLarge, "Community video size limit not enforced");
            }
            Console.WriteLine("PASS community media anonymous/discovery/active-place/active-region/owner/admin permissions, report eligibility, list flags, legacy playback and new upload limits");
        }
        finally
        {
            Token("950005");
            await Exec("""
                DELETE FROM media_report WHERE media_id IN (SELECT media_id FROM community_media WHERE place_id=951100);
                DELETE FROM community_media WHERE place_id=951100;
                DELETE FROM user_discovered_place WHERE place_id=951100;
                DELETE FROM place WHERE place_id=951100;
                DELETE FROM region WHERE region_id=951100;
                UPDATE app_user SET status='ACTIVE' WHERE user_id=950001;
                """);
        }
    }
}
