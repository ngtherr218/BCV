package com.project.bcv.feature.questitem.inventory.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.R
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
import com.project.bcv.databinding.ActivitySpeedShoesBinding
import com.project.bcv.feature.questitem.inventory.domain.SpeedShoesStatus
import java.text.NumberFormat
import java.util.Locale

/** Speed shoes: the configured boost, the journey it would apply to, and using one. */
class SpeedShoesActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpeedShoesBinding
    private lateinit var model: SpeedShoesViewModel
    private val number = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply { maximumFractionDigits = 2 }
    private var shown: SpeedShoesStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeedShoesBinding.inflate(layoutInflater)
        setUpDetailScreen(binding.root)
        binding.topBar.screenTitle.text = "Giày tăng tốc"
        binding.topBar.backButton.setOnClickListener { finish() }
        binding.backToInventory.setOnClickListener { finish() }
        model = ViewModelProvider(this, viewModelFactory { SpeedShoesViewModel(InventoryModule.items(this, intent.getLongExtra("item_id", -1).takeIf { it > 0 })) })[
            SpeedShoesViewModel::class.java]
        binding.state.stateRetry.setOnClickListener { model.load() }
        binding.useButton.setOnClickListener { confirmUse() }
        model.state.observe(this) { state ->
            if (state != Remote.Loading) Skeleton.hide(binding.skeleton.root)
            when (state) {
                Remote.Loading -> if (shown == null) Skeleton.show(binding.skeleton.root, binding.content, binding.state.root)
                is Remote.Failed -> if (shown == null) binding.state.showError(binding.content, state.message)
                Remote.SessionExpired -> openSignIn()
                is Remote.Ready -> show(state.value)
            }
        }
        model.using.observe(this) { shown?.let(::show) }
        model.used.observe(this) { event ->
            event.take()?.onSuccess { status ->
                setResult(RESULT_OK)
                Snackbar.make(binding.root, "Đã kích hoạt giày tăng tốc. Còn ${status.quantity} giày.", Snackbar.LENGTH_LONG).show()
            }?.onFailure { Snackbar.make(binding.root, it.message.orEmpty(), Snackbar.LENGTH_LONG).show() }
        }
    }

    private fun show(status: SpeedShoesStatus) = with(binding) {
        val first = shown == null
        shown = status
        state.showContent(content)
        val using = model.using.value == true
        val effect = status.effect?.takeIf { it.remainingSteps > 0 }
        val factor = "×" + number.format(effect?.multiplier ?: status.multiplier)
        multiplier.text = factor
        multiplier.contentDescription = "Nhân ${number.format(effect?.multiplier ?: status.multiplier)} lần"
        ownedChip.text = "Đang có × ${status.quantity}"
        factorValue.text = factor
        rangeValue.text = "${number.format(effect?.range ?: status.range)} bước hợp lệ"
        effectNote.text = if (effect != null) "Đang áp dụng · còn ${number.format(effect.remainingSteps)} bước được tăng tốc."
            else "Thông số từ cấu hình vật phẩm hiện tại."

        val journey = status.journey
        val blocked = journey == null || effect != null
        journeyPanel.backgroundTintList = ColorStateList.valueOf(if (blocked) 0xFFF6EEDF.toInt() else 0xFFE8F0E2.toInt())
        journeyLabel.text = if (journey != null) "Hành trình đang thực hiện" else "Chưa có hành trình đang thực hiện"
        journeyName.text = journey?.destination ?: "Bắt đầu một hành trình ở Khám phá"
        journeyCheck.text = when {
            journey == null -> "Giày chỉ dùng được khi đang có hành trình."
            effect != null -> "Hiệu ứng trước còn hiệu lực, chưa thể kích hoạt thêm."
            else -> "Đã kiểm tra điều kiện tương thích."
        }

        val reason = when {
            !status.active -> "Vật phẩm tạm ngưng"
            status.quantity <= 0 -> "Chưa sở hữu giày"
            journey == null || effect != null -> "Chưa thể sử dụng"
            else -> null
        }
        useButton.isEnabled = reason == null && !using
        useButton.text = if (using) "Đang kích hoạt…" else reason ?: "Sử dụng 1 giày"
        useButton.backgroundTintList = ColorStateList.valueOf(
            if (useButton.isEnabled) ContextCompat.getColor(this@SpeedShoesActivity, R.color.teal) else 0xFFE4E7E1.toInt())
        useButton.setTextColor(if (useButton.isEnabled) 0xFFFFFFFF.toInt() else 0xFF8A8F86.toInt())
        if (first) ScreenMotion.reveal(content)
    }

    private fun confirmUse() {
        val status = shown ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Dùng 1 giày tăng tốc?")
            .setMessage("Áp dụng cho hành trình đến ${status.journey?.destination}. Bạn còn ${status.quantity} giày.")
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Sử dụng") { _, _ -> model.use() }
            .show()
    }
}
