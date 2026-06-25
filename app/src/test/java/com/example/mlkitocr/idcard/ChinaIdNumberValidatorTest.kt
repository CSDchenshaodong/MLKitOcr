package com.example.mlkitocr.idcard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChinaIdNumberValidatorTest {

    @Test
    fun accepts_valid_id_number() {
        assertTrue(ChinaIdNumberValidator().isValid("11010519491231002X"))
    }

    @Test
    fun rejects_invalid_checksum() {
        assertFalse(ChinaIdNumberValidator().isValid("110105194912310021"))
    }

    @Test
    fun rejects_invalid_birth_date() {
        assertFalse(ChinaIdNumberValidator().isValid("11010519990231002X"))
    }
}
