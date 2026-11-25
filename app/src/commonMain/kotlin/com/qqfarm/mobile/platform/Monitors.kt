package com.qqfarm.mobile.platform

import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.AccountLogEvent
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.data.prefs.StoredConfig

/** 离线提醒监控门面（Android: 系统通知；iOS: 空实现） */
interface OfflineEventsMonitor {
    fun onAccountLog(ev: AccountLogEvent)
    fun stop()
    fun updateSubscription(enabled: Boolean, accountIds: List<String>)
}

/** 农牧动态通知门面（Android: 系统通知；iOS: 空实现） */
interface ActivityNotifier {
    fun updateConfig(nextConfig: StoredConfig, accounts: List<AccountItem>, isSocketConnected: Boolean)
    fun onRealtimeLog(entry: com.qqfarm.mobile.data.model.RunLogEntry) {}
    fun sendTestNotification(): Boolean = false
    fun stop() {}
    fun clearRollingHistory() {}
}

expect fun createOfflineMonitor(apiClient: FarmApiClient): OfflineEventsMonitor

expect fun createActivityNotifier(apiClient: FarmApiClient): ActivityNotifier

/** 前台日志策略：与 androidMain 的 OfflineEventMonitor 共用 RunLogEntry 判定 */
fun shouldNotifyOffline(entry: RunLogEntry): Boolean = entry.event.startsWith("account_")
