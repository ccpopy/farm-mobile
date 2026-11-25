package com.qqfarm.mobile.ui.components

import androidx.compose.foundation.clickable
import com.qqfarm.mobile.platform.rememberAppHaptics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.prefs.ServerProfile
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.ui.theme.LocalAppleColors

/**
 * 「我的 → 服务器」抽屉：列出已保存的远程服务器，点一下即切换；
 * 每项可重命名、删除（当前服务器不可删），底部可添加新服务器或修改当前连接。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSwitcherSheet(
    profiles: List<ServerProfile>,
    activeProfileId: String,
    realtimeConnected: Boolean,
    onSwitch: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onAddServer: () -> Unit,
    onEditCurrent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renaming by remember { mutableStateOf<ServerProfile?>(null) }
    var removing by remember { mutableStateOf<ServerProfile?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 620.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp),
            ) {
                Text("服务器", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                Text(
                    text = "点选即切换，各服务器的授权与上次选中的账号会分别保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                )
            }

            AppleHairlineDivider(startIndent = 16.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppleGroupedCard {
                    profiles.forEachIndexed { idx, profile ->
                        val active = profile.id == activeProfileId
                        ServerProfileRow(
                            profile = profile,
                            active = active,
                            realtimeConnected = realtimeConnected,
                            onClick = {
                                if (!active) {
                                    onSwitch(profile.id)
                                    onDismiss()
                                }
                            },
                            onRename = { renaming = profile },
                            onRemove = { removing = profile },
                        )
                        if (idx < profiles.lastIndex) AppleHairlineDivider(startIndent = 62.dp)
                    }
                }

                AppleGroupedCard {
                    AppleSettingRow(
                        icon = Icons.Rounded.Add,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "添加服务器",
                        subtitle = "填写地址并授权，成功后自动切换过去",
                        showChevron = true,
                        onClick = {
                            onDismiss()
                            onAddServer()
                        },
                    )
                    AppleHairlineDivider()
                    AppleSettingRow(
                        icon = Icons.Rounded.Tune,
                        iconTint = apple.blue,
                        title = "修改当前服务器连接",
                        subtitle = "更换地址或重新授权",
                        showChevron = true,
                        onClick = {
                            onDismiss()
                            onEditCurrent()
                        },
                    )
                }
            }
        }
    }

    renaming?.let { profile ->
        var name by remember(profile.id) { mutableStateOf(profile.name) }
        AppleAlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("重命名服务器") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("备注名") },
                    placeholder = { Text(profile.displayName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRename(profile.id, name)
                    renaming = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("取消") } },
        )
    }

    removing?.let { profile ->
        AppleAlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("删除服务器") },
            text = { Text("将从本机移除「${profile.displayName}」及其保存的授权，不影响服务器本身。") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove(profile.id)
                        removing = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = apple.red,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                    ),
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ServerProfileRow(
    profile: ServerProfile,
    active: Boolean,
    realtimeConnected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.tick()
                onClick()
            }
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleIconBadge(
            icon = Icons.Rounded.Dns,
            tint = when {
                active -> MaterialTheme.colorScheme.primary
                profile.isAuthenticated -> apple.blue
                else -> apple.secondaryText
            },
            size = 34.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = apple.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = profile.serverUrl,
                style = MaterialTheme.typography.bodySmall,
                color = apple.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val status = when {
                active && realtimeConnected -> "当前 · 实时在线"
                active -> "当前使用"
                !profile.isAuthenticated -> "需要重新授权"
                profile.authMode == StoredConfig.AUTH_MODE_PASSWORD -> "管理员会话 · ${profile.username}"
                else -> "API Token"
            }
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    active -> MaterialTheme.colorScheme.primary
                    !profile.isAuthenticated -> apple.orange
                    else -> apple.secondaryText
                },
            )
        }
        if (active) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "当前服务器",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "更多", tint = apple.secondaryText)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text(if (active) "删除（请先切换到其它服务器）" else "删除", color = if (active) apple.secondaryText else apple.red) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = if (active) apple.secondaryText else apple.red)
                    },
                    enabled = !active,
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
    }
}
