package com.project.bcv.feature.engagement

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.GOLD
import com.project.bcv.feature.engagement.EngagementUi.GOLD_SOFT
import com.project.bcv.feature.engagement.EngagementUi.LINE
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.SOFT
import com.project.bcv.feature.engagement.EngagementUi.TEAL
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.icon
import com.project.bcv.feature.engagement.EngagementUi.leadingIcon
import com.project.bcv.feature.engagement.EngagementUi.full
import com.project.bcv.feature.engagement.EngagementUi.number
import com.project.bcv.feature.engagement.EngagementUi.pill
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.text
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * F76 "Trạm câu hỏi": the destination's questions are spread evenly along the journey (N questions →
 * stops at 1/(N+1) … N/(N+1)). A stop opens when the walker reaches it. A correct answer earns explorer
 * points and a card piece — never steps, so walking stays the only way to the destination.
 */
class CultureQuizSheet(
    private val activity: AppCompatActivity,
    private val focusStop: Int? = null,
    private val onChanged: () -> Unit,
) {
    private val dialog = BottomSheetDialog(activity)
    private val api = EngagementApi(activity)
    private val scroll = ScrollView(activity)
    private val body = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(activity.dp(20), activity.dp(10), activity.dp(20), activity.dp(28))
    }
    private var changed = false
    private var scrolled = false

    /**
     * Loads first, then opens at a fixed height: the sheet never grows from empty, never resizes
     * when a stop is answered, and lands on the right stop without a visible scroll.
     */
    fun show() {
        activity.lifecycleScope.launch {
            val result = runCatching { api.quiz() }
            if (activity.isFinishing || activity.isDestroyed) return@launch
            scroll.isFillViewport = true
            scroll.addView(body)
            dialog.setContentView(scroll)
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.setOnDismissListener { if (changed) onChanged() }
            apply(result)
            dialog.show()
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.backgroundTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
                val height = (activity.resources.displayMetrics.heightPixels * 0.86f).toInt()
                sheet.layoutParams = sheet.layoutParams.apply { this.height = height }
                dialog.behavior.peekHeight = height
            }
        }
    }

    /** Re-renders after an answer, keeping the scroll position so nothing moves under the finger. */
    private fun load() {
        activity.lifecycleScope.launch {
            val result = runCatching { api.quiz() }
            val y = scroll.scrollY
            apply(result)
            scroll.post { scroll.scrollTo(0, y) }
        }
    }

    private fun apply(result: Result<JSONObject>) {
        body.removeAllViews()
        result.onFailure { body.full(activity.text(it.message ?: "Không tải được câu hỏi.", 15f, MUTED), 8) }
            .onSuccess(::render)
    }

    private fun render(data: JSONObject) = with(activity) {
        body.removeAllViews()
        body.addView(View(this).apply { background = rounded(0xFFDFE8DD.toInt(), 2, null, context) },
            LinearLayout.LayoutParams(dp(40), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(12) })
        body.addView(text("Trạm câu hỏi", 20f, FOREST, true))
        if (!data.optBoolean("available")) {
            body.full(text(data.optString("message").takeIf { it.isNotBlank() }
                ?: "Hãy chọn một điểm đến để mở các trạm câu hỏi trên đường đi.", 15f, MUTED), 8)
            return@with
        }
        val stops = data.getJSONArray("stops")
        val required = data.getInt("requiredSteps"); val recorded = data.getInt("recordedSteps")
        body.addView(text("Trên đường đến ${data.getString("placeName")} · ${stops.length()} trạm dọc hành trình", 13f, MUTED))

        // The journey as a line with its stops, like a route seen from the side.
        val track = JourneyTrack(this, recorded.toFloat() / required.coerceAtLeast(1),
            List(stops.length()) { i -> stops.getJSONObject(i).let { it.getDouble("fraction").toFloat() to it.getString("state") } })
        body.addView(track, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(14) })
        val legend = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        legend.addView(text("Xuất phát", 11f, MUTED), LinearLayout.LayoutParams(0, -2, 1f))
        legend.addView(text("${number(recorded)}/${number(required)} bước", 11f, TEAL, true))
        legend.addView(text("Điểm đến", 11f, MUTED).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, 1f))
        body.addView(legend)

        // Rewards shown here: explorer points only, more for harder questions.
        val byDifficulty = data.optJSONObject("pointsByDifficulty")
        val rewards = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rounded(SOFT, 16, null, context)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        rewards.addView(text("${number(data.getInt("earnedPoints"))} điểm khám phá", 15f, FOREST, true)
            .leadingIcon(com.project.bcv.R.drawable.ic_m3_star, 0xFFC9962B.toInt(), 18), LinearLayout.LayoutParams(0, -2, 1f))
        rewards.addView(text(if (byDifficulty == null) "" else "Dễ +${byDifficulty.optInt("easy")} · Vừa +${byDifficulty.optInt("medium")} · Khó +${byDifficulty.optInt("hard")}",
            12f, MUTED))
        body.full(rewards, 12)

        var focusView: View? = null
        for (i in 0 until stops.length()) {
            val stop = stops.getJSONObject(i)
            val card = stopCard(stop)
            body.full(card, 12)
            val n = stop.getInt("number")
            if (n == focusStop || (focusStop == null && focusView == null && stop.getString("state") == "OPEN")) focusView = card
        }
        if (!scrolled) focusView?.let { target ->
            scrolled = true
            // Jump before the first frame is drawn, so the user never sees the list scroll.
            scroll.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    scroll.viewTreeObserver.removeOnPreDrawListener(this)
                    scroll.scrollTo(0, (target.top - dp(12)).coerceAtLeast(0))
                    return true
                }
            })
        }
    }

    private fun stopCard(stop: JSONObject): LinearLayout = with(activity) {
        val state = stop.getString("state")
        val stopNo = stop.getInt("number")
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = when (state) {
                "OPEN" -> rounded(GOLD_SOFT, 18, GOLD, context)
                "CORRECT" -> rounded(0xFFF0F8F5.toInt(), 18, 0xFFBFE0D5.toInt(), context)
                "WRONG" -> rounded(0xFFF7F8F7.toInt(), 18, LINE, context)
                else -> rounded(0xFFFFFFFF.toInt(), 18, LINE, context)
            }
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        // State mark: "?" for an open stop, Material Symbols check / close / lock for the others.
        head.addView(android.widget.FrameLayout(this).apply {
            background = rounded(when (state) { "OPEN" -> 0xFFFFC94D.toInt(); "CORRECT" -> TEAL; "WRONG" -> 0xFFB8C4BD.toInt(); else -> 0xFFE3EBE3.toInt() }, 14, null, context)
            val mark = when (state) { "CORRECT" -> com.project.bcv.R.drawable.ic_m3_check; "WRONG" -> com.project.bcv.R.drawable.ic_m3_close; "LOCKED" -> com.project.bcv.R.drawable.ic_m3_lock; else -> 0 }
            if (mark == 0) addView(text("?", 13f, 0xFF5A3B00.toInt(), true).apply { gravity = Gravity.CENTER },
                android.widget.FrameLayout.LayoutParams(-1, -1))
            else addView(icon(mark, if (state == "LOCKED") MUTED else 0xFFFFFFFF.toInt(), 16),
                android.widget.FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        head.addView(text("Trạm $stopNo · ${stop.getInt("unlockPercent")}% hành trình", 15f, FOREST, true).apply {
            setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        when (state) {
            // Open: how hard it is and what it is worth, so a hard question reads as the bigger prize.
            "OPEN" -> head.addView(pill("${stop.optString("difficultyName")} · +${stop.optInt("points")}", 0xFF8A5A00.toInt(), 0xFFFFE7A8.toInt()))
            "CORRECT" -> head.addView(pill("+${stop.optInt("points")} điểm"))
            "WRONG" -> head.addView(pill("Chưa đúng", MUTED, 0xFFEDEFEE.toInt()))
        }
        card.addView(head)

        if (state == "LOCKED") {
            card.full(text("Đi thêm ${number(stop.getInt("stepsToGo"))} bước để tới trạm này.", 13f, MUTED), 8)
            return@with card
        }
        card.full(text(stop.getString("text"), 15f, FOREST, true), 10)
        val options = stop.getJSONArray("options")
        if (state == "OPEN") {
            val buttons = mutableListOf<MaterialButton>()
            val feedback = text("", 13f, MUTED).apply { visibility = View.GONE }
            for (i in 0 until options.length()) {
                val option = options.getString(i)
                val button = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = "${'A' + i}.  $option"; isAllCaps = false; gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setTextColor(FOREST); textSize = 15f; cornerRadius = dp(14)
                    backgroundTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
                    strokeColor = ColorStateList.valueOf(0xFFE7D7AE.toInt())
                    setOnClickListener {
                        buttons.forEach { it.isEnabled = false }
                        lifecycleScope.launch {
                            runCatching { api.answer(stop.getString("key"), option) }
                                .onSuccess { changed = true; load() }
                                .onFailure {
                                    buttons.forEach { b -> b.isEnabled = true }
                                    feedback.text = it.message ?: "Không gửi được câu trả lời."
                                    feedback.visibility = View.VISIBLE
                                }
                        }
                    }
                }
                buttons += button
                card.full(button, 6)
            }
            card.full(feedback, 6)
            card.full(text("Câu ${stop.optString("difficultyName").lowercase()} · trả lời đúng: +${stop.optInt("points")} điểm khám phá",
                11f, 0xFF8A5A00.toInt()), 6)
            return@with card
        }
        // Answered: the player's pick, the right answer and the explanation.
        val answer = stop.getString("answer"); val chosen = stop.optString("chosen")
        for (i in 0 until options.length()) {
            val option = options.getString(i)
            val isAnswer = option == answer; val isChosen = option == chosen
            if (!isAnswer && !isChosen) continue
            card.full(text("${'A' + i}.  $option" + when { isAnswer && isChosen -> "  ✓ bạn chọn"; isAnswer -> "  ✓ đáp án"; else -> "  ✕ bạn chọn" },
                13f, if (isAnswer) TEAL else 0xFFC36E59.toInt(), true).apply {
                background = rounded(if (isAnswer) 0xFFE2F2EC.toInt() else 0xFFFBEAE5.toInt(), 12, null, context)
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }, 6)
        }
        stop.optString("explanation").takeIf { it.isNotBlank() && it != "null" }?.let {
            card.full(text(it, 13f, FOREST).leadingIcon(com.project.bcv.R.drawable.ic_m3_lightbulb, 0xFFC9962B.toInt(), 16).apply {
                gravity = Gravity.TOP or Gravity.START }, 8)
        }
        card
    }

    /** Horizontal journey line: done part in teal, stops as circles, the walker as a blue dot. */
    private class JourneyTrack(context: Context, private val progress: Float,
                               private val stops: List<Pair<Float, String>>) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val density = context.resources.displayMetrics.density

        override fun onDraw(canvas: Canvas) {
            val left = 16 * density; val right = width - 16 * density; val y = height / 2f
            fun x(f: Float) = left + (right - left) * f.coerceIn(0f, 1f)
            paint.strokeCap = Paint.Cap.ROUND; paint.strokeWidth = 8 * density
            paint.color = LINE; canvas.drawLine(left, y, right, y, paint)
            paint.color = TEAL; canvas.drawLine(left, y, x(progress), y, paint)
            paint.style = Paint.Style.FILL
            paint.color = TEAL; canvas.drawCircle(left, y, 6 * density, paint)
            paint.color = 0xFFC36E59.toInt(); canvas.drawCircle(right, y, 7 * density, paint)
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER; textSize = 11 * density; typeface = Typeface.DEFAULT_BOLD }
            stops.forEachIndexed { index, (fraction, state) ->
                val cx = x(fraction); val r = 12 * density
                paint.style = Paint.Style.FILL
                paint.color = when (state) { "OPEN" -> 0xFFFFC94D.toInt(); "CORRECT" -> TEAL; "WRONG" -> 0xFFB8C4BD.toInt(); else -> 0xFFFFFFFF.toInt() }
                canvas.drawCircle(cx, y, r, paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.5f * density
                paint.color = if (state == "LOCKED") 0xFFB8C4BD.toInt() else 0xFFFFFFFF.toInt()
                canvas.drawCircle(cx, y, r, paint)
                label.color = when (state) { "OPEN" -> 0xFF5A3B00.toInt(); "LOCKED" -> MUTED; else -> 0xFFFFFFFF.toInt() }
                val mark = when (state) { "OPEN" -> "?"; "CORRECT" -> "✓"; "WRONG" -> "✕"; else -> "${index + 1}" }
                canvas.drawText(mark, cx, y + 4 * density, label)
            }
            paint.style = Paint.Style.FILL
            paint.color = 0xFF3C7DB0.toInt(); canvas.drawCircle(x(progress), y, 7 * density, paint)
            paint.color = 0xFFFFFFFF.toInt(); canvas.drawCircle(x(progress), y, 3 * density, paint)
        }
    }
}
