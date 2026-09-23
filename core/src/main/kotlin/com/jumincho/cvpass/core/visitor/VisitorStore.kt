package com.jumincho.cvpass.core.visitor

import com.jumincho.cvpass.core.pass.VerifiedPass
import kotlinx.coroutines.flow.Flow

/** The visitor's own data on their device: the profile and the verified pass, if any. */
interface VisitorStore {

    /** The saved profile, or `null` before onboarding. */
    val profile: Flow<VisitorProfile?>

    /** The last verified pass, or `null` if none was verified. */
    val pass: Flow<VerifiedPass?>

    /** Saves [profile], replacing any previous one. */
    suspend fun saveProfile(profile: VisitorProfile)

    /** Saves [pass], replacing any previous one. */
    suspend fun savePass(pass: VerifiedPass)

    /** Forgets the verified pass, for example after the profile name changed. */
    suspend fun clearPass()
}
