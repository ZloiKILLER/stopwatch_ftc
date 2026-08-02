package com.ftc.stopwatch.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.ftc.stopwatch.domain.Lap
import com.ftc.stopwatch.domain.StopwatchStatus
import com.ftc.stopwatch.ui.theme.StopwatchTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StopwatchScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    /**
     * Regression: a LazyColumn keeps its scroll anchored on the item that was first before an
     * insert. With laps prepended, every new lap past a screenful used to land above the viewport,
     * which read as the app having stopped recording them.
     */
    @Test
    fun newest_lap_stays_visible_once_the_list_is_long_enough_to_scroll() {
        val laps = mutableStateOf(emptyList<Lap>())
        setContent(laps)

        repeat(LAPS_WELL_PAST_A_SCREENFUL) { index ->
            val number = index + 1
            laps.value =
                listOf(
                    Lap(number = number, lapMillis = 1_000L, totalMillis = number * 1_000L)
                ) + laps.value
            composeTestRule.waitForIdle()
        }

        composeTestRule
            .onNodeWithTag(StopwatchTestTags.lapRow(LAPS_WELL_PAST_A_SCREENFUL))
            .assertIsDisplayed()
    }

    @Test
    fun lap_is_only_available_while_running() {
        setContent(mutableStateOf(emptyList()), status = StopwatchStatus.RUNNING)

        composeTestRule.onNodeWithTag(StopwatchTestTags.LAP).assertIsEnabled()
        composeTestRule.onNodeWithTag(StopwatchTestTags.RESET).assertIsNotEnabled()
    }

    @Test
    fun reset_is_only_available_while_paused() {
        setContent(mutableStateOf(emptyList()), status = StopwatchStatus.PAUSED)

        composeTestRule.onNodeWithTag(StopwatchTestTags.RESET).assertIsEnabled()
        composeTestRule.onNodeWithTag(StopwatchTestTags.LAP).assertIsNotEnabled()
    }

    private fun setContent(
        laps: androidx.compose.runtime.MutableState<List<Lap>>,
        status: StopwatchStatus = StopwatchStatus.RUNNING,
    ) {
        composeTestRule.setContent {
            StopwatchTheme {
                StopwatchContent(
                    status = status,
                    laps = laps.value,
                    highlights = LapHighlights(),
                    elapsedMillis = { 12_340L },
                    onStart = {},
                    onPause = {},
                    onLap = {},
                    onReset = {},
                )
            }
        }
    }

    private companion object {
        const val LAPS_WELL_PAST_A_SCREENFUL = 14
    }
}
