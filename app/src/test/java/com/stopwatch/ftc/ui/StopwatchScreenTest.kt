package com.stopwatch.ftc.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stopwatch.ftc.domain.Lap
import com.stopwatch.ftc.domain.StopwatchStatus
import com.stopwatch.ftc.ui.theme.StopwatchTheme
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

    /**
     * A phone in landscape has barely 400dp of height. Stacking the dial above the list would leave
     * the list unusable, so the layout has to switch to two panes and keep everything on screen.
     */
    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun landscape_phone_keeps_the_dial_and_the_laps_usable() {
        assertEverythingVisible(laps = threeLaps())
    }

    @Test
    @Config(qualifiers = "sw600dp-w1280dp-h800dp")
    fun tablet_landscape_keeps_the_dial_and_the_laps_usable() {
        assertEverythingVisible(laps = threeLaps())
    }

    @Test
    @Config(qualifiers = "sw600dp-w800dp-h1280dp")
    fun tablet_portrait_keeps_the_dial_and_the_laps_usable() {
        assertEverythingVisible(laps = threeLaps())
    }

    /** Galaxy Fold, folded: a tall, very narrow window. */
    @Test
    @Config(qualifiers = "sw320dp-w320dp-h750dp")
    fun folded_phone_keeps_the_dial_and_the_laps_usable() {
        assertEverythingVisible(laps = threeLaps())
    }

    /** Galaxy Fold, unfolded: close to square. */
    @Test
    @Config(qualifiers = "sw600dp-w674dp-h841dp")
    fun unfolded_phone_keeps_the_dial_and_the_laps_usable() {
        assertEverythingVisible(laps = threeLaps())
    }

    /** A maximised desktop window, far wider than any phone. */
    @Test
    @Config(qualifiers = "sw600dp-w1920dp-h1080dp")
    fun desktop_window_keeps_the_dial_and_the_laps_usable() {
        assertEverythingVisible(laps = threeLaps())
    }

    private fun assertEverythingVisible(laps: List<Lap>) {
        setContent(mutableStateOf(laps))

        listOf(
                StopwatchTestTags.DIAL,
                StopwatchTestTags.LAP,
                StopwatchTestTags.START_PAUSE,
                StopwatchTestTags.RESET,
                StopwatchTestTags.lapRow(laps.first().number),
            )
            .forEach { composeTestRule.onNodeWithTag(it).assertIsDisplayed() }
    }

    private fun threeLaps() =
        (3 downTo 1).map { number ->
            Lap(number = number, lapMillis = 1_000L, totalMillis = number * 1_000L)
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
