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

    private fun showCards(places: List<RecommendedPlace>) {
        binding.cards.removeAllViews()
        cards.clear()
        places.forEachIndexed { index, place ->
            val item = ItemRecommendationBinding.inflate(LayoutInflater.from(this), binding.cards, true)
            cards[place.id] = item
            val best = index == 0
            val accent = STATUS_COLORS[index % STATUS_COLORS.size]
            item.card.setCardBackgroundColor(if (best) 0xFFF0F6EE.toInt() else 0xFFFFFFFF.toInt())
            item.card.strokeColor = if (best) 0xFFA9CBB0.toInt() else 0xFFDCE6DD.toInt()
            item.rank.text = "%02d".format(place.rank)
            item.rank.setTextColor(if (best) 0xFF07836D.toInt() else 0xFF85968F.toInt())
            item.status.text = if (place.isDiscovered) "ĐÃ MỞ KHÓA" else "CHƯA MỞ KHÓA"
            item.status.setTextColor(accent)
            item.name.text = place.name
            item.distance.text = "${distanceText(place.distanceKm)} · ~${stepsText(place.requiredSteps)} bước"
            val reasons = place.reasons.ifEmpty {
                if (place.matchedPreferences.isEmpty()) emptyList() else
                    listOf("Hợp sở thích: ${place.matchedPreferences.joinToString(", ")}")
            }
            item.matched.text = reasons.take(2).joinToString(" · ")
            item.matched.contentDescription = reasons.joinToString(". ")
            // Keeps its line when empty so every card has the skeleton's height.
            item.matched.visibility = if (reasons.isEmpty()) View.INVISIBLE else View.VISIBLE
            item.selectButton.backgroundTintList = ColorStateList.valueOf(
                if (best) 0xFF07836D.toInt() else 0xFFEFF2EA.toInt())
            item.selectButton.setTextColor(if (best) 0xFFFFFFFF.toInt() else 0xFF07836D.toInt())
            item.selectButton.contentDescription = "Chọn ${place.name} làm điểm đến"
            item.selectButton.setOnClickListener { model.select(place.id, place.name) }
            if (Build.VERSION.SDK_INT >= 31 && !place.isDiscovered) {
                item.image.setRenderEffect(RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.CLAMP))
            }
            place.imageUrl?.let { url -> loadImage(item, url, place.isDiscovered) }
        }
        renderSaving(model.savingPlaceId.value)
    }

    private fun loadImage(item: ItemRecommendationBinding, url: String, discovered: Boolean) {
        lifecycleScope.launch {
            val bitmap = RemoteImages.load(url, sample = 4) ?: return@launch
            item.image.setImageBitmap(if (Build.VERSION.SDK_INT < 31 && !discovered) blurFallback(bitmap) else bitmap)
        }
    }

    private fun blurFallback(source: Bitmap): Bitmap {
        val small = Bitmap.createScaledBitmap(source, max(1, source.width / 16), max(1, source.height / 16), true)
        return Bitmap.createScaledBitmap(small, source.width, source.height, true)
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
