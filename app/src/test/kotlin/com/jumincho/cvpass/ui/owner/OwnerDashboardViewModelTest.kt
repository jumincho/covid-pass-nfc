package com.jumincho.cvpass.ui.owner

import com.jumincho.cvpass.core.checkin.CheckIn
import com.jumincho.cvpass.core.testing.InMemoryCheckInRepository
import com.jumincho.cvpass.core.testing.InMemoryVenueStore
import com.jumincho.cvpass.nfc.TagWriteResult
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.testing.MainDispatcherExtension
import com.jumincho.cvpass.testing.keepCollecting
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MainDispatcherExtension::class)
class OwnerDashboardViewModelTest {

    private val checkIns = InMemoryCheckInRepository(Fixtures.zone)
    private val store = InMemoryVenueStore(Fixtures.venue)

    // Created on first use, after MainDispatcherExtension has installed the test dispatcher.
    private val viewModel by lazy { OwnerDashboardViewModel(store, checkIns, Fixtures.clock()) }

    private fun checkInAt(instant: Instant, id: String) =
        CheckIn(id, Fixtures.venueNumber, "카페 전주", "홍길동", Fixtures.visitor.phone, instant)

    @Test
    fun `counts today's visitors as they arrive`() = runTest {
        checkIns.add(checkInAt(Fixtures.now.minus(Duration.ofDays(1)), "yesterday"))
        checkIns.add(checkInAt(Fixtures.now, "today-1"))
        keepCollecting(viewModel.state)
        assertEquals(1, viewModel.state.value.visitorsToday)

        checkIns.add(checkInAt(Fixtures.now.plusSeconds(60), "today-2"))

        assertEquals(2, viewModel.state.value.visitorsToday)
        assertEquals(Fixtures.venue, viewModel.state.value.venue)
    }

    @Test
    fun `shows when the count is unavailable`() = runTest {
        checkIns.failure = IOException("offline")
        keepCollecting(viewModel.state)

        assertTrue(viewModel.state.value.countFailed)
        assertNull(viewModel.state.value.visitorsToday)
    }

    @Test
    fun `tracks a tag write from start to finish`() = runTest {
        keepCollecting(viewModel.state)

        viewModel.startTagWrite()
        assertEquals(TagWriteState.Waiting, viewModel.state.value.tagWrite)

        viewModel.onTagWritten(TagWriteResult.TooSmall(requiredBytes = 150, capacityBytes = 137))
        assertEquals(TagWriteState.Finished(TagWriteResult.TooSmall(150, 137)), viewModel.state.value.tagWrite)
    }

    @Test
    fun `ignores tags when not waiting for one`() = runTest {
        keepCollecting(viewModel.state)

        viewModel.onTagWritten(TagWriteResult.Written)

        assertEquals(TagWriteState.Idle, viewModel.state.value.tagWrite)
    }

    @Test
    fun `cancelling stops waiting for a tag`() = runTest {
        keepCollecting(viewModel.state)
        viewModel.startTagWrite()

        viewModel.cancelTagWrite()

        assertEquals(TagWriteState.Idle, viewModel.state.value.tagWrite)
    }

    @Test
    fun `changing venue forgets the current one`() = runTest {
        keepCollecting(viewModel.state)

        viewModel.changeVenue()

        assertNull(store.saved)
        assertNull(viewModel.state.value.venue)
        assertTrue(viewModel.state.value.loaded)
    }
}
