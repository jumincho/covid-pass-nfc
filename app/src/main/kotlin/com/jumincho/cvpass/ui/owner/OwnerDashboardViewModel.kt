package com.jumincho.cvpass.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.appContainer
import com.jumincho.cvpass.core.checkin.CheckInRepository
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueStore
import com.jumincho.cvpass.nfc.TagWriteResult
import com.jumincho.cvpass.ui.STOP_TIMEOUT_MILLIS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Progress of writing the venue to an NFC tag. */
sealed interface TagWriteState {
    data object Idle : TagWriteState

    /** Reader mode is on, waiting for the owner to hold a tag to the phone. */
    data object Waiting : TagWriteState

    data class Finished(val result: TagWriteResult) : TagWriteState
}

/** The owner's dashboard. */
data class OwnerDashboardUiState(
    val loaded: Boolean = false,
    val venue: RegisteredVenue? = null,
    /** Check-ins today, or `null` while loading or when unavailable. */
    val visitorsToday: Int? = null,
    val countFailed: Boolean = false,
    val tagWrite: TagWriteState = TagWriteState.Idle,
)

private data class VenueSummary(val venue: RegisteredVenue?, val visitorsToday: Int?, val countFailed: Boolean)

/** Shows the registered venue with today's visitor count and writes its NFC tag. */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerDashboardViewModel(
    private val store: VenueStore,
    checkIns: CheckInRepository,
    clock: Clock,
) : ViewModel() {

    private val tagWrite = MutableStateFlow<TagWriteState>(TagWriteState.Idle)

    private val summary = store.venue.flatMapLatest { venue ->
        if (venue == null) {
            flowOf(VenueSummary(null, null, countFailed = false))
        } else {
            checkIns.countOn(venue.tag.businessNumber, LocalDate.now(clock))
                .map { VenueSummary(venue, it, countFailed = false) }
                .catch { emit(VenueSummary(venue, null, countFailed = true)) }
        }
    }

    val state: StateFlow<OwnerDashboardUiState> =
        combine(summary, tagWrite) { summary, write ->
            OwnerDashboardUiState(
                loaded = true,
                venue = summary.venue,
                visitorsToday = summary.visitorsToday,
                countFailed = summary.countFailed,
                tagWrite = write,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OwnerDashboardUiState())

    /** Enables tag writing until a tag is written or [cancelTagWrite] is called. */
    fun startTagWrite() {
        tagWrite.value = TagWriteState.Waiting
    }

    fun cancelTagWrite() {
        tagWrite.value = TagWriteState.Idle
    }

    /** Records the outcome of a write. Safe to call from the reader-mode thread. */
    fun onTagWritten(result: TagWriteResult) {
        viewModelScope.launch {
            if (tagWrite.value == TagWriteState.Waiting) tagWrite.value = TagWriteState.Finished(result)
        }
    }

    /** Forgets the venue so that the owner can register another one. */
    fun changeVenue() {
        viewModelScope.launch { store.clear() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                OwnerDashboardViewModel(container.venueStore, container.checkIns, container.clock)
            }
        }
    }
}
