using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;
using Bcv.Api.Modules.Shared.Api;

namespace Bcv.Api.Modules.Account.Api;

public static class AccountEndpoints
{
    public static void MapAccountEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/account");
        group.MapPost("/register", async (RegisterRequest request, AccountService service, ILoggerFactory logs, CancellationToken ct) =>
            await Execute(() => service.Register(new RegisterCommand(request.Email, request.Password, request.ConfirmPassword), ct), logs));
        group.MapPost("/login", async (LoginRequest request, AccountService service, ILoggerFactory logs, CancellationToken ct) =>
            await Execute(() => service.Login(new LoginCommand(request.Email, request.Password), ct), logs));
        group.MapGet("/me", async (HttpRequest request, ISessionStore sessions, AccountService service, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            var active = await service.GetActiveUser(user.UserId, ct);
            return active is null ? Results.Unauthorized() : Results.Ok(active);
        });
        group.MapPost("/password-reset/request", async (PasswordResetRequest request, PasswordResetService service,
                ILoggerFactory logs, CancellationToken ct) =>
            await Execute(() => service.Request(request.Email, ct), logs));
        group.MapPost("/password-reset/verify", async (PasswordResetVerifyRequest request, PasswordResetService service,
                ILoggerFactory logs, CancellationToken ct) =>
            await Execute(() => service.Verify(request.Email, request.Code, ct), logs));
        group.MapPost("/password-reset/complete", async (PasswordResetCompleteRequest request,
                PasswordResetService service, ILoggerFactory logs, CancellationToken ct) =>
            await Execute(() => service.Complete(request.ResetToken, request.Password, request.ConfirmPassword, ct), logs));
        group.MapPost("/change-password", async (ChangePasswordRequest body, HttpRequest request, ISessionStore sessions,
            AccountService service, ILoggerFactory logs, CancellationToken ct) =>
        {
            var user = CurrentUser.Get(request, sessions);
            if (user is null) return Results.Unauthorized();
            return await Execute(() => service.ChangePassword(user.UserId,
                new ChangePasswordCommand(body.CurrentPassword, body.NewPassword, body.ConfirmPassword), ct), logs);
        });
        group.MapPost("/logout", (HttpRequest request, ISessionStore sessions) =>
        {
            sessions.Revoke(CurrentUser.Token(request));
            return Results.NoContent();
        });
    }

    private static async Task<IResult> Execute<T>(Func<Task<T>> action, ILoggerFactory logs)
    {
        try { return Results.Ok(await action()); }
        catch (AccountException ex)
        {
            var status = ex.Code switch
            {
                "EMAIL_EXISTS" => 409,
                "INVALID_CREDENTIALS" or "ACCOUNT_INACTIVE" => 401,
                "RESEND_TOO_SOON" or "TOO_MANY_ATTEMPTS" => 429,
                "EMAIL_DELIVERY_FAILED" => 503,
                _ => 400
            };
            return Results.Json(new ApiError(ex.Code, ex.Message, ex.Field), statusCode: status);
        }
        catch (Exception ex)
        {
            logs.CreateLogger("Account").LogError(ex, "Account request failed");
            return Results.Json(new ApiError("SERVER_ERROR", "Máy chủ đang gặp lỗi. Vui lòng thử lại."), statusCode: 500);
        }
    }
}
