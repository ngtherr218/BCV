package com.project.bcv.feature.journey.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.ui.TabRefresh
import com.project.bcv.feature.discovery.data.StepTestPreferences
import com.project.bcv.feature.discovery.domain.ExploreRepository
import com.project.bcv.feature.discovery.domain.ExploreSnapshot
import com.project.bcv.feature.discovery.domain.StepSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface JourneyUiState {
    data object Loading : JourneyUiState
    /** Reading saved progress is distinct from successfully uploading the device's steps. */
    data class Loaded(val snapshot: ExploreSnapshot, val syncedAtMillis: Long,
        val stepsSynced: Boolean = false, val syncMessage: String? = null) : JourneyUiState
    data class Failed(val message: String) : JourneyUiState
    data object SessionExpired : JourneyUiState
}

/** Reconciles recorded background steps on entry; only server-confirmed progress is displayed. */
class JourneyViewModel(private val repository: ExploreRepository, private val steps: StepSource,
    private val isTestJourney: (Long) -> Boolean = { false }) : ViewModel() {
    private val mutableState = MutableLiveData<JourneyUiState>(JourneyUiState.Loading)
    val state: LiveData<JourneyUiState> = mutableState
    private var loading = false
    private val freshness = TabRefresh()
    private var lastSent: Pair<Long, Long>? = null
    var scrollY: Int = 0

    init { load(force = false) }

    fun refreshIfNeeded() = load(force = false)

    fun load(force: Boolean = true) {
        if (loading || !freshness.shouldRefresh(mutableState.value is JourneyUiState.Loaded, force)) return
        loading = true
        if (mutableState.value !is JourneyUiState.Loaded) mutableState.value = JourneyUiState.Loading
        viewModelScope.launch {
            val next = try {
                val snapshot = withContext(Dispatchers.IO) { repository.load() }
                freshness.loaded()
                reconcile(snapshot)
            } catch (error: ApiFailure) {
                if (error.code == "UNAUTHORIZED") JourneyUiState.SessionExpired
                else failed(error.message)
            } catch (error: IOException) {
                Log.w(TAG, "Journey load could not reach the API", error)
                failed("Không kết nối được máy chủ. Kiểm tra Wi-Fi rồi thử lại.")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e(TAG, "Journey load failed", error)
                failed("Chưa tải được hành trình. Vui lòng thử lại.")
            } finally {
                loading = false
            }
            publish(next)
        }
    }

    /** Read the local counter on return without fetching the same journey again. */
    fun syncDeviceSteps() {
        val current = mutableState.value as? JourneyUiState.Loaded ?: return
        if (loading || current.snapshot.journey?.status != "IN_PROGRESS") return
        loading = true
        viewModelScope.launch {
            try {
                publish(reconcile(current.snapshot))
            } catch (error: ApiFailure) {
                if (error.code == "UNAUTHORIZED") mutableState.value = JourneyUiState.SessionExpired
            } finally { loading = false }
        }
    }

    private suspend fun reconcile(saved: ExploreSnapshot): JourneyUiState.Loaded {
        var snapshot = saved
        val journey = saved.journey?.takeIf { it.status == "IN_PROGRESS" }
        var synced = false
        var message: String? = null
        if (journey != null && BuildConfig.STEP_TEST_ENABLED && isTestJourney(journey.id)) {
            message = "Hành trình thử · Không tự động cộng bước cảm biến."
        } else try {
            steps.capture(journey?.id ?: 0, journey?.startedAtEpochSeconds ?: 0, journey?.destinationName.orEmpty())
            if (journey != null) {
                if (steps.hasPermission()) {
                    val total = steps.totalStepsSince(journey.id, journey.startedAtEpochSeconds)
                    val sent = maxOf(journey.sourceSteps ?: -1L,
                        lastSent?.takeIf { it.first == journey.id }?.second ?: -1L)
                    if (total > sent) {
                        snapshot = withContext(Dispatchers.IO) { repository.syncSteps(journey.id, total) }
                        lastSent = journey.id to total
                        if (snapshot.journey?.status == "COMPLETED") steps.capture(0)
                    }
                    synced = true
                } else message = "Chưa có quyền đọc bước chân. Mở Khám phá để cấp quyền."
            }
        } catch (error: ApiFailure) {
            if (error.code == "UNAUTHORIZED") throw error
            message = "Chưa đồng bộ bước chân. " + error.message
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            message = "Chưa đồng bộ bước chân. Tiến độ đã lưu được giữ nguyên; sẽ thử lại."
            Log.w(TAG, "Keep confirmed progress until step sync can be retried", error)
        }
        return JourneyUiState.Loaded(snapshot, System.currentTimeMillis(), synced, message)
    }

    private fun failed(message: String): JourneyUiState =
        (mutableState.value as? JourneyUiState.Loaded)?.copy(stepsSynced = false, syncMessage = message)
            ?: JourneyUiState.Failed(message)

    private fun publish(next: JourneyUiState) {
        val old = mutableState.value
        if (old is JourneyUiState.Loaded && next is JourneyUiState.Loaded &&
            old.snapshot == next.snapshot && old.stepsSynced == next.stepsSynced && old.syncMessage == next.syncMessage) return
        mutableState.value = next
    }

    private companion object { const val TAG = "JourneyViewModel" }
}

class JourneyViewModelFactory(private val repository: ExploreRepository, private val steps: StepSource,
    private val stepTests: StepTestPreferences? = null) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        JourneyViewModel(repository, steps) { id -> stepTests?.isTestJourney(id) == true } as T
}
