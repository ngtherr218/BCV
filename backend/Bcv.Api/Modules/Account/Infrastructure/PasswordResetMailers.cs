using System.Net;
using System.Net.Mail;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;

namespace Bcv.Api.Modules.Account.Infrastructure;

public sealed record SmtpSettings(string Host, int Port, string? User, string? Password, string From, bool EnableSsl);

public sealed record ResendSettings(string ApiKey, string From);

public sealed record BrevoSettings(string ApiKey, string FromEmail, string FromName);

public sealed class BrevoPasswordResetMailer(HttpClient client, BrevoSettings settings,
    ILogger<BrevoPasswordResetMailer> logger) : IPasswordResetMailer
{
    public async Task SendCodeAsync(string email, string code, CancellationToken cancellationToken)
    {
        using var request = new HttpRequestMessage(HttpMethod.Post, "https://api.brevo.com/v3/smtp/email");
        request.Headers.Add("api-key", settings.ApiKey);
        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        request.Content = JsonContent.Create(new {
            sender = new { email = settings.FromEmail, name = settings.FromName },
            to = new[] { new { email } },
            subject = "Mã xác minh Bước Chân Việt",
            textContent = $"Mã xác minh khôi phục mật khẩu của bạn là: {code}\n\n" +
                $"Mã có hiệu lực trong {PasswordResetService.CodeLifetimeSeconds / 60} phút. " +
                "Không chia sẻ mã này với người khác. Nếu bạn không yêu cầu, hãy bỏ qua email này."
        });
        try
        {
            using var response = await client.SendAsync(request, cancellationToken);
            if (response.IsSuccessStatusCode) return;
            logger.LogWarning("Password reset email rejected by Brevo (HTTP {Status})", (int)response.StatusCode);
        }
        catch (HttpRequestException)
        {
            logger.LogWarning("Could not connect to Brevo email provider");
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            logger.LogWarning("Brevo email provider timed out");
        }
        throw new AccountException("EMAIL_DELIVERY_FAILED", "Chưa thể gửi email xác minh. Vui lòng thử lại sau một phút.");
    }
}

/// <summary>HTTPS delivery works on Render Free, where SMTP ports are blocked.</summary>
public sealed class ResendPasswordResetMailer(HttpClient client, ResendSettings settings,
    ILogger<ResendPasswordResetMailer> logger) : IPasswordResetMailer
{
    public async Task SendCodeAsync(string email, string code, CancellationToken cancellationToken)
    {
        using var request = new HttpRequestMessage(HttpMethod.Post, "https://api.resend.com/emails");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", settings.ApiKey);
        request.Headers.Add("Idempotency-Key", Guid.NewGuid().ToString("N"));
        request.Content = JsonContent.Create(new {
            from = settings.From,
            to = new[] { email },
            subject = "Mã xác minh Bước Chân Việt",
            text = $"Mã xác minh khôi phục mật khẩu của bạn là: {code}\n\n" +
                $"Mã có hiệu lực trong {PasswordResetService.CodeLifetimeSeconds / 60} phút. " +
                "Không chia sẻ mã này với người khác. Nếu bạn không yêu cầu, hãy bỏ qua email này."
        });
        try
        {
            using var response = await client.SendAsync(request, cancellationToken);
            if (response.IsSuccessStatusCode) return;
            // Provider bodies may contain personal data. Never log the code, key or response body.
            logger.LogWarning("Password reset email rejected by Resend (HTTP {Status})", (int)response.StatusCode);
        }
        catch (HttpRequestException)
        {
            logger.LogWarning("Could not connect to password reset email provider");
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            logger.LogWarning("Password reset email provider timed out");
        }
        throw new AccountException("EMAIL_DELIVERY_FAILED", "Chưa thể gửi email xác minh. Vui lòng thử lại sau một phút.");
    }
}

/// <summary>A deployed server must not silently pretend it sent an email.</summary>
public sealed class UnavailablePasswordResetMailer : IPasswordResetMailer
{
    public Task SendCodeAsync(string email, string code, CancellationToken cancellationToken) =>
        throw new AccountException("EMAIL_DELIVERY_FAILED", "Dịch vụ gửi email chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
}

/// <summary>Sends the code by email. Configured through the Smtp__* settings.</summary>
public sealed class SmtpPasswordResetMailer(SmtpSettings settings) : IPasswordResetMailer
{
    public async Task SendCodeAsync(string email, string code, CancellationToken cancellationToken)
    {
        using var message = new MailMessage(settings.From, email)
        {
            Subject = "Mã xác minh Bước Chân Việt",
            Body = $"Mã xác minh khôi phục mật khẩu của bạn là: {code}\n\n" +
                $"Mã có hiệu lực trong {PasswordResetService.CodeLifetimeSeconds / 60} phút. " +
                "Nếu bạn không yêu cầu, hãy bỏ qua email này."
        };
        using var client = new SmtpClient(settings.Host, settings.Port) { EnableSsl = settings.EnableSsl };
        if (!string.IsNullOrEmpty(settings.User))
            client.Credentials = new NetworkCredential(settings.User, settings.Password);
        await client.SendMailAsync(message, cancellationToken);
    }
}

/// <summary>Development fallback when no SMTP server is configured: the code is written to the API log.</summary>
public sealed class LogPasswordResetMailer(ILogger<LogPasswordResetMailer> logger) : IPasswordResetMailer
{
    public Task SendCodeAsync(string email, string code, CancellationToken cancellationToken)
    {
        logger.LogWarning("Password reset code for {Email}: {Code} (SMTP not configured)", email, code);
        return Task.CompletedTask;
    }
}
