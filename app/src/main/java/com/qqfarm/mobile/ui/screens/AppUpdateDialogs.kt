package com.qqfarm.mobile.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qqfarm.mobile.BuildConfig
import com.qqfarm.mobile.data.network.DEFAULT_UPDATE_ACCELERATOR
import com.qqfarm.mobile.data.network.normalizeUpdateAccelerator
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.state.AppUpdateViewModel
import com.qqfarm.mobile.ui.state.UpdateUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import java.io.File
import java.util.Locale

@Composable
internal fun AcceleratorAddressDialog(currentAddress: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var address by rememberSaveable { mutableStateOf(currentAddress) }
    val normalized = remember(address) { normalizeUpdateAccelerator(address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppleRadius.card),
        title = { Text("下载加速地址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("HTTPS 地址") },
                    placeholder = { Text(DEFAULT_UPDATE_ACCELERATOR) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    isError = address.isNotBlank() && normalized == null,
                    supportingText = { Text("支持输入域名或完整 HTTPS 地址，服务需支持拼接 GitHub 下载链接") },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { address = DEFAULT_UPDATE_ACCELERATOR }) { Text("恢复默认地址") }
            }
        },
        confirmButton = {
            Button(onClick = { normalized?.let(onSave) }, enabled = normalized != null) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
internal fun AppUpdateDialogs(model: AppUpdateViewModel, config: StoredConfig) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ready = state as? UpdateUiState.Ready
    var installError by remember(ready?.apk) { mutableStateOf<String?>(null) }

    fun launchInstaller(apk: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        } catch (_: ActivityNotFoundException) {
            installError = "设备上没有可用的 APK 安装程序"
        } catch (_: SecurityException) {
            installError = "系统未允许安装，请检查应用安装权限"
        }
    }

    val installPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val pending = model.state.value as? UpdateUiState.Ready
        if (pending != null && context.packageManager.canRequestPackageInstalls()) launchInstaller(pending.apk)
    }

    fun install(apk: File) {
        installError = null
        if (context.packageManager.canRequestPackageInstalls()) {
            launchInstaller(apk)
        } else {
            try {
                installPermission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", context.packageName, null)))
            } catch (_: ActivityNotFoundException) {
                installError = "请在设备的系统设置中为此应用开启安装权限"
            }
        }
    }

    LaunchedEffect(ready) {
        model.takeAutomaticInstall()?.let(::install)
    }

    val dialogShape = RoundedCornerShape(AppleRadius.card)
    when (val current = state) {
        UpdateUiState.Idle -> Unit
        UpdateUiState.Checking -> AlertDialog(
            onDismissRequest = model::dismiss,
            shape = dialogShape,
            title = { Text("检查更新") },
            text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = model::dismiss) { Text("取消") } },
        )
        is UpdateUiState.Authorization -> {
            var token by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = model::dismiss,
                shape = dialogShape,
                title = { Text("GitHub 授权") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(current.message)
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text("GitHub Token") },
                            supportingText = { Text("使用有此仓库 Contents 读取权限的细粒度 Token，仅保留在本次运行中") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { model.check(config, token) }, enabled = token.isNotBlank()) { Text("检查更新") }
                },
                dismissButton = { TextButton(onClick = model::dismiss) { Text("取消") } },
            )
        }
        is UpdateUiState.Notice -> AlertDialog(
            onDismissRequest = model::dismiss,
            shape = dialogShape,
            title = { Text(current.title) },
            text = { Text(current.message) },
            confirmButton = { TextButton(onClick = model::dismiss) { Text("确定") } },
        )
        is UpdateUiState.Available -> UpdateAvailableDialog(
            update = current,
            onSetAcceleration = model::setDownloadAcceleration,
            onConfirm = model::download,
            onDismiss = model::dismiss,
        )
        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = {},
            shape = dialogShape,
            title = { Text("正在下载 ${current.update.release.version}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(progress = { current.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${current.percent}%")
                }
            },
            confirmButton = { TextButton(onClick = model::dismiss) { Text("取消下载") } },
        )
        is UpdateUiState.Ready -> AlertDialog(
            onDismissRequest = model::dismiss,
            shape = dialogShape,
            title = { Text("准备安装") },
            text = { Text(installError ?: "安装包已下载和校验，请在系统界面允许安装并确认更新。") },
            confirmButton = { Button(onClick = { install(current.apk) }) { Text("继续安装") } },
            dismissButton = { TextButton(onClick = model::dismiss) { Text("取消") } },
        )
    }
}

@Composable
private fun UpdateAvailableDialog(
    update: UpdateUiState.Available,
    onSetAcceleration: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val notes = update.release.notes
    val sizeLabel = remember(update.release.size) {
        val mb = update.release.size / (1024.0 * 1024.0)
        if (mb >= 0.1) String.format(Locale.US, "约 %.1f MB", mb) else "${update.release.size / 1024} KB"
    }
    val formattedNotes = remember(notes) {
        buildAnnotatedString {
            notes.trim().lineSequence().forEachIndexed { index, line ->
                if (index > 0) append('\n')
                val heading = releaseHeading.matchEntire(line.trim())
                val text = (heading?.groupValues?.get(1) ?: line)
                    .replace(releaseBullet, "$1• ")
                    .replace("**", "").replace("`", "")
                if (heading != null) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text) }
                else append(text)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppleRadius.card),
        title = { Text("发现新版本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "v${BuildConfig.VERSION_NAME} → v${update.release.version}（$sizeLabel）",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (notes.isBlank()) {
                    Text("该版本暂无发布说明")
                } else {
                    Text("更新内容", style = MaterialTheme.typography.titleSmall)
                    Text(
                        formattedNotes,
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                    )
                }
                val accelerator = update.accelerator
                if (accelerator != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(if (update.useAcceleration) "本次使用下载加速" else "本次直连 GitHub")
                            Text(
                                text = if (update.useAcceleration) accelerator else "https://github.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.secondaryText,
                            )
                        }
                        AppleSwitch(checked = update.useAcceleration, onCheckedChange = onSetAcceleration)
                    }
                } else {
                    Text(
                        if (update.release.requiresAuthorization) "私有仓库使用 GitHub 授权直连下载"
                        else "发布的安装包缺少校验值，本次直连 GitHub",
                    )
                }
                update.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("下载并安装") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private val releaseHeading = Regex("^#{1,6}\\s+(.+)$")
private val releaseBullet = Regex("^(\\s*)[-*]\\s+")
