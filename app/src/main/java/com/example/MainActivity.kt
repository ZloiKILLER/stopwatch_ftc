package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

/**
 * Main activity of the Stopwatch App.
 * Manages runtime permission requests for Notifications on Android 13+.
 * Employs Jetpack Compose to display a highly aesthetic Neon Sports-Style Stopwatch.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: StopwatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StopwatchManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = LocalStopwatchColors.current.background
                ) { innerPadding ->
                    StopwatchScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val stopwatchColors = LocalStopwatchColors.current

    // States observed from the ViewModel
    val state by viewModel.state.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val timeParts by viewModel.formattedTime.collectAsState()
    val laps by viewModel.laps.collectAsState()
    val extremums by viewModel.lapExtremums.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(stopwatchColors.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header (Professional Polish Theme with localizations)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = "Stopwatch Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.app_name),
                    color = stopwatchColors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Center Dial Timer Area (Professional Polish Theme with localizations)
        Box(
            modifier = Modifier
                .size(260.dp)
                .testTag("timer_dial_container"),
            contentAlignment = Alignment.Center
        ) {
            // Elegant progress arc
            val sweepAngle = ((elapsedTime % 60000) / 60000f) * 360f
            val primaryColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background subtle ring
                drawCircle(
                    color = stopwatchColors.dividerColor.copy(alpha = 0.2f),
                    radius = size.minDimension / 2.1f,
                    style = Stroke(width = 4.dp.toPx())
                )

                // Professional theme progress arc
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    ),
                    size = size / 1.05f
                )
            }

            // Central digital display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // The Big Monospace Timer Display (Hours:Minutes:Seconds)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    val displayTime = if (timeParts.hours != "00") {
                        "${timeParts.hours}:${timeParts.minutes}:${timeParts.seconds}"
                    } else {
                        "${timeParts.minutes}:${timeParts.seconds}"
                    }
                    Text(
                        text = displayTime,
                        color = stopwatchColors.textPrimary,
                        fontSize = if (timeParts.hours != "00") 44.sp else 54.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1.5).sp
                    )

                    // Hundredths of a second
                    Text(
                        text = ".${timeParts.hundredths}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = if (timeParts.hours != "00") 24.sp else 28.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Underline decorative subtitle
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.elapsed_time),
                    color = stopwatchColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions Control Panel (Professional Polish Theme with localizations)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LAP Button (Secondary action on the left)
            val isLapEnabled = state == StopwatchState.RUNNING
            val lapAlpha by animateFloatAsState(targetValue = if (isLapEnabled) 1.0f else 0.3f, label = "lap_alpha")
            val lapBgColor = if (isLapEnabled) {
                stopwatchColors.dividerColor.copy(alpha = 0.4f)
            } else {
                stopwatchColors.dividerColor.copy(alpha = 0.08f)
            }
            val lapBorderColor = if (isLapEnabled) {
                stopwatchColors.textSecondary
            } else {
                stopwatchColors.dividerColor.copy(alpha = 0.2f)
            }
            val lapBorderWidth = if (isLapEnabled) 1.5.dp else 1.dp

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(lapBgColor)
                    .border(
                        width = lapBorderWidth,
                        color = lapBorderColor,
                        shape = CircleShape
                    )
                    .clickable(enabled = isLapEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.lap()
                    }
                    .testTag("lap_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.lap),
                    color = stopwatchColors.textPrimary.copy(alpha = lapAlpha),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // PRIMARY PLAY / PAUSE Button (Center block button)
            val isRunning = state == StopwatchState.RUNNING
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isRunning) {
                            viewModel.pause()
                        } else {
                            viewModel.start()
                        }
                    }
                    .testTag("start_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = androidx.compose.ui.res.stringResource(
                        id = if (isRunning) R.string.pause else R.string.start
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // RESET Button (Secondary action on the right)
            val isResetEnabled = state == StopwatchState.PAUSED
            val resetAlpha by animateFloatAsState(targetValue = if (isResetEnabled) 1.0f else 0.3f, label = "reset_alpha")
            val resetBgColor = if (isResetEnabled) {
                stopwatchColors.dividerColor.copy(alpha = 0.4f)
            } else {
                stopwatchColors.dividerColor.copy(alpha = 0.08f)
            }
            val resetBorderColor = if (isResetEnabled) {
                stopwatchColors.textSecondary
            } else {
                stopwatchColors.dividerColor.copy(alpha = 0.2f)
            }
            val resetBorderWidth = if (isResetEnabled) 1.5.dp else 1.dp

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(resetBgColor)
                    .border(
                        width = resetBorderWidth,
                        color = resetBorderColor,
                        shape = CircleShape
                    )
                    .clickable(enabled = isResetEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.reset()
                    }
                    .testTag("reset_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.reset),
                    color = stopwatchColors.textPrimary.copy(alpha = resetAlpha),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Laps List Title Header (Professional Polish Theme with localizations)
        if (laps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.header_lap),
                    color = stopwatchColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.header_lap_time),
                    color = stopwatchColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.header_total_time),
                    color = stopwatchColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider(color = stopwatchColors.dividerColor.copy(alpha = 0.4f), thickness = 1.dp)
        }

        // Laps List LazyColumn (Professional Polish Theme with localizations)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (laps.isEmpty()) {
                // Aesthetic Empty State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = "No laps",
                        tint = stopwatchColors.textSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.empty_laps_title),
                        color = stopwatchColors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.empty_laps_desc),
                        color = stopwatchColors.textSecondary.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    items(
                        items = laps,
                        key = { it.id }
                    ) { lap ->
                        val isFastest = lap.id == extremums.fastestLapId
                        val isSlowest = lap.id == extremums.slowestLapId

                        val itemBgColor = when {
                            isFastest -> stopwatchColors.lapFastestBg
                            isSlowest -> stopwatchColors.lapSlowestBg
                            else -> Color.Transparent
                        }

                        val timeColor = when {
                            isFastest -> stopwatchColors.lapFastestText
                            isSlowest -> stopwatchColors.lapSlowestText
                            else -> stopwatchColors.textPrimary
                        }

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(itemBgColor)
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .testTag("lap_item_${lap.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Lap Number
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format("%02d", lap.id),
                                        color = stopwatchColors.textSecondary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (isFastest) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(stopwatchColors.lapFastestText.copy(alpha = 0.15f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = androidx.compose.ui.res.stringResource(id = R.string.tag_fastest),
                                                color = stopwatchColors.lapFastestText,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (isSlowest) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(stopwatchColors.lapSlowestText.copy(alpha = 0.15f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = androidx.compose.ui.res.stringResource(id = R.string.tag_slowest),
                                                color = stopwatchColors.lapSlowestText,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Lap Time
                                Text(
                                    text = formatDuration(lap.lapTime),
                                    color = timeColor,
                                    fontWeight = if (isFastest || isSlowest) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(2f),
                                    textAlign = TextAlign.Center
                                )

                                // Total Time
                                Text(
                                    text = formatDuration(lap.totalTime),
                                    color = stopwatchColors.textPrimary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(2f),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(
                                color = stopwatchColors.dividerColor.copy(alpha = 0.15f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-fidelity formatter to format durations into human readable intervals.
 */
fun formatDuration(ms: Long): String {
    val hundredths = (ms % 1000) / 10
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 60000) % 60
    val hours = (ms / 3600000)

    return if (hours > 0) {
        String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
    } else {
        String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
    }
}
