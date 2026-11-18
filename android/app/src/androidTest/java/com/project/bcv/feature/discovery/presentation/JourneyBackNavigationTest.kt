package com.project.bcv.feature.discovery.presentation

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.bcv.R
import com.project.bcv.feature.journey.presentation.JourneyActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JourneyBackNavigationTest {
    @Test fun visibleBackButtonClosesJourneyDetails() {
        ActivityScenario.launch(JourneyActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val back=activity.findViewById<android.view.View>(R.id.backButton)
                assertTrue("Journey details need a visible back action",back.isShown)
                back.performClick()
            }
            val deadline=System.currentTimeMillis()+5000
            while(scenario.state!=Lifecycle.State.DESTROYED && System.currentTimeMillis()<deadline)Thread.sleep(25)
            assertEquals(Lifecycle.State.DESTROYED,scenario.state)
        }
    }
}
