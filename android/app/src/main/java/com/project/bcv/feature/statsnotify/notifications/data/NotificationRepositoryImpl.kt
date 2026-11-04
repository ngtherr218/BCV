package com.project.bcv.feature.statsnotify.notifications.data

import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.core.network.ServerDates
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.statsnotify.notifications.domain.AppNotification
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationPage
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationRepository
import org.json.JSONObject

class NotificationRepositoryImpl(
    private val remote: JsonApiClient,
    private val session: SessionPreferences
) : NotificationRepository {
    private fun token() = session.token
        ?: throw ApiFailure("UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")

    override fun notifications() = remote.get("api/notifications", token()).toPage()

    override fun markRead(id: Long) = remote.post("api/notifications/$id/read", JSONObject(), token()).toPage()

    private fun JSONObject.toPage(): NotificationPage {
        val items = getJSONArray("items")
        return NotificationPage(getInt("unreadCount"), (0 until items.length()).map { index ->
            items.getJSONObject(index).let {
                AppNotification(it.getLong("notificationId"), it.getString("type"), it.getString("title"),
                    it.getString("content"), it.getBoolean("read"), ServerDates.parse(it.optString("createdAt")))
            }
        })
    }
}
