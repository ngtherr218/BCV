package com.project.bcv

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.core.ui.ActiveItemBanner
import com.project.bcv.feature.discovery.domain.ActiveItemEffect
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveItemBannerTest {
    @Test fun showsConfirmedEffectAndHidesForExpiredOrCompletedJourney() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val banner = ActiveItemBanner(instrumentation.targetContext)
            val journey = ExploreJourney(1, "Điểm đến", Coordinates(21.0, 105.0), 10000, 600,
                "IN_PROGRESS", 0, activeItem = ActiveItemEffect("SPEED_SHOES", "Giày tăng tốc", 2.0, 5000, 4800))
            banner.show(journey)
            assertEquals(View.VISIBLE, banner.visibility)
            assertTrue(banner.contentDescription.contains("Giày tăng tốc ×2"))
            assertTrue(banner.contentDescription.contains("4.800 bước"))
            assertTrue(banner.isClickable)
            banner.show(journey.copy(activeItem = journey.activeItem!!.copy(remainingSteps = 0)))
            assertEquals(View.GONE, banner.visibility)
            banner.show(journey.copy(status = "COMPLETED"))
            assertEquals(View.GONE, banner.visibility)
            banner.show(null)
            assertEquals(View.GONE, banner.visibility)
        }
    }
}
