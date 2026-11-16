package com.project.bcv

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.core.ui.AppDataChanges
import com.project.bcv.feature.discovery.data.StepTestPreferences
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.discovery.domain.ExplorePlaceDetail
import com.project.bcv.feature.discovery.domain.ExploreRepository
import com.project.bcv.feature.discovery.domain.ExploreSnapshot
import com.project.bcv.feature.discovery.domain.Recommendations
import com.project.bcv.feature.discovery.domain.StepSource
import com.project.bcv.feature.discovery.presentation.ExploreStage
import com.project.bcv.feature.discovery.presentation.ExploreUiState
import com.project.bcv.feature.discovery.presentation.ExploreViewModel
import com.project.bcv.feature.location.startingpoint.domain.AddressLookup
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ExploreRefreshTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun until(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 8_000L
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue("Timed out waiting for the refresh to finish", condition())
    }

    private fun state(model: ExploreViewModel): ExploreUiState {
        lateinit var result: ExploreUiState
        main { result = checkNotNull(model.state.value) }
        return result
    }

    private fun scenario(
        repo: FakeRepository = FakeRepository(),
        waitForInitialLoad: Boolean = true,
        action: (ExploreViewModel, FakeRepository, FakeSteps) -> Unit
    ) {
        val store = ViewModelStore()
        val steps = FakeSteps()
        lateinit var model: ExploreViewModel
        main {
            model = ExploreViewModel(repo, steps,
                object : AddressLookup { override fun describe(coordinates: Coordinates): String? = null },
                StepTestPreferences(instrumentation.targetContext))
            store.put("explore-refresh", model)
            model.load()
        }
        try {
            if (waitForInitialLoad) until {
                state(model).stage == ExploreStage.ACTIVE && steps.readCalls.get() >= 1
            }
            action(model, repo, steps)
        } finally {
            repo.releaseLoad.countDown()
            main { store.clear() }
        }
    }

    @Test fun returningRepeatedlyToFreshContentDoesNotFetchAgain() = scenario { model, repo, _ ->
        main { repeat(10) { model.refreshIfNeeded() } }
        instrumentation.waitForIdleSync()

        assertEquals(1, repo.loadCalls.get())
        assertEquals(ExploreStage.ACTIVE, state(model).stage)
        assertEquals(repo.snapshot, state(model).snapshot)
    }

    @Test fun mutationChecksTheServerQuietlyAndDoesNotRedrawIdenticalData() = scenario { model, repo, steps ->
        val observed = mutableListOf<ExploreUiState>()
        val observer = Observer<ExploreUiState> { observed.add(it) }
        main {
            model.state.observeForever(observer)
            observed.clear()
        }
        try {
            repo.blockedLoadNumber = 2
            AppDataChanges.changed()
            main { model.refreshIfNeeded() }
            assertTrue(repo.loadEntered.await(5, TimeUnit.SECONDS))
            assertEquals(ExploreStage.ACTIVE, state(model).stage)
            assertEquals(repo.snapshot, state(model).snapshot)
            main { assertTrue("A background check must retain the visible state", observed.isEmpty()) }

            repo.releaseLoad.countDown()
            until { steps.readCalls.get() >= 2 }
            instrumentation.waitForIdleSync()
            assertEquals(2, repo.loadCalls.get())
            main { assertTrue("Identical data must not emit another UI state", observed.isEmpty()) }
        } finally {
            repo.releaseLoad.countDown()
            main { model.state.removeObserver(observer) }
        }
    }

    @Test fun forceRefreshDuringAnOlderFetchQueuesOneCheckForTheNewJourney() {
        val repo = FakeRepository().apply { blockedLoadNumber = 1 }
        scenario(repo, waitForInitialLoad = false) { model, source, steps ->
            assertTrue(source.loadEntered.await(5, TimeUnit.SECONDS))
            val newerJourney = source.snapshot.journey!!.copy(
                id = source.snapshot.journey!!.id + 1,
                destinationName = "Newly selected destination")
            source.snapshot = source.snapshot.copy(journey = newerJourney)

            // A place-detail result can arrive while the previous request still has old data.
            main { repeat(10) { model.load(force = true) } }
            assertEquals(1, source.loadCalls.get())
            source.releaseLoad.countDown()

            until { state(model).snapshot?.journey?.id == newerJourney.id && steps.readCalls.get() >= 1 }
            instrumentation.waitForIdleSync()
            assertEquals(2, source.loadCalls.get())
            assertEquals(newerJourney, state(model).snapshot?.journey)
            assertEquals(ExploreStage.ACTIVE, state(model).stage)
        }
    }

    @Test fun unchangedZeroStepsNeverPostsAnAutomaticSync() = scenario { model, repo, steps ->
        main { repeat(5) { model.autoSyncSteps() } }
        until { steps.readCalls.get() >= 2 }
        instrumentation.waitForIdleSync()

        assertEquals(0L, state(model).snapshot?.journey?.sourceSteps)
        assertEquals(0, repo.syncCalls.get())
        assertEquals(1, repo.loadCalls.get())
    }

    private class FakeSteps : StepSource {
        val readCalls = AtomicInteger()
        override suspend fun hasPermission() = true
        override suspend fun needsActivityPermission() = false
        override suspend fun capture(journeyId: Long, startedAtEpochSeconds: Long, destinationName: String) = Unit
        override suspend fun totalStepsSince(journeyId: Long, startedAtEpochSeconds: Long): Long {
            readCalls.incrementAndGet()
            return 0L
        }
    }

    private class FakeRepository : ExploreRepository {
        val loadCalls = AtomicInteger()
        val syncCalls = AtomicInteger()
        val loadEntered = CountDownLatch(1)
        val releaseLoad = CountDownLatch(1)
        @Volatile var blockedLoadNumber = 0
        @Volatile var snapshot = ExploreSnapshot(null,
            ExploreJourney(System.nanoTime(), "Refresh test destination", Coordinates(21.0, 105.0),
                1_000, 0, "IN_PROGRESS", 1, sourceSteps = 0), emptyList())

        override fun load(): ExploreSnapshot {
            val call = loadCalls.incrementAndGet()
            // Capture before the gate to reproduce an old response racing a user change.
            val response = snapshot
            if (call == blockedLoadNumber) {
                loadEntered.countDown()
                check(releaseLoad.await(8, TimeUnit.SECONDS)) { "Test refresh gate timed out" }
            }
            return response
        }

        override fun syncSteps(journeyId: Long, cumulativeSteps: Long): ExploreSnapshot {
            syncCalls.incrementAndGet()
            return snapshot
        }
        override fun loadPlace(placeId: Long): ExplorePlaceDetail = error("Not used")
        override fun loadRecommendations(): Recommendations = error("Not used")
        override fun createJourney(placeId: Long, replaceActive: Boolean): ExploreSnapshot = error("Not used")
    }
}
