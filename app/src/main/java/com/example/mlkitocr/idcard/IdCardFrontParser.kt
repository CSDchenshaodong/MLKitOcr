package com.example.mlkitocr.idcard

import com.example.mlkitocr.ocr.OcrTextLine
import kotlin.math.abs
import kotlin.math.min

class IdCardFrontParser(
    private val idNumberValidator: ChinaIdNumberValidator
) {
    fun parse(lines: List<OcrTextLine>): IdCardFrontParseResult {
        if (lines.isEmpty()) {
            return lowConfidence("未检测到身份证文字")
        }

        val sortedLines = lines.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
        val anchors = AnchorMap(sortedLines)

        if (!anchors.hasKnownAnchor()) {
            return fallbackExtract(sortedLines)
        }

        val warnings = mutableListOf<String>()

        val name = extractFieldValue(anchors.nameMatch, sortedLines, warnings, "姓名")
        val gender = extractFieldValue(anchors.genderMatch, sortedLines, warnings, "性别")
        val ethnicity = extractFieldValue(anchors.ethnicityMatch, sortedLines, warnings, "民族")
        val birthDate = extractBirthDate(anchors.birthMatch, sortedLines, warnings)
        var address = extractAddress(anchors.addressMatch, anchors.idNumberMatch, sortedLines)
        val idNumber = extractIdNumber(anchors.idNumberMatch, sortedLines)

        // Post-extraction: derive address from ID number position if address anchor is missing
        if (address.isBlank() && anchors.idNumberMatch.line != null) {
            address = deriveAddressByPosition(anchors.idNumberMatch.line!!, sortedLines)
            if (address.isNotBlank()) {
                warnings.add("地址通过身份证号码位置推导，可能不完整")
            }
        }

        val fields = IdCardFrontFields(
            name = ParsedField(name, name.isNotBlank()),
            gender = ParsedField(gender, gender.isNotBlank()),
            ethnicity = ParsedField(ethnicity, ethnicity.isNotBlank()),
            birthDate = ParsedField(birthDate, birthDate.isNotBlank()),
            address = ParsedField(address, address.isNotBlank()),
            idNumber = ParsedField(idNumber, idNumberValidator.isValid(idNumber))
        )

        val certainCount = listOf(
            fields.name, fields.gender, fields.ethnicity,
            fields.birthDate, fields.address, fields.idNumber
        ).count { it.value.isNotBlank() }

        val confidence = when {
            fields.idNumber.isCertain && certainCount >= 5 -> RecognitionConfidence.HIGH
            certainCount >= 1 -> RecognitionConfidence.PARTIAL
            else -> RecognitionConfidence.LOW
        }

        val failureReason = if (confidence == RecognitionConfidence.LOW) {
            "识别可信度较低，建议重新拍摄身份证正面"
        } else {
            null
        }

        return IdCardFrontParseResult(
            fields = fields,
            confidence = confidence,
            failureReason = failureReason,
            warnings = warnings
        )
    }

    // ── Field extraction with multi-direction search ──

    private fun extractFieldValue(
        match: AnchorMatch,
        lines: List<OcrTextLine>,
        warnings: MutableList<String>,
        fieldName: String
    ): String {
        if (match.type == AnchorMatchType.NONE) return ""

        if (match.type == AnchorMatchType.PREFIX && match.prefixRemainder.isNotBlank()) {
            return match.prefixRemainder.trim()
        }

        val anchor = match.line ?: return ""

        // Step 1: same row, to the right
        val sameRowRight = lines
            .filter { it !== anchor && isSameRow(it, anchor) && it.bounds.left >= anchor.bounds.right }
            .minByOrNull { it.bounds.left }
        if (sameRowRight != null && sameRowRight.text.isNotBlank()) {
            return sameRowRight.text.trim()
        }

        // Step 2: directly below (left-aligned)
        val below = lines
            .filter {
                it !== anchor &&
                    it.bounds.top >= anchor.bounds.bottom &&
                    abs(it.bounds.left - anchor.bounds.left) <= anchor.rowHeight * 2
            }
            .minByOrNull { it.bounds.top }
        if (below != null && below.text.isNotBlank()) {
            return below.text.trim()
        }

        // Step 3: bottom-right area
        val bottomRight = lines
            .filter {
                it !== anchor &&
                    it.bounds.top >= anchor.bounds.top &&
                    it.bounds.left >= anchor.bounds.right &&
                    it.bounds.top <= anchor.bounds.bottom + anchor.rowHeight * 2
            }
            .minByOrNull { line -> abs(line.bounds.top - anchor.bounds.top) * 100 + line.bounds.left }
        if (bottomRight != null && bottomRight.text.isNotBlank()) {
            return bottomRight.text.trim()
        }

        return ""
    }

    // ── Birth date extraction ──

    private fun extractBirthDate(
        match: AnchorMatch,
        lines: List<OcrTextLine>,
        warnings: MutableList<String>
    ): String {
        if (match.type == AnchorMatchType.NONE) return ""

        val anchor = match.line
        val candidateTexts = mutableListOf<String>()

        if (match.type == AnchorMatchType.PREFIX && match.prefixRemainder.isNotBlank()) {
            candidateTexts.add(match.prefixRemainder)
        }

        if (anchor != null) {
            // Same row right
            candidateTexts.addAll(
                lines.filter { it !== anchor && isSameRow(it, anchor) && it.bounds.left >= anchor.bounds.right }
                    .sortedBy { it.bounds.left }
                    .map { it.text }
            )
            // Below row
            candidateTexts.addAll(
                lines.filter {
                    it !== anchor &&
                        it.bounds.top >= anchor.bounds.bottom &&
                        it.bounds.top <= anchor.bounds.bottom + anchor.rowHeight * 2
                }
                .sortedBy { it.bounds.left }
                .map { it.text }
            )
        }

        if (candidateTexts.isEmpty()) return ""

        // Extract all digit groups
        val digitGroups = candidateTexts
            .flatMap { text -> text.split(Regex("[^0-9]+")).filter { it.isNotBlank() } }

        if (digitGroups.isEmpty()) return ""

        val year = digitGroups.firstOrNull { it.length == 4 && it.startsWith("19") || it.startsWith("20") }
            ?: digitGroups.getOrNull(0)?.padStart(4, '0')?.take(4).orEmpty()
        val month = digitGroups.getOrNull(
            if (digitGroups.firstOrNull { it.length == 4 } != null) 1 else 0
        )?.padStart(2, '0')?.takeLast(2).orEmpty()
        val day = digitGroups.getOrNull(
            if (digitGroups.firstOrNull { it.length == 4 } != null) 2 else 1
        )?.padStart(2, '0')?.takeLast(2).orEmpty()

        // Additional pass: try to find month in candidate text
        val monthFromText = if (month.toIntOrNull() == null || month.toInt() > 12) {
            candidateTexts.joinToString("")
                .let { Regex("(\\d{2})\\s*月").find(it)?.groupValues?.get(1) }
                .orEmpty()
        } else month

        return listOf(year, monthFromText.ifBlank { month }, day)
            .filter { it.isNotBlank() }
            .joinToString("-")
    }

    // ── Address extraction ──

    private fun extractAddress(
        addressMatch: AnchorMatch,
        idNumberMatch: AnchorMatch,
        lines: List<OcrTextLine>
    ): String {
        if (addressMatch.type == AnchorMatchType.NONE) return ""

        val addressAnchor = addressMatch.line
        val addressTop = addressAnchor?.bounds?.top ?: return ""

        val bottomLimit = idNumberMatch.line?.bounds?.top ?: Int.MAX_VALUE

        val knownLabels = setOf("姓名", "性别", "民族", "出生", "住址", "公民身份号码")

        val addressLines = lines
            .filter { line ->
                line !== addressAnchor &&
                    line.bounds.top >= addressTop &&
                    line.bounds.top < bottomLimit &&
                    knownLabels.none { label -> line.text.contains(label) }
            }
            .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
            .map { it.text.trim() }
            .filter { it.isNotBlank() }

        // If prefix match has remainder, prepend it
        val prefixRemainder = if (addressMatch.type == AnchorMatchType.PREFIX) {
            addressMatch.prefixRemainder.trim()
        } else ""

        val parts = listOf(prefixRemainder) + addressLines
        return parts.filter { it.isNotBlank() }.joinToString("")
    }

    // ── Position-based address derivation (when address anchor is missing) ──

    private fun deriveAddressByPosition(
        idNumberLine: OcrTextLine,
        lines: List<OcrTextLine>
    ): String {
        val idTop = idNumberLine.bounds.top
        val knownLabels = setOf("姓名", "性别", "民族", "出生", "住址", "公民身份号码")

        return lines
            .filter { line ->
                line !== idNumberLine &&
                    line.bounds.top < idTop &&
                    knownLabels.none { label -> line.text.contains(label) } &&
                    line.text.trim().length >= 4 &&
                    line.text.any { c -> c in '一'..'鿿' }
            }
            .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
            .map { it.text.trim() }
            .joinToString("")
    }

    // ── ID number extraction ──

    private fun extractIdNumber(match: AnchorMatch, lines: List<OcrTextLine>): String {
        val candidates = buildList {
            val anchor = match.line
            if (anchor != null) {
                if (match.type == AnchorMatchType.PREFIX && match.prefixRemainder.isNotBlank()) {
                    add(match.prefixRemainder)
                }
                addAll(
                    lines
                        .filter { it !== anchor && isSameRow(it, anchor) && it.bounds.left >= anchor.bounds.right }
                        .map { it.text }
                )
            }
            addAll(lines.map { it.text })
        }

        return candidates
            .map(::normalizeIdNumber)
            .firstOrNull { idNumberValidator.isValid(it) }
            .orEmpty()
    }

    // ── Fallback extraction (no anchors found) ──

    private fun fallbackExtract(lines: List<OcrTextLine>): IdCardFrontParseResult {
        val allText = lines.joinToString(" ") { it.text }
        val warnings = mutableListOf<String>()
        var name = ""
        var gender = ""
        var ethnicity = ""
        var birthDate = ""
        var address = ""
        var idNumber = ""

        // Try regex for ID number
        val idCandidates = Regex("\\d{17}[\\dXx]").findAll(allText).map { it.value }.toList()
        val rawId = idCandidates.firstOrNull { idNumberValidator.isValid(normalizeIdNumber(it)) }
            ?: idCandidates.firstOrNull()
        if (rawId != null) {
            idNumber = normalizeIdNumber(rawId)
            warnings.add("身份证号码通过模糊匹配提取，请仔细核对")
        }

        // Try regex for birth date
        val datePatterns = listOf(
            Regex("((?:19|20)\\d{2})[年\\-./]\\s*(\\d{1,2})[月\\-./]\\s*(\\d{1,2})"),
            Regex("((?:19|20)\\d{2})\\s*[年]\\s*(\\d{1,2})\\s*[月]\\s*(\\d{1,2})\\s*[日]"),
            Regex("((?:19|20)\\d{2})[\\-./](\\d{1,2})[\\-./](\\d{1,2})")
        )
        for (pattern in datePatterns) {
            val match = pattern.find(allText)
            if (match != null) {
                val year = match.groupValues[1]
                val month = match.groupValues[2].padStart(2, '0')
                val day = match.groupValues[3].padStart(2, '0')
                birthDate = "$year-$month-$day"
                warnings.add("出生日期通过模糊匹配提取，请仔细核对")
                break
            }
        }

        // If ID number found, try to derive address from lines above it
        if (idNumber.isNotBlank()) {
            val idLine = lines.firstOrNull {
                normalizeIdNumber(it.text).contains(idNumber.take(6))
            } ?: lines.firstOrNull {
                it.text.contains(idNumber.take(6))
            }

            if (idLine != null) {
                val idTop = idLine.bounds.top
                val addressLines = lines
                    .filter { it.bounds.top < idTop && it !== idLine }
                    .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
                    .map { it.text.trim() }
                    .filter { it.isNotBlank() && it.length > 3 }

                // Pick lines that look like address (long Chinese text, not short labels)
                val likelyAddress = addressLines.filter { it.length >= 4 && it.any { c -> c in '一'..'鿿' } }
                if (likelyAddress.isNotEmpty()) {
                    address = likelyAddress.joinToString("")
                    warnings.add("地址通过位置推导提取，可能不完整")
                }
            }
        }

        // Try to find name: short Chinese text (2-4 chars) in top-left area
        val shortChinese = lines
            .filter {
                val t = it.text.trim()
                t.length in 2..4 && t.all { c -> c in '一'..'鿿' }
            }
            .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
        if (shortChinese.isNotEmpty()) {
            name = shortChinese.first().text.trim()
            warnings.add("姓名通过位置推导提取，请仔细核对")
        }

        val confidence = if (idNumber.isNotBlank()) RecognitionConfidence.PARTIAL else RecognitionConfidence.LOW

        return IdCardFrontParseResult(
            fields = IdCardFrontFields(
                name = ParsedField(name, name.isNotBlank()),
                gender = ParsedField(gender, gender.isNotBlank()),
                ethnicity = ParsedField(ethnicity, ethnicity.isNotBlank()),
                birthDate = ParsedField(birthDate, birthDate.isNotBlank()),
                address = ParsedField(address, address.isNotBlank()),
                idNumber = ParsedField(idNumber, idNumberValidator.isValid(idNumber))
            ),
            confidence = confidence,
            failureReason = if (confidence == RecognitionConfidence.LOW) "未识别到身份证关键字段，建议重新拍摄" else null,
            warnings = warnings
        )
    }

    // ── Helpers ──

    private fun normalizeIdNumber(raw: String): String {
        val source = raw.uppercase()
        val builder = StringBuilder(source.length)
        source.forEachIndexed { index, char ->
            val normalized = when {
                index < 17 && char == 'O' -> '0'
                index < 17 && char == 'I' -> '1'
                index < 17 && char == 'B' -> '8'
                else -> char
            }
            if (normalized.isDigit() || normalized == 'X') {
                builder.append(normalized)
            }
        }
        return builder.toString()
    }

    private fun isSameRow(candidate: OcrTextLine, anchor: OcrTextLine): Boolean {
        return abs(candidate.centerY - anchor.centerY) <= anchor.rowHeight * 0.6f
    }

    private fun lowConfidence(reason: String, warnings: List<String> = emptyList()): IdCardFrontParseResult {
        return IdCardFrontParseResult(
            fields = IdCardFrontFields(),
            confidence = RecognitionConfidence.LOW,
            failureReason = reason,
            warnings = warnings
        )
    }

    // ── Levenshtein distance ──

    private fun levenshteinDistance(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                curr[j] = if (a[i - 1] == b[j - 1]) {
                    prev[j - 1]
                } else {
                    1 + minOf(prev[j], curr[j - 1], prev[j - 1])
                }
            }
            prev.indices.forEach { prev[it] = curr[it] }
        }
        return prev[b.length]
    }

    // ── Anchor matching types ──

    private enum class AnchorMatchType { EXACT, PREFIX, FUZZY, MERGED, NONE }

    private data class AnchorMatch(
        val line: OcrTextLine?,
        val type: AnchorMatchType,
        val prefixRemainder: String = ""
    )

    // ── AnchorMap ──

    private inner class AnchorMap(lines: List<OcrTextLine>) {
        val nameMatch = findLabel("姓名", lines)
        val genderMatch = findLabel("性别", lines)
        val ethnicityMatch = findLabel("民族", lines)
        val birthMatch = findLabel("出生", lines)
        val addressMatch = findLabel("住址", lines)
        val idNumberMatch = findIdNumberLabel(lines)

        fun hasKnownAnchor(): Boolean {
            return listOf(nameMatch, genderMatch, ethnicityMatch, birthMatch, addressMatch, idNumberMatch)
                .any { it.type != AnchorMatchType.NONE }
        }

        private fun findLabel(label: String, lines: List<OcrTextLine>): AnchorMatch {
            // Level 1: exact contains match
            val exact = lines.firstOrNull { it.text.contains(label) }
            if (exact != null) {
                // Check if this is a prefix match (label + value in same line)
                val afterLabel = exact.text.substringAfter(label)
                if (afterLabel.isNotBlank()) {
                    return AnchorMatch(exact, AnchorMatchType.PREFIX, afterLabel)
                }
                return AnchorMatch(exact, AnchorMatchType.EXACT)
            }

            // Level 2: prefix match (line starts with label chars)
            val prefix = lines.firstOrNull { line ->
                label.length <= line.text.length &&
                    line.text.take(label.length) == label
            }
            if (prefix != null) {
                val remainder = prefix.text.drop(label.length)
                return AnchorMatch(prefix, AnchorMatchType.PREFIX, remainder)
            }

            // Level 3: edit distance for short labels
            if (label.length in 2..4) {
                val fuzzy = lines.firstOrNull { line ->
                    val t = line.text.trim()
                    t.length in (label.length - 2)..(label.length + 2) &&
                        levenshteinDistance(label, t.take(label.length + 2)) <= 1
                }
                if (fuzzy != null) {
                    return AnchorMatch(fuzzy, AnchorMatchType.FUZZY)
                }
            }

            return AnchorMatch(null, AnchorMatchType.NONE)
        }

        private fun findIdNumberLabel(lines: List<OcrTextLine>): AnchorMatch {
            val fullLabel = "公民身份号码"
            val part1 = "公民身份"
            val part2 = "号码"

            // Level 1: exact match
            val exact = lines.firstOrNull { it.text.contains(fullLabel) }
            if (exact != null) {
                val after = exact.text.substringAfter(fullLabel)
                if (after.isNotBlank() && after.length <= 20) {
                    return AnchorMatch(exact, AnchorMatchType.PREFIX, after)
                }
                return AnchorMatch(exact, AnchorMatchType.EXACT)
            }

            // Level 2: prefix with "公民"
            val prefixMatch = lines.firstOrNull {
                it.text.contains("公民") && (
                    it.text.contains("身份") || it.text.contains("号码")
                )
            }
            if (prefixMatch != null) {
                return AnchorMatch(prefixMatch, AnchorMatchType.FUZZY)
            }

            // Level 3: split-label merge (公民身份 + 号码 on adjacent lines)
            val topParts = lines.filter { it.text.contains(part1) }
            for (top in topParts) {
                val bottom = lines.firstOrNull { other ->
                    other !== top &&
                        other.text.contains(part2) &&
                        other.bounds.top >= top.bounds.bottom &&
                        other.bounds.top <= top.bounds.bottom + top.rowHeight * 2 &&
                        abs(other.bounds.left - top.bounds.left) <= top.rowHeight * 3
                }
                if (bottom != null) {
                    return AnchorMatch(top, AnchorMatchType.MERGED)
                }
            }

            // Level 4: edit distance for "公民身份号码"
            val fuzzy = lines.firstOrNull { line ->
                val t = line.text.trim()
                t.length in (fullLabel.length - 3)..(fullLabel.length + 3) &&
                    levenshteinDistance(fullLabel, t.take(fullLabel.length + 3)) <= 2
            }
            if (fuzzy != null) {
                return AnchorMatch(fuzzy, AnchorMatchType.FUZZY)
            }

            return AnchorMatch(null, AnchorMatchType.NONE)
        }
    }
}
