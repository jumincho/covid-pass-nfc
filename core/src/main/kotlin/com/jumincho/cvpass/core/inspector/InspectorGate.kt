package com.jumincho.cvpass.core.inspector

import java.security.MessageDigest

/**
 * Unlocks inspector mode with a PIN configured at build time.
 *
 * This is a demonstration gate, not access control: the PIN ships inside the app and can
 * be extracted from it. A real deployment would authenticate inspectors on a server and
 * enforce access in the backend's rules.
 *
 * @param pin the configured PIN; blank disables inspector mode entirely.
 */
class InspectorGate(pin: String) {

    private val expected = pin.trim().encodeToByteArray()

    /** Whether a PIN is configured at all. */
    val isEnabled: Boolean
        get() = expected.isNotEmpty()

    /** Whether [input] is the configured PIN. Compares in constant time. */
    fun unlocks(input: String): Boolean =
        isEnabled && MessageDigest.isEqual(expected, input.trim().encodeToByteArray())
}
