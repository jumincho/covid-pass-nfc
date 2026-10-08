package com.jumincho.cvpass.ui.visitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jumincho.cvpass.R
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.ui.components.StatusBadge
import com.jumincho.cvpass.ui.components.rememberFormats
import com.jumincho.cvpass.ui.theme.BrandColors
import com.jumincho.cvpass.ui.theme.CvPassTheme

/** The visitor's pass, styled as a card: their details, the pass status and the verified facts. */
@Composable
internal fun PassCard(profile: VisitorProfile, pass: VerifiedPass?, status: PassStatus?) {
    val formats = rememberFormats()
    val secondary = Color.White.copy(alpha = 0.85f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BrandColors.Navy, BrandColors.DeepBlue)))
            .semantics(mergeDescendants = true) { }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Vaccines, contentDescription = null, tint = secondary)
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.pass_card_label),
                style = MaterialTheme.typography.labelLarge,
                color = secondary,
                modifier = Modifier.weight(1f),
            )
            PassStatusBadge(status)
        }
        Text(profile.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(profile.phone.formatted, style = MaterialTheme.typography.bodyMedium, color = secondary)
        if (pass != null) {
            val record = pass.record
            val doses = pluralStringResource(R.plurals.pass_doses, record.doses, record.doses)
            val summary = if (record.doses > record.primarySeriesDoses) "$doses · ${stringResource(R.string.pass_booster)}" else doses
            Text(summary, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.pass_last_dose, formats.date(record.lastDoseDate)),
                style = MaterialTheme.typography.bodyMedium,
                color = secondary,
            )
            Text(
                text = stringResource(R.string.pass_verified_on, formats.dateTime(pass.verifiedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
            )
        }
    }
}

@Composable
private fun PassStatusBadge(status: PassStatus?) {
    val formats = rememberFormats()
    val statusColors = CvPassTheme.statusColors
    val colors = MaterialTheme.colorScheme
    when (status) {
        PassStatus.Valid -> StatusBadge(
            label = stringResource(R.string.pass_status_valid),
            containerColor = statusColors.success,
            contentColor = statusColors.onSuccess,
            icon = Icons.Outlined.CheckCircle,
        )

        is PassStatus.NotYetValid -> StatusBadge(
            label = stringResource(R.string.pass_status_pending, formats.date(status.validFrom)) + " · " +
                pluralStringResource(R.plurals.pass_days_remaining, status.daysRemaining, status.daysRemaining),
            containerColor = statusColors.pending,
            contentColor = statusColors.onPending,
            icon = Icons.Outlined.HourglassTop,
        )

        is PassStatus.Invalid -> StatusBadge(
            label = stringResource(R.string.pass_status_invalid),
            containerColor = colors.errorContainer,
            contentColor = colors.onErrorContainer,
            icon = Icons.Outlined.ErrorOutline,
        )

        null -> StatusBadge(
            label = stringResource(R.string.pass_status_none),
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurfaceVariant,
        )
    }
}
