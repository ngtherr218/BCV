package com.project.bcv.feature.engagement

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.GOLD
import com.project.bcv.feature.engagement.EngagementUi.GOLD_SOFT
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.SOFT
import com.project.bcv.feature.engagement.EngagementUi.TEAL
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.icon
import com.project.bcv.feature.engagement.EngagementUi.leadingIcon
import com.project.bcv.feature.engagement.EngagementUi.number
import com.project.bcv.feature.engagement.EngagementUi.outlinedButton
import com.project.bcv.feature.engagement.EngagementUi.primaryButton
import com.project.bcv.feature.engagement.EngagementUi.progress
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.text
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Celebrates a successful check-in: a passport stamp lands, then what the visit earned
 * (points, a newly unlocked place, the province's passport page, the card still to walk for).
 */
object CheckinSuccessDialog {
    private const val GOLD_DARK = 0xFF9A6B12.toInt()


    fun show(activity: Activity, result: JSONObject, test: Boolean, onJournal: () -> Unit) = with(activity) {
        val dialog = Dialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(Color.WHITE, 28, null, context)
            setPadding(dp(22), dp(24), dp(22), dp(18))
        }
        val place = result.optString("placeName")
        val stamp = StampMark(this, place)
        box.addView(stamp, LinearLayout.LayoutParams(dp(132), dp(132)))
        if (test) box.addView(text("CHECK-IN THỬ", 11f, MUTED, true).leadingIcon(com.project.bcv.R.drawable.ic_m3_science, MUTED, 13, 4).apply {
            letterSpacing = 0.08f; background = rounded(SOFT, 10, null, context); setPadding(dp(10), dp(3), dp(10), dp(3))
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
        box.addView(text("Check-in thành công!", 20f, FOREST, true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        val visits = result.optInt("visits", 1)
        box.addView(text(if (visits <= 1) "Lần đầu bạn đặt chân đến $place" else "Lần ghé thứ $visits tại $place",
            15f, MUTED).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })

        val rewards = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        rewards.addView(reward(com.project.bcv.R.drawable.ic_m3_star, "+${number(result.optInt("points", 200))} điểm khám phá", null))
        if (result.optBoolean("newDiscovery")) rewards.addView(reward(com.project.bcv.R.drawable.ic_m3_lock_open, "Mở khóa địa danh", "Đã thêm vào bộ sưu tập của bạn"))
        // Cards come only from finishing the journey: say which one is waiting at the end of it.
        result.optString("pendingCard").takeIf { it.isNotBlank() && it != "null" }?.let {
            rewards.addView(reward(com.project.bcv.R.drawable.ic_m3_style, "Thẻ $it", "Đi hết hành trình để nhận thẻ"))
        }
        result.optString("province").takeIf { it.isNotBlank() && it != "null" }?.let { province ->
            val stamped = result.optInt("provinceStamped"); val total = result.optInt("provinceTotal").coerceAtLeast(1)
            val row = reward(com.project.bcv.R.drawable.ic_m3_id_card, "Hộ chiếu · $province",
                if (stamped >= total) "Đã có dấu tất cả $total địa danh của tỉnh!" else "$stamped/$total địa danh có dấu")
            (row.getChildAt(1) as LinearLayout).addView(progress(stamped, total, GOLD),
                LinearLayout.LayoutParams(-1, dp(5)).apply { topMargin = dp(6) })
            rewards.addView(row)
        }
        box.addView(rewards, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        box.addView(primaryButton("Viết nhật ký").apply {
            setOnClickListener { dialog.dismiss(); onJournal() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        box.addView(outlinedButton("Để sau").apply { setOnClickListener { dialog.dismiss() } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        dialog.setContentView(box)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels - dp(48)).coerceAtMost(dp(400)), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
        stamp.land()
        // Rewards follow the stamp one by one.
        for (i in 0 until rewards.childCount) rewards.getChildAt(i).apply {
            alpha = 0f; translationY = dp(10).toFloat()
            animate().alpha(1f).translationY(0f).setStartDelay(420L + i * 110L).setDuration(260).start()
        }
    }

    /**
     * Check-in did not go through: says why in plain words (too far, no GPS, offline…) and offers to try again.
     * [code] is the server's error code, or NO_LOCATION when the phone could not get a fix.
     */
    fun showFailure(activity: Activity, code: String?, message: String?, onRetry: () -> Unit) = with(activity) {
        val (symbol, title, hint) = when (code) {
            "TOO_FAR" -> Triple(com.project.bcv.R.drawable.ic_m3_location_off, "Bạn chưa đến nơi",
                "Check-in chỉ được tính khi bạn ở trong bán kính 750 m quanh địa danh. Hãy đi gần hơn rồi thử lại.")
            "NO_LOCATION" -> Triple(com.project.bcv.R.drawable.ic_m3_gps_off, "Chưa xác định được vị trí",
                "Bật GPS, ra chỗ thoáng (ít nhà cao tầng) và đợi vài giây trước khi thử lại.")
            "IMPLAUSIBLE" -> Triple(com.project.bcv.R.drawable.ic_m3_warning, "Vị trí chưa hợp lệ",
                "Vị trí của bạn thay đổi quá nhanh so với lần check-in trước. Hãy thử lại sau ít phút.")
            "NOT_JOURNEY_DESTINATION" -> Triple(com.project.bcv.R.drawable.ic_m3_explore, "Chưa phải điểm đến",
                "Check-in chỉ dùng tại điểm đến của hành trình đang đi, hoặc nơi vị trí ảo của bạn đang đứng.")
            "TEST_DISABLED" -> Triple(com.project.bcv.R.drawable.ic_m3_science, "Không check-in thử được",
                "Check-in thử chỉ dùng được trên máy chủ thử nghiệm.")
            "NETWORK_ERROR", "TIMEOUT", "NO_NETWORK", "OFFLINE" -> Triple(com.project.bcv.R.drawable.ic_m3_wifi_off, "Mất kết nối",
                "Kiểm tra mạng rồi thử lại.")
            "NOT_FOUND", "PLACE_NOT_FOUND" -> Triple(com.project.bcv.R.drawable.ic_m3_block, "Địa danh tạm ngừng",
                "Địa danh này hiện không nhận check-in.")
            else -> Triple(com.project.bcv.R.drawable.ic_m3_error, "Chưa check-in được", "Vui lòng thử lại sau giây lát.")
        }
        val dialog = Dialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(Color.WHITE, 28, null, context)
            setPadding(dp(22), dp(26), dp(22), dp(18))
        }
        val badge = android.widget.FrameLayout(this).apply {
            addView(icon(symbol, 0xFFC0473A.toInt(), 38), android.widget.FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(0xFFFDF0EE.toInt())
                setStroke(dp(2), 0xFFF3C9C3.toInt())
            }
        }
        box.addView(badge, LinearLayout.LayoutParams(dp(84), dp(84)))
        box.addView(text(title, 20f, FOREST, true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        if (!message.isNullOrBlank()) box.addView(text(message, 15f, FOREST).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        box.addView(text(hint, 13f, MUTED).apply {
            gravity = Gravity.CENTER; background = rounded(SOFT, 14, null, context); setPadding(dp(14), dp(10), dp(14), dp(10))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        if (code !in setOf("TEST_DISABLED", "NOT_FOUND", "PLACE_NOT_FOUND", "NOT_JOURNEY_DESTINATION"))
            box.addView(primaryButton("Thử lại").apply { setOnClickListener { dialog.dismiss(); onRetry() } },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        box.addView(outlinedButton("Đóng").apply { setOnClickListener { dialog.dismiss() } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        dialog.setContentView(box)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels - dp(48)).coerceAtMost(dp(400)), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
        // A small shake on the badge says "not this time" without being alarming.
        badge.translationX = 0f
        ObjectAnimator.ofFloat(badge, View.TRANSLATION_X, 0f, -dp(8).toFloat(), dp(8).toFloat(), -dp(5).toFloat(), dp(5).toFloat(), 0f)
            .apply { duration = 420; startDelay = 150; start() }
    }

    private fun Context.reward(iconRes: Int, title: String, detail: String?) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        background = rounded(GOLD_SOFT, 16, null, context)
        setPadding(dp(12), dp(10), dp(14), dp(10))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        addView(icon(iconRes, GOLD_DARK, 22), LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginStart = dp(3); marginEnd = dp(3) })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(title, 15f, FOREST, true))
            if (detail != null) addView(text(detail, 13f, MUTED))
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
    }

    /** A round passport stamp — double ring, "ĐÃ ĐẾN", today's date — that lands with a thump. */
    private class StampMark(context: Context, private val place: String) : View(context) {
        private val ink = 0xFFB23A2E.toInt()
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = ink }
        private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22C9962B }
        private var halo = 0f

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f; val cy = height / 2f; val d = resources.displayMetrics.density
            val r = minOf(cx, cy) - 8 * d
            if (halo > 0f) { glow.alpha = ((1f - halo) * 120).toInt(); canvas.drawCircle(cx, cy, r + halo * 8 * d, glow) }
            canvas.save(); canvas.rotate(-12f, cx, cy)
            ring.strokeWidth = 3.5f * d; canvas.drawCircle(cx, cy, r, ring)
            ring.strokeWidth = 1.5f * d; canvas.drawCircle(cx, cy, r - 7 * d, ring)
            label.textSize = 11 * d; label.letterSpacing = 0.12f
            canvas.drawText(shortName(), cx, cy - r * 0.38f, label)
            label.textSize = 22 * d; label.letterSpacing = 0.04f
            canvas.drawText("ĐÃ ĐẾN", cx, cy + 8 * d, label)
            val bar = RectF(cx - r * 0.62f, cy + 15 * d, cx + r * 0.62f, cy + 16.5f * d)
            canvas.drawRect(bar, label)
            label.textSize = 11 * d; label.letterSpacing = 0.08f
            canvas.drawText(date, cx, cy + r * 0.52f, label)
            canvas.restore()
        }

        private fun shortName(): String {
            val upper = place.uppercase()
            return if (upper.length <= 18) upper else upper.take(16).trimEnd() + "…"
        }

        fun land() {
            scaleX = 2.2f; scaleY = 2.2f; alpha = 0f; rotation = 14f
            val set = AnimatorSet()
            set.playTogether(
                ObjectAnimator.ofFloat(this, SCALE_X, 2.2f, 1f), ObjectAnimator.ofFloat(this, SCALE_Y, 2.2f, 1f),
                ObjectAnimator.ofFloat(this, ALPHA, 0f, 1f), ObjectAnimator.ofFloat(this, ROTATION, 14f, 0f),
            )
            set.duration = 380; set.interpolator = OvershootInterpolator(1.4f); set.startDelay = 120
            set.start()
            ValueAnimator.ofFloat(0f, 1f).apply {
                startDelay = 420; duration = 520
                addUpdateListener { halo = it.animatedValue as Float; invalidate() }
                start()
            }
            postDelayed({
                performHapticFeedback(if (android.os.Build.VERSION.SDK_INT >= 30) android.view.HapticFeedbackConstants.CONFIRM
                    else android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }, 480)
        }
    }
}
