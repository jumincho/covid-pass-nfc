package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.business.BusinessRegistry
import com.jumincho.cvpass.core.business.BusinessVerification
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VenueRegistrarTest {

    private val number = BusinessNumber.parse("124-81-00998")!!
    private val tag = VenueTag(number, "카페 전주")
    private val registration = BusinessRegistration(number, "홍길동", LocalDate.of(2019, 5, 2))

    @Test
    fun `without a registry only the checksum is trusted`() = runTest {
        val registrar = VenueRegistrar(registry = null)

        assertFalse(registrar.verifiesWithTaxService)
        assertEquals(
            VenueRegistrationResult.Registered(RegisteredVenue(tag, registration, verifiedWithTaxService = false)),
            registrar.register(tag, registration),
        )
    }

    @Test
    fun `a confirmed registration is marked as verified`() = runTest {
        val asked = mutableListOf<BusinessRegistration>()
        val registrar = VenueRegistrar(BusinessRegistry { asked += it; BusinessVerification.Valid })

        assertTrue(registrar.verifiesWithTaxService)
        assertEquals(
            VenueRegistrationResult.Registered(RegisteredVenue(tag, registration, verifiedWithTaxService = true)),
            registrar.register(tag, registration),
        )
        assertEquals(listOf(registration), asked)
    }

    @Test
    fun `any other answer rejects the registration`() = runTest {
        val answers = listOf(
            BusinessVerification.NotMatched("확인할 수 없습니다."),
            BusinessVerification.InvalidKey,
            BusinessVerification.Unexpected("HTTP 500"),
        )
        for (answer in answers) {
            val registrar = VenueRegistrar { answer }
            assertEquals(VenueRegistrationResult.Rejected(answer), registrar.register(tag, registration))
        }
    }

    @Test
    fun `the tag must belong to the registered business`() = runTest {
        val other = VenueTag(BusinessNumber.parse("220-81-62517")!!, "서점")
        assertFailsWith<IllegalArgumentException> { VenueRegistrar(null).register(other, registration) }
    }
}
