package com.jumincho.cvpass.ui.visitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.cvpass.R
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.ui.components.FormattedDigitsTransformation
import com.jumincho.cvpass.ui.components.MessageCard
import com.jumincho.cvpass.ui.components.MessageTone
import com.jumincho.cvpass.ui.components.ScreenScaffold
import com.jumincho.cvpass.ui.theme.CvPassTheme

/** Collects the visitor's name and mobile number; calls [onDone] once they are saved. */
@Composable
fun OnboardingRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.saved) {
        if (state.saved) currentOnDone()
    }
    OnboardingScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onPhoneChange = viewModel::onPhoneChange,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@Composable
internal fun OnboardingScreen(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.onboarding_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.field_name)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.field_name_error)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.phoneDigits,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.field_phone)) },
                placeholder = { Text(stringResource(R.string.field_phone_placeholder)) },
                isError = state.phoneError,
                supportingText = if (state.phoneError) {
                    { Text(stringResource(R.string.field_phone_error)) }
                } else {
                    null
                },
                singleLine = true,
                visualTransformation = remember { FormattedDigitsTransformation(PhoneNumber::formatPartial) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.nameChanged) {
                MessageCard(body = stringResource(R.string.onboarding_name_change_note), tone = MessageTone.PENDING)
            }
            MessageCard(body = stringResource(R.string.onboarding_privacy), tone = MessageTone.INFO, icon = Icons.Outlined.Lock)

            Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Preview(name = "Details", showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    CvPassTheme {
        OnboardingScreen(
            state = OnboardingUiState(name = "홍길동", phoneDigits = "0101234", phoneError = true),
            onNameChange = {},
            onPhoneChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
