package com.jumincho.cvpass.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.cvpass.R
import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessVerification
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.ui.components.DateField
import com.jumincho.cvpass.ui.components.FormattedDigitsTransformation
import com.jumincho.cvpass.ui.components.MessageCard
import com.jumincho.cvpass.ui.components.MessageTone
import com.jumincho.cvpass.ui.components.ScreenScaffold
import com.jumincho.cvpass.ui.theme.CvPassTheme
import java.time.LocalDate

/** Venue registration; calls [onRegistered] once the venue is saved. */
@Composable
fun OwnerSetupRoute(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: OwnerSetupViewModel = viewModel(factory = OwnerSetupViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnRegistered by rememberUpdatedState(onRegistered)
    LaunchedEffect(state.registered) {
        if (state.registered) currentOnRegistered()
    }
    OwnerSetupScreen(
        state = state,
        verifiesWithTaxService = viewModel.verifiesWithTaxService,
        today = viewModel.today,
        onBusinessNumberChange = viewModel::onBusinessNumberChange,
        onRepresentativeChange = viewModel::onRepresentativeChange,
        onOpeningDateChange = viewModel::onOpeningDateChange,
        onVenueNameChange = viewModel::onVenueNameChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
    )
}

@Composable
internal fun OwnerSetupScreen(
    state: OwnerSetupUiState,
    verifiesWithTaxService: Boolean,
    today: LocalDate,
    onBusinessNumberChange: (String) -> Unit,
    onRepresentativeChange: (String) -> Unit,
    onOpeningDateChange: (LocalDate) -> Unit,
    onVenueNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.owner_setup_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.owner_setup_intro), style = MaterialTheme.typography.bodyLarge)

            val numberError = when (state.numberProblem) {
                BusinessNumber.Problem.WRONG_FORMAT -> stringResource(R.string.field_business_number_format)
                BusinessNumber.Problem.CHECKSUM_MISMATCH -> stringResource(R.string.field_business_number_checksum)
                null -> null
            }
            OutlinedTextField(
                value = state.businessDigits,
                onValueChange = onBusinessNumberChange,
                label = { Text(stringResource(R.string.field_business_number)) },
                supportingText = { Text(numberError ?: stringResource(R.string.field_business_number_hint)) },
                isError = numberError != null,
                singleLine = true,
                visualTransformation = remember { FormattedDigitsTransformation(BusinessNumber::formatPartial) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.representative,
                onValueChange = onRepresentativeChange,
                label = { Text(stringResource(R.string.field_representative)) },
                isError = state.representativeError,
                supportingText = if (state.representativeError) {
                    { Text(stringResource(R.string.field_representative_error)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(
                label = stringResource(R.string.field_opening_date),
                date = state.openingDate,
                onDateSelected = onOpeningDateChange,
                latest = today,
                errorText = if (state.openingDateError) stringResource(R.string.field_opening_date_error) else null,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.venueName,
                onValueChange = onVenueNameChange,
                label = { Text(stringResource(R.string.field_venue_name)) },
                isError = state.venueNameError,
                supportingText = if (state.venueNameError) {
                    { Text(pluralStringResource(R.plurals.field_venue_name_error, VenueTag.MAX_NAME_LENGTH, VenueTag.MAX_NAME_LENGTH)) }
                } else {
                    { Text("${state.venueName.length} / ${VenueTag.MAX_NAME_LENGTH}") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            MessageCard(
                body = stringResource(if (verifiesWithTaxService) R.string.owner_setup_nts_on else R.string.owner_setup_nts_off),
                tone = if (verifiesWithTaxService) MessageTone.INFO else MessageTone.PENDING,
            )

            state.rejection?.let { rejection ->
                MessageCard(
                    title = stringResource(R.string.owner_rejected_title),
                    body = rejectionMessage(rejection),
                    tone = MessageTone.ERROR,
                )
            }

            if (state.submitting) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(stringResource(R.string.owner_setup_submitting), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Button(onClick = onSubmit, enabled = !state.submitting, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (verifiesWithTaxService) R.string.owner_setup_submit else R.string.owner_setup_submit_local))
            }
        }
    }
}

@Composable
private fun rejectionMessage(rejection: BusinessVerification): String = when (rejection) {
    is BusinessVerification.NotMatched -> {
        val base = stringResource(R.string.owner_rejected_not_matched)
        rejection.message?.let { base + "\n" + stringResource(R.string.owner_rejected_service_message, it) } ?: base
    }
    BusinessVerification.InvalidKey -> stringResource(R.string.owner_rejected_invalid_key)
    is BusinessVerification.NetworkError -> stringResource(R.string.owner_rejected_network)
    is BusinessVerification.Unexpected, BusinessVerification.Valid -> stringResource(R.string.owner_rejected_unexpected)
}

@Preview(name = "Register venue", showBackground = true)
@Composable
private fun OwnerSetupScreenPreview() {
    CvPassTheme {
        OwnerSetupScreen(
            state = OwnerSetupUiState(businessDigits = "1248100997", numberProblem = BusinessNumber.Problem.CHECKSUM_MISMATCH),
            verifiesWithTaxService = false,
            today = LocalDate.of(2021, 10, 20),
            onBusinessNumberChange = {},
            onRepresentativeChange = {},
            onOpeningDateChange = {},
            onVenueNameChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
