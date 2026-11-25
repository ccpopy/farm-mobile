package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.data.model.ServerVersionInfo
import com.qqfarm.mobile.data.prefs.ServerProfile
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleIconBadge
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleSettingRow
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.Eco

@Composable
fun ServerSetupScreen(
    initialConfig: StoredConfig,
    isReconfiguring: Boolean,
    onBack: () -> Unit,
    onTestConnection: suspend (String) -> Result<ServerVersionInfo>,
    onConnectWithToken: suspend (String, String) -> Result<Unit>,
    onConnectWithPassword: suspend (String, String, String, Boolean) -> Result<String>,
    onForgetPassword: (String) -> Unit,
    onSuccessMessage: (String) -> Unit,
    isAddingServer: Boolean = false,
    otherServers: List<ServerProfile> = emptyList(),
    onSwitchServer: (String) -> Unit = {},
) {
    val apple = LocalAppleColors.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember(initialConfig.serverUrl) {
        mutableStateOf(initialConfig.serverUrl)
    }
    var authTabIndex by remember(initialConfig.authMode) {
        mutableIntStateOf(
            if (initialConfig.authMode == StoredConfig.AUTH_MODE_API_TOKEN && (initialConfig.authToken.isNotBlank() || initialConfig.savedPassword.isEmpty())) 1 else 0
        )
    }
    var apiToken by remember(initialConfig.authToken) {
        mutableStateOf(
            if (initialConfig.authMode == StoredConfig.AUTH_MODE_API_TOKEN) initialConfig.authToken else ""
        )
    }
    var showToken by remember { mutableStateOf(false) }

    var username by remember(initialConfig.username) { mutableStateOf(initialConfig.username) }
    var password by remember { mutableStateOf(initialConfig.savedPassword) }
    var passwordFromProfile by remember { mutableStateOf(password.isNotEmpty()) }
    var rememberPassword by remember { mutableStateOf(passwordFromProfile) }
    fun setRememberPassword(enabled: Boolean) {
        rememberPassword = enabled
        if (!enabled && initialConfig.activeProfile?.sealedPassword?.isNotEmpty() == true &&
            FarmApiClient.normalizeBaseUrl(serverUrl) == initialConfig.serverUrl && username.trim() == initialConfig.username
        ) {
            onForgetPassword(initialConfig.activeProfileId)
        }
    }
    var showPassword by remember { mutableStateOf(false) }

    var testing by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var verifiedInfo by remember { mutableStateOf<ServerVersionInfo?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (isReconfiguring) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAddingServer) "返回" else "返回设置")
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppleIconBadge(
                icon = Icons.Rounded.Eco,
                tint = MaterialTheme.colorScheme.primary,
                size = 64.dp,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            )
            Text(
                text = when {
                    isAddingServer -> "添加服务器"
                    isReconfiguring -> "服务器与授权"
                    else -> "QQ农场助手"
                },
                style = MaterialTheme.typography.displaySmall,
                color = apple.primaryText,
            )
            Text(
                text = if (isAddingServer) {
                    "添加后自动切换过去，原服务器保留在「我的 → 服务器」中"
                } else {
                    "配置远程 Node 后端地址并选择安全授权方式"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = apple.secondaryText,
            )
        }

        // 已退出授权时仍可直接切到其它已保存的服务器，不必重新输入。
        if (!isReconfiguring && otherServers.isNotEmpty()) {
            AppleGroupedCard {
                Text(
                    text = "切换到已保存的服务器",
                    style = MaterialTheme.typography.labelLarge,
                    color = apple.secondaryText,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
                otherServers.forEachIndexed { idx, profile ->
                    AppleSettingRow(
                        icon = Icons.Rounded.Dns,
                        iconTint = if (profile.isAuthenticated) apple.blue else apple.secondaryText,
                        title = profile.displayName,
                        subtitle = profile.serverUrl,
                        trailingText = if (profile.isAuthenticated) "切换" else "需授权",
                        trailingColor = if (profile.isAuthenticated) MaterialTheme.colorScheme.primary else apple.secondaryText,
                        showChevron = true,
                        onClick = { onSwitchServer(profile.id) },
                    )
                    if (idx < otherServers.lastIndex) AppleHairlineDivider()
                }
            }
        }

        AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppleIconBadge(icon = Icons.Rounded.Dns, tint = apple.blue, size = 30.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "远程服务器地址",
                        style = MaterialTheme.typography.titleSmall,
                        color = apple.primaryText,
                    )
                    Text(
                        text = "支持局域网 IP、域名或反向代理 HTTPS 地址",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = serverUrl,
                enabled = !submitting && !testing,
                onValueChange = {
                    if (FarmApiClient.normalizeBaseUrl(it) != initialConfig.serverUrl) {
                        if (apiToken.isNotBlank() && apiToken == initialConfig.authToken) {
                            apiToken = ""
                        }
                        if (passwordFromProfile) {
                            passwordFromProfile = false
                            password = ""
                        }
                    }
                    serverUrl = it
                    verifiedInfo = null
                    errorMsg = ""
                },
                label = { Text("服务器 URL") },
                placeholder = { Text("https://farm.example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (verifiedInfo != null) {
                    val v = verifiedInfo!!
                    AppleStatusPill(
                        text = "已连通 · v${v.botVersion.ifBlank { v.clientVersion.ifBlank { "OK" } }}",
                        color = apple.green,
                    )
                } else {
                    Text(
                        text = "建议先测试服务器连通性",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }

                OutlinedButton(
                    onClick = {
                        errorMsg = ""
                        testing = true
                        scope.launch {
                            val res = onTestConnection(serverUrl)
                            res.onSuccess { info ->
                                verifiedInfo = info
                            }.onFailure { err ->
                                verifiedInfo = null
                                errorMsg = err.message ?: "无法连接到服务器"
                            }
                            testing = false
                        }
                    },
                    enabled = !testing && !submitting && serverUrl.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (verifiedInfo != null) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = apple.green,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("测试连接")
                }
            }
        }

        AppleSegmentedControl(
            options = listOf("账号密码登录", "会话 Token"),
            selectedIndex = authTabIndex,
            onSelect = {
                authTabIndex = it
                errorMsg = ""
            },
        )

        AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
            if (authTabIndex == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppleIconBadge(icon = Icons.Rounded.VerifiedUser, tint = apple.emerald, size = 30.dp)
                    Column {
                        Text(
                            text = "管理员登录",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Text(
                            text = "使用与 Web 管理面板相同的用户名和密码",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        if (passwordFromProfile && it.trim() != initialConfig.username) {
                            passwordFromProfile = false
                            password = ""
                        }
                        username = it
                    },
                    label = { Text("用户名") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        passwordFromProfile = false
                        password = it
                    },
                    label = { Text("密码") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "显示/隐藏密码",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .toggleable(value = rememberPassword, enabled = !submitting, role = Role.Switch, onValueChange = ::setRememberPassword)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "记住密码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = apple.primaryText,
                    )
                    AppleSwitch(
                        checked = rememberPassword,
                        onCheckedChange = null,
                        enabled = !submitting,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "适配上游 liyangpengs/qq-farm-bot：会话由服务端内存维护，服务端重启后重新登录即可。",
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppleIconBadge(icon = Icons.Rounded.Key, tint = apple.orange, size = 30.dp)
                    Column {
                        Text(
                            text = "会话 Token 授权",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Text(
                            text = "粘贴已登录 Web 管理面板的 x-admin-token 会话凭证",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("x-admin-token") },
                    placeholder = { Text("粘贴 48 位会话 Token") },
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    clipboard.getText()?.text?.let { pasted ->
                                        apiToken = pasted.trim()
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = "粘贴")
                            }
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "显示/隐藏",
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "可在浏览器开发者工具请求头或本地会话存储中获取当前有效的 x-admin-token。",
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                )
            }
        }

        if (errorMsg.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = apple.red.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.red,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }

        Button(
            onClick = {
                errorMsg = ""
                submitting = true
                scope.launch {
                    if (authTabIndex == 0) {
                        val res = onConnectWithPassword(serverUrl, username, password, rememberPassword)
                        res.onSuccess { note ->
                            onSuccessMessage(note.ifBlank { "登录成功，已连接远程服务器" })
                        }.onFailure { err ->
                            errorMsg = err.message ?: "登录失败"
                        }
                    } else {
                        val res = onConnectWithToken(serverUrl, apiToken)
                        res.onSuccess {
                            onSuccessMessage("已连接远程服务器")
                        }.onFailure { err ->
                            errorMsg = err.message ?: "Token 验证失败"
                        }
                    }
                    submitting = false
                }
            },
            enabled = !submitting && !testing && serverUrl.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = when {
                    isAddingServer -> "验证并添加"
                    isReconfiguring -> "保存并切换"
                    else -> "验证并连接"
                },
                style = MaterialTheme.typography.titleSmall,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
