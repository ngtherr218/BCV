package com.project.bcv.feature.discovery.domain

import org.junit.Assert.*
import org.junit.Test

class PhotoLoadFailuresTest {
    @Test fun aDeniedThumbnailIsRetriedWhenThePlaceIsUnlockedWithoutChangingItsUrl() {
        val failures = PhotoLoadFailures()
        val url = "/api/community-media/4/content"
        failures.add(12, url, false)
        assertTrue(failures.contains(12, url, false))
        assertFalse(failures.contains(12, url, true))
        failures.add(12, url, true)
        assertTrue(failures.contains(12, url, true))
        assertFalse(failures.contains(13, url, true))
        failures.clear()
        assertFalse(failures.contains(12, url, true))
    }
}
