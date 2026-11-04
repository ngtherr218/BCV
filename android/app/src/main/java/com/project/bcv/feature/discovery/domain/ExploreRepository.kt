package com.project.bcv.feature.discovery.domain

import com.project.bcv.feature.location.startingpoint.domain.Coordinates

data class ExploreLocation(val regionName: String, val placeName: String?, val coordinates: Coordinates)

data class ExploreJourney(
    val id: Long,
    val destinationName: String,
    val destination: Coordinates,
    val requiredSteps: Int,
    val recordedSteps: Int,
    val status: String,
    val startedAtEpochSeconds: Long,
    /** Where the journey began; the virtual location moves to the destination once it is completed. */
    val start: Coordinates? = null,
    /** The destination PLACE, opened from the journey screen once it is unlocked. */
    val destinationPlaceId: Long? = null,
    /** Unboosted, cumulative server total; null means the server needs updating. */
    val sourceSteps: Long? = null,
    val activeItem: ActiveItemEffect? = null,
    val startPlaceName: String? = null,
    /** Only provided by the server after the card is confirmed as owned. */
    val awardedCard: JourneyCardReward? = null
) {
    val startLabel: String
        get() = startPlaceName?.takeIf(String::isNotBlank)
            ?: start?.let { String.format(java.util.Locale.US, "Vị trí xuất phát (%.5f, %.5f)", it.latitude, it.longitude) }
            ?: "Chưa có thông tin điểm xuất phát"

    val progressPercent: Int
        get() = if (requiredSteps <= 0) 0 else (recordedSteps.toLong() * 100 / requiredSteps).toInt().coerceIn(0, 100)
}

data class JourneyCardReward(val id: Long, val name: String)

data class ExploreMapPlace(val id: Long, val name: String, val coordinates: Coordinates, val isDiscovered: Boolean)

data class ActiveItemEffect(val itemType: String, val name: String, val multiplier: Double,
    val range: Int, val remainingSteps: Int)

/** [imageUrls] is filled only once the place is discovered; [imageUrl] is the locked (blurred) preview. */
data class ExplorePlaceDetail(val id: Long, val name: String, val placeType: String,
    val imageUrl: String?, val isDiscovered: Boolean, val distanceKm: Double,
    val requiredSteps: Int, val introduction: String?, val highlights: String?,
    val regionName: String? = null, val imageUrls: List<String> = emptyList(),
    val rating: Double? = null, val ratingCount: Int? = null)

data class ExploreSnapshot(val location: ExploreLocation?, val journey: ExploreJourney?,
    val places: List<ExploreMapPlace>)

/** One of the (at most three) destinations ranked by the server's P/C/A/R/N score. */
data class RecommendedPlace(val rank: Int, val id: Long, val name: String, val regionName: String,
    val imageUrl: String?, val isDiscovered: Boolean, val distanceKm: Double, val requiredSteps: Int,
    val matchedPreferences: List<String>, val reasons: List<String> = emptyList())

/** [location] is the virtual location, looked up to the same street address the Explore map shows. */
data class Recommendations(val regionName: String, val location: Coordinates, val places: List<RecommendedPlace>)

interface ExploreRepository {
    fun load(): ExploreSnapshot
    fun loadPlace(placeId: Long): ExplorePlaceDetail
    fun loadRecommendations(): Recommendations
    fun createJourney(placeId: Long, replaceActive: Boolean): ExploreSnapshot
    fun syncSteps(journeyId: Long, cumulativeSteps: Long): ExploreSnapshot
}

/** Health Connect first; Recording API and direct sensors provide fallbacks. */
interface StepSource {
    /** Steps can be read: Health Connect access, or the Recording API allowed as fallback. */
    suspend fun hasPermission(): Boolean
    /** No app records steps in Health Connect and the Recording API still needs physical-activity access. */
    suspend fun needsActivityPermission(): Boolean
    /** Starts the journey's background hardware shadow; journeyId=0 stops tracking after confirmed completion. */
    suspend fun capture(journeyId: Long, startedAtEpochSeconds: Long = 0, destinationName: String = "")
    /** Steps since the journey started, cumulative, from the journey's source. */
    suspend fun totalStepsSince(journeyId: Long, startedAtEpochSeconds: Long): Long
    /** Release listeners owned by this screen; Recording API subscriptions can continue in background. */
    fun release() = Unit
}
