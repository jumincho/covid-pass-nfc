package com.jumincho.cvpass.core.pass

import com.jumincho.cvpass.core.visitor.PersonName
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Whether a visitor's vaccination currently qualifies for entry. */
sealed interface PassStatus {

    /** The pass is valid today. */
    data object Valid : PassStatus

    /** The primary series is complete but the waiting period has not elapsed yet. */
    data class NotYetValid(val validFrom: LocalDate, val daysRemaining: Int) : PassStatus

    /** The certificate or record does not qualify, for [reason]. */
    data class Invalid(val reason: Reason) : PassStatus

    /** Why a pass is [Invalid]. */
    sealed interface Reason {
        /** The visitor's name could not be found on the certificate. */
        data object NameNotFound : Reason

        /** The certificate names someone else. */
        data class NameMismatch(val nameOnCertificate: String) : Reason

        /** No dose marker such as `2차 접종` could be read. */
        data object NoDoseFound : Reason

        /** No vaccination date could be read. */
        data object NoDoseDate : Reason

        /** Fewer doses than the primary series requires. */
        data class IncompleteSeries(val doses: Int, val required: Int) : Reason
    }
}

/**
 * The entry rule, kept in one place so that it is easy to audit or change.
 *
 * It models Korea's vaccine-pass rule as applied in 2021–22: the primary series (two
 * doses, or one of the Janssen vaccine) counts from [WAITING_PERIOD_DAYS] days after its
 * last dose, and a booster counts from the day it is given. The 180-day expiry
 * introduced on 3 January 2022 is not modelled.
 */
object PassPolicy {

    /** Days that must pass after the last dose of the primary series. */
    const val WAITING_PERIOD_DAYS: Long = 14

    /**
     * Evaluates a freshly parsed [certificate] for the visitor called [profileName]. The
     * holder's name must be on the certificate and must match the profile.
     */
    fun evaluate(certificate: ParsedCertificate, profileName: String, today: LocalDate): PassStatus {
        val holder = certificate.holderName ?: return PassStatus.Invalid(PassStatus.Reason.NameNotFound)
        if (!PersonName.matches(holder, profileName)) {
            return PassStatus.Invalid(PassStatus.Reason.NameMismatch(holder))
        }
        val record = certificate.toRecord() ?: return PassStatus.Invalid(
            if (certificate.doses == 0) PassStatus.Reason.NoDoseFound else PassStatus.Reason.NoDoseDate,
        )
        return evaluate(record, today)
    }

    /** Evaluates a stored [record] on [today]. */
    fun evaluate(record: VaccinationRecord, today: LocalDate): PassStatus = when {
        record.doses < record.primarySeriesDoses ->
            PassStatus.Invalid(PassStatus.Reason.IncompleteSeries(record.doses, record.primarySeriesDoses))

        record.doses > record.primarySeriesDoses -> PassStatus.Valid

        else -> {
            val validFrom = record.lastDoseDate.plusDays(WAITING_PERIOD_DAYS)
            if (today.isBefore(validFrom)) {
                PassStatus.NotYetValid(validFrom, ChronoUnit.DAYS.between(today, validFrom).toInt())
            } else {
                PassStatus.Valid
            }
        }
    }
}
