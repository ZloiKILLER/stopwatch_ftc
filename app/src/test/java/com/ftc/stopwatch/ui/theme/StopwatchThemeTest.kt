package com.ftc.stopwatch.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StopwatchThemeTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `light and dark resolve to different surfaces and text`() {
        val (light, dark) = capture(Theme(darkTheme = false), Theme(darkTheme = true))

        assertNotEquals(light.colors.background, dark.colors.background)
        assertNotEquals(light.colors.textPrimary, dark.colors.textPrimary)
        assertTrue("light background should be light", light.colors.background.luminance() > 0.5f)
        assertTrue("dark background should be dark", dark.colors.background.luminance() < 0.5f)
    }

    @Test
    fun `text contrasts with the background in both themes`() {
        val captured = capture(Theme(darkTheme = false), Theme(darkTheme = true))

        captured.forEach {
            val gap = abs(it.colors.textPrimary.luminance() - it.colors.background.luminance())
            assertTrue("dark=${it.darkTheme} needs readable text, gap was $gap", gap > 0.4f)
        }
    }

    @Test
    fun `stopwatch colours are taken from the Material scheme`() {
        val (captured) = capture(Theme(darkTheme = true))

        assertEquals(captured.scheme.background, captured.colors.background)
        assertEquals(captured.scheme.onBackground, captured.colors.textPrimary)
        assertEquals(captured.scheme.onSurfaceVariant, captured.colors.textSecondary)
        assertEquals(captured.scheme.outlineVariant, captured.colors.divider)
    }

    @Test
    fun `lap accents stay green and red rather than following the palette`() {
        val captured = capture(Theme(darkTheme = false), Theme(darkTheme = true))

        captured.forEach {
            val fastest = it.colors.lapFastestText
            val slowest = it.colors.lapSlowestText
            assertTrue("dark=${it.darkTheme} fastest should read green", fastest.green > fastest.red)
            assertTrue("dark=${it.darkTheme} slowest should read red", slowest.red > slowest.green)
        }
    }

    @Test
    fun `dynamic colour replaces the brand accent from Android 12`() {
        val (brand, dynamic) =
            capture(
                Theme(darkTheme = true, dynamicColor = false),
                Theme(darkTheme = true, dynamicColor = true),
            )

        assertEquals(AccentLight, brand.scheme.primary)
        assertNotEquals(
            "the wallpaper palette should replace the brand accent",
            brand.scheme.primary,
            dynamic.scheme.primary,
        )
    }

    /** Dynamic colour needs Android 12; older releases must still get a usable brand palette. */
    @Test
    @Config(sdk = [30])
    fun `falls back to the brand palette below Android 12`() {
        val (brand, dynamic) =
            capture(
                Theme(darkTheme = true, dynamicColor = false),
                Theme(darkTheme = true, dynamicColor = true),
            )

        assertEquals(AccentLight, dynamic.scheme.primary)
        assertEquals(brand.scheme.primary, dynamic.scheme.primary)
        assertEquals(brand.colors.background, dynamic.colors.background)
    }

    private data class Theme(val darkTheme: Boolean, val dynamicColor: Boolean = false)

    private class Captured(
        val darkTheme: Boolean,
        val scheme: ColorScheme,
        val colors: StopwatchColors,
    )

    /**
     * Renders every requested theme inside a single composition; [createComposeRule] only allows
     * one `setContent` per test.
     */
    private fun capture(vararg themes: Theme): List<Captured> {
        val captured = arrayOfNulls<Captured>(themes.size)
        composeTestRule.setContent {
            themes.forEachIndexed { index, theme ->
                StopwatchTheme(darkTheme = theme.darkTheme, dynamicColor = theme.dynamicColor) {
                    Capture(theme.darkTheme) { captured[index] = it }
                }
            }
        }
        composeTestRule.waitForIdle()
        return captured.map { requireNotNull(it) }
    }

    @Composable
    private fun Capture(darkTheme: Boolean, onCaptured: (Captured) -> Unit) {
        onCaptured(Captured(darkTheme, MaterialTheme.colorScheme, LocalStopwatchColors.current))
    }

    private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
