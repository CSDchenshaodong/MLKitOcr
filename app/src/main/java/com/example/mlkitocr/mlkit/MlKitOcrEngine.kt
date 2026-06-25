package com.example.mlkitocr.mlkit

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.mlkitocr.ocr.OcrTextLine
import com.example.mlkitocr.ocr.TextBounds
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MlKitOcrEngine {

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    suspend fun recognize(bitmap: Bitmap): List<OcrTextLine> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).await()
        return text.textBlocks
            .flatMap { block -> block.lines }
            .mapNotNull { line ->
                val bounds = line.boundingBox ?: return@mapNotNull null
                OcrTextLine(
                    text = line.text,
                    bounds = bounds.toTextBounds()
                )
            }
    }

    private fun Rect.toTextBounds(): TextBounds {
        return TextBounds(
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
