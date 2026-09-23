package com.jumincho.cvpass.testing

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.pass.VaccinationRecord
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.testing.MutableClock
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.core.visitor.VisitorProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Shared test data: one visitor, one venue, a clock on 20 October 2021 at noon in Seoul. */
object Fixtures {
    val zone: ZoneId = ZoneId.of("Asia/Seoul")
    val now: Instant = Instant.parse("2021-10-20T03:00:00Z")
    val today: LocalDate = LocalDate.of(2021, 10, 20)

    fun clock() = MutableClock(now, zone)

    val visitor = VisitorProfile("홍길동", PhoneNumber.parse("010-1234-5678")!!)

    val validPass = VerifiedPass(
        VaccinationRecord(doses = 2, primarySeriesDoses = 2, lastDoseDate = LocalDate.of(2021, 8, 1)),
        verifiedAt = Instant.parse("2021-10-01T00:00:00Z"),
    )

    val venueNumber = BusinessNumber.parse("124-81-00998")!!
    val venueTag = VenueTag(venueNumber, "카페 전주")

    val venue = RegisteredVenue(
        tag = venueTag,
        registration = BusinessRegistration(venueNumber, "김사장", LocalDate.of(2019, 5, 2)),
        verifiedWithTaxService = false,
    )
}
