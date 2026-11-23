package com.qqfarm.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private fun formatServerTimestamp(tsMs: Long?): String {
    if (tsMs == null || tsMs <= 0L) return "尚未检查"
    return runCatching {
        SERVER_TIME_FORMAT.format(Instant.ofEpochMilli(tsMs).atZone(ZoneId.systemDefault()))
    }.getOrDefault("刚刚")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    var pendingApplyPullFirst by remember { mutableStateOf<Boolean?>(null) }

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
                    if (job.status == "succeeded") {
                        val msg = "服务端更新完成 (${next.shortCommit})"
                        actionNote = msg
                        onMessage(msg)
                        onServerUpdated()
                    } else if (job.status == "failed") {
                        errorMsg = job.error.ifBlank { "服务端更新任务执行失败" }
                    }
                }
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.httpCode == 401) {
                    pollingActive = false
                    reconnecting = false
                    errorMsg = e.message
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

    ModalBottomSheet(
        onDismissRequest = { if (busyAction == null && !tokenBusy) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 660.dp)
                .retainSheetHeight()
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
                    isUpdatingNow -> "更新中" to apple.blue
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

            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                                        text = "本地代码已更新至 ${cur.shortCommit}，当前进程仍在运行旧版本 (${cur.startupCommit.take(7)})，需要应用更新以重启生效",
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

                        // Update Job Progress Card
                        cur.job?.let { job ->
                            val (jobLabel, jobColor) = when (job.status) {
                                "running" -> "执行中" to apple.blue
                                "succeeded" -> "已完成" to apple.green
                                "failed" -> "失败" to apple.red
                                else -> job.status to apple.secondaryText
                            }
                            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("更新任务进度", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                        AppleStatusPill(text = jobLabel, color = jobColor, showDot = job.status == "running")
                                    }
                                    if (job.phase.isNotBlank()) {
                                        Text(job.phase, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = apple.primaryText)
                                    }
                                    val commitSpan = listOf(job.fromCommit, job.targetCommit).filter { it.isNotBlank() }.joinToString(" -> ")
                                    if (commitSpan.isNotBlank()) {
                                        Text(
                                            text = "版本切换：$commitSpan",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = apple.secondaryText,
                                        )
                                    }
                                    if (job.detail.isNotBlank()) {
                                        Text(job.detail, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                    }
                                    if (job.error.isNotBlank()) {
                                        Text(job.error, style = MaterialTheme.typography.labelSmall, color = apple.red)
                                    }
                                    if (job.recentLines.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = apple.elevated,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = job.recentLines.takeLast(12).joinToString("\n"),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp,
                                                ),
                                                color = apple.primaryText,
                                                modifier = Modifier
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(10.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (cur.canCheck) {
                                OutlinedButton(
                                    onClick = {
                                        busyAction = "check"
                                        errorMsg = ""
                                        actionNote = ""
                                        scope.launch {
                                            try {
                                                val updated = apiClient.checkSystemUpdates()
                                                status = updated
                                                actionNote = if (updated.behindCount > 0) {
                                                    "发现 ${updated.behindCount} 个可用更新"
                                                } else {
                                                    "服务端已是最新版本"
                                                }
                                            } catch (e: ApiException) {
                                                errorMsg = e.message
                                            } finally {
                                                busyAction = null
                                            }
                                        }
                                    },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    if (busyAction == "check") {
                                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("检查更新")
                                }
                            }

                            if (cur.canPull) {
                                OutlinedButton(
                                    onClick = {
                                        busyAction = "pull"
                                        errorMsg = ""
                                        actionNote = ""
                                        scope.launch {
                                            try {
                                                val result = apiClient.pullSystemUpdates()
                                                status = result.status
                                                actionNote = if (result.pulled) {
                                                    "已拉取更新 (${result.fromCommit} -> ${result.toCommit})"
                                                } else {
                                                    "没有需要拉取的提交"
                                                }
                                            } catch (e: ApiException) {
                                                errorMsg = e.message
                                            } finally {
                                                busyAction = null
                                            }
                                        }
                                    },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    if (busyAction == "pull") {
                                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("仅拉取代码")
                                }
                            }

                            if (cur.applySupported && cur.behindCount > 0) {
                                Button(
                                    onClick = { pendingApplyPullFirst = true },
                                    enabled = cur.canApply && !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    if (busyAction == "apply") {
                                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("拉取并更新")
                                }
                            } else if (cur.applySupported && cur.needsApply) {
                                Button(
                                    onClick = { pendingApplyPullFirst = false },
                                    enabled = cur.canApply && !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    if (busyAction == "apply") {
                                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("应用更新")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingApplyPullFirst?.let { pullFirst ->
        AppleAlertDialog(
            onDismissRequest = { if (busyAction != "apply") pendingApplyPullFirst = null },
            title = { Text(if (pullFirst) "确认拉取并更新服务端" else "确认应用服务端更新") },
            text = {
                Text(
                    if (pullFirst) {
                        "服务端将拉取最新代码并触发构建与重启切换，客户端会短暂重连。"
                    } else {
                        "服务端将使用当前磁盘最新代码构建并重启切换，客户端会短暂重连。"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingApplyPullFirst = null
                        busyAction = "apply"
                        errorMsg = ""
                        actionNote = ""
                        scope.launch {
                            try {
                                val next = apiClient.applySystemUpdate(pullFirst = pullFirst)
                                status = next
                                pollingActive = next.job?.status == "running" || next.deployBusy
                                if (!pollingActive && next.job?.status == "succeeded") {
                                    actionNote = "服务端更新完成 (${next.shortCommit})"
                                    onServerUpdated()
                                }
                            } catch (e: ApiException) {
                                errorMsg = e.message
                            } finally {
                                busyAction = null
                            }
                        }
                    },
                    enabled = busyAction == null,
                ) {
                    Text("确认执行")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingApplyPullFirst = null }) {
                    Text("取消")
                }
            },
        )
    }
}
