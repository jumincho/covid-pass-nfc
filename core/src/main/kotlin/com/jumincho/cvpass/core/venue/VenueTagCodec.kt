package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessNumber
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Result of [VenueTagCodec.decode]. */
sealed interface VenueTagDecoding {

    /** The payload identifies [tag]. */
    data class Success(val tag: VenueTag) : VenueTagDecoding

    /** The payload uses a newer format [version] than this app understands. */
    data class UnsupportedVersion(val version: Int) : VenueTagDecoding

    /** The payload is not a CV-PASS venue payload, for [reason]. */
    data class Malformed(val reason: Reason) : VenueTagDecoding

    /** Why a payload is [Malformed]. */
    enum class Reason { TOO_LARGE, NOT_UTF8, NOT_JSON, BAD_VERSION, BAD_BUSINESS_NUMBER, BAD_NAME }
}

/**
 * Encodes the payload of the MIME record ([MIME_TYPE]) that CV-PASS writes to venue tags:
 * UTF-8 JSON such as `{"v":1,"id":"1248100998","name":"Cafe Jeonju"}`.
 *
 * `v` versions the format. Readers reject versions newer than [VERSION] so that a future
 * format change fails loudly instead of being misread; unknown fields are ignored so that
 * version 1 can gain optional fields.
 */
object VenueTagCodec {

    /** MIME type of the NDEF record holding the payload. */
    const val MIME_TYPE: String = "application/vnd.jumincho.cvpass.venue"

    /** Payload format written by this version of the app. */
    const val VERSION: Int = 1

    /** Larger payloads are rejected before parsing; real ones are well under 200 bytes. */
    const val MAX_PAYLOAD_BYTES: Int = 512

    private val json = Json { ignoreUnknownKeys = true }

    /** Encodes [tag] as a compact UTF-8 JSON payload. */
    fun encode(tag: VenueTag): ByteArray = buildJsonObject {
        put("v", VERSION)
        put("id", tag.businessNumber.digits)
        put("name", tag.name)
    }.toString().encodeToByteArray()

    /** Decodes and validates a payload read from a tag. Never throws. */
    fun decode(payload: ByteArray): VenueTagDecoding {
        if (payload.size > MAX_PAYLOAD_BYTES) return malformed(VenueTagDecoding.Reason.TOO_LARGE)
        val text = decodeUtf8(payload) ?: return malformed(VenueTagDecoding.Reason.NOT_UTF8)
        val fields = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: SerializationException) {
            null
        } ?: return malformed(VenueTagDecoding.Reason.NOT_JSON)

        val version = fields.number("v") ?: return malformed(VenueTagDecoding.Reason.BAD_VERSION)
        if (version > VERSION) return VenueTagDecoding.UnsupportedVersion(version)
        if (version < VERSION) return malformed(VenueTagDecoding.Reason.BAD_VERSION)

        val number = fields.string("id")?.let(BusinessNumber::parse)
            ?: return malformed(VenueTagDecoding.Reason.BAD_BUSINESS_NUMBER)
        val name = fields.string("name")?.let(VenueTag::normalizeName)
            ?: return malformed(VenueTagDecoding.Reason.BAD_NAME)
        return VenueTagDecoding.Success(VenueTag(number, name))
    }

    private fun malformed(reason: VenueTagDecoding.Reason) = VenueTagDecoding.Malformed(reason)

    private fun decodeUtf8(bytes: ByteArray): String? = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: CharacterCodingException) {
        null
    }

    private fun JsonObject.number(key: String): Int? =
        (get(key) as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

    private fun JsonObject.string(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
}
