package com.project.bcv.feature.account.interests.presentation

import android.app.Activity
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.R
import com.project.bcv.core.di.InterestModule
import com.project.bcv.databinding.ActivityInterestsBinding
import com.project.bcv.feature.account.interests.domain.PlaceType

class InterestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInterestsBinding
    private lateinit var model: InterestsViewModel
    private var renderedIds: List<Long> = emptyList()
    private val cards = mutableMapOf<Long, MaterialCardView>()
    private var orbitAnimator: ObjectAnimator? = null
    private var skeletonAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityInterestsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val enabledStates = arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf())
        binding.continueButton.backgroundTintList = ColorStateList(
            enabledStates, intArrayOf(color(R.color.teal), 0xFFDFE8DC.toInt())
        )
        binding.continueButton.setTextColor(ColorStateList(
            enabledStates, intArrayOf(color(R.color.white), 0xFF809482.toInt())
        ))
        binding.continueButton.iconTint = ColorStateList(
            enabledStates, intArrayOf(color(R.color.white), 0xFF809482.toInt())
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        model = ViewModelProvider(this, InterestsViewModelFactory(InterestModule.repository(this)))[InterestsViewModel::class.java]
        buildSkeleton()
        model.state.observe(this, ::render)
        binding.continueButton.setOnClickListener { model.save() }
        binding.retryButton.setOnClickListener { model.load() }
    }

    override fun onResume() {
        super.onResume()
        if (!ValueAnimator.areAnimatorsEnabled()) return
        val orbit = binding.orbitDecoration
        orbitAnimator = ObjectAnimator.ofFloat(orbit, View.ROTATION, orbit.rotation, orbit.rotation + 360f).apply {
            duration = 18_000L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    override fun onPause() {
        orbitAnimator?.cancel()
        orbitAnimator = null
        super.onPause()
    }

    override fun onDestroy() {
        stopSkeletonPulse()
        super.onDestroy()
    }

    /** Five rows of two placeholders: the ten place types, laid out like the grid built in showPlaceTypes. */
    private fun buildSkeleton() {
        repeat(5) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            repeat(2) { index ->
                val card = layoutInflater.inflate(R.layout.item_place_type_skeleton, row, false)
                if (index == 1) {
                    card.layoutParams = (card.layoutParams as LinearLayout.LayoutParams).apply { marginStart = dp(10) }
                }
                row.addView(card)
            }
            binding.placeTypeSkeleton.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
    }

    private fun startSkeletonPulse() {
        if (skeletonAnimator != null || !ValueAnimator.areAnimatorsEnabled()) return
        skeletonAnimator = ObjectAnimator.ofFloat(binding.placeTypeSkeleton, View.ALPHA, 1f, 0.55f).apply {
            duration = 850L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopSkeletonPulse() {
        skeletonAnimator?.cancel()
        skeletonAnimator = null
        binding.placeTypeSkeleton.alpha = 1f
    }

    private fun render(state: InterestsUiState) {
        if (state.saved) {
            setResult(Activity.RESULT_OK)
            finish()
            return
        }
        if (renderedIds != state.placeTypes.map { it.id }) {
            showPlaceTypes(state.placeTypes)
        }
        cards.forEach { (id, card) ->
            val selected = id in state.selectedIds
            card.strokeColor = color(if (selected) R.color.teal else R.color.field_line)
            card.strokeWidth = dp(if (selected) 2 else 1)
            card.setCardBackgroundColor(color(if (selected) R.color.field_bg else R.color.white))
            card.findViewById<ImageView>(R.id.selectionIndicator).apply {
                setBackgroundResource(if (selected) R.drawable.interest_selection_checked else R.drawable.interest_selection)
                setImageResource(if (selected) R.drawable.ic_check else 0)
                imageTintList = ColorStateList.valueOf(color(R.color.white))
            }
            card.isEnabled = !state.loading && !state.saving
            card.contentDescription = "${card.findViewById<TextView>(R.id.placeTypeName).text}, " +
                if (selected) "đã chọn" else "chưa chọn"
        }
        val showSkeleton = state.loading && state.placeTypes.isEmpty()
        binding.loadingContent.visibility = if (showSkeleton) View.VISIBLE else View.GONE
        binding.loadingFooter.visibility = binding.loadingContent.visibility
        binding.selectionCount.visibility = if (showSkeleton) View.GONE else View.VISIBLE
        if (showSkeleton) startSkeletonPulse() else stopSkeletonPulse()
        binding.retryButton.visibility = if (!state.loading && state.placeTypes.isEmpty()) View.VISIBLE else View.GONE
        binding.selectionCount.text = when {
            state.selectedIds.isEmpty() -> "Chưa chọn nhóm nào"
            state.selectedIds.size > InterestsViewModel.MAX_INTERESTS ->
                "Bỏ bớt ${state.selectedIds.size - InterestsViewModel.MAX_INTERESTS} nhóm"
            else -> "Đã chọn ${state.selectedIds.size}/${InterestsViewModel.MAX_INTERESTS} nhóm"
        }
        binding.continueButton.isEnabled = !state.loading && !state.saving &&
            state.selectedIds.size in 1..InterestsViewModel.MAX_INTERESTS
        binding.continueButton.text = if (state.saving) "Đang lưu..." else "Tiếp tục"
        binding.requestError.text = state.error
        binding.requestError.visibility = if (state.error == null) View.GONE else View.VISIBLE
    }

    private fun showPlaceTypes(types: List<PlaceType>) {
        renderedIds = types.map { it.id }
        cards.clear()
        binding.placeTypeRows.removeAllViews()
        types.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val rowCards = mutableListOf<MaterialCardView>()
            pair.forEachIndexed { index, placeType ->
                val card = layoutInflater.inflate(R.layout.item_place_type, row, false) as MaterialCardView
                card.findViewById<TextView>(R.id.placeTypeName).text = placeType.name
                card.setOnClickListener {
                    if (!model.toggle(placeType.id)) {
                        Snackbar.make(binding.root, "Chỉ được chọn tối đa 3 nhóm địa danh.", Snackbar.LENGTH_SHORT).show()
                    }
                }
                row.addView(card, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index == 1) marginStart = dp(10)
                })
                cards[placeType.id] = card
                rowCards.add(card)
            }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = dp(10) })
            binding.placeTypeRows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
            row.post {
                val height = rowCards.maxOfOrNull { it.height } ?: return@post
                rowCards.forEach { card ->
                    card.layoutParams = card.layoutParams.apply { this.height = height }
                }
            }
        }
        if (types.isNotEmpty()) {
            binding.placeTypeRows.alpha = 0f
            binding.placeTypeRows.translationY = dp(10).toFloat()
            binding.placeTypeRows.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(60L)
                .setDuration(280L)
                .start()
        }
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
