package com.jumincho.cvpass.core.checkin

import com.jumincho.cvpass.core.business.BusinessNumber
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The visitor's own history, always kept on the visitor's device. */
interface VisitHistory {

    /** Records [visit]. */
    suspend fun add(visit: Visit)

    /** When the visitor last checked in at [venueId], or `null` if never (or purged). */
    suspend fun lastVisitAt(venueId: BusinessNumber): Instant?

    /** The [limit] most recent visits, newest first, updated as new ones are added. */
    fun recent(limit: Int): Flow<List<Visit>>

    /** Deletes visits made before [cutoff]. */
    suspend fun deleteOlderThan(cutoff: Instant)
}
