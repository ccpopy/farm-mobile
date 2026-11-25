package com.qqfarm.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.ui.state.AppUpdateViewModel

@Composable
actual fun AppUpdateDialogs(updateModel: AppUpdateViewModel, config: StoredConfig) {
    // iOS 暂无应用内 APK 更新流程
}
