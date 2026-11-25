package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.AccountSettingsModel
import com.qqfarm.mobile.data.model.PluginAccountDetail
import com.qqfarm.mobile.data.model.PluginDefinition
import com.qqfarm.mobile.data.model.PluginListPayload
import com.qqfarm.mobile.data.model.PluginQuietWindow
import com.qqfarm.mobile.data.model.PluginSpendingPolicy
import com.qqfarm.mobile.data.model.PluginSummary
import com.qqfarm.mobile.data.model.SeedOption
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleAlertDialog
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleIconBadge
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.components.AppleTabStrip
import com.qqfarm.mobile.ui.components.EqualHeightRow
import com.qqfarm.mobile.platform.rememberAppHaptics
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.qqfarm.mobile.data.network.json.JSONArray
import com.qqfarm.mobile.data.network.json.JSONObject

private data class ExtensionBindingDraft(
    val enabled: Boolean = false,
    val settingsJson: String = "{}",
    val quietWindows: List<PluginQuietWindow> = emptyList(),
    val accountQuietWindows: List<PluginQuietWindow> = emptyList(),
    val spending: PluginSpendingPolicy = PluginSpendingPolicy(),
)

private data class PendingResolveConfirmation(
    val operationId: String,
    val resolution: String,
    val isNative: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId

    var payload by remember { mutableStateOf<PluginListPayload?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var updatingPluginId by remember { mutableStateOf<String?>(null) }
    var selectedPlugin by remember { mutableStateOf<PluginSummary?>(null) }
    var builtinPaused by remember(accountId) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    suspend fun loadBuiltinPaused(list: List<PluginSummary>) {
        if (accountId.isBlank()) return
        val result = coroutineScope {
            list.filter { it.isBuiltin }.map { plugin ->
                async {
                    try {
                        plugin.id to apiClient.fetchBuiltinPluginPaused(plugin.id, accountId)
                    } catch (_: ApiException) {
                        null
                    }
                }
            }.awaitAll()
        }
        builtinPaused = result.filterNotNull().toMap()
    }

    suspend fun loadPlugins() {
        if (refreshing || updatingPluginId != null) return
        refreshing = true
        try {
            val next = apiClient.fetchPlugins()
            payload = next
            loadBuiltinPaused(next.plugins)
        } catch (e: ApiException) {
            builtinPaused = emptyMap()
            onMessage(e.message)
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(accountId) { loadPlugins() }

    fun toggleBuiltin(plugin: PluginSummary, run: Boolean) {
        if (accountId.isBlank() || refreshing || updatingPluginId != null) return
        val previous = builtinPaused[plugin.id] ?: return
        updatingPluginId = plugin.id
        builtinPaused = builtinPaused + (plugin.id to !run)
        scope.launch {
            try {
                val paused = apiClient.setBuiltinPluginPaused(plugin.id, accountId, paused = !run)
                builtinPaused = builtinPaused + (plugin.id to paused)
                onMessage(if (paused) "已暂停当前账号的「${plugin.name}」" else "已恢复「${plugin.name}」")
            } catch (e: ApiException) {
                builtinPaused = builtinPaused + (plugin.id to previous)
                onMessage(e.message)
            } finally {
                updatingPluginId = null
            }
        }
    }

    fun toggleExtension(plugin: PluginSummary, enabled: Boolean) {
        if (refreshing || updatingPluginId != null) return
        val previous = payload ?: return
        updatingPluginId = plugin.id
        scope.launch {
            try {
                val updated = apiClient.setPluginEnabled(plugin.id, enabled)
                payload = previous.copy(plugins = previous.plugins.map { if (it.id == updated.id) updated else it })
                onMessage(if (updated.enabled) "已启用「${updated.name}」" else "已停用「${updated.name}」")
            } catch (e: ApiException) {
                onMessage(e.message)
            } finally {
                updatingPluginId = null
            }
        }
    }

    fun openDetail(plugin: PluginSummary) {
        if (refreshing || updatingPluginId != null) return
        if (accountId.isBlank()) {
            onMessage("请先选择农场账号再配置插件")
        } else {
            selectedPlugin = plugin
        }
    }

    val builtinList = payload?.plugins?.filter { it.isBuiltin }.orEmpty()
    val extList = payload?.plugins?.filter { !it.isBuiltin }.orEmpty()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { scope.launch { loadPlugins() } },
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            payload?.let { p ->
                item {
                    val enabledBuiltin = builtinList.count { builtinPaused[it.id] == false }
                    val statusesKnown = accountId.isNotBlank() && builtinList.all { it.id in builtinPaused }
                    AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppleIconBadge(
                                icon = Icons.Rounded.Extension,
                                tint = if (p.runtimeAvailable) apple.emerald else apple.orange,
                                size = 40.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (p.runtimeAvailable) "插件运行正常" else "扩展插件暂不可用",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = apple.primaryText,
                                )
                                Text(
                                    text = p.runtimeReason.ifBlank { "宿主 API v${p.hostApiVersion.ifBlank { "1" }}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.secondaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        EqualHeightRow(spacing = 8.dp) {
                            PluginStatTile(
                                value = if (statusesKnown) "$enabledBuiltin/${builtinList.size}" else "--",
                                label = "官方模块启用",
                                color = apple.emerald,
                                modifier = Modifier.weight(1f),
                            )
                            PluginStatTile(
                                value = "${extList.count { it.enabled }}/${extList.size}",
                                label = "扩展已启用",
                                color = apple.blue,
                                modifier = Modifier.weight(1f),
                            )
                            val pending = p.plugins.sumOf { it.pendingConfirmations }
                            PluginStatTile(
                                value = pending.toString(),
                                label = "待核对",
                                color = if (pending > 0) apple.orange else apple.secondaryText,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (builtinList.isNotEmpty()) {
                item {
                    AppleSectionHeader(title = "官方基础模块")
                }
                item {
                    AppleGroupedCard {
                        builtinList.forEachIndexed { idx, plugin ->
                            val paused = builtinPaused[plugin.id]
                            PluginCardRow(
                                plugin = plugin,
                                icon = builtinPluginIcon(plugin.id),
                                tint = builtinPluginTint(plugin.id),
                                active = accountId.isNotBlank() && paused == false,
                                statusText = when {
                                    accountId.isBlank() -> "未选择账号"
                                    paused == null -> if (refreshing) "读取中" else "读取失败，下拉刷新"
                                    paused -> "当前账号已暂停"
                                    else -> "当前账号已启用"
                                },
                                statusColor = when {
                                    accountId.isBlank() || paused == null -> apple.secondaryText
                                    paused -> apple.orange
                                    else -> apple.green
                                },
                                busy = plugin.id == updatingPluginId,
                                switchEnabled = paused != null && !refreshing && updatingPluginId == null,
                                onToggle = { run -> toggleBuiltin(plugin, run) },
                                onClick = { openDetail(plugin) },
                            )
                            if (idx < builtinList.lastIndex) {
                                AppleHairlineDivider(startIndent = 68.dp)
                            }
                        }
                    }
                }
            }

            item {
                AppleSectionHeader(title = "扩展插件")
            }
            if (extList.isEmpty()) {
                item {
                    AppleGroupedCard {
                        AppleEmptyState(
                            icon = Icons.Rounded.Extension,
                            title = "暂无已安装扩展插件",
                            subtitle = "可在 Web 管理后台上传安装扩展包后，在此处启停、配置规则、查看任务与核对消费确认",
                        )
                    }
                }
            } else {
                item {
                    AppleGroupedCard {
                        extList.forEachIndexed { idx, plugin ->
                            val stateColor = when (plugin.executionState) {
                                "running" -> apple.green
                                "failed" -> apple.red
                                else -> apple.secondaryText
                            }
                            PluginCardRow(
                                plugin = plugin,
                                icon = Icons.Rounded.Extension,
                                tint = apple.indigo,
                                active = plugin.enabled,
                                statusText = when {
                                    !plugin.enabled -> "已停用"
                                    plugin.executionLabel.isNotBlank() -> plugin.executionLabel
                                    else -> "已启用"
                                },
                                statusColor = if (plugin.enabled) stateColor else apple.secondaryText,
                                busy = plugin.id == updatingPluginId,
                                switchEnabled = !refreshing && updatingPluginId == null && (plugin.canDisable || !plugin.enabled),
                                onToggle = { next -> toggleExtension(plugin, next) },
                                onClick = { openDetail(plugin) },
                            )
                            if (idx < extList.lastIndex) {
                                AppleHairlineDivider(startIndent = 68.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (accountId.isNotBlank()) {
        selectedPlugin?.let { plugin ->
            PluginDetailSheet(
                plugin = plugin,
                accountId = accountId,
                accountTitle = session.currentAccount?.displayTitle.orEmpty(),
                connected = session.status?.connected == true,
                apiClient = apiClient,
                listRefreshing = refreshing,
                onMessage = onMessage,
                onUpdated = { scope.launch { loadPlugins() } },
                onBuiltinPauseChanged = { paused -> builtinPaused = builtinPaused + (plugin.id to paused) },
                onDismiss = { selectedPlugin = null },
            )
        }
    }
}

@Composable
private fun PluginStatTile(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = apple.elevated,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = color,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText, maxLines = 1)
        }
    }
}

private fun builtinPluginIcon(id: String): ImageVector = when (id) {
    "official.daily-farm" -> Icons.Rounded.Agriculture
    "official.daily-ranch" -> Icons.Rounded.Pets
    "official.fertilizer" -> Icons.Rounded.Science
    "official.friends" -> Icons.Rounded.People
    "official.mystery-shop" -> Icons.Rounded.Storefront
    else -> Icons.Rounded.AutoAwesome
}

@Composable
private fun builtinPluginTint(id: String): Color {
    val apple = LocalAppleColors.current
    return when (id) {
        "official.daily-farm" -> apple.emerald
        "official.daily-ranch" -> apple.orange
        "official.fertilizer" -> apple.cyan
        "official.friends" -> apple.blue
        "official.mystery-shop" -> apple.indigo
        else -> apple.green
    }
}

@Composable
private fun PluginCardRow(
    plugin: PluginSummary,
    icon: ImageVector,
    tint: Color,
    active: Boolean,
    statusText: String,
    statusColor: Color,
    busy: Boolean,
    switchEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.tick()
                onClick()
            }
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleIconBadge(
            icon = icon,
            tint = if (active) tint else apple.secondaryText.copy(alpha = 0.7f),
            size = 40.dp,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = apple.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (plugin.pendingConfirmations > 0) {
                    AppleStatusPill("待核对 ${plugin.pendingConfirmations}", apple.orange, showDot = false)
                }
            }
            if (plugin.description.isNotBlank()) {
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "查看详情",
                    tint = apple.secondaryText.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(contentAlignment = Alignment.Center) {
            AppleSwitch(
                checked = active,
                onCheckedChange = onToggle,
                enabled = switchEnabled && !busy,
            )
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = apple.secondaryText,
                )
            }
        }
    }
}

private fun jsonElementsEqual(a: Any?, b: Any?): Boolean {
    if (a === b) return true
    if (a == null || a == com.qqfarm.mobile.data.network.json.JSON_NULL) return b == null || b == com.qqfarm.mobile.data.network.json.JSON_NULL
    if (b == null || b == com.qqfarm.mobile.data.network.json.JSON_NULL) return false
    if (a is JSONObject && b is JSONObject) {
        if (a.length() != b.length()) return false
        for (key in a.keys()) {
            if (!b.has(key) || !jsonElementsEqual(a.opt(key), b.opt(key))) return false
        }
        return true
    }
    if (a is JSONArray && b is JSONArray) {
        if (a.length() != b.length()) return false
        for (i in 0 until a.length()) {
            if (!jsonElementsEqual(a.opt(i), b.opt(i))) return false
        }
        return true
    }
    if (a is Number && b is Number) return a.toDouble() == b.toDouble()
    if (a is Boolean && b is Boolean) return a == b
    return a.toString() == b.toString()
}

private fun extensionDraftsEqual(a: ExtensionBindingDraft?, b: ExtensionBindingDraft?): Boolean {
    if (a == null || b == null) return a === b
    if (a.enabled != b.enabled) return false
    if (a.quietWindows != b.quietWindows) return false
    if (a.accountQuietWindows != b.accountQuietWindows) return false
    if (a.spending != b.spending) return false
    val ja = runCatching { JSONObject(a.settingsJson) }.getOrNull()
    val jb = runCatching { JSONObject(b.settingsJson) }.getOrNull()
    return if (ja != null && jb != null) jsonElementsEqual(ja, jb) else a.settingsJson == b.settingsJson
}

private fun toExtensionDraft(detail: PluginAccountDetail): ExtensionBindingDraft = ExtensionBindingDraft(
    enabled = detail.bindingEnabled,
    settingsJson = detail.bindingSettingsJson,
    quietWindows = detail.quietWindows,
    accountQuietWindows = detail.accountQuietWindows,
    spending = detail.spendingPolicy,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginDetailSheet(
    plugin: PluginSummary,
    accountId: String,
    accountTitle: String,
    connected: Boolean,
    apiClient: FarmApiClient,
    listRefreshing: Boolean,
    onMessage: (String) -> Unit,
    onUpdated: () -> Unit,
    onBuiltinPauseChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()

    var definition by remember(plugin.id) { mutableStateOf<PluginDefinition?>(null) }
    var detail by remember(plugin.id, accountId) { mutableStateOf<PluginAccountDetail?>(null) }
    var seeds by remember(accountId) { mutableStateOf<List<SeedOption>>(emptyList()) }

    var builtinDraft by remember(plugin.id, accountId) { mutableStateOf<AccountSettingsModel?>(null) }
    var builtinBaseline by remember(plugin.id, accountId) { mutableStateOf<AccountSettingsModel?>(null) }
    var extDraft by remember(plugin.id, accountId) { mutableStateOf<ExtensionBindingDraft?>(null) }
    var extBaseline by remember(plugin.id, accountId) { mutableStateOf<ExtensionBindingDraft?>(null) }

    var loading by remember(plugin.id, accountId) { mutableStateOf(true) }
    var loadError by remember(plugin.id, accountId) { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var runningJobId by remember { mutableStateOf("") }

    var showBagPriority by remember(accountId, plugin.id) { mutableStateOf(false) }
    var showSeedPicker by remember { mutableStateOf(false) }
    var animalPickerKind by remember { mutableStateOf<String?>(null) }
    var confirmCloseDiscard by remember { mutableStateOf(false) }
    var pendingResolve by remember { mutableStateOf<PendingResolveConfirmation?>(null) }

    val tabs = remember(plugin.id, plugin.isBuiltin) {
        if (plugin.isBuiltin) {
            buildList {
                add("automation" to "脚本设置")
                if (plugin.id == "official.daily-farm") add("strategy" to "种植策略")
                if (plugin.id == "official.daily-ranch") add("strategy" to "养殖策略")
                add("status" to "运行状态")
            }
        } else {
            listOf(
                "config" to "脚本配置",
                "rules" to "运行规则",
                "status" to "状态与日志",
            )
        }
    }
    var selectedTabIdx by remember(plugin.id) { mutableIntStateOf(0) }

    val dirty = if (plugin.isBuiltin) {
        builtinDraft != null && builtinBaseline != null && builtinDraft != builtinBaseline
    } else {
        extDraft != null && extBaseline != null && !extensionDraftsEqual(extDraft, extBaseline)
    }

    val hasPendingEdits by rememberUpdatedState(dirty)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { next ->
            if (next != SheetValue.Hidden) return@rememberModalBottomSheetState true
            if (saving || busy) return@rememberModalBottomSheetState false
            if (hasPendingEdits) {
                confirmCloseDiscard = true
                return@rememberModalBottomSheetState false
            }
            true
        },
    )

    fun applyAccountDetail(next: PluginAccountDetail) {
        detail = next
        if (next.isBuiltin) {
            builtinDraft = next.builtinSettings
            builtinBaseline = next.builtinSettings
            onBuiltinPauseChanged(next.paused)
        } else {
            val d = toExtensionDraft(next)
            extDraft = d
            extBaseline = d
        }
    }

    suspend fun loadAll(preserveDraft: Boolean = false) {
        if (!preserveDraft) {
            loading = true
            loadError = ""
        }
        val before = detail
        try {
            val next = coroutineScope {
                val view = async { apiClient.fetchPluginAccountDetail(plugin.id, accountId, plugin.isBuiltin) }
                if (definition == null) definition = apiClient.fetchPluginDefinition(plugin.id)
                view.await()
            }
            if (preserveDraft && before != null) {
                if (detail !== before) return
                if (builtinDraft != builtinBaseline || !extensionDraftsEqual(extDraft, extBaseline)) {
                    // 状态可以刷新，草稿仍须用打开时的版本提交，交给服务端检测并发修改。
                    detail = next.copy(settingsRevision = before.settingsRevision, bindingRevision = before.bindingRevision)
                    return
                }
            }
            applyAccountDetail(next)
        } catch (e: ApiException) {
            if (!preserveDraft) loadError = e.message.ifBlank { "加载失败" }
            onMessage(e.message)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(plugin.id, accountId) { loadAll() }
    LaunchedEffect(plugin.id, accountId, selectedTabIdx) {
        if (plugin.id == "official.daily-farm" && tabs[selectedTabIdx].first == "strategy" && seeds.isEmpty()) {
            seeds = try { apiClient.fetchSeedCatalog(accountId) } catch (_: ApiException) { emptyList() }
        }
    }

    fun requestClose() {
        if (saving || busy) return
        if (dirty) {
            confirmCloseDiscard = true
        } else {
            onDismiss()
        }
    }

    fun discardChanges() {
        detail?.let { current ->
            if (current.isBuiltin) {
                builtinDraft = builtinBaseline ?: current.builtinSettings
            } else {
                extDraft = extBaseline ?: toExtensionDraft(current)
            }
        }
    }

    fun saveChanges() {
        val current = detail ?: return
        if (saving || busy || !dirty) return
        if (current.isBuiltin) {
            val draft = builtinDraft ?: return
            val baseline = builtinBaseline ?: return
            if (plugin.id == "official.friends" && draft.quietHoursEnabled) {
                val s = draft.quietHoursStart.trim()
                val e = draft.quietHoursEnd.trim()
                if (!isValidQuietWindowTime(s) || !isValidQuietWindowTime(e)) {
                    onMessage("好友静默时段需为 HH:mm 格式")
                    return
                }
            }
            saving = true
            scope.launch {
                try {
                    val result = apiClient.saveBuiltinPluginSettings(
                        pluginId = plugin.id,
                        accountId = accountId,
                        model = draft,
                        baseline = baseline,
                        settingsRevision = current.settingsRevision,
                    )
                    applyAccountDetail(result.detail)
                    if (result.unconfirmed) {
                        onMessage(result.message.ifBlank { "配置已保存，运行进程尚未确认应用" })
                    } else {
                        onMessage("${plugin.name}设置已保存")
                    }
                    onUpdated()
                } catch (e: ApiException) {
                    onMessage("保存失败：${e.message}")
                } finally {
                    saving = false
                }
            }
        } else {
            val draft = extDraft ?: return
            val def = definition ?: PluginDefinition(id = plugin.id, name = plugin.name)
            if (hasExtensionSchemaErrors(def, draft.settingsJson)) {
                onMessage("请先修正脚本配置中的错误")
                selectedTabIdx = 0
                return
            }
            val badWindow = (draft.quietWindows + draft.accountQuietWindows).any { w ->
                !isValidQuietWindowTime(w.start) || !isValidQuietWindowTime(w.end) || w.start.trim() == w.end.trim()
            }
            if (badWindow) {
                onMessage("请检查静默时段格式（HH:mm 且起止时间不能相同）")
                selectedTabIdx = 1
                return
            }
            if (draft.spending.rules.any { !isValidResourceKey(it.resourceKey) }) {
                onMessage("消费限制中的资源需为 item:物品ID")
                selectedTabIdx = 1
                return
            }
            saving = true
            scope.launch {
                try {
                    val updated = apiClient.saveExtensionPluginBinding(
                        pluginId = plugin.id,
                        accountId = accountId,
                        revision = current.bindingRevision,
                        enabled = draft.enabled,
                        settingsJson = draft.settingsJson,
                        quietWindows = draft.quietWindows,
                        spending = draft.spending,
                        accountQuietWindows = draft.accountQuietWindows.takeIf { it != extBaseline?.accountQuietWindows },
                    )
                    applyAccountDetail(updated)
                    onMessage("脚本设置已保存")
                    onUpdated()
                } catch (e: ApiException) {
                    onMessage("保存失败：${e.message}")
                } finally {
                    saving = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!saving && !busy) requestClose() },
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        // 固定抽屉总高度与顶部标题/标签/底部操作栏：加载中、切换标签、展开子项均不发生整体高度回弹，且标题永不滚出视野。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // 1. 固定标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppleIconBadge(
                    icon = if (plugin.isBuiltin) builtinPluginIcon(plugin.id) else Icons.Rounded.Extension,
                    tint = if (plugin.isBuiltin) builtinPluginTint(plugin.id) else apple.indigo,
                    size = 40.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = definition?.name?.ifBlank { plugin.name } ?: plugin.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = apple.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val ver = definition?.version?.ifBlank { plugin.version } ?: plugin.version
                        if (ver.isNotBlank()) {
                            Text(
                                text = "v$ver",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                        }
                    }
                    Text(
                        text = buildString {
                            val badge = definition?.badge?.ifBlank { plugin.badge } ?: plugin.badge
                            if (badge.isNotBlank()) append("$badge · ")
                            append("账号：${accountTitle.ifBlank { accountId }}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = ::requestClose, enabled = !saving && !busy) {
                    Icon(Icons.Rounded.Cancel, contentDescription = "关闭", tint = apple.secondaryText)
                }
            }

            // 2. 固定标签栏
            AppleTabStrip(
                options = tabs.map { it.second },
                selectedIndex = selectedTabIdx.coerceIn(tabs.indices),
                onSelect = { selectedTabIdx = it },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            AppleHairlineDivider(startIndent = 0.dp)

            // 3. 中部可滚动内容区（占满剩余高度）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    saving || (loading && detail == null) -> AppleLoadingState(Modifier.fillMaxSize())

                    loadError.isNotBlank() && detail == null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(loadError, style = MaterialTheme.typography.bodyMedium, color = apple.red)
                            OutlinedButton(onClick = { scope.launch { loadAll() } }) {
                                Text("重试")
                            }
                        }
                    }

                    detail != null -> {
                        val currentDetail = detail!!
                        val currentKey = tabs.getOrNull(selectedTabIdx)?.first ?: tabs.first().first
                        key(currentKey) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (currentDetail.isBuiltin) {
                                    val model = builtinDraft ?: currentDetail.builtinSettings
                                    when (currentKey) {
                                        "automation" -> when (plugin.id) {
                                            "official.daily-farm" -> BuiltinDailyFarmSettingsTab(
                                                model = model,
                                                paused = currentDetail.paused,
                                                onChange = { builtinDraft = it },
                                            )
                                            "official.daily-ranch" -> BuiltinDailyRanchSettingsTab(
                                                model = model,
                                                paused = currentDetail.paused,
                                                onChange = { builtinDraft = it },
                                            )
                                            "official.fertilizer" -> BuiltinFertilizerSettingsTab(
                                                model = model,
                                                paused = currentDetail.paused,
                                                onChange = { builtinDraft = it },
                                            )
                                            "official.friends" -> BuiltinFriendsSettingsTab(
                                                model = model,
                                                paused = currentDetail.paused,
                                                onChange = { builtinDraft = it },
                                            )
                                            "official.mystery-shop" -> BuiltinMysteryShopSettingsTab(
                                                model = model,
                                                paused = currentDetail.paused,
                                                onChange = { builtinDraft = it },
                                            )
                                        }

                                        "strategy" -> when (plugin.id) {
                                            "official.daily-farm" -> BuiltinDailyFarmStrategyTab(
                                                model = model,
                                                seeds = seeds,
                                                paused = currentDetail.paused,
                                                onChange = { builtinDraft = it },
                                                onEditBagPriority = { showBagPriority = true },
                                                onPickSeed = { showSeedPicker = true },
                                            )
                                            "official.daily-ranch" -> BuiltinDailyRanchStrategyTab(
                                                model = model,
                                                animalOptions = definition?.animalOptions.orEmpty(),
                                                paused = currentDetail.paused,
                                                onChange = { builtinDraft = it },
                                                onEditBagPriority = { showBagPriority = true },
                                                onPickAnimal = { kind -> animalPickerKind = kind },
                                            )
                                        }

                                        "status" -> BuiltinStatusTab(
                                            detail = currentDetail,
                                            busy = busy || listRefreshing || saving,
                                            onTogglePause = { wantPaused ->
                                                if (busy || listRefreshing || saving) return@BuiltinStatusTab
                                                busy = true
                                                scope.launch {
                                                    try {
                                                        val paused = apiClient.setBuiltinPluginPaused(plugin.id, accountId, paused = wantPaused)
                                                        detail = currentDetail.copy(paused = paused)
                                                        onBuiltinPauseChanged(paused)
                                                        onMessage(if (paused) "已暂停模块" else "已恢复模块")
                                                        onUpdated()
                                                    } catch (e: ApiException) {
                                                        onMessage(e.message)
                                                    } finally {
                                                        busy = false
                                                    }
                                                }
                                            },
                                            onRefresh = { scope.launch { loadAll(preserveDraft = true) } },
                                            onResolveOp = { opId, res, native ->
                                                pendingResolve = PendingResolveConfirmation(opId, res, native)
                                            },
                                        )

                                    }
                                } else {
                                    val def = definition ?: PluginDefinition(id = plugin.id, name = plugin.name)
                                    val draft = extDraft ?: toExtensionDraft(currentDetail)
                                    when (currentKey) {
                                        "config" -> ExtensionConfigTab(
                                            definition = def,
                                            enabled = draft.enabled,
                                            settingsJson = draft.settingsJson,
                                            conflicts = currentDetail.conflicts,
                                            disabled = saving,
                                            onEnabledChange = { next -> extDraft = draft.copy(enabled = next) },
                                            onSettingsJsonChange = { nextJson -> extDraft = draft.copy(settingsJson = nextJson) },
                                        )

                                        "rules" -> ExtensionRulesTab(
                                            quietWindows = draft.quietWindows,
                                            accountQuietWindows = draft.accountQuietWindows,
                                            spendingPolicy = draft.spending,
                                            spendingUsage = currentDetail.spendingUsage,
                                            resourceNames = def.resourceNames,
                                            disabled = saving,
                                            onQuietChange = { next -> extDraft = draft.copy(quietWindows = next) },
                                            onAccountQuietChange = { next -> extDraft = draft.copy(accountQuietWindows = next) },
                                            onSpendingChange = { next -> extDraft = draft.copy(spending = next) },
                                        )

                                        "status" -> ExtensionStatusLogsTab(
                                            detail = currentDetail,
                                            definition = def,
                                            dirty = dirty,
                                            runningJobId = runningJobId,
                                            busy = busy || saving,
                                            onRefresh = { scope.launch { loadAll(preserveDraft = true) } },
                                            onRunJob = { jobId ->
                                                if (runningJobId.isNotBlank() || saving || busy) return@ExtensionStatusLogsTab
                                                runningJobId = jobId
                                                scope.launch {
                                                    runCatching { apiClient.runPluginJob(plugin.id, jobId, accountId) }
                                                        .onSuccess {
                                                            onMessage("已触发任务")
                                                            loadAll(preserveDraft = true)
                                                        }
                                                        .onFailure { onMessage(it.message ?: "触发失败") }
                                                    runningJobId = ""
                                                }
                                            },
                                            onResolveOp = { opId, res, native ->
                                                pendingResolve = PendingResolveConfirmation(opId, res, native)
                                            },
                                        )

                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. 固定底部操作栏
            AppleHairlineDivider(startIndent = 0.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(apple.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (dirty) "有未保存修改" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = apple.orange,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedButton(
                    onClick = ::discardChanges,
                    enabled = !saving && dirty,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("撤销修改", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = ::requestClose,
                    enabled = !saving && !busy,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("关闭", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = ::saveChanges,
                    enabled = detail != null && dirty && !saving && !busy && !loading,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (saving) "保存中" else "保存", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showBagPriority) builtinDraft?.let { draft ->
        BagPrioritySheet(
            accountId, connected, plugin.id == "official.daily-ranch", apiClient, draft,
            onApply = { builtinDraft = it; showBagPriority = false },
            onDismiss = { showBagPriority = false },
        )
    }
    if (showSeedPicker) {
        val currentDraft = builtinDraft
        PluginSeedPickerSheet(
            seeds = seeds,
            selectedSeedId = currentDraft?.preferredSeedId ?: 0L,
            onSelect = { id ->
                if (currentDraft != null) builtinDraft = currentDraft.copy(preferredSeedId = id)
                showSeedPicker = false
            },
            onDismiss = { showSeedPicker = false },
        )
    }

    val pickedKind = animalPickerKind
    if (pickedKind != null) {
        val currentDraft = builtinDraft
        PluginRanchAnimalPickerSheet(
            kind = pickedKind,
            options = definition?.animalOptions?.filter { it.kind == pickedKind }.orEmpty(),
            selectedValue = if (pickedKind == "窝") currentDraft?.ranchCoopAnimal ?: 0 else currentDraft?.ranchShedAnimal ?: 0,
            onSelect = { chosen ->
                if (currentDraft != null) {
                    builtinDraft = if (pickedKind == "窝") {
                        currentDraft.copy(ranchCoopAnimal = chosen)
                    } else {
                        currentDraft.copy(ranchShedAnimal = chosen)
                    }
                }
                animalPickerKind = null
            },
            onDismiss = { animalPickerKind = null },
        )
    }

    if (confirmCloseDiscard) {
        AppleAlertDialog(
            onDismissRequest = { confirmCloseDiscard = false },
            title = { Text("确认关闭") },
            text = { Text("当前有未保存的修改，关闭后将丢弃这些修改。") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmCloseDiscard = false
                        onDismiss()
                    },
                ) { Text("确定丢弃") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCloseDiscard = false }) { Text("继续编辑") }
            },
        )
    }

    pendingResolve?.let { req ->
        val message = when {
            req.isNative && req.resolution == "confirmed" -> "已在游戏中确认该交易生效？确认后将释放本次预留。"
            req.isNative -> "已在游戏中确认该交易未生效？确认后将释放预留，并允许重新尝试。"
            req.resolution == "confirmed" -> "确认该操作已在游戏中生效？将计入今日额度，并解除同类操作的暂停。"
            else -> "确认该操作未生效？将释放额度，并解除同类操作的暂停。"
        }
        AppleAlertDialog(
            onDismissRequest = { if (!busy) pendingResolve = null },
            title = { Text("确认核对") },
            text = { Text(message) },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        pendingResolve = null
                        busy = true
                        scope.launch {
                            runCatching {
                                apiClient.resolvePluginOperation(plugin.id, req.operationId, req.resolution, req.isNative, accountId)
                            }
                                .onSuccess {
                                    onMessage(if (req.resolution == "confirmed") "已确认生效" else "已标记未生效")
                                    loadAll(preserveDraft = true)
                                    onUpdated()
                                }
                                .onFailure { onMessage(it.message ?: "核对失败") }
                            busy = false
                        }
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pendingResolve = null }, enabled = !busy) { Text("取消") }
            },
        )
    }
}
