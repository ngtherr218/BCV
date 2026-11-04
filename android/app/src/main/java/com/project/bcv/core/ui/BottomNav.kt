package com.project.bcv.core.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import androidx.core.content.ContextCompat
import com.project.bcv.R
import com.project.bcv.databinding.ViewBottomNavBinding

enum class NavTab { EXPLORE, JOURNEY, COLLECTION, QUEST }

/** Styling and switching for the shared bottom navigation (layout/view_bottom_nav.xml). */
object BottomNav {
    private const val INACTIVE = 0xFF7D8A82.toInt()

    fun select(nav: ViewBottomNavBinding, selected: NavTab) {
        val teal = ContextCompat.getColor(nav.root.context, R.color.teal)
        listOf(
            Triple(NavTab.EXPLORE, nav.tabExploreIcon, nav.tabExploreLabel) to "Khám phá",
            Triple(NavTab.JOURNEY, nav.tabJourneyIcon, nav.tabJourneyLabel) to "Hành trình",
            Triple(NavTab.COLLECTION, nav.tabCollectionIcon, nav.tabCollectionLabel) to "Bộ sưu tập",
            Triple(NavTab.QUEST, nav.tabQuestIcon, nav.tabQuestLabel) to "Nhiệm vụ"
        ).forEach { (views, name) ->
            val (tab, icon, label) = views
            val active = tab == selected
            icon.setBackgroundResource(if (active) R.drawable.nav_active_indicator_bg else 0)
            icon.imageTintList = ColorStateList.valueOf(if (active) teal else INACTIVE)
            label.setTextColor(if (active) teal else INACTIVE)
            label.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
            (icon.parent as android.view.View).contentDescription = if (active) "$name, đang chọn" else name
        }
    }

    /** Gives the bar its height plus the gesture-area inset, so its surface reaches the screen edge. */
    fun applyBottomInset(nav: ViewBottomNavBinding, bottom: Int) = nav.root.apply {
        setPadding(paddingLeft, paddingTop, paddingRight, bottom)
        layoutParams = layoutParams.apply {
            height = (72 * resources.displayMetrics.density).toInt() + bottom
        }
    }

    /**
     * Slides a tab's content in, from the right for a tab to the right of the previous one.
     * Only the content moves; the bar (and anything else outside [content]) stays put.
     */
    fun enter(content: List<View>, fromRight: Boolean) {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        val shift = SHIFT_DP * content.firstOrNull()?.resources?.displayMetrics?.density.orZero()
        content.forEach { view ->
            view.animate().cancel()
            view.alpha = 0f
            view.translationX = if (fromRight) shift else -shift
            view.animate().alpha(1f).translationX(0f).setDuration(ENTER_MS)
                .setInterpolator(FastOutSlowInInterpolator()).start()
        }
    }

    /** Slides a tab's content out, then runs [end] (e.g. finish) once it is gone. */
    fun exit(content: List<View>, toRight: Boolean, end: () -> Unit) {
        if (!ValueAnimator.areAnimatorsEnabled() || content.isEmpty()) {
            end()
            return
        }
        val shift = SHIFT_DP * content.first().resources.displayMetrics.density
        content.forEachIndexed { index, view ->
            view.animate().cancel()
            view.animate().alpha(0f).translationX(if (toRight) shift else -shift).setDuration(EXIT_MS)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction(if (index == 0) Runnable(end) else null).start()
        }
    }

    /** Intent extra: the tab the user left, so the new tab slides in from the matching side. */
    const val EXTRA_FROM_TAB = "from_tab"

    fun intent(context: Context, target: Class<out Activity>, from: NavTab): Intent =
        Intent(context, target).putExtra(EXTRA_FROM_TAB, from.name)

    /** True when [self] is to the right of the tab this activity was opened from. */
    fun enteredFromRight(activity: Activity, self: NavTab): Boolean {
        val from = activity.intent.getStringExtra(EXTRA_FROM_TAB)
            ?.let { name -> NavTab.entries.firstOrNull { it.name == name } } ?: NavTab.EXPLORE
        return self.ordinal > from.ordinal
    }

    /** Every child of the screen root except the navigation bar. */
    fun contentOf(root: ViewGroup, nav: ViewBottomNavBinding): List<View> =
        (0 until root.childCount).map(root::getChildAt).filter { it !== nav.root }

    private fun Float?.orZero() = this ?: 0f

    private const val SHIFT_DP = 28f
    private const val ENTER_MS = 220L
    private const val EXIT_MS = 160L

    /**
     * Tab switches show no window animation: the bar stays exactly where it is and only the
     * content above it changes. Call right after startActivity/finish, and in the new tab's onCreate.
     */
    @Suppress("DEPRECATION")
    fun skipTransition(activity: Activity, opening: Boolean) {
        if (Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(
                if (opening) Activity.OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            activity.overridePendingTransition(0, 0)
        }
    }
}
