package com.jumincho.cvpass.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.business.BusinessVerification
import com.jumincho.cvpass.core.venue.VenueRegistrar
import com.jumincho.cvpass.core.venue.VenueRegistrationResult
import com.jumincho.cvpass.core.venue.VenueStore
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.core.visitor.PersonName
import com.jumincho.cvpass.di.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** The venue registration form. */
data class OwnerSetupUiState(
    /** Whether submissions are confirmed with the National Tax Service or only checked locally. */
    val verifiesWithTaxService: Boolean,
    /** The latest selectable opening date. */
    val today: LocalDate,
    /** Digits only; the text field formats them. */
    val businessDigits: String = "",
    val representative: String = "",
    val openingDate: LocalDate? = null,
    val venueName: String = "",
    val numberProblem: BusinessNumber.Problem? = null,
    val representativeError: Boolean = false,
    val openingDateError: Boolean = false,
    val venueNameError: Boolean = false,
    val submitting: Boolean = false,
    /** Why the tax service did not confirm the last submission, if it did not. */
    val rejection: BusinessVerification? = null,
    val registered: Boolean = false,
)

/**
 * Registers the owner's venue. The number's checksum is validated as it is typed; the
 * registration is confirmed with the National Tax Service when an API key is configured.
 */
class OwnerSetupViewModel(
    private val registrar: VenueRegistrar,
    private val store: VenueStore,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OwnerSetupUiState(verifiesWithTaxService = registrar.verifiesWithTaxService, today = LocalDate.now(clock)),
    )
    val state: StateFlow<OwnerSetupUiState> = _state.asStateFlow()

    fun onBusinessNumberChange(value: String) {
        val digits = BusinessNumber.digitsOf(value)
        _state.update { it.copy(businessDigits = digits, numberProblem = BusinessNumber.problemWhileTyping(digits), rejection = null) }
    }

    fun onRepresentativeChange(value: String) {
        _state.update { it.copy(representative = value.take(PersonName.MAX_LENGTH), representativeError = false, rejection = null) }
    }

    fun onOpeningDateChange(date: LocalDate) {
        _state.update { it.copy(openingDate = date, openingDateError = false, rejection = null) }
    }

    fun onVenueNameChange(value: String) {
        _state.update { it.copy(venueName = value.take(VenueTag.MAX_NAME_LENGTH), venueNameError = false) }
    }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val number = BusinessNumber.parse(current.businessDigits)
        val representative = PersonName.normalize(current.representative)
        val openingDate = current.openingDate?.takeUnless { it.isAfter(LocalDate.now(clock)) }
        val venueName = VenueTag.normalizeName(current.venueName)
        if (number == null || representative == null || openingDate == null || venueName == null) {
            _state.update {
                it.copy(
                    numberProblem = BusinessNumber.problemWith(current.businessDigits),
                    representativeError = representative == null,
                    openingDateError = openingDate == null,
                    venueNameError = venueName == null,
                )
            }
            return
        }

        _state.update { it.copy(submitting = true, rejection = null) }
        viewModelScope.launch {
            val result = registrar.register(
                VenueTag(number, venueName),
                BusinessRegistration(number, representative, openingDate),
            )
            when (result) {
                is VenueRegistrationResult.Registered -> {
                    store.save(result.venue)
                    _state.update { it.copy(submitting = false, registered = true) }
                }

                is VenueRegistrationResult.Rejected ->
                    _state.update { it.copy(submitting = false, rejection = result.verification) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                OwnerSetupViewModel(container.venueRegistrar, container.venueStore, container.clock)
            }
        }
    }
}
