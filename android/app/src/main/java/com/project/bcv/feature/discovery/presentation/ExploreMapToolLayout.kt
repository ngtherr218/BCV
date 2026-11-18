package com.project.bcv.feature.discovery.presentation

import android.view.View

/** Reflows map actions into one rail, keeping them above the journey sheet. */
internal object ExploreMapToolLayout {
    /** Keep one tool rail clear of both the location card and the expanded sheet. */
    fun apply(binding: com.project.bcv.databinding.ActivityExploreBinding, mapReady: Boolean, collapsed: Boolean, filtered: Boolean) {
        val density = binding.root.resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()
        val tools = binding.mapTools
        val visible = listOf(binding.recenterButton, binding.quizMapButton, binding.addStepsMapButton).filter { it.visibility == View.VISIBLE }
        if (binding.mapContainer.height == 0 || visible.isEmpty()) return
        val covered = if (collapsed) dp(86) else binding.bottomSheet.height
        val available = (binding.mapContainer.height - covered).coerceAtLeast(0)
        tools.visibility = if (mapReady && available >= dp(76)) View.VISIBLE else View.INVISIBLE
        binding.mapFilterBar.visibility = if (mapReady && filtered && available >= dp(124)) View.VISIBLE else View.GONE
        val horizontal = available < dp(visible.size * 48 + (visible.size - 1) * 8 + 28)
        val orientation = if (horizontal) android.widget.LinearLayout.HORIZONTAL else android.widget.LinearLayout.VERTICAL
        if (tools.orientation != orientation) tools.orientation = orientation
        visible.forEachIndexed { index, button ->
            val params = button.layoutParams as android.widget.LinearLayout.LayoutParams
            val top = if (!horizontal && index > 0) dp(8) else 0
            val start = if (horizontal && index > 0) dp(8) else 0
            if (params.topMargin != top || params.marginStart != start) {
                params.topMargin = top; params.marginStart = start; button.layoutParams = params
            }
        }
        val width = if (horizontal) dp(visible.size * 48 + (visible.size - 1) * 8) else dp(48)
        if (tools.layoutParams.width != width) tools.layoutParams = tools.layoutParams.apply { this.width = width }
        val cardFits = !horizontal || binding.mapContainer.width >= dp(220 + 48) + width
        binding.locationCard.visibility = if (mapReady && cardFits && available >= dp(78)) View.VISIBLE else View.GONE
    }

}
