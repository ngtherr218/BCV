package com.project.bcv.feature.engagement

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.bcv.BuildConfig
import com.project.bcv.core.ui.NavTab
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.GOLD
import com.project.bcv.feature.engagement.EngagementUi.GOLD_SOFT
import com.project.bcv.feature.engagement.EngagementUi.LINE
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.SOFT
import com.project.bcv.feature.engagement.EngagementUi.TEAL
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.full
import com.project.bcv.feature.engagement.EngagementUi.outlinedButton
import com.project.bcv.feature.engagement.EngagementUi.pill
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.scaffold
import com.project.bcv.feature.engagement.EngagementUi.text
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/**
 * Bộ sưu tập › Nhật ký (F88): every place the user really went to (GPS check-in) becomes a private page where the user keeps
 * what the trip felt like — a few lines and their own photos — to read again later or turn into
 * a share card. Shown as a timeline by month, newest first. Nothing here is public or moderated.
 */
class JournalActivity : AppCompatActivity() {
    private lateinit var body: LinearLayout
    private lateinit var api: EngagementApi
    private var shown: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = EngagementApi(this)
        body = scaffold("Bộ sưu tập", tab = NavTab.COLLECTION,
            top = EngagementBlocks.collectionSwitcher(this, "journal"))
        cached?.let(::render)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        if (body.childCount == 0) body.full(text("Đang tải nhật ký…", 15f, MUTED), 16)
        lifecycleScope.launch {
            runCatching { api.journal().getJSONArray("items") }
                .onSuccess { items -> cached = items; render(items) }
                .onFailure {
                    if (shown != null) return@onFailure
                    body.removeAllViews()
                    body.full(text(it.message ?: "Không tải được nhật ký.", 15f, MUTED), 16)
                    body.full(outlinedButton("Thử lại").apply { setOnClickListener { load() } }, 12)
                }
        }
    }

    private fun render(items: JSONArray) {
        val json = items.toString()
        if (json == shown) return
        shown = json
        body.removeAllViews()
        body.full(intro(items), 4)
        if (items.length() == 0) return

        var month = ""
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val date = LocalDateTime.parse(item.getString("discoveredAt").take(19))
            val label = "THÁNG ${date.monthValue}/${date.year}"
            if (label != month) {
                month = label
                body.full(text(label, 12f, MUTED, true).apply { letterSpacing = 0.08f }, 22)
            }
            body.full(entryCard(item, date), 10)
        }
    }

    /** What the journal is for, plus a quick count of what the user has written. */
    private fun intro(items: JSONArray): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(GOLD_SOFT, 20, null, context)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        addView(text("📖  Nhật ký hành trình", 17f, FOREST, true))
        // The explanation is for newcomers; once pages exist a one-line summary is enough.
        if (items.length() == 0) addView(text("Mỗi nơi bạn đến tận nơi (check-in) là một trang riêng. Ghi lại cảm nhận, thêm ảnh " +
            "của bạn để xem lại sau hoặc tạo thẻ chia sẻ. Chỉ mình bạn xem được.", 13f, 0xFF7A6234.toInt()).apply {
            setPadding(0, dp(4), 0, 0); setLineSpacing(dp(2).toFloat(), 1f) })
        if (items.length() > 0) {
            val written = (0 until items.length()).count { items.getJSONObject(it).optString("note").isNotBlank() }
            val photos = (0 until items.length()).sumOf { items.getJSONObject(it).optInt("photoCount") }
            addView(text("${items.length()} trang · $written đã viết · $photos ảnh · chỉ mình bạn xem", 12f, GOLD, true).apply { setPadding(0, dp(4), 0, 0) })
        } else {
            addView(text("Khi đứng tại một địa danh ngoài đời, bấm Check-in ở trang địa danh để mở trang nhật ký đầu tiên.", 13f, MUTED).apply {
                setPadding(0, dp(8), 0, 0) })
        }
    }

    /** One page: cover photo, place and date, the first lines of the note (or an invitation to write). */
    private fun entryCard(item: JSONObject, date: LocalDateTime): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(0xFFFFFFFF.toInt(), 20, LINE, context)
        clipToOutline = true
        isClickable = true

        val cover = FrameLayout(context)
        val image = ImageView(context).apply {
            // Placeholder: a small book icon centred on a soft card until a photo loads.
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setBackgroundColor(SOFT)
            setImageResource(com.project.bcv.R.drawable.ic_journal_book)
            imageTintList = android.content.res.ColorStateList.valueOf(0xFFB5C7BE.toInt())
            setPadding(dp(40), dp(40), dp(40), dp(40))
        }
        cover.addView(image, FrameLayout.LayoutParams(-1, -1))
        cover.addView(text("%02d/%02d".format(date.dayOfMonth, date.monthValue), 12f, FOREST, true).apply {
            background = rounded(0xF2FFFFFF.toInt(), 12, null, context); setPadding(dp(9), dp(4), dp(9), dp(4))
        }, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply { setMargins(dp(12), dp(12), 0, 0) })
        if (item.optInt("photoCount") > 0) cover.addView(text("📷 ${item.getInt("photoCount")}", 12f, 0xFFFFFFFF.toInt(), true).apply {
            background = rounded(0x80000000.toInt(), 12, null, context); setPadding(dp(9), dp(4), dp(9), dp(4))
        }, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { setMargins(0, dp(12), dp(12), 0) })
        addView(cover, LinearLayout.LayoutParams(-1, dp(128)))
        // Own photo first; otherwise the place's official image; the book icon only as a last resort.
        lifecycleScope.launch {
            val own = item.optString("coverUrl").takeIf { it.isNotBlank() && it != "null" }?.let { url ->
                RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, url, api.photoToken(), 2)
            }
            (own ?: api.placeImage(item.optString("placeImageUrl"), 2))?.let {
                image.imageTintList = null; image.setPadding(0, 0, 0, 0)
                image.scaleType = ImageView.ScaleType.CENTER_CROP; image.setImageBitmap(it)
            }
        }

        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(14)) }
        val title = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        title.addView(text(item.getString("name"), 17f, FOREST, true).apply {
            maxLines = 2; ellipsize = TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(0, -2, 1f))
        texts.addView(title)
        texts.addView(text(item.getString("regionName"), 12f, MUTED).apply { setPadding(0, dp(2), 0, 0) })
        val note = item.optString("note")
        texts.addView(if (note.isNotBlank())
            text("“${note.trim()}”", 15f, 0xFF4F6559.toInt()).apply {
                setTypeface(Typeface.SERIF, Typeface.ITALIC); maxLines = 3; ellipsize = TextUtils.TruncateAt.END
                setLineSpacing(dp(2).toFloat(), 1f); setPadding(0, dp(10), 0, 0) }
        else text("✎  Chưa viết gì — chạm để ghi lại kỷ niệm", 13f, TEAL, true).apply { setPadding(0, dp(10), 0, 0) })
        addView(texts)

        contentDescription = "${item.getString("name")}, ${if (note.isNotBlank()) "đã viết" else "chưa viết"}"
        setOnClickListener {
            startActivity(Intent(this@JournalActivity, JournalEntryActivity::class.java)
                .putExtra(JournalEntryActivity.EXTRA_PLACE_ID, item.getLong("placeId"))
                .putExtra(JournalEntryActivity.EXTRA_PLACE_NAME, item.getString("name")))
        }
    }

    companion object {
        private var cached: JSONArray? = null
    }
}
