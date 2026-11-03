package com.project.bcv.feature.discovery.data

import android.content.Context
import com.project.bcv.BuildConfig

/** A journey used for simulation keeps that source across reloads/restarts. */
class StepTestPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = context.applicationContext.getSharedPreferences("bcv_step_tests", Context.MODE_PRIVATE)
    private fun key(id: Long) = "${BuildConfig.API_BASE_URL}:$id"
    fun isTestJourney(id: Long) = preferences.getBoolean(key(id), false)
    fun useTestSteps(id: Long) {
        preferences.edit().putBoolean(key(id), true).apply()
        JourneyStepService.stop(appContext)
    }
    fun pendingTotal(id: Long): Long? = preferences.getLong("pending:${key(id)}", -1L).takeIf { it >= 0 }
    fun prepare(id: Long, total: Long) { preferences.edit().putLong("pending:${key(id)}", total).commit() }
    fun acknowledge(id: Long) { preferences.edit().remove("pending:${key(id)}").apply() }
}
