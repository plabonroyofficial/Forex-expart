package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ProTradingColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFFB5F4FF),
    secondary = BullishEmerald,
    onSecondary = Color(0xFF00201A),
    secondaryContainer = Color(0xFF005143),
    onSecondaryContainer = Color(0xFF70F7D9),
    tertiary = NeonViolet,
    onTertiary = Color(0xFF1E0060),
    tertiaryContainer = Color(0xFF3F1B96),
    onTertiaryContainer = Color(0xFFE9DDFF),
    background = CanvasBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = DividerDark,
    error = BearishRed,
    onError = Color.White
)

@Composable
fun ForexPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProTradingColorScheme,
        typography = Typography,
        content = content
    )
}
