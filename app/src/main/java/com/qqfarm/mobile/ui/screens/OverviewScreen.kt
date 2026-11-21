package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import com.qqfarm.mobile.data.model.MallGoods
import com.qqfarm.mobile.data.network.ApiException
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.DailyGiftItem
import com.qqfarm.mobile.data.model.GameSpace
import com.qqfarm.mobile.data.model.RanchPayload
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleIconBadge
import com.qqfarm.mobile.ui.components.AppleMetricTile
import com.qqfarm.mobile.ui.components.AppleQuickEntryRow
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.QuickEntryItem
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatBucketHours
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.components.formatNumber
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxHeight
import com.qqfarm.mobile.ui.components.EqualHeightRow
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ShoppingBasket
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleSearchField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    gameSpace: GameSpace,
    onRefresh: () -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    onStartAccount: (String) -> Unit,
    onClearLogs: () -> Unit,
    onMessage: (String) -> Unit,
    onSwitchSpace: (GameSpace) -> Unit,
    onOpenDiscover: (Int, Int) -> Unit,
    onOpenWorkspace: (Int) -> Unit,
    onOpenAutomation: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    var clockTickMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            clockTickMs = SystemClock.elapsedRealtime()
        }
    }

    var showAllLogs by remember { mutableStateOf(false) }

    val accountId = session.config.currentAccountId
    val isOnline = session.status?.connected == true && accountId.isNotBlank()
    var showBuyFertilizer by remember(accountId, isOnline, gameSpace) { mutableStateOf(false) }
    var ranchPayload by remember { mutableStateOf<RanchPayload?>(null) }
    var ranchLoading by remember { mutableStateOf(false) }
    var ranchError by remember { mutableStateOf("") }

    suspend fun loadRanchOverview() {
        if (!isOnline) {
            ranchPayload = null
            ranchError = ""
            return
        }
        ranchLoading = true
        ranchError = ""
        runCatching { apiClient.fetchRanchAnimals(accountId) }
            .onSuccess { ranchPayload = it }
            .onFailure { ranchError = it.message ?: "读取牧场概览失败" }
        ranchLoading = false
    }

    LaunchedEffect(gameSpace, accountId, isOnline) {
        if (gameSpace == GameSpace.RANCH) {
            loadRanchOverview()
        }
    }

    PullToRefreshBox(
        isRefreshing = if (gameSpace == GameSpace.RANCH) {
            ranchLoading || session.accountsLoading
        } else {
            session.statusLoading || session.accountsLoading
        },
        onRefresh = {
            onRefresh()
            if (gameSpace == GameSpace.RANCH) {
                scope.launch { loadRanchOverview() }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        if (session.accounts.isEmpty() && !session.accountsLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    AppleGroupedCard {
                        AppleEmptyState(
                            icon = Icons.Rounded.Person,
                            title = "尚未添加农场账号",
                            subtitle = "点击下方按钮添加 QQ 或微信农场账号，即可开始智能挂机与实时监控",
                            actionLabel = "添加 / 切换账号",
                            onAction = onOpenAccountSwitcher,
                        )
                    }
                }
            }
        } else if (gameSpace == GameSpace.RANCH) {
            RanchOverviewContent(
                session = session,
                ranch = ranchPayload,
                loading = ranchLoading,
                error = ranchError,
                clockTickMs = clockTickMs,
                onStartAccount = onStartAccount,
                onRetry = { scope.launch { loadRanchOverview() } },
                onSwitchToFarm = { onSwitchSpace(GameSpace.FARM) },
                onOpenDiscover = onOpenDiscover,
                onOpenWorkspace = onOpenWorkspace,
                onOpenAllLogs = { showAllLogs = true },
            )
        } else {
            val status = session.status
            val account = session.currentAccount
            val isOnline = status?.connected == true
            val elapsedSinceSnapshotSec = if (status != null && status.receivedAtElapsedMs > 0L) {
                ((clockTickMs - status.receivedAtElapsedMs) / 1000L).coerceAtLeast(0L)
            } else 0L
            val estimatedServerNowMs = if (status != null && status.serverTimeMs > 0L) {
                status.serverTimeMs + elapsedSinceSnapshotSec * 1000L
            } else {
                System.currentTimeMillis()
            }

            val uptimeSec = if (isOnline && status != null) {
                (status.uptimeSec + elapsedSinceSnapshotSec).coerceAtLeast(0L)
            } else 0L
            val expRateText = remember(status, uptimeSec) {
                if (status == null || !isOnline) "0/时" else {
                    val baseSec = if (status.sessionElapsedSec > 0L) {
                        status.sessionElapsedSec + elapsedSinceSnapshotSec
                    } else uptimeSec
                    if (baseSec <= 0L) "0/时"
                    else "${formatNumber(((status.sessionExpGained * 3600.0) / baseSec).toLong())}/时"
                }
            }

            val normalFert = session.systemItems.firstOrNull { it.id == 1011 }
            val organicFert = session.systemItems.firstOrNull { it.id == 1012 }
            val normalColl = session.systemItems.firstOrNull { it.id == 3001 }
            val rareColl = session.systemItems.firstOrNull { it.id == 3002 }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (session.connectionWarning.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = apple.orange.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = apple.orange)
                                Text(
                                    text = session.connectionWarning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.primaryText,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                // Hero Account Card
                item {
                    val avatarUrl = status?.avatarUrl?.ifBlank { account?.avatar.orEmpty() }
                        ?: account?.avatar.orEmpty()
                    val title = when {
                        !status?.name.isNullOrBlank() && !account?.name.isNullOrBlank() && status?.name != account?.name ->
                            "${status?.name} (${account?.name})"
                        !status?.name.isNullOrBlank() -> status!!.name
                        else -> account?.displayTitle ?: "未选择账号"
                    }

                    AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(modifier = Modifier.size(56.dp)) {
                                RemoteAssetImage(
                                    url = avatarUrl,
                                    contentDescription = title,
                                    fallbackIcon = Icons.Rounded.Person,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(apple.track),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(apple.card)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) apple.green else apple.secondaryText.copy(alpha = 0.5f)),
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = apple.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    ) {
                                        Text(
                                            text = "Lv.${status?.level ?: 0}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum",
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    AppleStatusPill(
                                        text = if (isOnline) "在线" else if (account?.running == true) "连接中" else "已停止",
                                        color = if (isOnline) apple.green else if (account?.running == true) apple.orange else apple.secondaryText,
                                    )
                                    if (account != null) {
                                        AppleStatusPill(
                                            text = account.platformLabel,
                                            color = if (account.platform == "wx") apple.green else apple.blue,
                                            showDot = false,
                                        )
                                    }
                                    if (isOnline && uptimeSec > 0L) {
                                        AppleStatusPill(
                                            text = "已运行 ${formatDurationSec(uptimeSec)}",
                                            color = apple.secondaryText,
                                            showDot = false,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val progress = status?.levelProgress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "升级经验",
                                style = MaterialTheme.typography.labelMedium,
                                color = apple.secondaryText,
                            )
                            Text(
                                text = if (progress != null && progress.needed > 0L) {
                                    "${formatNumber(progress.current)} / ${formatNumber(progress.needed)} (${(progress.fraction * 100).toInt()}%)"
                                } else {
                                    "-- / --"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = apple.primaryText,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress?.fraction ?: 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = apple.track,
                            strokeCap = StrokeCap.Round,
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "经验速率 $expRateText",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                            if (!status?.levelForecastText.isNullOrBlank()) {
                                Text(
                                    text = status!!.levelForecastText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        if (account != null && !account.running) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onStartAccount(account.id) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("启动当前账号")
                            }
                        }
                    }
                }

                // Quick Navigation Entries
                item {
                    AppleQuickEntryRow(
                        entries = listOf(
                            QuickEntryItem("活动中心", Icons.Rounded.Celebration, apple.orange) {
                                onOpenDiscover(0, 0)
                            },
                            QuickEntryItem("游戏商城", Icons.Rounded.Storefront, apple.blue) {
                                onOpenDiscover(1, 0)
                            },
                            QuickEntryItem("节令日历", Icons.Rounded.CalendarMonth, apple.red) {
                                onOpenDiscover(2, 0)
                            },
                            QuickEntryItem("自动化设置", Icons.Rounded.Tune, apple.indigo) {
                                onOpenAutomation()
                            },
                        ),
                    )
                }

                // Assets 2x2 Bento
                item {
                    AppleSectionHeader(title = "资产概览")
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        EqualHeightRow(spacing = 10.dp) {
                            val goldDelta = status?.sessionGoldGained ?: 0L
                            AppleMetricTile(
                                icon = Icons.Rounded.Paid,
                                iconTint = apple.orange,
                                label = "金币",
                                value = formatNumber(status?.gold ?: 0L),
                                deltaText = if (goldDelta != 0L) "本次 ${if (goldDelta > 0) "+" else ""}${formatNumber(goldDelta)}" else null,
                                deltaPositive = goldDelta >= 0L,
                                modifier = Modifier.weight(1f),
                            )
                            val couponDelta = status?.sessionCouponGained ?: 0L
                            AppleMetricTile(
                                icon = Icons.Rounded.ConfirmationNumber,
                                iconTint = apple.emerald,
                                label = "点券",
                                value = formatNumber(status?.coupon ?: 0L),
                                deltaText = if (couponDelta != 0L) "本次 ${if (couponDelta > 0) "+" else ""}${formatNumber(couponDelta)}" else null,
                                deltaPositive = couponDelta >= 0L,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        EqualHeightRow(spacing = 10.dp) {
                            AppleMetricTile(
                                icon = Icons.Rounded.Eco,
                                iconTint = Color(0xFFD98200),
                                label = "金豆豆",
                                value = formatNumber(status?.goldBean ?: 0L),
                                modifier = Modifier.weight(1f),
                            )
                            AppleMetricTile(
                                icon = Icons.Rounded.Diamond,
                                iconTint = apple.cyan,
                                label = "钻石",
                                value = formatNumber(session.diamondBalance),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Fertilizer & Collection Stock
                item {
                    AppleSectionHeader(
                        title = "库存与化肥",
                        actionLabel = if (isOnline) "补充化肥" else null,
                        onAction = if (isOnline) ({ showBuyFertilizer = true }) else null,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        EqualHeightRow(spacing = 10.dp) {
                            AppleMetricTile(
                                icon = Icons.Rounded.Science,
                                iconTint = apple.emerald,
                                label = "普通化肥",
                                value = formatBucketHours(normalFert?.count ?: 0L, normalFert?.hoursText.orEmpty()),
                                modifier = Modifier.weight(1f),
                            )
                            AppleMetricTile(
                                icon = Icons.Rounded.AutoAwesome,
                                iconTint = apple.green,
                                label = "有机化肥",
                                value = formatBucketHours(organicFert?.count ?: 0L, organicFert?.hoursText.orEmpty()),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        EqualHeightRow(spacing = 10.dp) {
                            AppleMetricTile(
                                icon = Icons.Rounded.Star,
                                iconTint = apple.indigo,
                                label = "普通收藏点",
                                value = formatNumber(normalColl?.count ?: 0L),
                                modifier = Modifier.weight(1f),
                            )
                            AppleMetricTile(
                                icon = Icons.Rounded.Star,
                                iconTint = apple.orange,
                                label = "典藏收藏点",
                                value = formatNumber(rareColl?.count ?: 0L),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Next Inspection Countdown
                item {
                    AppleSectionHeader(title = "下次巡查倒计时")
                    Spacer(modifier = Modifier.height(4.dp))
                    val farmCountdown = when {
                        !isOnline -> "账号未登录"
                        status?.farmQuiet == true -> "静默中"
                        status != null && status.nextFarmRunAtMs > 0L -> {
                            val rem = ((status.nextFarmRunAtMs - estimatedServerNowMs) / 1000L).coerceAtLeast(0L)
                            if (rem > 0L) formatDurationSec(rem) else "巡查中..."
                        }
                        else -> "巡查中..."
                    }
                    val friendCountdown = when {
                        !isOnline -> "账号未登录"
                        status?.friendQuiet == true -> "静默中"
                        status != null && status.nextFriendRunAtMs > 0L -> {
                            val rem = ((status.nextFriendRunAtMs - estimatedServerNowMs) / 1000L).coerceAtLeast(0L)
                            if (rem > 0L) formatDurationSec(rem) else "巡查中..."
                        }
                        else -> "巡查中..."
                    }

                    AppleGroupedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppleIconBadge(icon = Icons.Rounded.Eco, tint = apple.green)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "自家农场巡查",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = apple.primaryText,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = farmCountdown,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = if (isOnline) apple.primaryText else apple.secondaryText,
                            )
                        }
                        AppleHairlineDivider(startIndent = 60.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppleIconBadge(icon = Icons.Rounded.Group, tint = apple.blue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "好友农场任务",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = apple.primaryText,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = friendCountdown,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = if (isOnline) apple.primaryText else apple.secondaryText,
                            )
                        }
                    }
                }

                // Today's Operations
                if (isOnline && status != null && status.operations.isNotEmpty()) {
                    item {
                        AppleSectionHeader(title = "今日收益统计")
                        Spacer(modifier = Modifier.height(4.dp))
                        val opEntries = status.operations.entries.toList()
                        AppleGroupedCard(contentPadding = PaddingValues(12.dp)) {
                            val rows = opEntries.chunked(4)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                rows.forEach { rowItems ->
                                    EqualHeightRow(spacing = 8.dp) {
                                        rowItems.forEach { (key, count) ->
                                            Surface(
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = apple.elevated,
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                ) {
                                                    Text(
                                                        text = count.toString(),
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontFeatureSettings = "tnum",
                                                        ),
                                                        color = apple.primaryText,
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = opLabel(key),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = apple.secondaryText,
                                                        maxLines = 1,
                                                    )
                                                }
                                            }
                                        }
                                        repeat(4 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Gifts & Tasks
                if (session.dailyGifts != null && session.dailyGifts.gifts.isNotEmpty()) {
                    item {
                        AppleSectionHeader(title = "每日礼包与任务")
                        Spacer(modifier = Modifier.height(4.dp))
                        AppleGroupedCard {
                            val gifts = session.dailyGifts.gifts
                            gifts.forEachIndexed { idx, gift ->
                                DailyGiftRow(gift = gift)
                                if (idx < gifts.lastIndex) {
                                    AppleHairlineDivider(startIndent = 60.dp)
                                }
                            }
                        }
                    }
                }

                // Live Run Logs Preview
                item {
                    AppleSectionHeader(
                        title = "运行日志",
                        actionLabel = "查看全部",
                        onAction = { showAllLogs = true },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val previewLogs = session.logs.takeLast(8).reversed()
                    AppleGroupedCard {
                        if (previewLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "暂无运行日志",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = apple.secondaryText,
                                )
                            }
                        } else {
                            previewLogs.forEachIndexed { idx, log ->
                                LogEntryRow(entry = log)
                                if (idx < previewLogs.lastIndex) {
                                    AppleHairlineDivider(startIndent = 16.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAllLogs) {
        FullLogsSheet(
            accountId = session.config.currentAccountId,
            realtimeLogs = session.logs,
            apiClient = apiClient,
            onClear = onClearLogs,
            onDismiss = { showAllLogs = false },
        )
    }

    if (showBuyFertilizer) {
        BuyFertilizerSheet(
            accountId = accountId,
            apiClient = apiClient,
            onPurchased = onRefresh,
            onMessage = onMessage,
            onDismiss = { showBuyFertilizer = false },
        )
    }
}

@Composable
private fun DailyGiftRow(gift: DailyGiftItem) {
    val apple = LocalAppleColors.current
    val (statusText, statusColor) = when {
        gift.result == "running" -> "检查中" to apple.blue
        gift.result == "error" || gift.result == "partial" -> "未完整领取" to apple.orange
        gift.key == "vip_daily_gift" && gift.isVip == false -> "未开通" to apple.secondaryText
        gift.key == "month_card_gift" && gift.hasCard == false -> "未开通" to apple.secondaryText
        gift.doneToday -> "今日已完成" to apple.green
        gift.enabled -> "等待执行" to apple.blue
        else -> "未开启" to apple.secondaryText
    }
    val sub = buildString {
        if (gift.totalCount > 0) {
            append("进度 ${gift.completedCount}/${gift.totalCount}")
        }
        if (gift.key == "email_rewards" && gift.scheduledHour != null && !gift.doneToday) {
            if (isNotEmpty()) append(" · ")
            append("每天 ${gift.scheduledHour.toString().padStart(2, '0')}:00 检查")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleIconBadge(icon = Icons.Rounded.CardGiftcard, tint = statusColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gift.label.ifBlank { gift.key },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = apple.primaryText,
            )
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                )
            }
        }
        AppleStatusPill(text = statusText, color = statusColor, showDot = false)
    }
}

@Composable
private fun LogEntryRow(entry: RunLogEntry) {
    val apple = LocalAppleColors.current
    val accent = when {
        entry.tag == "错误" || entry.result == "error" || entry.result == "failed" -> apple.red
        entry.isWarn || entry.tag == "警告" || entry.result == "defer" -> apple.orange
        entry.result in listOf("ok", "success", "done") -> apple.green
        else -> apple.blue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.tag.ifBlank { entry.module.ifBlank { "日志" } },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = accent,
                )
                Text(
                    text = entry.time.takeLast(8),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = apple.secondaryText,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.msg,
                style = MaterialTheme.typography.bodySmall,
                color = apple.primaryText,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullLogsSheet(
    accountId: String,
    realtimeLogs: List<RunLogEntry>,
    apiClient: FarmApiClient,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var selectedModule by remember { mutableStateOf("") }
    var warnOnly by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var serverLogs by remember { mutableStateOf<List<RunLogEntry>?>(null) }

    LaunchedEffect(accountId, selectedModule, warnOnly, keyword) {
        if (selectedModule.isBlank() && !warnOnly && keyword.isBlank()) {
            serverLogs = null
        } else {
            runCatching {
                apiClient.fetchLogs(
                    accountId = accountId,
                    module = selectedModule,
                    keyword = keyword.trim(),
                    isWarn = if (warnOnly) true else null,
                    limit = 200,
                )
            }.onSuccess { serverLogs = it }
        }
    }

    val displayLogs = remember(serverLogs, realtimeLogs) {
        (serverLogs ?: realtimeLogs).reversed()
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppleIconBadge(icon = Icons.Rounded.Terminal, tint = apple.indigo)
                    Text("完整运行日志", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                }
                IconButton(onClick = {
                    scope.launch {
                        onClear()
                        serverLogs = emptyList()
                    }
                }) {
                    Icon(Icons.Rounded.ClearAll, contentDescription = "清空日志", tint = apple.red)
                }
            }

            AppleSearchField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = "搜索日志关键字...",
                modifier = Modifier.fillMaxWidth(),
            )

            val modules = listOf(
                "" to "全部",
                "farm" to "农场",
                "ranch" to "牧场",
                "friend" to "好友",
                "warehouse" to "仓库",
                "task" to "任务",
                "system" to "系统",
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AppleFilterChip(
                        selected = warnOnly,
                        onClick = { warnOnly = !warnOnly },
                        label = { Text("仅异常") },
                    )
                }
                items(modules) { (code, label) ->
                    AppleFilterChip(
                        selected = selectedModule == code,
                        onClick = { selectedModule = code },
                        label = { Text(label) },
                    )
                }
            }

            AppleGroupedCard {
                if (displayLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("无匹配日志", color = apple.secondaryText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                    ) {
                        items(displayLogs) { item ->
                            LogEntryRow(entry = item)
                            AppleHairlineDivider(startIndent = 16.dp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuyFertilizerSheet(
    accountId: String,
    apiClient: FarmApiClient,
    onPurchased: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var typeIndex by remember { mutableIntStateOf(0) }
    var offers by remember { mutableStateOf<List<MallGoods>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var purchasing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun loadOffers() {
        loading = true
        error = null
        try {
            offers = apiClient.fetchMallCatalog(accountId, slotType = 1, subSlotType = 0).goods
        } catch (e: ApiException) {
            error = e.message
        } finally {
            loading = false
        }
    }

    LaunchedEffect(accountId) { loadOffers() }
    val offerId = if (typeIndex == 0) 1003 else 1002
    val selectedOffer = offers.firstOrNull { it.id == offerId }

    ModalBottomSheet(
        onDismissRequest = { if (!purchasing) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("补充化肥", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            AppleSegmentedControl(
                options = listOf("普通化肥", "有机化肥"),
                selectedIndex = typeIndex,
                onSelect = { if (!purchasing) typeIndex = it },
            )
            when {
                loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                error != null -> {
                    Text(error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = apple.red)
                    TextButton(onClick = { scope.launch { loadOffers() } }) { Text("重新加载") }
                }

                selectedOffer == null -> {
                    Text("当前商城暂无这款礼包", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
                }

                else -> MallPurchaseContent(
                    goods = selectedOffer,
                    pending = purchasing,
                    onDismiss = onDismiss,
                    onConfirm = { count ->
                        if (purchasing) return@MallPurchaseContent
                        purchasing = true
                        scope.launch {
                            try {
                                apiClient.purchaseMallGoods(
                                    accountId = accountId,
                                    goods = selectedOffer,
                                    slotType = 1,
                                    propsShop = false,
                                    count = count,
                                )
                                onMessage("已购买 ${selectedOffer.name} ×$count")
                                onDismiss()
                                onPurchased()
                            } catch (e: ApiException) {
                                error = e.message
                            } finally {
                                purchasing = false
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun opLabel(key: String): String = when (key) {
    "harvest" -> "收获"
    "farming" -> "务农"
    "fertilize" -> "施肥"
    "plant" -> "种植"
    "steal" -> "偷菜"
    "helpFarming" -> "帮务农"
    "taskClaim" -> "任务"
    "sell" -> "出售"
    else -> key
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RanchOverviewContent(
    session: SessionUiState,
    ranch: RanchPayload?,
    loading: Boolean,
    error: String,
    clockTickMs: Long,
    onStartAccount: (String) -> Unit,
    onRetry: () -> Unit,
    onSwitchToFarm: () -> Unit,
    onOpenDiscover: (Int, Int) -> Unit,
    onOpenWorkspace: (Int) -> Unit,
    onOpenAllLogs: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val status = session.status
    val account = session.currentAccount
    val isOnline = status?.connected == true

    if (!isOnline) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Pets,
                        title = "账号未在线",
                        subtitle = "请先启动当前账号后查看牧场等级、资产、设施与动物状态",
                        actionLabel = if (account != null && !account.running) "启动当前账号" else null,
                        onAction = if (account != null && !account.running) ({ onStartAccount(account.id) }) else null,
                    )
                }
            }
        }
        return
    }

    if (ranch == null && error.isNotBlank()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.WarningAmber,
                        title = "牧场状态读取失败",
                        subtitle = error,
                        actionLabel = "重试",
                        onAction = onRetry,
                    )
                }
            }
        }
        return
    }

    if (ranch != null && !ranch.unlocked) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Pets,
                        title = "牧场尚未解锁",
                        subtitle = ranch.reason.ifBlank { "种植等级达到开启条件并激活牧场后，可在此统筹管理动物、设施与牧场商店" },
                        actionLabel = "返回农场空间",
                        onAction = onSwitchToFarm,
                    )
                }
            }
        }
        return
    }

    val allAnimals = ranch?.pens?.flatMap { it.animals } ?: emptyList()
    val totalCapacity = ranch?.facilities?.filter { it.hasRanch }?.sumOf { it.ranchCapacity }
        ?: ranch?.pens?.sumOf { it.capacity }
        ?: 0
    val blockedCount = allAnimals.count { it.grassBlocked }
    val harvestCount = allAnimals.count { it.canHarvest } + (ranch?.shelvesCollectableCount ?: 0)
    val produceCount = allAnimals.count { it.canProduce }
    val feedCount = allAnimals.count { it.canFeed }
    val hazardsCount = ranch?.hazardsCount ?: 0

    val elapsedSec = if (ranch != null && ranch.fetchedAtElapsedMs > 0L) {
        ((clockTickMs - ranch.fetchedAtElapsedMs) / 1000L).coerceAtLeast(0L)
    } else 0L
    val serverNowMs = if (ranch != null && ranch.serverNowMs > 0L) {
        ranch.serverNowMs + elapsedSec * 1000L
    } else System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (error.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = apple.orange.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = apple.orange)
                        Text(error, style = MaterialTheme.typography.bodySmall, color = apple.primaryText, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Ranch Hero Card
        item {
            val avatarUrl = status?.avatarUrl?.ifBlank { account?.avatar.orEmpty() } ?: account?.avatar.orEmpty()
            val displayName = status?.name?.ifBlank { account?.displayTitle.orEmpty() } ?: account?.displayTitle ?: "我的牧场"
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RemoteAssetImage(
                        url = avatarUrl,
                        contentDescription = displayName,
                        fallbackIcon = Icons.Rounded.Pets,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(apple.track),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = apple.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Surface(
                                shape = CircleShape,
                                color = apple.orange.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "牧场 Lv.${ranch?.level ?: 0}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = apple.orange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AppleStatusPill("在线", apple.green)
                            AppleStatusPill("饲养 ${allAnimals.size} / $totalCapacity", apple.orange, showDot = false)
                            if (account != null) {
                                AppleStatusPill(account.platformLabel, if (account.platform == "wx") apple.green else apple.blue, showDot = false)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                val needed = ranch?.neededExp ?: 0L
                val cur = ranch?.currentExp ?: 0L
                val expFraction = if (needed > 0L) (cur.toFloat() / needed.toFloat()).coerceIn(0f, 1f) else 1f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("牧场经验", style = MaterialTheme.typography.labelMedium, color = apple.secondaryText)
                    Text(
                        text = if (needed > 0L) "${formatCompactCount(cur)} / ${formatCompactCount(needed)} (${(expFraction * 100).toInt()}%)" else if (ranch != null) "已满级" else "--",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = apple.primaryText,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { expFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = apple.orange,
                    trackColor = apple.track,
                    strokeCap = StrokeCap.Round,
                )
                if (ranch != null && needed > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "距 Lv.${ranch.level + 1} 还差 ${formatCompactCount((needed - cur).coerceAtLeast(0L))} 经验",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = apple.secondaryText,
                    )
                }
            }
        }

        // Quick Entries for Ranch
        item {
            AppleQuickEntryRow(
                entries = listOf(
                    QuickEntryItem("牧场商城", Icons.Rounded.Storefront, apple.orange) {
                        onOpenDiscover(1, 0)
                    },
                    QuickEntryItem("仓鼠集市", Icons.Rounded.LocalOffer, apple.emerald) {
                        onOpenDiscover(1, 1)
                    },
                    QuickEntryItem("动物图鉴", Icons.Rounded.MenuBook, apple.indigo) {
                        onOpenWorkspace(4)
                    },
                    QuickEntryItem("牧场活动", Icons.Rounded.Celebration, apple.red) {
                        onOpenDiscover(0, 0)
                    },
                ),
            )
        }

        if (blockedCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = apple.orange.copy(alpha = 0.14f),
                    border = BorderStroke(0.5.dp, apple.orange.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = apple.orange)
                        Text(
                            text = "牧草不足，$blockedCount 只动物已暂停成长",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.primaryText,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onOpenWorkspace(1) }) {
                            Text("去补充牧草", color = apple.orange)
                        }
                    }
                }
            }
        }

        // Pending Animal Ops 2x2
        if (ranch != null) {
            item {
                AppleSectionHeader(
                    title = "牧场待处理",
                    actionLabel = "管理动物",
                    onAction = { onOpenWorkspace(0) },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EqualHeightRow(spacing = 10.dp) {
                        AppleMetricTile(
                            icon = Icons.Rounded.ShoppingBasket,
                            iconTint = apple.green,
                            label = "可收获",
                            value = harvestCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        AppleMetricTile(
                            icon = Icons.Rounded.Autorenew,
                            iconTint = apple.orange,
                            label = "可生产",
                            value = produceCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    EqualHeightRow(spacing = 10.dp) {
                        AppleMetricTile(
                            icon = Icons.Rounded.Grass,
                            iconTint = apple.emerald,
                            label = "可喂养",
                            value = feedCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        AppleMetricTile(
                            icon = Icons.Rounded.CleaningServices,
                            iconTint = apple.indigo,
                            label = "待清理",
                            value = hazardsCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Ranch Assets & Buckets
            item {
                val grassItem = ranch.grass
                val dung = ranch.currencies.firstOrNull { it.id == 7002 }
                val normalBucket = ranch.buckets.firstOrNull { it.id == 7003 }
                val orgBucket = ranch.buckets.firstOrNull { it.id == 7004 }
                val assetCurrencies = listOf(1007, 1002, 1005, 1008).mapNotNull { cid ->
                    ranch.currencies.firstOrNull { it.id == cid }
                }

                if (assetCurrencies.isNotEmpty()) {
                    AppleSectionHeader(title = "牧场资产")
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        assetCurrencies.chunked(2).forEach { pair ->
                            EqualHeightRow(spacing = 10.dp) {
                                pair.forEach { cur ->
                                    AppleMetricTile(
                                        icon = Icons.Rounded.Paid,
                                        iconTint = if (cur.id == 1008) apple.orange else apple.emerald,
                                        label = cur.name.ifBlank { "货币 ${cur.id}" },
                                        value = formatCompactCount(cur.count),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AppleSectionHeader(title = "牧场库存")
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EqualHeightRow(spacing = 10.dp) {
                        AppleMetricTile(
                            icon = Icons.Rounded.Grass,
                            iconTint = apple.green,
                            label = "牧草",
                            value = formatCompactCount(grassItem),
                            modifier = Modifier.weight(1f),
                        )
                        AppleMetricTile(
                            icon = Icons.Rounded.Eco,
                            iconTint = apple.orange,
                            label = dung?.name ?: "便便",
                            value = formatCompactCount(dung?.count ?: 0L),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    EqualHeightRow(spacing = 10.dp) {
                        AppleMetricTile(
                            icon = Icons.Rounded.Science,
                            iconTint = apple.blue,
                            label = "普通罐头",
                            value = formatBucketHours(normalBucket?.seconds ?: 0L),
                            modifier = Modifier.weight(1f),
                        )
                        AppleMetricTile(
                            icon = Icons.Rounded.AutoAwesome,
                            iconTint = apple.indigo,
                            label = "有机罐头",
                            value = formatBucketHours(orgBucket?.seconds ?: 0L),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Facilities Summary
            if (ranch.facilities.isNotEmpty()) {
                item {
                    AppleSectionHeader(
                        title = "牧场设施",
                        actionLabel = "管理设施",
                        onAction = { onOpenWorkspace(1) },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ranch.facilities.forEach { f ->
                                val statusText: String
                                val fraction: Float
                                when {
                                    f.state == 2 || (!f.active && f.moduleType == 7) -> {
                                        statusText = if (f.canActivate) "可解锁" else "待解锁"
                                        fraction = 0f
                                    }
                                    f.upgradeState != 0 -> {
                                        val remSec = ((f.upgradeEndTimeMs - serverNowMs) / 1000L).coerceAtLeast(0L)
                                        statusText = if (remSec > 0L) "升级中 · ${formatDurationSec(remSec)}" else "升级待确认"
                                        fraction = 1f
                                    }
                                    f.hasRanch -> {
                                        statusText = "${f.ranchCount} / ${f.ranchCapacity}"
                                        fraction = if (f.ranchCapacity > 0) (f.ranchCount.toFloat() / f.ranchCapacity).coerceIn(0f, 1f) else 0f
                                    }
                                    f.hasFeed -> {
                                        statusText = "牧草 ${f.feedCount} / ${f.capacity}"
                                        fraction = if (f.capacity > 0) (f.feedCount.toFloat() / f.capacity).coerceIn(0f, 1f) else 0f
                                    }
                                    else -> {
                                        statusText = "容量 ${f.capacity}"
                                        fraction = 0f
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "${f.name}  Lv.${f.level}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = apple.primaryText,
                                        )
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                            color = if (f.upgradeState != 0 || f.canActivate) apple.orange else apple.secondaryText,
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape),
                                        color = apple.orange,
                                        trackColor = apple.track,
                                        strokeCap = StrokeCap.Round,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (loading) {
            item {
                AppleGroupedCard(contentPadding = PaddingValues(24.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("正在读取牧场状态...", color = apple.secondaryText)
                    }
                }
            }
        }

        // Shared Run Logs Preview
        item {
            AppleSectionHeader(
                title = "运行日志",
                actionLabel = "查看全部",
                onAction = onOpenAllLogs,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val previewLogs = session.logs.takeLast(8).reversed()
            AppleGroupedCard {
                if (previewLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("暂无运行日志", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
                    }
                } else {
                    previewLogs.forEachIndexed { idx, log ->
                        LogEntryRow(entry = log)
                        if (idx < previewLogs.lastIndex) {
                            AppleHairlineDivider(startIndent = 16.dp)
                        }
                    }
                }
            }
        }
    }
}
