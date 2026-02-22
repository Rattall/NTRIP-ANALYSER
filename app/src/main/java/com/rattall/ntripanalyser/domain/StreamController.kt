package com.rattall.ntripanalyser.domain

import com.rattall.ntripanalyser.model.NtripConfig
import com.rattall.ntripanalyser.model.ConnectionEvent
import com.rattall.ntripanalyser.model.StreamStats
import com.rattall.ntripanalyser.network.NtripClient
import com.rattall.ntripanalyser.rtcm.RtcmDecodedMessage
import com.rattall.ntripanalyser.rtcm.RtcmDecoderRegistry
import com.rattall.ntripanalyser.rtcm.RtcmFramer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StreamController(
    private val ntripClient: NtripClient = NtripClient(),
    private val decoderRegistry: RtcmDecoderRegistry = RtcmDecoderRegistry(),
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy()
) {
    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var streamJob: Job? = null

    private val _stats = MutableStateFlow(StreamStats())
    val stats: StateFlow<StreamStats> = _stats.asStateFlow()

    private val _messages = MutableSharedFlow<RtcmDecodedMessage>(extraBufferCapacity = 512)
    val messages: SharedFlow<RtcmDecodedMessage> = _messages.asSharedFlow()

    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<ConnectionEvent> = _events.asSharedFlow()

    fun start(config: NtripConfig) {
        stop()
        _stats.value = StreamStats(connected = false, statusMessage = "Connecting...")
        streamJob = scope.launch {
            val startAt = System.currentTimeMillis()
            var totalBytes = 0L
            var totalMessages = 0L
            var crcFailures = 0L
            var reconnectAttempts = 0
            var lastError: String? = null

            while (isActive) {
                val connectionStatus = if (reconnectAttempts == 0) {
                    "Connecting..."
                } else {
                    "Reconnecting... attempt $reconnectAttempts/${reconnectPolicy.maxAttempts}"
                }
                _events.tryEmit(ConnectionEvent(message = connectionStatus))
                withContext(Dispatchers.Main) {
                    _stats.value = _stats.value.copy(
                        connected = false,
                        statusMessage = connectionStatus,
                        reconnectAttempts = reconnectAttempts,
                        totalBytes = totalBytes,
                        totalMessages = totalMessages,
                        crcFailures = crcFailures,
                        sessionUptimeMs = System.currentTimeMillis() - startAt
                    )
                }

                val streamResult = runCatching {
                    ntripClient.openStream(config).use { session ->
                        val framer = RtcmFramer()
                        val readBuffer = ByteArray(4096)
                        var windowBytes = 0L
                        var windowMessages = 0L
                        var windowStart = System.currentTimeMillis()

                        withContext(Dispatchers.Main) {
                            _stats.value = _stats.value.copy(
                                connected = true,
                                statusMessage = "Connected",
                                reconnectAttempts = reconnectAttempts
                            )
                        }
                        _events.tryEmit(ConnectionEvent(message = "Connected"))

                        while (isActive) {
                            val count = session.inputStream.read(readBuffer)
                            if (count <= 0) {
                                throw IllegalStateException("Stream ended")
                            }
                            totalBytes += count
                            windowBytes += count

                            val frames = framer.push(readBuffer, count)
                            val now = System.currentTimeMillis()
                            for (frame in frames) {
                                if (!frame.crcValid) {
                                    crcFailures += 1
                                }
                                val decoded = decoderRegistry.decode(frame)
                                totalMessages += 1
                                windowMessages += 1
                                _messages.tryEmit(decoded)
                            }

                            if (now - windowStart >= 1000) {
                                val elapsed = (now - windowStart).coerceAtLeast(1)
                                val bps = (windowBytes * 1000.0) / elapsed
                                val mps = (windowMessages * 1000.0) / elapsed
                                val status = if (totalBytes > 0 && totalMessages == 0L) {
                                    "Connected: no RTCM frames yet (check mountpoint/GGA requirement)"
                                } else {
                                    "Streaming"
                                }

                                withContext(Dispatchers.Main) {
                                    _stats.value = _stats.value.copy(
                                        connected = true,
                                        statusMessage = status,
                                        totalBytes = totalBytes,
                                        bytesPerSecond = bps,
                                        totalMessages = totalMessages,
                                        messagesPerSecond = mps,
                                        crcFailures = crcFailures,
                                        reconnectAttempts = reconnectAttempts,
                                        sessionUptimeMs = now - startAt,
                                        lastMessageTimestampMs = now
                                    )
                                }
                                windowStart = now
                                windowBytes = 0
                                windowMessages = 0
                            }
                        }
                    }
                }

                if (streamResult.isSuccess || !isActive) {
                    break
                }

                lastError = streamResult.exceptionOrNull()?.message ?: "unknown"
                if (reconnectAttempts >= reconnectPolicy.maxAttempts) {
                    break
                }

                reconnectAttempts += 1
                val delayMs = reconnectPolicy.delayForAttempt(reconnectAttempts)
                withContext(Dispatchers.Main) {
                    _stats.value = _stats.value.copy(
                        connected = false,
                        statusMessage = "Retrying in ${delayMs}ms: $lastError",
                        reconnectAttempts = reconnectAttempts,
                        totalBytes = totalBytes,
                        totalMessages = totalMessages,
                        crcFailures = crcFailures,
                        sessionUptimeMs = System.currentTimeMillis() - startAt
                    )
                }
                _events.tryEmit(ConnectionEvent(message = "Retrying in ${delayMs}ms: $lastError"))
                delay(delayMs)
            }

            val finalStatus = if (lastError != null && reconnectAttempts >= reconnectPolicy.maxAttempts) {
                "Disconnected after retries: $lastError"
            } else {
                "Disconnected"
            }
            withContext(Dispatchers.Main) {
                _stats.value = _stats.value.copy(
                    connected = false,
                    statusMessage = finalStatus,
                    reconnectAttempts = reconnectAttempts,
                    totalBytes = totalBytes,
                    totalMessages = totalMessages,
                    crcFailures = crcFailures,
                    bytesPerSecond = 0.0,
                    messagesPerSecond = 0.0,
                    sessionUptimeMs = System.currentTimeMillis() - startAt
                )
            }
            _events.tryEmit(ConnectionEvent(message = finalStatus))
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        _stats.value = _stats.value.copy(connected = false, statusMessage = "Stopped")
        _events.tryEmit(ConnectionEvent(message = "Stopped by user"))
    }
}
