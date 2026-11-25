package com.qqfarm.mobile.ui.state

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel

// 更新检查 UI 接缝：Android 有完整的 APK 下载/安装实现；iOS 为空实现
@Composable
expect fun rememberAppUpdateViewModel(): AppUpdateViewModel

expect class AppUpdateViewModel : ViewModel {
    fun check(config: com.qqfarm.mobile.data.prefs.StoredConfig)
    fun saveGithubToken(token: String)
    fun setPreferredDownloadSettings(settings: com.qqfarm.mobile.data.prefs.UpdateDownloadSettings)
}
