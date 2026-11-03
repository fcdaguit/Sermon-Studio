package com.dlbc.sermonstudio.ui.theme

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
import com.dlbc.sermonstudio.data.prefs.ThemeMode

/**
 * Fallback palette for devices without dynamic colour (below Android 12) and
 * for users who turn it off. Chosen for long-form reading rather than brand
 * presence: warm, low-chroma, and high enough contrast to stay readable on a
 * phone held at a pulpit under stage lighting.
 */
private val SeedInk = Color(0xFF3A4A6B)
private val SeedInkLight = Color(0xFF8FA3C8)

private val LightScheme = lightColorScheme(
    primary = SeedInk,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2F7),
    onPrimaryContainer = Color(0xFF111C2E),
    secondary = Color(0xFF6B5B45),
    secondaryContainer = Color(0xFFF2E4CE),
    background = Color(0xFFFCFAF6),
    onBackground = Color(0xFF1B1B1C),
    surface = Color(0xFFFCFAF6),
    onSurface = Color(0xFF1B1B1C),
    surfaceVariant = Color(0xFFE4E1DA),
    onSurfaceVariant = Color(0xFF46464A)
)

private val DarkScheme = darkColorScheme(
    primary = SeedInkLight,
    onPrimary = Color(0xFF13233F),
    primaryContainer = Color(0xFF2A3A57),
    onPrimaryContainer = Color(0xFFD9E2F7),
    secondary = Color(0xFFD6C3A5),
    secondaryContainer = Color(0xFF53442F),
    background = Color(0xFF131316),
    onBackground = Color(0xFFE5E2DE),
    surface = Color(0xFF131316),
    onSurface = Color(0xFFE5E2DE),
    surfaceVariant = Color(0xFF46464A),
    onSurfaceVariant = Color(0xFFC8C6CA)
)

@Composable
fun SermonStudioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = SermonTypography,
        content = content
    )
}
