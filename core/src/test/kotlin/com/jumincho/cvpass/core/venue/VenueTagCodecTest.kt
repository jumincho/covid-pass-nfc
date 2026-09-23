package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessNumber
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VenueTagCodecTest {

    private val number = BusinessNumber.parse("124-81-00998")!!

    private fun decode(json: String) = VenueTagCodec.decode(json.encodeToByteArray())

    @Test
    fun `encodes compact UTF-8 JSON`() {
        val payload = VenueTagCodec.encode(VenueTag(number, "카페 전주"))
        assertContentEquals("""{"v":1,"id":"1248100998","name":"카페 전주"}""".encodeToByteArray(), payload)
    }

    @ParameterizedTest
    @ValueSource(strings = ["카페 전주", "Cafe \"Quote\" \\ Backslash", "치킨&맥주 1호점", "a"])
    fun `round-trips any valid venue`(name: String) {
        val tag = VenueTag(number, name)
        assertEquals(VenueTagDecoding.Success(tag), VenueTagCodec.decode(VenueTagCodec.encode(tag)))
    }

    @Test
    fun `ignores unknown fields and formatting`() {
        val decoded = decode(
            """
            { "name" : "카페 전주", "id" : "124-81-00998", "v" : 1, "added_later" : { "x" : [1, 2] } }
            """,
        )
        assertEquals(VenueTagDecoding.Success(VenueTag(number, "카페 전주")), decoded)
    }

    @Test
    fun `normalises the venue name`() {
        assertEquals(
            VenueTagDecoding.Success(VenueTag(number, "카페 전주")),
            decode("""{"v":1,"id":"1248100998","name":"  카페   전주 "}"""),
        )
    }

    @Test
    fun `reports newer versions as unsupported`() {
        assertEquals(VenueTagDecoding.UnsupportedVersion(2), decode("""{"v":2,"venue":"something else"}"""))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            """{"id":"1248100998","name":"x"}                  | BAD_VERSION""",
            """{"v":"1","id":"1248100998","name":"x"}          | BAD_VERSION""",
            """{"v":0,"id":"1248100998","name":"x"}            | BAD_VERSION""",
            """{"v":1.5,"id":"1248100998","name":"x"}          | BAD_VERSION""",
            """{"v":null,"id":"1248100998","name":"x"}         | BAD_VERSION""",
            """{"v":1,"name":"x"}                              | BAD_BUSINESS_NUMBER""",
            """{"v":1,"id":1248100998,"name":"x"}              | BAD_BUSINESS_NUMBER""",
            """{"v":1,"id":"1248100997","name":"x"}            | BAD_BUSINESS_NUMBER""",
            """{"v":1,"id":"1248100998"}                       | BAD_NAME""",
            """{"v":1,"id":"1248100998","name":"   "}          | BAD_NAME""",
            """{"v":1,"id":"1248100998","name":"a\u0007b"}     | BAD_NAME""",
            """{"v":1,"id":"1248100998","name":"0123456789012345678901234567890"} | BAD_NAME""",
            """[1, 2, 3]                                       | NOT_JSON""",
            """"just a string"                                 | NOT_JSON""",
            """{"v":1,                                         | NOT_JSON""",
            """not json at all                                 | NOT_JSON""",
        ],
    )
    fun `rejects malformed payloads`(json: String, reason: VenueTagDecoding.Reason) {
        assertEquals(VenueTagDecoding.Malformed(reason), decode(json))
    }

    @Test
    fun `rejects invalid UTF-8`() {
        val payload = byteArrayOf('{'.code.toByte(), 0xC3.toByte(), 0x28, '}'.code.toByte())
        assertEquals(VenueTagDecoding.Malformed(VenueTagDecoding.Reason.NOT_UTF8), VenueTagCodec.decode(payload))
    }

    @Test
    fun `rejects oversized payloads before parsing them`() {
        val payload = ByteArray(VenueTagCodec.MAX_PAYLOAD_BYTES + 1) { ' '.code.toByte() }
        assertEquals(VenueTagDecoding.Malformed(VenueTagDecoding.Reason.TOO_LARGE), VenueTagCodec.decode(payload))
    }

    @Test
    fun `validates venue names`() {
        assertEquals("카페 전주", VenueTag.normalizeName("  카페\t전주  "))
        assertNull(VenueTag.normalizeName(""))
        assertNull(VenueTag.normalizeName("x".repeat(VenueTag.MAX_NAME_LENGTH + 1)))
        assertFailsWith<IllegalArgumentException> { VenueTag(number, " not normalised") }
    }
}
