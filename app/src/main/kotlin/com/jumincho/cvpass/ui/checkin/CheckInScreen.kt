package com.jumincho.cvpass.ui.checkin

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.cvpass.R
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.feedback.rememberCheckInAnnouncer
import com.jumincho.cvpass.nfc.NfcAvailability
import com.jumincho.cvpass.nfc.NfcReaderMode
import com.jumincho.cvpass.nfc.VenueTagNfc
import com.jumincho.cvpass.nfc.rememberNfcAvailability
import com.jumincho.cvpass.ui.components.NfcUnavailableMessage
import com.jumincho.cvpass.ui.components.ScreenScaffold
import com.jumincho.cvpass.ui.components.rememberFormats
import com.jumincho.cvpass.ui.theme.CvPassTheme
import java.time.Instant

/**
 * Reads a venue tag and checks the visitor in. [payload] is a tag already read by the
 * system, when the app was opened by tapping one.
 */
@Composable
fun CheckInRoute(
    payload: ByteArray?,
    onVerifyPass: () -> Unit,
    onSetUpProfile: () -> Unit,
    onBack: () -> Unit,
    viewModel: CheckInViewModel = viewModel(factory = CheckInViewModel.factory(payload)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val availability = rememberNfcAvailability()
    val announcer = rememberCheckInAnnouncer()

    LaunchedEffect(viewModel, announcer) {
        viewModel.announcements.collect { announcer.announce() }
    }
    if (availability == NfcAvailability.ENABLED) {
        NfcReaderMode { tag -> viewModel.onTagRead(VenueTagNfc.read(tag)) }
    }
    CheckInScreen(
        state = state,
        availability = availability,
        onScanAgain = viewModel::reset,
        onVerifyPass = onVerifyPass,
        onSetUpProfile = onSetUpProfile,
        onBack = onBack,
    )
}

@Composable
internal fun CheckInScreen(
    state: CheckInUiState,
    availability: NfcAvailability,
    onScanAgain: () -> Unit,
    onVerifyPass: () -> Unit,
    onSetUpProfile: () -> Unit,
    onBack: () -> Unit,
) {
    val formats = rememberFormats()
    val colors = MaterialTheme.colorScheme
    ScreenScaffold(title = stringResource(R.string.checkin_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            when (state) {
                CheckInUiState.Waiting ->
                    if (availability == NfcAvailability.ENABLED) {
                        WaitingForTag()
                    } else {
                        NfcUnavailableMessage(availability, stringResource(R.string.nfc_unsupported_visitor))
                    }

                CheckInUiState.Processing -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.checkin_processing), style = MaterialTheme.typography.bodyLarge)
                }

                is CheckInUiState.CheckedIn -> {
                    ResultMessage(
                        icon = Icons.Outlined.CheckCircle,
                        tint = colors.tertiary,
                        title = stringResource(R.string.checkin_success_title),
                        body = stringResource(R.string.checkin_success_body, state.venueName, formats.time(state.at)),
                    )
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_done)) }
                    OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.checkin_scan_again))
                    }
                }

                is CheckInUiState.AlreadyCheckedIn -> {
                    ResultMessage(
                        icon = Icons.Outlined.Info,
                        tint = colors.primary,
                        title = stringResource(R.string.checkin_duplicate_title),
                        body = stringResource(R.string.checkin_duplicate_body, state.venueName, formats.time(state.previousAt)),
                    )
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_done)) }
                }

                is CheckInUiState.PassRequired -> {
                    val body = when (val status = state.status) {
                        null -> stringResource(R.string.checkin_pass_required_none)
                        is PassStatus.NotYetValid ->
                            stringResource(R.string.checkin_pass_required_pending, formats.date(status.validFrom))
                        else -> stringResource(R.string.checkin_pass_required_invalid)
                    }
                    ResultMessage(Icons.Outlined.ErrorOutline, colors.error, stringResource(R.string.checkin_pass_required_title), body)
                    Button(onClick = onVerifyPass, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pass_verify_action))
                    }
                }

                CheckInUiState.ProfileMissing -> {
                    ResultMessage(
                        icon = Icons.Outlined.Person,
                        tint = colors.primary,
                        title = stringResource(R.string.checkin_profile_missing_title),
                        body = stringResource(R.string.checkin_profile_missing_body),
                    )
                    Button(onClick = onSetUpProfile, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.checkin_set_up_profile))
                    }
                }

                is CheckInUiState.TagError -> {
                    val body = when (state.problem) {
                        TagProblem.NOT_VENUE_TAG -> stringResource(R.string.checkin_tag_not_cvpass)
                        TagProblem.UNSUPPORTED_VERSION -> stringResource(R.string.checkin_tag_unsupported)
                        TagProblem.READ_FAILED -> stringResource(R.string.checkin_tag_read_failed)
                    }
                    ResultMessage(Icons.Outlined.ErrorOutline, colors.error, stringResource(R.string.checkin_tag_problem_title), body)
                    OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_retry))
                    }
                }

                CheckInUiState.SaveFailed -> {
                    ResultMessage(
                        icon = Icons.Outlined.ErrorOutline,
                        tint = colors.error,
                        title = stringResource(R.string.checkin_save_failed_title),
                        body = stringResource(R.string.checkin_save_failed_body),
                    )
                    Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingForTag() {
    val pulse = rememberInfiniteTransition(label = "nfc-pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "nfc-scale",
    )
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .size(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Nfc, contentDescription = null, modifier = Modifier.size(72.dp))
        }
    }
    Text(
        text = stringResource(R.string.checkin_waiting_title),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = stringResource(R.string.checkin_waiting_body),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ResultMessage(icon: ImageVector, tint: Color, title: String, body: String) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(96.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics {
            heading()
            liveRegion = LiveRegionMode.Polite
        },
    )
    Text(text = body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}

@Preview(name = "Check-in complete", showBackground = true)
@Composable
private fun CheckInScreenPreview() {
    CvPassTheme {
        CheckInScreen(
            state = CheckInUiState.CheckedIn("카페 전주", Instant.parse("2021-10-20T06:30:00Z")),
            availability = NfcAvailability.ENABLED,
            onScanAgain = {},
            onVerifyPass = {},
            onSetUpProfile = {},
            onBack = {},
        )
    }
}
