plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Live MQTT settings come from gradle.properties (overridable with -P or ~/.gradle/gradle.properties).
// No fallback defaults: gradle.properties is the single source, and a missing key fails the build.
fun prop(key: String): String = providers.gradleProperty(key).orNull?.trim()?.takeIf { it.isNotEmpty() }
    ?: error("Missing Gradle property '$key' — set it in gradle.properties or pass -P$key=...")

android {
    namespace = "com.intellicar.swapstation"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.intellicar.swapstation"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Live HW1 broker: TLS 1.2 + X.509 client cert from assets/hw1/
        buildConfigField("String", "LIVE_MQTT_HOST", "\"${prop("live.mqtt.host")}\"")
        buildConfigField("int", "LIVE_MQTT_PORT", prop("live.mqtt.port"))
        // Topic segments (the topic structure itself is fixed in LiveTopics.kt)
        buildConfigField("String", "LIVE_TOPIC_ORG", "\"${prop("live.topic.org")}\"")
        buildConfigField("String", "LIVE_TOPIC_STATION_TYPE", "\"${prop("live.topic.stationType")}\"")
        buildConfigField("String", "LIVE_TOPIC_VERSION", "\"${prop("live.topic.version")}\"")
        buildConfigField("String", "LIVE_STATION_ID", "\"${prop("live.station.id")}\"")
        // Periodic heartbeat
        buildConfigField("int", "LIVE_HEARTBEAT_INTERVAL_SECONDS", prop("live.heartbeat.intervalSeconds"))
        buildConfigField("int", "LIVE_STATION_SLOT_COUNT", prop("live.station.slotCount"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            // Netty (used by the HiveMQ client) ships these in several jars
            excludes += setOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // MQTT + JSON + coroutines
    implementation(libs.hivemq.mqtt.client)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
