package com.rattall.ntripanalyser.model

data class StreamStats(
    val connected: Boolean = false,
    val statusMessage: String = "Idle",
    val totalBytes: Long = 0,
    val bytesPerSecond: Double = 0.0,
    val totalMessages: Long = 0,
    val messagesPerSecond: Double = 0.0,
    val crcFailures: Long = 0,
    val malformedFrames: Long = 0,
    val reconnectAttempts: Int = 0,
    val sessionUptimeMs: Long = 0,
    val lastMessageTimestampMs: Long? = null
)
