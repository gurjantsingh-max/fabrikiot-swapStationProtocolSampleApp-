# Swap Station Android App

Kotlin · Jetpack Compose · MVVM · Hilt · Coroutines/Flow · HiveMQ MQTT client

**Why HiveMQ client (not Eclipse Paho Android Service):** Paho's Android service is unmaintained
and crashes on Android 12+ (PendingIntent/foreground-service changes). HiveMQ's client is
actively maintained and has a future-based API that maps neatly onto coroutines.

The app connects to the live HW1 broker over TLS 1.2 with mutual X.509 auth
(cert files in `app/src/main/assets/hw1/`).

## MQTT topics (fixed contract)

| Purpose   | Direction        | Topic                                                      |
|-----------|------------------|------------------------------------------------------------|
| Commands  | Server → Station | `ITPL/testingStation/500.0.0/testingStation_01/RPC/REQUEST` |
| Heartbeat | Station → Server | `ITPL/testingStation/500.0.0/testingStation_01/PERIODIC`    |
| Events    | Station → Server | `ITPL/testingStation/500.0.0/testingStation_01/EVENTS`      |

The topic **structure and direction are fixed** in `core/config/LiveTopics.kt` (guarded by
`LiveTopicsTest`). Only the segment values are configuration.

## Periodic heartbeat

While connected, `HeartbeatSender` publishes a heartbeat to `.../PERIODIC` every
`live.heartbeat.intervalSeconds` (5 s), and stops on disconnect. The payload follows Vendor API
Reference v5.0.0 §2.1 (station-level fields + one `slotPayloads[]` entry per slot, guarded by
`HeartbeatPayloadTest`). Two caveats:
- It is sent as **JSON**: the spec says Protobuf, but no `.proto` schema was provided.
- Slot readings are **simulated** (`HeartbeatBuilder`) since the app has no real station hardware.

## Configure

`gradle.properties`:
```properties
live.mqtt.host=hw1.buzzenergy.in
live.mqtt.port=11883
live.topic.org=ITPL
live.topic.stationType=testingStation
live.topic.version=500.0.0
live.station.id=testingStation_01
live.heartbeat.intervalSeconds=5
live.station.slotCount=9
```
Override without editing: `./gradlew installDebug -Plive.station.id=testingStation_02`.

There are no fallback defaults in `build.gradle.kts`: if a key is missing the build fails with a
clear message. Values are copied into `BuildConfig` at build time, so rebuild + reinstall after a change.

## Build / run

Open the folder in Android Studio and Run, or:
```bash
./gradlew installDebug
./gradlew testDebugUnitTest
```
minSdk 26, targetSdk 37.

## Structure

```
core/config/   LiveServerConfig (host/port/certs), LiveTopics (fixed topic contract), OutgoingTopic
data/live/     LiveMqttManager   TLS MQTT client: connect/retry/disconnect, subscribe RPC/REQUEST, publish
               LiveTlsFactory    CA trust + client cert key manager, TLS diagnostics
               ClientIdProvider  android-<random>, stable per install
  heartbeat/   HeartbeatSender (5 s loop while connected), HeartbeatBuilder, HeartbeatPayload
domain/model/  ConnectionState
presentation/
  live/        LiveServerScreen + ViewModel
  common/      StatusPill, status colours, time formatting
di/            AppModule (app scope), LiveServerModule (config)
```
