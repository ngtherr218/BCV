package com.project.bcv.core.di

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.interests.data.InterestRepositoryImpl
import com.project.bcv.feature.account.interests.domain.InterestRepository

object InterestModule {
    fun repository(context: Context): InterestRepository = InterestRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )
}
