package com.project.bcv.feature.discovery.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF

/** Locked previews never share the clear thumbnail or badge of a discovered place. */
internal object PlacePhotoMarker {
    fun render(source: Bitmap, discovered: Boolean, density: Float): Bitmap {
        val size = (44 * density).toInt().coerceAtLeast(44)
        val scale = size / 44f
        val center = size / 2f
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.color = if (discovered) 0xFFEE8F3A.toInt() else 0xFFB54B43.toInt()
        canvas.drawCircle(center, center, center, paint)
        val clipped = canvas.save()
        canvas.clipPath(Path().apply { addCircle(center, center, center - 3 * scale, Path.Direction.CW) })
        val preview = if (discovered) source else Bitmap.createScaledBitmap(source, 4, 4, true)
        val side = minOf(preview.width, preview.height)
        val crop = Rect((preview.width-side)/2, (preview.height-side)/2, (preview.width+side)/2, (preview.height+side)/2)
        canvas.drawBitmap(preview, crop, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
        if (!discovered) { paint.color = 0x66000000; canvas.drawCircle(center, center, center, paint) }
        canvas.restoreToCount(clipped)
        if (!discovered) {
            if (preview !== source) preview.recycle()
            paint.color = -1; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2 * scale
            canvas.drawArc(RectF(center-5*scale, center-10*scale, center+5*scale, center+2*scale), 180f, 180f, false, paint)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(center-8*scale, center-4*scale, center+8*scale, center+8*scale), 2*scale, 2*scale, paint)
            paint.color = 0xFF7F4942.toInt(); canvas.drawCircle(center, center+scale, 1.5f*scale, paint)
        } else {
            val x = size - 9 * scale; val y = size - 9 * scale
            paint.color = -1; canvas.drawCircle(x, y, 8*scale, paint)
            paint.color = 0xFF07836D.toInt(); canvas.drawCircle(x, y, 6.5f*scale, paint)
            paint.color = -1; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2*scale
            paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
            canvas.drawPath(Path().apply { moveTo(x-3*scale,y); lineTo(x-scale,y+2*scale); lineTo(x+3*scale,y-2*scale) }, paint)
        }
        return result
    }
}
