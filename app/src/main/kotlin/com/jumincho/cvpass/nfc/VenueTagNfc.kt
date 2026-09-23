package com.jumincho.cvpass.nfc

import android.content.Intent
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.TagTechnology
import androidx.core.content.IntentCompat
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.core.venue.VenueTagCodec
import java.io.IOException

/** Result of reading a tag. Free of Android types so that view models stay unit-testable. */
sealed interface TagReadResult {

    /** The tag holds a CV-PASS venue record with this payload (not yet validated). */
    class Payload(val bytes: ByteArray) : TagReadResult

    /** The tag is readable but holds no CV-PASS venue record. */
    data object NotVenueTag : TagReadResult

    /** Communication with the tag failed, usually because it moved away. */
    data object ReadFailed : TagReadResult
}

/** Result of writing a venue to a tag. */
sealed interface TagWriteResult {
    data object Written : TagWriteResult

    data object ReadOnly : TagWriteResult

    data class TooSmall(val requiredBytes: Int, val capacityBytes: Int) : TagWriteResult

    /** The tag supports neither NDEF nor NDEF formatting. */
    data object Unsupported : TagWriteResult

    data object TagLost : TagWriteResult

    data object Failed : TagWriteResult
}

/**
 * Reads and writes CV-PASS venue tags: an NDEF message with a [VenueTagCodec.MIME_TYPE]
 * record, followed by an Android Application Record so that tapping the tag opens the app.
 * The functions that talk to a [Tag] block and must run off the main thread, as the
 * reader-mode callback does.
 */
object VenueTagNfc {

    /** The message written to a venue's tag. */
    fun message(venue: VenueTag, packageName: String): NdefMessage = NdefMessage(
        NdefRecord.createMime(VenueTagCodec.MIME_TYPE, VenueTagCodec.encode(venue)),
        NdefRecord.createApplicationRecord(packageName),
    )

    /** The venue payload carried by an `NDEF_DISCOVERED` intent, if any. */
    fun payloadFrom(intent: Intent): ByteArray? {
        if (intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return null
        val messages = IntentCompat.getParcelableArrayExtra(intent, NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
            ?: return null
        return venuePayload(messages.filterIsInstance<NdefMessage>())
    }

    /** Reads the venue payload from [tag]. */
    fun read(tag: Tag): TagReadResult {
        val ndef = Ndef.get(tag) ?: return TagReadResult.NotVenueTag
        val message = try {
            ndef.cachedNdefMessage ?: ndef.run {
                connect()
                ndefMessage
            }
        } catch (e: IOException) {
            return TagReadResult.ReadFailed
        } catch (e: FormatException) {
            return TagReadResult.NotVenueTag
        } finally {
            ndef.closeQuietly()
        }
        val payload = message?.let { venuePayload(listOf(it)) } ?: return TagReadResult.NotVenueTag
        return TagReadResult.Payload(payload)
    }

    /** Writes [message] to [tag], formatting it first if it is blank. */
    fun write(tag: Tag, message: NdefMessage): TagWriteResult {
        val size = message.byteArrayLength
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            if (!ndef.isWritable) return TagWriteResult.ReadOnly
            if (ndef.maxSize < size) return TagWriteResult.TooSmall(requiredBytes = size, capacityBytes = ndef.maxSize)
            return ndef.perform {
                connect()
                writeNdefMessage(message)
            }
        }
        val formatable = NdefFormatable.get(tag) ?: return TagWriteResult.Unsupported
        return formatable.perform {
            connect()
            format(message)
        }
    }

    private fun venuePayload(messages: List<NdefMessage>): ByteArray? =
        messages.asSequence()
            .flatMap { it.records.asSequence() }
            .firstOrNull { it.tnf == NdefRecord.TNF_MIME_MEDIA && it.toMimeType() == VenueTagCodec.MIME_TYPE }
            ?.payload

    private inline fun <T : TagTechnology> T.perform(action: T.() -> Unit): TagWriteResult = try {
        action()
        TagWriteResult.Written
    } catch (e: TagLostException) {
        TagWriteResult.TagLost
    } catch (e: IOException) {
        TagWriteResult.Failed
    } catch (e: FormatException) {
        TagWriteResult.Failed
    } finally {
        closeQuietly()
    }

    private fun TagTechnology.closeQuietly() {
        runCatching { close() }
    }
}
