package com.jumincho.cvpass.core.business

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Responses follow the shape documented for the public-data portal; they are not recordings. */
class NtsBusinessRegistryTest {

    private val server = MockWebServer()
    private val client = OkHttpClient.Builder()
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()
    private val registration = BusinessRegistration(
        number = BusinessNumber.parse("124-81-00998")!!,
        representativeName = " 홍길동 ",
        openingDate = LocalDate.of(2019, 5, 2),
    )

    @BeforeEach
    fun start() = server.start()

    @AfterEach
    fun stop() = server.close()

    private fun registry(key: String = RAW_KEY) = NtsBusinessRegistry(key, client, server.url("/"))

    private fun verify(registry: NtsBusinessRegistry = registry()) = runBlocking { registry.verify(registration) }

    private fun respond(code: Int, body: String) = server.enqueue(
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build(),
    )

    @Test
    fun `sends the documented request`() {
        respond(200, validBody())

        verify()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/nts-businessman/v1/validate", request.url.encodedPath)
        assertEquals("serviceKey=$ENCODED_KEY&returnType=JSON", request.url.encodedQuery)
        assertEquals(
            """{"businesses":[{"b_no":"1248100998","start_dt":"20190502","p_nm":"홍길동"}]}""",
            request.body?.utf8(),
        )
        assertTrue(request.headers["Content-Type"].orEmpty().startsWith("application/json"))
    }

    @Test
    fun `accepts the percent-encoded variant of the service key`() {
        respond(200, validBody())

        verify(registry(key = ENCODED_KEY))

        assertEquals("serviceKey=$ENCODED_KEY&returnType=JSON", server.takeRequest().url.encodedQuery)
    }

    @Test
    fun `maps valid 01 to Valid`() {
        respond(200, validBody())
        assertEquals(BusinessVerification.Valid, verify())
    }

    @Test
    fun `maps valid 02 to NotMatched with the service's message`() {
        respond(
            200,
            """
            {"request_cnt":1,"valid_cnt":0,"status_code":"OK","data":[{"b_no":"1248100998","valid":"02",
            "valid_msg":"확인할 수 없습니다.","request_param":{"b_no":"1248100998","start_dt":"20190502","p_nm":"홍길동"}}]}
            """,
        )
        assertEquals(BusinessVerification.NotMatched("확인할 수 없습니다."), verify())
    }

    @Test
    fun `NotMatched without a message has none`() {
        respond(200, """{"status_code":"OK","data":[{"b_no":"1248100998","valid":"02","valid_msg":""}]}""")
        assertEquals(BusinessVerification.NotMatched(null), verify())
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `maps a rejected key to InvalidKey`(code: Int) {
        respond(code, """{"code":-4,"msg":"등록되지 않은 인증키 입니다."}""")
        assertEquals(BusinessVerification.InvalidKey, verify())
    }

    @Test
    fun `maps server errors to Unexpected`() {
        respond(500, "Internal Server Error")
        assertEquals(BusinessVerification.Unexpected("HTTP 500"), verify())
    }

    @Test
    fun `maps a body that is not the documented JSON to Unexpected`() {
        respond(200, "<html><body>Service unavailable</body></html>")
        val result = assertIs<BusinessVerification.Unexpected>(verify())
        assertTrue(result.detail.startsWith("Malformed response"))
    }

    @Test
    fun `maps an error status_code to Unexpected`() {
        respond(200, """{"status_code":"BAD_JSON_REQUEST"}""")
        assertEquals(BusinessVerification.Unexpected("status_code BAD_JSON_REQUEST"), verify())
    }

    @Test
    fun `maps an empty result list to Unexpected`() {
        respond(200, """{"status_code":"OK","request_cnt":1,"valid_cnt":0,"data":[]}""")
        assertEquals(BusinessVerification.Unexpected("Response contains no result"), verify())
    }

    @Test
    fun `maps an unknown validity code to Unexpected`() {
        respond(200, """{"status_code":"OK","data":[{"b_no":"1248100998","valid":"03"}]}""")
        assertEquals(BusinessVerification.Unexpected("valid 03"), verify())
    }

    @Test
    fun `rejects a result for another business number`() {
        respond(200, """{"status_code":"OK","data":[{"b_no":"2208162517","valid":"01"}]}""")
        assertEquals(BusinessVerification.Unexpected("Response is for a different business number"), verify())
    }

    @Test
    fun `maps a timeout to NetworkError`() {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())
        val result = assertIs<BusinessVerification.NetworkError>(verify())
        assertIs<SocketTimeoutException>(result.cause)
    }

    @Test
    fun `maps an unreachable server to NetworkError`() {
        val registry = registry()
        server.close()
        assertIs<BusinessVerification.NetworkError>(verify(registry))
    }

    @Test
    fun `cancelling the coroutine abandons the call`() {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())
        val registry = registry()
        val result = runBlocking { withTimeoutOrNull(100) { registry.verify(registration) } }
        assertNull(result)
    }

    @Test
    fun `normalises both key variants to the raw key`() {
        assertEquals(RAW_KEY, NtsBusinessRegistry.normalizeServiceKey(RAW_KEY))
        assertEquals(RAW_KEY, NtsBusinessRegistry.normalizeServiceKey(" $ENCODED_KEY "))
    }

    private fun validBody() = """
        {"request_cnt":1,"valid_cnt":1,"status_code":"OK","data":[{"b_no":"1248100998","valid":"01",
        "request_param":{"b_no":"1248100998","start_dt":"20190502","p_nm":"홍길동","p_nm2":"","b_nm":"",
        "corp_no":"","b_sector":"","b_type":"","b_adr":""},"status":{"b_no":"1248100998","b_stt":"계속사업자",
        "b_stt_cd":"01","tax_type":"부가가치세 일반과세자","tax_type_cd":"01","end_dt":"","utcc_yn":"N",
        "tax_type_change_dt":"","invoice_apply_dt":"","rbf_tax_type":"","rbf_tax_type_cd":""}}]}
    """.trimIndent()

    private companion object {
        const val RAW_KEY = "raw+key/with=="
        const val ENCODED_KEY = "raw%2Bkey%2Fwith%3D%3D"
    }
}
