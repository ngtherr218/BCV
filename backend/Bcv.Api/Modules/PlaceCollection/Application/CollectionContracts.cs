namespace Bcv.Api.Modules.PlaceCollection.Application;

public sealed record CollectionSummary(long CollectionId, string Name, string? Description,
    int OwnedCards, int TotalCards, IReadOnlyList<string> PreviewImageUrls, int NewCards = 0);

public sealed record CollectionOverview(int OwnedCards, int TotalCards, IReadOnlyList<CollectionSummary> Collections, int NewCards = 0);

/// <summary>A card in a collection. Owned active cards remain in history when their place is retired.</summary>
public sealed record CollectionCard(long CardId, long PlaceId, string Name, string? ImageUrl, bool Owned,
    bool IsNew = false, bool PlaceAvailable = true);

public sealed record CollectionDetail(long CollectionId, string Name, string? Description,
    int OwnedCards, int TotalCards, IReadOnlyList<CollectionCard> Cards);
