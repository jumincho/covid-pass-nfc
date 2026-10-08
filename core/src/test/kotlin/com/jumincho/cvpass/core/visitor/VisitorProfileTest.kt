package com.jumincho.cvpass.core.visitor

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VisitorProfileTest {

    private val phone = PhoneNumber.parse("010-1234-5678")!!

    @Test
    fun `requires a normalised name`() {
        assertEquals("홍길동", VisitorProfile("홍길동", phone).name)
        assertFailsWith<IllegalArgumentException> { VisitorProfile(" 홍길동", phone) }
        assertFailsWith<IllegalArgumentException> { VisitorProfile("홍", phone) }
    }
}
