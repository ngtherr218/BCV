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
    /** Top gallery: the place's photos, then its videos (unlocked only), shown together. */
    private sealed class Media {
        data class Photo(val url: String) : Media()
        data class Clip(val video: com.project.bcv.feature.discovery.domain.PlaceVideo) : Media()
    }
    private var galleryItems = emptyList<Media>()
    private var galleryIndex = 0
    /** Play button and length over the hero while a video is selected. */
    private var heroPlay: View? = null
    private var preparingPlaceId: Long? = null
    /** Place whose journal page exists (it has a real check-in), and the place last asked about. */
    private var journalOpen: Long? = null
    private var journalChecked: Long? = null
    /** Debug-only "🧪" button; shown with Check-in, i.e. only at the journey's destination. */
    private var testCheckinButton: View? = null
    override fun onResume() {
        super.onResume()
    }

    private val locationSource by lazy { com.project.bcv.feature.location.startingpoint.data.AndroidDeviceLocationSource(this) }
    private val checkinPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) checkIn()
        else Snackbar.make(binding.root, "Cần quyền vị trí để xác nhận bạn đang ở địa danh.", Snackbar.LENGTH_LONG).show()
    }

    /** F77: being physically at the place (≤ 750 m, checked by the server) unlocks it with a real-visit stamp. */
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
                com.project.bcv.feature.engagement.CheckinSuccessDialog.showFailure(this@PlaceDetailActivity, "NO_LOCATION",
                    "Chưa lấy được vị trí của bạn. Hãy bật GPS và thử lại ở nơi thoáng.") { checkIn() }
                return@findCurrent
            }
            lifecycleScope.launch {
                val result = runCatching {
                    com.project.bcv.feature.engagement.EngagementApi(this@PlaceDetailActivity)
                        .checkIn(placeId, coordinates.latitude, coordinates.longitude)
                }
                binding.checkinButton.isEnabled = true
                binding.checkinButton.text = "Check-in"
                checkedIn(placeId, result, test = false)
            }
        }
    }

    /** Debug only: the server accepts it just on test servers, and stands the user at the place. */
    private fun testCheckIn() {
        val placeId = intent.getLongExtra(EXTRA_PLACE_ID, 0L)
        binding.checkinButton.isEnabled = false
        lifecycleScope.launch {
            val result = runCatching {
                com.project.bcv.feature.engagement.EngagementApi(this@PlaceDetailActivity).checkIn(placeId, 0.0, 0.0, test = true)
            }
            binding.checkinButton.isEnabled = true
            checkedIn(placeId, result, test = true)
        }
    }

    /** New check-in → celebration dialog; already today or failed → a short snackbar. */
    private fun checkedIn(placeId: Long, result: Result<org.json.JSONObject>, test: Boolean) {
        val body = result.getOrElse {
            if (!isFinishing && !isDestroyed) com.project.bcv.feature.engagement.CheckinSuccessDialog.showFailure(this,
                (it as? com.project.bcv.core.network.ApiFailure)?.code ?: if (it is java.io.IOException) "NETWORK_ERROR" else null,
                if (it is java.io.IOException) "Không kết nối được tới máy chủ." else it.message) { if (test) testCheckIn() else checkIn() }
            return
        }
        journalChecked = null
        model.refresh(placeId)
        if (body.optBoolean("alreadyToday")) {
            Snackbar.make(binding.root, body.optString("message", "Hôm nay bạn đã check-in ở đây."), Snackbar.LENGTH_LONG).show()
            return
        }
        if (isFinishing || isDestroyed) return
        com.project.bcv.feature.engagement.CheckinSuccessDialog.show(this, body, test) {
            binding.communityMediaButton.performClick()
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
                com.google.android.material.R.attr.materialButtonOutlinedStyle).also { testCheckinButton = it }.apply {
                visibility = View.GONE
                text = ""; setIconResource(com.project.bcv.R.drawable.ic_m3_science); iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START; iconPadding = 0; minWidth = 0; minimumWidth = 0
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
                renderedPlaceId == place.id -> {
                    // Already drawn once: make sure no placeholder is left over it.
                    if (binding.loadingContent.visibility == View.VISIBLE) {
                        Skeleton.hide(binding.loadingSkeleton)
                        binding.loadingContent.visibility = View.GONE
                    }
                    showPlace(place)
                }
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
        ratingValue.text = place.rating?.takeIf { (place.ratingCount ?: 0) > 0 }?.let { String.format(Locale.forLanguageTag("vi-VN"), "%.1f", it) } ?: "—"
        ratingValue.compoundDrawablePadding = (4 * resources.displayMetrics.density).toInt()
        ratingValue.setCompoundDrawablesRelative(ContextCompat.getDrawable(this@PlaceDetailActivity, R.drawable.ic_m3_star)!!.mutate().apply {
            setTint(0xFFC9962B.toInt()); val s = (20 * resources.displayMetrics.density).toInt(); setBounds(0, 0, s, s) }, null, null, null)
        ratingLabel.text = place.ratingCount?.takeIf { it > 0 }?.let { "${number.format(it)} đánh giá" } ?: "Chưa có đánh giá"
        distanceText.text = if (place.distanceKm < 1) "${(place.distanceKm * 1000).toInt()} m"
            else String.format(Locale.forLanguageTag("vi-VN"), "%.1f km", place.distanceKm)
        stepsText.text = if (place.distanceKm <= 0.001) "Vị trí ảo ở đây" else "Từ vị trí ảo · ~${number.format(place.requiredSteps)} bước"
        placeTypeText.text = place.placeType.ifBlank { "—" }

        // Locked: what the walk unlocks.
        unlockCard.visibility = if (discovered) View.GONE else View.VISIBLE
        unlockHint.text = when {
            place.isJourneyDestination -> "Điểm đến của hành trình bạn đang đi. Đi đủ bước để mở khóa, hoặc check-in khi đến nơi thật."
            place.distanceKm <= 0.001 -> "Vị trí ảo của bạn đang ở đây. Đến nơi thật thì check-in để đóng dấu hộ chiếu."
            else -> "Khoảng ${number.format(place.requiredSteps)} bước từ vị trí của bạn. Chọn làm điểm đến để bắt đầu hành trình."
        }

        storyText.text = place.introduction ?: "Nội dung về địa danh này đang được cập nhật."
        val fact = place.highlights?.removePrefix("Bạn có biết?")?.trim()
            // Seed data sometimes repeats the introduction; only show a fact that adds something.
            ?.takeUnless { f -> place.introduction?.let { intro -> intro.trim().contains(f.trimEnd('.')) || f.contains(intro.trim().trimEnd('.')) } == true }
        factCard.visibility = if (fact.isNullOrEmpty()) View.GONE else View.VISIBLE
        factText.text = fact

        showVideos(place)

        // Check-in only at the journey's destination or where the virtual location stands (the server enforces the same).
        checkinButton.visibility = if (place.canCheckIn) View.VISIBLE else View.GONE
        testCheckinButton?.visibility = checkinButton.visibility
        // Bottom bar: journal once checked in here, otherwise pick as destination.
        // The journal is for real visits only: its button appears once the user has checked in here.
        communityMediaButton.visibility = if (journalOpen == place.id) View.VISIBLE else View.GONE
        if (journalChecked != place.id) {
            journalChecked = place.id
            lifecycleScope.launch {
                val open = runCatching { com.project.bcv.feature.engagement.EngagementApi(this@PlaceDetailActivity).journalEntry(place.id) }.isSuccess
                journalOpen = if (open) place.id else null
                communityMediaButton.visibility = if (open) View.VISIBLE else View.GONE
                updateBottomBar()
            }
        }
        // Offer to pick it unless it is already the destination: for a locked place, or an unlocked one
        // whose card still has to be walked for (cards come only from finishing a journey).
        selectButton.visibility = if ((!discovered || place.cardPending) && !place.isJourneyDestination) View.VISIBLE else View.GONE
        updateBottomBar()

        val items = photosOf(place).map { Media.Photo(it) } +
            (if (discovered) place.videos.map { Media.Clip(it) } else emptyList())
        heroPlaceholder.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        if (items != galleryItems) {
            galleryItems = items
            buildThumbnails()
            selectImage(0)
        }
    }

    /** Videos sit in the top gallery once unlocked; before that, one line says how many unlocking adds. */
    private fun showVideos(place: ExplorePlaceDetail) = with(binding) {
        officialVideos.removeAllViews()
        val locked = !place.isDiscovered && place.videoCount > 0
        officialVideos.visibility = if (locked) View.VISIBLE else View.GONE
        if (!locked) return@with
        val density = resources.displayMetrics.density
        officialVideos.addView(android.widget.TextView(this@PlaceDetailActivity).apply {
            text = "${place.videoCount} video về ${place.name}. Mở khóa địa danh để xem."
            compoundDrawablePadding = (8 * density).toInt()
            setCompoundDrawablesRelative(ContextCompat.getDrawable(context, com.project.bcv.R.drawable.ic_m3_lock)!!.mutate().apply {
                setTint(0xFF5F7468.toInt()); setBounds(0, 0, (18 * density).toInt(), (18 * density).toInt()) }, null, null, null)
            textSize = 15f; setTextColor(0xFF5F7468.toInt())
            background = GradientDrawable().apply { setColor(0xFFF1F5F1.toInt()); cornerRadius = 16 * density }
            setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
        })
    }

    /** The bar hides itself when none of its buttons applies (e.g. discovered, not a destination, no journal). */
    private fun updateBottomBar() = with(binding) {
        if (placeContent.visibility != View.VISIBLE) return@with
        val any = listOf(checkinButton, communityMediaButton, selectButton).any { it.visibility == View.VISIBLE }
        bottomBar.visibility = if (any) View.VISIBLE else View.GONE
    }

    private fun buildThumbnails() = with(binding) {
        galleryThumbs.removeAllViews()
        galleryThumbRow.visibility = if (galleryItems.size > 1) View.VISIBLE else View.GONE
        if (galleryItems.size <= 1) return@with
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        galleryItems.forEachIndexed { index, item ->
            val image = ImageView(this@PlaceDetailActivity).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            val thumb = android.widget.FrameLayout(this@PlaceDetailActivity).apply {
                clipToOutline = true
                val inset = dp(2)
                setPadding(inset, inset, inset, inset)
                isClickable = true
                setOnClickListener { selectImage(index) }
                addView(image, android.widget.FrameLayout.LayoutParams(-1, -1))
            }
            when (item) {
                is Media.Photo -> {
                    thumb.contentDescription = "Ảnh ${index + 1} trên ${galleryItems.size}"
                    lifecycleScope.launch { loadBitmap(item.url, sample = 4)?.let(image::setImageBitmap) }
                }
                is Media.Clip -> {
                    thumb.contentDescription = "Video ${index + 1} trên ${galleryItems.size}, ${item.video.durationSeconds.toInt()} giây"
                    image.setBackgroundColor(0xFF19473B.toInt())
                    thumb.addView(playButton(16), android.widget.FrameLayout.LayoutParams(dp(26), dp(26), android.view.Gravity.CENTER))
                    thumb.addView(android.widget.TextView(this@PlaceDetailActivity).apply {
                        text = duration(item.video.durationSeconds); textSize = 10f; setTextColor(0xFFFFFFFF.toInt())
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        background = GradientDrawable().apply { setColor(0x99000000.toInt()); cornerRadius = 6 * density }
                        setPadding(dp(4), dp(1), dp(4), dp(1))
                    }, android.widget.FrameLayout.LayoutParams(-2, -2, android.view.Gravity.BOTTOM or android.view.Gravity.END).apply {
                        setMargins(0, 0, dp(5), dp(4)) })
                    lifecycleScope.launch { videoFrame(item.video)?.let(image::setImageBitmap) }
                }
            }
            galleryThumbs.addView(thumb, LinearLayout.LayoutParams(dp(76), dp(58))
                .apply { if (index > 0) marginStart = dp(8) })
        }
    }

    private fun duration(seconds: Double) = seconds.toInt().let { String.format(Locale.ROOT, "%d:%02d", it / 60, it % 60) }

    /** Shows item [index] in the hero: a photo, or a video's own frame with a play button; thumbnails mark it. */
    private fun selectImage(index: Int) = with(binding) {
        galleryIndex = index
        val item = galleryItems.getOrNull(index)
        galleryCounter.text = String.format(Locale.ROOT, "%d / %d", index + 1, galleryItems.size)
        galleryCounter.visibility = if (galleryItems.size > 1) View.VISIBLE else View.GONE
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
        heroPlay?.let { heroCard.removeView(it) }
        heroPlay = null
        heroCard.setOnClickListener(null); heroCard.isClickable = false
        when (item) {
            null -> placeImage.setImageDrawable(null)
            is Media.Photo -> {
                placeImage.contentDescription = "Ảnh ${placeName.text}, chạm để xem toàn màn hình"
                heroCard.isClickable = true
                heroCard.setOnClickListener { viewPhotos(item.url) }
                // Cached photos (the first is preloaded before the page shows) are set without a frame of gap.
                bitmaps["2|${item.url}"]?.let { placeImage.setImageBitmap(it); return@with }
                lifecycleScope.launch {
                    val bitmap = loadBitmap(item.url, sample = 2)
                    if (galleryIndex == index && bitmap != null) placeImage.setImageBitmap(bitmap)
                }
            }
            is Media.Clip -> {
                val video = item.video
                placeImage.contentDescription = "Video ${placeName.text}, chạm để phát"
                placeImage.setImageBitmap(bitmaps["frame|${video.id}"])
                val play = heroPlayOverlay(video)
                heroCard.addView(play, heroCard.indexOfChild(statusTag))
                heroPlay = play
                heroCard.isClickable = true
                heroCard.setOnClickListener { openVideo(video) }
                if (bitmaps["frame|${video.id}"] == null) lifecycleScope.launch {
                    val frame = videoFrame(video)
                    if (galleryIndex == index && frame != null) placeImage.setImageBitmap(frame)
                }
            }
        }
    }

    /**
     * The place's photos full screen on black: swipe between them, "i / n" on top, close button.
     * Opens on [startUrl]; images load at full size (the hero keeps its smaller copy).
     */
    private fun viewPhotos(startUrl: String) {
        val urls = galleryItems.filterIsInstance<Media.Photo>().map { it.url }
        if (urls.isEmpty()) return
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val root = android.widget.FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        val counter = android.widget.TextView(this).apply {
            textSize = 15f; setTextColor(0xFFFFFFFF.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
        }
        val pager = androidx.viewpager2.widget.ViewPager2(this)
        pager.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun getItemCount() = urls.size
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                object : androidx.recyclerview.widget.RecyclerView.ViewHolder(ImageView(parent.context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams = android.view.ViewGroup.LayoutParams(-1, -1)
                }) {}
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val image = holder.itemView as ImageView
                val url = urls[position]
                image.setImageBitmap(bitmaps["1|$url"] ?: bitmaps["2|$url"])
                lifecycleScope.launch { loadBitmap(url, sample = 1)?.let { if (holder.bindingAdapterPosition == position) image.setImageBitmap(it) } }
            }
        }
        pager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { counter.text = "${position + 1} / ${urls.size}" }
        })
        root.addView(pager, android.widget.FrameLayout.LayoutParams(-1, -1))
        val start = urls.indexOf(startUrl).coerceAtLeast(0)
        pager.setCurrentItem(start, false)
        counter.text = "${start + 1} / ${urls.size}"
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(40), dp(16), dp(12))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x99000000.toInt(), 0x00000000))
        }
        bar.addView(android.widget.FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x33FFFFFF) }
            isClickable = true; contentDescription = "Đóng"
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_close); imageTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            }, android.widget.FrameLayout.LayoutParams(dp(22), dp(22), android.view.Gravity.CENTER))
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        bar.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER_HORIZONTAL
            addView(android.widget.TextView(context).apply {
                text = binding.placeName.text; textSize = 13f; setTextColor(0xCCFFFFFF.toInt()); maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END; gravity = android.view.Gravity.CENTER
            })
            addView(counter)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(View(this), LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(bar, android.widget.FrameLayout.LayoutParams(-1, -2, android.view.Gravity.TOP))
        dialog.setContentView(root)
        dialog.show()
    }

    /** White round button with the Material Symbols play_arrow icon in it. */
    private fun playButton(iconDp: Int) = android.widget.FrameLayout(this).apply {
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xF2FFFFFF.toInt()) }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        val size = (iconDp * resources.displayMetrics.density).toInt()
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_m3_play_arrow)
            imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.forest))
        }, android.widget.FrameLayout.LayoutParams(size, size, android.view.Gravity.CENTER))
    }

    /** Big play button in the middle of the hero and the length in a corner. */
    private fun heroPlayOverlay(video: com.project.bcv.feature.discovery.domain.PlaceVideo) =
        android.widget.FrameLayout(this).apply {
            val density = resources.displayMetrics.density
            fun dp(v: Int) = (v * density).toInt()
            addView(playButton(36), android.widget.FrameLayout.LayoutParams(dp(68), dp(68), android.view.Gravity.CENTER).apply { bottomMargin = dp(30) })
            addView(android.widget.TextView(context).apply {
                text = "Video · ${duration(video.durationSeconds)}"; textSize = 12f; setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = GradientDrawable().apply { setColor(0x99000000.toInt()); cornerRadius = 10 * density }
                setPadding(dp(10), dp(5), dp(10), dp(5))
            }, android.widget.FrameLayout.LayoutParams(-2, -2, android.view.Gravity.TOP or android.view.Gravity.END).apply {
                setMargins(0, dp(52), dp(14), 0) })
            layoutParams = android.widget.FrameLayout.LayoutParams(-1, -1)
        }

    private fun openVideo(video: com.project.bcv.feature.discovery.domain.PlaceVideo) {
        val placeId = model.state.value?.place?.id ?: intent.getLongExtra(EXTRA_PLACE_ID, 0L)
        startActivity(android.content.Intent(this, PlaceVideoActivity::class.java)
            .putExtra("place_id", placeId).putExtra("video_id", video.id)
            .putExtra(PlaceVideoActivity.EXTRA_TITLE, model.state.value?.place?.name))
    }

    /** A real frame of the video (about one second in), read over the authenticated stream and cached. */
    private suspend fun videoFrame(video: com.project.bcv.feature.discovery.domain.PlaceVideo): Bitmap? {
        val key = "frame|${video.id}"
        bitmaps[key]?.let { return it }
        val url = MediaUrlPolicy.authenticatedUrl(BuildConfig.API_BASE_URL, video.url) ?: return null
        val token = SessionPreferences(this).token ?: return null
        val frame = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            val result = runCatching {
                retriever.setDataSource(url, mapOf("Authorization" to "Bearer $token"))
                val at = (minOf(1.0, video.durationSeconds / 2) * 1_000_000).toLong()
                retriever.getFrameAtTime(at, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { raw ->
                    val scale = minOf(1f, 960f / maxOf(raw.width, raw.height))
                    if (scale < 1f) Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true)
                    else raw
                }
            }.getOrNull()
            runCatching { retriever.release() }
            result
        }
        if (frame != null) bitmaps[key] = frame
        return frame
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
