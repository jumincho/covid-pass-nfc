package com.jumincho.cvpass.core.visitor

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonNameTest {

    @ParameterizedTest
    @CsvSource(
        "홍길동, 홍길동",
        "'  홍길동  ', 홍길동",
        "'Hong   Gil-dong', Hong Gil-dong",
        "남궁민수, 남궁민수",
    )
    fun `normalises plausible names`(input: String, expected: String) {
        assertEquals(expected, PersonName.normalize(input))
    }

    @Test
    fun `accepts apostrophes and dots in names`() {
        assertEquals("O'Neil", PersonName.normalize("O'Neil"))
        assertEquals("J. Kim", PersonName.normalize("J.  Kim"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "홍", "홍길동1", "010-1234-5678", "홍길동!", "--", "abcdefghijklmnopqrstuvwxyzabcde"])
    fun `rejects implausible names`(input: String) {
        assertNull(PersonName.normalize(input))
    }

    @Test
    fun `compares names ignoring spacing and case`() {
        assertTrue(PersonName.matches("홍 길동", "홍길동"))
        assertTrue(PersonName.matches("HONG GILDONG", "Hong Gildong"))
        assertFalse(PersonName.matches("홍길동", "홍길순"))
    }

    @Test
    fun `profile requires a normalised name`() {
        val phone = PhoneNumber.parse("010-1234-5678")!!
        assertEquals("홍길동", VisitorProfile("홍길동", phone).name)
        assertFailsWith<IllegalArgumentException> { VisitorProfile(" 홍길동", phone) }
    }
}
