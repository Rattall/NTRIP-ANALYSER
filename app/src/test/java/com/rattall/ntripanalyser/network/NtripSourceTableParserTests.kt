package com.rattall.ntripanalyser.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NtripSourceTableParserTests {

    @Test
    fun parse_extractsOnlyStrEntries() {
        val text = """
            SOURCETABLE 200 OK
            CAS;Caster;owner;ops;0;0;misc
            STR;MOUNT1;ID-1;RTCM 3.2;1004(1),1012(1);2;GPS+GLO;NET;AU; -37.814 ; 144.96332 ;0;0;GEN;none;B;N;0;
            NET;SomeNet;AU
            STR;MOUNT2;ID-2;RTCM 3.3;1074(1);1;GPS;NET;NZ;;;0;0;GEN;none;B;N;0;
            ENDSOURCETABLE
        """.trimIndent()

        val entries = NtripSourceTableParser().parse(text)

        assertEquals(2, entries.size)
        assertEquals("MOUNT1", entries[0].mountpoint)
        assertEquals("ID-1", entries[0].identifier)
        assertEquals("RTCM 3.2", entries[0].format)
        assertEquals("GPS+GLO", entries[0].navSystem)
        assertEquals("AU", entries[0].country)
        assertEquals(-37.814, entries[0].latitude)
        assertEquals(144.96332, entries[0].longitude)

        assertEquals("MOUNT2", entries[1].mountpoint)
        assertNull(entries[1].latitude)
        assertNull(entries[1].longitude)
    }

    @Test
    fun parse_ignoresMalformedStrLines() {
        val text = """
            STR;TOO_SHORT;ONLY_THREE_FIELDS
            STR;VALID;ID;RTCM 3.3;1074(1);1;GPS;NET;US;37.0;-122.0;0;0;GEN;none;B;N;0;
        """.trimIndent()

        val entries = NtripSourceTableParser().parse(text)

        assertEquals(1, entries.size)
        assertEquals("VALID", entries[0].mountpoint)
    }
}
