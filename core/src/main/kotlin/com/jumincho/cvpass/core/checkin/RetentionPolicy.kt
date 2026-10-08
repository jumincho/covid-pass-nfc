package com.jumincho.cvpass.core.checkin

import java.time.Duration
import java.time.Instant

/**
 * Entry logs are personal data kept only as long as contact tracing needs them. Korean
 * guidance in 2021 was to destroy entry records after four weeks.
 */
object RetentionPolicy {

    /** How long check-ins and visits are kept. */
    val RETENTION: Duration = Duration.ofDays(28)

    /** Entries made before the returned instant are due for deletion at [now]. */
    fun cutoff(now: Instant): Instant = now.minus(RETENTION)
}
