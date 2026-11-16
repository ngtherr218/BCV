using Bcv.Api.Modules.Journey.Application;
using Bcv.Api.Modules.Journey.Infrastructure;
using Bcv.Api.Modules.StatsNotify.Infrastructure;
using Npgsql;

internal static class RankingIntegrationChecks
{
    public static async Task Run()
    {
        var raw = Environment.GetEnvironmentVariable("BCV_CHECK_CONNECTION") ?? throw new Exception("Set BCV_CHECK_CONNECTION.");
        var settings = new NpgsqlConnectionStringBuilder(raw);
        if (settings.Host is not ("localhost" or "127.0.0.1" or "::1") ||
            settings.Database is null || !settings.Database.StartsWith("bcv_deploy_checks_", StringComparison.Ordinal))
            throw new Exception("Ranking integration checks require a dedicated local bcv_deploy_checks_ database.");
        await using var db = NpgsqlDataSource.Create(raw);
        async Task Exec(string sql) { await using var command = db.CreateCommand(sql); await command.ExecuteNonQueryAsync(); }
        async Task<long> Scalar(string sql) { await using var command = db.CreateCommand(sql); return Convert.ToInt64(await command.ExecuteScalarAsync()); }
        void Check(bool value, string message) { if (!value) throw new Exception(message); }
        // Previous fixtures in this dedicated database must not affect dense-rank expectations on rerun.
        await Exec("UPDATE app_user SET status='INACTIVE'");
        var tag = Guid.NewGuid().ToString("N");
        async Task<long> User(string name, string suffix)
        {
            await using var command = db.CreateCommand("INSERT INTO app_user(full_name,email,password_hash,role,status) VALUES($1,$2,'integration-not-a-login','USER','ACTIVE') RETURNING user_id");
            command.Parameters.AddWithValue(name); command.Parameters.AddWithValue(tag + suffix + "@example.invalid");
            return (long)(await command.ExecuteScalarAsync())!;
        }
        var me = await User("Người thử nghiệm", "me");
        var peer = await User("Bạn đồng hành", "peer");
        var peer2 = await User("Người đi bộ", "peer2");
        // This database is disposable and created only for deployment checks.
        await Exec("UPDATE quest SET status='INACTIVE'; UPDATE explorer_reward_config SET discovery_points=150;");
        await Exec($"""
            INSERT INTO explorer_score_event(user_id,event_type,source_key,points,rule_version,occurred_at) VALUES
              ({me},'QUEST','{tag}me',50,'integration',CURRENT_TIMESTAMP),
              ({peer},'QUEST','{tag}peer',100,'integration',CURRENT_TIMESTAMP),
              ({peer2},'QUEST','{tag}peer2',80,'integration',CURRENT_TIMESTAMP);
            INSERT INTO quest(name,quest_type,condition_type,target_value,cycle_type,status,reward_points)
              VALUES('Ranking check step','DAILY','STEP_COUNT',1,'DAILY','ACTIVE',20),
                    ('Ranking check journey','DAILY','COMPLETE_JOURNEY',1,'DAILY','ACTIVE',30);
            """);
        var start = await Scalar("SELECT min(place_id) FROM place WHERE status='ACTIVE'");
        var destination = await Scalar($"SELECT min(place_id) FROM place WHERE status='ACTIVE' AND place_id<>{start} AND (latitude,longitude)<>(SELECT latitude,longitude FROM place WHERE place_id={start})");
        await Exec($"INSERT INTO virtual_location(user_id,region_id,nearest_place_id,latitude,longitude) SELECT {me},region_id,place_id,latitude,longitude FROM place WHERE place_id={start}");
        var journeys = new PostgresJourneyRepository(db);
        var leaderboard = new LeaderboardRepository(db);
        var before = await leaderboard.LoadAsync(me, new(), default);
        Check(before.Me.Rank == 3 && before.Me.DisplayName == "Người thử nghiệm", "Leaderboard must use profile name and correct initial rank.");
        await journeys.CreateJourneyAsync(me, new(destination, false), default);
        var id = await Scalar($"SELECT journey_id FROM journey WHERE user_id={me} AND status='IN_PROGRESS'");
        var required = await Scalar($"SELECT required_steps FROM journey WHERE journey_id={id}");
        await journeys.SyncStepsAsync(me, new(id, required), default);
        var after = await leaderboard.LoadAsync(me, new(), default);
        Check(after.Me.Rank == 1 && after.Me.Points == 250, "Final rank must include 150 discovery + 20 step quest + 30 completion quest.");
        await using (var notice = db.CreateCommand($"SELECT content FROM notification WHERE user_id={me} AND notification_type='JOURNEY_COMPLETED'"))
        {
            var content = (string?)(await notice.ExecuteScalarAsync()) ?? "";
            Check(content.Contains("#3 → #1") && content.Contains("tăng 2 bậc"), "Completion notice must record before/after rank.");
        }
        await journeys.SyncStepsAsync(me, new(id, required), default);
        Check(await Scalar($"SELECT count(*) FROM notification WHERE user_id={me} AND notification_type='JOURNEY_COMPLETED'")==1,
            "Repeated sync must not duplicate the completion notice.");
        Check((await leaderboard.LoadAsync(me, new(), default)).Me.Points==250, "Repeated sync must not award again.");
        Check(after.Top.All(x => !x.DisplayName.Contains(x.UserId.ToString())), "Leaderboard names must not append internal IDs.");
        Console.WriteLine("PASS ranking integration: profile names, rank 3 to 1, discovery and both quest rewards, idempotent sync.");
    }
}
