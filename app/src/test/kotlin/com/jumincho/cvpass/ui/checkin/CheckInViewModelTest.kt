package com.jumincho.cvpass.ui.checkin

import app.cash.turbine.test
import com.jumincho.cvpass.core.checkin.CheckInService
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.testing.InMemoryCheckInRepository
import com.jumincho.cvpass.core.testing.InMemoryVisitHistory
import com.jumincho.cvpass.core.testing.InMemoryVisitorStore
import com.jumincho.cvpass.core.venue.VenueTagCodec
import com.jumincho.cvpass.core.visitor.VisitorStore
import com.jumincho.cvpass.nfc.TagReadResult
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.time.Duration
import kotlin.test.assertEquals

@ExtendWith(MainDispatcherExtension::class)
class CheckInViewModelTest {

    private val clock = Fixtures.clock()
    private val checkIns = InMemoryCheckInRepository(Fixtures.zone)
    private val service = CheckInService(checkIns, InMemoryVisitHistory(), clock)
    private val venuePayload = VenueTagCodec.encode(Fixtures.venueTag)

    private fun viewModel(
        store: VisitorStore = InMemoryVisitorStore(Fixtures.visitor, Fixtures.validPass),
        initialPayload: ByteArray? = null,
    ) = CheckInViewModel(service, store, initialPayload)

    @Test
    fun `checks in straight away when opened from a tag`() = runTest {
        val viewModel = viewModel(initialPayload = venuePayload)

        assertEquals(CheckInUiState.CheckedIn("카페 전주", Fixtures.now), viewModel.state.value)
        assertEquals(1, checkIns.all.size)
        viewModel.announcements.test { awaitItem() }
    }

    @Test
    fun `waits for a tag otherwise`() = runTest {
        assertEquals(CheckInUiState.Waiting, viewModel().state.value)
    }

    @Test
    fun `checks in when reader mode reads a venue tag`() = runTest {
        val viewModel = viewModel()

        viewModel.onTagRead(TagReadResult.Payload(venuePayload))

        assertEquals(CheckInUiState.CheckedIn("카페 전주", Fixtures.now), viewModel.state.value)
    }

    @Test
    fun `a second tap within ten minutes is not logged again`() = runTest {
        val viewModel = viewModel()
        viewModel.onTagRead(TagReadResult.Payload(venuePayload))
        clock.advance(Duration.ofMinutes(3))

        viewModel.onTagRead(TagReadResult.Payload(venuePayload))

        assertEquals(CheckInUiState.AlreadyCheckedIn("카페 전주", Fixtures.now), viewModel.state.value)
        assertEquals(1, checkIns.all.size)
    }

    @Test
    fun `requires a verified pass`() = runTest {
        val viewModel = viewModel(store = InMemoryVisitorStore(Fixtures.visitor))

        viewModel.onTagRead(TagReadResult.Payload(venuePayload))

        assertEquals(CheckInUiState.PassRequired(null), viewModel.state.value)
    }

    @Test
    fun `explains a pass that is not valid yet`() = runTest {
        val recent = Fixtures.validPass.copy(record = Fixtures.validPass.record.copy(lastDoseDate = Fixtures.today))
        val viewModel = viewModel(store = InMemoryVisitorStore(Fixtures.visitor, recent))

        viewModel.onTagRead(TagReadResult.Payload(venuePayload))

        assertEquals(
            CheckInUiState.PassRequired(PassStatus.NotYetValid(Fixtures.today.plusDays(14), 14)),
            viewModel.state.value,
        )
    }

    @Test
    fun `requires the visitor's details`() = runTest {
        val viewModel = viewModel(store = InMemoryVisitorStore())

        viewModel.onTagRead(TagReadResult.Payload(venuePayload))

        assertEquals(CheckInUiState.ProfileMissing, viewModel.state.value)
    }

    @Test
    fun `explains tags it cannot use`() = runTest {
        val viewModel = viewModel()

        viewModel.onTagRead(TagReadResult.Payload("hello".encodeToByteArray()))
        assertEquals(CheckInUiState.TagError(TagProblem.NOT_VENUE_TAG), viewModel.state.value)

        viewModel.onTagRead(TagReadResult.Payload("""{"v":2}""".encodeToByteArray()))
        assertEquals(CheckInUiState.TagError(TagProblem.UNSUPPORTED_VERSION), viewModel.state.value)

        viewModel.onTagRead(TagReadResult.NotVenueTag)
        assertEquals(CheckInUiState.TagError(TagProblem.NOT_VENUE_TAG), viewModel.state.value)

        viewModel.onTagRead(TagReadResult.ReadFailed)
        assertEquals(CheckInUiState.TagError(TagProblem.READ_FAILED), viewModel.state.value)
    }

    @Test
    fun `reports a storage failure`() = runTest {
        checkIns.failure = IOException("disk full")
        val viewModel = viewModel()

        viewModel.onTagRead(TagReadResult.Payload(venuePayload))

        assertEquals(CheckInUiState.SaveFailed, viewModel.state.value)
    }

    @Test
    fun `scanning again returns to waiting`() = runTest {
        val viewModel = viewModel(initialPayload = venuePayload)

        viewModel.reset()

        assertEquals(CheckInUiState.Waiting, viewModel.state.value)
    }
}
