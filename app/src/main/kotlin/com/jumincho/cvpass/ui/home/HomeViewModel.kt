package com.jumincho.cvpass.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.appContainer
import com.jumincho.cvpass.core.pass.PassPolicy
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueStore
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.core.visitor.VisitorStore
import com.jumincho.cvpass.ui.STOP_TIMEOUT_MILLIS
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/** What the role chooser shows about each role. */
data class HomeUiState(
    val loaded: Boolean = false,
    val profile: VisitorProfile? = null,
    /** Status of the verified pass, or `null` if none was verified. */
    val passStatus: PassStatus? = null,
    val venue: RegisteredVenue? = null,
)

/** Summarises the visitor's pass and the owner's venue for the role chooser. */
class HomeViewModel(visitorStore: VisitorStore, venueStore: VenueStore, clock: Clock) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(visitorStore.profile, visitorStore.pass, venueStore.venue) { profile, pass, venue ->
            HomeUiState(
                loaded = true,
                profile = profile,
                passStatus = pass?.let { PassPolicy.evaluate(it.record, LocalDate.now(clock)) },
                venue = venue,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                HomeViewModel(container.visitorStore, container.venueStore, container.clock)
            }
        }
    }
}
