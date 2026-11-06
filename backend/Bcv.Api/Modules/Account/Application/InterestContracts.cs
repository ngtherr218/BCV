namespace Bcv.Api.Modules.Account.Application;

public sealed record PlaceTypeOption(long Id, string Name);
public sealed record InterestSnapshot(IReadOnlyList<PlaceTypeOption> PlaceTypes, IReadOnlyList<long> SelectedPlaceTypeIds);
public sealed class InterestException(string code, string message) : Exception(message)
{
    public string Code { get; } = code;
}

public interface IInterestRepository
{
    Task<InterestSnapshot> LoadAsync(long userId, CancellationToken ct);
    Task<IReadOnlyList<long>> SaveAsync(long userId, IReadOnlyList<long> placeTypeIds, CancellationToken ct);
}

public sealed class InterestService(IInterestRepository repository)
{
    private const int MaxPlaceTypes = 3;

    public Task<InterestSnapshot> LoadAsync(long userId, CancellationToken ct) => repository.LoadAsync(userId, ct);

    public Task<IReadOnlyList<long>> SaveAsync(long userId, long[]? ids, CancellationToken ct)
    {
        if (ids is null || ids.Length == 0)
            throw new InterestException("INTEREST_REQUIRED", "Hãy chọn ít nhất một nhóm địa danh.");
        if (ids.Length > MaxPlaceTypes)
            throw new InterestException("INTEREST_LIMIT_EXCEEDED", "Chỉ được chọn tối đa 3 nhóm sở thích.");
        if (ids.Any(id => id <= 0) || ids.Distinct().Count() != ids.Length)
            throw new InterestException("INVALID_INTERESTS", "Danh sách nhóm địa danh không hợp lệ.");
        return repository.SaveAsync(userId, ids, ct);
    }
}
