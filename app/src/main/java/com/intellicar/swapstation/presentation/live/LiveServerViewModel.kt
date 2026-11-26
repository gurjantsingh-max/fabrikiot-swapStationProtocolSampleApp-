package com.intellicar.swapstation.presentation.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intellicar.swapstation.core.config.LiveServerConfig
import com.intellicar.swapstation.core.config.LiveTopics
import com.intellicar.swapstation.core.config.OutgoingTopic
import com.intellicar.swapstation.data.live.LiveLogEntry
import com.intellicar.swapstation.data.live.LiveMqttManager
import com.intellicar.swapstation.data.live.heartbeat.HeartbeatSender
import com.intellicar.swapstation.data.live.heartbeat.HeartbeatStatus
import com.intellicar.swapstation.domain.model.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveServerUiState(
    val brokerUrl: String,
    val clientId: String,
    /** The fixed topic contract (read-only in the UI). */
    val topics: LiveTopics,
    val connection: ConnectionState = ConnectionState.Idle,
    /** Which Station -> Server topic the next message goes to. */
    val outgoing: OutgoingTopic = OutgoingTopic.EVENTS,
    val message: String = "hi",
    val log: List<LiveLogEntry> = emptyList(),
    val heartbeat: HeartbeatStatus,
)

@HiltViewModel
class LiveServerViewModel @Inject constructor(
    private val live: LiveMqttManager,
    private val config: LiveServerConfig,
    heartbeat: HeartbeatSender,
) : ViewModel() {

    private data class Form(val outgoing: OutgoingTopic, val message: String)

    private val form = MutableStateFlow(Form(OutgoingTopic.EVENTS, "hi"))

    private val initial = LiveServerUiState(live.brokerUrl, live.clientId, config.topics, heartbeat = heartbeat.status.value)

    val uiState: StateFlow<LiveServerUiState> = combine(
        live.state, live.log, form, heartbeat.status,
    ) { conn, log, f, hb ->
        LiveServerUiState(live.brokerUrl, live.clientId, config.topics, conn, f.outgoing, f.message, log, hb)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun onOutgoing(value: OutgoingTopic) = form.update { it.copy(outgoing = value) }
    fun onMessage(value: String) = form.update { it.copy(message = value) }

    fun connect() = live.connect()
    fun retry() = live.retry()
    fun disconnect() = live.disconnect()
    fun clearLog() = live.clearLog()

    fun send() {
        val payload = form.value.message
        if (payload.isEmpty()) return notify("Enter a message")
        val topic = form.value.outgoing.resolve(config.topics)
        viewModelScope.launch {
            live.publish(topic, payload)
                .onSuccess { notify("Sent to $topic") }
                .onFailure { notify("Send failed: ${it.message}") }
        }
    }

    private fun notify(message: String) { _messages.trySend(message) }
}
