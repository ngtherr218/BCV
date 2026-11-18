package com.project.bcv.feature.engagement.data

import com.project.bcv.feature.engagement.domain.CheckinResult
import com.project.bcv.feature.engagement.domain.CurrentQuiz
import com.project.bcv.feature.engagement.domain.EngagementRepository
import com.project.bcv.feature.engagement.domain.EngagementSummary
import com.project.bcv.feature.engagement.domain.QuizStop
import org.json.JSONObject

/** [EngagementRepository] over the engagement API: maps the server's JSON to domain models. */
class EngagementRepositoryImpl(private val api: EngagementApi) : EngagementRepository {

    override suspend fun checkIn(placeId: Long, latitude: Double, longitude: Double, test: Boolean): CheckinResult =
        api.checkIn(placeId, latitude, longitude, test).let { json ->
            CheckinResult(
                alreadyToday = json.optBoolean("alreadyToday"),
                message = json.optString("message"),
                placeName = json.optString("placeName"),
                points = json.optInt("points"),
                newDiscovery = json.optBoolean("newDiscovery"),
                pendingCard = json.text("pendingCard"),
                visits = json.optInt("visits", 1),
                province = json.text("province"),
                provinceStamped = json.optInt("provinceStamped"),
                provinceTotal = json.optInt("provinceTotal"),
            )
        }

    override suspend fun hasJournal(placeId: Long): Boolean = runCatching { api.journalEntry(placeId) }.isSuccess

    override suspend fun dismiss(placeId: Long) { api.dismiss(placeId) }
    override suspend fun undoDismiss(placeId: Long) { api.undoDismiss(placeId) }
    override suspend fun masteredPlaces(): Set<Long> = api.masteredPlaces()

    override suspend fun currentQuiz(): CurrentQuiz = api.quiz().let { json ->
        val stops = json.optJSONArray("stops")
        CurrentQuiz(
            journeyId = json.optLong("journeyId").takeIf { it > 0 },
            stops = (0 until (stops?.length() ?: 0)).map { i ->
                val stop = stops!!.getJSONObject(i)
                QuizStop(stop.optString("key"), stop.getInt("number"), stop.getDouble("fraction"),
                    stop.getString("state"), stop.optInt("unlockSteps", Int.MAX_VALUE))
            })
    }

    override suspend fun summary(): EngagementSummary = api.summary().let {
        EngagementSummary(it.getJSONObject("streak"), it.getJSONArray("badges"))
    }

    override suspend fun reportToday(force: Boolean) { api.reportToday(force) }
    override fun cachedDailyGoal(): Int = api.cachedDailyGoal()

    /** A string field, or null when absent, JSON null or blank. */
    private fun JSONObject.text(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
}
