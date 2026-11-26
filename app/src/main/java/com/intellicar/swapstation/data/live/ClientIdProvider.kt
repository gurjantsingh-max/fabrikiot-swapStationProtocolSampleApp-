package com.intellicar.swapstation.data.live

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MQTT client IDs must be unique per broker: if two clients use the same ID the broker kicks
 * the older one off. We generate "android-<random>" once per install and keep it, so the same
 * phone always has the same ID (handy when reading broker logs) and different phones never clash.
 */
@Singleton
class ClientIdProvider @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("mqtt", Context.MODE_PRIVATE)

    val clientId: String by lazy {
        prefs.getString(KEY, null) ?: ("android-" + UUID.randomUUID().toString().replace("-", "").take(12))
            .also { prefs.edit { putString(KEY, it) } }
    }

    private companion object { const val KEY = "client_id" }
}
