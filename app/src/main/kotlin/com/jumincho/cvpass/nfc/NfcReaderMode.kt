package com.jumincho.cvpass.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect

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

private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
    NfcAdapter.FLAG_READER_NFC_B or
    NfcAdapter.FLAG_READER_NFC_F or
    NfcAdapter.FLAG_READER_NFC_V
