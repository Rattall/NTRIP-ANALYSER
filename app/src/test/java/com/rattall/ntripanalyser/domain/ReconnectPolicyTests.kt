package com.rattall.ntripanalyser.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectPolicyTests {

    @Test
    fun delayForAttempt_exponentialUntilCap() {
        val policy = ReconnectPolicy(maxAttempts = 8, baseDelayMs = 1_000, maxDelayMs = 10_000)

        assertEquals(1_000, policy.delayForAttempt(1))
        assertEquals(2_000, policy.delayForAttempt(2))
        assertEquals(4_000, policy.delayForAttempt(3))
        assertEquals(8_000, policy.delayForAttempt(4))
        assertEquals(10_000, policy.delayForAttempt(5))
        assertEquals(10_000, policy.delayForAttempt(6))
    }

    @Test
    fun delayForAttempt_withSmallBase() {
        val policy = ReconnectPolicy(maxAttempts = 3, baseDelayMs = 250, maxDelayMs = 1_000)

        assertEquals(250, policy.delayForAttempt(1))
        assertEquals(500, policy.delayForAttempt(2))
        assertEquals(1_000, policy.delayForAttempt(3))
    }
}
