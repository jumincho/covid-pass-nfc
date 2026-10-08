package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.business.BusinessRegistry
import com.jumincho.cvpass.core.business.BusinessVerification
import kotlinx.coroutines.CancellationException

/** Outcome of [VenueRegistrar.register]. */
sealed interface VenueRegistrationResult {

    /** The venue was saved and may be used; see [RegisteredVenue.verifiedWithTaxService]. */
    data class Registered(val venue: RegisteredVenue) : VenueRegistrationResult

    /** The tax service did not confirm the registration; [verification] says why. Nothing was saved. */
    data class Rejected(val verification: BusinessVerification) : VenueRegistrationResult

    /** The registration was accepted, but saving the venue failed. */
    data class Failed(val cause: Exception) : VenueRegistrationResult
}

/**
 * Registers the owner's venue: confirms the business with [registry] when one is configured,
 * then saves the venue to [store]. Without a registry the number's checksum (already
 * guaranteed by [com.jumincho.cvpass.core.business.BusinessNumber]) is all that is checked,
 * and the venue is marked as not verified.
 */
class VenueRegistrar(private val registry: BusinessRegistry?, private val store: VenueStore) {

    /** Whether registrations are confirmed with the tax service. */
    val verifiesWithTaxService: Boolean
        get() = registry != null

    /** Registers the venue identified by [tag] and [registration]. */
    suspend fun register(tag: VenueTag, registration: BusinessRegistration): VenueRegistrationResult {
        require(tag.businessNumber == registration.number) { "Tag and registration must match" }
        val verification = registry?.verify(registration)
        if (verification != null && verification != BusinessVerification.Valid) {
            return VenueRegistrationResult.Rejected(verification)
        }
        val venue = RegisteredVenue(tag, registration, verifiedWithTaxService = verification == BusinessVerification.Valid)
        return try {
            store.save(venue)
            VenueRegistrationResult.Registered(venue)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            VenueRegistrationResult.Failed(e)
        }
    }
}
