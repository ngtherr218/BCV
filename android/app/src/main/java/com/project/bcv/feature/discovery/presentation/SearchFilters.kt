package com.project.bcv.feature.discovery.presentation

import com.project.bcv.feature.discovery.domain.ExploreMapPlace
import kotlin.math.ceil

/**
 * F72: map filters next to the text search. Duration is estimated with the user's own daily goal,
 * so "≈1 ngày" means about one day of walking for this person, not for an average walker.
 */
data class SearchFilters(val distance: Int = 0, val duration: Int = 0, val status: Int = 0) {
    val active get() = distance != 0 || duration != 0 || status != 0

    fun matches(place: ExploreMapPlace, km: Double?, dailyGoal: Int): Boolean {
        if (status == 1 && place.isDiscovered) return false
        if (status == 2 && !place.isDiscovered) return false
        if (km == null) return distance == 0 && duration == 0
        when (distance) {
            1 -> if (km >= 5) return false
            2 -> if (km < 5 || km > 20) return false
            3 -> if (km <= 20) return false
        }
        val days = days(km, dailyGoal)
        return when (duration) {
            1 -> days <= 1
            2 -> days in 2..3
            3 -> days >= 4
            else -> true
        }
    }

    fun labels(): List<String> = listOfNotNull(
        DISTANCE.getOrNull(distance - 1), DURATION.getOrNull(duration - 1), STATUS.getOrNull(status - 1))

    companion object {
        val DISTANCE = listOf("Dưới 5 km", "5–20 km", "Trên 20 km")
        val DURATION = listOf("≈1 ngày đi", "2–3 ngày đi", "4+ ngày đi")
        val STATUS = listOf("Chưa khám phá", "Đã khám phá")

        /** Same conversion as the server: 0.75 m per step. */
        fun days(km: Double, dailyGoal: Int): Int =
            ceil(km * 1000 / 0.75 / dailyGoal.coerceAtLeast(1)).toInt().coerceAtLeast(1)
    }
}
