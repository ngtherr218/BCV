package com.project.bcv.feature.collection.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.core.di.CollectionModule
import com.project.bcv.core.ui.BottomNav
import com.project.bcv.core.ui.NavTab
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.databinding.ActivityCollectionBinding
import com.project.bcv.databinding.ItemCollectionCardBinding
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.collection.domain.CollectionOverview
import com.project.bcv.feature.collection.domain.CollectionSummary
import com.project.bcv.feature.journey.presentation.JourneyActivity
import com.project.bcv.feature.questitem.quest.presentation.QuestActivity
import kotlinx.coroutines.launch

/** "Bộ sưu tập" tab: cards owned overall and per collection. */
class CollectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCollectionBinding
    private lateinit var model: CollectionViewModel
    private var leaving = false
    private var resumedOnce = false
    private var shownOverview: CollectionOverview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            BottomNav.applyBottomInset(binding.bottomNavigation, bars.bottom)
            insets
        }
        binding.summaryBar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> layoutSummaryBar() }

        BottomNav.select(binding.bottomNavigation, NavTab.COLLECTION)
        BottomNav.skipTransition(this, opening = true)
        BottomNav.skipTransition(this, opening = false)
        BottomNav.enter(BottomNav.contentOf(binding.root, binding.bottomNavigation),
            fromRight = BottomNav.enteredFromRight(this, NavTab.COLLECTION))
        binding.bottomNavigation.tabExplore.setOnClickListener { leave() }
        binding.bottomNavigation.tabJourney.setOnClickListener {
            leave(Intent(BottomNav.intent(this, JourneyActivity::class.java, NavTab.COLLECTION)))
        }
        binding.bottomNavigation.tabCollection.setOnClickListener { model.load() }
        binding.bottomNavigation.tabQuest.setOnClickListener {
            leave(BottomNav.intent(this, QuestActivity::class.java, NavTab.COLLECTION), toRight = false)
        }
        binding.retryButton.setOnClickListener { model.load() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = leave()
        })

        model = ViewModelProvider(this, CollectionViewModelFactory(CollectionModule.repository(this)))[
            CollectionViewModel::class.java]
        model.state.observe(this, ::render)
    }

    override fun onResume() {
        super.onResume()
        // A journey finished elsewhere may have earned a card while this tab was in the back stack.
        if (resumedOnce) model.load()
        resumedOnce = true
    }

    override fun finish() {
        super.finish()
        BottomNav.skipTransition(this, opening = false)
    }

    /**
     * Leaves this tab: content slides toward the tab being opened (every other tab is to the left),
     * then either returns to Khám phá or opens [next] in place.
     */
    private fun leave(next: Intent? = null, toRight: Boolean = true) {
        if (leaving) return
        leaving = true
        BottomNav.exit(BottomNav.contentOf(binding.root, binding.bottomNavigation), toRight = toRight) {
            if (next != null) {
                startActivity(next)
                BottomNav.skipTransition(this, opening = true)
            }
            finish()
        }
    }

    private fun render(state: CollectionUiState) = with(binding) {
        when (state) {
            CollectionUiState.Loading -> {
                Skeleton.show(loadingSkeleton.root, loadedContent, messageState)
            }
            is CollectionUiState.Failed -> {
                Skeleton.hide(loadingSkeleton.root)
                loadedContent.visibility = View.GONE
                showMessage(text = state.message, retry = true)
            }
            CollectionUiState.SessionExpired -> {
                SessionPreferences(this@CollectionActivity).token = null
                startActivity(Intent(this@CollectionActivity, AccountActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            is CollectionUiState.Loaded -> showOverview(state.overview)
        }
    }

    private fun showMessage(text: String, retry: Boolean = false) = with(binding) {
        messageState.visibility = View.VISIBLE
        messageText.text = text
        retryButton.visibility = if (retry) View.VISIBLE else View.GONE
    }

    private fun showOverview(overview: CollectionOverview) = with(binding) {
        if (overview == shownOverview && loadedContent.visibility == View.VISIBLE) return@with
        shownOverview = overview
        Skeleton.hide(loadingSkeleton.root)
        loadedContent.visibility = View.VISIBLE
        ownedCount.text = overview.ownedCards.toString()
        ownedTotal.text = "/ ${overview.totalCards} thẻ đã sở hữu"
        summaryCard.contentDescription = "Đã sở hữu ${overview.ownedCards} trên ${overview.totalCards} thẻ"
        layoutSummaryBar()
        collectionCount.text = "${overview.collections.size} bộ"

        collectionList.removeAllViews()
        overview.collections.forEachIndexed { index, collection ->
            collectionList.addView(collectionCard(collection, ACCENTS[index % ACCENTS.size]).root)
        }
        if (overview.collections.isEmpty()) {
            showMessage(text = "Chưa có bộ sưu tập nào. Hoàn thành hành trình để nhận thẻ.")
        } else {
            messageState.visibility = View.GONE
        }
    }

    private fun layoutSummaryBar() {
        val overview = shownOverview ?: return
        val bar = binding.summaryBar
        if (bar.width == 0) return
        val fraction = if (overview.totalCards == 0) 0f
            else overview.ownedCards.toFloat() / overview.totalCards
        binding.summaryFill.layoutParams = binding.summaryFill.layoutParams.apply {
            width = (bar.width * fraction.coerceIn(0f, 1f)).toInt()
        }
    }

    private fun collectionCard(collection: CollectionSummary, accent: Int): ItemCollectionCardBinding {
        val item = ItemCollectionCardBinding.inflate(layoutInflater, binding.collectionList, false)
        item.collectionName.text = collection.name
        item.collectionOwned.text = "${collection.ownedCards} / ${collection.totalCards} thẻ đã sở hữu"
        item.collectionProgress.max = collection.totalCards.coerceAtLeast(1)
        item.collectionProgress.progress = collection.ownedCards
        item.collectionProgress.setIndicatorColor(accent)
        item.collectionAction.setTextColor(accent)
        item.collectionArrow.imageTintList = ColorStateList.valueOf(accent)
        item.root.contentDescription = "${collection.name}, đã sở hữu ${collection.ownedCards} trên " +
            "${collection.totalCards} thẻ. Xem bộ sưu tập"
        item.root.setOnClickListener {
            startActivity(Intent(this, CollectionDetailActivity::class.java)
                .putExtra(CollectionDetailActivity.EXTRA_COLLECTION_ID, collection.id)
                .putExtra(CollectionDetailActivity.EXTRA_COLLECTION_NAME, collection.name)
                .putExtra(CollectionDetailActivity.EXTRA_COLLECTION_DESCRIPTION, collection.description))
        }
        fillPreview(item.previewGrid, collection)
        return item
    }

    /** Four slots: owned card photos first, then empty frames for cards still to collect. */
    private fun fillPreview(grid: GridLayout, collection: CollectionSummary) {
        val density = resources.displayMetrics.density
        val slots = minOf(4, collection.totalCards.coerceAtLeast(1))
        repeat(4) { slot ->
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                background = GradientDrawable().apply {
                    cornerRadius = 6 * density
                    setColor(0xFFF1F3EE.toInt())
                    setStroke((1 * density).toInt().coerceAtLeast(1), 0xFFE2E8DF.toInt())
                }
                visibility = if (slot < slots) View.VISIBLE else View.INVISIBLE
            }
            grid.addView(image, GridLayout.LayoutParams().apply {
                width = (40 * density).toInt()
                height = (52 * density).toInt()
                setMargins(0, 0, if (slot % 2 == 0) (5 * density).toInt() else 0,
                    if (slot < 2) (5 * density).toInt() else 0)
            })
            val url = collection.previewImageUrls.getOrNull(slot) ?: return@repeat
            RemoteImages.cached(url, sample = 8)?.let { image.setImageBitmap(it); return@repeat }
            lifecycleScope.launch { RemoteImages.load(url, sample = 8)?.let(image::setImageBitmap) }
        }
    }

    private fun unavailable(name: String) {
        Snackbar.make(binding.root, "$name sẽ được bổ sung ở màn tiếp theo.", Snackbar.LENGTH_SHORT).show()
    }

    private companion object {
        /** Per-collection accent, cycling like the design: green, brown, blue. */
        val ACCENTS = intArrayOf(0xFF07836D.toInt(), 0xFF9C6B2E.toInt(), 0xFF3C7DB0.toInt())
    }
}
