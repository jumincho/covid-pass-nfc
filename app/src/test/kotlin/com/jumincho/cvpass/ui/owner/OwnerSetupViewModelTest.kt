package com.jumincho.cvpass.ui.owner

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.business.BusinessRegistry
import com.jumincho.cvpass.core.business.BusinessVerification
import com.jumincho.cvpass.core.testing.InMemoryVenueStore
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueRegistrar
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MainDispatcherExtension::class)
class OwnerSetupViewModelTest {

    private val store = InMemoryVenueStore()

    private fun viewModel(registry: BusinessRegistry? = null) =
        OwnerSetupViewModel(VenueRegistrar(registry), store, Fixtures.clock())

    private fun OwnerSetupViewModel.fillIn(openingDate: LocalDate = LocalDate.of(2019, 5, 2)) {
        onBusinessNumberChange("124-81-00998")
        onRepresentativeChange("김사장")
        onOpeningDateChange(openingDate)
        onVenueNameChange("카페 전주")
    }

    @Test
    fun `validates the checksum once all ten digits are typed`() = runTest {
        val viewModel = viewModel()

        viewModel.onBusinessNumberChange("124810099")
        assertNull(viewModel.state.value.numberProblem)

        viewModel.onBusinessNumberChange("124-81-00997")
        assertEquals(BusinessNumber.Problem.CHECKSUM_MISMATCH, viewModel.state.value.numberProblem)
        assertEquals("1248100997", viewModel.state.value.businessDigits)

        viewModel.onBusinessNumberChange("1248100998")
        assertNull(viewModel.state.value.numberProblem)
    }

    @Test
    fun `flags every missing field`() = runTest {
        val viewModel = viewModel()

        viewModel.submit()

        val state = viewModel.state.value
        assertEquals(BusinessNumber.Problem.WRONG_FORMAT, state.numberProblem)
        assertTrue(state.representativeError)
        assertTrue(state.openingDateError)
        assertTrue(state.venueNameError)
        assertNull(store.saved)
    }

    @Test
    fun `rejects an opening date in the future`() = runTest {
        val viewModel = viewModel()
        viewModel.fillIn(openingDate = Fixtures.today.plusDays(1))

        viewModel.submit()

        assertTrue(viewModel.state.value.openingDateError)
        assertNull(store.saved)
    }

    @Test
    fun `without an API key the venue is saved as not verified`() = runTest {
        val viewModel = viewModel(registry = null)
        assertFalse(viewModel.verifiesWithTaxService)
        viewModel.fillIn()

        viewModel.submit()

        assertTrue(viewModel.state.value.registered)
        assertEquals(
            RegisteredVenue(
                tag = VenueTag(Fixtures.venueNumber, "카페 전주"),
                registration = BusinessRegistration(Fixtures.venueNumber, "김사장", LocalDate.of(2019, 5, 2)),
                verifiedWithTaxService = false,
            ),
            store.saved,
        )
    }

    @Test
    fun `a venue confirmed by the tax service is saved as verified`() = runTest {
        val asked = mutableListOf<BusinessRegistration>()
        val viewModel = viewModel(
            registry = BusinessRegistry {
                asked += it
                BusinessVerification.Valid
            },
        )
        viewModel.fillIn()

        viewModel.submit()

        assertEquals(true, store.saved?.verifiedWithTaxService)
        assertEquals(listOf(BusinessRegistration(Fixtures.venueNumber, "김사장", LocalDate.of(2019, 5, 2))), asked)
    }

    @Test
    fun `a rejection is shown and nothing is saved`() = runTest {
        val viewModel = viewModel(registry = BusinessRegistry { BusinessVerification.NotMatched("확인할 수 없습니다.") })
        viewModel.fillIn()

        viewModel.submit()

        val state = viewModel.state.value
        assertEquals(BusinessVerification.NotMatched("확인할 수 없습니다."), state.rejection)
        assertFalse(state.registered)
        assertFalse(state.submitting)
        assertNull(store.saved)
    }

    @Test
    fun `editing a field clears the previous rejection`() = runTest {
        val viewModel = viewModel(registry = BusinessRegistry { BusinessVerification.InvalidKey })
        viewModel.fillIn()
        viewModel.submit()

        viewModel.onRepresentativeChange("김대표")

        assertNull(viewModel.state.value.rejection)
    }
}
