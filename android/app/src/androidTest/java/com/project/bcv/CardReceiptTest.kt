package com.project.bcv

import com.project.bcv.core.network.ApiEnvironment

import android.content.Context
import android.os.SystemClock
import android.os.ParcelFileDescriptor
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.project.bcv.core.ui.JourneyCompletionDialog
import com.project.bcv.feature.collection.presentation.CollectionActivity
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.discovery.domain.JourneyCardReward
import com.project.bcv.testhost.ReceiptHostActivity
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** UI-only fixture: no journey, step or card ownership is changed on the server. */
@RunWith(AndroidJUnit4::class)
class CardReceiptTest {
    @Test fun receiptRequiresConfirmationOpensCollectionAndIsNotRepeated() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("bcv_card_receipts", Context.MODE_PRIVATE)
        val journey = ExploreJourney(-2147483001, "Địa danh kiểm thử giao diện", Coordinates(21.0, 105.0),
            100, 100, "COMPLETED", 1, destinationPlaceId = -2147483001,
            awardedCard = JourneyCardReward(-2147483001, "Thẻ kiểm thử giao diện"))
        val key = "${ApiEnvironment.currentUrl}:${journey.id}:${journey.awardedCard!!.id}"
        preferences.edit().remove(key).commit()
        // Some OEMs block activity starts from an instrumented process in the background.
        // The debug-only exported host can be started through the instrumentation shell instead.
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "am start -n com.project.bcv/.testhost.ReceiptHostActivity"
        )).use { it.readBytes() }
        var resumedHost: ReceiptHostActivity? = null
        for (attempt in 0 until 100) {
            instrumentation.runOnMainSync {
                resumedHost = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<ReceiptHostActivity>().firstOrNull()
            }
            if (resumedHost != null) break
            SystemClock.sleep(100)
        }
        val activity = requireNotNull(resumedHost) { "The debug receipt host did not resume" }
        try {
            instrumentation.runOnMainSync {
                JourneyCompletionDialog.showIfNeeded(activity, journey.copy(status = "IN_PROGRESS"))
                JourneyCompletionDialog.showIfNeeded(activity, journey.copy(awardedCard = null))
            }
            onView(withText("Bạn đã nhận được thẻ mới!")).check(doesNotExist())
            instrumentation.runOnMainSync { JourneyCompletionDialog.showIfNeeded(activity, journey) }
            onView(withText("Bạn đã nhận được thẻ mới!")).check(matches(isDisplayed()))
            onView(withText("Xem Bộ sưu tập")).perform(click())
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val collection = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<CollectionActivity>().firstOrNull()
                assertTrue("Receipt button opens Collection", collection != null)
                collection!!.finish()
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync { JourneyCompletionDialog.showIfNeeded(activity, journey) }
            onView(withText("Bạn đã nhận được thẻ mới!")).check(doesNotExist())
        } finally {
            preferences.edit().remove(key).commit()
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
