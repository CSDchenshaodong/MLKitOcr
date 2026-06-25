package com.example.mlkitocr.idcard

import java.time.DateTimeException
import java.time.LocalDate

class ChinaIdNumberValidator {

    fun isValid(rawValue: String): Boolean {
        val normalized = rawValue.trim().uppercase()
        if (!normalized.matches(Regex("\\d{17}[0-9X]"))) {
            return false
        }
        if (!isBirthDateValid(normalized.substring(6, 14))) {
            return false
        }
        return checksum(normalized.substring(0, 17)) == normalized.last()
    }

    private fun isBirthDateValid(value: String): Boolean {
        return try {
            val year = value.substring(0, 4).toInt()
            val month = value.substring(4, 6).toInt()
            val day = value.substring(6, 8).toInt()
            val birthDate = LocalDate.of(year, month, day)
            birthDate.isBefore(LocalDate.now().plusDays(1))
        } catch (_: DateTimeException) {
            false
        } catch (_: NumberFormatException) {
            false
        }
    }

    private fun checksum(firstSeventeenDigits: String): Char {
        val weights = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
        val checkMap = charArrayOf('1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2')
        val sum = firstSeventeenDigits.mapIndexed { index, char ->
            (char - '0') * weights[index]
        }.sum()
        return checkMap[sum % 11]
    }
}
