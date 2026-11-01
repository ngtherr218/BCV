package com.project.bcv.feature.account.healthconnect.presentation

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.ViewModelProvider
import com.project.bcv.R
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.databinding.ActivityHealthConnectBinding
import com.project.bcv.feature.account.healthconnect.data.HealthConnectRepositoryImpl

class HealthConnectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHealthConnectBinding
    private lateinit var model: HealthConnectViewModel
    private var renderedStage: HealthConnectStage? = null
    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { model.permissionRequestReturned() }

    private val externalLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { model.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityHealthConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        model = ViewModelProvider(
            this, HealthConnectViewModelFactory(HealthConnectRepositoryImpl(this))
        )[HealthConnectViewModel::class.java]
        model.stage.observe(this, ::render)
        binding.backButton.setOnClickListener { finish() }
        binding.primaryButton.setOnClickListener { onPrimaryClick() }
        binding.secondaryButton.setOnClickListener { finishStep(granted = false) }
        if (savedInstanceState == null) model.refresh()
    }

    private fun onPrimaryClick() {
        when (model.stage.value) {
            HealthConnectStage.READY, HealthConnectStage.DENIED,
            HealthConnectStage.REQUEST_FAILED -> {
                model.permissionRequestStarted()
                try {
                    permissionLauncher.launch(setOf(readStepsPermission))
                } catch (_: Exception) {
                    model.permissionRequestFailed()
                }
            }
            HealthConnectStage.GRANTED -> finishStep(granted = true)
            HealthConnectStage.INSTALL_OR_UPDATE -> openHealthConnectStore()
            HealthConnectStage.UNSUPPORTED -> finishStep(granted = false)
            HealthConnectStage.REVOKED, HealthConnectStage.SETTINGS_REQUIRED -> openHealthConnectSettings()
            else -> Unit
        }
    }

    private fun openHealthConnectSettings() {
        try {
            externalLauncher.launch(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
        } catch (_: Exception) {
            model.permissionRequestFailed()
        }
    }

    private fun openHealthConnectStore() {
        val packageId = "com.google.android.apps.healthdata"
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageId"))
            .setPackage("com.android.vending")
        val browser = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageId"))
        try {
            externalLauncher.launch(market)
        } catch (_: Exception) {
            try {
                externalLauncher.launch(browser)
            } catch (_: Exception) {
                model.permissionRequestFailed()
            }
        }
    }

    private fun finishStep(granted: Boolean) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_READ_STEPS_GRANTED, granted))
        finish()
    }

    private fun render(stage: HealthConnectStage) = with(binding) {
        val copy = screenCopy(stage)
        initialHero.visibility = if (stage == HealthConnectStage.READY) View.VISIBLE else View.GONE
        stateHero.visibility = if (stage == HealthConnectStage.READY) View.GONE else View.VISIBLE
        if (stage != HealthConnectStage.READY) {
            stateOrbit.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(copy.orbitColor)
                setStroke(dp(1), copy.orbitStrokeColor)
            }
            stateIconCircle.backgroundTintList = ColorStateList.valueOf(copy.iconColor)
            stateSpinner.visibility = if (stage == HealthConnectStage.CHECKING ||
                stage == HealthConnectStage.WAITING_FOR_SYSTEM) View.VISIBLE else View.GONE
            stateIcon.visibility = if (stateSpinner.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (stateIcon.visibility == View.VISIBLE) stateIcon.setImageResource(copy.icon)
        }
        eyebrow.text = copy.eyebrow
        headlineTop.text = copy.titleTop
        headlineAccent.text = copy.titleAccent
        headlineAccent.setTextColor(copy.accentColor)
        description.text = copy.description
        detailTitle.text = copy.detailTitle
        detailBody.text = copy.detailBody
        detailFootnote.text = copy.detailFootnote
        detailFootnote.visibility = if (copy.detailFootnote.isEmpty()) View.GONE else View.VISIBLE
        detailIcon.setImageResource(copy.detailIcon)
        detailIcon.imageTintList = ColorStateList.valueOf(copy.accentColor)
        detailIconBox.backgroundTintList = ColorStateList.valueOf(copy.detailIconBackground)
        privacyRow.visibility = if (copy.showPrivacy) View.VISIBLE else View.GONE
        notice.visibility = if (copy.notice.isEmpty()) View.GONE else View.VISIBLE
        notice.text = copy.notice
        primaryButton.text = copy.primaryText
        primaryButton.isEnabled = copy.primaryEnabled
        primaryButton.icon = if (copy.primaryEnabled) getDrawable(R.drawable.ic_arrow) else null
        primaryButton.backgroundTintList = ColorStateList.valueOf(
            if (copy.primaryEnabled) color(R.color.teal) else 0xFFE0EAE6.toInt()
        )
        primaryButton.setTextColor(if (copy.primaryEnabled) color(R.color.white) else 0xFF688D91.toInt())
        secondaryButton.visibility = if (copy.showSecondary) View.VISIBLE else View.GONE
        footer.text = copy.footer
        if (renderedStage != stage) {
            contentScroll.scrollTo(0, 0)
            if (renderedStage != null) ScreenMotion.reveal(contentScroll)
        }
        renderedStage = stage
    }

    private fun screenCopy(stage: HealthConnectStage): ScreenCopy = when (stage) {
        HealthConnectStage.READY -> ScreenCopy(
            "BƯỚC CHÂN THÀNH HÀNH TRÌNH", "Biến bước chân", "thành hành trình.",
            "Cho phép đọc số bước từ Health Connect và đồng bộ để cập nhật tiến độ hành trình.",
            "Số bước chân", "Số bước cùng thời gian ghi nhận.", "Bước này chỉ xin quyền đọc Health Connect",
            "Nếu để sau, app có thể dùng cảm biến sau khi bạn cho phép Hoạt động thể chất.",
            "Cho phép đọc bước chân", "Bạn sẽ xác nhận quyền trên màn hình hệ thống.",
            R.drawable.ic_directions_walk, R.drawable.ic_health_link
        )
        HealthConnectStage.CHECKING -> ScreenCopy(
            "ĐANG KIỂM TRA KẾT NỐI", "Chuẩn bị kết nối", "bước chân của bạn.",
            "Ứng dụng đang kiểm tra Health Connect và quyền đọc bước chân.",
            "Health Connect", "Đang kiểm tra khả dụng và quyền hiện tại.", "",
            "", "Đang kiểm tra...", "Chưa đọc dữ liệu trong lúc kiểm tra.",
            R.drawable.ic_health_link, R.drawable.ic_health_link,
            accentColor = 0xFF4B91B6.toInt(), iconColor = 0xFF4B91B6.toInt(),
            orbitColor = 0xFFEAF5FC.toInt(), orbitStrokeColor = 0xFFD2E7F4.toInt(),
            detailIconBackground = 0xFFEAF5FC.toInt(), primaryEnabled = false, showPrivacy = false
        )
        HealthConnectStage.WAITING_FOR_SYSTEM -> ScreenCopy(
            "XÁC NHẬN TRÊN ANDROID", "Tiếp tục trên", "màn hình hệ thống.",
            "Chọn cho phép hoặc từ chối trên Android. Quyết định cấp quyền luôn thuộc về bạn.",
            "Đang chờ bạn xác nhận", "App sẽ kiểm tra quyền khi bạn quay lại.", "",
            "", "Chờ xác nhận...", "Đây là trạng thái app, không phải hộp cấp quyền Android.",
            R.drawable.ic_health_external, R.drawable.ic_health_external,
            accentColor = 0xFF7769B8.toInt(), iconColor = 0xFF7769B8.toInt(),
            orbitColor = 0xFFF1EEFC.toInt(), orbitStrokeColor = 0xFFDFD8F3.toInt(),
            detailIconBackground = 0xFFF1EEFC.toInt(), primaryEnabled = false,
            showSecondary = false, showPrivacy = false
        )
        HealthConnectStage.GRANTED -> ScreenCopy(
            "ĐÃ CẤP QUYỀN", "Đã kết nối.", "", "Bạn đã cho phép đọc bước chân từ Health Connect.",
            "Quyền đọc bước chân", "Đã cho phép · Quyền ghi chỉ hỏi khi dùng bước thử", "",
            "", "Tiếp tục", "Có quyền đọc không đồng nghĩa đã có dữ liệu bước chân.",
            R.drawable.ic_health_shield, R.drawable.ic_check,
            showSecondary = false, showPrivacy = false
        )
        HealthConnectStage.DENIED -> ScreenCopy(
            "BẠN CÓ THỂ QUYẾT ĐỊNH SAU", "Chưa có quyền", "đọc bước chân.",
            "Không sao, bạn có thể kết nối lại bất cứ khi nào sẵn sàng.",
            "Đếm bước bằng điện thoại", "Có thể dùng cảm biến với quyền Hoạt động thể chất.", "",
            "Chưa truy cập dữ liệu Health Connect.", "Thử cấp lại", "Bạn có thể kết nối lại khi sẵn sàng.",
            R.drawable.ic_health_block, R.drawable.ic_health_block,
            accentColor = 0xFFB18443.toInt(), iconColor = 0xFFB18443.toInt(),
            orbitColor = 0xFFFBF1E2.toInt(), orbitStrokeColor = 0xFFF1DDBE.toInt(),
            detailIconBackground = 0xFFFBF1E2.toInt(), showPrivacy = false
        )
        HealthConnectStage.INSTALL_OR_UPDATE -> ScreenCopy(
            "CẦN CÀI / CẬP NHẬT", "Chuẩn bị", "Health Connect.",
            "Cần cài đặt hoặc cập nhật trước khi yêu cầu quyền đọc bước chân.",
            "Bước tiếp theo", "Mở trang cài đặt / cập nhật phù hợp.", "Quay lại app để kiểm tra kết nối.",
            "Chỉ áp dụng khi Health Connect yêu cầu cài hoặc cập nhật.",
            "Cài đặt / cập nhật", "Chỉ áp dụng khi Health Connect yêu cầu cài / cập nhật.",
            R.drawable.ic_health_phone, R.drawable.ic_health_phone,
            accentColor = 0xFFAF7A33.toInt(), iconColor = 0xFFAF7A33.toInt(),
            orbitColor = 0xFFFAF0E0.toInt(), orbitStrokeColor = 0xFFF2E0C3.toInt(),
            detailIconBackground = 0xFFFAF0E0.toInt(), showPrivacy = false
        )
        HealthConnectStage.UNSUPPORTED -> ScreenCopy(
            "THIẾT BỊ KHÔNG HỖ TRỢ", "Thiết bị này", "chưa được hỗ trợ.",
            "Chưa thể sử dụng Health Connect trên thiết bị hiện tại.",
            "Đếm bước bằng điện thoại", "App có thể dùng cảm biến sau khi được cấp quyền Hoạt động thể chất.", "",
            "", "Tiếp tục", "Khả năng đếm bước phụ thuộc cảm biến của thiết bị.",
            R.drawable.ic_health_block, R.drawable.ic_health_block,
            accentColor = 0xFF718994.toInt(), iconColor = 0xFF718994.toInt(),
            orbitColor = 0xFFEFF3F5.toInt(), orbitStrokeColor = 0xFFDCE5E9.toInt(),
            detailIconBackground = 0xFFEFF3F5.toInt(), showSecondary = false, showPrivacy = false
        )
        HealthConnectStage.REQUEST_FAILED -> ScreenCopy(
            "YÊU CẦU CHƯA HOÀN TẤT", "Chưa mở được", "yêu cầu cấp quyền.",
            "Bạn có thể thử lại hoặc chọn Để sau. Ứng dụng chưa xác nhận được quyền đọc.",
            "Chưa thể gửi yêu cầu cấp quyền", "Chưa truy cập dữ liệu bước chân.", "",
            "Không có lỗi nào được coi là đã cấp quyền.", "Thử lại", "Bạn có thể thử mở lại yêu cầu cấp quyền.",
            R.drawable.ic_health_alert, R.drawable.ic_health_alert,
            accentColor = 0xFFC36E59.toInt(), iconColor = 0xFFC36E59.toInt(),
            orbitColor = 0xFFFCEFEA.toInt(), orbitStrokeColor = 0xFFF4DBD2.toInt(),
            detailIconBackground = 0xFFFCEFEA.toInt(), showPrivacy = false
        )
        HealthConnectStage.REVOKED -> ScreenCopy(
            "KẾT NỐI TẠM DỪNG", "Quyền đọc bước chân", "đã bị thu hồi.",
            "Ứng dụng đã dừng truy cập bước chân. Kết nối lại khi bạn muốn tiếp tục.",
            "Đọc bước mới", "Đã dừng", "Đồng bộ bước mới đã dừng",
            "Chỉ đọc lại sau khi bạn cấp lại quyền.", "Mở Health Connect", "Chỉ đọc lại sau khi bạn cấp lại quyền.",
            R.drawable.ic_health_block, R.drawable.ic_health_block,
            accentColor = 0xFFAD8049.toInt(), iconColor = 0xFFAD8049.toInt(),
            orbitColor = 0xFFFBF0E5.toInt(), orbitStrokeColor = 0xFFF0DFC9.toInt(),
            detailIconBackground = 0xFFFBF0E5.toInt(), showPrivacy = false
        )
        HealthConnectStage.SETTINGS_REQUIRED -> ScreenCopy(
            "BẠN QUẢN LÝ QUYỀN TRUY CẬP", "Bật lại quyền trong", "Health Connect.",
            "Khi không thể mở lại hộp xin quyền, bạn có thể quản lý quyền trong hệ thống.",
            "Cho phép đọc bước chân", "Mở Health Connect và kiểm tra quyền của Bước Chân Việt.", "",
            "Không cần cấp quyền sức khỏe khác.", "Mở Health Connect", "App sẽ kiểm tra lại quyền khi bạn quay về.",
            R.drawable.ic_health_external, R.drawable.ic_health_external,
            accentColor = 0xFF7769B8.toInt(), iconColor = 0xFF7769B8.toInt(),
            orbitColor = 0xFFF1EEFC.toInt(), orbitStrokeColor = 0xFFDFD8F3.toInt(),
            detailIconBackground = 0xFFF1EEFC.toInt(), showPrivacy = false
        )
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private data class ScreenCopy(
        val eyebrow: String,
        val titleTop: String,
        val titleAccent: String,
        val description: String,
        val detailTitle: String,
        val detailBody: String,
        val detailFootnote: String,
        val notice: String,
        val primaryText: String,
        val footer: String,
        val detailIcon: Int,
        val icon: Int,
        val accentColor: Int = 0xFF07836D.toInt(),
        val iconColor: Int = 0xFF07836D.toInt(),
        val orbitColor: Int = 0xFFF0F6EC.toInt(),
        val orbitStrokeColor: Int = 0xFFDCE9D9.toInt(),
        val detailIconBackground: Int = 0xFFE4F0E1.toInt(),
        val primaryEnabled: Boolean = true,
        val showSecondary: Boolean = true,
        val showPrivacy: Boolean = true
    )

    companion object {
        const val EXTRA_READ_STEPS_GRANTED = "read_steps_granted"
    }
}
