package com.project.bcv.feature.discovery.data

import com.project.bcv.core.network.ApiEnvironment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.project.bcv.core.network.BackendScope
import com.project.bcv.feature.discovery.domain.SensorStepProgress

/** Hardware counter owned by JourneyStepService, including phones without Google Play services. */
class DeviceStepCounter private constructor(context: Context) : SensorEventListener {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences(BackendScope.preferenceFile("bcv_sensor_steps", ApiEnvironment.currentUrl), Context.MODE_PRIVATE)
    private val manager = app.getSystemService(SensorManager::class.java)
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        ?: manager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    @Volatile private var listening = false
    private var listeningSince = 0L
    private val boot get() = Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1)
    val isAvailable get() = sensor != null
    val requiresWakeLock get() = sensor?.let { it.type == Sensor.TYPE_STEP_DETECTOR && !it.isWakeUpSensor } ?: false
    fun hasPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    val isUsable get() = isAvailable && hasPermission()
    fun isTracking(journeyId: Long) = preferences.getLong("journey", -1) == journeyId

    fun start(journeyId: Long) {
        check(isUsable) { "Cần cảm biến bước chân và quyền Hoạt động thể chất." }
        if (!isTracking(journeyId)) {
            stop()
            preferences.edit().clear().putLong("journey", journeyId).apply()
        }
        if (!listening) {
            listeningSince = SystemClock.elapsedRealtimeNanos()
            listening = manager!!.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL,
                Handler(Looper.getMainLooper()))
            check(listening) { "Chưa khởi động được cảm biến bước chân." }
        }
    }

    fun stop(resetBaseline: Boolean = false) {
        manager?.unregisterListener(this)
        listening = false
        if (resetBaseline) preferences.edit().remove("raw").remove("boot").apply()
    }

    fun total() = preferences.getLong("total", 0)

    override fun onSensorChanged(event: SensorEvent) {
        if (!listening || !hasPermission() || event.values.isEmpty()) return
        val previous = SensorStepProgress(total(), preferences.getLong("raw", -1).takeIf { it >= 0 },
            preferences.getInt("boot", -1))
        val next = when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val raw = event.values[0]
                if (!raw.isFinite() || raw < 0) return
                previous.counter(raw.toLong(), boot)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.timestamp < listeningSince || event.values[0] != 1f) return
                previous.detected()
            }
            else -> return
        }
        preferences.edit().putLong("total", next.total).putLong("raw", next.lastRaw ?: -1)
            .putInt("boot", next.boot).apply()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private val instances = mutableMapOf<String, DeviceStepCounter>()
        fun get(context: Context): DeviceStepCounter = synchronized(this) {
            instances.getOrPut(ApiEnvironment.currentUrl) {
                DeviceStepCounter(context)
            }
        }
    }
}
