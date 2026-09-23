package com.jumincho.cvpass.nfc

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Whether this device can read and write NFC tags right now. */
enum class NfcAvailability { UNSUPPORTED, DISABLED, ENABLED }

private fun Context.nfcAvailability(): NfcAvailability {
    val adapter = NfcAdapter.getDefaultAdapter(this) ?: return NfcAvailability.UNSUPPORTED
    return if (adapter.isEnabled) NfcAvailability.ENABLED else NfcAvailability.DISABLED
}

/** The current [NfcAvailability], updated when the user toggles NFC or returns to the app. */
@Composable
fun rememberNfcAvailability(): NfcAvailability {
    val context = LocalContext.current
    var availability by remember(context) { mutableStateOf(context.nfcAvailability()) }

    LifecycleResumeEffect(context) {
        availability = context.nfcAvailability()
        onPauseOrDispose { }
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                availability = context.nfcAvailability()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return availability
}

/**
 * Keeps NFC reader mode enabled while the calling screen is resumed, delivering each
 * discovered tag to [onTag] on a binder thread, where blocking tag I/O is allowed. Reader
 * mode also stops other apps and the system from handling the tag in the meantime.
 */
@Composable
fun NfcReaderMode(onTag: (Tag) -> Unit) {
    val activity = LocalActivity.current ?: return
    val adapter = remember(activity) { NfcAdapter.getDefaultAdapter(activity) } ?: return
    val currentOnTag by rememberUpdatedState(onTag)

    LifecycleResumeEffect(activity, adapter) {
        adapter.enableReaderMode(activity, { tag -> currentOnTag(tag) }, READER_FLAGS, null)
        onPauseOrDispose { adapter.disableReaderMode(activity) }
    }
}

/** Opens the system NFC settings, falling back to the wireless settings screen. */
fun Context.openNfcSettings() {
    try {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }
}

private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
    NfcAdapter.FLAG_READER_NFC_B or
    NfcAdapter.FLAG_READER_NFC_F or
    NfcAdapter.FLAG_READER_NFC_V
