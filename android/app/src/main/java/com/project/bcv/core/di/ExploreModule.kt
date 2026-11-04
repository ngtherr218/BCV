package com.project.bcv.core.di

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.discovery.data.ExploreRepositoryImpl
import com.project.bcv.feature.discovery.data.HealthConnectStepSource

object ExploreModule {
    fun repository(context: Context) = ExploreRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )

    @Volatile private var sharedStepSource: HealthConnectStepSource? = null
    fun stepSource(context: Context): HealthConnectStepSource = sharedStepSource ?: synchronized(this) {
        sharedStepSource ?: HealthConnectStepSource(context.applicationContext).also { sharedStepSource = it }
    }

    fun addressLookup() = StartingPointModule.addressLookup()
}
