using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Discovery.Infrastructure;
using Npgsql;

internal static class PlaceFilterChecks
{
    public static async Task Run()
    {
        foreach (var query in new[] {
            new PlaceSearchQuery(MaxRequiredSteps: 0),
            new PlaceSearchQuery(MaxRequiredSteps: -1),
            new PlaceSearchQuery(InterestGroupIds: "101,102,103,104") })
        {
            try { query.Validate(); throw new Exception("Invalid filter accepted."); }
            catch (ExploreException) { }
        }
        await using var db = NpgsqlDataSource.Create(
            Environment.GetEnvironmentVariable("BCV_CHECK_CONNECTION") ?? throw new Exception("Set BCV_CHECK_CONNECTION."));
        await using var findUser = db.CreateCommand("""
            SELECT v.user_id FROM virtual_location v JOIN user_preference u USING(user_id)
            ORDER BY v.user_id LIMIT 1
            """);
        var userId = (long)(await findUser.ExecuteScalarAsync() ?? throw new Exception("No configured test user."));
        var repository = new PlaceSearchRepository(db);
        var baseline = await repository.SearchAsync(userId, new(OnlyUndiscovered: true, PageSize: 50), default);
        if (baseline.Places.Count == 0) throw new Exception("No destinations available to verify filter boundaries.");
        foreach (var typeId in new long[] { 101, 108, 112 })
        {
            var exact = await repository.SearchAsync(userId, new(PlaceTypeId: typeId, OnlyUndiscovered: true, PageSize: 50), default);
            var selected = await repository.SearchAsync(userId, new(InterestGroupIds: typeId.ToString(), OnlyUndiscovered: true, PageSize: 50), default);
            if (selected.Total != exact.Total || !selected.Places.Select(place => place.PlaceId).SequenceEqual(exact.Places.Select(place => place.PlaceId)))
                throw new Exception("Selected destination types must match exact place types, not inferred interest features.");
        }
        // Include a real destination at its exact boundary and exclude it one step below.
        var boundary = baseline.Places[0].RequiredSteps;
        foreach (var distance in new[] { 0.5m, 12m })
        {
            foreach (int? steps in new int?[] { null, 2000 })
            {
                var query = new PlaceSearchQuery(MaxDistanceKm: distance, MaxRequiredSteps: steps, OnlyUndiscovered: true, PageSize: 50);
                var result = await repository.SearchAsync(userId, query, default);
                if (result.MaxDistanceKm != distance || result.MaxRequiredSteps != steps ||
                    result.Places.Any(place => place.DistanceKm > distance || (steps.HasValue && place.RequiredSteps > steps)))
                    throw new Exception("Distance and step limits must both apply.");
            }
        }
        foreach (var limit in new[] { 2000, boundary, Math.Max(1, boundary - 1) }.Distinct())
        {
            var query = new PlaceSearchQuery(MaxRequiredSteps: limit, OnlyUndiscovered: true, PageSize: 50);
            var result = await repository.SearchAsync(userId, query, default);
            if (result.MaxRequiredSteps != limit || result.Places.Any(place => place.RequiredSteps > limit))
                throw new Exception("Step limit was not applied.");
            if (limit == boundary && result.Places.All(place => place.PlaceId != baseline.Places[0].PlaceId))
                throw new Exception("Inclusive step boundary excluded the destination.");
            long count = result.Places.Count;
            for (var page = 2; count < result.Total; page++)
            {
                var next = await repository.SearchAsync(userId, query with { Page = page }, default);
                if (next.Places.Count == 0 || next.Places.Any(place => place.RequiredSteps > limit))
                    throw new Exception("Step-filtered pagination is inconsistent.");
                count += next.Places.Count;
            }
            if (count != result.Total) throw new Exception("Filtered total does not match pagination.");
        }
        Console.WriteLine("PASS: read-only destination search enforces exact types, step limits, inclusive boundaries, totals, pagination and the three-type limit.");
    }
}
