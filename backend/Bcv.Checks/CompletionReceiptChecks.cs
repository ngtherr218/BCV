using Bcv.Api.Modules.Journey.Infrastructure;
using Npgsql;

internal static class CompletionReceiptChecks
{
    public static async Task Run()
    {
        await using var db=NpgsqlDataSource.Create(Environment.GetEnvironmentVariable("BCV_CHECK_CONNECTION")??throw new Exception("Set BCV_CHECK_CONNECTION."));
        await using var command=db.CreateCommand("""
            SELECT j.user_id,j.journey_id FROM journey j WHERE status='COMPLETED' AND required_steps>0 AND recorded_steps>=required_steps
            ORDER BY EXISTS(SELECT 1 FROM journey_quiz_answer a WHERE a.journey_id=j.journey_id) DESC,j.journey_id DESC LIMIT 5
            """);
        var trips=new List<(long User,long Id)>();
        await using(var reader=await command.ExecuteReaderAsync())
            while(await reader.ReadAsync())trips.Add((reader.GetInt64(0),reader.GetInt64(1)));
        if(trips.Count==0)throw new Exception("No completed journeys available for receipt checks.");
        foreach(var trip in trips)
        {
            var receipt=await JourneyCompletion.LoadAsync(db,trip.User,trip.Id,default);
            var total=receipt.GetProperty("rankingPoints").GetInt64()+receipt.GetProperty("quizPoints").GetInt64()+receipt.GetProperty("masteryPoints").GetInt64();
            if(total!=receipt.GetProperty("totalPoints").GetInt64())throw new Exception("Receipt total is inconsistent.");
            if(receipt.GetProperty("quizCorrect").GetInt32()>receipt.GetProperty("quizAnswered").GetInt32())throw new Exception("Incorrect quiz count.");
            foreach(var key in new[]{"rankBefore","rankAfter"})
                if(receipt.GetProperty(key).ValueKind!=System.Text.Json.JsonValueKind.Null && receipt.GetProperty(key).GetInt64()<1)throw new Exception("Invalid rank.");
            var reopened=await JourneyCompletion.LoadAsync(db,trip.User,trip.Id,default);
            if(receipt.ToString()!=reopened.ToString())throw new Exception("Reopening changed the receipt.");
        }
        Console.WriteLine($"PASS: {trips.Count} completed-journey receipts return consistent reward totals, quiz counts and historical ranks on repeated read-only loads.");
    }
}
