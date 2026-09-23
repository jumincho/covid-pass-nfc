package com.jumincho.cvpass.core.visitor

/** Validation and comparison rules for a person's name as typed into the app. */
object PersonName {

    /** Shortest accepted name, in characters. Korean names have at least two syllables. */
    const val MIN_LENGTH: Int = 2

    /** Longest accepted name, in characters. */
    const val MAX_LENGTH: Int = 30

    private val WHITESPACE = Regex("\\s+")
    private val PUNCTUATION = setOf(' ', '-', '.', '\'')

    /**
     * Returns [input] trimmed and with inner whitespace collapsed, or `null` if it is not
     * a plausible name: [MIN_LENGTH]..[MAX_LENGTH] letters, spaces, hyphens, dots or
     * apostrophes.
     */
    fun normalize(input: String): String? {
        val name = input.trim().replace(WHITESPACE, " ")
        val plausible = name.length in MIN_LENGTH..MAX_LENGTH &&
            name.all { it.isLetter() || it in PUNCTUATION } &&
            name.any { it.isLetter() }
        return name.takeIf { plausible }
    }

    /** Whether [a] and [b] name the same person, ignoring spacing and letter case. */
    fun matches(a: String, b: String): Boolean = comparisonKey(a) == comparisonKey(b)

    private fun comparisonKey(name: String): String = name.filterNot { it.isWhitespace() }.uppercase()
}
