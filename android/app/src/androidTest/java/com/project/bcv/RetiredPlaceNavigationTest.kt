package com.project.bcv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.collection.data.CardInbox
import com.project.bcv.feature.collection.data.CollectionApi
import com.project.bcv.feature.collection.data.CollectionRepositoryImpl
import com.project.bcv.feature.collection.domain.NewCollectionCard
import com.project.bcv.feature.collection.presentation.CardDetailActivity
import com.project.bcv.feature.collection.presentation.CardIntents
import com.project.bcv.feature.discovery.domain.ExplorePlaceDetail
import com.project.bcv.feature.discovery.domain.ExploreRepository
import com.project.bcv.feature.discovery.domain.ExploreSnapshot
import com.project.bcv.feature.discovery.domain.Recommendations
import com.project.bcv.feature.discovery.presentation.PlaceDetailState
import com.project.bcv.feature.discovery.presentation.PlaceDetailActivity
import com.project.bcv.feature.discovery.presentation.PlaceDetailViewModel
import com.project.bcv.testhost.ReceiptHostActivity
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** No live requests, rewards or account data are changed by these fixtures. */
@RunWith(AndroidJUnit4::class)
class RetiredPlaceNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun state(model: PlaceDetailViewModel): PlaceDetailState {
        lateinit var result: PlaceDetailState
        main { result = checkNotNull(model.state.value) }
        return result
    }

    private fun loaded(model: PlaceDetailViewModel): PlaceDetailState {
        val deadline = System.currentTimeMillis() + 8_000
        while (state(model).loading && System.currentTimeMillis() < deadline) Thread.sleep(20)
        return state(model).also { assertFalse("Place request did not finish", it.loading) }
    }

    @Test fun missingOrRetiredPlaceEndsWithUnavailableStateInsteadOfNetworkRetry() {
        for (code in listOf("PLACE_NOT_FOUND", "PLACE_UNAVAILABLE", "NOT_FOUND")) {
            scenario(ApiFailure(code, "Old server message")) { model, _ ->
                val result = loaded(model)
                assertTrue("$code must be handled as unavailable", result.unavailable)
                assertNull(result.place)
                assertTrue(result.error.orEmpty().contains("không còn khả dụng"))
            }
        }
    }

    @Test fun networkAndAccountFailuresRemainRetryableAndSuccessfulReloadClearsTheError() {
        for (code in listOf("TIMEOUT", "SERVER_ERROR", "UNAUTHORIZED", "NO_STARTING_POINT")) {
            scenario(ApiFailure(code, "Retain this failure reason")) { model, repository ->
                assertFalse("$code is not evidence of a deleted place", loaded(model).unavailable)
                assertEquals("Retain this failure reason", state(model).error)
                repository.failure = null
                main { model.load(11) }
                val recovered = loaded(model)
                assertNotNull(recovered.place)
                assertFalse(recovered.unavailable)
                assertNull(recovered.error)
            }
        }
    }

    @Test fun collectionKeepsOwnedRetiredCardsAndPassesTheirAvailabilityToTheDetailScreen() {
        val context = instrumentation.targetContext
        val scope = "retired-place-${UUID.randomUUID()}"
        val session = SessionPreferences(context, "$scope-session").apply { token = "fixture" }
        val inbox = CardInbox(context, session, "$scope-inbox")
        val api = object : CollectionApi {
            override fun get(path: String, token: String) = JSONObject("""
                {"collectionId":1,"name":"Fixture","ownedCards":3,"totalCards":3,"cards":[
                  {"cardId":1,"placeId":11,"name":"Legacy","owned":true},
                  {"cardId":2,"placeId":12,"name":"Published","owned":true,"placeAvailable":true},
                  {"cardId":3,"placeId":13,"name":"Retired","owned":true,"placeAvailable":false}
                ]}
            """.trimIndent())
            override fun post(path: String, body: JSONObject, token: String) = JSONObject()
        }
        try {
            val detail = CollectionRepositoryImpl(api, session, inbox).loadDetail(1)
            assertEquals(3, detail.cards.size)
            assertEquals(3, detail.ownedCards)
            assertTrue("Old servers must remain compatible", detail.cards[0].placeAvailable)
            assertTrue(detail.cards[1].placeAvailable)
            val retired = detail.cards[2]
            assertTrue("Retirement must not revoke an owned card", retired.owned)
            assertFalse(retired.placeAvailable)
            val intent = CardIntents.detail(context, NewCollectionCard(retired, 1, "Fixture"))
            assertEquals(13L, intent.getLongExtra(CardDetailActivity.EXTRA_PLACE_ID, 0))
            assertFalse(intent.getBooleanExtra(CardDetailActivity.EXTRA_PLACE_AVAILABLE, true))
            assertTrue(intent.getBooleanExtra(CardDetailActivity.EXTRA_OWNED, false))
        } finally {
            context.getSharedPreferences("$scope-session", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("$scope-inbox", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun ownedRetiredCardStaysVisibleButOnlyPublishedCardCanOpenPlace() {
        val context = instrumentation.targetContext
        val placeLaunch = instrumentation.addMonitor(PlaceDetailActivity::class.java.name, null, true)
        try {
            // Vivo blocks background starts from the instrumented process. Bring up the
            // exported, network-free debug host first, then launch from its foreground UI.
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n com.project.bcv/.testhost.ReceiptHostActivity"
            )).use { it.readBytes() }
            val host = awaitFocusedActivity(ReceiptHostActivity::class.java)
            for (available in listOf(false, true)) {
                // Positive, unused IDs; isNew=false and no image prevent mark-seen and media
                // requests. This fixture checks the real Activity without touching live data.
                val intent = Intent(context, CardDetailActivity::class.java)
                    .putExtra(CardDetailActivity.EXTRA_CARD_ID, 2_147_483_001L)
                    .putExtra(CardDetailActivity.EXTRA_PLACE_ID, 2_147_483_001L)
                    .putExtra(CardDetailActivity.EXTRA_CARD_NAME, "Thẻ địa danh kiểm thử")
                    .putExtra(CardDetailActivity.EXTRA_COLLECTION_NAME, "Bộ sưu tập kiểm thử")
                    .putExtra(CardDetailActivity.EXTRA_OWNED, true)
                    .putExtra(CardDetailActivity.EXTRA_IS_NEW, false)
                    .putExtra(CardDetailActivity.EXTRA_PLACE_AVAILABLE, available)
                main { host.startActivity(intent) }
                val activity = awaitFocusedActivity(CardDetailActivity::class.java)
                try {
                    instrumentation.waitForIdleSync()
                    main {
                        val name = activity.findViewById<TextView>(R.id.cardName)
                        val ownership = activity.findViewById<TextView>(R.id.ownedChip)
                        val note = activity.findViewById<TextView>(R.id.ownedNote)
                        val button = activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.placeButton)
                        assertTrue("The owned card must stay visible", name.isShown)
                        assertEquals("Thẻ địa danh kiểm thử", name.text.toString())
                        assertEquals("Đã sở hữu", ownership.text.toString())
                        assertEquals(View.GONE, activity.findViewById<View>(R.id.cardLock).visibility)
                        assertEquals("Only a published destination can be opened", available, button.isEnabled)
                        if (available) {
                            assertEquals("Xem nội dung địa danh", button.text.toString())
                            assertTrue(note.text.toString().contains("Thẻ đã có trong bộ sưu tập"))
                        } else {
                            assertTrue(note.text.toString().contains("không còn khả dụng"))
                            assertTrue(note.text.toString().contains("Thẻ đã sở hữu vẫn được giữ"))
                            assertEquals("Địa danh không còn khả dụng", button.text.toString())
                            // Even a programmatic click must not reopen a retired destination.
                            button.performClick()
                            assertEquals(0, placeLaunch.hits)
                        }
                    }
                } finally {
                    main { activity.finish() }
                    awaitFocusedActivity(ReceiptHostActivity::class.java)
                }
            }
        } finally {
            instrumentation.removeMonitor(placeLaunch)
            // Also covers a start that failed to gain focus before the bounded wait ended.
            main {
                listOf(Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
                    .flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                    .distinct().filter { activity ->
                        activity is ReceiptHostActivity || (activity is CardDetailActivity &&
                            activity.intent.getLongExtra(CardDetailActivity.EXTRA_CARD_ID, 0L) == 2_147_483_001L)
                    }.forEach { if (!it.isFinishing && !it.isDestroyed) it.finish() }
            }
        }
    }

    private fun <T : Activity> awaitFocusedActivity(type: Class<T>): T {
        val deadline = SystemClock.uptimeMillis() + 10_000
        do {
            var focused: T? = null
            main {
                focused = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { type.isInstance(it) && !it.isFinishing && !it.isDestroyed && it.hasWindowFocus() }
                    ?.let(type::cast)
            }
            focused?.let { return it }
            Thread.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("${type.simpleName} did not gain window focus within 10 seconds")
    }

    private fun scenario(failure: ApiFailure, action: (PlaceDetailViewModel, FakeRepository) -> Unit) {
        val repository = FakeRepository(failure)
        val store = ViewModelStore()
        lateinit var model: PlaceDetailViewModel
        main {
            model = PlaceDetailViewModel(repository)
            store.put("retired-place", model)
            model.load(11)
        }
        try { action(model, repository) }
        finally { main { store.clear() } }
    }

    private class FakeRepository(@Volatile var failure: ApiFailure?) : ExploreRepository {
        override fun loadPlace(placeId: Long): ExplorePlaceDetail {
            failure?.let { throw it }
            return ExplorePlaceDetail(placeId, "Published place", "Type", null, true, 1.0, 1_000, null, null)
        }
        override fun load(): ExploreSnapshot = error("Not used")
        override fun loadRecommendations(): Recommendations = error("Not used")
        override fun createJourney(placeId: Long, replaceActive: Boolean): ExploreSnapshot = error("Not used")
        override fun syncSteps(journeyId: Long, cumulativeSteps: Long): ExploreSnapshot = error("Not used")
    }
}
