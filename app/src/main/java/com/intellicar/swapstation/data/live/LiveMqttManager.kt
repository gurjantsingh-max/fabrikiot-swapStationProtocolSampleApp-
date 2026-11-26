package com.intellicar.swapstation.data.live

import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAckReturnCode
import com.intellicar.swapstation.core.config.LiveServerConfig
import com.intellicar.swapstation.core.di.ApplicationScope
import com.intellicar.swapstation.domain.model.ConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

enum class LiveLogKind { SYSTEM, SENT, RECEIVED, ERROR }

data class LiveLogEntry(val time: Instant, val kind: LiveLogKind, val text: String)

/**
 * MQTT client for the live HW1 broker (TLS 1.2 + client certificate).
 * Only connects when the user taps Connect on the Live Server screen.
 *
 * Topic directions follow [com.intellicar.swapstation.core.config.LiveTopics]:
 * we subscribe to RPC/REQUEST (Server -> Station) and publish to PERIODIC / EVENTS (Station -> Server).
 */
@Singleton
class LiveMqttManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: LiveServerConfig,
    clientIdProvider: ClientIdProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val clientId: String = clientIdProvider.clientId + "-hw1"
    val brokerUrl: String get() = config.brokerUrl

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<LiveLogEntry>>(emptyList())
    val log: StateFlow<List<LiveLogEntry>> = _log.asStateFlow()

    /** Commands from the server (Server -> Station). Subscribed after each connect. */
    private val commandTopic: String = config.topics.rpcRequest

    // Built lazily: loading the certificates is only done when the tab is actually used.
    private var _client: Mqtt3AsyncClient? = null
    private val incomingFlowActive = AtomicBoolean(false)

    /** Set when the user taps Disconnect/Cancel, so a connect that finishes late is dropped. */
    @Volatile private var stopRequested = false

    /** Returns the client, building it (and loading the certificates) on first use. */
    private fun client(): Mqtt3AsyncClient = _client ?: buildClient().also { _client = it }

    private fun buildClient(): Mqtt3AsyncClient {
        val tls = LiveTlsFactory.create(context, config, tlsLogger)
        log("MQTT: building client (mutual TLS, client certificate) " +
            "id=$clientId host=${config.host} port=${config.port} " +
            "keepAlive=${config.keepAliveSeconds}s handshakeTimeout=15s autoReconnect=OFF (manual Retry)")
        return MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .serverHost(config.host)
            .serverPort(config.port)
            .sslConfig()
                .protocols(listOf(config.tlsProtocol))
                .keyManagerFactory(tls.keyManagerFactory)
                .trustManagerFactory(tls.trustManagerFactory)
                .hostnameVerifier(tls.hostnameVerifier)
                .handshakeTimeout(15, TimeUnit.SECONDS)
                .applySslConfig()
            .addConnectedListener {
                if (stopRequested) {
                    // The connect finished just after the user cancelled: drop it.
                    scope.launch { runCatching { _client?.disconnect()?.await() } }
                    return@addConnectedListener
                }
                ensureIncomingFlow()
                val took = connectStartedAt.takeIf { it > 0 }?.let { " in ${System.currentTimeMillis() - it} ms" } ?: ""
                add(LiveLogKind.SYSTEM, "Connected to ${config.brokerUrl} (TLS 1.2, client cert) as $clientId$took")
                _state.value = ConnectionState.Connected
                subscribeCommandTopic()
            }
            .addDisconnectedListener { ctx ->
                // No automatic reconnect: every failure/loss ends in Disconnected, and the user
                // decides when to try again (Retry button in the Activity section).
                val reason = rootMessage(ctx.cause)
                log("MQTT: disconnected listener — source=${ctx.source}, clientState=${ctx.clientConfig.state}, " +
                    "stopRequested=$stopRequested")
                logCauseChain(ctx.cause)
                if (ctx.source == MqttDisconnectSource.USER) {
                    add(LiveLogKind.SYSTEM, "Disconnected")
                    _state.value = ConnectionState.Disconnected("Disconnected by user")
                } else if (stopRequested) {
                    _state.value = ConnectionState.Disconnected("Cancelled by user")
                } else {
                    add(LiveLogKind.ERROR, "Connection lost/failed: $reason. Tap Retry to try again.")
                    add(LiveLogKind.SYSTEM, "Hint: ${hintFor(ctx.cause)}")
                    _state.value = ConnectionState.Disconnected("Failed: $reason")
                }
            }
            .buildAsync()
    }

    private fun ensureIncomingFlow() {
        val c = _client ?: return
        if (!incomingFlowActive.compareAndSet(false, true)) return
        c.toRx().publishes(MqttGlobalPublishFilter.ALL).subscribe(
            { p -> add(LiveLogKind.RECEIVED, "${p.topic}  ${p.payloadAsBytes.decodeToString()}  (qos=${p.qos}, retain=${p.isRetain})") },
            { e -> log("MQTT: incoming flow ended with error: ${rootMessage(e)}"); incomingFlowActive.set(false) },
            { log("MQTT: incoming flow completed"); incomingFlowActive.set(false) },
        )
    }

    /** Connects with mutual TLS: the device presents its client certificate (client.crt + client.key). */
    fun connect(isRetry: Boolean = false) {
        // Every Connect / Retry starts with a fresh Activity log.
        clearLog()
        if (isRetry) add(LiveLogKind.SYSTEM, "Retry requested")
        log("MQTT: connect() requested — current state=${_state.value}, client=${_client?.state ?: "not built"}")
        val c = try {
            client()
        } catch (e: Exception) {
            Log.e(TAG, "Could not load HW1 certificates", e)
            add(LiveLogKind.ERROR, "Could not load certificates: ${rootMessage(e)}")
            logCauseChain(e)
            _state.value = ConnectionState.Disconnected("Certificate error")
            return
        }
        if (c.state != MqttClientState.DISCONNECTED) {
            log("MQTT: connect() ignored — client is already ${c.state}")
            return
        }
        stopRequested = false
        _state.value = ConnectionState.Connecting
        add(LiveLogKind.SYSTEM, "Connecting to ${config.brokerUrl}")
        ensureIncomingFlow()
        scope.launch {
            networkPreflight()
            connectStartedAt = System.currentTimeMillis()
            log("MQTT: sending CONNECT (cleanSession=true, keepAlive=${config.keepAliveSeconds}s) — TCP + TLS handshake starting")
            try {
                val ack = c.connectWith()
                    .cleanSession(true)
                    .keepAlive(config.keepAliveSeconds)
                    .send()
                    .await()
                log("MQTT: CONNACK received — returnCode=${ack.returnCode}, sessionPresent=${ack.isSessionPresent}")
            } catch (e: Exception) {
                val took = System.currentTimeMillis() - connectStartedAt
                Log.w(TAG, "Live connect failed after $took ms", e)
                add(LiveLogKind.ERROR, "CONNECT failed after $took ms: ${e.javaClass.simpleName}: ${rootMessage(e)}")
                logCauseChain(e)
                // Safety net in case the disconnected listener did not run.
                if (_state.value == ConnectionState.Connecting) {
                    _state.value = ConnectionState.Disconnected("Failed: ${rootMessage(e)}")
                }
            }
        }
    }

    /** Manual retry: one new connection attempt. */
    fun retry() = connect(isRetry = true)

    fun disconnect() {
        val c = _client ?: return
        log("MQTT: disconnect() requested — client state=${c.state}")
        stopRequested = true
        if (c.state != MqttClientState.CONNECTED) {
            // Still connecting: the attempt in flight is dropped when it finishes.
            add(LiveLogKind.SYSTEM, "Cancelled")
            _state.value = ConnectionState.Disconnected("Cancelled by user")
        }
        scope.launch {
            try {
                c.disconnect().await()
            } catch (e: Exception) {
                Log.w(TAG, "Live disconnect: ${e.message}")
            }
            _state.value = ConnectionState.Disconnected("Disconnected by user")
        }
    }

    /**
     * @param logText what the Activity log shows for this message (default: topic + payload).
     * @param quiet true = skip the extra PUBLISH diagnostic line (used for the 5 s heartbeat).
     */
    suspend fun publish(
        topic: String,
        payload: String,
        logText: String = "$topic  $payload",
        quiet: Boolean = false,
    ): Result<Unit> = runCatching {
        val c = _client
        check(c != null && c.state == MqttClientState.CONNECTED) { "Not connected to live server (state=${c?.state})" }
        if (!quiet) log("MQTT: PUBLISH topic=$topic qos=1 bytes=${payload.toByteArray().size}")
        c.publishWith()
            .topic(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .payload(payload.toByteArray())
            .send()
            .await()
        add(LiveLogKind.SENT, logText)
    }.onFailure { add(LiveLogKind.ERROR, "Publish failed: ${rootMessage(it)}") }

    private fun subscribeCommandTopic() {
        val c = _client ?: return
        val topic = commandTopic
        scope.launch {
            log("MQTT: SUBSCRIBE $topic qos=1")
            try {
                val ack = c.subscribeWith().topicFilter(topic).qos(MqttQos.AT_LEAST_ONCE).send().await()
                log("MQTT: SUBACK for $topic — returnCodes=${ack.returnCodes}")
                if (ack.returnCodes.any { it == Mqtt3SubAckReturnCode.FAILURE }) {
                    add(LiveLogKind.ERROR, "Broker refused subscription to $topic (publishing still works)")
                } else {
                    add(LiveLogKind.SYSTEM, "Listening for commands on $topic")
                }
            } catch (e: Exception) {
                add(LiveLogKind.ERROR, "Subscribe to $topic failed: ${rootMessage(e)}")
            }
        }
    }

    fun clearLog() { _log.value = emptyList() }

    @Volatile private var connectStartedAt = 0L

    /** TLS handshake diagnostics from [LiveTlsFactory] (called on Netty threads). */
    private val tlsLogger = object : LiveTlsLogger {
        override fun info(message: String) = log(message)
        override fun error(message: String, t: Throwable?) {
            if (t != null) Log.e(TAG, message, t)
            add(LiveLogKind.ERROR, message)
        }
    }

    /**
     * Resolves the host and opens a plain TCP socket to the broker port before MQTT starts,
     * so "can't reach the server" (firewall / VPN / wrong port) is told apart from TLS/MQTT errors.
     * Only diagnostic: the real connect is attempted whatever the result.
     */
    private suspend fun networkPreflight() = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val addresses = try {
            InetAddress.getAllByName(config.host).also { addrs ->
                log("NET: DNS ${config.host} → ${addrs.joinToString { it.hostAddress ?: "?" }} (${System.currentTimeMillis() - t0} ms)")
            }
        } catch (e: Exception) {
            add(LiveLogKind.ERROR, "NET: DNS lookup for ${config.host} FAILED: ${rootMessage(e)} — check internet / DNS")
            return@withContext
        }
        val target = addresses.first()
        val t1 = System.currentTimeMillis()
        try {
            Socket().use { it.connect(InetSocketAddress(target, config.port), PREFLIGHT_TIMEOUT_MS) }
            log("NET: TCP ${target.hostAddress}:${config.port} reachable (${System.currentTimeMillis() - t1} ms) ✓")
        } catch (e: Exception) {
            add(
                LiveLogKind.ERROR,
                "NET: TCP ${target.hostAddress}:${config.port} NOT reachable after ${System.currentTimeMillis() - t1} ms " +
                    "(${e.javaClass.simpleName}: ${e.message}). Port blocked by firewall, IP not whitelisted, or VPN needed.",
            )
        }
    }

    private fun logCauseChain(t: Throwable?) {
        var e: Throwable? = t
        var depth = 0
        while (e != null && depth < 8) {
            log("  cause[$depth] ${e.javaClass.name}: ${e.message}")
            if (e.cause === e) break
            e = e.cause
            depth++
        }
    }

    /** Human hint for the most common live-broker failures. */
    private fun hintFor(t: Throwable): String {
        val chain = generateSequence(t) { it.cause?.takeIf { c -> c !== it } }.toList()
        val text = chain.joinToString(" | ") { "${it.javaClass.simpleName} ${it.message}" }.lowercase()
        return when {
            "unknownhost" in text -> "DNS failed — check the device's internet connection."
            "connecttimeout" in text || "connection timed out" in text || "timed out" in text ->
                "TCP timed out — port ${config.port} is unreachable from this network (firewall/whitelist/VPN)."
            "connection refused" in text -> "Server refused the TCP connection — wrong port or broker down."
            "certificate_unknown" in text || "bad_certificate" in text || "unknown_ca" in text ->
                "Broker rejected our client certificate — check client.crt/client.key are the ones the broker expects."
            "certpathvalidator" in text || "trust anchor" in text ->
                "Broker's certificate does not chain to ca.crt — wrong CA file?"
            "not yet valid" in text || "notyetvalid" in text || "expired" in text ->
                "Certificate date problem — check the device date/time."
            "handshake" in text || "ssl" in text -> "TLS handshake failed — see TLS lines above for the step that broke."
            "connack" in text || "not authorized" in text || "identifier rejected" in text ->
                "Broker rejected the MQTT CONNECT (auth / client id)."
            else -> "see cause lines above."
        }
    }

    private fun log(text: String) = add(LiveLogKind.SYSTEM, text)

    private fun add(kind: LiveLogKind, text: String) {
        if (kind == LiveLogKind.ERROR) Log.e(TAG, text) else Log.d(TAG, "$kind $text")
        _log.update { (listOf(LiveLogEntry(Instant.now(), kind, text)) + it).take(MAX_LOG) }
    }

    private fun rootMessage(t: Throwable): String {
        var e: Throwable = t
        while (e.cause != null && e.cause !== e) e = e.cause!!
        return e.message ?: e.javaClass.simpleName
    }

    private companion object {
        const val TAG = "LiveMqtt"
        const val MAX_LOG = 500
        const val PREFLIGHT_TIMEOUT_MS = 8_000
    }
}
