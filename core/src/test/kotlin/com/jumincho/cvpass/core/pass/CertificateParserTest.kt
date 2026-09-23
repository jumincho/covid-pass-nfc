package com.jumincho.cvpass.core.pass

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The OCR texts below are synthetic; they mimic layouts and recognition noise. */
class CertificateParserTest {

    private val today = LocalDate.of(2021, 10, 20)

    private fun parse(text: String, profileName: String? = "홍길동") =
        CertificateParser.parse(text.trimIndent(), profileName, today)

    @Test
    fun `reads an app screenshot that prints the name without a label`() {
        val certificate = parse(
            """
            COOV
            코로나19 예방접종 증명서
            홍길동
            1990.01.01
            접종완료
            화이자
            2차 접종
            접종일 2021.08.15
            발급일시 2021.10.20 14:03:11
            """,
        )

        assertEquals("홍길동", certificate.holderName)
        assertEquals(2, certificate.highestDose)
        assertEquals(2, certificate.doses)
        assertFalse(certificate.booster)
        assertEquals(LocalDate.of(2021, 8, 15), certificate.lastDoseDate)
        assertEquals(setOf(Vaccine.PFIZER), certificate.vaccines)
    }

    @Test
    fun `reads a labelled certificate with a table of doses`() {
        val certificate = parse(
            """
            예방접종증명서 (Certificate of Vaccination)
            성명(Name) 홍길동 HONG GILDONG
            생년월일(Date of Birth) 1985-03-12
            접종명 차수 접종일 백신명
            코로나19 1차 2021-06-10 아스트라제네카
            코로나19 2차 2021-08-19 아스트라제네카
            발급일 2021-10-01
            """,
        )

        assertEquals("홍길동", certificate.holderName)
        assertEquals(2, certificate.doses)
        assertEquals(LocalDate.of(2021, 8, 19), certificate.lastDoseDate)
        assertEquals(setOf(Vaccine.ASTRAZENECA), certificate.vaccines)
    }

    @Nested
    inner class Doses {

        @ParameterizedTest
        @CsvSource(
            "1차 접종 완료, 1",
            "2차접종, 2",
            "'2 차 접종', 2",
            "3차 접종 2021.10.15, 3",
            "접종차수: 2, 2",
            "코로나19 차수 1, 1",
            "2nd dose, 2",
            "Dose 2 of 2, 2",
            "Doses: 1, 1",
        )
        fun `reads the dose number`(line: String, expected: Int) {
            assertEquals(expected, parse(line).highestDose)
        }

        @Test
        fun `does not read the 19 in COVID-19 as a dose number`() {
            assertEquals(0, parse("코로나19 차수").highestDose)
            assertEquals(0, parse("코로나19차 접종 안내").highestDose)
        }

        @ParameterizedTest
        @CsvSource(
            "추가 접종",
            "추가접종 완료",
            "'추 가 접 종'",
            "부스터샷",
            "Booster dose",
        )
        fun `recognises a booster`(line: String) {
            val certificate = parse(line)
            assertTrue(certificate.booster)
            assertEquals(3, certificate.doses)
        }

        @Test
        fun `counts a Janssen booster as the second dose`() {
            val certificate = parse("얀센\n추가접종 2021.10.01")
            assertEquals(1, certificate.primarySeriesDoses)
            assertEquals(2, certificate.doses)
        }

        @Test
        fun `treats a bare completion statement as the primary series`() {
            val certificate = parse("접종 완료\n2021.08.15")
            assertTrue(certificate.seriesComplete)
            assertEquals(2, certificate.doses)
        }

        @Test
        fun `does not upgrade an explicit first dose to a complete series`() {
            assertEquals(1, parse("1차 접종 완료\n2021.08.15").doses)
        }

        @Test
        fun `ignores planned doses`() {
            val certificate = parse(
                """
                1차 접종 2021.08.01
                2차 접종 예정일 2021.09.12
                """,
            )
            assertEquals(1, certificate.doses)
            assertEquals(LocalDate.of(2021, 8, 1), certificate.lastDoseDate)
        }

        @Test
        fun `reports no doses when nothing matches`() {
            val certificate = parse("홍길동\n2021.08.15")
            assertEquals(0, certificate.doses)
            assertNull(certificate.toRecord())
        }
    }

    @Nested
    inner class Dates {

        @ParameterizedTest
        @CsvSource(
            "접종일 2021.10.01, 2021-10-01",
            "접종일 2021-10-01, 2021-10-01",
            "접종일 2021/10/01, 2021-10-01",
            "'접종일 2021. 10. 1.', 2021-10-01",
            "접종일 2021년 10월 1일, 2021-10-01",
            "'접종일 2021년10월1일(금)', 2021-10-01",
            "접종일：２０２１．１０．０１, 2021-10-01",
        )
        fun `reads common date formats`(line: String, expected: LocalDate) {
            assertEquals(expected, parse(line).lastDoseDate)
        }

        @Test
        fun `picks the latest vaccination date`() {
            val certificate = parse("1차 2021.06.01 / 2차 2021.08.15")
            assertEquals(LocalDate.of(2021, 8, 15), certificate.lastDoseDate)
        }

        @Test
        fun `ignores issue, birth and validity dates`() {
            val certificate = parse(
                """
                생년월일 1990.01.01
                접종일 2021.08.15
                발급일 2021.10.01
                출력일시 2021.10.20 10:00
                유효기간 2021.10.20
                """,
            )
            assertEquals(LocalDate.of(2021, 8, 15), certificate.lastDoseDate)
        }

        @Test
        fun `lets the keyword closest to a date decide its meaning`() {
            val certificate = parse("접종증명서 발급일 2021.10.18 최종 접종일 2021.08.15")
            assertEquals(LocalDate.of(2021, 8, 15), certificate.lastDoseDate)
        }

        @Test
        fun `uses a label on the line above a bare date`() {
            val certificate = parse(
                """
                발급일
                2021.10.18
                최종 접종일
                2021.08.15
                """,
            )
            assertEquals(LocalDate.of(2021, 8, 15), certificate.lastDoseDate)
        }

        @Test
        fun `uses a label written after the date`() {
            val certificate = parse("2021.10.18 발급\n2021.08.15 접종")
            assertEquals(LocalDate.of(2021, 8, 15), certificate.lastDoseDate)
        }

        @Test
        fun `ignores dates before the campaign or after today`() {
            val certificate = parse(
                """
                접종일 2021.02.25
                접종일 2021.10.21
                접종일 2021.03.02
                """,
            )
            assertEquals(LocalDate.of(2021, 3, 2), certificate.lastDoseDate)
        }

        @Test
        fun `ignores impossible dates`() {
            assertNull(parse("접종일 2021.13.01\n접종일 2021.02.30").lastDoseDate)
        }

        @Test
        fun `survives line breaks and stray spaces from OCR`() {
            val certificate = parse(
                """
                코 로 나 19
                2 차
                접종
                접종 일자
                2021 . 08 . 15
                """,
            )
            assertEquals(2, certificate.doses)
            assertEquals(LocalDate.of(2021, 8, 15), certificate.lastDoseDate)
        }
    }

    @Nested
    inner class HolderName {

        @Test
        fun `finds the profile name even with particles or spaces`() {
            assertEquals("홍길동", parse("홍길동님의 예방접종 증명서").holderName)
            assertEquals("홍길동", parse("홍 길 동").holderName)
        }

        @Test
        fun `does not match a name that is only part of a longer one`() {
            assertNull(parse("홍길동", profileName = "홍길").holderName)
        }

        @Test
        fun `falls back to a labelled name when the profile name is absent`() {
            assertEquals("김철수", parse("성명: 김철수\n2차 접종 2021.08.15").holderName)
            assertEquals("김철수", parse("성 명\n김철수").holderName)
            assertEquals("김철수", parse("이름 / Name 김철수").holderName)
        }

        @Test
        fun `reads a romanised name`() {
            assertEquals("HONG GILDONG", parse("Name: HONG GILDONG\nDose 2", profileName = "김철수").holderName)
            assertEquals("Hong Gildong", parse("Name: HONG GILDONG", profileName = "Hong Gildong").holderName)
        }

        @Test
        fun `returns no name when there is none`() {
            assertNull(parse("2차 접종 2021.08.15", profileName = null).holderName)
            assertNull(parse("2차 접종 2021.08.15", profileName = "김철수").holderName)
        }
    }

    @Test
    fun `reads an English certificate`() {
        val certificate = parse(
            """
            COVID-19 Vaccination Certificate
            Name: HONG GILDONG
            Vaccine: Pfizer-BioNTech (Comirnaty)
            Dose 2
            Date of vaccination: 2021-08-15
            Date of issue: 2021-10-01
            """,
            profileName = "Hong Gildong",
        )
        assertEquals("Hong Gildong", certificate.holderName)
        assertEquals(2, certificate.doses)
        assertEquals(LocalDate.of(2021, 8, 15), certificate.lastDoseDate)
        assertEquals(setOf(Vaccine.PFIZER), certificate.vaccines)
    }

    @Test
    fun `recognises every vaccine used in Korea`() {
        val certificate = parse("화 이 자 / 모더나 / AstraZeneca / 얀센 / Novavax")
        assertEquals(Vaccine.entries.toSet(), certificate.vaccines)
        assertEquals(2, certificate.primarySeriesDoses)
    }

    @Test
    fun `returns an empty result for empty text`() {
        val certificate = parse("", profileName = null)
        assertEquals(
            ParsedCertificate(
                holderName = null,
                highestDose = 0,
                booster = false,
                seriesComplete = false,
                lastDoseDate = null,
                vaccines = emptySet(),
            ),
            certificate,
        )
    }
}
