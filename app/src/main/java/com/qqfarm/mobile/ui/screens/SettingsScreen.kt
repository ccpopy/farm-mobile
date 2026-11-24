package com.qqfarm.mobile.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qqfarm.mobile.BuildConfig
import com.qqfarm.mobile.data.model.RUN_LOG_RETENTION_DAYS
import com.qqfarm.mobile.data.model.ActivityNotifyGroup
import com.qqfarm.mobile.data.model.ActivityNotifyType
import com.qqfarm.mobile.data.network.ActivityNotificationManager
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.data.network.PROJECT_GITHUB_URL
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.ui.components.AppleAlertDialog
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSettingRow
import com.qqfarm.mobile.ui.components.AppleSwitchRow
import com.qqfarm.mobile.ui.components.ScrollToTopOnTabReselect
import com.qqfarm.mobile.ui.components.ServerSwitcherSheet
import com.qqfarm.mobile.ui.components.ServerUpdateSheet
import com.qqfarm.mobile.ui.state.AppUpdateViewModel
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.CustomAppearanceConfig
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.ui.theme.SoilGold
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    onOpenReconfigureServer: () -> Unit,
    onAddServer: () -> Unit,
    onSwitchServer: (String) -> Unit,
    onRenameServer: (String, String) -> Unit,
    onRemoveServer: (String) -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenIllustrated: () -> Unit,
    onDisconnect: () -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onUpdateCustomAppearance: (CustomAppearanceConfig) -> Unit,
    onToggleOfflineAlerts: (Boolean) -> Unit,
    onToggleActivityPush: (Boolean) -> Unit,
    onToggleNotifyCollapse: (Boolean) -> Unit,
    onToggleNotifySilent: (Boolean) -> Unit,
    onToggleNotifyAllAccounts: (Boolean) -> Unit,
    onToggleNotifyType: (String, Boolean) -> Unit,
    onApplyNotifyTypes: (Set<String>) -> Unit,
    onSendTestNotification: () -> Boolean,
    onClearLogs: suspend (String) -> Unit,
    onServerUpdated: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val updateModel: AppUpdateViewModel = viewModel()

    var showNotifySheet by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDownloadEditor by rememberSaveable { mutableStateOf(false) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showServerUpdateSheet by rememberSaveable(session.config.serverUrl) { mutableStateOf(false) }
    var showGithubTokenEditor by rememberSaveable { mutableStateOf(false) }
    var showLogCleanup by remember(session.config.currentAccountId) { mutableStateOf(false) }
    var showCustomAppearanceSheet by remember { mutableStateOf(false) }

    var notificationsGranted by remember {
        mutableStateOf(ActivityNotificationManager.areNotificationsEnabled(context))
    }
    var pendingPermissionTarget by remember { mutableStateOf<String?>(null) }

    val config = session.config
    var onlineCount by remember(config.activeProfileId, config.serverUrl, config.authToken) { mutableStateOf<Int?>(null) }
    LaunchedEffect(
        lifecycleOwner, config.activeProfileId, config.serverUrl, config.authToken,
        session.realtimeConnected, session.status?.connected,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                onlineCount = try {
                    apiClient.fetchOnlineAccountCount()
                } catch (_: ApiException) {
                    null
                }
                delay(15_000L)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = ActivityNotificationManager.areNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val enabledInSystem = ActivityNotificationManager.areNotificationsEnabled(context)
        notificationsGranted = enabledInSystem
        val target = pendingPermissionTarget
        pendingPermissionTarget = null

        if (granted && enabledInSystem) {
            when (target) {
                "offline" -> onToggleOfflineAlerts(true)
                "activity" -> {
                    onToggleActivityPush(true)
                    onMessage("已开启农牧动态推送")
                }
                else -> onMessage("已授予通知权限")
            }
        } else {
            if (target == "offline") onToggleOfflineAlerts(false)
            if (target == "activity") onToggleActivityPush(false)
            onMessage("通知权限未开启，请前往系统设置授权")
        }
    }

    fun requestNotificationPermissionAndEnable(target: String) {
        if (ActivityNotificationManager.areNotificationsEnabled(context)) {
            notificationsGranted = true
            when (target) {
                "offline" -> onToggleOfflineAlerts(true)
                "activity" -> onToggleActivityPush(true)
                else -> onMessage("通知权限已开启")
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ActivityNotificationManager.hasPostPermission(context)
        ) {
            pendingPermissionTarget = target
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ActivityNotificationManager.openSystemNotificationSettings(context)
        }
    }

    val listState = rememberLazyListState()
    ScrollToTopOnTabReselect(4, listState)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 0. Extensions & Official Upstream Automation Entry
        item {
            AppleSectionHeader(title = "扩展与自动化")
            Spacer(modifier = Modifier.height(4.dp))
            AppleGroupedCard {
                AppleSettingRow(
                    icon = Icons.Rounded.Tune,
                    iconTint = apple.indigo,
                    title = "自动化设置",
                    trailingText = session.currentAccount?.name?.takeIf { it.isNotBlank() },
                    showChevron = true,
                    onClick = onOpenAutomation,
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.MenuBook,
                    iconTint = apple.emerald,
                    title = "图鉴",
                    showChevron = true,
                    onClick = onOpenIllustrated,
                )
            }
        }

        // 1. Server & Auth
        item {
            AppleSectionHeader(title = "服务器与授权")
            Spacer(modifier = Modifier.height(4.dp))
            AppleGroupedCard {
                val activeServer = session.config.activeProfile
                val otherCount = (session.config.serverProfiles.size - 1).coerceAtLeast(0)
                AppleSettingRow(
                    icon = Icons.Rounded.Dns,
                    iconTint = apple.blue,
                    title = "远程服务器",
                    subtitle = activeServer?.displayName?.let { "$it · ${session.config.serverUrl}" }
                        ?: session.config.serverUrl.ifBlank { "未配置" },
                    trailingText = when {
                        otherCount > 0 -> "切换 (${session.config.serverProfiles.size})"
                        session.realtimeConnected -> "实时在线"
                        else -> "切换 / 添加"
                    },
                    trailingColor = if (session.realtimeConnected) apple.green else apple.secondaryText,
                    showChevron = true,
                    onClick = { showServerSheet = true },
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Key,
                    iconTint = apple.orange,
                    title = "当前登录凭证",
                    trailingText = "复制 Token",
                    onClick = {
                        val token = session.config.authToken.trim()
                        if (token.isNotEmpty()) {
                            clipboard.setText(AnnotatedString(token))
                            onMessage("已复制当前会话 Token")
                        }
                    },
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Person,
                    iconTint = apple.emerald,
                    title = "农场账号管理",
                    trailingText = onlineCount?.let { "$it 在线" } ?: "—",
                    trailingColor = if ((onlineCount ?: 0) > 0) apple.green else apple.secondaryText,
                    showChevron = true,
                    onClick = onOpenAccountSwitcher,
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Logout,
                    iconTint = apple.red,
                    title = "退出当前服务器登录",
                    onClick = { showLogoutConfirm = true },
                )
            }
        }

        // 2. Push Notifications (Default OFF)
        item {
            AppleSectionHeader(title = "消息推送与通知")
            Spacer(modifier = Modifier.height(4.dp))
            val enabledCount = session.config.enabledNotifyTypes.size
            val pushStatusText = when {
                !session.config.activityPushEnabled -> "已关闭（默认）"
                enabledCount == 0 -> "已开启 · 未选类型"
                else -> "已开启 · $enabledCount 项"
            }
            AppleGroupedCard {
                AppleSettingRow(
                    icon = if (notificationsGranted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                    iconTint = if (notificationsGranted) apple.green else apple.orange,
                    title = "系统通知权限",
                    trailingText = if (notificationsGranted) "已授权" else "去授权",
                    trailingColor = if (notificationsGranted) apple.green else apple.orange,
                    showChevron = true,
                    onClick = {
                        if (notificationsGranted) {
                            ActivityNotificationManager.openSystemNotificationSettings(context)
                        } else {
                            requestNotificationPermissionAndEnable("permission_only")
                        }
                    },
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Notifications,
                    iconTint = apple.emerald,
                    title = "农牧动态推送",
                    trailingText = pushStatusText,
                    trailingColor = if (session.config.activityPushEnabled) apple.emerald else apple.secondaryText,
                    showChevron = true,
                    onClick = { showNotifySheet = true },
                )
                AppleHairlineDivider()
                AppleSwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    iconTint = apple.red,
                    title = "账号掉线重登提醒",
                    checked = session.config.offlineAlertsEnabled,
                    onCheckedChange = { wantEnable ->
                        if (!wantEnable) {
                            onToggleOfflineAlerts(false)
                        } else {
                            requestNotificationPermissionAndEnable("offline")
                        }
                    },
                )
            }
        }

        // 3. Appearance & Updates
        item {
            AppleSectionHeader(title = "外观与通用")
            Spacer(modifier = Modifier.height(4.dp))
            val themeModeLabel = when (session.config.themeMode) {
                StoredConfig.THEME_LIGHT -> "浅色"
                StoredConfig.THEME_DARK -> "深色"
                StoredConfig.THEME_CUSTOM -> "自定义"
                else -> "跟随系统"
            }
            AppleGroupedCard {
                AppleSettingRow(
                    icon = Icons.Rounded.Tune,
                    iconTint = apple.emerald,
                    title = "主题外观",
                    trailingText = themeModeLabel,
                    showChevron = true,
                    onClick = { showCustomAppearanceSheet = true },
                )
                AppleHairlineDivider()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AppleSwitchRow(
                        icon = Icons.Rounded.Palette,
                        iconTint = apple.indigo,
                        title = "动态壁纸取色",
                        checked = session.config.dynamicColor,
                        onCheckedChange = onUpdateDynamicColor,
                    )
                    AppleHairlineDivider()
                }
                AppleSettingRow(
                    icon = Icons.Rounded.SystemUpdateAlt,
                    iconTint = apple.blue,
                    title = "客户端更新",
                    trailingText = "v${BuildConfig.VERSION_NAME}",
                    showChevron = true,
                    onClick = { updateModel.check(session.config) },
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Update,
                    iconTint = apple.emerald,
                    title = "服务端更新",
                    showChevron = true,
                    onClick = { showServerUpdateSheet = true },
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Key,
                    iconTint = apple.indigo,
                    title = "GitHub 授权",
                    trailingText = if (session.config.githubToken.isBlank()) "未设置" else "已保存",
                    trailingColor = if (session.config.githubToken.isBlank()) apple.secondaryText else apple.green,
                    showChevron = true,
                    onClick = { showGithubTokenEditor = true },
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Speed,
                    iconTint = apple.emerald,
                    title = "下载方式",
                    trailingText = session.config.updateDownload.mode.label,
                    showChevron = true,
                    onClick = { showDownloadEditor = true },
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    iconTint = apple.orange,
                    title = "日志清理",
                    trailingText = "保留 $RUN_LOG_RETENTION_DAYS 天",
                    showChevron = true,
                    onClick = { showLogCleanup = true },
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Star,
                    iconTint = SoilGold,
                    title = "仓库地址",
                    trailingText = "GitHub",
                    showChevron = true,
                    onClick = { openGithubRepository(context, PROJECT_GITHUB_URL, onMessage) },
                )
            }
        }
    }

    if (showServerSheet) {
        ServerSwitcherSheet(
            profiles = session.config.serverProfiles,
            activeProfileId = session.config.activeProfileId,
            realtimeConnected = session.realtimeConnected,
            onSwitch = onSwitchServer,
            onRename = onRenameServer,
            onRemove = onRemoveServer,
            onAddServer = onAddServer,
            onEditCurrent = onOpenReconfigureServer,
            onDismiss = { showServerSheet = false },
        )
    }
    if (showGithubTokenEditor) {
        GithubTokenDialog(
            savedToken = session.config.githubToken,
            onSave = { token ->
                updateModel.saveGithubToken(token)
                showGithubTokenEditor = false
                onMessage(if (token.isBlank()) "已清除 GitHub 授权" else "已保存 GitHub 授权")
            },
            onDismiss = { showGithubTokenEditor = false },
        )
    }
    if (showDownloadEditor) {
        UpdateDownloadSettingsDialog(
            settings = session.config.updateDownload,
            onSave = { settings ->
                updateModel.setPreferredDownloadSettings(settings)
                showDownloadEditor = false
            },
            onDismiss = { showDownloadEditor = false },
        )
    }
    if (showLogCleanup) {
        LogCleanupDialog(
            accountId = session.config.currentAccountId,
            accountName = session.currentAccount?.displayTitle.orEmpty(),
            onClear = onClearLogs,
            onDismiss = { showLogCleanup = false },
        )
    }
    AppUpdateDialogs(updateModel, session.config)
    if (showServerUpdateSheet) {
        ServerUpdateSheet(
            apiClient = apiClient,
            serverUrl = session.config.serverUrl,
            appGithubToken = session.config.githubToken,
            onServerUpdated = onServerUpdated,
            onMessage = onMessage,
            onDismiss = { showServerUpdateSheet = false },
        )
    }
    if (showCustomAppearanceSheet) {
        CustomAppearanceSheet(
            themeMode = session.config.themeMode,
            onUpdateThemeMode = onUpdateThemeMode,
            config = session.config.customAppearance,
            onUpdate = onUpdateCustomAppearance,
            onReset = { onUpdateCustomAppearance(CustomAppearanceConfig()) },
            onDismiss = { showCustomAppearanceSheet = false },
        )
    }

    if (showNotifySheet) {
        NotificationSettingsSheet(
            config = session.config,
            notificationsGranted = notificationsGranted,
            onRequestPermission = { requestNotificationPermissionAndEnable("activity") },
            onOpenSystemSettings = { ActivityNotificationManager.openSystemNotificationSettings(context) },
            onToggleMaster = { wantEnable ->
                if (!wantEnable) {
                    onToggleActivityPush(false)
                } else {
                    requestNotificationPermissionAndEnable("activity")
                }
            },
            onToggleCollapse = onToggleNotifyCollapse,
            onToggleSilent = onToggleNotifySilent,
            onToggleAllAccounts = onToggleNotifyAllAccounts,
            onToggleType = onToggleNotifyType,
            onApplyTypes = onApplyNotifyTypes,
            onSendTest = {
                if (!notificationsGranted) {
                    requestNotificationPermissionAndEnable("permission_only")
                } else if (onSendTestNotification()) {
                    onMessage("已发送测试通知，请在通知栏查看效果")
                } else {
                    onMessage("发送失败，请检查系统通知权限")
                }
            },
            onDismiss = { showNotifySheet = false },
        )
    }

    if (showLogoutConfirm) {
        AppleAlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出当前服务器登录") },
            text = { Text("确定清除本机保存的 Token 并返回服务器连接页面吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onDisconnect()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = apple.red),
                ) { Text("退出登录") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsSheet(
    config: StoredConfig,
    notificationsGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleCollapse: (Boolean) -> Unit,
    onToggleSilent: (Boolean) -> Unit,
    onToggleAllAccounts: (Boolean) -> Unit,
    onToggleType: (String, Boolean) -> Unit,
    onApplyTypes: (Set<String>) -> Unit,
    onSendTest: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 640.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "农牧动态推送",
                        style = MaterialTheme.typography.titleLarge,
                        color = apple.primaryText,
                    )
                    Text(
                        text = "默认关闭；仅推送所选农牧与好友动作，自动过滤系统杂讯",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!notificationsGranted) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = apple.orange.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsOff,
                                    contentDescription = null,
                                    tint = apple.orange,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "未授予系统通知权限",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = apple.primaryText,
                                    )
                                    Text(
                                        text = "Android 系统要求先授权通知权限，才能在通知栏显示动态",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = apple.secondaryText,
                                    )
                                }
                                Button(
                                    onClick = onRequestPermission,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text("授权", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                item {
                    AppleGroupedCard {
                        AppleSwitchRow(
                            icon = Icons.Rounded.NotificationsActive,
                            iconTint = apple.emerald,
                            title = "开启农牧动态推送",
                            subtitle = "开启后仅从当前时刻起推送新发生的动作",
                            checked = config.activityPushEnabled,
                            onCheckedChange = onToggleMaster,
                        )
                    }
                }

                item {
                    AppleSectionHeader(title = "防堆积与显示策略")
                    Spacer(modifier = Modifier.height(4.dp))
                    AppleGroupedCard {
                        AppleSwitchRow(
                            icon = Icons.Rounded.FilterList,
                            iconTint = apple.blue,
                            title = "按类别合并防堆积",
                            subtitle = "农场、好友、牧场、收益各自聚合为 1 条摘要，最多保留最近 5 条明细",
                            checked = config.notifyCollapseEnabled,
                            onCheckedChange = onToggleCollapse,
                        )
                        AppleHairlineDivider()
                        AppleSwitchRow(
                            icon = Icons.Rounded.VolumeOff,
                            iconTint = apple.indigo,
                            title = "合并更新时静音",
                            subtitle = "同类别新消息就地刷新内容，不反复响铃或震动打扰",
                            checked = config.notifySilentUpdate,
                            onCheckedChange = onToggleSilent,
                            enabled = config.notifyCollapseEnabled,
                        )
                        AppleHairlineDivider()
                        AppleSwitchRow(
                            icon = Icons.Rounded.Group,
                            iconTint = apple.green,
                            title = "监听全部运行中账号",
                            subtitle = "关闭时仅推送当前选中的农场账号动态",
                            checked = config.notifyAllAccounts,
                            onCheckedChange = onToggleAllAccounts,
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppleFilterChip(
                            selected = config.enabledNotifyTypes == ActivityNotifyType.defaultKeys,
                            onClick = { onApplyTypes(ActivityNotifyType.defaultKeys) },
                            label = { Text("推荐精选") },
                        )
                        AppleFilterChip(
                            selected = config.enabledNotifyTypes.size == ActivityNotifyType.entries.size,
                            onClick = { onApplyTypes(ActivityNotifyType.allKeys) },
                            label = { Text("全部开启") },
                        )
                        AppleFilterChip(
                            selected = config.enabledNotifyTypes.isEmpty(),
                            onClick = { onApplyTypes(emptySet()) },
                            label = { Text("全部取消") },
                        )
                    }
                }

                ActivityNotifyGroup.entries.forEach { group ->
                    val groupTypes = ActivityNotifyType.entries.filter { it.group == group }
                    val groupIcon = when (group) {
                        ActivityNotifyGroup.FARM -> Icons.Rounded.Eco
                        ActivityNotifyGroup.FRIEND -> Icons.Rounded.Group
                        ActivityNotifyGroup.RANCH -> Icons.Rounded.Pets
                        ActivityNotifyGroup.ECONOMY -> Icons.Rounded.Paid
                    }
                    val groupTint = when (group) {
                        ActivityNotifyGroup.FARM -> apple.green
                        ActivityNotifyGroup.FRIEND -> apple.blue
                        ActivityNotifyGroup.RANCH -> apple.orange
                        ActivityNotifyGroup.ECONOMY -> apple.emerald
                    }

                    item {
                        AppleSectionHeader(title = group.label)
                        Spacer(modifier = Modifier.height(4.dp))
                        AppleGroupedCard {
                            groupTypes.forEachIndexed { idx, type ->
                                AppleSwitchRow(
                                    icon = groupIcon,
                                    iconTint = groupTint,
                                    title = type.label,
                                    subtitle = type.description,
                                    checked = type.key in config.enabledNotifyTypes,
                                    onCheckedChange = { checked -> onToggleType(type.key, checked) },
                                )
                                if (idx < groupTypes.lastIndex) {
                                    AppleHairlineDivider()
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenSystemSettings,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("系统通知设置")
                }
                Button(
                    onClick = onSendTest,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Text("发送测试通知")
                }
            }
        }
    }
}

private fun openGithubRepository(
    context: Context,
    repoUrl: String,
    onError: (String) -> Unit,
) {
    val uri = Uri.parse(repoUrl)
    val launchedGithubApp = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.github.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.isSuccess
    if (launchedGithubApp) return

    val launchedBrowser = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.isSuccess
    if (!launchedBrowser) {
        onError("未找到可打开链接的应用，仓库地址：$repoUrl")
    }
}
