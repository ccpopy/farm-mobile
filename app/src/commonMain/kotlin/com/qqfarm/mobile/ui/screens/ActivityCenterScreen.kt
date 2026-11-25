package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WineBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.ActivityDirectoryItem
import com.qqfarm.mobile.data.model.ActivityDirectoryResponse
import com.qqfarm.mobile.data.model.GameSpace
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.ScrollToTopOnTabReselect
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.platform.formatEpoch
import com.qqfarm.mobile.platform.wallClockMillis
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.delay

private val SUPPORTED_ACTIVITY_KEYS = setOf(
    "muge",
    "milk",
    "happy",
    "wish",
    "stellar",
    "qixi",
    "qingmei",
    "charity",
    "weather",
)

private val STANDING_ACTIVITY_IDS = setOf("2026092200", "2026092201")
private val FRIEND_RECALL_IDS = setOf("2026111800", "2026111801")
private const val ONE_YEAR_MS = 365L * 86_400_000L
private const val MIN_VALID_ACTIVITY_TIME_MS = 946_684_800_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCenterScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    gameSpace: GameSpace,
    onDetailOpenChange: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()

    var loading by remember(accountId, gameSpace, isConnected) { mutableStateOf(false) }
    var directory by remember(accountId, gameSpace, isConnected) { mutableStateOf<ActivityDirectoryResponse?>(null) }
    var errorText by remember(accountId, gameSpace, isConnected) { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showEnded by remember { mutableStateOf(false) }
    var selectedActivity by remember(accountId, gameSpace, isConnected) { mutableStateOf<Pair<String, ActivityDirectoryItem>?>(null) }
    var clockTickMs by remember { mutableLongStateOf(com.qqfarm.mobile.platform.monotonicMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            clockTickMs = com.qqfarm.mobile.platform.monotonicMillis()
        }
    }

    LaunchedEffect(selectedActivity) {
        onDetailOpenChange(selectedActivity != null)
    }

    DisposableEffect(Unit) {
        onDispose { onDetailOpenChange(false) }
    }

    com.qqfarm.mobile.platform.PlatformBackHandler(enabled = selectedActivity != null) {
        selectedActivity = null
    }

    fun loadDirectory() {
        if (isConnected) refreshTrigger++
    }

    LaunchedEffect(accountId, isConnected, gameSpace, refreshTrigger) {
        if (!isConnected) return@LaunchedEffect
        loading = true
        errorText = null
        try {
            directory = apiClient.fetchActivityDirectory(accountId, gameSpace == GameSpace.RANCH)
        } catch (e: ApiException) {
            errorText = e.message
        } finally {
            loading = false
        }
    }

    val activeDetail = selectedActivity
    if (activeDetail != null) {
        ActivityDetailScreen(
            gameplayKey = activeDetail.first,
            directoryItem = activeDetail.second,
            session = session,
            apiClient = apiClient,
            onBack = { selectedActivity = null },
            onMessage = onMessage,
        )
        return
    }

    if (accountId.isBlank()) {
        AppleEmptyState(
            icon = Icons.Rounded.Celebration,
            title = "未选择账号",
            subtitle = "请先切换或登录账号后查看活动目录",
        )
        return
    }

    if (!isConnected) {
        AppleEmptyState(
            icon = Icons.Rounded.Celebration,
            title = "账号未上线",
            subtitle = "请先在总览启动账号，实时同步活动列表与玩法进度",
        )
        return
    }

    val nowMs = directory?.let {
        if (it.serverTimeMs > 0L) {
            it.serverTimeMs + (clockTickMs - it.receivedAtElapsedMs).coerceAtLeast(0L)
        } else {
            wallClockMillis()
        }
    } ?: wallClockMillis()

    val classified = remember(directory, nowMs) {
        classifyActivities(directory?.activities.orEmpty(), nowMs)
    }

    val monthFmt = remember { { ms: Long -> formatEpoch("yyyy 年 M 月结束", ms) } }
    val dateFmt = remember { { ms: Long -> formatEpoch("MM-dd HH:mm", ms) } }

    val dedupedEnded = remember(classified.ended) {
        classified.ended
            .sortedByDescending { it.item.endTimeMs ?: 0L }
            .distinctBy { Triple(it.item.name, it.item.startTimeMs, it.item.endTimeMs) }
    }

    val endedGroups = remember(dedupedEnded, monthFmt) {
        dedupedEnded
            .groupBy { entry ->
                val endMs = entry.item.endTimeMs
                if (endMs != null && endMs >= MIN_VALID_ACTIVITY_TIME_MS) monthFmt(endMs) else "历史活动"
            }
            .entries
            .toList()
    }

    PullToRefreshBox(
        isRefreshing = loading && directory != null,
        onRefresh = ::loadDirectory,
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        if (directory == null && errorText == null) {
            AppleLoadingState()
            return@PullToRefreshBox
        }
        val listState = rememberLazyListState()
        ScrollToTopOnTabReselect(2, listState)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!errorText.isNullOrBlank()) {
                item {
                    AppleGroupedCard(
                        contentPadding = PaddingValues(14.dp),
                        containerColor = apple.red.copy(alpha = 0.08f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = errorText.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.red,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = ::loadDirectory) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "重试",
                                    tint = apple.red,
                                )
                            }
                        }
                    }
                }
            }

            if (classified.timed.isNotEmpty()) {
                item {
                    AppleSectionHeader(
                        title = if (gameSpace == GameSpace.RANCH) {
                            "牧场活动 (${classified.timed.size})"
                        } else {
                            "限时活动 (${classified.timed.size})"
                        },
                        actionLabel = "刷新",
                        onAction = ::loadDirectory,
                    )
                }
                items(classified.timed, key = { it.item.id + "_" + (it.gameplayKey ?: "") }) { entry ->
                    ActivityDirectoryCard(
                        entry = entry,
                        nowMs = nowMs,
                        dateFmt = dateFmt,
                        accentColor = if (gameSpace == GameSpace.RANCH) apple.orange else apple.emerald,
                        onClick = {
                            val key = entry.gameplayKey
                            val upcoming = entry.item.startTimeMs != null && entry.item.startTimeMs > nowMs
                            when {
                                upcoming -> onMessage("「${entry.item.name}」尚未开始")
                                key != null && key in SUPPORTED_ACTIVITY_KEYS -> selectedActivity = key to entry.item
                                else -> onMessage("「${entry.item.name}」暂未适配移动端玩法")
                            }
                        },
                    )
                }
            } else if (!loading && errorText == null && classified.standing.isEmpty() && classified.ended.isEmpty()) {
                item {
                    AppleEmptyState(
                        icon = Icons.Rounded.Celebration,
                        title = if (gameSpace == GameSpace.RANCH) "暂无牧场活动" else "暂无进行中活动",
                        subtitle = "下拉刷新同步服务器最新活动目录",
                        actionLabel = "立即刷新",
                        onAction = ::loadDirectory,
                    )
                }
            }

            if (classified.standing.isNotEmpty()) {
                item {
                    AppleSectionHeader(title = "常驻活动 (${classified.standing.size})")
                }
                item {
                    AppleGroupedCard {
                        classified.standing.forEachIndexed { idx, entry ->
                            if (idx > 0) AppleHairlineDivider(startIndent = 70.dp)
                            val activity = entry.item
                            val isNewbie = activity.isNewbieActivity()
                            val upcoming = activity.startTimeMs != null && activity.startTimeMs > nowMs
                            val openKey = entry.gameplayKey?.takeIf { it in SUPPORTED_ACTIVITY_KEYS && !upcoming }
                            val highlighted = isNewbie || openKey != null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (openKey != null) {
                                            Modifier.clickable { selectedActivity = openKey to activity }
                                        } else Modifier
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RemoteAssetImage(
                                    url = resolveActivityIconUrl(activity, entry.gameplayKey),
                                    contentDescription = null,
                                    fallbackIcon = resolveActivityFallbackIcon(activity, entry.gameplayKey),
                                    fallbackTint = if (highlighted) apple.emerald else apple.secondaryText,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (highlighted) apple.emerald.copy(alpha = 0.12f) else apple.track),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = entry.item.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = apple.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isNewbie) {
                                        Text(
                                            text = "资格以游戏内为准",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = apple.secondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (openKey != null) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = "进入活动",
                                        tint = apple.secondaryText,
                                    )
                                } else {
                                    AppleStatusPill(
                                        text = if (upcoming) "未开始" else "长期开放",
                                        color = if (upcoming) apple.orange else apple.emerald,
                                        showDot = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (endedGroups.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showEnded = !showEnded },
                        shape = RoundedCornerShape(14.dp),
                        color = apple.card,
                        border = BorderStroke(0.5.dp, apple.separator),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = apple.secondaryText,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "往期活动 (${dedupedEnded.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = apple.primaryText,
                                )
                            }
                            Icon(
                                imageVector = if (showEnded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (showEnded) "收起" else "展开",
                                tint = apple.secondaryText,
                            )
                        }
                    }
                }

                if (showEnded) {
                    endedGroups.forEach { (monthLabel, entries) ->
                        item(key = "ended_header_$monthLabel") {
                            Text(
                                text = monthLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = apple.secondaryText,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                        item(key = "ended_group_$monthLabel") {
                            AppleGroupedCard(modifier = Modifier.alpha(0.86f)) {
                                entries.forEachIndexed { idx, entry ->
                                    if (idx > 0) AppleHairlineDivider(startIndent = 56.dp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        RemoteAssetImage(
                                            url = resolveActivityIconUrl(entry.item, entry.gameplayKey),
                                            contentDescription = entry.item.name,
                                            fallbackIcon = resolveActivityFallbackIcon(entry.item, entry.gameplayKey),
                                            fallbackTint = apple.secondaryText,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(apple.track),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.item.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = apple.primaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            val endMs = entry.item.endTimeMs
                                            val endStr = if (endMs != null && endMs >= MIN_VALID_ACTIVITY_TIME_MS) {
                                                "结束于 ${dateFmt(endMs)}"
                                            } else "已结束"
                                            Text(
                                                text = endStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = apple.secondaryText,
                                            )
                                        }
                                        AppleStatusPill(
                                            text = "已结束",
                                            color = apple.secondaryText,
                                            showDot = false,
                                        )
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

private data class ClassifiedActivity(
    val item: ActivityDirectoryItem,
    val gameplayKey: String?,
    val statusOrder: Int,
)

private data class ClassifiedDirectory(
    val timed: List<ClassifiedActivity>,
    val standing: List<ClassifiedActivity>,
    val ended: List<ClassifiedActivity>,
)

private fun ActivityDirectoryItem.isNewbieActivity(): Boolean =
    id.trim() in STANDING_ACTIVITY_IDS || activityIds.any { it.trim() in STANDING_ACTIVITY_IDS } || "新田园" in name

private fun classifyActivities(
    raw: List<ActivityDirectoryItem>,
    nowMs: Long,
): ClassifiedDirectory {
    val timed = mutableListOf<ClassifiedActivity>()
    val standing = mutableListOf<ClassifiedActivity>()
    val ended = mutableListOf<ClassifiedActivity>()

    raw.forEach { item ->
        val key = resolveGameplayKey(item)
        val supported = key != null && key in SUPPORTED_ACTIVITY_KEYS
        val start = item.startTimeMs
        val end = item.endTimeMs
        val durationMs = if (start != null && end != null) end - start else 0L

        // 与网页端一致：只要有结束时间且已过（含 0 / 负数等异常时间）就归入往期，
        // 避免异常时间的条目被当成“进行中”展示。
        if (end != null && nowMs >= end) {
            ended.add(ClassifiedActivity(item, key, 2))
        } else if (item.isNewbieActivity() || (!supported && durationMs > ONE_YEAR_MS)) {
            standing.add(ClassifiedActivity(item, key, 1))
        } else {
            val isUpcoming = start != null && start > nowMs
            val sortOrder = when {
                !isUpcoming && supported -> 0
                !isUpcoming -> 1
                else -> 2
            }
            timed.add(ClassifiedActivity(item, key, sortOrder))
        }
    }

    return ClassifiedDirectory(
        timed = timed.sortedWith(compareBy<ClassifiedActivity> { it.statusOrder }.thenBy { it.item.endTimeMs ?: Long.MAX_VALUE }),
        standing = standing,
        ended = ended,
    )
}

internal fun resolveGameplayKey(item: ActivityDirectoryItem): String? {
    val directTargets = buildList {
        item.detailTarget?.let { add(it) }
        item.gameplayKey?.let { add(it) }
        addAll(item.gameplayTargets)
    }
    for (raw in directTargets) {
        val norm = raw.trim().lowercase()
        when {
            norm in SUPPORTED_ACTIVITY_KEYS -> return norm
            norm == "pet" || norm == "pet-diary" -> return "pet"
            norm == "share-reward" -> return "happy"
            norm == "autumn-wish" -> return "wish"
            norm == "milk-tea" -> return "milk"
            norm == "charity-red-flower" -> return "charity"
        }
    }

    val ids = listOf(item.id) + item.activityIds
    for (rawId in ids) {
        val id = rawId.trim()
        when (id) {
            "2026111700", "2026111701", "2026111702", "2026111703", "2026111704", "2026111705", "2026111706" -> return "muge"
            "2026110100", "2026110101", "2026110102", "2026110103" -> return "milk"
            "2026092500", "2026092501", "2026110104" -> return "happy"
            "2026092400", "2026092401" -> return "wish"
            "2026072700", "2026072701" -> return "stellar"
            "2026081800", "2026081801", "2026081802" -> return "qixi"
            "2026081200", "2026081201", "2026081202" -> return "qingmei"
            "2026090900", "2026090901" -> return "charity"
            "2026070300", "2026070301", "2026070302", "2026070303", "2026070304", "2026070305" -> return "weather"
            "2026090100", "2026090101", "2026090102", "2026090103" -> return "pet"
        }
    }

    val title = item.name
    return when {
        "牧歌" in title -> "muge"
        "奶茶" in title -> "milk"
        "快乐" in title || "独享" in title -> "happy"
        "秋祈" in title || "良愿" in title -> "wish"
        "雨落" in title -> "weather"
        "鹊桥" in title || "七夕" in title -> "qixi"
        "青梅" in title || "青酿" in title -> "qingmei"
        "公益" in title || "小红花" in title -> "charity"
        "千星" in title || "星砂" in title || "星宿" in title -> "stellar"
        "萌宠" in title || "日记" in title -> "pet"
        else -> null
    }
}

private fun resolveActivityIconUrl(item: ActivityDirectoryItem, key: String?): String {
    val allIds = listOf(item.id) + item.activityIds
    if (allIds.any { it.trim() in FRIEND_RECALL_IDS } || "故友重逢" in item.name) {
        return "/activity-assets/friend-recall/icon.png"
    }
    return when (key) {
        "muge" -> "/activity-assets/muge/icon.png"
        "milk" -> "/activity-assets/milk-tea/logo.png"
        "wish" -> "/activity-assets/autumn/wish.png"
        "happy" -> "/activity-assets/autumn/happy.png"
        "pet" -> "/activity-assets/pet-diary/img_s3PhotoWall_photo0.webp"
        else -> ""
    }
}

private fun resolveActivityFallbackIcon(item: ActivityDirectoryItem, key: String?): ImageVector {
    if (item.isNewbieActivity()) {
        return Icons.Rounded.CardGiftcard
    }
    val allIds = listOf(item.id) + item.activityIds
    if (allIds.any { it.trim() in FRIEND_RECALL_IDS } || "故友" in item.name) {
        return Icons.Rounded.People
    }
    return when (key) {
        "muge" -> Icons.Rounded.Pets
        "milk" -> Icons.Rounded.LocalCafe
        "wish" -> Icons.Rounded.AutoAwesome
        "happy" -> Icons.Rounded.Celebration
        "stellar" -> Icons.Rounded.Stars
        "qixi" -> Icons.Rounded.Favorite
        "qingmei" -> Icons.Rounded.WineBar
        "charity" -> Icons.Rounded.Favorite
        "weather" -> Icons.Rounded.WaterDrop
        "pet" -> Icons.Rounded.Pets
        else -> Icons.Rounded.Celebration
    }
}

@Composable
private fun ActivityDirectoryCard(
    entry: ClassifiedActivity,
    nowMs: Long,
    dateFmt: (Long) -> String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val item = entry.item
    val key = entry.gameplayKey
    val supported = key != null && key in SUPPORTED_ACTIVITY_KEYS
    val upcoming = item.startTimeMs != null && item.startTimeMs > nowMs
    val canOpen = supported && !upcoming
    val targetTimeMs = if (upcoming) item.startTimeMs else item.endTimeMs
    val remainSec = if (targetTimeMs != null && targetTimeMs > nowMs) {
        ((targetTimeMs - nowMs) / 1000L).coerceAtLeast(1L)
    } else 0L

    AppleGroupedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemoteAssetImage(
                url = resolveActivityIconUrl(item, key),
                contentDescription = item.name,
                fallbackIcon = resolveActivityFallbackIcon(item, key),
                fallbackTint = if (canOpen) accentColor else apple.secondaryText,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canOpen) accentColor.copy(alpha = 0.12f) else apple.track),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = apple.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    AppleStatusPill(
                        text = if (upcoming) "未开始" else "进行中",
                        color = if (upcoming) apple.orange else apple.green,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = apple.secondaryText,
                        modifier = Modifier.size(13.dp),
                    )
                    val scheduleText = buildString {
                        if (remainSec > 0L) {
                            append(if (upcoming) "距开启 " else "剩余 ")
                            append(formatDurationSec(remainSec))
                        } else if (item.startTimeMs != null && item.endTimeMs != null) {
                            append(dateFmt(item.startTimeMs ?: 0L))
                            append(" ~ ")
                            append(dateFmt(item.endTimeMs ?: 0L))
                        } else {
                            append("活动进行中")
                        }
                    }
                    Text(
                        text = scheduleText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = apple.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (canOpen) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "进入活动",
                    tint = apple.secondaryText,
                )
            } else {
                AppleStatusPill(
                    text = if (upcoming) "待开启" else "待适配",
                    color = if (upcoming) apple.orange else apple.secondaryText,
                    showDot = false,
                )
            }
        }
    }
}
