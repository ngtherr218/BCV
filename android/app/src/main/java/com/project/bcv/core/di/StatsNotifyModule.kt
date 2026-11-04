package com.project.bcv.core.di

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.statsnotify.notifications.data.NotificationRepositoryImpl
import com.project.bcv.feature.statsnotify.stats.data.StatsRepositoryImpl

object StatsNotifyModule {
    fun stats(context: Context) = StatsRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )

    fun notifications(context: Context) = NotificationRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )
}
