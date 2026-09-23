package com.jumincho.cvpass.core.pass

import java.text.Normalizer
import java.time.DateTimeException
import java.time.LocalDate

/**
 * Extracts vaccination facts from text recognised on a COVID-19 vaccination certificate,
 * such as a screenshot of the COOV app (코로나19 전자예방접종증명서).
 *
 * OCR output has arbitrary line breaks, stray spaces inside words and full-width
 * characters, so the parser is deliberately tolerant. It looks for:
 * - dose markers: `1차`/`2차`/`3차 접종`, `접종차수 2`, `2nd dose`, booster phrases
 *   (`추가 접종`, `추가접종`, `부스터`, `booster`) and `접종완료` ("vaccination complete");
 * - dates written as `2021.10.01`, `2021-10-01`, `2021/10/01` or `2021년 10월 1일`,
 *   skipping dates labelled as issue, print, birth or validity dates and dates outside
 *   [CAMPAIGN_START]..today;
 * - the holder's name, either where the visitor's profile name appears on its own or
 *   after a `성명`/`이름`/`Name` label.
 *
 * Lines about planned doses (`예정`, `예약`) are ignored.
 */
object CertificateParser {

    /** Korea's COVID-19 vaccination campaign began on this day; earlier dates are not doses. */
    val CAMPAIGN_START: LocalDate = LocalDate.of(2021, 2, 26)

    private val INVISIBLE = Regex("[\u200B-\u200D\uFEFF]")
    private val WHITESPACE = Regex("\\s+")
    private val PLANNED = Regex("예\\s*정|예\\s*약|(?i:scheduled|appointment)")
    private val DOSE_NUMBER = Regex("(?<![0-9])([1-9])\\s*차(?!\\s*수)")
    private val DOSE_LABEL = Regex("차\\s*수\\s*:?\\s*([1-9])(?![0-9])")
    private val DOSE_ENGLISH =
        Regex("(?i)\\b(?:([1-9])(?:st|nd|rd|th)\\s*dose|doses?\\s*(?:no\\.?\\s*)?:?\\s*([1-9])(?![0-9]))")
    private val BOOSTER = Regex("추\\s*가\\s*접\\s*종|부\\s*스\\s*터|(?i:booster)")
    private val SERIES_COMPLETE = Regex("접\\s*종\\s*완\\s*료|(?i:fully\\s+vaccinated)")
    private val DATE =
        Regex("(20[0-9]{2})\\s*(?:[./-]|년)\\s*([0-9]{1,2})\\s*(?:[./-]|월)\\s*([0-9]{1,2})(?![0-9])")
    private val KOREAN_NAME_LABEL =
        Regex("(?:성\\s*명|이\\s*름)\\s*(?:[(/]\\s*(?i:name)\\s*\\)?)?\\s*:?\\s*([가-힣]{2,5})(?![가-힣])")
    private val LATIN_NAME_LABEL =
        Regex("(?i:\\bname)\\s*:?\\s*([A-Z]{2,}(?:[ -][A-Z]{2,}){0,3})(?![A-Za-z])")

    private val DOSE_KEYWORDS = listOf("접종", "차", "vaccin", "dose")
    private val OTHER_DATE_KEYWORDS = listOf(
        "발급", "출력", "생년", "생일", "조회", "기준", "유효", "만료",
        "birth", "issue", "print", "valid", "expir",
    )

    /** Particles and honorifics that may follow a Korean name (`홍길동님`, `홍길동의`). */
    private const val NAME_SUFFIX = "(?:님|씨)?(?:의|은|는|이|가|을|를)?"

    /**
     * Parses recognised [text].
     *
     * @param profileName the visitor's name; used to find the holder's name on layouts that
     *   print it without a label, as the COOV app does.
     * @param today the current date; later dates cannot be dose dates.
     */
    fun parse(text: String, profileName: String?, today: LocalDate): ParsedCertificate {
        val lines = normalizedLines(text)
        val current = lines.filterNot { PLANNED.containsMatchIn(it) }
        return ParsedCertificate(
            holderName = holderName(lines, profileName),
            highestDose = current.maxOfOrNull(::highestDoseIn) ?: 0,
            booster = current.any { BOOSTER.containsMatchIn(it) },
            seriesComplete = current.any { SERIES_COMPLETE.containsMatchIn(it) },
            lastDoseDate = doseDates(lines, today).maxByOrNull { it.toEpochDay() },
            vaccines = vaccinesIn(lines),
        )
    }

    private fun normalizedLines(text: String): List<String> =
        Normalizer.normalize(text, Normalizer.Form.NFKC)
            .replace(INVISIBLE, "")
            .lines()
            .map { it.replace(WHITESPACE, " ").trim() }
            .filter { it.isNotEmpty() }

    private fun highestDoseIn(line: String): Int {
        val numbers = DOSE_NUMBER.findAll(line).map { it.groupValues[1] } +
            DOSE_LABEL.findAll(line).map { it.groupValues[1] } +
            DOSE_ENGLISH.findAll(line).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }
        return numbers.maxOfOrNull { it.toInt() } ?: 0
    }

    private fun vaccinesIn(lines: List<String>): Set<Vaccine> {
        val compact = lines.joinToString("").replace(" ", "").lowercase()
        return Vaccine.entries.filterTo(mutableSetOf()) { vaccine -> vaccine.keywords.any { it in compact } }
    }

    private fun holderName(lines: List<String>, profileName: String?): String? {
        if (profileName != null && lines.any { mentionsName(it, profileName) }) return profileName
        val joined = lines.joinToString(" ")
        return KOREAN_NAME_LABEL.find(joined)?.groupValues?.get(1)
            ?: LATIN_NAME_LABEL.find(joined)?.groupValues?.get(1)
    }

    /** Whether [line] contains [name] as a word of its own, tolerating spaces inside it. */
    private fun mentionsName(line: String, name: String): Boolean {
        val letters = name.filterNot { it.isWhitespace() }
        if (letters.length < 2) return false
        val body = letters.map { Regex.escape(it.toString()) }.joinToString("\\s*")
        return Regex("(?<!\\p{L})$body$NAME_SUFFIX(?!\\p{L})", RegexOption.IGNORE_CASE).containsMatchIn(line)
    }

    private fun doseDates(lines: List<String>, today: LocalDate): List<LocalDate> =
        lines.flatMapIndexed { index, line ->
            if (PLANNED.containsMatchIn(line)) return@flatMapIndexed emptyList()
            val matches = DATE.findAll(line).toList()
            val labelAbove = lines.getOrNull(index - 1)?.takeUnless { DATE.containsMatchIn(it) }
            matches.mapIndexedNotNull { i, match ->
                val date = match.toDate() ?: return@mapIndexedNotNull null
                val before = line.substring(matches.getOrNull(i - 1)?.range?.last?.plus(1) ?: 0, match.range.first)
                val after = line.substring(match.range.last + 1, matches.getOrNull(i + 1)?.range?.first ?: line.length)
                val isDoseDate = classifyLabel(before, precedesDate = true)
                    ?: classifyLabel(after, precedesDate = false)
                    ?: labelAbove?.let { classifyLabel(it, precedesDate = true) }
                    ?: true
                date.takeIf { isDoseDate && !it.isBefore(CAMPAIGN_START) && !it.isAfter(today) }
            }
        }

    /**
     * Decides whether [label] marks a date as a dose date (`true`), as some other date
     * (`false`), or says nothing about it (`null`). When a label mixes both kinds of
     * keyword, as in `접종증명서 발급일`, the keyword closest to the date wins.
     */
    private fun classifyLabel(label: String, precedesDate: Boolean): Boolean? {
        val text = label.lowercase()
        fun closest(keywords: List<String>): Int? {
            val positions = keywords.mapNotNull { keyword ->
                (if (precedesDate) text.lastIndexOf(keyword) else text.indexOf(keyword)).takeIf { it >= 0 }
            }
            return if (precedesDate) positions.maxOrNull() else positions.minOrNull()
        }
        val dose = closest(DOSE_KEYWORDS)
        val other = closest(OTHER_DATE_KEYWORDS)
        return when {
            dose == null && other == null -> null
            dose == null -> false
            other == null -> true
            precedesDate -> dose > other
            else -> dose < other
        }
    }

    private fun MatchResult.toDate(): LocalDate? {
        val (year, month, day) = destructured
        return try {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        } catch (e: DateTimeException) {
            null
        }
    }
}
