package com.jumincho.cvpass.ui.components

import androidx.compose.ui.text.AnnotatedString
import com.jumincho.cvpass.core.visitor.PhoneNumber
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FormattedDigitsTransformationTest {

    private val transformation = FormattedDigitsTransformation(PhoneNumber::formatPartial)

    @Test
    fun `shows digits with separators`() {
        assertEquals("010-1234-5678", transformation.filter(AnnotatedString("01012345678")).text.text)
    }

    @Test
    fun `maps cursor positions across the inserted separators`() {
        val mapping = transformation.filter(AnnotatedString("01012345678")).offsetMapping

        assertEquals(listOf(0, 1, 2, 3, 5, 6, 7, 8, 10, 11, 12, 13), (0..11).map(mapping::originalToTransformed))
        assertEquals(listOf(0, 1, 2, 3, 3, 4, 5, 6, 7, 7, 8, 9, 10, 11), (0..13).map(mapping::transformedToOriginal))
    }

    @Test
    fun `handles partial and empty input`() {
        val empty = transformation.filter(AnnotatedString(""))
        assertEquals("", empty.text.text)
        assertEquals(0, empty.offsetMapping.originalToTransformed(0))

        val partial = transformation.filter(AnnotatedString("0101"))
        assertEquals("010-1", partial.text.text)
        assertEquals(5, partial.offsetMapping.originalToTransformed(4))
        assertEquals(4, partial.offsetMapping.transformedToOriginal(5))
    }
}
