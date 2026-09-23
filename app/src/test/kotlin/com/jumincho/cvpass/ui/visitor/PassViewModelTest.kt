package com.jumincho.cvpass.ui.visitor

import com.jumincho.cvpass.core.checkin.Visit
import com.jumincho.cvpass.core.pass.PassStatus
import com.jumincho.cvpass.core.pass.VaccinationRecord
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.testing.InMemoryVisitHistory
import com.jumincho.cvpass.core.testing.InMemoryVisitorStore
import com.jumincho.cvpass.ocr.CertificateTextReader
import com.jumincho.cvpass.testing.Fixtures
import com.jumincho.cvpass.testing.MainDispatcherExtension
import com.jumincho.cvpass.testing.keepCollecting
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MainDispatcherExtension::class)
class PassViewModelTest {

    private val clock = Fixtures.clock()
    private val visits = InMemoryVisitHistory()

    private fun viewModel(store: InMemoryVisitorStore, reader: CertificateTextReader = CertificateTextReader { "" }) =
        PassViewModel(store, visits, reader, clock)

    @Test
    fun `evaluates the stored pass for today`() = runTest {
        val pass = VerifiedPass(
            VaccinationRecord(doses = 2, primarySeriesDoses = 2, lastDoseDate = LocalDate.of(2021, 10, 15)),
            verifiedAt = Fixtures.now,
        )
        val viewModel = viewModel(InMemoryVisitorStore(Fixtures.visitor, pass))
        keepCollecting(viewModel.state)

        assertEquals(PassStatus.NotYetValid(LocalDate.of(2021, 10, 29), 9), viewModel.state.value.status)
    }

    @Test
    fun `shows the most recent visits`() = runTest {
        visits.add(Visit(Fixtures.venueNumber, "카페 전주", Fixtures.now))
        val viewModel = viewModel(InMemoryVisitorStore(Fixtures.visitor))
        keepCollecting(viewModel.state)

        assertEquals(listOf("카페 전주"), viewModel.state.value.recentVisits.map { it.venueName })
    }

    @Test
    fun `a valid certificate is accepted and only its derived facts are stored`() = runTest {
        val store = InMemoryVisitorStore(Fixtures.visitor)
        val viewModel = viewModel(store) { imageUri ->
            check(imageUri == "content://picked/1")
            "홍길동\n화이자 2차 접종\n접종일 2021.09.01\n발급일 2021.10.20"
        }
        keepCollecting(viewModel.state)

        viewModel.onCertificatePicked("content://picked/1")

        assertEquals(VerificationState.Accepted(PassStatus.Valid), viewModel.state.value.verification)
        assertEquals(
            VerifiedPass(VaccinationRecord(2, 2, LocalDate.of(2021, 9, 1)), verifiedAt = Fixtures.now),
            store.savedPass,
        )
        assertEquals(PassStatus.Valid, viewModel.state.value.status)
    }

    @Test
    fun `a certificate within the waiting period is saved as not yet valid`() = runTest {
        val store = InMemoryVisitorStore(Fixtures.visitor)
        val viewModel = viewModel(store) { "홍길동\n2차 접종\n접종일 2021.10.18" }
        keepCollecting(viewModel.state)

        viewModel.onCertificatePicked("content://picked/2")

        assertEquals(
            VerificationState.Accepted(PassStatus.NotYetValid(LocalDate.of(2021, 11, 1), 12)),
            viewModel.state.value.verification,
        )
        assertEquals(LocalDate.of(2021, 10, 18), store.savedPass?.record?.lastDoseDate)
    }

    @Test
    fun `a certificate for someone else is rejected and nothing is stored`() = runTest {
        val store = InMemoryVisitorStore(Fixtures.visitor)
        val viewModel = viewModel(store) { "성명 김철수\n2차 접종\n접종일 2021.09.01" }
        keepCollecting(viewModel.state)

        viewModel.onCertificatePicked("content://picked/3")

        assertEquals(
            VerificationState.Rejected(PassStatus.Reason.NameMismatch("김철수")),
            viewModel.state.value.verification,
        )
        assertNull(store.savedPass)
    }

    @Test
    fun `an unreadable image is reported`() = runTest {
        val viewModel = viewModel(InMemoryVisitorStore(Fixtures.visitor)) { throw IOException("cannot decode") }
        keepCollecting(viewModel.state)

        viewModel.onCertificatePicked("content://picked/4")

        assertEquals(VerificationState.ReadFailed, viewModel.state.value.verification)
    }

    @Test
    fun `the result can be dismissed`() = runTest {
        val viewModel = viewModel(InMemoryVisitorStore(Fixtures.visitor)) { "" }
        keepCollecting(viewModel.state)
        viewModel.onCertificatePicked("content://picked/5")

        viewModel.dismissVerification()

        assertEquals(VerificationState.Idle, viewModel.state.value.verification)
    }
}
