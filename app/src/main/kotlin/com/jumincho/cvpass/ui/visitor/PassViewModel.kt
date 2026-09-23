package com.jumincho.cvpass.ui.visitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.appContainer
import com.jumincho.cvpass.core.checkin.Visit
import com.jumincho.cvpass.core.checkin.VisitHistory
import com.jumincho.cvpass.core.pass.CertificateParser
import com.jumincho.cvpass.core.pass.PassPolicy
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.core.visitor.VisitorStore
import com.jumincho.cvpass.ocr.CertificateTextReader
import com.jumincho.cvpass.ui.STOP_TIMEOUT_MILLIS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Progress of verifying a newly picked certificate. */
sealed interface VerificationState {
    data object Idle : VerificationState

    data object Reading : VerificationState

    /** The certificate was accepted and saved; [status] is the resulting pass status. */
    data class Accepted(val status: PassStatus) : VerificationState

    /** The certificate did not qualify, for [reason]; nothing was saved. */
    data class Rejected(val reason: PassStatus.Reason) : VerificationState

    /** The image could not be read at all. */
    data object ReadFailed : VerificationState
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
 * Shows the visitor's pass and verifies certificates: the picked image is read on the
 * device, parsed, and checked against [PassPolicy]. Only the derived record is saved.
 */
class PassViewModel(
    private val store: VisitorStore,
    visits: VisitHistory,
    private val reader: CertificateTextReader,
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PassUiState())

    /** Reads and evaluates the certificate image at [imageUri]. */
    fun onCertificatePicked(imageUri: String) {
        if (verification.value == VerificationState.Reading) return
        verification.value = VerificationState.Reading
        viewModelScope.launch {
            verification.value = verify(imageUri)
        }
    }

    /** Hides the result of the last verification. */
    fun dismissVerification() {
        verification.value = VerificationState.Idle
    }

    private suspend fun verify(imageUri: String): VerificationState {
        val profile = store.profile.first() ?: return VerificationState.Idle
        val text = try {
            reader.read(imageUri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return VerificationState.ReadFailed
        }
        val today = LocalDate.now(clock)
        val certificate = CertificateParser.parse(text, profile.name, today)
        val status = PassPolicy.evaluate(certificate, profile.name, today)
        if (status is PassStatus.Invalid) return VerificationState.Rejected(status.reason)
        val record = certificate.toRecord() ?: return VerificationState.ReadFailed
        store.savePass(VerifiedPass(record, clock.instant()))
        return VerificationState.Accepted(status)
    }

    companion object {
        private const val RECENT_VISITS = 5

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                PassViewModel(container.visitorStore, container.visits, container.certificateReader, container.clock)
            }
        }
    }
}
