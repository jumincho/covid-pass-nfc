package com.jumincho.cvpass.core.checkin

import com.jumincho.cvpass.core.pass.PassPolicy
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.core.visitor.VisitorProfile
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Outcome of [CheckInService.checkIn]. */
sealed interface CheckInResult {

    /** The visitor was logged at the venue. */
    data class CheckedIn(val checkIn: CheckIn) : CheckInResult

    /** The visitor already checked in here at [previousAt], within the duplicate window. */
    data class AlreadyCheckedIn(val previousAt: Instant) : CheckInResult

    /** The visitor has not entered their name and phone number yet. */
    data object ProfileMissing : CheckInResult

    /** Entry needs a valid pass; [status] is the current one, or `null` if none was verified. */
    data class PassRequired(val status: PassStatus?) : CheckInResult

    /** Storing the check-in failed. */
    data class Failed(val cause: Exception) : CheckInResult
}

/**
 * Checks the visitor in at a venue: enforces the pass requirement and
 * [DuplicateCheckInRule], then writes to the venue's log and the visitor's history.
 */
class CheckInService(
    private val checkIns: CheckInRepository,
    private val visits: VisitHistory,
    private val clock: Clock,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {

    /** Checks in [visitor], holding [pass], at the venue identified by [venue]. */
    suspend fun checkIn(visitor: VisitorProfile?, pass: VerifiedPass?, venue: VenueTag): CheckInResult {
        if (visitor == null) return CheckInResult.ProfileMissing
        val status = pass?.let { PassPolicy.evaluate(it.record, LocalDate.now(clock)) }
        if (status != PassStatus.Valid) return CheckInResult.PassRequired(status)

        return try {
            val now = clock.instant()
            val previous = visits.lastVisitAt(venue.businessNumber)
            if (previous != null && DuplicateCheckInRule.isDuplicate(previous, now)) {
                CheckInResult.AlreadyCheckedIn(previous)
            } else {
                val checkIn = CheckIn(
                    id = newId(),
                    venueId = venue.businessNumber,
                    venueName = venue.name,
                    visitorName = visitor.name,
                    phone = visitor.phone,
                    checkedInAt = now,
                )
                checkIns.add(checkIn)
                visits.add(Visit(venue.businessNumber, venue.name, now))
                CheckInResult.CheckedIn(checkIn)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CheckInResult.Failed(e)
        }
    }

    /** Deletes check-ins and visits older than [RetentionPolicy.RETENTION]. */
    suspend fun purgeExpired() {
        val cutoff = RetentionPolicy.cutoff(clock.instant())
        checkIns.deleteOlderThan(cutoff)
        visits.deleteOlderThan(cutoff)
    }
}
