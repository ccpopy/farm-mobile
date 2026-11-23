package com.qqfarm.mobile.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.SystemUpdateJob
import com.qqfarm.mobile.data.model.SystemUpdateStatus
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SERVER_UPDATE_POLL_MS = 2_000L
private const val SERVER_UPDATE_MAX_RETRY = 90
private val SERVER_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
private val TOKEN_ASCII_REGEX = Regex("^[\u0021-\u007E]{1,1024}$")

internal fun formatServerTimestamp(tsMs: Long?): String {
    if (tsMs == null || tsMs <= 0L) return "尚未检查"
    return runCatching {
        SERVER_TIME_FORMAT.format(Instant.ofEpochMilli(tsMs).atZone(ZoneId.systemDefault()))
    }.getOrDefault("刚刚")
}

private sealed interface ServerDeployment {
    data class Update(val pullFirst: Boolean) : ServerDeployment
    data class Rollback(val releaseId: String) : ServerDeployment
}

private sealed interface ServerReleaseCleanup {
    data class Backups(val releaseIds: List<String>) : ServerReleaseCleanup
    data object Orphans : ServerReleaseCleanup
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerUpdateSheet(
    apiClient: FarmApiClient,
    serverUrl: String,
    appGithubToken: String,
    onServerUpdated: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var busyAction by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<SystemUpdateStatus?>(null) }
    var unsupported404 by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var actionNote by remember { mutableStateOf("") }

    var pollingActive by remember { mutableStateOf(false) }
    var reconnecting by remember { mutableStateOf(false) }
    var pollTimedOut by remember { mutableStateOf(false) }

    var showTokenEditor by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var tokenBusy by remember { mutableStateOf(false) }

    var showRollbackTab by remember { mutableStateOf(false) }
    var selectedReleaseId by remember { mutableStateOf<String?>(null) }
    var pendingDeployment by remember { mutableStateOf<ServerDeployment?>(null) }
    var batchDeleteMode by remember { mutableStateOf(false) }
    var checkedReleaseIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingCleanup by remember { mutableStateOf<ServerReleaseCleanup?>(null) }
    val updateList = rememberLazyListState()
    val rollbackList = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || (busyAction == null && !tokenBusy) },
    )

    fun showJobResult(job: SystemUpdateJob) {
        if (job.status == "succeeded") {
            val operation = if (job.isRollback) "服务端回滚完成" else "服务端更新完成"
            val message = if (job.targetCommit.isBlank()) operation else "$operation (${job.targetCommit})"
            actionNote = message
            onMessage(message)
            onServerUpdated()
        } else if (job.status == "failed") {
            errorMsg = job.error.ifBlank { if (job.isRollback) "服务端回滚失败" else "服务端更新失败" }
        }
    }

    suspend fun loadStatus() {
        loading = true
        errorMsg = ""
        pollTimedOut = false
        try {
            val loaded = apiClient.fetchSystemUpdateStatus()
            status = loaded
            unsupported404 = false
            pollingActive = loaded.job?.status == "running" || loaded.deployBusy
            reconnecting = false
        } catch (e: ApiException) {
            status = null
            unsupported404 = e.httpCode == 404
            if (!unsupported404) {
                errorMsg = e.message
            }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadStatus()
    }

    LaunchedEffect(pollingActive) {
        if (!pollingActive) return@LaunchedEffect
        var observedJobId = status?.job?.takeIf { it.status == "running" }?.id
        var consecutiveErrors = 0
        while (true) {
            delay(SERVER_UPDATE_POLL_MS)
            try {
                val next = apiClient.fetchSystemUpdateStatus()
                status = next
                reconnecting = false
                pollTimedOut = false
                errorMsg = ""
                consecutiveErrors = 0

                val job = next.job
                if (job?.status == "running") observedJobId = job.id
                if (job?.status == "running" || next.deployBusy) continue
                pollingActive = false
                if (job != null && job.id == observedJobId) {
                    showJobResult(job)
                } else if (observedJobId != null) {
                    onMessage("服务端已重新连接，请核对当前运行版本")
                }
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.httpCode == 401 || e.httpCode == 403 || e.httpCode == 404) {
                    pollingActive = false
                    reconnecting = false
                    status = null
                    unsupported404 = e.httpCode == 404
                    if (unsupported404) onMessage("服务端已重新响应，当前版本未提供任务进度") else errorMsg = e.message
                    break
                }
                consecutiveErrors++
                reconnecting = true
                if (consecutiveErrors >= SERVER_UPDATE_MAX_RETRY) {
                    pollingActive = false
                    reconnecting = false
                    pollTimedOut = true
                    break
                }
            }
        }
    }

    val cur = status
    val isUpdatingNow = pollingActive
    val isBusy = loading || busyAction != null || tokenBusy || pollingActive || pollTimedOut
    val backups = cur?.releases.orEmpty()
    val orphanSummary = cur?.releaseCleanup?.takeIf { it.hasOrphans }
    val hasRollback = cur?.releases != null && (cur.gitSupported || backups.isNotEmpty() || orphanSummary != null)
    val rollbackPage = hasRollback && showRollbackTab
    val selectedBackup = backups.firstOrNull { it.releaseId == selectedReleaseId && it.canRollback }
    val deletableBackups = backups.filter { it.canDelete }
    val checkedBackups = deletableBackups.filter { it.releaseId in checkedReleaseIds }
    val canManageBackups = !isBusy && cur?.deployBusy == false
    val canDeploy = canManageBackups && cur?.gitSupported == true && cur.applySupported

    fun runAction(name: String, operation: suspend () -> Unit) {
        if (isBusy || busyAction != null) return
        busyAction = name
        errorMsg = ""
        actionNote = ""
        scope.launch {
            try {
                operation()
            } catch (e: ApiException) {
                if (name == "rollback" || name == "apply" || name == "delete") {
                    // 部署锁或备份可能已经变化，重新读取状态。
                    loadStatus()
                }
                errorMsg = e.message
                if (name == "delete") rollbackList.requestScrollToItem(0)
            } finally {
                busyAction = null
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (busyAction == null && !tokenBusy) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppleIconBadge(icon = Icons.Rounded.Update, tint = apple.emerald, size = 34.dp)
                Text(
                    text = "服务端更新",
                    style = MaterialTheme.typography.titleLarge,
                    color = apple.primaryText,
                    modifier = Modifier.weight(1f),
                )
                val (badgeLabel, badgeColor) = when {
                    isUpdatingNow -> (if (cur?.job?.isRollback == true) "回滚中" else "更新中") to apple.blue
                    busyAction == "delete" -> "清理中" to apple.orange
                    unsupported404 -> "暂不支持" to apple.secondaryText
                    pollTimedOut -> "等待重连" to apple.orange
                    errorMsg.isNotBlank() && cur == null -> "读取失败" to apple.red
                    cur == null -> "加载中" to apple.secondaryText
                    !cur.gitSupported -> "暂不支持" to apple.orange
                    cur.behindCount > 0 -> "有 ${cur.behindCount} 个更新" to apple.orange
                    cur.needsApply -> "待应用" to apple.orange
                    cur.lastCheckedAt != null && cur.lastCheckError.isBlank() -> "已是最新" to apple.green
                    else -> "就绪" to apple.secondaryText
                }
                AppleStatusPill(text = badgeLabel, color = badgeColor, showDot = isUpdatingNow)
                IconButton(
                    onClick = { scope.launch { loadStatus() } },
                    enabled = !loading && busyAction == null && !tokenBusy && !pollingActive,
                ) {
                    if (loading && cur != null) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新状态")
                    }
                }
            }

            if (hasRollback) {
                AppleTabStrip(
                    options = listOf("在线更新", "版本回滚"),
                    selectedIndex = if (rollbackPage) 1 else 0,
                    onSelect = { showRollbackTab = it == 1 },
                )
            }
            if (rollbackPage && batchDeleteMode) {
                val outdatedIds = deletableBackups.filter { it.isOutdated }.map { it.releaseId }
                if (outdatedIds.isNotEmpty()) {
                    val allOutdatedSelected = outdatedIds.all { it in checkedReleaseIds }
                    TextButton(
                        onClick = {
                            checkedReleaseIds = if (allOutdatedSelected) checkedReleaseIds - outdatedIds
                                else checkedReleaseIds + outdatedIds
                        },
                        enabled = canManageBackups,
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Text(if (allOutdatedSelected) "取消选择过时版本" else "选中过时版本（${outdatedIds.size}）")
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                state = if (rollbackPage) rollbackList else updateList,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                if (reconnecting || pollTimedOut || actionNote.isNotBlank() || errorMsg.isNotBlank()) {
                    item(key = "notices") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (reconnecting) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = apple.blue.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = apple.blue)
                                        Text(
                                            text = "服务端正在重启并切换版本，等待恢复连接…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = apple.primaryText,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }

                            if (pollTimedOut) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = apple.orange.copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = apple.orange, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "等待重连超时，请检查服务端是否已启动完成",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = apple.primaryText,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(onClick = {
                                            pollTimedOut = false
                                            pollingActive = true
                                        }) {
                                            Text("重试")
                                        }
                                    }
                                }
                            }

                            if (actionNote.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = apple.green.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = apple.green, modifier = Modifier.size(18.dp))
                                        Text(actionNote, style = MaterialTheme.typography.bodySmall, color = apple.primaryText, modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            if (errorMsg.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = apple.red.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = apple.red, modifier = Modifier.size(18.dp))
                                        Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = apple.red, modifier = Modifier.weight(1f))
                                        if (cur == null) {
                                            TextButton(onClick = { scope.launch { loadStatus() } }) { Text("重试") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                cur?.job?.let { job ->
                    item(key = "job-${job.id}") { ServerUpdateTaskCard(job) }
                }
                if (rollbackPage) {
                    if (cur?.applySupported == false && cur.applyUnsupportedReason.isNotBlank() && !batchDeleteMode) {
                        item { Text(cur.applyUnsupportedReason, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText) }
                    }
                    if (orphanSummary != null && !batchDeleteMode) {
                        item(key = "orphan-backups") {
                            AppleGroupedCard(contentPadding = PaddingValues(start = 12.dp, end = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "冗余备份 · ${Formatter.formatShortFileSize(context, orphanSummary.orphanSizeBytes)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = apple.primaryText,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        onClick = { pendingCleanup = ServerReleaseCleanup.Orphans },
                                        enabled = canManageBackups,
                                    ) { Text("清理冗余") }
                                }
                            }
                        }
                    }
                    if (backups.isEmpty()) {
                        item {
                            AppleGroupedCard {
                                AppleEmptyState(Icons.Rounded.History, "暂无历史版本", "服务端尚未保留版本备份")
                            }
                        }
                    } else {
                        items(backups, key = { it.releaseId }, contentType = { "backup" }) { backup ->
                            val id = backup.releaseId
                            ServerReleaseRow(
                                backup = backup,
                                selected = if (batchDeleteMode) backup.canDelete && id in checkedReleaseIds else id == selectedBackup?.releaseId,
                                selectionEnabled = if (batchDeleteMode) canManageBackups && backup.canDelete else canDeploy && backup.canRollback,
                                batchMode = batchDeleteMode,
                                deleteEnabled = canManageBackups,
                                onSelect = {
                                    if (batchDeleteMode) {
                                        checkedReleaseIds = if (id in checkedReleaseIds) checkedReleaseIds - id else checkedReleaseIds + id
                                    } else {
                                        selectedReleaseId = id
                                    }
                                },
                                onDelete = { pendingCleanup = ServerReleaseCleanup.Backups(listOf(id)) },
                            )
                        }
                    }
                } else {
                    item(key = "server-status") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when {
                                loading && cur == null -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(160.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                unsupported404 -> {
                                    AppleGroupedCard {
                                        AppleEmptyState(
                                            icon = Icons.Rounded.SystemUpdateAlt,
                                            title = "暂不支持在线更新",
                                            subtitle = "当前连接的后端版本未提供在线版本更新接口",
                                        )
                                    }
                                }

                                cur != null && !cur.gitSupported -> {
                                    AppleGroupedCard {
                                        AppleEmptyState(
                                            icon = Icons.Rounded.SystemUpdateAlt,
                                            title = "当前环境不支持 Git 更新",
                                            subtitle = cur.unsupportedReason.ifBlank { "当前服务端运行方式不支持在客户端内检查或拉取更新" },
                                        )
                                    }
                                }

                                cur != null -> {
                                    // Version & Repo Card
                                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = if (cur.detached || cur.branch.isBlank()) "HEAD 游离" else "分支 ${cur.branch}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = apple.primaryText,
                                                )
                                                Text(
                                                    text = cur.shortCommit.ifBlank { cur.commit.take(7) },
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontFeatureSettings = "tnum",
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }

                                            if (cur.commitSubject.isNotBlank()) {
                                                Text(
                                                    text = cur.commitSubject,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = apple.primaryText,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = cur.upstream.ifBlank { "${cur.remote}/${cur.remoteBranch}".trim('/') }.ifBlank { "无上游追踪分支" },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = apple.secondaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "上次检查 ${formatServerTimestamp(cur.lastCheckedAt)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                    color = apple.secondaryText,
                                                )
                                            }

                                            if (cur.lastCheckError.isNotBlank()) {
                                                Text(
                                                    text = "最近检查失败：${cur.lastCheckError}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = apple.red,
                                                )
                                            }

                                            if (cur.dirtyFiles.isNotEmpty()) {
                                                val preview = cur.dirtyFiles.take(3).joinToString("、") +
                                                    if (cur.dirtyFiles.size > 3) " 等 ${cur.dirtyFiles.size} 项" else ""
                                                Text(
                                                    text = "服务端存在未提交改动，暂不可拉取或在线更新：$preview",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = apple.orange,
                                                )
                                            }

                                            if (cur.aheadCount > 0) {
                                                Text(
                                                    text = "本地分支领先远程 ${cur.aheadCount} 个提交，不支持自动 ff-only 更新",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = apple.orange,
                                                )
                                            }

                                            if (cur.needsApply && !reconnecting) {
                                                Text(
                                                    text = "当前运行 ${cur.startupCommit.take(7)} · 源码版本 ${cur.shortCommit}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = apple.orange,
                                                )
                                            }

                                            if (!cur.applySupported && cur.applyUnsupportedReason.isNotBlank()) {
                                                Text(
                                                    text = cur.applyUnsupportedReason,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = apple.secondaryText,
                                                )
                                            }
                                        }
                                    }

                                    // Server GitHub Token Card
                                    if (cur.githubTokenConfigured != null) {
                                        val configured = cur.githubTokenConfigured
                                        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(Icons.Rounded.Key, contentDescription = null, tint = apple.indigo, modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = "服务端 GitHub 授权",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = apple.primaryText,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    AppleStatusPill(
                                                        text = if (configured) "已配置" else "未配置",
                                                        color = if (configured) apple.green else apple.secondaryText,
                                                        showDot = false,
                                                    )
                                                    TextButton(
                                                        onClick = { showTokenEditor = !showTokenEditor },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    ) {
                                                        Text(if (showTokenEditor) "收起" else "管理")
                                                    }
                                                }

                                                if (!configured && appGithubToken.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Text(
                                                            text = "同步后，$serverUrl 将保存手机上的 GitHub Token",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = apple.secondaryText,
                                                            modifier = Modifier.weight(1f),
                                                        )
                                                        OutlinedButton(
                                                            onClick = {
                                                                tokenBusy = true
                                                                errorMsg = ""
                                                                actionNote = ""
                                                                scope.launch {
                                                                    try {
                                                                        apiClient.saveServerGithubToken(serverUrl, appGithubToken.trim())
                                                                        status = cur.copy(githubTokenConfigured = true)
                                                                        actionNote = "已同步客户端 GitHub 授权到服务端"
                                                                        loadStatus()
                                                                    } catch (e: ApiException) {
                                                                        errorMsg = e.message
                                                                    } finally {
                                                                        tokenBusy = false
                                                                    }
                                                                }
                                                            },
                                                            enabled = !isBusy,
                                                            shape = RoundedCornerShape(10.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(32.dp),
                                                        ) {
                                                            Text("同步授权", style = MaterialTheme.typography.labelMedium)
                                                        }
                                                    }
                                                }

                                                if (showTokenEditor) {
                                                    Text(
                                                        text = "私有仓库检查与拉取需要具备当前仓库只读代码权限的 GitHub Token",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = apple.secondaryText,
                                                    )
                                                    val trimmed = tokenInput.trim()
                                                    val invalid = trimmed.isNotEmpty() && !TOKEN_ASCII_REGEX.matches(trimmed)
                                                    OutlinedTextField(
                                                        value = tokenInput,
                                                        onValueChange = { tokenInput = it },
                                                        label = { Text("服务端 GitHub Token") },
                                                        placeholder = { Text(if (configured) "输入新 Token 以替换，或留空后点移除" else "ghp_... / github_pat_...") },
                                                        singleLine = true,
                                                        isError = invalid,
                                                        supportingText = if (invalid) ({ Text("Token 只能包含可见的 ASCII 字符") }) else null,
                                                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                        trailingIcon = {
                                                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                                                Icon(
                                                                    imageVector = if (tokenVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                                    contentDescription = "显示/隐藏",
                                                                )
                                                            }
                                                        },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                tokenBusy = true
                                                                errorMsg = ""
                                                                actionNote = ""
                                                                scope.launch {
                                                                    try {
                                                                        apiClient.saveServerGithubToken(serverUrl, trimmed)
                                                                        status = cur.copy(githubTokenConfigured = true)
                                                                        tokenInput = ""
                                                                        showTokenEditor = false
                                                                        actionNote = "已保存服务端 GitHub 授权"
                                                                    } catch (e: ApiException) {
                                                                        errorMsg = e.message
                                                                    } finally {
                                                                        tokenBusy = false
                                                                    }
                                                                }
                                                            },
                                                            enabled = !isBusy && trimmed.isNotEmpty() && !invalid,
                                                            shape = RoundedCornerShape(10.dp),
                                                            modifier = Modifier.weight(1f),
                                                        ) {
                                                            Text("保存授权")
                                                        }
                                                        if (configured) {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    tokenBusy = true
                                                                    errorMsg = ""
                                                                    actionNote = ""
                                                                    scope.launch {
                                                                        try {
                                                                            apiClient.clearServerGithubToken(serverUrl)
                                                                            status = cur.copy(githubTokenConfigured = false)
                                                                            tokenInput = ""
                                                                            showTokenEditor = false
                                                                            actionNote = "已移除服务端 GitHub 授权"
                                                                        } catch (e: ApiException) {
                                                                            errorMsg = e.message
                                                                        } finally {
                                                                            tokenBusy = false
                                                                        }
                                                                    }
                                                                },
                                                                enabled = !isBusy,
                                                                shape = RoundedCornerShape(10.dp),
                                                                modifier = Modifier.weight(1f),
                                                            ) {
                                                                Text("移除", color = apple.red)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Incoming Commits
                                    if (cur.incomingCommits.isNotEmpty()) {
                                        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "待拉取更新 (${cur.behindCount.coerceAtLeast(cur.incomingCommits.size)})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = apple.primaryText,
                                                )
                                                cur.incomingCommits.take(8).forEachIndexed { idx, commit ->
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Text(
                                                                text = commit.shortHash.ifBlank { commit.hash.take(7) },
                                                                style = MaterialTheme.typography.labelMedium.copy(
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontFeatureSettings = "tnum",
                                                                ),
                                                                color = MaterialTheme.colorScheme.primary,
                                                            )
                                                            Text(
                                                                text = commit.subject,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = apple.primaryText,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f),
                                                            )
                                                        }
                                                        val meta = listOfNotNull(
                                                            commit.author.takeIf { it.isNotBlank() },
                                                            commit.timestampMs.takeIf { it > 0L }?.let { formatServerTimestamp(it) },
                                                        ).joinToString(" · ")
                                                        if (meta.isNotBlank()) {
                                                            Text(
                                                                text = meta,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                                color = apple.secondaryText,
                                                            )
                                                        }
                                                    }
                                                    if (idx < cur.incomingCommits.take(8).lastIndex) {
                                                        AppleHairlineDivider()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (rollbackPage && (backups.isNotEmpty() || batchDeleteMode)) {
                AppleHairlineDivider(startIndent = 0.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (deletableBackups.isNotEmpty() || batchDeleteMode) {
                        TextButton(
                            onClick = {
                                batchDeleteMode = !batchDeleteMode
                                checkedReleaseIds = emptySet()
                                if (batchDeleteMode) rollbackList.requestScrollToItem(0)
                            },
                            enabled = canManageBackups,
                        ) { Text(if (batchDeleteMode) "取消" else "批量删除") }
                    }
                    if (batchDeleteMode) {
                        Button(
                            onClick = { pendingCleanup = ServerReleaseCleanup.Backups(checkedBackups.map { it.releaseId }) },
                            enabled = canManageBackups && checkedBackups.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            val count = checkedBackups.size
                            ServerActionLabel(if (count == 0) "删除所选" else "删除 ${count} 个备份", busyAction == "delete")
                        }
                    } else {
                        Button(
                            onClick = { selectedBackup?.let { pendingDeployment = ServerDeployment.Rollback(it.releaseId) } },
                            enabled = canDeploy && selectedBackup != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            ServerActionLabel("回滚到所选版本", busyAction == "rollback")
                        }
                    }
                }
            } else if (!rollbackPage && cur?.gitSupported == true &&
                (cur.canCheck || cur.canPull || cur.applySupported && (cur.behindCount > 0 || cur.needsApply))
            ) {
                AppleHairlineDivider(startIndent = 0.dp)
                ServerUpdateActions(
                    status = cur,
                    enabled = !isBusy,
                    busyAction = busyAction,
                    onCheck = {
                        runAction("check") {
                            val updated = apiClient.checkSystemUpdates()
                            status = updated
                            actionNote = when {
                                updated.behindCount > 0 -> "发现 ${updated.behindCount} 个可用更新"
                                updated.needsApply -> "代码已是最新，等待应用到服务端"
                                else -> "服务端已是最新版本"
                            }
                        }
                    },
                    onPull = {
                        runAction("pull") {
                            val result = apiClient.pullSystemUpdates()
                            status = result.status
                            actionNote = if (result.pulled) "已拉取更新 (${result.fromCommit} → ${result.toCommit})" else "没有需要拉取的提交"
                        }
                    },
                    onApply = { pullFirst -> pendingDeployment = ServerDeployment.Update(pullFirst) },
                )
            }
        }
    }

    pendingCleanup?.let { request ->
        val cleanOrphans = request == ServerReleaseCleanup.Orphans
        val ids = (request as? ServerReleaseCleanup.Backups)?.releaseIds.orEmpty()
        val targets = deletableBackups.filter { it.releaseId in ids }
        val available = if (cleanOrphans) orphanSummary != null else ids.isNotEmpty() && targets.size == ids.size
        val target = targets.singleOrNull()
        val estimate = if (cleanOrphans) orphanSummary?.orphanSizeBytes ?: 0L else targets.sumOf { it.sizeBytes }
        val size = Formatter.formatShortFileSize(context, estimate)
        val description = when {
            !available -> "备份列表已变化，请刷新后重试。"
            cleanOrphans && orphanSummary != null -> {
                val parts = listOfNotNull(
                    orphanSummary.orphanHostCount.takeIf { it > 0 }?.let { "${it} 个旧会话宿主备份" },
                    orphanSummary.orphanWebCount.takeIf { it > 0 }?.let { "${it} 个未关联网页备份" },
                )
                "将永久删除 ${parts.joinToString("、")}。预计释放 $size。"
            }
            target != null -> {
                val time = target.createdAt.takeIf { it > 0 }?.let { "（${formatServerTimestamp(it)}）" }.orEmpty()
                "将永久删除版本 ${target.label}${time} 的备份。预计释放 $size。"
            }
            else -> "将永久删除所选的 ${targets.size} 个历史版本备份。预计释放 $size。"
        }
        AppleAlertDialog(
            onDismissRequest = { pendingCleanup = null },
            title = { Text(if (cleanOrphans) "清理冗余备份" else "删除历史版本备份") },
            text = { Text(description) },
            confirmButton = {
                Button(
                    enabled = canManageBackups && available,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    onClick = {
                        pendingCleanup = null
                        runAction("delete") {
                            val result = apiClient.deleteSystemReleases(serverUrl, ids, cleanOrphans)
                            status = result.status
                            pollingActive = result.status.deployBusy || result.status.job?.status == "running"
                            if (result.status.releases?.any { it.releaseId == selectedReleaseId } != true) {
                                selectedReleaseId = null
                            }
                            checkedReleaseIds = emptySet()
                            batchDeleteMode = false
                            val count = if (cleanOrphans) result.deletedHostIds.size + result.deletedWebIds.size
                                else result.deletedReleaseIds.size
                            actionNote = if (count == 0) {
                                if (cleanOrphans) "当前没有可清理的冗余备份" else "所选备份已不存在"
                            } else {
                                val freed = Formatter.formatShortFileSize(context, result.freedBytes)
                                val type = if (cleanOrphans) "冗余备份" else "历史版本备份"
                                "已删除 ${count} 个${type}，释放 $freed"
                            }
                            rollbackList.requestScrollToItem(0)
                        }
                    },
                ) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { pendingCleanup = null }) { Text("取消") } },
        )
    }

    pendingDeployment?.let { command ->
        val rollback = command as? ServerDeployment.Rollback
        val target = if (rollback == null) null else cur?.releases?.find { it.releaseId == rollback.releaseId }
        val allowed = canDeploy && when (command) {
            is ServerDeployment.Rollback -> target?.canRollback == true
            is ServerDeployment.Update -> cur?.canApply == true && (if (command.pullFirst) cur.behindCount > 0 else cur.needsApply)
        }
        AppleAlertDialog(
            onDismissRequest = { pendingDeployment = null },
            title = { Text(if (rollback != null) "确认回滚服务端" else "确认更新服务端") },
            text = {
                Text(when (command) {
                    is ServerDeployment.Rollback -> if (target == null) "所选备份已不可用" else {
                        val pairedWeb = if (target.pairedWebReleaseId.isNotEmpty()) "及配套网页" else ""
                        "将服务端${pairedWeb}切换到 ${target.label}。切换期间客户端会短暂重连，账号数据保留。"
                    }
                    is ServerDeployment.Update -> if (command.pullFirst) {
                        "服务端将拉取最新代码并构建，客户端会在切换期间短暂重连。"
                    } else {
                        "服务端将使用当前代码构建，客户端会在切换期间短暂重连。"
                    }
                })
            },
            confirmButton = {
                Button(
                    enabled = allowed,
                    onClick = {
                        pendingDeployment = null
                        runAction(if (rollback != null) "rollback" else "apply") {
                            val next = when (command) {
                                is ServerDeployment.Rollback -> apiClient.rollbackSystemRelease(serverUrl, command.releaseId)
                                is ServerDeployment.Update -> apiClient.applySystemUpdate(command.pullFirst, serverUrl)
                            }
                            status = next
                            pollingActive = next.job?.status == "running" || next.deployBusy
                            if (!pollingActive) next.job?.let(::showJobResult)
                            if (rollback != null) selectedReleaseId = null
                            val list = if (showRollbackTab && next.releases != null && next.gitSupported) rollbackList else updateList
                            list.requestScrollToItem(0)
                        }
                    },
                ) {
                    Text(if (rollback != null) "确认回滚" else "确认更新")
                }
            },
            dismissButton = { TextButton(onClick = { pendingDeployment = null }) { Text("取消") } },
        )
    }
}
