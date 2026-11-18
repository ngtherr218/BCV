package com.project.bcv.feature.discovery.presentation

import com.project.bcv.core.ui.roundIconButton
import com.project.bcv.core.ui.formatClock
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.project.bcv.BuildConfig
import com.project.bcv.R
import com.project.bcv.core.network.MediaUrlPolicy
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import java.util.Locale

/**
 * Full-screen player for a place's official video: black, edge to edge, system bars hidden.
 * Tap shows/hides the controls (back + title on top, play/pause in the middle, a seek bar with the
 * time and a rotate button at the bottom); they fade away on their own while playing.
 */
class PlaceVideoActivity : AppCompatActivity() {
    private lateinit var video: VideoView
    private lateinit var loading: ProgressBar
    private lateinit var playButton: FrameLayout
    private lateinit var playIcon: ImageView
    private lateinit var seek: SeekBar
    private lateinit var time: TextView
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var errorBox: LinearLayout
    private lateinit var errorText: TextView
    private var contentUrl = ""
    private var position = 0
    private var prepared = false
    private var playWhenReady = true
    private var foreground = false
    private var controlsShown = true
    private var dragging = false
    private val handler = Handler(Looper.getMainLooper())
    private val hideControls = Runnable { if (video.isPlaying) setControls(false) }
    private val tick = object : Runnable {
        override fun run() {
            if (prepared && !dragging) {
                seek.max = video.duration.coerceAtLeast(1)
                seek.progress = video.currentPosition
                time.text = "${clock(video.currentPosition)} / ${clock(video.duration)}"
            }
            // One last update after a pause, then rest until playback starts again (startTicking).
            if (video.isPlaying) handler.postDelayed(this, 250)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val placeId = intent.getLongExtra("place_id", 0)
        val videoId = intent.getLongExtra("video_id", 0)
        val path = intent.getStringExtra(EXTRA_CONTENT_PATH)
            ?: if (placeId > 0 && videoId > 0) "/api/explore/places/$placeId/videos/$videoId" else ""
        contentUrl = MediaUrlPolicy.authenticatedUrl(BuildConfig.API_BASE_URL, path)
            ?.takeUnless { Uri.parse(it).path?.endsWith("/thumbnail") == true }.orEmpty()
        if (contentUrl.isEmpty()) { Toast.makeText(this, "Liên kết video không hợp lệ.", Toast.LENGTH_LONG).show(); finish(); return }
        position = savedInstanceState?.getInt("position") ?: 0
        playWhenReady = savedInstanceState?.getBoolean("playing") ?: true

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        video = VideoView(this)
        root.addView(video, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        loading = ProgressBar(this).apply { indeterminateTintList = ColorStateList.valueOf(Color.WHITE) }
        root.addView(loading, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER))

        // Top: back and the place's name over a soft shade.
        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xAA000000.toInt(), 0x00000000))
            addView(roundIconButton(R.drawable.ic_arrow_back_engagement, "Quay lại") { finish() },
                LinearLayout.LayoutParams(dp(44), dp(44)))
            addView(TextView(context).apply {
                text = intent.getStringExtra(EXTRA_TITLE)?.takeIf(String::isNotBlank) ?: "Video địa danh"
                textSize = 17f; setTextColor(Color.WHITE); setTypeface(typeface, android.graphics.Typeface.BOLD)
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(12), 0, dp(8), 0)
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(topBar, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        // Middle: one big play / pause button.
        playIcon = ImageView(this).apply { imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.forest)) }
        playButton = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xEEFFFFFF.toInt()) }
            isClickable = true; contentDescription = "Phát / tạm dừng"
            addView(playIcon, FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER))
            setOnClickListener { togglePlay() }
        }
        root.addView(playButton, FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER))

        // Bottom: time, seek bar, rotate.
        seek = SeekBar(this).apply {
            progressTintList = ColorStateList.valueOf(Color.WHITE); thumbTintList = ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = ColorStateList.valueOf(0x66FFFFFF)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) time.text = "${clock(value)} / ${clock(video.duration)}"
                }
                override fun onStartTrackingTouch(bar: SeekBar) { dragging = true; handler.removeCallbacks(hideControls) }
                override fun onStopTrackingTouch(bar: SeekBar) {
                    dragging = false; if (prepared) video.seekTo(bar.progress); scheduleHide()
                }
            })
        }
        time = TextView(this).apply { textSize = 12f; setTextColor(Color.WHITE); text = "0:00 / 0:00" }
        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0xAA000000.toInt(), 0x00000000))
            addView(time, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(4) })
            addView(seek, LinearLayout.LayoutParams(0, -2, 1f))
            addView(roundIconButton(R.drawable.ic_m3_screen_rotation, "Xoay màn hình") {
                requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }, LinearLayout.LayoutParams(dp(40), dp(40)))
        }
        root.addView(bottomBar, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

        // Error: a short message and "Thử lại", in the middle.
        errorText = TextView(this).apply { textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        errorBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; visibility = View.GONE
            setPadding(dp(32), 0, dp(32), 0)
            addView(errorText)
            addView(MaterialButton(context).apply {
                text = "Thử lại"; setOnClickListener { playWhenReady = true; openVideo() }
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(14) })
        }
        root.addView(errorBox, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            topBar.setPadding(bars.left + dp(12), bars.top + dp(12), bars.right + dp(12), dp(24))
            bottomBar.setPadding(bars.left + dp(16), dp(24), bars.right + dp(8), bars.bottom + dp(12))
            insets
        }
        root.setOnClickListener { setControls(!controlsShown) }
        setContentView(root)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        video.setOnPreparedListener {
            prepared = true; loading.visibility = View.GONE; errorBox.visibility = View.GONE
            seek.max = video.duration.coerceAtLeast(1)
            if (position > 0) video.seekTo(position)
            if (foreground && playWhenReady) video.start()
            updatePlayIcon(); scheduleHide(); startTicking()
        }
        video.setOnErrorListener { _, _, _ ->
            prepared = false; loading.visibility = View.GONE; playButton.visibility = View.GONE
            errorText.text = "Không phát được video.\nKiểm tra kết nối; video có thể đã bị gỡ, chưa mở khóa hoặc phiên đăng nhập đã hết hạn."
            errorBox.visibility = View.VISIBLE; setControls(true)
            true
        }
        video.setOnCompletionListener {
            playWhenReady = false; position = 0; updatePlayIcon(); setControls(true)
        }
        openVideo()
    }

    private fun togglePlay() {
        if (!prepared) return
        if (video.isPlaying) { video.pause(); playWhenReady = false; handler.removeCallbacks(hideControls) }
        else { video.start(); playWhenReady = true; scheduleHide() }
        updatePlayIcon(); startTicking()
    }

    private fun startTicking() { handler.removeCallbacks(tick); handler.post(tick) }

    private fun updatePlayIcon() {
        playIcon.setImageResource(if (prepared && video.isPlaying) R.drawable.ic_m3_pause else R.drawable.ic_m3_play_arrow)
    }

    /** Shows or hides the overlays with a short fade; the middle button hides only while playing. */
    private fun setControls(show: Boolean) {
        controlsShown = show
        listOf<View>(topBar, bottomBar, playButton).forEach { view ->
            if (view === playButton && errorBox.visibility == View.VISIBLE) return@forEach
            view.animate().cancel()
            if (show) { view.visibility = View.VISIBLE; view.animate().alpha(1f).setDuration(160).start() }
            else view.animate().alpha(0f).setDuration(200).withEndAction { view.visibility = View.INVISIBLE }.start()
        }
        if (show) scheduleHide()
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideControls)
        handler.postDelayed(hideControls, 2800)
    }

    private fun openVideo() {
        val auth = SessionPreferences(this).token
        if (auth.isNullOrBlank()) {
            errorText.text = "Phiên đăng nhập đã hết hạn. Hãy quay lại và đăng nhập."
            errorBox.visibility = View.VISIBLE; loading.visibility = View.GONE; return
        }
        prepared = false; errorBox.visibility = View.GONE; loading.visibility = View.VISIBLE; playButton.visibility = View.VISIBLE
        video.setVideoURI(Uri.parse(contentUrl), mapOf(
            "Authorization" to "Bearer $auth",
            // Android's MediaHTTPConnection consumes this header instead of forwarding it.
            "android-allow-cross-domain-redirect" to "0"))
    }

    private fun clock(ms: Int) = formatClock(ms / 1000)

    override fun onResume() {
        super.onResume(); foreground = true
        if (::video.isInitialized && prepared && playWhenReady) video.start()
        if (::video.isInitialized) { startTicking(); updatePlayIcon() }
    }
    override fun onPause() {
        foreground = false
        handler.removeCallbacks(tick); handler.removeCallbacks(hideControls)
        if (::video.isInitialized && prepared) { position = video.currentPosition; playWhenReady = video.isPlaying; video.pause() }
        super.onPause()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("position", if (::video.isInitialized && prepared) video.currentPosition else position)
        outState.putBoolean("playing", if (::video.isInitialized && prepared && foreground) video.isPlaying else playWhenReady)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); if (::video.isInitialized) video.stopPlayback(); super.onDestroy() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_CONTENT_PATH = "content_path"
        const val EXTRA_TITLE = "video_title"
    }
}
