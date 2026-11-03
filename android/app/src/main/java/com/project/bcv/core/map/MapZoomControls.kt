package com.project.bcv.core.map

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import com.project.bcv.R

/** Explicit zoom controls also work with a mouse on the Android emulator. */
object MapZoomControls {
    fun attach(host: FrameLayout, topDp: Int = 12, zoomIn: () -> Unit, zoomOut: () -> Unit): LinearLayout {
        val density = host.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val controls = LinearLayout(host.context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.starting_point_card_bg)
            elevation = dp(6).toFloat()
        }
        listOf(Triple("+", "Phóng to bản đồ", zoomIn), Triple("−", "Thu nhỏ bản đồ", zoomOut)).forEach { (label, description, action) ->
            controls.addView(AppCompatButton(host.context).apply {
                text = label
                textSize = 23f
                contentDescription = description
                setTextColor(0xFF07836D.toInt())
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setPadding(0, 0, 0, 0)
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
        }
        host.addView(controls, FrameLayout.LayoutParams(dp(48), -2).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = dp(topDp)
            marginEnd = dp(12)
        })
        return controls
    }
}
