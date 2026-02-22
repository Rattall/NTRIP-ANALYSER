package com.rattall.ntripanalyser.network

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NtripHttpResponseParserTests {

    @Test
    fun consumeHeaders_acceptsHttp200AndLeavesBody() {
        val body = "D3PAYLOAD".toByteArray()
        val response = (
            "HTTP/1.1 200 OK\r\n" +
                "Server: NTRIP\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray() + body

        val input = ByteArrayInputStream(response)
        val status = NtripHttpResponseParser.consumeHeadersOrThrow(input)

        assertEquals("HTTP/1.1 200 OK", status)
        val remaining = input.readBytes()
        assertTrue(remaining.contentEquals(body))
    }

    @Test
    fun consumeHeaders_acceptsIcy200() {
        val response = (
            "ICY 200 OK\r\n" +
                "Server: Caster\r\n\r\n"
            ).toByteArray()

        val input = ByteArrayInputStream(response)
        val status = NtripHttpResponseParser.consumeHeadersOrThrow(input)

        assertEquals("ICY 200 OK", status)
    }

    @Test
    fun consumeHeaders_rejectsUnauthorizedStatus() {
        val response = (
            "HTTP/1.1 401 Unauthorized\r\n" +
                "WWW-Authenticate: Basic\r\n\r\n"
            ).toByteArray()

        val input = ByteArrayInputStream(response)

        runCatching {
            NtripHttpResponseParser.consumeHeadersOrThrow(input)
        }.onSuccess {
            throw AssertionError("Expected unauthorized response to throw")
        }.onFailure { ex ->
            assertTrue(ex is IllegalStateException)
            assertTrue(ex.message?.contains("401") == true)
        }
    }
}
