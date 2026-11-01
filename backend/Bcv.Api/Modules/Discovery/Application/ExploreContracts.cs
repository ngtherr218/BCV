namespace Bcv.Api.Modules.Discovery.Application;

public sealed record ExploreLocation(string RegionName, string? PlaceName, decimal Latitude, decimal Longitude);

public sealed record ExploreJourney(
    long JourneyId, string DestinationName, decimal DestinationLatitude,
    decimal DestinationLongitude, int RequiredSteps, int RecordedSteps,
    string Status, long StartedAtEpochSeconds, decimal StartLatitude, decimal StartLongitude,
    long DestinationPlaceId, ExploreCardReward? AwardedCard = null);

public sealed record ExploreCardReward(long CardId, string Name);

public sealed record ExploreMapPlace(long PlaceId, string Name, decimal Latitude, decimal Longitude, bool IsDiscovered);

/// <summary>
/// ImageUrl stays the first photo for older clients. ImageUrls (every PLACE_IMAGE row)
/// is only listed once the place is discovered, like the text content.
/// </summary>
public sealed record ExplorePlaceDetail(long PlaceId, string Name, string PlaceType, string? ImageUrl,
    bool IsDiscovered, decimal DistanceKm, int RequiredSteps, string? Introduction, string? Highlights,
    string RegionName, IReadOnlyList<string> ImageUrls);

public sealed record ExploreSnapshot(ExploreLocation? Location, ExploreJourney? Journey,
    IReadOnlyList<ExploreMapPlace> Places);

public sealed class ExploreException(string code, string message) : Exception(message)
{
    public string Code { get; } = code;
}

public interface IExploreRepository
{
    Task<ExploreSnapshot> LoadAsync(long userId, CancellationToken ct);
    Task<ExplorePlaceDetail> LoadPlaceAsync(long userId, long placeId, CancellationToken ct);
}

public sealed class ExploreService(IExploreRepository repository)
{
    public Task<ExploreSnapshot> LoadAsync(long userId, CancellationToken ct) =>
        repository.LoadAsync(userId, ct);

    public Task<ExplorePlaceDetail> LoadPlaceAsync(long userId, long placeId, CancellationToken ct)
    {
        if (placeId <= 0) throw new ExploreException("INVALID_PLACE", "Địa danh không hợp lệ.");
        return repository.LoadPlaceAsync(userId, placeId, ct);
    }
}
