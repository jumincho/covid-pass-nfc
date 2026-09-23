package com.jumincho.cvpass.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.appContainer
import com.jumincho.cvpass.core.checkin.CheckInResult
import com.jumincho.cvpass.core.checkin.CheckInService
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.core.venue.VenueTagCodec
import com.jumincho.cvpass.core.venue.VenueTagDecoding
import com.jumincho.cvpass.core.visitor.VisitorStore
import com.jumincho.cvpass.nfc.TagReadResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant

/** What went wrong with a tag before check-in could start. */
enum class TagProblem { NOT_VENUE_TAG, UNSUPPORTED_VERSION, READ_FAILED }

/** The check-in screen's states. */
sealed interface CheckInUiState {
    /** Waiting for the visitor to tap a tag. */
    data object Waiting : CheckInUiState

    data object Processing : CheckInUiState

    data class CheckedIn(val venueName: String, val at: Instant) : CheckInUiState

    data class AlreadyCheckedIn(val venueName: String, val previousAt: Instant) : CheckInUiState

    /** A valid pass is needed; [status] is the current one, or `null` if none was verified. */
    data class PassRequired(val status: PassStatus?) : CheckInUiState

    data object ProfileMissing : CheckInUiState

    data class TagError(val problem: TagProblem) : CheckInUiState

    data object SaveFailed : CheckInUiState
}

/**
 * Turns tag reads into check-ins. Tags arrive either from reader mode while the screen is
 * open or, as [initialPayload], from the intent that launched the app.
 */
class CheckInViewModel(
    private val service: CheckInService,
    private val store: VisitorStore,
    initialPayload: ByteArray?,
) : ViewModel() {

    private val _state = MutableStateFlow<CheckInUiState>(CheckInUiState.Waiting)
    val state: StateFlow<CheckInUiState> = _state.asStateFlow()

    private val _announcements = Channel<Unit>(Channel.BUFFERED)

    /** Emits once per successful check-in, to trigger the spoken and haptic confirmation. */
    val announcements: Flow<Unit> = _announcements.receiveAsFlow()

    init {
        if (initialPayload != null) viewModelScope.launch { process(initialPayload) }
    }

    /** Handles a tag read by reader mode. Safe to call from any thread; work happens on the main thread. */
    fun onTagRead(result: TagReadResult) {
        viewModelScope.launch {
            when (result) {
                is TagReadResult.Payload -> process(result.bytes)
                TagReadResult.NotVenueTag -> showTagError(TagProblem.NOT_VENUE_TAG)
                TagReadResult.ReadFailed -> showTagError(TagProblem.READ_FAILED)
            }
        }
    }

    /** Returns to waiting for a tag. */
    fun reset() {
        if (_state.value != CheckInUiState.Processing) _state.value = CheckInUiState.Waiting
    }

    private suspend fun process(payload: ByteArray) {
        when (val decoded = VenueTagCodec.decode(payload)) {
            is VenueTagDecoding.Success -> checkIn(decoded.tag)
            is VenueTagDecoding.UnsupportedVersion -> showTagError(TagProblem.UNSUPPORTED_VERSION)
            is VenueTagDecoding.Malformed -> showTagError(TagProblem.NOT_VENUE_TAG)
        }
    }

    private fun showTagError(problem: TagProblem) {
        if (_state.value != CheckInUiState.Processing) _state.value = CheckInUiState.TagError(problem)
    }

    private suspend fun checkIn(venue: VenueTag) {
        if (_state.value == CheckInUiState.Processing) return
        _state.value = CheckInUiState.Processing
        val result = service.checkIn(store.profile.first(), store.pass.first(), venue)
        _state.value = when (result) {
            is CheckInResult.CheckedIn -> CheckInUiState.CheckedIn(venue.name, result.checkIn.checkedInAt)
            is CheckInResult.AlreadyCheckedIn -> CheckInUiState.AlreadyCheckedIn(venue.name, result.previousAt)
            is CheckInResult.PassRequired -> CheckInUiState.PassRequired(result.status)
            CheckInResult.ProfileMissing -> CheckInUiState.ProfileMissing
            is CheckInResult.Failed -> CheckInUiState.SaveFailed
        }
        if (result is CheckInResult.CheckedIn) _announcements.send(Unit)
    }

    companion object {
        /** Factory for a view model that first processes [initialPayload], if any. */
        fun factory(initialPayload: ByteArray?) = viewModelFactory {
            initializer {
                val container = appContainer
                CheckInViewModel(container.checkInService, container.visitorStore, initialPayload)
            }
        }
    }
}
