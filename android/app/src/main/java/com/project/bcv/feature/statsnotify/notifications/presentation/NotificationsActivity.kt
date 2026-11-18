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
    private var shown: NotificationPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setUpDetailScreen(binding.root)
        binding.topBar.screenTitle.text = "Thông báo"
        binding.topBar.backButton.setOnClickListener { finish() }
        model = ViewModelProvider(this, viewModelFactory { NotificationsViewModel(StatsNotifyModule.notifications(this)) })[
            NotificationsViewModel::class.java]
        binding.markAllReadButton.setOnClickListener { model.markAllRead() }
        binding.deleteAllButton.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Xóa tất cả thông báo?")
                .setMessage("Tất cả thông báo hiện tại sẽ được ẩn khỏi danh sách. Thông báo mới vẫn xuất hiện bình thường.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa tất cả") { _, _ -> model.deleteAll() }
                .show()
        }
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
        unreadChip.text = when {
            page.items.isEmpty() -> "Thông báo"
            page.unread > 0 -> "${page.unread} chưa đọc"
            else -> "Đã đọc hết"
        }
        hint.visibility = if (page.items.isEmpty()) View.VISIBLE else View.GONE
        hint.text = "Chưa có thông báo nào.\nHoàn thành hành trình, nhiệm vụ hay check-in sẽ hiện ở đây."
        markAllReadButton.visibility = if (page.unread > 0) View.VISIBLE else View.GONE
        deleteAllButton.visibility = if (page.items.isNotEmpty()) View.VISIBLE else View.GONE
        val scrollY = content.scrollY
        notificationList.removeAllViews()
        val density = resources.displayMetrics.density
        page.items.groupBy { dayLabel(it.createdAt) }.forEach { (day, items) ->
            notificationList.addView(android.widget.TextView(this@NotificationsActivity).apply {
                text = day; textSize = 12f; setTextColor(0xFF7C9086.toInt()); letterSpacing = 0.06f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding((4 * density).toInt(), (18 * density).toInt(), 0, (8 * density).toInt())
            })
            val group = android.widget.LinearLayout(this@NotificationsActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.journey_card_bg)
                clipToOutline = true
            }
            items.forEachIndexed { index, notice ->
                if (index > 0) group.addView(View(this@NotificationsActivity).apply { setBackgroundColor(0xFFE8EEE6.toInt()) },
                    android.widget.LinearLayout.LayoutParams(-1, 1).apply { marginStart = (62 * density).toInt() })
                group.addView(row(notice, group).root)
            }
            notificationList.addView(group)
        }
        if (first) ScreenMotion.reveal(content) else content.post { content.scrollTo(0, scrollY) }
    }

    private val hourFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    /** "HÔM NAY", "HÔM QUA" or the date, for the group headers. */
    private fun dayLabel(date: java.util.Date?): String {
        if (date == null) return "TRƯỚC ĐÓ"
        val day = java.util.Calendar.getInstance().apply { time = date }
        val today = java.util.Calendar.getInstance()
        fun same(a: java.util.Calendar, b: java.util.Calendar) =
            a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) && a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
        if (same(day, today)) return "HÔM NAY"
        today.add(java.util.Calendar.DAY_OF_YEAR, -1)
        if (same(day, today)) return "HÔM QUA"
        return dayFormat.format(date)
    }

    private fun row(notice: AppNotification, parent: android.view.ViewGroup): ItemNotificationBinding {
        val item = ItemNotificationBinding.inflate(layoutInflater, parent, false)
        val teal = ContextCompat.getColor(this, R.color.teal)
        // One Material Symbols icon and colour per kind of event.
        val (icon, accent) = when (notice.type) {
            "JOURNEY_COMPLETED" -> R.drawable.ic_m3_flag to teal
            "PLACE_CHECKIN" -> R.drawable.ic_m3_location_on to 0xFFC0473A.toInt()
            "CARD_RECEIVED" -> R.drawable.ic_m3_style to 0xFF6B4FA3.toInt()
            "PLACE_MASTERED" -> R.drawable.ic_m3_workspace_premium to 0xFFB07C1E.toInt()
            "QUEST_COMPLETED" -> R.drawable.ic_task_alt to 0xFFA0702E.toInt()
            "POINTS_RECEIVED" -> R.drawable.ic_m3_star to 0xFFC9962B.toInt()
            else -> R.drawable.ic_backpack_outline to 0xFF3C7DB0.toInt()
        }
        item.icon.setImageResource(icon)
        item.icon.imageTintList = ColorStateList.valueOf(accent)
        item.iconFrame.backgroundTintList = ColorStateList.valueOf((accent and 0x00FFFFFF) or 0x1F000000)
        item.title.text = notice.title
        item.body.text = notice.content.replace(Regex("\\s*\\n\\s*\\n\\s*"), " ").trim()
        item.time.text = notice.createdAt?.let(hourFormat::format).orEmpty()
        item.title.setTypeface(null, if (notice.read) android.graphics.Typeface.NORMAL else android.graphics.Typeface.BOLD)
        item.unreadDot.visibility = if (notice.read) View.GONE else View.VISIBLE
        item.unreadDot.backgroundTintList = ColorStateList.valueOf(accent)
        if (!notice.read) item.root.setBackgroundColor(0xFFF5F9F4.toInt())
        // The full text is always shown; a tap only marks it read (same height, so nothing moves).
        if (!notice.read) item.root.setOnClickListener { model.markRead(notice.id) }
        else { item.root.isClickable = false; item.root.background = null }
        item.root.contentDescription = "${notice.title}. ${notice.content}. ${item.time.text}. ${if (notice.read) "Đã đọc" else "Chưa đọc"}"
        return item
    }
}
