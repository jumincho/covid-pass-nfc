package com.jumincho.cvpass.core.pass

import java.time.LocalDate

/**
 * The only vaccination data CV-PASS keeps: facts derived from a certificate, never the
 * certificate image or its recognised text.
 */
data class VaccinationRecord(
    /** Doses received, counting a booster as one dose beyond the primary series. */
    val doses: Int,
    /** Doses that complete the primary series (1 for Janssen, otherwise 2). */
    val primarySeriesDoses: Int,
    /** Date of the most recent dose. */
    val lastDoseDate: LocalDate,
) {
    init {
        require(doses >= 1) { "A record needs at least one dose" }
        require(primarySeriesDoses in 1..2) { "Primary series has one or two doses" }
    }
}
