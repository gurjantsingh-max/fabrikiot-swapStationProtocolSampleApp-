package com.intellicar.swapstation.data.live.heartbeat

import com.intellicar.swapstation.core.config.LiveServerConfig
import com.intellicar.swapstation.core.di.ApplicationScope
import com.intellicar.swapstation.data.live.LiveMqttManager
import com.intellicar.swapstation.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class HeartbeatStatus(
    val intervalSeconds: Int,
    val running: Boolean = false,
    val sentCount: Long = 0,
    val lastSentAt: Instant? = null,
    val lastError: String? = null,
)

/**
 * Sends the periodic heartbeat (Station -> Server, PERIODIC topic) every
 * [LiveServerConfig.heartbeatIntervalSeconds] while connected to the live broker.
 * Starts automatically on connect and stops on disconnect.
 */
@Singleton
class HeartbeatSender @Inject constructor(
    private val live: LiveMqttManager,
    private val config: LiveServerConfig,
    private val builder: HeartbeatBuilder,
    private val json: Json,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow(HeartbeatStatus(config.heartbeatIntervalSeconds))
    val status: StateFlow<HeartbeatStatus> = _status.asStateFlow()

    private val started = AtomicBoolean(false)

    /** Call once (from the Application). Safe to call more than once. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            live.state
                .map { it == ConnectionState.Connected }
                .distinctUntilChanged()
                .collectLatest { connected ->
                    // collectLatest cancels the loop below as soon as the connection drops.
                    _status.update { it.copy(running = connected) }
                    if (!connected) return@collectLatest
                    val intervalMs = config.heartbeatIntervalSeconds * 1000L
                    while (isActive) {
                        val startedAt = System.currentTimeMillis()
                        sendOnce()
                        // Fixed rate: subtract the time the publish took.
                        delay((intervalMs - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0))
                    }
                }
        }
    }

    private suspend fun sendOnce() {
        val topic = config.topics.periodic
        val payload = json.encodeToString(StationHeartbeat.serializer(), builder.build())
        val n = _status.value.sentCount + 1
        live.publish(topic, payload, logText = "Heartbeat #$n → $topic (${payload.length} bytes)", quiet = true)
            .onSuccess { _status.update { it.copy(sentCount = n, lastSentAt = Instant.now(), lastError = null) } }
            .onFailure { e -> _status.update { it.copy(lastError = e.message ?: e.javaClass.simpleName) } }
    }
}
