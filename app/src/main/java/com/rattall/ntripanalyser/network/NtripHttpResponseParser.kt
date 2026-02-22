package com.rattall.ntripanalyser.network

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal object NtripHttpResponseParser {
    fun consumeHeadersOrThrow(inputStream: InputStream): String {
        val headerBytes = ByteArrayOutputStream()
        var matched = 0
        val delimiter = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())

        while (matched < delimiter.size) {
            val current = inputStream.read()
            if (current == -1) {
                throw IllegalStateException("No HTTP response body found from caster")
            }
            headerBytes.write(current)
            if (current.toByte() == delimiter[matched]) {
                matched += 1
            } else {
                matched = if (current.toByte() == delimiter[0]) 1 else 0
            }
        }

        val headerText = headerBytes.toString(Charsets.UTF_8.name())
        val statusLine = headerText.lineSequence().firstOrNull()?.trim().orEmpty()
        if (statusLine.isBlank()) {
            throw IllegalStateException("Missing HTTP status line from caster")
        }

        val accepted = statusLine.startsWith("ICY 200") ||
            statusLine.startsWith("HTTP/1.0 200") ||
            statusLine.startsWith("HTTP/1.1 200")

        if (!accepted) {
            throw IllegalStateException("Caster rejected request: $statusLine")
        }

        return statusLine
    }
}
