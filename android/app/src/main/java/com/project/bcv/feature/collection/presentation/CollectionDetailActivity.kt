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
    private var inboxOnly = false
    private var cardFilter = "Tất cả"

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
        inboxOnly = intent.getBooleanExtra("new_cards",false)
        cardFilter = savedInstanceState?.getString("cardFilter") ?: "Tất cả"
        if (collectionId < 0 || (collectionId == 0L && !intent.getBooleanExtra("all_cards",false))) { finish(); return }
        // Known from the list, so the heading shows before the API answers.
        binding.collectionName.text = intent.getStringExtra(EXTRA_COLLECTION_NAME).orEmpty()
        binding.collectionDescription.text = intent.getStringExtra(EXTRA_COLLECTION_DESCRIPTION).orEmpty()
        binding.backButton.setOnClickListener { finish() }
        binding.retryButton.setOnClickListener { model.load(collectionId) }
        if(!inboxOnly) {
            val filters = com.google.android.material.chip.ChipGroup(this).apply { isSingleSelection=true; isSelectionRequired=true }
            for(name in listOf("Tất cả","Đã sở hữu","Chưa sở hữu")) {
                filters.addView(com.google.android.material.chip.Chip(this).apply {
                    text=name;isCheckable=true;isChecked=cardFilter==name
                    chipBackgroundColor=getColorStateList(R.color.destination_chip_background)
                    setTextColor(getColorStateList(R.color.destination_chip_text))
                    setOnCheckedChangeListener { _,checked -> if(checked) {cardFilter=name;shown?.let { show(it,force=true) }} }
                })
            }
            val parent=binding.cardGrid.parent as LinearLayout
            parent.addView(filters,parent.indexOfChild(binding.cardGrid))
        }

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

    private fun show(detail: CollectionDetail, force: Boolean = false) = with(binding) {
        if (detail == shown && !force) return@with
        val first = shown == null
        shown = detail
        messageState.visibility = View.GONE
        content.visibility = View.VISIBLE
        collectionName.text = if(inboxOnly) "Thẻ mới" else detail.name
        collectionDescription.text = if(inboxOnly) "Những thẻ vừa nhận, được giữ riêng để bạn xem khi thuận tiện." else detail.description.orEmpty()
        collectionDescription.visibility = if (!inboxOnly && detail.description.isNullOrBlank()) View.GONE else View.VISIBLE
        summaryCard.visibility=if(inboxOnly) View.GONE else View.VISIBLE
        cardsSectionTitle.text=if(inboxOnly) "Thẻ chưa xem" else "Thẻ trong album"

        ownedText.text = "${detail.ownedCards} / ${detail.totalCards} thẻ" +
            detail.cards.count { it.isNew }.let { if (it > 0) " · $it thẻ mới" else "" }
        val percent = if (detail.totalCards == 0) 0.0 else detail.ownedCards * 100.0 / detail.totalCards
        percentText.text = String.format(Locale.forLanguageTag("vi-VN"), "%.1f", percent).removeSuffix(",0") + "%"
        ownedProgress.max = detail.totalCards.coerceAtLeast(1)
        ownedProgress.setProgressCompat(detail.ownedCards, !first)
        summaryCard.contentDescription = "Đã sở hữu ${detail.ownedCards} trên ${detail.totalCards} thẻ, ${percentText.text}"

        cardGrid.removeAllViews()
        val density = resources.displayMetrics.density
        val visibleCards=detail.cards.filter { if(inboxOnly) it.isNew else when(cardFilter) { "Đã sở hữu" -> it.owned;"Chưa sở hữu" -> !it.owned;else -> true } }
        if(visibleCards.isEmpty()) cardGrid.addView(android.widget.TextView(this@CollectionDetailActivity).apply {
            text=if(inboxOnly) "Bạn đã xem hết thẻ mới. Thẻ nhận từ hành trình tiếp theo sẽ xuất hiện tại đây." else "Chưa có thẻ trong mục này."
            textSize=15f;setTextColor(getColor(R.color.muted));setPadding(0,24,0,24)
        })
        visibleCards.sortedByDescending { it.isNew }.chunked(2).forEachIndexed { rowIndex, pair ->
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
        item.newCardBadge.visibility = if (card.isNew) View.VISIBLE else View.GONE
        item.cardName.text = card.name
        item.cardName.setTextColor(ContextCompat.getColor(this, R.color.forest))
        item.cardState.text = if (card.isNew) "MỚI · Chưa xem" else if (card.owned) "Đã sở hữu" else "Chưa sở hữu"
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
        item.root.setBackgroundResource(if (card.isNew) R.drawable.collection_card_new_bg else R.drawable.journey_card_bg)
        item.root.contentDescription = "${card.name}, ${item.cardState.text}"
        item.cardLock.visibility = View.GONE
        (item.cardImage.parent as View).setBackgroundResource(
            R.drawable.card_image_bg)
        card.imageUrl?.let { url ->
            RemoteImages.cached(url, sample = 4)?.let(item.cardImage::setImageBitmap)
                ?: lifecycleScope.launch { RemoteImages.load(url, sample = 4)?.let(item.cardImage::setImageBitmap) }
        }
        item.root.setOnClickListener {
            startActivity(Intent(this, CardDetailActivity::class.java)
                .putExtra(CardDetailActivity.EXTRA_CARD_ID, card.id)
                .putExtra(CardDetailActivity.EXTRA_IS_NEW, card.isNew)
                .putExtra(CardDetailActivity.EXTRA_CARD_NAME, card.name)
                .putExtra(CardDetailActivity.EXTRA_PLACE_ID, card.placeId)
                .putExtra(CardDetailActivity.EXTRA_IMAGE_URL, card.imageUrl)
                .putExtra(CardDetailActivity.EXTRA_OWNED, card.owned)
                .putExtra(CardDetailActivity.EXTRA_COLLECTION_NAME, detail.name))
        }
        return item
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("cardFilter",cardFilter);super.onSaveInstanceState(outState)
    }

    companion object {
        const val EXTRA_COLLECTION_ID = "collection_id"
        const val EXTRA_COLLECTION_NAME = "collection_name"
        const val EXTRA_COLLECTION_DESCRIPTION = "collection_description"
    }
}
