package com.project.bcv.feature.questitem.quest.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.project.bcv.R
import com.project.bcv.core.di.QuestModule
import com.project.bcv.core.ui.BottomNav
import com.project.bcv.core.ui.NavTab
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.core.ui.TabViewModels
import com.project.bcv.databinding.ActivityQuestBinding
import com.project.bcv.databinding.ItemQuestBinding
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.collection.presentation.CollectionActivity
import com.project.bcv.feature.questitem.inventory.presentation.InventoryActivity
import com.project.bcv.feature.journey.presentation.JourneyActivity
import com.project.bcv.feature.questitem.quest.domain.Quest
import com.project.bcv.feature.questitem.quest.domain.QuestOverview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Nhiệm vụ" tab: today's and this week's quests with confirmed progress and rewards. */
class QuestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestBinding
    private lateinit var model: QuestViewModel
    private var weekly = false
    private var leaving = false
    private var shown: QuestOverview? = null
    private var renderedQuests: List<Quest>? = null
    private var restoredScroll = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityQuestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            BottomNav.applyBottomInset(binding.bottomNavigation, bars.bottom)
            insets
        }
        model = TabViewModels.provider(applicationContext, QuestViewModelFactory(QuestModule.repository(this)))[QuestViewModel::class.java]
        weekly = savedInstanceState?.getBoolean(WEEKLY_KEY) ?: model.weeklySelected
        model.weeklySelected = weekly

        BottomNav.select(binding.bottomNavigation, NavTab.CHALLENGE)
        BottomNav.skipTransition(this, opening = true)
        BottomNav.skipTransition(this, opening = false)
        BottomNav.enter(BottomNav.contentOf(binding.root, binding.bottomNavigation),
            fromRight = BottomNav.enteredFromRight(this, NavTab.CHALLENGE))
        BottomNav.wire(this, binding.bottomNavigation, NavTab.CHALLENGE, binding.root) { model.refreshIfNeeded() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = leave()
        })
        binding.dailyTab.setOnClickListener { selectCycle(weekly = false) }
        binding.weeklyTab.setOnClickListener { selectCycle(weekly = true) }
        binding.retryButton.setOnClickListener { model.load() }
        binding.inventoryButton.setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }
        styleSegments()

        model.state.observe(this, ::render)
    }

    override fun onResume() {
        super.onResume()
        model.refreshIfNeeded()
        loadEngagement()
    }

    /** Streak card on top and badges at the bottom; quests still load on their own. */
    private fun loadEngagement(): Unit { lifecycleScope.launch {
        val api = com.project.bcv.feature.engagement.EngagementApi(this@QuestActivity)
        api.reportToday()
        val data = runCatching { api.summary() }.getOrNull() ?: return@launch
        val blocks = com.project.bcv.feature.engagement.EngagementBlocks
        binding.streakSlot.removeAllViews()
        binding.streakSlot.addView(blocks.streakCard(this@QuestActivity, data.getJSONObject("streak")) { loadEngagement() })
        binding.badgeSlot.removeAllViews()
        blocks.badges(this@QuestActivity, binding.badgeSlot, data.getJSONArray("badges"))
    } }

    override fun onPause() {
        (binding.root.getChildAt(0) as? android.widget.ScrollView)?.let { model.scrollY = it.scrollY }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(WEEKLY_KEY, weekly)
    }

    override fun finish() {
        super.finish()
        BottomNav.skipTransition(this, opening = false)
    }

    /** Every other tab is to the left: slide out to the right, then return or open [next] in place. */
    private fun leave(next: Intent? = null) {
        if (leaving) return
        leaving = true
        BottomNav.exit(BottomNav.contentOf(binding.root, binding.bottomNavigation), toRight = true) {
            if (next != null) {
                startActivity(next)
                BottomNav.skipTransition(this, opening = true)
            }
            finish()
        }
    }

    private fun render(state: QuestUiState) = with(binding) {
        if (state != QuestUiState.Loading) Skeleton.hide(skeleton.root)
        when (state) {
            QuestUiState.Loading -> {
                status(updating = true, title = "Đang cập nhật tiến độ",
                    body = "Các số liệu được giữ đến khi tải xong.")
                // First load only: quests already listed stay while they refresh.
                if (shown == null) Skeleton.show(skeleton.root, questList)
            }
            is QuestUiState.Failed -> {
                status(failed = true, title = "Chưa cập nhật được", body = state.message)
                retryButton.visibility = View.VISIBLE
            }
            QuestUiState.SessionExpired -> {
                SessionPreferences(this@QuestActivity).token = null
                startActivity(Intent(this@QuestActivity, AccountActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            is QuestUiState.Loaded -> {
                val time = SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(state.overview.updatedAtMillis))
                if (state.refreshError != null) {
                    status(failed = true, title = "Chưa cập nhật được", body = state.refreshError)
                    retryButton.visibility = View.VISIBLE
                } else { binding.statusCard.visibility = View.GONE; updatedAt = time }
                shown = state.overview
                showQuests()
                if (!restoredScroll) {
                    restoredScroll = true
                    val scrollY = model.scrollY
                    (binding.root.getChildAt(0) as? android.widget.ScrollView)?.let { scroll ->
                        scroll.post { scroll.scrollTo(0, scrollY) }
                    }
                }
            }
        }
    }

    /** Only problems get a card; a normal refresh is just "cập nhật hh:mm" next to the quest count. */
    private var updatedAt: String? = null

    private fun status(title: String, body: String, updating: Boolean = false, failed: Boolean = false) = with(binding) {
        retryButton.visibility = View.GONE
        statusCard.visibility = if (failed) View.VISIBLE else View.GONE
        statusTitle.text = title
        statusBody.text = body
        val accent = when {
            failed -> ContextCompat.getColor(this@QuestActivity, R.color.error_red)
            updating -> 0xFF3C7DB0.toInt()
            else -> ContextCompat.getColor(this@QuestActivity, R.color.teal)
        }
        statusEyebrow.setTextColor(accent)
        statusEyebrow.text = if (failed) "CHƯA TẢI ĐƯỢC DỮ LIỆU" else "DỮ LIỆU ĐÃ XÁC NHẬN"
        // Only a failure needs the extra label; the normal state stays a quiet one-liner.
        statusEyebrow.visibility = if (failed) View.VISIBLE else View.GONE
        statusIcon.setImageResource(if (updating || failed) R.drawable.ic_sync_outline else R.drawable.ic_check_circle)
        statusIcon.imageTintList = ColorStateList.valueOf(accent)
        statusCard.backgroundTintList = ColorStateList.valueOf(when {
            failed -> 0xFFFCEFEA.toInt()
            updating -> 0xFFE6EDF0.toInt()
            else -> 0xFFE8F0E2.toInt()
        })
    }

    private fun selectCycle(weekly: Boolean) {
        if (this.weekly == weekly) return
        this.weekly = weekly
        model.weeklySelected = weekly
        styleSegments()
        showQuests()
    }

    private fun styleSegments() = with(binding) {
        val teal = ContextCompat.getColor(this@QuestActivity, R.color.teal)
        listOf(dailyTab to !weekly, weeklyTab to weekly).forEach { (tab, active) ->
            tab.setBackgroundResource(if (active) R.drawable.quest_segment_selected_bg else 0)
            tab.setTextColor(if (active) teal else 0xFF7C9086.toInt())
            tab.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
            tab.isSelected = active
            tab.contentDescription = "${tab.text}${if (active) ", đang chọn" else ""}"
        }
        sectionTitle.text = "Nhiệm vụ"
    }

    private fun showQuests() = with(binding) {
        val overview = shown ?: return@with
        val quests = if (weekly) overview.weekly else overview.daily
        val done = quests.count { it.completed }
        sectionCount.text = "$done/${quests.size} xong" + (updatedAt?.let { " · cập nhật $it" } ?: "")
        questList.visibility = View.VISIBLE
        if (renderedQuests == quests) return@with
        renderedQuests = quests
        questList.removeAllViews()
        quests.forEach { questList.addView(questCard(it).root) }
    }

    private fun questCard(quest: Quest): ItemQuestBinding {
        val item = ItemQuestBinding.inflate(layoutInflater, binding.questList, false)
        val teal = ContextCompat.getColor(this, R.color.teal)
        val done = quest.completed
        val unit = when (quest.conditionType) {
            "STEP_COUNT" -> "bước"
            "DISCOVER_PLACE" -> "địa danh"
            "COMPLETE_JOURNEY" -> "hành trình"
            "PHOTO_APPROVED" -> "ảnh"
            "MEDIA_APPROVED" -> "nội dung"
            "QUIZ_CORRECT" -> "câu đúng"
            else -> "lượt"
        }
        val (icon, iconTint, iconBox) = when (quest.conditionType) {
            "QUIZ_CORRECT" -> Triple(R.drawable.ic_quiz, 0xFFC9962B.toInt(), 0xFFFBF3E2.toInt())
            "DISCOVER_PLACE" -> Triple(R.drawable.ic_travel_explore, 0xFF3C7DB0.toInt(), 0xFFE6EEF3.toInt())
            "COMPLETE_JOURNEY" -> Triple(R.drawable.ic_nav_route, 0xFFA0702E.toInt(), 0xFFF3EBDD.toInt())
            "PHOTO_APPROVED", "MEDIA_APPROVED" -> Triple(R.drawable.ic_check_circle, teal, 0xFFE8F0E2.toInt())
            else -> Triple(R.drawable.ic_directions_walk, teal, 0xFFE8F0E2.toInt())
        }
        item.questIcon.setImageResource(icon)
        item.questIcon.imageTintList = ColorStateList.valueOf(iconTint)
        item.questIcon.backgroundTintList = ColorStateList.valueOf(if (done) 0xFFDDEAD6.toInt() else iconBox)

        item.questName.text = quest.name
        val shownProgress = quest.progress.coerceAtMost(quest.target)
        item.questProgressText.text = "${format(shownProgress)} / ${format(quest.target)} $unit"
        item.questProgress.max = quest.target
        item.questProgress.progress = shownProgress
        item.questDone.visibility = if (done) View.VISIBLE else View.GONE
        item.root.setBackgroundResource(if (done) R.drawable.quest_done_bg else R.drawable.journey_card_bg)

        val rewardColor = if (done) teal else 0xFFA0702E.toInt()
        // A quest pays an item, explorer points, or both.
        val itemReward = quest.rewardName?.let { "$it × ${quest.rewardQuantity ?: 1}" }
        val points = if (quest.completed) quest.awardedRankingPoints else quest.rankingPoints
        val pointReward = points.takeIf { it > 0 }?.let { "⭐ +$it điểm khám phá${if (quest.completed) " · Đã cộng" else ""}" }
        item.questReward.text = listOfNotNull(itemReward, pointReward).joinToString(" · ").ifEmpty { "Không có phần thưởng" }
        item.questReward.setTextColor(rewardColor)
        // Same icons as the items in Túi đồ.
        val rewardIcon = if (quest.rewardType == "FLIGHT_TICKET") R.drawable.ic_flight else R.drawable.ic_sneaker
        ContextCompat.getDrawable(this, rewardIcon)?.mutate()?.let { drawable ->
            val size = (16 * resources.displayMetrics.density).toInt()
            drawable.setBounds(0, 0, size, size)
            drawable.setTint(rewardColor)
            item.questReward.setCompoundDrawablesRelative(if (quest.rewardName != null) drawable else null, null, null, null)
        }

        val percent = if (quest.target <= 0) 0 else (shownProgress * 100L / quest.target).toInt()
        item.questStatus.text = when {
            done && quest.rewardReceived -> "Đã nhận thưởng"
            done -> "Đã hoàn thành"
            else -> "$percent% hoàn thành"
        }
        item.questStatus.setTextColor(if (done) teal else 0xFF7C9086.toInt())
        item.questStatus.setTypeface(null, if (done) Typeface.BOLD else Typeface.NORMAL)
        item.root.contentDescription = "${quest.name}, ${item.questProgressText.text}, ${item.questStatus.text}. " +
            "Phần thưởng: ${item.questReward.text}"
        return item
    }

    private fun format(value: Int) = String.format(Locale.forLanguageTag("vi-VN"), "%,d", value)

    private companion object { const val WEEKLY_KEY = "quest_weekly" }
}
