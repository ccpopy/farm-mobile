package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import com.qqfarm.mobile.data.network.isValidGithubToken
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.prefs.UpdateDownloadMode
import com.qqfarm.mobile.data.prefs.UpdateDownloadSettings
import com.qqfarm.mobile.data.network.DEFAULT_UPDATE_ACCELERATOR
import com.qqfarm.mobile.data.network.normalizeUpdateAccelerator
import com.qqfarm.mobile.data.network.normalizeUpdateProxy
import com.qqfarm.mobile.ui.components.AppleAlertDialog
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
@Composable
fun UpdateDownloadSettingsDialog(
    settings: UpdateDownloadSettings,
    onSave: (UpdateDownloadSettings) -> Unit,
    onDismiss: () -> Unit,
    canAccelerate: Boolean = true,
    title: String = "下载方式",
) {
    val modes = UpdateDownloadMode.entries.filter { canAccelerate || it != UpdateDownloadMode.ACCELERATOR }
    var selected by rememberSaveable { mutableStateOf(settings.forRelease(canAccelerate).mode) }
    var accelerator by rememberSaveable { mutableStateOf(settings.acceleratorUrl) }
    var proxy by rememberSaveable { mutableStateOf(settings.proxyUrl) }
    val normalizedAccelerator = remember(accelerator) { normalizeUpdateAccelerator(accelerator) }
    val normalizedProxy = remember(proxy) { normalizeUpdateProxy(proxy) }
    val saved = when (selected) {
        UpdateDownloadMode.DIRECT -> settings.copy(mode = selected)
        UpdateDownloadMode.ACCELERATOR -> normalizedAccelerator?.let { settings.copy(mode = selected, acceleratorUrl = it) }
        UpdateDownloadMode.PROXY -> normalizedProxy?.let { settings.copy(mode = selected, proxyUrl = it) }
    }

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppleSegmentedControl(
                    options = modes.map { it.label },
                    selectedIndex = modes.indexOf(selected),
                    onSelect = { selected = modes[it] },
                )
                when (selected) {
                    UpdateDownloadMode.DIRECT -> Text("使用当前网络访问 GitHub", style = MaterialTheme.typography.bodySmall)
                    UpdateDownloadMode.ACCELERATOR -> {
                        OutlinedTextField(
                            value = accelerator,
                            onValueChange = { accelerator = it },
                            label = { Text("加速地址") },
                            placeholder = { Text(DEFAULT_UPDATE_ACCELERATOR) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            isError = accelerator.isNotBlank() && normalizedAccelerator == null,
                            supportingText = { Text("用于公开安装包，填写域名或 HTTPS 地址") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(onClick = { accelerator = DEFAULT_UPDATE_ACCELERATOR }) { Text("恢复默认地址") }
                    }
                    UpdateDownloadMode.PROXY -> OutlinedTextField(
                        value = proxy,
                        onValueChange = { proxy = it },
                        label = { Text("代理地址") },
                        placeholder = { Text("http://192.168.1.2:7890") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        isError = proxy.isNotBlank() && normalizedProxy == null,
                        supportingText = { Text("填写 http://主机:端口 或 socks5://主机:端口") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { saved?.let(onSave) }, enabled = saved != null) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 编辑记住的 GitHub Token：可修改或清除。 */
@Composable
fun GithubTokenDialog(
    savedToken: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var token by remember(savedToken) { mutableStateOf(savedToken) }
    var visible by remember { mutableStateOf(false) }
    val trimmed = token.trim()
    val invalid = trimmed.isNotEmpty() && !isValidGithubToken(trimmed)
    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub 授权") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (savedToken.isBlank()) "可选配置：用于避免 GitHub API 限流或访问受限发行版，保存后检查更新时自动使用。"
                    else "已保存 Token，检查更新时自动使用。"
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    isError = invalid,
                    supportingText = if (invalid) ({ Text("Token 只能包含可见的 ASCII 字符") }) else null,
                    label = { Text("GitHub Token") },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { visible = !visible }) { Text(if (visible) "隐藏" else "显示") }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(trimmed) }, enabled = isValidGithubToken(trimmed) && trimmed != savedToken) { Text("保存") }
        },
        dismissButton = {
            if (savedToken.isNotBlank()) {
                TextButton(onClick = { onSave("") }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

