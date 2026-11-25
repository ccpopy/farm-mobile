package com.qqfarm.mobile.platform

import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.AccountLogEvent
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.data.prefs.StoredConfig

actual fun createOfflineMonitor(apiClient: FarmApiClient): OfflineEventsMonitor = NoopOfflineMonitor

actual fun createActivityNotifier(apiClient: FarmApiClient): ActivityNotifier = NoopActivityNotifier

private object NoopOfflineMonitor : OfflineEventsMonitor {
    override fun onAccountLog(ev: AccountLogEvent) {}
    override fun stop() {}
    override fun updateSubscription(enabled: Boolean, accountIds: List<String>) {}
}

private object NoopActivityNotifier : ActivityNotifier {
    override fun updateConfig(nextConfig: StoredConfig, accounts: List<AccountItem>, isSocketConnected: Boolean) {}
    override fun clearRollingHistory() {}
}
