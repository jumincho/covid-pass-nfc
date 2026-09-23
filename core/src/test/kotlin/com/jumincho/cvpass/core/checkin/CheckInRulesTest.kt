package com.jumincho.cvpass.core.checkin

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckInRulesTest {

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

    @Test
    fun `entries older than four weeks are due for deletion`() {
        val now = Instant.parse("2021-11-17T09:30:00Z")
        assertEquals(Instant.parse("2021-10-20T09:30:00Z"), RetentionPolicy.cutoff(now))
    }
}
