package com.jumincho.cvpass.ui.inspector

import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.checkin.CheckIn
import com.jumincho.cvpass.core.inspector.InspectorGate
import com.jumincho.cvpass.core.testing.InMemoryCheckInRepository
import com.jumincho.cvpass.core.testing.InMemoryVenueStore
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MainDispatcherExtension::class)
class InspectorViewModelTest {

    private val checkIns = InMemoryCheckInRepository(Fixtures.zone)
    private val morning = CheckIn("a", Fixtures.venueNumber, "카페 전주", "홍길동", Fixtures.visitor.phone, Fixtures.now)
    private val noon = morning.copy(id = "b", visitorName = "남궁민수", checkedInAt = Fixtures.now.plus(Duration.ofHours(2)))

    private fun viewModel(pin: String = "4321", venueStore: InMemoryVenueStore = InMemoryVenueStore()) =
        InspectorViewModel(InspectorGate(pin), checkIns, venueStore, Fixtures.clock())

    private fun InspectorViewModel.unlocked() = apply {
        onPinChange("4321")
        unlock()
    }

    @Test
    fun `is disabled without a configured PIN`() = runTest {
        assertFalse(viewModel(pin = "").state.value.enabled)
    }

    @Test
    fun `unlocks with the configured PIN only`() = runTest {
        val viewModel = viewModel()

        viewModel.onPinChange("1234")
        viewModel.unlock()
        assertTrue(viewModel.state.value.pinError)
        assertFalse(viewModel.state.value.unlocked)

        viewModel.onPinChange("4321")
        assertFalse(viewModel.state.value.pinError)
        viewModel.unlock()
        assertTrue(viewModel.state.value.unlocked)
        assertEquals("", viewModel.state.value.pin)
    }

    @Test
    fun `does not search while locked`() = runTest {
        val viewModel = viewModel()
        viewModel.onBusinessNumberChange(Fixtures.venueNumber.digits)

        viewModel.search()

        assertEquals(LogResults.NotSearched, viewModel.state.value.results)
    }

    @Test
    fun `prefills the venue registered on this device`() = runTest {
        val viewModel = viewModel(venueStore = InMemoryVenueStore(Fixtures.venue))

        assertEquals(Fixtures.venueNumber.digits, viewModel.state.value.businessDigits)
    }

    @Test
    fun `lists the day's check-ins oldest first`() = runTest {
        checkIns.add(noon)
        checkIns.add(morning)
        val viewModel = viewModel().unlocked()
        viewModel.onBusinessNumberChange("124-81-00998")

        viewModel.search()

        assertEquals(
            LogResults.Loaded(Fixtures.venueNumber, Fixtures.today, listOf(morning, noon)),
            viewModel.state.value.results,
        )
    }

    @Test
    fun `searches the chosen date`() = runTest {
        checkIns.add(morning)
        val viewModel = viewModel().unlocked()
        viewModel.onBusinessNumberChange(Fixtures.venueNumber.digits)
        viewModel.onDateChange(Fixtures.today.minusDays(1))

        viewModel.search()

        assertEquals(
            LogResults.Loaded(Fixtures.venueNumber, Fixtures.today.minusDays(1), emptyList()),
            viewModel.state.value.results,
        )
    }

    @Test
    fun `validates the business number before searching`() = runTest {
        val viewModel = viewModel().unlocked()
        viewModel.onBusinessNumberChange("1248100997")

        viewModel.search()

        assertEquals(BusinessNumber.Problem.CHECKSUM_MISMATCH, viewModel.state.value.numberProblem)
        assertEquals(LogResults.NotSearched, viewModel.state.value.results)
    }

    @Test
    fun `reports a failed query`() = runTest {
        checkIns.failure = IOException("permission denied")
        val viewModel = viewModel().unlocked()
        viewModel.onBusinessNumberChange(Fixtures.venueNumber.digits)

        viewModel.search()

        assertEquals(LogResults.Failed, viewModel.state.value.results)
    }

    @Test
    fun `reveals one entry at a time`() = runTest {
        val viewModel = viewModel().unlocked()

        viewModel.select(morning)
        assertEquals(morning, viewModel.state.value.selected)

        viewModel.select(null)
        assertNull(viewModel.state.value.selected)
    }
}
