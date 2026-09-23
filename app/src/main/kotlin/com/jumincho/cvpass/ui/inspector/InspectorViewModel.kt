package com.jumincho.cvpass.ui.inspector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.appContainer
import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.checkin.CheckIn
import com.jumincho.cvpass.core.checkin.CheckInRepository
import com.jumincho.cvpass.core.inspector.InspectorGate
import com.jumincho.cvpass.core.venue.VenueStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Results of a visitor-log query. */
sealed interface LogResults {
    data object NotSearched : LogResults

    data object Loading : LogResults

    /** Check-ins at [venueId] on [date], oldest first. */
    data class Loaded(val venueId: BusinessNumber, val date: LocalDate, val checkIns: List<CheckIn>) : LogResults

    data object Failed : LogResults
}

/** Inspector mode: a PIN gate, then a per-venue, per-day visitor log. */
data class InspectorUiState(
    /** `false` when the build has no inspector PIN, which disables the mode. */
    val enabled: Boolean,
    val today: LocalDate,
    val unlocked: Boolean = false,
    val pin: String = "",
    val pinError: Boolean = false,
    /** Digits only; the text field formats them. */
    val businessDigits: String = "",
    val numberProblem: BusinessNumber.Problem? = null,
    val date: LocalDate = today,
    val results: LogResults = LogResults.NotSearched,
    /** The entry whose full details are revealed, if any. */
    val selected: CheckIn? = null,
)

/**
 * Lets an inspector query a venue's visitor log. The list shows masked names and phone
 * numbers; full details are revealed one entry at a time.
 */
class InspectorViewModel(
    private val gate: InspectorGate,
    private val checkIns: CheckInRepository,
    venueStore: VenueStore,
    clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(InspectorUiState(enabled = gate.isEnabled, today = LocalDate.now(clock)))
    val state: StateFlow<InspectorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val venue = venueStore.venue.first() ?: return@launch
            _state.update { if (it.businessDigits.isEmpty()) it.copy(businessDigits = venue.tag.businessNumber.digits) else it }
        }
    }

    fun onPinChange(value: String) {
        _state.update { it.copy(pin = value.take(MAX_PIN_LENGTH), pinError = false) }
    }

    fun unlock() {
        val unlocked = gate.unlocks(_state.value.pin)
        _state.update { it.copy(unlocked = unlocked, pinError = !unlocked, pin = if (unlocked) "" else it.pin) }
    }

    fun onBusinessNumberChange(value: String) {
        val digits = value.filter { it in '0'..'9' }.take(BusinessNumber.LENGTH)
        val problem = if (digits.length == BusinessNumber.LENGTH) BusinessNumber.problemWith(digits) else null
        _state.update { it.copy(businessDigits = digits, numberProblem = problem) }
    }

    fun onDateChange(date: LocalDate) {
        _state.update { it.copy(date = date) }
    }

    fun search() {
        val current = _state.value
        if (!current.unlocked || current.results == LogResults.Loading) return
        val venueId = BusinessNumber.parse(current.businessDigits)
        if (venueId == null) {
            _state.update { it.copy(numberProblem = BusinessNumber.problemWith(current.businessDigits)) }
            return
        }
        _state.update { it.copy(results = LogResults.Loading, selected = null) }
        viewModelScope.launch {
            val results = try {
                LogResults.Loaded(venueId, current.date, checkIns.checkInsOn(venueId, current.date))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogResults.Failed
            }
            _state.update { it.copy(results = results) }
        }
    }

    /** Reveals [checkIn]'s full details, or hides them when `null`. */
    fun select(checkIn: CheckIn?) {
        _state.update { it.copy(selected = checkIn) }
    }

    companion object {
        private const val MAX_PIN_LENGTH = 32

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                InspectorViewModel(container.inspectorGate, container.checkIns, container.venueStore, container.clock)
            }
        }
    }
}
