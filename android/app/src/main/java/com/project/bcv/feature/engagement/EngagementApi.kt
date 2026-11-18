package com.project.bcv.feature.engagement
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
class EngagementApi(context: Context) {
    private val app = context.applicationContext
    private val client = JsonApiClient(app, BuildConfig.API_BASE_URL)
    private fun token() = SessionPreferences(app).token?.takeIf(String::isNotBlank) ?: error("Vui lòng đăng nhập lại.")
    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
    suspend fun summary(): JSONObject = io { client.get("api/engagement/summary", token()) }.also {
        it.optJSONObject("streak")?.optInt("dailyGoal")?.takeIf { goal -> goal > 0 }?.let { goal ->
            app.getSharedPreferences(com.project.bcv.core.network.BackendScope.preferenceFile("engagement_" + SessionPreferences(app).userId, BuildConfig.API_BASE_URL), Context.MODE_PRIVATE).edit().putInt("daily_goal", goal).apply()
        }
    }
    suspend fun pause(days: Int): JSONObject = io { client.post("api/activity/pause", JSONObject().put("days", days), token()) }
    suspend fun endPause(): JSONObject = io { client.delete("api/activity/pause", token()) }


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
     * board. Throttled to once per [(5 * 60 * 1000L)]; silently skipped without permission.
     */
    suspend fun reportToday(force: Boolean = false): JSONObject? {
        val prefs = app.getSharedPreferences("engagement", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong("last_report", 0) < MIN_REPORT_MILLIS) return null
        val steps = runCatching {
            val health = HealthConnectClient.getOrCreate(app)
            val granted = health.permissionController.getGrantedPermissions()
            if (HealthPermission.getReadPermission(StepsRecord::class) !in granted) return null
            val zone = ZoneId.of("Asia/Ho_Chi_Minh")
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


    suspend fun masteredPlaces(): Set<Long> = withContext(Dispatchers.IO) {
        val ids = client.get("api/engagement/mastered-places", token()).getJSONArray("placeIds")
        (0 until ids.length()).map { ids.getLong(it) }.toSet()
    }
    suspend fun ranking(week: String): JSONObject = withContext(Dispatchers.IO) {
        client.get("api/leaderboard?limit=20&week=$week", token())
    }
    suspend fun quiz(): JSONObject = withContext(Dispatchers.IO) { client.get("api/journeys/current/quiz", token()) }
    suspend fun answer(key: String, answer: String): JSONObject = withContext(Dispatchers.IO) {
        client.post("api/journeys/current/quiz", JSONObject().put("key", key).put("answer", answer), token())
    }
    companion object { private const val MIN_REPORT_MILLIS = 5 * 60 * 1000L }

}
