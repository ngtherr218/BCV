package com.project.bcv.core.di

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.questitem.quest.data.QuestRepositoryImpl

object QuestModule {
    fun repository(context: Context) = QuestRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )
}
