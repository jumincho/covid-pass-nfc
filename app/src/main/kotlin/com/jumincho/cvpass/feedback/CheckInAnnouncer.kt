package com.jumincho.cvpass.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.jumincho.cvpass.R
import java.util.Locale

/**
 * Confirms a check-in the way the original app did: a short vibration and a spoken
 * "입장이 완료되었습니다" — or "Check-in complete" when the device is not set to Korean.
 */
class CheckInAnnouncer(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var ready = false
    private var pendingAnnouncement = false

    /** Connects to the text-to-speech engine. */
    fun start() {
        if (textToSpeech != null) return
        textToSpeech = TextToSpeech(context) { status -> onEngineReady(status == TextToSpeech.SUCCESS) }
    }

    /** Vibrates and speaks the confirmation, once the engine is ready. */
    fun announce() {
        vibrate()
        if (ready) speak() else pendingAnnouncement = true
    }

    /** Releases the text-to-speech engine. */
    fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
        pendingAnnouncement = false
    }

    private fun onEngineReady(success: Boolean) {
        val engine = textToSpeech ?: return
        val language = if (context.resources.configuration.locales[0].language == Locale.KOREAN.language) {
            Locale.KOREAN
        } else {
            Locale.ENGLISH
        }
        ready = success && engine.setLanguage(language) >= TextToSpeech.LANG_AVAILABLE
        if (ready && pendingAnnouncement) speak()
        pendingAnnouncement = false
    }

    private fun speak() {
        textToSpeech?.speak(context.getString(R.string.tts_check_in_complete), TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, -1))
        }
    }

    private companion object {
        const val UTTERANCE_ID = "check-in-complete"
        val VIBRATION_PATTERN = longArrayOf(0, 120, 80, 120)
    }
}

/** A [CheckInAnnouncer] tied to the calling composable's lifetime. */
@Composable
fun rememberCheckInAnnouncer(): CheckInAnnouncer {
    val context = LocalContext.current
    val announcer = remember(context) { CheckInAnnouncer(context) }
    DisposableEffect(announcer) {
        announcer.start()
        onDispose { announcer.shutdown() }
    }
    return announcer
}
