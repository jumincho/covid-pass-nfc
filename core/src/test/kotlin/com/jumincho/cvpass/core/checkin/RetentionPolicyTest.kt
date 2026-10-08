package com.jumincho.cvpass.core.checkin

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class RetentionPolicyTest {

    @Test
    fun `entries older than four weeks are due for deletion`() {
        val now = Instant.parse("2021-11-17T09:30:00Z")
        assertEquals(Instant.parse("2021-10-20T09:30:00Z"), RetentionPolicy.cutoff(now))
    }
}
