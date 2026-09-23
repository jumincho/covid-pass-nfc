package com.jumincho.cvpass.ui.visitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.cvpass.appContainer
import com.jumincho.cvpass.core.visitor.PersonName
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.core.visitor.VisitorStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Form state for the visitor's name and mobile number. */
data class OnboardingUiState(
    val name: String = "",
    /** Digits only; the text field formats them. */
    val phoneDigits: String = "",
    val nameError: Boolean = false,
    val phoneError: Boolean = false,
    /** The profile being edited, if one exists. */
    val existing: VisitorProfile? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    /** Whether saving would discard the verified pass, because it was checked against another name. */
    val nameChanged: Boolean
        get() = existing != null && !PersonName.matches(existing.name, name)
}

/** Collects and validates the visitor's details. */
class OnboardingViewModel(private val store: VisitorStore) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = store.profile.first() ?: return@launch
            _state.update { it.copy(name = existing.name, phoneDigits = existing.phone.digits, existing = existing) }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value.take(PersonName.MAX_LENGTH), nameError = false) }
    }

    fun onPhoneChange(value: String) {
        _state.update { it.copy(phoneDigits = value.filter { c -> c in '0'..'9' }.take(PhoneNumber.MAX_LENGTH), phoneError = false) }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val name = PersonName.normalize(current.name)
        val phone = PhoneNumber.parse(current.phoneDigits)
        if (name == null || phone == null) {
            _state.update { it.copy(nameError = name == null, phoneError = phone == null) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            if (current.nameChanged) store.clearPass()
            store.saveProfile(VisitorProfile(name, phone))
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OnboardingViewModel(appContainer.visitorStore) }
        }
    }
}
