package com.project.bcv.feature.discovery.presentation

import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.core.network.MediaUrlPolicy
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Approved contributions are visible in the place page, not only in the submission screen. */
class ApprovedPlaceMediaView(private val activity: AppCompatActivity) : LinearLayout(activity) {
    private var job: Job? = null
    private var placeId = 0L
    init { orientation = VERTICAL; val pad = (16 * resources.displayMetrics.density).toInt(); setPadding(0,pad,0,pad) }
    fun clear() {
        job?.cancel()
        job = null
        placeId = 0L
        removeAllViews()
    }
    fun load(id: Long) {
        placeId = id
        job?.cancel()
        removeAllViews()
        addView(TextView(context).apply { text = "Ảnh/video cộng đồng đã duyệt"; textSize = 19f; setTypeface(typeface, android.graphics.Typeface.BOLD) })
        val status = TextView(context).apply { text = "Đang tải nội dung cộng đồng…" }
        addView(status)
        addView(Button(context).apply { text = "Tải lại nội dung cộng đồng"; setOnClickListener { load(placeId) } })
        val content = LinearLayout(context).apply { orientation = VERTICAL }
        addView(content)
        val allButton = Button(context).apply {
            text = "Xem tất cả / Gửi ảnh, video"
            isEnabled = false
            setOnClickListener { openAll() }
        }
        addView(allButton)
        job = activity.lifecycleScope.launch {
            try {
                val token = SessionPreferences(context).token?.takeIf { it.isNotBlank() }
                    ?: error("Vui lòng đăng nhập lại.")
                val items = withContext(Dispatchers.IO) {
                    JsonApiClient(context,BuildConfig.API_BASE_URL).get("api/places/" + id + "/media?approvedOnly=true",token).getJSONArray("items")
                }
                allButton.isEnabled = true
                val approved = (0 until items.length()).map { items.getJSONObject(it) }.filter {
                    it.optString("status") == "APPROVED" && it.optBoolean("contentAvailable", false)
                }
                status.text = if (approved.isEmpty()) "Chưa có ảnh/video cộng đồng được duyệt." else "Nội dung người dùng đóng góp · " + approved.size + " mục đã duyệt"
                for (item in approved.take(4)) {
                    val url = MediaUrlPolicy.authenticatedUrl(BuildConfig.API_BASE_URL, item.optString("contentUrl"))
                        ?: continue
                    val caption = item.optString("caption")
                    content.addView(TextView(context).apply { text = item.optString("author") + if(caption.isBlank()) "" else "\n" + caption; textSize = 15f; setPadding(0,16,0,8) })
                    if (item.optString("mimeType").startsWith("image/")) {
                        val image = ImageView(context).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = caption.ifBlank { "Ảnh cộng đồng" }; setOnClickListener { openAll() } }
                        content.addView(image,LayoutParams(-1,(180*resources.displayMetrics.density).toInt()))
                        launch {
                            val bitmap = RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL,url,token,2)
                            if (bitmap != null) image.setImageBitmap(bitmap)
                            else { image.visibility = View.GONE; content.addView(TextView(context).apply { text = "Ảnh không còn khả dụng. Hãy tải lại." }) }
                        }
                    } else content.addView(Button(context).apply { text = "▶ Xem video cộng đồng"; setOnClickListener { openAll() } })
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { status.text = error.message ?: "Chưa tải được nội dung cộng đồng. Nhấn Tải lại để thử." }
        }
    }
    private fun openAll() { activity.startActivity(Intent(activity,CommunityMediaActivity::class.java).putExtra("place_id",placeId)) }
}
