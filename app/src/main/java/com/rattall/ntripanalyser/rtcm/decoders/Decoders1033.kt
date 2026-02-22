package com.rattall.ntripanalyser.rtcm.decoders

import com.rattall.ntripanalyser.rtcm.BitBuffer
import com.rattall.ntripanalyser.rtcm.RtcmDecoder

class Decoders1033 : RtcmDecoder {
    override fun decode(payload: ByteArray): Map<String, Any?> {
        val bits = BitBuffer(payload)
        bits.readUnsigned(12)
        val stationId = bits.readUnsigned(12).toInt()
        val descriptorCount = bits.readUnsigned(8).toInt()
        val descriptor = readAscii(bits, descriptorCount)
        val setupId = bits.readUnsigned(8).toInt()
        val serialCount = bits.readUnsigned(8).toInt()
        val serial = readAscii(bits, serialCount)

        return mapOf(
            "stationId" to stationId,
            "antennaDescriptor" to descriptor,
            "antennaSetupId" to setupId,
            "antennaSerial" to serial,
            "bitsRemaining" to bits.bitsRemaining()
        )
    }

    private fun readAscii(bits: BitBuffer, count: Int): String {
        val chars = CharArray(count)
        for (i in 0 until count) {
            chars[i] = bits.readUnsigned(8).toInt().toChar()
        }
        return String(chars)
    }
}
