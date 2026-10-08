package com.jumincho.cvpass.core.pass

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParsedCertificateTest {

    private val lastDose = LocalDate.of(2021, 8, 1)

    private fun certificate(
        highestDose: Int = 0,
        booster: Boolean = false,
        seriesComplete: Boolean = false,
        lastDoseDate: LocalDate? = lastDose,
        vaccines: Set<Vaccine> = setOf(Vaccine.PFIZER),
    ) = ParsedCertificate("홍길동", highestDose, booster, seriesComplete, lastDoseDate, vaccines)

    @Test
    fun `counts the highest dose number printed`() {
        assertEquals(2, certificate(highestDose = 2).doses)
        assertEquals(0, certificate().doses)
    }

    @Test
    fun `counts a booster as one dose beyond the primary series`() {
        assertEquals(3, certificate(booster = true).doses)
        assertEquals(2, certificate(booster = true, vaccines = setOf(Vaccine.JANSSEN)).doses)
        assertEquals(4, certificate(highestDose = 4, booster = true).doses)
    }

    @Test
    fun `counts a bare completion statement as the primary series`() {
        assertEquals(2, certificate(seriesComplete = true).doses)
        assertEquals(1, certificate(seriesComplete = true, vaccines = setOf(Vaccine.JANSSEN)).doses)
        assertEquals(1, certificate(highestDose = 1, seriesComplete = true).doses)
    }

    @Test
    fun `builds the record to keep`() {
        assertEquals(
            VaccinationRecord(doses = 3, primarySeriesDoses = 2, lastDoseDate = lastDose),
            certificate(highestDose = 2, booster = true).toRecord(),
        )
    }

    @Test
    fun `has no record without a dose or a date`() {
        assertNull(certificate().toRecord())
        assertNull(certificate(highestDose = 2, lastDoseDate = null).toRecord())
    }
}
