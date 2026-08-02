package com.stopwatch.ftc.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stopwatch.ftc.R
import com.stopwatch.ftc.domain.Lap
import com.stopwatch.ftc.domain.StopwatchStatus
import com.stopwatch.ftc.ui.theme.LocalStopwatchColors

/** Stable identifiers for UI tests. */
internal object StopwatchTestTags {
    const val DIAL = "timer_dial"
    const val START_PAUSE = "start_pause_button"
    const val LAP = "lap_button"
    const val RESET = "reset_button"

    fun lapRow(number: Int) = "lap_row_$number"
}

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel, modifier: Modifier = Modifier) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()

    // Kept as State and deliberately not read here: only the composables that actually call the
    // lambda re-run per frame, which keeps the header, the buttons and the lap list off the
    // 60-times-a-second path.
    val elapsed = rememberElapsedMillis(status, viewModel::elapsedMillis)

    CheckpointOnStop(viewModel::checkpoint)

    StopwatchContent(
        status = status,
        laps = laps,
        highlights = highlights,
        elapsedMillis = { elapsed.value },
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onLap = viewModel::lap,
        onReset = viewModel::reset,
        modifier = modifier,
    )
}

@Composable
internal fun StopwatchContent(
    status: StopwatchStatus,
    laps: List<Lap>,
    highlights: LapHighlights,
    elapsedMillis: () -> Long,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStopwatchColors.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Every measurement comes from the window rather than the device, so a resized window, a
        // folding screen and a desktop pane all get the layout that fits what they currently are.
        val windowWidth = maxWidth
        val windowHeight = maxHeight

        // Width alone is not enough: a tablet held upright is wide but also very tall, and there
        // the stacked layout uses the space better than two half-empty columns.
        val twoPane =
            windowWidth >= TWO_PANE_MIN_WIDTH && windowWidth >= windowHeight * TWO_PANE_MIN_RATIO
        val dial: @Composable (Modifier) -> Unit = { paneModifier ->
            DialPane(
                status = status,
                elapsedMillis = elapsedMillis,
                onStart = onStart,
                onPause = onPause,
                onLap = onLap,
                onReset = onReset,
                availableHeight = if (twoPane) windowHeight else windowHeight * 0.9f,
                modifier = paneModifier,
            )
        }

        Column(
            modifier =
                Modifier.widthIn(max = MAX_CONTENT_WIDTH)
                    .fillMaxSize()
                    .padding(horizontal = if (twoPane) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The header spans the whole window in both layouts, so the two panes start from a
            // shared baseline instead of drifting apart.
            AppHeader()

            if (twoPane) {
                Row(Modifier.weight(1f)) {
                    dial(Modifier.weight(1f).fillMaxHeight())
                    Spacer(Modifier.width(24.dp))
                    LapSection(
                        laps = laps,
                        highlights = highlights,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Spacer(Modifier.height(verticalGap(windowHeight)))
                dial(Modifier.fillMaxWidth())
                Spacer(Modifier.height(verticalGap(windowHeight)))
                LapSection(laps = laps, highlights = highlights, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Dial and buttons: the part that stays together however the window is shaped. */
@Composable
private fun DialPane(
    status: StopwatchStatus,
    elapsedMillis: () -> Long,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    availableHeight: Dp,
    modifier: Modifier = Modifier,
) {
    // Centred so the pane stays balanced when a tall two-pane window gives it more room than the
    // dial and buttons need. In the stacked layout the box wraps its content and this is a no-op.
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // The dial takes its size from whichever of the two axes runs out first, so it never
        // squeezes the lap list off a short screen or overflows a narrow pane.
        val dialDiameter =
            minOf(maxWidth * 0.85f, availableHeight * 0.34f).coerceIn(120.dp, 260.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimerDial(elapsedMillis = elapsedMillis, diameter = dialDiameter)
            Spacer(Modifier.height(verticalGap(availableHeight)))
            ControlRow(
                status = status,
                onStart = onStart,
                onPause = onPause,
                onLap = onLap,
                onReset = onReset,
                // Buttons stop growing well before a desktop pane does; stretched to 400dp each
                // they would read as a toolbar rather than a set of controls.
                modifier = Modifier.widthIn(max = MAX_CONTROL_ROW_WIDTH),
            )
        }
    }
}

private fun verticalGap(availableHeight: Dp): Dp = if (availableHeight < 640.dp) 12.dp else 28.dp

@Composable
private fun AppHeader() {
    val colors = LocalStopwatchColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stopwatch),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun TimerDial(elapsedMillis: () -> Long, diameter: Dp, modifier: Modifier = Modifier) {
    val colors = LocalStopwatchColors.current
    val trackColor = colors.divider.copy(alpha = 0.2f)
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(diameter).testTag(StopwatchTestTags.DIAL),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Sampling the time inside the draw scope keeps the per-frame update in the draw phase:
            // this composable never has to recompose for the arc to advance.
            val sweep = (elapsedMillis() % ONE_MINUTE_MILLIS) / ONE_MINUTE_MILLIS.toFloat() * 360f
            val stroke = 5.dp.toPx()
            val ringDiameter = size.minDimension - stroke
            drawCircle(color = trackColor, radius = ringDiameter / 2f, style = Stroke(stroke))
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                // Explicit topLeft: the default is the canvas origin, which would push the arc off
                // centre relative to the track ring.
                topLeft =
                    Offset((size.width - ringDiameter) / 2f, (size.height - ringDiameter) / 2f),
                size = Size(ringDiameter, ringDiameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        TimerReadout(elapsedMillis, diameter)
    }
}

@Composable
private fun TimerReadout(elapsedMillis: () -> Long, diameter: Dp) {
    val colors = LocalStopwatchColors.current
    // The state read lands here, so this is the only composable that re-runs per frame.
    val parts = timePartsOf(elapsedMillis())

    // Scaled off the dial rather than fixed, so the readout keeps fitting inside the ring on every
    // window size. The factors come from the widest string each layout has to hold: "MM:SS.hh" is
    // eight monospace glyphs, "HH:MM:SS.hh" is eleven.
    val primarySize = diameter * (if (parts.hasHours) 0.140f else 0.195f)
    val fractionSize = primarySize * 0.52f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text =
                    if (parts.hasHours) "${parts.hours}:${parts.minutes}:${parts.seconds}"
                    else "${parts.minutes}:${parts.seconds}",
                color = colors.textPrimary,
                fontSize = primarySize.toSp(),
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1.5).sp,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = ".${parts.hundredths}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = fractionSize.toSp(),
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = diameter * 0.025f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.elapsed_time),
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            maxLines = 1,
        )
    }
}

/** Dp sizes converted for text, so the readout tracks the dial instead of the font scale alone. */
@Composable private fun Dp.toSp(): TextUnit = with(LocalDensity.current) { this@toSp.toSp() }

@Composable
private fun ControlRow(
    status: StopwatchStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val colors = LocalStopwatchColors.current
    val running = status == StopwatchStatus.RUNNING

    fun withHaptics(action: () -> Unit): () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        action()
    }

    val secondaryColors =
        ButtonDefaults.outlinedButtonColors(
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textPrimary.copy(alpha = 0.38f),
        )

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = withHaptics(onLap),
            enabled = running,
            shape = CircleShape,
            colors = secondaryColors,
            border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = if (running) 1f else 0.3f)),
            // Trimmed from the Material default so the label still fits on a 320dp folded screen.
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.weight(1f).height(56.dp).testTag(StopwatchTestTags.LAP),
        ) {
            Text(
                text = stringResource(R.string.lap),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }

        Button(
            onClick = withHaptics(if (running) onPause else onStart),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.width(96.dp).height(56.dp).testTag(StopwatchTestTags.START_PAUSE),
        ) {
            Icon(
                painter =
                    painterResource(if (running) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                contentDescription =
                    stringResource(if (running) R.string.pause else R.string.start),
                modifier = Modifier.size(28.dp),
            )
        }

        OutlinedButton(
            onClick = withHaptics(onReset),
            enabled = status == StopwatchStatus.PAUSED,
            shape = CircleShape,
            colors = secondaryColors,
            border =
                BorderStroke(
                    1.dp,
                    colors.textSecondary.copy(
                        alpha = if (status == StopwatchStatus.PAUSED) 1f else 0.3f
                    ),
                ),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.weight(1f).height(56.dp).testTag(StopwatchTestTags.RESET),
        ) {
            Text(
                text = stringResource(R.string.reset),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LapSection(laps: List<Lap>, highlights: LapHighlights, modifier: Modifier = Modifier) {
    val colors = LocalStopwatchColors.current

    if (laps.isEmpty()) {
        EmptyLaps(modifier)
        return
    }

    val listState = rememberLazyListState()
    // New laps are prepended. A LazyColumn keeps its scroll anchored on the item that was first
    // before the insert, so without this the newest lap lands just above the viewport and the list
    // looks frozen once it is long enough to scroll.
    LaunchedEffect(laps.size) { listState.animateScrollToItem(0) }

    Column(modifier) {
        LapColumnHeader()
        HorizontalDivider(color = colors.divider.copy(alpha = 0.4f), thickness = 1.dp)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(items = laps, key = Lap::number) { lap ->
                LapRow(
                    lap = lap,
                    accent =
                        when (lap.number) {
                            highlights.fastestLapNumber -> LapAccent.FASTEST
                            highlights.slowestLapNumber -> LapAccent.SLOWEST
                            else -> LapAccent.NONE
                        },
                )
                HorizontalDivider(
                    color = colors.divider.copy(alpha = 0.15f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun LapColumnHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LapHeaderCell(stringResource(R.string.header_lap), Modifier.weight(LAP_COLUMN_WEIGHT), TextAlign.Start)
        LapHeaderCell(stringResource(R.string.header_lap_time), Modifier.weight(TIME_COLUMN_WEIGHT), TextAlign.Center)
        LapHeaderCell(stringResource(R.string.header_total_time), Modifier.weight(TIME_COLUMN_WEIGHT), TextAlign.End)
    }
}

@Composable
private fun LapHeaderCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text = text,
        color = LocalStopwatchColors.current.textSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        textAlign = align,
        modifier = modifier,
    )
}

private enum class LapAccent {
    NONE,
    FASTEST,
    SLOWEST,
}

@Composable
private fun LapRow(lap: Lap, accent: LapAccent) {
    val colors = LocalStopwatchColors.current

    val background =
        when (accent) {
            LapAccent.FASTEST -> colors.lapFastestBackground
            LapAccent.SLOWEST -> colors.lapSlowestBackground
            LapAccent.NONE -> Color.Transparent
        }
    val lapTimeColor =
        when (accent) {
            LapAccent.FASTEST -> colors.lapFastestText
            LapAccent.SLOWEST -> colors.lapSlowestText
            LapAccent.NONE -> colors.textPrimary
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(background)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag(StopwatchTestTags.lapRow(lap.number)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(LAP_COLUMN_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = lap.number.toString().padStart(2, '0'),
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
            )
            if (accent != LapAccent.NONE) {
                Spacer(Modifier.width(8.dp))
                AccentTag(
                    text =
                        stringResource(
                            if (accent == LapAccent.FASTEST) R.string.tag_fastest
                            else R.string.tag_slowest
                        ),
                    color = lapTimeColor,
                )
            }
        }
        Text(
            text = formatDuration(lap.lapMillis),
            color = lapTimeColor,
            fontWeight = if (accent == LapAccent.NONE) FontWeight.Normal else FontWeight.SemiBold,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(TIME_COLUMN_WEIGHT),
        )
        Text(
            text = formatDuration(lap.totalMillis),
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(TIME_COLUMN_WEIGHT),
        )
    }
}

@Composable
private fun AccentTag(text: String, color: Color) {
    Box(
        modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyLaps(modifier: Modifier = Modifier) {
    val colors = LocalStopwatchColors.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stopwatch),
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.empty_laps_title),
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_laps_desc),
                color = colors.textSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
            )
        }
    }
}

/**
 * Samples the stopwatch once per rendered frame while it runs.
 *
 * [withFrameMillis] resumes only when the Compose frame clock actually produces a frame, so a
 * backgrounded app stops sampling on its own — there is no timer left spinning behind the UI.
 */
@Composable
private fun rememberElapsedMillis(
    status: StopwatchStatus,
    elapsedMillis: () -> Long,
): State<Long> {
    val currentElapsed by rememberUpdatedState(elapsedMillis)
    return produceState(initialValue = elapsedMillis(), status) {
        value = currentElapsed()
        if (status == StopwatchStatus.RUNNING) {
            while (true) {
                withFrameMillis { value = currentElapsed() }
            }
        }
    }
}

/** Writes a checkpoint when the app leaves the foreground. */
@Composable
private fun CheckpointOnStop(onStop: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStop by rememberUpdatedState(onStop)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) currentOnStop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private const val ONE_MINUTE_MILLIS = 60_000L

/**
 * Column split for the lap table. Even thirds: the lap column carries the number plus a
 * FASTEST/SLOWEST tag, and an even share is what keeps that tag on one line all the way down to a
 * 320dp folded screen and in the wordier languages.
 */
private const val LAP_COLUMN_WEIGHT = 1f
private const val TIME_COLUMN_WEIGHT = 1f

/**
 * Material's medium window width breakpoint. Above it there is room to put the dial and the lap
 * list side by side: a tablet, an unfolded foldable, a desktop window, or simply a phone held
 * sideways, where stacking them would leave the list a few pixels tall.
 */
private val TWO_PANE_MIN_WIDTH = 600.dp

/** How wide the window has to be relative to its height before splitting it is worth it. */
private const val TWO_PANE_MIN_RATIO = 0.8f

/** Past this the content stops stretching and centres, so a maximised desktop window stays legible. */
private val MAX_CONTENT_WIDTH = 1280.dp

/** Buttons stop growing here; beyond it they read as a toolbar rather than a set of controls. */
private val MAX_CONTROL_ROW_WIDTH = 420.dp
