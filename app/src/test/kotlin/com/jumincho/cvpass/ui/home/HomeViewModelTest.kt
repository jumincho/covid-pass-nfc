package com.jumincho.cvpass.ui.home

import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.testing.InMemoryVenueStore
import com.jumincho.cvpass.core.testing.InMemoryVisitorStore
import com.jumincho.cvpass.data.StorageBackend
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.testing.MainDispatcherExtension
import com.jumincho.cvpass.testing.keepCollecting
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MainDispatcherExtension::class)
class HomeViewModelTest {

    private fun viewModel(
        visitorStore: InMemoryVisitorStore = InMemoryVisitorStore(),
        venueStore: InMemoryVenueStore = InMemoryVenueStore(),
        storage: StorageBackend = StorageBackend.Device,
        verifiesWithTaxService: Boolean = false,
    ) = HomeViewModel(visitorStore, venueStore, Fixtures.clock(), storage, verifiesWithTaxService)

    @Test
    fun `starts empty on a fresh install`() = runTest {
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        assertEquals(HomeUiState(StorageBackend.Device, verifiesWithTaxService = false, loaded = true), viewModel.state.value)
    }

    @Test
    fun `summarises the visitor's pass and the owner's venue`() = runTest {
        val viewModel = viewModel(InMemoryVisitorStore(Fixtures.visitor, Fixtures.validPass), InMemoryVenueStore(Fixtures.venue))
        keepCollecting(viewModel.state)

        assertEquals(
            HomeUiState(
                storage = StorageBackend.Device,
                verifiesWithTaxService = false,
                loaded = true,
                profile = Fixtures.visitor,
                passStatus = PassStatus.Valid,
                venue = Fixtures.venue,
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `shows the configured backends from the start`() = runTest {
        val viewModel = viewModel(storage = StorageBackend.Firestore("cv-pass-demo"), verifiesWithTaxService = true)

        val state = viewModel.state.value
        assertEquals(StorageBackend.Firestore("cv-pass-demo"), state.storage)
        assertEquals(true, state.verifiesWithTaxService)
    }
}
