package com.rattall.ntripanalyser.network

import com.rattall.ntripanalyser.model.NtripConfig
import com.rattall.ntripanalyser.model.NtripProtocol
import com.rattall.ntripanalyser.model.SourceTableEntry
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.util.Base64
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NtripClient(
    private val sourceTableParser: NtripSourceTableParser = NtripSourceTableParser()
) {
    suspend fun fetchSourceTable(
        host: String,
        port: Int,
        useTls: Boolean,
        username: String,
        password: String
    ): List<SourceTableEntry> = withContext(Dispatchers.IO) {
        val socket = createSocket(host = host, port = port, useTls = useTls)
        socket.use { activeSocket ->
            val output = activeSocket.getOutputStream()
            val auth = basicAuth(username = username, password = password)
            val request = buildString {
                append("GET / HTTP/1.1\r\n")
                append("Host: $host\r\n")
                append("User-Agent: NTRIP NTRIP-ANALYSER/0.1\r\n")
                append("Ntrip-Version: Ntrip/2.0\r\n")
                append("Authorization: Basic $auth\r\n")
                append("Connection: close\r\n\r\n")
            }
            output.write(request.toByteArray())
            output.flush()

            val response = BufferedReader(InputStreamReader(activeSocket.getInputStream()))
            val text = buildString {
                while (true) {
                    val line = response.readLine() ?: break
                    appendLine(line)
                }
            }
            sourceTableParser.parse(text)
        }
    }

    suspend fun openStream(config: NtripConfig): NtripSession = withContext(Dispatchers.IO) {
        val socket = createSocket(host = config.host, port = config.port, useTls = config.useTls)
        val output = socket.getOutputStream()
        val auth = basicAuth(username = config.username, password = config.password)
        val mountpoint = config.mountpoint.trim().trimStart('/')
        val ntripVersionLine = if (config.protocol == NtripProtocol.REV2) {
            "Ntrip-Version: Ntrip/2.0\r\n"
        } else {
            ""
        }
        val httpVersion = if (config.protocol == NtripProtocol.REV2) "HTTP/1.1" else "HTTP/1.0"

        val request = buildString {
            append("GET /$mountpoint $httpVersion\r\n")
            append("Host: ${config.host}\r\n")
            append("User-Agent: NTRIP NTRIP-ANALYSER/0.1\r\n")
            append(ntripVersionLine)
            append("Authorization: Basic $auth\r\n")
            append("Accept: */*\r\n")
            append("Connection: keep-alive\r\n\r\n")
        }
        output.write(request.toByteArray())
        output.flush()

        val input = socket.getInputStream()
        NtripHttpResponseParser.consumeHeadersOrThrow(input)
        NtripSession(socket = socket, inputStream = input)
    }

    private fun createSocket(host: String, port: Int, useTls: Boolean): Socket {
        return if (useTls) {
            SSLSocketFactory.getDefault().createSocket(host, port) as Socket
        } else {
            Socket(host, port)
        }
    }

    private fun basicAuth(username: String, password: String): String {
        val raw = "$username:$password"
        return Base64.getEncoder().encodeToString(raw.toByteArray())
    }
}
