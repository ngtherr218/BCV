package com.project.bcv.feature.discovery.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF

/**
 * Photo pin: a rounded photo frame whose pointer tip sits exactly on the place (anchor = bottom).
 * Discovered: full colour, teal frame, ✓ badge. Not yet: the same photo faded to black and white,
 * grey frame, 🔒 badge. Without a photo the frame holds a plain tinted card.
 */
internal object PlacePhotoMarker {
    /** Full-size pin in dp; the tap target is centred [CENTER_LIFT_DP] above the tip. */
    const val WIDTH_DP = 54f
    const val HEIGHT_DP = 62f
    const val CENTER_LIFT_DP = 34f
    private const val FRAME_DP = 46f
    private const val POINTER_DP = 9f
    private const val TEAL = 0xFF07836D.toInt()
    private const val GREY = 0xFF8E9A93.toInt()

    fun render(source: Bitmap?, discovered: Boolean, density: Float): Bitmap {
        val s = density
        val width = (WIDTH_DP * s).toInt(); val height = (HEIGHT_DP * s).toInt()
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val ring = if (discovered) TEAL else GREY
        val frame = FRAME_DP * s
        val left = (width - frame) / 2f; val top = 4 * s
        val box = RectF(left, top, left + frame, top + frame)
        val tipY = top + frame + POINTER_DP * s

        // Soft shadow, then frame with its pointer.
        paint.color = 0x33000000
        canvas.drawRoundRect(RectF(box.left + s, box.top + 2 * s, box.right + s, box.bottom + 2 * s), 14 * s, 14 * s, paint)
        paint.color = ring
        canvas.drawRoundRect(box, 14 * s, 14 * s, paint)
        canvas.drawPath(Path().apply {
            moveTo(width / 2f - 7 * s, box.bottom - 2 * s); lineTo(width / 2f, tipY); lineTo(width / 2f + 7 * s, box.bottom - 2 * s); close()
        }, paint)

        // Photo (or plain card) inside a white rim.
        val inner = RectF(box.left + 3 * s, box.top + 3 * s, box.right - 3 * s, box.bottom - 3 * s)
        paint.color = 0xFFFFFFFF.toInt(); canvas.drawRoundRect(inner, 11 * s, 11 * s, paint)
        val photo = RectF(inner.left + 1.5f * s, inner.top + 1.5f * s, inner.right - 1.5f * s, inner.bottom - 1.5f * s)
        val clip = canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(photo, 9.5f * s, 9.5f * s, Path.Direction.CW) })
        if (source != null) {
            val side = minOf(source.width, source.height)
            val crop = Rect((source.width - side) / 2, (source.height - side) / 2, (source.width + side) / 2, (source.height + side) / 2)
            if (!discovered) paint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            canvas.drawBitmap(source, crop, photo, paint)
            paint.colorFilter = null
            if (!discovered) { paint.color = 0x4DFFFFFF; canvas.drawRect(photo, paint) }
        } else {
            paint.color = if (discovered) 0xFFDCEFE8.toInt() else 0xFFEDF0EE.toInt(); canvas.drawRect(photo, paint)
            paint.color = ring; canvas.drawCircle(photo.centerX(), photo.centerY(), 5 * s, paint)
        }
        canvas.restoreToCount(clip)

        // Corner badge: ✓ discovered, lock not yet.
        val bx = box.right - 2 * s; val by = box.top + 2 * s; val r = 8.5f * s
        paint.color = 0xFFFFFFFF.toInt(); canvas.drawCircle(bx, by, r, paint)
        paint.color = if (discovered) TEAL else 0xFF6F7D76.toInt(); canvas.drawCircle(bx, by, r - 1.8f * s, paint)
        paint.color = 0xFFFFFFFF.toInt()
        if (discovered) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2 * s
            paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
            canvas.drawPath(Path().apply { moveTo(bx - 3.2f * s, by); lineTo(bx - s, by + 2.2f * s); lineTo(bx + 3.4f * s, by - 2.2f * s) }, paint)
        } else {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.6f * s
            canvas.drawArc(RectF(bx - 2.6f * s, by - 5f * s, bx + 2.6f * s, by + 0.5f * s), 180f, 180f, false, paint)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(bx - 3.8f * s, by - 1.6f * s, bx + 3.8f * s, by + 3.8f * s), 1.2f * s, 1.2f * s, paint)
        }
        paint.style = Paint.Style.FILL
        return result
    }
}
