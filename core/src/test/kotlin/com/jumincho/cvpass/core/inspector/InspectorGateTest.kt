package com.jumincho.cvpass.core.inspector

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InspectorGateTest {

    @Test
    fun `unlocks with the configured PIN only`() {
        val gate = InspectorGate("4321")

        assertTrue(gate.isEnabled)
        assertTrue(gate.unlocks("4321"))
        assertTrue(gate.unlocks(" 4321 "))
        assertFalse(gate.unlocks("1234"))
        assertFalse(gate.unlocks("43210"))
        assertFalse(gate.unlocks(""))
    }

    @Test
    fun `a blank PIN disables inspector mode`() {
        val gate = InspectorGate("  ")

        assertFalse(gate.isEnabled)
        assertFalse(gate.unlocks(""))
        assertFalse(gate.unlocks("  "))
    }
}
