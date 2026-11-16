package com.project.bcv.feature.journey.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.project.bcv.feature.discovery.domain.ExploreJourney

/** Visual keepsake derived only from server-confirmed progress; never grants rewards. */
class JourneyMilestoneImage @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bitmap: Bitmap? = null
    private var reached = emptyList<Int>()
    private var destination = ""
    fun show(journey: ExploreJourney) {
        reached = journey.reachedMilestones
        destination = journey.destinationName
        describe()
        invalidate()
    }
    fun setPhoto(photo: Bitmap?) {
        bitmap = photo
        describe()
        invalidate()
    }
    private fun describe() {
        contentDescription = "Ảnh hành trình $destination: đã mở ${reached.size}/4 mảnh."
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val photo = bitmap
        val density = resources.displayMetrics.density
        for (part in 0..3) {
            val column = part % 2
            val row = part / 2
            val tile = RectF(column * width / 2f, row * height / 2f, (column + 1) * width / 2f, (row + 1) * height / 2f)
            paint.color = Color.rgb(227, 237, 228)
            canvas.drawRect(tile, paint)
            if (photo != null && (part + 1) * 25 in reached) {
                val scale = maxOf(width.toFloat() / photo.width, height.toFloat() / photo.height)
                val cropWidth = width / scale
                val cropHeight = height / scale
                val left = (photo.width - cropWidth) / 2f
                val top = (photo.height - cropHeight) / 2f
                val source = Rect((left + column * cropWidth / 2).toInt(), (top + row * cropHeight / 2).toInt(),
                    (left + (column + 1) * cropWidth / 2).toInt(), (top + (row + 1) * cropHeight / 2).toInt())
                canvas.drawBitmap(photo, source, tile, paint)
            } else {
                paint.color = Color.rgb(45, 93, 75)
                paint.textSize = 18 * density
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("${if ((part + 1) * 25 in reached) "✓ " else ""}${(part + 1) * 25}%", tile.centerX(), tile.centerY() + 6 * density, paint)
            }
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2 * density
            canvas.drawRect(tile, paint)
            paint.style = Paint.Style.FILL
        }
    }
}
