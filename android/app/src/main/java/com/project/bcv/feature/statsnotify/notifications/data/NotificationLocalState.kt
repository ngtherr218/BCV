package com.project.bcv.feature.statsnotify.notifications.data

import android.content.Context
import com.project.bcv.BuildConfig
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationPage
import java.security.MessageDigest

/** Read/hidden watermarks affect existing notices only; later notices retain their own state. */
class NotificationLocalState(context: Context, private val session: SessionPreferences,
    namespace: String = "notification_display_state") {
    private val prefs = context.applicationContext.getSharedPreferences(namespace, Context.MODE_PRIVATE)
    private fun scope(): String {
        val identity = session.userId?.toString() ?: session.token.orEmpty()
        return MessageDigest.getInstance("SHA-256").digest((BuildConfig.API_BASE_URL + identity).toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    fun markAllRead(page: NotificationPage) {
        val key = "${scope()}:read"
        val latest = page.items.maxOfOrNull { it.id } ?: return
        prefs.edit().putLong(key, maxOf(latest, prefs.getLong(key, 0))).apply()
    }
    fun hideAll(page: NotificationPage) {
        val key = "${scope()}:hidden"
        val latest = page.items.maxOfOrNull { it.id } ?: return
        prefs.edit().putLong(key, maxOf(latest, prefs.getLong(key, 0))).apply()
    }
    fun apply(page: NotificationPage): NotificationPage {
        val key = scope()
        val read = prefs.getLong("$key:read", 0)
        val hidden = prefs.getLong("$key:hidden", 0)
        val items = page.items.filter { it.id > hidden }.map {
            if (it.id <= read) it.copy(read = true) else it
        }
        val unread = if (read == 0L && hidden == 0L) page.unread else items.count { !it.read }
        return NotificationPage(unread, items)
    }
}
