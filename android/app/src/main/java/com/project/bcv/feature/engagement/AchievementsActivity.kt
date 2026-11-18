package com.project.bcv.feature.engagement

import com.project.bcv.feature.engagement.EngagementUi.icon
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.bcv.core.ui.NavTab
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.GOLD_SOFT
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.card
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.full
import com.project.bcv.feature.engagement.EngagementUi.number
import com.project.bcv.feature.engagement.EngagementUi.SOFT
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.scaffold
import com.project.bcv.feature.engagement.EngagementUi.text
import kotlinx.coroutines.launch

/**
 * "Xếp hạng" tab (F85): the user's explorer points, then the weekly board by Khám phá, Vận động
 * or Giữ chuỗi — podium for the top 3 (fixed height, so switching never moves the page), the rest
 * below, and the user's own row if further down.
 */
class AchievementsActivity : AppCompatActivity() {
    private lateinit var body: LinearLayout
    private val api by lazy { EngagementApi(this) }
    private var scope = "knowledge"
    private val points by lazy { text("—", 20f, 0xFFFFFFFF.toInt(), true).apply { includeFontPadding = false } }
    /** The top card follows the tab: explorer points, this week's steps, or the longest streak. */
    private val pointsLabel by lazy { text("Điểm khám phá của bạn", 13f, 0xFFE9C46A.toInt(), true) }
    private var totalPoints: Int? = null
    private val tabs by lazy { FrameLayout(this) }
    private val caption by lazy { text("", 12f, MUTED) }
    /** "Your rank" line under the caption: where you stand without scrolling through the top 50. */
    private val myRank by lazy { text(" ", 13f, FOREST, true).apply {
        background = rounded(SOFT, 12, null, context); setPadding(dp(12), dp(8), dp(12), dp(8))
    } }
    private val stage by lazy { FrameLayout(this) }
    private val list by lazy { LinearLayout(this).apply { orientation = LinearLayout.VERTICAL } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        body = scaffold("Xếp hạng", tab = NavTab.RANKING)
        body.full(pointsCard(), 4)
        body.full(tabs, 16)
        body.full(caption, 10)
        body.full(myRank, 8)
        body.addView(stage, LinearLayout.LayoutParams(-1, dp(270)).apply { topMargin = dp(8) })
        body.full(list, 12)
        show(scope)
    }

    /** Your points as one slim row: small gold medallion, label, number on the right. */
    private fun pointsCard() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF22594A.toInt(), 0xFF153B31.toInt())).apply { cornerRadius = dp(16).toFloat() }
        setPadding(dp(12), dp(10), dp(16), dp(10))
        // Gold medallion: a drawn star, no emoji.
        addView(android.widget.FrameLayout(context).apply {
            background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFFF6D98B.toInt(), 0xFFC9962B.toInt())).apply { shape = android.graphics.drawable.GradientDrawable.OVAL }
            addView(icon(com.project.bcv.R.drawable.ic_m3_star, 0xFF7A5512.toInt(), 20),
                android.widget.FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(34), dp(34)))
        addView(pointsLabel, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        addView(points)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            runCatching { api.summary() }.onSuccess {
                totalPoints = it.optInt("points")
                if (scope == "knowledge") points.text = number(totalPoints!!)
            }
        }
    }

    private fun show(key: String) {
        scope = key
        tabs.removeAllViews()
        tabs.addView(LeaderboardViews.segmented(this, key) { show(it) })
        stage.removeAllViews()
        stage.addView(LeaderboardViews.podium(this, lifecycleScope, null, loading = true))
        list.removeAllViews()
        caption.text = " "
        myRank.text = " "
        pointsLabel.text = when (key) {
            "activity" -> "Số bước tuần này của bạn"
            "streak" -> "Chuỗi dài nhất của bạn"
            else -> "Điểm khám phá của bạn"
        }
        points.text = if (key == "knowledge") totalPoints?.let(::number) ?: "—" else "—"
        lifecycleScope.launch {
            val data = runCatching { api.leaderboard(key) }
            if (scope != key) return@launch
            stage.removeAllViews()
            data.onFailure {
                stage.addView(LeaderboardViews.podium(this@AchievementsActivity, lifecycleScope, null))
                list.full(text(it.message ?: "Không tải được bảng xếp hạng.", 13f, MUTED))
            }.onSuccess { board ->
                val participants = board.optInt("participants")
                caption.text = "${board.optString("scopeName")} · ${number(participants)} người · " +
                    if (key == "streak") "chuỗi dài nhất từng giữ" else "tuần này"
                val top = board.getJSONArray("top")
                val mine = board.optJSONObject("me")
                // Steps and streak come from the board itself (0 when you are not on it yet).
                if (key != "knowledge") points.text = "${number(mine?.optInt("value") ?: 0)} ${if (key == "streak") "ngày" else "bước"}"
                myRank.text = when {
                    mine == null -> if (key == "streak") "Bạn chưa có chuỗi ngày nào. Đạt mục tiêu bước hôm nay để bắt đầu."
                        else "Bạn chưa có hạng tuần này."
                    else -> "Hạng của bạn: #${number(mine.getInt("rank"))} / ${number(participants)} · " +
                        "${number(mine.getInt("value"))} ${mine.optString("unit")}" +
                        if (mine.getInt("rank") > top.length()) " · ngoài top ${top.length()}" else ""
                }
                stage.addView(LeaderboardViews.podium(this@AchievementsActivity, lifecycleScope, top))
                for (i in 3 until top.length())
                    list.full(LeaderboardViews.rankRow(this@AchievementsActivity, lifecycleScope, top.getJSONObject(i)), if (i == 3) 0 else 8)
                val me = board.optJSONObject("me")
                if (me != null && me.getInt("rank") > top.length()) {
                    list.full(text("⋯", 15f, MUTED).apply { gravity = Gravity.CENTER }, 6)
                    list.full(LeaderboardViews.rankRow(this@AchievementsActivity, lifecycleScope, me), 6)
                }
            }
        }
    }
}
