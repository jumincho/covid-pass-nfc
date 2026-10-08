package com.jumincho.cvpass.data.local

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.checkin.Visit
import com.jumincho.cvpass.core.checkin.VisitHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

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
