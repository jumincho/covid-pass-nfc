package com.jumincho.cvpass.core.pass

import java.time.Instant

/** A [VaccinationRecord] the visitor proved with a certificate at [verifiedAt]. */
data class VerifiedPass(val record: VaccinationRecord, val verifiedAt: Instant)
