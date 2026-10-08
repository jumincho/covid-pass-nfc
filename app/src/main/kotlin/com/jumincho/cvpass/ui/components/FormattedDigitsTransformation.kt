package com.jumincho.cvpass.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Shows digits-only input with separators, such as `01012345678` as `010-1234-5678`,
 * while the underlying value stays plain digits.
 */
class FormattedDigitsTransformation(private val format: (String) -> String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = format(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var digits = 0
                formatted.forEachIndexed { index, char ->
                    if (char.isDigit()) {
                        digits++
                        if (digits == offset) return index + 1
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it.isDigit() }.coerceAtMost(text.length)
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
