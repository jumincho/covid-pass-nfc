package com.jumincho.cvpass.core.pass

import java.time.Instant
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

/** A [VaccinationRecord] the visitor proved with a certificate at [verifiedAt]. */
data class VerifiedPass(val record: VaccinationRecord, val verifiedAt: Instant)

/**
 * What [CertificateParser] could read from a vaccination certificate. Any field may be
 * missing because recognition can fail on any part of the image.
 */
data class ParsedCertificate(
    /** The holder's name as printed on the certificate, or `null` if none was found. */
    val holderName: String?,
    /** Highest dose number printed (`2차 접종` → 2), or 0 if none was found. */
    val highestDose: Int,
    /** Whether the certificate mentions a booster dose (`추가 접종`). */
    val booster: Boolean,
    /** Whether the certificate states that vaccination is complete (`접종완료`). */
    val seriesComplete: Boolean,
    /** The most recent date that reads as a vaccination date. */
    val lastDoseDate: LocalDate?,
    /** Vaccines named on the certificate. */
    val vaccines: Set<Vaccine>,
) {

    /** Doses that complete the primary series for the vaccines on this certificate. */
    val primarySeriesDoses: Int
        get() = Vaccine.primarySeriesDoses(vaccines)

    /**
     * Doses received. A booster counts as one dose beyond the primary series; a bare
     * "vaccination complete" statement counts as the primary series only when no dose
     * number is printed at all.
     */
    val doses: Int
        get() = when {
            booster -> maxOf(highestDose, primarySeriesDoses + 1)
            highestDose > 0 -> highestDose
            seriesComplete -> primarySeriesDoses
            else -> 0
        }

    /** The facts worth keeping, or `null` if the dose count or the date is missing. */
    fun toRecord(): VaccinationRecord? {
        val date = lastDoseDate ?: return null
        if (doses < 1) return null
        return VaccinationRecord(doses = doses, primarySeriesDoses = primarySeriesDoses, lastDoseDate = date)
    }
}
