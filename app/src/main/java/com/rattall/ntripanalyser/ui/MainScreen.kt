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
    val hiddenKeys = setOf("messageType", "stationId", "satelliteCount")

    val tableFields = summary.lastFields
        .toSortedMap()
        .mapNotNull { (key, value) ->
            val rows = value.asListOfMaps() ?: return@mapNotNull null
            key to rows
        }

    val scalarFields = summary.lastFields
        .toSortedMap()
        .filter { (key, value) -> key !in hiddenKeys && value.asListOfMaps() == null }

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
        verticalArrangement = Arrangement.spacedBy(8.dp)
import androidx.compose.material3.HorizontalDivider
        scalarFields.forEach { (key, value) ->
            StatRow(label = key, value = renderFieldValue(value))
        }

        tableFields.forEach { (key, rows) ->
            Text(
                text = tableTitle(key),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            val discoveredColumns = rows.flatMap { it.keys }.distinct()
            val columns = orderedColumnsForField(
                messageType = summary.messageType,
                fieldKey = key,
                discoveredColumns = discoveredColumns
            )
            DetailDataTable(rows = rows, columns = columns)
        }

        if (scalarFields.isEmpty() && tableFields.isEmpty()) {
            Text("No parsed detail fields", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DetailDataTable(rows: List<Map<String, Any?>>, columns: List<String>) {
    if (rows.isEmpty()) {
        Text("(empty)", style = MaterialTheme.typography.bodySmall)
        return
    }

    val allColumns = if (columns.isEmpty()) {
        rows.flatMap { it.keys }.distinct().sorted()
    } else {
        columns
    }

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rattall.ntripanalyser.model.NtripProtocol
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
    val hiddenKeys = setOf(
        "messageType",
        "stationId",
        "satelliteCount"
    )

    val tableFields = summary.lastFields
        .toSortedMap()
        .mapNotNull { (key, value) ->
            val rows = value.asListOfMaps() ?: return@mapNotNull null
            key to rows
        }

    val scalarFields = summary.lastFields
        .toSortedMap()
        .filter { (key, value) ->
            key !in hiddenKeys && value.asListOfMaps() == null
        }

        if (scalarFields.isNotEmpty()) {
            scalarFields.forEach { (key, value) ->
                StatRow(label = key, value = renderFieldValue(value))
            }
        }

        tableFields.forEach { (key, rows) ->
            Text(
                text = tableTitle(key),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            DetailDataTable(rows = rows)
        }

        if (scalarFields.isEmpty() && tableFields.isEmpty()) {
            Text("No parsed detail fields", style = MaterialTheme.typography.bodySmall)
        }
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

private fun tableTitle(key: String): String {
    return when (key) {
        "cellData" -> "Cell Data"
        "satelliteData" -> "Satellite Data"
        "satellites" -> "Satellites"
        else -> key
    }
}

private fun orderedColumnsForField(
    messageType: Int,
    fieldKey: String,
    discoveredColumns: List<String>
): List<String> {
    val preferred = when {
        fieldKey == "satellites" && messageType == 1004 -> listOf(
            "index", "satelliteId", "l1Code", "l1Pseudorange", "l1PhaseRangeMinusPseudorange",
            "l1LockTimeIndicator", "integerL1PseudorangeModulus", "l1Cn0", "l2Code",
            "l2MinusL1Pseudorange", "l2MinusL1PhaseRange", "l2LockTimeIndicator", "l2Cn0"
        )

        fieldKey == "satellites" && messageType == 1012 -> listOf(
            "index", "satelliteId", "frequencyChannelNumber", "l1Code", "l1Pseudorange",
            "l1PhaseRangeMinusPseudorange", "l1LockTimeIndicator", "integerL1PseudorangeModulus",
            "l1Cn0", "l2Code", "l2MinusL1Pseudorange", "l2MinusL1PhaseRange",
            "l2LockTimeIndicator", "l2Cn0"
        )

        fieldKey == "satelliteData" -> listOf(
            "satelliteId", "roughRangeMs", "extendedInfo", "rangeModulo_1_1024ms", "roughPhaseRangeRate"
        )

        fieldKey == "cellData" -> listOf(
            "satelliteId", "signalId", "finePseudorange", "finePhaseRange", "lockTimeIndicator",
            "halfCycleAmbiguity", "cnr", "finePhaseRangeRate"
        )

        else -> listOf("index", "satelliteId", "signalId")
    }

    val preferredRank = preferred.withIndex().associate { it.value to it.index }
    return discoveredColumns.sortedWith(
        compareBy<String>(
            { preferredRank[it] ?: Int.MAX_VALUE },
            { it }
        )
    )
}

private fun renderFieldValue(value: Any?): String {
    return when (value) {
        null -> "null"
        is List<*> -> {
            if (value.isEmpty()) {
                "[]"
            } else {
                "[${value.joinToString(", ") { renderNestedValue(it) }}]"
            }
        }
        is Map<*, *> -> {
            if (value.isEmpty()) {
                "{}"
            } else {
                value.entries
                    .sortedBy { it.key?.toString().orEmpty() }
                    .joinToString(prefix = "{", postfix = "}") { (key, item) ->
                        "${key}=${renderNestedValue(item)}"
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
