package com.jumincho.cvpass.core.testing

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.checkin.CheckIn
import com.jumincho.cvpass.core.checkin.CheckInRepository
import com.jumincho.cvpass.core.checkin.Visit
import com.jumincho.cvpass.core.checkin.VisitHistory
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueStore
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.core.visitor.VisitorStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A [Clock] that only moves when told to. */
class MutableClock(private var now: Instant, private val zone: ZoneId) : Clock() {

    /** Moves the clock forward by [duration]. */
    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
}

/** In-memory [CheckInRepository]; set [failure] to make writes and reads throw. */
class InMemoryCheckInRepository(private val zone: ZoneId) : CheckInRepository {
    private val state = MutableStateFlow(emptyList<CheckIn>())

    /** Thrown by every operation while set. */
    var failure: Exception? = null

    /** Everything stored, in insertion order. */
    val all: List<CheckIn>
        get() = state.value

    override suspend fun add(checkIn: CheckIn) {
        failure?.let { throw it }
        state.value += checkIn
    }

    override suspend fun checkInsOn(venueId: BusinessNumber, date: LocalDate): List<CheckIn> {
        failure?.let { throw it }
        return state.value.filter { it.isAt(venueId, date) }.sortedBy { it.checkedInAt }
    }

    override fun countOn(venueId: BusinessNumber, date: LocalDate): Flow<Int> =
        state.map { list ->
            failure?.let { throw it }
            list.count { it.isAt(venueId, date) }
        }

    override suspend fun deleteOlderThan(cutoff: Instant) {
        state.value = state.value.filterNot { it.checkedInAt.isBefore(cutoff) }
    }

    private fun CheckIn.isAt(venue: BusinessNumber, date: LocalDate) =
        venueId == venue && checkedInAt.atZone(zone).toLocalDate() == date
}

/** In-memory [VisitHistory]. */
class InMemoryVisitHistory : VisitHistory {
    private val state = MutableStateFlow(emptyList<Visit>())

    /** Everything stored, in insertion order. */
    val all: List<Visit>
        get() = state.value

    override suspend fun add(visit: Visit) {
        state.value += visit
    }

    override suspend fun lastVisitAt(venueId: BusinessNumber): Instant? =
        state.value.filter { it.venueId == venueId }.maxOfOrNull { it.visitedAt }

    override fun recent(limit: Int): Flow<List<Visit>> =
        state.map { list -> list.sortedByDescending { it.visitedAt }.take(limit) }

    override suspend fun deleteOlderThan(cutoff: Instant) {
        state.value = state.value.filterNot { it.visitedAt.isBefore(cutoff) }
    }
}

/** In-memory [VisitorStore]. */
class InMemoryVisitorStore(
    profile: VisitorProfile? = null,
    pass: VerifiedPass? = null,
) : VisitorStore {
    private val profileState = MutableStateFlow(profile)
    private val passState = MutableStateFlow(pass)

    override val profile: Flow<VisitorProfile?> = profileState
    override val pass: Flow<VerifiedPass?> = passState

    /** The currently saved profile. */
    val savedProfile: VisitorProfile?
        get() = profileState.value

    /** The currently saved pass. */
    val savedPass: VerifiedPass?
        get() = passState.value

    override suspend fun saveProfile(profile: VisitorProfile) {
        profileState.value = profile
    }

    override suspend fun savePass(pass: VerifiedPass) {
        passState.value = pass
    }

    override suspend fun clearPass() {
        passState.value = null
    }
}

/** In-memory [VenueStore]. */
class InMemoryVenueStore(venue: RegisteredVenue? = null) : VenueStore {
    private val state = MutableStateFlow(venue)

    override val venue: Flow<RegisteredVenue?> = state

    /** The currently saved venue. */
    val saved: RegisteredVenue?
        get() = state.value

    override suspend fun save(venue: RegisteredVenue) {
        state.value = venue
    }

    override suspend fun clear() {
        state.value = null
    }
}
