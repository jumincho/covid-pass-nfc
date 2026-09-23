package com.jumincho.cvpass.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.cvpass.R
import com.jumincho.cvpass.StorageBackend
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.ui.components.CvPassLogo
import com.jumincho.cvpass.ui.components.SectionTitle
import com.jumincho.cvpass.ui.components.rememberFormats
import com.jumincho.cvpass.ui.theme.CvPassTheme

/** Role chooser: visitor, venue owner or inspector. */
@Composable
fun HomeRoute(
    storage: StorageBackend,
    verifiesWithTaxService: Boolean,
    onVisitor: (hasProfile: Boolean) -> Unit,
    onOwner: (hasVenue: Boolean) -> Unit,
    onInspector: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        storage = storage,
        verifiesWithTaxService = verifiesWithTaxService,
        onVisitor = { onVisitor(state.profile != null) },
        onOwner = { onOwner(state.venue != null) },
        onInspector = onInspector,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    storage: StorageBackend,
    verifiesWithTaxService: Boolean,
    onVisitor: () -> Unit,
    onOwner: () -> Unit,
    onInspector: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CvPassLogo(size = 56.dp)
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionTitle(stringResource(R.string.home_choose_role), modifier = Modifier.padding(top = 8.dp))

            RoleCard(
                icon = Icons.Outlined.Vaccines,
                title = stringResource(R.string.role_visitor_title),
                description = stringResource(R.string.role_visitor_description),
                status = visitorStatus(state),
                enabled = state.loaded,
                onClick = onVisitor,
            )
            RoleCard(
                icon = Icons.Outlined.Storefront,
                title = stringResource(R.string.role_owner_title),
                description = stringResource(R.string.role_owner_description),
                status = state.venue?.tag?.name ?: stringResource(R.string.home_owner_no_venue),
                enabled = state.loaded,
                onClick = onOwner,
            )
            RoleCard(
                icon = Icons.Outlined.Policy,
                title = stringResource(R.string.role_inspector_title),
                description = stringResource(R.string.role_inspector_description),
                status = null,
                enabled = true,
                onClick = onInspector,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                val storageText = when (storage) {
                    StorageBackend.Device -> stringResource(R.string.home_storage_device)
                    is StorageBackend.Firestore -> stringResource(R.string.home_storage_firestore, storage.projectId)
                }
                val taxServiceText = stringResource(
                    if (verifiesWithTaxService) R.string.home_tax_service_on else R.string.home_tax_service_off,
                )
                listOf(storageText, taxServiceText).forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun visitorStatus(state: HomeUiState): String {
    val profile = state.profile ?: return stringResource(R.string.home_visitor_no_profile)
    val status = when (val pass = state.passStatus) {
        null -> stringResource(R.string.home_visitor_no_pass)
        PassStatus.Valid -> stringResource(R.string.pass_status_valid)
        is PassStatus.NotYetValid -> stringResource(R.string.pass_status_pending, rememberFormats().date(pass.validFrom))
        is PassStatus.Invalid -> stringResource(R.string.pass_status_invalid)
    }
    return "${profile.name} · $status"
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    description: String,
    status: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (status != null) {
                    Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Preview(name = "Home", showBackground = true)
@Composable
private fun HomeScreenPreview() {
    CvPassTheme {
        HomeScreen(
            state = HomeUiState(loaded = true),
            storage = StorageBackend.Device,
            verifiesWithTaxService = false,
            onVisitor = {},
            onOwner = {},
            onInspector = {},
        )
    }
}
