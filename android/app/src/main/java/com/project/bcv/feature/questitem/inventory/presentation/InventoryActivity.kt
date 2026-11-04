package com.project.bcv.feature.questitem.inventory.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.project.bcv.R
import com.project.bcv.core.di.InventoryModule
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.databinding.ActivityInventoryBinding
import com.project.bcv.databinding.ItemInventoryBinding
import com.project.bcv.databinding.SheetItemConditionsBinding
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.questitem.inventory.domain.InventoryItem
import com.project.bcv.feature.questitem.inventory.domain.InventoryOverview

/**
 * Every item type with its quantity (0 when not owned) and, per item, whether it can be used now.
 * Quantities are never guessed: while loading, refreshing or after a failed refresh the last
 * confirmed numbers stay, and a failed first load shows an error rather than an empty inventory.
 */
class InventoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInventoryBinding
    private lateinit var model: InventoryViewModel
    private var shown: InventoryOverview? = null
    private var resumedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.errorRetry.setOnClickListener { model.load() }
        binding.statusRetry.setOnClickListener { model.load() }

        model = ViewModelProvider(this, InventoryViewModelFactory(InventoryModule.repository(this)))[
            InventoryViewModel::class.java]
        model.state.observe(this, ::render)
    }

    override fun onResume() {
        super.onResume()
        // A quest reward or a finished journey may have changed quantities meanwhile.
        if (resumedOnce) model.load()
        resumedOnce = true
    }

    private fun render(state: InventoryUiState) = with(binding) {
        when (state) {
            InventoryUiState.Loading -> {
                status(EYEBROW_BLUE, "ĐANG KẾT NỐI", "Đang tải kho…", "Số lượng sẽ hiện khi máy chủ xác nhận.")
                Skeleton.show(skeleton.root, content, errorPanel)
            }
            is InventoryUiState.Failed -> {
                statusCard.visibility = View.GONE
                Skeleton.hide(skeleton.root)
                content.visibility = View.GONE
                errorPanel.visibility = View.VISIBLE
                errorMessage.text = "${state.message}\nSố lượng chưa được xác định."
            }
            InventoryUiState.SessionExpired -> {
                SessionPreferences(this@InventoryActivity).token = null
                startActivity(Intent(this@InventoryActivity, AccountActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            is InventoryUiState.Loaded -> {
                Skeleton.hide(skeleton.root)
                errorPanel.visibility = View.GONE
                show(state.overview)
                when {
                    state.refreshing -> {
                        status(EYEBROW_BLUE, "ĐANG CẬP NHẬT", "Giữ số lượng cũ", "Chờ xác nhận số lượng mới.")
                        banner(R.drawable.ic_sync_outline, "Đang giữ dữ liệu lần tải trước")
                    }
                    state.refreshError != null -> {
                        status(EYEBROW_RED, "CHƯA CÓ DỮ LIỆU MỚI", "Giữ số lượng cũ",
                            "${previousCounts(state.overview)}, lần tải trước.", retry = true)
                        banner(R.drawable.ic_cloud_off, "Cập nhật thất bại · Không đổi số lượng")
                    }
                    else -> {
                        statusCard.visibility = View.GONE
                        banner(R.drawable.ic_nav_route, journeyBanner(state.overview))
                    }
                }
            }
        }
    }

    private fun status(color: Int, eyebrow: String, title: String, body: String, retry: Boolean = false) = with(binding) {
        statusCard.visibility = View.VISIBLE
        statusCard.backgroundTintList = ColorStateList.valueOf(if (color == EYEBROW_RED) 0xFFFBE9E5.toInt() else 0xFFE6EDF0.toInt())
        statusEyebrow.text = eyebrow
        statusEyebrow.setTextColor(color)
        statusTitle.text = title
        statusBody.text = body
        statusIcon.visibility = if (retry) View.GONE else View.VISIBLE
        statusRetry.visibility = if (retry) View.VISIBLE else View.GONE
    }

    private fun banner(icon: Int, text: String) = with(binding.banner) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        compoundDrawablesRelative.firstOrNull()?.let { drawable ->
            val size = (20 * resources.displayMetrics.density).toInt()
            drawable.setBounds(0, 0, size, size)
            setCompoundDrawablesRelative(drawable, null, null, null)
        }
        this.text = text
    }

    private fun journeyBanner(overview: InventoryOverview) = when {
        overview.totalQuantity == 0 -> "Bạn chưa sở hữu vật phẩm"
        overview.hasActiveJourney -> "Đang có hành trình thực hiện"
        else -> "Chưa có hành trình đang thực hiện"
    }

    private fun previousCounts(overview: InventoryOverview) = overview.items
        .filter { it.quantity > 0 }
        .joinToString(" · ") { "${it.quantity} ${shortName(it)}" }
        .ifEmpty { "Chưa có vật phẩm" }

    private fun show(overview: InventoryOverview) = with(binding) {
        content.visibility = View.VISIBLE
        if (overview == shown) return@with
        val first = shown == null
        shown = overview
        val owned = overview.totalQuantity > 0
        totalCount.text = String.format("%02d", overview.totalQuantity)
        // "Giày tăng tốc và vé máy bay"
        totalCaption.text = overview.items.joinToString(" và ") { it.name }.lowercase().replaceFirstChar { it.uppercase() }
        summaryCard.contentDescription = "Hành trang của bạn: ${overview.totalQuantity} vật phẩm"
        sectionTitle.text = if (owned) "Vật phẩm của bạn" else "Các loại vật phẩm"
        sectionCount.text = "${overview.items.size} loại"

        itemList.removeAllViews()
        overview.items.forEach { itemList.addView(itemCard(it, overview).root) }

        footerNote.text = if (owned) "Chỉ trừ khi sử dụng thành công." else "Vật phẩm chưa sở hữu vẫn hiển thị số 0."
        footerHint.text = if (owned) "Hiệu ứng áp dụng theo cấu hình vật phẩm." else "Xem phần thưởng đang áp dụng tại Nhiệm vụ."
        if (first) ScreenMotion.reveal(content)
    }

    private fun itemCard(item: InventoryItem, overview: InventoryOverview): ItemInventoryBinding {
        val card = ItemInventoryBinding.inflate(layoutInflater, binding.itemList, false)
        val look = lookOf(item)
        card.itemIcon.setImageResource(look.icon)
        card.itemIcon.imageTintList = ColorStateList.valueOf(look.accent)
        card.itemIcon.backgroundTintList = ColorStateList.valueOf(look.box)
        card.itemName.text = item.name
        card.itemEffect.text = look.effect
        card.itemQuantity.text = "× ${item.quantity}"
        card.itemQuantity.setTextColor(look.accent)
        card.itemQuantity.backgroundTintList = ColorStateList.valueOf(look.pill)
        card.itemCondition.text = look.condition
        val muted = 0xFF8A8F86.toInt()
        if (item.quantity > 0) {
            card.itemAction.text = "Xem điều kiện  →"
            card.itemAction.setTextColor(ContextCompat.getColor(this, R.color.teal))
            card.itemAction.backgroundTintList = ColorStateList.valueOf(0xFFEAF2E6.toInt())
            card.itemAction.setOnClickListener { showConditions(item, overview) }
        } else {
            card.itemAction.text = "Chưa sở hữu"
            card.itemAction.setTextColor(muted)
            card.itemAction.backgroundTintList = ColorStateList.valueOf(0xFFEEF0EC.toInt())
            card.itemAction.isEnabled = false
            card.itemAction.isClickable = false
        }
        card.root.contentDescription = "${item.name}, số lượng ${item.quantity}. ${look.effect} ${look.condition}"
        return card
    }

    /** One row of the checklist: met, blocking, or informational. */
    private enum class Check { OK, BLOCKED, INFO }

    private fun showConditions(item: InventoryItem, overview: InventoryOverview) {
        val sheet = SheetItemConditionsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)
        val look = lookOf(item)
        val shoes = item.type == TYPE_SHOES
        val ownedRow = "Vật phẩm còn hoạt động · Sở hữu %02d".format(item.quantity)
        val journey = overview.hasActiveJourney

        val title: String
        val intro: String
        val checks: List<Pair<Check, String>>
        val note: String
        val action: String?
        var accent = look.accent
        var box = 0xFFEAF2E6.toInt()
        when {
            !item.active -> {
                title = "Tạm không thể sử dụng"
                intro = "Vật phẩm này đang tạm ngưng hoạt động. Số lượng của bạn được giữ nguyên."
                checks = listOf(Check.BLOCKED to "Vật phẩm đang tạm ngưng", Check.OK to "Số lượng vẫn được giữ · %02d".format(item.quantity))
                note = "Bạn sẽ dùng lại được khi vật phẩm hoạt động trở lại."
                action = null
                accent = 0xFFC2553F.toInt()
                box = 0xFFFBE9E5.toInt()
            }
            shoes && journey && overview.boostActive -> {
                title = "Chưa thể kích hoạt"
                intro = "Hiệu ứng giày tăng tốc trước vẫn đang áp dụng cho hành trình này."
                checks = listOf(Check.OK to ownedRow, Check.OK to "Có hành trình đang thực hiện",
                    Check.BLOCKED to "Hiệu ứng trước chưa hết phạm vi")
                note = "Dùng tiếp khi hiệu ứng hiện tại đã hết bước được tăng tốc."
                action = "Xem hiệu ứng"
                box = 0xFFF6EEDF.toInt()
            }
            shoes && journey -> {
                title = "Sẵn sàng tăng tốc"
                intro = "Giày tăng tốc có thể dùng cho hành trình đang thực hiện."
                checks = listOf(Check.OK to ownedRow, Check.OK to "Có hành trình đang thực hiện",
                    Check.OK to "Không có hiệu ứng không tương thích")
                note = "Chỉ trừ 1 vật phẩm khi sử dụng thành công."
                action = "Xem hiệu ứng"
            }
            shoes -> {
                title = "Cần có hành trình"
                intro = "Giày tăng tốc chỉ dùng được khi bạn đang thực hiện một hành trình."
                checks = listOf(Check.OK to ownedRow, Check.BLOCKED to "Chưa có hành trình đang thực hiện",
                    Check.BLOCKED to "Chưa kích hoạt hiệu ứng")
                note = "Bắt đầu một hành trình ở màn Khám phá rồi quay lại để sử dụng."
                action = null
                box = 0xFFF6EEDF.toInt()
            }
            !journey -> {
                title = "Sẵn sàng đổi khu vực"
                intro = "Vé máy bay đưa vị trí ảo của bạn sang một khu vực khác."
                checks = listOf(Check.OK to ownedRow, Check.OK to "Không có hành trình đang thực hiện",
                    Check.INFO to "Tiếp theo: chọn khu vực và địa danh")
                note = "Chỉ trừ 1 vé khi đổi vị trí thành công."
                action = "Chọn khu vực đích"
                box = 0xFFE8F0F5.toInt()
            }
            else -> {
                title = "Hành trình chưa kết thúc"
                intro = "Vé máy bay chỉ dùng được khi không có hành trình đang thực hiện."
                checks = listOf(Check.OK to ownedRow, Check.BLOCKED to "Có hành trình đang thực hiện",
                    Check.INFO to "Vị trí ảo vẫn được giữ nguyên")
                note = "Hoàn thành hoặc kết thúc hành trình hiện tại rồi quay lại để sử dụng."
                action = null
                box = 0xFFF6EEDF.toInt()
            }
        }

        sheet.sheetIcon.setImageResource(look.icon)
        sheet.sheetIcon.imageTintList = ColorStateList.valueOf(accent)
        sheet.sheetIcon.backgroundTintList = ColorStateList.valueOf(look.box)
        sheet.sheetEyebrow.text = "${item.name.uppercase()} · ĐANG CÓ %02d".format(item.quantity)
        sheet.sheetEyebrow.setTextColor(accent)
        sheet.sheetTitle.text = title
        sheet.sheetIntro.text = intro
        sheet.sheetNote.text = note
        sheet.sheetChecks.backgroundTintList = ColorStateList.valueOf(box)
        checks.forEachIndexed { index, (kind, text) -> sheet.sheetChecks.addView(checkRow(kind, text, index > 0)) }
        sheet.sheetClose.setOnClickListener { dialog.dismiss() }

        if (action != null) {
            sheet.sheetAction.text = action
            sheet.sheetAction.backgroundTintList = ColorStateList.valueOf(
                if (shoes) ContextCompat.getColor(this, R.color.teal) else BLUE)
            sheet.sheetAction.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this,
                    if (shoes) SpeedShoesActivity::class.java else FlightTicketActivity::class.java))
            }
        } else {
            sheet.sheetAction.text = "Chưa thể sử dụng"
            sheet.sheetAction.isEnabled = false
            sheet.sheetAction.backgroundTintList = ColorStateList.valueOf(0xFFE4E7E1.toInt())
            sheet.sheetAction.setTextColor(0xFF8A8F86.toInt())
        }
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.show()
        // Material 3 tints sheets lavender; the design keeps them white like the cards.
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.backgroundTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
    }

    private fun checkRow(kind: Check, text: String, spaced: Boolean): View {
        val density = resources.displayMetrics.density
        val color = when (kind) {
            Check.OK -> ContextCompat.getColor(this, R.color.teal)
            Check.BLOCKED -> 0xFFA0702E.toInt()
            Check.INFO -> BLUE
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { if (spaced) topMargin = (12 * density).toInt() }
            addView(ImageView(context).apply {
                setImageResource(when (kind) {
                    Check.OK -> R.drawable.ic_check_circle
                    Check.BLOCKED -> R.drawable.ic_block
                    Check.INFO -> R.drawable.ic_info_outline
                })
                imageTintList = ColorStateList.valueOf(color)
            }, LinearLayout.LayoutParams((20 * density).toInt(), (20 * density).toInt()))
            addView(TextView(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(if (kind == Check.BLOCKED) color else ContextCompat.getColor(context, R.color.forest))
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = (12 * density).toInt() })
            contentDescription = when (kind) {
                Check.OK -> "Đạt: $text"
                Check.BLOCKED -> "Chưa đạt: $text"
                Check.INFO -> text
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }

    private class Look(val icon: Int, val accent: Int, val box: Int, val pill: Int, val effect: String, val condition: String)

    private fun lookOf(item: InventoryItem) = if (item.type == TYPE_SHOES) Look(
        R.drawable.ic_sneaker, ContextCompat.getColor(this, R.color.teal), 0xFFE8F0E2.toInt(), 0xFFDCEBD6.toInt(),
        "Tăng bước tính vào tiến độ hành trình.", "Cần hành trình đang thực hiện."
    ) else Look(
        R.drawable.ic_flight, BLUE, 0xFFE1EBF1.toInt(), 0xFFDCE8EF.toInt(),
        "Đổi vị trí ảo sang một khu vực khác.", "Không dùng khi đang có hành trình."
    )

    private fun shortName(item: InventoryItem) = if (item.type == TYPE_SHOES) "giày" else "vé"

    private companion object {
        const val TYPE_SHOES = "SPEED_SHOES"
        const val BLUE = 0xFF3C7DB0.toInt()
        const val EYEBROW_BLUE = BLUE
        const val EYEBROW_RED = 0xFFC2553F.toInt()
    }
}
