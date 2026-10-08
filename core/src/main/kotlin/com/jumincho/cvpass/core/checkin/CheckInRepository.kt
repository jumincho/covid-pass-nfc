package com.jumincho.cvpass.core.checkin

import com.jumincho.cvpass.core.business.BusinessNumber
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * The venue-side visitor log that inspectors query. Implementations may store it on the
 * device or in a shared backend; dates are interpreted in the implementation's time zone.
 */
interface CheckInRepository {

    /** Appends [checkIn] to its venue's log. */
    suspend fun add(checkIn: CheckIn)

    /** All check-ins at [venueId] on [date], oldest first. */
    suspend fun checkInsOn(venueId: BusinessNumber, date: LocalDate): List<CheckIn>

    /** Number of check-ins at [venueId] on [date], updated as new ones arrive. */
    fun countOn(venueId: BusinessNumber, date: LocalDate): Flow<Int>

    /**
     * Deletes check-ins made before [cutoff]. Backends that expire entries on the server
     * (for example with a TTL policy) may implement this as a no-op.
     */
    suspend fun deleteOlderThan(cutoff: Instant)
}
