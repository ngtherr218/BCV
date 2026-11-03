package com.project.bcv.feature.discovery.presentation

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.project.bcv.core.ui.confirmReplaceJourney
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.core.di.ExploreModule
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.R
import com.project.bcv.databinding.ActivityPlaceDetailBinding
import com.project.bcv.feature.discovery.domain.ExplorePlaceDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

class PlaceDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaceDetailBinding
    private lateinit var model: PlaceDetailViewModel
    private var shownImageUrl: String? = null
    private var replaceDialogShown = false
    private var shownError: String? = null
    private var renderedPlaceId: Long? = null
    private val bitmaps = mutableMapOf<String, Bitmap>()
    private var galleryUrls = emptyList<String>()
    private var galleryIndex = 0
    private var preparingPlaceId: Long? = null
    /** Which placeholder is up: the map's hint first, then what the server says. */
    private var skeletonDiscovered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.selectButton.setOnClickListener { model.select() }
        model = ViewModelProvider(this, PlaceDetailViewModelFactory(
            ExploreModule.repository(this)))[PlaceDetailViewModel::class.java]
        model.state.observe(this) { state ->
            if (state.saved) {
                setResult(Activity.RESULT_OK)
                finish()
                return@observe
            }
            binding.selectButton.isEnabled = state.place?.distanceKm?.let { it > 0.001 } == true && !state.saving
            binding.selectButton.text = when {
                state.saving -> "Đang lưu điểm đến..."
                state.place?.distanceKm?.let { it <= 0.001 } == true -> "Bạn đang ở địa danh này"
                else -> "Chọn làm điểm đến"
            }
            val place = state.place
            when {
                place == null -> showLoading(state.error)
                renderedPlaceId == place.id -> showPlace(place)
                else -> prepare(place)
            }
            // A failed first load is shown in the loading block; later errors (e.g. saving) use a snackbar.
            if (place != null && state.error != null && state.error != shownError) {
                shownError = state.error
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_LONG).show()
            }
            if (state.confirmReplace && !replaceDialogShown) {
                replaceDialogShown = true
                confirmReplaceJourney(
                    onReplace = { replaceDialogShown = false; model.select(replaceActive = true) },
                    onKeep = { replaceDialogShown = false; model.dismissReplace() })
            }
        }
        val placeId = intent.getLongExtra(EXTRA_PLACE_ID, -1L)
        // What the map already knows is shown at once, so the header is right before the API answers.
        val discoveredHint = intent.getBooleanExtra(EXTRA_DISCOVERED, false)
        showSkeletonFor(intent.getStringExtra(EXTRA_PLACE_NAME).orEmpty(), discoveredHint)
        binding.topTitle.text = if (discoveredHint) "Nội dung địa danh" else "Chọn điểm đến"
        binding.retryButton.setOnClickListener { model.load(placeId) }
        if (placeId <= 0) finish() else if (model.state.value?.place == null) model.load(placeId)
    }

    /** Fills the placeholder of the page that will open: locked destination or discovered content. */
    private fun showSkeletonFor(name: String, discovered: Boolean) = with(binding) {
        skeletonLocked.skeletonName.text = name
        skeletonUnlocked.skeletonName.text = name
        skeletonLocked.root.visibility = if (discovered) View.GONE else View.VISIBLE
        skeletonUnlocked.root.visibility = if (discovered) View.VISIBLE else View.GONE
        skeletonDiscovered = discovered
    }

    private fun showLoading(error: String?) = with(binding) {
        loadingContent.visibility = View.VISIBLE
        lockedContent.visibility = View.GONE
        unlockedContent.visibility = View.GONE
        if (error != null) {
            Skeleton.hide(loadingSkeleton)
            bottomBar.visibility = View.GONE
            loadingError.visibility = View.VISIBLE
            loadingStatus.text = "$error\nKiểm tra kết nối rồi nhấn Thử lại."
        } else {
            Skeleton.show(loadingSkeleton, loadingError)
            // The destination picker keeps its (still disabled) button in place while loading.
            bottomBar.visibility = if (skeletonDiscovered) View.GONE else View.VISIBLE
        }
    }

    /** Keeps the skeleton up until the first photo is decoded, then swaps to the real page once. */
    private fun prepare(place: ExplorePlaceDetail) {
        if (preparingPlaceId == place.id) return
        preparingPlaceId = place.id
        showSkeletonFor(place.name, place.isDiscovered)
        showLoading(null)
        val firstUrl = (if (place.isDiscovered) place.imageUrls.firstOrNull() else null) ?: place.imageUrl
        lifecycleScope.launch {
            firstUrl?.takeIf { it.startsWith("https://") }?.let { loadBitmap(it, sample = 2) }
            renderedPlaceId = place.id
            Skeleton.hide(binding.loadingSkeleton)
            binding.loadingContent.visibility = View.GONE
            model.state.value?.place?.let(::showPlace)
            ScreenMotion.reveal(if (place.isDiscovered) binding.unlockedContent else binding.lockedContent)
        }
    }

    private fun showPlace(place: ExplorePlaceDetail) = with(binding) {
        // A discovered place opens its content page; a locked one stays the destination picker.
        lockedContent.visibility = if (place.isDiscovered) View.GONE else View.VISIBLE
        unlockedContent.visibility = if (place.isDiscovered) View.VISIBLE else View.GONE
        bottomBar.visibility = lockedContent.visibility
        topTitle.text = if (place.isDiscovered) "Nội dung địa danh" else "Chọn điểm đến"
        if (place.isDiscovered) {
            showUnlocked(place)
            return@with
        }
        placeName.text = place.name
        distanceText.text = if (place.distanceKm < 1) {
            "${(place.distanceKm * 1000).toInt()} m"
        } else String.format(Locale.forLanguageTag("vi-VN"), "%.1f km", place.distanceKm)
        val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
        stepsText.text = if (place.distanceKm <= 0.001) "Trùng với vị trí ảo hiện tại"
            else "Khoảng ${number.format(place.requiredSteps)} bước từ vị trí ảo"
        lockedHeroContent.visibility = if (place.isDiscovered) View.GONE else View.VISIBLE
        heroShade.visibility = if (place.isDiscovered) View.GONE else View.VISIBLE
        lockedNotice.visibility = if (place.isDiscovered) View.GONE else View.VISIBLE
        unlockedDescription.visibility = View.VISIBLE
        unlockedDescription.text = listOfNotNull(
            ratingText(place),
            place.regionName?.takeIf(String::isNotBlank)?.let { "Khu vực: $it" },
            place.placeType.takeIf(String::isNotBlank)?.let { "Loại địa danh: $it" },
            "Giới thiệu\n${place.introduction ?: "Chưa có dữ liệu giới thiệu."}")
            .filter(String::isNotBlank).joinToString("\n\n")
        if (Build.VERSION.SDK_INT >= 31) {
            placeImage.setRenderEffect(if (place.isDiscovered) null else
                RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.CLAMP))
        }
        if (place.imageUrl != shownImageUrl) {
            shownImageUrl = place.imageUrl
            placeImage.setImageDrawable(null)
            place.imageUrl?.let(::loadImage)
        }
    }

    private fun showUnlocked(place: ExplorePlaceDetail) = with(binding) {
        unlockedName.text = place.name
        regionText.text = place.regionName
        regionText.visibility = if (place.regionName.isNullOrBlank()) View.GONE else View.VISIBLE
        storyText.text = "${ratingText(place)}\n\n${place.introduction ?: "Nội dung về địa danh này đang được cập nhật."}"
        val fact = place.highlights?.removePrefix("Bạn có biết?")?.trim()
        factCard.visibility = if (fact.isNullOrEmpty()) View.GONE else View.VISIBLE
        factText.text = fact
        placeTypeText.text = place.placeType

        val urls = place.imageUrls.ifEmpty { listOfNotNull(place.imageUrl) }.filter { it.startsWith("https://") }
        // Without a PLACE_IMAGE row there is no photo of this place; never stand in the app's hero art.
        galleryFrame.visibility = if (urls.isEmpty()) View.GONE else View.VISIBLE
        galleryCaptionRow.visibility = galleryFrame.visibility
        storyTitle.updateLayoutParams<LinearLayout.LayoutParams> {
            topMargin = ((if (urls.isEmpty()) 18 else 22) * resources.displayMetrics.density).toInt()
        }
        if (urls != galleryUrls) {
            galleryUrls = urls
            buildThumbnails()
            selectImage(0)
        }
    }

    private fun ratingText(place: ExplorePlaceDetail): String {
        val score = place.rating?.let { String.format(Locale.forLanguageTag("vi-VN"), "%.1f/5", it) } ?: "Chưa có dữ liệu"
        val count = place.ratingCount?.let { NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(it) } ?: "Chưa có dữ liệu"
        return "Điểm đánh giá: $score\nSố lượt đánh giá: $count"
    }

    private fun buildThumbnails() = with(binding) {
        galleryThumbs.removeAllViews()
        galleryThumbRow.visibility = if (galleryUrls.size > 1) View.VISIBLE else View.GONE
        if (galleryUrls.size <= 1) return@with
        val density = resources.displayMetrics.density
        galleryUrls.forEachIndexed { index, url ->
            val thumb = ImageView(this@PlaceDetailActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                val inset = (3 * density).toInt()
                setPadding(inset, inset, inset, inset)
                contentDescription = "Ảnh ${index + 1} trên ${galleryUrls.size}"
                setOnClickListener { selectImage(index) }
            }
            galleryThumbs.addView(thumb, LinearLayout.LayoutParams((72 * density).toInt(), (56 * density).toInt())
                .apply { if (index > 0) marginStart = (8 * density).toInt() })
            lifecycleScope.launch { loadBitmap(url, sample = 4)?.let(thumb::setImageBitmap) }
        }
    }

    private fun selectImage(index: Int) = with(binding) {
        galleryIndex = index
        val url = galleryUrls.getOrNull(index)
        // PLACE_IMAGE has no per-photo caption, so the place name labels every photo.
        galleryCaption.text = unlockedName.text
        galleryCounter.text = if (galleryUrls.size > 1)
            String.format(Locale.ROOT, "%02d / %02d", index + 1, galleryUrls.size) else ""
        galleryImage.contentDescription = "Ảnh ${unlockedName.text}"
        val density = resources.displayMetrics.density
        for (position in 0 until galleryThumbs.childCount) {
            galleryThumbs.getChildAt(position).background = GradientDrawable().apply {
                cornerRadius = 14 * density
                setColor(0xFFFFFFFF.toInt())
                if (position == index) setStroke((2 * density).toInt(), ContextCompat.getColor(
                    this@PlaceDetailActivity, R.color.teal))
                else setStroke((1.5f * density).toInt(), 0xFFE2E8DF.toInt())
            }
        }
        if (url == null) {
            galleryImage.setImageDrawable(null)
            return@with
        }
        // Cached photos (the first is preloaded before the page shows) are set without a frame of gap.
        bitmaps["2|$url"]?.let { galleryImage.setImageBitmap(it); return@with }
        galleryImage.setImageDrawable(null)
        lifecycleScope.launch {
            val bitmap = loadBitmap(url, sample = 2)
            if (galleryIndex == index && bitmap != null) galleryImage.setImageBitmap(bitmap)
        }
    }

    /** Downloads and decodes once per URL and sample size; later calls reuse the bitmap. */
    private suspend fun loadBitmap(url: String, sample: Int): Bitmap? {
        val key = "$sample|$url"
        bitmaps[key]?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("User-Agent", "BCV Android App (educational demo)")
                try {
                    connection.inputStream.use {
                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                    }
                } finally { connection.disconnect() }
            } catch (_: Exception) { null }
        }
        if (bitmap != null) bitmaps[key] = bitmap
        return bitmap
    }

    private fun loadImage(url: String) {
        if (!url.startsWith("https://")) return
        bitmaps["2|$url"]?.let { showHeroBitmap(it); return }
        lifecycleScope.launch {
            val bitmap = loadBitmap(url, sample = 2)
            if (shownImageUrl == url && bitmap != null) showHeroBitmap(bitmap)
        }
    }

    private fun showHeroBitmap(bitmap: Bitmap) {
        val displayed = if (Build.VERSION.SDK_INT < 31 && model.state.value?.place?.isDiscovered == false)
            blurFallback(bitmap) else bitmap
        binding.placeImage.setImageBitmap(displayed)
    }

    private fun blurFallback(source: Bitmap): Bitmap {
        val small = Bitmap.createScaledBitmap(source,
            max(1, source.width / 16), max(1, source.height / 16), true)
        return Bitmap.createScaledBitmap(small, source.width, source.height, true)
    }

    companion object {
        const val EXTRA_PLACE_ID = "place_id"
        const val EXTRA_PLACE_NAME = "place_name"
        const val EXTRA_DISCOVERED = "place_discovered"
    }
}
