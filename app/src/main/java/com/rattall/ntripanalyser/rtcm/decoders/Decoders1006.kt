package com.rattall.ntripanalyser.rtcm.decoders

import com.rattall.ntripanalyser.rtcm.BitBuffer
import com.rattall.ntripanalyser.rtcm.RtcmDecoder

class Decoders1006 : RtcmDecoder {
    override fun decode(payload: ByteArray): Map<String, Any?> {
        val bits = BitBuffer(payload)
        bits.readUnsigned(12)
        val stationId = bits.readUnsigned(12).toInt()
        val itrFYear = bits.readUnsigned(6).toInt()
        val gpsIndicator = bits.readUnsigned(1).toInt() == 1
        val glonassIndicator = bits.readUnsigned(1).toInt() == 1
        val galileoIndicator = bits.readUnsigned(1).toInt() == 1
        bits.readUnsigned(1)
        val referenceStationIndicator = bits.readUnsigned(1).toInt() == 1
        val ecefX = bits.readSigned(38)
        bits.readUnsigned(1)
        val ecefY = bits.readSigned(38)
        bits.readUnsigned(2)
        val ecefZ = bits.readSigned(38)
        val antennaHeight = bits.readUnsigned(16)

        return mapOf(
            "stationId" to stationId,
            "itrfYear" to itrFYear,
            "gpsIndicator" to gpsIndicator,
            "glonassIndicator" to glonassIndicator,
            "galileoIndicator" to galileoIndicator,
            "referenceStationIndicator" to referenceStationIndicator,
            "ecefX_0_1mm" to ecefX,
            "ecefY_0_1mm" to ecefY,
            "ecefZ_0_1mm" to ecefZ,
            "antennaHeight_mm" to antennaHeight
        )
    }
}
