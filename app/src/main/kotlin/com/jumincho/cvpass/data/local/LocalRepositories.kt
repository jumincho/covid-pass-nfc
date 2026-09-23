package com.jumincho.cvpass.data.local

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.checkin.CheckIn
import com.jumincho.cvpass.core.checkin.CheckInRepository
import com.jumincho.cvpass.core.checkin.Visit
import com.jumincho.cvpass.core.checkin.VisitHistory
import com.jumincho.cvpass.core.visitor.PhoneNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** [CheckInRepository] backed by Room, so the whole flow works on a single device. */
class LocalCheckInRepository(private val dao: CheckInDao, private val zone: ZoneId) : CheckInRepository {

    override suspend fun add(checkIn: CheckIn) {
        dao.insert(
            CheckInEntity(
                id = checkIn.id,
                venueId = checkIn.venueId.digits,
                venueName = checkIn.venueName,
                visitorName = checkIn.visitorName,
                phone = checkIn.phone.digits,
                checkedInAt = checkIn.checkedInAt.toEpochMilli(),
            ),
        )
    }

    override suspend fun checkInsOn(venueId: BusinessNumber, date: LocalDate): List<CheckIn> {
        val (from, until) = dayBounds(date)
        return dao.between(venueId.digits, from, until).mapNotNull { it.toCheckIn() }
    }

    override fun countOn(venueId: BusinessNumber, date: LocalDate): Flow<Int> {
        val (from, until) = dayBounds(date)
        return dao.countBetween(venueId.digits, from, until)
    }

    override suspend fun deleteOlderThan(cutoff: Instant) {
        dao.deleteBefore(cutoff.toEpochMilli())
    }

    private fun dayBounds(date: LocalDate): Pair<Long, Long> =
        date.atStartOfDay(zone).toInstant().toEpochMilli() to
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun CheckInEntity.toCheckIn(): CheckIn? {
        val venue = BusinessNumber.parse(venueId) ?: return null
        val phoneNumber = PhoneNumber.parse(phone) ?: return null
        return CheckIn(id, venue, venueName, visitorName, phoneNumber, Instant.ofEpochMilli(checkedInAt))
    }
}

/** [VisitHistory] backed by Room; always local, whatever backend the venue log uses. */
class LocalVisitHistory(private val dao: VisitDao) : VisitHistory {

    override suspend fun add(visit: Visit) {
        dao.insert(
            VisitEntity(
                venueId = visit.venueId.digits,
                venueName = visit.venueName,
                visitedAt = visit.visitedAt.toEpochMilli(),
            ),
        )
    }

    override suspend fun lastVisitAt(venueId: BusinessNumber): Instant? =
        dao.lastVisitAt(venueId.digits)?.let(Instant::ofEpochMilli)

    override fun recent(limit: Int): Flow<List<Visit>> =
        dao.recent(limit).map { rows ->
            rows.mapNotNull { row ->
                BusinessNumber.parse(row.venueId)?.let { Visit(it, row.venueName, Instant.ofEpochMilli(row.visitedAt)) }
            }
        }

    override suspend fun deleteOlderThan(cutoff: Instant) {
        dao.deleteBefore(cutoff.toEpochMilli())
    }
}
