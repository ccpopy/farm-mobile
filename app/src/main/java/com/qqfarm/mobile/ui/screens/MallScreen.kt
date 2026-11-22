package com.qqfarm.mobile.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.CommerceItem
import com.qqfarm.mobile.data.model.IllustratedBookItem
import com.qqfarm.mobile.data.model.RanchQuote
import com.qqfarm.mobile.data.model.GameSpace
import com.qqfarm.mobile.data.model.MallCatalog
import com.qqfarm.mobile.data.model.MallGoods
import com.qqfarm.mobile.data.model.MallMembership
import com.qqfarm.mobile.data.model.MysteryShopState
import com.qqfarm.mobile.data.model.RanchShopGood
import com.qqfarm.mobile.data.model.RanchViewSnapshot
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleQuantityStepper
import com.qqfarm.mobile.ui.components.AppleScrollablePills
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.formatNumber
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.components.retainSheetHeight
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigInteger
import java.util.Locale
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.searchHeader
import com.qqfarm.mobile.ui.components.AppleSearchField
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs

private val ACTIVITY_ITEM_IDS = setOf(
    1028, 1029, 1030, 80101, 80102, 80103, 90031, 90032, 90033, 90034,
    90041, 29004, 29005, 101305, 101613, 101618, 101619, 6001, 6002, 26030, 90042,
)

@Composable
fun MallScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    gameSpace: GameSpace,
    selectedShopTab: Int,
    onSelectShopTab: (Int) -> Unit,
    busy: State<Boolean>,
    onBusyChange: (Boolean) -> Unit,
    onRefreshOverview: () -> Unit = {},
    onMessage: (String) -> Unit,
) {
    DisposableEffect(Unit) {
        onDispose { onBusyChange(false) }
    }

    if (gameSpace == GameSpace.RANCH) {
        RanchMallContent(
            session = session,
            apiClient = apiClient,
            selectedTab = selectedShopTab.coerceIn(0, 2),
            onSelectTab = onSelectShopTab,
            busyState = busy,
            onBusyChange = onBusyChange,
            onMessage = onMessage,
        )
    } else {
        FarmMallContent(
            session = session,
            apiClient = apiClient,
            selectedTab = selectedShopTab.coerceIn(0, 5),
            onSelectTab = onSelectShopTab,
            busyState = busy,
            onBusyChange = onBusyChange,
            onRefreshOverview = onRefreshOverview,
            onMessage = onMessage,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FarmMallContent(
    session: SessionUiState,
    apiClient: FarmApiClient,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    busyState: State<Boolean>,
    onBusyChange: (Boolean) -> Unit,
    onRefreshOverview: () -> Unit = {},
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()
    val purchasing by busyState

    var refreshing by remember(accountId, selectedTab, isConnected) { mutableStateOf(false) }
    var catalog by remember(accountId, selectedTab, isConnected) { mutableStateOf<MallCatalog?>(null) }
    var mystery by remember(accountId, selectedTab, isConnected) { mutableStateOf<MysteryShopState?>(null) }
    var seedItems by remember(accountId, selectedTab, isConnected) { mutableStateOf<List<IllustratedBookItem>?>(null) }
    var loadError by remember(accountId, selectedTab, isConnected) { mutableStateOf("") }

    var filterIdx by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var selectedGoods by remember(selectedTab, isConnected) { mutableStateOf<MallGoods?>(null) }
    var selectedMystery by remember(selectedTab, isConnected) { mutableStateOf<MysteryShopState?>(null) }
    var selectedSeedItem by remember(accountId, selectedTab, isConnected) { mutableStateOf<IllustratedBookItem?>(null) }

    var clockTickMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            clockTickMs = SystemClock.elapsedRealtime()
        }
    }

    val slotTypeForTab = when (selectedTab) {
        2 -> 5
        3 -> 0
        4 -> 4
        else -> 1
    }
    val isSeedShopTab = selectedTab == 1
    val isPropsShop = selectedTab == 3
    val isMysteryTab = selectedTab == 5

    suspend fun loadCurrentShop() {
        if (!isConnected || refreshing) return
        refreshing = true
        loadError = ""
        selectedGoods = null
        selectedMystery = null
        selectedSeedItem = null
        try {
            when {
                isMysteryTab -> mystery = apiClient.fetchMysteryShop(accountId)
                isSeedShopTab -> {
                    seedItems = apiClient.fetchSeedShopItems(accountId).sortedWith(
                        compareBy<IllustratedBookItem>(
                            { it.shopSeed?.requiredLevel ?: Int.MAX_VALUE },
                            { it.seedId },
                        )
                    )
                }
                else -> catalog = apiClient.fetchMallCatalog(accountId, slotTypeForTab, isPropsShop)
            }
        } catch (e: ApiException) {
            loadError = e.message
            if (isSeedShopTab) seedItems = null
            if (catalog != null || mystery != null) onMessage(e.message)
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(accountId, isConnected, selectedTab) {
        filterIdx = 0
        if (isConnected) loadCurrentShop()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppleScrollablePills(
            options = listOf("游戏商城", "种子商店", "限时商店", "道具商店", "SVIP", "神秘商人"),
            selectedIndex = selectedTab,
            onSelect = { idx ->
                if (!purchasing) onSelectTab(idx)
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .swipeToSwitchTabs(selectedTab, 6, !purchasing) { idx -> onSelectTab(idx) },
        ) {

            if (!isConnected) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                        AppleEmptyState(
                            icon = Icons.Rounded.Storefront,
                            title = "账号未在线",
                            subtitle = "请先启动当前账号后查看商店货品与货币余额",
                        )
                    }
                }
            } else {
                PullToRefreshBox(
                isRefreshing = refreshing && (catalog != null || mystery != null || seedItems != null),
                onRefresh = { if (!purchasing) scope.launch { loadCurrentShop() } },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (isMysteryTab) {
                    val m = mystery
                    if (m == null && loadError.isBlank()) {
                        AppleLoadingState()
                        return@PullToRefreshBox
                    }
                    val elapsedMs = if (m != null && m.receivedAtElapsedMs > 0L) {
                        (clockTickMs - m.receivedAtElapsedMs).coerceAtLeast(0L)
                    } else 0L
                    val serverNowMs = (m?.serverTimeMs ?: System.currentTimeMillis()) + elapsedMs
                    val remainSec = if (m != null && m.expireTimeMs > serverNowMs) {
                        ((m.expireTimeMs - serverNowMs) / 1000L).coerceAtLeast(0L)
                    } else 0L

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (m == null && loadError.isNotBlank()) {
                            item {
                                AppleGroupedCard {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.Storefront,
                                        title = "神秘商人读取失败",
                                        subtitle = loadError,
                                        actionLabel = "重新读取",
                                        onAction = { scope.launch { loadCurrentShop() } },
                                    )
                                }
                            }
                        } else if (m == null || !m.active || m.npc == null) {
                            item {
                                AppleGroupedCard {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.HourglassEmpty,
                                        title = "神秘商人暂未来访",
                                        subtitle = "商人来访时会在这里显示限时折扣货品",
                                        actionLabel = "刷新",
                                        onAction = { scope.launch { loadCurrentShop() } },
                                    )
                                }
                            }
                        } else {
                            val npc = m.npc
                            val canBuy = m.active && m.purchaseState.isBlank() &&
                                m.expireTimeMs > serverNowMs &&
                                npc.reward.count > 0L && npc.priceCount > 0L &&
                                npc.priceBalance != null && npc.priceBalance >= npc.priceCount

                            val hint = when {
                                m.purchaseState == "confirmed" -> "本轮购买已确认，不会重复下单。"
                                m.purchaseState == "unknown" -> "本轮已提交购买，结果尚未确认；请在游戏中核对，本轮不会重复下单。"
                                m.purchaseState == "unavailable" -> "交易记录暂不可用，请重启账号后刷新。"
                                m.expireTimeMs <= serverNowMs -> "商人已离开，请刷新查看最新状态。"
                                npc.priceBalance == null -> "余额暂时无法确认，请刷新后再购买。"
                                npc.priceBalance < npc.priceCount -> "${npc.priceName}余额不足。"
                                else -> ""
                            }

                            val btnLabel = when {
                                purchasing -> "购买中..."
                                canBuy -> "购买"
                                m.purchaseState == "confirmed" -> "已购买"
                                m.purchaseState == "unknown" -> "等待核对"
                                else -> "暂不可购买"
                            }

                            item {
                                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(Icons.Rounded.Storefront, contentDescription = null, tint = apple.indigo)
                                            Text(
                                                text = if (remainSec > 0L) "来访中 · 剩 ${formatDurationSec(remainSec)}" else "已离开",
                                                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                                color = if (remainSec > 0L) apple.indigo else apple.secondaryText,
                                            )
                                        }
                                        if (npc.discountPercent > 0) {
                                            val disc = npc.discountPercent / 10.0
                                            val text = if (npc.discountPercent % 10 == 0) {
                                                "${npc.discountPercent / 10}折"
                                            } else {
                                                String.format(Locale.US, "%.1f折", disc)
                                            }
                                            AppleStatusPill(text, apple.red, showDot = false)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        RemoteAssetImage(
                                            url = npc.reward.image,
                                            contentDescription = npc.reward.name,
                                            fallbackIcon = Icons.Rounded.ShoppingBag,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(apple.elevated)
                                                .padding(6.dp),
                                        )
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "${npc.reward.name} ×${formatCompactCount(npc.reward.count)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = apple.primaryText,
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "${formatCompactCount(npc.priceCount)} ${npc.priceName}",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontFeatureSettings = "tnum",
                                                    ),
                                                    color = apple.orange,
                                                )
                                                if (npc.originalPrice > npc.priceCount) {
                                                    Text(
                                                        text = formatCompactCount(npc.originalPrice),
                                                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                                        color = apple.secondaryText,
                                                    )
                                                }
                                            }
                                            if (npc.priceBalance != null) {
                                                Text(
                                                    text = "当前余额：${formatCompactCount(npc.priceBalance)} ${npc.priceName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = apple.secondaryText,
                                                )
                                            }
                                        }
                                    }

                                    if (hint.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(hint, style = MaterialTheme.typography.bodySmall, color = apple.orange)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { selectedMystery = m },
                                        enabled = canBuy && !refreshing && !purchasing,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text(btnLabel) }
                                }
                            }
                        }
                    }
                } else if (isSeedShopTab) {
                    val seeds = seedItems
                    if (seeds == null) {
                        if (loadError.isBlank()) {
                            AppleLoadingState()
                            return@PullToRefreshBox
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                            item {
                                AppleGroupedCard {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.Eco,
                                        title = "种子商店读取失败",
                                        subtitle = loadError,
                                        actionLabel = "重新读取",
                                        onAction = { scope.launch { loadCurrentShop() } },
                                    )
                                }
                            }
                        }
                        return@PullToRefreshBox
                    }
                    SeedShopGridContent(
                        items = seeds,
                        goldBalance = session.status?.gold ?: 0L,
                        isConnected = isConnected,
                        onSelectItem = { if (!refreshing && !purchasing) selectedSeedItem = it },
                    )
                } else {
                    val cat = catalog
                    if (cat == null) {
                        if (loadError.isBlank()) {
                            AppleLoadingState()
                            return@PullToRefreshBox
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                            item {
                                AppleGroupedCard {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.Storefront,
                                        title = "商城读取失败",
                                        subtitle = loadError,
                                        actionLabel = "重新读取",
                                        onAction = { scope.launch { loadCurrentShop() } },
                                    )
                                }
                            }
                        }
                        return@PullToRefreshBox
                    }

                    val filterDefs = if (isPropsShop) {
                        listOf("全部", "免费", "折扣")
                    } else {
                        listOf("全部", "免费", "折扣", "化肥", "狗粮", "活动限定")
                    }
                    val elapsedSec = ((clockTickMs - cat.receivedAtElapsedMs) / 1000L).coerceAtLeast(0L)
                    val serverNowMs = cat.serverTimeMs + elapsedSec * 1000L

                    val filtered = remember(cat, filterIdx, search, isPropsShop) {
                        val kw = search.trim().lowercase()
                        cat.goods.filter { g ->
                            val names = (listOf(g.name) + g.rewards.map { it.name }).joinToString(" ").lowercase()
                            val matchesKw = kw.isEmpty() || names.contains(kw) || g.id.toString().contains(kw)
                            val matchesFilter = when (filterIdx) {
                                1 -> g.isFree
                                2 -> g.isDiscounted || g.discountText.isNotBlank()
                                3 -> if (isPropsShop) true else names.contains("化肥")
                                4 -> if (isPropsShop) true else names.contains("狗粮")
                                5 -> if (isPropsShop) true else (g.isDiscounted || g.rewards.any { it.id in ACTIVITY_ITEM_IDS })
                                else -> true
                            }
                            matchesKw && matchesFilter
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Balance & Status Header
                        item {
                            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val statusText = when {
                                        isPropsShop -> "道具商店"
                                        slotTypeForTab == 4 && cat.membership != null -> if (cat.membership.isSvip) {
                                            "SVIP 生效中 · 剩 ${cat.membership.remainingDays} 天"
                                        } else "非 SVIP 会员（仅预览）"
                                        cat.refreshCountdownSec > 0L -> {
                                            val rem = (cat.refreshCountdownSec - elapsedSec).coerceAtLeast(0L)
                                            "${formatDurationSec(rem)} 后刷新"
                                        }
                                        else -> "游戏商城"
                                    }
                                    Text(statusText, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                    IconButton(
                                        onClick = { scope.launch { loadCurrentShop() } },
                                        enabled = !refreshing && !purchasing,
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新", modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (cat.currencies.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        cat.currencies.forEach { cur ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = apple.elevated,
                                                border = BorderStroke(0.5.dp, apple.separator),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                ) {
                                                    RemoteAssetImage(
                                                        url = cur.image,
                                                        contentDescription = cur.name,
                                                        fallbackIcon = Icons.Rounded.CardGiftcard,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Text(
                                                        text = "${cur.name} ${if (cur.balanceKnown) formatCompactCount(cur.count) else "--"}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                        color = apple.primaryText,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Filter chips & Search
                        searchHeader {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filterDefs.forEachIndexed { idx, label ->
                                        item {
                                            AppleFilterChip(
                                                selected = filterIdx == idx,
                                                onClick = { filterIdx = idx },
                                                label = { Text(label) },
                                            )
                                        }
                                    }
                                }
                                AppleSearchField(
                                    value = search,
                                    onValueChange = { search = it },
                                    placeholder = "搜索商品或礼包内容",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        if (filtered.isEmpty()) {
                            item {
                                AppleGroupedCard {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.Search,
                                        title = "没有匹配的商品",
                                        subtitle = "换个分类或搜索词试试",
                                    )
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { goods ->
                                val action = remember(goods, cat.membership) {
                                    resolveMallAction(goods, if (slotTypeForTab == 4) cat.membership else null)
                                }
                                AppleGroupedCard(
                                    modifier = Modifier.alpha(if (action.enabled) 1f else 0.65f),
                                    contentPadding = PaddingValues(14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        RemoteAssetImage(
                                            url = goods.rewards.firstOrNull()?.image.orEmpty(),
                                            contentDescription = goods.name,
                                            fallbackIcon = Icons.Rounded.ShoppingBag,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(apple.elevated)
                                                .padding(4.dp),
                                        )
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = goods.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = apple.primaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                )
                                                if (!goods.isFree && goods.discountText.isNotBlank()) {
                                                    AppleStatusPill(goods.discountText, apple.red, showDot = false)
                                                }
                                            }
                                            val rewardDesc = if (goods.rewards.size == 1) {
                                                "${goods.rewards[0].name} ×${formatCompactCount(goods.rewards[0].count)}"
                                            } else {
                                                "${goods.rewards.size}件：" + goods.rewards.take(3).joinToString(" · ") { "${it.name}×${formatCompactCount(it.count)}" }
                                            }
                                            Text(
                                                text = rewardDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = apple.secondaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            val metaParts = buildList {
                                                goods.limit?.let { lim ->
                                                    val prefix = if (isPropsShop) "限购" else mallLimitLabel(lim.type)
                                                    add("$prefix ${lim.bought}/${lim.max}")
                                                }
                                                if (goods.discountEndTimeMs > serverNowMs) {
                                                    add("特惠剩 ${formatDurationSec((goods.discountEndTimeMs - serverNowMs) / 1000L)}")
                                                }
                                            }
                                            if (metaParts.isNotEmpty()) {
                                                Text(
                                                    text = metaParts.joinToString(" · "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = apple.orange,
                                                )
                                            }
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            if (goods.isFree) {
                                                Text("免费", style = MaterialTheme.typography.titleSmall, color = apple.emerald)
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    RemoteAssetImage(
                                                        url = goods.priceImage,
                                                        contentDescription = goods.priceName,
                                                        fallbackIcon = Icons.Rounded.CardGiftcard,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Text(
                                                        text = formatCompactCount(goods.priceCount),
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontFeatureSettings = "tnum",
                                                        ),
                                                        color = apple.primaryText,
                                                    )
                                                    if (goods.originalPrice != null && goods.originalPrice > goods.priceCount) {
                                                        Text(
                                                            text = formatCompactCount(goods.originalPrice),
                                                            style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                                                            color = apple.secondaryText,
                                                        )
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { selectedGoods = goods },
                                                enabled = action.enabled && !refreshing && !purchasing,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp),
                                            ) { Text(action.label, style = MaterialTheme.typography.labelMedium) }
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

    selectedGoods?.let { goods ->
        MallPurchaseSheet(
            goods = goods,
            limitLabel = if (isPropsShop) "限购" else mallLimitLabel(goods.limit?.type),
            pending = purchasing,
            onDismiss = { if (!purchasing) selectedGoods = null },
            onConfirm = { count ->
                if (purchasing || refreshing || !isConnected) return@MallPurchaseSheet
                onBusyChange(true)
                scope.launch {
                    try {
                        runCatching {
                            apiClient.purchaseMallGoods(
                                accountId = accountId,
                                goods = goods,
                                slotType = slotTypeForTab,
                                propsShop = isPropsShop,
                                count = count,
                            )
                        }.onSuccess { res ->
                            selectedGoods = null
                            val rewardMsg = res.rewards.joinToString("、") { "${it.name} ×${formatCompactCount(it.count)}" }
                            onMessage(rewardMsg.ifBlank { "购买成功：${goods.name} ×$count" })
                            if (res.catalog != null) catalog = res.catalog else loadCurrentShop()
                        }.onFailure { err ->
                            if (err is CancellationException) throw err
                            selectedGoods = null
                            catalog = null
                            onMessage(err.message ?: "购买失败：${goods.name}")
                            loadCurrentShop()
                        }
                    } finally {
                        onBusyChange(false)
                    }
                }
            },
        )
    }

    selectedMystery?.let { currentShop ->
        val npc = currentShop.npc ?: return@let
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!purchasing) selectedMystery = null },
            sheetState = sheetState,
            containerColor = apple.card,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("确认购买神秘商品？", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                Text("${npc.reward.name} ×${formatCompactCount(npc.reward.count)}", style = MaterialTheme.typography.titleMedium, color = apple.primaryText)
                Text(
                    text = "需支付 ${formatCompactCount(npc.priceCount)} ${npc.priceName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = apple.orange,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { selectedMystery = null },
                        enabled = !purchasing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            if (purchasing || refreshing || !isConnected) return@Button
                            onBusyChange(true)
                            scope.launch {
                                try {
                                    runCatching {
                                        apiClient.purchaseMysteryOffer(accountId, npc.id, currentShop.rawJson)
                                    }.onSuccess { res ->
                                        selectedMystery = null
                                        if (res.shop != null) mystery = res.shop
                                        val rewardText = res.reward?.let { "${it.name} ×${formatCompactCount(it.count)}" }.orEmpty()
                                        onMessage(if (rewardText.isNotBlank()) "购买成功：$rewardText" else "购买成功")
                                    }.onFailure { err ->
                                        if (err is CancellationException) throw err
                                        selectedMystery = null
                                        onMessage(err.message ?: "神秘商人购买失败")
                                        val code = (err as? ApiException)?.errorCode.orEmpty()
                                        if (code.isBlank() || code in setOf("MYSTERY_PURCHASE_NOT_CONFIRMED", "MYSTERY_PURCHASE_PENDING", "COMMERCE_TIMEOUT")) {
                                            mystery = mystery?.copy(purchaseState = "unknown")
                                        } else {
                                            mystery = null
                                            loadCurrentShop()
                                        }
                                    }
                                } finally {
                                    onBusyChange(false)
                                }
                            }
                        },
                        enabled = !purchasing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text(if (purchasing) "购买中..." else "确认购买") }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    selectedSeedItem?.let { item ->
        val seed = item.shopSeed
        CropItemDetailSheet(
            item = item,
            goldBalance = session.status?.gold ?: 0L,
            isConnected = isConnected,
            busy = purchasing,
            seedShopMode = true,
            onDismiss = { if (!purchasing) selectedSeedItem = null },
            onPurchase = { count ->
                if (purchasing || refreshing || !isConnected || seed == null) return@CropItemDetailSheet
                onBusyChange(true)
                scope.launch {
                    try {
                        runCatching {
                            apiClient.purchaseIllustratedSeed(accountId, seed.goodsId, seed.seedId, count)
                        }.onSuccess { received ->
                            selectedSeedItem = null
                            onRefreshOverview()
                            onMessage("已购入 ${item.name} ×${formatNumber(received)} 颗")
                            loadCurrentShop()
                        }.onFailure { err ->
                            if (err is CancellationException) throw err
                            selectedSeedItem = null
                            onMessage(err.message ?: "种子购买失败：${item.name}")
                            loadCurrentShop()
                        }
                    } finally {
                        onBusyChange(false)
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RanchMallContent(
    session: SessionUiState,
    apiClient: FarmApiClient,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    busyState: State<Boolean>,
    onBusyChange: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()
    val busy by busyState

    var shopId by remember { mutableIntStateOf(5) }
    var mallSlot by remember { mutableIntStateOf(6) }
    var selectedShelf by remember { mutableIntStateOf(-1) }
    var autoPlace by remember { mutableStateOf(false) }

    val viewName = when (selectedTab) {
        1 -> "market"
        2 -> "mall"
        else -> "shop"
    }
    val shopParam = when (selectedTab) {
        0 -> shopId
        2 -> mallSlot
        else -> null
    }

    var refreshing by remember(accountId, selectedTab, shopParam, isConnected) { mutableStateOf(false) }
    var snapshot by remember(accountId, selectedTab, shopParam, isConnected) { mutableStateOf<RanchViewSnapshot?>(null) }
    var loadError by remember(accountId, selectedTab, shopParam, isConnected) { mutableStateOf("") }
    var selectedGood by remember(selectedTab, shopParam, isConnected) { mutableStateOf<RanchShopGood?>(null) }

    suspend fun loadRanchMall() {
        if (!isConnected || refreshing) return
        refreshing = true
        loadError = ""
        selectedGood = null
        try {
            snapshot = apiClient.fetchRanchView(accountId, viewName, shopId = shopParam)
        } catch (e: ApiException) {
            loadError = e.message
            if (snapshot != null) onMessage(e.message)
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(accountId, isConnected, selectedTab, shopParam) {
        if (isConnected) loadRanchMall()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppleScrollablePills(
            options = listOf("牧场商店", "仓鼠集市", "礼包商城"),
            selectedIndex = selectedTab,
            onSelect = { idx ->
                if (!busy) onSelectTab(idx)
            },
            accentColor = apple.orange,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .swipeToSwitchTabs(selectedTab, 3, !busy) { idx -> onSelectTab(idx) },
        ) {

            if (!isConnected) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                        AppleEmptyState(
                            icon = Icons.Rounded.Pets,
                            title = "账号未在线",
                            subtitle = "请先启动当前账号后访问牧场商店与仓鼠集市",
                        )
                    }
                }
            } else {
                PullToRefreshBox(
                isRefreshing = refreshing && snapshot != null,
                onRefresh = { scope.launch { loadRanchMall() } },
                modifier = Modifier.fillMaxSize(),
            ) {
                val current = snapshot
                if (current == null) {
                    if (loadError.isBlank()) {
                        AppleLoadingState()
                        return@PullToRefreshBox
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            AppleGroupedCard {
                                AppleEmptyState(
                                    icon = Icons.Rounded.Storefront,
                                    title = "牧场商城读取失败",
                                    subtitle = loadError,
                                    actionLabel = "重试",
                                    onAction = { scope.launch { loadRanchMall() } },
                                )
                            }
                        }
                    }
                    return@PullToRefreshBox
                }

                if (!current.base.unlocked) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            AppleGroupedCard {
                                AppleEmptyState(
                                    icon = Icons.Rounded.Pets,
                                    title = "牧场暂未解锁",
                                    subtitle = current.base.reason.ifBlank { "达到解锁等级后可浏览并购买牧场商品" },
                                )
                            }
                        }
                    }
                    return@PullToRefreshBox
                }

                val allGoods = when (selectedTab) {
                    1 -> current.marketGoods
                    2 -> current.mallGoods ?: current.canShopGoods
                    else -> current.shopGoods
                }
                val visibleGoods = if (selectedTab == 1 && selectedShelf >= 0) {
                    allGoods.filter { it.shelfId == selectedShelf }
                } else allGoods

                fun balanceOf(good: RanchShopGood): Long? =
                    good.priceBalance ?: current.base.currencies.firstOrNull { it.id == good.priceId }?.count

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Sub-segment Controls & Balance
                    item {
                        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                            when (selectedTab) {
                                0 -> {
                                    AppleSegmentedControl(
                                        options = listOf("幼崽", "饲料"),
                                        selectedIndex = if (shopId == 6) 1 else 0,
                                        onSelect = { if (!busy) shopId = if (it == 1) 6 else 5 },
                                    )
                                    if (shopId == 5) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("购买后自动放养", style = MaterialTheme.typography.bodyMedium, color = apple.primaryText)
                                            AppleSwitch(
                                                checked = autoPlace,
                                                onCheckedChange = { autoPlace = it },
                                                enabled = !busy,
                                                checkedTrackColor = apple.orange,
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.Info, contentDescription = null, tint = if (current.marketPresent) apple.emerald else apple.orange, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = if (current.marketPresent) "仓鼠驿站今日到访，可用便便或松果兑换" else "仓鼠驿站当前未到访",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (current.marketPresent) apple.primaryText else apple.orange,
                                        )
                                    }
                                    if (current.marketShelves.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            item {
                                                AppleFilterChip(
                                                    selected = selectedShelf < 0,
                                                    onClick = { selectedShelf = -1 },
                                                    label = { Text("全部") },
                                                )
                                            }
                                            items(current.marketShelves, key = { it.id }) { sh ->
                                                AppleFilterChip(
                                                    selected = selectedShelf == sh.id,
                                                    onClick = { selectedShelf = sh.id },
                                                    label = { Text(sh.name) },
                                                )
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    AppleSegmentedControl(
                                        options = listOf("礼包商城", "特惠商城"),
                                        selectedIndex = if (mallSlot == 7) 1 else 0,
                                        onSelect = { if (!busy) mallSlot = if (it == 1) 7 else 6 },
                                    )
                                }
                            }

                            // Currency Balances Used in Current List
                            val usedCurrencies = remember(allGoods, current.base.currencies) {
                                allGoods.filter { !it.free && it.priceId > 0 }
                                    .distinctBy { it.priceId }
                                    .map { g -> Triple(g.priceId, g.priceName, g.priceImage) to balanceOf(g) }
                            }
                            if (usedCurrencies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    usedCurrencies.forEach { (info, bal) ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = apple.elevated,
                                            border = BorderStroke(0.5.dp, apple.separator),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            ) {
                                                RemoteAssetImage(
                                                    url = info.third,
                                                    contentDescription = info.second,
                                                    fallbackIcon = Icons.Rounded.CardGiftcard,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Text(
                                                    text = "${info.second} ${if (bal != null) formatCompactCount(bal) else "--"}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                    color = apple.primaryText,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (visibleGoods.isEmpty()) {
                        item {
                            AppleGroupedCard {
                                AppleEmptyState(
                                    icon = Icons.Rounded.Storefront,
                                    title = if (selectedTab == 1) "集市暂无商品" else "暂无商品",
                                    subtitle = if (selectedTab == 1) "仓鼠驿站到访时会上架兑换商品" else "切换分类看看",
                                )
                            }
                        }
                    } else {
                        items(visibleGoods, key = { it.id }) { good ->
                            val maxCap = ranchPurchaseLimit(good, balanceOf(good))
                            val canClick = !busy && !refreshing && good.available && maxCap >= 1
                            val unavailText = when {
                                !good.available && good.remaining == 0 -> "已售罄"
                                !good.available && good.requiredLevel > 0 && current.base.level < good.requiredLevel -> "需牧场 ${good.requiredLevel} 级"
                                !good.available && selectedTab == 1 && !current.marketPresent -> "未到访"
                                !good.available -> "暂不可购"
                                balanceOf(good) == null && !good.free -> "余额未确认"
                                maxCap < 1 && !good.free -> "余额不足"
                                else -> ""
                            }

                            AppleGroupedCard(
                                modifier = Modifier.alpha(if (good.available) 1f else 0.65f),
                                contentPadding = PaddingValues(14.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    RemoteAssetImage(
                                        url = good.reward.image,
                                        contentDescription = good.name,
                                        fallbackIcon = Icons.Rounded.Pets,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(apple.elevated)
                                            .padding(4.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                text = good.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = apple.primaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (good.husbandryType.isNotBlank()) {
                                                AppleStatusPill(
                                                    text = good.husbandryType,
                                                    color = if (good.husbandryType == "棚") apple.orange else apple.blue,
                                                    showDot = false,
                                                )
                                            }
                                        }
                                        val subReward = ranchGoodsContents(good)
                                        Text(subReward, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val metaParts = buildList {
                                            if (good.remaining != null) add("剩余 ${good.remaining}")
                                            if (unavailText.isNotBlank()) add(unavailText)
                                            else if (good.requiredLevel > 1) add("牧场 ${good.requiredLevel} 级")
                                        }
                                        if (metaParts.isNotEmpty()) {
                                            Text(
                                                text = metaParts.joinToString(" · "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (unavailText.isNotBlank()) apple.orange else apple.secondaryText,
                                            )
                                        }
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        if (good.free) {
                                            Text("免费", style = MaterialTheme.typography.titleSmall, color = apple.emerald)
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                RemoteAssetImage(
                                                    url = good.priceImage,
                                                    contentDescription = good.priceName,
                                                    fallbackIcon = Icons.Rounded.CardGiftcard,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Text(
                                                    text = formatCompactCount(good.priceCount),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontFeatureSettings = "tnum",
                                                    ),
                                                    color = apple.primaryText,
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = { selectedGood = good },
                                            enabled = canClick,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp),
                                        ) {
                                            Text(
                                                when {
                                                    good.free -> "领取"
                                                    selectedTab == 1 -> "兑换"
                                                    else -> "购买"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
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
    }

    selectedGood?.let { good ->
        val actionName = when (selectedTab) {
            1 -> "exchange"
            2 -> if (snapshot?.mallGoods != null) "buyMall" else "buyCan"
            else -> "buy"
        }
        val verb = when {
            good.free -> "领取"
            selectedTab == 1 -> "兑换"
            else -> "购买"
        }
        val currentBase = snapshot?.base
        val bal = good.priceBalance ?: currentBase?.currencies?.firstOrNull { it.id == good.priceId }?.count
        val maxCap = ranchPurchaseLimit(good, bal)

        RanchMallAmountSheet(
            good = good,
            verb = verb,
            balance = bal,
            maxCount = maxCap,
            showAutoPlaceNote = autoPlace && selectedTab == 0 && shopId == 5 && good.husbandryType.isNotBlank(),
            pending = busy,
            onDismiss = { if (!busy) selectedGood = null },
            onConfirm = { count ->
                if (busy || refreshing || !isConnected) return@RanchMallAmountSheet
                onBusyChange(true)
                scope.launch {
                    try {
                        val place = autoPlace && selectedTab == 0 && shopId == 5 && good.husbandryType.isNotBlank()
                        val input = JSONObject()
                            .put("goodsId", good.id)
                            .put("count", count)
                            .put("shopId", shopId)
                            .put("slotType", mallSlot)
                            .put("autoPlace", place)

                        val quoteRes = runCatching { apiClient.quoteRanchAction(accountId, actionName, input) }
                        val quote = quoteRes.getOrNull()
                        if (quote == null) {
                            onMessage(quoteRes.exceptionOrNull()?.message ?: "报价读取失败")
                            return@launch
                        }
                        if (quote.skipped) {
                            selectedGood = null
                            onMessage(quote.message.ifBlank { "当前没有可操作项目" })
                            return@launch
                        }

                        if (!ranchQuoteMatchesGoods(good, count, quote)) {
                            selectedGood = null
                            onMessage("商品价格或内容已变化，请重新选择")
                            loadRanchMall()
                            return@launch
                        }

                        runCatching {
                            apiClient.executeRanchAction(accountId, actionName, input, quote.signature)
                        }.onSuccess { res ->
                            selectedGood = null
                            val message = if (res.status == "skipped") "当前没有可操作项目" else "${verb}成功：${good.name} ×$count"
                            onMessage(res.notice.ifBlank { message })
                            loadRanchMall()
                        }.onFailure { err ->
                            if (err is CancellationException) throw err
                            selectedGood = null
                            snapshot = null
                            onMessage(err.message ?: "${verb}失败，请刷新核对")
                            loadRanchMall()
                        }
                    } finally {
                        onBusyChange(false)
                    }
                }
            },
        )
    }
}

private data class MallActionUi(
    val enabled: Boolean,
    val label: String,
    val reason: String,
)

private fun mallLimitLabel(type: Int?): String = when (type) {
    1 -> "每日限购"
    2 -> "每周限购"
    3 -> "每月限购"
    4 -> "永久限购"
    else -> "限购"
}

private fun resolveMallAction(goods: MallGoods, membership: MallMembership?): MallActionUi {
    fun blocked(label: String, reason: String = goods.unavailableReason.ifBlank { label }) =
        MallActionUi(false, label, reason)

    if (membership?.isSvip == false || goods.purchaseStatus == "svip_required") {
        return blocked("SVIP 限定", "需要 SVIP 会员身份")
    }
    if (goods.purchaseStatus == "owned") return blocked("已拥有")
    if (goods.purchaseStatus == "sold_out" || goods.limit?.remaining == 0) {
        return blocked(if (goods.isFree) "已领取" else "售罄")
    }
    if (goods.purchaseStatus == "ad_required") return blocked("广告领取")
    if (goods.purchaseStatus == "share_required") return blocked("需先分享")
    if (!goods.purchasable) return blocked("暂不可购")
    if (!goods.isFree) {
        if (goods.priceId <= 0 || goods.priceCount <= 0L) return blocked("价格未确认")
        if (goods.priceBalance == null) return blocked("余额未确认")
        if (goods.priceBalance < goods.priceCount) return blocked("余额不足", "${goods.priceName}余额不足")
    }
    return MallActionUi(true, if (goods.isFree) "领取" else "购买", "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MallPurchaseSheet(
    goods: MallGoods,
    limitLabel: String,
    pending: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!pending) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 560.dp)
                .retainSheetHeight()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MallPurchaseContent(goods, pending, onDismiss, onConfirm, limitLabel)
        }
    }
}

@Composable
internal fun ColumnScope.MallPurchaseContent(
    goods: MallGoods,
    pending: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    limitLabel: String = mallLimitLabel(goods.limit?.type),
) {
    val apple = LocalAppleColors.current
    val action = resolveMallAction(goods, null)
    val maxCount = if (action.enabled) {
        val quota = goods.limit?.remaining ?: 9999
        val affordable = if (goods.isFree) 9999L else (goods.priceBalance ?: 0L) / goods.priceCount
        minOf(9999L, quota.toLong(), affordable).coerceAtLeast(0L).toInt()
    } else 0
    var requestedCount by remember(goods.id) { mutableIntStateOf(1) }
    val count = requestedCount.coerceAtMost(maxCount)
    val totalCost = if (goods.isFree) 0L else goods.priceCount * count.toLong()

    Text(goods.name, style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (goods.isFree || (goods.priceId > 0 && goods.priceCount > 0L)) {
            Text(
                text = if (goods.isFree) "免费领取" else "单价 ${formatNumber(goods.priceCount)} ${goods.priceName}",
                style = MaterialTheme.typography.bodySmall,
                color = apple.secondaryText,
            )
        }

        AppleGroupedCard(contentPadding = PaddingValues(12.dp), containerColor = apple.elevated) {
            Text("每份包含 (${goods.rewards.size})", style = MaterialTheme.typography.labelMedium, color = apple.secondaryText)
            Spacer(modifier = Modifier.height(6.dp))
            goods.rewards.forEach { rew ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RemoteAssetImage(
                        url = rew.image,
                        contentDescription = rew.name,
                        fallbackIcon = Icons.Rounded.CardGiftcard,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(rew.name, style = MaterialTheme.typography.bodyMedium, color = apple.primaryText, modifier = Modifier.weight(1f))
                    Text("×${formatNumber(rew.count)}", style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"), color = apple.primaryText)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("购买数量", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                if (goods.limit != null) {
                    Text("$limitLabel · 可买 $maxCount 份", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                }
            }
            AppleQuantityStepper(value = count, max = maxCount, onChange = { requestedCount = it }, enabled = !pending)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("合计", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                if (!goods.isFree && goods.priceBalance != null) {
                    Text("当前余额 ${formatNumber(goods.priceBalance)}", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                }
            }
            Text(
                text = when {
                    count == 0 -> "—"
                    goods.isFree -> "免费"
                    else -> "${formatNumber(totalCost)} ${goods.priceName}"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = if (goods.isFree) apple.emerald else apple.primaryText,
            )
        }

        if (!action.enabled) {
            Text(action.reason, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onDismiss,
            enabled = !pending,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
        ) { Text("取消") }
        Button(
            onClick = { onConfirm(count) },
            enabled = !pending && count > 0,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
        ) { Text(if (pending) "购买中..." else if (goods.isFree) "确认领取" else "确认购买") }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RanchMallAmountSheet(
    good: RanchShopGood,
    verb: String,
    balance: Long?,
    maxCount: Int,
    showAutoPlaceNote: Boolean,
    pending: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var requestedCount by remember(good.id) { mutableIntStateOf(1) }
    val count = requestedCount.coerceAtMost(maxCount)
    val totalCost = good.priceCount * count.toLong()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("$verb ${good.name}", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RemoteAssetImage(
                    url = good.reward.image,
                    contentDescription = good.name,
                    fallbackIcon = Icons.Rounded.Pets,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(apple.elevated)
                        .padding(4.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(good.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                    Text(ranchGoodsContents(good), style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                    val balNote = if (balance != null && !good.free) " · 余额 ${formatCompactCount(balance)}" else ""
                    Text(
                        text = if (good.free) "免费领取" else "单价 ${formatCompactCount(good.priceCount)} ${good.priceName}$balNote",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
            }

            if (showAutoPlaceNote) {
                Surface(shape = RoundedCornerShape(10.dp), color = apple.orange.copy(alpha = 0.14f)) {
                    Text(
                        text = "购买后将自动放养至${good.husbandryType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.orange,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("数量", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                AppleQuantityStepper(value = count, max = maxCount, onChange = { requestedCount = it }, enabled = !pending)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("总消耗", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                Text(
                    text = if (good.free) "免费" else "${formatCompactCount(totalCost)} ${good.priceName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = if (good.free) apple.emerald else apple.orange,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !pending,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) { Text("取消") }
                Button(
                    onClick = { onConfirm(count) },
                    enabled = !pending && good.available && count > 0,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) { Text(if (pending) "提交中..." else verb) }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

internal fun ranchGoodsContents(good: RanchShopGood): String =
    good.rewards.ifEmpty { listOf(good.reward) }
        .joinToString(" · ") {
            if (it.id == 1006) "${it.name} ${formatDurationSec(it.count)}"
            else "${it.name} ×${formatCompactCount(it.count)}"
        }

internal fun ranchPurchaseLimit(good: RanchShopGood, balance: Long?): Int {
    val quota = minOf(good.maxCount, good.remaining ?: good.maxCount).coerceAtLeast(0)
    if (good.free) return quota
    if (balance == null || good.priceCount <= 0L) return 0
    return minOf(quota.toLong(), balance / good.priceCount).coerceAtLeast(0L).toInt()
}

internal fun ranchQuoteMatchesGoods(good: RanchShopGood, count: Int, quote: RanchQuote): Boolean {
    val expectedCost = if (good.free) "" else {
        val total = BigInteger.valueOf(good.priceCount).multiply(BigInteger.valueOf(count.toLong()))
        if (total == BigInteger.ZERO) "" else "${good.priceId}:$total"
    }
    return itemSignature(quote.costItems) == expectedCost &&
        itemSignature(quote.rewards) == scaledItemSignature(good.rewards, count)
}

private fun itemSignature(items: List<CommerceItem>): String {
    val totals = mutableMapOf<Int, BigInteger>()
    for (item in items) {
        totals[item.id] = (totals[item.id] ?: BigInteger.ZERO) + BigInteger.valueOf(item.count)
    }
    return totals.entries
        .filter { it.value != BigInteger.ZERO }
        .sortedBy { it.key }
        .joinToString("|") { "${it.key}:${it.value}" }
}

private fun scaledItemSignature(items: List<CommerceItem>, count: Int): String {
    val mul = BigInteger.valueOf(count.toLong())
    val totals = mutableMapOf<Int, BigInteger>()
    for (item in items) {
        totals[item.id] = (totals[item.id] ?: BigInteger.ZERO) + BigInteger.valueOf(item.count).multiply(mul)
    }
    return totals.entries
        .filter { it.value != BigInteger.ZERO }
        .sortedBy { it.key }
        .joinToString("|") { "${it.key}:${it.value}" }
}
