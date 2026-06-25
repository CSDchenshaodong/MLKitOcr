package com.example.mlkitocr.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class IdCardOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3000000")
        style = Paint.Style.FILL
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4DD0E1")
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 4f
    }

    val framingRectFraction: RectF
        get() {
            val frameWidth = width * 0.84f
            val frameHeight = frameWidth / 1.58f
            val left = (width - frameWidth) / 2f
            val top = (height - frameHeight) / 2f
            return RectF(
                left / width,
                top / height,
                (left + frameWidth) / width,
                (top + frameHeight) / height
            )
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val frame = actualFrame()
        val overlayPath = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addRoundRect(frame, 28f, 28f, Path.Direction.CW)
        }
        canvas.drawPath(overlayPath, scrimPaint)
        canvas.drawRoundRect(frame, 28f, 28f, framePaint)
        drawCorners(canvas, frame)
    }

    private fun drawCorners(canvas: Canvas, frame: RectF) {
        val corner = min(frame.width(), frame.height()) * 0.12f
        canvas.drawLine(frame.left, frame.top, frame.left + corner, frame.top, cornerPaint)
        canvas.drawLine(frame.left, frame.top, frame.left, frame.top + corner, cornerPaint)

        canvas.drawLine(frame.right, frame.top, frame.right - corner, frame.top, cornerPaint)
        canvas.drawLine(frame.right, frame.top, frame.right, frame.top + corner, cornerPaint)

        canvas.drawLine(frame.left, frame.bottom, frame.left + corner, frame.bottom, cornerPaint)
        canvas.drawLine(frame.left, frame.bottom, frame.left, frame.bottom - corner, cornerPaint)

        canvas.drawLine(frame.right, frame.bottom, frame.right - corner, frame.bottom, cornerPaint)
        canvas.drawLine(frame.right, frame.bottom, frame.right, frame.bottom - corner, cornerPaint)
    }

    private fun actualFrame(): RectF {
        val normalized = framingRectFraction
        return RectF(
            normalized.left * width,
            normalized.top * height,
            normalized.right * width,
            normalized.bottom * height
        )
    }
}
