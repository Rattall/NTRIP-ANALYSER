package com.rattall.ntripanalyser.rtcm.decoders

import com.rattall.ntripanalyser.rtcm.BitBuffer
import com.rattall.ntripanalyser.rtcm.RtcmDecoder

class Decoders1230 : RtcmDecoder {
    override fun decode(payload: ByteArray): Map<String, Any?> {
        val bits = BitBuffer(payload)
        bits.readUnsigned(12)
        val stationId = bits.readUnsigned(12).toInt()
        bits.readUnsigned(1)
        val codePhaseBiasMask = bits.readUnsigned(4).toInt()
        val fdmaSignalMask = bits.readUnsigned(4).toInt()

        return mapOf(
            "stationId" to stationId,
            "codePhaseBiasMask" to codePhaseBiasMask,
            "fdmaSignalMask" to fdmaSignalMask,
            "bitsRemaining" to bits.bitsRemaining()
        )
    }
}
