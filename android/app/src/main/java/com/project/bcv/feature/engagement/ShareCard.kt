package com.project.bcv.feature.engagement

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/** F89: draws a 1080×1350 share card on the phone, so photos are shared without a public upload. */
object ShareCard {
    data class Content(val placeName: String, val region: String, val steps: Int?, val days: Int?,
        val visited: Boolean, val note: String, val photo: Bitmap?, val date: LocalDate = LocalDate.now())

    private const val W = 1080
    private const val H = 1350

    fun render(context: Context, content: Content): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Background: the user's photo (centre-cropped) or the brand gradient.
        val photo = content.photo
        if (photo != null) {
            val scale = maxOf(W.toFloat() / photo.width, H.toFloat() / photo.height)
            val w = photo.width * scale; val h = photo.height * scale
            canvas.drawBitmap(photo, null, RectF((W - w) / 2, (H - h) / 2, (W + w) / 2, (H + h) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            paint.shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), 0xFF07836D.toInt(), 0xFF19473B.toInt(), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
            paint.shader = null
        }
        paint.shader = LinearGradient(0f, H * 0.35f, 0f, H.toFloat(), 0x00000000, 0xE6102E25.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
        paint.shader = null

        val white = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        // Brand pill
        paint.color = 0x33FFFFFF
        canvas.drawRoundRect(RectF(60f, 60f, 470f, 130f), 35f, 35f, paint)
        white.textSize = 34f; white.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("BƯỚC CHÂN VIỆT", 92f, 108f, white)

        // Stamp
        val gold = 0xFFE6B54A.toInt()
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 8f; paint.color = if (content.visited) gold else 0xFFFFFFFF.toInt()
        canvas.drawCircle(W - 170f, 190f, 105f, paint)
        paint.style = Paint.Style.FILL
        val stamp = TextPaint(white).apply { textAlign = Paint.Align.CENTER; color = paint.color; textSize = 30f }
        canvas.drawText(if (content.visited) "★ ĐẾN" else "ĐÃ", W - 170f, 180f, stamp)
        canvas.drawText(if (content.visited) "TẬN NƠI" else "KHÁM PHÁ", W - 170f, 222f, stamp)

        // Title block, drawn bottom-up
        var y = H - 90f
        val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
        val stats = listOfNotNull(content.steps?.let { "${number.format(it)} bước" }, content.days?.let { "$it ngày" },
            content.date.let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) }).joinToString("  ·  ")
        white.typeface = Typeface.DEFAULT; white.textSize = 36f; white.color = 0xDDFFFFFF.toInt()
        canvas.drawText(stats, 70f, y, white)
        y -= 70f
        if (content.note.isNotBlank()) {
            val notePaint = TextPaint(white).apply { textSize = 38f; typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); color = 0xFFFFFFFF.toInt() }
            val layout = StaticLayout.Builder.obtain("“${content.note.take(140)}”", 0, minOf(content.note.length, 140) + 2, notePaint, W - 140)
                .setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            y -= layout.height
            canvas.save(); canvas.translate(70f, y); layout.draw(canvas); canvas.restore()
            y -= 30f
        }
        white.textSize = 40f; white.color = gold; white.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(content.region.uppercase(Locale.forLanguageTag("vi-VN")), 70f, y, white)
        y -= 64f
        val title = TextPaint(white).apply { textSize = 88f; color = 0xFFFFFFFF.toInt() }
        val titleLayout = StaticLayout.Builder.obtain(content.placeName, 0, content.placeName.length, title, W - 140)
            .setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        y -= titleLayout.height
        canvas.save(); canvas.translate(70f, y); titleLayout.draw(canvas); canvas.restore()
        return bitmap
    }

    /** Writes to app cache and exposes it through the FileProvider; no storage permission on any API level. */
    fun save(context: Context, bitmap: Bitmap): Uri? {
        val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = java.io.File(dir, "bcv_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    }
}
