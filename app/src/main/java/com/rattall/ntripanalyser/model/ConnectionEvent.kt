package com.rattall.ntripanalyser.model

data class ConnectionEvent(
    val timestampMs: Long = System.currentTimeMillis(),
    val message: String
)
