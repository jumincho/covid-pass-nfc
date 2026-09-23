package com.jumincho.cvpass.core.business

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BusinessNumberTest {

    @ParameterizedTest
    @ValueSource(strings = ["124-81-00998", "220-81-62517", "120-81-47521"])
    fun `accepts registered numbers of well-known companies`(input: String) {
        val number = assertNotNull(BusinessNumber.parse(input))
        assertEquals(input, number.formatted)
        assertEquals(input.replace("-", ""), number.digits)
    }

    @Test
    fun `adds the carry of the ninth digit to the checksum`() {
        // 124-81-00998: the weighted sum is 118; the ninth digit contributes ⌊9 × 5 / 10⌋ = 4
        // more, so the check digit is (10 - 122 % 10) % 10 = 8. Without the carry it would be 2.
        assertEquals(8, BusinessNumber.checkDigit("124810099"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["1248100998", " 124 81 00998 ", "124-8100-998"])
    fun `ignores spaces and hyphens`(input: String) {
        assertEquals("1248100998", BusinessNumber.parse(input)?.digits)
    }

    @ParameterizedTest
    @CsvSource(
        "124-81-0099, WRONG_FORMAT",
        "124-81-009981, WRONG_FORMAT",
        "124-81-0099a, WRONG_FORMAT",
        "124.81.00998, WRONG_FORMAT",
        "'', WRONG_FORMAT",
        "124-81-00997, CHECKSUM_MISMATCH",
        "000-00-00001, CHECKSUM_MISMATCH",
    )
    fun `explains why a number is rejected`(input: String, problem: BusinessNumber.Problem) {
        assertEquals(problem, BusinessNumber.problemWith(input))
        assertNull(BusinessNumber.parse(input))
    }

    @Test
    fun `rejects non-ASCII digits`() {
        assertEquals(BusinessNumber.Problem.WRONG_FORMAT, BusinessNumber.problemWith("１２４８１００９９８"))
    }

    @ParameterizedTest
    @CsvSource(
        "'', ''",
        "124, 124",
        "1248, 124-8",
        "12481, 124-81",
        "124810, 124-81-0",
        "1248100998, 124-81-00998",
        "12481009981, 124-81-00998",
    )
    fun `formats partial input progressively`(digits: String, expected: String) {
        assertEquals(expected, BusinessNumber.formatPartial(digits))
    }

    @Test
    fun `renders as the formatted number`() {
        assertEquals("124-81-00998", BusinessNumber.parse("1248100998").toString())
    }
}
