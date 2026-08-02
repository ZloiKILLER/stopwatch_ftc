package com.ftc.stopwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.ftc.stopwatch.ui.StopwatchScreen
import com.ftc.stopwatch.ui.StopwatchViewModel
import com.ftc.stopwatch.ui.theme.LocalStopwatchColors
import com.ftc.stopwatch.ui.theme.StopwatchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StopwatchViewModel by viewModels { StopwatchViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
