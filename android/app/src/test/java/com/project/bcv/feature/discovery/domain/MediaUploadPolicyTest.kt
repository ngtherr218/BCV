package com.project.bcv.feature.discovery.domain
import org.junit.Assert.*
import org.junit.Test
class MediaUploadPolicyTest {
    @Test fun supportedPhotosKeepTheirLimit() {
        for (mime in MediaUploadPolicy.acceptedMimeTypes) assertEquals(10*1024*1024, MediaUploadPolicy.maximumBytes(mime))
    }
    @Test fun videosAndUnsupportedFormatsAreRejected() {
        for (mime in listOf("video/mp4","video/webm","image/gif")) assertTrue(runCatching { MediaUploadPolicy.maximumBytes(mime) }.isFailure)
    }
}
