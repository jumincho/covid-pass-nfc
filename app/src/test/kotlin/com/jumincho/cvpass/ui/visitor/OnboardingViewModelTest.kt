package com.jumincho.cvpass.ui.visitor

import com.jumincho.cvpass.core.testing.InMemoryVisitorStore
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MainDispatcherExtension::class)
class OnboardingViewModelTest {

    @Test
    fun `flags invalid fields and saves nothing`() = runTest {
        val store = InMemoryVisitorStore()
        val viewModel = OnboardingViewModel(store)

        viewModel.onNameChange("홍")
        viewModel.onPhoneChange("0101234")
        viewModel.save()

        val state = viewModel.state.value
        assertTrue(state.nameError)
        assertTrue(state.phoneError)
        assertFalse(state.saved)
        assertNull(store.savedProfile)
    }

    @Test
    fun `keeps only the digits of a phone number`() = runTest {
        val viewModel = OnboardingViewModel(InMemoryVisitorStore())

        viewModel.onPhoneChange("010-1234-5678 ext. 99")

        assertEquals("01012345678", viewModel.state.value.phoneDigits)
    }

    @Test
    fun `saves a normalised profile`() = runTest {
        val store = InMemoryVisitorStore()
        val viewModel = OnboardingViewModel(store)

        viewModel.onNameChange("  홍길동 ")
        viewModel.onPhoneChange("01012345678")
        viewModel.save()

        assertTrue(viewModel.state.value.saved)
        assertEquals(VisitorProfile("홍길동", PhoneNumber.parse("010-1234-5678")!!), store.savedProfile)
    }

    @Test
    fun `editing prefills the saved profile`() = runTest {
        val viewModel = OnboardingViewModel(InMemoryVisitorStore(Fixtures.visitor))

        val state = viewModel.state.value
        assertEquals("홍길동", state.name)
        assertEquals("01012345678", state.phoneDigits)
        assertEquals(Fixtures.visitor, state.existing)
    }

    @Test
    fun `changing the name discards the verified pass`() = runTest {
        val store = InMemoryVisitorStore(Fixtures.visitor, Fixtures.validPass)
        val viewModel = OnboardingViewModel(store)

        viewModel.onNameChange("김철수")
        assertTrue(viewModel.state.value.nameChanged)
        viewModel.save()

        assertNull(store.savedPass)
        assertEquals("김철수", store.savedProfile?.name)
    }

    @Test
    fun `changing only the phone number keeps the pass`() = runTest {
        val store = InMemoryVisitorStore(Fixtures.visitor, Fixtures.validPass)
        val viewModel = OnboardingViewModel(store)

        viewModel.onPhoneChange("01098765432")
        assertFalse(viewModel.state.value.nameChanged)
        viewModel.save()

        assertEquals(Fixtures.validPass, store.savedPass)
        assertEquals("01098765432", store.savedProfile?.phone?.digits)
    }
}
