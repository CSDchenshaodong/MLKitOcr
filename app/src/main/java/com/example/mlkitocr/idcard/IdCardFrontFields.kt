package com.example.mlkitocr.idcard

data class IdCardFrontFields(
    val name: ParsedField = ParsedField(),
    val gender: ParsedField = ParsedField(),
    val ethnicity: ParsedField = ParsedField(),
    val birthDate: ParsedField = ParsedField(),
    val address: ParsedField = ParsedField(),
    val idNumber: ParsedField = ParsedField()
)

data class IdCardFrontParseResult(
    val fields: IdCardFrontFields,
    val confidence: RecognitionConfidence,
    val failureReason: String? = null,
    val warnings: List<String> = emptyList()
)
