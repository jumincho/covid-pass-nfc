package com.jumincho.cvpass.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.core.pass.PassPolicy
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueStore
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.core.visitor.VisitorStore
import com.jumincho.cvpass.data.StorageBackend
import com.jumincho.cvpass.di.appContainer
import com.jumincho.cvpass.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/** What the role chooser shows about each role and about this build's backends. */
data class HomeUiState(
    /** Where check-ins are stored. */
    val storage: StorageBackend,
    /** Whether venues are confirmed with the National Tax Service or only checked locally. */
    val verifiesWithTaxService: Boolean,
    val loaded: Boolean = false,
    val profile: VisitorProfile? = null,
    /** Status of the verified pass, or `null` if none was verified. */
    val passStatus: PassStatus? = null,
    val venue: RegisteredVenue? = null,
)

/** Summarises the visitor's pass and the owner's venue for the role chooser. */
class HomeViewModel(
    visitorStore: VisitorStore,
    venueStore: VenueStore,
    clock: Clock,
    storage: StorageBackend,
    verifiesWithTaxService: Boolean,
) : ViewModel() {

    private val initialState = HomeUiState(storage, verifiesWithTaxService)

    val state: StateFlow<HomeUiState> =
        combine(visitorStore.profile, visitorStore.pass, venueStore.venue) { profile, pass, venue ->
            initialState.copy(
                loaded = true,
                profile = profile,
                passStatus = pass?.let { PassPolicy.evaluate(it.record, LocalDate.now(clock)) },
                venue = venue,
            )
        }.stateIn(viewModelScope, WhileUiSubscribed, initialState)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                HomeViewModel(
                    visitorStore = container.visitorStore,
                    venueStore = container.venueStore,
                    clock = container.clock,
                    storage = container.storage,
                    verifiesWithTaxService = container.venueRegistrar.verifiesWithTaxService,
                )
            }
        }
    }
}
