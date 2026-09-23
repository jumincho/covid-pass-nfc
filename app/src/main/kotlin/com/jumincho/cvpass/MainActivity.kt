package com.jumincho.cvpass

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jumincho.cvpass.nfc.VenueTagNfc
import com.jumincho.cvpass.ui.navigation.CvPassNavHost
import com.jumincho.cvpass.ui.theme.CvPassTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The only activity. Besides the launcher it receives `NDEF_DISCOVERED` intents for
 * CV-PASS venue tags, which the navigation graph turns into check-ins.
 */
class MainActivity : ComponentActivity() {

    private val venueTagChannel = Channel<ByteArray>(Channel.BUFFERED)
    private val venueTagPayloads: Flow<ByteArray> = venueTagChannel.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) forwardVenueTag(intent)

        val container = (application as CvPassApplication).container
        setContent {
            CvPassTheme {
                CvPassNavHost(container = container, venueTagPayloads = venueTagPayloads)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        forwardVenueTag(intent)
    }

    private fun forwardVenueTag(intent: Intent) {
        VenueTagNfc.payloadFrom(intent)?.let { venueTagChannel.trySend(it) }
    }
}
