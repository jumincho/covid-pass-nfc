package com.jumincho.cvpass.ui.visitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.core.checkin.Visit
import com.jumincho.cvpass.core.checkin.VisitHistory
import com.jumincho.cvpass.core.pass.PassPolicy
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.pass.PassVerificationResult
import com.jumincho.cvpass.core.pass.PassVerifier
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.core.visitor.VisitorStore
import com.jumincho.cvpass.di.appContainer
import com.jumincho.cvpass.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Progress of verifying a newly picked certificate. */
sealed interface VerificationState {
    /** No verification is in progress or being shown. */
    data object Idle : VerificationState

    /** The picked image is being read. */
    data object Reading : VerificationState

    /** The certificate was accepted and saved; [status] is the resulting pass status. */
    data class Accepted(val status: PassStatus) : VerificationState

    /** The certificate did not qualify, for [reason]; nothing was saved. */
    data class Rejected(val reason: PassStatus.Reason) : VerificationState

    /** The image could not be read at all. */
    data object ReadFailed : VerificationState

    /** The certificate was accepted, but the pass could not be saved. */
    data object SaveFailed : VerificationState
}

/** Everything the pass screen shows. */
data class PassUiState(
    val loaded: Boolean = false,
    val profile: VisitorProfile? = null,
    val pass: VerifiedPass? = null,
    /** Status of [pass] today, or `null` if no pass was verified. */
    val status: PassStatus? = null,
    val recentVisits: List<Visit> = emptyList(),
    val verification: VerificationState = VerificationState.Idle,
)

/**
 * Shows the visitor's pass and verifies newly picked certificates with [PassVerifier], which
 * reads the image on the device and saves only the facts derived from it.
 */
class PassViewModel(
    store: VisitorStore,
    visits: VisitHistory,
    private val verifier: PassVerifier,
    private val clock: Clock,
) : ViewModel() {

    private val verification = MutableStateFlow<VerificationState>(VerificationState.Idle)

    val state: StateFlow<PassUiState> =
        combine(store.profile, store.pass, visits.recent(RECENT_VISITS), verification) { profile, pass, recent, verifying ->
            PassUiState(
                loaded = true,
                profile = profile,
                pass = pass,
                status = pass?.let { PassPolicy.evaluate(it.record, LocalDate.now(clock)) },
                recentVisits = recent,
                verification = verifying,
            )
        }.stateIn(viewModelScope, WhileUiSubscribed, PassUiState())

    /** Reads and evaluates the certificate image at [imageUri]. */
    fun onCertificatePicked(imageUri: String) {
        if (verification.value == VerificationState.Reading) return
        verification.value = VerificationState.Reading
        viewModelScope.launch {
            verification.value = when (val result = verifier.verify(imageUri)) {
                is PassVerificationResult.Accepted -> VerificationState.Accepted(result.status)
                is PassVerificationResult.Rejected -> VerificationState.Rejected(result.reason)
                is PassVerificationResult.Unreadable -> VerificationState.ReadFailed
                is PassVerificationResult.Failed -> VerificationState.SaveFailed
                PassVerificationResult.ProfileMissing -> VerificationState.Idle // The pass screen needs a profile to open.
            }
        }
    }

    /** Hides the result of the last verification. */
    fun dismissVerification() {
        verification.value = VerificationState.Idle
    }

    companion object {
        private const val RECENT_VISITS = 5

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                PassViewModel(container.visitorStore, container.visits, container.passVerifier, container.clock)
            }
        }
    }
}
