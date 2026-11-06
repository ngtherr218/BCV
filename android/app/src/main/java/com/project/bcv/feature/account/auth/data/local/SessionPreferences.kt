package com.project.bcv.feature.account.auth.data.local

import android.content.Context
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.BackendScope
import com.project.bcv.core.ui.AppDataChanges
import com.project.bcv.core.ui.TabViewModels

class SessionPreferences(context: Context, namespace: String = BackendScope.preferenceFile("bcv_session", BuildConfig.API_BASE_URL)) {
    private val appContext = context.applicationContext
    private val preferences = context.applicationContext
        .getSharedPreferences(namespace, Context.MODE_PRIVATE)
    /** Routing hint only; every protected API still validates the token on the server. */
    var readyForHome: Boolean
        get() = token != null && preferences.getBoolean("ready_for_home", false)
        set(value) { preferences.edit().putBoolean("ready_for_home", value && token != null).apply() }

    var userId: Long?
        get() = preferences.getLong("user_id", 0L).takeIf { it > 0 }
        set(value) { preferences.edit().apply { if (value == null) remove("user_id") else putLong("user_id", value) }.apply() }

    var token: String?
        get() = preferences.getString("token", null)
        set(value) {
            val previousToken = token
            val changed = previousToken != value
            preferences.edit().apply {
                if (changed || value == null) { remove("ready_for_home"); remove("user_id") }
                if (value == null) remove("token") else putString("token", value)
            }.apply()
            if (changed || value == null) {
                TabViewModels.clearSession(previousToken)
                AppDataChanges.changed()
            }
            if (value == null) com.project.bcv.feature.discovery.data.JourneyStepService.stop(appContext)
        }
}
