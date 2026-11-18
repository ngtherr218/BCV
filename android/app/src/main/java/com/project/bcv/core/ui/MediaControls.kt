package com.project.bcv.core.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import java.util.Locale

/** m:ss for a video length or playback position given in whole seconds. */
fun formatClock(totalSeconds: Int): String =
    totalSeconds.coerceAtLeast(0).let { String.format(Locale.ROOT, "%d:%02d", it / 60, it % 60) }

/**
 * Translucent round icon button for controls laid over photos and video (photo viewers, video player).
 * The caller sizes it through its layout params; the icon stays 22 dp.
 */
fun Context.roundIconButton(iconRes: Int, label: String, tint: Int = 0xFFFFFFFF.toInt(), onClick: () -> Unit) =
    FrameLayout(this).apply {
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x33FFFFFF) }
        isClickable = true; contentDescription = label
        val size = (22 * resources.displayMetrics.density).toInt()
        addView(ImageView(context).apply {
            setImageResource(iconRes); imageTintList = ColorStateList.valueOf(tint)
        }, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        setOnClickListener { onClick() }
    }
