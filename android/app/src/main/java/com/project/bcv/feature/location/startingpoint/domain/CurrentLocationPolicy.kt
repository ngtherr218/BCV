package com.project.bcv.feature.location.startingpoint.domain

internal data class LocationFix(
    val coordinates: Coordinates,
    val elapsedRealtimeMillis: Long,
    val accuracyMeters: Float?
)

/** Evaluate freshness again at delivery, because a cached fix can expire while providers run. */
internal object CurrentLocationPolicy {
    const val MAX_AGE_MILLIS = 30_000L
    private const val PRECISE_METERS = 50f

    fun best(fixes: Iterable<LocationFix>, nowMillis: Long): LocationFix? = fixes
        .filter { usable(it, nowMillis) }
        .minWithOrNull(compareBy<LocationFix> { it.accuracyMeters ?: Float.POSITIVE_INFINITY }
            .thenByDescending { it.elapsedRealtimeMillis })

    fun result(current: Iterable<LocationFix>, cached: Iterable<LocationFix>, nowMillis: Long): LocationFix? =
        best(current, nowMillis) ?: best(cached, nowMillis)

    fun precise(fix: LocationFix): Boolean = fix.accuracyMeters?.let { it <= PRECISE_METERS } == true

    private fun usable(fix: LocationFix, nowMillis: Long): Boolean =
        fix.elapsedRealtimeMillis > 0 && nowMillis >= fix.elapsedRealtimeMillis &&
            nowMillis - fix.elapsedRealtimeMillis <= MAX_AGE_MILLIS &&
            fix.coordinates.latitude.isFinite() && fix.coordinates.latitude in -90.0..90.0 &&
            fix.coordinates.longitude.isFinite() && fix.coordinates.longitude in -180.0..180.0 &&
            (fix.accuracyMeters == null || (fix.accuracyMeters.isFinite() && fix.accuracyMeters >= 0))
}
