package com.project.bcv.core.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class MediaImageDecodeTest {
    @Test fun uploadedDataUriImagesDecodeWithoutAnHttpRequest() {
        val original = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        for ((mime, format) in listOf("image/jpeg" to Bitmap.CompressFormat.JPEG, "image/png" to Bitmap.CompressFormat.PNG)) {
            val bytes = ByteArrayOutputStream().apply { original.compress(format, 90, this) }.toByteArray()
            val image = RemoteImages.decodeDataImage("data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP), 1)
            assertNotNull(image)
            assertEquals(32, image!!.width)
            assertEquals(24, image.height)
            image.recycle()
        }
        original.recycle()
    }

    @Test fun malformedAndNonImageDataUrisAreRejected() {
        assertNull(RemoteImages.decodeDataImage("data:text/html;base64,PGh0bWw+", 1))
        assertNull(RemoteImages.decodeDataImage("data:image/png;base64,not-an-image", 1))
        assertNull(RemoteImages.decodeDataImage("data:image/svg+xml;base64,AAAA", 1))
    }
}
