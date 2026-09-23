package com.jumincho.cvpass.core.checkin

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.pass.VaccinationRecord
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.testing.InMemoryCheckInRepository
import com.jumincho.cvpass.core.testing.InMemoryVisitHistory
import com.jumincho.cvpass.core.testing.MutableClock
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.core.visitor.VisitorProfile
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckInServiceTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val clock = MutableClock(Instant.parse("2021-10-20T03:00:00Z"), zone)
    private val checkIns = InMemoryCheckInRepository(zone)
    private val visits = InMemoryVisitHistory()
    private var nextId = 0
    private val service = CheckInService(checkIns, visits, clock) { "check-in-${nextId++}" }

    private val venue = VenueTag(BusinessNumber.parse("124-81-00998")!!, "카페 전주")
    private val visitor = VisitorProfile("홍길동", PhoneNumber.parse("010-1234-5678")!!)
    private val validPass = VerifiedPass(
        VaccinationRecord(doses = 2, primarySeriesDoses = 2, lastDoseDate = LocalDate.of(2021, 8, 1)),
        verifiedAt = Instant.parse("2021-10-01T00:00:00Z"),
    )

    @Test
    fun `logs the visitor at the venue and in their own history`() = runTest {
        val result = service.checkIn(visitor, validPass, venue)

        val expected = CheckIn(
            id = "check-in-0",
            venueId = venue.businessNumber,
            venueName = "카페 전주",
            visitorName = "홍길동",
            phone = visitor.phone,
            checkedInAt = clock.instant(),
        )
        assertEquals(CheckInResult.CheckedIn(expected), result)
        assertEquals(listOf(expected), checkIns.checkInsOn(venue.businessNumber, LocalDate.of(2021, 10, 20)))
        assertEquals(clock.instant(), visits.lastVisitAt(venue.businessNumber))
    }

    @Test
    fun `requires a profile`() = runTest {
        assertEquals(CheckInResult.ProfileMissing, service.checkIn(null, validPass, venue))
        assertTrue(checkIns.all.isEmpty())
    }

    @Test
    fun `requires a verified pass`() = runTest {
        assertEquals(CheckInResult.PassRequired(null), service.checkIn(visitor, null, venue))
    }

    @Test
    fun `requires the pass to be valid today`() = runTest {
        val recent = validPass.copy(record = validPass.record.copy(lastDoseDate = LocalDate.of(2021, 10, 15)))
        assertEquals(
            CheckInResult.PassRequired(PassStatus.NotYetValid(LocalDate.of(2021, 10, 29), 9)),
            service.checkIn(visitor, recent, venue),
        )
        assertTrue(checkIns.all.isEmpty())
    }

    @Test
    fun `ignores a second tap within ten minutes`() = runTest {
        service.checkIn(visitor, validPass, venue)
        val first = clock.instant()
        clock.advance(Duration.ofMinutes(9))

        assertEquals(CheckInResult.AlreadyCheckedIn(first), service.checkIn(visitor, validPass, venue))
        assertEquals(1, checkIns.all.size)
    }

    @Test
    fun `logs a new visit after ten minutes`() = runTest {
        service.checkIn(visitor, validPass, venue)
        clock.advance(Duration.ofMinutes(10))

        assertIs<CheckInResult.CheckedIn>(service.checkIn(visitor, validPass, venue))
        assertEquals(2, checkIns.all.size)
    }

    @Test
    fun `does not treat another venue as a duplicate`() = runTest {
        service.checkIn(visitor, validPass, venue)
        val other = VenueTag(BusinessNumber.parse("220-81-62517")!!, "서점")

        assertIs<CheckInResult.CheckedIn>(service.checkIn(visitor, validPass, other))
    }

    @Test
    fun `reports storage failures instead of throwing`() = runTest {
        checkIns.failure = IOException("disk full")

        val result = assertIs<CheckInResult.Failed>(service.checkIn(visitor, validPass, venue))
        assertEquals("disk full", result.cause.message)
        assertNull(visits.lastVisitAt(venue.businessNumber))
    }

    @Test
    fun `purges entries older than four weeks`() = runTest {
        service.checkIn(visitor, validPass, venue)
        clock.advance(Duration.ofDays(28).plusSeconds(1))
        service.checkIn(visitor, validPass, venue)

        service.purgeExpired()

        assertEquals(listOf(clock.instant()), checkIns.all.map { it.checkedInAt })
        assertEquals(clock.instant(), visits.lastVisitAt(venue.businessNumber))
        assertEquals(1, visits.all.size)
    }
}
