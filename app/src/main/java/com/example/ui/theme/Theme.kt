package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElegantDarkPrimary,
    onPrimary = ElegantDarkOnPrimary,
    primaryContainer = ElegantDarkPrimaryContainer,
    onPrimaryContainer = ElegantDarkOnPrimaryContainer,
    secondary = ElegantDarkSecondary,
    onSecondary = ElegantDarkOnPrimary,
    secondaryContainer = ElegantDarkSecondaryContainer,
    onSecondaryContainer = ElegantDarkOnSecondaryContainer,
    background = ElegantDarkBg,
    onBackground = ElegantDarkOnBg,
    surface = ElegantDarkSurface,
    onSurface = ElegantDarkOnSurface,
    surfaceVariant = ElegantDarkSurfaceVariant,
    onSurfaceVariant = ElegantDarkOnSurfaceVariant,
    outline = ElegantDarkOutline,
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = ElegantLightPrimary,
    onPrimary = ElegantLightOnPrimary,
    primaryContainer = ElegantLightPrimaryContainer,
    onPrimaryContainer = ElegantLightOnPrimaryContainer,
    secondary = ElegantLightSecondary,
    onSecondary = ElegantLightOnPrimary,
    secondaryContainer = ElegantLightSecondaryContainer,
    onSecondaryContainer = ElegantLightOnSecondaryContainer,
    background = ElegantLightBg,
    onBackground = ElegantLightOnBg,
    surface = ElegantLightSurface,
    onSurface = ElegantLightOnSurface,
    surfaceVariant = ElegantLightSurfaceVariant,
    onSurfaceVariant = ElegantLightOnSurfaceVariant,
    outline = ElegantLightOutline,
    error = Color(0xFFBA1A1A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force Light theme: "Elegant Light"
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme // Always use Elegant Light scheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
