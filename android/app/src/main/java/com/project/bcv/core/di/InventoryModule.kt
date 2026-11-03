package com.project.bcv.core.di

import android.content.Context
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.questitem.inventory.data.InventoryRepositoryImpl
import com.project.bcv.feature.questitem.inventory.data.ItemUseRepositoryImpl

object InventoryModule {
    fun repository(context: Context) = InventoryRepositoryImpl(
        JsonApiClient(context, BuildConfig.API_BASE_URL), SessionPreferences(context)
    )

    fun items(context: Context, itemId: Long? = null) = ItemUseRepositoryImpl(
        JsonApiClient(context, BuildConfig.API_BASE_URL), SessionPreferences(context), itemId
    )
}
