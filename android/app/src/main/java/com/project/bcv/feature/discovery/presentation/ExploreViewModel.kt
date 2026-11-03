package com.project.bcv.feature.discovery.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.BuildConfig
import com.project.bcv.feature.discovery.data.StepTestPreferences
import com.project.bcv.feature.discovery.domain.ExploreRepository
import com.project.bcv.feature.discovery.domain.ExploreSnapshot
import com.project.bcv.feature.discovery.domain.StepSource
import com.project.bcv.feature.location.startingpoint.domain.AddressLookup
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException

enum class ExploreStage {
    LOADING, EMPTY, ACTIVE, COMPLETED, RESTORING, LOAD_FAILED, RESTORE_FAILED,
    SYNCING, SYNC_FAILED, NO_NEW_STEPS, PERMISSION_REQUIRED, SESSION_EXPIRED
}

data class ExploreUiState(
    val stage: ExploreStage = ExploreStage.LOADING,
    val snapshot: ExploreSnapshot? = null,
    val message: String? = null
)

data class StepTestUiState(val running: Boolean = false, val busy: Boolean = false,
    val batches: Int = 0, val message: String = "Chọn số bước và gửi, hoặc bật mô phỏng định kỳ.")

class ExploreViewModel(
    private val repository: ExploreRepository,
    private val steps: StepSource,
    private val addressLookup: AddressLookup,
    private val stepTests: StepTestPreferences
) : ViewModel() {
    private val mutableState = MutableLiveData(ExploreUiState())
    val state: LiveData<ExploreUiState> = mutableState
    private val mutableAddress = MutableLiveData<String?>()
    /** Street address of the virtual location; kept apart because states are rebuilt often. */
    val address: LiveData<String?> = mutableAddress
    private var addressFor: Coordinates? = null
    private var autoSyncing = false
    /** Last cumulative total sent automatically, per journey, so unchanged totals are not sent again. */
    private var autoSentTotal: Pair<Long, Long>? = null
    private val mutableStepTest = MutableLiveData(StepTestUiState())
    val stepTest: LiveData<StepTestUiState> = mutableStepTest
    private var simulationJob: Job? = null
    private var simulationRequested = false
    private fun isTestJourney(id: Long) = BuildConfig.STEP_TEST_ENABLED && stepTests.isTestJourney(id)

    private companion object { const val TAG = "ExploreViewModel" }

    fun load() {
        val current = mutableState.value ?: ExploreUiState()
        if (current.stage == ExploreStage.RESTORING || current.stage == ExploreStage.SYNCING) return
        mutableState.value = current.copy(
            stage = if (current.snapshot == null) ExploreStage.LOADING else ExploreStage.RESTORING,
            message = null
        )
        viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { repository.load() }
                mutableState.value = ExploreUiState(snapshot.stage(), snapshot)
                captureDeviceSteps()
                lookupAddress(snapshot)
                checkPermission()
                autoSyncSteps()
            } catch (error: ApiFailure) {
                if (error.code == "UNAUTHORIZED") sessionExpired() else failLoad(error.message)
            } catch (error: IOException) {
                Log.w(TAG, "Explore load could not reach the API", error)
                failLoad("Không kết nối được máy chủ. Kiểm tra Wi-Fi rồi thử lại.")
            } catch (error: Exception) {
                Log.e(TAG, "Explore load failed", error)
                failLoad("Chưa tải được dữ liệu khám phá. Vui lòng thử lại.")
            }
        }
    }

    /**
     * Starts the Recording API whenever the screen shows, when the current journey uses it
     * (no app records steps in Health Connect); steps are recorded only from then on.
     */
    fun captureDeviceSteps() {
        val snapshot = mutableState.value?.snapshot ?: return
        val journey = snapshot.journey?.takeIf { it.status == "IN_PROGRESS" }
        if (journey != null && isTestJourney(journey.id)) return
        viewModelScope.launch { runCatching {
            steps.capture(journey?.id ?: 0, journey?.startedAtEpochSeconds ?: 0, journey?.destinationName.orEmpty())
        } }
    }

    /** Calls [ask] when the Recording API is the only way to count walking and still needs permission. */
    fun whenActivityPermissionNeeded(ask: () -> Unit) {
        if (mutableState.value?.snapshot?.journey?.id?.let(::isTestJourney) == true) return
        viewModelScope.launch { if (runCatching { steps.needsActivityPermission() }.getOrDefault(false)) ask() }
    }

    /** An already usable sensor must not be blocked by another Health Connect prompt on every launch. */
    fun whenStepPermissionNeeded(ask: () -> Unit) {
        viewModelScope.launch { if (!runCatching { steps.hasPermission() }.getOrDefault(false)) ask() }
    }

    fun checkPermission() {
        val current = mutableState.value ?: return
        if (current.snapshot?.journey?.status != "IN_PROGRESS" ||
            current.stage in setOf(ExploreStage.SYNCING, ExploreStage.RESTORING)) return
        if (isTestJourney(current.snapshot.journey.id)) return
        viewModelScope.launch {
            val granted = runCatching { steps.hasPermission() }.getOrDefault(false)
            val latest = mutableState.value ?: return@launch
            if (latest.snapshot?.journey?.id != current.snapshot.journey?.id) return@launch
            if (!granted) mutableState.value = latest.copy(stage = ExploreStage.PERMISSION_REQUIRED)
            else if (latest.stage == ExploreStage.PERMISSION_REQUIRED) {
                mutableState.value = latest.copy(stage = ExploreStage.ACTIVE)
            }
        }
    }

    fun syncSteps() {
        val current = mutableState.value ?: return
        val journey = current.snapshot?.journey ?: return
        if (isTestJourney(journey.id)) { load(); return }
        if (journey.status != "IN_PROGRESS" || current.stage == ExploreStage.SYNCING) return
        mutableState.value = current.copy(stage = ExploreStage.SYNCING, message = null)
        viewModelScope.launch {
            try {
                if (!steps.hasPermission()) {
                    mutableState.value = current.copy(stage = ExploreStage.PERMISSION_REQUIRED)
                    return@launch
                }
                val total = steps.totalStepsSince(journey.id, journey.startedAtEpochSeconds)
                val snapshot = withContext(Dispatchers.IO) { repository.syncSteps(journey.id, total) }
                autoSentTotal = journey.id to total
                val newJourney = snapshot.journey
                mutableState.value = ExploreUiState(
                    if (newJourney?.status == "COMPLETED") ExploreStage.COMPLETED
                    else if (newJourney?.recordedSteps == journey.recordedSteps) ExploreStage.NO_NEW_STEPS
                    else ExploreStage.ACTIVE,
                    snapshot
                )
                lookupAddress(snapshot)
                captureDeviceSteps()
            } catch (error: ApiFailure) {
                if (error.code == "UNAUTHORIZED") sessionExpired()
                else mutableState.value = current.copy(stage = ExploreStage.SYNC_FAILED, message = error.message)
            } catch (_: Exception) {
                mutableState.value = current.copy(stage = ExploreStage.SYNC_FAILED,
                    message = "Chưa đồng bộ được bước chân. Vui lòng thử lại.")
            }
        }
    }

    /**
     * Sends new steps without a tap, while the screen shows. Quiet: no syncing state, and a failure
     * keeps the saved progress on screen; the next run (or the Đồng bộ button) tries again.
     */
    fun autoSyncSteps() {
        val current = mutableState.value ?: return
        val journey = current.snapshot?.journey ?: return
        if (isTestJourney(journey.id) || autoSyncing || journey.status != "IN_PROGRESS" || current.stage !in
            setOf(ExploreStage.ACTIVE, ExploreStage.NO_NEW_STEPS, ExploreStage.SYNC_FAILED)) return
        autoSyncing = true
        viewModelScope.launch {
            try {
                if (!steps.hasPermission()) return@launch
                val total = steps.totalStepsSince(journey.id, journey.startedAtEpochSeconds)
                val sent = autoSentTotal?.takeIf { it.first == journey.id }?.second
                if (sent != null && total <= sent) return@launch
                val snapshot = withContext(Dispatchers.IO) { repository.syncSteps(journey.id, total) }
                autoSentTotal = journey.id to total
                val latest = mutableState.value ?: return@launch
                // A manual sync, reload or new journey since then has the newer state.
                if (latest.snapshot?.journey != journey || latest.stage !in
                    setOf(ExploreStage.ACTIVE, ExploreStage.NO_NEW_STEPS, ExploreStage.SYNC_FAILED)) return@launch
                val newJourney = snapshot.journey
                mutableState.value = ExploreUiState(
                    if (newJourney?.status == "COMPLETED") ExploreStage.COMPLETED
                    else if (newJourney?.recordedSteps == journey.recordedSteps) latest.stage
                    else ExploreStage.ACTIVE,
                    snapshot
                )
                lookupAddress(snapshot)
                captureDeviceSteps()
            } catch (error: ApiFailure) {
                if (error.code == "UNAUTHORIZED") sessionExpired()
                else Log.w(TAG, "Automatic step sync was refused: ${error.message}")
            } catch (error: Exception) {
                Log.w(TAG, "Automatic step sync failed", error)
            } finally {
                autoSyncing = false
            }
        }
    }

    fun sendTestSteps(amount: Int) {
        if (!BuildConfig.STEP_TEST_ENABLED || mutableStepTest.value?.busy == true || simulationRequested) return
        viewModelScope.launch { submitTestSteps(amount) }
    }

    fun startStepSimulation(amount: Int, intervalSeconds: Int) {
        if (!BuildConfig.STEP_TEST_ENABLED || simulationJob?.isActive == true || mutableStepTest.value?.busy == true) return
        if (amount !in 1..100_000 || intervalSeconds !in 2..3600) {
            mutableStepTest.value = StepTestUiState(message = "Nhập 1–100.000 bước và chu kỳ 2–3.600 giây.")
            return
        }
        val journeyId = mutableState.value?.snapshot?.journey?.id ?: return
        simulationRequested = true
        mutableStepTest.value = StepTestUiState(running = true, message = "Đang chạy $amount bước mỗi $intervalSeconds giây.")
        simulationJob = viewModelScope.launch {
            try {
                while (simulationRequested && mutableState.value?.snapshot?.journey?.id == journeyId) {
                    if (!submitTestSteps(amount) || !simulationRequested) break
                    delay(intervalSeconds * 1000L)
                }
            } finally {
                simulationRequested = false
                mutableStepTest.value = (mutableStepTest.value ?: StepTestUiState()).copy(running = false)
            }
        }
    }

    /** Stops scheduled batches. An already-sent request is allowed to finish and be acknowledged. */
    fun stopStepSimulation() {
        simulationRequested = false
        if (mutableStepTest.value?.busy != true) simulationJob?.cancel()
        mutableStepTest.value = (mutableStepTest.value ?: StepTestUiState()).copy(running = false)
    }

    private suspend fun submitTestSteps(amount: Int): Boolean {
        val current = mutableState.value ?: return false
        val journey = current.snapshot?.journey
        if (amount !in 1..100_000 || journey?.status != "IN_PROGRESS" || autoSyncing ||
            current.stage in setOf(ExploreStage.SYNCING, ExploreStage.RESTORING)) {
            mutableStepTest.value = (mutableStepTest.value ?: StepTestUiState()).copy(
                message = "Cần hành trình đang đi và không có yêu cầu đồng bộ khác; nhập 1–100.000 bước.")
            return false
        }
        stepTests.useTestSteps(journey.id)
        mutableState.value = current.copy(stage = ExploreStage.SYNCING, message = null)
        mutableStepTest.value = (mutableStepTest.value ?: StepTestUiState()).copy(busy = true)
        try {
            val fresh = withContext(Dispatchers.IO) { repository.load() }
            val active = fresh.journey?.takeIf { it.id == journey.id }
                ?: throw IllegalStateException("Hành trình đã kết thúc hoặc thay đổi. Hãy tải lại.")
            val sourceTotal = active.sourceSteps
                ?: throw IllegalStateException("Backend cần cập nhật trường sourceSteps trước khi test.")
            // Reuse an uncertain request after timeouts/restarts; never add the same batch twice.
            val pending = stepTests.pendingTotal(journey.id)
            if (pending != null && sourceTotal >= pending) {
                stepTests.acknowledge(journey.id)
                mutableState.value = ExploreUiState(fresh.stage(), fresh)
                lookupAddress(fresh)
                mutableStepTest.value = (mutableStepTest.value ?: StepTestUiState()).copy(
                    message = "Yêu cầu trước đã được xác nhận: $sourceTotal bước gốc; không cộng trùng.")
                return active.status == "IN_PROGRESS"
            }
            require(active.status == "IN_PROGRESS") { "Hành trình đã kết thúc. Hãy tải lại." }
            val target = pending ?: (sourceTotal + amount)
            require(target <= 50_000_000L) { "Tổng bước vượt giới hạn 50 triệu của hành trình." }
            withContext(Dispatchers.IO) { stepTests.prepare(journey.id, target) }
            val confirmed = withContext(Dispatchers.IO) { repository.syncSteps(journey.id, target) }
            require((confirmed.journey?.sourceSteps ?: -1L) >= target) { "Máy chủ chưa xác nhận đủ bước; có thể gửi lại cùng yêu cầu." }
            stepTests.acknowledge(journey.id)
            mutableState.value = ExploreUiState(confirmed.stage(), confirmed)
            lookupAddress(confirmed)
            val status = confirmed.journey!!
            val prior = mutableStepTest.value ?: StepTestUiState()
            mutableStepTest.value = prior.copy(batches = prior.batches + 1,
                message = "Đã xác nhận #${prior.batches + 1}: bước gốc ${status.sourceSteps}; tiến độ ${status.recordedSteps}/${status.requiredSteps} (${status.progressPercent}%)." +
                    if (status.status == "COMPLETED") " Hành trình hoàn thành; đã dừng mô phỏng." else "")
            return status.status == "IN_PROGRESS"
        } catch (error: CancellationException) {
            mutableState.value = current.copy(stage = current.snapshot?.stage() ?: ExploreStage.EMPTY)
            throw error
        } catch (error: Exception) {
            mutableState.value = current.copy(stage = ExploreStage.SYNC_FAILED, message = error.message)
            mutableStepTest.value = (mutableStepTest.value ?: StepTestUiState()).copy(
                message = error.message ?: "Gửi bước thử chưa thành công; mô phỏng đã dừng.")
            if (error is ApiFailure && error.code == "UNAUTHORIZED") sessionExpired()
            return false
        } finally {
            mutableStepTest.value = (mutableStepTest.value ?: StepTestUiState()).copy(busy = false)
        }
    }

    private fun lookupAddress(snapshot: ExploreSnapshot) {
        val coordinates = snapshot.location?.coordinates ?: return
        if (coordinates == addressFor) return
        addressFor = coordinates
        mutableAddress.value = null
        viewModelScope.launch {
            val address = withContext(Dispatchers.IO) { addressLookup.describe(coordinates) }
            if (addressFor != coordinates) return@launch
            mutableAddress.value = address
            if (address == null) addressFor = null // retry on the next load

        }
    }

    private fun failLoad(message: String) {
        val current = mutableState.value ?: return
        mutableState.value = current.copy(
            stage = if (current.snapshot == null) ExploreStage.LOAD_FAILED else ExploreStage.RESTORE_FAILED,
            message = message
        )
    }

    private fun sessionExpired() {
        mutableState.value = ExploreUiState(ExploreStage.SESSION_EXPIRED,
            message = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
    }

    private fun ExploreSnapshot.stage() = when (journey?.status) {
        "IN_PROGRESS" -> ExploreStage.ACTIVE
        "COMPLETED" -> ExploreStage.COMPLETED
        else -> ExploreStage.EMPTY
    }

    override fun onCleared() {
        steps.release()
        super.onCleared()
    }
}

class ExploreViewModelFactory(
    private val repository: ExploreRepository,
    private val steps: StepSource,
    private val addressLookup: AddressLookup,
    private val stepTests: StepTestPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ExploreViewModel(repository, steps, addressLookup, stepTests) as T
}
