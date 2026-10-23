package com.smd.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Steam-inspired dark color palette
val SteamBlue = Color(0xFF1B2838)
val SteamBlueLight = Color(0xFF2A475E)
val SteamAccent = Color(0xFF66C0F4)
val SteamAccentDim = Color(0xFF4B8EBF)
val SteamGreen = Color(0xFF4CAF50)
val SteamRed = Color(0xFFEF5350)
val SteamOrange = Color(0xFFFF9800)
val SteamGray = Color(0xFF8F98A0)
val SteamBackground = Color(0xFF1B2838)
val SteamSurface = Color(0xFF16202D)
val SteamSurfaceVariant = Color(0xFF1E2D3D)
val SteamOnSurface = Color(0xFFC6D4DF)
val SteamDivider = Color(0xFF2A3F5F)

// Light theme colors
val SteamLightBackground = Color(0xFFF0F0F0)
val SteamLightSurface = Color(0xFFFFFFFF)
val SteamLightSurfaceVariant = Color(0xFFE8EEF4)
val SteamLightOnSurface = Color(0xFF1B2838)
val SteamLightAccent = Color(0xFF1A6B9A)

private val DarkColorScheme = darkColorScheme(
    primary = SteamAccent,
    onPrimary = SteamBackground,
    primaryContainer = SteamBlueLight,
    onPrimaryContainer = SteamAccent,
    secondary = SteamAccentDim,
    onSecondary = Color.White,
    background = SteamBackground,
    onBackground = SteamOnSurface,
    surface = SteamSurface,
    onSurface = SteamOnSurface,
    surfaceVariant = SteamSurfaceVariant,
    onSurfaceVariant = SteamGray,
    outline = SteamDivider,
    error = SteamRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SteamLightAccent,
    onPrimary = Color.White,
    primaryContainer = SteamLightSurfaceVariant,
    onPrimaryContainer = SteamLightAccent,
    secondary = SteamAccentDim,
    onSecondary = Color.White,
    background = SteamLightBackground,
    onBackground = SteamLightOnSurface,
    surface = SteamLightSurface,
    onSurface = SteamLightOnSurface,
    surfaceVariant = SteamLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF546E7A),
    outline = Color(0xFFB0BEC5),
    error = SteamRed,
    onError = Color.White
)

@Composable
fun SteamManifestDownloaderTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
