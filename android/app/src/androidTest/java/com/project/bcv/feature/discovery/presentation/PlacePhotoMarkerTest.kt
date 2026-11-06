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
        assertEquals(0xFFB54B43.toInt(), locked.getPixel(22, 1))
        assertEquals(0xFFEE8F3A.toInt(), unlocked.getPixel(22, 1))
        assertNotEquals("The lock must cover the source preview", locked.getPixel(22, 22), unlocked.getPixel(22, 22))
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
