package com.jumincho.cvpass.core.privacy

import com.jumincho.cvpass.core.visitor.PhoneNumber
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class MaskingTest {

    @ParameterizedTest
    @CsvSource(
        "홍길동, 홍*동",
        "이준, 이*",
        "남궁민수, 남**수",
        "'John Smith', 'J*** ****h'",
        "A, *",
        "'', ''",
    )
    fun `masks all but the first and last character of a name`(name: String, expected: String) {
        assertEquals(expected, Masking.name(name))
    }

    @ParameterizedTest
    @CsvSource(
        "010-1234-5678, 010-****-5678",
        "011-123-4567, 011-***-4567",
    )
    fun `masks the middle group of a phone number`(phone: String, expected: String) {
        assertEquals(expected, Masking.phone(PhoneNumber.parse(phone)!!))
    }
}
