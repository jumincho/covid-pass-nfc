package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessNumber
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VenueTagTest {

    private val number = BusinessNumber.parse("124-81-00998")!!

    @Test
    fun `normalises venue names`() {
        assertEquals("카페 전주", VenueTag.normalizeName("  카페\t전주  "))
        assertEquals("x".repeat(VenueTag.MAX_NAME_LENGTH), VenueTag.normalizeName("x".repeat(VenueTag.MAX_NAME_LENGTH)))
    }

    @Test
    fun `rejects blank, overlong and control-character names`() {
        assertNull(VenueTag.normalizeName(""))
        assertNull(VenueTag.normalizeName("   "))
        assertNull(VenueTag.normalizeName("x".repeat(VenueTag.MAX_NAME_LENGTH + 1)))
        assertNull(VenueTag.normalizeName("a\u0007b"))
    }

    @Test
    fun `requires a normalised name`() {
        assertFailsWith<IllegalArgumentException> { VenueTag(number, " not normalised") }
    }
}
