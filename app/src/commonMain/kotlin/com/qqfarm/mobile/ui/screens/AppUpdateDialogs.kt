package com.qqfarm.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.ui.state.AppUpdateViewModel

// Android：APK 更新弹窗；iOS：空实现
@Composable
expect fun AppUpdateDialogs(updateModel: AppUpdateViewModel, config: StoredConfig)
