package com.jumincho.cvpass.core.visitor

/**
 * A Korean mobile phone number: `010` followed by eight digits, or one of the legacy
 * `011`/`016`/`017`/`018`/`019` prefixes followed by seven or eight digits.
 *
 * Instances are always valid; obtain one via [parse].
 */
@JvmInline
value class PhoneNumber private constructor(
    /** The number as plain digits, e.g. `01012345678`. */
    val digits: String,
) {

    /** The conventional hyphenated presentation, e.g. `010-1234-5678`. */
    val formatted: String
        get() = formatPartial(digits)

    override fun toString(): String = formatted

    companion object {
        /** Maximum number of digits in a Korean mobile number. */
        const val MAX_LENGTH: Int = 11

        private val MOBILE = Regex("^(?:010[0-9]{8}|01[16789][0-9]{7,8})$")
        private val ALLOWED = Regex("^[0-9+\\-(). ]+$")

        /**
         * Parses [input], tolerating spaces, hyphens, dots, parentheses and an international
         * `+82` prefix. Returns `null` if the result is not a Korean mobile number.
         */
        fun parse(input: String): PhoneNumber? {
            val trimmed = input.trim()
            if (!ALLOWED.matches(trimmed)) return null
            var digits = trimmed.filter { it in '0'..'9' }
            if (trimmed.startsWith("+")) {
                if (!digits.startsWith("82")) return null
                digits = digits.removePrefix("82").let { if (it.startsWith("0")) it else "0$it" }
            }
            return if (MOBILE.matches(digits)) PhoneNumber(digits) else null
        }

        /**
         * Formats a partially typed number (digits only) as it would be written in full:
         * `010-1234-5678` for eleven-digit numbers, `011-123-4567` for ten-digit ones.
         */
        fun formatPartial(digits: String): String {
            val number = digits.take(MAX_LENGTH)
            val middle = if (number.length == MAX_LENGTH || number.startsWith("010")) 4 else 3
            return when {
                number.length <= 3 -> number
                number.length <= 3 + middle -> "${number.take(3)}-${number.drop(3)}"
                else -> "${number.take(3)}-${number.substring(3, 3 + middle)}-${number.drop(3 + middle)}"
            }
        }
    }
}
