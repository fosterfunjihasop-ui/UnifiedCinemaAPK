package com.example.unifiedcinema

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import com.example.unifiedcinema.di.AppContainer
import com.example.unifiedcinema.ui.screens.MainScreen
import com.example.unifiedcinema.ui.theme.UnifiedCinemaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnifiedCinemaTheme {
                MainScreen(
                    container = AppContainer.getInstance(LocalContext.current)
                )
            }
        }
    }
}