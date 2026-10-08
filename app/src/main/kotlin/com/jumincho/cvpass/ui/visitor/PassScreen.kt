package com.jumincho.cvpass.ui.visitor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jumincho.cvpass.core.checkin.Visit
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.pass.VaccinationRecord
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.ui.components.ButtonLabel
import com.jumincho.cvpass.ui.components.MessageCard
import com.jumincho.cvpass.ui.components.MessageTone
import com.jumincho.cvpass.ui.components.ScreenScaffold
import com.jumincho.cvpass.ui.components.SectionTitle
import com.jumincho.cvpass.ui.components.rememberFormats
import com.jumincho.cvpass.ui.theme.CvPassTheme
import java.time.Instant
import java.time.LocalDate

/** The visitor's pass, certificate verification and recent check-ins. */
@Composable
fun PassRoute(
    onCheckIn: () -> Unit,
    onEditProfile: () -> Unit,
    onBack: () -> Unit,
    viewModel: PassViewModel = viewModel(factory = PassViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onCertificatePicked(uri.toString())
    }
    PassScreen(
        state = state,
        onVerify = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onDismissVerification = viewModel::dismissVerification,
        onCheckIn = onCheckIn,
        onEditProfile = onEditProfile,
        onBack = onBack,
    )
}

@Composable
internal fun PassScreen(
    state: PassUiState,
    onVerify: () -> Unit,
    onDismissVerification: () -> Unit,
    onCheckIn: () -> Unit,
    onEditProfile: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        title = stringResource(R.string.pass_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = onEditProfile) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.pass_edit_profile))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val profile = state.profile ?: return@Column
            PassCard(profile = profile, pass = state.pass, status = state.status)

            VerificationFeedback(state.verification, profile.name, onDismissVerification)

            val reading = state.verification == VerificationState.Reading
            if (state.status == PassStatus.Valid) {
                Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth()) {
                    ButtonLabel(Icons.Outlined.Nfc, stringResource(R.string.pass_check_in_action))
                }
                OutlinedButton(onClick = onVerify, enabled = !reading, modifier = Modifier.fillMaxWidth()) {
                    ButtonLabel(Icons.Outlined.PhotoLibrary, stringResource(R.string.pass_reverify_action))
                }
            } else {
                Button(onClick = onVerify, enabled = !reading, modifier = Modifier.fillMaxWidth()) {
                    ButtonLabel(Icons.Outlined.PhotoLibrary, stringResource(R.string.pass_verify_action))
                }
            }
            Text(
                text = stringResource(R.string.pass_verify_hint) + "\n" + stringResource(R.string.pass_rule_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle(stringResource(R.string.pass_recent_visits), modifier = Modifier.padding(top = 8.dp))
            RecentVisits(state.recentVisits)
        }
    }
}

@Composable
private fun VerificationFeedback(verification: VerificationState, profileName: String, onDismiss: () -> Unit) {
    val formats = rememberFormats()
    val modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    val dismiss: @Composable () -> Unit = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
    }
    when (verification) {
        VerificationState.Idle -> Unit

        VerificationState.Reading -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            Text(stringResource(R.string.pass_reading), style = MaterialTheme.typography.bodyMedium)
        }

        is VerificationState.Accepted -> {
            val status = verification.status
            if (status is PassStatus.NotYetValid) {
                MessageCard(
                    body = stringResource(R.string.pass_accepted_pending, formats.date(status.validFrom)),
                    tone = MessageTone.PENDING,
                    modifier = modifier,
                    action = dismiss,
                )
            } else {
                MessageCard(
                    body = stringResource(R.string.pass_accepted_valid),
                    tone = MessageTone.SUCCESS,
                    modifier = modifier,
                    action = dismiss,
                )
            }
        }

        is VerificationState.Rejected -> MessageCard(
            title = stringResource(R.string.pass_rejected_title),
            body = rejectionMessage(verification.reason, profileName) + "\n" + stringResource(R.string.pass_tip),
            tone = MessageTone.ERROR,
            modifier = modifier,
            action = dismiss,
        )

        VerificationState.ReadFailed -> MessageCard(
            body = stringResource(R.string.pass_read_failed),
            tone = MessageTone.ERROR,
            modifier = modifier,
            action = dismiss,
        )

        VerificationState.SaveFailed -> MessageCard(
            body = stringResource(R.string.pass_save_failed),
            tone = MessageTone.ERROR,
            modifier = modifier,
            action = dismiss,
        )
    }
}

@Composable
private fun rejectionMessage(reason: PassStatus.Reason, profileName: String): String = when (reason) {
    PassStatus.Reason.NameNotFound -> stringResource(R.string.pass_reason_name_not_found, profileName)
    is PassStatus.Reason.NameMismatch -> stringResource(R.string.pass_reason_name_mismatch, reason.nameOnCertificate, profileName)
    PassStatus.Reason.NoDoseFound -> stringResource(R.string.pass_reason_no_dose)
    PassStatus.Reason.NoDoseDate -> stringResource(R.string.pass_reason_no_date)
    is PassStatus.Reason.IncompleteSeries ->
        pluralStringResource(R.plurals.pass_reason_incomplete, reason.required, reason.doses, reason.required)
}

@Composable
private fun RecentVisits(visits: List<Visit>) {
    val formats = rememberFormats()
    if (visits.isEmpty()) {
        Text(
            text = stringResource(R.string.pass_no_visits),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column {
        visits.forEach { visit ->
            ListItem(
                headlineContent = { Text(visit.venueName) },
                supportingContent = { Text(formats.dateTime(visit.visitedAt)) },
                leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
            )
        }
    }
}

@Preview(name = "Pass", showBackground = true)
@Composable
private fun PassScreenPreview() {
    val venue = BusinessNumber.parse("124-81-00998")!!
    CvPassTheme {
        PassScreen(
            state = PassUiState(
                profile = VisitorProfile("홍길동", PhoneNumber.parse("010-1234-5678")!!),
                pass = VerifiedPass(
                    VaccinationRecord(doses = 2, primarySeriesDoses = 2, lastDoseDate = LocalDate.of(2021, 8, 15)),
                    verifiedAt = Instant.parse("2021-10-20T05:00:00Z"),
                ),
                status = PassStatus.Valid,
                recentVisits = listOf(Visit(venue, "카페 전주", Instant.parse("2021-10-20T06:30:00Z"))),
            ),
            onVerify = {},
            onDismissVerification = {},
            onCheckIn = {},
            onEditProfile = {},
            onBack = {},
        )
    }
}
