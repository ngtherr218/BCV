package com.project.bcv.feature.discovery.presentation

import com.project.bcv.feature.engagement.domain.EngagementRepository
import com.project.bcv.feature.engagement.domain.CheckinResult
import com.project.bcv.core.presentation.Event
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.feature.discovery.domain.ExplorePlaceDetail
import com.project.bcv.feature.discovery.domain.ExploreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaceDetailState(
    val place: ExplorePlaceDetail? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val confirmReplace: Boolean = false,
    val saved: Boolean = false,
    val unavailable: Boolean = false
)

/** A finished check-in attempt; [test] tells a debug check-in apart (shown and retried the same way). */
data class CheckinOutcome(val test: Boolean, val result: Result<CheckinResult>)

class PlaceDetailViewModel(
    private val repository: ExploreRepository,
    private val engagement: EngagementRepository
) : ViewModel() {
    private val mutableState = MutableLiveData(PlaceDetailState())
    val state: LiveData<PlaceDetailState> = mutableState
    private val mutableCheckin = MutableLiveData<Event<CheckinOutcome>>()
    val checkin: LiveData<Event<CheckinOutcome>> = mutableCheckin
    private val mutableHasJournal = MutableLiveData(false)
    /** The journal is for real visits only: the user has a page here once they checked in. */
    val hasJournal: LiveData<Boolean> = mutableHasJournal

    fun checkJournal(placeId: Long) {
        viewModelScope.launch { mutableHasJournal.value = withContext(Dispatchers.IO) { engagement.hasJournal(placeId) } }
    }

    /** Sends the check-in; on success the page and its journal button refresh. */
    fun checkIn(placeId: Long, latitude: Double, longitude: Double, test: Boolean = false) {
        viewModelScope.launch {
            val result = try {
                Result.success(withContext(Dispatchers.IO) { engagement.checkIn(placeId, latitude, longitude, test) })
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            if (result.isSuccess) { refresh(placeId); checkJournal(placeId) }
            mutableCheckin.value = Event(CheckinOutcome(test, result))
        }
    }

    fun load(placeId: Long) {
        mutableState.value = PlaceDetailState()
        viewModelScope.launch {
            try {
                val place = withContext(Dispatchers.IO) { repository.loadPlace(placeId) }
                mutableState.value = PlaceDetailState(place = place, loading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiFailure) {
                val unavailable = error.code in setOf("PLACE_NOT_FOUND", "PLACE_UNAVAILABLE", "NOT_FOUND")
                mutableState.value = PlaceDetailState(loading = false, unavailable = unavailable,
                    error = if (unavailable) "Địa danh này hiện không còn khả dụng. Nội dung có thể đã được ngừng hiển thị."
                    else error.message)
            } catch (error: Exception) {
                mutableState.value = PlaceDetailState(loading = false,
                    error = error.message ?: "Không tải được địa danh.")
            }
        }
    }

    /** Reloads in place: the page stays on screen (no skeleton) and keeps it if the reload fails. */
    fun refresh(placeId: Long) {
        if (mutableState.value?.place == null) return load(placeId)
        viewModelScope.launch {
            try {
                val place = withContext(Dispatchers.IO) { repository.loadPlace(placeId) }
                mutableState.value = mutableState.value!!.copy(place = place, loading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Keep showing what is already there.
            }
        }
    }

    fun select(replaceActive: Boolean = false) {
        val current = mutableState.value ?: return
        val place = current.place ?: return
        if (current.saving) return
        mutableState.value = current.copy(saving = true, error = null, confirmReplace = false)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.createJourney(place.id, replaceActive) }
                mutableState.value = current.copy(saved = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiFailure) {
                mutableState.value = current.copy(saving = false,
                    confirmReplace = error.code == "ACTIVE_JOURNEY_EXISTS",
                    error = if (error.code == "ACTIVE_JOURNEY_EXISTS") null else error.message)
            } catch (error: Exception) {
                mutableState.value = current.copy(saving = false,
                    error = error.message ?: "Không lưu được điểm đến.")
            }
        }
    }

    fun dismissReplace() {
        mutableState.value = mutableState.value?.copy(confirmReplace = false)
    }
}

class PlaceDetailViewModelFactory(
    private val repository: ExploreRepository,
    private val engagement: EngagementRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlaceDetailViewModel(repository, engagement) as T
}
