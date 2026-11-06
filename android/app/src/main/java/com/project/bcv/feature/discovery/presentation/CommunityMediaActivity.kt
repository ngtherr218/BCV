package com.project.bcv.feature.discovery.presentation

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.bcv.BuildConfig
import com.project.bcv.R
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.core.network.MediaUrlPolicy
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.discovery.domain.MediaUploadPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class CommunityMediaActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var upload: MaterialButton
    private lateinit var refresh: MaterialButton
    private lateinit var caption: EditText
    private var placeId = 0L
    private var sending = false
    private var accessReady = false
    private var loadJob: Job? = null
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::send) }
    private fun token() = SessionPreferences(this).token?.takeIf(String::isNotBlank) ?: error("Vui lòng đăng nhập lại.")
    private fun client() = JsonApiClient(this, BuildConfig.API_BASE_URL)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 15f) = TextView(this).apply {
        text = value; textSize = size; setTextColor(ContextCompat.getColor(context, R.color.forest))
        setPadding(0, dp(6), 0, dp(6))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        placeId = intent.getLongExtra("place_id", 0)
        if (placeId <= 0) { finish(); return }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(-1) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(dp(20) + bars.left, bars.top, dp(20) + bars.right, bars.bottom); insets
        }
        root.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "‹ Quay lại"; contentDescription = "Quay lại chi tiết địa danh"; setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-2, -2))
        root.addView(label("Ảnh và video cộng đồng", 23f).apply { setTypeface(typeface, Typeface.BOLD) })
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(20)) }
        root.addView(ScrollView(this).apply { addView(content); isFillViewport = true }, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(label("Chia sẻ góc nhìn của bạn. Nội dung chỉ hiển thị cho cộng đồng sau khi được duyệt."))
        content.addView(label("Ảnh JPEG/PNG/WebP tối đa 10 MB. Video MP4/WebM tối đa 100 MB, 60 giây, 1080p.", 13f))
        caption = EditText(this).apply {
            hint = "Mô tả ảnh hoặc video (tối đa 500 ký tự)"
            filters = arrayOf(InputFilter.LengthFilter(500)); minLines = 2; maxLines = 4
            setText(savedInstanceState?.getString("caption").orEmpty())
            setTextColor(ContextCompat.getColor(context, R.color.forest))
        }
        content.addView(caption)
        upload = MaterialButton(this).apply {
            text = "Chọn ảnh hoặc video để gửi"; isEnabled = false
            setOnClickListener { picker.launch(MediaUploadPolicy.acceptedMimeTypes) }
        }
        content.addView(upload, LinearLayout.LayoutParams(-1, -2))
        refresh = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Tải lại trạng thái duyệt"; setOnClickListener { load() }
        }
        content.addView(refresh)
        status = label("")
        content.addView(status)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)
        setContentView(root)
        load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("caption", caption.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            status.text = "Đang tải nội dung…"
            accessReady = false
            upload.isEnabled = false
            refresh.isEnabled = false
            list.removeAllViews()
            try {
                val auth = token()
                val items = withContext(Dispatchers.IO) { client().get("api/places/$placeId/media", auth).getJSONArray("items") }
                showItems(items, auth)
                accessReady = true
                upload.isEnabled = !sending
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { status.text = error.message ?: "Chưa tải được nội dung. Vui lòng thử lại." }
            finally { refresh.isEnabled = !sending }
        }
    }

    private fun showItems(items: JSONArray, auth: String) {
        status.text = if (items.length() == 0) "Chưa có nội dung. Bạn có thể gửi ảnh hoặc video đầu tiên." else "${items.length()} nội dung gần đây"
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val state = item.getString("status")
            val stateText = when (state) {
                "APPROVED" -> "Đã duyệt"; "REJECTED" -> "Bị từ chối"; "FLAGGED" -> "Tạm ẩn để kiểm tra"
                "REMOVED" -> "Đã gỡ"; "EXPIRED" -> "Hết hạn"; else -> "Chờ duyệt"
            }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(14))
                background = GradientDrawable().apply { setColor(0xFFF6F9F7.toInt()); cornerRadius = dp(16).toFloat() }
            }
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
            val author = if (item.optBoolean("isOwner")) "Bạn" else item.optString("author", "Thành viên")
            card.addView(label("$author · $stateText", 16f).apply { setTypeface(typeface, Typeface.BOLD) })
            val description = item.optString("caption").takeUnless { it == "null" }.orEmpty()
            if (description.isNotBlank()) card.addView(label(description))
            item.optString("reviewNote").takeUnless { it.isBlank() || it == "null" }?.let {
                card.addView(label("${if (state == "REJECTED") "Lý do từ chối" else "Ghi chú kiểm duyệt"}: $it", 14f))
            }
            val path = item.optString("contentUrl")
            val contentUrl = MediaUrlPolicy.authenticatedUrl(BuildConfig.API_BASE_URL, path)
            if (item.optBoolean("contentAvailable", false) && contentUrl != null) {
                if (item.optString("mimeType").startsWith("image/")) {
                    val image = ImageView(this).apply {
                        adjustViewBounds = true; scaleType = ImageView.ScaleType.CENTER_CROP
                        contentDescription = description.ifBlank { "Ảnh tại địa danh" }
                    }
                    card.addView(image, LinearLayout.LayoutParams(-1, dp(210)))
                    val imageStatus = label("Đang tải ảnh…", 13f)
                    card.addView(imageStatus)
                    lifecycleScope.launch {
                        val bitmap = RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, contentUrl, auth, 4)
                        if (bitmap != null) { image.setImageBitmap(bitmap); imageStatus.visibility = View.GONE }
                        else { image.visibility = View.GONE; imageStatus.text = "Ảnh chưa tải được hoặc không còn quyền xem. Hãy tải lại trạng thái." }
                    }
                } else if (item.optString("mimeType") in setOf("video/mp4", "video/webm")) {
                    card.addView(MaterialButton(this).apply {
                        text = "▶ Xem video"
                        setOnClickListener { startActivity(Intent(this@CommunityMediaActivity, PlaceVideoActivity::class.java)
                            .putExtra(PlaceVideoActivity.EXTRA_CONTENT_PATH, path)
                            .putExtra(PlaceVideoActivity.EXTRA_TITLE, description.ifBlank { "Video cộng đồng" })) }
                    })
                }
            } else if (state in setOf("REMOVED", "EXPIRED")) card.addView(label("Tệp không còn được lưu.", 13f))
            if (state == "APPROVED" && item.optBoolean("canReport", false) && !item.optBoolean("isOwner", true)) {
                card.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = "Báo cáo nội dung"; setOnClickListener { report(item.getLong("mediaId"), this) }
                })
            }
        }
    }

    private fun report(mediaId: Long, button: MaterialButton) {
        val reason = EditText(this).apply { hint = "Lý do báo cáo"; minLines = 2; filters = arrayOf(InputFilter.LengthFilter(500)) }
        val frame = LinearLayout(this).apply { setPadding(dp(24), 0, dp(24), 0); addView(reason, LinearLayout.LayoutParams(-1, -2)) }
        val dialog = MaterialAlertDialogBuilder(this).setTitle("Báo cáo nội dung")
            .setMessage("Mô tả vấn đề để quản trị viên xem xét (1–500 ký tự).")
            .setView(frame).setNegativeButton("Hủy", null).setPositiveButton("Gửi báo cáo", null).create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val text = reason.text.toString().trim()
                if (text.isEmpty()) { reason.error = "Vui lòng nhập lý do."; return@setOnClickListener }
                dialog.dismiss(); button.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val auth = token()
                        withContext(Dispatchers.IO) { client().post("api/community-media/$mediaId/reports", JSONObject().put("reason", text), auth) }
                        Toast.makeText(this@CommunityMediaActivity, "Đã gửi báo cáo để quản trị viên xem xét.", Toast.LENGTH_LONG).show()
                        load()
                    } catch (error: CancellationException) { throw error }
                    catch (error: Exception) { status.text = error.message ?: "Chưa gửi được báo cáo."; button.isEnabled = true }
                }
            }
        }
        dialog.show()
    }

    private fun send(uri: Uri) {
        if (sending) return
        sending = true; upload.isEnabled = false; refresh.isEnabled = false
        val text = caption.text.toString()
        lifecycleScope.launch {
            status.text = "Đang kiểm tra và tải lên…"
            try {
                val auth = token()
                withContext(Dispatchers.IO) {
                    val mime = contentResolver.getType(uri)?.lowercase() ?: error("Không xác định được định dạng tệp.")
                    val limit = MediaUploadPolicy.maximumBytes(mime)
                    val length = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                    require(length <= limit) { "Tệp quá lớn: ảnh tối đa 10 MB, video tối đa 100 MB." }
                    require(length != 0L) { "Tệp được chọn không có nội dung." }
                    if (mime.startsWith("video/")) validateVideo(uri)
                    else {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        val source = contentResolver.openInputStream(uri) ?: error("Không đọc được tệp ảnh.")
                        source.use { BitmapFactory.decodeStream(it, null, bounds) }
                        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Tệp ảnh bị lỗi hoặc không đọc được." }
                    }
                    // Stream large videos instead of keeping a 100 MB copy in the app heap.
                    // Unknown-length document providers use chunked transfer; the transport still enforces limit.
                    client().postStream("api/places/$placeId/media?caption=${URLEncoder.encode(text, "UTF-8")}", mime, auth, length, limit.toLong()) {
                        contentResolver.openInputStream(uri) ?: error("Không đọc được tệp.")
                    }
                }
                Toast.makeText(this@CommunityMediaActivity, "Đã gửi nội dung, đang chờ duyệt.", Toast.LENGTH_LONG).show()
                caption.text.clear(); sending = false; load()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { status.text = error.message ?: "Tải lên thất bại. Vui lòng thử lại." }
            finally { sending = false; upload.isEnabled = accessReady; refresh.isEnabled = true }
        }
    }

    private fun validateVideo(uri: Uri) {
        val metadata = MediaMetadataRetriever()
        try {
            try { metadata.setDataSource(this, uri) }
            catch (_: Exception) { error("Không đọc được video. Hãy chọn tệp MP4/WebM hợp lệ.") }
            MediaUploadPolicy.validateVideo(
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes")
        } finally { metadata.release() }
    }
}
