package com.project.bcv.feature.engagement

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
    private val points by lazy { text("⭐ —", 20f, FOREST, true) }
    private val tabs by lazy { FrameLayout(this) }
    private val caption by lazy { text("", 12f, MUTED) }
    private val stage by lazy { FrameLayout(this) }
    private val list by lazy { LinearLayout(this).apply { orientation = LinearLayout.VERTICAL } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        body = scaffold("Xếp hạng", tab = NavTab.RANKING)
        body.full(card(color = GOLD_SOFT).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(text("Điểm khám phá của bạn", 13f, MUTED), LinearLayout.LayoutParams(0, -2, 1f))
            addView(points)
        }, 4)
        body.full(tabs, 16)
        body.full(caption, 10)
        body.addView(stage, LinearLayout.LayoutParams(-1, dp(270)).apply { topMargin = dp(8) })
        body.full(list, 12)
        show(scope)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            runCatching { api.summary() }.onSuccess { points.text = "⭐ ${number(it.optInt("points"))}" }
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
        lifecycleScope.launch {
            val data = runCatching { api.leaderboard(key) }
            if (scope != key) return@launch
            stage.removeAllViews()
            data.onFailure {
                stage.addView(LeaderboardViews.podium(this@AchievementsActivity, lifecycleScope, null))
                list.full(text(it.message ?: "Không tải được bảng xếp hạng.", 13f, MUTED))
            }.onSuccess { board ->
                caption.text = "${board.optString("scopeName")} · ${board.optInt("participants")} người · tuần này"
                val top = board.getJSONArray("top")
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
