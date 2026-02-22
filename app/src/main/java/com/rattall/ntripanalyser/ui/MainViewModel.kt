package com.rattall.ntripanalyser.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rattall.ntripanalyser.data.CredentialStore
import com.rattall.ntripanalyser.domain.StreamController
import com.rattall.ntripanalyser.model.ConnectionEvent
import com.rattall.ntripanalyser.model.NtripConfig
import com.rattall.ntripanalyser.model.NtripProtocol
import com.rattall.ntripanalyser.model.SourceTableEntry
import com.rattall.ntripanalyser.model.StreamStats
import com.rattall.ntripanalyser.network.NtripClient
import com.rattall.ntripanalyser.rtcm.RtcmDecodedMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RtcmTypeSummary(
    val messageType: Int,
    val lastReceivedAtMs: Long,
    val payloadLength: Int,
    val satelliteCount: Int?,
    val stationId: Int?,
    val count: Long,
    val lastFields: Map<String, Any?>
)

data class MainUiState(
    val host: String = "",
    val port: String = "2101",
    val username: String = "",
    val password: String = "",
    val useTls: Boolean = true,
    val protocol: NtripProtocol = NtripProtocol.REV2,
    val mountpoint: String = "",
    val sourceTable: List<SourceTableEntry> = emptyList(),
    val status: String = "Idle",
    val recentMessages: List<RtcmDecodedMessage> = emptyList(),
    val connectionEvents: List<ConnectionEvent> = emptyList(),
    val messageSummaries: List<RtcmTypeSummary> = emptyList()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val credentialStore = CredentialStore(app)
    private val ntripClient = NtripClient()
    private val streamController = StreamController(ntripClient = ntripClient)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private val summaryByType = mutableMapOf<Int, RtcmTypeSummary>()

    val stats: StateFlow<StreamStats> = streamController.stats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StreamStats()
    )

    init {
        credentialStore.load()?.let { cfg ->
            _uiState.value = _uiState.value.copy(
                host = cfg.host,
                port = cfg.port.toString(),
                username = cfg.username,
                password = cfg.password,
                useTls = cfg.useTls,
                protocol = cfg.protocol,
                mountpoint = cfg.mountpoint
            )
        }

        viewModelScope.launch {
            streamController.messages.collect { decoded ->
                val current = _uiState.value.recentMessages
                val next = listOf(decoded) + current

                val previous = summaryByType[decoded.messageType]
                val updated = RtcmTypeSummary(
                    messageType = decoded.messageType,
                    lastReceivedAtMs = decoded.receivedAtMs,
                    payloadLength = decoded.payloadLength,
                    satelliteCount = extractSatelliteCount(decoded),
                    stationId = extractStationId(decoded),
                    count = (previous?.count ?: 0L) + 1,
                    lastFields = decoded.fields
                )
                summaryByType[decoded.messageType] = updated

                _uiState.value = _uiState.value.copy(
                    recentMessages = next.take(200),
                    messageSummaries = summaryByType.values.sortedBy { it.messageType }
                )
            }
        }

        viewModelScope.launch {
            streamController.stats.collect { streamStats ->
                _uiState.value = _uiState.value.copy(status = streamStats.statusMessage)
            }
        }

        viewModelScope.launch {
            streamController.events.collect { event ->
                val current = _uiState.value.connectionEvents
                val next = listOf(event) + current
                _uiState.value = _uiState.value.copy(connectionEvents = next.take(50))
            }
        }
    }

    fun setHost(value: String) {
        _uiState.value = _uiState.value.copy(host = value)
    }

    fun setPort(value: String) {
        _uiState.value = _uiState.value.copy(port = value.filter { it.isDigit() })
    }

    fun setUsername(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun setPassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun setUseTls(value: Boolean) {
        _uiState.value = _uiState.value.copy(useTls = value)
    }

    fun setProtocol(protocol: NtripProtocol) {
        _uiState.value = _uiState.value.copy(protocol = protocol)
    }

    fun selectMountpoint(mountpoint: String) {
        _uiState.value = _uiState.value.copy(mountpoint = mountpoint)
    }

    fun fetchSourceTable() {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.value = current.copy(status = "Loading source table...")
            runCatching {
                ntripClient.fetchSourceTable(
                    host = current.host,
                    port = current.port.toIntOrNull() ?: 2101,
                    useTls = current.useTls,
                    username = current.username,
                    password = current.password
                )
            }.onSuccess { entries ->
                _uiState.value = _uiState.value.copy(
                    sourceTable = entries,
                    status = "Loaded ${entries.size} mountpoints"
                )
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    status = "Source table error: ${ex.message ?: "unknown"}"
                )
            }
        }
    }

    fun connect() {
        val state = _uiState.value
        if (state.host.isBlank()) {
            _uiState.value = state.copy(status = "Host is required")
            return
        }

        val effectiveMountpoint = when {
            state.mountpoint.isNotBlank() -> state.mountpoint
            state.sourceTable.isNotEmpty() -> state.sourceTable.first().mountpoint
            else -> ""
        }

        if (effectiveMountpoint.isBlank()) {
            _uiState.value = state.copy(status = "Select a mountpoint before connecting")
            return
        }

        if (effectiveMountpoint != state.mountpoint) {
            _uiState.value = state.copy(mountpoint = effectiveMountpoint)
        }

        val cfg = NtripConfig(
            host = state.host,
            port = state.port.toIntOrNull() ?: 2101,
            username = state.username,
            password = state.password,
            mountpoint = effectiveMountpoint,
            useTls = state.useTls,
            protocol = state.protocol
        )
        credentialStore.save(cfg)
        streamController.start(cfg)
    }

    fun disconnect() {
        streamController.stop()
    }

    private fun extractStationId(message: RtcmDecodedMessage): Int? {
        val raw = message.fields["stationId"] ?: return null
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
    }

    private fun extractSatelliteCount(message: RtcmDecodedMessage): Int? {
        val directCount = message.fields["satelliteCount"]
        if (directCount is Number) {
            return directCount.toInt()
        }
        val satelliteIds = message.fields["satelliteIds"]
        if (satelliteIds is List<*>) {
            return satelliteIds.size
        }
        val satellites = message.fields["satellites"]
        if (satellites is List<*>) {
            return satellites.size
        }
        return null
    }
}
