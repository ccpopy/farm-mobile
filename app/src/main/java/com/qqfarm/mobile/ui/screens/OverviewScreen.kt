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
import androidx.compose.material.icons.rounded.BarChart
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
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.qqfarm.mobile.data.model.DailyGiftItem
import com.qqfarm.mobile.data.model.GameSpace
import com.qqfarm.mobile.data.model.RanchCareer
import com.qqfarm.mobile.data.model.RanchPayload
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.RunLogScope
import com.qqfarm.mobile.data.model.RUN_LOG_RETENTION_DAYS
import com.qqfarm.mobile.data.model.runLogScope
import com.qqfarm.mobile.data.model.scope
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleIconBadge
import com.qqfarm.mobile.ui.components.AppleMetricTile
import com.qqfarm.mobile.ui.components.AppleProgressBar
import com.qqfarm.mobile.ui.components.AppleQuickEntryRow
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.QuickEntryItem
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatBucketHours
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.components.formatNumber
import com.qqfarm.mobile.ui.components.spaceAccent
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.components.retainSheetHeight
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxHeight
import com.qqfarm.mobile.ui.components.EqualHeightRow
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ShoppingBasket
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleSearchField
import com.qqfarm.mobile.ui.components.AppleAlertDialog

private const val INSPECTION_STATUS_REFRESH_MS = 4_000L
private const val INSPECTION_LABEL_GRACE_MS = 12_000L

private fun inspectionCountdownText(nextRunAtMs: Long?, serverNowMs: Long, online: Boolean, quiet: Boolean): String {
    if (!online) return "账号未登录"
    if (quiet) return "静默中"
    if (nextRunAtMs == null || nextRunAtMs == 0L) return "等待下次巡查"

    val remainingMs = nextRunAtMs - serverNowMs
    return when {
        remainingMs > 0L -> formatDurationSec((remainingMs + 999L) / 1000L)
        remainingMs > -INSPECTION_LABEL_GRACE_MS -> "巡查中..."
        else -> "等待下次巡查"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    gameSpace: GameSpace,
    onRefresh: () -> Unit,
    onRefreshInspectionStatus: suspend () -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    onStartAccount: (String) -> Unit,
    onClearLogs: suspend (String) -> Unit,
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

    val accountId = session.config.currentAccountId
    var openLogScope by remember(accountId, gameSpace) { mutableStateOf<RunLogScope?>(null) }
    val isOnline = session.status?.connected == true && accountId.isNotBlank()
    var showBuyFertilizer by remember(accountId, isOnline, gameSpace) { mutableStateOf(false) }
    var showBuyCans by remember(accountId, isOnline, gameSpace) { mutableStateOf(false) }
    var ranchPayload by remember(accountId, isOnline) { mutableStateOf<RanchPayload?>(null) }
    var ranchLoading by remember(accountId, isOnline) { mutableStateOf(false) }
    var ranchError by remember(accountId, isOnline) { mutableStateOf("") }

    suspend fun loadRanchOverview() {
        if (!isOnline) {
            ranchPayload = null
            ranchError = ""
            return
        }
        if (ranchLoading) return
        ranchLoading = true
        ranchError = ""
        try {
            ranchPayload = apiClient.fetchRanchView(accountId, "overview").base
        } catch (e: ApiException) {
            ranchError = e.message
        } finally {
            ranchLoading = false
        }
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
                onRefillCans = { showBuyCans = true },
                onOpenAllLogs = { openLogScope = it },
            )
        } else {
            val status = session.status
            val account = session.currentAccount
            val isOnline = status?.connected == true
            val elapsedSinceSnapshotMs = if (status != null && status.receivedAtElapsedMs > 0L) {
                (clockTickMs - status.receivedAtElapsedMs).coerceAtLeast(0L)
            } else 0L
            val elapsedSinceSnapshotSec = elapsedSinceSnapshotMs / 1000L
            val estimatedServerNowMs = if (status != null && status.serverTimeMs > 0L) {
                status.serverTimeMs + elapsedSinceSnapshotMs
            } else {
                System.currentTimeMillis()
            }

            val farmQuiet = status?.farmQuiet == true
            val friendQuiet = status?.friendQuiet == true
            val farmRunAtMs = status?.nextFarmRunAtMs
            val friendRunAtMs = status?.nextFriendRunAtMs
            val farmOverdue = !farmQuiet && farmRunAtMs != null && farmRunAtMs <= estimatedServerNowMs
            val friendOverdue = !friendQuiet && friendRunAtMs != null && friendRunAtMs <= estimatedServerNowMs
            val needsInspectionStatus = isOnline && (farmOverdue || friendOverdue)
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(lifecycle, accountId, needsInspectionStatus) {
                if (!needsInspectionStatus) return@LaunchedEffect
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        delay(INSPECTION_STATUS_REFRESH_MS)
                        onRefreshInspectionStatus()
                    }
                }
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
                        AppleProgressBar(
                            progress = { progress?.fraction ?: 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = apple.track,
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
                                value = formatCompactCount(status?.gold ?: 0L),
                                exactValue = formatNumber(status?.gold ?: 0L),
                                deltaText = if (goldDelta != 0L) "本次 ${if (goldDelta > 0) "+" else ""}${formatCompactCount(goldDelta)}" else null,
                                exactDeltaText = if (goldDelta != 0L) "本次 ${if (goldDelta > 0) "+" else ""}${formatNumber(goldDelta)}" else null,
                                deltaPositive = goldDelta >= 0L,
                                modifier = Modifier.weight(1f),
                            )
                            val couponDelta = status?.sessionCouponGained ?: 0L
                            AppleMetricTile(
                                icon = Icons.Rounded.ConfirmationNumber,
                                iconTint = apple.emerald,
                                label = "点券",
                                value = formatCompactCount(status?.coupon ?: 0L),
                                exactValue = formatNumber(status?.coupon ?: 0L),
                                deltaText = if (couponDelta != 0L) "本次 ${if (couponDelta > 0) "+" else ""}${formatCompactCount(couponDelta)}" else null,
                                exactDeltaText = if (couponDelta != 0L) "本次 ${if (couponDelta > 0) "+" else ""}${formatNumber(couponDelta)}" else null,
                                deltaPositive = couponDelta >= 0L,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        EqualHeightRow(spacing = 10.dp) {
                            AppleMetricTile(
                                icon = Icons.Rounded.Eco,
                                iconTint = Color(0xFFD98200),
                                label = "金豆豆",
                                value = formatCompactCount(status?.goldBean ?: 0L),
                                exactValue = formatNumber(status?.goldBean ?: 0L),
                                modifier = Modifier.weight(1f),
                            )
                            AppleMetricTile(
                                icon = Icons.Rounded.Diamond,
                                iconTint = apple.cyan,
                                label = "钻石",
                                value = formatCompactCount(session.diamondBalance),
                                exactValue = formatNumber(session.diamondBalance),
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
                                value = formatCompactCount(normalColl?.count ?: 0L),
                                exactValue = formatNumber(normalColl?.count ?: 0L),
                                modifier = Modifier.weight(1f),
                            )
                            AppleMetricTile(
                                icon = Icons.Rounded.Star,
                                iconTint = apple.orange,
                                label = "典藏收藏点",
                                value = formatCompactCount(rareColl?.count ?: 0L),
                                exactValue = formatNumber(rareColl?.count ?: 0L),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Next Inspection Countdown
                item {
                    AppleSectionHeader(title = "下次巡查倒计时")
                    Spacer(modifier = Modifier.height(4.dp))
                    val farmCountdown = inspectionCountdownText(farmRunAtMs, estimatedServerNowMs, isOnline, farmQuiet)
                    val friendCountdown = inspectionCountdownText(friendRunAtMs, estimatedServerNowMs, isOnline, friendQuiet)

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
                        OverviewStatistics("今日收益统计", status.operations.map { (key, count) -> opLabel(key) to count.toString() })
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

                item {
                    RunLogsPreview(
                        entries = session.logs,
                        businessScope = gameSpace.runLogScope,
                        onOpen = { openLogScope = it },
                    )
                }
            }
        }
    }

    openLogScope?.let { openedScope ->
        FullLogsSheet(
            accountId = accountId,
            accountName = session.currentAccount?.displayTitle.orEmpty(),
            entries = session.logs,
            businessScope = gameSpace.runLogScope,
            initialScope = openedScope,
            onClear = onClearLogs,
            onDismiss = { openLogScope = null },
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

    if (showBuyCans) {
        RanchCanRefillSheet(
            accountId = accountId,
            apiClient = apiClient,
            onPurchased = {
                onRefresh()
                scope.launch { loadRanchOverview() }
            },
            onMessage = onMessage,
            onDismiss = { showBuyCans = false },
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
private fun RunLogsPreview(
    entries: List<RunLogEntry>,
    businessScope: RunLogScope,
    onOpen: (RunLogScope) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scopes = listOf(businessScope, RunLogScope.SYSTEM)
    var selected by remember(businessScope) { mutableIntStateOf(0) }
    val scope = scopes[selected]
    val logs = remember(entries, scope) { entries.filter { it.scope == scope }.takeLast(8).asReversed() }
    AppleSectionHeader(title = "运行日志", actionLabel = "查看全部", onAction = { onOpen(scope) })
    Spacer(modifier = Modifier.height(6.dp))
    AppleSegmentedControl(scopes.map { it.label }, selected, onSelect = { selected = it })
    Spacer(modifier = Modifier.height(8.dp))
    AppleGroupedCard {
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("暂无${scope.label}", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
            }
        } else {
            logs.forEachIndexed { index, entry ->
                LogEntryRow(entry)
                if (index < logs.lastIndex) AppleHairlineDivider(startIndent = 16.dp)
            }
        }
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
                    text = entry.time.takeLast(14),
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
    accountName: String,
    entries: List<RunLogEntry>,
    businessScope: RunLogScope,
    initialScope: RunLogScope,
    onClear: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scopes = listOf(businessScope, RunLogScope.SYSTEM)
    var scopeIndex by remember(initialScope) { mutableIntStateOf(if (initialScope == RunLogScope.SYSTEM) 1 else 0) }
    val selectedScope = scopes[scopeIndex]
    var selectedModule by remember(selectedScope) { mutableStateOf("") }
    var warnOnly by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var showCleanup by remember(accountId) { mutableStateOf(false) }

    val displayLogs = remember(entries, selectedScope, selectedModule, warnOnly, keyword) {
        val terms = keyword.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        entries.asReversed().filter { log ->
            log.scope == selectedScope && (!warnOnly || log.isWarn) &&
                (selectedModule.isBlank() || selectedModule == log.module) &&
                terms.all { term ->
                    log.msg.contains(term, ignoreCase = true) ||
                        log.tag.contains(term, ignoreCase = true) ||
                        log.searchText.contains(term, ignoreCase = true)
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
                IconButton(onClick = { showCleanup = true }, enabled = accountId.isNotBlank()) {
                    Icon(Icons.Rounded.ClearAll, contentDescription = "日志清理", tint = apple.red)
                }
            }

            AppleSegmentedControl(scopes.map { it.label }, scopeIndex, onSelect = { scopeIndex = it })
            AppleSearchField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = "搜索日志关键字...",
                modifier = Modifier.fillMaxWidth(),
            )

            val modules = if (selectedScope == RunLogScope.FARM) {
                listOf("" to "全部", "farm" to "农场", "friend" to "好友", "warehouse" to "仓库", "task" to "任务")
            } else {
                emptyList()
            }
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
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                ) {
                    if (displayLogs.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("无匹配日志", color = apple.secondaryText)
                            }
                        }
                    }
                    items(displayLogs) { item ->
                        LogEntryRow(entry = item)
                        AppleHairlineDivider(startIndent = 16.dp)
                    }
                }
            }
        }
    }
    if (showCleanup) {
        LogCleanupDialog(accountId, accountName, onClear, onDismiss = { showCleanup = false })
    }
}

@Composable
internal fun LogCleanupDialog(
    accountId: String,
    accountName: String,
    onClear: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var clearing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val target = accountName.ifBlank { "当前账号" }
    AppleAlertDialog(
        onDismissRequest = { if (!clearing) onDismiss() },
        title = { Text("日志清理") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App 自动清理 $RUN_LOG_RETENTION_DAYS 天前的运行日志。")
                Text(
                    if (accountId.isBlank()) "选择账号后可清空日志。"
                    else "立即清空会删除「$target」的服务端运行日志。",
                )
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                enabled = !clearing && accountId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    clearing = true
                    error = ""
                    scope.launch {
                        try {
                            onClear(accountId)
                            onDismiss()
                        } catch (e: ApiException) {
                            error = e.message
                        } finally {
                            clearing = false
                        }
                    }
                },
            ) { Text(if (clearing) "正在清理…" else "立即清空") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !clearing) { Text("取消") } },
    )
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
                .retainSheetHeight()
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
                loading && selectedOffer == null -> Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
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
                    pending = purchasing || loading,
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

@Composable
internal fun RanchCareerStatistics(career: RanchCareer, embedded: Boolean = false) {
    val apple = LocalAppleColors.current
    val items = listOf(
        Triple("累计产物", formatNumber(career.harvest), Icons.Rounded.Inventory2 to apple.orange),
        Triple("累计成熟", formatNumber(career.mature), Icons.Rounded.Pets to apple.emerald),
        Triple("收取好友产物", formatNumber(career.steal), Icons.Rounded.ShoppingBag to apple.blue),
        Triple("收偷比", career.ratioText, Icons.Rounded.BarChart to apple.indigo),
    )
    if (!embedded) {
        AppleSectionHeader(title = "牧场生涯")
        Spacer(modifier = Modifier.height(4.dp))
    }
    AppleGroupedCard(contentPadding = PaddingValues(if (embedded) 14.dp else 12.dp)) {
        if (embedded) {
            Text(
                text = "牧场生涯",
                style = MaterialTheme.typography.titleSmall,
                color = apple.primaryText,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { row ->
                EqualHeightRow(spacing = 8.dp) {
                    row.forEach { (label, value, badge) ->
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(AppleRadius.tile),
                            color = apple.elevated,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = badge.first,
                                        contentDescription = null,
                                        tint = badge.second,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = apple.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = apple.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    repeat(2 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun OverviewStatistics(title: String, values: List<Pair<String, String>>, columns: Int = 4) {
    val apple = LocalAppleColors.current
    AppleSectionHeader(title = title)
    Spacer(modifier = Modifier.height(4.dp))
    AppleGroupedCard(contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            values.chunked(columns).forEach { row ->
                EqualHeightRow(spacing = 8.dp) {
                    row.forEach { (label, value) ->
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
                                    text = value,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = apple.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
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
    onRefillCans: () -> Unit,
    onOpenAllLogs: (RunLogScope) -> Unit,
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
            item {
                Spacer(modifier = Modifier.height(12.dp))
                RunLogsPreview(session.logs, RunLogScope.RANCH, onOpenAllLogs)
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
            item {
                Spacer(modifier = Modifier.height(12.dp))
                RunLogsPreview(session.logs, RunLogScope.RANCH, onOpenAllLogs)
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

    val ranchAccent = spaceAccent(GameSpace.RANCH)
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
                                color = ranchAccent.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "牧场 Lv.${ranch?.level ?: 0}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = ranchAccent,
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
                            AppleStatusPill("饲养 ${allAnimals.size} / $totalCapacity", ranchAccent, showDot = false)
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
                AppleProgressBar(
                    progress = { expFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    color = ranchAccent,
                    trackColor = apple.track,
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
                    QuickEntryItem("礼包商城", Icons.Rounded.CardGiftcard, apple.indigo) {
                        onOpenDiscover(1, 2)
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
                                        exactValue = formatNumber(cur.count),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AppleSectionHeader(title = "牧场库存", actionLabel = "补充罐头", onAction = onRefillCans)
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

            ranch.career?.let { career ->
                item {
                    RanchCareerStatistics(career)
                }
            }
            status?.ranchOperations?.takeIf { it.isNotEmpty() }?.let { counts ->
                item {
                    val labels = mapOf("harvest" to "收获", "produce" to "生产", "helpFarming" to "帮忙清理", "steal" to "偷取")
                    OverviewStatistics("今日收益统计", counts.map { (key, count) -> labels.getValue(key) to count.toString() })
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
                                    AppleProgressBar(
                                        progress = { fraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp),
                                        color = ranchAccent,
                                        trackColor = apple.track,
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

        item {
            RunLogsPreview(session.logs, RunLogScope.RANCH, onOpenAllLogs)
        }
    }
}
