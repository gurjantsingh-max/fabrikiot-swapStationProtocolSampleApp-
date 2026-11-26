package com.intellicar.swapstation.core.config

/**
 * The live MQTT topic contract. The STRUCTURE and DIRECTION of these topics are fixed and must
 * not change; only the four segment values come from configuration (gradle.properties):
 *
 *   <org>/<stationType>/<version>/<stationId>/RPC/REQUEST   Server -> Station (we subscribe)
 *   <org>/<stationType>/<version>/<stationId>/PERIODIC      Station -> Server (heartbeat, we publish)
 *   <org>/<stationType>/<version>/<stationId>/EVENTS        Station -> Server (we publish)
 *
 * e.g. ITPL/testingStation/500.0.0/testingStation_01/PERIODIC
 */
data class LiveTopics(
    val org: String,
    val stationType: String,
    val version: String,
    val stationId: String,
) {
    private val base: String get() = "$org/$stationType/$version/$stationId"

    /** Commands, Server -> Station. The station SUBSCRIBES here. */
    val rpcRequest: String get() = "$base/RPC/REQUEST"

    /** Heartbeat, Station -> Server. The station PUBLISHES here. */
    val periodic: String get() = "$base/PERIODIC"

    /** Events, Station -> Server. The station PUBLISHES here. */
    val events: String get() = "$base/EVENTS"
}

/** The two topics the station is allowed to publish to. */
enum class OutgoingTopic(val label: String) {
    PERIODIC("Heartbeat (PERIODIC)"),
    EVENTS("Event (EVENTS)");

    fun resolve(topics: LiveTopics): String = when (this) {
        PERIODIC -> topics.periodic
        EVENTS -> topics.events
    }
}
