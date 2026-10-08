package com.jumincho.cvpass.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. */
sealed interface Destination {
    @Serializable
    data object Home : Destination

    /** Visitor details; [editing] returns to the previous screen when done. */
    @Serializable
    data class Onboarding(val editing: Boolean = false) : Destination

    @Serializable
    data object Pass : Destination

    /** Check-in; [payload] is a tag payload the system already read, Base64url-encoded. */
    @Serializable
    data class CheckIn(val payload: String? = null) : Destination

    @Serializable
    data object OwnerSetup : Destination

    @Serializable
    data object OwnerDashboard : Destination

    @Serializable
    data object Inspector : Destination
}
