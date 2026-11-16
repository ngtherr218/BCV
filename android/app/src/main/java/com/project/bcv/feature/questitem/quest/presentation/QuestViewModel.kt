package com.project.bcv.feature.questitem.quest.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.ui.TabRefresh
import com.project.bcv.feature.questitem.quest.domain.QuestOverview
import com.project.bcv.feature.questitem.quest.domain.QuestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface QuestUiState {
    data object Loading : QuestUiState
    data class Loaded(val overview: QuestOverview, val refreshError: String? = null) : QuestUiState
    data class Failed(val message: String) : QuestUiState
    data object SessionExpired : QuestUiState
}

class QuestViewModel(private val repository: QuestRepository,
    private val freshness: TabRefresh = TabRefresh()) : ViewModel() {
    private val mutableState = MutableLiveData<QuestUiState>(QuestUiState.Loading)
    val state: LiveData<QuestUiState> = mutableState

    private var loading = false
    var weeklySelected = false
    var scrollY = 0

    init { refreshIfNeeded() }

    fun refreshIfNeeded() = load(force = false)

    fun load(force: Boolean = true) {
        if (loading) return
        val previous = mutableState.value as? QuestUiState.Loaded
        if (!freshness.shouldRefresh(previous != null, force)) return
        loading = true
        if (previous == null) mutableState.value = QuestUiState.Loading
        viewModelScope.launch {
            val next = try {
                val overview = withContext(Dispatchers.IO) { repository.load() }
                freshness.loaded()
                // Server timestamps alone do not mean the visible quest progress changed.
                val unchanged = previous?.overview?.let { it.daily == overview.daily && it.weekly == overview.weekly } == true
                QuestUiState.Loaded(if (unchanged) previous!!.overview else overview)
            } catch (error: ApiFailure) {
                if (error.code == "UNAUTHORIZED") QuestUiState.SessionExpired
                else previous?.copy(refreshError = error.message) ?: QuestUiState.Failed(error.message)
            } catch (error: IOException) {
                Log.w(TAG, "Quests could not reach the API", error)
                val message = if (previous == null) "Không kết nối được máy chủ. Kiểm tra kết nối rồi thử lại."
                    else "Chưa cập nhật được. Dữ liệu đã tải vẫn được giữ; kiểm tra kết nối rồi thử lại."
                previous?.copy(refreshError = message) ?: QuestUiState.Failed(message)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e(TAG, "Quests load failed", error)
                val message = "Chưa cập nhật được nhiệm vụ. Vui lòng thử lại."
                previous?.copy(refreshError = message) ?: QuestUiState.Failed(message)
            } finally {
                loading = false
            }
            if (next != mutableState.value) mutableState.value = next
        }
    }

    private companion object { const val TAG = "QuestViewModel" }
}

class QuestViewModelFactory(private val repository: QuestRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = QuestViewModel(repository) as T
}
