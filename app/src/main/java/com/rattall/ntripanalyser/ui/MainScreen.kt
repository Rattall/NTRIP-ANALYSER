package com.rattall.ntripanalyser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rattall.ntripanalyser.model.NtripProtocol
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val expandedByType = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("NTRIP RTCM3 Analyser", style = MaterialTheme.typography.titleLarge)
                        Text(ui.status, style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionCard(title = "Connection") {
                    OutlinedTextField(
                        value = ui.host,
                        onValueChange = viewModel::setHost,
                        label = { Text("Caster Host") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ui.port,
                        onValueChange = viewModel::setPort,
                        label = { Text("Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ui.username,
                        onValueChange = viewModel::setUsername,
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ui.password,
                        onValueChange = viewModel::setPassword,
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Use TLS", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(checked = ui.useTls, onCheckedChange = viewModel::setUseTls)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = ui.protocol == NtripProtocol.REV1,
                            onClick = { viewModel.setProtocol(NtripProtocol.REV1) },
                            label = { Text("NTRIP Rev1") }
                        )
                        FilterChip(
                            selected = ui.protocol == NtripProtocol.REV2,
                            onClick = { viewModel.setProtocol(NtripProtocol.REV2) },
                            label = { Text("NTRIP Rev2") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(modifier = Modifier.weight(1f), onClick = viewModel::fetchSourceTable) {
                            Text("Load Mountpoints")
                        }
                        Button(modifier = Modifier.weight(1f), onClick = viewModel::connect) {
                            Text("Connect")
                        }
                        Button(modifier = Modifier.weight(1f), onClick = viewModel::disconnect) {
                            Text("Stop")
                        }
                    }
                }
            }

            if (ui.sourceTable.isNotEmpty()) {
                item {
                    SectionCard(title = "Mountpoints") {
                        Text(
                            "Selected: ${ui.mountpoint.ifBlank { "(none)" }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        ui.sourceTable.take(20).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.mountpoint, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${entry.format} • ${entry.navSystem}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.selectMountpoint(entry.mountpoint) }) {
                                    Text("Select")
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Live Stats") {
                    StatRow("Connected", if (stats.connected) "Yes" else "No")
                    StatRow("Bytes / sec", "${"%.1f".format(stats.bytesPerSecond)}")
                    StatRow("Total Bytes", "${stats.totalBytes}")
                    StatRow("Messages / sec", "${"%.1f".format(stats.messagesPerSecond)}")
                    StatRow("Total Messages", "${stats.totalMessages}")
                    StatRow("CRC Failures", "${stats.crcFailures}")
                    StatRow("Malformed Frames", "${stats.malformedFrames}")
                    StatRow("Reconnect Attempts", "${stats.reconnectAttempts}")
                    StatRow("Uptime", "${stats.sessionUptimeMs / 1000}s")
                }
            }

            if (ui.connectionEvents.isNotEmpty()) {
                item {
                    SectionCard(title = "Connection Events") {
                        ui.connectionEvents.take(10).forEach { event ->
                            Text(event.message, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            item {
                SectionCard(title = "RTCM Message Types") {
                    if (ui.messageSummaries.isEmpty()) {
                        Text(
                            "No RTCM message types received yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        MessageSummaryHeader()
                        ui.messageSummaries.forEach { summary ->
                            val expanded = expandedByType[summary.messageType] == true
                            MessageSummaryRow(
                                summary = summary,
                                expanded = expanded,
                                onToggle = {
                                    expandedByType[summary.messageType] = !expanded
                                }
                            )
                            if (expanded) {
                                MessageDetailBlock(summary = summary)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                content()
            }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MessageSummaryHeader() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Type", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Last", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Size", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Sats", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Station", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Count", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MessageSummaryRow(
    summary: RtcmTypeSummary,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        tonalElevation = if (expanded) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(summary.messageType.toString(), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(rememberTimeFormatter().format(Date(summary.lastReceivedAtMs)), modifier = Modifier.weight(2f))
            Text(summary.payloadLength.toString(), modifier = Modifier.weight(1f))
            Text(summary.satelliteCount?.toString() ?: "-", modifier = Modifier.weight(1f))
            Text(summary.stationId?.toString() ?: "-", modifier = Modifier.weight(1f))
            Text(summary.count.toString(), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MessageDetailBlock(summary: RtcmTypeSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        summary.lastFields
            .toSortedMap()
            .forEach { (key, value) ->
                val listOfMaps = value.asListOfMaps()
                if (listOfMaps != null) {
                    Text(key, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    DetailDataTable(rows = listOfMaps)
                } else {
                    val rendered = renderFieldValue(value)
                    Text("$key: $rendered", style = MaterialTheme.typography.bodySmall)
                }
            }
    }
}

@Composable
private fun DetailDataTable(rows: List<Map<String, Any?>>) {
    if (rows.isEmpty()) {
        Text("(empty)", style = MaterialTheme.typography.bodySmall)
        return
    }

    val preferred = listOf(
        "satelliteId",
        "signalId",
        "index",
        "roughRangeMs",
        "rangeModulo_1_1024ms",
        "finePseudorange",
        "finePhaseRange",
        "cnr",
        "lockTimeIndicator"
    )

    val allColumns = rows
        .flatMap { it.keys }
        .distinct()
        .sortedWith(compareBy<String>({ preferred.indexOf(it).let { idx -> if (idx == -1) Int.MAX_VALUE else idx } }, { it }))

    val scroll = rememberScrollState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allColumns.forEach { column ->
                    Text(
                        text = column,
                        modifier = Modifier.width(120.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider()
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allColumns.forEach { column ->
                        Text(
                            text = renderNestedValue(row[column]),
                            modifier = Modifier.width(120.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun Any?.asListOfMaps(): List<Map<String, Any?>>? {
    if (this !is List<*>) {
        return null
    }
    if (this.isEmpty()) {
        return emptyList()
    }

    val converted = mutableListOf<Map<String, Any?>>()
    for (item in this) {
        if (item !is Map<*, *>) {
            return null
        }
        converted += item.entries.associate { (k, v) -> k.toString() to v }
    }
    return converted
}

private fun renderFieldValue(value: Any?): String {
    return when (value) {
        null -> "null"
        is List<*> -> {
            if (value.isEmpty()) {
                "[]"
            } else {
                buildString {
                    append("list(size=${value.size})")
                    value.forEachIndexed { index, item ->
                        append("\n  [")
                        append(index)
                        append("] ")
                        append(renderNestedValue(item))
                    }
                }
            }
        }
        is Map<*, *> -> {
            if (value.isEmpty()) {
                "{}"
            } else {
                buildString {
                    append("map(size=${value.size})")
                    value.entries
                        .sortedBy { it.key?.toString().orEmpty() }
                        .forEach { (key, item) ->
                            append("\n  ")
                            append(key)
                            append("=")
                            append(renderNestedValue(item))
                        }
                }
            }
        }
        else -> value.toString()
    }
}

private fun renderNestedValue(value: Any?): String {
    return when (value) {
        null -> "null"
        is List<*> -> {
            if (value.isEmpty()) "[]" else "[${value.joinToString(", ") { renderNestedValue(it) }}]"
        }
        is Map<*, *> -> {
            if (value.isEmpty()) "{}" else value.entries
                .sortedBy { it.key?.toString().orEmpty() }
                .joinToString(prefix = "{", postfix = "}") { (k, v) -> "${k}=${renderNestedValue(v)}" }
        }
        else -> value.toString()
    }
}

@Composable
private fun rememberTimeFormatter(): SimpleDateFormat {
    return androidx.compose.runtime.remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }
}
