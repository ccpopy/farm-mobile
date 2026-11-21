package com.qqfarm.mobile.data.network

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qqfarm.mobile.MainActivity
import com.qqfarm.mobile.R
import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.ActivityNotifyGroup
import com.qqfarm.mobile.data.model.ActivityNotifyType
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.prefs.StoredConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationCandidate(
    val type: ActivityNotifyType,
    val title: String,
    val body: String,
)

private data class RollingNotificationLine(
    val timestampMs: Long,
    val line: String,
)

class ActivityNotificationManager(
    private val context: Context,
    private val apiClient: FarmApiClient,
) {
    companion object {
        const val CHANNEL_ID = "farm_activity"
        private const val POLL_INTERVAL_MS = 20_000L
        private const val HISTORY_TTL_MS = 2 * 60 * 60 * 1000L
        private const val MAX_GROUP_LINES = 5
        private const val MAX_SEEN_KEYS = 500
        private const val MAX_UNCOLLAPSED_SLOTS = 6
        private const val UNCOLLAPSED_BASE_ID = 2200

        private val TRAILING_ID_LIST_REGEX = Regex("""\s*\(\d+(?:,\s*\d+)*\)\s*$""")
        private val FERT_SCOPE_REGEX = Regex("""（范围:[^）]*）""")

        fun hasPostPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun areNotificationsEnabled(context: Context): Boolean {
            return hasPostPermission(context) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        fun openSystemNotificationSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    private val lock = Any()
    private var config: StoredConfig = StoredConfig()
    private var socketConnected: Boolean = false
    private var accountNameById: Map<String, String> = emptyMap()
    private var startedListeningAtMs: Long = System.currentTimeMillis()
    private val seenKeys = LinkedHashSet<String>()
    private val groupHistory = mutableMapOf<ActivityNotifyGroup, ArrayDeque<RollingNotificationLine>>()
    private var uncollapsedSlot = 0

    fun updateConfig(
        nextConfig: StoredConfig,
        accounts: List<AccountItem>,
        isSocketConnected: Boolean,
    ) {
        val shouldPoll: Boolean
        synchronized(lock) {
            val wasEnabled = config.activityPushEnabled
            val nowEnabled = nextConfig.activityPushEnabled && nextConfig.isAuthenticated
            if (!wasEnabled && nowEnabled) {
                startedListeningAtMs = System.currentTimeMillis()
                seenKeys.clear()
                groupHistory.clear()
            } else if (!nowEnabled) {
                groupHistory.clear()
            }
            config = nextConfig
            socketConnected = isSocketConnected
            accountNameById = accounts.associate { acc ->
                acc.id to acc.name.ifBlank { acc.nick.ifBlank { acc.id } }
            }
            shouldPoll = nowEnabled && (nextConfig.notifyAllAccounts || !isSocketConnected)
        }

        if (shouldPoll) {
            startPollingIfNeeded()
        } else {
            stopPolling()
        }
    }

    fun onRealtimeLog(entry: RunLogEntry) {
        processEntry(entry)
    }

    fun clearRollingHistory() {
        synchronized(lock) {
            groupHistory.clear()
        }
    }

    fun stop() {
        stopPolling()
        synchronized(lock) {
            config = StoredConfig()
            groupHistory.clear()
        }
    }

    fun sendTestNotification(): Boolean {
        if (!areNotificationsEnabled(context)) return false
        val sample = NotificationCandidate(
            type = ActivityNotifyType.FARM_HARVEST,
            title = "农场收获",
            body = "收获 18 块土地（通知测试）",
        )
        return postNotification(
            candidate = sample,
            accountLabel = "",
            eventTs = System.currentTimeMillis(),
            collapse = true,
            silentUpdate = false,
        )
    }

    @Synchronized
    private fun startPollingIfNeeded() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val activeCfg: StoredConfig
                synchronized(lock) { activeCfg = config }
                if (!activeCfg.activityPushEnabled || !activeCfg.isAuthenticated) break
                try {
                    val logs = apiClient.fetchLogs(accountId = "all", limit = 30)
                    logs.sortedBy { it.ts }.forEach { processEntry(it) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Transient poll errors will retry on the next cycle.
                }
            }
        }
    }

    @Synchronized
    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun processEntry(entry: RunLogEntry) {
        val activeCfg: StoredConfig
        val accMap: Map<String, String>
        val sinceMs: Long
        synchronized(lock) {
            activeCfg = config
            accMap = accountNameById
            sinceMs = startedListeningAtMs
        }

        if (!activeCfg.activityPushEnabled || !activeCfg.isAuthenticated) return
        if (!activeCfg.notifyAllAccounts &&
            activeCfg.currentAccountId.isNotBlank() &&
            entry.accountId.isNotBlank() &&
            entry.accountId != activeCfg.currentAccountId
        ) {
            return
        }
        val effectiveTs = if (entry.ts > 0L) entry.ts else System.currentTimeMillis()
        if (effectiveTs <= sinceMs) return
        if (!areNotificationsEnabled(context)) return

        val candidate = classify(entry, activeCfg.enabledNotifyTypes) ?: return
        val dedupKey = "${entry.accountId}|${entry.ts}|${entry.event}|${entry.msg}"
        synchronized(lock) {
            if (!seenKeys.add(dedupKey)) return
            while (seenKeys.size > MAX_SEEN_KEYS) {
                val oldest = seenKeys.firstOrNull() ?: break
                seenKeys.remove(oldest)
            }
        }

        val showAccountPrefix = accMap.size > 1 && entry.accountId.isNotBlank()
        val accountLabel = if (showAccountPrefix) accMap[entry.accountId].orEmpty() else ""

        postNotification(
            candidate = candidate,
            accountLabel = accountLabel,
            eventTs = effectiveTs,
            collapse = activeCfg.notifyCollapseEnabled,
            silentUpdate = activeCfg.notifySilentUpdate,
        )
    }

    private fun postNotification(
        candidate: NotificationCandidate,
        accountLabel: String,
        eventTs: Long,
        collapse: Boolean,
        silentUpdate: Boolean,
    ): Boolean {
        if (!hasPostPermission(context)) return false

        val group = candidate.type.group
        val contentIntent = PendingIntent.getActivity(
            context,
            group.notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val bodyLine = if (accountLabel.isNotBlank()) "[$accountLabel] ${candidate.body}" else candidate.body
        val timePrefix = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(eventTs))

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sprout)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        val notifyId: Int
        if (collapse) {
            val snapshotLines: List<RollingNotificationLine>
            synchronized(lock) {
                val deque = groupHistory.getOrPut(group) { ArrayDeque() }
                val cutoff = System.currentTimeMillis() - HISTORY_TTL_MS
                while (deque.isNotEmpty() && deque.first().timestampMs < cutoff) {
                    deque.removeFirst()
                }
                deque.addLast(RollingNotificationLine(eventTs, "$timePrefix $bodyLine"))
                while (deque.size > MAX_GROUP_LINES) {
                    deque.removeFirst()
                }
                snapshotLines = deque.toList()
            }

            notifyId = group.notificationId
            if (snapshotLines.size <= 1) {
                val singleTitle = if (accountLabel.isNotBlank()) {
                    "${candidate.title} · $accountLabel"
                } else {
                    candidate.title
                }
                builder.setContentTitle(singleTitle)
                    .setContentText(candidate.body)
            } else {
                val style = NotificationCompat.InboxStyle()
                    .setBigContentTitle("${group.label} · 最近 ${snapshotLines.size} 条")
                snapshotLines.asReversed().forEach { style.addLine(it.line) }
                builder.setContentTitle("${group.label} · 最近 ${snapshotLines.size} 条")
                    .setContentText(bodyLine)
                    .setNumber(snapshotLines.size)
                    .setStyle(style)
            }
            if (silentUpdate && snapshotLines.size > 1) {
                builder.setOnlyAlertOnce(true)
            }
        } else {
            synchronized(lock) {
                notifyId = UNCOLLAPSED_BASE_ID + (uncollapsedSlot++ % MAX_UNCOLLAPSED_SLOTS)
            }
            val singleTitle = if (accountLabel.isNotBlank()) {
                "${candidate.title} · $accountLabel"
            } else {
                candidate.title
            }
            builder.setContentTitle(singleTitle)
                .setContentText(candidate.body)
        }

        return try {
            NotificationManagerCompat.from(context).notify(notifyId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    internal fun classify(
        entry: RunLogEntry,
        enabledTypes: Set<String>,
    ): NotificationCandidate? {
        if (entry.isWarn) return null
        if (entry.module == "system" || entry.event == "runtime_diagnostics") return null
        val msg = entry.msg.trim()
        if (msg.isBlank()) return null

        val module = entry.module.trim()
        val event = entry.event.trim()
        val result = entry.result.trim()

        // 1. Farm events
        if (module == "farm") {
            if (event == "农场循环") {
                val careActions = entry.actions.filter {
                    it.startsWith("一键务农") || it.startsWith("单点务农")
                }
                if (careActions.isEmpty() || ActivityNotifyType.FARM_CARE.key !in enabledTypes) {
                    return null
                }
                val text = careActions.joinToString(" · ")
                return NotificationCandidate(
                    type = ActivityNotifyType.FARM_CARE,
                    title = "农场务农",
                    body = clampText(text),
                )
            }
            if (result != "ok") return null

            return when (event) {
                "收获作物", "施肥后收获作物" -> takeIfEnabled(
                    ActivityNotifyType.FARM_HARVEST,
                    enabledTypes,
                    "农场收获",
                    msg.replace("收获完成 ", "收获 "),
                )
                "种植种子" -> {
                    val short = msg
                        .replace("已按背包优先策略种植: ", "背包种植：")
                        .replace(TRAILING_ID_LIST_REGEX, "")
                        .replace("已在 ", "已种植 ")
                        .replace(" 块地种植", " 块地")
                    takeIfEnabled(ActivityNotifyType.FARM_PLANT, enabledTypes, "农场种植", short)
                }
                "施肥", "手动施肥", "一键施肥", "多季节施肥" -> {
                    val short = FERT_SCOPE_REGEX.replace(msg, "").trim()
                    takeIfEnabled(ActivityNotifyType.FARM_CARE, enabledTypes, "农场施肥", short)
                }
                "铲除植物", "土地维护" -> {
                    val short = TRAILING_ID_LIST_REGEX.replace(msg, "").trim()
                    takeIfEnabled(ActivityNotifyType.FARM_CARE, enabledTypes, "农场清理", short)
                }
                "解锁土地", "升级土地" -> {
                    takeIfEnabled(
                        ActivityNotifyType.FARM_LAND,
                        enabledTypes,
                        if (event == "解锁土地") "土地解锁" else "土地升级",
                        msg,
                    )
                }
                else -> null
            }
        }

        // 2. Friend visit events (split by sub-action preference: steal / help / bad)
        if (module == "friend") {
            if (event != "照顾好友" || result != "ok") return null
            val tokens = if (entry.actions.isNotEmpty()) {
                entry.actions
            } else {
                msg.substringAfter(": ", "").split("/").map { it.trim() }.filter { it.isNotBlank() }
            }
            if (tokens.isEmpty()) return null

            val kept = mutableListOf<String>()
            val matchedTypes = linkedSetOf<ActivityNotifyType>()
            for (token in tokens) {
                val subType = when {
                    token.startsWith("偷") -> ActivityNotifyType.FRIEND_STEAL
                    token.startsWith("放草") || token.startsWith("放虫") -> ActivityNotifyType.FRIEND_BAD
                    token.startsWith("一键务农") ||
                        token.contains("浇水") ||
                        token.contains("除草") ||
                        token.contains("除虫") ||
                        token.contains("帮忙") -> ActivityNotifyType.FRIEND_HELP
                    else -> null
                } ?: continue

                if (subType.key in enabledTypes) {
                    matchedTypes.add(subType)
                    val condensed = token
                        .replace(Regex("""一键务农(\d+)块/\d+项"""), "帮务农$1块")
                        .replace("(自动获得)", "")
                    kept.add(condensed)
                }
            }
            if (kept.isEmpty()) return null

            val primaryType = matchedTypes.first()
            val title = when {
                matchedTypes.size == 1 && primaryType == ActivityNotifyType.FRIEND_STEAL -> "好友偷菜"
                matchedTypes.size == 1 && primaryType == ActivityNotifyType.FRIEND_HELP -> "好友帮忙"
                matchedTypes.size == 1 && primaryType == ActivityNotifyType.FRIEND_BAD -> "好友捣乱"
                else -> "好友互动"
            }
            val friend = entry.friendName.ifBlank { msg.substringBefore(": ", "好友") }
            return NotificationCandidate(
                type = primaryType,
                title = title,
                body = clampText("$friend · ${kept.joinToString(" / ")}"),
            )
        }

        // 3. Ranch events
        if (module == "ranch") {
            if (result != "ok") return null
            return when (event) {
                "ranch_harvest" -> takeIfEnabled(ActivityNotifyType.RANCH_HARVEST, enabledTypes, "牧场收获", msg)
                "ranch_produce" -> takeIfEnabled(ActivityNotifyType.RANCH_PRODUCE, enabledTypes, "牧场生产", msg)
                "ranch_feed", "ranch_clean", "ranch_addGrass", "ranch_useCan" ->
                    takeIfEnabled(ActivityNotifyType.RANCH_CARE, enabledTypes, "牧场照料", msg)
                "ranch_buy", "ranch_place", "ranch_sell", "ranch_exchange",
                "ranch_upgrade", "ranch_completeUpgrade", "ranch_activateShed" ->
                    takeIfEnabled(ActivityNotifyType.RANCH_TRADE, enabledTypes, "牧场经营", msg)
                "ranch_steal" ->
                    takeIfEnabled(ActivityNotifyType.FRIEND_STEAL, enabledTypes, "牧场偷取", msg)
                "ranch_stroke", "ranch_cleanFriend" ->
                    takeIfEnabled(ActivityNotifyType.FRIEND_HELP, enabledTypes, "牧场帮忙", msg)
                "ranch_putMosquito" ->
                    takeIfEnabled(ActivityNotifyType.FRIEND_BAD, enabledTypes, "牧场捣乱", msg)
                "ranch_task_claim", "ranch_daily_share", "ranch_daily_benefit",
                "ranch_claimTasks", "ranch_claimIllustrated" ->
                    takeIfEnabled(ActivityNotifyType.TASK_REWARD, enabledTypes, "牧场奖励", msg)
                else -> null
            }
        }

        // 4. Warehouse sell events
        if (module == "warehouse") {
            if (event in setOf("sell_success", "sell_done") && result in setOf("ok", "unknown_gain")) {
                return takeIfEnabled(ActivityNotifyType.WAREHOUSE_SELL, enabledTypes, "仓库出售", msg)
            }
            return null
        }

        // 5. Task & daily gift events
        if (module == "task") {
            if (result !in setOf("ok", "partial")) return null
            if (event in setOf(
                    "领取任务",
                    "领取活跃奖励",
                    "图鉴奖励",
                    "email_rewards",
                    "mall_free_gifts",
                    "daily_share",
                    "vip_daily_gift",
                    "month_card_gift",
                )
            ) {
                return takeIfEnabled(ActivityNotifyType.TASK_REWARD, enabledTypes, "任务与礼包", msg)
            }
            return null
        }

        return null
    }

    private fun takeIfEnabled(
        type: ActivityNotifyType,
        enabledTypes: Set<String>,
        title: String,
        body: String,
    ): NotificationCandidate? {
        if (type.key !in enabledTypes) return null
        return NotificationCandidate(
            type = type,
            title = title,
            body = clampText(body),
        )
    }

    private fun clampText(raw: String, maxLen: Int = 56): String {
        val clean = raw.replace('\n', ' ').replace(Regex("""\s+"""), " ").trim()
        return if (clean.length <= maxLen) clean else clean.take(maxLen - 1) + "…"
    }
}
