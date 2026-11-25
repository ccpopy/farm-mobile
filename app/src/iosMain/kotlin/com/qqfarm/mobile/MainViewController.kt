package com.qqfarm.mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.qqfarm.mobile.platform.AppContainer

fun MainViewController() = ComposeUIViewController {
    FarmBotApp(AppContainer())
}
