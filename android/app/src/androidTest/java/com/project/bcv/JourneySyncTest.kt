package com.project.bcv

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.feature.discovery.domain.*
import com.project.bcv.feature.journey.presentation.JourneyUiState
import com.project.bcv.feature.journey.presentation.JourneyViewModel
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class JourneySyncTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun loaded(model: JourneyViewModel): JourneyUiState.Loaded {
        val deadline = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < deadline) {
            var current: JourneyUiState? = null
            main { current = model.state.value }
            if (current is JourneyUiState.Loaded) return current as JourneyUiState.Loaded
            Thread.sleep(20)
        }
        error("Timed out waiting for journey progress")
    }

    @Test fun failedStepUploadKeepsSavedProgressWithoutClaimingSync() {
        val repo = FakeRepository().apply { failSync = true }
        val store = ViewModelStore()
        lateinit var model: JourneyViewModel
        main { model = JourneyViewModel(repo, FakeSteps()); store.put("journey", model) }
        try {
            val state = loaded(model)
            assertEquals(7, state.snapshot.journey!!.recordedSteps)
            assertFalse(state.stepsSynced)
            assertTrue(state.syncMessage!!.contains("Chưa đồng bộ"))
            assertEquals(1, repo.syncCalls.get())
        } finally { main { store.clear() } }
    }

    @Test fun overlappingRefreshesShareOneLoadAndOneUpload() {
        val gate = CountDownLatch(1)
        val repo = FakeRepository().apply { loadGate = gate }
        val steps = FakeSteps()
        val store = ViewModelStore()
        lateinit var model: JourneyViewModel
        main { model = JourneyViewModel(repo, steps); store.put("journey", model) }
        try {
            assertTrue(repo.loadStarted.await(5, TimeUnit.SECONDS))
            main { repeat(10) { model.load() } }
            assertEquals(1, repo.loadCalls.get())
            gate.countDown()
            assertTrue(loaded(model).stepsSynced)
            assertEquals(1, repo.syncCalls.get())
            // A later explicit refresh can read the server, but unchanged local steps are not posted again.
            repo.loadGate = null
            main { model.load() }
            val deadline = System.currentTimeMillis() + 5000
            var reread = false
            while (!reread && System.currentTimeMillis() < deadline) {
                main { reread = steps.readCalls >= 2 }
                if (!reread) Thread.sleep(20)
            }
            assertTrue(reread)
            assertEquals(2, repo.loadCalls.get())
            assertEquals(1, repo.syncCalls.get())
        } finally { gate.countDown(); main { store.clear() } }
    }

    @Test fun returningToJourneyUsesSavedProgressAndOnlyUploadsNewSensorSteps() {
        val repo = FakeRepository()
        val steps = FakeSteps()
        val store = ViewModelStore()
        lateinit var model: JourneyViewModel
        main { model = JourneyViewModel(repo, steps); store.put("journey", model) }
        try {
            assertEquals(12, loaded(model).snapshot.journey!!.recordedSteps)
            main { model.refreshIfNeeded(); model.syncDeviceSteps() }
            assertEquals(1, repo.loadCalls.get())
            assertEquals(1, repo.syncCalls.get())
            main { steps.cumulative = 15; model.syncDeviceSteps() }
            val deadline = System.currentTimeMillis() + 5_000
            var recorded = 0
            while (recorded != 15 && System.currentTimeMillis() < deadline) {
                main { recorded = (model.state.value as? JourneyUiState.Loaded)?.snapshot?.journey?.recordedSteps ?: 0 }
                if (recorded != 15) Thread.sleep(20)
            }
            assertEquals(15, recorded)
            assertEquals(1, repo.loadCalls.get())
            assertEquals(2, repo.syncCalls.get())
        } finally { main { store.clear() } }
    }

    @Test fun simulatedJourneyDoesNotStartOrUploadRealSensorSteps() {
        assertTrue("This test exercises the debug simulation policy", BuildConfig.STEP_TEST_ENABLED)
        val repo = FakeRepository()
        val steps = FakeSteps()
        val store = ViewModelStore()
        lateinit var model: JourneyViewModel
        main { model = JourneyViewModel(repo, steps) { true }; store.put("journey", model) }
        try {
            val state = loaded(model)
            assertFalse(state.stepsSynced)
            assertTrue(state.syncMessage!!.contains("Hành trình thử"))
            assertEquals(0, steps.captureCalls)
            assertEquals(0, steps.readCalls)
            assertEquals(0, repo.syncCalls.get())
        } finally { main { store.clear() } }
    }

    private class FakeSteps : StepSource {
        var captureCalls = 0
        var readCalls = 0
        var cumulative = 12L
        override suspend fun hasPermission() = true
        override suspend fun needsActivityPermission() = false
        override suspend fun capture(journeyId: Long, startedAtEpochSeconds: Long, destinationName: String) {
            captureCalls++
        }
        override suspend fun totalStepsSince(journeyId: Long, startedAtEpochSeconds: Long): Long {
            readCalls++
            return cumulative
        }
    }

    private class FakeRepository : ExploreRepository {
        val loadCalls = AtomicInteger()
        val syncCalls = AtomicInteger()
        val loadStarted = CountDownLatch(1)
        @Volatile var loadGate: CountDownLatch? = null
        var failSync = false
        @Volatile private var snapshot = ExploreSnapshot(null,
            ExploreJourney(73, "Điểm thử", Coordinates(21.0, 105.0), 100, 7, "IN_PROGRESS", 1), emptyList())
        override fun load(): ExploreSnapshot {
            loadCalls.incrementAndGet()
            loadStarted.countDown()
            check(loadGate?.await(8, TimeUnit.SECONDS) != false) { "Test load gate timed out" }
            return snapshot
        }
        override fun syncSteps(journeyId: Long, cumulativeSteps: Long): ExploreSnapshot {
            syncCalls.incrementAndGet()
            if (failSync) throw IOException("Test offline upload")
            snapshot = snapshot.copy(journey = snapshot.journey!!.copy(
                recordedSteps = cumulativeSteps.toInt(), sourceSteps = cumulativeSteps))
            return snapshot
        }
        override fun loadPlace(placeId: Long): ExplorePlaceDetail = error("Not used")
        override fun loadRecommendations(): Recommendations = error("Not used")
        override fun createJourney(placeId: Long, replaceActive: Boolean): ExploreSnapshot = error("Not used")
    }
}
