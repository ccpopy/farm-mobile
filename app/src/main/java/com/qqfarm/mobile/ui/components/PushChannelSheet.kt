package com.qqfarm.mobile.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.OfflineReminderConfig
import com.qqfarm.mobile.data.model.PUSH_CHANNEL_SPECS
import com.qqfarm.mobile.data.model.PushChannelEntry
import com.qqfarm.mobile.data.model.PushChannelSpec
import com.qqfarm.mobile.data.model.findPushChannelSpec
import com.qqfarm.mobile.data.model.parseOfflineDeleteSeconds
import com.qqfarm.mobile.data.model.validateDingTalkWebhookUrl
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushChannelSheet(
    settings: FarmApiClient.PushSettings,
    initialConfig: OfflineReminderConfig?,
    onConfigUpdated: (OfflineReminderConfig) -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf("") }
    var config by remember { mutableStateOf(initialConfig) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !saving },
    )

    var searchQuery by remember { mutableStateOf("") }
    var editingChannelId by remember { mutableStateOf<String?>(null) }
    var draftTitle by remember { mutableStateOf(initialConfig?.title.orEmpty()) }
    var draftMsg by remember { mutableStateOf(initialConfig?.msg.orEmpty()) }
    var draftDeleteSecText by remember { mutableStateOf((initialConfig?.offlineDeleteSec ?: 0).toString()) }
    var generalDirty by remember { mutableStateOf(false) }

    fun applyServerConfig(next: OfflineReminderConfig) {
        config = next
        if (!generalDirty) {
            draftTitle = next.title
            draftMsg = next.msg
            draftDeleteSecText = next.offlineDeleteSec.toString()
        }
        onConfigUpdated(next)
    }

    suspend fun loadConfig() {
        loadError = ""
        try {
            applyServerConfig(settings.fetch())
        } catch (e: ApiException) {
            loadError = e.message
        } finally {
            loading = false
        }
    }

    fun refresh() {
        if (loading || saving) return
        loading = true
        scope.launch { loadConfig() }
    }

    suspend fun persist(
        savedGeneralFields: Boolean = false,
        update: suspend () -> OfflineReminderConfig,
    ): Result<Unit> {
        if (loading || saving || loadError.isNotEmpty()) {
            return Result.failure(ApiException("请等推送渠道配置载入后再操作"))
        }
        saving = true
        return try {
            val saved = update()
            if (savedGeneralFields) generalDirty = false
            applyServerConfig(saved)
            Result.success(Unit)
        } catch (e: ApiException) {
            Result.failure(e)
        } finally {
            saving = false
        }
    }

    fun saveNotificationType(type: String, enabled: Boolean) {
        scope.launch {
            persist { settings.setNotificationType(type, enabled) }
                .onFailure { onMessage(it.message ?: "保存通知类型失败") }
        }
    }

    LaunchedEffect(settings) {
        loadConfig()
    }

    val current = config
    val busy = loading || saving
    val isSearching = searchQuery.isNotBlank()
    val sortedSpecs = remember(current, searchQuery) {
        val query = searchQuery.trim()
        PUSH_CHANNEL_SPECS
            .filter { spec -> spec.label.contains(query, ignoreCase = true) || spec.value.contains(query, ignoreCase = true) }
            .sortedBy { spec ->
                val entry = current?.entryFor(spec.value)
                when {
                    entry?.isEffectivelyEnabled == true -> 0
                    entry?.isConfigured == true -> 1
                    else -> 2
                }
            }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
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
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("推送渠道", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                    if (current != null) {
                        Text(
                            current.summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (current.hasActiveChannel) apple.green else apple.secondaryText,
                        )
                    }
                }
                IconButton(enabled = !busy, onClick = { refresh() }) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新渠道")
                    }
                }
            }

            if (loadError.isNotEmpty()) {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Text(loadError, style = MaterialTheme.typography.bodyMedium, color = apple.red)
                    TextButton(onClick = { refresh() }) { Text("重新加载") }
                }
            } else if (current != null) {
                AppleSearchField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        listState.requestScrollToItem(0)
                    },
                    placeholder = "搜索渠道",
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                ) {
                    if (!current.supportsMultiChannel && !isSearching) {
                        item(key = "legacy") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = apple.orange.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "当前服务端仅支持单渠道，保存新渠道会替换现有渠道。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.primaryText,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                    }

                    if (current.hasNotificationTypes && !isSearching) {
                        item(key = "types") {
                            AppleSectionHeader(title = "通知类型")
                            Spacer(modifier = Modifier.height(4.dp))
                            AppleGroupedCard {
                                AppleSwitchRow(
                                    icon = Icons.Rounded.NotificationsOff,
                                    iconTint = apple.red,
                                    title = "账号下线",
                                    checked = current.notificationTypes.offline,
                                    enabled = !busy,
                                    onCheckedChange = { saveNotificationType("offline", it) },
                                )
                                AppleHairlineDivider()
                                AppleSwitchRow(
                                    icon = Icons.Rounded.Notifications,
                                    iconTint = apple.orange,
                                    title = "神秘商人到货",
                                    checked = current.notificationTypes.mysteryArrival,
                                    enabled = !busy,
                                    onCheckedChange = { saveNotificationType("mysteryArrival", it) },
                                )
                                AppleHairlineDivider()
                                AppleSwitchRow(
                                    icon = Icons.Rounded.ShoppingBag,
                                    iconTint = apple.emerald,
                                    title = "神秘商人自动购买",
                                    checked = current.notificationTypes.mysteryPurchase,
                                    enabled = !busy,
                                    onCheckedChange = { saveNotificationType("mysteryPurchase", it) },
                                )
                            }
                        }
                    }

                    item(key = "channels") {
                        AppleGroupedCard {
                            if (sortedSpecs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("没有匹配的推送渠道", color = apple.secondaryText)
                                }
                            } else {
                                sortedSpecs.forEachIndexed { index, spec ->
                                    val entry = current.entryFor(spec.value)
                                    PushChannelRow(
                                        spec = spec,
                                        entry = entry,
                                        busy = busy,
                                        supportsMultiChannel = current.supportsMultiChannel,
                                        onOpenConfig = { editingChannelId = spec.value },
                                        onToggleEnabled = { enabled ->
                                            scope.launch {
                                                persist { settings.setChannelEnabled(spec.value, enabled) }
                                                    .onFailure { onMessage(it.message ?: "更新渠道状态失败") }
                                            }
                                        },
                                    )
                                    if (index < sortedSpecs.lastIndex) AppleHairlineDivider()
                                }
                            }
                        }
                    }

                    if (!isSearching) {
                        item(key = "general") {
                            val deleteSeconds = parseOfflineDeleteSeconds(draftDeleteSecText)
                            AppleSectionHeader(title = "下线通知与处理")
                            Spacer(modifier = Modifier.height(4.dp))
                            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = draftTitle,
                                        onValueChange = {
                                            draftTitle = it
                                            generalDirty = true
                                        },
                                        label = { Text("通知标题") },
                                        placeholder = { Text("账号下线提醒") },
                                        singleLine = true,
                                        enabled = !busy,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedTextField(
                                        value = draftDeleteSecText,
                                        onValueChange = {
                                            draftDeleteSecText = it
                                            generalDirty = true
                                        },
                                        label = { Text("离线删除账号（秒）") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        enabled = !busy,
                                        isError = deleteSeconds == null,
                                        supportingText = {
                                            Text(if (deleteSeconds == null) "请输入有效的非负整数" else "设为 0 时不自动删除账号")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedTextField(
                                        value = draftMsg,
                                        onValueChange = {
                                            draftMsg = it
                                            generalDirty = true
                                        },
                                        label = { Text("通知内容") },
                                        placeholder = { Text("账号下线") },
                                        minLines = 2,
                                        maxLines = 4,
                                        enabled = !busy,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Button(
                                        onClick = {
                                            if (deleteSeconds != null) {
                                                scope.launch {
                                                    persist(savedGeneralFields = true) {
                                                        settings.saveGeneral(draftTitle, draftMsg, deleteSeconds)
                                                    }.onSuccess {
                                                        onMessage("下线通知设置已保存")
                                                    }.onFailure {
                                                        onMessage(it.message ?: "保存下线通知设置失败")
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !busy && generalDirty && deleteSeconds != null,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                    ) {
                                        Text("保存下线通知设置")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val editingSpec = editingChannelId?.let(::findPushChannelSpec)
    if (editingSpec != null && current != null) {
        val entry = current.entryFor(editingSpec.value)
        PushChannelConfigSheet(
            spec = editingSpec,
            initialEntry = entry,
            supportsMultiChannel = current.supportsMultiChannel,
            saving = saving,
            onTest = { candidate -> settings.testChannel(candidate, draftTitle, draftMsg) },
            onSave = { candidate ->
                persist { settings.saveChannel(candidate, current.supportsMultiChannel) }.onSuccess {
                    onMessage("${editingSpec.label} 配置已保存")
                }
            },
            onClear = {
                persist { settings.clearChannel(entry, current.supportsMultiChannel) }.onSuccess {
                    onMessage("已清空 ${editingSpec.label} 配置")
                }
            },
            onMessage = onMessage,
            onDismiss = { if (!saving) editingChannelId = null },
        )
    }
}

@Composable
private fun PushChannelRow(
    spec: PushChannelSpec,
    entry: PushChannelEntry,
    busy: Boolean,
    supportsMultiChannel: Boolean,
    onOpenConfig: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
) {
    val apple = LocalAppleColors.current
    val configured = entry.isConfigured
    val enabled = entry.isEffectivelyEnabled

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(enabled = !busy, onClick = onOpenConfig)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.label,
                style = MaterialTheme.typography.bodyLarge,
                color = apple.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    enabled -> "已启用"
                    configured -> "已配置 · 未启用"
                    else -> "未配置"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    enabled -> apple.green
                    configured -> apple.orange
                    else -> apple.secondaryText
                },
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (!configured || !supportsMultiChannel) {
                        Modifier.clickable(enabled = !busy, onClick = onOpenConfig)
                    } else {
                        Modifier
                    }
                ),
        ) {
            AppleSwitch(
                checked = enabled,
                onCheckedChange = if (configured && !busy && supportsMultiChannel) {
                    { next -> onToggleEnabled(next) }
                } else {
                    null
                },
                enabled = configured && !busy && supportsMultiChannel,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PushChannelConfigSheet(
    spec: PushChannelSpec,
    initialEntry: PushChannelEntry,
    supportsMultiChannel: Boolean,
    saving: Boolean,
    onTest: suspend (PushChannelEntry) -> String,
    onSave: suspend (PushChannelEntry) -> Result<Unit>,
    onClear: suspend () -> Result<Unit>,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var endpoint by remember(spec.value) { mutableStateOf(initialEntry.endpoint) }
    var token by remember(spec.value) { mutableStateOf(initialEntry.token) }
    var secret by remember(spec.value) { mutableStateOf(initialEntry.secret) }
    var draftEnabled by remember(spec.value) {
        mutableStateOf(if (initialEntry.hasCredentials) initialEntry.enabled else true)
    }
    var testing by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var testSuccessText by remember { mutableStateOf("") }
    val busy = saving || testing
    val latestSaving by rememberUpdatedState(saving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !(latestSaving || testing) },
    )

    val entry = PushChannelEntry(
        channel = spec.value,
        enabled = draftEnabled || !supportsMultiChannel,
        endpoint = endpoint.trim(),
        token = token.trim(),
        secret = secret.trim(),
    )
    val endpointError = when {
        endpoint.isBlank() -> ""
        spec.isDingTalk -> validateDingTalkWebhookUrl(endpoint)
        (spec.usesEndpoint || spec.isMeow) && endpoint.trim().toHttpUrlOrNull() == null ->
            "请输入完整的 HTTP 或 HTTPS 接口地址"
        else -> ""
    }
    val canSave = entry.isConfigured && endpointError.isEmpty()
    fun resetStatus() {
        errorText = ""
        testSuccessText = ""
    }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .heightIn(max = 640.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    spec.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = apple.primaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (spec.docUrl.isNotBlank()) {
                    TextButton(onClick = { openExternalDoc(context, spec.docUrl, onMessage) }) {
                        Text("配置文档")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (spec.usesEndpoint || spec.isMeow) {
                            PushCredentialField(
                                label = when {
                                    spec.isDingTalk -> "Webhook 地址"
                                    spec.isMeow -> "接口地址（可选）"
                                    else -> "接口地址"
                                },
                                value = endpoint,
                                onValueChange = {
                                    endpoint = it
                                    resetStatus()
                                },
                                enabled = !busy,
                                error = endpointError,
                                hint = if (spec.isMeow) "留空使用默认接口" else "",
                            )
                        }
                        if (spec.isMeow) {
                            OutlinedTextField(
                                value = token,
                                onValueChange = {
                                    token = it
                                    resetStatus()
                                },
                                label = { Text("MeoW 昵称") },
                                singleLine = true,
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else if (!spec.isDingTalk) {
                            PushCredentialField(
                                label = if (spec.usesEndpoint) "Token（可选）" else "Token",
                                value = token,
                                onValueChange = {
                                    token = it
                                    resetStatus()
                                },
                                enabled = !busy,
                            )
                        } else {
                            PushCredentialField(
                                label = "加签密钥（可选）",
                                value = secret,
                                onValueChange = {
                                    secret = it
                                    resetStatus()
                                },
                                enabled = !busy,
                                hint = "机器人开启加签时填写",
                            )
                        }
                    }
                }

                AppleGroupedCard {
                    AppleSwitchRow(
                        icon = Icons.Rounded.NotificationsActive,
                        iconTint = if (canSave && entry.enabled) apple.green else apple.secondaryText,
                        title = "启用此渠道",
                        subtitle = if (supportsMultiChannel) null else "旧版服务端保存后即启用",
                        checked = canSave && entry.enabled,
                        enabled = canSave && supportsMultiChannel && !busy,
                        onCheckedChange = { draftEnabled = it },
                    )
                }
            }

            if (errorText.isNotEmpty()) {
                Text(errorText, style = MaterialTheme.typography.bodySmall, color = apple.red)
            } else if (testSuccessText.isNotEmpty()) {
                Text(testSuccessText, style = MaterialTheme.typography.bodySmall, color = apple.green)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        resetStatus()
                        testing = true
                        scope.launch {
                            try {
                                val message = onTest(entry)
                                testSuccessText = if (message.equals("ok", ignoreCase = true)) "测试通知已发送" else message
                            } catch (e: ApiException) {
                                errorText = e.message
                            } finally {
                                testing = false
                            }
                        }
                    },
                    enabled = canSave && !busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Text(if (testing) "测试中…" else "测试通知", fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = {
                        resetStatus()
                        scope.launch {
                            onSave(entry)
                                .onSuccess { onDismiss() }
                                .onFailure { errorText = it.message ?: "保存配置失败" }
                        }
                    },
                    enabled = canSave && !busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Text(if (saving) "保存中…" else "保存配置", fontWeight = FontWeight.Medium)
                }
            }

            if (initialEntry.hasCredentials) {
                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = apple.red),
                    onClick = { confirmClear = true },
                ) {
                    Text("清空此渠道配置")
                }
            }
        }
    }

    if (confirmClear) {
        AppleAlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空 ${spec.label} 配置？") },
            text = { Text("此渠道将停用，重新启用时需要填写配置。") },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = apple.red),
                    onClick = {
                        confirmClear = false
                        resetStatus()
                        scope.launch {
                            onClear()
                                .onSuccess { onDismiss() }
                                .onFailure { errorText = it.message ?: "清空配置失败" }
                        }
                    },
                ) { Text("清空") }
            },
        )
    }
}

@Composable
private fun PushCredentialField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    error: String = "",
    hint: String = "",
) {
    var visible by remember { mutableStateOf(false) }
    val help = error.ifBlank { hint }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error.isNotEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "隐藏$label" else "显示$label",
                )
            }
        },
        supportingText = if (help.isNotBlank()) ({ Text(help) }) else null,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun openExternalDoc(
    context: Context,
    url: String,
    onError: (String) -> Unit,
) {
    val launched = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess
    if (!launched) {
        onError("未找到可打开网页的应用：$url")
    }
}
