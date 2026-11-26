package com.intellicar.swapstation

import com.intellicar.swapstation.core.config.LiveServerConfig
import com.intellicar.swapstation.core.config.LiveTopics
import com.intellicar.swapstation.data.live.heartbeat.HeartbeatBuilder
import com.intellicar.swapstation.data.live.heartbeat.StationHeartbeat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Heartbeat JSON must carry every field name from Vendor API Reference v5.0.0 §2.1. */
class HeartbeatPayloadTest {
    private val config = LiveServerConfig(
        host = "h", port = 1,
        topics = LiveTopics("ITPL", "testingStation", "500.0.0", "testingStation_01"),
        heartbeatIntervalSeconds = 5,
        slotCount = 9,
    )
    private val json = Json { encodeDefaults = true }
    private val encoded = json.encodeToString(StationHeartbeat.serializer(), HeartbeatBuilder(config).build(nowEpochSeconds = 1_777_450_809, uptimeSeconds = 123))
    private val root = json.parseToJsonElement(encoded).jsonObject

    @Test fun `station-level fields match spec`() {
        val expected = setOf(
            "ts", "deviceId", "iccid", "backupSupplyStatus", "stationStatus", "stationDiagnosticCode", "mbFirmwareVersion",
            "mbHardwareVersion", "relaySetStatus", "currentLanguage", "protoVersion", "ticks", "slotPayloads",
        )
        assertEquals(expected, root.keys)
    }

    @Test fun `one slot payload per configured slot with all spec fields`() {
        val slots = root.getValue("slotPayloads").jsonArray
        assertEquals(9, slots.size)
        val expected = setOf(
            "batteryPresent", "batteryIdentification", "batteryMode", "batteryVoltage", "batteryTerminalVoltage",
            "batteryCurrent", "batteryFaultCode", "batteryTemperature", "batterySoc", "batterySoh", "batteryCycles",
            "batteryCapacity", "batterySerialNumber", "batteryCompatibility",
            "chargerPresent", "chargerMode", "chargerFaultCode", "chargerTemperature", "chargerACVoltage",
            "chargerTerminalVoltage", "chargerCurrent",
            "doorLockStatus", "slotTemperature1", "slotTemperature2", "gasSensor", "demandCurrent", "demandVoltage",
            "ticks", "dbFirmwareVersion", "dbHardwareVersion", "dbHwUniqueId", "dbStatus", "doorOpenCount",
        )
        slots.forEach { assertEquals(expected, it.jsonObject.keys) }
    }

    @Test fun `ts is real-time epoch seconds`() = assertTrue(encoded.contains("\"ts\":1777450809"))

    @Test fun `deviceId equals station id`() = assertTrue(encoded.contains("\"deviceId\":\"testingStation_01\""))

    @Test fun `default ts is the current clock in seconds`() {
        val before = System.currentTimeMillis() / 1000
        val ts = HeartbeatBuilder(config).build(uptimeSeconds = 0).ts
        assertTrue(ts in before..(System.currentTimeMillis() / 1000))
    }

    @Test fun `ticks carries uptime`() = assertTrue(encoded.contains("\"ticks\":123"))
}
