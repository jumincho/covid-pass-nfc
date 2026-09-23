package com.jumincho.cvpass.core.business

import java.io.IOException
import java.time.LocalDate

/** The details the tax service needs to confirm that a business registration is genuine. */
data class BusinessRegistration(
    val number: BusinessNumber,
    /** Name of the representative (대표자명) exactly as registered. */
    val representativeName: String,
    /** Opening date (개업일자) exactly as registered. */
    val openingDate: LocalDate,
)

/** Outcome of asking a [BusinessRegistry] whether a [BusinessRegistration] is genuine. */
sealed interface BusinessVerification {

    /** The registry confirms the registration. */
    data object Valid : BusinessVerification

    /**
     * The registry could not match the details; usually one of the three fields differs
     * from the registered record. [message] is the registry's explanation, if it gave one.
     */
    data class NotMatched(val message: String?) : BusinessVerification

    /** The registry rejected the API key (HTTP 401 or 403). */
    data object InvalidKey : BusinessVerification

    /** The request did not complete: no connectivity, a timeout, a TLS failure, … */
    data class NetworkError(val cause: IOException) : BusinessVerification

    /** The registry answered with something this client does not understand. */
    data class Unexpected(val detail: String) : BusinessVerification
}

/** An authoritative source that can confirm business registrations. */
fun interface BusinessRegistry {

    /** Checks [registration] against the registry. Never throws for remote failures. */
    suspend fun verify(registration: BusinessRegistration): BusinessVerification
}
