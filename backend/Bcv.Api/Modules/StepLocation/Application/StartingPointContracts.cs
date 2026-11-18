namespace Bcv.Api.Modules.StepLocation.Application;

public sealed record StartingPointCandidate(
    long PlaceId, long RegionId, string RegionName, string PlaceName,
    decimal Latitude, decimal Longitude);

public sealed record StartingPointSnapshot(
    long VirtualLocationId, long? NearestPlaceId, string RegionName,
    string? PlaceName, decimal Latitude, decimal Longitude);

public sealed record StartingPointOptions(
    StartingPointSnapshot? Saved, StartingPointCandidate? Suggested);

public sealed record SaveStartingPointCommand(string? Source, decimal? Latitude, decimal? Longitude);

public sealed class StartingPointException(string code, string message) : Exception(message)
{
    public string Code { get; } = code;
}

public interface IStartingPointRepository
{
    Task<StartingPointSnapshot?> LoadSavedAsync(long userId, CancellationToken ct);
    Task<StartingPointCandidate?> LoadSuggestedAsync(CancellationToken ct);
    Task<StartingPointCandidate?> ResolveCurrentAsync(decimal latitude, decimal longitude, CancellationToken ct);
    Task<StartingPointSnapshot> SaveIfMissingAsync(long userId, StartingPointCandidate point,
        decimal latitude, decimal longitude, CancellationToken ct);
}

public sealed class StartingPointService(IStartingPointRepository repository)
{
    public async Task<StartingPointOptions> LoadAsync(long userId, CancellationToken ct) =>
        new(await repository.LoadSavedAsync(userId, ct), await repository.LoadSuggestedAsync(ct));

    public async Task<StartingPointCandidate> ResolveCurrentAsync(decimal? latitude, decimal? longitude,
        CancellationToken ct)
    {
        if (latitude is null or < -90 or > 90 || longitude is null or < -180 or > 180)
            throw new StartingPointException("INVALID_COORDINATES", "Tọa độ vị trí không hợp lệ.");
        return await repository.ResolveCurrentAsync(latitude.Value, longitude.Value, ct)
            ?? throw new StartingPointException("OUT_OF_SUPPORTED_AREA",
                "Vị trí này nằm ngoài Việt Nam nên chưa được hỗ trợ. Hãy chọn điểm hệ thống đề xuất.");
    }

    public async Task<StartingPointSnapshot> SaveAsync(long userId, SaveStartingPointCommand request,
        CancellationToken ct)
    {
        var existing = await repository.LoadSavedAsync(userId, ct);
        if (existing is not null) return existing;

        StartingPointCandidate point;
        decimal latitude;
        decimal longitude;
        switch (request.Source?.ToUpperInvariant())
        {
            case "SUGGESTED":
                point = await repository.LoadSuggestedAsync(ct)
                    ?? throw new StartingPointException("NO_SUGGESTION", "Chưa tải được điểm hệ thống đề xuất.");
                latitude = point.Latitude;
                longitude = point.Longitude;
                break;
            case "CURRENT":
                point = await ResolveCurrentAsync(request.Latitude, request.Longitude, ct);
                latitude = request.Latitude!.Value;
                longitude = request.Longitude!.Value;
                break;
            default:
                throw new StartingPointException("INVALID_SOURCE", "Hãy chọn cách khởi tạo điểm xuất phát.");
        }

        return await repository.SaveIfMissingAsync(userId, point, latitude, longitude, ct);
    }
}
