package com.project.bcv.feature.engagement

import com.project.bcv.feature.engagement.data.EngagementApi
import com.project.bcv.feature.engagement.EngagementUi.leadingIcon
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

    /** A page counts as written once it has a note or at least one photo. */
    private fun written(item: JSONObject) = item.optString("note").isNotBlank() || item.optInt("photoCount") > 0

    private fun render(items: JSONArray, force: Boolean = false) {
        val json = items.toString()
        if (json == shown && !force) return
        shown = json
        body.removeAllViews()
        // The intro only explains an empty journal; once pages exist the filter's counts say it all.
        if (items.length() == 0) { body.full(intro(items), 4); return }

        val all = (0 until items.length()).map(items::getJSONObject)
        val todo = all.count { !written(it) }
        body.full(filterBar(all.size, todo, all.size - todo) { filter = it; render(items, force = true) }, 4)
        val visible = all.filter { when (filter) { FILTER_TODO -> !written(it); FILTER_DONE -> written(it); else -> true } }
        if (visible.isEmpty()) {
            body.full(text(if (filter == FILTER_TODO) "Tuyệt! Trang nào cũng đã có kỷ niệm của bạn." else "Chưa có trang nào được ghi.",
                15f, MUTED).apply { gravity = Gravity.CENTER; setPadding(0, dp(28), 0, dp(28)) })
            return
        }

        var month = ""
        for (item in visible) {
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
        addView(text("Nhật ký hành trình", 17f, FOREST, true).leadingIcon(com.project.bcv.R.drawable.ic_m3_menu_book, FOREST, 20, 8))
        // The explanation is for newcomers; once pages exist a one-line summary is enough.
        if (items.length() == 0) addView(text("Mỗi nơi bạn đến tận nơi (check-in) là một trang riêng. Ghi lại cảm nhận, thêm ảnh " +
            "của bạn để xem lại sau hoặc tạo thẻ chia sẻ. Chỉ mình bạn xem được.", 13f, 0xFF7A6234.toInt()).apply {
            setPadding(0, dp(4), 0, 0); setLineSpacing(dp(2).toFloat(), 1f) })
        if (items.length() > 0) {
            val todo = (0 until items.length()).count { !written(items.getJSONObject(it)) }
            addView(text(if (todo == 0) "${items.length()} trang, trang nào cũng đã có kỷ niệm · chỉ mình bạn xem"
                else "${items.length()} trang · còn $todo trang chưa ghi · chỉ mình bạn xem", 12f, GOLD, true).apply { setPadding(0, dp(4), 0, 0) })
        } else {
            addView(text("Khi đứng tại một địa danh ngoài đời, bấm Check-in ở trang địa danh để mở trang nhật ký đầu tiên.", 13f, MUTED).apply {
                setPadding(0, dp(8), 0, 0) })
        }
    }

    /** Tất cả / Chưa ghi / Đã ghi: the "Chưa ghi" chip is orange so pages still waiting stand out. */
    private fun filterBar(total: Int, todo: Int, done: Int, onPick: (Int) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        listOf(Triple(FILTER_ALL, "Tất cả", total), Triple(FILTER_TODO, "Chưa ghi", todo), Triple(FILTER_DONE, "Đã ghi", done))
            .forEachIndexed { i, (key, label, count) ->
                val on = filter == key
                val accent = if (key == FILTER_TODO) TODO_ORANGE else TEAL
                addView(text("$label  $count", 13f, if (on) 0xFFFFFFFF.toInt() else accent, true).apply {
                    gravity = Gravity.CENTER
                    background = rounded(if (on) accent else 0xFFFFFFFF.toInt(), 18, if (on) null else LINE, context)
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    isClickable = true
                    contentDescription = "$label, $count trang" + if (on) ", đang chọn" else ""
                    setOnClickListener { if (!on) onPick(key) }
                }, LinearLayout.LayoutParams(-2, -2).apply { if (i > 0) marginStart = dp(8) })
            }
    }

    /** One page: cover photo, place and date, the first lines of the note (or an invitation to write). */
    private fun entryCard(item: JSONObject, date: LocalDateTime): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val done = written(item)
        // Written: a solid white page. Still empty: a dashed orange outline, like a page waiting to be filled.
        background = if (done) rounded(0xFFFFFFFF.toInt(), 20, LINE, context)
            else android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFFCF6.toInt()); cornerRadius = dp(20).toFloat()
                setStroke(dp(2), 0xFFF0C08A.toInt(), dp(7).toFloat(), dp(5).toFloat())
            }
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
        // Status, top right: green "Đã ghi" (with the photo count) or orange "Chưa ghi".
        val photoCount = item.optInt("photoCount")
        val status = if (done) "Đã ghi" + (if (photoCount > 0) " · $photoCount ảnh" else "") else "Chưa ghi"
        cover.addView(text(status, 12f, 0xFFFFFFFF.toInt(), true).apply {
            leadingIcon(if (done) com.project.bcv.R.drawable.ic_m3_check else com.project.bcv.R.drawable.ic_m3_edit, 0xFFFFFFFF.toInt(), 13, 4)
            background = rounded(if (done) TEAL else TODO_ORANGE, 12, null, context); setPadding(dp(10), dp(4), dp(10), dp(4))
        }, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { setMargins(0, dp(12), dp(12), 0) })
        if (!done) {
            // Not yet written: a quieter, desaturated cover so the written pages read as the "full" ones.
            image.colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0.25f) })
            image.alpha = 0.75f
        }
        addView(cover, LinearLayout.LayoutParams(-1, dp(128)))
        // Same rule as the page's banner: the place's own image, so a cover never changes with your photos;
        // your first photo only for a place without one; the book icon as a last resort.
        lifecycleScope.launch {
            val place = api.placeImage(item.optString("placeImageUrl"), 2)
            (place ?: item.optString("coverUrl").takeIf { it.isNotBlank() && it != "null" }?.let { url ->
                RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, url, api.photoToken(), 2)
            })?.let {
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
        when {
            // The note as a quoted strip of paper with a gold margin line.
            note.isNotBlank() -> texts.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(android.view.View(context).apply { background = rounded(GOLD, 2, null, context) },
                    LinearLayout.LayoutParams(dp(3), -1))
                addView(text("“${note.trim()}”", 15f, 0xFF4F6559.toInt()).apply {
                    setTypeface(Typeface.SERIF, Typeface.ITALIC); maxLines = 3; ellipsize = TextUtils.TruncateAt.END
                    setLineSpacing(dp(2).toFloat(), 1f); setPadding(dp(12), dp(2), 0, dp(2))
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            done -> texts.addView(text("Có $photoCount ảnh · chưa viết cảm nhận", 13f, MUTED).apply { setPadding(0, dp(10), 0, 0) })
            else -> texts.addView(text("Ghi lại kỷ niệm", 14f, TODO_ORANGE, true).leadingIcon(com.project.bcv.R.drawable.ic_m3_edit, TODO_ORANGE, 17).apply {
                gravity = Gravity.CENTER
                background = rounded(0xFFFCEEDD.toInt(), 14, null, context); setPadding(dp(14), dp(9), dp(14), dp(9))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        }
        addView(texts)

        contentDescription = "${item.getString("name")}, ${if (done) "đã ghi" else "chưa ghi"}"
        setOnClickListener {
            startActivity(Intent(this@JournalActivity, JournalEntryActivity::class.java)
                .putExtra(JournalEntryActivity.EXTRA_PLACE_ID, item.getLong("placeId"))
                .putExtra(JournalEntryActivity.EXTRA_PLACE_NAME, item.getString("name")))
        }
    }

    companion object {
        private var cached: JSONArray? = null
        private var filter = 0
        private const val FILTER_ALL = 0
        private const val FILTER_TODO = 1
        private const val FILTER_DONE = 2
        private const val TODO_ORANGE = 0xFFD9822B.toInt()
    }
}
