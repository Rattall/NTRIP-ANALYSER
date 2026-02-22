package com.rattall.ntripanalyser.rtcm.decoders

import com.rattall.ntripanalyser.rtcm.BitBuffer
import com.rattall.ntripanalyser.rtcm.RtcmDecoder

class Decoders1012 : RtcmDecoder {
    override fun decode(payload: ByteArray): Map<String, Any?> {
        val bits = BitBuffer(payload)
        bits.readUnsigned(12)
        val stationId = bits.readUnsigned(12).toInt()
        val epochTime = bits.readUnsigned(27)
        val synchronous = bits.readUnsigned(1).toInt() == 1
        val satelliteCount = bits.readUnsigned(5).toInt()
        val smoothingIndicator = bits.readUnsigned(1).toInt() == 1
        val smoothingInterval = bits.readUnsigned(3).toInt()

        val satellites = mutableListOf<Map<String, Any?>>()
        repeat(satelliteCount) { index ->
            if (bits.bitsRemaining() < 130) {
                satellites += mapOf(
                    "index" to index,
                    "truncated" to true,
                    "bitsRemaining" to bits.bitsRemaining()
                )
                return@repeat
            }

            val satelliteId = bits.readUnsigned(6).toInt()
            val l1Code = bits.readUnsigned(1).toInt()
            val frequencyChannelNumber = bits.readSigned(5).toInt()
            val l1Pseudorange = bits.readUnsigned(25)
            val l1PhaseRangeMinusPseudorange = bits.readSigned(20)
            val l1LockTimeIndicator = bits.readUnsigned(7).toInt()
            val integerL1PseudorangeModulus = bits.readUnsigned(7).toInt()
            val l1Cn0 = bits.readUnsigned(8).toInt()
            val l2Code = bits.readUnsigned(2).toInt()
            val l2MinusL1Pseudorange = bits.readSigned(14)
            val l2MinusL1PhaseRange = bits.readSigned(20)
            val l2LockTimeIndicator = bits.readUnsigned(7).toInt()
            val l2Cn0 = bits.readUnsigned(8).toInt()

            satellites += mapOf(
                "index" to index,
                "satelliteId" to satelliteId,
                "frequencyChannelNumber" to frequencyChannelNumber,
                "l1Code" to l1Code,
                "l1Pseudorange" to l1Pseudorange,
                "l1PhaseRangeMinusPseudorange" to l1PhaseRangeMinusPseudorange,
                "l1LockTimeIndicator" to l1LockTimeIndicator,
                "integerL1PseudorangeModulus" to integerL1PseudorangeModulus,
                "l1Cn0" to l1Cn0,
                "l2Code" to l2Code,
                "l2MinusL1Pseudorange" to l2MinusL1Pseudorange,
                "l2MinusL1PhaseRange" to l2MinusL1PhaseRange,
                "l2LockTimeIndicator" to l2LockTimeIndicator,
                "l2Cn0" to l2Cn0
            )
        }

        return mapOf(
            "stationId" to stationId,
            "glonassEpochTime" to epochTime,
            "synchronous" to synchronous,
            "satelliteCount" to satelliteCount,
            "smoothingIndicator" to smoothingIndicator,
            "smoothingInterval" to smoothingInterval,
            "satellites" to satellites,
            "bitsRemaining" to bits.bitsRemaining()
        )
    }
}
