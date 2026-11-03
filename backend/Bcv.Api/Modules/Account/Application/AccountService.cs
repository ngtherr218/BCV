using System.Net.Mail;
using Bcv.Api.Modules.Account.Domain;

namespace Bcv.Api.Modules.Account.Application;

public sealed record RegisterCommand(string? Email, string? Password, string? ConfirmPassword);
public sealed record LoginCommand(string? Email, string? Password);
public sealed record SignedInUser(long UserId, string FullName, string Email, string Role,
    bool NeedsInterestSelection, bool NeedsActivityLevel, bool NeedsVirtualLocation);
public sealed record ChangePasswordCommand(string? CurrentPassword, string? NewPassword, string? ConfirmPassword);

public sealed class AccountService(IAccountRepository repository, ISessionStore sessions)
{
    public async Task<AccountSession> Register(RegisterCommand request, CancellationToken cancellationToken)
    {
        var email = NormalizeEmail(request.Email);
        var password = request.Password ?? "";
        ValidateNewPassword(password, request.ConfirmPassword);
        if (await repository.FindByEmailAsync(email, cancellationToken) is not null)
            throw new AccountException("EMAIL_EXISTS", "Email này đã được sử dụng. Vui lòng đăng nhập.", "email");

        // The registration design has no name field; the profile flow can replace this display name.
        var account = await repository.CreateAsync("Người khám phá", email, PasswordHasher.Hash(password), cancellationToken);
        return sessions.Create(account) with
        {
            NeedsInterestSelection = true, NeedsActivityLevel = true, NeedsVirtualLocation = true
        };
    }

    public async Task<AccountSession> Login(LoginCommand request, CancellationToken cancellationToken, bool requireAdmin = false)
    {
        var email = NormalizeEmail(request.Email);
        if (string.IsNullOrEmpty(request.Password))
            throw new AccountException("REQUIRED_PASSWORD", "Vui lòng nhập mật khẩu.", "password");
        var account = await repository.FindByEmailAsync(email, cancellationToken);
        if (account is null || !PasswordHasher.Verify(request.Password, account.PasswordHash))
            throw new AccountException("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng.");
        if (account.Status != "ACTIVE")
            throw new AccountException("ACCOUNT_INACTIVE", "Tài khoản hiện không hoạt động.");
        if (requireAdmin && account.Role != "ADMIN")
            throw new AccountException("FORBIDDEN", "Tài khoản không có quyền quản trị.");
        var onboarding = await repository.GetOnboardingStatusAsync(account.UserId, cancellationToken);
        var session = sessions.Create(account);
        try { await repository.RecordLoginAsync(account.UserId, cancellationToken); }
        catch { sessions.Revoke(session.Token); throw; }
        return session with
        {
            NeedsInterestSelection = !onboarding.HasInterests,
            NeedsActivityLevel = !onboarding.HasActivityLevel,
            NeedsVirtualLocation = !onboarding.HasVirtualLocation
        };
    }

    /// <summary>
    /// The signed-in user with the same onboarding flags as login, so the app can resume a saved
    /// session at the right screen; null when the account is no longer active.
    /// </summary>
    public async Task<SignedInUser?> GetActiveUser(long userId, CancellationToken cancellationToken)
    {
        var account = await repository.FindByIdAsync(userId, cancellationToken);
        if (account?.Status != "ACTIVE") return null;
        var onboarding = await repository.GetOnboardingStatusAsync(userId, cancellationToken);
        return new SignedInUser(account.UserId, account.FullName, account.Email, account.Role,
            !onboarding.HasInterests, !onboarding.HasActivityLevel, !onboarding.HasVirtualLocation);
    }

    /// <summary>Signed-in change: the current password stands in for the email code of a reset.</summary>
    public async Task<bool> ChangePassword(long userId, ChangePasswordCommand request, CancellationToken cancellationToken)
    {
        if (string.IsNullOrEmpty(request.CurrentPassword))
            throw new AccountException("REQUIRED_CURRENT_PASSWORD", "Vui lòng nhập mật khẩu hiện tại.", "currentPassword");
        var account = await repository.FindByIdAsync(userId, cancellationToken);
        if (account is null || account.Status != "ACTIVE")
            throw new AccountException("ACCOUNT_INACTIVE", "Tài khoản hiện không hoạt động.");
        if (!PasswordHasher.Verify(request.CurrentPassword, account.PasswordHash))
            throw new AccountException("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng.", "currentPassword");
        var password = request.NewPassword ?? "";
        ValidateNewPassword(password, request.ConfirmPassword);
        if (password == request.CurrentPassword)
            throw new AccountException("SAME_PASSWORD", "Mật khẩu mới cần khác mật khẩu hiện tại.", "password");
        await repository.UpdatePasswordAsync(userId, PasswordHasher.Hash(password), cancellationToken);
        return true;
    }

    internal static void ValidateNewPassword(string password, string? confirmPassword)
    {
        if (password.Length < 8 || password.Length > 128 || !password.Any(char.IsLetter) || !password.Any(char.IsDigit))
            throw new AccountException("INVALID_PASSWORD", "Mật khẩu cần 8–128 ký tự, có chữ và số.", "password");
        if (password != confirmPassword)
            throw new AccountException("PASSWORD_MISMATCH", "Mật khẩu xác nhận không khớp.", "confirmPassword");
    }

    internal static string NormalizeEmail(string? value)
    {
        var email = value?.Trim().ToLowerInvariant() ?? "";
        if (email.Length == 0) throw new AccountException("REQUIRED_EMAIL", "Vui lòng nhập email.", "email");
        if (email.Length > 255 || email.Contains(' ') || !MailAddress.TryCreate(email, out var parsed) || parsed.Address != email)
            throw new AccountException("INVALID_EMAIL", "Email không đúng định dạng.", "email");
        return email;
    }
}
