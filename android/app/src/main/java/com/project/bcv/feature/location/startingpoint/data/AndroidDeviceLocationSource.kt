package com.project.bcv.feature.location.startingpoint.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import com.project.bcv.feature.location.startingpoint.domain.CurrentLocationPolicy
import com.project.bcv.feature.location.startingpoint.domain.DeviceLocationSource
import com.project.bcv.feature.location.startingpoint.domain.LocationFix

/**
 * Asks every enabled provider at once, including on devices without GMS. Cached positions are
 * only a fallback and must still be fresh when delivered; they never bypass a current request.
 */
class AndroidDeviceLocationSource(context: Context) : DeviceLocationSource {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val signals = mutableListOf<CancellationSignal>()
    private val listeners = mutableListOf<LocationListener>()
    private var timeout: Runnable? = null
    private var pending: ((Coordinates?) -> Unit)? = null
    private var settle: Runnable? = null
    private val cachedFixes = mutableListOf<LocationFix>()
    private val currentFixes = mutableListOf<LocationFix>()
    private val waitingProviders = mutableSetOf<String>()
    private var generation = 0L

    override fun findCurrent(callback: (Coordinates?) -> Unit) {
        cancel()
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!fine && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            callback(null)
            return
        }
        val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER).filter { name ->
            (fine || name != LocationManager.GPS_PROVIDER) &&
                runCatching { manager.isProviderEnabled(name) }.getOrDefault(false)
        }
        if (providers.isEmpty()) {
            callback(null)
            return
        }

        val requestGeneration = generation
        pending = callback
        providers.mapNotNullTo(cachedFixes) { provider ->
            runCatching { manager.getLastKnownLocation(provider)?.toFix() }.getOrNull()
        }
        waitingProviders.addAll(providers)
        timeout = Runnable { finish(requestGeneration) }
            .also { handler.postDelayed(it, TIMEOUT_MILLIS) }
        providers.forEach { request(it, requestGeneration) }
    }

    override fun cancel() {
        generation++
        pending = null
        cachedFixes.clear()
        currentFixes.clear()
        waitingProviders.clear()
        timeout?.let(handler::removeCallbacks)
        timeout = null
        settle?.let(handler::removeCallbacks)
        settle = null
        signals.forEach { it.cancel() }
        signals.clear()
        listeners.forEach { runCatching { manager.removeUpdates(it) } }
        listeners.clear()
    }

    private fun request(provider: String, requestGeneration: Long) {
        if (requestGeneration != generation || pending == null) return
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val signal = CancellationSignal().also(signals::add)
                val request = LocationRequest.Builder(0L)
                    .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
                    .setDurationMillis(TIMEOUT_MILLIS)
                    .build()
                manager.getCurrentLocation(provider, request, signal, appContext.mainExecutor) {
                    onResult(requestGeneration, provider, it)
                }
            } else if (Build.VERSION.SDK_INT >= 30) {
                val signal = CancellationSignal().also(signals::add)
                manager.getCurrentLocation(provider, signal, appContext.mainExecutor) {
                    onResult(requestGeneration, provider, it)
                }
            } else {
                val listener = LocationListener {
                    onResult(requestGeneration, provider, it)
                }.also(listeners::add)
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        } catch (_: Exception) {
            onResult(requestGeneration, provider, null)
        }
    }

    private fun onResult(requestGeneration: Long, provider: String, location: Location?) {
        if (requestGeneration != generation || pending == null || !waitingProviders.remove(provider)) return
        location?.toFix()?.let(currentFixes::add)
        val current = CurrentLocationPolicy.best(currentFixes, SystemClock.elapsedRealtime())
        when {
            current != null && CurrentLocationPolicy.precise(current) -> finish(requestGeneration)
            waitingProviders.isEmpty() -> finish(requestGeneration)
            // A rough fix arrived first; give the other providers a moment to beat it.
            current != null && settle == null ->
                settle = Runnable { finish(requestGeneration) }
                    .also { handler.postDelayed(it, SETTLE_MILLIS) }
        }
    }

    private fun finish(requestGeneration: Long) {
        if (requestGeneration != generation) return
        val callback = pending ?: return
        val fix = CurrentLocationPolicy.result(currentFixes, cachedFixes, SystemClock.elapsedRealtime())
        cancel()
        callback(fix?.coordinates)
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun Location.toFix() = LocationFix(
        Coordinates(latitude, longitude), elapsedRealtimeNanos / 1_000_000,
        accuracy.takeIf { hasAccuracy() }
    )

    private companion object {
        const val TIMEOUT_MILLIS = 20_000L
        const val SETTLE_MILLIS = 6_000L
    }
}
