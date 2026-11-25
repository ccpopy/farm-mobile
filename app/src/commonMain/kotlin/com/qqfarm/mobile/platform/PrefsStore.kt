package com.qqfarm.mobile.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/** 创建平台偏好存储：Android 用 Context.dataStore；iOS 用文件 DataStore */
expect fun createPreferencesDataStore(): DataStore<Preferences>
