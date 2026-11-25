package com.qqfarm.mobile.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.qqfarm.mobile.data.model.MapleMarketFriendQuoteRow
import com.qqfarm.mobile.data.model.MapleMarketFriendQuotesState
import com.qqfarm.mobile.data.model.MapleMarketGoodsItem
import com.qqfarm.mobile.data.model.MapleMarketPurchaseLogItem
import com.qqfarm.mobile.data.model.MapleMarketQuote
import com.qqfarm.mobile.data.model.MapleMarketState
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleAlertDialog
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleProgressBar
import com.qqfarm.mobile.ui.components.AppleQuantityStepper
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.components.formatNumber
import com.qqfarm.mobile.ui.components.formatTimestamp
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val MAPLE_DAILY_QUOTA_CAP = 8000L

private enum class MapleCompareFilter(val label: String) {
    ALL("全部"),
    SELLABLE("可售"),
    FULL("已满"),
    UNBOOSTED("未加成"),
    UNAVAILABLE("不可售"),
}

private data class MapleSellTarget(
    val gid: Long,
    val shopTitle: String,
    val quote: MapleMarketQuote,
    val saleMax: Int,
    val saleLimitHint: String,
)

private data class MapleGoodsActionState(
    val enabled: Boolean,
    val label: String,
    val reason: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapleMarketDetailContent(
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

    var loading by remember(accountId) { mutableStateOf(false) }
    var busy by remember(accountId) { mutableStateOf(false) }
    var state by remember(accountId) { mutableStateOf<MapleMarketState?>(null) }
    var errorText by remember(accountId) { mutableStateOf<String?>(null) }
    var actionLocked by remember(accountId) { mutableStateOf(false) }
    var actionLockError by remember(accountId) { mutableStateOf<String?>(null) }
    var lastShareRateBp by remember(accountId) { mutableIntStateOf(0) }
    var selectedTab by remember(accountId) { mutableIntStateOf(0) }

    var friendQuotes by remember(accountId) { mutableStateOf<MapleMarketFriendQuotesState?>(null) }
    var compareStarting by remember(accountId) { mutableStateOf(false) }
    var compareLoading by remember(accountId) { mutableStateOf(false) }
    var compareError by remember(accountId) { mutableStateOf<String?>(null) }
    var compareFilter by remember(accountId) { mutableStateOf(MapleCompareFilter.ALL) }

    var showPurchaseLog by remember(accountId) { mutableStateOf(false) }
    var purchaseLogLoading by remember(accountId) { mutableStateOf(false) }
    var purchaseLogError by remember(accountId) { mutableStateOf<String?>(null) }
    var purchaseLogs by remember(accountId) { mutableStateOf<List<MapleMarketPurchaseLogItem>>(emptyList()) }

    var sellTarget by remember(accountId) { mutableStateOf<MapleSellTarget?>(null) }
    var exchangeTarget by remember(accountId) { mutableStateOf<MapleMarketGoodsItem?>(null) }

    var loadJob by remember(accountId) { mutableStateOf<Job?>(null) }
    var compareJob by remember(accountId) { mutableStateOf<Job?>(null) }

    fun lockActionsOnError(message: String) {
        actionLocked = true
        actionLockError = message
        sellTarget = null
        exchangeTarget = null
        state = state?.let { cur ->
            cur.copy(
                canClaimSeed = false,
                canShare = false,
                canClaimCommission = false,
                quote = cur.quote?.copy(canSell = false),
                goods = cur.goods.map { it.copy(exchangeable = false) },
            )
        }
        friendQuotes = friendQuotes?.let { cur ->
            cur.copy(
                rows = cur.rows.map { row ->
                    row.copy(quote = row.quote?.copy(canSell = false))
                },
            )
        }
        onMessage(message)
    }

    fun loadMarket() {
        loadJob?.cancel()
        loadJob = scope.launch {
            loading = true
            errorText = null
            runCatching { apiClient.getMapleMarketState(accountId) }
                .onSuccess { res ->
                    state = res
                    actionLocked = false
                    actionLockError = null
                    if (res.quote != null && res.quote.priceRateBp > 0) {
                        lastShareRateBp = res.quote.priceRateBp
                    } else if (!res.shared) {
                        lastShareRateBp = 0
                    }
                    onRules(res.rules)
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    errorText = err.message ?: "读取以薯换福活动失败"
                }
            loading = false
        }
    }

    fun loadFriendQuotes(startScan: Boolean = false) {
        if (compareLoading || compareStarting) return
        compareJob?.cancel()
        compareJob = scope.launch {
            compareLoading = true
            compareStarting = startScan
            compareError = null
            if (startScan) {
                friendQuotes = null
            }
            runCatching {
                if (startScan) {
                    apiClient.refreshMapleMarketFriendQuotes(accountId)
                } else {
                    apiClient.getMapleMarketFriendQuotes(accountId)
                }
            }.onSuccess { res ->
                friendQuotes = res
                if (res.error.isNotBlank()) {
                    compareError = res.error
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                compareError = err.message ?: "读取好友报价失败"
            }
            compareStarting = false
            compareLoading = false
        }
    }

    fun openPurchaseLog() {
        showPurchaseLog = true
        if (purchaseLogLoading) return
        scope.launch {
            purchaseLogLoading = true
            purchaseLogError = null
            runCatching { apiClient.getMapleMarketPurchaseLog(accountId) }
                .onSuccess { records ->
                    purchaseLogs = records
                    state = state?.copy(purchaseLogUnread = false)
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    purchaseLogError = err.message ?: "读取收购记录失败"
                }
            purchaseLogLoading = false
        }
    }

    fun runShopAction(block: suspend () -> String) {
        if (busy || actionLocked) return
        scope.launch {
            busy = true
            runCatching { block() }
                .onSuccess { msg ->
                    onMessage(msg)
                    loadMarket()
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    lockActionsOnError(err.message ?: "操作失败，请刷新确认最新状态")
                }
            busy = false
        }
    }

    fun openSellDialogForRow(row: MapleMarketFriendQuoteRow) {
        val current = state ?: return
        val quote = row.quote ?: return
        if (actionLocked) {
            onMessage(actionLockError ?: "状态待核对，请先刷新")
            return
        }
        if (current.sellableCropStock <= 0L) {
            onMessage("当前暂无可出售的蜜薯")
            return
        }
        if (!quote.canSell || (row.gid > 0L && quote.remainingQuota <= 0L)) {
            onMessage("当前小铺暂不可出售")
            return
        }
        val maxRaw = if (row.gid == 0L) {
            current.sellableCropStock
        } else {
            minOf(current.sellableCropStock, quote.remainingQuota)
        }
        val saleMax = maxRaw.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        if (saleMax <= 0) {
            onMessage("当前无可售数量")
            return
        }
        val limitHint = if (row.gid == 0L || current.sellableCropStock <= quote.remainingQuota) {
            "受可卖蜜薯限制"
        } else {
            "受好友剩余额度限制"
        }
        sellTarget = MapleSellTarget(
            gid = row.gid,
            shopTitle = if (row.gid == 0L) "出售给自家小铺" else "出售给 · ${row.name}",
            quote = quote,
            saleMax = saleMax,
            saleLimitHint = limitHint,
        )
    }

    LaunchedEffect(accountId, refreshTrigger) {
        loadMarket()
        if (selectedTab == 2 || friendQuotes != null) {
            loadFriendQuotes(startScan = false)
        }
    }

    LaunchedEffect(accountId, selectedTab, state?.phase) {
        if (selectedTab == 2 && state?.phase == "active" && friendQuotes == null && !compareLoading) {
            loadFriendQuotes(startScan = false)
        }
    }

    val isCompareRunning = friendQuotes?.status == "running"
    LaunchedEffect(accountId, isCompareRunning, state?.phase) {
        if (state?.phase != "active" || !isCompareRunning) return@LaunchedEffect
        while (isActive) {
            delay(700L)
            val latest = runCatching { apiClient.getMapleMarketFriendQuotes(accountId) }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    compareError = err.message ?: "读取好友报价失败"
                }
                .getOrNull() ?: break
            friendQuotes = latest
            if (latest.error.isNotBlank()) {
                compareError = latest.error
            }
            if (latest.status != "running") break
        }
    }

    LaunchedEffect(state, clockTickMs) {
        val s = state
        if (s == null) {
            onSubtitle(null)
        } else {
            val nowMs = if (s.serverTimeMs > 0L) {
                s.serverTimeMs + (clockTickMs - s.receivedAtElapsedMs).coerceAtLeast(0L)
            } else {
                System.currentTimeMillis()
            }
            val remSec = ((s.endTimeMs - nowMs) / 1000L).coerceAtLeast(0L)
            val subtitle = when (s.phase) {
                "upcoming" -> "未开始"
                "settlement" -> if (remSec > 0L) "兑换期 · 剩余 ${formatDurationSec(remSec)}" else "兑换期"
                "active" -> if (remSec > 0L) "进行中 · 剩余 ${formatDurationSec(remSec)}" else "进行中"
                else -> "已结束"
            }
            onSubtitle(subtitle)
        }
    }

    val current = state
    if (current == null && !loading && errorText != null) {
        AppleEmptyState(
            icon = Icons.Rounded.Storefront,
            title = "加载失败",
            subtitle = errorText.orEmpty(),
            actionLabel = "重试",
            onAction = { loadMarket() },
        )
        return
    }

    val shanghaiTz = remember { TimeZone.getTimeZone("Asia/Shanghai") }
    val periodFmt = remember(shanghaiTz) {
        SimpleDateFormat("M月d日", Locale.CHINA).apply { timeZone = shanghaiTz }
    }
    val nowMs = current?.let {
        if (it.serverTimeMs > 0L) {
            it.serverTimeMs + (clockTickMs - it.receivedAtElapsedMs).coerceAtLeast(0L)
        } else {
            System.currentTimeMillis()
        }
    } ?: System.currentTimeMillis()

    PullToRefreshBox(
        isRefreshing = loading && current != null,
        onRefresh = {
            loadMarket()
            if (selectedTab == 2 && state?.phase == "active") {
                loadFriendQuotes(startScan = false)
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (current == null) {
            AppleLoadingState()
            return@PullToRefreshBox
        }

        Column(modifier = Modifier.fillMaxSize()) {
            MapleMarketOverviewCard(
                state = current,
                nowMs = nowMs,
                periodFmt = periodFmt,
                onOpenPurchaseLog = { openPurchaseLog() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )

            if (actionLocked && !actionLockError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(AppleRadius.tile),
                    color = apple.red.copy(alpha = 0.09f),
                    border = BorderStroke(0.5.dp, apple.red.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = apple.red,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = actionLockError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.red,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                loadMarket()
                                if (selectedTab == 2 && current.phase == "active") {
                                    loadFriendQuotes(startScan = false)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text("刷新同步")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                AppleSegmentedControl(
                    options = listOf("红薯小铺", "兑换商店", "好友比价"),
                    selectedIndex = selectedTab,
                    onSelect = { if (!busy) selectedTab = it },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .swipeToSwitchTabs(selectedTab, 3, !busy) { if (!busy) selectedTab = it },
            ) {
                when (selectedTab) {
                    0 -> MapleShopTab(
                        state = current,
                        lastShareRateBp = lastShareRateBp,
                        busy = busy,
                        actionLocked = actionLocked,
                        onClaimSeed = {
                            runShopAction {
                                val res = apiClient.claimMapleMarketSeed(accountId)
                                if (res.status == "confirmed") {
                                    "已领取${res.seedName} ×${formatNumber(res.seedCount)}"
                                } else {
                                    res.message.ifBlank { "今日红薯种子已领取" }
                                }
                            }
                        },
                        onShareBoost = {
                            runShopAction {
                                val res = apiClient.shareBoostMapleMarket(accountId)
                                if (res.status == "confirmed") {
                                    if (res.priceRateBp > 0) {
                                        lastShareRateBp = res.priceRateBp
                                    }
                                    val seedNote = if (res.seedCount > 0L) {
                                        "，额外获得${res.seedName} ×${formatNumber(res.seedCount)}"
                                    } else {
                                        ""
                                    }
                                    "已触发分享暴击 +${formatNumber(res.boost)}，今日收购价 ${formatNumber(res.finalPrice)} 福气币$seedNote"
                                } else {
                                    res.message.ifBlank { "今日分享暴击已触发" }
                                }
                            }
                        },
                        onClaimCommission = {
                            runShopAction {
                                val res = apiClient.claimMapleMarketCommission(accountId)
                                if (res.status == "confirmed") {
                                    "已领取小铺提成 ${formatNumber(res.claimedCoin)} 福气币"
                                } else {
                                    res.message.ifBlank { "当前没有可领取的小铺提成" }
                                }
                            }
                        },
                        onOpenPurchaseLog = { openPurchaseLog() },
                        onOpenSelfSell = {
                            val quote = current.quote ?: return@MapleShopTab
                            openSellDialogForRow(
                                MapleMarketFriendQuoteRow(
                                    gid = 0L,
                                    name = quote.ownerName.ifBlank { "自家小铺" },
                                    quote = quote,
                                )
                            )
                        },
                        onJumpCompare = { selectedTab = 2 },
                    )

                    1 -> MapleExchangeTab(
                        state = current,
                        busy = busy,
                        actionLocked = actionLocked,
                        onSelectGoods = { exchangeTarget = it },
                    )

                    2 -> MapleCompareTab(
                        state = current,
                        comparison = friendQuotes,
                        starting = compareStarting,
                        loading = compareLoading,
                        errorText = compareError,
                        filter = compareFilter,
                        busy = busy,
                        actionLocked = actionLocked,
                        hasOpenDialog = sellTarget != null,
                        onFilterChange = { compareFilter = it },
                        onRefreshQuotes = { loadFriendQuotes(startScan = true) },
                        onSelectRow = { openSellDialogForRow(it) },
                    )
                }
            }
        }
    }

    if (showPurchaseLog) {
        MaplePurchaseLogDialog(
            loading = purchaseLogLoading,
            errorText = purchaseLogError,
            records = purchaseLogs,
            onRetry = { openPurchaseLog() },
            onDismiss = { showPurchaseLog = false },
        )
    }

    sellTarget?.let { target ->
        MapleSellDialog(
            target = target,
            busy = busy,
            actionLocked = actionLocked,
            onDismiss = { if (!busy) sellTarget = null },
            onConfirm = { count ->
                if (busy || actionLocked) return@MapleSellDialog
                scope.launch {
                    busy = true
                    val result = runCatching {
                        if (target.gid == 0L) {
                            apiClient.sellMapleMarketCrop(accountId, count.toLong(), target.quote.unitPrice)
                        } else {
                            apiClient.sellMapleMarketToFriend(accountId, target.gid, count.toLong(), target.quote.unitPrice)
                        }
                    }
                    result.onSuccess { res ->
                        sellTarget = null
                        onMessage("已卖出 ${formatNumber(res.sold)} 个蜜薯，获得 ${formatNumber(res.income)} 福气币")
                        res.remainingQuota?.let { nextQuota ->
                            friendQuotes = friendQuotes?.let { cur ->
                                cur.copy(
                                    rows = cur.rows.map { row ->
                                        if (row.gid == target.gid && row.quote != null) {
                                            row.copy(quote = row.quote.copy(remainingQuota = nextQuota))
                                        } else row
                                    }
                                )
                            }
                        }
                        loadMarket()
                        if (target.gid > 0L) {
                            loadFriendQuotes(startScan = false)
                        }
                    }.onFailure { err ->
                        if (err is CancellationException) throw err
                        lockActionsOnError(err.message ?: "出售失败，请刷新确认最新报价")
                    }
                    busy = false
                }
            },
        )
    }

    exchangeTarget?.let { offer ->
        MapleExchangeDialog(
            offer = offer,
            coinBalance = current?.coinBalance ?: 0L,
            busy = busy,
            actionLocked = actionLocked,
            onDismiss = { if (!busy) exchangeTarget = null },
            onConfirm = { count ->
                if (busy || actionLocked) return@MapleExchangeDialog
                scope.launch {
                    busy = true
                    runCatching {
                        apiClient.exchangeMapleMarketGoods(accountId, offer.id, count, offer.price)
                    }.onSuccess { res ->
                        exchangeTarget = null
                        onMessage("已兑换 ${offer.name} ×${res.count}，扣除 ${formatNumber(res.cost)} 福气币")
                        loadMarket()
                    }.onFailure { err ->
                        if (err is CancellationException) throw err
                        lockActionsOnError(err.message ?: "兑换失败，请刷新确认商品信息")
                    }
                    busy = false
                }
            },
        )
    }
}

@Composable
private fun MapleMarketOverviewCard(
    state: MapleMarketState,
    nowMs: Long,
    periodFmt: SimpleDateFormat,
    onOpenPurchaseLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    val (phaseLabel, phaseColor) = when (state.phase) {
        "upcoming" -> "未开始" to apple.orange
        "settlement" -> "兑换期" to apple.orange
        "active" -> {
            val leftDays = kotlin.math.ceil((state.endTimeMs - nowMs).toDouble() / 86_400_000.0).toLong()
            val text = if (state.endTimeMs > 0L) {
                if (leftDays <= 0L) "今天结束" else "还剩 $leftDays 天"
            } else "进行中"
            text to apple.green
        }
        else -> "已结束" to apple.secondaryText
    }
    val periodText = if (state.startTimeMs > 0L && state.endTimeMs > 0L) {
        "${periodFmt.format(Date(state.startTimeMs))} – ${periodFmt.format(Date(state.endTimeMs))}"
    } else {
        "以薯换福"
    }

    AppleGroupedCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = apple.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AppleStatusPill(text = phaseLabel, color = phaseColor)
                }
                if (state.purchaseLogUnread) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenPurchaseLog),
                        shape = CircleShape,
                        color = apple.red.copy(alpha = 0.14f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(apple.red),
                            )
                            Text(
                                text = "新收购记录",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = apple.red,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapleWalletTile(
                    itemId = state.coinItemId,
                    label = "福气币",
                    value = formatCompactCount(state.coinBalance),
                    subValue = "活动货币",
                    fallbackIcon = Icons.Rounded.Paid,
                    accentColor = apple.orange,
                    modifier = Modifier.weight(1f),
                )
                MapleWalletTile(
                    itemId = state.seedItemId,
                    label = "红薯种子",
                    value = formatCompactCount(state.seedStock),
                    subValue = if (state.seedClaimed) "今日已领" else "可种植",
                    fallbackIcon = Icons.Rounded.Grass,
                    accentColor = apple.emerald,
                    modifier = Modifier.weight(1f),
                )
                MapleWalletTile(
                    itemId = state.cropItemId,
                    label = "蜜薯库存",
                    value = formatCompactCount(state.cropStock),
                    subValue = "可售 ${formatCompactCount(state.sellableCropStock)}",
                    fallbackIcon = Icons.Rounded.Eco,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MapleWalletTile(
    itemId: Int,
    label: String,
    value: String,
    subValue: String,
    fallbackIcon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppleRadius.tile),
        color = apple.elevated,
        border = BorderStroke(0.5.dp, apple.separator),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                RemoteAssetImage(
                    url = mapleItemImageUrl(itemId),
                    contentDescription = label,
                    fallbackIcon = fallbackIcon,
                    fallbackTint = accentColor,
                    modifier = Modifier.size(16.dp),
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
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = apple.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MapleShopTab(
    state: MapleMarketState,
    lastShareRateBp: Int,
    busy: Boolean,
    actionLocked: Boolean,
    onClaimSeed: () -> Unit,
    onShareBoost: () -> Unit,
    onClaimCommission: () -> Unit,
    onOpenPurchaseLog: () -> Unit,
    onOpenSelfSell: () -> Unit,
    onJumpCompare: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val quote = state.quote

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Storefront,
                                contentDescription = null,
                                tint = apple.orange,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = quote?.ownerName?.takeIf { it.isNotBlank() }?.let { "${it}的红薯小铺" }
                                    ?: "我的红薯小铺",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = apple.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (quote != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                quote.tier?.let { tier ->
                                    AppleStatusPill(
                                        text = "${tier}档",
                                        color = when (tier) {
                                            "超高价" -> apple.red
                                            "高价" -> apple.orange
                                            else -> apple.emerald
                                        },
                                        showDot = false,
                                    )
                                }
                                AppleStatusPill(
                                    text = if (quote.complete) "已暴击" else "未暴击",
                                    color = if (quote.complete) apple.green else apple.secondaryText,
                                )
                            }
                        }
                    }

                    if (quote != null) {
                        Surface(
                            shape = RoundedCornerShape(AppleRadius.tile),
                            color = apple.elevated,
                            border = BorderStroke(0.5.dp, apple.separator),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(
                                            text = "今日收购价",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = apple.secondaryText,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = formatNumber(quote.unitPrice),
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFeatureSettings = "tnum",
                                                ),
                                                color = apple.orange,
                                            )
                                            Text(
                                                text = "福气币 / 个",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = apple.secondaryText,
                                                modifier = Modifier.padding(bottom = 3.dp),
                                            )
                                        }
                                    }
                                    val effectiveRateBp = if (quote.priceRateBp > 0) quote.priceRateBp else lastShareRateBp
                                    if (effectiveRateBp > 0) {
                                        AppleStatusPill(
                                            text = "暴击率 ${formatBasisPoints(effectiveRateBp)}",
                                            color = apple.orange,
                                            showDot = false,
                                        )
                                    }
                                }
                                Text(
                                    text = buildString {
                                        append("基础价 ")
                                        append(formatNumber(quote.basePrice))
                                        append(" + ")
                                        if (quote.boost > 0L) {
                                            append("分享暴击 ")
                                            append(formatNumber(quote.boost))
                                        } else {
                                            append("未分享暴击 0")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = apple.secondaryText,
                                )
                            }
                        }

                        val quotaFraction = (quote.remainingQuota.toFloat() / MAPLE_DAILY_QUOTA_CAP.toFloat())
                            .coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "好友剩余可售额度",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                )
                                Text(
                                    text = "${formatNumber(quote.remainingQuota)} / ${formatNumber(MAPLE_DAILY_QUOTA_CAP)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = apple.primaryText,
                                )
                            }
                            AppleProgressBar(
                                progress = { quotaFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp),
                                color = if (quote.remainingQuota > 0L) apple.orange else apple.secondaryText,
                                trackColor = apple.track,
                            )
                        }

                        if (state.phase == "active") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = apple.secondaryText,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "每日 24:00 按自家当日最终价自动结算剩余蜜薯",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (state.phase == "settlement") {
                                "收购已结束，可前往兑换商店使用福气币"
                            } else {
                                "当前暂无收购报价"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                        )
                    }

                    if (state.phase == "active") {
                        val canSelfSell = !busy && !actionLocked && quote?.canSell == true && state.sellableCropStock > 0L
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = onOpenSelfSell,
                                enabled = canSelfSell,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sell,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (state.sellableCropStock > 0L) {
                                        "卖给自家 (${formatCompactCount(state.sellableCropStock)})"
                                    } else {
                                        "暂无可售蜜薯"
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = onJumpCompare,
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("好友比价")
                            }
                        }
                    }
                }
            }
        }

        item {
            AppleSectionHeader(
                title = "小铺事项",
                actionLabel = if (state.purchaseLogUnread) "收购记录 · 新" else "收购记录",
                onAction = onOpenPurchaseLog,
            )
        }

        item {
            AppleGroupedCard {
                MapleTaskRow(
                    icon = Icons.Rounded.Grass,
                    iconTint = apple.emerald,
                    title = "每日红薯种子",
                    subtitle = "每天可领一次，种出蜜薯后出售换福气币",
                    action = {
                        when {
                            state.seedClaimed -> AppleStatusPill(text = "今日已领", color = apple.green)
                            !actionLocked && state.canClaimSeed -> Button(
                                onClick = onClaimSeed,
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) { Text("领取") }
                            else -> AppleStatusPill(
                                text = "暂不可领",
                                color = apple.secondaryText,
                                showDot = false,
                            )
                        }
                    },
                )
                AppleHairlineDivider(startIndent = 56.dp)
                val shareSubtitle = when {
                    !state.shared -> "每日触发一次当日收购价暴击，并额外获得红薯种子"
                    (quote?.boost ?: 0L) > 0L -> "今日收购价已暴击 +${formatNumber(quote?.boost ?: 0L)} 福气币，次日 0 点重置"
                    else -> "今日已触发分享暴击，次日 0 点重置"
                }
                MapleTaskRow(
                    icon = Icons.Rounded.AutoAwesome,
                    iconTint = apple.orange,
                    title = "每日分享暴击",
                    subtitle = shareSubtitle,
                    action = {
                        when {
                            state.shared -> AppleStatusPill(text = "今日已分享", color = apple.green)
                            !actionLocked && state.canShare -> Button(
                                onClick = onShareBoost,
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) { Text("分享暴击") }
                            else -> AppleStatusPill(
                                text = "暂不可分享",
                                color = apple.secondaryText,
                                showDot = false,
                            )
                        }
                    },
                )
                AppleHairlineDivider(startIndent = 56.dp)
                val commissionTitle = if (state.pendingCommissionCoin > 0L) {
                    "小铺提成 (+${formatNumber(state.pendingCommissionCoin)})"
                } else {
                    "小铺提成"
                }
                val commissionSubtitle = if (state.pendingCommissionCount > 0) {
                    "${state.pendingCommissionCount} 笔好友成交待领 · 按成交额 2% 分成"
                } else {
                    "好友通过你的分享出售蜜薯，你获得 2% 福气币分成"
                }
                MapleTaskRow(
                    icon = Icons.Rounded.Paid,
                    iconTint = apple.blue,
                    title = commissionTitle,
                    subtitle = commissionSubtitle,
                    action = {
                        if (!actionLocked && state.canClaimCommission) {
                            Button(
                                onClick = onClaimCommission,
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) { Text("领取") }
                        } else {
                            AppleStatusPill(
                                text = "暂无待领",
                                color = apple.secondaryText,
                                showDot = false,
                            )
                        }
                    },
                )
                if (state.phase == "active" || state.phase == "settlement") {
                    AppleHairlineDivider(startIndent = 56.dp)
                    MapleTaskRow(
                        icon = Icons.Rounded.History,
                        iconTint = apple.indigo,
                        title = "小铺收购记录",
                        subtitle = if (state.purchaseLogUnread) "有新的好友成交记录待查看" else "查看好友在本铺卖薯明细与提成",
                        onClick = onOpenPurchaseLog,
                        action = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (state.purchaseLogUnread) {
                                    AppleStatusPill(text = "未读", color = apple.red)
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "查看收购记录",
                                    tint = apple.secondaryText,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MapleTaskRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    action: @Composable () -> Unit,
) {
    val apple = LocalAppleColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = apple.primaryText,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = apple.secondaryText,
            )
        }
        action()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapleExchangeTab(
    state: MapleMarketState,
    busy: Boolean,
    actionLocked: Boolean,
    onSelectGoods: (MapleMarketGoodsItem) -> Unit,
) {
    val apple = LocalAppleColors.current
    if (state.goods.isEmpty()) {
        AppleEmptyState(
            icon = Icons.Rounded.CardGiftcard,
            title = "暂无可兑换商品",
            subtitle = "请下拉刷新获取最新兑换商店目录",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AppleSectionHeader(
                title = "兑换商品 (${state.goods.size}) · 持有 ${formatNumber(state.coinBalance)} 福气币",
            )
        }
        items(state.goods, key = { it.id }) { goods ->
            val action = evaluateGoodsAction(state, goods, actionLocked)
            val soldOut = goods.remaining == 0
            AppleGroupedCard(
                modifier = Modifier.alpha(if (soldOut) 0.65f else 1f),
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val firstReward = goods.items.firstOrNull()
                        RemoteAssetImage(
                            url = firstReward?.image?.ifBlank { mapleItemImageUrl(firstReward.id) }
                                ?: mapleItemImageUrl(state.coinItemId),
                            contentDescription = goods.name,
                            fallbackIcon = Icons.Rounded.ShoppingBag,
                            fallbackTint = apple.orange,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(apple.track),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = goods.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = apple.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val limitText = if (goods.remaining != null && goods.limitTimes != null) {
                                "${mapleLimitLabel(goods.limitType)} · 剩 ${goods.remaining}/${goods.limitTimes}"
                            } else {
                                "不限兑换次数"
                            }
                            Text(
                                text = limitText,
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                            if (action.reason.isNotBlank()) {
                                Text(
                                    text = action.reason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (soldOut) apple.secondaryText else apple.red,
                                )
                            }
                        }
                        Button(
                            onClick = { onSelectGoods(goods) },
                            enabled = !busy && action.enabled,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(action.label)
                        }
                    }

                    if (goods.items.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            goods.items.forEach { reward ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = apple.elevated,
                                    border = BorderStroke(0.5.dp, apple.separator),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        RemoteAssetImage(
                                            url = reward.image.ifBlank { mapleItemImageUrl(reward.id) },
                                            contentDescription = reward.name,
                                            fallbackIcon = Icons.Rounded.CardGiftcard,
                                            fallbackTint = apple.emerald,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "${reward.name.ifBlank { "#${reward.id}" }} ×${formatCompactCount(reward.count)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = apple.primaryText,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        RemoteAssetImage(
                            url = mapleItemImageUrl(state.coinItemId),
                            contentDescription = "福气币",
                            fallbackIcon = Icons.Rounded.Paid,
                            fallbackTint = apple.orange,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "${formatNumber(goods.price)} 福气币",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = apple.orange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapleCompareTab(
    state: MapleMarketState,
    comparison: MapleMarketFriendQuotesState?,
    starting: Boolean,
    loading: Boolean,
    errorText: String?,
    filter: MapleCompareFilter,
    busy: Boolean,
    actionLocked: Boolean,
    hasOpenDialog: Boolean,
    onFilterChange: (MapleCompareFilter) -> Unit,
    onRefreshQuotes: () -> Unit,
    onSelectRow: (MapleMarketFriendQuoteRow) -> Unit,
) {
    val apple = LocalAppleColors.current
    if (state.phase != "active") {
        AppleEmptyState(
            icon = Icons.Rounded.Schedule,
            title = "当前阶段暂不支持好友比价",
            subtitle = "仅活动进行期间可查询并出售给好友红薯小铺",
        )
        return
    }

    val running = starting || comparison?.status == "running"
    val collator = remember { Collator.getInstance(Locale.CHINA) }

    val rankedRows = remember(comparison, state.quote) {
        if (comparison?.status != "ready") {
            emptyList()
        } else {
            val combined = buildList {
                addAll(comparison.rows)
                state.quote?.let { own ->
                    add(
                        MapleMarketFriendQuoteRow(
                            gid = 0L,
                            name = own.ownerName.ifBlank { "自家" },
                            quote = own,
                            failed = false,
                            reused = false,
                        )
                    )
                }
            }
            combined.sortedWith { a, b ->
                val priceCmp = (b.quote?.unitPrice ?: -1L).compareTo(a.quote?.unitPrice ?: -1L)
                if (priceCmp != 0) return@sortedWith priceCmp
                val compCmp = (if (b.quote?.complete == true) 1 else 0).compareTo(if (a.quote?.complete == true) 1 else 0)
                if (compCmp != 0) return@sortedWith compCmp
                val aQuota = if (a.gid == 0L) Long.MAX_VALUE else (a.quote?.remainingQuota ?: -1L)
                val bQuota = if (b.gid == 0L) Long.MAX_VALUE else (b.quote?.remainingQuota ?: -1L)
                val quotaCmp = bQuota.compareTo(aQuota)
                if (quotaCmp != 0) return@sortedWith quotaCmp
                collator.compare(a.name, b.name)
            }
        }
    }

    val bestOffer = remember(rankedRows, state.quote) {
        val ownPrice = state.quote?.unitPrice ?: 0L
        rankedRows.firstOrNull { row ->
            row.gid > 0L &&
                row.quote?.canSell == true &&
                row.quote.remainingQuota > 0L &&
                row.quote.unitPrice > ownPrice
        }
    }

    val filterCounts = remember(rankedRows) {
        mapOf(
            MapleCompareFilter.ALL to rankedRows.size,
            MapleCompareFilter.SELLABLE to rankedRows.count {
                it.quote?.canSell == true && (it.gid == 0L || it.quote.remainingQuota > 0L)
            },
            MapleCompareFilter.FULL to rankedRows.count {
                it.gid > 0L && it.quote != null && it.quote.remainingQuota <= 0L
            },
            MapleCompareFilter.UNBOOSTED to rankedRows.count {
                it.quote != null && !it.quote.complete
            },
            MapleCompareFilter.UNAVAILABLE to rankedRows.count {
                it.quote == null || !it.quote.canSell || (it.gid > 0L && it.quote.remainingQuota <= 0L)
            },
        )
    }

    val visibleRows = remember(rankedRows, filter) {
        when (filter) {
            MapleCompareFilter.ALL -> rankedRows
            MapleCompareFilter.SELLABLE -> rankedRows.filter {
                it.quote?.canSell == true && (it.gid == 0L || it.quote.remainingQuota > 0L)
            }
            MapleCompareFilter.FULL -> rankedRows.filter {
                it.gid > 0L && it.quote != null && it.quote.remainingQuota <= 0L
            }
            MapleCompareFilter.UNBOOSTED -> rankedRows.filter {
                it.quote != null && !it.quote.complete
            }
            MapleCompareFilter.UNAVAILABLE -> rankedRows.filter {
                it.quote == null || !it.quote.canSell || (it.gid > 0L && it.quote.remainingQuota <= 0L)
            }
        }
    }

    val incompleteCount = remember(comparison) {
        if (comparison?.status == "ready") {
            comparison.rows.count { it.quote?.complete != true }
        } else 0
    }
    val queryBtnText = when {
        running -> "正在获取报价"
        comparison?.status == "ready" && comparison.total > 0 -> {
            if (incompleteCount > 0) "补查未完整 ($incompleteCount)" else "重新核对好友"
        }
        else -> "查询全部好友"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "好友铺子比价",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = apple.primaryText,
                            )
                            Text(
                                text = "按实际收购价排序，同日再查自动复用已暴击报价",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRefreshQuotes,
                            enabled = !loading && !running && !busy && !hasOpenDialog,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            if (running) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(queryBtnText)
                        }
                    }

                    if (running) {
                        val total = comparison?.total ?: 0
                        val checked = comparison?.checked ?: 0
                        val reused = comparison?.reused ?: 0
                        val queried = comparison?.queried ?: 0
                        val progressFrac = if (total > 0) {
                            (checked.toFloat() / total.toFloat()).coerceIn(0.04f, 1f)
                        } else {
                            0.08f
                        }
                        AppleProgressBar(
                            progress = { progressFrac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = apple.track,
                        )
                        val statusLine = when {
                            total <= 0 -> "正在拉取好友名单并分批获取报价…"
                            reused > 0 -> "已查 $checked / $total（复用 $reused 位当日完整报价，补查 $queried 位）"
                            else -> "已查 $checked / $total"
                        }
                        Text(
                            text = statusLine,
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = apple.secondaryText,
                        )
                    } else if (comparison?.status == "ready" && comparison.total > 0) {
                        val ownQuote = state.quote
                        if (bestOffer?.quote != null && ownQuote != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(AppleRadius.tile))
                                    .clickable(enabled = !actionLocked && !busy && state.sellableCropStock > 0L) {
                                        onSelectRow(bestOffer)
                                    },
                                shape = RoundedCornerShape(AppleRadius.tile),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    val diff = bestOffer.quote.unitPrice - ownQuote.unitPrice
                                    Text(
                                        text = "推荐卖到 ${bestOffer.name}：${formatNumber(bestOffer.quote.unitPrice)} 福气币/个（比自家高 +${formatNumber(diff)}）",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFeatureSettings = "tnum",
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        } else if (ownQuote != null) {
                            Surface(
                                shape = RoundedCornerShape(AppleRadius.tile),
                                color = apple.elevated,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Storefront,
                                        contentDescription = null,
                                        tint = apple.emerald,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "当前自家收购价最高（${formatNumber(ownQuote.unitPrice)} 福气币/个）",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        color = apple.primaryText,
                                    )
                                }
                            }
                        }

                        val metaParts = buildList {
                            if (comparison.reused > 0) add("复用 ${comparison.reused} 位当日完整报价")
                            if (comparison.queried > 0) add("本次查询 ${comparison.queried} 位")
                            if (comparison.partial) add("好友列表仅返回部分数据")
                        }
                        if (metaParts.isNotEmpty()) {
                            Text(
                                text = metaParts.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                    }
                }
            }
        }

        val effectiveError = errorText?.ifBlank { null } ?: comparison?.error?.ifBlank { null }
        if (!effectiveError.isNullOrBlank()) {
            item {
                AppleGroupedCard(
                    contentPadding = PaddingValues(12.dp),
                    containerColor = apple.red.copy(alpha = 0.08f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = apple.red,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = effectiveError,
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.red,
                        )
                    }
                }
            }
        }

        when {
            running -> Unit
            comparison == null || comparison.status == "idle" -> {
                item {
                    AppleEmptyState(
                        icon = Icons.Rounded.People,
                        title = "还没有查询好友报价",
                        subtitle = "点击右上角「查询全部好友」读取好友小铺报价，点击报价行即可直接出售蜜薯",
                    )
                }
            }
            comparison.status == "failed" && rankedRows.isEmpty() -> {
                item {
                    AppleEmptyState(
                        icon = Icons.Rounded.WarningAmber,
                        title = "好友报价获取失败",
                        subtitle = effectiveError ?: "请稍后重新查询好友报价",
                        actionLabel = "重新查询",
                        onAction = onRefreshQuotes,
                    )
                }
            }
            comparison.status == "ready" && comparison.total == 0 && rankedRows.isEmpty() -> {
                item {
                    AppleEmptyState(
                        icon = Icons.Rounded.People,
                        title = "暂无好友可查询",
                        subtitle = "当前未获取到可比价的好友小铺",
                    )
                }
            }
            comparison.status == "ready" && rankedRows.isNotEmpty() -> {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        items(MapleCompareFilter.entries, key = { it.name }) { chip ->
                            val count = filterCounts[chip] ?: 0
                            AppleFilterChip(
                                selected = filter == chip,
                                onClick = { onFilterChange(chip) },
                                label = { Text("${chip.label} $count") },
                                accentColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                if (visibleRows.isEmpty()) {
                    item {
                        AppleGroupedCard(contentPadding = PaddingValues(20.dp)) {
                            Text(
                                text = "当前筛选条件下没有匹配的小铺",
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.secondaryText,
                            )
                        }
                    }
                } else {
                    itemsIndexed(visibleRows, key = { _, row -> row.gid }) { index, row ->
                        MapleCompareRowCard(
                            rank = index + 1,
                            row = row,
                            ownQuote = state.quote,
                            isBestOffer = row.gid > 0L && row.gid == bestOffer?.gid,
                            canSellStock = state.sellableCropStock > 0L,
                            busy = busy,
                            actionLocked = actionLocked,
                            onClick = { onSelectRow(row) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapleCompareRowCard(
    rank: Int,
    row: MapleMarketFriendQuoteRow,
    ownQuote: MapleMarketQuote?,
    isBestOffer: Boolean,
    canSellStock: Boolean,
    busy: Boolean,
    actionLocked: Boolean,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val quote = row.quote
    val rowSellable = quote != null && quote.canSell && (row.gid == 0L || quote.remainingQuota > 0L)
    val clickable = rowSellable && !busy && !actionLocked

    val bgColor = when {
        isBestOffer -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        row.gid == 0L -> apple.elevated
        else -> apple.card
    }

    AppleGroupedCard(
        containerColor = bgColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val topRank = quote != null && rank <= 3
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (topRank) apple.orange.copy(alpha = 0.16f) else apple.track),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (topRank) apple.orange else apple.secondaryText,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = apple.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.gid == 0L) {
                        AppleStatusPill(text = "自家", color = apple.emerald, showDot = false)
                    } else if (isBestOffer) {
                        AppleStatusPill(text = "最高", color = MaterialTheme.colorScheme.primary, showDot = false)
                    }
                }

                if (quote != null) {
                    val detailText = buildString {
                        if (quote.boost > 0L) {
                            append("${formatNumber(quote.basePrice)} + ${formatNumber(quote.boost)}")
                        } else if (row.failed) {
                            append("复查失败")
                        } else {
                            append("未加成")
                        }
                        append(" · ")
                        if (row.gid == 0L) {
                            append("额度不限")
                        } else {
                            append("余 ${formatNumber(quote.remainingQuota)}")
                        }
                    }
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = if (row.failed && quote.boost <= 0L) apple.red else apple.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = if (row.failed) "报价查询失败" else "暂无收购报价",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (row.failed) apple.red else apple.secondaryText,
                    )
                }
            }

            if (quote != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = formatNumber(quote.unitPrice),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = apple.primaryText,
                        )
                        if (row.gid > 0L && ownQuote != null) {
                            val diff = quote.unitPrice - ownQuote.unitPrice
                            val diffStr = when {
                                diff > 0L -> "+${formatNumber(diff)}"
                                diff < 0L -> "-${formatNumber(-diff)}"
                                else -> "持平"
                            }
                            val diffCol = when {
                                diff > 0L -> apple.green
                                diff < 0L -> apple.red
                                else -> apple.secondaryText
                            }
                            Text(
                                text = diffStr,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = diffCol,
                            )
                        }
                    }
                    when {
                        rowSellable && !actionLocked -> AppleStatusPill(
                            text = if (canSellStock) "点击出售" else "无蜜薯",
                            color = if (canSellStock) MaterialTheme.colorScheme.primary else apple.secondaryText,
                            showDot = false,
                        )
                        row.gid > 0L && quote.remainingQuota <= 0L -> AppleStatusPill(
                            text = "额度已满",
                            color = apple.secondaryText,
                            showDot = false,
                        )
                        else -> AppleStatusPill(
                            text = "暂不可售",
                            color = apple.secondaryText,
                            showDot = false,
                        )
                    }
                }
            } else {
                AppleStatusPill(
                    text = if (row.failed) "失败" else "无报价",
                    color = if (row.failed) apple.red else apple.secondaryText,
                    showDot = false,
                )
            }
        }
    }
}

@Composable
private fun MaplePurchaseLogDialog(
    loading: Boolean,
    errorText: String?,
    records: List<MapleMarketPurchaseLogItem>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("小铺收购记录") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    loading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                    }

                    !errorText.isNullOrBlank() -> Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.red,
                        )
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重试")
                        }
                    }

                    records.isEmpty() -> Text(
                        text = "好友通过你的分享在铺子出售蜜薯后，成交和提成记录会显示在这里。",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(records, key = { idx, item -> "${item.sellerGid}_${item.timestampMs}_$idx" }) { _, item ->
                            Surface(
                                shape = RoundedCornerShape(AppleRadius.tile),
                                color = apple.elevated,
                                border = BorderStroke(0.5.dp, apple.separator),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        val lvl = if (item.sellerLevel > 0) " Lv.${item.sellerLevel}" else ""
                                        Text(
                                            text = "${item.sellerName}$lvl",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = apple.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        val timePrefix = if (item.timestampMs > 0L) "${formatTimestamp(item.timestampMs)} · " else ""
                                        Text(
                                            text = "${timePrefix}卖出 ${formatNumber(item.soldCount)} 个（${formatNumber(item.income)} 币）",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = apple.secondaryText,
                                        )
                                    }
                                    AppleStatusPill(
                                        text = "+${formatNumber(item.commission)} 提成",
                                        color = apple.orange,
                                        showDot = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
private fun MapleSellDialog(
    target: MapleSellTarget,
    busy: Boolean,
    actionLocked: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    var count by remember(target.gid, target.saleMax) {
        mutableIntStateOf(if (target.saleMax > 0) 1 else 0)
    }
    val clampedCount = if (target.saleMax > 0) count.coerceIn(1, target.saleMax) else 0
    val expectedIncome = clampedCount.toLong() * target.quote.unitPrice

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.shopTitle) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(AppleRadius.tile),
                    color = apple.elevated,
                    border = BorderStroke(0.5.dp, apple.separator),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("收购单价", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                            Text(
                                text = "${formatNumber(target.quote.unitPrice)} 福气币/个",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = apple.primaryText,
                            )
                        }
                        if (target.quote.boost > 0L) {
                            Text(
                                text = "构成：基础 ${formatNumber(target.quote.basePrice)} + 暴击 ${formatNumber(target.quote.boost)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("最多可卖", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                            Text(
                                text = "${formatNumber(target.saleMax.toLong())} 个（${target.saleLimitHint}）",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("出售数量", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                    AppleQuantityStepper(
                        value = clampedCount,
                        min = 1,
                        max = target.saleMax,
                        onChange = { count = it },
                        enabled = !busy && !actionLocked && target.saleMax > 0,
                    )
                }

                Text(
                    text = "预计获得：${formatNumber(expectedIncome)} 福气币",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = apple.orange,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(clampedCount) },
                enabled = !busy && !actionLocked && target.quote.canSell && clampedCount in 1..target.saleMax,
            ) {
                Text(if (busy) "出售中..." else "卖出 ${formatNumber(clampedCount.toLong())} 个")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun MapleExchangeDialog(
    offer: MapleMarketGoodsItem,
    coinBalance: Long,
    busy: Boolean,
    actionLocked: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val maxQty = if (offer.price <= 0L) {
        0
    } else {
        val afford = (coinBalance / offer.price).coerceIn(0L, 9999L).toInt()
        minOf(offer.remaining ?: 9999, afford).coerceAtLeast(0)
    }
    var count by remember(offer.id, maxQty) {
        mutableIntStateOf(if (maxQty > 0) 1 else 0)
    }
    val clampedQty = if (maxQty > 0) count.coerceIn(1, maxQty) else 0
    val totalCost = clampedQty.toLong() * offer.price

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("兑换 · ${offer.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (offer.items.isNotEmpty()) {
                    Text(
                        text = "包含：" + offer.items.joinToString("、") {
                            "${it.name.ifBlank { "#${it.id}" }} ×${formatCompactCount(it.count)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
                Text(
                    text = "单价：${formatNumber(offer.price)} 福气币 · 当前持有 ${formatNumber(coinBalance)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = apple.secondaryText,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("兑换数量", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                    AppleQuantityStepper(
                        value = clampedQty,
                        min = 1,
                        max = maxQty,
                        onChange = { count = it },
                        enabled = !busy && !actionLocked && maxQty > 0,
                    )
                }
                Text(
                    text = "预计消耗：${formatNumber(totalCost)} 福气币",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = apple.orange,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(clampedQty) },
                enabled = !busy && !actionLocked && offer.exchangeable && clampedQty in 1..maxQty,
            ) {
                Text(if (busy) "兑换中..." else "确认兑换")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("取消")
            }
        },
    )
}

private fun mapleItemImageUrl(itemId: Int): String =
    "/game-config/seed_images_named/seed_images/$itemId.png"

private fun mapleLimitLabel(limitType: Int): String = when (limitType) {
    1 -> "每日限购"
    2 -> "活动限购"
    else -> "限购"
}

private fun formatBasisPoints(bp: Int): String {
    val pct = bp / 100.0
    val formatted = if (pct % 1.0 == 0.0) {
        pct.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", pct).trimEnd('0').trimEnd('.')
    }
    return "+$formatted%"
}

private fun evaluateGoodsAction(
    state: MapleMarketState,
    goods: MapleMarketGoodsItem,
    actionLocked: Boolean,
): MapleGoodsActionState {
    if (goods.remaining == 0) {
        return MapleGoodsActionState(
            enabled = false,
            label = if (goods.limitType == 1) "今日已兑完" else "已兑完",
            reason = if (goods.limitType == 1) "今日兑换次数已达上限" else "活动兑换次数已达上限",
        )
    }
    val canExchangeNow = state.phase == "active" || state.phase == "settlement"
    if (!canExchangeNow) {
        return MapleGoodsActionState(
            enabled = false,
            label = "暂不可兑",
            reason = "当前活动阶段不可兑换",
        )
    }
    if (goods.price > state.coinBalance) {
        return MapleGoodsActionState(
            enabled = false,
            label = "兑换",
            reason = "福气币不足，还差 ${formatNumber(goods.price - state.coinBalance)}",
        )
    }
    if (!goods.exchangeable || actionLocked) {
        return MapleGoodsActionState(
            enabled = false,
            label = "暂不可兑",
            reason = if (actionLocked) "状态待刷新，请刷新后重试" else "该商品当前暂不可兑换",
        )
    }
    return MapleGoodsActionState(enabled = true, label = "兑换", reason = "")
}
