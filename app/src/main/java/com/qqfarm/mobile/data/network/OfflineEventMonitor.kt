package com.qqfarm.mobile.data.network

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qqfarm.mobile.MainActivity
import com.qqfarm.mobile.R
import com.qqfarm.mobile.data.model.AccountLogEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OfflineEventMonitor(
    private val context: Context,
    private val apiClient: FarmApiClient,
) {
    companion object {
        const val CHANNEL_ID = "relogin_alerts"
        private val OFFLINE_ACTIONS = setOf("ws_400", "kickout_stop", "disconnect_stop", "offline_delete")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null
    private val seenLock = Any()
    private val seenKeys = LinkedHashSet<String>()

    @Volatile
    private var baselineInitialized: Boolean = false

    @Volatile
    private var trackedAccountIds: Set<String> = emptySet()

    @Volatile
    private var running: Boolean = false

    fun updateSubscription(enabled: Boolean, accountIds: List<String>) {
        trackedAccountIds = accountIds.filter { it.isNotBlank() }.toSet()
        if (!enabled) {
            stop()
            return
        }
        if (!running) {
            start()
        }
    }

    fun onAccountLog(event: AccountLogEvent) {
        if (!running || !baselineInitialized) return
        if (event.action !in OFFLINE_ACTIONS) return
        val ids = trackedAccountIds
        if (ids.isNotEmpty() && event.accountId.isNotBlank() && event.accountId !in ids) return
        if (markSeen(event.eventKey)) {
            postNotification(event)
        }
    }

    @Synchronized
    private fun start() {
        if (running) return
        running = true
        baselineInitialized = false
        synchronized(seenLock) { seenKeys.clear() }
        pollJob = scope.launch {
            while (isActive && running) {
                try {
                    val events = apiClient.fetchAccountLogs(limit = 40)
                    if (!baselineInitialized) {
                        for (ev in events) {
                            markSeen(ev.eventKey)
                        }
                        baselineInitialized = true
                    } else {
                        for (ev in events) {
                            onAccountLog(ev)
                        }
                    }
                    delay(25_000L)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    delay(12_000L)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        running = false
        baselineInitialized = false
        pollJob?.cancel()
        pollJob = null
    }

    private fun markSeen(key: String): Boolean {
        if (key.isBlank()) return false
        synchronized(seenLock) {
            if (!seenKeys.add(key)) return false
            while (seenKeys.size > 300) {
                val first = seenKeys.firstOrNull() ?: break
                seenKeys.remove(first)
            }
            return true
        }
    }

    private fun postNotification(event: AccountLogEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = when (event.action) {
            "ws_400" -> "登录凭证已失效"
            "kickout_stop" -> "账号被踢下线"
            "offline_delete" -> "账号持续离线已移除"
            else -> "账号连接已断开"
        }
        val bodyText = event.msg.ifBlank {
            "${event.accountName.ifBlank { event.accountId }} 需要重新扫码或更新登录凭证"
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val requestCode = (event.accountId.ifBlank { event.eventKey }).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sprout)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        }
    }
}
