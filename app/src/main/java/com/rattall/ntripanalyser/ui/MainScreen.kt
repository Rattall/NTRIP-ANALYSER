package com.rattall.ntripanalyser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rattall.ntripanalyser.model.NtripProtocol

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("NTRIP RTCM3 Analyser", style = MaterialTheme.typography.headlineSmall)
            Text(ui.status, style = MaterialTheme.typography.bodyMedium)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ui.host, onValueChange = viewModel::setHost, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ui.port, onValueChange = viewModel::setPort, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ui.username, onValueChange = viewModel::setUsername, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = ui.password,
                        onValueChange = viewModel::setPassword,
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = ui.useTls, onCheckedChange = viewModel::setUseTls)
                        Text("Use TLS")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Protocol:")
                        Button(onClick = { viewModel.setProtocol(NtripProtocol.REV1) }) {
                            Text("REV1")
                        }
                        Button(onClick = { viewModel.setProtocol(NtripProtocol.REV2) }) {
                            Text("REV2")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::fetchSourceTable) { Text("Load Mountpoints") }
                        Button(onClick = viewModel::connect) { Text("Connect") }
                        Button(onClick = viewModel::disconnect) { Text("Stop") }
                    }
                }
            }
        }

        item {
            if (ui.sourceTable.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Mountpoints", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        ui.sourceTable.take(25).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.mountpoint)
                                Button(onClick = { viewModel.selectMountpoint(entry.mountpoint) }) {
                                    Text("Select")
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Live Stats", style = MaterialTheme.typography.titleMedium)
                    Text("Connected: ${stats.connected}")
                    Text("Bytes/s: ${"%.1f".format(stats.bytesPerSecond)} | Total: ${stats.totalBytes}")
                    Text("Msgs/s: ${"%.1f".format(stats.messagesPerSecond)} | Total: ${stats.totalMessages}")
                    Text("CRC Failures: ${stats.crcFailures}")
                    Text("Malformed Frames: ${stats.malformedFrames}")
                    Text("Uptime: ${stats.sessionUptimeMs / 1000}s")
                    Text("Selected mountpoint: ${ui.mountpoint.ifBlank { "(none)" }}")
                }
            }
        }

        item {
            if (ui.connectionEvents.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Connection Events", style = MaterialTheme.typography.titleMedium)
                        ui.connectionEvents.take(10).forEach { event ->
                            Text(event.message)
                        }
                    }
                }
            }
        }

        item {
            Text("Recent RTCM Messages", style = MaterialTheme.typography.titleMedium)
        }

        items(ui.recentMessages) { msg ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Type ${msg.messageType} | Len ${msg.payloadLength} | CRC ${msg.crcValid}")
                    Text(msg.fields.entries.joinToString(" | ") { "${it.key}=${it.value}" })
                }
            }
        }
    }
}
