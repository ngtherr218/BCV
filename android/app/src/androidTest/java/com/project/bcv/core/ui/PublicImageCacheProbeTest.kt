package com.project.bcv.core.ui

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class PublicImageCacheProbeTest {
    @Test fun publicImageReusesOriginalBytesAcrossSizes() = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val url=InstrumentationRegistry.getArguments().getString("imageProbeUrl")
        assumeTrue("Explicit public HTTPS probe required",url?.startsWith("https://")==true)
        val context=instrumentation.targetContext
        RemoteImages.initialize(context)
        val start=SystemClock.elapsedRealtime()
        val thumb=RemoteImages.load(url!!,4)
        val cold=SystemClock.elapsedRealtime()-start
        assertNotNull("Public image must load",thumb)
        val startDetail=SystemClock.elapsedRealtime()
        assertNotNull(RemoteImages.load(url,2))
        val detail=SystemClock.elapsedRealtime()-startDetail
        val startMemory=SystemClock.elapsedRealtime()
        assertSame(thumb,RemoteImages.load(url,4))
        val memory=SystemClock.elapsedRealtime()-startMemory
        assertNotNull(PublicImageDiskCache(File(context.cacheDir,"public-images-v1")).read(url))
        File(context.getExternalFilesDir(null),"image-cache-probe.txt").writeText(
            "First load: ${cold}ms\nDifferent-size decode from shared public cache: ${detail}ms\nMemory hit: ${memory}ms\n")
    }
}
