package com.project.bcv.core.di

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.location.startingpoint.data.AndroidDeviceLocationSource
import com.project.bcv.feature.location.startingpoint.data.GoongAddressLookup
import com.project.bcv.feature.location.startingpoint.data.StartingPointRepositoryImpl
import com.project.bcv.feature.location.startingpoint.domain.StartingPointRepository

object StartingPointModule {
    fun repository(context: Context): StartingPointRepository = StartingPointRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )

    fun locationSource(context: Context) = AndroidDeviceLocationSource(context)

    fun addressLookup() = GoongAddressLookup(BuildConfig.GOONG_API_KEY)
}
