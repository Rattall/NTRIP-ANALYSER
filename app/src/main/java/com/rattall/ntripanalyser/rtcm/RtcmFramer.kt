package com.rattall.ntripanalyser.rtcm

class RtcmFramer {
    private val buffer = ArrayList<Byte>(8192)

    fun push(bytes: ByteArray, length: Int = bytes.size): List<RtcmFrame> {
        for (index in 0 until length) {
            buffer.add(bytes[index])
        }

        val frames = mutableListOf<RtcmFrame>()
        while (true) {
            val preambleIndex = buffer.indexOfFirst { it == 0xD3.toByte() }
            if (preambleIndex < 0) {
                buffer.clear()
                break
            }
            if (preambleIndex > 0) {
                repeat(preambleIndex) { buffer.removeAt(0) }
            }
            if (buffer.size < 3) {
                break
            }

            val lenHigh = buffer[1].toInt() and 0x03
            val lenLow = buffer[2].toInt() and 0xFF
            val payloadLength = (lenHigh shl 8) or lenLow
            val totalFrameLength = 3 + payloadLength + 3
            if (buffer.size < totalFrameLength) {
                break
            }

            val frameBytes = ByteArray(totalFrameLength)
            for (idx in 0 until totalFrameLength) {
                frameBytes[idx] = buffer[idx]
            }
            repeat(totalFrameLength) { buffer.removeAt(0) }

            val payload = frameBytes.copyOfRange(3, 3 + payloadLength)
            val expectedCrc = ((frameBytes[3 + payloadLength].toInt() and 0xFF) shl 16) or
                ((frameBytes[3 + payloadLength + 1].toInt() and 0xFF) shl 8) or
                (frameBytes[3 + payloadLength + 2].toInt() and 0xFF)
            val computedCrc = Crc24Q.calculate(frameBytes, 0, 3 + payloadLength)

            frames += RtcmFrame(
                rawFrame = frameBytes,
                payload = payload,
                crcValid = expectedCrc == computedCrc
            )
        }

        return frames
    }
}
