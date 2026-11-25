package com.qqfarm.mobile.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.data.prefs.UpdateDownloadSettings

@Composable
actual fun rememberAppUpdateViewModel(): AppUpdateViewModel = remember { AppUpdateViewModel() }

/** iOS 暂不支持应用内更新下载，全部空实现 */
actual class AppUpdateViewModel : ViewModel() {
    actual fun check(config: StoredConfig) {}
    actual fun saveGithubToken(token: String) {}
    actual fun setPreferredDownloadSettings(settings: UpdateDownloadSettings) {}
}
