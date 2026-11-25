package com.qqfarm.mobile.platform

import com.qqfarm.mobile.data.network.FarmApiClient

/** 应用级依赖容器（双端共用） */
class AppContainer(
    val preferences: com.qqfarm.mobile.data.prefs.AppPreferences = com.qqfarm.mobile.data.prefs.AppPreferences(createPreferencesDataStore()),
    val apiClient: FarmApiClient = FarmApiClient(),
    val socketManager: com.qqfarm.mobile.data.network.RealtimeSocketManager = com.qqfarm.mobile.data.network.RealtimeSocketManager(),
) {
    val imageLoader: coil3.ImageLoader = createFarmImageLoader()
    val offlineMonitor: OfflineEventsMonitor = createOfflineMonitor(apiClient)
    val activityNotifier: ActivityNotifier = createActivityNotifier(apiClient)
}
