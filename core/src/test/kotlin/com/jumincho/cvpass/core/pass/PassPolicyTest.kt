package com.jumincho.cvpass.core.pass

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PassPolicyTest {

    private val lastDose = LocalDate.of(2021, 8, 1)

    private fun certificate(
        holderName: String? = "홍길동",
        highestDose: Int = 2,
        booster: Boolean = false,
        lastDoseDate: LocalDate? = lastDose,
        vaccines: Set<Vaccine> = setOf(Vaccine.PFIZER),
    ) = ParsedCertificate(holderName, highestDose, booster, seriesComplete = false, lastDoseDate, vaccines)

    @ParameterizedTest
    @CsvSource(
        "2021-08-01, 14",
        "2021-08-02, 13",
        "2021-08-14, 1",
    )
    fun `a complete series is not valid until 14 days have passed`(today: LocalDate, daysRemaining: Int) {
        assertEquals(
            PassStatus.NotYetValid(validFrom = LocalDate.of(2021, 8, 15), daysRemaining = daysRemaining),
            PassPolicy.evaluate(certificate(), "홍길동", today),
        )
    }

    @ParameterizedTest
    @CsvSource("2021-08-15", "2021-12-31", "2022-06-01")
    fun `a complete series is valid from day 14 on`(today: LocalDate) {
        assertEquals(PassStatus.Valid, PassPolicy.evaluate(certificate(), "홍길동", today))
    }

    @Test
    fun `a single dose of a two-dose vaccine is incomplete`() {
        assertEquals(
            PassStatus.Invalid(PassStatus.Reason.IncompleteSeries(doses = 1, required = 2)),
            PassPolicy.evaluate(certificate(highestDose = 1), "홍길동", LocalDate.of(2021, 12, 1)),
        )
    }

    @Test
    fun `a single Janssen dose completes the series`() {
        val janssen = certificate(highestDose = 1, vaccines = setOf(Vaccine.JANSSEN))
        assertEquals(
            PassStatus.NotYetValid(LocalDate.of(2021, 8, 15), 1),
            PassPolicy.evaluate(janssen, "홍길동", LocalDate.of(2021, 8, 14)),
        )
        assertEquals(PassStatus.Valid, PassPolicy.evaluate(janssen, "홍길동", LocalDate.of(2021, 8, 15)))
    }

    @Test
    fun `a booster counts from the day it is given`() {
        val boosted = certificate(highestDose = 3, lastDoseDate = LocalDate.of(2021, 10, 20))
        assertEquals(PassStatus.Valid, PassPolicy.evaluate(boosted, "홍길동", LocalDate.of(2021, 10, 20)))
        val boosterPhrase = certificate(highestDose = 0, booster = true, lastDoseDate = LocalDate.of(2021, 10, 20))
        assertEquals(PassStatus.Valid, PassPolicy.evaluate(boosterPhrase, "홍길동", LocalDate.of(2021, 10, 20)))
    }

    @Test
    fun `the certificate must name the visitor`() {
        val today = LocalDate.of(2021, 12, 1)
        assertEquals(
            PassStatus.Invalid(PassStatus.Reason.NameNotFound),
            PassPolicy.evaluate(certificate(holderName = null), "홍길동", today),
        )
        assertEquals(
            PassStatus.Invalid(PassStatus.Reason.NameMismatch("김철수")),
            PassPolicy.evaluate(certificate(holderName = "김철수"), "홍길동", today),
        )
        assertEquals(PassStatus.Valid, PassPolicy.evaluate(certificate(holderName = "홍 길동"), "홍길동", today))
    }

    @Test
    fun `explains missing doses or dates`() {
        val today = LocalDate.of(2021, 12, 1)
        assertEquals(
            PassStatus.Invalid(PassStatus.Reason.NoDoseFound),
            PassPolicy.evaluate(certificate(highestDose = 0), "홍길동", today),
        )
        assertEquals(
            PassStatus.Invalid(PassStatus.Reason.NoDoseDate),
            PassPolicy.evaluate(certificate(lastDoseDate = null), "홍길동", today),
        )
    }

    @Test
    fun `re-evaluates a stored record as time passes`() {
        val record = VaccinationRecord(doses = 2, primarySeriesDoses = 2, lastDoseDate = lastDose)
        assertEquals(PassStatus.NotYetValid(LocalDate.of(2021, 8, 15), 5), PassPolicy.evaluate(record, LocalDate.of(2021, 8, 10)))
        assertEquals(PassStatus.Valid, PassPolicy.evaluate(record, LocalDate.of(2021, 8, 15)))
    }

    @Test
    fun `builds the record to keep from a certificate`() {
        assertEquals(
            VaccinationRecord(doses = 3, primarySeriesDoses = 2, lastDoseDate = lastDose),
            certificate(highestDose = 2, booster = true).toRecord(),
        )
    }

    @Test
    fun `rejects impossible records`() {
        assertFailsWith<IllegalArgumentException> { VaccinationRecord(0, 2, lastDose) }
        assertFailsWith<IllegalArgumentException> { VaccinationRecord(2, 3, lastDose) }
    }

    @Test
    fun `evaluates a parsed certificate end to end`() {
        val text = """
            홍길동
            모더나 2차 접종
            접종일자 2021.09.01
            발급일 2021.10.20
        """.trimIndent()
        val parsed = CertificateParser.parse(text, "홍길동", LocalDate.of(2021, 10, 20))
        assertEquals(PassStatus.Valid, PassPolicy.evaluate(parsed, "홍길동", LocalDate.of(2021, 10, 20)))
    }
}
