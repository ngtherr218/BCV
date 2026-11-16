package com.project.bcv.feature.discovery.presentation

import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.core.network.MediaUrlPolicy
import com.project.bcv.BuildConfig
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.R
import com.project.bcv.databinding.ActivityPlaceDetailBinding
import com.project.bcv.feature.discovery.domain.ExplorePlaceDetail
import kotlinx.coroutines.launch
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
    /** Place whose journal page exists (it has a real check-in), and the place last asked about. */
    private var journalOpen: Long? = null
    private var journalChecked: Long? = null
    override fun onResume() {
        super.onResume()
    }

    private val locationSource by lazy { com.project.bcv.feature.location.startingpoint.data.AndroidDeviceLocationSource(this) }
    private val checkinPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) checkIn()
        else Snackbar.make(binding.root, "Cần quyền vị trí để xác nhận bạn đang ở địa danh.", Snackbar.LENGTH_LONG).show()
    }

    /** F77: being physically at the place (≤ 300 m, checked by the server) unlocks it with a real-visit stamp. */
    private fun checkIn() {
        val placeId = intent.getLongExtra(EXTRA_PLACE_ID, 0L)
        val fine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            checkinPermission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        binding.checkinButton.isEnabled = false
        binding.checkinButton.text = "Đang định vị…"
        locationSource.findCurrent { coordinates ->
            if (coordinates == null) {
                binding.checkinButton.isEnabled = true
                binding.checkinButton.text = "Check-in"
                Snackbar.make(binding.root, "Chưa lấy được vị trí. Hãy bật GPS và thử lại ngoài trời.", Snackbar.LENGTH_LONG).show()
                return@findCurrent
            }
            lifecycleScope.launch {
                val message = runCatching {
                    com.project.bcv.feature.engagement.EngagementApi(this@PlaceDetailActivity)
                        .checkIn(placeId, coordinates.latitude, coordinates.longitude)
                }.fold({ it }, { it.message ?: "Không check-in được." })
                binding.checkinButton.isEnabled = true
                binding.checkinButton.text = "Check-in"
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                journalChecked = null
                model.load(placeId)
            }
        }
    }

    /** Debug only: the server accepts it just on test servers, and stands the user at the place. */
    private fun testCheckIn() {
        val placeId = intent.getLongExtra(EXTRA_PLACE_ID, 0L)
        binding.checkinButton.isEnabled = false
        lifecycleScope.launch {
            val message = runCatching {
                com.project.bcv.feature.engagement.EngagementApi(this@PlaceDetailActivity).checkIn(placeId, 0.0, 0.0, test = true)
            }.fold({ "[Thử] $it" }, { it.message ?: "Không check-in thử được." })
            binding.checkinButton.isEnabled = true
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            journalChecked = null
            model.load(placeId)
        }
    }

    override fun onDestroy() {
        locationSource.cancel()
        super.onDestroy()
    }

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
        binding.communityMediaButton.visibility = View.GONE
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.communityMediaButton.setOnClickListener {
            startActivity(android.content.Intent(this, com.project.bcv.feature.engagement.JournalEntryActivity::class.java)
                .putExtra(com.project.bcv.feature.engagement.JournalEntryActivity.EXTRA_PLACE_ID, intent.getLongExtra(EXTRA_PLACE_ID, 0L))
                .putExtra(com.project.bcv.feature.engagement.JournalEntryActivity.EXTRA_PLACE_NAME,
                    model.state.value?.place?.name ?: intent.getStringExtra(EXTRA_PLACE_NAME)))
        }
        binding.checkinButton.setOnClickListener { checkIn() }
        // Test builds: a small "🧪" button checks in as if standing at the place (no GPS needed).
        if (BuildConfig.STEP_TEST_ENABLED) binding.bottomBar.addView(
            com.google.android.material.button.MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "🧪"; textSize = 17f; minWidth = 0; minimumWidth = 0
                setPadding(0, 0, 0, 0); cornerRadius = (16 * resources.displayMetrics.density).toInt()
                contentDescription = "Check-in thử (chỉ bản thử nghiệm)"
                androidx.appcompat.widget.TooltipCompat.setTooltipText(this, "Check-in thử")
                setOnClickListener { testCheckIn() }
            }, 0, android.widget.LinearLayout.LayoutParams((54 * resources.displayMetrics.density).toInt(),
                (54 * resources.displayMetrics.density).toInt()).apply { marginEnd = (10 * resources.displayMetrics.density).toInt() })
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
                place == null -> {
                    showLoading(state.error, state.unavailable)
                    if (state.unavailable) setResult(RESULT_PLACE_UNAVAILABLE)
                }
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
        binding.topTitle.text = if (discoveredHint) "Địa danh đã khám phá" else "Xem trước địa danh"
        binding.retryButton.setOnClickListener {
            if (model.state.value?.unavailable == true) finish() else model.load(placeId)
        }
        if (placeId <= 0) finish()
        else if (model.state.value?.place == null && model.state.value?.unavailable != true) model.load(placeId)
    }

    /** Fills the placeholder of the page that will open: locked destination or discovered content. */
    private fun showSkeletonFor(name: String, discovered: Boolean) = with(binding) {
        skeletonLocked.skeletonName.text = name
        skeletonUnlocked.skeletonName.text = name
        skeletonLocked.root.visibility = if (discovered) View.GONE else View.VISIBLE
        skeletonUnlocked.root.visibility = if (discovered) View.VISIBLE else View.GONE
        skeletonDiscovered = discovered
    }

    private fun showLoading(error: String?, unavailable: Boolean = false) = with(binding) {
        loadingContent.visibility = View.VISIBLE
        placeContent.visibility = View.GONE
        if (error != null) {
            Skeleton.hide(loadingSkeleton)
            bottomBar.visibility = View.GONE
            loadingError.visibility = View.VISIBLE
            loadingContent.contentDescription = if (unavailable) "Địa danh không còn khả dụng" else "Không tải được nội dung địa danh"
            loadingStatus.setTextColor(ContextCompat.getColor(this@PlaceDetailActivity,
                if (unavailable) R.color.forest else R.color.error_red))
            loadingStatus.text = if (unavailable) error else "$error\nKiểm tra kết nối rồi nhấn Thử lại."
            retryButton.text = if (unavailable) "Quay lại" else "Thử lại"
        } else {
            loadingContent.contentDescription = "Đang tải nội dung địa danh"
            Skeleton.show(loadingSkeleton, loadingError)
            bottomBar.visibility = View.GONE
        }
    }

    /** Keeps the skeleton up until the first photo is decoded, then swaps to the real page once. */
    private fun prepare(place: ExplorePlaceDetail) {
        if (preparingPlaceId == place.id) return
        preparingPlaceId = place.id
        showSkeletonFor(place.name, place.isDiscovered)
        showLoading(null)
        val firstUrl = photosOf(place).firstOrNull()
        lifecycleScope.launch {
            firstUrl?.let { loadBitmap(it, sample = 2) }
            renderedPlaceId = place.id
            Skeleton.hide(binding.loadingSkeleton)
            binding.loadingContent.visibility = View.GONE
            model.state.value?.place?.let(::showPlace)
            ScreenMotion.reveal(binding.placeContent)
        }
    }

    /** Photos the user may see: the full gallery once discovered, the cover photo as a preview before. */
    private fun photosOf(place: ExplorePlaceDetail) =
        (if (place.isDiscovered) place.imageUrls.ifEmpty { listOfNotNull(place.imageUrl) } else listOfNotNull(place.imageUrl))
            .filter { RemoteImages.supports(it) || MediaUrlPolicy.authenticatedUrl(BuildConfig.API_BASE_URL, it) != null }

    /**
     * One page for both states. Locked = preview (cover, story, fun fact) plus what the walk unlocks;
     * discovered = the same page with the gallery, videos and journal.
     */
    private fun showPlace(place: ExplorePlaceDetail) = with(binding) {
        val discovered = place.isDiscovered
        val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
        placeContent.visibility = View.VISIBLE
        bottomBar.visibility = View.VISIBLE
        topTitle.text = if (discovered) "Địa danh đã khám phá" else "Xem trước địa danh"

        // Hero: photo, state tag, name and region.
        placeName.text = place.name
        regionText.text = place.regionName
        regionText.visibility = if (place.regionName.isNullOrBlank()) View.GONE else View.VISIBLE
        statusIcon.setImageResource(if (discovered) R.drawable.ic_lock_open_outline else R.drawable.ic_lock)
        statusIcon.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this@PlaceDetailActivity, if (discovered) R.color.teal else R.color.forest))
        statusText.text = if (discovered) "Đã khám phá" else "Xem trước · chưa mở khóa"
        statusText.setTextColor(ContextCompat.getColor(this@PlaceDetailActivity, if (discovered) R.color.teal else R.color.forest))

        // At a glance.
        ratingValue.text = place.rating?.takeIf { (place.ratingCount ?: 0) > 0 }?.let { "★ " + String.format(Locale.forLanguageTag("vi-VN"), "%.1f", it) } ?: "★ —"
        ratingLabel.text = place.ratingCount?.takeIf { it > 0 }?.let { "${number.format(it)} đánh giá" } ?: "Chưa có đánh giá"
        distanceText.text = if (place.distanceKm < 1) "${(place.distanceKm * 1000).toInt()} m"
            else String.format(Locale.forLanguageTag("vi-VN"), "%.1f km", place.distanceKm)
        stepsText.text = if (place.distanceKm <= 0.001) "Vị trí ảo ở đây" else "Từ vị trí ảo · ~${number.format(place.requiredSteps)} bước"
        placeTypeText.text = place.placeType.ifBlank { "—" }

        // Locked: what the walk unlocks.
        unlockCard.visibility = if (discovered) View.GONE else View.VISIBLE
        unlockHint.text = if (place.distanceKm <= 0.001) "Bạn đang ở đây — check-in để mở khóa ngay."
            else "Khoảng ${number.format(place.requiredSteps)} bước từ vị trí của bạn, hoặc check-in khi đến nơi."

        storyText.text = place.introduction ?: "Nội dung về địa danh này đang được cập nhật."
        val fact = place.highlights?.removePrefix("Bạn có biết?")?.trim()
            // Seed data sometimes repeats the introduction; only show a fact that adds something.
            ?.takeUnless { f -> place.introduction?.let { intro -> intro.trim().contains(f.trimEnd('.')) || f.contains(intro.trim().trimEnd('.')) } == true }
        factCard.visibility = if (fact.isNullOrEmpty()) View.GONE else View.VISIBLE
        factText.text = fact

        officialVideos.removeAllViews()
        officialVideos.visibility = if (discovered && place.videos.isNotEmpty()) View.VISIBLE else View.GONE
        if (discovered) place.videos.forEachIndexed { index, video ->
            officialVideos.addView(com.google.android.material.button.MaterialButton(this@PlaceDetailActivity, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "▶  Video ${index + 1} · ${video.durationSeconds.toInt()} giây"
                isAllCaps = false; cornerRadius = (14 * resources.displayMetrics.density).toInt()
                setTextColor(ContextCompat.getColor(context, R.color.teal))
                setOnClickListener { startActivity(android.content.Intent(this@PlaceDetailActivity, PlaceVideoActivity::class.java)
                    .putExtra("place_id", place.id).putExtra("video_id", video.id)) }
            })
        }

        // Bottom bar: check-in always; journal once discovered, otherwise pick as destination.
        // The journal is for real visits only: its button appears once the user has checked in here.
        communityMediaButton.visibility = if (journalOpen == place.id) View.VISIBLE else View.GONE
        if (journalChecked != place.id) {
            journalChecked = place.id
            lifecycleScope.launch {
                val open = runCatching { com.project.bcv.feature.engagement.EngagementApi(this@PlaceDetailActivity).journalEntry(place.id) }.isSuccess
                journalOpen = if (open) place.id else null
                communityMediaButton.visibility = if (open) View.VISIBLE else View.GONE
            }
        }
        selectButton.visibility = if (discovered) View.GONE else View.VISIBLE

        val urls = photosOf(place)
        heroPlaceholder.visibility = if (urls.isEmpty()) View.VISIBLE else View.GONE
        if (urls != galleryUrls) {
            galleryUrls = urls
            buildThumbnails()
            selectImage(0)
        }
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
                val inset = (2 * density).toInt()
                setPadding(inset, inset, inset, inset)
                contentDescription = "Ảnh ${index + 1} trên ${galleryUrls.size}"
                setOnClickListener { selectImage(index) }
            }
            galleryThumbs.addView(thumb, LinearLayout.LayoutParams((76 * density).toInt(), (58 * density).toInt())
                .apply { if (index > 0) marginStart = (8 * density).toInt() })
            lifecycleScope.launch { loadBitmap(url, sample = 4)?.let(thumb::setImageBitmap) }
        }
    }

    /** Shows photo [index] in the hero; thumbnails mark the one on screen. */
    private fun selectImage(index: Int) = with(binding) {
        galleryIndex = index
        val url = galleryUrls.getOrNull(index)
        galleryCounter.text = String.format(Locale.ROOT, "%d / %d", index + 1, galleryUrls.size)
        galleryCounter.visibility = if (galleryUrls.size > 1) View.VISIBLE else View.GONE
        placeImage.contentDescription = "Ảnh ${placeName.text}"
        val density = resources.displayMetrics.density
        for (position in 0 until galleryThumbs.childCount) {
            galleryThumbs.getChildAt(position).background = GradientDrawable().apply {
                cornerRadius = 14 * density
                setColor(0xFFFFFFFF.toInt())
                if (position == index) setStroke((2.5f * density).toInt(), ContextCompat.getColor(
                    this@PlaceDetailActivity, R.color.teal))
                else setStroke((1 * density).toInt(), 0xFFE2E8DF.toInt())
            }
        }
        if (url == null) {
            placeImage.setImageDrawable(null)
            return@with
        }
        // Cached photos (the first is preloaded before the page shows) are set without a frame of gap.
        bitmaps["2|$url"]?.let { placeImage.setImageBitmap(it); return@with }
        lifecycleScope.launch {
            val bitmap = loadBitmap(url, sample = 2)
            if (galleryIndex == index && bitmap != null) placeImage.setImageBitmap(bitmap)
        }
    }

    /** Downloads and decodes once per URL and sample size; later calls reuse the bitmap. */
    private suspend fun loadBitmap(url: String, sample: Int): Bitmap? {
        val key = "$sample|$url"
        bitmaps[key]?.let { return it }
        val protectedUrl = MediaUrlPolicy.authenticatedUrl(BuildConfig.API_BASE_URL, url)
        val bitmap = if (protectedUrl != null) SessionPreferences(this).token?.let {
            RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, protectedUrl, it, sample)
        } else RemoteImages.load(url, sample)
        if (bitmap != null) bitmaps[key] = bitmap
        return bitmap
    }

    companion object {
        const val RESULT_PLACE_UNAVAILABLE = Activity.RESULT_FIRST_USER + 1
        const val EXTRA_PLACE_ID = "place_id"
        const val EXTRA_PLACE_NAME = "place_name"
        const val EXTRA_DISCOVERED = "place_discovered"
    }
}
