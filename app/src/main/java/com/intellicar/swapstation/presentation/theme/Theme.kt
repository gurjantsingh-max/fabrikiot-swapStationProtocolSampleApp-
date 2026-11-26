package com.intellicar.swapstation.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E7C66),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F0DD),
    onPrimaryContainer = Color(0xFF00382C),
    secondary = Color(0xFF4A635A),
    secondaryContainer = Color(0xFFCCE8DC),
    tertiary = Color(0xFFB7791F),
    background = Color(0xFFF4F7F6),
    surface = Color(0xFFFBFDFC),
    surfaceVariant = Color(0xFFDCE5E1),
    surfaceContainer = Color(0xFFEDF2F0),
    surfaceContainerHigh = Color(0xFFE6ECEA),
    outlineVariant = Color(0xFFC0C9C5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD9B9),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFB8F0DD),
    secondary = Color(0xFFB1CCC0),
    secondaryContainer = Color(0xFF334B43),
    tertiary = Color(0xFFF5BE5B),
    background = Color(0xFF0F1513),
    surface = Color(0xFF121A17),
    surfaceVariant = Color(0xFF3F4945),
    surfaceContainer = Color(0xFF1A2320),
    surfaceContainerHigh = Color(0xFF222C29),
    outlineVariant = Color(0xFF3F4945),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun SwapStationTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
