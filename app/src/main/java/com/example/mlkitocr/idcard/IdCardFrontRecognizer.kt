package com.example.mlkitocr.idcard

import android.graphics.Bitmap
import com.example.mlkitocr.mlkit.MlKitOcrEngine

class IdCardFrontRecognizer(
    private val ocrEngine: MlKitOcrEngine,
    private val parser: IdCardFrontParser
) {

    suspend fun recognize(bitmap: Bitmap): IdCardFrontParseResult {
        val lines = ocrEngine.recognize(bitmap)
        if (lines.isEmpty()) {
            return IdCardFrontParseResult(
                fields = IdCardFrontFields(),
                confidence = RecognitionConfidence.LOW,
                failureReason = "未识别到有效文字，请调整角度后重试"
            )
        }
        return parser.parse(lines)
    }
}
