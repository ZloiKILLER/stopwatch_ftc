package com.ftc.stopwatch.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ftc.stopwatch.R
import com.ftc.stopwatch.domain.Lap
import com.ftc.stopwatch.domain.StopwatchStatus
import com.ftc.stopwatch.ui.theme.LocalStopwatchColors

/** Stable identifiers for UI tests. */
internal object StopwatchTestTags {
    const val DIAL = "timer_dial"
    const val START_PAUSE = "start_pause_button"
    const val LAP = "lap_button"
    const val RESET = "reset_button"
    const val LAP_LIST = "lap_list"

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

    BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
        // Sizing the dial from the available height is what guarantees the lap list always keeps a
        // usable slice of the screen, including on short screens and in landscape.
        val dialDiameter = (maxHeight * 0.30f).coerceIn(140.dp, 260.dp)
        val gap = if (maxHeight < 640.dp) 16.dp else 28.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppHeader()
            Spacer(Modifier.height(gap))
            TimerDial(elapsedMillis = elapsedMillis, diameter = dialDiameter)
            Spacer(Modifier.height(gap))
            ControlRow(
                status = status,
                onStart = onStart,
                onPause = onPause,
                onLap = onLap,
                onReset = onReset,
            )
            Spacer(Modifier.height(gap))
            LapSection(laps = laps, highlights = highlights, modifier = Modifier.weight(1f))
        }
    }
}

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
        TimerReadout(elapsedMillis)
    }
}

@Composable
private fun TimerReadout(elapsedMillis: () -> Long) {
    val colors = LocalStopwatchColors.current
    // The state read lands here, so this is the only composable that re-runs per frame.
    val parts = timePartsOf(elapsedMillis())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text =
                    if (parts.hasHours) "${parts.hours}:${parts.minutes}:${parts.seconds}"
                    else "${parts.minutes}:${parts.seconds}",
                color = colors.textPrimary,
                fontSize = if (parts.hasHours) 40.sp else 50.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1.5).sp,
            )
            Text(
                text = ".${parts.hundredths}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (parts.hasHours) 22.sp else 26.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.elapsed_time),
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun ControlRow(
    status: StopwatchStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = withHaptics(onLap),
            enabled = running,
            shape = CircleShape,
            colors = secondaryColors,
            border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = if (running) 1f else 0.3f)),
            modifier = Modifier.weight(1f).height(56.dp).testTag(StopwatchTestTags.LAP),
        ) {
            Text(text = stringResource(R.string.lap), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
            modifier = Modifier.weight(1f).height(56.dp).testTag(StopwatchTestTags.RESET),
        ) {
            Text(
                text = stringResource(R.string.reset),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
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
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag(StopwatchTestTags.LAP_LIST),
        ) {
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
 * Column split for the lap table. The lap column carries the number plus a FASTEST/SLOWEST tag, so
 * it needs noticeably more than an even share to keep the tag on one line in every language.
 */
private const val LAP_COLUMN_WEIGHT = 1.6f
private const val TIME_COLUMN_WEIGHT = 2f
