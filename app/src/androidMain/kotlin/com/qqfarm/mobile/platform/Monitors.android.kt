package com.qqfarm.mobile.platform

import android.content.Context
import com.qqfarm.mobile.data.network.FarmApiClient

actual fun createOfflineMonitor(apiClient: FarmApiClient): OfflineEventsMonitor =
    OfflineEventsMonitorAdapter(
        com.qqfarm.mobile.data.network.OfflineEventMonitor(
            PlatformGlobals.appContext as? Context
                ?: throw IllegalStateException("Application context not initialized"),
            apiClient,
        ),
    )

actual fun createActivityNotifier(apiClient: FarmApiClient): ActivityNotifier =
    ActivityNotifierAdapter(
        com.qqfarm.mobile.data.network.ActivityNotificationManager(
            PlatformGlobals.appContext as? Context
                ?: throw IllegalStateException("Application context not initialized"),
            apiClient,
        ),
    )

internal class OfflineEventsMonitorAdapter(private val impl: com.qqfarm.mobile.data.network.OfflineEventMonitor) :
    OfflineEventsMonitor {
    override fun stop() = impl.stop()
    override fun updateSubscription(enabled: Boolean, accountIds: List<String>) =
        impl.updateSubscription(enabled, accountIds)
}

internal class ActivityNotifierAdapter(private val impl: com.qqfarm.mobile.data.network.ActivityNotificationManager) :
    ActivityNotifier {
    override fun updateConfig(
        nextConfig: com.qqfarm.mobile.data.prefs.StoredConfig,
        accounts: List<com.qqfarm.mobile.data.model.AccountItem>,
        isSocketConnected: Boolean,
    ) = impl.updateConfig(nextConfig, accounts, isSocketConnected)

    override fun onRealtimeLog(entry: com.qqfarm.mobile.data.model.RunLogEntry) = impl.onRealtimeLog(entry)

    override fun sendTestNotification(): Boolean = impl.sendTestNotification()

    override fun stop() = impl.stop()

    override fun clearRollingHistory() = impl.clearRollingHistory()
}
