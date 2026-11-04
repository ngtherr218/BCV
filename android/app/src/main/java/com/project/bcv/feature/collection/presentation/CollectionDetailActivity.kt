package com.project.bcv.feature.collection.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.project.bcv.R
import com.project.bcv.core.di.CollectionModule
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.databinding.ActivityCollectionDetailBinding
import com.project.bcv.databinding.ItemCollectionPlaceCardBinding
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.collection.domain.CollectionCard
import com.project.bcv.feature.collection.domain.CollectionDetail
import kotlinx.coroutines.launch
import java.util.Locale

/** One collection: owned share and every card, owned ones with their photo, others locked. */
class CollectionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCollectionDetailBinding
    private lateinit var model: CollectionDetailViewModel
    private var collectionId = 0L
    private var shown: CollectionDetail? = null
    private var resumedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCollectionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        collectionId = intent.getLongExtra(EXTRA_COLLECTION_ID, 0L)
        if (collectionId <= 0) { finish(); return }
        // Known from the list, so the heading shows before the API answers.
        binding.collectionName.text = intent.getStringExtra(EXTRA_COLLECTION_NAME).orEmpty()
        binding.collectionDescription.text = intent.getStringExtra(EXTRA_COLLECTION_DESCRIPTION).orEmpty()
        binding.backButton.setOnClickListener { finish() }
        binding.retryButton.setOnClickListener { model.load(collectionId) }

        model = ViewModelProvider(this, CollectionDetailViewModelFactory(CollectionModule.repository(this)))[
            CollectionDetailViewModel::class.java]
        model.state.observe(this, ::render)
        if (savedInstanceState == null) model.load(collectionId)
    }

    override fun onResume() {
        super.onResume()
        // Coming back from a card or place: a journey may have unlocked something meanwhile.
        if (resumedOnce) model.load(collectionId)
        resumedOnce = true
    }

    private fun render(state: CollectionDetailUiState) = with(binding) {
        if (state != CollectionDetailUiState.Loading) Skeleton.hide(skeleton.root)
        when (state) {
            // A reload over cards already shown keeps them; only the first load shows the placeholder.
            CollectionDetailUiState.Loading -> if (shown == null) Skeleton.show(skeleton.root, content, messageState)
            is CollectionDetailUiState.Failed -> showMessage(state.message, retry = true)
            CollectionDetailUiState.SessionExpired -> {
                SessionPreferences(this@CollectionDetailActivity).token = null
                startActivity(Intent(this@CollectionDetailActivity, AccountActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            is CollectionDetailUiState.Loaded -> show(state.detail)
        }
    }

    private fun showMessage(text: String, retry: Boolean = false) = with(binding) {
        content.visibility = View.GONE
        messageState.visibility = View.VISIBLE
        messageProgress.visibility = View.GONE
        messageText.text = text
        retryButton.visibility = if (retry) View.VISIBLE else View.GONE
    }

    private fun show(detail: CollectionDetail) = with(binding) {
        if (detail == shown) return@with
        val first = shown == null
        shown = detail
        messageState.visibility = View.GONE
        content.visibility = View.VISIBLE
        collectionName.text = detail.name
        collectionDescription.text = detail.description.orEmpty()
        collectionDescription.visibility = if (detail.description.isNullOrBlank()) View.GONE else View.VISIBLE

        ownedText.text = "${detail.ownedCards} / ${detail.totalCards} thẻ"
        val percent = if (detail.totalCards == 0) 0.0 else detail.ownedCards * 100.0 / detail.totalCards
        percentText.text = String.format(Locale.forLanguageTag("vi-VN"), "%.1f", percent).removeSuffix(",0") + "%"
        ownedProgress.max = detail.totalCards.coerceAtLeast(1)
        ownedProgress.setProgressCompat(detail.ownedCards, !first)
        summaryCard.contentDescription = "Đã sở hữu ${detail.ownedCards} trên ${detail.totalCards} thẻ, ${percentText.text}"

        cardGrid.removeAllViews()
        val density = resources.displayMetrics.density
        detail.cards.chunked(2).forEachIndexed { rowIndex, pair ->
            val row = LinearLayout(this@CollectionDetailActivity).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { index, card ->
                val item = cardTile(card, detail)
                row.addView(item.root, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (index == 1) marginStart = (12 * density).toInt() })
            }
            if (pair.size == 1) row.addView(View(this@CollectionDetailActivity),
                LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = (12 * density).toInt() })
            cardGrid.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                if (rowIndex > 0) topMargin = (12 * density).toInt()
            })
        }
        if (first) ScreenMotion.reveal(content)
    }

    private fun cardTile(card: CollectionCard, detail: CollectionDetail): ItemCollectionPlaceCardBinding {
        val item = ItemCollectionPlaceCardBinding.inflate(layoutInflater, binding.cardGrid, false)
        val muted = 0xFF8A8F86.toInt()
        item.cardName.text = card.name
        item.cardName.setTextColor(if (card.owned) ContextCompat.getColor(this, R.color.forest) else muted)
        item.cardState.text = if (card.owned) "Đã sở hữu" else "Chưa sở hữu"
        item.cardState.setTextColor(if (card.owned) ContextCompat.getColor(this, R.color.teal) else muted)
        item.cardState.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (card.owned) R.drawable.ic_check_circle else R.drawable.ic_lock, 0, 0, 0)
        item.cardState.compoundDrawablesRelative.firstOrNull()?.let { icon ->
            val size = (16 * resources.displayMetrics.density).toInt()
            icon.setBounds(0, 0, size, size)
            item.cardState.setCompoundDrawablesRelative(icon, null, null, null)
        }
        item.cardState.compoundDrawableTintList = ColorStateList.valueOf(
            if (card.owned) ContextCompat.getColor(this, R.color.teal) else muted)
        item.root.setBackgroundResource(if (card.owned) R.drawable.journey_card_bg else R.drawable.collection_card_locked_bg)
        item.root.contentDescription = "${card.name}, ${item.cardState.text}"
        item.cardLock.visibility = if (card.owned) View.GONE else View.VISIBLE
        (item.cardImage.parent as View).setBackgroundResource(
            if (card.owned) R.drawable.card_image_bg else R.drawable.collection_lock_placeholder)
        card.imageUrl?.let { url ->
            RemoteImages.cached(url, sample = 4)?.let(item.cardImage::setImageBitmap)
                ?: lifecycleScope.launch { RemoteImages.load(url, sample = 4)?.let(item.cardImage::setImageBitmap) }
        }
        item.root.setOnClickListener {
            startActivity(Intent(this, CardDetailActivity::class.java)
                .putExtra(CardDetailActivity.EXTRA_CARD_NAME, card.name)
                .putExtra(CardDetailActivity.EXTRA_PLACE_ID, card.placeId)
                .putExtra(CardDetailActivity.EXTRA_IMAGE_URL, card.imageUrl)
                .putExtra(CardDetailActivity.EXTRA_OWNED, card.owned)
                .putExtra(CardDetailActivity.EXTRA_COLLECTION_NAME, detail.name))
        }
        return item
    }

    companion object {
        const val EXTRA_COLLECTION_ID = "collection_id"
        const val EXTRA_COLLECTION_NAME = "collection_name"
        const val EXTRA_COLLECTION_DESCRIPTION = "collection_description"
    }
}
