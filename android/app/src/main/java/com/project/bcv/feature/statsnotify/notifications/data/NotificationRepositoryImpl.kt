package com.project.bcv.feature.statsnotify.notifications.data

import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.ServerDates
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.statsnotify.notifications.domain.AppNotification
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationPage
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationRepository
import org.json.JSONObject

class NotificationRepositoryImpl(
    private val remote: NotificationApi,
    private val session: SessionPreferences,
    private val local: NotificationLocalState,
    private val readSync: java.util.concurrent.Executor = synchronization
) : NotificationRepository {
    private fun token() = session.token
        ?: throw ApiFailure("UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")

    private var latest: NotificationPage? = null

    override fun notifications(): NotificationPage {
        val page = remote.get("api/notifications", token()).toPage()
        latest = page
        return local.apply(page)
    }

    override fun markRead(id: Long): NotificationPage {
        val response = remote.post("api/notifications/$id/read", JSONObject(), token())
        val page = if (response.has("items")) response.toPage() else current().let { page ->
            page.copy(unread = (page.unread - 1).coerceAtLeast(0), items = page.items.map { if (it.id == id) it.copy(read = true) else it })
        }
        latest = page
        return local.apply(page)
    }

    private fun current(): NotificationPage = latest ?: run { notifications(); latest!! }

    override fun markAllRead(): NotificationPage {
        val page = current()
        val sessionToken = token()
        local.markAllRead(page)
        readSync.execute {
            try { remote.post("api/notifications/read-all", JSONObject(), sessionToken) }
            catch (_: Exception) {
                // Older/offline APIs must not undo the display state or block the button.
            }
        }
        return local.apply(page)
    }

    override fun deleteAll(): NotificationPage {
        val page = current()
        local.hideAll(page)
        return local.apply(page)
    }

    private fun JSONObject.toPage(): NotificationPage {
        val items = getJSONArray("items")
        return NotificationPage(getInt("unreadCount"), (0 until items.length()).map { index ->
            items.getJSONObject(index).let {
                AppNotification(it.getLong("notificationId"), it.getString("type"), it.getString("title"),
                    it.getString("content"), it.getBoolean("read"), ServerDates.parse(it.optString("createdAt")))
            }
        })
    }

    private companion object {
        val synchronization: java.util.concurrent.Executor = java.util.concurrent.Executors.newSingleThreadExecutor()
    }
}
