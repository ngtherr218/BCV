package com.project.bcv.core.di

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.questitem.inventory.data.InventoryRepositoryImpl
import com.project.bcv.feature.questitem.inventory.data.ItemUseRepositoryImpl

object InventoryModule {
    fun repository(context: Context) = InventoryRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )

    fun items(context: Context) = ItemUseRepositoryImpl(
        JsonApiClient(context, ApiEnvironment.currentUrl), SessionPreferences(context)
    )
}
