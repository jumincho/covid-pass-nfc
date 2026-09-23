package com.jumincho.cvpass.core.business

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URLDecoder
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [BusinessRegistry] backed by the National Tax Service's business registration
 * validation API (국세청 사업자등록정보 진위확인), which the public-data portal data.go.kr
 * serves from `api.odcloud.kr`.
 *
 * @param serviceKey the data.go.kr service key. Both the "Encoding" and the "Decoding"
 *   variant shown on the portal are accepted.
 * @param httpClient client used for the call; configure its timeouts to taste.
 * @param baseUrl scheme and host of the API, replaceable for tests.
 */
class NtsBusinessRegistry(
    serviceKey: String,
    private val httpClient: OkHttpClient,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL,
) : BusinessRegistry {

    private val serviceKey = normalizeServiceKey(serviceKey)

    override suspend fun verify(registration: BusinessRegistration): BusinessVerification {
        val payload = ValidateRequest(
            businesses = listOf(
                BusinessRecord(
                    number = registration.number.digits,
                    openingDate = registration.openingDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                    representativeName = registration.representativeName.trim(),
                ),
            ),
        )
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments(VALIDATE_PATH)
                    .addQueryParameter("serviceKey", serviceKey)
                    .addQueryParameter("returnType", "JSON")
                    .build(),
            )
            .header("Accept", "application/json")
            .post(json.encodeToString(ValidateRequest.serializer(), payload).toRequestBody(JSON))
            .build()

        val reply = try {
            httpClient.newCall(request).await()
        } catch (e: IOException) {
            return BusinessVerification.NetworkError(e)
        }
        return interpret(reply, registration.number)
    }

    private fun interpret(reply: HttpReply, number: BusinessNumber): BusinessVerification {
        if (reply.code == 401 || reply.code == 403) return BusinessVerification.InvalidKey
        if (reply.code !in 200..299) return BusinessVerification.Unexpected("HTTP ${reply.code}")

        val response = try {
            json.decodeFromString(ValidateResponse.serializer(), reply.body)
        } catch (e: IllegalArgumentException) {
            return BusinessVerification.Unexpected("Malformed response: ${e.message}")
        }
        if (response.statusCode != null && response.statusCode != "OK") {
            return BusinessVerification.Unexpected("status_code ${response.statusCode}")
        }
        val result = response.data.firstOrNull()
            ?: return BusinessVerification.Unexpected("Response contains no result")
        if (result.number != null && result.number != number.digits) {
            return BusinessVerification.Unexpected("Response is for a different business number")
        }
        return when (result.valid) {
            VALID -> BusinessVerification.Valid
            NOT_MATCHED -> BusinessVerification.NotMatched(result.message?.takeIf { it.isNotBlank() })
            else -> BusinessVerification.Unexpected("valid ${result.valid}")
        }
    }

    companion object {
        /** Production endpoint of the public-data portal. */
        val DEFAULT_BASE_URL: HttpUrl = "https://api.odcloud.kr/".toHttpUrl()

        internal const val VALIDATE_PATH = "api/nts-businessman/v1/validate"

        private const val VALID = "01"
        private const val NOT_MATCHED = "02"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * data.go.kr shows every key twice: raw ("Decoding") and percent-encoded
         * ("Encoding"). OkHttp encodes query values itself, so decode an already encoded
         * key once to avoid sending it double-encoded. Raw keys never contain `%`.
         */
        internal fun normalizeServiceKey(key: String): String {
            val trimmed = key.trim()
            return if ('%' in trimmed) URLDecoder.decode(trimmed, "UTF-8") else trimmed
        }
    }
}

private class HttpReply(val code: Int, val body: String)

/** Executes the call asynchronously, cancelling it if the coroutine is cancelled. */
private suspend fun Call.await(): HttpReply = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val reply = try {
                    response.use { HttpReply(it.code, it.body.string()) }
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                    return
                }
                continuation.resume(reply)
            }
        },
    )
}

@Serializable
private data class ValidateRequest(val businesses: List<BusinessRecord>)

@Serializable
private data class BusinessRecord(
    @SerialName("b_no") val number: String,
    @SerialName("start_dt") val openingDate: String,
    @SerialName("p_nm") val representativeName: String,
)

@Serializable
private data class ValidateResponse(
    @SerialName("status_code") val statusCode: String? = null,
    val data: List<ValidationResult> = emptyList(),
)

@Serializable
private data class ValidationResult(
    @SerialName("b_no") val number: String? = null,
    val valid: String? = null,
    @SerialName("valid_msg") val message: String? = null,
)
