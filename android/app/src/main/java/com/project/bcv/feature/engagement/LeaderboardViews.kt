package com.project.bcv.feature.engagement

import com.project.bcv.feature.engagement.EngagementUi.icon
import com.project.bcv.feature.engagement.EngagementUi.leadingIcon
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.project.bcv.feature.account.profile.presentation.Avatars
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.SOFT
import com.project.bcv.feature.engagement.EngagementUi.TEAL
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.number
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.text
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Weekly leaderboard pieces shared by Thành tích (podium preview) and the full board screen. */
internal object LeaderboardViews {
    val SCOPES = listOf("knowledge" to "Khám phá", "activity" to "Vận động", "streak" to "Giữ chuỗi")

    private val GOLD_RING = 0xFFE9C14E.toInt()
    private val SILVER_RING = 0xFFC9D3DE.toInt()
    private val BRONZE_RING = 0xFFE7B9A3.toInt()

    /** Segmented control; the selected option is a white pill on a warm grey track. */
    fun segmented(context: Context, selected: String, options: List<Pair<String, String>> = SCOPES,
                  onSelect: (String) -> Unit): LinearLayout = with(context) {
        val track = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(0xFFF1EEEA.toInt(), 18, null, context)
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        options.forEach { (key, label) ->
            val on = key == selected
            track.addView(TextView(this).apply {
                text = label; gravity = Gravity.CENTER; textSize = 15f; maxLines = 1
                setTypeface(typeface, if (on) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (on) 0xFF1E1E1E.toInt() else 0xFF7A716A.toInt())
                background = if (on) rounded(0xFFFFFFFF.toInt(), 14, null, context) else null
                elevation = if (on) dp(1).toFloat() else 0f
                isClickable = true
                contentDescription = label + if (on) ", đang chọn" else ""
                setOnClickListener { if (!on) onSelect(key) }
            }, LinearLayout.LayoutParams(0, dp(42), 1f))
        }
        track
    }

    /**
     * Top 3 on a podium (2 · 1 · 3). Always the same height, empty or loading included,
     * so switching tabs never makes the page jump.
     */
    fun podium(context: Context, scope: LifecycleCoroutineScope, top: JSONArray?, loading: Boolean = false): FrameLayout = with(context) {
        val box = FrameLayout(this)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        // Visual order 2, 1, 3; heights give the podium its steps.
        listOf(1 to 72, 0 to 104, 2 to 56).forEach { (index, pedestal) ->
            row.addView(column(this, scope, top?.optJSONObject(index), index + 1, pedestal),
                LinearLayout.LayoutParams(0, -1, 1f))
        }
        box.addView(row, FrameLayout.LayoutParams(-1, -1))
        if (loading || top == null || top.length() == 0) {
            box.addView(text(if (loading) "Đang tải…" else "Chưa có ai trong bảng tuần này", 13f, MUTED).apply {
                gravity = Gravity.CENTER
                background = rounded(0xCCFFFFFF.toInt(), 12, null, context)
                setPadding(dp(12), dp(6), dp(12), dp(6))
            }, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        }
        box
    }

    private fun column(context: Context, scope: LifecycleCoroutineScope, row: JSONObject?, rank: Int, pedestal: Int): LinearLayout = with(context) {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM }
        val (ring, base, rankColor, valueColor) = when (rank) {
            1 -> listOf(GOLD_RING, 0xFFFDF3D9.toInt(), 0xFF8A6418.toInt(), 0xFF8A6418.toInt())
            2 -> listOf(SILVER_RING, 0xFFEEF1F5.toInt(), 0xFF5D6B7C.toInt(), FOREST)
            else -> listOf(BRONZE_RING, 0xFFF9EAE2.toInt(), 0xFF8E4F32.toInt(), FOREST)
        }
        if (rank == 1) col.addView(icon(com.project.bcv.R.drawable.ic_m3_workspace_premium, GOLD_RING, 24), LinearLayout.LayoutParams(dp(24), dp(24)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        val size = if (rank == 1) 64 else 54
        col.addView(avatar(this, scope, row, ring, size), LinearLayout.LayoutParams(dp(size), dp(size)))
        col.addView(text(row?.optString("name")?.let { if (row.optBoolean("isMe")) "$it (bạn)" else it } ?: "—", 13f, FOREST).apply {
            gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; setPadding(dp(4), dp(6), dp(4), 0)
            if (row?.optBoolean("champion") == true) championMark()
        }, LinearLayout.LayoutParams(-1, -2))
        col.addView(text(row?.let { "${number(it.getInt("value"))} ${it.optString("unit")}" } ?: " ", 15f, valueColor, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(2), 0, dp(6))
        }, LinearLayout.LayoutParams(-1, -2))
        col.addView(text("$rank", 20f, rankColor, true).apply {
            gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(10), 0, 0)
            background = GradientDrawable().apply {
                setColor(base); val r = dp(16).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }, LinearLayout.LayoutParams(-1, dp(pedestal)))
        col
    }

    /** Photo when the user has a public avatar, otherwise their initials on a teal disc. */
    fun avatar(context: Context, scope: LifecycleCoroutineScope, row: JSONObject?, ring: Int, size: Int): FrameLayout = with(context) {
        val frame = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(ring) }
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        val name = row?.optString("name").orEmpty()
        val initials = TextView(this).apply {
            text = if (row == null) "" else Avatars.initials(name).ifBlank { "?" }
            gravity = Gravity.CENTER; textSize = (size / 2.6f); setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (row == null) 0xFFE9EEEA.toInt() else 0xFF2A5D96.toInt()) }
        }
        frame.addView(initials, FrameLayout.LayoutParams(-1, -1))
        row?.optString("avatar")?.takeIf { it.startsWith("https://") }?.let { url ->
            val photo = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFFFFFFFF.toInt()) }
            }
            frame.addView(photo, FrameLayout.LayoutParams(-1, -1))
            scope.launch { Avatars.load(url)?.let(photo::setImageBitmap) ?: run { photo.visibility = View.GONE } }
        }
        frame
    }

    /** Last week's #1 of this board ("Quán quân tuần"): a gold crown before the name. */
    private fun TextView.championMark() {
        leadingIcon(com.project.bcv.R.drawable.ic_m3_workspace_premium, GOLD_RING, 16, 3)
        contentDescription = "$text, quán quân tuần trước"
    }

    /** The icon for a prize item: plane for a flight ticket, runner for speed shoes, backpack otherwise. */
    fun itemIcon(itemType: String?) = when (itemType) {
        "FLIGHT_TICKET" -> com.project.bcv.R.drawable.ic_flight
        "SPEED_SHOES" -> com.project.bcv.R.drawable.ic_directions_run
        else -> com.project.bcv.R.drawable.ic_backpack_outline
    }

    /** A round medal with the rank in it: gold, silver or bronze. */
    fun medal(context: Context, rank: Int, sizeDp: Int): TextView = with(context) {
        val (fill, ink) = when (rank) {
            1 -> 0xFFF6D98B.toInt() to 0xFF7A5512.toInt()
            2 -> 0xFFDDE3EA.toInt() to 0xFF4E5B6A.toInt()
            else -> 0xFFF0CDB9.toInt() to 0xFF7E432A.toInt()
        }
        text("$rank", sizeDp * 0.45f, ink, true).apply {
            gravity = Gravity.CENTER; includeFontPadding = false
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(fill) }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    /** One line of the full board: rank, avatar, name, value. */
    fun rankRow(context: Context, scope: LifecycleCoroutineScope, row: JSONObject): LinearLayout = with(context) {
        val isMe = row.getBoolean("isMe")
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(14), dp(10))
            background = rounded(if (isMe) SOFT else 0xFFFFFFFF.toInt(), 14, if (isMe) TEAL else 0xFFE3EBE3.toInt(), context)
            addView(text(number(row.getInt("rank")), if (row.getInt("rank") >= 100) 13f else 15f, MUTED, true).apply {
                gravity = Gravity.CENTER; maxLines = 1 }, LinearLayout.LayoutParams(dp(36), -2))
            addView(avatar(this@with, scope, row, 0xFFE3EBE3.toInt(), 40), LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginStart = dp(4) })
            addView(text(row.getString("name") + if (isMe) " (bạn)" else "", 15f, FOREST, isMe).apply {
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END; setPadding(dp(12), 0, dp(8), 0)
                if (row.optBoolean("champion")) championMark()
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(text("${number(row.getInt("value"))} ${row.optString("unit")}", 15f, TEAL, true))
        }
    }
}
