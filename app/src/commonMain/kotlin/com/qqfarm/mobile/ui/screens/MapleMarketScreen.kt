package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qqfarm.mobile.data.model.MapleMarketComparison
import com.qqfarm.mobile.data.model.MapleMarketFriendQuoteRow
import com.qqfarm.mobile.data.model.MapleMarketGoods
import com.qqfarm.mobile.data.model.MapleMarketPurchaseRecord
import com.qqfarm.mobile.data.model.MapleMarketQuote
import com.qqfarm.mobile.data.model.MapleMarketState
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleProgressBar
import com.qqfarm.mobile.ui.components.AppleQuantityStepper
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
import kotlinx.coroutines.launch
import kotlin.math.ceil
import com.qqfarm.mobile.platform.wallClockMillis
import com.qqfarm.mobile.platform.formatEpoch

private const val MAPLE_DAILY_QUOTA_CAP = 8000
private const val MAPLE_COIN_IMAGE = "/game-config/seed_images_named/seed_images/1033.png"
private const val MAPLE_CROP_IMAGE = "/game-config/seed_images_named/seed_images/1032.png"
private const val MAPLE_SEED_IMAGE = "/game-config/seed_images_named/seed_images/20307.png"

private enum class CompareFilter(val label: String) {
    ALL("全部"),
    SELLABLE("可出售"),
    COMPLETE("已暴击"),
    UNBOOSTED("未暴击"),
    UNAVAILABLE("暂无/失败"),
}

private data class ExchangeButtonPolicy(
    val openSheet: Boolean,
    val label: String,
    val insufficientText: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapleMarketDetailContent(
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
    var activeTab by remember(accountId) { mutableIntStateOf(0) } // 0=红薯小铺, 1=兑换商店, 2=好友比价
    var saleCount by remember(accountId) { mutableIntStateOf(1) }

    // 兑换商店弹窗
    var selectedExchangeGoods by remember(accountId) { mutableStateOf<MapleMarketGoods?>(null) }

    // 收购记录弹窗
    var purchaseLogOpen by remember(accountId) { mutableStateOf(false) }
    var purchaseLogLoading by remember(accountId) { mutableStateOf(false) }
    var purchaseLogError by remember(accountId) { mutableStateOf<String?>(null) }
    var purchaseRecords by remember(accountId) { mutableStateOf<List<MapleMarketPurchaseRecord>>(emptyList()) }

    // 好友比价状态提升至外层，切换页签不中断后台查价
    var comparison by remember(accountId) { mutableStateOf<MapleMarketComparison?>(null) }
    var compareLoading by remember(accountId) { mutableStateOf(false) }
    var compareStarting by remember(accountId) { mutableStateOf(false) }
    var compareSelling by remember(accountId) { mutableStateOf(false) }
    var compareError by remember(accountId) { mutableStateOf<String?>(null) }
    var compareFilter by remember(accountId) { mutableStateOf(CompareFilter.ALL) }
    var selectedFriendRow by remember(accountId) { mutableStateOf<MapleMarketFriendQuoteRow?>(null) }
    var compareJob by remember(accountId) { mutableStateOf<Job?>(null) }
    var compareAutoLoaded by remember(accountId) { mutableStateOf(false) }

    val shortDateFmt = remember { { ms: Long -> formatEpoch("M/d", ms) } }

    fun lockMutableActionsOnError() {
        val prev = state ?: return
        state = prev.copy(
            canClaimSeed = false,
            canClaimCommission = false,
            quote = prev.quote?.copy(canSell = false),
            goods = prev.goods.map { it.copy(exchangeable = false) },
        )
    }

    fun loadMarket() {
        if (busy) return
        scope.launch {
            loading = true
            errorText = null
            runCatching { apiClient.fetchMapleMarket(accountId) }
                .onSuccess { res ->
                    state = res
                    onRules(res.rules)
                    val maxSale = res.sellableCropStock.coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    if (saleCount > maxSale) saleCount = maxSale
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    errorText = err.message ?: "读取以薯换福失败"
                }
            loading = false
        }
    }

    fun loadComparison(start: Boolean = false, preserveError: Boolean = false) {
        if (compareLoading || compareSelling) return
        compareJob?.cancel()
        compareLoading = true
        compareStarting = start
        if (start) comparison = null
        if (!preserveError) compareError = null

        compareJob = scope.launch {
            try {
                var current = if (start) {
                    apiClient.startMapleMarketComparison(accountId)
                } else {
                    apiClient.fetchMapleMarketComparison(accountId)
                }
                compareStarting = false
                comparison = current
                while (current.status == "running") {
                    delay(650L)
                    current = apiClient.fetchMapleMarketComparison(accountId)
                    comparison = current
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                compareError = e.message ?: "读取好友报价失败"
            } finally {
                compareLoading = false
                compareStarting = false
            }
        }
    }

    fun openPurchaseLog() {
        if (purchaseLogLoading) return
        purchaseLogOpen = true
        purchaseLogLoading = true
        purchaseLogError = null
        scope.launch {
            runCatching { apiClient.fetchMapleMarketPurchaseLog(accountId) }
                .onSuccess { list ->
                    purchaseRecords = list
                    state = state?.copy(purchaseLogUnread = false)
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    purchaseLogError = err.message ?: "读取收购记录失败"
                }
            purchaseLogLoading = false
        }
    }

    DisposableEffect(accountId) {
        onDispose { compareJob?.cancel() }
    }

    LaunchedEffect(accountId, refreshTrigger) {
        loadMarket()
    }

    LaunchedEffect(state?.phase, accountId) {
        if (state?.phase == "active" && !compareAutoLoaded) {
            compareAutoLoaded = true
            loadComparison(start = false)
        }
    }

    LaunchedEffect(state, clockTickMs) {
        val s = state
        if (s == null) {
            onSubtitle(null)
            return@LaunchedEffect
        }
        val period = if (s.startTimeMs > 0L && s.endTimeMs > 0L) {
            "${shortDateFmt(s.startTimeMs)} – ${shortDateFmt(s.endTimeMs)}"
        } else ""
        val nowMs = if (s.serverTimeMs > 0L) {
            s.serverTimeMs + (clockTickMs - s.receivedAtElapsedMs).coerceAtLeast(0L)
        } else {
            wallClockMillis()
        }
        val phaseLabel = when (s.phase) {
            "upcoming" -> "未开始"
            "settlement" -> "兑换期"
            "ended", "unavailable" -> "已结束"
            else -> {
                val remainSec = ((s.endTimeMs - nowMs) / 1000L).coerceAtLeast(0L)
                if (remainSec > 0L) {
                    val leftDays = ceil((s.endTimeMs - nowMs).toDouble() / 86_400_000.0).toInt()
                    if (leftDays <= 0) "今天结束" else "剩 $leftDays 天 (${formatDurationSec(remainSec)})"
                } else {
                    "今天结束"
                }
            }
        }
        onSubtitle(listOf(period, phaseLabel).filter { it.isNotBlank() }.joinToString(" · "))
    }

    val current = state
    if (current == null && !loading && errorText != null) {
        AppleEmptyState(
            icon = Icons.Rounded.Storefront,
            title = "加载失败",
            subtitle = errorText.orEmpty(),
            actionLabel = "重试",
            onAction = ::loadMarket,
        )
        return
    }

    val hasShopDot = (current?.canClaimSeed == true) || (current?.canClaimCommission == true)
    val tabs = listOf(
        if (hasShopDot) "红薯小铺 •" else "红薯小铺",
        "兑换商店",
        "好友比价",
    )

    PullToRefreshBox(
        isRefreshing = loading && current != null,
        onRefresh = ::loadMarket,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (current == null) {
            AppleLoadingState()
            return@PullToRefreshBox
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            MapleWalletStrip(state = current)
            Spacer(modifier = Modifier.height(10.dp))
            AppleSegmentedControl(
                options = tabs,
                selectedIndex = activeTab,
                onSelect = { activeTab = it },
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (!errorText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = apple.red.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.red,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .swipeToSwitchTabs(activeTab, tabs.size) { activeTab = it },
            ) {
                when (activeTab) {
                    0 -> MapleShopTab(
                        state = current,
                        busy = busy,
                        saleCount = saleCount,
                        onSaleCountChange = { saleCount = it },
                        onClaimSeed = {
                            if (busy || !current.canClaimSeed) return@MapleShopTab
                            scope.launch {
                                busy = true
                                errorText = null
                                runCatching { apiClient.claimMapleMarketSeed(accountId) }
                                    .onSuccess { res ->
                                        onMessage(
                                            if (res.status == "confirmed") "已领取红薯种子 ×${res.count}"
                                            else res.message.ifBlank { "今日红薯种子已领取" }
                                        )
                                        busy = false
                                        loadMarket()
                                    }
                                    .onFailure { err ->
                                        busy = false
                                        lockMutableActionsOnError()
                                        errorText = err.message ?: "领取种子失败"
                                    }
                            }
                        },
                        onClaimCommission = {
                            if (busy || !current.canClaimCommission) return@MapleShopTab
                            scope.launch {
                                busy = true
                                errorText = null
                                runCatching { apiClient.claimMapleMarketCommission(accountId) }
                                    .onSuccess { res ->
                                        onMessage(
                                            if (res.status == "confirmed") "已领取小铺提成 ${formatNumber(res.claimedCoin)} 福气币"
                                            else res.message.ifBlank { "当前暂无可领提成" }
                                        )
                                        busy = false
                                        loadMarket()
                                    }
                                    .onFailure { err ->
                                        busy = false
                                        lockMutableActionsOnError()
                                        errorText = err.message ?: "领取提成失败"
                                    }
                            }
                        },
                        onOpenPurchaseLog = ::openPurchaseLog,
                        onSellOwn = { qty, price ->
                            if (busy) return@MapleShopTab
                            scope.launch {
                                busy = true
                                errorText = null
                                runCatching { apiClient.sellMapleMarketCrop(accountId, qty, price) }
                                    .onSuccess { res ->
                                        saleCount = 1
                                        onMessage("已卖出 ${res.sold} 个蜜薯，获得 ${formatNumber(res.income)} 福气币")
                                        busy = false
                                        loadMarket()
                                    }
                                    .onFailure { err ->
                                        busy = false
                                        lockMutableActionsOnError()
                                        errorText = err.message ?: "出售失败，请刷新确认"
                                    }
                            }
                        },
                        onGoCompare = { activeTab = 2 },
                    )

                    1 -> MapleExchangeTab(
                        state = current,
                        busy = busy,
                        onOpenExchange = { selectedExchangeGoods = it },
                    )

                    else -> MapleCompareTab(
                        state = current,
                        comparison = comparison,
                        running = compareStarting || comparison?.status == "running",
                        loading = compareLoading,
                        activityBusy = busy || compareSelling,
                        compareError = compareError,
                        filter = compareFilter,
                        onFilterChange = { compareFilter = it },
                        onStartCompare = { loadComparison(start = true) },
                        onSelectRow = { selectedFriendRow = it },
                    )
                }
            }
        }
    }

    // 收购记录弹窗
    if (purchaseLogOpen) {
        MaplePurchaseLogSheet(
            loading = purchaseLogLoading,
            errorText = purchaseLogError,
            records = purchaseRecords,
            onDismiss = { purchaseLogOpen = false },
        )
    }

    // 兑换商店确认弹窗（余额不足也允许打开，并在弹窗内明确提示还差多少）
    val exchangeOffer = selectedExchangeGoods
    if (exchangeOffer != null && current != null) {
        MapleExchangeConfirmSheet(
            goods = exchangeOffer,
            coinBalance = current.coinBalance,
            canExchangeNow = current.phase == "active" || current.phase == "settlement",
            pending = busy,
            onDismiss = { if (!busy) selectedExchangeGoods = null },
            onConfirm = { qty ->
                if (busy) return@MapleExchangeConfirmSheet
                scope.launch {
                    busy = true
                    errorText = null
                    runCatching { apiClient.exchangeMapleMarketGoods(accountId, exchangeOffer.id, qty, exchangeOffer.price) }
                        .onSuccess { res ->
                            selectedExchangeGoods = null
                            onMessage("已兑换 ${exchangeOffer.name} ×${res.count}，扣除 ${formatNumber(res.cost)} 福气币")
                            busy = false
                            loadMarket()
                        }
                        .onFailure { err ->
                            selectedExchangeGoods = null
                            busy = false
                            lockMutableActionsOnError()
                            errorText = err.message ?: "兑换失败，请刷新确认"
                        }
                }
            },
        )
    }

    // 好友铺子出售确认弹窗
    val sellRow = selectedFriendRow
    if (sellRow != null && current != null) {
        MapleFriendSellSheet(
            row = sellRow,
            sellableStock = current.sellableCropStock,
            selling = compareSelling || busy,
            onDismiss = { if (!compareSelling && !busy) selectedFriendRow = null },
            onConfirm = { qty ->
                val quote = sellRow.quote ?: return@MapleFriendSellSheet
                if (compareSelling || busy) return@MapleFriendSellSheet
                scope.launch {
                    compareSelling = true
                    compareError = null
                    val result = if (sellRow.gid == 0L) {
                        runCatching { apiClient.sellMapleMarketCrop(accountId, qty, quote.unitPrice) }
                    } else {
                        runCatching { apiClient.sellMapleMarketToFriend(accountId, sellRow.gid, qty, quote.unitPrice) }
                    }
                    result
                        .onSuccess { res ->
                            selectedFriendRow = null
                            compareSelling = false
                            onMessage("已卖出 ${res.sold} 个蜜薯，获得 ${formatNumber(res.income)} 福气币")
                            res.remainingQuota?.let { nextQuota ->
                                comparison = comparison?.let { cmp ->
                                    cmp.copy(
                                        rows = cmp.rows.map { r ->
                                            if (r.gid == sellRow.gid && r.quote != null) {
                                                r.copy(quote = r.quote.copy(remainingQuota = nextQuota))
                                            } else r
                                        }
                                    )
                                }
                            }
                            loadMarket()
                        }
                        .onFailure { err ->
                            selectedFriendRow = null
                            compareSelling = false
                            compareError = err.message ?: "出售失败，已同步最新报价"
                            loadComparison(start = false, preserveError = true)
                            loadMarket()
                        }
                }
            },
        )
    }
}

@Composable
private fun MapleWalletStrip(state: MapleMarketState) {
    val apple = LocalAppleColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MapleWalletChip(
            label = "福气币",
            count = state.coinBalance,
            imageUrl = MAPLE_COIN_IMAGE,
            fallbackIcon = Icons.Rounded.Paid,
            tint = apple.orange,
            modifier = Modifier.weight(1f),
        )
        MapleWalletChip(
            label = "蜜薯",
            count = state.cropStock,
            imageUrl = MAPLE_CROP_IMAGE,
            fallbackIcon = Icons.Rounded.Spa,
            tint = apple.red,
            modifier = Modifier.weight(1f),
        )
        MapleWalletChip(
            label = "红薯种子",
            count = state.seedStock,
            imageUrl = MAPLE_SEED_IMAGE,
            fallbackIcon = Icons.Rounded.Spa,
            tint = apple.emerald,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MapleWalletChip(
    label: String,
    count: Long,
    imageUrl: String,
    fallbackIcon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    AppleGroupedCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RemoteAssetImage(
                url = imageUrl,
                contentDescription = label,
                fallbackIcon = fallbackIcon,
                fallbackTint = tint,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = apple.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatCompactCount(count),
                    style = MaterialTheme.typography.titleSmall.copy(
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
}

@Composable
private fun MapleShopTab(
    state: MapleMarketState,
    busy: Boolean,
    saleCount: Int,
    onSaleCountChange: (Int) -> Unit,
    onClaimSeed: () -> Unit,
    onClaimCommission: () -> Unit,
    onOpenPurchaseLog: () -> Unit,
    onSellOwn: (count: Int, expectedUnitPrice: Int) -> Unit,
    onGoCompare: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val quote = state.quote
    val maxSellable = state.sellableCropStock.coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val safeSaleCount = saleCount.coerceIn(1, maxSellable)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. 今日行情卡
        item {
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = apple.emerald,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (!quote?.ownerName.isNullOrBlank()) "${quote?.ownerName}的红薯小铺" else "我的红薯小铺",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = apple.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!quote?.tier.isNullOrBlank()) {
                        AppleStatusPill(
                            text = "${quote?.tier}档",
                            color = apple.emerald,
                            showDot = false,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (quote != null) {
                    Text(
                        text = "今日收购价",
                        style = MaterialTheme.typography.labelSmall,
                        color = apple.secondaryText,
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = formatNumber(quote.unitPrice.toLong()),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp,
                                fontFeatureSettings = "tnum",
                            ),
                            color = apple.emerald,
                        )
                        Text(
                            text = "福气币 / 个",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = apple.track,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text("基础价", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                Text(
                                    text = formatNumber(quote.basePrice.toLong()),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = apple.primaryText,
                                )
                            }
                        }
                        val hasBoost = quote.boost > 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (hasBoost) apple.orange.copy(alpha = 0.14f) else apple.track,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = if (hasBoost) "分享暴击" else "未分享暴击",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasBoost) apple.orange else apple.secondaryText,
                                )
                                Text(
                                    text = "+${formatNumber(quote.boost.toLong())}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = if (hasBoost) apple.orange else apple.secondaryText,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("好友剩余可售额度", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                        Text(
                            text = "${formatNumber(quote.remainingQuota.toLong())} / ${formatNumber(MAPLE_DAILY_QUOTA_CAP.toLong())}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontFeatureSettings = "tnum",
                            ),
                            color = apple.primaryText,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    AppleProgressBar(
                        progress = { (quote.remainingQuota.toFloat() / MAPLE_DAILY_QUOTA_CAP.toFloat()).coerceIn(0f, 1f) },
                        color = apple.emerald,
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = apple.track,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (state.phase == "settlement") {
                                "收购已结束，可前往「兑换商店」使用福气币"
                            } else {
                                "当前暂无收购报价"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                if (state.phase == "active") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            }
        }

        // 2. 小铺事项（领种子 & 领提成 + 收购记录入口）
        item {
            AppleSectionHeader(
                title = "小铺事项",
                actionLabel = if (state.phase == "active" || state.phase == "settlement") {
                    if (state.purchaseLogUnread) "收购记录 •" else "收购记录"
                } else null,
                onAction = if (state.phase == "active" || state.phase == "settlement") onOpenPurchaseLog else null,
            )
            Spacer(modifier = Modifier.height(4.dp))
            AppleGroupedCard {
                // 每日红薯种子
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RemoteAssetImage(
                        url = MAPLE_SEED_IMAGE,
                        contentDescription = "红薯种子",
                        fallbackIcon = Icons.Rounded.Spa,
                        fallbackTint = apple.emerald,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(apple.emerald.copy(alpha = 0.12f)),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "每日红薯种子",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = apple.primaryText,
                        )
                        Text(
                            text = "每天可领一次，种出蜜薯后出售换福气币",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                        )
                    }
                    when {
                        state.seedClaimed -> AppleStatusPill(text = "今日已领", color = apple.green, showDot = false)
                        state.canClaimSeed -> Button(
                            onClick = onClaimSeed,
                            enabled = !busy,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) { Text("领取") }
                        else -> AppleStatusPill(text = "暂不可领", color = apple.secondaryText, showDot = false)
                    }
                }

                AppleHairlineDivider(startIndent = 64.dp)

                // 小铺提成
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RemoteAssetImage(
                        url = MAPLE_COIN_IMAGE,
                        contentDescription = "小铺提成",
                        fallbackIcon = Icons.Rounded.Paid,
                        fallbackTint = apple.orange,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(apple.orange.copy(alpha = 0.12f)),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "小铺提成",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = apple.primaryText,
                            )
                            if (state.pendingCommissionCoin > 0L) {
                                AppleStatusPill(
                                    text = "+${formatCompactCount(state.pendingCommissionCoin)}",
                                    color = apple.orange,
                                    showDot = false,
                                )
                            }
                        }
                        Text(
                            text = if (state.pendingCommissionCount > 0) {
                                "${state.pendingCommissionCount} 笔好友成交待领 · 按成交额 2% 分成"
                            } else {
                                "好友通过你的分享出售蜜薯，你获得 2% 福气币分成"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                        )
                    }
                    when {
                        state.canClaimCommission -> Button(
                            onClick = onClaimCommission,
                            enabled = !busy,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) { Text("领取") }
                        else -> AppleStatusPill(text = "暂无待领", color = apple.secondaryText, showDot = false)
                    }
                }
            }
        }

        // 3. 卖给自家小铺收银台
        item {
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = "卖给自家小铺",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = apple.primaryText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "按今日收购价结算，不占好友额度",
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (quote != null && quote.canSell && state.sellableCropStock > 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("可卖蜜薯", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                            if (state.cropStock > state.sellableCropStock) {
                                Text(
                                    text = "背包共 ${formatNumber(state.cropStock)}，其余已锁定",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                )
                            }
                        }
                        Text(
                            text = "${formatNumber(state.sellableCropStock)} 个",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = apple.primaryText,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppleQuantityStepper(
                            value = safeSaleCount,
                            min = 1,
                            max = maxSellable,
                            onChange = onSaleCountChange,
                            enabled = !busy,
                        )
                        OutlinedButton(
                            onClick = { onSaleCountChange(maxSellable) },
                            enabled = !busy && safeSaleCount < maxSellable,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("全部")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$safeSaleCount × ${quote.unitPrice}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = apple.secondaryText,
                        )
                        Text(
                            text = "${formatNumber(safeSaleCount.toLong() * quote.unitPrice)} 福气币",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = apple.orange,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onSellOwn(safeSaleCount, quote.unitPrice) },
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                    ) {
                        Text("卖出 $safeSaleCount 个")
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = apple.track,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (state.phase != "active") "当前阶段已停止收购蜜薯" else "暂无可出售的蜜薯，收获后再来",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                if (state.phase == "active") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = onGoCompare, enabled = !busy) {
                            Text("好友出价可能更高，去比价")
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun evaluateExchangePolicy(goods: MapleMarketGoods, state: MapleMarketState): ExchangeButtonPolicy {
    if (goods.remaining == 0) {
        return ExchangeButtonPolicy(
            openSheet = false,
            label = if (goods.limitType == 1) "今日已兑完" else "已兑完",
        )
    }
    val canExchangeNow = state.phase == "active" || state.phase == "settlement"
    if (!canExchangeNow || !goods.exchangeable) {
        return ExchangeButtonPolicy(openSheet = false, label = "暂不可兑")
    }
    if (goods.price > state.coinBalance) {
        return ExchangeButtonPolicy(
            openSheet = true,
            label = "兑换",
            insufficientText = "福气币不足，还差 ${formatNumber(goods.price - state.coinBalance)}",
        )
    }
    return ExchangeButtonPolicy(openSheet = true, label = "兑换")
}

@Composable
private fun MapleExchangeTab(
    state: MapleMarketState,
    busy: Boolean,
    onOpenExchange: (MapleMarketGoods) -> Unit,
) {
    val apple = LocalAppleColors.current
    if (state.goods.isEmpty()) {
        AppleEmptyState(
            icon = Icons.Rounded.CardGiftcard,
            title = "暂无可兑换商品",
            subtitle = "活动商店暂未上架商品",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AppleSectionHeader(
                title = "兑换商品 (${state.goods.size})",
                actionLabel = "持有 ${formatCompactCount(state.coinBalance)} 福气币",
                onAction = null,
            )
        }
        items(state.goods, key = { it.id }) { goods ->
            val policy = evaluateExchangePolicy(goods, state)
            val isSoldOut = goods.remaining == 0
            AppleGroupedCard(
                modifier = if (isSoldOut) Modifier.alpha(0.72f) else Modifier,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.size(52.dp)) {
                        RemoteAssetImage(
                            url = goods.items.firstOrNull()?.image.orEmpty(),
                            contentDescription = goods.name,
                            fallbackIcon = Icons.Rounded.CardGiftcard,
                            fallbackTint = apple.emerald,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(apple.track),
                        )
                        val firstCount = goods.items.firstOrNull()?.count ?: 0
                        if (goods.items.size == 1 && firstCount > 1) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = apple.primaryText.copy(alpha = 0.78f),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                            ) {
                                Text(
                                    text = "×$firstCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = apple.card,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }

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
                        if (goods.items.size > 1) {
                            Text(
                                text = "含 ${goods.items.size} 种道具",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                        }
                        Text(
                            text = if (goods.remaining != null && goods.limitTimes != null) {
                                "${goods.limitLabel} · 剩 ${goods.remaining} / ${goods.limitTimes}"
                            } else {
                                "不限兑换次数"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = apple.secondaryText,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                AppleHairlineDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RemoteAssetImage(
                            url = MAPLE_COIN_IMAGE,
                            contentDescription = "福气币",
                            fallbackIcon = Icons.Rounded.Paid,
                            fallbackTint = apple.orange,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = formatNumber(goods.price.toLong()),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = if (policy.insufficientText != null) apple.secondaryText else apple.orange,
                        )
                    }

                    if (policy.openSheet) {
                        if (policy.insufficientText != null) {
                            OutlinedButton(
                                onClick = { onOpenExchange(goods) },
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(policy.label)
                            }
                        } else {
                            Button(
                                onClick = { onOpenExchange(goods) },
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            ) {
                                Text(policy.label)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(policy.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapleCompareTab(
    state: MapleMarketState,
    comparison: MapleMarketComparison?,
    running: Boolean,
    loading: Boolean,
    activityBusy: Boolean,
    compareError: String?,
    filter: CompareFilter,
    onFilterChange: (CompareFilter) -> Unit,
    onStartCompare: () -> Unit,
    onSelectRow: (MapleMarketFriendQuoteRow) -> Unit,
) {
    val apple = LocalAppleColors.current
    if (state.phase != "active") {
        AppleEmptyState(
            icon = Icons.Rounded.Storefront,
            title = "当前阶段暂不支持好友比价",
            subtitle = "仅活动进行期间可查询并出售给好友红薯小铺",
        )
        return
    }

    val ranked = remember(comparison, state.quote) {
        if (comparison?.status != "ready") {
            emptyList()
        } else {
            val own = state.quote
            val list = comparison.rows.toMutableList()
            if (own != null) {
                list.add(
                    MapleMarketFriendQuoteRow(
                        gid = 0L,
                        name = own.ownerName.ifBlank { "自家" },
                        quote = own,
                        failed = false,
                    )
                )
            }
            list.sortedWith(
                compareByDescending<MapleMarketFriendQuoteRow> { it.quote?.unitPrice ?: -1 }
                    .thenByDescending { if (it.quote?.complete == true) 1 else 0 }
                    .thenByDescending {
                        if (it.gid == 0L) Int.MAX_VALUE else (it.quote?.remainingQuota ?: -1)
                    }
                    .thenBy { it.name }
            )
        }
    }

    val ownPrice = state.quote?.unitPrice
    val bestOffer = remember(ranked, ownPrice) {
        val candidate = ranked.firstOrNull { r ->
            r.gid > 0L && r.quote?.canSell == true && (r.quote.remainingQuota > 0)
        }
        if (candidate?.quote != null && candidate.quote.unitPrice > (ownPrice ?: 0)) candidate else null
    }

    val visibleRows = remember(ranked, filter) {
        when (filter) {
            CompareFilter.ALL -> ranked
            CompareFilter.SELLABLE -> ranked.filter { r ->
                r.quote?.canSell == true && (r.gid == 0L || r.quote.remainingQuota > 0)
            }
            CompareFilter.COMPLETE -> ranked.filter { it.gid > 0L && it.quote?.complete == true }
            CompareFilter.UNBOOSTED -> ranked.filter { it.gid > 0L && it.quote != null && !it.quote.complete }
            CompareFilter.UNAVAILABLE -> ranked.filter { it.gid > 0L && it.quote == null }
        }
    }

    val incompleteCount = remember(comparison) {
        if (comparison?.status == "ready") comparison.rows.count { it.quote?.complete != true } else 0
    }
    val queryBtnText = when {
        running -> "正在获取报价..."
        comparison?.status == "ready" && comparison.total > 0 ->
            if (incompleteCount > 0) "补查未完整 ($incompleteCount)" else "重新核对好友"
        else -> "查询全部好友"
    }

    val statusText = remember(comparison, running) {
        when {
            running -> {
                val total = comparison?.total ?: 0
                if (total <= 0) "正在拉取好友名单并分批获取报价…"
                else if (comparison!!.reused > 0) "已查 ${comparison.checked} / $total（复用 ${comparison.reused} 位当日完整报价）"
                else "已查 ${comparison.checked} / $total"
            }
            comparison?.status == "ready" -> {
                buildList {
                    if (comparison.reused > 0) add("本次跳过 ${comparison.reused} 位当日已暴击好友")
                    if (comparison.partial) add("好友列表仅返回部分数据")
                }.joinToString(" · ")
            }
            else -> ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "好友铺子比价",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = apple.primaryText,
                        )
                        Text(
                            text = "按暴击后实际收购价排序；同日补查跳过已暴击好友",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onStartCompare,
                        enabled = !running && !loading && !activityBusy,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(queryBtnText, style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (running) {
                    val total = comparison?.total ?: 0
                    val pct = if (total > 0) ((comparison!!.checked.toFloat() / total.toFloat()).coerceIn(0.04f, 1f)) else 0.1f
                    Spacer(modifier = Modifier.height(10.dp))
                    AppleProgressBar(
                        progress = { pct },
                        color = apple.emerald,
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                    if (statusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(statusText, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                    }
                } else if (comparison?.status == "ready" && comparison.total > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (bestOffer?.quote != null && ownPrice != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = apple.green.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = apple.orange, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "推荐卖到 ${bestOffer.name}：${bestOffer.quote.unitPrice} 福气币/个（比自家高 +${bestOffer.quote.unitPrice - ownPrice}）",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = apple.primaryText,
                                )
                            }
                        }
                    } else if (ownPrice != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = apple.track,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "当前自家收购价最高（$ownPrice 福气币/个）",
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.primaryText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                    }
                    if (statusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(statusText, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                    }
                }
            }
        }

        val shownCompareError = compareError ?: comparison?.error?.takeIf { it.isNotBlank() }
        if (!shownCompareError.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = apple.red.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = shownCompareError,
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.red,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        val readyComparison = comparison
        if (!running && (readyComparison == null || readyComparison.status == "idle")) {
            item {
                AppleEmptyState(
                    icon = Icons.Rounded.Group,
                    title = "还没有查询好友报价",
                    subtitle = "点击右上角「查询全部好友」，后台会分批读取好友小铺报价",
                )
            }
        } else if (!running && readyComparison?.status == "ready" && readyComparison.total == 0) {
            item {
                AppleEmptyState(
                    icon = Icons.Rounded.Group,
                    title = "暂无好友可查询",
                    subtitle = "好友列表为空",
                )
            }
        } else if (readyComparison != null && readyComparison.status == "ready" && readyComparison.total > 0) {
            item {
                val rawRows = readyComparison.rows
                val countMap = mapOf(
                    CompareFilter.ALL to ranked.size,
                    CompareFilter.SELLABLE to ranked.count { it.quote?.canSell == true && (it.gid == 0L || it.quote.remainingQuota > 0) },
                    CompareFilter.COMPLETE to rawRows.count { it.quote?.complete == true },
                    CompareFilter.UNBOOSTED to rawRows.count { it.quote != null && !it.quote.complete },
                    CompareFilter.UNAVAILABLE to rawRows.count { it.quote == null },
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    items(CompareFilter.entries) { f ->
                        AppleFilterChip(
                            selected = filter == f,
                            onClick = { onFilterChange(f) },
                            label = { Text("${f.label} ${countMap[f] ?: 0}") },
                        )
                    }
                }
            }

            if (visibleRows.isEmpty()) {
                item {
                    AppleGroupedCard(contentPadding = PaddingValues(20.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("当前筛选条件下没有好友", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                        }
                    }
                }
            } else {
                item {
                    AppleGroupedCard {
                        visibleRows.forEachIndexed { idx, row ->
                            if (idx > 0) AppleHairlineDivider(startIndent = 48.dp)
                            MapleFriendCompareRow(
                                rank = idx + 1,
                                row = row,
                                ownPrice = ownPrice,
                                isBest = bestOffer != null && row.gid == bestOffer.gid,
                                canAction = !activityBusy && state.sellableCropStock > 0L,
                                onSellClick = { onSelectRow(row) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapleFriendCompareRow(
    rank: Int,
    row: MapleMarketFriendQuoteRow,
    ownPrice: Int?,
    isBest: Boolean,
    canAction: Boolean,
    onSellClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val quote = row.quote
    val diff = if (quote != null && ownPrice != null && row.gid != 0L) quote.unitPrice - ownPrice else null
    val sellReady = quote?.canSell == true && (row.gid == 0L || quote.remainingQuota > 0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isBest) Modifier.background(apple.green.copy(alpha = 0.06f)) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (quote != null && rank <= 3) apple.orange.copy(alpha = 0.16f) else apple.track,
            modifier = Modifier.size(26.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (quote != null && rank <= 3) apple.orange else apple.secondaryText,
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                    AppleStatusPill(text = "我", color = apple.blue, showDot = false)
                } else if (isBest) {
                    AppleStatusPill(text = "最高", color = apple.green, showDot = false)
                }
            }

            if (quote != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val breakdown = when {
                        quote.boost > 0 -> "${quote.basePrice} + ${quote.boost}"
                        row.failed -> "复查失败"
                        else -> "未暴击"
                    }
                    Text(
                        text = "$breakdown · ${if (row.gid == 0L) "额度不限" else "余 ${formatNumber(quote.remainingQuota.toLong())}"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = if (row.failed) apple.red else apple.secondaryText,
                    )
                    if (diff != null) {
                        val diffColor = when {
                            diff > 0 -> apple.green
                            diff < 0 -> apple.red
                            else -> apple.secondaryText
                        }
                        val diffLabel = when {
                            diff > 0 -> "+$diff"
                            diff < 0 -> "$diff"
                            else -> "持平"
                        }
                        Text(
                            text = diffLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = diffColor,
                        )
                    }
                }
            } else {
                Text(
                    text = if (row.failed) "查询失败" else "暂无报价",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (row.failed) apple.red else apple.secondaryText,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (quote != null) {
                Text(
                    text = formatNumber(quote.unitPrice.toLong()),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (isBest) apple.green else apple.primaryText,
                )
            }
            when {
                quote == null -> Unit
                sellReady -> Button(
                    onClick = onSellClick,
                    enabled = canAction,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("卖这里", style = MaterialTheme.typography.labelMedium)
                }
                row.gid > 0L && quote.remainingQuota <= 0 -> AppleStatusPill(
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaplePurchaseLogSheet(
    loading: Boolean,
    errorText: String?,
    records: List<MapleMarketPurchaseRecord>,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 580.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("小铺收购记录", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)

            when {
                loading -> Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    AppleLoadingState(modifier = Modifier.height(80.dp))
                }
                !errorText.isNullOrBlank() -> Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = apple.red.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(errorText, style = MaterialTheme.typography.bodySmall, color = apple.red, modifier = Modifier.padding(12.dp))
                }
                records.isEmpty() -> AppleGroupedCard(contentPadding = PaddingValues(20.dp)) {
                    Text(
                        text = "暂无收购记录。好友通过你的分享在铺子出售蜜薯后，成交与提成记录会显示在这里。",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(records) { idx, record ->
                        AppleGroupedCard(contentPadding = PaddingValues(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = apple.emerald.copy(alpha = 0.14f),
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = record.sellerName.take(1).ifBlank { "友" },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = apple.emerald,
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = record.sellerName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = apple.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (record.sellerLevel > 0) {
                                            Text(
                                                text = "Lv.${record.sellerLevel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = apple.secondaryText,
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${formatTimestamp(record.timestampMs)} · 卖出 ${formatNumber(record.soldCount.toLong())} 个（${formatNumber(record.income)} 币）",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = apple.secondaryText,
                                    )
                                }
                                AppleStatusPill(
                                    text = "+${formatCompactCount(record.commission)} 提成",
                                    color = apple.green,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapleExchangeConfirmSheet(
    goods: MapleMarketGoods,
    coinBalance: Long,
    canExchangeNow: Boolean,
    pending: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxByBalance = if (goods.price > 0) (coinBalance / goods.price).coerceAtLeast(0L) else 9999L
    val limitCap = (goods.remaining ?: 9999).toLong()
    val maxCount = minOf(limitCap, maxByBalance).coerceIn(1L, 9999L).toInt()
    var count by remember(goods.id) { mutableIntStateOf(1) }
    val totalCost = count.toLong() * goods.price.toLong()
    val insufficient = totalCost > coinBalance
    val canConfirm = canExchangeNow && goods.exchangeable && (goods.remaining != 0) && !insufficient && !pending && count >= 1

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RemoteAssetImage(
                    url = goods.items.firstOrNull()?.image.orEmpty(),
                    contentDescription = goods.name,
                    fallbackIcon = Icons.Rounded.CardGiftcard,
                    fallbackTint = apple.emerald,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(apple.track),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = goods.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = apple.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("单价", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                        RemoteAssetImage(
                            url = MAPLE_COIN_IMAGE,
                            contentDescription = null,
                            fallbackIcon = Icons.Rounded.Paid,
                            fallbackTint = apple.orange,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = formatNumber(goods.price.toLong()),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = apple.orange,
                        )
                    }
                }
            }

            if (goods.items.isNotEmpty()) {
                AppleGroupedCard(contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (goods.items.size > 1) "每份包含 · ${goods.items.size} 件" else "每份包含",
                            style = MaterialTheme.typography.labelSmall,
                            color = apple.secondaryText,
                        )
                        goods.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RemoteAssetImage(
                                    url = item.image,
                                    contentDescription = item.name,
                                    fallbackIcon = Icons.Rounded.CardGiftcard,
                                    fallbackTint = apple.emerald,
                                    modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)),
                                )
                                Text(
                                    text = item.name.ifBlank { "物品 #${item.id}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.primaryText,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "×${formatNumber(item.count.toLong())}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = apple.primaryText,
                                )
                            }
                        }
                    }
                }
            }

            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("兑换数量", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                        if (goods.remaining != null) {
                            Text(
                                text = "${goods.limitLabel} · 还可兑 ${goods.remaining} 份",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppleQuantityStepper(
                            value = count,
                            min = 1,
                            max = maxCount,
                            onChange = { count = it },
                            enabled = !pending && maxCount >= 1,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                AppleHairlineDivider()
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("合计", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                        Text(
                            text = "余额 ${formatNumber(coinBalance)} 福气币",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = apple.secondaryText,
                        )
                    }
                    Text(
                        text = "${formatNumber(totalCost)} 福气币",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (insufficient) apple.red else apple.orange,
                    )
                }
            }

            if (insufficient) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = apple.red.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "福气币不足，还差 ${formatNumber(totalCost - coinBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.red,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !pending,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(count) },
                    enabled = canConfirm,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.4f).height(46.dp),
                ) {
                    Text(if (pending) "兑换中..." else "确认兑换")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapleFriendSellSheet(
    row: MapleMarketFriendQuoteRow,
    sellableStock: Long,
    selling: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val quote = row.quote ?: return
    val maxSellable = (if (row.gid == 0L) sellableStock else minOf(sellableStock, quote.remainingQuota.toLong()))
        .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    var count by remember(row.gid) { mutableIntStateOf(1) }
    val safeCount = if (maxSellable > 0) count.coerceIn(1, maxSellable) else 0
    val valid = safeCount in 1..maxSellable

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (row.gid == 0L) "出售给自家小铺" else "出售给 · ${row.name}",
                style = MaterialTheme.typography.titleLarge,
                color = apple.primaryText,
            )

            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = apple.track,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("收购单价", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                            Text(
                                text = "${quote.unitPrice} 福气币/个",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = apple.primaryText,
                            )
                            if (quote.boost > 0) {
                                Text(
                                    text = "${quote.basePrice} + 暴击 ${quote.boost}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.orange,
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = apple.track,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("最多可卖", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                            Text(
                                text = "${formatNumber(maxSellable.toLong())} 个",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = apple.primaryText,
                            )
                            Text(
                                text = if (row.gid == 0L || sellableStock <= quote.remainingQuota) {
                                    "受可卖蜜薯限制"
                                } else {
                                    "受好友剩余额度限制"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppleQuantityStepper(
                        value = safeCount.coerceAtLeast(1),
                        min = 1,
                        max = maxSellable.coerceAtLeast(1),
                        onChange = { count = it },
                        enabled = !selling && maxSellable >= 1,
                    )
                    OutlinedButton(
                        onClick = { count = maxSellable },
                        enabled = !selling && maxSellable > 0 && safeCount < maxSellable,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("全部")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                AppleHairlineDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$safeCount × ${quote.unitPrice}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = apple.secondaryText,
                    )
                    Text(
                        text = "${formatNumber(if (valid) safeCount.toLong() * quote.unitPrice else 0L)} 福气币",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = apple.orange,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !selling,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(safeCount) },
                    enabled = valid && !selling,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.4f).height(46.dp),
                ) {
                    Text(if (selling) "出售中..." else "卖出 $safeCount 个")
                }
            }
        }
    }
}
