package com.qqfarm.mobile.ui.screens

import android.Manifest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qqfarm.mobile.BuildConfig
import com.qqfarm.mobile.data.model.ActivityNotifyGroup
import com.qqfarm.mobile.data.model.ActivityNotifyType
import com.qqfarm.mobile.data.network.ActivityNotificationManager
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleSettingRow
import com.qqfarm.mobile.ui.components.AppleSwitchRow
import com.qqfarm.mobile.ui.state.AppUpdateViewModel
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors

@Composable
fun SettingsScreen(
    session: SessionUiState,
    onOpenReconfigureServer: () -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    onOpenAutomation: () -> Unit,
    onDisconnect: () -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onToggleOfflineAlerts: (Boolean) -> Unit,
    onToggleActivityPush: (Boolean) -> Unit,
    onToggleNotifyCollapse: (Boolean) -> Unit,
    onToggleNotifySilent: (Boolean) -> Unit,
    onToggleNotifyAllAccounts: (Boolean) -> Unit,
    onToggleNotifyType: (String, Boolean) -> Unit,
    onApplyNotifyTypes: (Set<String>) -> Unit,
    onSendTestNotification: () -> Boolean,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val updateModel: AppUpdateViewModel = viewModel()

    var showNotifySheet by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showAcceleratorEditor by rememberSaveable { mutableStateOf(false) }

    var notificationsGranted by remember {
        mutableStateOf(ActivityNotificationManager.areNotificationsEnabled(context))
    }
    var pendingPermissionTarget by remember { mutableStateOf<String?>(null) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 0. Official Upstream Automation Entry
        item {
            AppleSectionHeader(title = "自动化与策略")
            Spacer(modifier = Modifier.height(4.dp))
            AppleGroupedCard {
                AppleSettingRow(
                    icon = Icons.Rounded.Tune,
                    iconTint = apple.indigo,
                    title = "自动化设置",
                    subtitle = "种植与养殖策略、日常农牧、施肥、好友与神秘商人",
                    trailingText = session.currentAccount?.name?.takeIf { it.isNotBlank() },
                    showChevron = true,
                    onClick = onOpenAutomation,
                )
            }
        }

        // 1. Server & Auth
        item {
            AppleSectionHeader(title = "服务器与授权")
            Spacer(modifier = Modifier.height(4.dp))
            AppleGroupedCard {
                AppleSettingRow(
                    icon = Icons.Rounded.Dns,
                    iconTint = apple.blue,
                    title = "远程服务器",
                    subtitle = session.config.serverUrl.ifBlank { "未配置" },
                    trailingText = if (session.realtimeConnected) "实时在线" else "修改",
                    trailingColor = if (session.realtimeConnected) apple.green else apple.secondaryText,
                    showChevron = true,
                    onClick = onOpenReconfigureServer,
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Key,
                    iconTint = apple.orange,
                    title = "当前登录凭证",
                    subtitle = if (session.config.authMode == StoredConfig.AUTH_MODE_PASSWORD) {
                        "管理员会话 (${session.config.username.ifBlank { "admin" }}) · 点击复制 Token"
                    } else {
                        "会话 Token 授权 · 点击复制 Token"
                    },
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
                    subtitle = session.currentAccount?.displayTitle ?: "管理、切换与扫码添加账号",
                    trailingText = "${session.accounts.size} 个",
                    showChevron = true,
                    onClick = onOpenAccountSwitcher,
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Logout,
                    iconTint = apple.red,
                    title = "退出当前授权",
                    subtitle = "清除本地保存的会话 Token，保留服务器地址",
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
                    subtitle = if (notificationsGranted) "已允许在本机通知栏显示提醒" else "默认未授权，开启推送前需先授予权限",
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
                    subtitle = "偷取、帮忙、种植、收获、牧场与出售等（屏蔽系统杂讯）",
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
                    subtitle = "监听账号登录失效、被踢下线与异常断连（默认关闭）",
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
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Text(
                    text = "主题外观",
                    style = MaterialTheme.typography.titleSmall,
                    color = apple.primaryText,
                )
                Spacer(modifier = Modifier.height(10.dp))
                val themeIndex = when (session.config.themeMode) {
                    StoredConfig.THEME_LIGHT -> 1
                    StoredConfig.THEME_DARK -> 2
                    else -> 0
                }
                AppleSegmentedControl(
                    options = listOf("跟随系统", "浅色", "深色"),
                    selectedIndex = themeIndex,
                    onSelect = { idx ->
                        val mode = when (idx) {
                            1 -> StoredConfig.THEME_LIGHT
                            2 -> StoredConfig.THEME_DARK
                            else -> StoredConfig.THEME_SYSTEM
                        }
                        onUpdateThemeMode(mode)
                    },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            AppleGroupedCard {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AppleSwitchRow(
                        icon = Icons.Rounded.Palette,
                        iconTint = apple.indigo,
                        title = "动态壁纸取色 (Material You)",
                        subtitle = "开启后主按钮随系统壁纸色调变化；关闭则保持默认农牧配色",
                        checked = session.config.dynamicColor,
                        onCheckedChange = onUpdateDynamicColor,
                    )
                    AppleHairlineDivider()
                }
                AppleSettingRow(
                    icon = Icons.Rounded.SystemUpdateAlt,
                    iconTint = apple.blue,
                    title = "检查更新",
                    trailingText = "v${BuildConfig.VERSION_NAME}",
                    showChevron = true,
                    onClick = { updateModel.check(session.config) },
                )
                AppleHairlineDivider()
                AppleSwitchRow(
                    icon = Icons.Rounded.Speed,
                    iconTint = apple.emerald,
                    title = "下载加速",
                    checked = session.config.useUpdateAcceleration,
                    onCheckedChange = updateModel::setPreferredAcceleration,
                )
                AppleHairlineDivider()
                AppleSettingRow(
                    icon = Icons.Rounded.Link,
                    iconTint = apple.indigo,
                    title = "加速地址",
                    subtitle = session.config.updateAcceleratorUrl,
                    showChevron = true,
                    onClick = { showAcceleratorEditor = true },
                )
            }
        }
    }

    if (showAcceleratorEditor) {
        AcceleratorAddressDialog(
            currentAddress = session.config.updateAcceleratorUrl,
            onSave = { address ->
                updateModel.setAcceleratorUrl(address)
                showAcceleratorEditor = false
            },
            onDismiss = { showAcceleratorEditor = false },
        )
    }
    AppUpdateDialogs(updateModel, session.config)

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
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出授权") },
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
                    .heightIn(max = 500.dp),
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
