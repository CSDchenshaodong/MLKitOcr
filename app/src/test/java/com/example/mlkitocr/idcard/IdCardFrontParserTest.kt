package com.example.mlkitocr.idcard

import com.example.mlkitocr.ocr.OcrTextLine
import com.example.mlkitocr.ocr.TextBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdCardFrontParserTest {

    @Test
    fun parses_front_fields_from_anchored_lines() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("姓名", 24, 80, 120, 110),
                line("张三", 140, 80, 220, 110),
                line("性别", 24, 140, 100, 170),
                line("男", 110, 140, 140, 170),
                line("民族", 170, 140, 240, 170),
                line("汉", 250, 140, 280, 170),
                line("出生", 24, 200, 100, 230),
                line("1990", 110, 200, 190, 230),
                line("01", 200, 200, 240, 230),
                line("02", 250, 200, 290, 230),
                line("住址", 24, 260, 100, 290),
                line("北京市朝阳区建国路", 110, 260, 360, 290),
                line("88号", 110, 300, 180, 330),
                line("公民身份号码", 24, 360, 220, 390),
                line("110105199001020016", 230, 360, 460, 390)
            )
        )

        assertEquals("张三", result.fields.name.value)
        assertEquals("男", result.fields.gender.value)
        assertEquals("汉", result.fields.ethnicity.value)
        assertEquals("1990-01-02", result.fields.birthDate.value)
        assertEquals("北京市朝阳区建国路88号", result.fields.address.value)
        assertEquals("110105199001020016", result.fields.idNumber.value)
        assertEquals(RecognitionConfidence.HIGH, result.confidence)
    }

    @Test
    fun normalizes_common_id_number_ocr_confusion() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("公民身份号码", 24, 360, 220, 390),
                line("110105194912310O2X", 230, 360, 460, 390)
            )
        )

        assertEquals("11010519491231002X", result.fields.idNumber.value)
        assertEquals(RecognitionConfidence.PARTIAL, result.confidence)
    }

    @Test
    fun marks_low_confidence_when_no_anchor_is_found() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("随机文字", 20, 20, 120, 50)
            )
        )

        assertEquals(RecognitionConfidence.LOW, result.confidence)
        assertNotNull(result.failureReason)
        assertTrue(result.failureReason!!.contains("身份证"))
    }

    // ── New tests: fuzzy anchor matching ──

    @Test
    fun extracts_value_when_label_and_value_are_in_same_line() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("姓名张三", 24, 80, 160, 110),
                line("性别男", 24, 140, 100, 170),
                line("民族汉", 170, 140, 240, 170),
                line("出生19900102", 24, 200, 180, 230),
                line("住址北京市朝阳区建国路88号", 24, 260, 320, 290),
                line("公民身份号码110105199001020016", 24, 360, 360, 390)
            )
        )

        assertEquals("张三", result.fields.name.value)
        assertEquals("男", result.fields.gender.value)
        assertEquals("汉", result.fields.ethnicity.value)
        assertTrue(result.fields.birthDate.value.startsWith("1990"))
        assertEquals("北京市朝阳区建国路88号", result.fields.address.value)
        assertEquals("110105199001020016", result.fields.idNumber.value)
        assertEquals(RecognitionConfidence.HIGH, result.confidence)
    }

    @Test
    fun matches_split_id_number_label_on_adjacent_lines() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("姓名", 24, 80, 120, 110),
                line("张三", 140, 80, 220, 110),
                line("性别", 24, 140, 100, 170),
                line("男", 110, 140, 140, 170),
                line("民族", 170, 140, 240, 170),
                line("汉", 250, 140, 280, 170),
                line("出生", 24, 200, 100, 230),
                line("1990", 110, 200, 190, 230),
                line("01", 200, 200, 240, 230),
                line("02", 250, 200, 290, 230),
                line("住址", 24, 260, 100, 290),
                line("北京市朝阳区建国路", 110, 260, 360, 290),
                line("88号", 110, 300, 180, 330),
                line("公民身份", 24, 360, 140, 390),
                line("号码", 24, 395, 100, 425),
                line("110105199001020016", 230, 360, 460, 425)
            )
        )

        assertEquals("110105199001020016", result.fields.idNumber.value)
        assertEquals("张三", result.fields.name.value)
    }

    @Test
    fun matches_label_with_single_char_ocr_error() {
        // "性刃" is OCR misrecognition of "性别" (distance=1)
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("姓名", 24, 80, 120, 110),
                line("张三", 140, 80, 220, 110),
                line("性刃", 24, 140, 100, 170),
                line("男", 110, 140, 140, 170),
                line("民族", 170, 140, 240, 170),
                line("汉", 250, 140, 280, 170),
                line("出生", 24, 200, 100, 230),
                line("1990", 110, 200, 190, 230),
                line("01", 200, 200, 240, 230),
                line("02", 250, 200, 290, 230),
                line("住址", 24, 260, 100, 290),
                line("北京市朝阳区建国路88号", 110, 260, 360, 330),
                line("公民身份号码", 24, 360, 220, 390),
                line("110105199001020016", 230, 360, 460, 390)
            )
        )

        assertEquals("男", result.fields.gender.value)
        assertEquals(RecognitionConfidence.HIGH, result.confidence)
    }

    // ── New tests: fallback extraction ──

    @Test
    fun fallback_extracts_id_number_and_date_by_regex_when_no_anchors() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("110105199001020016", 200, 100, 500, 140),
                line("张三", 50, 60, 120, 90),
                line("1990年01月02日", 200, 150, 400, 180),
                line("北京市朝阳区建国路88号", 100, 200, 400, 240)
            )
        )

        assertEquals("110105199001020016", result.fields.idNumber.value)
        assertEquals("1990-01-02", result.fields.birthDate.value)
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("身份证号码") })
    }

    @Test
    fun fallback_with_only_id_number_anchor_derives_address() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("张三", 140, 80, 220, 110),
                line("男", 110, 140, 140, 170),
                line("汉", 250, 140, 280, 170),
                line("1990", 110, 200, 190, 230),
                line("北京市朝阳区建国路88号", 110, 260, 360, 330),
                line("公民身份号码", 24, 360, 220, 390),
                line("110105199001020016", 230, 360, 460, 390)
            )
        )

        assertEquals("110105199001020016", result.fields.idNumber.value)
        assertTrue(result.fields.address.value.isNotBlank())
    }

    // ── New tests: address extraction ──

    @Test
    fun merges_address_across_multiple_lines() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("姓名", 24, 80, 120, 110),
                line("张三", 140, 80, 220, 110),
                line("性别", 24, 140, 100, 170),
                line("男", 110, 140, 140, 170),
                line("民族", 170, 140, 240, 170),
                line("汉", 250, 140, 280, 170),
                line("出生", 24, 200, 100, 230),
                line("19900102", 110, 200, 220, 230),
                line("住址", 24, 260, 100, 290),
                line("北京市朝阳区", 24, 300, 200, 330),
                line("建国路88号院", 24, 340, 200, 370),
                line("1号楼2单元303室", 24, 380, 260, 410),
                line("公民身份号码", 24, 440, 220, 470),
                line("110105199001020016", 230, 440, 460, 470)
            )
        )

        assertTrue(result.fields.address.value.contains("北京市朝阳区"))
        assertTrue(result.fields.address.value.contains("建国路88号院"))
        assertTrue(result.fields.address.value.contains("1号楼2单元303室"))
    }

    // ── New tests: birth date across lines ──

    @Test
    fun reassembles_birth_date_from_separate_ocr_lines() {
        val result = IdCardFrontParser(
            idNumberValidator = ChinaIdNumberValidator()
        ).parse(
            listOf(
                line("姓名", 24, 80, 120, 110),
                line("张三", 140, 80, 220, 110),
                line("性别", 24, 140, 100, 170),
                line("男", 110, 140, 140, 170),
                line("出生", 24, 200, 100, 230),
                line("1990", 110, 200, 190, 230),
                line("01", 200, 200, 240, 230),
                line("02", 250, 200, 290, 230),
                line("住址", 24, 260, 100, 290),
                line("北京市朝阳区建国路88号", 110, 260, 360, 330),
                line("公民身份号码", 24, 360, 220, 390),
                line("110105199001020016", 230, 360, 460, 390)
            )
        )

        assertEquals("1990-01-02", result.fields.birthDate.value)
    }

    // ── Helper ──

    private fun line(text: String, left: Int, top: Int, right: Int, bottom: Int): OcrTextLine {
        return OcrTextLine(
            text = text,
            bounds = TextBounds(
                left = left,
                top = top,
                right = right,
                bottom = bottom
            )
        )
    }
}
