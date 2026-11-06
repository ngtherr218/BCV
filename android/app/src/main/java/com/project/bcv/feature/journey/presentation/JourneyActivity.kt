package com.project.bcv.feature.journey.presentation

import android.content.Intent
import com.project.bcv.feature.discovery.presentation.PlaceDetailActivity
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.R
import com.project.bcv.core.di.ExploreModule
import com.project.bcv.core.ui.BottomNav
import com.project.bcv.core.ui.NavTab
import com.project.bcv.core.ui.TabViewModels
import com.project.bcv.feature.collection.presentation.CollectionActivity
import com.project.bcv.feature.questitem.quest.presentation.QuestActivity
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.core.ui.JourneyCompletionDialog
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.databinding.ActivityJourneyBinding
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.discovery.data.StepTestPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Hành trình" tab: the confirmed progress of the current journey. */
class JourneyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJourneyBinding
    private lateinit var model: JourneyViewModel
    private lateinit var routeMap: JourneyRouteMap
    private var shownJourney: ExploreJourney? = null
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshProgress = object : Runnable {
        override fun run() {
            val state = model.state.value
            if (state is JourneyUiState.Failed ||
                (state is JourneyUiState.Loaded && state.snapshot.journey?.status == "IN_PROGRESS")) {
                model.refreshIfNeeded()
                model.syncDeviceSteps()
            }
            refreshHandler.postDelayed(this, STEP_SYNC_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityJourneyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            // Same as Explore: the cream nav surface continues behind the system gesture area.
            BottomNav.applyBottomInset(binding.bottomNavigation, bars.bottom)
            insets
        }
        routeMap = JourneyRouteMap(this, savedInstanceState, binding.mapHost)
        binding.progressBar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> layoutProgress() }

        model = TabViewModels.provider(applicationContext, JourneyViewModelFactory(ExploreModule.repository(applicationContext), ExploreModule.stepSource(applicationContext),
            StepTestPreferences(this)))[
            JourneyViewModel::class.java]
        model.state.observe(this, ::render)

        BottomNav.select(binding.bottomNavigation, NavTab.JOURNEY)
        // A tab, not a pushed screen: open and close in place so the bar never moves.
        BottomNav.skipTransition(this, opening = true)
        BottomNav.skipTransition(this, opening = false)
        // Khám phá is the tab to the left: content came in from the right and leaves the same way.
        BottomNav.enter(BottomNav.contentOf(binding.root, binding.bottomNavigation),
            fromRight = BottomNav.enteredFromRight(this, NavTab.JOURNEY))
        binding.bottomNavigation.tabExplore.setOnClickListener { closeTab() }
        // The full, interactive map of this journey lives on Khám phá.
        binding.mapTapTarget.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_FOCUS_MAP, true))
            closeTab()
        }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = closeTab()
        })
        binding.bottomNavigation.tabJourney.setOnClickListener { model.refreshIfNeeded() }
        binding.bottomNavigation.tabCollection.setOnClickListener { openTabToRight(CollectionActivity::class.java) }
        binding.bottomNavigation.tabQuest.setOnClickListener { openTabToRight(QuestActivity::class.java) }
    }

    private var closing = false

    /** Bộ sưu tập and Nhiệm vụ are to the right: slide out to the left and replace this tab in place. */
    private fun openTabToRight(target: Class<out android.app.Activity>) {
        if (closing) return
        closing = true
        BottomNav.exit(BottomNav.contentOf(binding.root, binding.bottomNavigation), toRight = false) {
            startActivity(BottomNav.intent(this, target, NavTab.JOURNEY))
            BottomNav.skipTransition(this, opening = true)
            finish()
        }
    }

    /** Back to Khám phá: slide this tab's content out to the right, then close without a window animation. */
    private fun closeTab() {
        if (closing) return
        closing = true
        BottomNav.exit(BottomNav.contentOf(binding.root, binding.bottomNavigation), toRight = true) { finish() }
    }

    override fun finish() {
        super.finish()
        BottomNav.skipTransition(this, opening = false)
    }

    override fun onStart() { super.onStart(); routeMap.onStart() }
    override fun onResume() {
        super.onResume()
        routeMap.onResume()
        model.refreshIfNeeded()
        model.syncDeviceSteps()
        binding.contentScroll.post { binding.contentScroll.scrollTo(0, model.scrollY) }
        refreshHandler.removeCallbacks(refreshProgress)
        refreshHandler.postDelayed(refreshProgress, STEP_SYNC_INTERVAL_MS)
    }
    override fun onPause() {
        model.scrollY = binding.contentScroll.scrollY
        refreshHandler.removeCallbacks(refreshProgress); routeMap.onPause(); super.onPause()
    }
    override fun onStop() { routeMap.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); routeMap.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        routeMap.onSaveInstanceState(outState)
    }
    override fun onDestroy() { refreshHandler.removeCallbacksAndMessages(null); routeMap.onDestroy(); super.onDestroy() }

    private fun render(state: JourneyUiState) = with(binding) {
        activeItemBanner.show((state as? JourneyUiState.Loaded)?.snapshot?.journey)
        when (state) {
            JourneyUiState.Loading -> showLoading()
            is JourneyUiState.Failed -> showMessage(title = "Chưa tải được hành trình", body = state.message,
                action = "Thử lại") { model.load() }
            JourneyUiState.SessionExpired -> showMessage(title = "Cần đăng nhập lại",
                body = "Phiên đăng nhập đã hết hạn. Hành trình của bạn vẫn được giữ nguyên.",
                action = "Đăng nhập lại") {
                SessionPreferences(this@JourneyActivity).token = null
                startActivity(Intent(this@JourneyActivity, AccountActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            is JourneyUiState.Loaded -> {
                val journey = state.snapshot.journey?.takeIf { it.status == "IN_PROGRESS" || it.status == "COMPLETED" }
                if (journey == null) {
                    showMessage(title = "Chưa có hành trình",
                        body = "Chọn một điểm đến trên bản đồ Khám phá để bắt đầu.",
                        action = "Về Khám phá") { closeTab() }
                } else {
                    showJourney(journey, state)
                }
            }
        }
    }

    /** Skeleton of the destination, progress and map cards while the saved journey loads. */
    private fun showLoading() = with(binding) {
        Skeleton.show(journeyLoading.root, journeyContent, messageState)
        statusChip.visibility = View.VISIBLE
        styleChip(loading = true)
        statusText.text = "Đang tải"
        statusChip.contentDescription = "Trạng thái: đang tải"
    }

    private fun hideLoading() = Skeleton.hide(binding.journeyLoading.root)

    /** Blue "Đang tải" chip while loading, green status chip once the journey is known. */
    private fun styleChip(loading: Boolean) = with(binding) {
        val accent = if (loading) 0xFF3C7DB0.toInt() else color(R.color.teal)
        statusChip.backgroundTintList = ColorStateList.valueOf(if (loading) 0xFFE6EEF6.toInt() else 0xFFE3EFE0.toInt())
        statusDot.backgroundTintList = ColorStateList.valueOf(accent)
        statusText.setTextColor(accent)
    }

    private fun showJourney(journey: ExploreJourney, state: JourneyUiState.Loaded) = with(binding) {
        val firstShow = journeyContent.visibility != View.VISIBLE
        hideLoading()
        messageState.visibility = View.GONE
        journeyContent.visibility = View.VISIBLE
        val completed = journey.status == "COMPLETED"

        statusChip.visibility = View.VISIBLE
        styleChip(loading = false)
        statusText.text = if (completed) "Hoàn thành" else "Đang đi"
        statusChip.contentDescription = "Trạng thái: ${statusText.text}"

        destinationName.text = journey.destinationName
        unlockIcon.setImageResource(if (completed) R.drawable.ic_check else R.drawable.ic_lock)
        unlockIcon.imageTintList = android.content.res.ColorStateList.valueOf(
            if (completed) color(R.color.teal) else 0xFF8FA196.toInt())
        unlockText.text = if (completed) "Đã mở khóa" else "Chưa mở khóa"
        unlockText.setTextColor(if (completed) color(R.color.teal) else 0xFF7C9086.toInt())
        // An unlocked destination opens its photos and story; a locked one stays a plain card.
        val placeId = journey.destinationPlaceId?.takeIf { completed }
        destinationChevron.visibility = if (placeId != null) View.VISIBLE else View.GONE
        destinationCard.isClickable = placeId != null
        destinationCard.foreground = if (placeId != null)
            ContextCompat.getDrawable(this@JourneyActivity, R.drawable.journey_card_ripple) else null
        destinationCard.contentDescription = if (placeId != null)
            "Điểm đến ${journey.destinationName}, đã mở khóa. Xem nội dung địa danh" else null
        destinationCard.setOnClickListener(placeId?.let { id -> View.OnClickListener {
            startActivity(Intent(this@JourneyActivity, PlaceDetailActivity::class.java)
                .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, id)
                .putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME, journey.destinationName)
                .putExtra(PlaceDetailActivity.EXTRA_DISCOVERED, true))
        } })

        val remaining = (journey.requiredSteps - journey.recordedSteps).coerceAtLeast(0)
        stepsCount.text = format(journey.recordedSteps)
        stepsTotal.text = "/ ${format(journey.requiredSteps)} bước"
        percentValue.text = percent(journey.recordedSteps, journey.requiredSteps)
        remainingValue.text = format(remaining)
        val checkedAt = SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(state.syncedAtMillis))
        syncedText.text = if (state.stepsSynced) "Đã đồng bộ bước · $checkedAt"
            else "Đã tải tiến độ · $checkedAt" + (state.syncMessage?.let { "\n$it" } ?: "")
        progressCard.contentDescription = "Tiến độ hành trình: đã đi ${stepsCount.text} trên " +
            "${format(journey.requiredSteps)} bước, ${percentValue.text}, còn ${remainingValue.text} bước. " +
            syncedText.text
        shownJourney = journey
        layoutProgress()

        noteLocation.text = if (completed) "Vị trí ảo đã chuyển tới điểm đến." else "Vị trí ảo chỉ đổi khi hoàn thành."
        noteContent.text = if (completed) "Ảnh và nội dung đã được mở khóa." else "Ảnh và nội dung vẫn đang được khóa."

        state.snapshot.location?.coordinates?.let { routeMap.show(it, journey) }
        if (firstShow) ScreenMotion.reveal(journeyContent)
        JourneyCompletionDialog.showIfNeeded(this@JourneyActivity, journey)
    }

    /** Sizes the light fill and places the white thumb at its end. */
    private fun layoutProgress() {
        val journey = shownJourney ?: return
        val bar = binding.progressBar
        if (bar.width == 0) return
        val fraction = if (journey.requiredSteps <= 0) 0f
            else (journey.recordedSteps.toFloat() / journey.requiredSteps).coerceIn(0f, 1f)
        val fillWidth = (bar.width * fraction).toInt()
        binding.progressFill.layoutParams = binding.progressFill.layoutParams.apply { width = fillWidth }
        val thumb = binding.progressThumb
        thumb.x = (fillWidth - thumb.width / 2f).coerceIn(0f, (bar.width - thumb.width).toFloat())
        thumb.visibility = if (fraction > 0f) View.VISIBLE else View.INVISIBLE
    }

    private fun showMessage(loading: Boolean = false, title: String, body: String,
                            action: String? = null, onAction: (() -> Unit)? = null) = with(binding) {
        hideLoading()
        journeyContent.visibility = View.GONE
        statusChip.visibility = View.GONE
        messageState.visibility = View.VISIBLE
        messageProgress.visibility = if (loading) View.VISIBLE else View.GONE
        messageIcon.visibility = if (loading) View.GONE else View.VISIBLE
        messageTitle.text = title
        messageBody.text = body
        messageButton.visibility = if (action == null) View.GONE else View.VISIBLE
        messageButton.text = action
        messageButton.setOnClickListener { onAction?.invoke() }
    }

    private fun unavailable(name: String) {
        Snackbar.make(binding.root, "$name sẽ được bổ sung ở màn tiếp theo.", Snackbar.LENGTH_SHORT).show()
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private fun format(value: Int) = String.format(Locale.forLanguageTag("vi-VN"), "%,d", value)

    /** One decimal like the design (54,5%), dropping a trailing ",0". */
    private fun percent(recorded: Int, required: Int): String {
        val value = if (required <= 0) 0.0 else (recorded * 100.0 / required).coerceIn(0.0, 100.0)
        return String.format(Locale.forLanguageTag("vi-VN"), "%.1f", value).removeSuffix(",0") + "%"
    }

    companion object {
        private const val STEP_SYNC_INTERVAL_MS = 5_000L
        /** Result extra: Khám phá should frame the current journey (or the virtual location). */
        const val EXTRA_FOCUS_MAP = "focus_map"
    }
}
