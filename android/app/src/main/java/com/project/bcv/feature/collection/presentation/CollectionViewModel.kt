package com.project.bcv.feature.collection.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.ui.TabRefresh
import com.project.bcv.feature.collection.domain.CollectionOverview
import com.project.bcv.feature.collection.domain.CollectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    data class Loaded(val overview: CollectionOverview, val refreshError: String? = null) : CollectionUiState
    data class Failed(val message: String) : CollectionUiState
    data object SessionExpired : CollectionUiState
}

class CollectionViewModel(private val repository: CollectionRepository,
    private val freshness: TabRefresh = TabRefresh()) : ViewModel() {
    private val mutableState = MutableLiveData<CollectionUiState>(CollectionUiState.Loading)
    val state: LiveData<CollectionUiState> = mutableState

    private var loading = false
    var scrollY = 0

    init { refreshIfNeeded() }

    fun refreshIfNeeded() = load(force = false)

    fun load(force: Boolean = true) {
        if (loading) return
        val previous = mutableState.value as? CollectionUiState.Loaded
        if (!freshness.shouldRefresh(previous != null, force)) return
        loading = true
        if (previous == null) mutableState.value = CollectionUiState.Loading
        viewModelScope.launch {
            val next = try {
                val overview = withContext(Dispatchers.IO) { repository.load() }
                freshness.loaded()
                CollectionUiState.Loaded(overview)
            } catch (error: ApiFailure) {
                if (error.code == "UNAUTHORIZED") CollectionUiState.SessionExpired
                else previous?.copy(refreshError = error.message) ?: CollectionUiState.Failed(error.message)
            } catch (error: IOException) {
                Log.w(TAG, "Collections could not reach the API", error)
                val message = if (previous == null) "Không kết nối được máy chủ. Kiểm tra kết nối rồi thử lại."
                    else "Chưa cập nhật được. Dữ liệu đã tải vẫn được giữ; kiểm tra kết nối rồi thử lại."
                previous?.copy(refreshError = message) ?: CollectionUiState.Failed(message)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e(TAG, "Collections load failed", error)
                val message = "Chưa cập nhật được bộ sưu tập. Vui lòng thử lại."
                previous?.copy(refreshError = message) ?: CollectionUiState.Failed(message)
            } finally {
                loading = false
            }
            if (next != mutableState.value) mutableState.value = next
        }
    }

    private companion object { const val TAG = "CollectionViewModel" }
}

class CollectionViewModelFactory(private val repository: CollectionRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CollectionViewModel(repository) as T
}
