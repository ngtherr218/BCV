package com.project.bcv.core.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.project.bcv.BuildConfig
import com.project.bcv.feature.account.auth.data.local.SessionPreferences

/** Primary tab models survive tab Activity replacement, bounded to the current login session. */
object TabViewModels {
    private data class SessionKey(val backend: String, val token: String?)
    private val lock = Any()
    private var key: SessionKey? = null
    private var store: ViewModelStore? = null

    fun provider(context: Context, factory: ViewModelProvider.Factory): ViewModelProvider {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Tab models must be accessed on the main thread." }
        val currentKey = SessionKey(BuildConfig.API_BASE_URL, SessionPreferences(context.applicationContext).token)
        var previous: ViewModelStore? = null
        val currentStore = synchronized(lock) {
            if (key != currentKey || store == null) {
                previous = store
                store = ViewModelStore()
                key = currentKey
            }
            store!!
        }
        previous?.clear()
        val owner = object : ViewModelStoreOwner { override val viewModelStore = currentStore }
        return ViewModelProvider(owner, factory)
    }

    /** Detach immediately; a queued clear only ever touches the detached old store. */
    fun clear() {
        val previous = synchronized(lock) {
            store.also { store = null; key = null }
        } ?: return
        clearDetached(previous)
    }

    /** A background login must not discard models already created for its replacement token. */
    fun clearSession(previousToken: String?) {
        val previous = synchronized(lock) {
            if (key?.token != previousToken) null
            else store.also { store = null; key = null }
        } ?: return
        clearDetached(previous)
    }

    private fun clearDetached(previous: ViewModelStore) {
        if (Looper.myLooper() == Looper.getMainLooper()) previous.clear()
        else Handler(Looper.getMainLooper()).post { previous.clear() }
    }
}
