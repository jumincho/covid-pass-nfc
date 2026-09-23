package com.jumincho.cvpass.core.privacy

import com.jumincho.cvpass.core.visitor.PhoneNumber

/**
 * Partial masking for lists that show personal data at a glance, in the style of Korean
 * entry logs: `홍*동`, `010-****-5678`.
 */
object Masking {

    /**
     * Keeps the first and last character of [name] and masks the rest (`홍길동` → `홍*동`,
     * `남궁민수` → `남**수`). Two-character names keep only the first one (`이준` → `이*`).
     * Spaces are preserved so that the shape of a multi-word name stays readable.
     */
    fun name(name: String): String {
        val trimmed = name.trim()
        return when (trimmed.length) {
            0 -> ""
            1 -> "*"
            2 -> "${trimmed.first()}*"
            else -> buildString {
                append(trimmed.first())
                trimmed.substring(1, trimmed.length - 1).forEach { append(if (it == ' ') ' ' else '*') }
                append(trimmed.last())
            }
        }
    }

    /** Masks the middle group of [phone]: `010-****-5678`, or `011-***-4567` for ten digits. */
    fun phone(phone: PhoneNumber): String {
        val digits = phone.digits
        return "${digits.take(3)}-${"*".repeat(digits.length - 7)}-${digits.takeLast(4)}"
    }
}
