package com.project.bcv.feature.account.auth.data.local

import android.content.Context
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.BackendScope

class SessionPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = context.applicationContext
        .getSharedPreferences(BackendScope.preferenceFile("bcv_session", BuildConfig.API_BASE_URL), Context.MODE_PRIVATE)

    var token: String?
        get() = preferences.getString("token", null)
        set(value) {
            preferences.edit().apply {
                if (value == null) remove("token") else putString("token", value)
            }.apply()
            if (value == null) com.project.bcv.feature.discovery.data.JourneyStepService.stop(appContext)
        }
}
