package com.project.bcv.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabRefreshTest {
    @Test fun returningToFreshContentDoesNotReload() {
        var now = 1_000L
        val refresh = TabRefresh { now }
        assertTrue(refresh.shouldRefresh(hasContent = false))
        refresh.loaded()

        assertFalse(refresh.shouldRefresh(hasContent = true))
        now += 59_999L
        assertFalse(refresh.shouldRefresh(hasContent = true))
    }

    @Test fun staleContentIsCheckedAgainAtTheTtlBoundary() {
        var now = 1_000L
        val refresh = TabRefresh { now }
        refresh.shouldRefresh(hasContent = false)
        refresh.loaded()

        now += 60_000L
        assertTrue(refresh.shouldRefresh(hasContent = true))
        refresh.loaded()
        assertFalse(refresh.shouldRefresh(hasContent = true))
    }

    @Test fun successfulMutationRefreshesOtherwiseFreshContent() {
        val refresh = TabRefresh { 1_000L }
        refresh.shouldRefresh(hasContent = false)
        refresh.loaded()
        assertFalse(refresh.shouldRefresh(hasContent = true))

        AppDataChanges.changed()
        assertTrue(refresh.shouldRefresh(hasContent = true))
        refresh.loaded()
        assertFalse(refresh.shouldRefresh(hasContent = true))
    }

    @Test fun responseDoesNotSwallowAMutationThatHappenedDuringTheFetch() {
        val refresh = TabRefresh { 1_000L }
        assertTrue(refresh.shouldRefresh(hasContent = false))

        // The response can still contain the snapshot from before this write.
        AppDataChanges.changed()
        refresh.loaded()

        assertTrue(refresh.shouldRefresh(hasContent = true))
        refresh.loaded()
        assertFalse(refresh.shouldRefresh(hasContent = true))
    }

    @Test fun explicitRetryAndInvalidationBypassFreshness() {
        val refresh = TabRefresh { 1_000L }
        refresh.shouldRefresh(hasContent = false)
        refresh.loaded()
        assertFalse(refresh.shouldRefresh(hasContent = true))

        assertTrue(refresh.shouldRefresh(hasContent = true, force = true))
        refresh.loaded()
        assertFalse(refresh.shouldRefresh(hasContent = true))

        refresh.invalidate()
        assertTrue(refresh.shouldRefresh(hasContent = true))
        refresh.loaded()
        assertFalse(refresh.shouldRefresh(hasContent = true))
    }

    @Test fun explicitInvalidationDuringFetchIsNotSwallowedByItsResponse() {
        val refresh = TabRefresh { 1_000L }
        assertTrue(refresh.shouldRefresh(hasContent = false))
        refresh.invalidate()
        refresh.loaded()

        assertTrue(refresh.shouldRefresh(hasContent = true))
        refresh.loaded()
        assertFalse(refresh.shouldRefresh(hasContent = true))
    }

    @Test fun missingContentIsLoadedEvenIfTheLastCheckWasRecent() {
        val refresh = TabRefresh { 1_000L }
        refresh.shouldRefresh(hasContent = false)
        refresh.loaded()

        assertTrue(refresh.shouldRefresh(hasContent = false))
    }
}
