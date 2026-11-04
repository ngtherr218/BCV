package com.project.bcv

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.feature.location.startingpoint.domain.*
import com.project.bcv.feature.location.startingpoint.presentation.StartingPointStage
import com.project.bcv.feature.location.startingpoint.presentation.StartingPointViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartingPointFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val gpsCoordinates = Coordinates(21.0278, 105.8342)
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun awaitStage(model: StartingPointViewModel, expected: StartingPointStage) {
        val deadline = System.currentTimeMillis() + 8000
        var actual: StartingPointStage? = null
        do {
            main { actual = model.state.value?.stage }
            if (actual == expected) return
            Thread.sleep(25)
        } while (System.currentTimeMillis() < deadline)
        assertEquals("Starting-point state did not settle", expected, actual)
    }

    private fun scenario(action: (StartingPointViewModel, FakeRepository, FakeLocation) -> Unit) {
        val repository = FakeRepository()
        val location = FakeLocation()
        val store = ViewModelStore()
        lateinit var model: StartingPointViewModel
        main {
            model = StartingPointViewModel(repository, location, object : AddressLookup {
                override fun describe(coordinates: Coordinates): String? = null
            })
            store.put("starting-point", model)
            model.load()
        }
        try {
            awaitStage(model, StartingPointStage.READY)
            action(model, repository, location)
        } finally {
            main { store.clear() }
        }
    }

    @Test fun currentChoiceSavesGpsInsteadOfSuggestedOrNearestPlaceCoordinates() = scenario { model, repo, gps ->
        main { model.locate(); gps.deliver(0, gpsCoordinates) }
        awaitStage(model, StartingPointStage.VALID_CURRENT)
        main {
            assertEquals(repo.nearest, model.state.value?.current)
            assertEquals(gpsCoordinates, model.state.value?.currentCoordinates)
            model.saveCurrent()
        }
        awaitStage(model, StartingPointStage.SAVED)
        assertEquals(gpsCoordinates, repo.resolvedCoordinates)
        assertEquals(StartingPointSource.CURRENT, repo.savedSource)
        assertEquals(gpsCoordinates, repo.savedCoordinates)
        assertEquals(1, repo.saveCalls)
        main { assertEquals(gpsCoordinates, model.state.value?.saved?.coordinates) }
    }

    @Test fun changingToSuggestionIgnoresLateGpsCallback() = scenario { model, repo, gps ->
        main {
            model.locate()
            model.useSuggestion()
            gps.deliver(0, gpsCoordinates)
            assertEquals(StartingPointStage.READY, model.state.value?.stage)
            assertNull(model.state.value?.currentCoordinates)
            model.saveSuggestion()
        }
        awaitStage(model, StartingPointStage.SAVED)
        assertEquals(1, gps.cancelCalls)
        assertNull(repo.resolvedCoordinates)
        assertEquals(StartingPointSource.SUGGESTED, repo.savedSource)
        assertEquals(1, repo.saveCalls)
        main { assertEquals(repo.suggested, model.state.value?.saved) }
    }

    @Test fun failedRetryCannotSaveCoordinatesFromEarlierFix() = scenario { model, repo, gps ->
        main { model.locate(); gps.deliver(0, gpsCoordinates) }
        awaitStage(model, StartingPointStage.VALID_CURRENT)
        main {
            model.locate()
            assertNull(model.state.value?.currentCoordinates)
            assertNull(model.state.value?.current)
            gps.deliver(1, null)
            assertEquals(StartingPointStage.NO_FIX, model.state.value?.stage)
            model.saveCurrent()
            assertEquals(StartingPointStage.NO_FIX, model.state.value?.stage)
        }
        assertEquals(0, repo.saveCalls)
    }

    @Test fun unsupportedGpsShowsErrorWithoutSavingOrChoosingSuggestion() = scenario { model, repo, gps ->
        repo.outsideArea = true
        main { model.locate(); gps.deliver(0, Coordinates(37.421998, -122.084)) }
        awaitStage(model, StartingPointStage.OUTSIDE_AREA)
        main {
            assertNull(model.state.value?.current)
            assertNull(model.state.value?.saved)
            assertEquals("Outside supported area", model.state.value?.message)
        }
        assertEquals(0, repo.saveCalls)
        assertNull(repo.savedSource)
    }

    private class FakeLocation : DeviceLocationSource {
        private val callbacks = mutableListOf<(Coordinates?) -> Unit>()
        var cancelCalls = 0
        override fun findCurrent(callback: (Coordinates?) -> Unit) { callbacks.add(callback) }
        override fun cancel() { cancelCalls++ }
        // Keep callbacks after cancel to simulate an already queued platform response.
        fun deliver(request: Int, coordinates: Coordinates?) = callbacks[request](coordinates)
    }

    private class FakeRepository : StartingPointRepository {
        val suggested = StartingPoint("Hà Nội", "Hồ Gươm", Coordinates(21.028889, 105.8525))
        val nearest = StartingPoint("Hà Nội", "Văn Miếu", Coordinates(21.0274, 105.8355))
        @Volatile var outsideArea = false
        @Volatile var resolvedCoordinates: Coordinates? = null
        @Volatile var savedSource: StartingPointSource? = null
        @Volatile var savedCoordinates: Coordinates? = null
        @Volatile var saveCalls = 0
        override fun load() = StartingPointOptions(null, suggested)
        override fun resolveCurrent(coordinates: Coordinates): StartingPoint {
            resolvedCoordinates = coordinates
            if (outsideArea) throw ApiFailure("OUT_OF_SUPPORTED_AREA", "Outside supported area")
            return nearest
        }
        override fun save(source: StartingPointSource, coordinates: Coordinates?): StartingPoint {
            savedSource = source
            savedCoordinates = coordinates
            saveCalls++
            return if (source == StartingPointSource.CURRENT) nearest.copy(coordinates = requireNotNull(coordinates))
            else suggested
        }
    }
}
