package com.intellicar.swapstation.data.live.heartbeat

import kotlinx.serialization.Serializable

/**
 * Periodic heartbeat, Station -> Server, published on the PERIODIC topic.
 * Field names follow BatterySmart Vendor API Reference v5.0.0, §2.1 "Periodic Heartbeat".
 *
 * NOTE: the spec says the wire format is Protobuf, but no .proto schema was provided, so this is
 * sent as JSON with the spec's field names. Switch the encoder once the .proto is available.
 */
@Serializable
data class StationHeartbeat(
    /** Real-time Unix timestamp in epoch SECONDS (§2 base field), taken when the heartbeat is built. */
    val ts: Long,
    /** Station device identifier (§2 base field) — same as the station id in the topic. */
    val deviceId: String,
    /** SIM card identifier. */
    val iccid: String,
    /** 1 = backup power active. */
    val backupSupplyStatus: Int,
    /** Overall station status code. */
    val stationStatus: Int,
    /** Non-zero = station-level issue detected. */
    val stationDiagnosticCode: String,
    val mbFirmwareVersion: String,
    val mbHardwareVersion: String,
    /** Relay board status. */
    val relaySetStatus: Int,
    /** Active language code (1 = English, see §1.7). */
    val currentLanguage: Int,
    /** Protocol version, e.g. "5.0.0". */
    val protoVersion: String,
    /** System uptime ticks. */
    val ticks: Long,
    /** One object per slot. */
    val slotPayloads: List<SlotPayload>,
)

/** Per-slot readings (§2.1 "Per-slot fields — slotPayloads[]"). */
@Serializable
data class SlotPayload(
    // Battery
    val batteryPresent: Int,
    val batteryIdentification: String,
    val batteryMode: Int,
    val batteryVoltage: Double,
    val batteryTerminalVoltage: Double,
    val batteryCurrent: Double,
    val batteryFaultCode: Int,
    val batteryTemperature: Double,
    val batterySoc: Int,
    val batterySoh: Int,
    val batteryCycles: Int,
    val batteryCapacity: Double,
    val batterySerialNumber: String,
    val batteryCompatibility: Int,
    // Charger
    val chargerPresent: Int,
    val chargerMode: Int,
    val chargerFaultCode: Int,
    val chargerTemperature: Double,
    val chargerACVoltage: Double,
    val chargerTerminalVoltage: Double,
    val chargerCurrent: Double,
    // Slot / daughterboard
    val doorLockStatus: Int,
    val slotTemperature1: Double,
    val slotTemperature2: Double,
    val gasSensor: Int,
    val demandCurrent: Double,
    val demandVoltage: Double,
    val ticks: Long,
    val dbFirmwareVersion: String,
    val dbHardwareVersion: String,
    val dbHwUniqueId: String,
    val dbStatus: Int,
    val doorOpenCount: Int,
)
