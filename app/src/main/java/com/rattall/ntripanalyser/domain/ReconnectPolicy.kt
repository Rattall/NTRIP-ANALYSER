package com.rattall.ntripanalyser.domain

data class ReconnectPolicy(
    val maxAttempts: Int = 5,
    val baseDelayMs: Long = 1_000,
    val maxDelayMs: Long = 15_000
) {
    init {
        require(maxAttempts >= 0)
        require(baseDelayMs > 0)
        require(maxDelayMs >= baseDelayMs)
    }

    fun delayForAttempt(attempt: Int): Long {
        require(attempt >= 1)
        val exponential = baseDelayMs * (1L shl (attempt - 1).coerceAtMost(20))
        return exponential.coerceAtMost(maxDelayMs)
    }
}
