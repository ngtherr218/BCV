package com.project.bcv.feature.statsnotify.notifications.domain

import java.util.Date

/** [type]: JOURNEY_COMPLETED, QUEST_COMPLETED or ITEM_RECEIVED. */
data class AppNotification(val id: Long, val type: String, val title: String, val content: String, val read: Boolean, val createdAt: Date?)

data class NotificationPage(val unread: Int, val items: List<AppNotification>)

interface NotificationRepository {
    fun notifications(): NotificationPage
    fun markRead(id: Long): NotificationPage
}
