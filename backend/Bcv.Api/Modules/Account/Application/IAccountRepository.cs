using Bcv.Api.Modules.Account.Domain;

namespace Bcv.Api.Modules.Account.Application;

public sealed record OnboardingStatus(bool HasInterests, bool HasActivityLevel, bool HasVirtualLocation);

public interface IAccountRepository
{
    Task<AccountUser> CreateAsync(string fullName, string email, string passwordHash, CancellationToken cancellationToken);
    Task<AccountUser?> FindByEmailAsync(string email, CancellationToken cancellationToken);
    Task<AccountUser?> FindByIdAsync(long userId, CancellationToken cancellationToken);
    Task UpdatePasswordAsync(long userId, string passwordHash, CancellationToken cancellationToken);
    Task RecordLoginAsync(long userId, CancellationToken cancellationToken);
    Task<OnboardingStatus> GetOnboardingStatusAsync(long userId, CancellationToken cancellationToken);
    Task<bool> HasSelectedPlaceTypesAsync(long userId, CancellationToken cancellationToken);
    Task<bool> HasActivityLevelAsync(long userId, CancellationToken cancellationToken);
    Task<bool> HasVirtualLocationAsync(long userId, CancellationToken cancellationToken);
}
