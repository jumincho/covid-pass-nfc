package com.jumincho.cvpass.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jumincho.cvpass.ui.theme.CvPassTheme

/** How a [MessageCard] should read: neutral, good news, waiting, or a problem. */
enum class MessageTone { INFO, SUCCESS, PENDING, ERROR }

/** An inline message with an icon, an optional title and an optional action. */
@Composable
fun MessageCard(
    body: String,
    tone: MessageTone,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector = tone.icon,
    action: (@Composable () -> Unit)? = null,
) {
    val status = CvPassTheme.statusColors
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        MessageTone.INFO -> colors.secondaryContainer to colors.onSecondaryContainer
        MessageTone.SUCCESS -> status.success to status.onSuccess
        MessageTone.PENDING -> status.pending to status.onPending
        MessageTone.ERROR -> colors.errorContainer to colors.onErrorContainer
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium)
                action?.invoke()
            }
        }
    }
}

private val MessageTone.icon: ImageVector
    get() = when (this) {
        MessageTone.INFO -> Icons.Outlined.Info
        MessageTone.SUCCESS -> Icons.Outlined.CheckCircle
        MessageTone.PENDING -> Icons.Outlined.HourglassTop
        MessageTone.ERROR -> Icons.Outlined.ErrorOutline
    }
