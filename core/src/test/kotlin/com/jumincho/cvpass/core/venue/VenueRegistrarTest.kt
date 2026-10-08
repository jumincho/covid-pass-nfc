package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.business.BusinessRegistry
import com.jumincho.cvpass.core.business.BusinessVerification
import com.jumincho.cvpass.core.testing.InMemoryVenueStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VenueRegistrarTest {

    private val number = BusinessNumber.parse("124-81-00998")!!
    private val tag = VenueTag(number, "카페 전주")
    private val registration = BusinessRegistration(number, "홍길동", LocalDate.of(2019, 5, 2))
    private val store = InMemoryVenueStore()

    @Test
    fun `without a registry only the checksum is trusted`() = runTest {
        val registrar = VenueRegistrar(registry = null, store)

        assertFalse(registrar.verifiesWithTaxService)
        val venue = RegisteredVenue(tag, registration, verifiedWithTaxService = false)
        assertEquals(VenueRegistrationResult.Registered(venue), registrar.register(tag, registration))
        assertEquals(venue, store.saved)
    }

    @Test
    fun `a confirmed registration is saved as verified`() = runTest {
        val asked = mutableListOf<BusinessRegistration>()
        val registry = BusinessRegistry {
            asked += it
            BusinessVerification.Valid
        }
        val registrar = VenueRegistrar(registry, store)

        assertTrue(registrar.verifiesWithTaxService)
        val venue = RegisteredVenue(tag, registration, verifiedWithTaxService = true)
        assertEquals(VenueRegistrationResult.Registered(venue), registrar.register(tag, registration))
        assertEquals(venue, store.saved)
        assertEquals(listOf(registration), asked)
    }

    @Test
    fun `any other answer rejects the registration and saves nothing`() = runTest {
        val answers = listOf(
            BusinessVerification.NotMatched("확인할 수 없습니다."),
            BusinessVerification.InvalidKey,
            BusinessVerification.NetworkError(IOException("offline")),
            BusinessVerification.Unexpected("HTTP 500"),
        )
        for (answer in answers) {
            val registrar = VenueRegistrar({ answer }, store)
            assertEquals(VenueRegistrationResult.Rejected(answer), registrar.register(tag, registration))
        }
        assertNull(store.saved)
    }

    @Test
    fun `reports a venue that could not be saved`() = runTest {
        store.failure = IOException("disk full")

        val result = VenueRegistrar(registry = null, store).register(tag, registration)

        assertEquals("disk full", assertIs<VenueRegistrationResult.Failed>(result).cause.message)
    }

    @Test
    fun `the tag must belong to the registered business`() = runTest {
        val other = VenueTag(BusinessNumber.parse("220-81-62517")!!, "서점")
        assertFailsWith<IllegalArgumentException> { VenueRegistrar(registry = null, store).register(other, registration) }
    }
}
