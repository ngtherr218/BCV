package com.project.bcv

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.feature.discovery.data.StepTestPreferences
import com.project.bcv.feature.discovery.domain.*
import com.project.bcv.feature.discovery.presentation.ExploreStage
import com.project.bcv.feature.discovery.presentation.ExploreViewModel
import com.project.bcv.feature.location.startingpoint.domain.AddressLookup
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class StepSimulationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun until(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 8000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(25)
        assertTrue("Timed out waiting for test state", condition())
    }
    private fun scenario(action: (ExploreViewModel, FakeRepository) -> Unit) {
        val repo = FakeRepository()
        val context = instrumentation.targetContext
        val preferences = StepTestPreferences(context)
        val store = ViewModelStore()
        lateinit var model: ExploreViewModel
        main {
            model = ExploreViewModel(repo, object : StepSource {
                override suspend fun hasPermission() = false
                override suspend fun needsActivityPermission() = false
                override suspend fun capture(journeyId: Long) {}
                override suspend fun totalStepsSince(journeyId: Long, startedAtEpochSeconds: Long) = 0L
            }, object : AddressLookup { override fun describe(coordinates: Coordinates): String? = null }, preferences)
            store.put("steps", model)
            model.load()
        }
        until { model.state.value?.snapshot != null && model.state.value?.stage != ExploreStage.RESTORING }
        try { action(model, repo) } finally {
            main { model.stopStepSimulation(); store.clear() }
            val key = "${BuildConfig.API_BASE_URL}:${repo.tripId}"
            context.getSharedPreferences("bcv_step_tests", 0).edit().remove(key).remove("pending:$key").commit()
        }
    }
    @Test fun testStepsUseRawTotalInsteadOfShoesBoostedProgress() = scenario { model, repo ->
        main { model.sendTestSteps(100); model.sendTestSteps(100) }
        until { model.stepTest.value?.busy == false && repo.calls > 0 }
        assertEquals(1, repo.calls)
        assertEquals(300L, repo.lastTarget)
        assertEquals(600, model.state.value?.snapshot?.journey?.recordedSteps)
    }
    @Test fun timeoutAfterServerCommitDoesNotAddBatchTwice() = scenario { model, repo ->
        repo.failAfterCommit = true
        main { model.sendTestSteps(100) }
        until { repo.calls == 1 && model.stepTest.value?.busy == false }
        main { model.sendTestSteps(100) }
        until { model.stepTest.value?.message?.contains("không cộng trùng") == true }
        assertEquals(1, repo.calls)
        assertEquals(300L, model.state.value?.snapshot?.journey?.sourceSteps)
    }
    @Test fun stopPreventsFutureBatches() = scenario { model, repo ->
        main { model.startStepSimulation(50, 2) }
        until { repo.calls == 1 && model.stepTest.value?.busy == false }
        main { model.stopStepSimulation() }
        Thread.sleep(2200)
        assertEquals(1, repo.calls)
        assertFalse(model.stepTest.value!!.running)
    }
    @Test fun completionStopsSimulation() = scenario { model, repo ->
        main { model.startStepSimulation(5000, 2) }
        until { repo.calls == 1 && model.stepTest.value?.running == false }
        assertEquals(ExploreStage.COMPLETED, model.state.value?.stage)
        assertEquals(1, repo.calls)
    }
    class FakeRepository : ExploreRepository {
        val tripId = System.nanoTime()
        @Volatile var calls = 0
        @Volatile var lastTarget = 0L
        @Volatile var failAfterCommit = false
        @Volatile private var journey = ExploreJourney(tripId, "Test destination", Coordinates(21.01,105.81),
            10000, 400, "IN_PROGRESS", 1, sourceSteps = 200)
        override fun load() = ExploreSnapshot(null, journey, emptyList())
        override fun syncSteps(journeyId: Long, cumulativeSteps: Long): ExploreSnapshot {
            Thread.sleep(50)
            calls++
            lastTarget = cumulativeSteps
            val progress = minOf(10000, journey.recordedSteps + ((cumulativeSteps - journey.sourceSteps!!) * 2).toInt())
            journey = journey.copy(sourceSteps = cumulativeSteps, recordedSteps = progress,
                status = if(progress == 10000) "COMPLETED" else "IN_PROGRESS")
            if(failAfterCommit) { failAfterCommit=false; throw IOException("Lost response after commit") }
            return load()
        }
        override fun loadPlace(placeId: Long): ExplorePlaceDetail = error("Not used")
        override fun loadRecommendations(): Recommendations = error("Not used")
        override fun createJourney(placeId: Long, replaceActive: Boolean): ExploreSnapshot = error("Not used")
    }
}
