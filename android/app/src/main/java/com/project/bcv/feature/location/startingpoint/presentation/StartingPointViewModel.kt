package com.project.bcv.feature.location.startingpoint.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.feature.location.startingpoint.domain.AddressLookup
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import com.project.bcv.feature.location.startingpoint.domain.DeviceLocationSource
import com.project.bcv.feature.location.startingpoint.domain.StartingPoint
import com.project.bcv.feature.location.startingpoint.domain.StartingPointRepository
import com.project.bcv.feature.location.startingpoint.domain.StartingPointSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StartingPointStage {
    LOADING, READY, PERMISSION_WAIT, LOCATING, VALID_CURRENT, PERMISSION_DENIED,
    NO_FIX, OUTSIDE_AREA, NO_SUGGESTION, SAVING, SAVE_FAILED, SAVED
}

data class StartingPointUiState(
    val stage: StartingPointStage = StartingPointStage.LOADING,
    val suggested: StartingPoint? = null,
    val current: StartingPoint? = null,
    val saved: StartingPoint? = null,
    val currentCoordinates: Coordinates? = null,
    val currentAddress: String? = null,
    val savedAddress: String? = null,
    val pendingSource: StartingPointSource = StartingPointSource.SUGGESTED,
    val alreadySaved: Boolean = false,
    val message: String? = null
)

class StartingPointViewModel(
    private val repository: StartingPointRepository,
    private val deviceLocation: DeviceLocationSource,
    private val addressLookup: AddressLookup
) : ViewModel() {
    private val mutableState = MutableLiveData(StartingPointUiState())
    val state: LiveData<StartingPointUiState> = mutableState
    private var locationRequestVersion = 0

    fun load() {
        val current = mutableState.value ?: return
        mutableState.value = current.copy(stage = StartingPointStage.LOADING, message = null)
        viewModelScope.launch {
            try {
                val options = withContext(Dispatchers.IO) { repository.load() }
                mutableState.value = if (options.saved != null) {
                    current.copy(stage = StartingPointStage.SAVED, saved = options.saved,
                        suggested = options.suggested, alreadySaved = true)
                } else if (options.suggested != null) {
                    current.copy(stage = StartingPointStage.READY, suggested = options.suggested,
                        message = null)
                } else {
                    current.copy(stage = StartingPointStage.NO_SUGGESTION,
                        message = "Chưa có dữ liệu điểm xuất phát phù hợp.")
                }
            } catch (error: ApiFailure) {
                mutableState.value = current.copy(stage = StartingPointStage.NO_SUGGESTION,
                    message = error.message)
            } catch (_: Exception) {
                mutableState.value = current.copy(stage = StartingPointStage.NO_SUGGESTION,
                    message = "Chưa tải được điểm đề xuất. Kiểm tra mạng và thử lại.")
            }
        }
    }

    fun waitingForPermission() {
        mutableState.value = mutableState.value?.copy(stage = StartingPointStage.PERMISSION_WAIT)
    }

    fun permissionDenied() {
        mutableState.value = mutableState.value?.copy(stage = StartingPointStage.PERMISSION_DENIED)
    }

    fun locate() {
        val current = mutableState.value ?: return
        val version = ++locationRequestVersion
        mutableState.value = current.copy(stage = StartingPointStage.LOCATING, message = null,
            current = null, currentCoordinates = null, currentAddress = null)
        deviceLocation.findCurrent { coordinates ->
            if (version != locationRequestVersion) return@findCurrent
            if (coordinates == null) {
                mutableState.value = mutableState.value?.copy(stage = StartingPointStage.NO_FIX,
                    message = "Chưa nhận được vị trí mới. Hãy bật Vị trí/GPS trên thiết bị và thử lại ở nơi thoáng.")
                return@findCurrent
            }
            mutableState.value = mutableState.value?.copy(currentCoordinates = coordinates)
            viewModelScope.launch {
                val address = withContext(Dispatchers.IO) { addressLookup.describe(coordinates) }
                if (version != locationRequestVersion) return@launch
                mutableState.value = mutableState.value?.copy(currentAddress = address)
            }
            viewModelScope.launch {
                try {
                    val point = withContext(Dispatchers.IO) { repository.resolveCurrent(coordinates) }
                    if (version != locationRequestVersion) return@launch
                    mutableState.value = mutableState.value?.copy(
                        stage = StartingPointStage.VALID_CURRENT,
                        current = point, currentCoordinates = coordinates,
                        pendingSource = StartingPointSource.CURRENT
                    )
                } catch (error: ApiFailure) {
                    if (version != locationRequestVersion) return@launch
                    mutableState.value = mutableState.value?.copy(
                        stage = if (error.code == "OUT_OF_SUPPORTED_AREA")
                            StartingPointStage.OUTSIDE_AREA else StartingPointStage.NO_FIX,
                        message = error.message
                    )
                } catch (_: Exception) {
                    if (version != locationRequestVersion) return@launch
                    mutableState.value = mutableState.value?.copy(stage = StartingPointStage.NO_FIX,
                        message = "Chưa kiểm tra được vị trí. Kiểm tra mạng và thử lại.")
                }
            }
        }
    }

    fun useSuggestion() {
        locationRequestVersion++
        deviceLocation.cancel()
        val current = mutableState.value ?: return
        mutableState.value = if (current.suggested == null) {
            current.copy(stage = StartingPointStage.NO_SUGGESTION)
        } else current.copy(stage = StartingPointStage.READY,
            pendingSource = StartingPointSource.SUGGESTED, message = null)
    }

    fun saveSuggestion() = save(StartingPointSource.SUGGESTED)

    fun saveCurrent() = save(StartingPointSource.CURRENT)

    fun retrySave() {
        val source = mutableState.value?.pendingSource ?: return
        save(source)
    }

    private fun save(source: StartingPointSource) {
        val current = mutableState.value ?: return
        if (current.stage == StartingPointStage.SAVING || current.stage == StartingPointStage.SAVED) return
        if (source == StartingPointSource.SUGGESTED && current.suggested == null) {
            mutableState.value = current.copy(stage = StartingPointStage.NO_SUGGESTION)
            return
        }
        if (source == StartingPointSource.CURRENT &&
            (current.currentCoordinates == null || current.current == null ||
                (current.stage != StartingPointStage.VALID_CURRENT &&
                    !(current.stage == StartingPointStage.SAVE_FAILED && current.pendingSource == source)))) {
            mutableState.value = current.copy(stage = StartingPointStage.NO_FIX)
            return
        }
        mutableState.value = current.copy(stage = StartingPointStage.SAVING, pendingSource = source)
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    repository.save(source, current.currentCoordinates)
                }
                val latest = mutableState.value
                val knownAddress = latest?.currentAddress
                    ?.takeIf { saved.coordinates == latest.currentCoordinates }
                mutableState.value = latest?.copy(stage = StartingPointStage.SAVED,
                    saved = saved, savedAddress = knownAddress, alreadySaved = false, message = null)
                if (knownAddress == null) {
                    val address = withContext(Dispatchers.IO) { addressLookup.describe(saved.coordinates) }
                    if (mutableState.value?.saved == saved) {
                        mutableState.value = mutableState.value?.copy(savedAddress = address)
                    }
                }
            } catch (error: ApiFailure) {
                mutableState.value = mutableState.value?.copy(stage = StartingPointStage.SAVE_FAILED,
                    message = error.message)
            } catch (_: Exception) {
                mutableState.value = mutableState.value?.copy(stage = StartingPointStage.SAVE_FAILED,
                    message = "Chưa lưu được điểm xuất phát. Kiểm tra mạng và thử lại.")
            }
        }
    }

    override fun onCleared() {
        deviceLocation.cancel()
        super.onCleared()
    }
}

class StartingPointViewModelFactory(
    private val repository: StartingPointRepository,
    private val deviceLocation: DeviceLocationSource,
    private val addressLookup: AddressLookup
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        StartingPointViewModel(repository, deviceLocation, addressLookup) as T
}
