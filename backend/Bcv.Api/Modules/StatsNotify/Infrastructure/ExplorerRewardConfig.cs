using Npgsql;
using System.Text.Json.Serialization;
namespace Bcv.Api.Modules.StatsNotify.Infrastructure;

public sealed record ExplorerRewardConfig([property:JsonRequired] int DiscoveryPoints=150,[property:JsonRequired] long Revision=0)
{
    public string RuleVersion => $"v3:d{DiscoveryPoints}";
    public static async Task<ExplorerRewardConfig> LoadAsync(NpgsqlConnection connection,NpgsqlTransaction? transaction,CancellationToken ct)
    {
        await using var query=new NpgsqlCommand("SELECT discovery_points,revision FROM explorer_reward_config WHERE singleton"+(transaction is null?"":" FOR SHARE"),connection,transaction);
        await using var reader=await query.ExecuteReaderAsync(ct);
        return await reader.ReadAsync(ct)?new(reader.GetInt32(0),reader.GetInt64(1)):new();
    }
}
