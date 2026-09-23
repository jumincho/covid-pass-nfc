package com.jumincho.cvpass.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jumincho.cvpass.AppContainer
import com.jumincho.cvpass.ui.checkin.CheckInRoute
import com.jumincho.cvpass.ui.home.HomeRoute
import com.jumincho.cvpass.ui.inspector.InspectorRoute
import com.jumincho.cvpass.ui.owner.OwnerDashboardRoute
import com.jumincho.cvpass.ui.owner.OwnerSetupRoute
import com.jumincho.cvpass.ui.visitor.OnboardingRoute
import com.jumincho.cvpass.ui.visitor.PassRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.util.Base64

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

/**
 * The single-activity navigation graph. [venueTagPayloads] carries venue tags that the
 * system dispatched to the app; each one opens a fresh check-in on top of the home screen.
 */
@Composable
fun CvPassNavHost(
    container: AppContainer,
    venueTagPayloads: Flow<ByteArray>,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(navController, venueTagPayloads) {
        venueTagPayloads.collect { payload ->
            navController.navigate(Destination.CheckIn(payload.encodeBase64Url())) {
                popUpTo<Destination.Home>()
            }
        }
    }

    NavHost(navController = navController, startDestination = Destination.Home) {
        composable<Destination.Home> {
            HomeRoute(
                storage = container.storage,
                verifiesWithTaxService = container.venueRegistrar.verifiesWithTaxService,
                onVisitor = { hasProfile ->
                    if (hasProfile) navController.navigate(Destination.Pass) else navController.navigate(Destination.Onboarding())
                },
                onOwner = { hasVenue ->
                    if (hasVenue) navController.navigate(Destination.OwnerDashboard) else navController.navigate(Destination.OwnerSetup)
                },
                onInspector = { navController.navigate(Destination.Inspector) },
            )
        }

        composable<Destination.Onboarding> { entry ->
            val editing = entry.toRoute<Destination.Onboarding>().editing
            OnboardingRoute(
                onDone = {
                    if (editing) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Destination.Pass) { popUpTo<Destination.Onboarding> { inclusive = true } }
                    }
                },
                onBack = { navController.navigateUp() },
            )
        }

        composable<Destination.Pass> {
            PassRoute(
                onCheckIn = { navController.navigate(Destination.CheckIn()) },
                onEditProfile = { navController.navigate(Destination.Onboarding(editing = true)) },
                onBack = { navController.navigateUp() },
            )
        }

        composable<Destination.CheckIn> { entry ->
            val payload = remember(entry) { entry.toRoute<Destination.CheckIn>().payload?.decodeBase64Url() }
            CheckInRoute(
                payload = payload,
                onVerifyPass = { navController.navigate(Destination.Pass) { popUpTo<Destination.Home>() } },
                onSetUpProfile = { navController.navigate(Destination.Onboarding()) { popUpTo<Destination.Home>() } },
                onBack = { navController.navigateUp() },
            )
        }

        composable<Destination.OwnerSetup> {
            OwnerSetupRoute(
                onRegistered = {
                    navController.navigate(Destination.OwnerDashboard) { popUpTo<Destination.OwnerSetup> { inclusive = true } }
                },
                onBack = { navController.navigateUp() },
            )
        }

        composable<Destination.OwnerDashboard> {
            OwnerDashboardRoute(
                onNoVenue = {
                    navController.navigate(Destination.OwnerSetup) { popUpTo<Destination.OwnerDashboard> { inclusive = true } }
                },
                onBack = { navController.navigateUp() },
            )
        }

        composable<Destination.Inspector> {
            InspectorRoute(onBack = { navController.navigateUp() })
        }
    }
}

private fun ByteArray.encodeBase64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(this)

private fun String.decodeBase64Url(): ByteArray? = runCatching { Base64.getUrlDecoder().decode(this) }.getOrNull()
