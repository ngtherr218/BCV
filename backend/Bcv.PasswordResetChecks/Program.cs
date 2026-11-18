using System.Net;
using System.Text.Json;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Bcv.Api.Modules.Account.Infrastructure;
using Microsoft.Extensions.Logging.Abstractions;

void Check(bool condition, string message) { if (!condition) throw new Exception(message); }
async Task Expect(string code, Func<Task> action)
{
    try { await action(); }
    catch (AccountException ex) when (ex.Code == code) { return; }
    throw new Exception($"Expected {code}");
}
var handler = new MailHandler();
using var client = new HttpClient(handler);
var mailer = new BrevoPasswordResetMailer(client, new BrevoSettings("fake-key", "sender@example.com", "BCV"),
    NullLogger<BrevoPasswordResetMailer>.Instance);
var repo = new Accounts();
var store = new InMemoryPasswordResetStore();
var sessions = new Sessions();
var service = new PasswordResetService(repo, store, mailer, sessions);
var requested = await service.Request(" USER@example.com ", default);
Check(requested.ResendAfterSeconds == 60 && requested.ExpiresInSeconds == 600, "Wrong code lifetime");
Check(handler.Uri == "https://api.brevo.com/v3/smtp/email" && handler.Authorization == "fake-key", "Wrong HTTPS request");
using var payload = JsonDocument.Parse(handler.Body!);
Check(payload.RootElement.GetProperty("sender").GetProperty("email").GetString() == "sender@example.com", "Wrong sender");
Check(payload.RootElement.GetProperty("to")[0].GetProperty("email").GetString() == "user@example.com", "Wrong recipient");
var text = payload.RootElement.GetProperty("textContent").GetString()!;
var code = System.Text.RegularExpressions.Regex.Match(text, @"\b\d{6}\b").Value;
Check(code.Length == 6, "Missing six-digit OTP in email");
await Expect("RESEND_TOO_SOON", async () => { await service.Request("user@example.com", default); });
var wrongCode = code == "000000" ? "000001" : "000000";
await Expect("INVALID_CODE", async () => { await service.Verify("user@example.com", wrongCode, default); });
var verified = await service.Verify("user@example.com", code, default);
await Expect("CODE_EXPIRED", async () => { await service.Verify("user@example.com", code, default); });
await service.Complete(verified.ResetToken, "NewPassword123", "NewPassword123", default);
Check(repo.Hash is not null && PasswordHasher.Verify("NewPassword123", repo.Hash), "Password not updated");
Check(sessions.Revoked == 42, "Old sessions not revoked");
await Expect("RESET_EXPIRED", async () => { await service.Complete(verified.ResetToken, "NewPassword123", "NewPassword123", default); });
var sent = handler.Calls;
await service.Request("unknown@example.com", default);
Check(handler.Calls == sent, "Sent email for unknown account");
handler.Status = HttpStatusCode.Forbidden;
await Expect("EMAIL_DELIVERY_FAILED", () => mailer.SendCodeAsync("user@example.com", "123456", default));
handler.FailNetwork = true;
await Expect("EMAIL_DELIVERY_FAILED", () => mailer.SendCodeAsync("user@example.com", "123456", default));
await Expect("EMAIL_DELIVERY_FAILED", () => new UnavailablePasswordResetMailer().SendCodeAsync("user@example.com", "123456", default));
Console.WriteLine("PASS: HTTPS email payload/auth, OTP verification, password update, single-use code/token, resend delay, session revocation, unknown accounts and delivery failures.");

sealed class MailHandler : HttpMessageHandler
{
    public string? Uri, Authorization, Body;
    public int Calls;
    public HttpStatusCode Status = HttpStatusCode.OK;
    public bool FailNetwork;
    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
    {
        Calls++;
        if (FailNetwork) throw new HttpRequestException("Network unavailable");
        Uri = request.RequestUri!.ToString();
        Authorization = request.Headers.GetValues("api-key").Single();
        Body = await request.Content!.ReadAsStringAsync(ct);
        return new HttpResponseMessage(Status) { Content = new StringContent("{\"id\":\"test-email\"}") };
    }
}
sealed class Accounts : IAccountRepository
{
    public string? Hash;
    public Task<AccountUser?> FindByEmailAsync(string email, CancellationToken ct) => Task.FromResult<AccountUser?>(
        email == "user@example.com" ? new(42, "Test", email, "USER", "ACTIVE", "old-hash") : null);
    public Task UpdatePasswordAsync(long id, string hash, CancellationToken ct) { Hash = hash; return Task.CompletedTask; }
    public Task<AccountUser> CreateAsync(string name, string email, string hash, CancellationToken ct) => throw new NotSupportedException();
    public Task<AccountUser?> FindByIdAsync(long id, CancellationToken ct) => throw new NotSupportedException();
    public Task RecordLoginAsync(long id, CancellationToken ct) => throw new NotSupportedException();
    public Task<OnboardingStatus> GetOnboardingStatusAsync(long id, CancellationToken ct) => throw new NotSupportedException();
    public Task<bool> HasSelectedPlaceTypesAsync(long id, CancellationToken ct) => throw new NotSupportedException();
    public Task<bool> HasActivityLevelAsync(long id, CancellationToken ct) => throw new NotSupportedException();
    public Task<bool> HasVirtualLocationAsync(long id, CancellationToken ct) => throw new NotSupportedException();
}
sealed class Sessions : ISessionStore
{
    public long Revoked;
    public void RevokeAll(long id) => Revoked = id;
    public AccountSession Create(AccountUser user) => throw new NotSupportedException();
    public PublicAccount? Get(string? token) => throw new NotSupportedException();
    public void Revoke(string? token) => throw new NotSupportedException();
}
