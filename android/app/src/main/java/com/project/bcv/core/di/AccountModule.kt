package com.project.bcv.core.di

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import com.project.bcv.feature.account.auth.data.AccountRepositoryImpl
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.domain.AccountRepository

/** Manual composition root for the Account feature. */
object AccountModule {
    fun repository(context: Context): AccountRepository = AccountRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl),
        SessionPreferences(context)
    )
}
