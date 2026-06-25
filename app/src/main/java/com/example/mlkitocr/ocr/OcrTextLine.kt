package com.example.mlkitocr.ocr

data class OcrTextLine(
    val text: String,
    val bounds: TextBounds
) {
    val centerY: Int get() = (bounds.top + bounds.bottom) / 2
    val rowHeight: Int get() = bounds.bottom - bounds.top
}
