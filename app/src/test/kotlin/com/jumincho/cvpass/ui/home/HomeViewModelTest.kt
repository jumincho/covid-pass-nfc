package com.jumincho.cvpass.ui.home

import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.testing.InMemoryVenueStore
import com.jumincho.cvpass.core.testing.InMemoryVisitorStore
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.testing.MainDispatcherExtension
import com.jumincho.cvpass.testing.keepCollecting
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MainDispatcherExtension::class)
class HomeViewModelTest {

    @Test
    fun `starts empty on a fresh install`() = runTest {
        val viewModel = HomeViewModel(InMemoryVisitorStore(), InMemoryVenueStore(), Fixtures.clock())
        keepCollecting(viewModel.state)

        assertEquals(HomeUiState(loaded = true), viewModel.state.value)
    }

    @Test
    fun `summarises the visitor's pass and the owner's venue`() = runTest {
        val viewModel = HomeViewModel(
            InMemoryVisitorStore(Fixtures.visitor, Fixtures.validPass),
            InMemoryVenueStore(Fixtures.venue),
            Fixtures.clock(),
        )
        keepCollecting(viewModel.state)

        assertEquals(
            HomeUiState(loaded = true, profile = Fixtures.visitor, passStatus = PassStatus.Valid, venue = Fixtures.venue),
            viewModel.state.value,
        )
    }
}
