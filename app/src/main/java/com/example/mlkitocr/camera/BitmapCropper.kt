package com.example.mlkitocr.camera

import android.graphics.Bitmap
import android.graphics.RectF
import kotlin.math.roundToInt

class BitmapCropper {

    fun crop(bitmap: Bitmap, normalizedRect: RectF): Bitmap {
        val left = (bitmap.width * normalizedRect.left).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (bitmap.height * normalizedRect.top).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (bitmap.width * normalizedRect.right).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bitmap.height * normalizedRect.bottom).roundToInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }
}
