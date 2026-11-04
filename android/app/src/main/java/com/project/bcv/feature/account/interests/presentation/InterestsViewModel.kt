package com.project.bcv.feature.account.interests.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.bcv.feature.account.interests.domain.InterestRepository
import com.project.bcv.feature.account.interests.domain.PlaceType
import java.io.IOException
import java.util.concurrent.Executors

data class InterestsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val placeTypes: List<PlaceType> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null,
    val saved: Boolean = false
)

class InterestsViewModel(private val repository: InterestRepository) : ViewModel() {
    companion object { const val MAX_INTERESTS = 3 }

    private val executor = Executors.newSingleThreadExecutor()
    private val mutableState = MutableLiveData(InterestsUiState())
    val state: LiveData<InterestsUiState> = mutableState

    init { load() }

    fun load() {
        mutableState.value = (mutableState.value ?: InterestsUiState()).copy(loading = true, error = null)
        executor.execute {
            try {
                val data = repository.load()
                mutableState.postValue(InterestsUiState(loading = false, placeTypes = data.placeTypes, selectedIds = data.selectedIds))
            } catch (error: Exception) {
                mutableState.postValue(InterestsUiState(loading = false, error = message(error)))
            }
        }
    }

    /** Returns false only when adding this group would exceed the selection limit. */
    fun toggle(id: Long): Boolean {
        val current = mutableState.value ?: return true
        if (current.loading || current.saving) return true
        val selected = current.selectedIds.toMutableSet()
        if (id !in selected && selected.size >= MAX_INTERESTS) return false
        if (!selected.add(id)) selected.remove(id)
        mutableState.value = current.copy(selectedIds = selected, error = null)
        return true
    }

    fun save() {
        val current = mutableState.value ?: return
        if (current.loading || current.saving || current.selectedIds.size !in 1..MAX_INTERESTS) return
        mutableState.value = current.copy(saving = true, error = null)
        executor.execute {
            try {
                repository.save(current.selectedIds)
                mutableState.postValue(current.copy(saved = true))
            } catch (error: Exception) {
                mutableState.postValue(current.copy(error = message(error)))
            }
        }
    }

    private fun message(error: Exception) = when (error) {
        is IOException -> "Chưa thể kết nối. Kiểm tra mạng và thử lại."
        else -> error.message ?: "Đã xảy ra lỗi. Vui lòng thử lại."
    }

    override fun onCleared() {
        executor.shutdown()
        super.onCleared()
    }
}

class InterestsViewModelFactory(private val repository: InterestRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = InterestsViewModel(repository) as T
}
