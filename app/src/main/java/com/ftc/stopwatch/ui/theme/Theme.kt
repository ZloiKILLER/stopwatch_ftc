package com.ftc.stopwatch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Colours the stopwatch needs that have no counterpart in the Material colour scheme. */
data class StopwatchColors(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val lapFastestBackground: Color,
    val lapFastestText: Color,
    val lapSlowestBackground: Color,
    val lapSlowestText: Color,
)

val LocalStopwatchColors =
    staticCompositionLocalOf<StopwatchColors> {
        error("No StopwatchColors available. Wrap the content in StopwatchTheme.")
    }

/** Material You wallpaper colours are only available from Android 12. */
val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun StopwatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Remembered rather than recomputed: dynamic*ColorScheme returns a fresh instance every call,
    // and LocalStopwatchColors is a static CompositionLocal, so an unstable value would recompose
    // the whole tree on every frame the timer ticks.
    val colorScheme =
        remember(darkTheme, dynamicColor, context) {
            when {
                dynamicColor && supportsDynamicColor ->
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
        }

    val stopwatchColors = remember(colorScheme, darkTheme) { colorScheme.stopwatchColors(darkTheme) }

    CompositionLocalProvider(LocalStopwatchColors provides stopwatchColors) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}

private fun ColorScheme.stopwatchColors(darkTheme: Boolean) =
    StopwatchColors(
        background = background,
        textPrimary = onBackground,
        textSecondary = onSurfaceVariant,
        divider = outlineVariant,
        // Fastest and slowest carry meaning, so they stay green and red rather than following the
        // wallpaper. Material has no "success" role, and a fastest lap tinted error-red would be a
        // worse outcome than a slight palette mismatch.
        lapFastestBackground = if (darkTheme) DarkLapFastestBackground else LightLapFastestBackground,
        lapFastestText = if (darkTheme) DarkLapFastestText else LightLapFastestText,
        lapSlowestBackground = if (darkTheme) DarkLapSlowestBackground else LightLapSlowestBackground,
        lapSlowestText = if (darkTheme) DarkLapSlowestText else LightLapSlowestText,
    )

/** Brand palette, used on Android 11 and older and whenever dynamic colour is switched off. */
private val DarkColorScheme =
    darkColorScheme(
        primary = AccentLight,
        onPrimary = OnAccentDark,
        secondary = DarkSurface,
        onSecondary = DarkTextPrimary,
        tertiary = DarkLapFastestText,
        background = DarkBackground,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkTextSecondary,
        outlineVariant = DarkDivider,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = AccentDark,
        onPrimary = Color.White,
        secondary = LightSurface,
        onSecondary = LightTextPrimary,
        tertiary = LightLapFastestText,
        background = LightBackground,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        onSurfaceVariant = LightTextSecondary,
        outline = LightTextSecondary,
        outlineVariant = LightDivider,
    )
