package com.example.mlkitocr.ocr

data class TextBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerX: Int
        get() = (left + right) / 2

    val centerY: Int
        get() = (top + bottom) / 2
}
