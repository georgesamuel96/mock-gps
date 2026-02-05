package com.learning.mockgps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.learning.mockgps.ui.theme.MockGPSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MockGPSTheme {
                val viewModel: MockLocationViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                MockGpsApp(
                    uiState = uiState,
                    actions = viewModel.actions
                )
            }
        }
    }
}
