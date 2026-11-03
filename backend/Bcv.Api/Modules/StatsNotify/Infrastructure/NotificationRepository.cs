using Npgsql;
using NpgsqlTypes;
using Bcv.Api.Modules.StatsNotify.Application;

namespace Bcv.Api.Modules.StatsNotify.Infrastructure;

public sealed class NotificationRepository(NpgsqlDataSource db)
{
    public async Task<NotificationList> NotificationsAsync(long userId, CancellationToken ct)
    {
        await using var connection = await db.OpenConnectionAsync(ct);
        await using var command = new NpgsqlCommand("""
            SELECT notification_id, notification_type, title, content, read_status, created_at
            FROM public.notification WHERE user_id = $1 AND hidden_at IS NULL
            ORDER BY created_at DESC, notification_id DESC
            LIMIT 100
            """, connection);
        command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        var items = new List<NotificationView>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
            items.Add(new NotificationView(reader.GetInt64(0), reader.GetString(1), reader.GetString(2),
                reader.GetString(3), reader.GetBoolean(4), reader.GetDateTime(5)));
        await reader.DisposeAsync();
        await using var unread = new NpgsqlCommand(
            "SELECT COUNT(*) FROM public.notification WHERE user_id = $1 AND hidden_at IS NULL AND read_status = FALSE", connection);
        unread.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
        return new NotificationList(Convert.ToInt32(await unread.ExecuteScalarAsync(ct)), items);
    }

    public async Task<NotificationList> MarkAllReadAsync(long userId, CancellationToken ct)
    {
        await using (var connection = await db.OpenConnectionAsync(ct))
        await using (var command = new NpgsqlCommand(
            "UPDATE public.notification SET read_status = TRUE WHERE user_id = $1 AND read_status = FALSE", connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await command.ExecuteNonQueryAsync(ct);
        }
        return await NotificationsAsync(userId, ct);
    }

    public async Task<NotificationList> DeleteAllAsync(long userId, CancellationToken ct)
    {
        await using (var connection = await db.OpenConnectionAsync(ct))
        await using (var command = new NpgsqlCommand(
            "UPDATE public.notification SET hidden_at = CURRENT_TIMESTAMP WHERE user_id = $1 AND hidden_at IS NULL", connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await command.ExecuteNonQueryAsync(ct);
        }
        return await NotificationsAsync(userId, ct);
    }

    public async Task<NotificationList> MarkReadAsync(long userId, long notificationId, CancellationToken ct)
    {
        await using (var connection = await db.OpenConnectionAsync(ct))
        await using (var command = new NpgsqlCommand("""
            UPDATE public.notification SET read_status = TRUE WHERE user_id = $1 AND notification_id = $2
            """, connection))
        {
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = userId, NpgsqlDbType = NpgsqlDbType.Bigint });
            command.Parameters.Add(new NpgsqlParameter<long> { TypedValue = notificationId, NpgsqlDbType = NpgsqlDbType.Bigint });
            await command.ExecuteNonQueryAsync(ct);
        }
        return await NotificationsAsync(userId, ct);
    }
}
