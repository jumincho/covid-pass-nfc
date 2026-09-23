package com.jumincho.cvpass.ui.inspector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.cvpass.R
import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.checkin.CheckIn
import com.jumincho.cvpass.core.privacy.Masking
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.ui.components.DateField
import com.jumincho.cvpass.ui.components.FormattedDigitsTransformation
import com.jumincho.cvpass.ui.components.MessageCard
import com.jumincho.cvpass.ui.components.MessageTone
import com.jumincho.cvpass.ui.components.ScreenScaffold
import com.jumincho.cvpass.ui.components.SectionTitle
import com.jumincho.cvpass.ui.components.rememberFormats
import com.jumincho.cvpass.ui.theme.CvPassTheme
import java.time.Instant
import java.time.LocalDate

/** Inspector mode: PIN gate, then the visitor-log query. */
@Composable
fun InspectorRoute(onBack: () -> Unit, viewModel: InspectorViewModel = viewModel(factory = InspectorViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InspectorScreen(
        state = state,
        onPinChange = viewModel::onPinChange,
        onUnlock = viewModel::unlock,
        onBusinessNumberChange = viewModel::onBusinessNumberChange,
        onDateChange = viewModel::onDateChange,
        onSearch = viewModel::search,
        onSelect = viewModel::select,
        onBack = onBack,
    )
}

@Composable
internal fun InspectorScreen(
    state: InspectorUiState,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onBusinessNumberChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSearch: () -> Unit,
    onSelect: (CheckIn?) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.inspector_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                !state.enabled -> MessageCard(body = stringResource(R.string.inspector_disabled), tone = MessageTone.PENDING)
                !state.unlocked -> PinGate(state, onPinChange, onUnlock)
                else -> LogQuery(state, onBusinessNumberChange, onDateChange, onSearch, onSelect)
            }
        }
    }
    state.selected?.let { CheckInDetails(it, onDismiss = { onSelect(null) }) }
}

@Composable
private fun PinGate(state: InspectorUiState, onPinChange: (String) -> Unit, onUnlock: () -> Unit) {
    Text(stringResource(R.string.inspector_gate_intro), style = MaterialTheme.typography.bodyLarge)
    OutlinedTextField(
        value = state.pin,
        onValueChange = onPinChange,
        label = { Text(stringResource(R.string.field_inspector_code)) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        isError = state.pinError,
        supportingText = if (state.pinError) {
            { Text(stringResource(R.string.inspector_code_error)) }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onUnlock() }),
        modifier = Modifier.fillMaxWidth(),
    )
    MessageCard(body = stringResource(R.string.inspector_gate_demo_note), tone = MessageTone.INFO)
    Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.inspector_unlock)) }
}

@Composable
private fun LogQuery(
    state: InspectorUiState,
    onBusinessNumberChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSearch: () -> Unit,
    onSelect: (CheckIn) -> Unit,
) {
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = Modifier.fillMaxWidth(),
    )
    DateField(
        label = stringResource(R.string.field_log_date),
        date = state.date,
        onDateSelected = onDateChange,
        latest = state.today,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = onSearch, enabled = state.results != LogResults.Loading, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Search, contentDescription = null)
        Text(stringResource(R.string.inspector_search), modifier = Modifier.padding(start = 8.dp))
    }

    when (val results = state.results) {
        LogResults.NotSearched -> Unit
        LogResults.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        LogResults.Failed -> MessageCard(body = stringResource(R.string.inspector_load_failed), tone = MessageTone.ERROR)
        is LogResults.Loaded -> LogEntries(results.checkIns, onSelect)
    }
}

@Composable
private fun LogEntries(checkIns: List<CheckIn>, onSelect: (CheckIn) -> Unit) {
    if (checkIns.isEmpty()) {
        MessageCard(body = stringResource(R.string.inspector_empty), tone = MessageTone.INFO)
        return
    }
    val formats = rememberFormats()
    SectionTitle(pluralStringResource(R.plurals.inspector_result_count, checkIns.size, checkIns.size))
    Text(
        text = stringResource(R.string.inspector_reveal_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column {
        checkIns.forEach { checkIn ->
            ListItem(
                headlineContent = { Text(Masking.name(checkIn.visitorName)) },
                supportingContent = { Text(Masking.phone(checkIn.phone)) },
                trailingContent = { Text(formats.time(checkIn.checkedInAt)) },
                leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null) },
                modifier = Modifier.clickable(onClickLabel = stringResource(R.string.inspector_details_title)) {
                    onSelect(checkIn)
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun CheckInDetails(checkIn: CheckIn, onDismiss: () -> Unit) {
    val formats = rememberFormats()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.inspector_details_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Detail(stringResource(R.string.inspector_detail_name), checkIn.visitorName)
                Detail(stringResource(R.string.inspector_detail_phone), checkIn.phone.formatted)
                Detail(stringResource(R.string.inspector_detail_time), formats.dateTime(checkIn.checkedInAt))
                Detail(stringResource(R.string.inspector_detail_venue), "${checkIn.venueName} (${checkIn.venueId.formatted})")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(name = "Inspector log", showBackground = true)
@Composable
private fun InspectorScreenPreview() {
    val venue = BusinessNumber.parse("124-81-00998")!!
    val phone = PhoneNumber.parse("010-1234-5678")!!
    val date = LocalDate.of(2021, 10, 20)
    CvPassTheme {
        InspectorScreen(
            state = InspectorUiState(
                enabled = true,
                today = date,
                unlocked = true,
                businessDigits = venue.digits,
                results = LogResults.Loaded(
                    venueId = venue,
                    date = date,
                    checkIns = listOf(
                        CheckIn("1", venue, "카페 전주", "홍길동", phone, Instant.parse("2021-10-20T03:12:00Z")),
                        CheckIn("2", venue, "카페 전주", "남궁민수", phone, Instant.parse("2021-10-20T04:40:00Z")),
                    ),
                ),
            ),
            onPinChange = {},
            onUnlock = {},
            onBusinessNumberChange = {},
            onDateChange = {},
            onSearch = {},
            onSelect = {},
            onBack = {},
        )
    }
}
