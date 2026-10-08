package com.jumincho.cvpass.core.checkin

import com.jumincho.cvpass.core.business.BusinessNumber
import java.time.Instant

/** An entry in the visitor's own history of places they checked in to. */
data class Visit(
    val venueId: BusinessNumber,
    val venueName: String,
    val visitedAt: Instant,
)
