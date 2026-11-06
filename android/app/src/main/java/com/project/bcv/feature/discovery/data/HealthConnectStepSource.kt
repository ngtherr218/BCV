package com.project.bcv.feature.discovery.data

import android.content.Context
import android.os.SystemClock
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.BackendScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.project.bcv.feature.discovery.domain.StepSource
import com.project.bcv.feature.discovery.domain.StepProvider
import com.project.bcv.feature.discovery.domain.StepReconciliation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.temporal.ChronoUnit

/** One journey source, a background hardware shadow, and a persisted monotonic reconciliation. */
class HealthConnectStepSource(context: Context) : StepSource {
    private val app = context.applicationContext
    private val readPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val sensor = DeviceStepCounter.get(app)
    private val store = JourneyStepStateStore(app)
    private val mutex = Mutex()
    private var activeJourney = 0L
    private var destination = ""

    // The service owns the listener. Closing a screen must not stop an active journey.
    override fun release() = Unit

    override suspend fun hasPermission(): Boolean = when (store.load(activeJourney, sensor.isUsable)?.provider) {
        StepProvider.SENSOR -> sensor.isUsable
        else -> canReadHealthConnect() || sensor.isUsable
    }

    // A hardware shadow is needed even with healthy Health Connect to detect a stalled writer.
    override suspend fun needsActivityPermission(): Boolean =
        sensor.isAvailable && !sensor.hasPermission()

    override suspend fun capture(journeyId: Long, startedAtEpochSeconds: Long, destinationName: String) = mutex.withLock {
        activeJourney = journeyId
        destination = destinationName
        if (journeyId <= 0) {
            JourneyStepService.stop(app)
            return@withLock
        }
        stateFor(journeyId)
        if (sensor.isUsable) JourneyStepService.start(app, journeyId, destination)
    }

    override suspend fun totalStepsSince(journeyId: Long, startedAtEpochSeconds: Long): Long = mutex.withLock {
        activeJourney = journeyId
        var state = stateFor(journeyId)
        val trackerReady = sensor.isUsable && JourneyStepService.start(app, journeyId, destination)
        val shadow = if (sensor.isUsable && sensor.isTracking(journeyId)) sensor.total() else null
        if (state.provider == StepProvider.SENSOR) {
            check(trackerReady) { "Chưa bật được đếm bước nền. Mở ứng dụng và kiểm tra quyền Hoạt động thể chất." }
            state = state.observe(null, shadow, SystemClock.elapsedRealtime())
        } else {
            val primary = recoverProvider {
                val start = Instant.ofEpochSecond(startedAtEpochSeconds)
                healthSteps(start)
            }
            // Sample the shadow AFTER the provider reply, so a slow read cannot overlap credited steps.
            val shadowAfterRead = if (sensor.isUsable && sensor.isTracking(journeyId)) sensor.total() else null
            if (primary == null && shadowAfterRead == null) {
                error("Chưa đọc được nguồn bước chân. Tiến độ đã lưu vẫn được giữ nguyên.")
            }
            state = state.observe(primary, shadowAfterRead, SystemClock.elapsedRealtime(), failed = primary == null)
        }
        store.save(journeyId, state)
        state.total
    }

    private suspend fun stateFor(journeyId: Long): StepReconciliation {
        store.load(journeyId, sensor.isUsable)?.let {
            store.save(journeyId, it)
            return it
        }
        val legacy = app.getSharedPreferences(BackendScope.preferenceFile("bcv_step_fallback", BuildConfig.API_BASE_URL), Context.MODE_PRIVATE)
        val saved = if (legacy.getLong("last_journey", -1) == journeyId) legacy.getLong("last_total", 0) else 0L
        val shadow = if (sensor.isTracking(journeyId)) sensor.total() else 0L
        val provider = when {
            legacy.getLong("sensor_journey", -1) == journeyId -> StepProvider.SENSOR
            hasRecentHealthWriter() -> StepProvider.HEALTH_CONNECT
            sensor.isUsable -> StepProvider.SENSOR
            else -> StepProvider.HEALTH_CONNECT
        }
        val carry = if (provider == StepProvider.SENSOR && legacy.getLong("sensor_journey", -1) == journeyId)
            maxOf(saved, legacy.getLong("sensor_offset", 0) + shadow) else saved
        return StepReconciliation(provider, total = carry, sensorAnchor = shadow,
            lastPrimaryAdvanceAt = SystemClock.elapsedRealtime()).also { store.save(journeyId, it) }
    }

    private suspend fun canReadHealthConnect(): Boolean = recoverProvider {
        HealthConnectClient.getSdkStatus(app) == HealthConnectClient.SDK_AVAILABLE &&
            HealthConnectClient.getOrCreate(app).permissionController.getGrantedPermissions().contains(readPermission)
    } ?: false

    private suspend fun hasRecentHealthWriter(): Boolean = canReadHealthConnect() && (recoverProvider {
        val end = Instant.now()
        HealthConnectClient.getOrCreate(app).aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL),
            TimeRangeFilter.between(end.minus(1, ChronoUnit.DAYS), end)))
            .dataOrigins.any { it.packageName != app.packageName }
    } ?: false)

    private suspend fun healthSteps(start: Instant): Long {
        check(canReadHealthConnect()) { "Chưa có quyền đọc Health Connect." }
        val end = Instant.now()
        if (!start.isBefore(end)) return 0
        val request = AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(start, end))
        return HealthConnectClient.getOrCreate(app).aggregate(request)[StepsRecord.COUNT_TOTAL] ?: 0
    }

    private suspend fun <T> recoverProvider(block: suspend () -> T): T? = try {
        withTimeout(5_000L) { block() }
    } catch (error: Exception) {
        if (error is CancellationException && error !is TimeoutCancellationException) throw error
        null
    }
}

/** Kept separate from raw sensor state, so service callbacks cannot overwrite a source checkpoint. */
private class JourneyStepStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(BackendScope.preferenceFile("bcv_journey_step_state", BuildConfig.API_BASE_URL), Context.MODE_PRIVATE)
    fun load(journeyId: Long, sensorUsable: Boolean): StepReconciliation? {
        if (journeyId <= 0 || preferences.getLong("journey", -1) != journeyId) return null
        val provider = preferences.getString("provider", null) ?: return null
        return StepReconciliation.restore(provider, preferences.getLong("total", 0),
            preferences.getLong("primary", -1).takeIf { it >= 0 },
            preferences.getLong("sensor_anchor", -1).takeIf { it >= 0 },
            preferences.getLong("last_advance", 0), sensorUsable)
    }
    fun save(journeyId: Long, state: StepReconciliation) {
        preferences.edit().putLong("journey", journeyId).putString("provider", state.provider.name)
            .putLong("total", state.total).putLong("primary", state.primaryRaw ?: -1)
            .putLong("sensor_anchor", state.sensorAnchor ?: -1)
            .putLong("last_advance", state.lastPrimaryAdvanceAt).apply()
    }
}
