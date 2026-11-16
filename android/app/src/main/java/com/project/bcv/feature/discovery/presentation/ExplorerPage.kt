package com.project.bcv.feature.discovery.presentation

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.project.bcv.R
import com.project.bcv.core.ui.setUpDetailScreen

/** Shared typography, spacing and surfaces for explorer detail screens. */
internal class ExplorerPage(val activity: AppCompatActivity, title: String) {
    val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(color(R.color.white)) }
    val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(24)) }
    init {
        val header = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(20), 0) }
        header.addView(ImageButton(activity).apply {
            setImageResource(R.drawable.ic_arrow); rotation = 180f; contentDescription = "Quay lại"
            setBackgroundResource(R.drawable.activity_back_circle); setPadding(dp(12), dp(12), dp(12), dp(12))
            imageTintList = android.content.res.ColorStateList.valueOf(color(R.color.forest))
            setOnClickListener { activity.finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(label(title, 18, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(64), 1f))
        root.addView(header)
        root.addView(ScrollView(activity).apply { isFillViewport = true; isVerticalScrollBarEnabled = false; addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        activity.setUpDetailScreen(root)
    }
    fun dp(n: Int) = (n * activity.resources.displayMetrics.density).toInt()
    fun color(id: Int) = activity.getColor(id)
    fun label(text: String, size: Int = 14, bold: Boolean = false, muted: Boolean = false) = TextView(activity).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color(if (muted) R.color.muted else R.color.forest))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    fun button(text: String, action: () -> Unit) = MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        this.text = text; isAllCaps = false; cornerRadius = dp(14); minHeight = dp(48)
        strokeColor = android.content.res.ColorStateList.valueOf(color(R.color.field_line))
        setTextColor(color(R.color.teal)); setOnClickListener { action() }
    }
    fun panel(dark: Boolean = false) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat(); setColor(color(if (dark) R.color.forest else R.color.field_bg))
            if (!dark) setStroke(dp(1), color(R.color.field_line))
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
    }
    fun LinearLayout.line(view: View, top: Int = 8) { addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }) }
    fun progress(value: Int) = LinearProgressIndicator(activity).apply {
        max = 100; progress = value.coerceIn(0,100); setIndicatorColor(color(R.color.teal))
        trackColor = color(R.color.field_line); trackThickness = dp(6); trackCornerRadius = dp(3); trackStopIndicatorSize = 0
    }
}
