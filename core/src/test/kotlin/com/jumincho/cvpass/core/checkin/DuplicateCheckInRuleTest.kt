package com.jumincho.cvpass.core.checkin

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DuplicateCheckInRuleTest {

    private val previous = Instant.parse("2021-10-20T12:00:00Z")

    @Test
    fun `a first check-in is never a duplicate`() {
        assertFalse(DuplicateCheckInRule.isDuplicate(previous = null, now = previous))
    }

    @Test
    fun `taps within ten minutes are duplicates`() {
        assertTrue(DuplicateCheckInRule.isDuplicate(previous, previous))
        assertTrue(DuplicateCheckInRule.isDuplicate(previous, previous.plus(Duration.ofMinutes(10).minusMillis(1))))
    }

    @Test
    fun `a tap ten minutes later is a new visit`() {
        assertFalse(DuplicateCheckInRule.isDuplicate(previous, previous.plus(Duration.ofMinutes(10))))
        assertFalse(DuplicateCheckInRule.isDuplicate(previous, previous.plus(Duration.ofHours(3))))
    }

    @Test
    fun `a clock that went backwards does not lock the visitor out`() {
        assertFalse(DuplicateCheckInRule.isDuplicate(previous, previous.minusSeconds(1)))
    }
}
