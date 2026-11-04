package com.project.bcv.feature.discovery.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.project.bcv.R
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.BackendScope
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.journey.presentation.JourneyActivity

/** Owns the hardware listener independently of Activities; never reads Health Connect in background. */
class JourneyStepService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val sensor by lazy { DeviceStepCounter.get(this) }
    private var detectorWakeLock: PowerManager.WakeLock? = null
    private var destination = ""
    private var lastDisplayed = -1L
    private val heartbeat = object : Runnable {
        override fun run() {
            if (!sensor.isUsable || SessionPreferences(this@JourneyStepService).token == null) {
                stop(this@JourneyStepService)
                return
            }
            if (sensor.requiresWakeLock) {
                if (detectorWakeLock == null) {
                    detectorWakeLock = getSystemService(PowerManager::class.java)
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BCV:JourneyStepDetector")
                        .apply { setReferenceCounted(false) }
                }
                // Only the non-wakeup STEP_DETECTOR needs CPU awake; a hardware counter retains its total.
                detectorWakeLock?.acquire(10 * 60_000L)
            }
            val total = sensor.total()
            if (total != lastDisplayed) {
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
                lastDisplayed = total
            }
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Đếm bước hành trình", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Duy trì đếm bước khi khóa màn hình hoặc chuyển ứng dụng." }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        val journeyId = intent?.getLongExtra("journey", 0) ?: saved.getLong("journey", 0)
        destination = intent?.getStringExtra("destination") ?: saved.getString("destination", "").orEmpty()
        if (journeyId <= 0 || !sensor.isUsable || SessionPreferences(this).token == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0)
            sensor.start(journeyId)
            runningJourney = journeyId
            saved.edit().putLong("journey", journeyId).putString("destination", destination).apply()
            handler.removeCallbacks(heartbeat)
            heartbeat.run()
            START_STICKY
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not keep the journey sensor running", error)
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, JourneyActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_directions_walk)
            .setContentTitle("Bước Chân Việt đang đếm bước")
            .setContentText(if (destination.isBlank()) "Tiếp tục ghi bước khi khóa màn hình." else "Hành trình đến $destination")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(heartbeat)
        sensor.stop()
        runningJourney = 0
        detectorWakeLock?.let { if (it.isHeld) it.release() }
        detectorWakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "JourneyStepService"
        private const val CHANNEL = "bcv_journey_steps"
        private const val NOTIFICATION_ID = 2101
        private val PREFERENCES = BackendScope.preferenceFile("bcv_step_service", BuildConfig.API_BASE_URL)
        @Volatile var runningJourney: Long = 0
            private set

        /** Call while the app is visible. A denied background start can be retried on the next resume. */
        fun start(context: Context, journeyId: Long, destination: String = ""): Boolean {
            if (!DeviceStepCounter.get(context).isUsable || SessionPreferences(context).token == null) return false
            if (runningJourney == journeyId) return true
            return try {
                ContextCompat.startForegroundService(context, Intent(context, JourneyStepService::class.java)
                    .putExtra("journey", journeyId).putExtra("destination", destination))
                true
            } catch (error: RuntimeException) {
                Log.w(TAG, "Start step tracking again when the app returns to foreground", error)
                false
            }
        }

        fun stop(context: Context) {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().remove("journey").apply()
            DeviceStepCounter.get(context).stop(resetBaseline = true)
            context.stopService(Intent(context, JourneyStepService::class.java))
        }
    }
}
