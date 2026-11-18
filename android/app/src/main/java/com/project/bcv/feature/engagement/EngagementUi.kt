package com.project.bcv.feature.engagement

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.project.bcv.R
import java.text.NumberFormat
import java.util.Locale

/** Small view builders so the engagement screens match the app's cards, colours and type scale. */
internal object EngagementUi {
    const val TEAL = 0xFF07836D.toInt()
    const val FOREST = 0xFF19473B.toInt()
    const val MUTED = 0xFF6F8579.toInt()
    const val LINE = 0xFFE3EBE3.toInt()
    const val SOFT = 0xFFF1F5F1.toInt()
    const val GOLD = 0xFFC9962B.toInt()
    const val GOLD_SOFT = 0xFFFBF3E2.toInt()
    val vi: Locale = Locale.forLanguageTag("vi-VN")

    fun Context.dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    fun number(value: Int): String = NumberFormat.getIntegerInstance(vi).format(value)

    fun rounded(color: Int, radius: Int, stroke: Int? = null, context: Context) = GradientDrawable().apply {
        setColor(color); cornerRadius = context.dp(radius).toFloat()
        if (stroke != null) setStroke(context.dp(1), stroke)
    }

    fun Context.text(value: CharSequence, size: Float = 15f, color: Int = FOREST, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    /**
     * A centred white card dialog (28dp corners, 24dp margins) for celebrations and details;
     * [build] fills its vertical, centred column.
     */
    fun Activity.cardDialog(build: LinearLayout.(android.app.Dialog) -> Unit): android.app.Dialog {
        val dialog = android.app.Dialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(0xFFFFFFFF.toInt(), 28, null, context)
            setPadding(dp(22), dp(24), dp(22), dp(18))
        }
        box.build(dialog)
        dialog.setContentView(box)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels - dp(48)).coerceAtMost(dp(400)), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return dialog
    }

    /** Springs a view in: scaled up from small with a little overshoot. */
    fun View.popIn(delay: Long = 80) {
        scaleX = 0.4f; scaleY = 0.4f; alpha = 0f
        animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(delay).setDuration(420)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.6f)).start()
    }

    /** A Material Symbols icon (res/drawable/ic_m3_*), tinted; replaces emoji used as UI icons. */
    fun Context.icon(res: Int, color: Int = FOREST, sizeDp: Int = 20) = android.widget.ImageView(this).apply {
        setImageResource(res)
        imageTintList = android.content.res.ColorStateList.valueOf(color)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    /** Puts a tinted Material Symbols icon before a line of text. */
    fun TextView.leadingIcon(res: Int, color: Int = currentTextColor, sizeDp: Int = 16, gapDp: Int = 6) = apply {
        val size = (sizeDp * resources.displayMetrics.density).toInt()
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, res)!!.mutate().apply {
            setTint(color); setBounds(0, 0, size, size)
        }
        setCompoundDrawablesRelative(drawable, null, null, null)
        compoundDrawablePadding = (gapDp * resources.displayMetrics.density).toInt()
        gravity = gravity or Gravity.CENTER_VERTICAL
    }

    fun Context.card(vertical: Boolean = true, color: Int = 0xFFFFFFFF.toInt()) = LinearLayout(this).apply {
        orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        background = rounded(color, 20, LINE, this@card)
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }

    fun Context.progress(value: Int, max: Int, color: Int = TEAL) =
        ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            this.max = max.coerceAtLeast(1); progress = value.coerceIn(0, max.coerceAtLeast(1))
            progressTintList = android.content.res.ColorStateList.valueOf(color)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(LINE)
        }

    fun Context.pill(value: String, fg: Int = TEAL, bg: Int = SOFT) = text(value, 11f, fg, true).apply {
        background = rounded(bg, 12, null, this@pill)
        setPadding(dp(9), dp(3), dp(9), dp(3))
    }

    fun Context.primaryButton(label: String) = MaterialButton(this).apply {
        text = label; isAllCaps = false; setTypeface(typeface, Typeface.BOLD)
        backgroundTintList = android.content.res.ColorStateList.valueOf(TEAL); cornerRadius = dp(16)
    }

    fun Context.outlinedButton(label: String) =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label; isAllCaps = false; setTextColor(TEAL); setTypeface(typeface, Typeface.BOLD)
            strokeColor = android.content.res.ColorStateList.valueOf(0xFFD6E3DA.toInt()); cornerRadius = dp(16)
        }

    fun LinearLayout.gap(height: Int) = addView(View(context), LinearLayout.LayoutParams(1, context.dp(height)))

    fun LinearLayout.full(view: View, top: Int = 0) =
        addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(top) })

    /**
     * Screen scaffold: title, scrollable body padded for system bars. Returns the body.
     * With [tab] the screen is a bottom-navigation tab (no back arrow, shared bar at the bottom);
     * without it, a pushed screen with a back arrow.
     */
    fun AppCompatActivity.scaffold(title: String, subtitle: String? = null,
                                   tab: com.project.bcv.core.ui.NavTab? = null, top: View? = null,
                                   action: View? = null): LinearLayout {
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.tab_screen_bg))
        }
        val nav = tab?.let { com.project.bcv.databinding.ViewBottomNavBinding.inflate(layoutInflater, root, false) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, if (nav != null) 0 else bars.bottom)
            nav?.let { com.project.bcv.core.ui.BottomNav.applyBottomInset(it, bars.bottom) }
            insets
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(if (tab != null) 20 else 16), dp(if (tab != null) 20 else 10), dp(16), dp(6))
        }
        if (tab == null) header.addView(FrameLayout(this).apply {
            background = ContextCompat.getDrawable(context, R.drawable.explore_header_circle)
            contentDescription = "Quay lại"; isClickable = true; isFocusable = true
            setOnClickListener { finish() }
            addView(android.widget.ImageView(context).apply {
                setImageResource(R.drawable.ic_arrow_back_engagement)
                setPadding(dp(11), dp(11), dp(11), dp(11))
            })
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(if (tab != null) 0 else dp(14), 0, 0, 0) }
        if (tab != null) titles.addView(text("BƯỚC CHÂN VIỆT", 12f, TEAL, true))
        titles.addView(text(title, if (tab != null) 30f else 26f, FOREST, true).apply { maxLines = 2 })
        if (subtitle != null) titles.addView(text(subtitle, 12f, MUTED))
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        // An optional control at the right of the title (e.g. "Giải thưởng" on Xếp hạng).
        if (action != null) header.addView(action)
        root.addView(header)
        // Fixed above the scrolling body, e.g. the Thẻ · Hộ chiếu · Nhật ký switcher.
        if (top != null) root.addView(top, LinearLayout.LayoutParams(-1, -2).apply {
            marginStart = dp(20); marginEnd = dp(20); topMargin = dp(6); bottomMargin = dp(8)
        })
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(28))
        }
        root.addView(ScrollView(this).apply { addView(body); isFillViewport = true; clipToPadding = false },
            LinearLayout.LayoutParams(-1, 0, 1f))
        if (nav != null && tab != null) {
            root.addView(nav.root, LinearLayout.LayoutParams(-1, dp(72)))
            setContentView(root)
            val bar = com.project.bcv.core.ui.BottomNav
            bar.wire(this, nav, tab, root) { }
            bar.skipTransition(this, opening = true)
            if (!intent.getBooleanExtra(bar.EXTRA_SAME_TAB, false))
                bar.enter(bar.contentOf(root, nav), fromRight = bar.enteredFromRight(this, tab))
            // Back returns to Khám phá, like the other tabs.
            onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = bar.go(this@scaffold, nav, tab, com.project.bcv.core.ui.NavTab.EXPLORE, root)
            })
        } else setContentView(root)
        return body
    }

    fun Activity.sectionTitle(label: String, hint: String? = null) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(20), 0, dp(8))
        addView(text(label, 17f, FOREST, true))
        if (hint != null) addView(text(hint, 12f, MUTED))
    }
}
