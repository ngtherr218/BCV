package com.project.bcv.feature.discovery.presentation

import com.project.bcv.feature.engagement.domain.EngagementRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.presentation.Event
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.loadInto
import com.project.bcv.feature.discovery.domain.ExploreRepository
import com.project.bcv.feature.discovery.domain.Recommendations
import com.project.bcv.feature.location.startingpoint.domain.AddressLookup
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What happened after "Chọn điểm đến"; each is shown once. */
sealed interface SelectOutcome {
    data object Saved : SelectOutcome
    data class ConfirmReplace(val placeId: Long, val placeName: String) : SelectOutcome
    data class Failed(val message: String) : SelectOutcome
}

/** What happened after "Không quan tâm"; each is shown once. */
sealed interface DismissOutcome {
    data class Hidden(val placeId: Long, val placeName: String) : DismissOutcome
    data class Failed(val placeId: Long, val message: String) : DismissOutcome
}

class RecommendationViewModel(
    private val repository: ExploreRepository,
    private val addressLookup: AddressLookup,
    private val engagement: EngagementRepository,
    knownAddress: String?
) : ViewModel() {
    private val mutableDismissed = MutableLiveData<Event<DismissOutcome>>()
    val dismissed: LiveData<Event<DismissOutcome>> = mutableDismissed

    /** F74: hides the place from recommendations for 30 days. */
    fun dismiss(placeId: Long, placeName: String) {
        viewModelScope.launch {
            val outcome = try {
                withContext(Dispatchers.IO) { engagement.dismiss(placeId) }
                DismissOutcome.Hidden(placeId, placeName)
            } catch (error: ApiFailure) {
                DismissOutcome.Failed(placeId, error.message)
            } catch (_: java.io.IOException) {
                DismissOutcome.Failed(placeId, "Không kết nối được máy chủ. Kiểm tra Wi-Fi rồi thử lại.")
            }
            mutableDismissed.value = Event(outcome)
        }
    }

    /** Undo: the place comes back, and the list reloads. */
    fun undoDismiss(placeId: Long) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { engagement.undoDismiss(placeId) } }
            refresh()
        }
    }

    private val mutableRecommendations = MutableLiveData<Remote<Recommendations>>()
    val recommendations: LiveData<Remote<Recommendations>> = mutableRecommendations
    private val mutableAddress = MutableLiveData(knownAddress)
    /** Street address of the virtual location, as on Explore; null until known (the region is shown). */
    val address: LiveData<String?> = mutableAddress

    /** Looks the address up when Explore did not pass one along. */
    fun describe(location: Coordinates) {
        if (mutableAddress.value != null) return
        viewModelScope.launch {
            mutableAddress.value = withContext(Dispatchers.IO) { addressLookup.describe(location) }
        }
    }
    private val mutableSavingPlaceId = MutableLiveData<Long?>(null)
    /** The place whose journey is being created; every button is locked meanwhile. */
    val savingPlaceId: LiveData<Long?> = mutableSavingPlaceId
    private val mutableOutcome = MutableLiveData<Event<SelectOutcome>>()
    val outcome: LiveData<Event<SelectOutcome>> = mutableOutcome

    fun load() = loadInto(mutableRecommendations) { repository.loadRecommendations() }

    /** Reloads without the loading state, so the cards on screen stay put while it runs. */
    fun refresh() {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.loadRecommendations() } }
                .onSuccess { mutableRecommendations.value = Remote.Ready(it) }
        }
    }

    fun select(placeId: Long, placeName: String, replaceActive: Boolean = false) {
        if (mutableSavingPlaceId.value != null) return
        mutableSavingPlaceId.value = placeId
        viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) { repository.createJourney(placeId, replaceActive) }
                SelectOutcome.Saved
            } catch (error: ApiFailure) {
                if (error.code == "ACTIVE_JOURNEY_EXISTS") SelectOutcome.ConfirmReplace(placeId, placeName)
                else SelectOutcome.Failed(error.message)
            } catch (_: Exception) {
                SelectOutcome.Failed("Không kết nối được máy chủ. Kiểm tra Wi-Fi rồi thử lại.")
            }
            mutableSavingPlaceId.value = null
            mutableOutcome.value = Event(result)
        }
    }
}
