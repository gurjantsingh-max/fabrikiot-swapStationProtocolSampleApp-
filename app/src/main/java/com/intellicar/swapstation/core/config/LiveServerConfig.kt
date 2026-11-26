package com.intellicar.swapstation.core.config

import com.intellicar.swapstation.BuildConfig

/**
 * Settings for the LIVE HW1 broker. Values come from gradle.properties via BuildConfig.
 *
 * Connection is TLS 1.2 with mutual X.509 auth: the broker is verified against our CA file and
 * the device presents its client certificate. All three files are PEM and live in
 * app/src/main/assets/hw1/ (ca.crt, client.crt, client.key — unencrypted PKCS#8 key).
 */
data class LiveServerConfig(
    val host: String,
    val port: Int,
    val topics: LiveTopics,
    /** Heartbeat period on the PERIODIC topic while connected. */
    val heartbeatIntervalSeconds: Int,
    /** Number of slots reported in the heartbeat's slotPayloads[]. */
    val slotCount: Int,
    val keepAliveSeconds: Int = 30,
    val caCertAsset: String = "hw1/ca.crt",
    /** PEM client certificate (may contain intermediates after the leaf). */
    val clientCertAsset: String = "hw1/client.crt",
    /** PEM private key for [clientCertAsset], unencrypted PKCS#8 ("BEGIN PRIVATE KEY"). */
    val clientKeyAsset: String = "hw1/client.key",
    val tlsProtocol: String = "TLSv1.2",
) {
    val brokerUrl: String get() = "ssl://$host:$port"

    companion object {
        fun fromBuildConfig() = LiveServerConfig(
            host = BuildConfig.LIVE_MQTT_HOST,
            port = BuildConfig.LIVE_MQTT_PORT,
            topics = LiveTopics(
                org = BuildConfig.LIVE_TOPIC_ORG,
                stationType = BuildConfig.LIVE_TOPIC_STATION_TYPE,
                version = BuildConfig.LIVE_TOPIC_VERSION,
                stationId = BuildConfig.LIVE_STATION_ID,
            ),
            heartbeatIntervalSeconds = BuildConfig.LIVE_HEARTBEAT_INTERVAL_SECONDS,
            slotCount = BuildConfig.LIVE_STATION_SLOT_COUNT,
        )
    }
}
