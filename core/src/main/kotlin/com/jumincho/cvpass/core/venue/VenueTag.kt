package com.jumincho.cvpass.core.venue

import com.jumincho.cvpass.core.business.BusinessNumber

/**
 * What a venue's NFC tag identifies: the business and the name visitors see on check-in.
 *
 * @property name a display name already normalised by [normalizeName].
 */
data class VenueTag(val businessNumber: BusinessNumber, val name: String) {
    init {
        require(normalizeName(name) == name) { "Venue name must be normalised first" }
    }

    companion object {
        /** Longest accepted venue name, in characters; keeps the tag payload small. */
        const val MAX_NAME_LENGTH: Int = 30

        private val WHITESPACE = Regex("\\s+")

        /**
         * Returns [input] trimmed with inner whitespace collapsed, or `null` if it is blank,
         * longer than [MAX_NAME_LENGTH] or contains control characters.
         */
        fun normalizeName(input: String): String? {
            val name = input.trim().replace(WHITESPACE, " ")
            val acceptable = name.isNotEmpty() &&
                name.length <= MAX_NAME_LENGTH &&
                name.none { Character.isISOControl(it) }
            return name.takeIf { acceptable }
        }
    }
}
