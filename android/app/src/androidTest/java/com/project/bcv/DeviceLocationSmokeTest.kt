package com.project.bcv

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import com.project.bcv.core.di.StartingPointModule
import com.project.bcv.feature.discovery.presentation.ExploreActivity
import com.project.bcv.feature.location.startingpoint.data.AndroidDeviceLocationSource
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in check on an emulator with GPS configured and an existing signed-in test session. */
class DeviceLocationSmokeTest {
    @Test fun currentGpsMatchesConfiguredLocationAndResolvesOnBackend() {
        val arguments = InstrumentationRegistry.getArguments()
        val latitude = arguments.getString("expectedLatitude")?.toDoubleOrNull()
        val longitude = arguments.getString("expectedLongitude")?.toDoubleOrNull()
        assumeTrue("Pass expectedLatitude/expectedLongitude to run the device GPS check",
            latitude != null && longitude != null)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = AndroidDeviceLocationSource(instrumentation.targetContext)
        val received = CountDownLatch(1)
        var coordinates: Coordinates? = null
        ActivityScenario.launch(ExploreActivity::class.java).use {
          try {
            instrumentation.runOnMainSync {
                source.findCurrent { coordinates = it; received.countDown() }
            }
            assertTrue("GPS request did not finish", received.await(25, TimeUnit.SECONDS))
            val fix = requireNotNull(coordinates) { "No fresh GPS fix received" }
            assertEquals(latitude!!, fix.latitude, 0.0002)
            assertEquals(longitude!!, fix.longitude, 0.0002)
            // Resolve is read-only: never replace this account's saved starting point or journey.
            val point = StartingPointModule.repository(instrumentation.targetContext).resolveCurrent(fix)
            assertTrue("Backend did not resolve a supported region", point.regionName.isNotBlank())
          } finally {
            instrumentation.runOnMainSync { source.cancel() }
          }
        }
    }
}
