package com.rattall.ntripanalyser.rtcm

fun interface RtcmDecoder {
    fun decode(payload: ByteArray): Map<String, Any?>
}
