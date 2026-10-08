package com.jumincho.cvpass.core.checkin

import java.time.Duration
import java.time.Instant

/**
 * Taps on the same venue's tag within [WINDOW] of the previous check-in are treated as
 * the same visit, so a visitor who taps twice is logged once.
 */
object DuplicateCheckInRule {

    /** How long after a check-in another tap at the same venue is ignored. */
    val WINDOW: Duration = Duration.ofMinutes(10)

    /**
     * Whether a check-in at [now] repeats the one at [previous]. A [previous] later than
     * [now] (the device clock went backwards) is not treated as a duplicate, so the
     * visitor is never locked out.
     */
    fun isDuplicate(previous: Instant?, now: Instant): Boolean {
        if (previous == null) return false
        val elapsed = Duration.between(previous, now)
        return !elapsed.isNegative && elapsed < WINDOW
    }
}
