package com.jumincho.cvpass.core.business

/**
 * A Korean business registration number (사업자등록번호).
 *
 * The number has ten digits; the last one is a check digit computed from the first nine
 * with the National Tax Service weights `1 3 7 1 3 7 1 3 5`, plus `⌊d₉ × 5 / 10⌋` for the
 * ninth digit. An instance always holds a well-formed number: obtain one via [parse].
 *
 * A valid checksum only proves the number is well formed, not that the business exists;
 * [NtsBusinessRegistry] asks the tax service for that.
 */
@JvmInline
value class BusinessNumber private constructor(
    /** The ten digits without separators, e.g. `1248100998`. */
    val digits: String,
) {

    /** The conventional `123-45-67890` presentation. */
    val formatted: String
        get() = "${digits.substring(0, 3)}-${digits.substring(3, 5)}-${digits.substring(5)}"

    override fun toString(): String = formatted

    /** Why a candidate string is not a valid business registration number. */
    enum class Problem {
        /** Not exactly ten digits once spaces and hyphens are removed. */
        WRONG_FORMAT,

        /** Ten digits, but the check digit does not match. */
        CHECKSUM_MISMATCH,
    }

    companion object {
        /** Number of digits in a business registration number. */
        const val LENGTH: Int = 10

        private val WEIGHTS = intArrayOf(1, 3, 7, 1, 3, 7, 1, 3, 5)

        /**
         * Parses [input], ignoring spaces and hyphens, or returns `null` if it is not a
         * valid number. Use [problemWith] to learn why a value was rejected.
         */
        fun parse(input: String): BusinessNumber? =
            if (problemWith(input) == null) BusinessNumber(stripSeparators(input)) else null

        /** Returns what is wrong with [input], or `null` if it is a valid number. */
        fun problemWith(input: String): Problem? {
            val digits = stripSeparators(input)
            return when {
                digits.length != LENGTH || !digits.all { it in '0'..'9' } -> Problem.WRONG_FORMAT
                checkDigit(digits) != digits.last().digitToInt() -> Problem.CHECKSUM_MISMATCH
                else -> null
            }
        }

        /** Computes the expected tenth digit for the first nine digits of [digits]. */
        internal fun checkDigit(digits: String): Int {
            require(digits.length >= WEIGHTS.size) { "Need at least ${WEIGHTS.size} digits" }
            val ninth = digits[8].digitToInt()
            val weighted = WEIGHTS.indices.sumOf { digits[it].digitToInt() * WEIGHTS[it] }
            val sum = weighted + ninth * 5 / 10
            return (10 - sum % 10) % 10
        }

        /**
         * Formats a partially typed number (digits only) progressively as `123-45-67890`,
         * for use while the user is still typing.
         */
        fun formatPartial(digits: String): String = buildString {
            digits.take(LENGTH).forEachIndexed { index, char ->
                if (index == 3 || index == 5) append('-')
                append(char)
            }
        }

        private fun stripSeparators(input: String): String =
            input.filterNot { it == '-' || it.isWhitespace() }
    }
}
