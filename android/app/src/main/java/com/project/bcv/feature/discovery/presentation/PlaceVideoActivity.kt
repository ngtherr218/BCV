package com.project.bcv.feature.discovery.presentation

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.project.bcv.BuildConfig
import com.project.bcv.R
import com.project.bcv.core.network.MediaUrlPolicy
import com.project.bcv.feature.account.auth.data.local.SessionPreferences

/** A single authenticated player for official videos and approved/owned community clips. */
class PlaceVideoActivity : AppCompatActivity() {
    private lateinit var video: VideoView
    private lateinit var status: TextView
    private lateinit var retry: MaterialButton
    private lateinit var loading: ProgressBar
    private var contentUrl = ""
    private var position = 0
    private var prepared = false
    private var playWhenReady = true
    private var foreground = false

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
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left + dp(16), bars.top, bars.right + dp(16), bars.bottom + dp(12)); insets
        }
        root.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "‹ Quay lại"; setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-2, -2))
        root.addView(TextView(this).apply {
            text = intent.getStringExtra(EXTRA_TITLE)?.takeIf(String::isNotBlank) ?: "Video địa danh"
            textSize = 20f; setTextColor(ContextCompat.getColor(context, R.color.forest))
            maxLines = 3; setPadding(0, dp(8), 0, dp(12))
        })
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        video = VideoView(this)
        frame.addView(video, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        loading = ProgressBar(this)
        frame.addView(loading, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
        status = TextView(this).apply {
            textSize = 14f; setTextColor(ContextCompat.getColor(context, R.color.forest)); setPadding(0, dp(12), 0, dp(6))
        }
        root.addView(status)
        retry = MaterialButton(this).apply { text = "Thử tải lại video"; visibility = View.GONE; setOnClickListener { playWhenReady = true; openVideo() } }
        root.addView(retry)
        setContentView(root)
        video.setMediaController(MediaController(this).apply { setAnchorView(frame) })
        video.setOnPreparedListener {
            prepared = true; loading.visibility = View.GONE; status.text = "Chạm video để hiện điều khiển phát."
            if (position > 0) video.seekTo(position)
            if (foreground && playWhenReady) video.start()
        }
        video.setOnErrorListener { _, _, _ ->
            prepared = false; loading.visibility = View.GONE; retry.visibility = View.VISIBLE
            status.text = "Không phát được video. Kiểm tra kết nối; nội dung có thể đã bị gỡ, chưa mở khóa hoặc phiên đăng nhập đã hết hạn."
            true
        }
        video.setOnCompletionListener { playWhenReady = false; position = 0; status.text = "Đã xem hết video. Chạm nút phát để xem lại." }
        openVideo()
    }

    private fun openVideo() {
        val auth = SessionPreferences(this).token
        if (auth.isNullOrBlank()) {
            status.text = "Phiên đăng nhập đã hết hạn. Hãy quay lại và đăng nhập."
            loading.visibility = View.GONE; retry.visibility = View.GONE; return
        }
        prepared = false; retry.visibility = View.GONE; loading.visibility = View.VISIBLE; status.text = "Đang tải video…"
        video.setVideoURI(Uri.parse(contentUrl), mapOf(
            "Authorization" to "Bearer $auth",
            // Android's MediaHTTPConnection consumes this header instead of forwarding it.
            "android-allow-cross-domain-redirect" to "0"))
    }

    override fun onResume() { super.onResume(); foreground = true; if (::video.isInitialized && prepared && playWhenReady) video.start() }
    override fun onPause() {
        foreground = false
        if (::video.isInitialized && prepared) { position = video.currentPosition; playWhenReady = video.isPlaying; video.pause() }
        super.onPause()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("position", if (::video.isInitialized && prepared) video.currentPosition else position)
        outState.putBoolean("playing", if (::video.isInitialized && prepared && foreground) video.isPlaying else playWhenReady)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { if (::video.isInitialized) video.stopPlayback(); super.onDestroy() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_CONTENT_PATH = "content_path"
        const val EXTRA_TITLE = "video_title"
    }
}
