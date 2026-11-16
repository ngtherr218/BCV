package com.project.bcv.core.ui

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class PublicImageDiskCacheTest {
    @Test fun publicBytesSurviveANewCacheInstance() {
        val dir=Files.createTempDirectory("bcv-images").toFile()
        try {
            val url="https://images.example.test/photo.jpg"
            val bytes=byteArrayOf(1,2,3,4)
            PublicImageDiskCache(dir).write(url,bytes)
            assertArrayEquals(bytes,PublicImageDiskCache(dir).read(url))
            assertNull(PublicImageDiskCache(dir).read("https://images.example.test/other.jpg"))
        } finally { dir.deleteRecursively() }
    }
    @Test fun expiredAndEmptyFilesAreNotReused() {
        val dir=Files.createTempDirectory("bcv-images").toFile()
        try {
            val cache=PublicImageDiskCache(dir)
            val url="https://images.example.test/photo.jpg"
            cache.write(url,byteArrayOf(1))
            assertTrue(dir.listFiles()!!.single().setLastModified(System.currentTimeMillis()-25*60*60*1000L))
            assertNull(cache.read(url))
            cache.write(url,byteArrayOf())
            assertNull(cache.read(url))
        } finally { dir.deleteRecursively() }
    }
}
