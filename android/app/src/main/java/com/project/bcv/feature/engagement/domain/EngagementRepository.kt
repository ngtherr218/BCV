package com.project.bcv.feature.engagement.domain

import org.json.JSONArray
import org.json.JSONObject

/** What a real check-in earned (or that today's was already counted). */
data class CheckinResult(
    val alreadyToday: Boolean,
    val message: String,
    val placeName: String,
    val points: Int,
    val newDiscovery: Boolean,
    /** The card still to earn by finishing the journey there, if any. */
    val pendingCard: String?,
    val visits: Int,
    /** The province (region) whose passport page got the stamp. */
    val province: String?,
    val provinceStamped: Int,
    val provinceTotal: Int
)

/** A question stop along the current journey; [state] is LOCKED, OPEN or ANSWERED. */
data class QuizStop(
    val key: String,
    val number: Int,
    val fraction: Double,
    val state: String,
    /** Journey steps at which a LOCKED stop opens. */
    val unlockSteps: Int
) {
    val isOpen get() = state == "OPEN"
    val isLocked get() = state == "LOCKED"
}

data class CurrentQuiz(val journeyId: Long?, val stops: List<QuizStop>)

/**
 * Streak and badges for the Quest tab. The engagement blocks still draw from the server's JSON;
 * typed models come with the engagement screens' own ViewModels.
 */
data class EngagementSummary(val streak: JSONObject, val badges: JSONArray)

/**
 * Engagement data for other features (place page, recommendations, Explore, collection, quests):
 * check-in, "Không quan tâm", culture-question stops, mastery, streak summary and today's steps.
 */
interface EngagementRepository {
    suspend fun checkIn(placeId: Long, latitude: Double, longitude: Double, test: Boolean = false): CheckinResult
    /** Whether the user has a journal page for the place (there is one once they checked in there). */
    suspend fun hasJournal(placeId: Long): Boolean
    suspend fun dismiss(placeId: Long)
    suspend fun undoDismiss(placeId: Long)
    /** Places whose card is in its "Am hiểu" edition. */
    suspend fun masteredPlaces(): Set<Long>
    suspend fun currentQuiz(): CurrentQuiz
    suspend fun summary(): EngagementSummary
    /** Sends today's Health Connect steps (throttled; skipped without permission). */
    suspend fun reportToday(force: Boolean = false)
    /** The personal daily goal from the last server answer. */
    fun cachedDailyGoal(): Int
}
