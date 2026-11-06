package com.project.bcv

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.os.ParcelFileDescriptor
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.project.bcv.core.ui.JourneyCompletionDialog
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.collection.presentation.CollectionActivity
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.discovery.domain.JourneyCardReward
import com.project.bcv.testhost.ReceiptHostActivity
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

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
        val key = "${BuildConfig.API_BASE_URL}:${journey.id}:${journey.awardedCard!!.id}"
        val inbox = context.getSharedPreferences("card_inbox", Context.MODE_PRIVATE)
        val account = MessageDigest.getInstance("SHA-256")
            .digest(SessionPreferences(context).token.orEmpty().toByteArray())
            .joinToString("") { "%02x".format(it) }
        fun clearFixtureReceipt() {
            preferences.edit().remove(key).commit()
            // CardInbox also persists dialog suppression and a pending place. Remove only this
            // fixture's negative IDs; real account rewards and other pending places stay intact.
            val placesKey = "$account:places"
            val places = inbox.getStringSet(placesKey, null)?.toMutableSet()
            val editor = inbox.edit()
                .remove("$account:dialog:${journey.id}")
                .remove("$account:journey:${journey.id}")
                .remove("$account:seen:${journey.awardedCard!!.id}")
                .remove("$account:card:${journey.awardedCard!!.id}")
            if (places?.remove(journey.destinationPlaceId.toString()) == true) {
                editor.putStringSet(placesKey, places)
            }
            editor.commit()
        }
        clearFixtureReceipt()
        fun <T : Activity> awaitFocusedActivity(type: Class<T>): T {
            val deadline = SystemClock.uptimeMillis() + 10_000
            do {
                var focused: T? = null
                instrumentation.runOnMainSync {
                    focused = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                        .filter { type.isInstance(it) && !it.isFinishing && !it.isDestroyed && it.hasWindowFocus() }
                        .firstOrNull()?.let(type::cast)
                }
                focused?.let { return it }
                SystemClock.sleep(50)
            } while (SystemClock.uptimeMillis() < deadline)
            throw AssertionError("${type.simpleName} did not become resumed with window focus")
        }
        // Some OEMs block activity starts from an instrumented process in the background.
        // The debug-only exported host can be started through the instrumentation shell instead.
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "am start -W -n com.project.bcv/.testhost.ReceiptHostActivity"
        )).use { it.readBytes() }
        val activity = awaitFocusedActivity(ReceiptHostActivity::class.java)
        // Verify the receipt's destination without launching a screen that restores a real session.
        // Blocking only Collection keeps this UI fixture independent of authentication and network.
        val collectionLaunch = instrumentation.addMonitor(CollectionActivity::class.java.name, null, true)
        try {
            instrumentation.runOnMainSync {
                JourneyCompletionDialog.showIfNeeded(activity, journey.copy(status = "IN_PROGRESS"))
                JourneyCompletionDialog.showIfNeeded(activity, journey.copy(awardedCard = null))
            }
            onView(withText("Bạn đã nhận được thẻ mới!")).check(doesNotExist())
            instrumentation.runOnMainSync { JourneyCompletionDialog.showIfNeeded(activity, journey) }
            instrumentation.waitForIdleSync()
            onView(withText("Bạn đã nhận được thẻ mới!")).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText("Xem Bộ sưu tập")).inRoot(isDialog()).perform(click())
            instrumentation.waitForIdleSync()
            assertEquals("Receipt button launches Collection exactly once", 1, collectionLaunch.hits)
            assertTrue("Receipt host regains focus after the dialog closes",
                awaitFocusedActivity(ReceiptHostActivity::class.java) === activity)
            instrumentation.runOnMainSync { JourneyCompletionDialog.showIfNeeded(activity, journey) }
            onView(withText("Bạn đã nhận được thẻ mới!")).check(doesNotExist())
        } finally {
            instrumentation.removeMonitor(collectionLaunch)
            clearFixtureReceipt()
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
