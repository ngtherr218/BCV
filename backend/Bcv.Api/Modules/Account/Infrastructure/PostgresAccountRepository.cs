using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.Account.Application;
using Bcv.Api.Modules.Account.Domain;

namespace Bcv.Api.Modules.Account.Infrastructure;

public sealed class PostgresAccountRepository(NpgsqlDataSource db) : IAccountRepository
{
    public async Task<AccountUser> CreateAsync(string fullName, string email, string passwordHash, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("""
            INSERT INTO public.app_user (full_name, email, password_hash, role, status)
            VALUES ($1, $2, $3, 'USER', 'ACTIVE')
            RETURNING user_id, full_name, email, role, status, password_hash
            """);
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = fullName, NpgsqlDbType = NpgsqlDbType.Varchar });
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = email, NpgsqlDbType = NpgsqlDbType.Varchar });
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = passwordHash, NpgsqlDbType = NpgsqlDbType.Varchar });
        try
        {
            await using var reader = await command.ExecuteReaderAsync(cancellationToken);
            await reader.ReadAsync(cancellationToken);
            return ReadAccount(reader);
        }
        catch (PostgresException exception) when (exception.SqlState == PostgresErrorCodes.UniqueViolation)
        {
            throw new AccountException("EMAIL_EXISTS", "Email này đã được sử dụng. Vui lòng đăng nhập.", "email");
        }
    }

    public async Task<AccountUser?> FindByEmailAsync(string email, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("""
            SELECT user_id, full_name, email, role, status, password_hash
            FROM public.app_user WHERE lower(email) = $1 LIMIT 1
            """);
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = email, NpgsqlDbType = NpgsqlDbType.Varchar });
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        return await reader.ReadAsync(cancellationToken) ? ReadAccount(reader) : null;
    }

    public async Task<AccountUser?> FindByIdAsync(long userId, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("""
            SELECT user_id, full_name, email, role, status, password_hash
            FROM public.app_user WHERE user_id = $1
            """);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        return await reader.ReadAsync(cancellationToken) ? ReadAccount(reader) : null;
    }

    public async Task UpdatePasswordAsync(long userId, string passwordHash, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("""
            UPDATE public.app_user SET password_hash = $2 WHERE user_id = $1
            """);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        command.Parameters.Add(new NpgsqlParameter<string> { TypedValue = passwordHash, NpgsqlDbType = NpgsqlDbType.Varchar });
        await command.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task RecordLoginAsync(long userId, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("UPDATE public.app_user SET last_login_at = clock_timestamp() WHERE user_id = $1");
        command.Parameters.AddWithValue(userId);
        await command.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task<OnboardingStatus> GetOnboardingStatusAsync(long userId, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("""
            SELECT EXISTS (SELECT 1 FROM public.user_preference up
                JOIN public.user_preference_place_type pt ON pt.user_preference_id = up.user_preference_id
                WHERE up.user_id = $1),
                EXISTS (SELECT 1 FROM public.user_preference WHERE user_id = $1 AND activity_level IN ('LOW','MEDIUM','HIGH')),
                EXISTS (SELECT 1 FROM public.virtual_location WHERE user_id = $1)
            """);
        command.Parameters.AddWithValue(userId);
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        await reader.ReadAsync(cancellationToken);
        return new OnboardingStatus(reader.GetBoolean(0), reader.GetBoolean(1), reader.GetBoolean(2));
    }

    public async Task<bool> HasSelectedPlaceTypesAsync(long userId, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("""
            SELECT EXISTS (
                SELECT 1 FROM public.user_preference up
                JOIN public.user_preference_place_type uppt ON uppt.user_preference_id = up.user_preference_id
                WHERE up.user_id = $1
            )
            """);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        return (bool)(await command.ExecuteScalarAsync(cancellationToken) ?? false);
    }

    public async Task<bool> HasActivityLevelAsync(long userId, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("""
            SELECT EXISTS (
                SELECT 1 FROM public.user_preference
                WHERE user_id = $1 AND activity_level IN ('LOW', 'MEDIUM', 'HIGH')
            )
            """);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        return (bool)(await command.ExecuteScalarAsync(cancellationToken) ?? false);
    }

    public async Task<bool> HasVirtualLocationAsync(long userId, CancellationToken cancellationToken)
    {
        await using var command = db.CreateCommand("""
            SELECT EXISTS (SELECT 1 FROM public.virtual_location WHERE user_id = $1)
            """);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        return (bool)(await command.ExecuteScalarAsync(cancellationToken) ?? false);
    }

    private static AccountUser ReadAccount(NpgsqlDataReader reader) => new(
        reader.GetInt64(0), reader.GetString(1), reader.GetString(2),
        reader.GetString(3), reader.GetString(4), reader.GetString(5));
}
