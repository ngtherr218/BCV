package com.project.bcv.core.di

import android.content.Context
import com.project.bcv.feature.engagement.data.EngagementApi
import com.project.bcv.feature.engagement.data.EngagementRepositoryImpl
import com.project.bcv.feature.engagement.domain.EngagementRepository

object EngagementModule {
    fun repository(context: Context): EngagementRepository = EngagementRepositoryImpl(EngagementApi(context))
}
