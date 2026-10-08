package com.jumincho.cvpass.core.pass

import com.jumincho.cvpass.core.testing.InMemoryVisitorStore
import com.jumincho.cvpass.core.testing.MutableClock
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.core.visitor.VisitorProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/** The certificate texts below are synthetic; they mimic what OCR reads from the COOV app. */
class PassVerifierTest {

    private val clock = MutableClock(Instant.parse("2021-10-20T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val visitor = VisitorProfile("홍길동", PhoneNumber.parse("010-1234-5678")!!)
    private val store = InMemoryVisitorStore(visitor)

    private fun verifier(read: suspend (String) -> String) = PassVerifier(CertificateTextReader(read), store, clock)

    @Test
    fun `saves only the facts derived from a qualifying certificate`() = runTest {
        val result = verifier { "홍길동\n화이자 2차 접종\n접종일 2021.09.01\n발급일 2021.10.20" }.verify("content://picked/1")

        val pass = VerifiedPass(VaccinationRecord(2, 2, LocalDate.of(2021, 9, 1)), verifiedAt = clock.instant())
        assertEquals(PassVerificationResult.Accepted(pass, PassStatus.Valid), result)
        assertEquals(pass, store.savedPass)
    }

    @Test
    fun `reads the image it is given`() = runTest {
        val asked = mutableListOf<String>()
        val verifier = verifier { imageUri ->
            asked += imageUri
            ""
        }

        verifier.verify("content://picked/2")

        assertEquals(listOf("content://picked/2"), asked)
    }

    @Test
    fun `accepts a certificate within the waiting period as not yet valid`() = runTest {
        val result = verifier { "홍길동\n2차 접종\n접종일 2021.10.18" }.verify("content://picked/3")

        val accepted = assertIs<PassVerificationResult.Accepted>(result)
        assertEquals(PassStatus.NotYetValid(LocalDate.of(2021, 11, 1), 12), accepted.status)
        assertEquals(LocalDate.of(2021, 10, 18), store.savedPass?.record?.lastDoseDate)
    }

    @Test
    fun `rejects a certificate for someone else and saves nothing`() = runTest {
        val result = verifier { "성명 김철수\n2차 접종\n접종일 2021.09.01" }.verify("content://picked/4")

        assertEquals(PassVerificationResult.Rejected(PassStatus.Reason.NameMismatch("김철수")), result)
        assertNull(store.savedPass)
    }

    @Test
    fun `rejects a certificate without a dose number`() = runTest {
        val result = verifier { "홍길동\n접종일 2021.09.01" }.verify("content://picked/5")

        assertEquals(PassVerificationResult.Rejected(PassStatus.Reason.NoDoseFound), result)
    }

    @Test
    fun `reports an image that cannot be read`() = runTest {
        val result = verifier { throw IOException("cannot decode") }.verify("content://picked/6")

        assertEquals("cannot decode", assertIs<PassVerificationResult.Unreadable>(result).cause.message)
        assertNull(store.savedPass)
    }

    @Test
    fun `reports a pass that could not be saved`() = runTest {
        store.failure = IOException("disk full")

        val result = verifier { "홍길동\n2차 접종\n접종일 2021.09.01" }.verify("content://picked/7")

        assertEquals("disk full", assertIs<PassVerificationResult.Failed>(result).cause.message)
    }

    @Test
    fun `needs the visitor's details to match the name`() = runTest {
        val verifier = PassVerifier({ error("The image must not be read") }, InMemoryVisitorStore(), clock)

        assertEquals(PassVerificationResult.ProfileMissing, verifier.verify("content://picked/8"))
    }

    @Test
    fun `does not swallow cancellation`() = runTest {
        assertFailsWith<CancellationException> {
            verifier { throw CancellationException("left the screen") }.verify("content://picked/9")
        }
    }
}
