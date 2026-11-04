package com.project.bcv.core.di

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.activitylevel.data.ActivityLevelRepositoryImpl
import com.project.bcv.feature.account.activitylevel.domain.ActivityLevelRepository

object ActivityLevelModule {
    fun repository(context: Context): ActivityLevelRepository = ActivityLevelRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )
}
