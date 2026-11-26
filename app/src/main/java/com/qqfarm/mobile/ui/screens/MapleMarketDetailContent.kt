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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
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
import com.qqfarm.mobile.data.model.MapleMarketGoodsItem
import com.qqfarm.mobile.data.model.MapleMarketPurchaseLogItem
import com.qqfarm.mobile.data.model.MapleMarketQuote
import com.qqfarm.mobile.data.model.MapleMarketState
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleAlertDialog
import com.qqfarm.mobile.ui.components.AppleEmptyState
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val MAPLE_DAILY_QUOTA_CAP = 8000L

private data class MapleSellTarget(
    val quote: MapleMarketQuote,
    val saleMax: Int,
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

    var showPurchaseLog by remember(accountId) { mutableStateOf(false) }
    var purchaseLogLoading by remember(accountId) { mutableStateOf(false) }
    var purchaseLogError by remember(accountId) { mutableStateOf<String?>(null) }
    var purchaseLogs by remember(accountId) { mutableStateOf<List<MapleMarketPurchaseLogItem>>(emptyList()) }

    var sellTarget by remember(accountId) { mutableStateOf<MapleSellTarget?>(null) }
    var exchangeTarget by remember(accountId) { mutableStateOf<MapleMarketGoodsItem?>(null) }

    var loadJob by remember(accountId) { mutableStateOf<Job?>(null) }

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

    fun openSelfSellDialog() {
        val current = state ?: return
        val quote = current.quote ?: return
        if (actionLocked) {
            onMessage(actionLockError ?: "状态待核对，请先刷新")
            return
        }
        if (current.sellableCropStock <= 0L) {
            onMessage("当前暂无可出售的蜜薯")
            return
        }
        if (!quote.canSell) {
            onMessage("当前小铺暂不可出售")
            return
        }
        val saleMax = current.sellableCropStock.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        if (saleMax <= 0) {
            onMessage("当前无可售数量")
            return
        }
        sellTarget = MapleSellTarget(
            quote = quote,
            saleMax = saleMax,
        )
    }

    LaunchedEffect(accountId, refreshTrigger) {
        loadMarket()
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
        onRefresh = { loadMarket() },
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
                            onClick = { loadMarket() },
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
                    options = listOf("红薯小铺", "兑换商店"),
                    selectedIndex = selectedTab,
                    onSelect = { if (!busy) selectedTab = it },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .swipeToSwitchTabs(selectedTab, 2, !busy) { if (!busy) selectedTab = it },
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
                        onOpenSelfSell = { openSelfSellDialog() },
                    )

                    1 -> MapleExchangeTab(
                        state = current,
                        busy = busy,
                        actionLocked = actionLocked,
                        onSelectGoods = { exchangeTarget = it },
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
                        apiClient.sellMapleMarketCrop(accountId, count.toLong(), target.quote.unitPrice)
                    }
                    result.onSuccess { res ->
                        sellTarget = null
                        onMessage("已卖出 ${formatNumber(res.sold)} 个蜜薯，获得 ${formatNumber(res.income)} 福气币")
                        loadMarket()
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
                        Button(
                            onClick = onOpenSelfSell,
                            enabled = canSelfSell,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
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
                            if (goods.description.isNotBlank()) {
                                Text(
                                    text = goods.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.secondaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
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
    var count by remember(target.saleMax) {
        mutableIntStateOf(if (target.saleMax > 0) 1 else 0)
    }
    val clampedCount = if (target.saleMax > 0) count.coerceIn(1, target.saleMax) else 0
    val expectedIncome = clampedCount.toLong() * target.quote.unitPrice

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("卖给自家小铺") },
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
                                text = "${formatNumber(target.saleMax.toLong())} 个（受可卖蜜薯限制）",
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AppleQuantityStepper(
                            value = clampedCount,
                            min = 1,
                            max = target.saleMax,
                            onChange = { count = it },
                            enabled = !busy && !actionLocked && target.saleMax > 0,
                        )
                        TextButton(
                            onClick = { count = target.saleMax },
                            enabled = !busy && !actionLocked && target.saleMax > 0 && clampedCount < target.saleMax,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text("全部")
                        }
                    }
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
    val detailDesc = offer.description.ifBlank {
        if (offer.items.size == 1) offer.items.first().description else ""
    }

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
                if (detailDesc.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(AppleRadius.tile),
                        color = apple.elevated,
                        border = BorderStroke(0.5.dp, apple.separator),
                    ) {
                        Text(
                            text = detailDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                        )
                    }
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
