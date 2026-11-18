package com.project.bcv.feature.discovery.presentation

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlacePhotoMarkerTest {
    @Test fun lockedPhotoAndUnlockedPhotoHaveDistinctBadgesAndDoNotModifySource() {
        val source = Bitmap.createBitmap(44, 44, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val locked = PlacePhotoMarker.render(source, false, 1f)
        val unlocked = PlacePhotoMarker.render(source, true, 1f)
        assertEquals(54, locked.width)
        assertEquals(62, locked.height)
        assertEquals(0xFF8E9A93.toInt(), locked.getPixel(27, 53))
        assertEquals(0xFF07836D.toInt(), unlocked.getPixel(27, 53))
        val grey = locked.getPixel(27, 26)
        assertEquals(Color.red(grey), Color.green(grey))
        assertEquals(Color.green(grey), Color.blue(grey))
        assertEquals("Discovered photo keeps its original colour", Color.BLUE, unlocked.getPixel(27, 26))
        assertFalse(source.isRecycled)
        assertEquals(Color.BLUE, source.getPixel(22, 22))
        locked.recycle(); unlocked.recycle(); source.recycle()
    }

    @Test fun tinyCachedImageSurvivesLockedMarkerRendering() {
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val marker = PlacePhotoMarker.render(source, false, 1f)
        assertFalse(source.isRecycled)
        marker.recycle(); source.recycle()
    }
}
