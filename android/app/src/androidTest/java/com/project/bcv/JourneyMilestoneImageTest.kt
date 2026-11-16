package com.project.bcv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.journey.presentation.JourneyMilestoneImage
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JourneyMilestoneImageTest {
    @Test fun onlyConfirmedMilestonesRevealPhotoAndRepeatedUpdatesDoNotRevealMore() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = JourneyMilestoneImage(instrumentation.targetContext)
            view.layout(0, 0, 400, 400)
            val photo = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            view.setPhoto(photo)
            fun render(steps: Int): Bitmap {
                view.show(ExploreJourney(1, "Địa danh fixture", Coordinates(21.0, 105.0), 100, steps, "IN_PROGRESS", 0))
                return Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
            }
            assertNotEquals(Color.RED, render(24).getPixel(50, 50))
            for (steps in listOf(25, 25, 50, 75, 100)) {
                val result = render(steps)
                val samples = listOf(50 to 50, 250 to 50, 50 to 250, 250 to 250)
                samples.forEachIndexed { index, (x, y) ->
                    assertEquals(index < steps / 25, result.getPixel(x, y) == Color.RED)
                }
            }
            assertNotEquals(Color.RED, render(0).getPixel(50, 50))
            view.setPhoto(null)
            assertNotEquals(Color.RED, render(100).getPixel(50, 50))
        }
    }
}
