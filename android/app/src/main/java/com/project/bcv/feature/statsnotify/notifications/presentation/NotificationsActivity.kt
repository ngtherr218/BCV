package com.project.bcv.feature.statsnotify.notifications.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.R
import androidx.lifecycle.ViewModelProvider
import com.project.bcv.core.di.StatsNotifyModule
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.viewModelFactory
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.core.ui.openSignIn
import com.project.bcv.core.ui.setUpDetailScreen
import com.project.bcv.core.ui.showContent
import com.project.bcv.core.ui.showError
import com.project.bcv.databinding.ActivityNotificationsBinding
import com.project.bcv.databinding.ItemNotificationBinding
import com.project.bcv.feature.statsnotify.notifications.domain.AppNotification
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationPage
import java.text.SimpleDateFormat
import java.util.Locale

/** Recorded events (journey finished, quest done, item received); opening one marks it read. */
class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var model: NotificationsViewModel
    private val timeFormat = SimpleDateFormat("dd/MM · HH:mm", Locale.US)
    private var shown: NotificationPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setUpDetailScreen(binding.root)
        binding.topBar.screenTitle.text = "Thông báo"
        binding.topBar.backButton.setOnClickListener { finish() }
        model = ViewModelProvider(this, viewModelFactory { NotificationsViewModel(StatsNotifyModule.notifications(this)) })[
            NotificationsViewModel::class.java]
        binding.state.stateRetry.setOnClickListener { model.load() }
        model.state.observe(this) { state ->
            if (state != Remote.Loading) Skeleton.hide(binding.skeleton.root)
            when (state) {
                Remote.Loading -> if (shown == null)
                    Skeleton.show(binding.skeleton.root, binding.content, binding.state.root)
                is Remote.Failed -> if (shown == null) binding.state.showError(binding.content, state.message)
                Remote.SessionExpired -> openSignIn()
                is Remote.Ready -> show(state.value)
            }
        }
        model.message.observe(this) { event ->
            event.take()?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun show(page: NotificationPage) = with(binding) {
        val first = shown == null
        shown = page
        state.showContent(content)
        unreadChip.text = if (page.unread > 0) "${page.unread} chưa đọc" else "Đã đọc hết"
        hint.text = if (page.items.isEmpty()) "Chưa có thông báo. Hoàn thành hành trình hoặc nhiệm vụ để nhận tin mới."
            else "Mở một thông báo để chuyển sang đã đọc."
        notificationList.removeAllViews()
        page.items.forEach { notificationList.addView(row(it).root) }
        if (first) ScreenMotion.reveal(content)
    }

    private fun row(notice: AppNotification): ItemNotificationBinding {
        val item = ItemNotificationBinding.inflate(layoutInflater, binding.notificationList, false)
        val teal = ContextCompat.getColor(this, R.color.teal)
        val (icon, accent, tint) = when (notice.type) {
            "JOURNEY_COMPLETED" -> Triple(R.drawable.ic_check_circle, teal, 0xFFE8F0E2.toInt())
            "QUEST_COMPLETED" -> Triple(R.drawable.ic_task_alt, 0xFFA0702E.toInt(), 0xFFF3EBDD.toInt())
            else -> Triple(R.drawable.ic_backpack_outline, 0xFF3C7DB0.toInt(), 0xFFE6EDF0.toInt())
        }
        item.icon.setImageResource(icon)
        item.icon.imageTintList = ColorStateList.valueOf(accent)
        item.title.text = notice.title
        item.body.text = notice.content
        item.time.text = notice.createdAt?.let(timeFormat::format).orEmpty()
        if (notice.read) {
            item.root.setBackgroundResource(R.drawable.journey_card_bg)
            item.icon.backgroundTintList = ColorStateList.valueOf(0xFFF1F4EF.toInt())
            item.unreadDot.visibility = View.GONE
            item.readState.text = "Đã đọc"
            item.readState.setTextColor(0xFF7C9086.toInt())
        } else {
            item.root.setBackgroundResource(R.drawable.panel_bg)
            item.root.backgroundTintList = ColorStateList.valueOf(tint)
            item.icon.backgroundTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            item.unreadDot.backgroundTintList = ColorStateList.valueOf(accent)
            item.readState.text = "Chưa đọc"
            item.readState.setTextColor(accent)
            item.root.setOnClickListener { model.markRead(notice.id) }
        }
        item.root.contentDescription = "${notice.title}. ${notice.content}. ${item.time.text}. ${item.readState.text}"
        return item
    }
}
