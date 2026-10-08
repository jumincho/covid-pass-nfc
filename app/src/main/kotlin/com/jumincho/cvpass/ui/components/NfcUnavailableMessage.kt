package com.jumincho.cvpass.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jumincho.cvpass.R
import com.jumincho.cvpass.nfc.NfcAvailability
import com.jumincho.cvpass.nfc.openNfcSettings

/** Explains why NFC cannot be used and offers the settings shortcut when it is only off. */
@Composable
fun NfcUnavailableMessage(availability: NfcAvailability, unsupportedMessage: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    when (availability) {
        NfcAvailability.UNSUPPORTED -> MessageCard(
            title = stringResource(R.string.nfc_unsupported_title),
            body = unsupportedMessage,
            tone = MessageTone.ERROR,
            modifier = modifier,
        )

        NfcAvailability.DISABLED -> MessageCard(
            title = stringResource(R.string.nfc_disabled_title),
            body = stringResource(R.string.nfc_disabled_body),
            tone = MessageTone.PENDING,
            modifier = modifier,
            icon = Icons.Outlined.Nfc,
            action = {
                TextButton(onClick = { context.openNfcSettings() }) { Text(stringResource(R.string.nfc_open_settings)) }
            },
        )

        NfcAvailability.ENABLED -> Unit
    }
}
