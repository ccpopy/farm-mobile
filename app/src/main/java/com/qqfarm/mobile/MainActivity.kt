package com.qqfarm.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qqfarm.mobile.ui.navigation.FarmNavHost
import com.qqfarm.mobile.ui.state.AppSessionViewModel
import com.qqfarm.mobile.ui.theme.FarmBotTheme

class AppUpdateFileProvider : FileProvider()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sessionViewModel: AppSessionViewModel = viewModel()
            val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
            FarmBotTheme(
                themeMode = sessionState.config.themeMode,
                dynamicColor = sessionState.config.dynamicColor,
                imageLoader = sessionViewModel.container.imageLoader,
                assetResolver = { raw -> sessionViewModel.apiClient.resolveAssetUrl(raw) },
            ) {
                FarmNavHost(viewModel = sessionViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as FarmBotApplication).container.activityNotificationManager.clearRollingHistory()
    }
}
