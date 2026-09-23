package com.jumincho.cvpass.core.visitor

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "010-1234-5678", "01012345678", " 010 1234 5678 ", "010.1234.5678", "(010) 1234-5678",
            "+82 10-1234-5678", "+82 010-1234-5678", "+821012345678",
        ],
    )
    fun `parses common ways of writing a mobile number`(input: String) {
        val phone = PhoneNumber.parse(input)
        assertEquals("01012345678", phone?.digits)
        assertEquals("010-1234-5678", phone?.formatted)
    }

    @ParameterizedTest
    @CsvSource(
        "011-123-4567, 011-123-4567",
        "0161234567, 016-123-4567",
        "017-1234-5678, 017-1234-5678",
        "019 987 6543, 019-987-6543",
    )
    fun `accepts legacy prefixes with ten or eleven digits`(input: String, expected: String) {
        assertEquals(expected, PhoneNumber.parse(input)?.formatted)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "", "010", "010-123-4567", "010-1234-56789", "02-123-4567", "031-123-4567",
            "012-1234-5678", "010-1234-567a", "+1 010-1234-5678", "82 10 1234 5678x", "010_1234_5678",
        ],
    )
    fun `rejects anything that is not a Korean mobile number`(input: String) {
        assertNull(PhoneNumber.parse(input))
    }

    @ParameterizedTest
    @CsvSource(
        "'', ''",
        "0, 0",
        "010, 010",
        "0101, 010-1",
        "0101234, 010-1234",
        "01012345, 010-1234-5",
        "01012345678, 010-1234-5678",
        "010123456789, 010-1234-5678",
        "0111234, 011-123-4",
        "0111234567, 011-123-4567",
        "01112345678, 011-1234-5678",
    )
    fun `formats partial input as it is typed`(digits: String, expected: String) {
        assertEquals(expected, PhoneNumber.formatPartial(digits))
    }
}
