package com.project.bcv.feature.engagement

import android.content.Intent
import android.graphics.Typeface
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import com.project.bcv.feature.engagement.EngagementUi.TEAL
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.BuildConfig
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.GOLD
import com.project.bcv.feature.engagement.EngagementUi.GOLD_SOFT
import com.project.bcv.feature.engagement.EngagementUi.LINE
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.SOFT
import com.project.bcv.feature.engagement.EngagementUi.card
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.full
import com.project.bcv.feature.engagement.EngagementUi.number
import com.project.bcv.feature.engagement.EngagementUi.outlinedButton
import com.project.bcv.feature.engagement.EngagementUi.pill
import com.project.bcv.feature.engagement.EngagementUi.primaryButton
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.scaffold
import com.project.bcv.feature.engagement.EngagementUi.sectionTitle
import com.project.bcv.feature.engagement.EngagementUi.text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

/**
 * F88/F89: one really-visited place's private journal page, opened to read first: cover, the note as a page and
 * the photos. "Sửa" edits the note in place; photos open large (delete there); a share card is
 * generated on the phone for posting outside the app. Nothing is public or moderated.
 */
class JournalEntryActivity : AppCompatActivity() {
    private lateinit var body: LinearLayout
    private lateinit var api: EngagementApi
    private var placeId = 0L
    private var entry: JSONObject? = null
    private val photos = mutableListOf<Pair<Long, Bitmap?>>()
    private var busy = false
    private var data: JSONObject? = null
    private var note = ""
    private var noteSlot: LinearLayout? = null
    private var noteHeader: LinearLayout? = null

    private val picker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::upload) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placeId = intent.getLongExtra(EXTRA_PLACE_ID, 0)
        if (placeId <= 0) { finish(); return }
        api = EngagementApi(this)
        body = scaffold("Nhật ký")
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val result = runCatching { api.journalEntry(placeId) }
            body.removeAllViews()
            result.onFailure {
                body.full(text(it.message ?: "Không tải được nhật ký.", 15f, MUTED), 20)
                body.full(outlinedButton("Thử lại").apply { setOnClickListener { load() } }, 12)
            }.onSuccess { loaded -> data = loaded; render(loaded) }
        }
    }

    private fun render(data: JSONObject) {
        val info = data.getJSONObject("entry").also { entry = it }
        note = info.optString("note")
        body.removeAllViews()
        body.addView(hero(info), LinearLayout.LayoutParams(-1, dp(220)).apply { topMargin = dp(4) })

        // Note: read it like a page; "Sửa" turns it into an editor in place.
        body.full(header("Cảm nhận của bạn", "Sửa") { showEditor() }.also { noteHeader = it }, 22)
        noteSlot = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.full(noteSlot!!, 8)
        showNote()

        // Photos: tap to view large (and delete there); the last tile adds one.
        val list = data.getJSONArray("photos")
        val photoHeader = header("Ảnh của bạn", "${list.length()}/$MAX_PHOTOS", null)
        photoCount = photoHeader.getChildAt(1) as? android.widget.TextView
        body.full(photoHeader, 24)
        photos.clear()
        for (i in 0 until list.length()) photos += list.getJSONObject(i).getLong("photoId") to null
        photoGrid = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        // One row that scrolls sideways: its height never changes, however many photos are added.
        body.addView(android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; clipToPadding = false
            addView(photoGrid)
        }.also { photoStrip = it }, LinearLayout.LayoutParams(-1, dp(PHOTO_DP)).apply { topMargin = dp(8) })
        renderPhotos()
        loadPhotoBitmaps(list)


        body.full(outlinedButton("Chia sẻ trang này").apply { setOnClickListener { share(note) } }, 24)

    }

    /** Cover (own photo, else the place's image) with the place, date and journey facts on it. */
    private fun hero(info: JSONObject): FrameLayout = FrameLayout(this).apply {
        background = rounded(0xFF2E5A4C.toInt(), 22, null, context); clipToOutline = true
        val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        addView(image, FrameLayout.LayoutParams(-1, -1))
        addView(android.view.View(context).apply {
            background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(0xCC0F2A21.toInt(), 0x330F2A21, 0x000F2A21))
        }, FrameLayout.LayoutParams(-1, -1))
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), dp(14)) }
        val date = LocalDateTime.parse(info.getString("discoveredAt").take(19))
        texts.addView(pill("★ Đã khám phá", GOLD, 0xFFFFFFFF.toInt()),
            LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(8) })
        texts.addView(text(info.getString("name"), 26f, 0xFFFFFFFF.toInt(), true).apply { maxLines = 2 })
        val facts = mutableListOf(info.getString("regionName"),
            "đến ngày ${"%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)}")
        info.optInt("checkins").takeIf { it > 1 }?.let { facts += "$it lần khám phá" }
        texts.addView(text(facts.joinToString(" · "), 13f, 0xE6FFFFFF.toInt()).apply { setPadding(0, dp(2), 0, 0) })
        addView(texts, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        lifecycleScope.launch {
            val cover = photosCover() ?: api.placeImage(info.optString("placeImageUrl"), 2)
            cover?.let(image::setImageBitmap)
        }
    }

    private suspend fun photosCover() = data?.getJSONArray("photos")?.optJSONObject(0)?.let {
        RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, it.getString("contentUrl"), api.photoToken(), 2)
    }

    private fun header(title: String, action: String?, onAction: (() -> Unit)?) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(text(title, 17f, FOREST, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (action != null) addView(text(action, 13f, if (onAction != null) TEAL else MUTED, onAction != null).apply {
            if (onAction != null) { isClickable = true; setPadding(dp(10), dp(6), 0, dp(6)); setOnClickListener { onAction() } }
        })
    }

    private fun tile() = GridLayout.LayoutParams().apply {
        width = 0; height = dp(108); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(dp(3), dp(3), dp(3), dp(3))
    }

    /** Read mode: the note as a page, or an invitation to write the first lines. */
    private fun showNote() {
        val slot = noteSlot ?: return
        slot.removeAllViews()
        noteHeader?.getChildAt(1)?.visibility = if (note.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        if (note.isBlank()) {
            slot.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(0xFFFFFFFF.toInt(), 18, 0xFFC9D8CF.toInt(), context)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                addView(text("Bạn chưa viết gì cho trang này.", 15f, FOREST, true))
                addView(text("Hôm ấy bạn thấy gì, ăn gì, đi cùng ai? Vài dòng thôi cũng đủ để sau này đọc lại.", 13f, MUTED).apply {
                    setPadding(0, dp(4), 0, dp(12)) })
                addView(primaryButton("✎  Viết cảm nhận").apply { setOnClickListener { showEditor() } })
            })
            return
        }
        slot.addView(text(note.trim(), 16f, 0xFF35483F.toInt()).apply {
            setTypeface(Typeface.SERIF, Typeface.NORMAL); setLineSpacing(dp(4).toFloat(), 1f)
            background = rounded(0xFFFFFBF3.toInt(), 18, 0xFFF0E4CC.toInt(), context)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setTextIsSelectable(true)
        })
    }

    private fun hideKeyboard(view: android.view.View) =
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)

    /** Full-screen photos: swipe left/right between them; Xoá deletes the one on screen. */
    private fun viewPhoto(photoId: Long) {
        val shown = photos.filter { it.first > 0 }
        if (shown.isEmpty()) return
        val start = shown.indexOfFirst { it.first == photoId }.coerceAtLeast(0)
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val root = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        val counter = text("", 15f, 0xFFFFFFFF.toInt(), true)
        val pager = androidx.viewpager2.widget.ViewPager2(this)
        pager.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun getItemCount() = shown.size
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                object : androidx.recyclerview.widget.RecyclerView.ViewHolder(ImageView(parent.context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams = android.view.ViewGroup.LayoutParams(-1, -1)
                }) {}
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val image = holder.itemView as ImageView
                val (id, bitmap) = shown[position]
                image.setImageBitmap(bitmap)
                if (bitmap == null) lifecycleScope.launch {
                    val url = "/api/journal/photos/$id/content"
                    RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, url, api.photoToken(), 1)?.let(image::setImageBitmap)
                }
            }
        }
        pager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { counter.text = "${position + 1} / ${shown.size}" }
        })
        root.addView(pager, FrameLayout.LayoutParams(-1, -1))
        pager.setCurrentItem(start, false)
        counter.text = "${start + 1} / ${shown.size}"
        root.addView(counter, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(36) })
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(16), dp(16), dp(16), dp(28)) }
        bar.addView(text("Đóng", 15f, 0xFFFFFFFF.toInt(), true).apply {
            isClickable = true; setPadding(dp(8), dp(8), dp(8), dp(8)); setOnClickListener { dialog.dismiss() } },
            LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(text("Xoá ảnh", 15f, 0xFFFF8A80.toInt(), true).apply {
            isClickable = true; setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { dialog.dismiss(); confirmDelete(shown[pager.currentItem].first) } })
        root.addView(bar, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        dialog.setContentView(root)
        dialog.show()
    }

    /**
     * Editor in its own bottom sheet: typing and the keyboard never resize or move the page.
     * Saving only swaps the note's text in place.
     */
    private fun showEditor() {
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        sheet.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(14), dp(20), dp(20)) }
        box.addView(android.view.View(this).apply { background = rounded(0xFFDFE8DD.toInt(), 2, null, context) },
            LinearLayout.LayoutParams(dp(40), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(12) })
        box.addView(text("Cảm nhận của bạn", 20f, FOREST, true))
        box.addView(text(entry?.optString("name").orEmpty(), 13f, MUTED))
        val counter = text("${note.length}/1.000", 12f, MUTED).apply { gravity = Gravity.END }
        val input = EditText(this).apply {
            setText(note); setSelection(note.length); gravity = Gravity.TOP
            minLines = 6; maxLines = 12; isVerticalScrollBarEnabled = true
            filters = arrayOf(InputFilter.LengthFilter(1000)); setTextColor(FOREST); textSize = 16f
            background = rounded(0xFFFFFBF3.toInt(), 16, 0xFFF0E4CC.toInt(), context); setPadding(dp(14), dp(12), dp(14), dp(12))
            hint = "Hôm ấy bạn thấy gì, ăn gì, đi cùng ai?"
            doAfterTextChanged { counter.text = "${it?.length ?: 0}/1.000" }
        }
        box.addView(input, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        box.addView(counter, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10), 0, 0) }
        actions.addView(outlinedButton("Huỷ").apply { setOnClickListener { sheet.dismiss() } },
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(10) })
        actions.addView(primaryButton("Lưu").apply {
            setOnClickListener {
                val value = input.text.toString().trim()
                isEnabled = false
                lifecycleScope.launch {
                    runCatching { api.saveNote(placeId, value) }
                        .onSuccess { note = value; showNote(); sheet.dismiss(); Snackbar.make(body, "Đã lưu vào nhật ký.", Snackbar.LENGTH_SHORT).show() }
                        .onFailure { isEnabled = true; Snackbar.make(box, it.message ?: "Không lưu được.", Snackbar.LENGTH_LONG).show() }
                }
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(actions)
        sheet.setContentView(box)
        sheet.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        sheet.behavior.skipCollapsed = true
        sheet.setOnShowListener {
            input.requestFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        sheet.show()
    }

    // ---------------- Photos, updated in place ----------------
    private var photoGrid: LinearLayout? = null
    private var photoStrip: android.widget.HorizontalScrollView? = null
    private var photoCount: android.widget.TextView? = null

    /** Rebuilds only the photo row from [photos] (newest first, "+" in front); the page never reloads. */
    private fun renderPhotos() {
        val row = photoGrid ?: return
        row.removeAllViews()
        photoCount?.text = "${photos.size}/$MAX_PHOTOS"
        fun params() = LinearLayout.LayoutParams(dp(PHOTO_DP), dp(PHOTO_DP)).apply { marginEnd = dp(10) }
        if (photos.size < MAX_PHOTOS) row.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFC9D8CF.toInt(), context)
            isClickable = true; contentDescription = "Thêm ảnh vào nhật ký"
            addView(text("＋", 28f, TEAL, true).apply { gravity = Gravity.CENTER })
            addView(text("Thêm ảnh", 12f, TEAL, true).apply { gravity = Gravity.CENTER })
            setOnClickListener { if (!busy) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        }, params())
        photos.forEachIndexed { i, (id, bitmap) ->
            row.addView(FrameLayout(this).apply {
                background = rounded(SOFT, 16, null, context); clipToOutline = true
                if (id < 0) {
                    // Still uploading: a spinner holds the photo's place.
                    addView(android.widget.ProgressBar(context).apply { isIndeterminate = true },
                        FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER))
                } else {
                    addView(ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        bitmap?.let(::setImageBitmap)
                    }, FrameLayout.LayoutParams(-1, -1))
                    contentDescription = "Ảnh nhật ký ${i + 1}. Chạm để xem lớn."
                    isClickable = true
                    setOnClickListener { viewPhoto(id) }
                }
            }, params())
        }
    }

    private fun loadPhotoBitmaps(list: org.json.JSONArray) {
        for (i in 0 until list.length()) {
            val photo = list.getJSONObject(i)
            val id = photo.getLong("photoId")
            lifecycleScope.launch {
                RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, photo.getString("contentUrl"), api.photoToken(), 2)?.let { bitmap ->
                    val index = photos.indexOfFirst { it.first == id }
                    if (index >= 0) { photos[index] = id to bitmap; renderPhotos() }
                }
            }
        }
    }

    /** The new photo appears at once as a spinner tile, then as itself — no page reload. */
    private fun upload(uri: Uri) {
        busy = true
        val pending = -System.nanoTime()
        photos.add(0, pending to null)
        renderPhotos()
        photoStrip?.post { photoStrip?.smoothScrollTo(0, 0) }
        lifecycleScope.launch {
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) { compress(uri) } ?: error("Không đọc được ảnh này.")
                val saved = api.addPhoto(placeId, bytes, "image/jpeg")
                val preview = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = 2 })
                }
                saved.getLong("photoId") to preview
            }
            busy = false
            val index = photos.indexOfFirst { it.first == pending }
            result.onSuccess { (id, preview) -> if (index >= 0) photos[index] = id to preview }
                .onFailure {
                    if (index >= 0) photos.removeAt(index)
                    Snackbar.make(body, it.message ?: "Không lưu được ảnh.", Snackbar.LENGTH_LONG).show()
                }
            renderPhotos()
        }
    }

    /** Re-encodes the picked photo as JPEG ≤ 1600 px so uploads stay small and EXIF location data is dropped. */
    private fun compress(uri: Uri): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
        val bitmap = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val scale = minOf(1f, 1600f / maxOf(bitmap.width, bitmap.height))
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
        return ByteArrayOutputStream().use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
    }

    private fun confirmDelete(photoId: Long) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xoá ảnh này khỏi nhật ký?")
            .setMessage("Ảnh sẽ bị xoá vĩnh viễn và không khôi phục được.")
            .setNegativeButton("Giữ lại", null)
            .setPositiveButton("Xoá") { _, _ ->
                lifecycleScope.launch {
                    runCatching { api.deletePhoto(photoId) }.onSuccess { photos.removeAll { it.first == photoId }; renderPhotos() }
                        .onFailure { Snackbar.make(body, it.message ?: "Không xoá được.", Snackbar.LENGTH_LONG).show() }
                }
            }.show()
    }

    private fun share(note: String) {
        val info = entry ?: return
        lifecycleScope.launch {
            // Background: the user's own photo, else the place's official image.
            val background = photos.firstNotNullOfOrNull { it.second } ?: api.placeImage(info.optString("placeImageUrl"), 1)
            val discovered = LocalDateTime.parse(info.getString("discoveredAt").take(19)).toLocalDate()
            val uri = withContext(Dispatchers.IO) {
                runCatching {
                    ShareCard.save(this@JournalEntryActivity, ShareCard.render(this@JournalEntryActivity, ShareCard.Content(
                        placeName = info.getString("name"), region = info.getString("regionName"),
                        steps = null, days = null,
                        visited = info.getBoolean("visited"), note = note.trim(),
                        photo = background, date = discovered)))
                }.getOrNull()
            }
            if (uri == null) { Snackbar.make(body, "Không tạo được thẻ chia sẻ.", Snackbar.LENGTH_LONG).show(); return@launch }
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri)
                // Lets the system share sheet show a preview of the card.
                clipData = android.content.ClipData.newRawUri(info.getString("name"), uri)
                putExtra(Intent.EXTRA_TEXT, "Mình vừa khám phá ${info.getString("name")} cùng Bước Chân Việt!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Chia sẻ thẻ hành trình"))
        }
    }

    companion object {
        private const val MAX_PHOTOS = 12
        private const val PHOTO_DP = 132
        const val EXTRA_PLACE_ID = "journal_place_id"
        const val EXTRA_PLACE_NAME = "journal_place_name"
    }
}
