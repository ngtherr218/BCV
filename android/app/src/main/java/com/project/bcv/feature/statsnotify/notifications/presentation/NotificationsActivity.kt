package com.project.bcv.feature.statsnotify.notifications.presentation

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.R
import com.project.bcv.core.di.StatsNotifyModule
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.viewModelFactory
import com.project.bcv.core.ui.*
import com.project.bcv.databinding.ActivityNotificationsBinding
import com.project.bcv.databinding.ItemNotificationBinding
import com.project.bcv.feature.statsnotify.notifications.domain.AppNotification
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationPage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Inbox for recorded events. Reading keeps the complete message in the list. */
class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var model: NotificationsViewModel
    private val locale = Locale.forLanguageTag("vi-VN")
    private var shown: NotificationPage? = null
    private var unreadOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setUpDetailScreen(binding.root)
        binding.topBar.screenTitle.text = "Thông báo"
        binding.topBar.backButton.setOnClickListener { finish() }
        unreadOnly = savedInstanceState?.getBoolean("unreadOnly") ?: false
        binding.filters.check(if (unreadOnly) R.id.unreadFilter else R.id.allFilter)
        binding.filters.addOnButtonCheckedListener { _, id, checked ->
            if (checked) { unreadOnly = id == R.id.unreadFilter; renderList() }
        }
        binding.showAllButton.setOnClickListener { binding.filters.check(R.id.allFilter) }
        binding.moreButton.setOnClickListener { showOptions() }
        model = ViewModelProvider(this, viewModelFactory { NotificationsViewModel(StatsNotifyModule.notifications(this)) })[
            NotificationsViewModel::class.java]
        binding.state.stateRetry.setOnClickListener { model.load() }
        model.state.observe(this) { state ->
            if (state != Remote.Loading) Skeleton.hide(binding.skeleton.root)
            when (state) {
                Remote.Loading -> if (shown == null) Skeleton.show(binding.skeleton.root, binding.content, binding.state.root)
                is Remote.Failed -> if (shown == null) binding.state.showError(binding.content, state.message)
                    else Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).setAction("Thử lại") { model.load() }.show()
                Remote.SessionExpired -> openSignIn()
                is Remote.Ready -> {
                    val first = shown == null
                    shown = state.value
                    binding.state.showContent(binding.content)
                    renderList()
                    if (first) ScreenMotion.reveal(binding.content)
                }
            }
        }
        model.message.observe(this) { event -> event.take()?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("unreadOnly", unreadOnly)
        super.onSaveInstanceState(outState)
    }

    private fun showOptions() {
        val page = shown
        PopupMenu(this, binding.moreButton).apply {
            menu.add(0, 1, 0, "Đánh dấu tất cả đã đọc").isEnabled = (page?.unread ?: 0) > 0
            menu.add(0, 2, 1, "Làm mới")
            menu.add(0, 3, 2, "Ẩn tất cả thông báo").isEnabled = page?.items?.isNotEmpty() == true
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> model.markAllRead()
                    2 -> model.load()
                    3 -> MaterialAlertDialogBuilder(this@NotificationsActivity)
                        .setTitle("Ẩn tất cả thông báo?")
                        .setMessage("Thông báo hiện tại sẽ được ẩn khỏi danh sách trên thiết bị này. Thông báo mới vẫn xuất hiện; điểm và phần thưởng của bạn được giữ nguyên.")
                        .setNegativeButton("Giữ lại", null)
                        .setPositiveButton("Ẩn thông báo") { _, _ -> model.deleteAll() }.show()
                }
                true
            }
            show()
        }
    }

    private fun dayLabel(date: Date?): String {
        if (date == null) return "Trước đó"
        fun day(value: Date) = Calendar.getInstance().apply { time = value }.let { it.get(Calendar.YEAR) to it.get(Calendar.DAY_OF_YEAR) }
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        return when (day(date)) {
            day(Date()) -> "Hôm nay"
            day(yesterday) -> "Hôm qua"
            else -> SimpleDateFormat("dd/MM/yyyy", locale).format(date)
        }
    }

    private fun renderList() = with(binding) {
        val page = shown ?: return@with
        unreadFilter.text = if (page.unread > 0) "Chưa đọc (${page.unread})" else "Chưa đọc"
        unreadChip.text = if (page.unread > 0) "${page.unread} thông báo chưa đọc · Chạm để đánh dấu đã đọc" else "Bạn đã đọc hết thông báo"
        val items = page.items.filter { !unreadOnly || !it.read }.sortedByDescending { it.createdAt?.time ?: Long.MIN_VALUE }
        notificationList.removeAllViews()
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        emptyTitle.text = if (unreadOnly) "Bạn đã xem hết tin mới" else "Chưa có thông báo"
        emptyBody.text = if (unreadOnly) "Các thông báo đã đọc vẫn được giữ trong mục Tất cả." else "Khi bạn hoàn thành hành trình, nhiệm vụ hoặc nhận vật phẩm, thông báo sẽ xuất hiện ở đây."
        showAllButton.visibility = if (unreadOnly && page.items.isNotEmpty()) View.VISIBLE else View.GONE
        unreadChip.visibility = if (page.items.isEmpty()) View.GONE else View.VISIBLE
        var previousDay: String? = null
        items.forEach { notice ->
            val day = dayLabel(notice.createdAt)
            if (day != previousDay) {
                notificationList.addView(TextView(this@NotificationsActivity).apply {
                    text = day; textSize = 13f; setTextColor(0xFF62786D.toInt()); setTypeface(typeface, Typeface.BOLD)
                    val density = resources.displayMetrics.density
                    setPadding(0, (20*density).toInt(), 0, (2*density).toInt())
                    androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
                })
                previousDay = day
            }
            notificationList.addView(row(notice).root)
        }
    }

    private fun row(notice: AppNotification): ItemNotificationBinding {
        val item = ItemNotificationBinding.inflate(layoutInflater, binding.notificationList, false)
        val teal = ContextCompat.getColor(this, R.color.teal)
        val (icon, accent, tint) = when (notice.type) {
            "JOURNEY_COMPLETED" -> Triple(R.drawable.ic_check_circle, teal, 0xFFE8F1E9.toInt())
            "QUEST_COMPLETED" -> Triple(R.drawable.ic_task_alt, 0xFF936829.toInt(), 0xFFF6EFDF.toInt())
            else -> Triple(R.drawable.ic_backpack_outline, 0xFF3C7DB0.toInt(), 0xFFEAF1F7.toInt())
        }
        item.icon.setImageResource(icon)
        item.icon.imageTintList = ColorStateList.valueOf(accent)
        item.icon.backgroundTintList = ColorStateList.valueOf(tint)
        item.title.text = notice.title
        item.title.setTypeface(item.title.typeface, if (notice.read) Typeface.NORMAL else Typeface.BOLD)
        item.body.text = notice.content
        item.time.text = notice.createdAt?.let { SimpleDateFormat("HH:mm · dd/MM", locale).format(it) }.orEmpty()
        item.root.backgroundTintList = ColorStateList.valueOf(if (notice.read) 0xFFFFFFFF.toInt() else 0xFFF0F6F1.toInt())
        item.unreadDot.visibility = if (notice.read) View.GONE else View.VISIBLE
        item.unreadDot.backgroundTintList = ColorStateList.valueOf(teal)
        item.readState.text = if (notice.read) "Đã đọc" else "Chưa đọc"
        item.readState.setTextColor(if (notice.read) 0xFF708277.toInt() else teal)
        item.root.isClickable = !notice.read
        item.root.isFocusable = !notice.read
        if (!notice.read) item.root.setOnClickListener { model.markRead(notice.id) }
        item.root.contentDescription = "${notice.title}. ${notice.content}. ${item.time.text}. ${item.readState.text}${if (!notice.read) ". Chạm để đánh dấu đã đọc" else ""}"
        return item
    }
}
