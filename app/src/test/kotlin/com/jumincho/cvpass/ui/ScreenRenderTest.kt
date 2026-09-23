package com.jumincho.cvpass.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.jumincho.cvpass.R
import com.jumincho.cvpass.StorageBackend
import com.jumincho.cvpass.core.checkin.CheckIn
import com.jumincho.cvpass.core.checkin.Visit
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.nfc.NfcAvailability
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.ui.checkin.CheckInScreen
import com.jumincho.cvpass.ui.checkin.CheckInUiState
import com.jumincho.cvpass.ui.home.HomeScreen
import com.jumincho.cvpass.ui.home.HomeUiState
import com.jumincho.cvpass.ui.inspector.InspectorScreen
import com.jumincho.cvpass.ui.inspector.InspectorUiState
import com.jumincho.cvpass.ui.inspector.LogResults
import com.jumincho.cvpass.ui.owner.OwnerDashboardScreen
import com.jumincho.cvpass.ui.owner.OwnerDashboardUiState
import com.jumincho.cvpass.ui.theme.CvPassTheme
import com.jumincho.cvpass.ui.visitor.PassScreen
import com.jumincho.cvpass.ui.visitor.PassUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.util.TimeZone

/**
 * Renders each main screen with Robolectric and checks its key content. When run through
 * `recordRoborazziDebug`, the same tests record the screenshots in `docs/screenshots/`.
 *
 * The screens are stateless, so a plain [Application] replaces `CvPassApplication` and its
 * start-up work.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val defaultTimeZone = TimeZone.getDefault()

    /** Times in the sample data read naturally in the venues' own time zone. */
    @Before
    fun useSeoulTime() {
        TimeZone.setDefault(TimeZone.getTimeZone(Fixtures.zone))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultTimeZone)
    }

    private fun text(@StringRes id: Int, vararg args: Any): String = compose.activity.getString(id, *args)

    private fun render(screenshot: String, darkTheme: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { CvPassTheme(darkTheme = darkTheme) { content() } }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/$screenshot.png", SCREENSHOT_OPTIONS)
    }

    @Test
    fun home() {
        var visitorClicks = 0
        render("home") {
            HomeScreen(
                state = HomeUiState(loaded = true, profile = Fixtures.visitor, passStatus = PassStatus.Valid, venue = Fixtures.venue),
                storage = StorageBackend.Device,
                verifiesWithTaxService = false,
                onVisitor = { visitorClicks++ },
                onOwner = {},
                onInspector = {},
            )
        }

        compose.onNodeWithText(text(R.string.role_owner_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_storage_device)).assertExists()
        compose.onNodeWithText(text(R.string.role_visitor_title)).performClick()
        assertEquals(1, visitorClicks)
    }

    @Test
    fun pass() {
        render("pass") {
            PassScreen(state = passState, onVerify = {}, onDismissVerification = {}, onCheckIn = {}, onEditProfile = {}, onBack = {})
        }

        compose.onNodeWithText(Fixtures.visitor.name).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.pass_status_valid)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.pass_check_in_action)).assertIsDisplayed()
    }

    @Test
    fun passInDarkTheme() {
        render("pass-dark", darkTheme = true) {
            PassScreen(state = passState, onVerify = {}, onDismissVerification = {}, onCheckIn = {}, onEditProfile = {}, onBack = {})
        }

        compose.onNodeWithText(text(R.string.pass_status_valid)).assertIsDisplayed()
    }

    @Test
    fun checkInComplete() {
        render("check-in") {
            CheckInScreen(
                state = CheckInUiState.CheckedIn(Fixtures.venueTag.name, Fixtures.now),
                availability = NfcAvailability.ENABLED,
                onScanAgain = {},
                onVerifyPass = {},
                onSetUpProfile = {},
                onBack = {},
            )
        }

        compose.onNodeWithText(text(R.string.checkin_success_title)).assertIsDisplayed()
    }

    @Test
    fun checkInWithoutNfc() {
        render("check-in-no-nfc") {
            CheckInScreen(
                state = CheckInUiState.Waiting,
                availability = NfcAvailability.DISABLED,
                onScanAgain = {},
                onVerifyPass = {},
                onSetUpProfile = {},
                onBack = {},
            )
        }

        compose.onNodeWithText(text(R.string.nfc_disabled_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nfc_open_settings)).assertIsDisplayed()
    }

    @Test
    fun ownerDashboard() {
        render("owner-dashboard") {
            OwnerDashboardScreen(
                state = OwnerDashboardUiState(loaded = true, venue = Fixtures.venue, visitorsToday = 12),
                availability = NfcAvailability.ENABLED,
                onWriteTag = {},
                onCancelWrite = {},
                onChangeVenue = {},
                onBack = {},
            )
        }

        compose.onNodeWithText(Fixtures.venueTag.name).assertIsDisplayed()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.owner_unverified_badge)).assertIsDisplayed()
    }

    @Test
    fun inspectorLog() {
        val morning = CheckIn("a", Fixtures.venueNumber, Fixtures.venueTag.name, "홍길동", Fixtures.visitor.phone, Fixtures.now)
        val later = morning.copy(id = "b", visitorName = "남궁민수", checkedInAt = Fixtures.now.plus(Duration.ofMinutes(95)))
        render("inspector") {
            InspectorScreen(
                state = InspectorUiState(
                    enabled = true,
                    today = Fixtures.today,
                    unlocked = true,
                    businessDigits = Fixtures.venueNumber.digits,
                    results = LogResults.Loaded(Fixtures.venueNumber, Fixtures.today, listOf(morning, later)),
                ),
                onPinChange = {},
                onUnlock = {},
                onBusinessNumberChange = {},
                onDateChange = {},
                onSearch = {},
                onSelect = {},
                onBack = {},
            )
        }

        compose.onNodeWithText("홍*동").assertIsDisplayed()
        compose.onNodeWithText("남**수").assertIsDisplayed()
        compose.onAllNodesWithText("010-****-5678").assertCountEquals(2)
    }

    private val passState = PassUiState(
        loaded = true,
        profile = Fixtures.visitor,
        pass = Fixtures.validPass,
        status = PassStatus.Valid,
        recentVisits = listOf(Visit(Fixtures.venueNumber, Fixtures.venueTag.name, Fixtures.now)),
    )

    private companion object {
        /** Relative to the app module, which is the working directory of unit tests. */
        const val SCREENSHOT_DIR = "../docs/screenshots"
        val SCREENSHOT_OPTIONS = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))
    }
}
