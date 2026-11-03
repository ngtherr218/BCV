package com.project.bcv.core.di

import android.content.Context
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.collection.data.CardInbox
import com.project.bcv.feature.collection.data.CollectionRepositoryImpl
import com.project.bcv.feature.collection.data.JsonCollectionApi

object CollectionModule {
    private var sessionToken: String? = null
    private var instance: CollectionRepositoryImpl? = null

    @Synchronized
    fun repository(context: Context): CollectionRepositoryImpl {
        val app = context.applicationContext
        val session = SessionPreferences(app)
        if (instance == null || sessionToken != session.token) {
            sessionToken = session.token
            instance = CollectionRepositoryImpl(JsonCollectionApi(JsonApiClient(app, BuildConfig.API_BASE_URL)),
                session, CardInbox(app))
        }
        return instance!!
    }
}
