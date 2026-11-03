package com.project.bcv

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class SessionRoutingTest {
    @Test fun homeRoutingRequiresSameSavedSessionAndClearsOnLogout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "session-routing-${UUID.randomUUID()}"
        try {
            val session = SessionPreferences(context, name)
            session.readyForHome = true
            assertFalse(session.readyForHome)
            session.token = "first-fixture"
            assertFalse(session.readyForHome)
            session.readyForHome = true
            assertTrue(SessionPreferences(context, name).readyForHome)
            session.token = "second-fixture"
            assertFalse(session.readyForHome)
            session.readyForHome = true
            session.token = null
            assertFalse(session.readyForHome)
        } finally {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
