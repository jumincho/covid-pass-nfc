package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessRegistration

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
