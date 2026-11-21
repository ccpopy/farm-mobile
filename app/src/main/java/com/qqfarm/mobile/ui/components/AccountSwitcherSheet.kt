package com.qqfarm.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.LoginConfigModel
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    accounts: List<AccountItem>,
    currentAccountId: String,
    apiClient: FarmApiClient,
    onDismiss: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onStartAccount: (String) -> Unit,
    onStopAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onAccountAdded: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddSheet by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<AccountItem?>(null) }

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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "切换农场账号",
                        style = MaterialTheme.typography.titleLarge,
                        color = apple.primaryText,
                    )
                    Text(
                        text = "共 ${accounts.size} 个账号",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
                Button(
                    onClick = { showAddSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加账号")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (accounts.isEmpty()) {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Person,
                        title = "暂无已配置账号",
                        subtitle = "点击右上角添加 QQ 或微信农场账号",
                        actionLabel = "立即添加",
                        onAction = { showAddSheet = true },
                    )
                }
            } else {
                AppleGroupedCard {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                    ) {
                        itemsIndexed(accounts, key = { _, acc -> acc.id }) { index, acc ->
                            val isSelected = acc.id == currentAccountId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectAccount(acc.id)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.size(42.dp)) {
                                    RemoteAssetImage(
                                        url = acc.avatar,
                                        contentDescription = acc.displayTitle,
                                        fallbackIcon = Icons.Rounded.Person,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(apple.track),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(apple.card)
                                            .padding(1.5.dp)
                                            .clip(CircleShape)
                                            .background(if (acc.running) apple.green else apple.secondaryText.copy(alpha = 0.5f)),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = acc.displayTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = apple.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = if (acc.platform == "wx") apple.green.copy(alpha = 0.14f) else apple.blue.copy(alpha = 0.14f),
                                        ) {
                                            Text(
                                                text = acc.platformLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (acc.platform == "wx") apple.green else apple.blue,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (acc.running) "运行中" else "已停止",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (acc.running) apple.green else apple.secondaryText,
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "当前账号",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (acc.running) onStopAccount(acc.id) else onStartAccount(acc.id)
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (acc.running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = if (acc.running) "停止" else "启动",
                                        tint = if (acc.running) apple.orange else apple.green,
                                    )
                                }
                                IconButton(
                                    onClick = { deleteCandidate = acc },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "删除账号",
                                        tint = apple.red.copy(alpha = 0.8f),
                                    )
                                }
                            }
                            if (index < accounts.lastIndex) {
                                AppleHairlineDivider(startIndent = 68.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddAccountSheet(
            apiClient = apiClient,
            onDismiss = { showAddSheet = false },
            onSaved = {
                showAddSheet = false
                onAccountAdded()
            },
        )
    }

    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("删除账号") },
            text = { Text("确定删除账号「${target.displayTitle}」？这会停止其运行并移除配置。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                        onDeleteAccount(target.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = apple.red),
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    apiClient: FarmApiClient,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var loginConfig by remember { mutableStateOf(LoginConfigModel()) }
    LaunchedEffect(Unit) {
        runCatching { apiClient.fetchLoginConfig() }.onSuccess { loginConfig = it }
    }

    val tabs = remember(loginConfig) {
        buildList {
            add("手动 Code")
            if (loginConfig.wechatQrLogin) add("微信扫码")
            if (loginConfig.qqQrLogin) add("QQ 扫码")
        }
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTab = tabs.getOrElse(selectedTabIndex) { "手动 Code" }

    var name by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("qq") }
    var code by remember { mutableStateOf("") }
    var wxSessionText by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // QR flow states
    var wxTaskId by remember { mutableStateOf("") }
    var wxQrUrl by remember { mutableStateOf("") }
    var wxStatusText by remember { mutableStateOf("") }
    var qqTaskId by remember { mutableStateOf("") }
    var qqQrImage by remember { mutableStateOf("") }
    var qqStatusText by remember { mutableStateOf("") }

    fun cleanupQrTasks() {
        val wId = wxTaskId
        val qId = qqTaskId
        wxTaskId = ""
        wxQrUrl = ""
        qqTaskId = ""
        qqQrImage = ""
        if (wId.isNotBlank()) scope.launch { apiClient.cancelWxQrTask(wId) }
        if (qId.isNotBlank()) scope.launch { apiClient.cancelQqQrTask(qId) }
    }

    DisposableEffect(Unit) {
        onDispose { cleanupQrTasks() }
    }

    suspend fun startWxQr() {
        cleanupQrTasks()
        errorMsg = ""
        wxStatusText = "正在获取微信二维码..."
        try {
            val task = apiClient.createWxQrTask()
            wxTaskId = task.taskId
            wxQrUrl = task.qrUrl
            wxStatusText = "请使用微信扫一扫授权登录"
        } catch (e: Exception) {
            errorMsg = e.message ?: "获取微信二维码失败"
            wxStatusText = ""
        }
    }

    suspend fun startQqQr() {
        cleanupQrTasks()
        errorMsg = ""
        qqStatusText = "正在获取 QQ 登录二维码..."
        try {
            val task = apiClient.createQqQrTask()
            qqTaskId = task.taskId
            qqQrImage = task.qrImage
            qqStatusText = "请使用手机 QQ 扫码登录"
        } catch (e: Exception) {
            errorMsg = e.message ?: "获取 QQ 二维码失败"
            qqStatusText = ""
        }
    }

    LaunchedEffect(selectedTab) {
        errorMsg = ""
        when (selectedTab) {
            "微信扫码" -> startWxQr()
            "QQ 扫码" -> startQqQr()
            else -> cleanupQrTasks()
        }
    }

    LaunchedEffect(wxTaskId) {
        val currentId = wxTaskId
        if (currentId.isBlank()) return@LaunchedEffect
        while (wxTaskId == currentId) {
            delay(1400L)
            if (wxTaskId != currentId) break
            try {
                val res = apiClient.pollWxQrStatus(currentId)
                when (res.status) {
                    "waiting" -> wxStatusText = "等待微信扫码..."
                    "scanned" -> wxStatusText = "已扫码，请在手机微信上确认"
                    "authorized" -> {
                        if (name.trim().isEmpty()) {
                            wxStatusText = "已确认授权，请先在上方填写账号备注"
                        } else {
                            wxStatusText = "正在建立会话..."
                            apiClient.confirmWxQr(currentId)
                            val claimedCode = apiClient.claimWxQrCode(currentId)
                            apiClient.saveAccount(
                                name = name.trim(),
                                platform = "wx",
                                code = claimedCode,
                                loginType = "wechat_qr",
                                wxLoginTaskId = currentId,
                            )
                            wxTaskId = ""
                            onSaved()
                            break
                        }
                    }
                    "cancelled", "expired", "failed" -> {
                        wxStatusText = "二维码已失效，请点击刷新重试"
                        break
                    }
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: "微信扫码状态查询失败"
                break
            }
        }
    }

    LaunchedEffect(qqTaskId) {
        val currentId = qqTaskId
        if (currentId.isBlank()) return@LaunchedEffect
        while (qqTaskId == currentId) {
            delay(1400L)
            if (qqTaskId != currentId) break
            try {
                val res = apiClient.pollQqQrStatus(currentId)
                if (res.qrImage.isNotBlank()) qqQrImage = res.qrImage
                when (res.status) {
                    "waiting_scan" -> qqStatusText = "等待 QQ 扫码..."
                    "scanned" -> qqStatusText = "已扫码，请在手机 QQ 上确认"
                    "confirmed" -> {
                        if (name.trim().isEmpty()) {
                            qqStatusText = "已确认授权，请先在上方填写账号备注"
                        } else {
                            qqStatusText = "正在获取授权 Code..."
                            val claimedCode = apiClient.claimQqQrCode(currentId)
                            apiClient.saveAccount(
                                name = name.trim(),
                                platform = "qq",
                                code = claimedCode,
                                loginType = "manual",
                            )
                            qqTaskId = ""
                            onSaved()
                            break
                        }
                    }
                    "cancelled", "expired", "failed" -> {
                        qqStatusText = "二维码已失效，请点击刷新重试"
                        break
                    }
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: "QQ 扫码状态查询失败"
                break
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "添加农场账号",
                style = MaterialTheme.typography.titleLarge,
                color = apple.primaryText,
            )

            if (tabs.size > 1) {
                AppleSegmentedControl(
                    options = tabs,
                    selectedIndex = selectedTabIndex,
                    onSelect = { selectedTabIndex = it },
                )
            }

            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账号备注（必填）") },
                    placeholder = { Text("例如：大号 / 小号") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (selectedTab == "手动 Code") {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppleSegmentedControl(
                        options = listOf("QQ 农场", "微信农场"),
                        selectedIndex = if (platform == "wx") 1 else 0,
                        onSelect = { platform = if (it == 1) "wx" else "qq" },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("登录 Code 或完整链接") },
                        placeholder = { Text("粘贴 code 或含 ?code=... 的链接") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (platform == "wx") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = wxSessionText,
                            onValueChange = { wxSessionText = it },
                            label = { Text("微信会话 JSON（可选）") },
                            placeholder = { Text("留空则仅使用 Code 登录") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    val qrUrlToShow = if (selectedTab == "微信扫码") wxQrUrl else qqQrImage
                    val qrStatus = if (selectedTab == "微信扫码") wxStatusText else qqStatusText
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(196.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, apple.separator),
                        ) {
                            if (qrUrlToShow.isNotBlank()) {
                                RemoteAssetImage(
                                    url = qrUrlToShow,
                                    contentDescription = "登录二维码",
                                    fallbackIcon = Icons.Rounded.QrCode2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        if (qrStatus.isNotBlank()) {
                            Text(
                                text = qrStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = apple.secondaryText,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    if (selectedTab == "微信扫码") startWxQr() else startQqQr()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("刷新二维码")
                        }
                    }
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
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (selectedTab == "手动 Code") {
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isEmpty()) {
                            errorMsg = "请先填写账号备注"
                            return@Button
                        }
                        var finalCode = code.trim()
                        val match = Regex("""[?&]code=([^&]+)""", RegexOption.IGNORE_CASE).find(finalCode)
                        if (match != null) {
                            finalCode = java.net.URLDecoder.decode(match.groupValues[1], "UTF-8")
                        }
                        val sessionJson = if (platform == "wx" && wxSessionText.isNotBlank()) {
                            try {
                                JSONObject(wxSessionText.trim())
                            } catch (_: Exception) {
                                errorMsg = "微信会话 JSON 格式无效"
                                return@Button
                            }
                        } else null

                        if (finalCode.isEmpty() && sessionJson == null) {
                            errorMsg = "请输入登录 Code"
                            return@Button
                        }

                        saving = true
                        errorMsg = ""
                        scope.launch {
                            try {
                                apiClient.saveAccount(
                                    name = trimmedName,
                                    platform = platform,
                                    code = finalCode,
                                    loginType = "manual",
                                    wxSessionJson = sessionJson,
                                )
                                onSaved()
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "保存账号失败"
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("保存并启动", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
