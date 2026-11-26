package com.intellicar.swapstation.data.live.heartbeat

import android.os.SystemClock
import com.intellicar.swapstation.core.config.LiveServerConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the heartbeat payload.
 *
 * This app has no real station hardware, so the readings below are SIMULATED placeholders
 * (stable, plausible values). Replace [slot] with real sensor/BMS data when it is available.
 */
@Singleton
class HeartbeatBuilder @Inject constructor(private val config: LiveServerConfig) {

    /**
     * @param nowEpochSeconds real-time clock, epoch seconds; read fresh for every heartbeat.
     * @param uptimeSeconds system uptime; defaults to the device's uptime since boot.
     */
    fun build(
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000,
        uptimeSeconds: Long = SystemClock.elapsedRealtime() / 1000,
    ): StationHeartbeat = StationHeartbeat(
        ts = nowEpochSeconds,
        deviceId = config.topics.stationId,
        iccid = "",
        backupSupplyStatus = 0,
        stationStatus = 0,
        stationDiagnosticCode = "0",
        mbFirmwareVersion = "1.0.0",
        mbHardwareVersion = "1.0.0",
        relaySetStatus = 0,
        currentLanguage = 1, // English
        protoVersion = PROTO_VERSION,
        ticks = uptimeSeconds,
        slotPayloads = (1..config.slotCount).map { slot(it, uptimeSeconds) },
    )

    private fun slot(n: Int, ticks: Long) = SlotPayload(
        batteryPresent = 1,
        batteryIdentification = "BAT$n",
        batteryMode = 0,
        batteryVoltage = 52.0,
        batteryTerminalVoltage = 52.0,
        batteryCurrent = 0.0,
        batteryFaultCode = 0,
        batteryTemperature = 30.0,
        batterySoc = (40 + n * 6).coerceAtMost(100),
        batterySoh = 100,
        batteryCycles = 0,
        batteryCapacity = 0.0,
        batterySerialNumber = "SN-BAT$n",
        batteryCompatibility = 1,
        chargerPresent = 1,
        chargerMode = 0,
        chargerFaultCode = 0,
        chargerTemperature = 30.0,
        chargerACVoltage = 230.0,
        chargerTerminalVoltage = 0.0,
        chargerCurrent = 0.0,
        doorLockStatus = 0,
        slotTemperature1 = 30.0,
        slotTemperature2 = 30.0,
        gasSensor = 0,
        demandCurrent = 0.0,
        demandVoltage = 0.0,
        ticks = ticks,
        dbFirmwareVersion = "1.0.0",
        dbHardwareVersion = "1.0.0",
        dbHwUniqueId = "DB-$n",
        dbStatus = 0,
        doorOpenCount = 0,
    )

    companion object {
        /** Vendor API Reference version this payload follows. */
        const val PROTO_VERSION = "5.0.0"
    }
}
