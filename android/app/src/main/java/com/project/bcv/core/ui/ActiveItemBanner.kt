package com.project.bcv.core.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.questitem.inventory.presentation.SpeedShoesActivity
import java.text.NumberFormat
import java.util.Locale

/** A server-confirmed effect: actual walking steps left, not boosted journey progress. */
class ActiveItemBanner @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val title = TextView(context)
    private val detail = TextView(context)
    private val meter = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
    private val number = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    private var effectKey: String? = null

    init {
        orientation = VERTICAL
        val pad = (12 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            setColor(0xFFE8F6EE.toInt())
            cornerRadius = 14 * resources.displayMetrics.density
            setStroke((resources.displayMetrics.density).toInt().coerceAtLeast(1), 0xFF92C9AA.toInt())
        }
        title.setTextColor(0xFF126747.toInt())
        title.textSize = 14f
        title.setTypeface(null, Typeface.BOLD)
        detail.setTextColor(0xFF426956.toInt())
        detail.textSize = 12f
        detail.setPadding(0, pad / 2, 0, pad / 2)
        meter.progressTintList = android.content.res.ColorStateList.valueOf(0xFF15966A.toInt())
        meter.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0xFFCDE6D7.toInt())
        addView(title)
        addView(detail)
        addView(meter, LayoutParams(LayoutParams.MATCH_PARENT, (4 * resources.displayMetrics.density).toInt()))
        isFocusable = true
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        setOnClickListener { context.startActivity(Intent(context, SpeedShoesActivity::class.java)) }
        visibility = GONE
    }

    fun show(journey: ExploreJourney?) {
        val effect = journey?.activeItem?.takeIf { journey.status == "IN_PROGRESS" && it.remainingSteps > 0 && it.itemType == "SPEED_SHOES" }
        if (effect == null) {
            visibility = GONE
            effectKey = null
            return
        }
        title.text = "Đang dùng ${effect.name} ×${number.format(effect.multiplier)}"
        detail.text = "Còn ${number.format(effect.remainingSteps)} bước được tăng tốc.\nTăng tiến độ hành trình; nhiệm vụ vẫn tính bước thực tế.\nChạm để xem vật phẩm."
        meter.max = effect.range.coerceAtLeast(1)
        meter.progress = effect.remainingSteps.coerceIn(0, meter.max)
        contentDescription = "${title.text}. ${detail.text}"
        val key = "${journey.id}:${effect.multiplier}:${effect.range}"
        visibility = VISIBLE
        if (effectKey != key && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            alpha = 0f
            animate().alpha(1f).setDuration(250).start()
        }
        effectKey = key
    }
}
