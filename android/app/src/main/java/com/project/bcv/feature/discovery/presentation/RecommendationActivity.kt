package com.project.bcv.feature.discovery.presentation

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.project.bcv.core.ui.confirmReplaceJourney
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.core.di.ExploreModule
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.viewModelFactory
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.core.ui.setUpDetailScreen
import com.project.bcv.core.ui.showContent
import com.project.bcv.core.ui.showError
import com.project.bcv.databinding.ActivityRecommendationBinding
import com.project.bcv.databinding.ItemRecommendationBinding
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.discovery.domain.RecommendedPlace
import com.project.bcv.feature.discovery.domain.Recommendations
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/** "Gợi ý điểm đến": the server's top three destinations from the virtual location. */
class RecommendationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecommendationBinding
    private lateinit var model: RecommendationViewModel
    private val cards = mutableMapOf<Long, ItemRecommendationBinding>()
    private val vietnamese = Locale.forLanguageTag("vi-VN")
    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecommendationBinding.inflate(layoutInflater)
        // Like the other screens: content padded clear of the status and navigation bars.
        setUpDetailScreen(binding.root)
        model = ViewModelProvider(this, viewModelFactory {
            RecommendationViewModel(ExploreModule.repository(this), ExploreModule.addressLookup(),
                intent.getStringExtra(EXTRA_ADDRESS))
        })[RecommendationViewModel::class.java]
        model.address.observe(this) { renderLocation() }

        binding.backButton.setOnClickListener { finish() }
        binding.state.stateRetry.setOnClickListener { model.load() }
        model.recommendations.observe(this, ::render)
        model.savingPlaceId.observe(this) { renderSaving(it) }
        model.outcome.observe(this) { event -> event.take()?.let(::handle) }
        if (model.recommendations.value == null) model.load()
    }

    private fun render(state: Remote<Recommendations>) {
        if (state != Remote.Loading) Skeleton.hide(binding.skeleton.root)
        when (state) {
            Remote.Loading -> Skeleton.show(binding.skeleton.root, binding.content, binding.state.root)
            is Remote.Failed -> binding.state.showError(binding.content, state.message)
            Remote.SessionExpired -> {
                startActivity(Intent(this, AccountActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
            is Remote.Ready -> {
                val result = state.value
                if (result.places.isEmpty()) {
                    binding.state.showError(binding.content,
                        "Chưa tìm được điểm đến phù hợp từ vị trí ảo hiện tại.")
                    return
                }
                binding.state.showContent(binding.content)
                model.describe(result.location)
                renderLocation()
                binding.subtitle.text = "${result.places.size} gợi ý phù hợp từ vị trí ảo hiện tại"
                showCards(result.places)
            }
        }
    }

    /** The street address like on Explore; the region until it is known (or when Goong has none). */
    private fun renderLocation() {
        val region = (model.recommendations.value as? Remote.Ready)?.value?.regionName
        binding.regionName.text = model.address.value ?: region.orEmpty()
    }

    /**
     * Cards already on screen are kept (no image reload, no jump) and restyled for their new rank;
     * only places that are new get a card, which fades in.
     */
    private fun showCards(places: List<RecommendedPlace>) {
        val wanted = places.map { it.id }.toSet()
        cards.keys.filter { it !in wanted }.forEach { id -> cards.remove(id)?.let { binding.cards.removeView(it.root) } }
        places.forEachIndexed { index, place ->
            val existing = cards[place.id]
            val item = existing ?: createCard(place).also { cards[place.id] = it }
            val at = binding.cards.indexOfChild(item.root)
            if (at != index) {
                if (at >= 0) binding.cards.removeViewAt(at)
                binding.cards.addView(item.root, index)
            }
            styleCard(item, place, index)
            if (existing == null && shownOnce) item.root.apply { alpha = 0f; animate().alpha(1f).setDuration(220).start() }
        }
        shownOnce = true
        renderSaving(model.savingPlaceId.value)
    }

    private var shownOnce = false

    /** Rank-dependent look: the first card is the highlighted one. */
    private fun styleCard(item: ItemRecommendationBinding, place: RecommendedPlace, index: Int) {
        val best = index == 0
        val accent = STATUS_COLORS[index % STATUS_COLORS.size]
        item.card.setCardBackgroundColor(if (best) 0xFFF0F6EE.toInt() else 0xFFFFFFFF.toInt())
        item.card.strokeColor = if (best) 0xFFA9CBB0.toInt() else 0xFFDCE6DD.toInt()
        item.rank.text = "%02d".format(place.rank)
        item.rank.setTextColor(if (best) 0xFF07836D.toInt() else 0xFF85968F.toInt())
        item.status.setTextColor(if (place.isSurprise) 0xFFC9962B.toInt() else accent)
        item.selectButton.backgroundTintList = ColorStateList.valueOf(
            if (best) 0xFF07836D.toInt() else 0xFFEFF2EA.toInt())
        item.selectButton.setTextColor(if (best) 0xFFFFFFFF.toInt() else 0xFF07836D.toInt())
    }

    private fun createCard(place: RecommendedPlace): ItemRecommendationBinding {
        val item = ItemRecommendationBinding.inflate(LayoutInflater.from(this), binding.cards, false)
        run {
            item.status.text = when {
                place.isSurprise -> "THỬ ĐIỀU MỚI"
                place.isDiscovered -> "ĐÃ MỞ KHÓA"
                else -> "CHƯA MỞ KHÓA"
            }
            item.name.text = place.name
            item.distance.text = "${distanceText(place.distanceKm)} · ~${stepsText(place.requiredSteps)} bước"
            val reasons = place.reasons.ifEmpty {
                if (place.matchedPreferences.isEmpty()) emptyList() else
                    listOf("Hợp sở thích: ${place.matchedPreferences.joinToString(", ")}")
            }
            // "Vì sao gợi ý điểm này": the server's own reasons, shown in full.
            item.matched.text = if (reasons.isEmpty()) "" else "Vì sao gợi ý: " + reasons.take(3).joinToString(" · ")
            item.matched.maxLines = 3
            item.matched.contentDescription = "Vì sao gợi ý: " + reasons.joinToString(". ")
            item.dismissButton.contentDescription = "Không quan tâm ${place.name}, ẩn khỏi gợi ý 30 ngày"
            item.dismissButton.setOnClickListener { dismiss(place) }
            // Keeps its line when empty so every card has the skeleton's height.
            item.matched.visibility = if (reasons.isEmpty()) View.INVISIBLE else View.VISIBLE
            item.selectButton.contentDescription = "Chọn ${place.name} làm điểm đến"
            item.selectButton.setOnClickListener { model.select(place.id, place.name) }
            item.card.isFocusable = true
            item.card.contentDescription = "Xem thông tin ${place.name}"
            item.card.setOnClickListener {
                if (model.savingPlaceId.value == null) {
                    detailLauncher.launch(Intent(this, PlaceDetailActivity::class.java)
                        .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, place.id)
                        .putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME, place.name)
                        .putExtra(PlaceDetailActivity.EXTRA_DISCOVERED, place.isDiscovered))
                }
            }
            // Preview photo is shown clearly; the small badge marks a place not yet unlocked.
            item.lockBadge.visibility = if (place.isDiscovered) android.view.View.GONE else android.view.View.VISIBLE
            place.imageUrl?.let { url -> loadImage(item, url, place.isDiscovered) }
        }
        return item
    }

    /** F74: hide for 30 days, refill the list, and let the user undo right away. */
    private fun dismiss(place: RecommendedPlace) {
        val api = com.project.bcv.feature.engagement.EngagementApi(this)
        cards[place.id]?.dismissButton?.isEnabled = false
        lifecycleScope.launch {
            runCatching { api.dismiss(place.id) }.onSuccess {
                collapse(place.id) { model.refresh() }
                Snackbar.make(binding.root, "Đã ẩn ${place.name} khỏi gợi ý trong 30 ngày.", Snackbar.LENGTH_LONG)
                    .setAction("Hoàn tác") {
                        lifecycleScope.launch { runCatching { api.undoDismiss(place.id) }; model.refresh() }
                    }.show()
            }.onFailure {
                cards[place.id]?.dismissButton?.isEnabled = true
                Snackbar.make(binding.root, it.message ?: "Không cập nhật được.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /** Fades the card out and folds its height away, so the cards below slide up instead of jumping. */
    private fun collapse(placeId: Long, then: () -> Unit) {
        val item = cards.remove(placeId) ?: return then()
        val view = item.root
        val height = view.height
        view.animate().alpha(0f).setDuration(160).withEndAction {
            android.animation.ValueAnimator.ofInt(height, 0).apply {
                duration = 220
                addUpdateListener { view.layoutParams = view.layoutParams.apply { this.height = it.animatedValue as Int }; view.requestLayout() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) { binding.cards.removeView(view); then() }
                })
                start()
            }
        }.start()
    }

    private fun loadImage(item: ItemRecommendationBinding, url: String, discovered: Boolean) {
        lifecycleScope.launch {
            val bitmap = RemoteImages.load(url, sample = 4) ?: return@launch
            item.image.setImageBitmap(bitmap)
        }
    }


    private fun renderSaving(placeId: Long?) {
        cards.forEach { (id, item) ->
            item.selectButton.isEnabled = placeId == null
            item.selectButton.text = if (id == placeId) "Đang lưu…" else "Chọn điểm đến"
        }
    }

    private fun handle(outcome: SelectOutcome) {
        when (outcome) {
            SelectOutcome.Saved -> {
                setResult(Activity.RESULT_OK)
                finish()
            }
            is SelectOutcome.ConfirmReplace -> confirmReplaceJourney(onReplace = {
                model.select(outcome.placeId, outcome.placeName, replaceActive = true)
            })
            is SelectOutcome.Failed -> Snackbar.make(binding.root, outcome.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun distanceText(km: Double): String =
        NumberFormat.getNumberInstance(vietnamese).apply { maximumFractionDigits = 1 }.format(km) + " km"

    private fun stepsText(steps: Int): String =
        NumberFormat.getIntegerInstance(vietnamese).format(steps)

    companion object {
        /** The virtual location's address Explore already looked up, shown at once. */
        const val EXTRA_ADDRESS = "address"

        /** Status label colours of the three cards, as in the design. */
        val STATUS_COLORS = intArrayOf(0xFFA0712B.toInt(), 0xFF3C7DB0.toInt(), 0xFF07836D.toInt())
    }
}
