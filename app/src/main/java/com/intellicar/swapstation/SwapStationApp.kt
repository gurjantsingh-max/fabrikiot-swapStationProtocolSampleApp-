package com.intellicar.swapstation

import android.app.Application
import com.intellicar.swapstation.data.live.heartbeat.HeartbeatSender
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** The live MQTT connection is started by the user from the Live Server screen. */
@HiltAndroidApp
class SwapStationApp : Application() {

    @Inject lateinit var heartbeatSender: HeartbeatSender

    override fun onCreate() {
        super.onCreate()
        // Sends the PERIODIC heartbeat every N seconds whenever the live broker is connected.
        heartbeatSender.start()
    }
}
