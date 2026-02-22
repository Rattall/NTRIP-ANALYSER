package com.rattall.ntripanalyser.rtcm

class BitBuffer(private val payload: ByteArray) {
    private var bitIndex = 0

    fun readUnsigned(bitCount: Int): Long {
        require(bitCount in 1..64)
        var value = 0L
        repeat(bitCount) {
            value = (value shl 1) or readBit().toLong()
        }
        return value
    }

    fun readSigned(bitCount: Int): Long {
        val unsigned = readUnsigned(bitCount)
        val signBit = 1L shl (bitCount - 1)
        return if ((unsigned and signBit) == 0L) {
            unsigned
        } else {
            unsigned - (1L shl bitCount)
        }
    }

    fun bitsRemaining(): Int = (payload.size * 8) - bitIndex

    private fun readBit(): Int {
        if (bitIndex >= payload.size * 8) {
            throw IndexOutOfBoundsException("No bits remaining")
        }
        val byteIndex = bitIndex / 8
        val shift = 7 - (bitIndex % 8)
        bitIndex += 1
        return (payload[byteIndex].toInt() shr shift) and 0x01
    }
}
