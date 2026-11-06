package com.project.bcv.feature.discovery.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URLEncoder

class CommunityMediaActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var upload: Button
    private lateinit var caption: EditText
    private var placeId = 0L
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::send) }
    private fun token() = SessionPreferences(this).token ?: error("Vui lòng đăng nhập lại.")
    private fun client() = JsonApiClient(this, BuildConfig.API_BASE_URL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placeId = intent.getLongExtra("place_id", 0)
        if (placeId <= 0) { finish(); return }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad + 28, pad, pad)
        }
        root.addView(Button(this).apply { text = "‹ Quay lại"; setOnClickListener { finish() } })
        root.addView(TextView(this).apply { text = "Ảnh và video cộng đồng"; textSize = 23f })
        root.addView(TextView(this).apply { text = "Nội dung bạn gửi chỉ công khai sau khi quản trị viên duyệt. Ảnh JPEG/PNG/WebP ≤ 10 MB; video MP4/WebM ≤ 30 MB." })
        caption = EditText(this).apply { hint = "Mô tả (tối đa 500 ký tự)"; filters = arrayOf(android.text.InputFilter.LengthFilter(500)) }
        root.addView(caption)
        upload = Button(this).apply { text = "Chọn ảnh hoặc video để gửi"; setOnClickListener { picker.launch(arrayOf("image/*", "video/*")) } }
        root.addView(upload)
        root.addView(Button(this).apply { text = "Tải lại trạng thái duyệt"; setOnClickListener { load() } })
        status = TextView(this)
        root.addView(status)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            status.text = "Đang tải…"
            try {
                val items = withContext(Dispatchers.IO) { client().get("api/places/$placeId/media", token()).getJSONArray("items") }
                showItems(items)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { status.text = error.message ?: "Chưa tải được nội dung." }
        }
    }

    private fun showItems(items: org.json.JSONArray) {
        list.removeAllViews()
        status.text = if (items.length() == 0) "Chưa có nội dung. Bạn có thể gửi ảnh/video đầu tiên." else "${items.length()} nội dung"
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val state = when (item.getString("status")) { "APPROVED" -> "Đã duyệt"; "REJECTED" -> "Từ chối"; "FLAGGED" -> "Tạm ẩn để kiểm tra"; "REMOVED" -> "Đã gỡ"; "EXPIRED" -> "Hết hạn"; else -> "Chờ duyệt" }
            list.addView(TextView(this).apply {
                text = "${item.getString("author")} · $state\n${item.getString("caption")}\n${item.optString("reviewNote").takeUnless { it == "null" }.orEmpty()}"
                textSize = 16f
                setPadding(0, 20, 0, 8)
            })
            if (item.getString("status") == "APPROVED") {
                val url = BuildConfig.API_BASE_URL.trimEnd('/') + item.getString("contentUrl")
                if (item.getString("mimeType").startsWith("image/")) {
                    val image = ImageView(this).apply { adjustViewBounds = true; contentDescription = item.getString("caption") }
                    list.addView(image, LinearLayout.LayoutParams(-1, (180 * resources.displayMetrics.density).toInt()))
                    lifecycleScope.launch { com.project.bcv.core.ui.RemoteImages.load(url, 2)?.let(image::setImageBitmap) }
                } else list.addView(Button(this).apply {
                    text = "▶ Xem video"
                    setOnClickListener {
                        try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), item.getString("mimeType"))) }
                        catch (_: android.content.ActivityNotFoundException) { Toast.makeText(this@CommunityMediaActivity, "Thiết bị chưa có ứng dụng phát video.", Toast.LENGTH_LONG).show() }
                    }
                })
            }
        }
    }

    private fun send(uri: Uri) {
        upload.isEnabled = false
        val text = caption.text.toString()
        lifecycleScope.launch {
            status.text = "Đang tải lên…"
            try {
                withContext(Dispatchers.IO) {
                    val mime = contentResolver.getType(uri) ?: error("Không xác định được định dạng tệp.")
                    require(mime in setOf("image/jpeg", "image/png", "image/webp", "video/mp4", "video/webm")) { "Chọn ảnh JPEG/PNG/WebP hoặc video MP4/WebM." }
                    val limit = (if (mime.startsWith("image/")) 10 else 30) * 1024 * 1024
                    val bytes = ByteArrayOutputStream()
                    contentResolver.openInputStream(uri)?.use { source ->
                        val buffer = ByteArray(81920)
                        while (true) {
                            val count = source.read(buffer)
                            if (count < 0) break
                            require(bytes.size() + count <= limit) { "Tệp quá lớn: ảnh tối đa 10 MB, video tối đa 30 MB." }
                            bytes.write(buffer, 0, count)
                        }
                    } ?: error("Không đọc được tệp.")
                    client().postBytes("api/places/$placeId/media?caption=${URLEncoder.encode(text, "UTF-8")}", bytes.toByteArray(), mime, token())
                }
                Toast.makeText(this@CommunityMediaActivity, "Đã gửi nội dung, đang chờ duyệt.", Toast.LENGTH_LONG).show()
                caption.text.clear()
                load()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { status.text = error.message ?: "Tải lên thất bại. Vui lòng thử lại." }
            finally { upload.isEnabled = true }
        }
    }
}
