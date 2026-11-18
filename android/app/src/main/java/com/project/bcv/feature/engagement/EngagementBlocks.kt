package com.project.bcv.feature.engagement

import com.project.bcv.feature.engagement.EngagementUi.leadingIcon
import com.project.bcv.feature.engagement.EngagementUi.icon
import android.app.Activity
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.core.ui.BottomNav
import com.project.bcv.core.ui.NavTab
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.GOLD
import com.project.bcv.feature.engagement.EngagementUi.GOLD_SOFT
import com.project.bcv.feature.engagement.EngagementUi.LINE
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.TEAL
import com.project.bcv.feature.engagement.EngagementUi.card
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.SOFT
import com.project.bcv.feature.engagement.EngagementUi.outlinedButton
import com.project.bcv.feature.engagement.EngagementUi.pill
import com.project.bcv.feature.engagement.EngagementUi.popIn
import com.project.bcv.feature.engagement.EngagementUi.cardDialog
import com.project.bcv.feature.engagement.EngagementUi.full
import com.project.bcv.feature.engagement.EngagementUi.gap
import com.project.bcv.feature.engagement.EngagementUi.number
import com.project.bcv.feature.engagement.EngagementUi.progress
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.sectionTitle
import com.project.bcv.feature.engagement.EngagementUi.text
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Blocks reused across the reorganised tabs: Thử thách (streak, badges) and Bộ sưu tập (passport). */
internal object EngagementBlocks {

    // ---------- Bộ sưu tập: Thẻ · Hộ chiếu · Nhật ký ----------
    val COLLECTION_PAGES = listOf("cards" to "Thẻ", "passport" to "Hộ chiếu", "journal" to "Nhật ký")

    /** Switches between the three collection pages in place (same tab, no slide). */
    fun collectionSwitcher(activity: Activity, selected: String) =
        LeaderboardViews.segmented(activity, selected, COLLECTION_PAGES) { key ->
            val target = when (key) {
                "passport" -> PassportActivity::class.java
                "journal" -> JournalActivity::class.java
                else -> com.project.bcv.feature.collection.presentation.CollectionActivity::class.java
            }
            BottomNav.switchWithinTab(activity, NavTab.COLLECTION, target)
        }

    // ---------- Thử thách: today's steps and streak ----------
    fun streakCard(activity: AppCompatActivity, streak: JSONObject, reload: () -> Unit): LinearLayout = with(activity) {
        val goal = streak.getInt("dailyGoal")
        val today = streak.getInt("todaySteps")
        val box = card()
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(text("${streak.getInt("current")} ngày liên tiếp", 17f, FOREST, true)
            .leadingIcon(com.project.bcv.R.drawable.ic_m3_local_fire_department, 0xFFD9782B.toInt(), 20), LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(top)
        box.full(text("Hôm nay ${number(today)} / ${number(goal)} bước", 13f, MUTED).apply {
            if (streak.getBoolean("todayDone")) leadingIcon(com.project.bcv.R.drawable.ic_m3_check_circle, TEAL, 15)
        }, 6)
        box.full(progress(today, goal), 8)
        box.gap(12)
        val week = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val days = streak.getJSONArray("week")
        for (i in 0 until days.length()) {
            val day = days.getJSONObject(i)
            val date = LocalDate.parse(day.getString("date"))
            val cell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            // Reached: teal with a tick. Missed (a past day below the goal): red with a cross. Otherwise grey.
            val met = day.getBoolean("met"); val missed = day.optBoolean("missed")
            cell.addView(android.widget.FrameLayout(this).apply {
                background = rounded(when { met -> TEAL; missed -> MISSED_RED; else -> LINE }, 14, null, context)
                contentDescription = "${weekday(date)}: ${number(day.getInt("steps"))} bước" +
                    when { met -> ", đạt mục tiêu"; missed -> ", lỡ chuỗi"; else -> "" }
                if (met || missed) addView(icon(if (met) com.project.bcv.R.drawable.ic_m3_check else com.project.bcv.R.drawable.ic_m3_close,
                    0xFFFFFFFF.toInt(), 16), android.widget.FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
            }, LinearLayout.LayoutParams(dp(28), dp(28)))
            cell.addView(text(weekday(date), 11f, if (missed) MISSED_RED else MUTED, missed).apply { gravity = Gravity.CENTER })
            week.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
        }
        box.addView(week)
        box
    }

    private const val MISSED_RED = 0xFFD9534F.toInt()

    // ---------- Thử thách: badges ----------

    private fun badgeImage(badge: JSONObject) = badge.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" }

    private fun share(badge: JSONObject) = badge.getString("progress").split("/")
        .let { (a, b) -> a.toDouble() / b.toDouble().coerceAtLeast(1.0) }

    /**
     * Every badge as a round medal in a 3-column grid: earned ones in colour with a gold ring,
     * locked ones faded with their progress, closest first. Tap a medal for what it takes.
     */
    fun badges(activity: Activity, container: LinearLayout, badges: JSONArray) = with(activity) {
        val all = (0 until badges.length()).map(badges::getJSONObject)
        val earned = all.count { it.getBoolean("earned") }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(24), 0, dp(4)) }
        head.addView(text("Huy hiệu", 17f, FOREST, true), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(text("$earned/${all.size} đã đạt", 13f, MUTED))
        container.addView(head)
        val sorted = all.sortedWith(compareByDescending<JSONObject> { it.getBoolean("earned") }.thenByDescending { share(it) })
        val grid = GridLayout(this).apply { columnCount = 3 }
        sorted.forEach { badge ->
            grid.addView(medal(activity, badge), GridLayout.LayoutParams().apply {
                width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(2), dp(10), dp(2), dp(2))
            })
        }
        container.full(grid, 2)
    }

    private fun medal(activity: Activity, badge: JSONObject): LinearLayout = with(activity) {
        val earned = badge.getBoolean("earned")
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            contentDescription = "${badge.getString("name")}, ${if (earned) "đã đạt" else "tiến độ ${badge.getString("progress")}"}"
            val circle = android.widget.FrameLayout(context).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (earned) GOLD_SOFT else 0xFFF1F4F2.toInt())
                    setStroke(dp(if (earned) 3 else 2), if (earned) GOLD else LINE)
                }
            }
            // Placeholder while the art loads (or if a badge has none): a plain star in the ring.
            val emoji = icon(com.project.bcv.R.drawable.ic_m3_workspace_premium, if (earned) GOLD else LINE, 28)
            circle.addView(emoji, android.widget.FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER))
            // Badge art from the admin web: a round picture inside the ring; locked ones in faded grey.
            badgeImage(badge)?.let { url ->
                val ring = dp(if (earned) 3 else 2)
                val image = android.widget.ImageView(context).apply {
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL }
                    clipToOutline = true; visibility = View.INVISIBLE
                    if (!earned) {
                        colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
                        alpha = 0.45f
                    }
                }
                circle.addView(image, 1, android.widget.FrameLayout.LayoutParams(-1, -1).apply { setMargins(ring, ring, ring, ring) })
                (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                    EngagementApi(activity).placeImage(url, 2)?.let {
                        image.setImageBitmap(it); image.visibility = View.VISIBLE; emoji.visibility = View.INVISIBLE
                    }
                }
            }
            if (!earned) circle.addView(android.widget.FrameLayout(context).apply {
                background = rounded(0xFFFFFFFF.toInt(), 10, LINE, context)
                addView(icon(com.project.bcv.R.drawable.ic_m3_lock, MUTED, 12), android.widget.FrameLayout.LayoutParams(dp(12), dp(12), Gravity.CENTER))
            }, android.widget.FrameLayout.LayoutParams(dp(20), dp(20), Gravity.BOTTOM or Gravity.END))
            addView(circle, LinearLayout.LayoutParams(dp(64), dp(64)))
            addView(text(badge.getString("name"), 12f, if (earned) FOREST else MUTED, earned).apply {
                gravity = Gravity.CENTER; maxLines = 2; minLines = 2; setPadding(dp(2), dp(6), dp(2), 0)
            }, LinearLayout.LayoutParams(-1, -2))
            if (!earned) {
                addView(progress(badge.getString("progress").substringBefore("/").toIntOrNull() ?: 0,
                    badge.getString("progress").substringAfter("/").toIntOrNull() ?: 1),
                    LinearLayout.LayoutParams(dp(56), dp(4)).apply { topMargin = dp(4) })
                addView(text(badge.getString("progress"), 11f, MUTED).apply { gravity = Gravity.CENTER })
            }
            setOnClickListener { showBadge(activity, badge) }
        }
    }

    /** Badge detail: the medal large, what it takes, and either "Đã đạt" or how far along you are. */
    private fun showBadge(activity: Activity, badge: JSONObject) = with(activity) {
        val earned = badge.getBoolean("earned")
        val (value, target) = badge.getString("progress").split("/").let { (a, b) -> (a.toIntOrNull() ?: 0) to (b.toIntOrNull() ?: 1) }
        cardDialog { dialog ->
            val ring = dp(if (earned) 5 else 3)
            val medal = android.widget.FrameLayout(context).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (earned) GOLD_SOFT else 0xFFF1F4F2.toInt()); setStroke(ring, if (earned) GOLD else LINE)
                }
                val placeholder = icon(com.project.bcv.R.drawable.ic_m3_workspace_premium, if (earned) GOLD else LINE, 48)
                addView(placeholder, android.widget.FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER))
                badgeImage(badge)?.let { url ->
                    val image = android.widget.ImageView(context).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL }
                        clipToOutline = true; visibility = View.INVISIBLE
                        if (!earned) {
                            colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
                            alpha = 0.5f
                        }
                    }
                    addView(image, android.widget.FrameLayout.LayoutParams(-1, -1).apply { setMargins(ring, ring, ring, ring) })
                    (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                        EngagementApi(activity).placeImage(url, 1)?.let { image.setImageBitmap(it); image.visibility = View.VISIBLE; placeholder.visibility = View.INVISIBLE }
                    }
                }
            }
            addView(medal, LinearLayout.LayoutParams(dp(128), dp(128)))
            medal.popIn()
            addView(text(badge.getString("name"), 20f, FOREST, true).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            addView(text(badge.getString("description"), 15f, MUTED).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
            if (earned) {
                addView(pill("Đã đạt", 0xFF8A6418.toInt(), GOLD_SOFT).apply {
                    textSize = 13f; setPadding(dp(14), dp(6), dp(14), dp(6))
                    leadingIcon(com.project.bcv.R.drawable.ic_m3_check_circle, 0xFF8A6418.toInt(), 16, 6)
                }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(16) })
            } else {
                val box = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    background = rounded(SOFT, 16, null, context); setPadding(dp(14), dp(12), dp(14), dp(12))
                }
                val headRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                headRow.addView(text("Tiến độ", 13f, MUTED), LinearLayout.LayoutParams(0, -2, 1f))
                headRow.addView(text("${number(value)} / ${number(target)}", 15f, FOREST, true))
                box.addView(headRow)
                box.addView(progress(value, target), LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(8) })
                box.addView(text("Còn ${number((target - value).coerceAtLeast(0))} nữa để đạt huy hiệu này.", 13f, MUTED),
                    LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
                addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
            }
            addView(outlinedButton("Đóng").apply { setOnClickListener { dialog.dismiss() } },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        }.show()
    }

    // ---------- Bộ sưu tập: passport ----------
    private data class Tier(val label: String, val ink: Int, val fill: Int, val ring: Int)
    private val TIERS = mapOf(
        "GOLD" to Tier("Vàng", 0xFF8A6418.toInt(), 0xFFFFF3D2.toInt(), 0xFFD9A93A.toInt()),
        "SILVER" to Tier("Bạc", 0xFF4F5F73.toInt(), 0xFFEFF3F7.toInt(), 0xFF9EAFC2.toInt()),
        "BRONZE" to Tier("Đồng", 0xFF8A4A2E.toInt(), 0xFFFBEDE5.toInt(), 0xFFCB8A68.toInt()),
        "NONE" to Tier("Chưa đến", 0xFFA6B2AB.toInt(), 0x00FFFFFF, 0xFFC9D3CC.toInt()),
    )
    private const val COVER = 0xFF173F33.toInt()
    private const val COVER_GOLD = 0xFFE6C36A.toInt()

    /**
     * Real footsteps only: each GPS check-in stamps one place. Styled as a passport: a cover with the
     * totals, then each region's provinces as round stamps that rise bronze (1 place) → silver
     * (half) → gold (all). Tap a stamp for the province's places.
     */
    fun passport(activity: Activity, container: LinearLayout, passport: JSONObject) = with(activity) {
        container.full(cover(this, passport), 4)
        val zones = passport.getJSONArray("zones")
        // Stamp marks must be unique: "Lai Châu" and "Lào Cai" both start with "LC".
        val names = (0 until zones.length()).flatMap { z -> zones.getJSONObject(z).getJSONArray("provinces")
            .let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("name") } } }
        val clash = names.groupBy(::initials).filterValues { it.size > 1 }.values.flatten().toSet()
        marks = names.associateWith { if (it in clash) longInitials(it) else initials(it) }
        for (z in 0 until zones.length()) {
            val zone = zones.getJSONObject(z)
            val provinces = zone.getJSONArray("provinces")
            val count = (0 until provinces.length()).count { provinces.getJSONObject(it).getInt("visited") > 0 }
            val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            head.addView(text(zone.getString("name"), 15f, FOREST, true), LinearLayout.LayoutParams(0, -2, 1f))
            head.addView(text("$count/${provinces.length()}", 13f, if (count > 0) TEAL else MUTED, true))
            container.full(head, 24)
            container.full(android.view.View(this).apply { setBackgroundColor(LINE) }.also {
                it.layoutParams = LinearLayout.LayoutParams(-1, dp(1)) }, 8)
            val grid = GridLayout(this).apply { columnCount = 3 }
            for (i in 0 until provinces.length()) {
                grid.addView(stampCell(activity, provinces.getJSONObject(i)), GridLayout.LayoutParams().apply {
                    width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(2), dp(10), dp(2), dp(2))
                })
            }
            container.full(grid, 2)
        }
    }

    /** Passport cover: dark green, gold lettering, the three totals and the tier key. */
    private fun cover(context: Activity, passport: JSONObject): LinearLayout = with(context) {
        val stamped = passport.getInt("stamped"); val total = passport.getInt("total")
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(COVER, 24, null, context)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            addView(text("HỘ CHIẾU", 11f, COVER_GOLD, true).apply { letterSpacing = 0.25f })
            addView(text("Dấu chân Việt", 26f, 0xFFFFFFFF.toInt(), true))
            val stats = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(14), 0, dp(12)) }
            listOf(
                "$stamped/$total" to "tỉnh có dấu",
                "${passport.optInt("placesStamped")}" to "địa danh đã đến",
                "${passport.optInt("gold")}" to "tỉnh hạng vàng",
            ).forEach { (value, label) ->
                stats.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(text(value, 20f, COVER_GOLD, true))
                    addView(text(label, 12f, 0xCCFFFFFF.toInt()))
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            addView(stats)
            addView(progress(stamped, total, COVER_GOLD).apply {
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0x33FFFFFF)
            }, LinearLayout.LayoutParams(-1, dp(6)))
            val key = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(14), 0, 0) }
            listOf("BRONZE" to "1 nơi", "SILVER" to "½", "GOLD" to "đủ").forEach { (code, hint) ->
                val tier = TIERS.getValue(code)
                key.addView(android.view.View(context).apply {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(tier.ring) }
                }, LinearLayout.LayoutParams(dp(10), dp(10)))
                key.addView(text("${tier.label} $hint", 12f, 0xCCFFFFFF.toInt()).apply { setPadding(dp(6), 0, dp(14), 0); maxLines = 1 })
            }
            addView(key)
            addView(text("Check-in khi đứng tại địa danh để đóng dấu.", 12f, 0x99FFFFFF.toInt()).apply { setPadding(0, dp(8), 0, 0) })
        }
    }

    private fun stampCell(activity: Activity, p: JSONObject): LinearLayout = with(activity) {
        val visited = p.getInt("visited"); val total = p.getInt("total")
        val tier = TIERS[p.optString("tier")] ?: TIERS.getValue("NONE")
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            background = android.util.TypedValue().let { tv ->
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)
                androidx.core.content.ContextCompat.getDrawable(context, tv.resourceId)
            }
            contentDescription = "${p.getString("name")}: $visited trên $total địa danh, hạng ${tier.label}"
            addView(StampView(context, marks[p.getString("name")] ?: initials(p.getString("name")), "$visited/$total", tier,
                tilt = if (visited > 0) ((p.getInt("provinceId") * 37) % 13 - 6).toFloat() else 0f),
                LinearLayout.LayoutParams(dp(78), dp(78)))
            addView(text(p.getString("name"), 12f, if (visited > 0) FOREST else MUTED, visited > 0).apply {
                gravity = Gravity.CENTER; maxLines = 2; setPadding(0, dp(6), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
            setOnClickListener { showProvince(activity, p) }
        }
    }

    private var marks: Map<String, String> = emptyMap()

    /** "Lai Châu" → "LCH", "Lào Cai" → "LCA": first letter, then the last word's first two. */
    private fun longInitials(name: String): String {
        val words = name.split(" ").filter { it.isNotBlank() }
        return (words.first().take(1) + words.last().take(2)).uppercase()
    }

    /** "Hà Nội" → "HN", "TP. Hồ Chí Minh" → "HCM", "Huế" → "HUẾ". */
    private fun initials(name: String): String {
        val words = name.removePrefix("TP.").trim().split(" ").filter { it.isNotBlank() }
        return if (words.size == 1) words[0].uppercase().take(4) else words.joinToString("") { it.take(1) }.uppercase()
    }

    /** Round ink stamp: double ring and a slight tilt once stamped, a dashed outline while empty. */
    private class StampView(context: android.content.Context, private val mark: String, private val count: String,
                            private val tier: Tier, tilt: Float) : android.view.View(context) {
        private val d = context.resources.displayMetrics.density
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private val stamped = tier.fill != 0x00FFFFFF
        init { rotation = tilt }

        override fun onDraw(canvas: android.graphics.Canvas) {
            val cx = width / 2f; val cy = height / 2f; val r = minOf(cx, cy) - 2 * d
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = if (stamped) tier.fill else 0xFFFFFFFF.toInt()
            canvas.drawCircle(cx, cy, r, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = tier.ring
            paint.strokeWidth = 2.5f * d
            paint.pathEffect = if (stamped) null else android.graphics.DashPathEffect(floatArrayOf(5 * d, 4 * d), 0f)
            canvas.drawCircle(cx, cy, r, paint)
            if (stamped) {
                paint.strokeWidth = 1 * d
                canvas.drawCircle(cx, cy, r - 5 * d, paint)
            }
            paint.pathEffect = null
            paint.style = android.graphics.Paint.Style.FILL
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.color = tier.ink
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.textSize = (if (mark.length > 3) 15 else 19) * d
            canvas.drawText(mark, cx, cy + 2 * d, paint)
            paint.typeface = android.graphics.Typeface.DEFAULT
            paint.textSize = 11 * d
            canvas.drawText(count, cx, cy + 17 * d, paint)
        }
    }

    /** The province's places: stamped ones with their first check-in date, the rest still to visit. */
    private fun showProvince(activity: Activity, p: JSONObject) = with(activity) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(28)) }
        val tier = TIERS[p.optString("tier")] ?: TIERS.getValue("NONE")
        body.addView(text(p.getString("name"), 20f, FOREST, true))
        body.full(text("${p.getInt("visited")}/${p.getInt("total")} địa danh đã check-in · hạng ${tier.label}", 13f,
            if (p.getInt("visited") > 0) tier.ink else MUTED), 2)
        val places = p.getJSONArray("places")
        for (i in 0 until places.length()) {
            val place = places.getJSONObject(i)
            val on = place.getBoolean("stamped")
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = rounded(if (on) 0xFFF0F8F5.toInt() else 0xFFFFFFFF.toInt(), 14, if (on) 0xFFBFE0D5.toInt() else LINE, context)
            }
            row.addView(icon(if (on) com.project.bcv.R.drawable.ic_m3_check_circle else com.project.bcv.R.drawable.ic_circle_outline, if (on) TEAL else 0xFFB8C4BD.toInt(), 20),
                LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(6) })
            row.addView(text(place.getString("name"), 15f, FOREST, on).apply { maxLines = 2 }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(text(when {
                on -> formatDate(place.getString("date"))
                else -> "Chưa đến"
            }, 12f, if (on) TEAL else MUTED))
            body.full(row, 8)
        }
        dialog.setContentView(android.widget.ScrollView(this).apply { addView(body) })
        dialog.show()
    }

    private fun weekday(date: LocalDate) = when (date.dayOfWeek.value) { 7 -> "CN"; else -> "T${date.dayOfWeek.value + 1}" }
    private fun formatDate(iso: String) = LocalDate.parse(iso).let { "%02d/%02d".format(it.dayOfMonth, it.monthValue) }
}
