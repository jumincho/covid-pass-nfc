package com.jumincho.cvpass.core.venue

import kotlinx.coroutines.flow.Flow

/** The venue its owner registered on this device. */
interface VenueStore {

    /** The registered venue, or `null` if none. */
    val venue: Flow<RegisteredVenue?>

    /** Saves [venue], replacing any previous one. */
    suspend fun save(venue: RegisteredVenue)

    /** Forgets the registered venue. */
    suspend fun clear()
}
