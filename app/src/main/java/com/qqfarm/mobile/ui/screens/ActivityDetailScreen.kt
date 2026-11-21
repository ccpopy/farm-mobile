package com.qqfarm.mobile.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.ActivityDirectoryItem
import com.qqfarm.mobile.data.model.AutumnWishState
import com.qqfarm.mobile.data.model.CharityState
import com.qqfarm.mobile.data.model.CommerceItem
import com.qqfarm.mobile.data.model.FriendItem
import com.qqfarm.mobile.data.model.MilkTeaCandidate
import com.qqfarm.mobile.data.model.MilkTeaGoods
import com.qqfarm.mobile.data.model.MilkTeaInteraction
import com.qqfarm.mobile.data.model.MilkTeaState
import com.qqfarm.mobile.data.model.MugeState
import com.qqfarm.mobile.data.model.MugeTier
import com.qqfarm.mobile.data.model.QingMeiState
import com.qqfarm.mobile.data.model.QixiState
import com.qqfarm.mobile.data.model.SeasonalSimpleStage
import com.qqfarm.mobile.data.model.ShareRewardState
import com.qqfarm.mobile.data.model.StellarShopGoods
import com.qqfarm.mobile.data.model.StellarSnapshot
import com.qqfarm.mobile.data.model.WeatherState
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleQuantityStepper
import com.qqfarm.mobile.ui.components.AppleScrollablePills
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.AppleSubPageHeader
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.components.formatTimestamp
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.AppleSearchField
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs
import com.qqfarm.mobile.ui.components.AppleAlertDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityDetailScreen(
    gameplayKey: String,
    directoryItem: ActivityDirectoryItem,
    session: SessionUiState,
    apiClient: FarmApiClient,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val accountId = session.config.currentAccountId

    var clockTickMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            clockTickMs = SystemClock.elapsedRealtime()
        }
    }

    var activeRules by remember(gameplayKey, directoryItem.id) { mutableStateOf<List<String>>(emptyList()) }
    var showRulesSheet by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var headerSubtitle by remember(directoryItem.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        AppleSubPageHeader(
            title = directoryItem.name,
            subtitle = headerSubtitle,
            onBack = onBack,
            trailing = {
                if (activeRules.isNotEmpty()) {
                    IconButton(onClick = { showRulesSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = "活动规则",
                            tint = apple.secondaryText,
                        )
                    }
                }
                IconButton(onClick = { refreshTrigger++ }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        Box(modifier = Modifier.weight(1f)) {
            when (gameplayKey) {
                "muge" -> MugeDetailContent(
                    accountId = accountId,
                    apiClient = apiClient,
                    clockTickMs = clockTickMs,
                    refreshTrigger = refreshTrigger,
                    onSubtitle = { headerSubtitle = it },
                    onRules = { activeRules = it },
                    onMessage = onMessage,
                )

                "milk" -> MilkTeaDetailContent(
                    accountId = accountId,
                    apiClient = apiClient,
                    clockTickMs = clockTickMs,
                    refreshTrigger = refreshTrigger,
                    onSubtitle = { headerSubtitle = it },
                    onRules = { activeRules = it },
                    onMessage = onMessage,
                )

                "happy" -> ShareRewardDetailContent(
                    accountId = accountId,
                    activityId = directoryItem.instanceId?.ifBlank { null } ?: directoryItem.id,
                    apiClient = apiClient,
                    clockTickMs = clockTickMs,
                    refreshTrigger = refreshTrigger,
                    onSubtitle = { headerSubtitle = it },
                    onRules = { activeRules = it },
                    onMessage = onMessage,
                )

                "wish" -> AutumnWishDetailContent(
                    accountId = accountId,
                    apiClient = apiClient,
                    clockTickMs = clockTickMs,
                    refreshTrigger = refreshTrigger,
                    onSubtitle = { headerSubtitle = it },
                    onRules = { activeRules = it },
                    onMessage = onMessage,
                )

                "stellar" -> StellarDetailContent(
                    accountId = accountId,
                    apiClient = apiClient,
                    clockTickMs = clockTickMs,
                    refreshTrigger = refreshTrigger,
                    onSubtitle = { headerSubtitle = it },
                    onMessage = onMessage,
                )

                "qixi" -> QixiDetailContent(
                    accountId = accountId,
                    apiClient = apiClient,
                    refreshTrigger = refreshTrigger,
                    onSubtitle = { headerSubtitle = it },
                    onMessage = onMessage,
                )

                "qingmei" -> QingMeiDetailContent(
                    accountId = accountId,
                    apiClient = apiClient,
                    refreshTrigger = refreshTrigger,
                    onMessage = onMessage,
                )

                "charity" -> CharityDetailContent(
                    accountId = accountId,
                    apiClient = apiClient,
                    refreshTrigger = refreshTrigger,
                    onSubtitle = { headerSubtitle = it },
                    onMessage = onMessage,
                )

                "weather" -> WeatherDetailContent(
                    accountId = accountId,
                    apiClient = apiClient,
                    refreshTrigger = refreshTrigger,
                    onSubtitle = { headerSubtitle = it },
                    onMessage = onMessage,
                )

                else -> AppleEmptyState(
                    icon = Icons.Rounded.Celebration,
                    title = "等待活动适配",
                    subtitle = "当前版本暂未内置「${directoryItem.name}」的原生交互视图",
                )
            }
        }
    }

    if (showRulesSheet && activeRules.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showRulesSheet = false },
            sheetState = sheetState,
            containerColor = apple.card,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${directoryItem.name} · 活动规则",
                    style = MaterialTheme.typography.titleLarge,
                    color = apple.primaryText,
                )
                activeRules.forEach { line ->
                    val isTitle = line.trim().startsWith("【") && line.trim().endsWith("】")
                    Text(
                        text = line,
                        style = if (isTitle) {
                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        color = if (isTitle) apple.primaryText else apple.secondaryText,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// =====================================================================
// Shared Activity Helpers
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityRewardFlowRow(
    rewards: List<CommerceItem>,
    accentColor: Color = LocalAppleColors.current.emerald,
) {
    val apple = LocalAppleColors.current
    if (rewards.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rewards.forEach { item ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = apple.elevated,
                border = BorderStroke(0.5.dp, apple.separator),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RemoteAssetImage(
                        url = item.image,
                        contentDescription = item.name,
                        fallbackIcon = Icons.Rounded.CardGiftcard,
                        fallbackTint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${item.name.ifBlank { "#${item.id}" }} ×${formatCompactCount(item.count)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = apple.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTierCard(
    title: String,
    subtitle: String? = null,
    statusText: String,
    statusColor: Color,
    highlighted: Boolean,
    rewards: List<CommerceItem>,
    actionLabel: String? = null,
    actionEnabled: Boolean = false,
    onAction: (() -> Unit)? = null,
) {
    val apple = LocalAppleColors.current
    AppleGroupedCard(
        contentPadding = PaddingValues(14.dp),
        containerColor = if (highlighted) statusColor.copy(alpha = 0.06f) else apple.card,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = apple.primaryText,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
            }
            AppleStatusPill(text = statusText, color = statusColor)
        }
        if (rewards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ActivityRewardFlowRow(rewards = rewards, accentColor = statusColor)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAction,
                enabled = actionEnabled,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun formatRemainFromMs(endMs: Long, serverMs: Long, receivedElapsed: Long, clockTickMs: Long): String? {
    if (endMs <= 0L) return null
    val nowMs = if (serverMs > 0L) {
        serverMs + (clockTickMs - receivedElapsed).coerceAtLeast(0L)
    } else {
        System.currentTimeMillis()
    }
    val remSec = (endMs - nowMs) / 1000L
    return if (remSec > 0L) "剩余 ${formatDurationSec(remSec)}" else "已结束"
}

private fun costsToJsonArray(items: List<CommerceItem>): JSONArray {
    val arr = JSONArray()
    items.forEach { cost ->
        arr.put(JSONObject().put("id", cost.id).put("count", cost.count))
    }
    return arr
}

// =====================================================================
// 1. Muge (牧歌初启)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MugeDetailContent(
    accountId: String,
    apiClient: FarmApiClient,
    clockTickMs: Long,
    refreshTrigger: Int,
    onSubtitle: (String?) -> Unit,
    onRules: (List<String>) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<MugeState?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var subTab by remember { mutableIntStateOf(0) }
    var selectedQuestionIdx by remember { mutableIntStateOf(0) }

    fun load() {
        scope.launch {
            loading = true
            errorText = null
            runCatching { apiClient.fetchMuge(accountId) }
                .onSuccess { res ->
                    state = res
                    onRules(res.rules)
                }
                .onFailure { err ->
                    errorText = err.message ?: "读取牧歌初启失败"
                }
            loading = false
        }
    }

    fun operate(action: String, body: JSONObject = JSONObject(), silent: Boolean = false) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { apiClient.operateMuge(accountId, action, body) }
                .onSuccess { (nextState, msg) ->
                    if (nextState != null) {
                        state = nextState
                        onRules(nextState.rules)
                    } else {
                        load()
                    }
                    if (!silent) onMessage(msg)
                }
                .onFailure { err ->
                    onMessage(err.message ?: "操作失败")
                }
            busy = false
        }
    }

    LaunchedEffect(accountId, refreshTrigger) { load() }

    LaunchedEffect(state, clockTickMs) {
        val s = state
        onSubtitle(
            if (s == null) null else {
                formatRemainFromMs(s.endTimeMs, s.serverTimeMs, s.receivedAtElapsedMs, clockTickMs)
                    ?: if (s.active) "进行中" else "已结束"
            }
        )
    }

    val current = state
    if (current == null && !loading && errorText != null) {
        AppleEmptyState(
            icon = Icons.Rounded.Pets,
            title = "加载失败",
            subtitle = errorText.orEmpty(),
            actionLabel = "重试",
            onAction = ::load,
        )
        return
    }

    val celReady = current?.celebrationTiers?.count { it.status == 2 } ?: 0
    val friendReady = current?.friendsTiers?.count { it.status == 2 } ?: 0
    val hbReady = (current?.handbookTiers?.count { it.status == 2 } ?: 0) +
        (current?.handbookTasks?.count { it.status == 2 } ?: 0)
    val tabs = buildList {
        add(if (celReady > 0) "兴牧庆典 ($celReady)" else "兴牧庆典")
        add(if (friendReady > 0) "牧友同行 ($friendReady)" else "牧友同行")
        if (current?.handbookAvailable != false) {
            add(if (hbReady > 0) "成长手册 ($hbReady)" else "成长手册")
        }
        add("牧场人格")
    }
    val safeTab = subTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))

    PullToRefreshBox(
        isRefreshing = loading && current != null,
        onRefresh = ::load,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppleScrollablePills(
                options = tabs,
                selectedIndex = safeTab,
                onSelect = { subTab = it },
                accentColor = apple.orange,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .swipeToSwitchTabs(safeTab, tabs.size) { idx -> subTab = idx },
            ) {
                if (current == null) {
                    AppleLoadingState()
                } else {
                    val isHandbookPresent = current.handbookAvailable
                    val personalityIdx = if (isHandbookPresent) 3 else 2

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    when (safeTab) {
                        0 -> {
                            item {
                                MugeCounterCard(
                                    title = "全服累计饲养",
                                    value = "${formatCompactCount(current.celebrationTotal)} 只",
                                    note = "全服饲养数量达到档位后，所有玩家都可领取",
                                    readyCount = celReady,
                                    busy = busy || !current.active,
                                    onClaimAll = { operate("claimCelebration") },
                                )
                            }
                            items(current.celebrationTiers, key = { "cel_${it.id}" }) { tier ->
                                MugeTierRowCard(
                                    prefix = "累计饲养",
                                    unit = "只",
                                    tier = tier,
                                    accentColor = apple.orange,
                                )
                            }
                        }

                        1 -> {
                            item {
                                MugeCounterCard(
                                    title = "已开启牧场的好友",
                                    value = "${formatCompactCount(current.friendsCount)} 位",
                                    note = "好友关系解除不影响已计数的好友人数",
                                    readyCount = friendReady,
                                    busy = busy || !current.active,
                                    onClaimAll = { operate("claimFriends") },
                                )
                            }
                            items(current.friendsTiers, key = { "fr_${it.id}" }) { tier ->
                                MugeTierRowCard(
                                    prefix = "好友达到",
                                    unit = "位",
                                    tier = tier,
                                    accentColor = apple.orange,
                                )
                            }
                        }

                        2 -> if (isHandbookPresent) {
                            val tierReady = current.handbookTiers.count { it.status == 2 }
                            val taskReady = current.handbookTasks.count { it.status == 2 }
                            item {
                                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            Text("成长积分", style = MaterialTheme.typography.labelMedium, color = apple.secondaryText)
                                            Text(
                                                text = "${current.handbookPoints}",
                                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                                color = apple.orange,
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (taskReady > 0) {
                                                Button(
                                                    onClick = { operate("claimTask") },
                                                    enabled = !busy && current.active,
                                                    shape = RoundedCornerShape(12.dp),
                                                ) {
                                                    Text("领任务 ($taskReady)")
                                                }
                                            }
                                            if (tierReady > 0) {
                                                Button(
                                                    onClick = { operate("claimHandbook") },
                                                    enabled = !busy && current.active,
                                                    shape = RoundedCornerShape(12.dp),
                                                ) {
                                                    Text("领积分档 ($tierReady)")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (current.handbookTiers.isNotEmpty()) {
                                item { AppleSectionHeader(title = "积分奖励 (${current.handbookTiers.size})") }
                                items(current.handbookTiers, key = { "hbt_${it.id}" }) { tier ->
                                    MugeTierRowCard(
                                        prefix = "积分达到",
                                        unit = "分",
                                        tier = tier,
                                        accentColor = apple.emerald,
                                    )
                                }
                            }
                            val groupedTasks = current.handbookTasks.groupBy { it.unlockDay }.toSortedMap()
                            groupedTasks.forEach { (day, tasks) ->
                                item(key = "day_hdr_$day") {
                                    val done = tasks.count { it.status == 3 }
                                    AppleSectionHeader(title = "第 $day 天任务 ($done/${tasks.size})")
                                }
                                items(tasks, key = { "task_${it.id}" }) { task ->
                                    val statusMeta = when (task.status) {
                                        3 -> "已领取" to apple.green
                                        2 -> "可领取" to apple.orange
                                        4 -> "第 ${task.unlockDay} 天解锁" to apple.secondaryText
                                        else -> "${task.progress}/${task.target}" to MaterialTheme.colorScheme.primary
                                    }
                                    ActivityTierCard(
                                        title = task.description,
                                        subtitle = "积分 +${task.points}",
                                        statusText = statusMeta.first,
                                        statusColor = statusMeta.second,
                                        highlighted = task.status == 2,
                                        rewards = task.rewards,
                                    )
                                }
                            }
                        }

                        personalityIdx -> {
                            val p = current.personality
                            val answers = p?.answers.orEmpty()
                            val questions = current.questions
                            val allAnswered = questions.isNotEmpty() && questions.all { answers.containsKey(it.id) }

                            if (p != null && p.personalityId > 0) {
                                item {
                                    AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Text(
                                                text = "专属牧场人格海报",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = apple.primaryText,
                                            )
                                            RemoteAssetImage(
                                                url = p.image.ifBlank { "/activity-assets/muge/personality-${p.personalityId}.png" },
                                                contentDescription = "牧场人格结果",
                                                fallbackIcon = Icons.Rounded.Pets,
                                                fallbackTint = apple.orange,
                                                modifier = Modifier
                                                    .size(180.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(apple.track),
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedQuestionIdx = 0
                                                        operate("saveAnswers", JSONObject().put("answers", JSONObject()).put("clear", true), silent = true)
                                                    },
                                                    enabled = !busy && current.active,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                    Text("重新测试")
                                                }
                                                Button(
                                                    onClick = { operate("claimShare") },
                                                    enabled = !busy && current.active && !p.shareRewardClaimed,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                    Text(if (p.shareRewardClaimed) "分享礼已领" else "领分享好礼")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (questions.isEmpty()) {
                                item {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.Quiz,
                                        title = "暂无测试题目",
                                        subtitle = "当前未配置牧场人格问卷",
                                    )
                                }
                            } else {
                                val safeQIdx = selectedQuestionIdx.coerceIn(0, questions.size - 1)
                                val question = questions[safeQIdx]
                                val chosenVal = answers[question.id]

                                item {
                                    AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = "第 ${safeQIdx + 1} / ${questions.size} 题",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = apple.orange,
                                                )
                                                Text(
                                                    text = "已答 ${answers.size}/${questions.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = apple.secondaryText,
                                                )
                                            }
                                            Text(
                                                text = question.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = apple.primaryText,
                                            )
                                            question.options.forEach { opt ->
                                                val isSelected = chosenVal == opt.value
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable(enabled = !busy && current.active) {
                                                            val ansJson = JSONObject().put(question.id.toString(), opt.value)
                                                            operate("saveAnswers", JSONObject().put("answers", ansJson), silent = true)
                                                            if (safeQIdx < questions.lastIndex) {
                                                                selectedQuestionIdx = safeQIdx + 1
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) apple.orange.copy(alpha = 0.14f) else apple.elevated,
                                                    border = BorderStroke(
                                                        if (isSelected) 1.dp else 0.5.dp,
                                                        if (isSelected) apple.orange else apple.separator,
                                                    ),
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    ) {
                                                        Text(
                                                            text = opt.label.ifBlank { "${opt.value}" },
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = if (isSelected) apple.orange else apple.secondaryText,
                                                        )
                                                        Text(
                                                            text = opt.text,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = apple.primaryText,
                                                            modifier = Modifier.weight(1f),
                                                        )
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = null,
                                                                tint = apple.orange,
                                                                modifier = Modifier.size(18.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                OutlinedButton(
                                                    onClick = { if (safeQIdx > 0) selectedQuestionIdx = safeQIdx - 1 },
                                                    enabled = !busy && safeQIdx > 0,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                    Text("上一题")
                                                }
                                                if (safeQIdx < questions.lastIndex) {
                                                    OutlinedButton(
                                                        onClick = { selectedQuestionIdx = safeQIdx + 1 },
                                                        enabled = !busy,
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Text("下一题")
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = { operate("settle") },
                                                        enabled = !busy && current.active && allAnswered,
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Text(if (allAnswered) "生成牧场人格" else "请完成全部题目")
                                                    }
                                                }
                                            }
                                            if (answers.isNotEmpty()) {
                                                TextButton(
                                                    onClick = {
                                                        selectedQuestionIdx = 0
                                                        operate("saveAnswers", JSONObject().put("answers", JSONObject()).put("clear", true), silent = true)
                                                    },
                                                    enabled = !busy && current.active,
                                                    modifier = Modifier.align(Alignment.End),
                                                ) {
                                                    Text("清空重填")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun MugeCounterCard(
    title: String,
    value: String,
    note: String,
    readyCount: Int,
    busy: Boolean,
    onClaimAll: () -> Unit,
) {
    val apple = LocalAppleColors.current
    AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelMedium, color = apple.secondaryText)
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = apple.orange,
                    )
                }
                Button(
                    onClick = onClaimAll,
                    enabled = !busy && readyCount > 0,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (readyCount > 0) "一键领取 ($readyCount)" else "暂无可领")
                }
            }
            Text(note, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
        }
    }
}

@Composable
private fun MugeTierRowCard(
    prefix: String,
    unit: String,
    tier: MugeTier,
    accentColor: Color,
) {
    val apple = LocalAppleColors.current
    val (statusText, color) = when (tier.status) {
        3 -> "已领取" to apple.green
        2 -> "可领取" to accentColor
        else -> "未达成" to apple.secondaryText
    }
    ActivityTierCard(
        title = "$prefix ${formatCompactCount(tier.threshold)} $unit",
        statusText = statusText,
        statusColor = color,
        highlighted = tier.status == 2,
        rewards = tier.rewards,
    )
}

// =====================================================================
// 2. MilkTea (奶茶热销中)
// =====================================================================

private enum class MilkTeaSheetKind {
    NONE,
    BOOST,
    WORKSHOP,
    UPGRADE,
    FUND,
    HIRE,
    SETTINGS,
    EXCHANGE_CONFIRM,
    INTERACT_CONFIRM,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MilkTeaDetailContent(
    accountId: String,
    apiClient: FarmApiClient,
    clockTickMs: Long,
    refreshTrigger: Int,
    onSubtitle: (String?) -> Unit,
    onRules: (List<String>) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var primaryTab by remember { mutableIntStateOf(0) } // 0=我的店铺 1=兑换商店 2=我的工作
    var state by remember { mutableStateOf<MilkTeaState?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var activeSheet by remember { mutableStateOf(MilkTeaSheetKind.NONE) }
    var pendingGoods by remember { mutableStateOf<MilkTeaGoods?>(null) }
    var pendingInteraction by remember { mutableStateOf<MilkTeaInteraction?>(null) }
    var pendingHireCandidate by remember { mutableStateOf<MilkTeaCandidate?>(null) }
    var hireDurationId by remember { mutableIntStateOf(1) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun currentTabPage(): String = when (primaryTab) {
        1 -> "exchange"
        2 -> "work"
        else -> when (activeSheet) {
            MilkTeaSheetKind.WORKSHOP -> "workshop"
            MilkTeaSheetKind.UPGRADE -> "upgrade"
            MilkTeaSheetKind.FUND -> "fund"
            MilkTeaSheetKind.HIRE -> "hireList"
            MilkTeaSheetKind.SETTINGS -> "settings"
            else -> "shop"
        }
    }

    fun load(page: String = currentTabPage(), extra: Map<String, String> = emptyMap()) {
        if (busy) return
        val params = if (page == "hireList" && extra.isEmpty()) {
            mapOf("durationId" to hireDurationId.toString())
        } else extra
        loadJob?.cancel()
        loading = true
        errorText = null
        loadJob = scope.launch {
            runCatching { apiClient.fetchMilkTea(accountId, page, params) }
                .onSuccess { res ->
                    state = res
                    if (res.rules.isNotEmpty()) onRules(res.rules)
                    if (res.hireDurations.isNotEmpty() && res.hireDurations.none { it.first == hireDurationId }) {
                        hireDurationId = res.hireDurations.first().first
                    }
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    state = null
                    errorText = err.message ?: "奶茶店铺数据读取失败"
                }
            loading = false
        }
    }

    fun operate(
        action: String,
        body: JSONObject = JSONObject(),
        closeSheetOnSuccess: Boolean = false,
        reloadAfter: String? = null,
    ) {
        if (busy || loading) return
        busy = true
        scope.launch {
            val result = runCatching { apiClient.operateMilkTea(accountId, action, body) }
            busy = false
            result
                .onSuccess { (nextState, msg) ->
                    if (nextState != null) {
                        state = nextState
                        if (nextState.rules.isNotEmpty()) onRules(nextState.rules)
                    }
                    onMessage(msg)
                    if (closeSheetOnSuccess) {
                        activeSheet = MilkTeaSheetKind.NONE
                        pendingGoods = null
                        pendingInteraction = null
                        pendingHireCandidate = null
                    }
                    if (reloadAfter != null) {
                        load(reloadAfter)
                    } else if (nextState == null) {
                        load()
                    }
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    pendingGoods = null
                    pendingInteraction = null
                    pendingHireCandidate = null
                    if (closeSheetOnSuccess) activeSheet = MilkTeaSheetKind.NONE
                    onMessage(err.message ?: "操作失败")
                    load()
                }
        }
    }

    LaunchedEffect(accountId, primaryTab, refreshTrigger) {
        load(currentTabPage())
    }

    LaunchedEffect(state, clockTickMs) {
        val s = state
        onSubtitle(
            if (s == null) null else {
                val nowSec = if (s.serverTimeSec > 0L) {
                    s.serverTimeSec + ((clockTickMs - s.receivedAtElapsedMs).coerceAtLeast(0L) / 1000L)
                } else System.currentTimeMillis() / 1000L
                val remSec = (s.endTimeSec - nowSec).coerceAtLeast(0L)
                when {
                    !s.active -> "已结束"
                    s.settling -> "结算期 · 剩余 ${formatDurationSec(remSec)}"
                    remSec > 0L -> "营业中 · 剩余 ${formatDurationSec(remSec)}"
                    else -> "营业中"
                }
            }
        )
    }

    val current = state
    val nowSec = current?.let {
        if (it.serverTimeSec > 0L) {
            it.serverTimeSec + ((clockTickMs - it.receivedAtElapsedMs).coerceAtLeast(0L) / 1000L)
        } else System.currentTimeMillis() / 1000L
    } ?: (System.currentTimeMillis() / 1000L)

    if (current == null && !loading && errorText != null) {
        AppleEmptyState(
            icon = Icons.Rounded.LocalCafe,
            title = "加载失败",
            subtitle = errorText.orEmpty(),
            actionLabel = "重试",
            onAction = { load() },
        )
        return
    }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { load() },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在同步奶茶店数据...", color = apple.secondaryText)
            }
            return@PullToRefreshBox
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Balance bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MilkTeaBalanceChip(
                    label = "奶茶券",
                    value = formatCompactCount(current.balances[1041] ?: 0L),
                    color = apple.orange,
                    modifier = Modifier.weight(1f),
                )
                MilkTeaBalanceChip(
                    label = "宣传喇叭",
                    value = formatCompactCount(current.balances[current.salesItemId] ?: 0L),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                MilkTeaBalanceChip(
                    label = "金币",
                    value = formatCompactCount(current.balances[1001] ?: 0L),
                    color = apple.emerald,
                    modifier = Modifier.weight(1f),
                )
            }

            if (!current.opened) {
                MilkTeaCreateShopView(
                    state = current,
                    busy = busy,
                    onCreate = { name -> operate("create", JSONObject().put("name", name)) },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    AppleSegmentedControl(
                        options = listOf("我的店铺", "兑换商店", "我的工作"),
                        selectedIndex = primaryTab,
                        onSelect = { if (!busy) primaryTab = it },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .swipeToSwitchTabs(primaryTab, 3, !busy) { if (!busy) primaryTab = it },
                ) {
                    when (primaryTab) {
                        0 -> MilkTeaShopTab(
                            state = current,
                            nowSec = nowSec,
                            busy = busy,
                            onClaimPending = { operate("claim") },
                            onOpenBoost = { activeSheet = MilkTeaSheetKind.BOOST },
                            onOpenWorkshop = {
                                activeSheet = MilkTeaSheetKind.WORKSHOP
                                load("workshop")
                            },
                            onOpenUpgrade = {
                                activeSheet = MilkTeaSheetKind.UPGRADE
                                load("upgrade")
                            },
                            onOpenFund = {
                                activeSheet = MilkTeaSheetKind.FUND
                                load("fund")
                            },
                            onOpenHire = {
                                activeSheet = MilkTeaSheetKind.HIRE
                                load("hireList", mapOf("durationId" to hireDurationId.toString()))
                            },
                            onOpenSettings = {
                                activeSheet = MilkTeaSheetKind.SETTINGS
                                load("settings")
                            },
                        )

                        1 -> MilkTeaExchangeTab(
                            state = current,
                            busy = busy,
                            onSelectGoods = {
                                pendingGoods = it
                                activeSheet = MilkTeaSheetKind.EXCHANGE_CONFIRM
                            },
                        )

                        2 -> MilkTeaWorkTab(
                            state = current,
                            nowSec = nowSec,
                            busy = busy,
                            onClaimWages = { operate("wages") },
                            onSelectInteract = {
                                pendingInteraction = it
                                activeSheet = MilkTeaSheetKind.INTERACT_CONFIRM
                            },
                        )
                    }
                }
            }
        }
    }

    if (current != null) {
        when (activeSheet) {
            MilkTeaSheetKind.BOOST -> MilkTeaBoostSheet(
                state = current,
                busy = busy,
                onDismiss = { if (!busy) activeSheet = MilkTeaSheetKind.NONE },
                onConfirm = { count ->
                    val expected = JSONArray().put(
                        JSONObject().put("id", current.salesItemId).put("count", 1)
                    )
                    operate(
                        action = "boost",
                        body = JSONObject().put("count", count).put("expectedCosts", expected),
                        closeSheetOnSuccess = true,
                    )
                },
            )

            MilkTeaSheetKind.WORKSHOP -> MilkTeaWorkshopSheet(
                state = current,
                busy = busy,
                onDismiss = {
                    if (!busy) {
                        activeSheet = MilkTeaSheetKind.NONE
                        load("shop")
                    }
                },
                onCraft = { selectedUids ->
                    val fruitsArr = JSONArray()
                    current.fruits.filter { it.uid in selectedUids && !it.locked && it.count > 0L }.forEach { f ->
                        fruitsArr.put(JSONObject().put("uid", f.uid).put("count", f.count))
                    }
                    operate("craft", JSONObject().put("fruits", fruitsArr))
                },
            )

            MilkTeaSheetKind.UPGRADE -> MilkTeaUpgradeSheet(
                state = current,
                busy = busy,
                onDismiss = {
                    if (!busy) {
                        activeSheet = MilkTeaSheetKind.NONE
                        load("shop")
                    }
                },
                onUpgrade = {
                    operate("upgrade", JSONObject().put("expectedLevel", current.level))
                },
            )

            MilkTeaSheetKind.FUND -> MilkTeaFundSheet(
                state = current,
                busy = busy,
                onDismiss = {
                    if (!busy) {
                        activeSheet = MilkTeaSheetKind.NONE
                        load("shop")
                    }
                },
                onUnlock = {
                    operate("unlock", JSONObject().put("expectedCosts", costsToJsonArray(current.fundCosts)))
                },
                onClaimFund = {
                    operate("fundClaim")
                },
            )

            MilkTeaSheetKind.HIRE -> MilkTeaHireSheet(
                state = current,
                nowSec = nowSec,
                durationId = hireDurationId,
                pendingCandidate = pendingHireCandidate,
                busy = busy,
                onSelectDuration = { nextDuration ->
                    pendingHireCandidate = null
                    hireDurationId = nextDuration
                    load("hireList", mapOf("durationId" to nextDuration.toString()))
                },
                onPickCandidate = { pendingHireCandidate = it },
                onConfirmHire = { cand ->
                    val expected = JSONArray().put(
                        JSONObject().put("id", 1041).put("count", current.hireWageTotal)
                    )
                    operate(
                        action = "hire",
                        body = JSONObject()
                            .put("targetGid", cand.gid)
                            .put("durationId", hireDurationId)
                            .put("expectedTalentId", cand.talentId)
                            .put("expectedIsRobot", cand.isRobot)
                            .put("expectedCosts", expected),
                    )
                    pendingHireCandidate = null
                },
                onDismiss = {
                    if (!busy) {
                        pendingHireCandidate = null
                        activeSheet = MilkTeaSheetKind.NONE
                        load("shop")
                    }
                },
            )

            MilkTeaSheetKind.SETTINGS -> MilkTeaSettingsSheet(
                state = current,
                nowSec = nowSec,
                busy = busy,
                onDismiss = {
                    if (!busy) {
                        activeSheet = MilkTeaSheetKind.NONE
                        load("shop")
                    }
                },
                onRename = { newName -> operate("rename", JSONObject().put("name", newName)) },
                onToggleHireDisabled = { disabled -> operate("hireSetting", JSONObject().put("disabled", disabled)) },
            )

            MilkTeaSheetKind.EXCHANGE_CONFIRM -> {
                val goods = pendingGoods
                if (goods != null) {
                    MilkTeaExchangeConfirmSheet(
                        goods = goods,
                        busy = busy,
                        onDismiss = {
                            if (!busy) {
                                pendingGoods = null
                                activeSheet = MilkTeaSheetKind.NONE
                            }
                        },
                        onConfirm = { qty ->
                            operate(
                                action = "exchange",
                                body = JSONObject()
                                    .put("goodsId", goods.id)
                                    .put("count", qty)
                                    .put("expectedCosts", costsToJsonArray(goods.costs)),
                                closeSheetOnSuccess = true,
                            )
                        },
                    )
                }
            }

            MilkTeaSheetKind.INTERACT_CONFIRM -> {
                val inter = pendingInteraction
                val shiftId = current.work?.shiftId.orEmpty()
                if (inter != null) {
                    AppleAlertDialog(
                        onDismissRequest = {
                            if (!busy) {
                                pendingInteraction = null
                                activeSheet = MilkTeaSheetKind.NONE
                            }
                        },
                        title = { Text("确认执行 · ${inter.name}") },
                        text = { Text(inter.description.ifBlank { "确认完成本次兼职工作互动？" }) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    operate(
                                        action = "interact",
                                        body = JSONObject()
                                            .put("interactionType", inter.id)
                                            .put("expectedShiftId", shiftId),
                                        closeSheetOnSuccess = true,
                                    )
                                },
                                enabled = !busy,
                            ) { Text("确认") }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    pendingInteraction = null
                                    activeSheet = MilkTeaSheetKind.NONE
                                },
                                enabled = !busy,
                            ) { Text("取消") }
                        },
                    )
                }
            }

            MilkTeaSheetKind.NONE -> Unit
        }
    }
}

@Composable
private fun MilkTeaBalanceChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = apple.card,
        border = BorderStroke(0.5.dp, apple.separator),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText, maxLines = 1)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = color,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MilkTeaCreateShopView(
    state: MilkTeaState,
    busy: Boolean,
    onCreate: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    var shopName by remember(state.recommendedNames) {
        mutableStateOf(state.recommendedNames.firstOrNull().orEmpty())
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "开设我的奶茶店",
                    style = MaterialTheme.typography.titleLarge,
                    color = apple.primaryText,
                )
                Text(
                    text = "店名需为 1-8 个字，开业后即可制作奶茶、雇佣好友并获取奶茶券。",
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                )
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { if (it.length <= 8) shopName = it },
                    label = { Text("店铺名称（1-8字）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.recommendedNames.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        state.recommendedNames.take(8).forEach { rec ->
                            AppleFilterChip(
                                selected = shopName == rec,
                                onClick = { shopName = rec.take(8) },
                                label = { Text(rec) },
                            )
                        }
                    }
                }
                val trimmed = shopName.trim()
                Button(
                    onClick = { onCreate(trimmed) },
                    enabled = !busy && state.active && !state.settling && trimmed.length in 1..8,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (busy) "开业中..." else "立即开业")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MilkTeaShopTab(
    state: MilkTeaState,
    nowSec: Long,
    busy: Boolean,
    onClaimPending: () -> Unit,
    onOpenBoost: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenUpgrade: () -> Unit,
    onOpenFund: () -> Unit,
    onOpenHire: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val canOperate = state.active && !state.settling
    val activeSlots = state.slots.filter { it.endAtSec > nowSec }
    val pendingHasReward = state.pendingRewards.any { it.count > 0L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.shopName.ifBlank { "我的奶茶店" },
                                style = MaterialTheme.typography.titleLarge,
                                color = apple.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "店铺 Lv.${state.level} · 时速 ${state.effectiveCupsPerHour} 杯/时 · 员工 ${activeSlots.size}/${state.employeeSlotCount}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                        if (state.canUpgrade && canOperate) {
                            AppleStatusPill(text = "可升级", color = apple.orange)
                        }
                    }

                    if (state.nextLevelRevenue > 0L) {
                        val frac = (state.totalRevenue.toFloat() / state.nextLevelRevenue.toFloat()).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("累计营收", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                Text(
                                    text = "${formatCompactCount(state.totalRevenue)} / ${formatCompactCount(state.nextLevelRevenue)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = apple.primaryText,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = apple.orange,
                                trackColor = apple.track,
                                strokeCap = StrokeCap.Round,
                            )
                        }
                    }

                    if (state.cashierCapacitySec > 0L) {
                        val capFrac = (state.cashierElapsedSec.toFloat() / state.cashierCapacitySec.toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("收银机时长", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                            Text(
                                text = "${formatDurationSec(state.cashierElapsedSec)} / ${formatDurationSec(state.cashierCapacitySec)} (${(capFrac * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                    }

                    HorizontalCashierPendingSection(
                        pendingRewards = state.pendingRewards,
                        pendingCups = state.pendingCups,
                        claimEnabled = !busy && state.active && pendingHasReward,
                        onClaim = onClaimPending,
                    )
                }
            }
        }

        item {
            AppleSectionHeader(title = "店铺经营")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MilkTeaActionTile(
                        title = "一键爆单",
                        subtitle = if (canOperate) "单次 +${state.boostCupsPerUse} 杯" else "结算中",
                        icon = Icons.Rounded.Bolt,
                        tint = apple.orange,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenBoost,
                    )
                    MilkTeaActionTile(
                        title = "奶茶工坊",
                        subtitle = "${state.recipes.count { it.unlocked }}/${state.recipes.size} 配方",
                        icon = Icons.Rounded.LocalCafe,
                        tint = apple.emerald,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenWorkshop,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MilkTeaActionTile(
                        title = "好友雇佣",
                        subtitle = "${activeSlots.size}/${state.employeeSlotCount} 上岗中",
                        icon = Icons.Rounded.Group,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHire,
                    )
                    MilkTeaActionTile(
                        title = "成长基金",
                        subtitle = if (state.fundUnlocked) "已解锁" else "待解锁",
                        icon = Icons.Rounded.CardGiftcard,
                        tint = apple.indigo,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenFund,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MilkTeaActionTile(
                        title = "店铺升级",
                        subtitle = if (state.canUpgrade) "可升下一级" else "Lv.${state.level}",
                        icon = Icons.Rounded.Upgrade,
                        tint = if (state.canUpgrade) apple.orange else apple.secondaryText,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenUpgrade,
                    )
                    MilkTeaActionTile(
                        title = "店铺设置",
                        subtitle = if (state.hireDisabled) "已拒被雇" else "允许被雇",
                        icon = Icons.Rounded.Settings,
                        tint = apple.secondaryText,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSettings,
                    )
                }
            }
        }

        if (activeSlots.isNotEmpty()) {
            item { AppleSectionHeader(title = "在岗员工 (${activeSlots.size}/${state.employeeSlotCount})") }
            item {
                AppleGroupedCard {
                    activeSlots.forEachIndexed { idx, slot ->
                        if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                        val remSec = (slot.endAtSec - nowSec).coerceAtLeast(0L)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = slot.employeeName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = apple.primaryText,
                                )
                                if (slot.talentName.isNotBlank()) {
                                    Text(
                                        text = slot.talentName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = apple.orange,
                                    )
                                }
                            }
                            Text(
                                text = "剩 ${formatDurationSec(remSec)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HorizontalCashierPendingSection(
    pendingRewards: List<CommerceItem>,
    pendingCups: List<Pair<String, Int>>,
    claimEnabled: Boolean,
    onClaim: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val validRewards = pendingRewards.filter { it.count > 0L }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = apple.elevated,
        border = BorderStroke(0.5.dp, apple.separator),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("收银机待领收益", style = MaterialTheme.typography.labelLarge, color = apple.primaryText)
                Button(
                    onClick = onClaim,
                    enabled = claimEnabled,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(if (claimEnabled) "收取" else "暂无")
                }
            }
            if (validRewards.isNotEmpty()) {
                ActivityRewardFlowRow(rewards = validRewards, accentColor = apple.orange)
            } else {
                Text("暂未累积可领取收益", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            }
            if (pendingCups.isNotEmpty()) {
                Text(
                    text = "待售队列：" + pendingCups.joinToString(" · ") { "${it.first} ×${it.second}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = apple.secondaryText,
                )
            }
        }
    }
}

@Composable
private fun MilkTeaActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = apple.card,
        border = BorderStroke(0.5.dp, apple.separator),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = apple.primaryText, maxLines = 1)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = apple.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MilkTeaExchangeTab(
    state: MilkTeaState,
    busy: Boolean,
    onSelectGoods: (MilkTeaGoods) -> Unit,
) {
    val apple = LocalAppleColors.current
    if (state.goods.isEmpty()) {
        AppleEmptyState(
            icon = Icons.Rounded.Storefront,
            title = "暂无可兑换商品",
            subtitle = "请下拉刷新获取最新奶茶商店列表",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.goods, key = { it.id }) { goods ->
            val enabled = !busy && state.canExchange && goods.available
            val costText = goods.costs.joinToString(" + ") { "${it.name} ×${formatCompactCount(it.count)}" }
            val limitText = goods.remaining?.let { rem ->
                val period = when (goods.limitPeriod) {
                    "daily" -> "今日"
                    "weekly" -> "本周"
                    "monthly" -> "本月"
                    else -> "限购"
                }
                "$period 剩 $rem/${goods.purchaseLimit}"
            }
            ActivityTierCard(
                title = goods.name,
                subtitle = buildString {
                    append("消耗：")
                    append(costText.ifBlank { "免费" })
                    if (!limitText.isNullOrBlank()) {
                        append(" · ")
                        append(limitText)
                    }
                    if (goods.unmetConditionText.isNotBlank()) {
                        append("\n")
                        append(goods.unmetConditionText)
                    }
                },
                statusText = when {
                    goods.remaining == 0 -> "已售罄"
                    goods.available -> "可兑换"
                    else -> "未满足"
                },
                statusColor = if (goods.available) apple.orange else apple.secondaryText,
                highlighted = goods.available,
                rewards = goods.rewards,
                actionLabel = if (enabled) "兑换" else null,
                actionEnabled = enabled,
                onAction = { onSelectGoods(goods) },
            )
        }
    }
}

@Composable
private fun MilkTeaWorkTab(
    state: MilkTeaState,
    nowSec: Long,
    busy: Boolean,
    onClaimWages: () -> Unit,
    onSelectInteract: (MilkTeaInteraction) -> Unit,
) {
    val apple = LocalAppleColors.current
    val work = state.work
    if (work == null) {
        AppleEmptyState(
            icon = Icons.Rounded.Work,
            title = "暂无兼职信息",
            subtitle = "请下拉刷新读取兼职状态",
        )
        return
    }

    val pendingWages = work.slips.any { it.wageStatus == 1 }
    val shiftRemSec = (work.endAtSec - nowSec).coerceAtLeast(0L)
    val canInteract = work.hasShift && state.active && !state.settling && shiftRemSec > 0L &&
        !work.interactDone && (work.interactLimit - work.interactCount) > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "我的打工天赋：${work.talentName.ifBlank { "普通打工人" }}",
                        style = MaterialTheme.typography.titleSmall,
                        color = apple.primaryText,
                    )
                    if (work.talentDesc.isNotBlank()) {
                        Text(work.talentDesc, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                    }
                }
            }
        }

        if (work.hasShift) {
            item {
                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("正在打工：${work.shopName}", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                Text(
                                    text = "店长：${work.employerName} · 基础工资 ${work.baseWage ?: 0} 奶茶券",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.secondaryText,
                                )
                            }
                            AppleStatusPill(
                                text = if (shiftRemSec > 0L) "剩 ${formatDurationSec(shiftRemSec)}" else "已下班",
                                color = if (shiftRemSec > 0L) apple.green else apple.secondaryText,
                            )
                        }
                        if (work.interactions.isNotEmpty()) {
                            val remainChances = if (work.interactDone) 0 else (work.interactLimit - work.interactCount).coerceAtLeast(0)
                            Text(
                                text = "工作事项（本班剩余 $remainChances/${work.interactLimit} 次）",
                                style = MaterialTheme.typography.labelMedium,
                                color = apple.secondaryText,
                            )
                            work.interactions.forEach { inter ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !busy && canInteract && inter.ready) {
                                            onSelectInteract(inter)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = apple.elevated,
                                    border = BorderStroke(0.5.dp, apple.separator),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(inter.name, style = MaterialTheme.typography.labelLarge, color = apple.primaryText)
                                            if (inter.description.isNotBlank()) {
                                                Text(inter.description, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                            }
                                        }
                                        AppleStatusPill(
                                            text = if (canInteract && inter.ready) "执行" else "不可用",
                                            color = if (canInteract && inter.ready) apple.orange else apple.secondaryText,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            AppleSectionHeader(
                title = "兼职记录 (${work.slips.size})",
                actionLabel = if (pendingWages) "一键领工资" else null,
                onAction = if (pendingWages && !busy) onClaimWages else null,
            )
        }

        if (work.slips.isEmpty()) {
            item {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Text("暂无兼职工资单", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                }
            }
        } else {
            items(work.slips, key = { it.shiftId }) { slip ->
                val (statusLabel, statusCol) = when (slip.wageStatus) {
                    1 -> "待领取" to apple.orange
                    2 -> "已领取" to apple.green
                    else -> "进行中" to apple.secondaryText
                }
                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(slip.shopName.ifBlank { "好友奶茶店" }, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                            Text(
                                text = "店长：${slip.employerName} · 结算：${formatTimestamp(slip.endAtSec * 1000L)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "+${slip.finalWage} 奶茶券",
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                color = apple.orange,
                            )
                            AppleStatusPill(text = statusLabel, color = statusCol)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkTeaBoostSheet(
    state: MilkTeaState,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stock = (state.balances[state.salesItemId] ?: 0L).coerceIn(0L, 9999L).toInt()
    var count by remember(stock) { mutableIntStateOf(if (stock > 0) 1 else 0) }

    val sortedRecipes = remember(state.recipes) {
        state.recipes.filter { it.unlocked }
            .sortedWith(compareByDescending<com.qqfarm.mobile.data.model.MilkTeaRecipe> { it.isAdvanced }.thenByDescending { it.tokenPrice })
    }
    var remCups = state.boostCupsPerUse * count
    var estToken = 0L
    var estCoin = 0L
    for (r in sortedRecipes) {
        if (remCups <= 0) break
        val cap = if (r.isAdvanced) r.advancedStockCups.toInt() else remCups
        val take = minOf(remCups, cap.coerceAtLeast(0))
        estToken += r.boostTokenPrice * take
        estCoin += (state.boostCoinPrices[r.recipeId] ?: r.boostCoinPrice) * take
        remCups -= take
    }
    val soldCups = state.boostCupsPerUse * count - remCups

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = apple.card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("一键爆单", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            Text(
                text = "当前拥有宣传喇叭：$stock 个（每次爆单卖出 ${state.boostCupsPerUse} 杯）",
                style = MaterialTheme.typography.bodySmall,
                color = apple.secondaryText,
            )
            if (stock > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("使用数量", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                    AppleQuantityStepper(value = count, min = 1, max = stock, onChange = { count = it }, enabled = !busy)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = apple.elevated,
                    border = BorderStroke(0.5.dp, apple.separator),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "预计售出 $soldCups 杯 · 约得 $estToken 奶茶券 + $estCoin 金币",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = apple.orange,
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(count) },
                    enabled = !busy && stock > 0 && count > 0 && state.active && !state.settling,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (busy) "提交中..." else "确认爆单")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkTeaWorkshopSheet(
    state: MilkTeaState,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCraft: (Set<String>) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val craftable = remember(state.fruits) { state.fruits.filter { !it.locked && it.count > 0L } }
    var selectedUids by remember(state.fruits) { mutableStateOf(emptySet<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = apple.card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("奶茶工坊", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)

            Text("配方列表 (${state.recipes.size})", style = MaterialTheme.typography.labelLarge, color = apple.secondaryText)
            state.recipes.forEach { r ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = apple.elevated,
                    border = BorderStroke(0.5.dp, apple.separator),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RemoteAssetImage(
                            url = r.image,
                            contentDescription = r.name,
                            fallbackIcon = Icons.Rounded.LocalCafe,
                            fallbackTint = if (r.unlocked) apple.orange else apple.secondaryText,
                            modifier = Modifier.size(34.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                            Text(
                                text = "券 +${r.tokenPrice} · 金币 +${r.coinPrice}" + if (r.isAdvanced) " · 库存 ${r.advancedStockCups} 杯" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                        AppleStatusPill(
                            text = if (r.unlocked) "已解锁" else "未解锁",
                            color = if (r.unlocked) apple.green else apple.secondaryText,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("可用材料 (${craftable.size})", style = MaterialTheme.typography.labelLarge, color = apple.secondaryText)
                if (craftable.isNotEmpty()) {
                    TextButton(onClick = {
                        selectedUids = if (selectedUids.size == craftable.size) emptySet() else craftable.map { it.uid }.toSet()
                    }) {
                        Text(if (selectedUids.size == craftable.size) "取消全选" else "全选材料")
                    }
                }
            }

            if (craftable.isEmpty()) {
                Text("背包暂无可用于制作高阶奶茶的未锁定果实", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            } else {
                craftable.forEach { f ->
                    val checked = f.uid in selectedUids
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !busy) {
                                selectedUids = if (checked) selectedUids - f.uid else selectedUids + f.uid
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (checked) apple.orange.copy(alpha = 0.12f) else apple.elevated,
                        border = BorderStroke(0.5.dp, apple.separator),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedUids = if (it) selectedUids + f.uid else selectedUids - f.uid
                                },
                            )
                            Text("${f.name} ×${f.count}", style = MaterialTheme.typography.bodyMedium, color = apple.primaryText, modifier = Modifier.weight(1f))
                            if (f.mutations.isNotEmpty()) {
                                Text(f.mutations.joinToString("+"), style = MaterialTheme.typography.labelSmall, color = apple.orange)
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Text("关闭")
                }
                Button(
                    onClick = { onCraft(selectedUids) },
                    enabled = !busy && state.canCraft && state.active && selectedUids.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (busy) "制作中..." else "确认制作 (${selectedUids.size})")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkTeaUpgradeSheet(
    state: MilkTeaState,
    busy: Boolean,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = apple.card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("店铺升级 · 当前 Lv.${state.level}", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            Button(
                onClick = onUpgrade,
                enabled = !busy && state.active && state.canUpgrade,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.canUpgrade) "升级至 Lv.${state.level + 1}" else "暂未达到升级营收条件")
            }
            state.levels.forEach { lv ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (lv.level <= state.level) apple.green.copy(alpha = 0.08f) else apple.elevated,
                    border = BorderStroke(0.5.dp, apple.separator),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lv.${lv.level}", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                            Text(
                                text = "需求营收 ${formatCompactCount(lv.revenueRequired)} · ${lv.employeeSlots} 员工位",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                        if (lv.recipes.isNotEmpty()) {
                            Text("解锁配方：${lv.recipes.joinToString("、")}", style = MaterialTheme.typography.labelSmall, color = apple.orange)
                        }
                        if (lv.rewards.isNotEmpty()) {
                            ActivityRewardFlowRow(rewards = lv.rewards)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkTeaFundSheet(
    state: MilkTeaState,
    busy: Boolean,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    onClaimFund: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val readyCount = state.fundTiers.count { it.state == 2 }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = apple.card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("成长基金", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            if (!state.fundUnlocked) {
                val costStr = state.fundCosts.joinToString(" + ") { "${it.name} ×${formatCompactCount(it.count)}" }
                Button(
                    onClick = onUnlock,
                    enabled = !busy && state.active && state.fundCosts.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("消耗 ${costStr.ifBlank { "基金券" }} 解锁成长基金")
                }
            } else {
                Button(
                    onClick = onClaimFund,
                    enabled = !busy && state.active && readyCount > 0,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (readyCount > 0) "一键领取 $readyCount 档基金奖励" else "暂无可领基金奖励")
                }
            }
            state.fundTiers.forEach { tier ->
                val (label, col) = when {
                    tier.state == 3 -> "已领取" to apple.green
                    !state.fundUnlocked -> "未解锁" to apple.secondaryText
                    tier.state == 2 -> "可领取" to apple.orange
                    else -> "未达成" to apple.secondaryText
                }
                ActivityTierCard(
                    title = "店铺达到 Lv.${tier.targetLevel}",
                    statusText = label,
                    statusColor = col,
                    highlighted = state.fundUnlocked && tier.state == 2,
                    rewards = tier.rewards,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MilkTeaHireSheet(
    state: MilkTeaState,
    nowSec: Long,
    durationId: Int,
    pendingCandidate: MilkTeaCandidate?,
    busy: Boolean,
    onSelectDuration: (Int) -> Unit,
    onPickCandidate: (MilkTeaCandidate) -> Unit,
    onConfirmHire: (MilkTeaCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val usedSlots = state.slots.count { it.endAtSec > nowSec }
    val hasFreeSlot = usedSlots < state.employeeSlotCount

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = apple.card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("好友雇佣 ($usedSlots/${state.employeeSlotCount})", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            if (state.hireDurations.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.hireDurations.forEach { (id, hours) ->
                        AppleFilterChip(
                            selected = durationId == id,
                            onClick = { onSelectDuration(id) },
                            label = { Text("${hours}小时") },
                        )
                    }
                }
            }
            Text(
                text = "本次雇佣预估工资：${formatCompactCount(state.hireWageTotal)} 奶茶券",
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = apple.orange,
            )

            if (pendingCandidate != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = apple.orange.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, apple.orange),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "确认花费 ${formatCompactCount(state.hireWageTotal)} 奶茶券雇佣「${pendingCandidate.name}」？",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Button(
                            onClick = { onConfirmHire(pendingCandidate) },
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (busy) "雇佣中..." else "确认雇佣")
                        }
                    }
                }
            }

            state.candidates.forEach { cand ->
                val available = hasFreeSlot && state.active && !state.settling &&
                    cand.state == 0 && cand.rehireAtSec <= nowSec
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = apple.elevated,
                    border = BorderStroke(0.5.dp, apple.separator),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cand.name + if (cand.isRobot) " · 机器人" else "",
                                style = MaterialTheme.typography.titleSmall,
                                color = apple.primaryText,
                            )
                            Text(cand.talentName.ifBlank { "普通员工" }, style = MaterialTheme.typography.labelSmall, color = apple.orange)
                            if (cand.stateUntilSec > nowSec) {
                                Text(
                                    text = "忙线中 · 剩 ${formatDurationSec(cand.stateUntilSec - nowSec)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                )
                            } else if (cand.rehireAtSec > nowSec) {
                                Text(
                                    text = "冷却中 · 剩 ${formatDurationSec(cand.rehireAtSec - nowSec)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                )
                            }
                        }
                        Button(
                            onClick = { onPickCandidate(cand) },
                            enabled = !busy && available,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(if (available) "雇佣" else "不可用")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkTeaSettingsSheet(
    state: MilkTeaState,
    nowSec: Long,
    busy: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onToggleHireDisabled: (Boolean) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nameInput by remember(state.shopName) { mutableStateOf(state.shopName) }
    val renameCdLeft = (state.renameCdUntilSec - nowSec).coerceAtLeast(0L)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = apple.card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("店铺设置", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            OutlinedTextField(
                value = nameInput,
                onValueChange = { if (it.length <= 8) nameInput = it },
                label = { Text("修改店名（1-8字）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (renameCdLeft > 0L) {
                Text("改名冷却中：还剩 ${formatDurationSec(renameCdLeft)}", style = MaterialTheme.typography.labelSmall, color = apple.orange)
            }
            val trimmed = nameInput.trim()
            Button(
                onClick = { onRename(trimmed) },
                enabled = !busy && state.active && !state.settling && renameCdLeft == 0L &&
                    trimmed.length in 1..8 && trimmed != state.shopName,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存新店名")
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = apple.elevated,
                border = BorderStroke(0.5.dp, apple.separator),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("拒绝被好友雇佣", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                        Text("开启后不会出现在好友的雇佣名单中", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                    }
                    AppleSwitch(
                        checked = state.hireDisabled,
                        onCheckedChange = { onToggleHireDisabled(it) },
                        enabled = !busy && state.active,
                        checkedTrackColor = apple.orange,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkTeaExchangeConfirmSheet(
    goods: MilkTeaGoods,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxQty = (goods.remaining ?: 99).coerceIn(0, 999)
    var requestedCount by remember(goods.id) { mutableIntStateOf(1) }
    val qty = requestedCount.coerceAtMost(maxQty)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = apple.card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("确认兑换 · ${goods.name}", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            ActivityRewardFlowRow(rewards = goods.rewards, accentColor = apple.emerald)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("兑换份数", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                AppleQuantityStepper(value = qty, max = maxQty, onChange = { requestedCount = it }, enabled = !busy)
            }
            val totalCosts = goods.costs.joinToString(" + ") { "${it.name} ×${formatCompactCount(it.count * qty)}" }
            Text("预计消耗：${totalCosts.ifBlank { "免费" }}", style = MaterialTheme.typography.bodyMedium, color = apple.orange)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(onClick = { onConfirm(qty) }, enabled = !busy && qty > 0, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Text(if (busy) "兑换中..." else "确认兑换")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// =====================================================================
// 3. ShareReward (快乐不独享)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareRewardDetailContent(
    accountId: String,
    activityId: String,
    apiClient: FarmApiClient,
    clockTickMs: Long,
    refreshTrigger: Int,
    onSubtitle: (String?) -> Unit,
    onRules: (List<String>) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<ShareRewardState?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            errorText = null
            runCatching { apiClient.fetchShareReward(accountId, activityId) }
                .onSuccess { res ->
                    state = res
                    onRules(res.rules)
                }
                .onFailure { err -> errorText = err.message ?: "加载失败" }
            loading = false
        }
    }

    fun operate(action: String) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { apiClient.operateShareReward(accountId, activityId, action) }
                .onSuccess { (nextState, msg) ->
                    if (nextState != null) state = nextState else load()
                    onMessage(msg)
                }
                .onFailure { err -> onMessage(err.message ?: "操作失败") }
            busy = false
        }
    }

    LaunchedEffect(accountId, activityId, refreshTrigger) { load() }
    LaunchedEffect(state, clockTickMs) {
        val s = state
        onSubtitle(
            if (s == null) null else formatRemainFromMs(s.endTimeMs, s.serverTimeMs, s.receivedAtElapsedMs, clockTickMs)
        )
    }

    val current = state
    if (current == null && !loading && errorText != null) {
        AppleEmptyState(icon = Icons.Rounded.Celebration, title = "加载失败", subtitle = errorText.orEmpty(), actionLabel = "重试", onAction = ::load)
        return
    }

    PullToRefreshBox(isRefreshing = loading && current != null, onRefresh = ::load, modifier = Modifier.fillMaxSize()) {
        if (current == null) {
            AppleLoadingState()
            return@PullToRefreshBox
        }

        val readyCount = current.milestones.count { it.state == 2 }
        val nextTier = current.milestones.firstOrNull { it.threshold > current.score }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("当前快乐值", style = MaterialTheme.typography.labelMedium, color = apple.secondaryText)
                                Text(
                                    text = formatCompactCount(current.score),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = apple.orange,
                                )
                            }
                            Button(
                                onClick = { operate("milestones") },
                                enabled = !busy && current.active && current.canClaimMilestones && readyCount > 0,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(if (readyCount > 0) "领取档位 ($readyCount)" else "暂无可领档位")
                            }
                        }
                        if (nextTier != null) {
                            val frac = (current.score.toFloat() / nextTier.threshold.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                                color = apple.orange,
                                trackColor = apple.track,
                                strokeCap = StrokeCap.Round,
                            )
                            Text(
                                text = "再攒 ${formatCompactCount(nextTier.threshold - current.score)} 解锁 ${formatCompactCount(nextTier.threshold)} 档",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp), modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("首次分享", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                            Text(
                                text = "+${current.firstShareReward} 快乐值\n礼包 ${current.poolClaimedCount}/${current.poolClaimLimit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                            Button(
                                onClick = { operate("share") },
                                enabled = !busy && current.active && current.canShare,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (current.firstShareAwarded) "已分享" else "立即分享")
                            }
                        }
                    }
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp), modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("每日打卡", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                            Text(
                                text = "+${current.dailyReward} 快乐值\n已领 ${current.claimedCount}/${current.claimLimit} 次",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                            Button(
                                onClick = { operate("daily") },
                                enabled = !busy && current.active && current.canClaimDaily,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (current.canClaimDaily) "领取每日" else "今日已领")
                            }
                        }
                    }
                }
            }

            item { AppleSectionHeader(title = "里程碑奖励 (${current.milestones.size})") }
            items(current.milestones, key = { it.id }) { tier ->
                val (statusLabel, statusCol) = when (tier.state) {
                    3 -> "已领取" to apple.green
                    2 -> "可领取" to apple.orange
                    else -> "未达成" to apple.secondaryText
                }
                ActivityTierCard(
                    title = "快乐值达到 ${formatCompactCount(tier.threshold)}",
                    statusText = statusLabel,
                    statusColor = statusCol,
                    highlighted = tier.state == 2,
                    rewards = tier.rewards,
                )
            }
        }
    }
}

// =====================================================================
// 4. AutumnWish (秋祈良愿)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutumnWishDetailContent(
    accountId: String,
    apiClient: FarmApiClient,
    clockTickMs: Long,
    refreshTrigger: Int,
    onSubtitle: (String?) -> Unit,
    onRules: (List<String>) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<AutumnWishState?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            errorText = null
            runCatching { apiClient.fetchAutumnWish(accountId) }
                .onSuccess { res ->
                    state = res
                    onRules(res.rules)
                }
                .onFailure { err -> errorText = err.message ?: "加载秋祈良愿失败" }
            loading = false
        }
    }

    fun operate(action: String, body: JSONObject = JSONObject()) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { apiClient.operateAutumnWish(accountId, action, body) }
                .onSuccess { (nextState, msg) ->
                    if (nextState != null) state = nextState else load()
                    onMessage(msg)
                }
                .onFailure { err -> onMessage(err.message ?: "操作失败") }
            busy = false
        }
    }

    LaunchedEffect(accountId, refreshTrigger) { load() }
    LaunchedEffect(state, clockTickMs) {
        val s = state
        onSubtitle(if (s == null) null else formatRemainFromMs(s.endTimeMs, s.serverTimeMs, s.receivedAtElapsedMs, clockTickMs))
    }

    val current = state
    if (current == null && !loading && errorText != null) {
        AppleEmptyState(icon = Icons.Rounded.AutoAwesome, title = "加载失败", subtitle = errorText.orEmpty(), actionLabel = "重试", onAction = ::load)
        return
    }

    PullToRefreshBox(isRefreshing = loading && current != null, onRefresh = ::load, modifier = Modifier.fillMaxSize()) {
        if (current == null) {
            AppleLoadingState()
            return@PullToRefreshBox
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("累计祈愿第 ${current.day} 天", style = MaterialTheme.typography.titleMedium, color = apple.primaryText)
                            Text("今日剩余祈愿次数：${current.remaining}", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                        }
                        AppleStatusPill(
                            text = if (current.canClaim) "待领奖" else if (current.canDraw) "可求签" else "今日已完成",
                            color = if (current.canClaim || current.canDraw) apple.orange else apple.green,
                        )
                    }
                }
            }

            if (current.canClaim || current.pendingText.isNotBlank() || current.pendingRewards.isNotEmpty()) {
                item {
                    ActivityTierCard(
                        title = "今日签文揭晓",
                        subtitle = current.pendingText.ifBlank { "好运签已生成" },
                        statusText = if (current.canClaim) "待领取" else "已揭晓",
                        statusColor = apple.orange,
                        highlighted = current.canClaim,
                        rewards = current.pendingRewards,
                        actionLabel = if (current.canClaim) "领取祈愿奖励" else null,
                        actionEnabled = !busy && current.active && current.canClaim,
                        onAction = { operate("claim") },
                    )
                }
            }

            if (current.choices.isNotEmpty()) {
                item { AppleSectionHeader(title = "选择心仪良愿进行抽签") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        current.choices.forEach { (chooseId, label) ->
                            val enabled = !busy && current.active && current.canDraw && current.remaining > 0 && !current.canClaim
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = enabled) {
                                        operate("draw", JSONObject().put("chooseId", chooseId))
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (current.pendingChooseId == chooseId) apple.orange.copy(alpha = 0.12f) else apple.card,
                                border = BorderStroke(
                                    0.5.dp,
                                    if (current.pendingChooseId == chooseId) apple.orange else apple.separator,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(label, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                    AppleStatusPill(
                                        text = if (enabled) "点击求签" else "已锁定",
                                        color = if (enabled) apple.orange else apple.secondaryText,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (current.rewardDays.isNotEmpty()) {
                item { AppleSectionHeader(title = "累计祈愿好礼") }
                items(current.rewardDays, key = { it.first }) { (d, reward) ->
                    val unlocked = current.day >= d
                    ActivityTierCard(
                        title = "累计第 $d 天",
                        statusText = if (unlocked) "已达成" else "第 $d 天",
                        statusColor = if (unlocked) apple.green else apple.secondaryText,
                        highlighted = false,
                        rewards = listOf(reward),
                    )
                }
            }
        }
    }
}

// =====================================================================
// 5. Stellar (千星游记)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StellarDetailContent(
    accountId: String,
    apiClient: FarmApiClient,
    clockTickMs: Long,
    refreshTrigger: Int,
    onSubtitle: (String?) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var subTab by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<StellarSnapshot?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var pendingGoods by remember { mutableStateOf<StellarShopGoods?>(null) }

    fun load() {
        scope.launch {
            loading = true
            errorText = null
            runCatching { apiClient.fetchStellarActivity(accountId) }
                .onSuccess { state = it }
                .onFailure { err -> errorText = err.message ?: "读取千星游记失败" }
            loading = false
        }
    }

    fun runAction(block: suspend () -> String) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { block() }
                .onSuccess { msg ->
                    onMessage(msg)
                    load()
                }
                .onFailure { err -> onMessage(err.message ?: "操作失败") }
            busy = false
        }
    }

    LaunchedEffect(accountId, refreshTrigger) { load() }
    LaunchedEffect(state, clockTickMs) {
        val s = state
        onSubtitle(if (s == null) null else formatRemainFromMs(s.endTimeMs, s.serverTimeMs, s.receivedAtElapsedMs, clockTickMs))
    }

    val current = state
    if (current == null && !loading && errorText != null) {
        AppleEmptyState(icon = Icons.Rounded.Stars, title = "加载失败", subtitle = errorText.orEmpty(), actionLabel = "重试", onAction = ::load)
        return
    }

    PullToRefreshBox(isRefreshing = loading, onRefresh = ::load, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppleScrollablePills(
                options = listOf("游记通行证", "星宿点亮", "星砂商店", "赛季节令"),
                selectedIndex = subTab,
                onSelect = { subTab = it },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .swipeToSwitchTabs(subTab, 4) { idx -> subTab = idx },
            ) {
                if (current == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("正在同步星宿与游记...", color = apple.secondaryText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    when (subTab) {
                        0 -> {
                            item {
                                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            Text("游记 Lv.${current.passLevel}", style = MaterialTheme.typography.titleMedium, color = apple.primaryText)
                                            Text("进度 ${current.passProgress}/${current.passProgressMax}", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                                        }
                                        Button(
                                            onClick = { runAction { apiClient.claimStellarPass(accountId) } },
                                            enabled = !busy && current.canClaimPass,
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Text(if (current.canClaimPass) "一键领奖" else "暂无可领")
                                        }
                                    }
                                }
                            }
                            items(current.passNodes, key = { it.id }) { node ->
                                val (statusLabel, col) = when {
                                    node.claimed -> "已领取" to apple.green
                                    node.claimable -> "可领取" to apple.orange
                                    node.locked -> "未解锁" to apple.secondaryText
                                    else -> "进行中" to MaterialTheme.colorScheme.primary
                                }
                                ActivityTierCard(
                                    title = "Lv.${node.level}",
                                    statusText = statusLabel,
                                    statusColor = col,
                                    highlighted = node.claimable,
                                    rewards = node.rewards,
                                )
                            }
                        }

                        1 -> {
                            item {
                                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("观星星宿进度", style = MaterialTheme.typography.titleMedium, color = apple.primaryText)
                                        Button(
                                            onClick = { runAction { apiClient.lightStellarConstellation(accountId) } },
                                            enabled = !busy && current.canLightConstellation,
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Text(if (current.canLightConstellation) "点亮星宿" else "暂不可点亮")
                                        }
                                    }
                                }
                            }
                            items(current.constellationGroups, key = { it.id }) { g ->
                                val col = when (g.visualState) {
                                    "lit" -> apple.green
                                    "lightable" -> apple.orange
                                    else -> apple.secondaryText
                                }
                                val statusText = when (g.visualState) {
                                    "lit" -> "已点亮"
                                    "lightable" -> "可点亮"
                                    else -> "未点亮"
                                }
                                ActivityTierCard(
                                    title = g.name,
                                    subtitle = g.explain,
                                    statusText = statusText,
                                    statusColor = col,
                                    highlighted = g.visualState == "lightable",
                                    rewards = g.rewards,
                                )
                            }
                        }

                        2 -> {
                            item {
                                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("当前${current.shopCurrencyName}余额", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
                                        Text(
                                            text = if (current.shopBalanceKnown) current.shopBalance else "--",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            items(current.shopGoods, key = { it.id }) { good ->
                                val enabled = !busy && good.exchangeable && !good.soldOut
                                ActivityTierCard(
                                    title = good.name.ifBlank { good.item.name },
                                    subtitle = "消耗 ${good.cost.name} ×${formatCompactCount(good.cost.count)}",
                                    statusText = if (good.soldOut) "已售罄" else if (good.exchangeable) "可兑换" else "暂不可换",
                                    statusColor = if (enabled) MaterialTheme.colorScheme.primary else apple.secondaryText,
                                    highlighted = enabled,
                                    rewards = listOf(good.item),
                                    actionLabel = if (enabled) "兑换" else null,
                                    actionEnabled = enabled,
                                    onAction = { pendingGoods = good },
                                )
                            }
                        }

                        3 -> {
                            items(current.solarTerms, key = { it.id }) { term ->
                                val (statusLabel, col) = when {
                                    term.claimed -> "已领取" to apple.green
                                    term.claimable -> "可领取" to apple.orange
                                    term.locked -> "未解锁" to apple.secondaryText
                                    else -> "未达成" to apple.secondaryText
                                }
                                ActivityTierCard(
                                    title = term.name,
                                    subtitle = term.description,
                                    statusText = statusLabel,
                                    statusColor = col,
                                    highlighted = term.claimable,
                                    rewards = term.rewards,
                                    actionLabel = if (term.claimable) "领取节令礼" else null,
                                    actionEnabled = !busy && term.claimable,
                                    onAction = { runAction { apiClient.claimStellarSolarTerm(accountId, term.id) } },
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    val chosenGood = pendingGoods
    if (chosenGood != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val maxQty = chosenGood.maxExchangeCount.coerceIn(0, 99)
        var requestedCount by remember(chosenGood.id) { mutableIntStateOf(1) }
        val qty = requestedCount.coerceAtMost(maxQty)
        ModalBottomSheet(onDismissRequest = { if (!busy) pendingGoods = null }, sheetState = sheetState, containerColor = apple.card) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("兑换 · ${chosenGood.name}", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("兑换数量", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                    AppleQuantityStepper(value = qty, max = maxQty, onChange = { requestedCount = it }, enabled = !busy)
                }
                Text(
                    text = "预计消耗 ${chosenGood.cost.name} ×${formatCompactCount(chosenGood.cost.count * qty)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = apple.orange,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { pendingGoods = null }, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            runAction { apiClient.exchangeStarSand(accountId, chosenGood.id, qty) }
                            pendingGoods = null
                        },
                        enabled = !busy && qty > 0,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("确认兑换")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// =====================================================================
// 6. Qixi (鹊桥寄情)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QixiDetailContent(
    accountId: String,
    apiClient: FarmApiClient,
    refreshTrigger: Int,
    onSubtitle: (String?) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<QixiState?>(null) }
    var friends by remember { mutableStateOf<List<FriendItem>>(emptyList()) }
    var selectedFriend by remember { mutableStateOf<FriendItem?>(null) }
    var manualGid by remember { mutableStateOf("") }
    var showFriendPicker by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            runCatching { apiClient.fetchQixi(accountId) }
                .onSuccess { res ->
                    state = res
                    onSubtitle(if (res.active) "进行中" else "已结束")
                }
                .onFailure { onMessage(it.message ?: "读取鹊桥寄情失败") }
            if (friends.isEmpty()) {
                runCatching { apiClient.fetchFriends(accountId).friends }
                    .onSuccess { friends = it.filter { f -> f.gid > 0L } }
            }
            loading = false
        }
    }

    LaunchedEffect(accountId, refreshTrigger) { load() }

    val current = state
    PullToRefreshBox(isRefreshing = loading && current != null, onRefresh = ::load, modifier = Modifier.fillMaxSize()) {
        if (current == null) {
            AppleLoadingState()
            return@PullToRefreshBox
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("鹊羽余额", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                Text(current.featherBalance, style = MaterialTheme.typography.titleLarge, color = apple.orange)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("香囊余额", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                Text(current.sachetBalance, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    runCatching { apiClient.claimQixiBridge(accountId) }
                                        .onSuccess { onMessage(it); load() }
                                        .onFailure { onMessage(it.message ?: "领取失败") }
                                    busy = false
                                }
                            },
                            enabled = !busy && current.canClaimBridge,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (current.canClaimBridge) "领取鹊桥阶段奖励" else "暂无可领鹊桥奖励")
                        }
                    }
                }
            }

            item {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("赠送鹊羽香囊", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = selectedFriend?.let { "${it.name} (${it.gid})" } ?: manualGid,
                                onValueChange = {
                                    selectedFriend = null
                                    manualGid = it.filter { ch -> ch.isDigit() }
                                },
                                label = { Text("好友 GID") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = { showFriendPicker = true },
                                enabled = friends.isNotEmpty() && !busy,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("选好友")
                            }
                        }
                        val targetGid = selectedFriend?.gid?.toString() ?: manualGid.trim()
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    runCatching { apiClient.giftQixiSachet(accountId, targetGid) }
                                        .onSuccess { onMessage(it); load() }
                                        .onFailure { onMessage(it.message ?: "赠送失败") }
                                    busy = false
                                }
                            },
                            enabled = !busy && targetGid.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("赠送香囊")
                        }
                    }
                }
            }

            if (current.stages.isNotEmpty()) {
                item { AppleSectionHeader(title = "鹊桥阶段") }
                items(current.stages, key = { it.id }) { stage ->
                    SimpleStageCard(stage = stage)
                }
            }
        }
    }

    if (showFriendPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var friendKeyword by remember { mutableStateOf("") }
        val filteredFriends = remember(friends, friendKeyword) {
            val kw = friendKeyword.trim().lowercase()
            if (kw.isEmpty()) friends.take(50) else friends.filter {
                it.name.lowercase().contains(kw) || it.gid.toString().contains(kw)
            }.take(50)
        }
        ModalBottomSheet(onDismissRequest = { showFriendPicker = false }, sheetState = sheetState, containerColor = apple.card) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("选择赠礼好友", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                AppleSearchField(
                    value = friendKeyword,
                    onValueChange = { friendKeyword = it },
                    placeholder = "搜索好友昵称或 GID",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (filteredFriends.isEmpty()) {
                    Text(
                        text = "未找到匹配好友",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                filteredFriends.forEach { friend ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppleRadius.tile))
                            .clickable {
                                selectedFriend = friend
                                manualGid = friend.gid.toString()
                                showFriendPicker = false
                            },
                        shape = RoundedCornerShape(AppleRadius.tile),
                        color = apple.elevated,
                        border = BorderStroke(0.5.dp, apple.separator),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(friend.name, style = MaterialTheme.typography.bodyMedium, color = apple.primaryText)
                            Text("Lv.${friend.level}", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// =====================================================================
// 7. QingMei (青酿换万金)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QingMeiDetailContent(
    accountId: String,
    apiClient: FarmApiClient,
    refreshTrigger: Int,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<QingMeiState?>(null) }
    var brewTarget by remember { mutableStateOf<QingMeiState?>(null) }
    var brewAmount by remember { mutableStateOf("1") }

    fun load() {
        if (loading || busy) return
        loading = true
        scope.launch {
            runCatching { apiClient.fetchQingMei(accountId) }
                .onSuccess { state = it }
                .onFailure { onMessage(it.message ?: "读取青酿换万金失败") }
            loading = false
        }
    }

    fun op(actionPath: String, body: JSONObject = JSONObject()) {
        if (busy || loading) return
        busy = true
        scope.launch {
            runCatching { apiClient.operateQingMei(accountId, actionPath, body) }
                .onSuccess { onMessage(it) }
                .onFailure {
                    if (it is CancellationException) throw it
                    onMessage(it.message ?: "操作失败")
                }
            busy = false
            load()
        }
    }

    LaunchedEffect(accountId, refreshTrigger) { load() }

    val current = state
    PullToRefreshBox(isRefreshing = loading && current != null, onRefresh = ::load, modifier = Modifier.fillMaxSize()) {
        if (current == null) {
            AppleLoadingState()
            return@PullToRefreshBox
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("可用青梅：${current.balance}", style = MaterialTheme.typography.titleMedium, color = apple.emerald)
                        Text("轮次 ${current.currentRound}/${current.maxRounds}", style = MaterialTheme.typography.labelLarge, color = apple.secondaryText)
                    }
                    Button(
                        onClick = { op("daily-seed/claim") },
                        enabled = !busy && !current.dailySeedClaimed,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (current.dailySeedClaimed) "今日青梅种子已领取" else "领取今日青梅种子")
                    }
                }
            }

            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (current.started) "当前酿造估值：${current.currentQuoteGold} 金币" else "尚未开启本轮酿造",
                        style = MaterialTheme.typography.titleSmall,
                        color = apple.orange,
                    )
                    if (!current.started) {
                        Button(
                            onClick = {
                                brewAmount = "1"
                                brewTarget = current
                            },
                            enabled = !busy && !loading && current.canStart &&
                                current.firstIngredientUid.isNotBlank() && current.firstIngredientCount > 0L,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("投入青梅开始酿造")
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { op("brew/continue") },
                                enabled = !busy && current.canContinue && !current.finished,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("继续发酵")
                            }
                            Button(
                                onClick = { op("brew/settle") },
                                enabled = !busy && current.canSettle,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("立即结算售出")
                            }
                        }
                    }
                }
            }
        }
    }

    brewTarget?.let { target ->
        val count = brewAmount.toLongOrNull()
        AppleAlertDialog(
            onDismissRequest = { brewTarget = null },
            title = { Text("投入青梅") },
            text = {
                OutlinedTextField(
                    value = brewAmount,
                    onValueChange = { brewAmount = it },
                    label = { Text("数量") },
                    supportingText = { Text("本组可投入 ${target.firstIngredientCount} 个") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    enabled = !busy && !loading && count != null && count in 1L..target.firstIngredientCount,
                    onClick = {
                        if (count != null && count in 1L..target.firstIngredientCount) {
                            val ingredient = JSONObject()
                                .put("uid", target.firstIngredientUid)
                                .put("count", count.toString())
                            brewTarget = null
                            op("brew/start", JSONObject().put("ingredients", JSONArray().put(ingredient)))
                        }
                    },
                ) { Text("开始酿造") }
            },
            dismissButton = { TextButton(onClick = { brewTarget = null }) { Text("取消") } },
        )
    }
}

// =====================================================================
// 8. Charity (公益小红花)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharityDetailContent(
    accountId: String,
    apiClient: FarmApiClient,
    refreshTrigger: Int,
    onSubtitle: (String?) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<CharityState?>(null) }
    var confirmDonate by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            runCatching { apiClient.fetchCharity(accountId) }
                .onSuccess {
                    state = it
                    onSubtitle(if (it.active) "进行中" else "已结束")
                }
                .onFailure { onMessage(it.message ?: "读取公益小红花失败") }
            loading = false
        }
    }

    fun op(actionPath: String, body: JSONObject = JSONObject()) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { apiClient.operateCharity(accountId, actionPath, body) }
                .onSuccess { onMessage(it); load() }
                .onFailure { onMessage(it.message ?: "操作失败") }
            busy = false
        }
    }

    LaunchedEffect(accountId, refreshTrigger) { load() }

    val current = state
    PullToRefreshBox(isRefreshing = loading, onRefresh = ::load, modifier = Modifier.fillMaxSize()) {
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在同步公益进度...", color = apple.secondaryText)
            }
            return@PullToRefreshBox
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("持有爱心", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                Text(current.loveBalance, style = MaterialTheme.typography.titleLarge, color = apple.red)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("已捐出爱心", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                Text(current.donatedLove, style = MaterialTheme.typography.titleLarge, color = apple.emerald)
                            }
                        }
                        OutlinedButton(
                            onClick = { op("seeds/claim") },
                            enabled = !busy && current.canClaimSeeds,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("领取今日种子")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { confirmDonate = true },
                                enabled = !busy && current.canDonateLove,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("捐出爱心")
                            }
                            Button(
                                onClick = { op("daily-gift/claim") },
                                enabled = !busy && current.canClaimDailyGift,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("领每日礼包")
                            }
                        }
                    }
                }
            }

            if (current.stages.isNotEmpty()) {
                item { AppleSectionHeader(title = "捐赠里程碑") }
                items(current.stages, key = { it.id }) { stage ->
                    SimpleStageCard(
                        stage = stage,
                        actionLabel = if (stage.claimable) "领取里程碑" else null,
                        onAction = { op("progress/claim", JSONObject().put("target", stage.id)) },
                        busy = busy,
                    )
                }
            }
        }
    }

    if (confirmDonate) {
        AppleAlertDialog(
            onDismissRequest = { if (!busy) confirmDonate = false },
            title = { Text("确认捐出全部可用爱心") },
            text = { Text("当前持有爱心：${current?.loveBalance ?: "0"}，确认捐出以推进公益进度？") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDonate = false
                        op("love/donate")
                    },
                    enabled = !busy,
                ) { Text("确认捐出") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDonate = false }, enabled = !busy) { Text("取消") }
            },
        )
    }
}

// =====================================================================
// 9. Weather (雨落成诗)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherDetailContent(
    accountId: String,
    apiClient: FarmApiClient,
    refreshTrigger: Int,
    onSubtitle: (String?) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<WeatherState?>(null) }

    fun load() {
        scope.launch {
            loading = true
            runCatching { apiClient.fetchWeather(accountId) }
                .onSuccess {
                    state = it
                    onSubtitle(it.activeWeatherName.ifBlank { "雨落成诗" })
                }
                .onFailure { onMessage(it.message ?: "读取雨落成诗失败") }
            loading = false
        }
    }

    fun op(actionPath: String) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { apiClient.operateWeather(accountId, actionPath) }
                .onSuccess { onMessage(it); load() }
                .onFailure { onMessage(it.message ?: "操作失败") }
            busy = false
        }
    }

    LaunchedEffect(accountId, refreshTrigger) { load() }

    val current = state
    PullToRefreshBox(isRefreshing = loading, onRefresh = ::load, modifier = Modifier.fillMaxSize()) {
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在同步天气数据...", color = apple.secondaryText)
            }
            return@PullToRefreshBox
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "当前天气：${current.activeWeatherName.ifBlank { "晴朗" }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("徽章 ${current.badgeBalance}", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                        Text("采集瓶 ${current.collectionBottleCount}", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                        Text("召唤瓶 ${current.rainBottleCount}", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                    }
                }
            }

            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { op("summon") },
                        enabled = !busy && current.canSummonRain,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (current.canSummonRain) "召唤雷雨" else "暂不可召唤雷雨")
                    }
                    OutlinedButton(
                        onClick = { op("shop/exchange") },
                        enabled = !busy && current.canExchangeCollector,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("兑换天气采集瓶")
                    }
                    if (current.nextResearchNodeId.isNotBlank()) {
                        OutlinedButton(
                            onClick = { op("research/${current.nextResearchNodeId}/advance") },
                            enabled = !busy && current.canAdvanceResearch,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("点亮科研节点 #${current.nextResearchNodeId}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleStageCard(
    stage: SeasonalSimpleStage,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    busy: Boolean = false,
) {
    val apple = LocalAppleColors.current
    val col = when {
        stage.claimed -> apple.green
        stage.claimable -> apple.orange
        else -> apple.secondaryText
    }
    ActivityTierCard(
        title = stage.label,
        statusText = stage.statusText,
        statusColor = col,
        highlighted = stage.claimable,
        rewards = stage.rewards,
        actionLabel = actionLabel,
        actionEnabled = !busy && stage.claimable,
        onAction = onAction,
    )
}
