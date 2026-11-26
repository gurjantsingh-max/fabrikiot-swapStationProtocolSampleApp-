package com.intellicar.swapstation.domain.model

/** State of the app's own connection to the MQTT broker. */
sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data class Reconnecting(val attempt: Int, val reason: String) : ConnectionState
    data class Disconnected(val reason: String?) : ConnectionState
}
