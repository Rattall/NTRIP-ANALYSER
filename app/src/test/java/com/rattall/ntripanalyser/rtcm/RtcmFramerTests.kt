package com.rattall.ntripanalyser.rtcm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RtcmFramerTests {

    @Test
    fun framer_parsesValidFrameWithCrc() {
        val payload = byteArrayOf(0x43, 0x20, 0x00, 0x01)
        val frame = buildRtcmFrame(payload)

        val framer = RtcmFramer()
        val decoded = framer.push(frame)

        assertEquals(1, decoded.size)
        assertTrue(decoded[0].crcValid)
        assertTrue(decoded[0].payload.contentEquals(payload))
    }

    @Test
    fun framer_marksInvalidCrcWhenTampered() {
        val payload = byteArrayOf(0x10, 0x20, 0x30, 0x40, 0x50)
        val frame = buildRtcmFrame(payload)
        frame[frame.size - 1] = (frame.last().toInt() xor 0x01).toByte()

        val framer = RtcmFramer()
        val decoded = framer.push(frame)

        assertEquals(1, decoded.size)
        assertFalse(decoded[0].crcValid)
        assertTrue(decoded[0].payload.contentEquals(payload))
    }

    @Test
    fun framer_reassemblesFragmentedInput() {
        val payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        val frame = buildRtcmFrame(payload)

        val framer = RtcmFramer()

        val first = framer.push(frame.copyOfRange(0, 2))
        val second = framer.push(frame.copyOfRange(2, 5))
        val third = framer.push(frame.copyOfRange(5, frame.size))

        assertTrue(first.isEmpty())
        assertTrue(second.isEmpty())
        assertEquals(1, third.size)
        assertTrue(third[0].crcValid)
        assertTrue(third[0].payload.contentEquals(payload))
    }

    @Test
    fun framer_parsesTwoFramesInSingleBuffer() {
        val payloadA = byteArrayOf(0x55, 0x66, 0x77)
        val payloadB = byteArrayOf(0x11, 0x22, 0x33, 0x44)
        val frameA = buildRtcmFrame(payloadA)
        val frameB = buildRtcmFrame(payloadB)
        val stream = frameA + frameB

        val framer = RtcmFramer()
        val decoded = framer.push(stream)

        assertEquals(2, decoded.size)
        assertTrue(decoded[0].crcValid)
        assertTrue(decoded[1].crcValid)
        assertTrue(decoded[0].payload.contentEquals(payloadA))
        assertTrue(decoded[1].payload.contentEquals(payloadB))
    }

    @Test
    fun crc24q_matchesFrameHeaderComputation() {
        val payload = byteArrayOf(0x01, 0x23, 0x45, 0x67)
        val frame = buildRtcmFrame(payload)

        val payloadLength = payload.size
        val expected =
            ((frame[3 + payloadLength].toInt() and 0xFF) shl 16) or
                ((frame[3 + payloadLength + 1].toInt() and 0xFF) shl 8) or
                (frame[3 + payloadLength + 2].toInt() and 0xFF)

        val computed = Crc24Q.calculate(frame, 0, 3 + payloadLength)
        assertEquals(expected, computed)
    }

    private fun buildRtcmFrame(payload: ByteArray): ByteArray {
        val length = payload.size
        val frame = ByteArray(3 + length + 3)
        frame[0] = 0xD3.toByte()
        frame[1] = ((length shr 8) and 0x03).toByte()
        frame[2] = (length and 0xFF).toByte()
        payload.copyInto(frame, destinationOffset = 3)

        val crc = Crc24Q.calculate(frame, 0, 3 + length)
        frame[3 + length] = ((crc shr 16) and 0xFF).toByte()
        frame[3 + length + 1] = ((crc shr 8) and 0xFF).toByte()
        frame[3 + length + 2] = (crc and 0xFF).toByte()

        return frame
    }
}
