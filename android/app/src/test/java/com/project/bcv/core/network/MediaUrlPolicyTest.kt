package com.project.bcv.core.network

import org.junit.Assert.*
import org.junit.Test

class MediaUrlPolicyTest {
    private val base = "https://api.example/"

    @Test fun onlyOurMediaEndpointsReceiveAuthorization() {
        assertEquals("https://api.example/api/journal/photos/9/content", MediaUrlPolicy.authenticatedUrl(base, "/api/journal/photos/9/content"))
        assertNotNull(MediaUrlPolicy.authenticatedUrl(base, "/api/explore/places/12/videos/4"))
        assertNotNull(MediaUrlPolicy.authenticatedUrl(base, "/api/places/12/thumbnail"))
        assertNotNull(MediaUrlPolicy.authenticatedUrl(base, "/api/engagement/badges/3/image"))
        assertNotNull(MediaUrlPolicy.authenticatedUrl(base, "https://API.EXAMPLE:443/api/journal/photos/9/content"))
        assertNotNull(MediaUrlPolicy.authenticatedUrl("http://10.0.2.2:5081/", "/api/journal/photos/9/content"))
    }

    @Test fun untrustedUrlsNeverReceiveSessionTokens() {
        listOf("https://evil.example/api/journal/photos/9/content", "//evil.example/api/journal/photos/9/content",
            "http://api.example/api/journal/photos/9/content", "https://api.example:444/api/journal/photos/9/content",
            "https://user@api.example/api/journal/photos/9/content", "/api/admin/users", "/api/journal/photos/9/content?next=evil",
            "/api/journal/photos/9/content#other", "/api/journal/photos/%39/content", "/api/journal/photos/9/../9/content",
            "api/journal/photos/9/content", "/api/journal/photos/0/content", "file:///api/journal/photos/9/content",
            "data:image/png;base64,AAAA", "https://api.example.evil/api/journal/photos/9/content").forEach {
            assertNull("Must reject $it", MediaUrlPolicy.authenticatedUrl(base, it))
        }
    }
}
