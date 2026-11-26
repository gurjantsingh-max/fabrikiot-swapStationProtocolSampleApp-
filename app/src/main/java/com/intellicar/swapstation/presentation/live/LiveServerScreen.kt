package com.intellicar.swapstation.presentation.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intellicar.swapstation.core.config.OutgoingTopic
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.intellicar.swapstation.data.live.LiveLogEntry
import com.intellicar.swapstation.data.live.LiveLogKind
import com.intellicar.swapstation.domain.model.ConnectionState
import com.intellicar.swapstation.presentation.common.StatusColors
import com.intellicar.swapstation.presentation.common.StatusPill
import com.intellicar.swapstation.presentation.common.StatusStyle
import com.intellicar.swapstation.presentation.common.style
import com.intellicar.swapstation.presentation.common.toClockTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveServerScreen(viewModel: LiveServerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    val connected = state.connection == ConnectionState.Connected
    val busy = state.connection == ConnectionState.Connecting || state.connection is ConnectionState.Reconnecting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Server (HW1)") },
                actions = {
                    IconButton(onClick = viewModel::clearLog) { Icon(Icons.Default.Delete, contentDescription = "Clear log") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Broker", Modifier.padding(start = 8.dp).weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            StatusPill(state.connection.style())
                        }
                        Text(state.brokerUrl, fontFamily = FontFamily.Monospace)
                        Text(
                            "TLS 1.2 · server verified against HW1 CA · X.509 client certificate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Client ID: ${state.clientId}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                        (state.connection as? ConnectionState.Disconnected)?.reason?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = StatusColors.Error)
                        }
                        val canConnect = !connected && !busy
                        Button(
                            onClick = viewModel::connect,
                            enabled = canConnect,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Connect") }
                        OutlinedButton(
                            onClick = viewModel::disconnect,
                            enabled = connected || busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (busy) "Cancel" else "Disconnect") }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Topics", style = MaterialTheme.typography.titleMedium)
                        TopicRow("Commands  (Server → Station, subscribed)", state.topics.rpcRequest)
                        TopicRow("Heartbeat (Station → Server)", state.topics.periodic)
                        TopicRow("Events    (Station → Server)", state.topics.events)
                    }
                }
            }

            item {
                val hb = state.heartbeat
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Periodic heartbeat", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            StatusPill(
                                if (hb.running) StatusStyle("Every ${hb.intervalSeconds}s", StatusColors.Available)
                                else StatusStyle("Stopped", StatusColors.Neutral),
                            )
                        }
                        Text(
                            "Sent automatically to PERIODIC while connected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Sent: ${hb.sentCount}   Last: ${hb.lastSentAt?.toClockTime() ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                        hb.lastError?.let {
                            Text("Last error: $it", style = MaterialTheme.typography.bodySmall, color = StatusColors.Error)
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Send from station", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutgoingTopic.entries.forEach { topic ->
                                FilterChip(
                                    selected = state.outgoing == topic,
                                    onClick = { viewModel.onOutgoing(topic) },
                                    label = { Text(topic.label) },
                                )
                            }
                        }
                        Text(
                            state.outgoing.resolve(state.topics),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = state.message, onValueChange = viewModel::onMessage,
                            label = { Text("Message") }, modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = viewModel::send, enabled = connected,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Text("Send", Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Activity", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    // Manual retry (no automatic reconnect): enabled after a connection failed / ended.
                    OutlinedButton(
                        onClick = viewModel::retry,
                        enabled = state.connection is ConnectionState.Disconnected,
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Retry", Modifier.padding(start = 6.dp))
                    }
                }
            }
            if (state.log.isEmpty()) {
                item {
                    Text(
                        "Nothing yet. Tap Connect, then Send.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.log) { LogRow(it) }
        }
    }
}

@Composable
private fun TopicRow(label: String, topic: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(topic, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun LogRow(entry: LiveLogEntry) {
    val (tag, color) = when (entry.kind) {
        LiveLogKind.SENT -> "SENT" to StatusColors.Occupied
        LiveLogKind.RECEIVED -> "RECV" to StatusColors.Available
        LiveLogKind.SYSTEM -> "INFO" to StatusColors.Neutral
        LiveLogKind.ERROR -> "ERR " to StatusColors.Error
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entry.time.toClockTime(), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        Text(tag, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = color, fontWeight = FontWeight.Bold)
        Text(entry.text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
