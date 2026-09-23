package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.business.BusinessRegistry
import com.jumincho.cvpass.core.business.BusinessVerification

/** A venue its owner has set up on this device. */
data class RegisteredVenue(
    /** What gets written to the venue's NFC tag. */
    val tag: VenueTag,
    /** The details the owner entered to prove the business registration. */
    val registration: BusinessRegistration,
    /**
     * Whether the National Tax Service confirmed [registration]. `false` means only the
     * number's checksum was validated because no API key was configured.
     */
    val verifiedWithTaxService: Boolean,
)

/** Outcome of [VenueRegistrar.register]. */
sealed interface VenueRegistrationResult {

    /** The venue may be used; see [RegisteredVenue.verifiedWithTaxService]. */
    data class Registered(val venue: RegisteredVenue) : VenueRegistrationResult

    /** The tax service did not confirm the registration; [verification] says why. */
    data class Rejected(val verification: BusinessVerification) : VenueRegistrationResult
}

/**
 * Registers the owner's venue, confirming the business with [registry] when one is
 * configured. Without a registry the number's checksum (already guaranteed by
 * [com.jumincho.cvpass.core.business.BusinessNumber]) is all that is checked, and the
 * venue is marked as not verified.
 */
class VenueRegistrar(private val registry: BusinessRegistry?) {

    /** Whether registrations are confirmed with the tax service. */
    val verifiesWithTaxService: Boolean
        get() = registry != null

    /** Registers the venue identified by [tag] and [registration]. */
    suspend fun register(tag: VenueTag, registration: BusinessRegistration): VenueRegistrationResult {
        require(tag.businessNumber == registration.number) { "Tag and registration must match" }
        if (registry == null) {
            return VenueRegistrationResult.Registered(RegisteredVenue(tag, registration, verifiedWithTaxService = false))
        }
        return when (val verification = registry.verify(registration)) {
            BusinessVerification.Valid ->
                VenueRegistrationResult.Registered(RegisteredVenue(tag, registration, verifiedWithTaxService = true))

            else -> VenueRegistrationResult.Rejected(verification)
        }
    }
}
