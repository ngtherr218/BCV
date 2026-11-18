package com.project.bcv.feature.engagement

import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.project.bcv.R
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.GOLD_SOFT
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.icon
import com.project.bcv.feature.engagement.EngagementUi.leadingIcon
import com.project.bcv.feature.engagement.EngagementUi.number
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.text
import com.project.bcv.feature.engagement.data.EngagementApi
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * "Bảng vàng": the top 3 of every settled week, newest first, as small podiums. Opens on one board
 * and switches between Khám phá, Vận động and Giữ chuỗi (the user's own level) inside the sheet.
 */
internal object GoldBoardSheet {
    private val BROWN = 0xFF8A6418.toInt()
    private val CREAM = 0xFFFFFBF0.toInt()
    private val CREAM_LINE = 0xFFF0E2BF.toInt()
    private val DAY = DateTimeFormatter.ofPattern("d/M")

    /** Ring, pedestal and ink per rank: gold, silver, bronze. */
    private fun colors(rank: Int) = when (rank) {
        1 -> Triple(0xFFE9C14E.toInt(), 0xFFF8E7B5.toInt(), 0xFF7A5512.toInt())
        2 -> Triple(0xFFC9D3DE.toInt(), 0xFFE7ECF1.toInt(), 0xFF4E5B6A.toInt())
        else -> Triple(0xFFE7B9A3.toInt(), 0xFFF5DDD0.toInt(), 0xFF7E432A.toInt())
    }

    fun show(activity: AppCompatActivity, api: EngagementApi, startScope: String) = with(activity) {
        val sheet = BottomSheetDialog(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(28))
        }
        // Drag handle.
        content.addView(View(this).apply { background = rounded(0xFFDCD6CC.toInt(), 4, null, context) },
            LinearLayout.LayoutParams(dp(36), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL })

        // Header: gold medallion, title, close.
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(FrameLayout(context).apply {
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(0xFFF6D98B.toInt(), 0xFFC9962B.toInt())).apply { shape = GradientDrawable.OVAL }
                addView(icon(R.drawable.ic_trophy, 0xFF7A5512.toInt(), 24), FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
            }, LinearLayout.LayoutParams(dp(46), dp(46)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text("Bảng vàng", 20f, FOREST, true))
                addView(text("Top 3 của từng tuần đã chốt", 13f, MUTED))
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
            addView(FrameLayout(context).apply {
                isClickable = true; isFocusable = true; contentDescription = "Đóng"
                background = rounded(0xFFF1EEEA.toInt(), 20, null, context)
                addView(icon(R.drawable.ic_close, MUTED, 20), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
                setOnClickListener { sheet.dismiss() }
            }, LinearLayout.LayoutParams(dp(40), dp(40)))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        val tabs = FrameLayout(this)
        content.addView(tabs, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        val stat = text(" ", 14f, BROWN, true).leadingIcon(R.drawable.ic_m3_workspace_premium, 0xFFC9962B.toInt(), 18, 8).apply {
            background = rounded(GOLD_SOFT, 14, null, context); setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        content.addView(stat, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val weeks = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(weeks, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })

        var shown = ""
        fun load(scope: String) {
            shown = scope
            tabs.removeAllViews()
            tabs.addView(LeaderboardViews.segmented(this, scope) { load(it) })
            // Keep the previous board on screen (dimmed) until the new one arrives, so nothing jumps.
            if (weeks.childCount == 0) stat.text = "Đang tải…"
            weeks.animate().alpha(0.4f).setDuration(120).start()
            lifecycleScope.launch {
                val result = runCatching { api.leaderboardHistory(scope) }
                if (shown != scope) return@launch // a newer tab was picked meanwhile
                weeks.animate().cancel(); weeks.alpha = 1f
                weeks.removeAllViews()
                // The sheet fits its content but never shrinks while open: switching to a board with
                // fewer weeks keeps the height, so the top edge doesn't jump.
                content.post { content.minimumHeight = maxOf(content.minimumHeight, content.height) }
                result.onFailure {
                    stat.text = "Chưa tải được Bảng vàng"
                    weeks.addView(text(it.message ?: "Kiểm tra kết nối rồi thử lại.", 14f, MUTED).apply {
                        gravity = Gravity.CENTER; setPadding(0, dp(24), 0, dp(24)) }, LinearLayout.LayoutParams(-1, -2))
                }.onSuccess { data ->
                    val mine = data.optInt("myPodiums")
                    stat.text = if (mine > 0) "Bạn đã lên bục $mine lần" else "Bạn chưa lên bục lần nào"
                    val list = data.optJSONArray("weeks")
                    if (list == null || list.length() == 0) {
                        weeks.addView(empty(this@with, data.optString("boardName")), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
                        return@onSuccess
                    }
                    for (i in 0 until list.length())
                        weeks.addView(week(this@with, list.getJSONObject(i), latest = i == 0),
                            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
                }
            }
        }
        load(if (startScope in setOf("activity", "streak")) startScope else "knowledge")

        // The rounded white card is the sheet's own content: Material flattens its container's corners
        // once the sheet is fully expanded, so the container stays transparent.
        sheet.setContentView(ScrollView(this).apply {
            addView(content)
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt()); val r = dp(28).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            clipToOutline = true
        })
        sheet.setOnShowListener {
            sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        sheet.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED; skipCollapsed = true
        }
        sheet.show()
    }

    /** Nothing settled yet: say when the first week will appear. */
    private fun empty(activity: AppCompatActivity, boardName: String) = with(activity) {
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(CREAM, 18, CREAM_LINE, context)
            setPadding(dp(20), dp(24), dp(20), dp(24))
            addView(FrameLayout(context).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFFF3ECDD.toInt()) }
                addView(icon(R.drawable.ic_trophy, 0xFFC9B48A.toInt(), 32), FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER))
            }, LinearLayout.LayoutParams(dp(64), dp(64)))
            addView(text("Chưa có thông tin", 16f, FOREST, true).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            addView(text("Bảng $boardName chưa có tuần nào được chốt. Bảng chốt lúc 0h thứ Hai; lọt top 3 tuần này để có tên ở đây!",
                14f, MUTED).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        }
    }

    /** One week: its dates, then a small 2 · 1 · 3 podium with avatar, name, result and tickets. */
    private fun week(activity: AppCompatActivity, week: JSONObject, latest: Boolean) = with(activity) {
        val start = LocalDate.parse(week.getString("weekStart"))
        val end = LocalDate.parse(week.getString("weekEnd"))
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(CREAM, 18, CREAM_LINE, context)
            clipToOutline = true // the pedestals follow the card's rounded bottom
            setPadding(dp(12), dp(12), dp(12), 0)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), 0, dp(4), 0)
                addView(text("Tuần ${start.format(DAY)} – ${end.format(DAY)}", 14f, FOREST, true),
                    LinearLayout.LayoutParams(0, -2, 1f))
                if (latest) addView(text("Mới nhất", 12f, BROWN, true).apply {
                    background = rounded(GOLD_SOFT, 10, null, context); setPadding(dp(8), dp(2), dp(8), dp(2))
                })
            })
            val top = week.getJSONArray("top")
            val byRank = (0 until top.length()).map(top::getJSONObject).associateBy { it.optInt("rank") }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
                listOf(2 to 26, 1 to 40, 3 to 16).forEach { (rank, pedestal) ->
                    addView(column(activity, byRank[rank], rank, pedestal), LinearLayout.LayoutParams(0, -2, 1f))
                }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        }
    }

    private fun column(activity: AppCompatActivity, row: JSONObject?, rank: Int, pedestal: Int) = with(activity) {
        val (ring, base, ink) = colors(rank)
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            setPadding(dp(3), 0, dp(3), 0)
            if (rank == 1) addView(icon(R.drawable.ic_m3_workspace_premium, ring, 20), LinearLayout.LayoutParams(dp(20), dp(20)))
            val size = if (rank == 1) 52 else 44
            addView(LeaderboardViews.avatar(context, lifecycleScope, row, ring, size), LinearLayout.LayoutParams(dp(size), dp(size)))
            val isMe = row?.optBoolean("isMe") == true
            addView(text(row?.optString("name")?.let { if (isMe) "$it (bạn)" else it } ?: "—", 12f, FOREST, isMe).apply {
                gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; setPadding(0, dp(6), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
            addView(text(row?.let { "${number(it.optInt("value"))} ${it.optString("unit")}" } ?: " ", 12f, MUTED).apply {
                gravity = Gravity.CENTER; maxLines = 1
            }, LinearLayout.LayoutParams(-1, -2))
            val quantity = row?.optInt("quantity") ?: 0
            addView(text(if (quantity > 0) "+$quantity" else " ", 12f, BROWN, true).apply {
                if (quantity > 0) {
                    leadingIcon(LeaderboardViews.itemIcon(row?.optString("itemType")), BROWN, 12, 3)
                    background = rounded(GOLD_SOFT, 10, null, context); setPadding(dp(7), dp(2), dp(7), dp(2))
                    contentDescription = "$quantity ${row?.optString("itemName").orEmpty().lowercase()}"
                }
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4); bottomMargin = dp(6) })
            addView(text("$rank", 14f, ink, true).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(base); val r = dp(10).toFloat()
                    cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                }
            }, LinearLayout.LayoutParams(-1, dp(pedestal + 8)))
        }
    }
}
