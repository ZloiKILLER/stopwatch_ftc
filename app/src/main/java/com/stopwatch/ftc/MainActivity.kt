package com.stopwatch.ftc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.stopwatch.ftc.ui.StopwatchScreen
import com.stopwatch.ftc.ui.StopwatchViewModel
import com.stopwatch.ftc.ui.theme.LocalStopwatchColors
import com.stopwatch.ftc.ui.theme.StopwatchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StopwatchViewModel by viewModels { StopwatchViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Has to run before super.onCreate so the launch window is handed over rather than replaced.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the launch window until the saved measurement is back, so the first frame the user
        // sees is already the real one instead of an empty 00:00 that jumps a moment later. The
        // restore always completes, failures included, so this cannot wedge the app on the splash.
        splashScreen.setKeepOnScreenCondition { !viewModel.isRestored.value }

        enableEdgeToEdge()
        setContent {
            StopwatchTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = LocalStopwatchColors.current.background,
                ) { innerPadding ->
                    StopwatchScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
