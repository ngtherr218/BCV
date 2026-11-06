package com.project.bcv

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.ui.AppDataChanges
import com.project.bcv.core.ui.TabRefresh
import com.project.bcv.feature.collection.domain.*
import com.project.bcv.feature.collection.presentation.CollectionUiState
import com.project.bcv.feature.collection.presentation.CollectionViewModel
import com.project.bcv.feature.questitem.quest.domain.*
import com.project.bcv.feature.questitem.quest.presentation.QuestUiState
import com.project.bcv.feature.questitem.quest.presentation.QuestViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class TabOverviewRefreshTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 8_000
        while (System.currentTimeMillis() < deadline) {
            var satisfied = false
            main { satisfied = condition() }
            if (satisfied) return
            Thread.sleep(20)
        }
        error("Timed out waiting for cached tab state")
    }

    @Test fun revisitingCollectionsUsesFreshContentThenFetchesAfterMutation() {
        val repo = Collections()
        val store = ViewModelStore()
        lateinit var model: CollectionViewModel
        main { model = CollectionViewModel(repo); store.put("collection", model) }
        try {
            awaitCondition { model.state.value is CollectionUiState.Loaded }
            main { repeat(10) { model.refreshIfNeeded() } }
            assertEquals(1, repo.calls.get())
            repo.overview = repo.overview.copy(ownedCards = 56)
            main { AppDataChanges.changed(); model.refreshIfNeeded() }
            awaitCondition { (model.state.value as? CollectionUiState.Loaded)?.overview?.ownedCards == 56 }
            assertEquals(2, repo.calls.get())
        } finally { main { store.clear() } }
    }

    @Test fun failedCollectionRefreshKeepsCardsAndRetryClearsInlineError() {
        val repo = Collections()
        val store = ViewModelStore()
        lateinit var model: CollectionViewModel
        main { model = CollectionViewModel(repo); store.put("collection", model) }
        try {
            awaitCondition { model.state.value is CollectionUiState.Loaded }
            repo.failure = IOException("Offline")
            main { model.load() }
            awaitCondition { (model.state.value as? CollectionUiState.Loaded)?.refreshError != null }
            main { assertEquals(55, (model.state.value as CollectionUiState.Loaded).overview.ownedCards) }
            repo.failure = null
            main { model.load() }
            awaitCondition { (model.state.value as? CollectionUiState.Loaded)?.refreshError == null }
        } finally { main { store.clear() } }
    }

    @Test fun repeatedCollectionRefreshesDoNotStartOverlappingRequests() {
        val gate = CountDownLatch(1)
        val repo = Collections().apply { loadGate = gate }
        val store = ViewModelStore()
        lateinit var model: CollectionViewModel
        main { model = CollectionViewModel(repo); store.put("collection", model) }
        try {
            assertTrue(repo.started.await(5, TimeUnit.SECONDS))
            main { repeat(10) { model.load() } }
            assertEquals(1, repo.calls.get())
            gate.countDown()
            awaitCondition { model.state.value is CollectionUiState.Loaded }
        } finally { gate.countDown(); main { store.clear() } }
    }

    @Test fun questTimestampOnlyRefreshDoesNotReemitOrRebuildProgress() {
        val repo = Quests()
        val store = ViewModelStore()
        lateinit var model: QuestViewModel
        val emissions = AtomicInteger()
        val observer = Observer<QuestUiState> { emissions.incrementAndGet() }
        main { model = QuestViewModel(repo); store.put("quest", model) }
        try {
            awaitCondition { model.state.value is QuestUiState.Loaded }
            main { model.state.observeForever(observer) }
            val initialEmissions = emissions.get()
            repo.overview = repo.overview.copy(updatedAtMillis = 2_000)
            main { model.load() }
            assertTrue(repo.refreshed.await(5, TimeUnit.SECONDS))
            repo.overview = repo.overview.copy(daily = listOf(quest(progress = 20)))
            main { AppDataChanges.changed() }
            awaitCondition {
                model.refreshIfNeeded()
                (model.state.value as? QuestUiState.Loaded)?.overview?.daily?.first()?.progress == 20
            }
            // Only the changed progress emits; the timestamp-only response caused no render.
            assertEquals(initialEmissions + 1, emissions.get())
        } finally { main { model.state.removeObserver(observer); store.clear() } }
    }

    @Test fun staleQuestCacheRefreshesQuietlyAndRejectedSessionDoesNotKeepPrivateContent() {
        var now = 0L
        val repo = Quests()
        val store = ViewModelStore()
        lateinit var model: QuestViewModel
        main { model = QuestViewModel(repo, TabRefresh { now }); store.put("quest", model) }
        try {
            awaitCondition { model.state.value is QuestUiState.Loaded }
            main { now = 59_999; model.refreshIfNeeded() }
            assertEquals(1, repo.calls.get())
            repo.failure = IOException("Offline")
            main { now = 60_000; model.refreshIfNeeded() }
            awaitCondition { (model.state.value as? QuestUiState.Loaded)?.refreshError != null }
            main { assertEquals(10, (model.state.value as QuestUiState.Loaded).overview.daily.first().progress) }
            repo.failure = ApiFailure("UNAUTHORIZED", "Expired")
            main { model.load() }
            awaitCondition { model.state.value == QuestUiState.SessionExpired }
        } finally { main { store.clear() } }
    }

    private class Collections : CollectionRepository {
        val calls = AtomicInteger()
        val started = CountDownLatch(1)
        @Volatile var loadGate: CountDownLatch? = null
        @Volatile var failure: Exception? = null
        @Volatile var overview = CollectionOverview(55, 311, emptyList())
        override fun load(): CollectionOverview {
            calls.incrementAndGet()
            started.countDown()
            check(loadGate?.await(8, TimeUnit.SECONDS) != false)
            failure?.let { throw it }
            return overview
        }
        override fun invalidate() = Unit
        override fun loadBadgeCount() = overview.newCards
        override fun markSeen(cardId: Long) = error("Not used")
        override fun loadDetail(collectionId: Long): CollectionDetail = error("Not used")
    }

    private class Quests : QuestRepository {
        val calls = AtomicInteger()
        val refreshed = CountDownLatch(1)
        @Volatile var failure: Exception? = null
        @Volatile var overview = QuestOverview(1_000, listOf(quest(10)), emptyList())
        override fun load(): QuestOverview {
            val response = overview
            if (calls.incrementAndGet() > 1) refreshed.countDown()
            failure?.let { throw it }
            return response
        }
    }

    private companion object {
        fun quest(progress: Int) = Quest(1, "Đi bộ", "STEP_COUNT", 100, progress,
            false, false, null, null, null)
    }
}
