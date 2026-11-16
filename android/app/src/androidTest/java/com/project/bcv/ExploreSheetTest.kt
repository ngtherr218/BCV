package com.project.bcv

import android.content.Intent
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.feature.discovery.presentation.ExploreActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExploreSheetTest {
    @Test
    fun sheetFollowsDragAndSettlesInBothDirections() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val explore = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, ExploreActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as ExploreActivity
        instrumentation.waitForIdleSync()
        try {
            SystemClock.sleep(500)
            val sheet = explore.findViewById<View>(R.id.bottomSheet)
            val handle = explore.findViewById<View>(R.id.sheetHandleArea)
            assertEquals(0f, sheet.translationY, 1f)

            val down = handleCenter(handle)
            val start = SystemClock.uptimeMillis()
            pointer(handle, MotionEvent.ACTION_DOWN, down.first, down.second, start)
            pointer(handle, MotionEvent.ACTION_MOVE, down.first, down.second + 140f, start)
            SystemClock.sleep(60)
            assertTrue("Sheet should follow a downward drag", sheet.translationY > 20f)
            pointer(handle, MotionEvent.ACTION_UP, down.first, down.second + 300f, start)
            SystemClock.sleep(400)
            val collapsedOffset = sheet.translationY
            assertTrue("Sheet should leave a visible peek", collapsedOffset > 100f)

            val up = handleCenter(handle)
            val next = SystemClock.uptimeMillis()
            pointer(handle, MotionEvent.ACTION_DOWN, up.first, up.second, next)
            pointer(handle, MotionEvent.ACTION_MOVE, up.first, up.second - 140f, next)
            SystemClock.sleep(60)
            assertTrue("Sheet should follow an upward drag", sheet.translationY < collapsedOffset - 20f)
            pointer(handle, MotionEvent.ACTION_UP, up.first, up.second - 300f, next)
            SystemClock.sleep(400)
            assertEquals("Sheet should expand again", 0f, sheet.translationY, 2f)
        } finally {
            explore.finish()
        }
    }

    private fun handleCenter(handle: View): Pair<Float, Float> {
        return handle.width / 2f to handle.height / 2f
    }

    private fun pointer(view: View, action: Int, x: Float, y: Float, downTime: Long) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { view.dispatchTouchEvent(event) }
        event.recycle()
    }
}
