package com.project.bcv.core.ui

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.project.bcv.core.di.CollectionModule
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

    fun attach(nav: ViewBottomNavBinding) {
        if (nav.collectionBadge.tag == ATTACHED_TAG) return
        val owner = owner(nav.root.context) ?: return
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
        try {
            val count = withContext(Dispatchers.IO) { CollectionModule.repository(nav.root.context).loadBadgeCount() }
            show(nav, count)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Keep the last confirmed count until the next successful refresh.
        }
    }

    fun show(nav: ViewBottomNavBinding, count: Int) {
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
