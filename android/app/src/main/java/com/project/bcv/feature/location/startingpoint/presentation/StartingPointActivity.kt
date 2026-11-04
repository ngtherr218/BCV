package com.project.bcv.feature.location.startingpoint.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.project.bcv.R
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.core.di.StartingPointModule
import com.project.bcv.databinding.ActivityStartingPointBinding
import com.project.bcv.feature.location.startingpoint.domain.StartingPointSource

class StartingPointActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartingPointBinding
    private lateinit var model: StartingPointViewModel
    private lateinit var goongMap: GoongStartingMap
    private var renderedStage: StartingPointStage? = null
    private var askedPrecise = false
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) model.locate() else model.permissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityStartingPointBinding.inflate(layoutInflater)
        setContentView(binding.root)
        goongMap = GoongStartingMap(this, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        model = ViewModelProvider(this, StartingPointViewModelFactory(
            StartingPointModule.repository(this), StartingPointModule.locationSource(this),
            StartingPointModule.addressLookup()
        ))[StartingPointViewModel::class.java]
        model.state.observe(this, ::render)
        binding.backButton.setOnClickListener { finish() }
        binding.suggestionCard.setOnClickListener { model.useSuggestion() }
        binding.currentCard.setOnClickListener { requestCurrentLocation() }
        binding.primaryButton.setOnClickListener { onPrimaryClick() }
        binding.secondaryButton.setOnClickListener { onSecondaryClick() }
        if (savedInstanceState == null) model.load()
    }

    override fun onStart() {
        super.onStart()
        goongMap.onStart()
    }

    override fun onResume() {
        super.onResume()
        goongMap.onResume()
    }

    override fun onPause() {
        goongMap.onPause()
        super.onPause()
    }

    override fun onStop() {
        goongMap.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        goongMap.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        goongMap.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        goongMap.onDestroy()
        super.onDestroy()
    }

    private fun requestCurrentLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        // Approximate-only access is shifted by kilometres, so ask to upgrade it to precise once.
        if (fineGranted || (coarseGranted && askedPrecise)) {
            model.locate()
        } else {
            askedPrecise = true
            model.waitingForPermission()
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun onPrimaryClick() {
        when (model.state.value?.stage) {
            StartingPointStage.READY -> model.saveSuggestion()
            StartingPointStage.VALID_CURRENT -> model.saveCurrent()
            StartingPointStage.PERMISSION_DENIED, StartingPointStage.OUTSIDE_AREA -> model.useSuggestion()
            StartingPointStage.NO_FIX -> requestCurrentLocation()
            StartingPointStage.NO_SUGGESTION -> model.load()
            StartingPointStage.SAVE_FAILED -> model.retrySave()
            StartingPointStage.SAVED -> finishStep()
            else -> Unit
        }
    }

    private fun onSecondaryClick() {
        when (model.state.value?.stage) {
            StartingPointStage.VALID_CURRENT, StartingPointStage.NO_FIX,
            StartingPointStage.LOCATING -> model.useSuggestion()
            StartingPointStage.PERMISSION_DENIED, StartingPointStage.OUTSIDE_AREA,
            StartingPointStage.NO_SUGGESTION -> requestCurrentLocation()
            StartingPointStage.SAVE_FAILED -> {
                if (model.state.value?.pendingSource == StartingPointSource.CURRENT) model.useSuggestion()
                else requestCurrentLocation()
            }
            else -> Unit
        }
    }

    private fun finishStep() {
        setResult(Activity.RESULT_OK, Intent())
        finish()
    }

    private fun render(state: StartingPointUiState) = with(binding) {
        if (state.alreadySaved && state.saved != null) {
            finishStep()
            return@with
        }
        suggestedRegion.text = state.suggested?.let { "${it.regionName} · Việt Nam" } ?: "Chưa có điểm đề xuất"
        val ready = state.stage == StartingPointStage.READY
        readyContent.visibility = if (ready) View.VISIBLE else View.GONE
        stateContent.visibility = if (ready) View.GONE else View.VISIBLE

        val copy = copyFor(state)
        stateEyebrow.text = copy.eyebrow
        stateTitleTop.text = copy.titleTop
        stateTitleAccent.text = copy.titleAccent
        stateTitleAccent.setTextColor(copy.accent)
        stateDescription.text = copy.description
        stateDetailTitle.text = copy.detailTitle
        stateDetailBody.text = copy.detailBody
        stateNotice.text = copy.notice
        stateNotice.visibility = if (copy.notice.isEmpty()) View.GONE else View.VISIBLE
        stateDetailCard.visibility = if (copy.detailTitle.isEmpty()) View.GONE else View.VISIBLE
        stateDetailIcon.setImageResource(copy.detailIcon)

        val mapState = state.stage == StartingPointStage.VALID_CURRENT ||
            state.stage == StartingPointStage.OUTSIDE_AREA
        stateMapContainer.visibility = if (mapState) View.VISIBLE else View.GONE
        stateHero.visibility = if (mapState || ready) View.GONE else View.VISIBLE
        when {
            ready -> goongMap.show(readyMapHost, state.suggested?.coordinates)
            mapState -> goongMap.show(stateMapHost, state.currentCoordinates, GoongStartingMap.CURRENT_ZOOM)
        }
        if (!mapState && !ready) {
            stateHero.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(copy.orbit)
                setStroke(resources.displayMetrics.density.toInt().coerceAtLeast(1), copy.accent)
            }
            stateIcon.backgroundTintList = ColorStateList.valueOf(copy.accent)
            stateIcon.imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            val spinning = state.stage == StartingPointStage.LOADING ||
                state.stage == StartingPointStage.LOCATING || state.stage == StartingPointStage.SAVING
            stateSpinner.visibility = if (spinning) View.VISIBLE else View.GONE
            stateIcon.visibility = if (spinning) View.GONE else View.VISIBLE
            if (!spinning) stateIcon.setImageResource(copy.heroIcon)
        }

        primaryButton.text = copy.primary
        primaryButton.isEnabled = copy.primaryEnabled
        primaryButton.icon = if (copy.primaryEnabled) getDrawable(R.drawable.ic_arrow) else null
        primaryButton.backgroundTintList = ColorStateList.valueOf(
            if (copy.primaryEnabled) ContextCompat.getColor(this@StartingPointActivity, R.color.teal)
            else 0xFFE0EAE6.toInt()
        )
        primaryButton.setTextColor(if (copy.primaryEnabled) 0xFFFFFFFF.toInt() else 0xFF718E86.toInt())
        secondaryButton.visibility = if (copy.secondary.isEmpty()) View.GONE else View.VISIBLE
        secondaryButton.text = copy.secondary
        footer.text = copy.footer
        if (renderedStage != state.stage) {
            contentScroll.scrollTo(0, 0)
            if (renderedStage != null) ScreenMotion.reveal(if (ready) readyContent else stateContent)
            if (state.stage == StartingPointStage.SAVED) {
                stateIcon.scaleX = 0.72f
                stateIcon.scaleY = 0.72f
                stateIcon.animate().scaleX(1f).scaleY(1f).setDuration(320).start()
            }
        }
        renderedStage = state.stage
    }

    private fun copyFor(state: StartingPointUiState): ScreenCopy = when (state.stage) {
        StartingPointStage.READY -> ScreenCopy("", "", "", "", "", "", "",
            "Xác nhận vị trí ảo", "", "Dùng làm điểm khởi đầu cho hành trình khám phá.")
        StartingPointStage.LOADING -> ScreenCopy("ĐANG TẢI ĐIỂM ĐỀ XUẤT", "Đang chuẩn bị", "điểm xuất phát.",
            "Ứng dụng đang kiểm tra vị trí ảo và điểm hệ thống đề xuất.", "Điểm hệ thống đề xuất",
            "Đang tải dữ liệu hỗ trợ.", "", "Đang tải...", "", "Chưa có điểm nào được lưu.",
            primaryEnabled = false, heroIcon = R.drawable.ic_starting_map)
        StartingPointStage.PERMISSION_WAIT -> ScreenCopy("BẠN QUYẾT ĐỊNH QUYỀN TRUY CẬP",
            "Cho phép lấy", "vị trí hiện tại?", "Xác nhận trên màn hình cấp quyền Android. Ứng dụng chỉ lấy vị trí sau khi bạn cho phép.",
            "Đang chờ xác nhận", "App sẽ kiểm tra khi bạn quay lại.", "", "Chờ xác nhận...", "",
            "Đây là màn hình của app, không phải hộp quyền Android.", primaryEnabled = false,
            accent = 0xFF7969B8.toInt(), orbit = 0xFFF0EDFB.toInt(), heroIcon = R.drawable.ic_health_external)
        StartingPointStage.LOCATING -> ScreenCopy("ĐANG XÁC ĐỊNH VỊ TRÍ", "Đang tìm", "vị trí của bạn.",
            "Đã có quyền truy cập vị trí. Vui lòng chờ để xác định điểm khởi đầu.", "Chưa lưu vị trí ảo",
            "Sẽ kiểm tra phạm vi trước khi xác nhận.", "", "Đang xác định vị trí...", "Dùng điểm đề xuất",
            "Bạn có thể chuyển sang cách không dùng vị trí thực.", primaryEnabled = false,
            accent = 0xFF4B91B6.toInt(), orbit = 0xFFEAF5FC.toInt(), heroIcon = R.drawable.ic_starting_target)
        StartingPointStage.VALID_CURRENT -> ScreenCopy("ĐÃ KIỂM TRA PHẠM VI HỖ TRỢ", "Bắt đầu từ", "vị trí hiện tại.",
            "Dùng điểm này làm vị trí ảo ban đầu. Bạn vẫn có thể xác nhận để lưu.", "Vị trí hiện tại",
            listOfNotNull(state.currentAddress,
                "${state.current?.regionName ?: "Vùng hỗ trợ"} · Điểm hợp lệ").joinToString("\n"),
            "", "Xác nhận vị trí ảo",
            "Đổi sang điểm đề xuất", "Chỉ lưu khi bạn xác nhận.", heroIcon = R.drawable.ic_starting_pin)
        StartingPointStage.PERMISSION_DENIED -> ScreenCopy("KHÔNG BẮT BUỘC DÙNG VỊ TRÍ THỰC",
            "Chưa cấp quyền?", "Vẫn có điểm bắt đầu.",
            "Bạn có thể chọn điểm hệ thống đề xuất mà không cần truy cập vị trí thực.",
            "Chưa truy cập vị trí của bạn", "Không tự lặp lại yêu cầu cấp quyền.",
            "Không cần quyền vị trí để chọn điểm đề xuất.", "Chọn điểm đề xuất", "Thử cấp quyền lại",
            "Chuyển sang điểm đề xuất, sau đó xác nhận vị trí ảo.",
            accent = 0xFFAF7A33.toInt(), orbit = 0xFFFCF1E1.toInt(), heroIcon = R.drawable.ic_health_shield)
        StartingPointStage.NO_FIX -> ScreenCopy("CHƯA LẤY ĐƯỢC VỊ TRÍ", "Chưa xác định được", "vị trí hiện tại.",
            "Hãy thử lấy vị trí lại hoặc chuyển sang điểm đề xuất để tiếp tục.",
            "Điểm hệ thống đề xuất", "Không cần dùng vị trí thực của bạn.", state.message.orEmpty(),
            "Thử lấy vị trí lại", "Dùng điểm đề xuất", "Chưa có vị trí hợp lệ nên chưa lưu dữ liệu.",
            accent = 0xFF718994.toInt(), orbit = 0xFFEFF3F5.toInt(), heroIcon = R.drawable.ic_starting_target)
        StartingPointStage.OUTSIDE_AREA -> ScreenCopy("VỊ TRÍ CHƯA HỢP LỆ", "Điểm này chưa nằm", "trong vùng hỗ trợ.",
            "Chưa thể dùng vị trí hiện tại làm điểm đầu. Hãy chọn một điểm hệ thống hỗ trợ.",
            "Không lưu vị trí ngoài phạm vi", "Chọn điểm đề xuất để tiếp tục.", state.message.orEmpty(),
            "Chọn điểm đề xuất", "Thử lấy vị trí lại", "Không tự lưu hoặc thay đổi vị trí đang chọn.",
            accent = 0xFFC36E59.toInt(), orbit = 0xFFFCEFEA.toInt(), heroIcon = R.drawable.ic_health_alert)
        StartingPointStage.NO_SUGGESTION -> ScreenCopy("ĐIỂM XUẤT PHÁT ẢO", "Chưa tải được", "điểm đề xuất.",
            "Hiện chưa có điểm hợp lệ để xác nhận. Thử lại hoặc dùng vị trí hiện tại.",
            "Chưa thể xác nhận vị trí", "Không lưu điểm nếu thiếu dữ liệu.", state.message.orEmpty(),
            "Tải lại điểm đề xuất", "Dùng vị trí hiện tại", "Dùng vị trí hiện tại sẽ kiểm tra quyền trước.",
            accent = 0xFF718994.toInt(), orbit = 0xFFEFF3F5.toInt(), heroIcon = R.drawable.ic_starting_map)
        StartingPointStage.SAVING -> ScreenCopy("ĐANG LƯU VỊ TRÍ", "Đang lưu điểm", "xuất phát của bạn.",
            "Vui lòng chờ hệ thống xác nhận. Không gửi lại yêu cầu trong lúc đang lưu.",
            state.pendingSource.label, "Lựa chọn của bạn đang được ghi lại.", "", "Đang lưu vị trí...", "",
            "Chưa chuyển sang khám phá khi chưa xác nhận lưu thành công.", primaryEnabled = false,
            accent = 0xFF4B91B6.toInt(), orbit = 0xFFEAF5FC.toInt(), heroIcon = R.drawable.ic_starting_pin)
        StartingPointStage.SAVE_FAILED -> ScreenCopy("CHƯA HOÀN TẤT KHỞI TẠO", "Chưa lưu được", "điểm xuất phát.",
            "Lựa chọn của bạn vẫn được giữ lại. Thử lưu lại khi kết nối sẵn sàng.",
            state.pendingSource.label, "Chưa được xác nhận lưu thành công.", state.message.orEmpty(),
            "Thử lưu lại", "Đổi cách khởi tạo", "Không báo hoàn thành khi yêu cầu lưu thất bại.",
            accent = 0xFFC36E59.toInt(), orbit = 0xFFFCEFEA.toInt(), heroIcon = R.drawable.ic_health_alert)
        StartingPointStage.SAVED -> ScreenCopy("ĐÃ KHỞI TẠO VỊ TRÍ", "Điểm đầu", "đã sẵn sàng.",
            "Vị trí ảo ban đầu đã được lưu. Bắt đầu khám phá từ điểm bạn đã chọn.",
            state.savedAddress ?: state.saved?.regionName ?: "Điểm xuất phát ảo",
            listOfNotNull(state.saved?.regionName?.takeIf { state.savedAddress != null },
                "Đã lưu thành công.").joinToString(" · "), "",
            "Bắt đầu khám phá", "", "Không tạo hành trình hoặc cộng bước ở bước này.",
            heroIcon = R.drawable.ic_check)
    }

    private val StartingPointSource.label: String
        get() = if (this == StartingPointSource.CURRENT) "Vị trí hiện tại" else "Điểm hệ thống đề xuất"

    private data class ScreenCopy(
        val eyebrow: String,
        val titleTop: String,
        val titleAccent: String,
        val description: String,
        val detailTitle: String,
        val detailBody: String,
        val notice: String,
        val primary: String,
        val secondary: String,
        val footer: String,
        val primaryEnabled: Boolean = true,
        val accent: Int = 0xFF07836D.toInt(),
        val orbit: Int = 0xFFF0F6EC.toInt(),
        val heroIcon: Int = R.drawable.ic_starting_pin,
        val detailIcon: Int = R.drawable.ic_starting_map
    )
}
