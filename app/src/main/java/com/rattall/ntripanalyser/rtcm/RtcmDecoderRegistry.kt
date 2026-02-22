package com.rattall.ntripanalyser.rtcm

import com.rattall.ntripanalyser.rtcm.decoders.Decoders1004
import com.rattall.ntripanalyser.rtcm.decoders.Decoders1005
import com.rattall.ntripanalyser.rtcm.decoders.Decoders1006
import com.rattall.ntripanalyser.rtcm.decoders.Decoders1012
import com.rattall.ntripanalyser.rtcm.decoders.Decoders1033
import com.rattall.ntripanalyser.rtcm.decoders.Decoders1230
import com.rattall.ntripanalyser.rtcm.decoders.MsmDecoder

class RtcmDecoderRegistry {
    private val map: Map<Int, RtcmDecoder> = buildMap {
        put(1004, Decoders1004())
        put(1012, Decoders1012())
        put(1005, Decoders1005())
        put(1006, Decoders1006())
        put(1033, Decoders1033())
        put(1230, Decoders1230())
        for (type in 1071..1077) put(type, MsmDecoder(type))
        for (type in 1081..1087) put(type, MsmDecoder(type))
        for (type in 1091..1097) put(type, MsmDecoder(type))
        for (type in 1121..1127) put(type, MsmDecoder(type))
    }

    fun decode(frame: RtcmFrame): RtcmDecodedMessage {
        val bitBuffer = BitBuffer(frame.payload)
        val messageType = bitBuffer.readUnsigned(12).toInt()
        val fields = map[messageType]?.decode(frame.payload)
            ?: mapOf(
                "unsupported" to true,
                "rawPayloadHex" to frame.payload.joinToString(separator = "") { "%02X".format(it) }
            )
        return RtcmDecodedMessage(
            messageType = messageType,
            crcValid = frame.crcValid,
            payloadLength = frame.payload.size,
            fields = fields
        )
    }
}
