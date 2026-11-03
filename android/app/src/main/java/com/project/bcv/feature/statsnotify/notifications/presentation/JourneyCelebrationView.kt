package com.project.bcv.feature.statsnotify.notifications.presentation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView

/** A single celebration for the journey and its discovery reward. */
class JourneyCelebrationView(context: Context, message: String) : LinearLayout(context) {
    private val body: TextView
    private val emblem: TextView
    private var sparkle: ObjectAnimator? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val padding = (24 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding / 2)
        emblem = TextView(context).apply {
            text = "✦  ✓  ✦"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(185, 140, 48))
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(emblem, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(TextView(context).apply {
            text = "Chúc mừng bạn đã hoàn thành!"
            textSize = 23f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(30, 91, 69))
            setPadding(0, padding / 2, 0, padding / 2)
            androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
        })
        body = TextView(context).apply {
            text = message
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(70, 100, 83))
            setLineSpacing(6f, 1.1f)
        }
        addView(body)
    }

    fun setMessage(message: String) { body.text = message }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        emblem.scaleX = 0f
        emblem.scaleY = 0f
        emblem.animate().scaleX(1f).scaleY(1f).setStartDelay(150).setDuration(650)
            .setInterpolator(OvershootInterpolator()).start()
        sparkle = ObjectAnimator.ofFloat(emblem, "rotation", -5f, 5f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = 3
            start()
        }
    }

    override fun onDetachedFromWindow() {
        sparkle?.cancel()
        emblem.animate().cancel()
        super.onDetachedFromWindow()
    }
}
