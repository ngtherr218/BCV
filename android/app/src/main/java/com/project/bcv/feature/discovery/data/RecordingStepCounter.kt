package com.project.bcv.feature.discovery.data

import com.project.bcv.core.network.ApiEnvironment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.project.bcv.core.network.BackendScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.LocalRecordingClient
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.data.LocalField
import com.google.android.gms.fitness.request.LocalDataReadRequest
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fallback step count from Google Play services' Recording API, used only when no app records steps
 * in Health Connect. Once subscribed, Play services counts steps with the phone's sensor in the
 * background (also while the app is closed and across reboots) and keeps them on the device for
 * 10 days. The steps stay with this app (they are never written to Health Connect), so nothing read
 * back from Health Connect can count them a second time.
 *
 * Steps are recorded only from the subscription on; a journey started earlier misses its first steps.
 */
class RecordingStepCounter(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(BackendScope.preferenceFile("bcv_recording_steps", ApiEnvironment.currentUrl), Context.MODE_PRIVATE)
    private val client by lazy { FitnessLocal.getLocalRecordingClient(appContext) }

    /** Play services is installed and new enough for the Recording API. */
    val isAvailable: Boolean by lazy {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext,
            LocalRecordingClient.LOCAL_RECORDING_CLIENT_MIN_VERSION_CODE) == ConnectionResult.SUCCESS
    }

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    val isUsable: Boolean get() = isAvailable && hasPermission()

    /** Whether this journey already counts with the Recording API (it then keeps doing so to the end). */
    fun isTracking(journeyId: Long): Boolean = preferences.getLong(KEY_JOURNEY, NONE) == journeyId

    /** Subscribes (again, which is harmless) and marks [journeyId] as counted by the Recording API. */
    suspend fun start(journeyId: Long) {
        if (!isUsable) return
        client.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA).await()
        preferences.edit().putLong(KEY_JOURNEY, journeyId).apply()
    }

    /** Stops recording once no journey needs it, so steps are not collected for nothing. */
    suspend fun stop() {
        if (!preferences.contains(KEY_JOURNEY) || !isAvailable) return
        runCatching { client.unsubscribe(LocalDataType.TYPE_STEP_COUNT_DELTA).await() }
        preferences.edit().remove(KEY_JOURNEY).apply()
    }

    /** Steps recorded since [startedAt]; only the last 10 days are kept by Play services. */
    suspend fun stepsSince(startedAt: Instant): Long {
        if (!isUsable) return 0
        val end = Instant.now()
        val start = maxOf(startedAt, end.minus(KEPT_DAYS, ChronoUnit.DAYS))
        if (!start.isBefore(end)) return 0
        val request = LocalDataReadRequest.Builder()
            .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(start.epochSecond, end.epochSecond, TimeUnit.SECONDS)
            .build()
        return client.readData(request).await().buckets
            .flatMap { it.dataSets }.flatMap { it.dataPoints }
            .sumOf { it.getValue(LocalField.FIELD_STEPS).asInt().toLong() }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }

    private companion object {
        const val KEY_JOURNEY = "journey_id"
        const val NONE = -1L
        const val KEPT_DAYS = 10L
    }
}
