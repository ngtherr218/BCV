package com.project.bcv.feature.questitem.inventory.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.ViewModelProvider
import com.project.bcv.core.di.InventoryModule
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.viewModelFactory
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.core.ui.openSignIn
import com.project.bcv.core.ui.setUpDetailScreen
import com.project.bcv.core.ui.showContent
import com.project.bcv.core.ui.showError
import com.project.bcv.databinding.ActivityFlightTicketBinding
import com.project.bcv.feature.questitem.inventory.domain.FlightTicketStatus

/** Flight ticket: pick another region and a place there, then move the virtual location to it and unlock it. */
class FlightTicketActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFlightTicketBinding
    private lateinit var model: FlightTicketViewModel
    private var status: FlightTicketStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlightTicketBinding.inflate(layoutInflater)
        setUpDetailScreen(binding.root)
        binding.topBar.screenTitle.text = "Vé máy bay"
        binding.topBar.backButton.setOnClickListener { finish() }
        binding.backToInventory.setOnClickListener { finish() }
        model = ViewModelProvider(this, viewModelFactory { FlightTicketViewModel(InventoryModule.items(this)) })[
            FlightTicketViewModel::class.java]
        binding.state.stateRetry.setOnClickListener { model.load() }
        binding.regionPicker.setOnClickListener { pickRegion() }
        binding.placePicker.setOnClickListener { pickPlace() }
        binding.moveButton.setOnClickListener { confirmMove() }
        model.state.observe(this) { state ->
            if (state != Remote.Loading) Skeleton.hide(binding.skeleton.root)
            when (state) {
                Remote.Loading -> Skeleton.show(binding.skeleton.root, binding.content, binding.state.root)
                is Remote.Failed -> binding.state.showError(binding.content, state.message)
                Remote.SessionExpired -> openSignIn()
                is Remote.Ready -> {
                    val first = status == null
                    status = state.value
                    binding.state.showContent(binding.content)
                    if (first) ScreenMotion.reveal(binding.content)
                    render()
                }
            }
        }
        model.selection.observe(this) { render() }
        model.message.observe(this) { event ->
            event.take()?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
        model.moved.observe(this) { event ->
            val result = event.take() ?: return@observe
            setResult(RESULT_OK)
            MaterialAlertDialogBuilder(this)
                .setTitle("Đã đổi vị trí ảo")
                .setMessage("Vị trí ảo mới: ${result.place}, ${result.region}. Địa danh đã được mở khóa. Còn ${result.quantity} vé.")
                .setCancelable(false)
                .setPositiveButton("Xong") { _, _ -> finish() }
                .show()
        }
    }

    private fun render() = with(binding) {
        val status = status ?: return@with
        val (region, places, place, moving) = model.selection.value ?: return@with
        ownedChip.text = "Đang có × ${status.quantity}"
        currentRegion.text = "Khác khu vực hiện tại: ${status.currentRegion ?: "chưa xác định"}"
        regionName.text = region?.name ?: "Chưa có khu vực khác"
        regionHint.text = region?.let { "${it.placeCount} địa danh đang hoạt động" } ?: "Không có khu vực nào để chọn."
        placeName.text = when {
            region == null -> "Chưa có địa danh"
            places == null -> "Đang tải địa danh…"
            place == null -> "Khu vực chưa có địa danh"
            else -> place.name
        }
        placeHint.text = if (place?.discovered == true) "Điểm đến hợp lệ theo hệ thống.\nBạn đã khám phá địa danh này."
            else "Điểm đến hợp lệ theo hệ thống.\nĐến nơi sẽ mở khóa nội dung và thẻ."
        placeHint.visibility = if (place == null) View.GONE else View.VISIBLE

        rulePanel.backgroundTintList = ColorStateList.valueOf(
            if (status.hasActiveJourney) 0xFFF6EEDF.toInt() else 0xFFE6EDF0.toInt())
        ruleTitle.text = if (status.hasActiveJourney) "Có hành trình đang thực hiện" else "Không có hành trình đang thực hiện"
        ruleTitle.compoundDrawableTintList = ColorStateList.valueOf(
            if (status.hasActiveJourney) 0xFFA0702E.toInt() else BLUE)

        val reason = when {
            !status.active -> "Vật phẩm tạm ngưng"
            status.quantity <= 0 -> "Chưa sở hữu vé"
            status.hasActiveJourney -> "Hành trình chưa kết thúc"
            place == null -> "Chọn địa danh đến"
            else -> null
        }
        moveButton.isEnabled = reason == null && !moving
        moveButton.text = if (moving) "Đang di chuyển…" else reason ?: "Di chuyển vị trí ảo"
        moveButton.backgroundTintList = ColorStateList.valueOf(if (moveButton.isEnabled) BLUE else 0xFFE4E7E1.toInt())
        moveButton.setTextColor(if (moveButton.isEnabled) 0xFFFFFFFF.toInt() else 0xFF8A8F86.toInt())
    }

    private fun pickRegion() {
        val regions = status?.regions.orEmpty()
        if (regions.isEmpty()) return
        val region = model.selection.value?.region
        MaterialAlertDialogBuilder(this)
            .setTitle("Chọn khu vực đích")
            .setItems(regions.map { "${it.name} · ${it.placeCount} địa danh" }.toTypedArray()) { _, index ->
                if (regions[index] != region) model.selectRegion(regions[index])
            }
            .show()
    }

    private fun pickPlace() {
        val options = model.selection.value?.places.orEmpty()
        if (options.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Chọn địa danh đến")
            .setItems(options.map { if (it.discovered) "${it.name} · đã khám phá" else it.name }.toTypedArray()) { _, index ->
                model.selectPlace(options[index])
            }
            .show()
    }

    private fun confirmMove() {
        val (region, _, target) = model.selection.value ?: return
        if (target == null) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Dùng 1 vé máy bay?")
            .setMessage("Vị trí ảo sẽ chuyển đến ${target.name}, ${region?.name}. Bạn còn ${status?.quantity} vé.")
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Di chuyển") { _, _ -> model.move() }
            .show()
    }

    private companion object { const val BLUE = 0xFF3C7DB0.toInt() }
}
