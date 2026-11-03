package com.project.bcv.feature.statsnotify.notifications.presentation

import android.animation.ObjectAnimator
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.project.bcv.core.di.StatsNotifyModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Poll only while visible; the server remains the authority for unread counts. */
class NotificationBell(
    private val activity: AppCompatActivity,
    private val bell: View,
    private val badge: TextView
) : DefaultLifecycleObserver {
    private val repository = StatsNotifyModule.notifications(activity)
    private var polling: Job? = null
    private var request: Job? = null
    private var animation: ObjectAnimator? = null
    private var unreadIds = emptySet<Long>()
    private var active = false

    init { activity.lifecycle.addObserver(this) }

    override fun onResume(owner: LifecycleOwner) {
        active = true
        polling = activity.lifecycleScope.launch {
            while (isActive) {
                refresh()
                delay(15_000)
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        active = false
        polling?.cancel()
        request?.cancel()
        animation?.cancel()
        bell.rotation = 0f
    }

    fun refresh() {
        if (!active || request?.isActive == true) return
        request = activity.lifecycleScope.launch {
            try {
                val page = withContext(Dispatchers.IO) { repository.notifications() }
                badge.text = if (page.unread > 99) "99+" else page.unread.toString()
                badge.visibility = if (page.unread > 0) View.VISIBLE else View.GONE
                bell.contentDescription = "Thông báo, ${page.unread} chưa đọc"
                val ids = page.items.filter { !it.read }.map { it.id }.toSet()
                if ((ids - unreadIds).isNotEmpty()) {
                    animation?.cancel()
                    bell.pivotX = bell.width / 2f
                    bell.pivotY = bell.height * .3f
                    animation = ObjectAnimator.ofFloat(bell, "rotation", 0f, -18f, 18f, -14f, 14f, -7f, 7f, 0f)
                        .apply { duration = 900; start() }
                }
                unreadIds = ids

            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Keep the last confirmed badge; retry on the next poll or resume.
            }
        }
    }

}
