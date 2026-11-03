package com.project.bcv.core.di

import android.content.Context
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.statsnotify.notifications.data.NotificationRepositoryImpl
import com.project.bcv.feature.statsnotify.notifications.data.JsonNotificationApi
import com.project.bcv.feature.statsnotify.notifications.data.NotificationLocalState
import com.project.bcv.feature.statsnotify.stats.data.StatsRepositoryImpl

object StatsNotifyModule {
    fun stats(context: Context) = StatsRepositoryImpl(
        JsonApiClient(context, BuildConfig.API_BASE_URL), SessionPreferences(context)
    )

    fun notifications(context: Context): NotificationRepositoryImpl {
        val session = SessionPreferences(context)
        return NotificationRepositoryImpl(JsonNotificationApi(JsonApiClient(context, BuildConfig.API_BASE_URL)),
            session, NotificationLocalState(context, session))
    }
}
