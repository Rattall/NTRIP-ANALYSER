package com.rattall.ntripanalyser.rtcm.decoders

import com.rattall.ntripanalyser.rtcm.BitBuffer
import com.rattall.ntripanalyser.rtcm.RtcmDecoder

class MsmDecoder(private val messageType: Int) : RtcmDecoder {
    override fun decode(payload: ByteArray): Map<String, Any?> {
        val bits = BitBuffer(payload)
        bits.readUnsigned(12)
        val stationId = bits.readUnsigned(12).toInt()
        val epochTime = bits.readUnsigned(30)
        val multipleMessage = bits.readUnsigned(1).toInt() == 1
        val issueOfDataStation = bits.readUnsigned(3).toInt()
        val reserved = bits.readUnsigned(7).toInt()
        val clockSteering = bits.readUnsigned(2).toInt()
        val externalClock = bits.readUnsigned(2).toInt()
        val smoothingIndicator = bits.readUnsigned(1).toInt() == 1
        val smoothingInterval = bits.readUnsigned(3).toInt()

        val msmType = messageType % 10
        val satelliteIds = readMaskIds(bits = bits, size = 64, baseIndex = 1)
        val signalIds = readMaskIds(bits = bits, size = 32, baseIndex = 1)

        val cellMaskSize = satelliteIds.size * signalIds.size
        val cellMaskBits = mutableListOf<Boolean>()
        repeat(cellMaskSize) {
            cellMaskBits += bits.readUnsigned(1).toInt() == 1
        }

        val activeCells = buildActiveCells(
            satelliteIds = satelliteIds,
            signalIds = signalIds,
            cellMaskBits = cellMaskBits
        )

        val satelliteData = decodeSatelliteData(bits = bits, satelliteIds = satelliteIds, msmType = msmType)
        val cellData = decodeCellData(bits = bits, activeCells = activeCells, msmType = msmType)

        return mapOf(
            "messageType" to messageType,
            "msmType" to msmType,
            "stationId" to stationId,
            "epochTime" to epochTime,
            "multipleMessage" to multipleMessage,
            "issueOfDataStation" to issueOfDataStation,
            "reserved" to reserved,
            "clockSteering" to clockSteering,
            "externalClockIndicator" to externalClock,
            "smoothingIndicator" to smoothingIndicator,
            "smoothingInterval" to smoothingInterval,
            "satelliteIds" to satelliteIds,
            "signalIds" to signalIds,
            "activeCellCount" to activeCells.size,
            "satelliteData" to satelliteData,
            "cellData" to cellData,
            "bitsRemaining" to bits.bitsRemaining()
        )
    }

    private fun readMaskIds(bits: BitBuffer, size: Int, baseIndex: Int): List<Int> {
        val active = mutableListOf<Int>()
        for (index in 0 until size) {
            val flag = bits.readUnsigned(1).toInt()
            if (flag == 1) {
                active += baseIndex + index
            }
        }
        return active
    }

    private fun buildActiveCells(
        satelliteIds: List<Int>,
        signalIds: List<Int>,
        cellMaskBits: List<Boolean>
    ): List<Pair<Int, Int>> {
        val active = mutableListOf<Pair<Int, Int>>()
        var maskIndex = 0
        for (satelliteId in satelliteIds) {
            for (signalId in signalIds) {
                if (cellMaskBits[maskIndex]) {
                    active += satelliteId to signalId
                }
                maskIndex += 1
            }
        }
        return active
    }

    private fun decodeSatelliteData(bits: BitBuffer, satelliteIds: List<Int>, msmType: Int): List<Map<String, Any?>> {
        val includeRoughRange = msmType >= 4
        val includeExtendedInfo = msmType == 5 || msmType == 7
        val includeRoughRate = msmType == 5 || msmType == 7
        val includeRangeModulo = msmType in 1..7

        val data = mutableListOf<Map<String, Any?>>()
        val roughRanges = readUnsignedList(bits, satelliteIds.size, if (includeRoughRange) 8 else 0)
        val extendedInfo = readUnsignedList(bits, satelliteIds.size, if (includeExtendedInfo) 4 else 0)
        val rangeModulo = readUnsignedList(bits, satelliteIds.size, if (includeRangeModulo) 10 else 0)
        val roughRates = readSignedList(bits, satelliteIds.size, if (includeRoughRate) 14 else 0)

        for (index in satelliteIds.indices) {
            data += mapOf(
                "satelliteId" to satelliteIds[index],
                "roughRangeMs" to roughRanges.getOrNull(index),
                "extendedInfo" to extendedInfo.getOrNull(index),
                "rangeModulo_1_1024ms" to rangeModulo.getOrNull(index),
                "roughPhaseRangeRate" to roughRates.getOrNull(index)
            )
        }
        return data
    }

    private fun decodeCellData(
        bits: BitBuffer,
        activeCells: List<Pair<Int, Int>>,
        msmType: Int
    ): List<Map<String, Any?>> {
        val pseudoBits = when (msmType) {
            1, 3, 4, 5 -> 15
            6, 7 -> 20
            else -> 0
        }
        val phaseBits = when (msmType) {
            2, 3, 4, 5 -> 22
            6, 7 -> 24
            else -> 0
        }
        val lockBits = when (msmType) {
            2, 3, 4, 5 -> 4
            6, 7 -> 10
            else -> 0
        }
        val halfCycleBits = when (msmType) {
            2, 3, 4, 5, 6, 7 -> 1
            else -> 0
        }
        val cnrBits = when (msmType) {
            4, 5 -> 6
            6, 7 -> 10
            else -> 0
        }
        val fineRateBits = when (msmType) {
            5, 7 -> 15
            else -> 0
        }

        val pseudoranges = readSignedList(bits, activeCells.size, pseudoBits)
        val phases = readSignedList(bits, activeCells.size, phaseBits)
        val locks = readUnsignedList(bits, activeCells.size, lockBits)
        val halfCycleFlags = readUnsignedList(bits, activeCells.size, halfCycleBits)
        val cnrs = readUnsignedList(bits, activeCells.size, cnrBits)
        val fineRates = readSignedList(bits, activeCells.size, fineRateBits)

        val cells = mutableListOf<Map<String, Any?>>()
        for (index in activeCells.indices) {
            val cell = activeCells[index]
            cells += mapOf(
                "satelliteId" to cell.first,
                "signalId" to cell.second,
                "finePseudorange" to pseudoranges.getOrNull(index),
                "finePhaseRange" to phases.getOrNull(index),
                "lockTimeIndicator" to locks.getOrNull(index),
                "halfCycleAmbiguity" to halfCycleFlags.getOrNull(index),
                "cnr" to cnrs.getOrNull(index),
                "finePhaseRangeRate" to fineRates.getOrNull(index)
            )
        }
        return cells
    }

    private fun readUnsignedList(bits: BitBuffer, count: Int, bitWidth: Int): List<Long?> {
        if (bitWidth == 0 || count == 0) {
            return List(count) { null }
        }
        val values = mutableListOf<Long?>()
        repeat(count) {
            values += if (bits.bitsRemaining() >= bitWidth) bits.readUnsigned(bitWidth) else null
        }
        return values
    }

    private fun readSignedList(bits: BitBuffer, count: Int, bitWidth: Int): List<Long?> {
        if (bitWidth == 0 || count == 0) {
            return List(count) { null }
        }
        val values = mutableListOf<Long?>()
        repeat(count) {
            values += if (bits.bitsRemaining() >= bitWidth) bits.readSigned(bitWidth) else null
        }
        return values
    }
}
