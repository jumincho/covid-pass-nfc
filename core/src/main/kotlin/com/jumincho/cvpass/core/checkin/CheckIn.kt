package com.jumincho.cvpass.core.checkin

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.visitor.PhoneNumber
import java.time.Instant

/** One entry in a venue's visitor log, as kept for contact tracing. */
data class CheckIn(
    /** Stable identifier, unique across devices. */
    val id: String,
    val venueId: BusinessNumber,
    val venueName: String,
    val visitorName: String,
    val phone: PhoneNumber,
    val checkedInAt: Instant,
)
