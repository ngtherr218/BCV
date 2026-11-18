package com.project.bcv.feature.engagement

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/**
 * Client for the engagement API: streaks, passport, badges, leaderboard, check-in,
 * culture questions, "Không quan tâm" and the private journal.
 */
class EngagementApi(context: Context) {
    private val app = context.applicationContext
    private val client = JsonApiClient(app, BuildConfig.API_BASE_URL)
    private fun token() = SessionPreferences(app).token?.takeIf(String::isNotBlank) ?: error("Vui lòng đăng nhập lại.")

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    suspend fun summary(): JSONObject = io { client.get("api/engagement/summary", token()) }.also {
        it.optJSONObject("streak")?.optInt("dailyGoal")?.takeIf { goal -> goal > 0 }?.let { goal ->
            app.getSharedPreferences("engagement", Context.MODE_PRIVATE).edit().putInt("daily_goal", goal).apply()
        }
    }
    suspend fun leaderboard(scope: String): JSONObject = io { client.get("api/engagement/leaderboard?scope=$scope", token()) }

    /** The server's result: message, alreadyToday, points, newDiscovery, newCard, province stamp… */
    suspend fun checkIn(placeId: Long, latitude: Double, longitude: Double, test: Boolean = false): JSONObject = io {
        client.post("api/explore/places/$placeId/checkin",
            JSONObject().put("latitude", latitude).put("longitude", longitude).put("test", test), token())
    }

    suspend fun dismiss(placeId: Long) = io { client.post("api/explore/places/$placeId/dismiss", JSONObject(), token()) }
    suspend fun undoDismiss(placeId: Long) = io { client.delete("api/explore/places/$placeId/dismiss", token()) }

    suspend fun masteredPlaces(): Set<Long> = io {
        client.get("api/engagement/mastery", token()).getJSONArray("placeIds").let { a -> (0 until a.length()).map(a::getLong).toSet() }
    }
    suspend fun quiz(): JSONObject = io { client.get("api/journeys/current/quiz", token()) }
    suspend fun answer(key: String, answer: String): JSONObject =
        io { client.post("api/journeys/current/quiz", JSONObject().put("key", key).put("answer", answer), token()) }

    suspend fun journal(): JSONObject = io { client.get("api/journal", token()) }
    suspend fun journalEntry(placeId: Long): JSONObject = io { client.get("api/journal/places/$placeId", token()) }
    suspend fun saveNote(placeId: Long, note: String) =
        io { client.put("api/journal/places/$placeId/note", JSONObject().put("note", note), token()) }
    suspend fun addPhoto(placeId: Long, bytes: ByteArray, mime: String): JSONObject =
        io { client.postBytes("api/journal/places/$placeId/photos", bytes, mime, token()) }
    suspend fun deletePhoto(photoId: Long) = io { client.delete("api/journal/photos/$photoId", token()) }
    fun photoToken() = token()

    /** Official place image: public https URLs load directly, inline ones through the thumbnail endpoint. */
    suspend fun placeImage(url: String?, sample: Int): android.graphics.Bitmap? = when {
        url.isNullOrBlank() || url == "null" -> null
        url.startsWith("https://") -> com.project.bcv.core.ui.RemoteImages.load(url, sample)
        else -> com.project.bcv.core.ui.RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, url, token(), sample)
    }

    /**
     * F83: today's total steps from Health Connect (local midnight → now) feed the streak and the weekly
     * board. Throttled to once per [MIN_REPORT_MILLIS]; silently skipped without permission.
     */
    suspend fun reportToday(force: Boolean = false): JSONObject? {
        val prefs = app.getSharedPreferences("engagement", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong("last_report", 0) < MIN_REPORT_MILLIS) return null
        val steps = runCatching {
            val health = HealthConnectClient.getOrCreate(app)
            val granted = health.permissionController.getGrantedPermissions()
            if (HealthPermission.getReadPermission(StepsRecord::class) !in granted) return null
            val zone = ZoneId.systemDefault()
            val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            health.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL),
                TimeRangeFilter.between(start, java.time.Instant.now())))[StepsRecord.COUNT_TOTAL] ?: 0L
        }.getOrNull() ?: return null
        val result = runCatching {
            io {
                client.post("api/activity/daily", JSONObject()
                    .put("date", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString())
                    .put("steps", steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()), token())
            }
        }.getOrNull()
        if (result != null) prefs.edit().putLong("last_report", now).putInt("daily_goal", result.optInt("dailyGoal", 6000)).apply()
        return result
    }

    companion object {
        const val MIN_REPORT_MILLIS = 5 * 60 * 1000L

        /** The personal daily goal from the last server answer (6,000 = "vừa" until known). */
        fun cachedDailyGoal(context: Context) =
            context.getSharedPreferences("engagement", Context.MODE_PRIVATE).getInt("daily_goal", 6000)

        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
            return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        }
    }
}
