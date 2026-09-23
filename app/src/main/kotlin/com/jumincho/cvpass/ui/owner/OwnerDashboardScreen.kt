package com.jumincho.cvpass.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.cvpass.R
import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.nfc.NfcAvailability
import com.jumincho.cvpass.nfc.NfcReaderMode
import com.jumincho.cvpass.nfc.TagWriteResult
import com.jumincho.cvpass.nfc.VenueTagNfc
import com.jumincho.cvpass.nfc.rememberNfcAvailability
import com.jumincho.cvpass.ui.components.MessageCard
import com.jumincho.cvpass.ui.components.MessageTone
import com.jumincho.cvpass.ui.components.NfcUnavailableMessage
import com.jumincho.cvpass.ui.components.ScreenScaffold
import com.jumincho.cvpass.ui.components.SectionTitle
import com.jumincho.cvpass.ui.theme.CvPassTheme
import java.time.LocalDate

/** The owner's venue, today's visitor count and tag writing. [onNoVenue] runs when no venue is registered. */
@Composable
fun OwnerDashboardRoute(
    onNoVenue: () -> Unit,
    onBack: () -> Unit,
    viewModel: OwnerDashboardViewModel = viewModel(factory = OwnerDashboardViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val availability = rememberNfcAvailability()
    val context = LocalContext.current

    val currentOnNoVenue by rememberUpdatedState(onNoVenue)
    LaunchedEffect(state.loaded, state.venue) {
        if (state.loaded && state.venue == null) currentOnNoVenue()
    }
    val venue = state.venue
    if (venue != null && state.tagWrite == TagWriteState.Waiting && availability == NfcAvailability.ENABLED) {
        val message = remember(venue) { VenueTagNfc.message(venue.tag, context.packageName) }
        NfcReaderMode { tag -> viewModel.onTagWritten(VenueTagNfc.write(tag, message)) }
    }
    OwnerDashboardScreen(
        state = state,
        availability = availability,
        onWriteTag = viewModel::startTagWrite,
        onCancelWrite = viewModel::cancelTagWrite,
        onChangeVenue = viewModel::changeVenue,
        onBack = onBack,
    )
}

@Composable
internal fun OwnerDashboardScreen(
    state: OwnerDashboardUiState,
    availability: NfcAvailability,
    onWriteTag: () -> Unit,
    onCancelWrite: () -> Unit,
    onChangeVenue: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmingChange by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.owner_dashboard_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val venue = state.venue ?: return@Column
            VenueCard(venue)
            VisitorsToday(state.visitorsToday, state.countFailed)

            SectionTitle(stringResource(R.string.owner_write_tag), modifier = Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.owner_write_tag_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NfcUnavailableMessage(availability, stringResource(R.string.nfc_unsupported_owner))

            val canWrite = availability == NfcAvailability.ENABLED
            when (val write = state.tagWrite) {
                TagWriteState.Idle -> WriteTagButton(enabled = canWrite, onClick = onWriteTag)

                TagWriteState.Waiting -> {
                    MessageCard(
                        body = stringResource(R.string.owner_write_waiting),
                        tone = MessageTone.INFO,
                        icon = Icons.Outlined.Nfc,
                        action = { TextButton(onClick = onCancelWrite) { Text(stringResource(R.string.action_cancel)) } },
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is TagWriteState.Finished -> {
                    MessageCard(
                        body = writeResultMessage(write.result),
                        tone = if (write.result == TagWriteResult.Written) MessageTone.SUCCESS else MessageTone.ERROR,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    WriteTagButton(enabled = canWrite, onClick = onWriteTag)
                }
            }

            OutlinedButton(onClick = { confirmingChange = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.owner_change_venue))
            }
        }
    }

    if (confirmingChange) {
        AlertDialog(
            onDismissRequest = { confirmingChange = false },
            title = { Text(stringResource(R.string.owner_change_venue_confirm_title)) },
            text = { Text(stringResource(R.string.owner_change_venue_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingChange = false
                        onChangeVenue()
                    },
                ) { Text(stringResource(R.string.owner_change_venue)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingChange = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun WriteTagButton(enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.owner_write_tag))
    }
}

@Composable
private fun VenueCard(venue: RegisteredVenue) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(venue.tag.name, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = venue.tag.businessNumber.formatted,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val statusColors = CvPassTheme.statusColors
            Surface(
                shape = CircleShape,
                color = if (venue.verifiedWithTaxService) statusColors.success else statusColors.pending,
                contentColor = if (venue.verifiedWithTaxService) statusColors.onSuccess else statusColors.onPending,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (venue.verifiedWithTaxService) Icons.Outlined.VerifiedUser else Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(
                            if (venue.verifiedWithTaxService) R.string.owner_verified_badge else R.string.owner_unverified_badge,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitorsToday(count: Int?, failed: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.owner_today_visitors), style = MaterialTheme.typography.titleMedium)
                if (failed) {
                    Text(
                        text = stringResource(R.string.owner_count_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (count != null) Text(count.toString(), style = MaterialTheme.typography.displaySmall)
        }
    }
}

@Composable
private fun writeResultMessage(result: TagWriteResult): String = when (result) {
    TagWriteResult.Written -> stringResource(R.string.owner_write_success)
    TagWriteResult.ReadOnly -> stringResource(R.string.owner_write_read_only)
    is TagWriteResult.TooSmall ->
        pluralStringResource(R.plurals.owner_write_too_small, result.requiredBytes, result.requiredBytes, result.capacityBytes)
    TagWriteResult.Unsupported -> stringResource(R.string.owner_write_unsupported)
    TagWriteResult.TagLost -> stringResource(R.string.owner_write_lost)
    TagWriteResult.Failed -> stringResource(R.string.owner_write_failed)
}

@Preview(name = "Venue dashboard", showBackground = true)
@Composable
private fun OwnerDashboardScreenPreview() {
    val number = BusinessNumber.parse("124-81-00998")!!
    CvPassTheme {
        OwnerDashboardScreen(
            state = OwnerDashboardUiState(
                loaded = true,
                venue = RegisteredVenue(
                    tag = VenueTag(number, "카페 전주"),
                    registration = BusinessRegistration(number, "홍길동", LocalDate.of(2019, 5, 2)),
                    verifiedWithTaxService = false,
                ),
                visitorsToday = 12,
            ),
            availability = NfcAvailability.ENABLED,
            onWriteTag = {},
            onCancelWrite = {},
            onChangeVenue = {},
            onBack = {},
        )
    }
}
