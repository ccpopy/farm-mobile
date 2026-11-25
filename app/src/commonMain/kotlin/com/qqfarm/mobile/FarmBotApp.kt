package com.qqfarm.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.qqfarm.mobile.platform.AppContainer
import com.qqfarm.mobile.ui.navigation.FarmNavHost
import com.qqfarm.mobile.ui.state.AppSessionViewModel
import com.qqfarm.mobile.ui.theme.FarmBotTheme

/**
 * 双端统一应用入口。
 * Android 由 MainActivity 以 Activity + ViewModel 工厂方式组装；
 * iOS 由 MainViewController 直接传入 AppContainer。
 */
@Composable
fun FarmBotApp(container: AppContainer) {
    val sessionViewModel = remember { AppSessionViewModel(container) }
    val sessionState by sessionViewModel.uiState.collectAsState()
    FarmBotTheme(
        themeMode = sessionState.config.themeMode,
        dynamicColor = sessionState.config.dynamicColor,
        customAppearance = sessionState.config.customAppearance,
        imageLoader = sessionViewModel.container.imageLoader,
        assetResolver = { raw -> sessionViewModel.apiClient.resolveAssetUrl(raw) },
    ) {
        FarmNavHost(viewModel = sessionViewModel)
    }
}
