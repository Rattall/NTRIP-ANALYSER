package com.rattall.ntripanalyser.network

import com.rattall.ntripanalyser.model.SourceTableEntry

class NtripSourceTableParser {
    fun parse(sourceTableText: String): List<SourceTableEntry> {
        return sourceTableText
            .lineSequence()
            .filter { it.startsWith("STR;") }
            .mapNotNull { line ->
                val parts = line.split(';')
                if (parts.size < 10) {
                    null
                } else {
                    SourceTableEntry(
                        mountpoint = parts[1],
                        identifier = parts[2],
                        format = parts[3],
                        navSystem = parts[6],
                        country = parts[8],
                        latitude = parts.getOrNull(9)?.toDoubleOrNull(),
                        longitude = parts.getOrNull(10)?.toDoubleOrNull()
                    )
                }
            }
            .toList()
    }
}
