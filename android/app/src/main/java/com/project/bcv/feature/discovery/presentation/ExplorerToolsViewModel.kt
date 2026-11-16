package com.project.bcv.feature.discovery.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ExplorerToolsState(val loading: Boolean = false, val result: JSONObject? = null, val error: String? = null)

/** Retains a single read request across Activity recreation; cancelled requests cannot overwrite newer filters. */
class ExplorerToolsViewModel(application: Application) : AndroidViewModel(application) {
    val state = MutableLiveData(ExplorerToolsState())
    val options = MutableLiveData<JSONObject>()
    private val api = JsonApiClient(application, BuildConfig.API_BASE_URL)
    private val session = SessionPreferences(application)
    private var request: Job? = null
    var lastPath: String? = null
        private set

    fun loadOptions() {
        if(options.value != null) return
        viewModelScope.launch {
            try { options.value = withContext(Dispatchers.IO) {
                val token = session.token ?: error("Vui lòng đăng nhập lại.")
                val filters = api.get("api/explore/filters", token)
                val interests = api.get("api/interests", token)
                filters.put("selectedPlaceTypeIds", interests.getJSONArray("selectedPlaceTypeIds"))
            } }
            catch(error: Exception) { if(error is kotlinx.coroutines.CancellationException) throw error; state.value=ExplorerToolsState(error=error.message ?: "Không tải được bộ lọc. Hãy thử lại.") }
        }
    }
    fun load(path: String) {
        request?.cancel()
        lastPath = path
        request = viewModelScope.launch {
            state.value = ExplorerToolsState(loading=true)
            try {
                val json=withContext(Dispatchers.IO) { api.get(path, session.token ?: error("Vui lòng đăng nhập lại.")) }
                state.value=ExplorerToolsState(result=json)
            } catch(error: Exception) {
                if(error is kotlinx.coroutines.CancellationException) throw error
                state.value=ExplorerToolsState(error=error.message ?: "Không tải được dữ liệu. Hãy thử lại.")
            }
        }
    }
}
