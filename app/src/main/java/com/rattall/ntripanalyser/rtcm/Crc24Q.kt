package com.rattall.ntripanalyser.rtcm

object Crc24Q {
    private const val POLY = 0x1864CFB

    fun calculate(buffer: ByteArray, offset: Int, length: Int): Int {
        var crc = 0
        for (index in offset until offset + length) {
            crc = crc xor ((buffer[index].toInt() and 0xFF) shl 16)
            repeat(8) {
                crc = crc shl 1
                if ((crc and 0x1000000) != 0) {
                    crc = crc xor POLY
                }
            }
        }
        return crc and 0xFFFFFF
    }
}
