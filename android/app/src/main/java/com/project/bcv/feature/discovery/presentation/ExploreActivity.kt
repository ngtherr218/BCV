package com.project.bcv.feature.discovery.presentation

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.R
import com.project.bcv.core.di.ExploreModule
import com.project.bcv.core.di.ProfileModule
import com.project.bcv.feature.account.profile.presentation.Avatars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.project.bcv.core.ui.BottomNav
import com.project.bcv.core.ui.NavTab
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.databinding.ActivityExploreBinding
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.statsnotify.notifications.presentation.NotificationsActivity
import com.project.bcv.feature.account.profile.presentation.ProfileActivity
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.healthconnect.presentation.HealthConnectActivity
import com.project.bcv.feature.journey.presentation.JourneyActivity
import com.project.bcv.feature.collection.presentation.CollectionActivity
import com.project.bcv.feature.questitem.quest.presentation.QuestActivity
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs

class ExploreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExploreBinding
    private lateinit var model: ExploreViewModel
    private lateinit var journeyRewards: com.project.bcv.feature.collection.presentation.JourneyRewardController
    private lateinit var notificationBell: com.project.bcv.feature.statsnotify.notifications.presentation.NotificationBell
    private lateinit var goongMap: GoongExploreMap
    private val handler = Handler(Looper.getMainLooper())
    private var sheetCollapsed = false
    private var sheetDragging = false
    private var mapLoaded = false
    private var mapFailed = false
    private val searchMapFocus = Runnable {
        if (binding.placeTypeSearch.text.isNotBlank()) goongMap.focusSearchResults()
    }
    private var placeTypeOptions = emptyList<String>()
    private var lastSnapshot: com.project.bcv.feature.discovery.domain.ExploreSnapshot? = null
    private var renderedStage: ExploreStage? = null
    private var shownStepsJourneyId: Long? = null
    /** Set while Hành trình is open, so coming back slides this tab in from the left. */
    private var returningFromTab = false
    private var resumedOnce = false
    /** Set when Hành trình's map card was tapped: frame the journey (or virtual location) on return. */
    private var focusMapOnReturn = false
    private val journeyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        focusMapOnReturn = result.data?.getBooleanExtra(JourneyActivity.EXTRA_FOCUS_MAP, false) == true
    }
    private var shownSteps = 0
    private var stepsAnimator: ValueAnimator? = null
    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private var stepsPermissionAsked = false
    private var activityPermissionAsked = false
    private var notificationPermissionAsked = false
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val stepsPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        model.checkPermission()
        requestActivityPermissionIfNeeded()
        if (readStepsPermission !in granted) {
            // Health Connect stops showing its dialog after repeated refusals; settings is the way back.
            Snackbar.make(binding.root, "Cần quyền đọc bước chân để cập nhật tiến độ hành trình.",
                Snackbar.LENGTH_LONG)
                .setAction("Mở cài đặt") { startActivity(Intent(this, HealthConnectActivity::class.java)) }
                .show()
        }
    }
    private val activityPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            model.checkPermission()
            model.captureDeviceSteps()
            requestTrackingNotificationPermission()
        }
        else Snackbar.make(binding.root, "Cần quyền Hoạt động thể chất để đếm bước khi đi bộ.",
            Snackbar.LENGTH_LONG).show()
    }
    private val placeDetailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) model.load()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sheetCollapsed = savedInstanceState?.getBoolean(SHEET_STATE_KEY) ?: false
        binding.bottomSheet.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (sheetCollapsed && !sheetDragging) binding.bottomSheet.translationY = collapsedOffset()
            updateMapCoverage()
        }
        binding.bottomSheet.post { setSheetCollapsed(sheetCollapsed, animate = false) }
        installSheetGesture(binding.bottomSheet) {}
        installSheetGesture(binding.sheetHandleArea) { toggleSheet() }
        installSheetGesture(binding.statusCard) {
            if (sheetCollapsed) setSheetCollapsed(false)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            // The nav bar's cream surface continues behind the system gesture area.
            BottomNav.applyBottomInset(binding.bottomNavigation, bars.bottom)
            insets
        }

        goongMap = GoongExploreMap(this, savedInstanceState,
            onLoaded = {
                mapLoaded = true
                mapFailed = false
                if (::model.isInitialized) render(model.state.value ?: ExploreUiState())
            },
            onFailed = {
                mapFailed = true
                mapLoaded = false
                if (::model.isInitialized) render(model.state.value ?: ExploreUiState())
            },
            onMapTap = { setSheetCollapsed(true) },
            onPlaceTap = { placeId ->
                val mapPlace = model.state.value?.snapshot?.places?.firstOrNull { it.id == placeId }
                placeDetailLauncher.launch(Intent(this, PlaceDetailActivity::class.java)
                    .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, placeId)
                    .putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME, mapPlace?.name)
                    .putExtra(PlaceDetailActivity.EXTRA_DISCOVERED, mapPlace?.isDiscovered == true))
            })
        goongMap.attach(binding.mapHost)
        scheduleMapTimeout()

        model = ViewModelProvider(this, ExploreViewModelFactory(
            ExploreModule.repository(this), ExploreModule.stepSource(this), ExploreModule.addressLookup(), com.project.bcv.feature.discovery.data.StepTestPreferences(this)
        ))[ExploreViewModel::class.java]
        notificationBell = com.project.bcv.feature.statsnotify.notifications.presentation.NotificationBell(
            this, binding.notificationsButton, binding.notificationBadge)
        journeyRewards = com.project.bcv.feature.collection.presentation.JourneyRewardController(this) {
            com.project.bcv.core.ui.CollectionNavBadge.refresh(binding.bottomNavigation)
        }
        model.state.observe(this) { state ->
            render(state)
            if (state.stage == ExploreStage.COMPLETED) {
                state.snapshot?.journey?.let(journeyRewards::completed)
                notificationBell.refresh()
                com.project.bcv.core.ui.CollectionNavBadge.refresh(binding.bottomNavigation)
            }
        }
        model.address.observe(this) { model.state.value?.let(::renderLocationName) }
        binding.placeTypeSearch.threshold = 1
        binding.placeTypeSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                lastSnapshot = null
                model.state.value?.let(::render)
                handler.removeCallbacks(searchMapFocus)
                handler.postDelayed(searchMapFocus, 400)
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        binding.recenterButton.setOnClickListener { goongMap.recenter() }
        binding.primaryButton.setOnClickListener { primaryAction() }
        binding.secondaryButton.setOnClickListener { secondaryAction() }
        binding.demoStepsButton.setOnClickListener { StepTestDialog.show(this, model) }
        // Icon only now: long-press shows what it does.
        TooltipCompat.setTooltipText(binding.demoStepsButton, "Test bước chân")
        binding.tertiaryButton.setOnClickListener { openRecommendations() }
        binding.notificationsButton.setOnClickListener { startActivity(Intent(this, NotificationsActivity::class.java)) }
        binding.profileButton.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        BottomNav.select(binding.bottomNavigation, NavTab.EXPLORE)
        binding.bottomNavigation.tabExplore.setOnClickListener { model.load() }
        binding.bottomNavigation.tabJourney.setOnClickListener { openJourney() }
        binding.bottomNavigation.tabCollection.setOnClickListener {
            startActivity(BottomNav.intent(this, CollectionActivity::class.java, NavTab.EXPLORE))
            BottomNav.skipTransition(this, opening = true)
            returningFromTab = true
        }
        binding.bottomNavigation.tabQuest.setOnClickListener {
            startActivity(BottomNav.intent(this, QuestActivity::class.java, NavTab.EXPLORE))
            BottomNav.skipTransition(this, opening = true)
            returningFromTab = true
        }
        if (model.state.value?.snapshot == null) model.load()
        stepsPermissionAsked = savedInstanceState?.getBoolean(STEPS_PERMISSION_ASKED_KEY) ?: false
        activityPermissionAsked = savedInstanceState?.getBoolean("activityPermissionAsked") ?: false
        notificationPermissionAsked = savedInstanceState?.getBoolean("notificationPermissionAsked") ?: false
        if (!stepsPermissionAsked) {
            stepsPermissionAsked = true
            model.whenStepPermissionNeeded { requestStepsPermission(openSetupIfUnavailable = false) }
        }
    }

    /**
     * Shows the Health Connect permission dialog when reading steps is not allowed yet.
     * Without Health Connect on the device, only an explicit tap opens the setup screen.
     */
    private fun requestStepsPermission(openSetupIfUnavailable: Boolean) {
        lifecycleScope.launch {
            if (HealthConnectClient.getSdkStatus(this@ExploreActivity) != HealthConnectClient.SDK_AVAILABLE) {
                if (openSetupIfUnavailable) {
                    startActivity(Intent(this@ExploreActivity, HealthConnectActivity::class.java))
                }
                requestActivityPermissionIfNeeded(force = openSetupIfUnavailable)
                return@launch
            }
            val granted = runCatching {
                HealthConnectClient.getOrCreate(this@ExploreActivity).permissionController.getGrantedPermissions()
            }.getOrDefault(emptySet())
            if (readStepsPermission !in granted) stepsPermissionLauncher.launch(setOf(readStepsPermission))
            else {
                model.checkPermission()
                requestActivityPermissionIfNeeded(force = openSetupIfUnavailable)
            }
        }
    }

    /**
     * Asks for Android's "physical activity" permission only when no app records steps in Health
     * Connect, so the Recording API is the only way to count real walking (see RecordingStepCounter).
     */
    private fun requestActivityPermissionIfNeeded(force: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (activityPermissionAsked && !force) return
        model.whenActivityPermissionNeeded {
            if (activityPermissionAsked && !force) return@whenActivityPermissionNeeded
            activityPermissionAsked = true
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun requestTrackingNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33 || notificationPermissionAsked ||
            model.state.value?.snapshot?.journey?.status != "IN_PROGRESS" ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) return
        notificationPermissionAsked = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStart() { super.onStart(); goongMap.onStart() }
    override fun onResume() {
        super.onResume()
        goongMap.onResume()
        // A directly opened item screen can equip shoes without switching tabs.
        if (resumedOnce && !returningFromTab) model.load()
        resumedOnce = true
        // Refreshed on every return so a photo changed in Hồ sơ shows here at once.
        loadProfileAvatar()
        if (::model.isInitialized) {
            model.checkPermission()
            model.captureDeviceSteps()
        }
        handler.removeCallbacks(autoSync)
        handler.post(autoSync)
        if (returningFromTab) {
            returningFromTab = false
            BottomNav.enter(BottomNav.contentOf(binding.root, binding.bottomNavigation), fromRight = false)
            // Other tabs can change what this screen shows (a flight ticket moves the virtual location).
            model.load()
        }
        if (focusMapOnReturn) {
            focusMapOnReturn = false
            // Glides to the whole in-progress route, or to the virtual location without a journey.
            binding.root.post { goongMap.recenter() }
        }
    }
    override fun onPause() { model.stopStepSimulation(); handler.removeCallbacks(autoSync); goongMap.onPause(); super.onPause() }
    override fun onStop() { goongMap.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); goongMap.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SHEET_STATE_KEY, sheetCollapsed)
        outState.putBoolean(STEPS_PERMISSION_ASKED_KEY, stepsPermissionAsked)
        outState.putBoolean("activityPermissionAsked", activityPermissionAsked)
        outState.putBoolean("notificationPermissionAsked", notificationPermissionAsked)
        goongMap.onSaveInstanceState(outState)
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); stepsAnimator?.cancel(); goongMap.onDestroy(); super.onDestroy() }

    /** Sends new steps on its own every minute while this screen shows (see ExploreViewModel.autoSyncSteps). */
    private val autoSync = object : Runnable {
        override fun run() {
            if (::model.isInitialized) model.autoSyncSteps()
            handler.postDelayed(this, AUTO_SYNC_INTERVAL_MS)
        }
    }

    private fun scheduleMapTimeout() {
        handler.postDelayed({
            if (!mapLoaded && !isFinishing) {
                mapFailed = true
                render(model.state.value ?: ExploreUiState())
            }
        }, 30_000L)
    }

    private fun retryMap() {
        mapLoaded = false
        mapFailed = false
        render(model.state.value ?: ExploreUiState())
        goongMap.retry()
        scheduleMapTimeout()
    }

    private fun toggleSheet() {
        setSheetCollapsed(!sheetCollapsed)
    }

    private fun collapsedOffset(): Float {
        val peekHeight = 86f * resources.displayMetrics.density
        return (binding.bottomSheet.height - peekHeight).coerceAtLeast(0f)
    }

    /** Tells the map how much of it the sheet hides, so the journey route is framed in the visible part. */
    private fun updateMapCoverage() {
        // Mid-drag the camera stays put; it reframes once the sheet settles open or collapsed.
        if (!::goongMap.isInitialized || sheetDragging) return
        val sheet = binding.bottomSheet
        goongMap.setCoveredBottom(if (sheetCollapsed) (sheet.height - collapsedOffset()).toInt() else sheet.height)
    }

    private fun setSheetCollapsed(collapsed: Boolean, animate: Boolean = true) {
        sheetCollapsed = collapsed
        updateMapCoverage()
        val sheet = binding.bottomSheet
        val target = if (collapsed) collapsedOffset() else 0f
        sheet.animate().cancel()
        if (animate && sheet.isLaidOut) {
            sheet.animate().translationY(target).setDuration(260L)
                .setInterpolator(FastOutSlowInInterpolator()).start()
        } else sheet.translationY = target
    }

    private fun installSheetGesture(view: View, onTap: () -> Unit) {
        val threshold = 28f * resources.displayMetrics.density
        val tapSlop = 8f * resources.displayMetrics.density
        var downY = 0f
        var startOffset = 0f
        view.setOnClickListener { onTap() }
        view.setOnTouchListener { target, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    binding.bottomSheet.animate().cancel()
                    downY = event.rawY
                    startOffset = binding.bottomSheet.translationY
                    sheetDragging = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    binding.bottomSheet.translationY =
                        (startOffset + event.rawY - downY).coerceIn(0f, collapsedOffset())
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val distance = event.rawY - downY
                    sheetDragging = false
                    if (abs(distance) < tapSlop) target.performClick()
                    else {
                        val collapse = if (abs(distance) > threshold) distance > 0
                            else binding.bottomSheet.translationY > collapsedOffset() / 2f
                        setSheetCollapsed(collapse)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    sheetDragging = false
                    setSheetCollapsed(sheetCollapsed)
                    true
                }
                else -> false
            }
        }
    }

    private fun primaryAction() {
        if (model.state.value?.stage == ExploreStage.SESSION_EXPIRED) {
            SessionPreferences(this).token = null
            startActivity(Intent(this, AccountActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            return
        }
        if (mapFailed) { retryMap(); return }
        when (model.state.value?.stage) {
            ExploreStage.LOAD_FAILED, ExploreStage.RESTORE_FAILED -> model.load()
            ExploreStage.SYNC_FAILED -> model.syncSteps()
            ExploreStage.PERMISSION_REQUIRED -> requestStepsPermission(openSetupIfUnavailable = true)
            ExploreStage.EMPTY, ExploreStage.COMPLETED -> openRecommendations()
            ExploreStage.ACTIVE, ExploreStage.NO_NEW_STEPS, ExploreStage.SYNCING -> openJourney()
            else -> Unit
        }
    }

    private fun secondaryAction() {
        when (model.state.value?.stage) {
            ExploreStage.ACTIVE, ExploreStage.NO_NEW_STEPS, ExploreStage.SYNC_FAILED -> model.syncSteps()
            ExploreStage.PERMISSION_REQUIRED -> openJourney()
            else -> Unit
        }
    }


    private fun renderLocationName(state: ExploreUiState) {
        val location = state.snapshot?.location
        val fallback = if (state.snapshot?.journey?.status == "COMPLETED")
            location?.placeName ?: location?.regionName
        else location?.regionName
        binding.locationName.text = (if (location != null) model.address.value else null)
            ?: fallback ?: "Chưa có vị trí ảo"
    }

    private fun render(state: ExploreUiState) = with(binding) {
        if (state.snapshot?.journey?.status == "IN_PROGRESS") {
            requestActivityPermissionIfNeeded()
            requestTrackingNotificationPermission()
        }
        activeItemBanner.show(state.snapshot?.journey)
        state.snapshot?.let { snapshot ->
            val options = snapshot.places.map { it.placeType }.filter(String::isNotBlank).distinct().sorted()
            if (options != placeTypeOptions) {
                placeTypeOptions = options
                placeTypeSearch.setAdapter(android.widget.ArrayAdapter(this@ExploreActivity, android.R.layout.simple_dropdown_item_1line, options))
            }
            val query = placeTypeSearch.text.toString().trim()
            val filtered = snapshot.copy(places = com.project.bcv.feature.discovery.domain.PlaceSearch.filter(snapshot.places, query))
            filterSummary.text = if (query.isEmpty()) "${snapshot.places.size} địa danh · Chọn loại để lọc bản đồ" else "${filtered.places.size} địa danh phù hợp"
            if (filtered != lastSnapshot) {
                goongMap.show(filtered)
                lastSnapshot = filtered
            }
        }
        renderLocationName(state)
        mapLoading.visibility = if (!mapLoaded && !mapFailed) View.VISIBLE else View.GONE
        mapFailure.visibility = if (mapFailed) View.VISIBLE else View.GONE
        locationCard.visibility = if (mapFailed || !mapLoaded) View.GONE else View.VISIBLE
        recenterButton.visibility = locationCard.visibility

        val copy = if (mapFailed) mapErrorCopy else copyFor(state)
        statusTitle.text = copy.noticeTitle
        statusDetail.text = copy.noticeDetail
        statusDetail.visibility = if (copy.noticeDetail.isBlank()) View.GONE else View.VISIBLE
        statusCard.backgroundTintList = ColorStateList.valueOf(copy.noticeBackground)
        statusTitle.setTextColor(copy.noticeColor)
        statusDetail.setTextColor(copy.noticeColor)
        // Revealed only when it changes: syncing keeps the destination name still.
        val titleChanged = sheetTitle.text.toString() != copy.title
        sheetTitle.text = copy.title
        sheetDescription.text = copy.description
        primaryButton.text = copy.primary
        primaryButton.isEnabled = copy.primaryEnabled
        secondaryButton.visibility = if (copy.secondary.isEmpty()) View.GONE else View.VISIBLE
        secondaryButton.text = copy.secondary
        secondaryButton.isEnabled = state.stage != ExploreStage.SYNCING
        sheetFooter.text = copy.footer

        val journey = state.snapshot?.journey
        val showProgress = journey != null && !mapFailed && state.stage !in setOf(
            ExploreStage.LOADING, ExploreStage.LOAD_FAILED)
        progressSection.visibility = if (showProgress) View.VISIBLE else View.GONE
        startLocation.visibility = if (journey != null) View.VISIBLE else View.GONE
        startLocation.text = journey?.let { "Xuất phát: ${it.startLabel}" }.orEmpty()
        // With progress shown the sheet follows the journey design: no description or footer line.
        sheetDescription.visibility = if (showProgress) View.GONE else View.VISIBLE
        sheetFooter.visibility = sheetDescription.visibility
        sheetTitle.textSize = if (showProgress) 22f else 25f
        if (journey != null && showProgress) {
            showSteps(journey.id, journey.recordedSteps)
            stepsTotal.text = "/ ${format(journey.requiredSteps)} bước"
            progressPercent.text = percent(journey.recordedSteps, journey.requiredSteps)
            // Same journey: the bar slides with the map walker instead of jumping after a sync.
            journeyProgress.setProgressCompat(journey.progressPercent, shownStepsJourneyId == journey.id)
            progressCaption.text = progressCaptionFor(state.stage)
            progressSection.contentDescription = "Đã đi ${format(journey.recordedSteps)} trên ${format(journey.requiredSteps)} bước, " +
                progressPercent.text
        }
        tertiaryButton.visibility = if (showProgress && journey?.status == "IN_PROGRESS" &&
            state.stage != ExploreStage.PERMISSION_REQUIRED) View.VISIBLE else View.GONE
        tertiaryButton.isEnabled = state.stage != ExploreStage.SYNCING
        demoStepsButton.visibility = if (com.project.bcv.BuildConfig.STEP_TEST_ENABLED) View.VISIBLE else View.GONE
        demoStepsButton.isEnabled = true
        demoStepsButton.alpha = 1f
        if (renderedStage != state.stage) {
            if (renderedStage != null) {
                ScreenMotion.reveal(statusCard)
                if (titleChanged) ScreenMotion.reveal(sheetTitle)
            }
            renderedStage = state.stage
        }
    }

    private fun copyFor(state: ExploreUiState): ScreenCopy {
        val journey = state.snapshot?.journey
        return when (state.stage) {
            ExploreStage.LOADING -> ScreenCopy("ĐANG TẢI DỮ LIỆU KHÁM PHÁ", "Đang kiểm tra vị trí và hành trình.",
                "Bản đồ của bạn\nsắp sẵn sàng.", "Vui lòng chờ dữ liệu được xác nhận.",
                "Đang tải...", footer = "Chưa hiển thị số liệu trước khi xác nhận.", primaryEnabled = false,
                noticeBackground = 0xFFEAF2F9.toInt(), noticeColor = 0xFF4B88AD.toInt())
            ExploreStage.LOAD_FAILED -> ScreenCopy("CHƯA TẢI ĐƯỢC DỮ LIỆU", "Không có dữ liệu khám phá mới.",
                "Chưa thể mở\nmàn khám phá.", state.message.orEmpty(), "Thử tải lại",
                footer = "Vị trí và hành trình cũ vẫn được giữ nguyên.",
                noticeBackground = 0xFFFCEFEA.toInt(), noticeColor = 0xFFC36E59.toInt())
            ExploreStage.SESSION_EXPIRED -> ScreenCopy("CẦN ĐĂNG NHẬP LẠI", "Phiên làm việc không còn hiệu lực.",
                "Tiếp tục hành trình\ncủa bạn.", state.message.orEmpty(), "Đăng nhập lại",
                footer = "Dữ liệu khám phá đã lưu vẫn thuộc tài khoản của bạn.",
                noticeBackground = 0xFFF0EDFA.toInt(), noticeColor = 0xFF7665B6.toInt())
            ExploreStage.RESTORING -> ScreenCopy("ĐANG KIỂM TRA HÀNH TRÌNH", "Chưa hiển thị tiến độ mới trên bản đồ.",
                "Đang khôi phục...", "Đang tải lại hành trình và số bước đã lưu.",
                "Đang khôi phục...", footer = "Vị trí ảo vẫn được giữ nguyên.", primaryEnabled = false,
                noticeBackground = 0xFFF0EDFA.toInt(), noticeColor = 0xFF7665B6.toInt())
            ExploreStage.RESTORE_FAILED -> ScreenCopy("CHƯA TẢI ĐƯỢC HÀNH TRÌNH", "Không tạo hành trình mới khi lỗi.",
                "Chưa thể tiếp nối\nhành trình của bạn.", state.message.orEmpty(), "Thử khôi phục lại",
                footer = "Dữ liệu đã lưu không bị thay đổi.",
                noticeBackground = 0xFFFCEFEA.toInt(), noticeColor = 0xFFC36E59.toInt())
            ExploreStage.EMPTY -> ScreenCopy("Chưa có địa danh đã khám phá", "Chọn một điểm đến để bắt đầu.",
                "Điểm đến\nđang chờ bạn.", "Hiện chưa có hành trình đang thực hiện.",
                "Gợi ý điểm đến", footer = "Gợi ý được tìm từ vị trí ảo hiện tại.")
            ExploreStage.ACTIVE -> ScreenCopy("Hành trình đang thực hiện", "Tiến độ được xác nhận từ số bước đã đồng bộ.",
                journey?.destinationName.orEmpty(), "Tiếp tục hành trình khám phá của bạn.",
                "Xem hành trình", secondary = "Đồng bộ", footer = "Vị trí ảo cập nhật khi hoàn thành hành trình.",
                noticeBackground = 0xFFEAF2F9.toInt(), noticeColor = 0xFF4B88AD.toInt())
            // Same buttons as ACTIVE so nothing moves; Đồng bộ is disabled while it runs.
            ExploreStage.SYNCING -> ScreenCopy("Đang đồng bộ bước chân", "Đang gửi tiến độ đã xác nhận.",
                journey?.destinationName.orEmpty(), "Vui lòng chờ kết quả từ máy chủ.",
                "Xem hành trình", secondary = "Đồng bộ", footer = "Không gửi yêu cầu trùng trong lúc đồng bộ.",
                noticeBackground = 0xFFEAF2F9.toInt(), noticeColor = 0xFF4B88AD.toInt())
            ExploreStage.SYNC_FAILED -> ScreenCopy("Đồng bộ chưa thành công", "Tiến độ vẫn giữ ở trạng thái đã lưu.",
                journey?.destinationName.orEmpty(), state.message.orEmpty(), "Thử lại",
                secondary = "Đồng bộ", footer = "Chỉ cập nhật sau khi máy chủ xác nhận.",
                noticeBackground = 0xFFFCEFEA.toInt(), noticeColor = 0xFFC36E59.toInt())
            ExploreStage.NO_NEW_STEPS -> ScreenCopy("Chưa có bước chân mới", "Không cộng lại bước đã ghi nhận.",
                journey?.destinationName.orEmpty(), "Tiến độ hiện tại của bạn đã được giữ nguyên.",
                "Xem hành trình", secondary = "Đồng bộ", footer = "Cập nhật lại khi có bước chân mới.")
            ExploreStage.PERMISSION_REQUIRED -> ScreenCopy("Chưa có quyền bước chân", "Tiến độ đã lưu vẫn được giữ nguyên.",
                journey?.destinationName.orEmpty(), "Cho phép Health Connect hoặc Hoạt động thể chất để đếm bước.",
                "Cho phép đếm bước", secondary = "Xem hành trình đã lưu",
                footer = "Giữ nguyên tiến độ đã được xác nhận.",
                noticeBackground = 0xFFFFF0D9.toInt(), noticeColor = 0xFFAD7A32.toInt())
            ExploreStage.COMPLETED -> ScreenCopy("Hành trình hoàn thành", "",
                journey?.destinationName.orEmpty(), "Bạn đã đến điểm đích của hành trình.",
                "Khám phá tiếp", footer = "Tiến độ hoàn thành được lưu trong hành trình.")
        }
    }

    private fun openJourney() {
        journeyLauncher.launch(Intent(this, JourneyActivity::class.java))
        BottomNav.skipTransition(this, opening = true)
        returningFromTab = true
    }

    /** Shows the user's avatar on the profile button; without one (or on any error) the person icon stays. */
    private fun loadProfileAvatar() {
        lifecycleScope.launch {
            val avatar = try {
                withContext(Dispatchers.IO) { ProfileModule.repository(this@ExploreActivity).profile().avatar }
            } catch (_: Exception) { return@launch }
            val bitmap = Avatars.immediate(avatar) ?: avatar?.let { Avatars.load(it) }
            binding.profileAvatar.setImageBitmap(bitmap)
            binding.profileAvatar.visibility = if (bitmap == null) View.GONE else View.VISIBLE
            binding.profileIcon.visibility = if (bitmap == null) View.VISIBLE else View.GONE
        }
    }

    /** A destination picked there starts a journey; the result reloads this screen like the place detail. */
    private fun openRecommendations() {
        placeDetailLauncher.launch(Intent(this, RecommendationActivity::class.java)
            .putExtra(RecommendationActivity.EXTRA_ADDRESS, model.address.value))
    }

    /** Counts the step number up to [steps] after a sync on the same journey; otherwise shows it at once. */
    private fun showSteps(journeyId: Long, steps: Int) {
        val from = shownSteps
        val sameJourney = shownStepsJourneyId == journeyId
        shownStepsJourneyId = journeyId
        if (sameJourney && steps == from) return
        stepsAnimator?.cancel()
        shownSteps = steps
        if (!sameJourney || steps < from || !ValueAnimator.areAnimatorsEnabled()) {
            binding.stepsCount.text = format(steps)
            return
        }
        stepsAnimator = ValueAnimator.ofInt(from, steps).apply {
            duration = 1_000L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { binding.stepsCount.text = format(it.animatedValue as Int) }
            start()
        }
    }

    private fun format(value: Int) = String.format(Locale.forLanguageTag("vi-VN"), "%,d", value)

    /** One decimal like the design (54,5%), dropping a trailing ",0". */
    private fun percent(recorded: Int, required: Int): String {
        val value = if (required <= 0) 0.0 else (recorded * 100.0 / required).coerceIn(0.0, 100.0)
        val text = String.format(Locale.forLanguageTag("vi-VN"), "%.1f", value).removeSuffix(",0")
        return "$text%"
    }

    private fun progressCaptionFor(stage: ExploreStage) = when (stage) {
        ExploreStage.NO_NEW_STEPS -> "Đã kiểm tra · Không có bước mới"
        ExploreStage.SYNCING -> "Đang đồng bộ bước chân..."
        ExploreStage.SYNC_FAILED -> "Chưa đồng bộ được · Giữ tiến độ đã lưu"
        ExploreStage.PERMISSION_REQUIRED -> "Quyền bước chân đã tắt · Giữ tiến độ đã lưu"
        ExploreStage.RESTORING -> "Đang khôi phục tiến độ..."
        ExploreStage.COMPLETED -> "Đã hoàn thành hành trình"
        else -> "Tiến độ xác nhận từ số bước đã đồng bộ"
    }

    private data class ScreenCopy(
        val noticeTitle: String,
        val noticeDetail: String,
        val title: String,
        val description: String,
        val primary: String,
        val secondary: String = "",
        val footer: String,
        val primaryEnabled: Boolean = true,
        val noticeBackground: Int = 0xFFEAF5E8.toInt(),
        val noticeColor: Int = 0xFF07836D.toInt()
    )

    private val mapErrorCopy = ScreenCopy("BẢN ĐỒ CHƯA KHẢ DỤNG", "Vị trí ảo và hành trình vẫn được giữ nguyên.",
        "Chưa tải được\nbản đồ khám phá.", "Hãy kiểm tra kết nối và thử tải lại.",
        "Tải lại bản đồ", footer = "Không đổi dữ liệu khi bản đồ đang lỗi.",
        noticeBackground = 0xFFFCEFEA.toInt(), noticeColor = 0xFFC36E59.toInt())

    private companion object {
        const val SHEET_STATE_KEY = "explore_sheet_state"
        const val STEPS_PERMISSION_ASKED_KEY = "explore_steps_permission_asked"
        const val AUTO_SYNC_INTERVAL_MS = 60_000L
    }
}
