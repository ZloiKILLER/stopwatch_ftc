package com.stopwatch.ftc.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.stopwatch.ftc.domain.Lap
import com.stopwatch.ftc.domain.StopwatchStatus
import com.stopwatch.ftc.ui.theme.StopwatchTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StopwatchScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun dark_theme_running_with_laps() {
        capture(darkTheme = true, fileName = "stopwatch_dark.png")
    }

    @Test
    fun light_theme_running_with_laps() {
        capture(darkTheme = false, fileName = "stopwatch_light.png")
    }

    @Test
    @Config(qualifiers = "+ru")
    fun russian_locale() {
        capture(darkTheme = true, fileName = "stopwatch_ru.png")
    }

    /** Two panes side by side, the layout a tablet, an unfolded foldable or a desktop window gets. */
    @Test
    @Config(qualifiers = "sw600dp-w1280dp-h800dp")
    fun wide_window_uses_two_panes() {
        capture(darkTheme = false, fileName = "stopwatch_wide.png")
    }

    /**
     * The longest readout the dial ever has to hold, on the smallest dial. Guards the regression
     * where a fixed font size pushed the hundredths onto a second line.
     */
    @Test
    @Config(qualifiers = "w320dp-h750dp")
    fun small_window_past_an_hour() {
        capture(darkTheme = true, fileName = "stopwatch_small_hours.png", elapsed = 4_521_890L)
    }

    private fun capture(darkTheme: Boolean, fileName: String, elapsed: Long = 154_820L) {
        composeTestRule.setContent {
            // Dynamic colour is switched off so the reference images stay deterministic; the
            // wallpaper palette is not something a screenshot test can pin down.
            StopwatchTheme(darkTheme = darkTheme, dynamicColor = false) { SampleScreen(elapsed) }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$fileName")
    }

    @Composable
    private fun SampleScreen(elapsed: Long) {
        StopwatchContent(
            status = StopwatchStatus.RUNNING,
            laps = SAMPLE_LAPS,
            highlights = LapHighlights(fastestLapNumber = 3, slowestLapNumber = 2),
            elapsedMillis = { elapsed },
            onStart = {},
            onPause = {},
            onLap = {},
            onReset = {},
        )
    }

    private companion object {
        val SAMPLE_LAPS =
            listOf(
                Lap(number = 4, lapMillis = 41_120L, totalMillis = 154_820L),
                Lap(number = 3, lapMillis = 33_400L, totalMillis = 113_700L),
                Lap(number = 2, lapMillis = 45_900L, totalMillis = 80_300L),
                Lap(number = 1, lapMillis = 34_400L, totalMillis = 34_400L),
            )
    }
}
