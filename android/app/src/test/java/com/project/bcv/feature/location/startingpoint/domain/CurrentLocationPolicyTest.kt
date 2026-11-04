package com.project.bcv.feature.location.startingpoint.domain

import org.junit.Assert.*
import org.junit.Test

class CurrentLocationPolicyTest {
    private fun fix(at: Long = 90_000L, accuracy: Float? = 10f, latitude: Double = 21.028889) =
        LocationFix(Coordinates(latitude, 105.8525), at, accuracy)

    @Test fun oldAccurateCacheDoesNotBecomeCurrentLocation() {
        assertNull(CurrentLocationPolicy.result(emptyList(), listOf(fix(at = 1_000L)), 100_000L))
    }

    @Test fun cacheExpiresWhileWaitingForProviders() {
        val cached = listOf(fix(at = 80_000L))
        assertNotNull(CurrentLocationPolicy.result(emptyList(), cached, 100_000L))
        assertNull(CurrentLocationPolicy.result(emptyList(), cached, 120_000L))
    }

    @Test fun freshnessBoundaryAndFutureTimestampsAreChecked() {
        assertNotNull(CurrentLocationPolicy.best(listOf(fix(at = 70_000L)), 100_000L))
        assertNull(CurrentLocationPolicy.best(listOf(fix(at = 69_999L)), 100_000L))
        assertNull(CurrentLocationPolicy.best(listOf(fix(at = 100_001L)), 100_000L))
        assertNull(CurrentLocationPolicy.best(listOf(fix(at = 0L)), 10_000L))
    }

    @Test fun currentFixTakesPriorityOverMoreAccurateCache() {
        val current = fix(at = 100_000L, accuracy = 500f, latitude = 21.03)
        assertEquals(current, CurrentLocationPolicy.result(listOf(current), listOf(fix()), 100_000L))
    }

    @Test fun expiredCurrentFixCannotHideFreshCoarseFixAtTimeout() {
        val coarse = fix(at = 110_000L, accuracy = 2_000f)
        assertEquals(coarse, CurrentLocationPolicy.result(
            listOf(fix(at = 80_000L), coarse), listOf(fix(at = 115_000L)), 120_000L
        ))
    }

    @Test fun bestValidProviderFixWinsAndNewerBreaksAccuracyTie() {
        val best = fix(at = 99_000L, accuracy = 10f)
        assertEquals(best, CurrentLocationPolicy.best(
            listOf(fix(accuracy = 2_000f), fix(at = 95_000L, accuracy = 10f), best), 100_000L
        ))
    }

    @Test fun approximateAccessRemainsUsableWithoutBeingPrecise() {
        val coarse = fix(accuracy = 3_000f)
        assertEquals(coarse, CurrentLocationPolicy.best(listOf(coarse), 100_000L))
        assertFalse(CurrentLocationPolicy.precise(coarse))
        assertFalse(CurrentLocationPolicy.precise(fix(accuracy = null)))
        assertTrue(CurrentLocationPolicy.precise(fix(accuracy = 50f)))
    }

    @Test fun invalidProviderCoordinatesOrAccuracyAreRejected() {
        val invalid = listOf(fix(latitude = Double.NaN), fix(latitude = 91.0),
            fix().copy(coordinates = Coordinates(21.0, 181.0)), fix(accuracy = -1f), fix(accuracy = Float.NaN))
        assertNull(CurrentLocationPolicy.best(invalid, 100_000L))
    }

    @Test fun validCacheIsOnlyFallbackWhenCurrentResponsesAreUnusable() {
        val cached = fix()
        assertEquals(cached, CurrentLocationPolicy.result(
            listOf(fix(at = 1_000L)), listOf(cached), 100_000L
        ))
    }
}
