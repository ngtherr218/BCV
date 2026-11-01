package com.project.bcv.core.network

import org.junit.Assert.*
import org.junit.Test

class BackendScopeTest {
    private fun file(url: String) = BackendScope.preferenceFile("bcv_session", url)
    @Test fun localSessionsAndProgressStayInTheirExistingFiles() {
        assertEquals("bcv_session", file("http://127.0.0.1:5081/"))
        assertEquals("bcv_session", file("http://10.0.2.2:5081/"))
    }
    @Test fun deployedSessionNeverReusesALocalToken() {
        assertNotEquals(file("http://127.0.0.1:5081/"), file("https://bcv-admin-test.onrender.com/"))
    }
    @Test fun differentServersCannotShareTheSameJourneyCounter() {
        assertNotEquals(BackendScope.preferenceFile("bcv_sensor_steps", "https://one.example/"),
            BackendScope.preferenceFile("bcv_sensor_steps", "https://two.example/"))
    }
    @Test fun equivalentEndpointSpellingsDoNotLogTheUserOut() {
        assertEquals(file("https://BCV-ADMIN-TEST.onrender.com:443/"), file("https://bcv-admin-test.onrender.com"))
    }
    @Test fun differentPortsAndApiPathsHaveIndependentSessions() {
        assertNotEquals(file("https://api.example:8443/"), file("https://api.example/"))
        assertNotEquals(file("https://api.example/a/"), file("https://api.example/b/"))
    }
}
