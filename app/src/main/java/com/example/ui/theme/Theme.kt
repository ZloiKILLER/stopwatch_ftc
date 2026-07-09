package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Custom design colors container to cleanly support Light & Dark themes
data class StopwatchColors(
    val background: Color,
    val surfaceCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val dividerColor: Color,
    val lapFastestBg: Color,
    val lapFastestText: Color,
    val lapSlowestBg: Color,
    val lapSlowestText: Color
)

val DarkStopwatchColors = StopwatchColors(
    background = DarkBg,
    surfaceCard = SurfaceCard,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    dividerColor = DividerColor,
    lapFastestBg = LapFastestBg,
    lapFastestText = LapFastestText,
    lapSlowestBg = LapSlowestBg,
    lapSlowestText = LapSlowestText
)

val LightStopwatchColors = StopwatchColors(
    background = LightBg,
    surfaceCard = LightSurfaceCard,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    dividerColor = LightDividerColor,
    lapFastestBg = LightLapFastestBg,
    lapFastestText = LightLapFastestText,
    lapSlowestBg = LightLapSlowestBg,
    lapSlowestText = LightLapSlowestText
)

val LocalStopwatchColors = staticCompositionLocalOf { DarkStopwatchColors }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = SurfaceCard,
    tertiary = LapFastestText,
    background = DarkBg,
    surface = SurfaceCard,
    onPrimary = Purple40,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = LightSurfaceCard,
    tertiary = LightLapFastestText,
    background = LightBg,
    surface = LightSurfaceCard,
    onPrimary = Color.White,
    onSecondary = LightTextPrimary,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val stopwatchColors = if (darkTheme) DarkStopwatchColors else LightStopwatchColors

    CompositionLocalProvider(LocalStopwatchColors provides stopwatchColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
