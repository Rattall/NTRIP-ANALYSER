package com.rattall.ntripanalyser.rtcm

data class RtcmFrame(
    val rawFrame: ByteArray,
    val payload: ByteArray,
    val crcValid: Boolean
)

data class RtcmDecodedMessage(
    val messageType: Int,
    val crcValid: Boolean,
    val payloadLength: Int,
    val fields: Map<String, Any?>,
    val receivedAtMs: Long = System.currentTimeMillis()
)
