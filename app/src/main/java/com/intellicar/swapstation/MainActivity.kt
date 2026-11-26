package com.intellicar.swapstation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.intellicar.swapstation.presentation.live.LiveServerScreen
import com.intellicar.swapstation.presentation.theme.SwapStationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwapStationTheme {
                LiveServerScreen()
            }
        }
    }
}
