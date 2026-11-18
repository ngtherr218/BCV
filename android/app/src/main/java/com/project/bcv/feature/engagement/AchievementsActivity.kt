package com.project.bcv.feature.engagement

import com.project.bcv.feature.engagement.data.EngagementApi
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
import com.project.bcv.feature.engagement.EngagementUi.cardDialog
import com.project.bcv.feature.engagement.EngagementUi.leadingIcon
import com.project.bcv.feature.engagement.EngagementUi.popIn
import com.project.bcv.feature.engagement.EngagementUi.primaryButton
import com.project.bcv.feature.engagement.EngagementUi.outlinedButton
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
    /** Tickets for rank 1–3, from the board; shown in the prize popup. */
    private var prizes: List<org.json.JSONObject> = emptyList()
    /** "Giải thưởng" pill by the title: opens the prizes and the history. */
    private val prizeButton by lazy {
        text("Giải thưởng", 13f, 0xFF8A6418.toInt(), true)
            .leadingIcon(com.project.bcv.R.drawable.ic_trophy, 0xFFC9962B.toInt(), 16, 5).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(GOLD_SOFT, 16, 0xFFF0DFB4.toInt(), context)
                setPadding(dp(10), 0, dp(12), 0)
                isClickable = true; isFocusable = true
                contentDescription = "Giải thưởng tuần và lịch sử"
                setOnClickListener { showPrizes() }
            }
    }
    private val stage by lazy { FrameLayout(this) }
    private val list by lazy { LinearLayout(this).apply { orientation = LinearLayout.VERTICAL } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // "Giải thưởng" sits by the title: it is the same for all three boards, so switching never moves it.
        body = scaffold("Xếp hạng", tab = NavTab.RANKING, action = prizeButton.apply {
            layoutParams = LinearLayout.LayoutParams(-2, dp(36)).apply { gravity = Gravity.BOTTOM; bottomMargin = dp(4) }
        })
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
                it.optJSONArray("rewards")?.takeIf { rewards -> rewards.length() > 0 }?.let(::showRewards)
            }
        }
    }

    /** Last week's top-3 prizes, once: a trophy, each placing and the tickets it brought. */
    private fun showRewards(rewards: org.json.JSONArray) {
        if (isFinishing || isDestroyed) return
        lateinit var trophy: android.view.View
        cardDialog { dialog ->
            trophy = FrameLayout(context).apply {
                background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    intArrayOf(0xFFF6D98B.toInt(), 0xFFC9962B.toInt())).apply { shape = android.graphics.drawable.GradientDrawable.OVAL }
                addView(icon(com.project.bcv.R.drawable.ic_trophy, 0xFF7A5512.toInt(), 40),
                    FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
            }
            addView(trophy, LinearLayout.LayoutParams(dp(84), dp(84)))
            addView(text("Bạn lọt top 3 tuần trước!", 20f, FOREST, true).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            addView(text("Phần thưởng đã được thêm vào kho đồ của bạn.", 14f, MUTED).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
            for (i in 0 until rewards.length()) {
                val reward = rewards.getJSONObject(i)
                val rank = reward.optInt("rank")
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    background = rounded(GOLD_SOFT, 16, null, context)
                    setPadding(dp(12), dp(10), dp(14), dp(10))
                    addView(LeaderboardViews.medal(context, rank, 32), LinearLayout.LayoutParams(dp(32), dp(32)))
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(text("Hạng $rank · ${reward.optString("boardName")}", 15f, FOREST, true))
                        val quantity = reward.optInt("quantity")
                        if (quantity > 0) addView(text("+$quantity ${reward.optString("itemName").lowercase()}", 13f, MUTED)
                            .leadingIcon(LeaderboardViews.itemIcon(reward.optString("itemType")), MUTED, 14, 4))
                    }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
                }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(if (i == 0) 16 else 8) })
            }
            addView(primaryButton("Tuyệt vời").apply { setOnClickListener { dialog.dismiss() } },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        }.show()
        trophy.popIn()
        lifecycleScope.launch { runCatching { api.markRewardsSeen() } }
    }

    /** "Giải thưởng tuần": what each place wins and how the week is settled; links to the history. */
    private fun showPrizes() {
        val scopeKey = scope
        cardDialog { dialog ->
            addView(FrameLayout(context).apply {
                background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    intArrayOf(0xFFF6D98B.toInt(), 0xFFC9962B.toInt())).apply { shape = android.graphics.drawable.GradientDrawable.OVAL }
                addView(icon(com.project.bcv.R.drawable.ic_trophy, 0xFF7A5512.toInt(), 32),
                    FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER))
            }, LinearLayout.LayoutParams(dp(68), dp(68)))
            addView(text("Giải thưởng tuần", 20f, FOREST, true).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            addView(text("Mỗi tuần, top 3 của cả 3 bảng nhận quà vào kho đồ.", 14f, MUTED).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
            val ranks = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(GOLD_SOFT, 16, null, context)
                setPadding(dp(14), dp(4), dp(14), dp(4))
            }
            prizes.forEachIndexed { index, prize ->
                val quantity = prize.optInt("quantity")
                ranks.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, dp(8))
                    addView(LeaderboardViews.medal(context, index + 1, 28), LinearLayout.LayoutParams(dp(28), dp(28)))
                    addView(text("Hạng ${index + 1}", 15f, FOREST, true).apply { setPadding(dp(10), 0, 0, 0) },
                        LinearLayout.LayoutParams(0, -2, 1f))
                    addView(if (quantity > 0) text("$quantity ${prize.optString("itemName").lowercase()}", 15f, 0xFF8A6418.toInt(), true)
                        .leadingIcon(LeaderboardViews.itemIcon(prize.optString("itemType")), 0xFF8A6418.toInt(), 16, 4)
                        else text("Không có quà", 15f, MUTED))
                })
            }
            addView(ranks, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
            listOf(
                "Trao giải lúc 0h thứ Hai. Hạng 1 được đeo vương miện suốt tuần sau.",
                "Khám phá và Vận động tính lại từ đầu mỗi tuần; Giữ chuỗi xét chuỗi dài nhất, không làm mới.",
                "Khám phá xét chung mọi người; Vận động và Giữ chuỗi có top 3 riêng cho từng mức vận động.",
                "Bước chân bị hệ thống từ chối trong tuần thì không được xét ở bảng Vận động.",
            ).forEach { rule ->
                addView(text(rule, 13f, MUTED).leadingIcon(com.project.bcv.R.drawable.ic_m3_check_circle, 0xFF6B8F7E.toInt(), 16, 8),
                    LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            }
            addView(primaryButton("Xem lịch sử").apply {
                setIconResource(com.project.bcv.R.drawable.ic_trophy)
                setOnClickListener { dialog.dismiss(); showHistory(scopeKey) }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
            addView(outlinedButton("Đóng").apply { setOnClickListener { dialog.dismiss() } },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }.show()
    }

    /** Bảng vàng of past weeks, newest first. */
    private fun showHistory(key: String) = GoldBoardSheet.show(this, api, key)

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
                board.optJSONArray("prizes")?.let { p -> prizes = (0 until p.length()).map(p::getJSONObject) }
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
