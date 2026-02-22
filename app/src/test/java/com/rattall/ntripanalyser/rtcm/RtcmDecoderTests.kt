package com.rattall.ntripanalyser.rtcm

import com.rattall.ntripanalyser.rtcm.decoders.Decoders1004
import com.rattall.ntripanalyser.rtcm.decoders.Decoders1012
import com.rattall.ntripanalyser.rtcm.decoders.MsmDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RtcmDecoderTests {

    @Test
    fun decode1004_perSatelliteFields() {
        val builder = BitPayloadBuilder()
        builder.appendUnsigned(1004, 12)
        builder.appendUnsigned(42, 12)
        builder.appendUnsigned(500_000, 30)
        builder.appendUnsigned(1, 1)
        builder.appendUnsigned(1, 5)
        builder.appendUnsigned(1, 1)
        builder.appendUnsigned(4, 3)

        builder.appendUnsigned(12, 6)
        builder.appendUnsigned(1, 1)
        builder.appendUnsigned(1_000_000, 24)
        builder.appendSigned(-12345, 20)
        builder.appendUnsigned(55, 7)
        builder.appendUnsigned(200, 8)
        builder.appendUnsigned(160, 8)
        builder.appendUnsigned(2, 2)
        builder.appendSigned(-250, 14)
        builder.appendSigned(1234, 20)
        builder.appendUnsigned(33, 7)
        builder.appendUnsigned(145, 8)

        val payload = builder.toByteArray()
        val decoded = Decoders1004().decode(payload)

        assertEquals(42, decoded["stationId"])
        assertEquals(1, decoded["satelliteCount"])

        @Suppress("UNCHECKED_CAST")
        val satellites = decoded["satellites"] as List<Map<String, Any?>>
        assertEquals(1, satellites.size)
        assertEquals(12, satellites[0]["satelliteId"])
        assertEquals(1_000_000L, satellites[0]["l1Pseudorange"])
        assertEquals(-12345L, satellites[0]["l1PhaseRangeMinusPseudorange"])
        assertEquals(-250L, satellites[0]["l2MinusL1Pseudorange"])
        assertEquals(1234L, satellites[0]["l2MinusL1PhaseRange"])
    }

    @Test
    fun decode1012_perSatelliteFields() {
        val builder = BitPayloadBuilder()
        builder.appendUnsigned(1012, 12)
        builder.appendUnsigned(7, 12)
        builder.appendUnsigned(321_000, 27)
        builder.appendUnsigned(0, 1)
        builder.appendUnsigned(1, 5)
        builder.appendUnsigned(0, 1)
        builder.appendUnsigned(5, 3)

        builder.appendUnsigned(8, 6)
        builder.appendUnsigned(1, 1)
        builder.appendSigned(-3, 5)
        builder.appendUnsigned(2_000_000, 25)
        builder.appendSigned(54321, 20)
        builder.appendUnsigned(70, 7)
        builder.appendUnsigned(100, 7)
        builder.appendUnsigned(171, 8)
        builder.appendUnsigned(1, 2)
        builder.appendSigned(-321, 14)
        builder.appendSigned(-2345, 20)
        builder.appendUnsigned(44, 7)
        builder.appendUnsigned(110, 8)

        val payload = builder.toByteArray()
        val decoded = Decoders1012().decode(payload)

        assertEquals(7, decoded["stationId"])
        assertEquals(1, decoded["satelliteCount"])

        @Suppress("UNCHECKED_CAST")
        val satellites = decoded["satellites"] as List<Map<String, Any?>>
        assertEquals(1, satellites.size)
        assertEquals(8, satellites[0]["satelliteId"])
        assertEquals(-3, satellites[0]["frequencyChannelNumber"])
        assertEquals(2_000_000L, satellites[0]["l1Pseudorange"])
        assertEquals(54321L, satellites[0]["l1PhaseRangeMinusPseudorange"])
        assertEquals(-321L, satellites[0]["l2MinusL1Pseudorange"])
        assertEquals(-2345L, satellites[0]["l2MinusL1PhaseRange"])
    }

    @Test
    fun decodeMsm1074_masksAndCellData() {
        val builder = BitPayloadBuilder()
        builder.appendUnsigned(1074, 12)
        builder.appendUnsigned(99, 12)
        builder.appendUnsigned(123_456, 30)
        builder.appendUnsigned(0, 1)
        builder.appendUnsigned(2, 3)
        builder.appendUnsigned(0, 7)
        builder.appendUnsigned(1, 2)
        builder.appendUnsigned(0, 2)
        builder.appendUnsigned(1, 1)
        builder.appendUnsigned(3, 3)

        for (id in 1..64) {
            builder.appendUnsigned(if (id == 3) 1 else 0, 1)
        }
        for (id in 1..32) {
            builder.appendUnsigned(if (id == 2) 1 else 0, 1)
        }
        builder.appendUnsigned(1, 1)

        builder.appendUnsigned(55, 8)
        builder.appendUnsigned(777, 10)

        builder.appendSigned(1234, 15)
        builder.appendSigned(-4321, 22)
        builder.appendUnsigned(7, 4)
        builder.appendUnsigned(1, 1)
        builder.appendUnsigned(35, 6)

        val payload = builder.toByteArray()
        val decoded = MsmDecoder(1074).decode(payload)

        assertEquals(1074, decoded["messageType"])
        assertEquals(4, decoded["msmType"])

        @Suppress("UNCHECKED_CAST")
        val satIds = decoded["satelliteIds"] as List<Int>
        @Suppress("UNCHECKED_CAST")
        val sigIds = decoded["signalIds"] as List<Int>
        @Suppress("UNCHECKED_CAST")
        val satData = decoded["satelliteData"] as List<Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val cellData = decoded["cellData"] as List<Map<String, Any?>>

        assertEquals(listOf(3), satIds)
        assertEquals(listOf(2), sigIds)
        assertEquals(1, satData.size)
        assertEquals(55L, satData[0]["roughRangeMs"])
        assertEquals(777L, satData[0]["rangeModulo_1_1024ms"])
        assertEquals(1, cellData.size)
        assertEquals(3, cellData[0]["satelliteId"])
        assertEquals(2, cellData[0]["signalId"])
        assertEquals(1234L, cellData[0]["finePseudorange"])
        assertEquals(-4321L, cellData[0]["finePhaseRange"])
        assertEquals(7L, cellData[0]["lockTimeIndicator"])
        assertEquals(1L, cellData[0]["halfCycleAmbiguity"])
        assertEquals(35L, cellData[0]["cnr"])
        assertTrue((decoded["bitsRemaining"] as Int) >= 0)
    }
}

private class BitPayloadBuilder {
    private val bits = StringBuilder()

    fun appendUnsigned(value: Int, width: Int) {
        appendUnsigned(value.toLong(), width)
    }

    fun appendUnsigned(value: Long, width: Int) {
        require(width > 0)
        val masked = if (width == 64) value else value and ((1L shl width) - 1)
        for (bit in width - 1 downTo 0) {
            val set = ((masked ushr bit) and 1L) == 1L
            bits.append(if (set) '1' else '0')
        }
    }

    fun appendSigned(value: Int, width: Int) {
        appendSigned(value.toLong(), width)
    }

    fun appendSigned(value: Long, width: Int) {
        require(width in 1..63)
        val modulus = 1L shl width
        val encoded = if (value < 0) modulus + value else value
        appendUnsigned(encoded, width)
    }

    fun toByteArray(): ByteArray {
        while (bits.length % 8 != 0) {
            bits.append('0')
        }
        val output = ByteArray(bits.length / 8)
        for (index in output.indices) {
            val start = index * 8
            val byteString = bits.substring(start, start + 8)
            output[index] = byteString.toInt(2).toByte()
        }
        return output
    }
}