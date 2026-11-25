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
import com.qqfarm.mobile.data.model.MobileOfflineEvent
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
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    @Volatile
    private var trackedAccountIds: List<String> = emptyList()

    @Volatile
    private var running: Boolean = false

    fun updateSubscription(enabled: Boolean, accountIds: List<String>) {
        trackedAccountIds = accountIds.filter { it.isNotBlank() }
        if (!enabled || trackedAccountIds.isEmpty()) {
            stop()
            return
        }
        if (!running) {
            start()
        }
    }

    @Synchronized
    private fun start() {
        if (running) return
        running = true
        pollJob = scope.launch {
            var cursor = ""
            val seenEventIds = LinkedHashSet<String>()
            while (isActive && running) {
                val ids = trackedAccountIds
                if (ids.isEmpty()) {
                    delay(5000L)
                    continue
                }
                try {
                    val (nextCursor, reset, events) = apiClient.pollOfflineEvents(cursor, ids, 25)
                    if (reset) {
                        seenEventIds.clear()
                    }
                    cursor = nextCursor
                    for (ev in events) {
                        if (ev.eventId.isNotBlank() && seenEventIds.add(ev.eventId)) {
                            if (seenEventIds.size > 200) {
                                val first = seenEventIds.firstOrNull()
                                if (first != null) seenEventIds.remove(first)
                            }
                            postNotification(ev)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiException) {
                    if (e.errorCode == "MOBILE_ASSISTANT_DISABLED" || e.httpCode == 401 || e.httpCode == 409) {
                        delay(15000L)
                    } else {
                        delay(6000L)
                    }
                } catch (_: Exception) {
                    delay(6000L)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        running = false
        pollJob?.cancel()
        pollJob = null
    }

    private fun postNotification(event: MobileOfflineEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val platformText = if (event.platform == "qq") "QQ" else "微信"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            event.accountId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sprout)
            .setContentTitle("账号需要重新登录")
            .setContentText("[$platformText] ${event.accountName} 已掉线，点击打开手机端处理")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(event.accountId.hashCode(), notification)
        }
    }
}
