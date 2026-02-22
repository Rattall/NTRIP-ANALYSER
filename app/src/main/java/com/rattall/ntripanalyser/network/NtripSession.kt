package com.rattall.ntripanalyser.network

import java.io.Closeable
import java.io.InputStream
import java.net.Socket
import javax.net.ssl.SSLSocket

data class NtripSession(
    val socket: Socket,
    val inputStream: InputStream
) : Closeable {
    override fun close() {
        runCatching { inputStream.close() }
        runCatching {
            if (socket is SSLSocket) {
                socket.close()
            } else {
                socket.close()
            }
        }
    }
}
