package com.project.bcv.feature.discovery.presentation

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.project.bcv.BuildConfig
import com.project.bcv.feature.account.auth.data.local.SessionPreferences

class PlaceVideoActivity : AppCompatActivity() {
    private lateinit var video: VideoView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val placeId = intent.getLongExtra("place_id", 0)
        val videoId = intent.getLongExtra("video_id", 0)
        val token = SessionPreferences(this).token
        if (placeId <= 0 || videoId <= 0 || token.isNullOrBlank()) { finish(); return }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,32,16,16) }
        root.addView(Button(this).apply { text = "‹ Quay lại"; setOnClickListener { finish() } })
        video = VideoView(this)
        root.addView(video, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        video.setMediaController(MediaController(this).apply { setAnchorView(video) })
        video.setVideoURI(Uri.parse(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/explore/places/$placeId/videos/$videoId"), mapOf("Authorization" to "Bearer $token"))
        video.setOnPreparedListener { video.start() }
        video.setOnErrorListener { _, _, _ -> Toast.makeText(this,"Không tải được video hoặc địa danh chưa mở khóa.",Toast.LENGTH_LONG).show(); true }
    }
    override fun onPause() { if (::video.isInitialized) video.pause(); super.onPause() }
    override fun onDestroy() { if (::video.isInitialized) video.stopPlayback(); super.onDestroy() }
}
