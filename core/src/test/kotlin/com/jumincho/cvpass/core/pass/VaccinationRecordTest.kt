package com.jumincho.cvpass.core.pass

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertFailsWith

class VaccinationRecordTest {

    private val lastDose = LocalDate.of(2021, 8, 1)

    @Test
    fun `rejects impossible records`() {
        assertFailsWith<IllegalArgumentException> { VaccinationRecord(0, 2, lastDose) }
        assertFailsWith<IllegalArgumentException> { VaccinationRecord(2, 3, lastDose) }
        assertFailsWith<IllegalArgumentException> { VaccinationRecord(2, 0, lastDose) }
    }
}
