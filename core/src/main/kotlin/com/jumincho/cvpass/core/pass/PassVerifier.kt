package com.jumincho.cvpass.core.pass

import com.jumincho.cvpass.core.visitor.VisitorStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

/** Outcome of [PassVerifier.verify]. */
sealed interface PassVerificationResult {

    /** The certificate qualifies and [pass] was saved; [status] is its status today. */
    data class Accepted(val pass: VerifiedPass, val status: PassStatus) : PassVerificationResult

    /** The certificate does not qualify, for [reason]; nothing was saved. */
    data class Rejected(val reason: PassStatus.Reason) : PassVerificationResult

    /** The visitor has not entered their details yet, so there is no name to look for. */
    data object ProfileMissing : PassVerificationResult

    /** The image could not be read at all. */
    data class Unreadable(val cause: Exception) : PassVerificationResult

    /** The certificate qualifies, but saving the pass failed. */
    data class Failed(val cause: Exception) : PassVerificationResult
}

/**
 * Verifies the visitor's vaccination certificate: reads the text on its image with
 * [reader], parses it with [CertificateParser] and checks it with [PassPolicy]. Only the
 * [VaccinationRecord] derived from a qualifying certificate is saved, never the image or
 * its text.
 */
class PassVerifier(
    private val reader: CertificateTextReader,
    private val store: VisitorStore,
    private val clock: Clock,
) {

    /** Verifies the certificate on the image that [imageUri] identifies. */
    suspend fun verify(imageUri: String): PassVerificationResult {
        val profile = store.profile.first() ?: return PassVerificationResult.ProfileMissing
        val text = try {
            reader.read(imageUri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return PassVerificationResult.Unreadable(e)
        }

        val today = LocalDate.now(clock)
        val certificate = CertificateParser.parse(text, profile.name, today)
        val status = PassPolicy.evaluate(certificate, profile.name, today)
        if (status is PassStatus.Invalid) return PassVerificationResult.Rejected(status.reason)

        // PassPolicy accepts only certificates that name a dose and its date.
        val pass = VerifiedPass(checkNotNull(certificate.toRecord()), clock.instant())
        return try {
            store.savePass(pass)
            PassVerificationResult.Accepted(pass, status)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PassVerificationResult.Failed(e)
        }
    }
}
