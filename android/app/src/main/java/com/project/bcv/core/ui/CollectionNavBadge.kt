package com.project.bcv.core.ui

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.project.bcv.core.di.CollectionModule
import com.project.bcv.BuildConfig
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.databinding.ViewBottomNavBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Shared by every tab; only the visible screen polls for unseen cards. */
object CollectionNavBadge {
    private const val ATTACHED_TAG = "collection_badge_attached"
    private var sessionKey: String? = null
    private var cachedCount: Int? = null
    private var freshness = TabRefresh()
    private var loading = false
    private var countVersion = 0L

    private fun session(context: Context): String = BuildConfig.API_BASE_URL + ":" + SessionPreferences(context).token

    private fun prepare(context: Context): String {
        val key = session(context)
        if (key != sessionKey) {
            sessionKey = key
            cachedCount = null
            freshness = TabRefresh()
            loading = false
            countVersion++
        }
        return key
    }

    fun attach(nav: ViewBottomNavBinding) {
        if (nav.collectionBadge.tag == ATTACHED_TAG) return
        val owner = owner(nav.root.context) ?: return
        prepare(nav.root.context)
        cachedCount?.let { render(nav, it) }
        nav.collectionBadge.tag = ATTACHED_TAG
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(750) // Let the initial screen and map draw before background badges.
                    update(nav)
                    delay(30_000)
                }
            }
        }
    }

    fun refresh(nav: ViewBottomNavBinding) {
        val owner = owner(nav.root.context) ?: return
        owner.lifecycleScope.launch { update(nav) }
    }

    private suspend fun update(nav: ViewBottomNavBinding) {
        val key = prepare(nav.root.context)
        cachedCount?.let { render(nav, it) }
        if (loading || !freshness.shouldRefresh(cachedCount != null)) return
        loading = true
        val requestFreshness = freshness
        val requestVersion = countVersion
        try {
            val count = withContext(Dispatchers.IO) { CollectionModule.repository(nav.root.context).loadBadgeCount() }
            if (key != session(nav.root.context) || key != sessionKey || requestVersion != countVersion) return
            cachedCount = count
            requestFreshness.loaded()
            render(nav, count)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Keep the last confirmed count until the next successful refresh.
        } finally {
            if (key == sessionKey) loading = false
        }
    }

    fun show(nav: ViewBottomNavBinding, count: Int) {
        prepare(nav.root.context)
        if (cachedCount != count) countVersion++
        cachedCount = count
        render(nav, count)
    }

    private fun render(nav: ViewBottomNavBinding, count: Int) {
        nav.collectionBadge.text = if (count > 99) "99+" else count.toString()
        nav.collectionBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        nav.tabCollection.contentDescription = if (count > 0) "Bộ sưu tập, $count thẻ mới chưa xem" else "Bộ sưu tập"
    }

    private fun owner(context: Context): LifecycleOwner? = when (context) {
        is LifecycleOwner -> context
        is ContextWrapper -> owner(context.baseContext)
        else -> null
    }
}
