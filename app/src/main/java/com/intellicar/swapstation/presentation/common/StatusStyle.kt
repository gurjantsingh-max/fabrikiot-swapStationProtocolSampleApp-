package com.intellicar.swapstation.presentation.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.intellicar.swapstation.domain.model.ConnectionState

/** Colour + label for a status, used consistently across the app. */
data class StatusStyle(val label: String, val color: Color)

object StatusColors {
    val Available = Color(0xFF2E9E5B)
    val Occupied = Color(0xFF2F6FD6)
    val Charging = Color(0xFFE59A12)
    val Error = Color(0xFFD9443A)
    val Neutral = Color(0xFF8A9590)
}

fun ConnectionState.style() = when (this) {
    ConnectionState.Connected -> StatusStyle("Connected", StatusColors.Available)
    ConnectionState.Connecting -> StatusStyle("Connecting…", StatusColors.Charging)
    is ConnectionState.Reconnecting -> StatusStyle("Reconnecting (#$attempt)…", StatusColors.Charging)
    is ConnectionState.Disconnected -> StatusStyle("Disconnected", StatusColors.Error)
    ConnectionState.Idle -> StatusStyle("Not connected", StatusColors.Neutral)
}

/** A translucent tint of a status colour for backgrounds. */
@Composable
fun Color.tint(): Color = copy(alpha = if (isSystemInDarkTheme()) 0.22f else 0.12f)
