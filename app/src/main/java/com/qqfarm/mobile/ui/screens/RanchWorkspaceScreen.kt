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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.RanchAnimal
import com.qqfarm.mobile.data.model.RanchBagItem
import com.qqfarm.mobile.data.model.RanchBookAnimal
import com.qqfarm.mobile.data.model.RanchBookData
import com.qqfarm.mobile.data.model.RanchFacility
import com.qqfarm.mobile.data.model.RanchQuote
import com.qqfarm.mobile.data.model.RanchViewSnapshot
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleIconBadge
import com.qqfarm.mobile.ui.components.AppleQuantityStepper
import com.qqfarm.mobile.ui.components.AppleScrollablePills
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.RanchQuoteConfirmSheet
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatBucketHours
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.ui.theme.SoilGold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.unit.sp
import com.qqfarm.mobile.ui.components.AppleActionPill
import com.qqfarm.mobile.ui.components.EqualHeightRow
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleSearchField
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs

private data class PendingRanchRequest(
    val action: String,
    val input: JSONObject,
    val quote: RanchQuote,
)

private data class AmountPromptConfig(
    val title: String,
    val itemName: String,
    val itemImage: String,
    val caption: String,
    val maxCount: Int,
    val submitLabel: String,
    val pens: List<RanchFacility> = emptyList(),
    val onConfirm: (count: Int, selectedPenId: String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RanchWorkspaceScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    initialSubTab: Int,
    onSubTabChanged: (Int) -> Unit,
    onOpenRanchMall: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()

    var subTab by remember { mutableIntStateOf(initialSubTab.coerceIn(0, 4)) }
    LaunchedEffect(initialSubTab) {
        subTab = initialSubTab.coerceIn(0, 4)
    }

    var refreshing by remember(subTab, isConnected) { mutableStateOf(false) }
    var actionBusy by remember { mutableStateOf(false) }
    var snapshot by remember(subTab, isConnected) { mutableStateOf<RanchViewSnapshot?>(null) }
    var loadError by remember(subTab, isConnected) { mutableStateOf("") }

    var pendingRequest by remember(subTab, isConnected) { mutableStateOf<PendingRanchRequest?>(null) }
    var showFeedSheet by remember(isConnected) { mutableStateOf(false) }
    var feedTargetAnimal by remember(isConnected) { mutableStateOf<RanchAnimal?>(null) }
    var amountPrompt by remember(isConnected) { mutableStateOf<AmountPromptConfig?>(null) }

    var clockTickMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            clockTickMs = SystemClock.elapsedRealtime()
        }
    }

    fun viewNameForTab(tab: Int): String = when (tab) {
        1 -> "facilities"
        2 -> "bag"
        3 -> "tasks"
        4 -> "illustrated"
        else -> "animals"
    }

    suspend fun loadCurrentView() {
        if (!isConnected || refreshing) return
        refreshing = true
        loadError = ""
        runCatching { apiClient.fetchRanchView(accountId, viewNameForTab(subTab)) }
            .onSuccess { snapshot = it }
            .onFailure {
                if (it is CancellationException) throw it
                val msg = it.message ?: "牧场数据读取失败"
                loadError = msg
                onMessage(msg)
            }
        refreshing = false
    }

    fun triggerQuoteAction(action: String, input: JSONObject) {
        if (actionBusy || !isConnected) return
        actionBusy = true
        scope.launch {
            runCatching { apiClient.quoteRanchAction(accountId, action, input) }
                .onSuccess { quote ->
                    if (quote.skipped) {
                        onMessage(quote.message.ifBlank { "当前没有可操作项目" })
                    } else {
                        pendingRequest = PendingRanchRequest(action, input, quote)
                    }
                }
                .onFailure { onMessage(it.message ?: "读取操作条件失败") }
            actionBusy = false
        }
    }

    fun triggerDirectAction(action: String, params: Map<String, Any>) {
        if (actionBusy || !isConnected) return
        actionBusy = true
        scope.launch {
            runCatching { apiClient.operateRanchDirect(accountId, action, params) }
                .onSuccess { msg ->
                    onMessage(msg)
                    loadCurrentView()
                }
                .onFailure { onMessage(it.message ?: "牧场操作失败") }
            actionBusy = false
        }
    }

    LaunchedEffect(accountId) {
        snapshot = null
        loadError = ""
    }

    LaunchedEffect(accountId, isConnected, subTab) {
        if (isConnected) loadCurrentView()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        AppleScrollablePills(
            options = listOf("动物", "设施", "背包", "任务", "图鉴"),
            selectedIndex = subTab,
            onSelect = { idx ->
                if (!actionBusy) {
                    subTab = idx
                    onSubTabChanged(idx)
                }
            },
            accentColor = apple.orange,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .swipeToSwitchTabs(subTab, 5, !actionBusy) { idx ->
                subTab = idx
                onSubTabChanged(idx)
            },
        ) {

            if (!isConnected) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                        AppleEmptyState(
                            icon = Icons.Rounded.Pets,
                            title = "账号未在线",
                            subtitle = "请先启动当前账号后管理牧场动物、设施、背包与图鉴",
                        )
                    }
                }
            } else {
                PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { loadCurrentView() } },
                modifier = Modifier.fillMaxSize(),
            ) {
                val current = snapshot
                when {
                    current == null && loadError.isNotBlank() -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                            item {
                                AppleGroupedCard {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.Pets,
                                        title = "牧场读取失败",
                                        subtitle = loadError,
                                        actionLabel = "重新加载",
                                        onAction = { scope.launch { loadCurrentView() } },
                                    )
                                }
                            }
                        }
                    }
                    current != null && !current.base.unlocked -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                            item {
                                AppleGroupedCard {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.Pets,
                                        title = "牧场暂未解锁",
                                        subtitle = current.base.reason.ifBlank { "达到解锁等级后可在此管理牧场" },
                                    )
                                }
                            }
                        }
                    }
                    else -> when (subTab) {
                        0 -> RanchAnimalsTab(
                            snapshot = current,
                            clockTickMs = clockTickMs,
                            busy = actionBusy,
                            onDirectAction = { act, params -> triggerDirectAction(act, params) },
                            onOpenFeed = { animal ->
                                feedTargetAnimal = animal
                                showFeedSheet = true
                            },
                            onReleaseAnimal = { animal ->
                                triggerQuoteAction(
                                    "release",
                                    JSONObject().put("ranchId", animal.ranchId).put("animalId", animal.id),
                                )
                            },
                            onOpenRanchMall = onOpenRanchMall,
                        )
                        1 -> RanchFacilitiesTab(
                            snapshot = current,
                            clockTickMs = clockTickMs,
                            busy = actionBusy,
                            onAddGrass = { fac ->
                                val grassOwned = current?.base?.grass ?: 0L
                                val room = (fac.capacity - fac.feedCount).coerceAtLeast(1)
                                val maxAdd = grassOwned.coerceAtMost(room.toLong()).toInt().coerceAtLeast(1)
                                amountPrompt = AmountPromptConfig(
                                    title = "为${fac.name}补充牧草",
                                    itemName = "牧草",
                                    itemImage = "/game-config/seed_images_named/seed_images/7001.png",
                                    caption = "库存 ${formatCompactCount(grassOwned)} · 还可放入 $room",
                                    maxCount = maxAdd,
                                    submitLabel = "补充牧草",
                                    onConfirm = { cnt, _ ->
                                        triggerQuoteAction(
                                            "addGrass",
                                            JSONObject().put("facilityId", fac.id).put("count", cnt),
                                        )
                                    },
                                )
                            },
                            onUpgrade = { fac ->
                                triggerQuoteAction("upgrade", JSONObject().put("facilityId", fac.id))
                            },
                            onCompleteUpgrade = { fac ->
                                triggerQuoteAction("completeUpgrade", JSONObject().put("facilityId", fac.id))
                            },
                            onActivateShed = { fac ->
                                triggerQuoteAction("activateShed", JSONObject().put("facilityId", fac.id))
                            },
                        )
                        2 -> RanchBagTab(
                            snapshot = current,
                            busy = actionBusy,
                            onSellAllProducts = {
                                triggerQuoteAction("sell", JSONObject().put("all", true))
                            },
                            onItemAction = { item, actionKey ->
                                val owned = item.count.coerceAtMost(9999L).toInt().coerceAtLeast(1)
                                when (actionKey) {
                                    "place" -> {
                                        val expectedModule = if (item.husbandryType == "棚") 7 else 6
                                        val pens = (current?.base?.facilities ?: emptyList()).filter {
                                            it.hasRanch && it.state != 2 &&
                                                (item.husbandryType.isBlank() || it.moduleType == expectedModule)
                                        }
                                        val freeSpace = pens.maxOfOrNull { (it.ranchCapacity - it.ranchCount).coerceAtLeast(0) } ?: 0
                                        if (freeSpace <= 0) {
                                            onMessage("${item.husbandryType.ifBlank { "窝或棚" }}已住满，请先收获或升级设施")
                                            return@RanchBagTab
                                        }
                                        amountPrompt = AmountPromptConfig(
                                            title = "放养 ${item.name}",
                                            itemName = item.name,
                                            itemImage = item.image,
                                            caption = "库存 ${formatCompactCount(item.count)}",
                                            maxCount = owned.coerceAtMost(freeSpace),
                                            submitLabel = "下一步",
                                            pens = pens,
                                            onConfirm = { cnt, penId ->
                                                triggerQuoteAction(
                                                    "place",
                                                    JSONObject()
                                                        .put("uid", item.uid)
                                                        .put("itemId", item.id)
                                                        .put("ranchId", penId)
                                                        .put("count", cnt),
                                                )
                                            },
                                        )
                                    }
                                    "useCan" -> {
                                        amountPrompt = AmountPromptConfig(
                                            title = "添加罐头到罐头桶",
                                            itemName = item.name,
                                            itemImage = item.image,
                                            caption = if (item.canSpeedUpSec > 0L) {
                                                "每个加速 ${formatDurationSec(item.canSpeedUpSec)} · 库存 ${formatCompactCount(item.count)}"
                                            } else "库存 ${formatCompactCount(item.count)}",
                                            maxCount = owned,
                                            submitLabel = "添加罐头",
                                            onConfirm = { cnt, _ ->
                                                triggerQuoteAction(
                                                    "useCan",
                                                    JSONObject()
                                                        .put("uid", item.uid)
                                                        .put("itemId", item.id)
                                                        .put("count", cnt),
                                                )
                                            },
                                        )
                                    }
                                    "sell" -> {
                                        val priceStr = item.sellPrices.joinToString(" + ") { "${formatCompactCount(it.count)} ${it.name}" }
                                        amountPrompt = AmountPromptConfig(
                                            title = "出售 ${item.name}",
                                            itemName = item.name,
                                            itemImage = item.image,
                                            caption = if (priceStr.isNotBlank()) "单价 $priceStr · 库存 ${formatCompactCount(item.count)}" else "库存 ${formatCompactCount(item.count)}",
                                            maxCount = owned,
                                            submitLabel = "下一步",
                                            onConfirm = { cnt, _ ->
                                                triggerQuoteAction(
                                                    "sell",
                                                    JSONObject()
                                                        .put("uid", item.uid)
                                                        .put("itemId", item.id)
                                                        .put("count", cnt),
                                                )
                                            },
                                        )
                                    }
                                }
                            },
                        )
                        3 -> RanchTasksTab(
                            snapshot = current,
                            session = session,
                            busy = actionBusy,
                            onClaimTasks = {
                                triggerQuoteAction("claimTasks", JSONObject().put("all", true))
                            },
                        )
                        4 -> RanchBookTab(
                            snapshot = current,
                            busy = actionBusy,
                            onClaimIllustrated = { kind ->
                                triggerQuoteAction("claimIllustrated", JSONObject().put("kind", kind))
                            },
                        )
                    }
                }
            }
            }
        }
    }

    pendingRequest?.let { pending ->
        RanchQuoteConfirmSheet(
            quote = pending.quote,
            busy = actionBusy,
            onDismiss = { if (!actionBusy) pendingRequest = null },
            onConfirm = {
                if (actionBusy || !isConnected) return@RanchQuoteConfirmSheet
                actionBusy = true
                scope.launch {
                    runCatching {
                        apiClient.executeRanchAction(
                            accountId = accountId,
                            action = pending.action,
                            input = pending.input,
                            expectedSignature = pending.quote.signature,
                        )
                    }.onSuccess { res ->
                        pendingRequest = null
                        onMessage(res.notice.ifBlank { if (res.status == "skipped") "当前没有可操作项目" else "操作完成" })
                        loadCurrentView()
                    }.onFailure {
                        if (it is CancellationException) throw it
                        pendingRequest = null
                        onMessage(it.message ?: "牧场操作失败，请刷新核对")
                        loadCurrentView()
                    }
                    actionBusy = false
                }
            },
        )
    }

    if (showFeedSheet) {
        RanchWorkspaceFeedSheet(
            targetAnimal = feedTargetAnimal,
            onFeed = { bucketType, produceAfterFeed ->
                val animal = feedTargetAnimal
                showFeedSheet = false
                feedTargetAnimal = null
                val params = if (animal != null) {
                    mapOf(
                        "ranchId" to animal.ranchId,
                        "animalId" to animal.id,
                        "bucketType" to bucketType,
                        "produceAfterFeed" to produceAfterFeed,
                    )
                } else {
                    mapOf(
                        "all" to true,
                        "bucketType" to bucketType,
                        "produceAfterFeed" to produceAfterFeed,
                    )
                }
                triggerDirectAction("feed", params)
            },
            onDismiss = {
                showFeedSheet = false
                feedTargetAnimal = null
            },
        )
    }

    amountPrompt?.let { cfg ->
        RanchAmountPickerSheet(
            config = cfg,
            onDismiss = { amountPrompt = null },
            onSubmit = { cnt, penId ->
                val callback = cfg.onConfirm
                amountPrompt = null
                callback(cnt, penId)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RanchAnimalsTab(
    snapshot: RanchViewSnapshot?,
    clockTickMs: Long,
    busy: Boolean,
    onDirectAction: (String, Map<String, Any>) -> Unit,
    onOpenFeed: (RanchAnimal?) -> Unit,
    onReleaseAnimal: (RanchAnimal) -> Unit,
    onOpenRanchMall: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val payload = snapshot?.base
    if (payload == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                AppleEmptyState(icon = Icons.Rounded.Pets, title = "正在读取牧场动物...", subtitle = "下拉可刷新最新状态")
            }
        }
        return
    }

    val allAnimals = payload.pens.flatMap { it.animals }
    val harvestCount = allAnimals.count { it.canHarvest } + payload.shelvesCollectableCount
    val produceCount = allAnimals.count { it.canProduce }
    val totalCapacity = payload.facilities.filter { it.hasRanch }.sumOf { it.ranchCapacity }
        .takeIf { it > 0 } ?: payload.pens.sumOf { it.capacity }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppleStatusPill("饲养 ${allAnimals.size}/$totalCapacity", apple.orange, showDot = false)
                    AppleStatusPill("牧草 ${formatCompactCount(payload.grass)}", apple.green, showDot = false)
                    payload.buckets.forEach { bucket ->
                        AppleStatusPill("${bucket.name} ${formatBucketHours(bucket.seconds)}", apple.blue, showDot = false)
                    }
                    if (payload.hazardsCount > 0) AppleStatusPill("待清理 ${payload.hazardsCount}", apple.red)
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    RanchPillBtn("一键收获 ($harvestCount)", true, !busy && harvestCount > 0) {
                        onDirectAction("harvest", mapOf("all" to true))
                    }
                }
                item {
                    RanchPillBtn("一键生产 ($produceCount)", false, !busy && produceCount > 0) {
                        onDirectAction("produce", mapOf("all" to true))
                    }
                }
                item {
                    RanchPillBtn("一键喂养", false, !busy && allAnimals.isNotEmpty()) {
                        onOpenFeed(null)
                    }
                }
                item {
                    RanchPillBtn("一键清理", false, !busy && payload.hazardsCount > 0) {
                        onDirectAction("clean", emptyMap())
                    }
                }
            }
        }

        if (allAnimals.isEmpty()) {
            item {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Pets,
                        title = "还没有饲养动物",
                        subtitle = "可以在牧场商店购买幼崽后放养到窝或棚",
                        actionLabel = "去牧场商店",
                        onAction = onOpenRanchMall,
                    )
                }
            }
        } else {
            payload.pens.forEach { pen ->
                item {
                    AppleSectionHeader(title = "${pen.name} (${pen.count}/${pen.capacity})")
                }
                if (pen.animals.isEmpty()) {
                    item {
                        AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                            Text("${pen.name}暂无放养动物", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
                        }
                    }
                } else {
                    val rows = pen.animals.chunked(2)
                    items(rows) { rowAnimals ->
                        EqualHeightRow {
                            rowAnimals.forEach { animal ->
                                RanchWorkspaceAnimalTile(
                                    animal = animal,
                                    serverNowMs = payload.serverNowMs,
                                    fetchedAtElapsedMs = payload.fetchedAtElapsedMs,
                                    clockTickMs = clockTickMs,
                                    busy = busy,
                                    modifier = Modifier.weight(1f),
                                    onHarvest = {
                                        onDirectAction("harvest", mapOf("ranchId" to animal.ranchId, "animalId" to animal.id))
                                    },
                                    onProduce = {
                                        onDirectAction("produce", mapOf("ranchId" to animal.ranchId, "animalId" to animal.id))
                                    },
                                    onFeed = { onOpenFeed(animal) },
                                    onRelease = { onReleaseAnimal(animal) },
                                )
                            }
                            if (rowAnimals.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RanchWorkspaceAnimalTile(
    animal: RanchAnimal,
    serverNowMs: Long,
    fetchedAtElapsedMs: Long,
    clockTickMs: Long,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onHarvest: () -> Unit,
    onProduce: () -> Unit,
    onFeed: () -> Unit,
    onRelease: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val deadline = if (animal.deliveringUntilMs > 0L) animal.deliveringUntilMs else animal.nextAtMs
    val estServerNow = serverNowMs + (clockTickMs - fetchedAtElapsedMs).coerceAtLeast(0L)
    val remainSec = if (deadline > 0L) ((deadline - estServerNow) / 1000L).coerceAtLeast(0L) else 0L
    var menuOpen by remember { mutableStateOf(false) }

    val highlight = when {
        animal.isGiant -> SoilGold
        animal.canHarvest -> apple.green
        else -> null
    }

    // 卡片每个区域都是固定高度：巨大动物、可生产轮次、可放生与否都不会改变卡片高度，
    // 同一行再由 EqualHeightRow + fillMaxHeight 兜底对齐。
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = apple.card,
        border = BorderStroke(
            width = if (highlight != null) 1.2.dp else 0.5.dp,
            color = highlight ?: apple.separator,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "#${animal.slot}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = apple.secondaryText,
                )
                if (animal.canRelease) {
                    Box {
                        // 破坏性操作收进“更多”菜单，避免和收获/喂养挤在一起误触；放生仍会弹出确认。
                        Box(
                            modifier = Modifier
                                .size(width = 32.dp, height = 22.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !busy) { menuOpen = true },
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "更多操作",
                                tint = apple.secondaryText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("放生「${animal.name}」", color = apple.red) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = apple.red)
                                },
                                onClick = {
                                    menuOpen = false
                                    onRelease()
                                },
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                RemoteAssetImage(
                    url = animal.image,
                    contentDescription = animal.name,
                    fallbackIcon = Icons.Rounded.Pets,
                    modifier = Modifier.size(52.dp),
                )
                if (animal.isGiant) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 2.dp),
                        shape = CircleShape,
                        color = SoilGold,
                        border = BorderStroke(1.5.dp, apple.card),
                    ) {
                        Text(
                            text = "巨",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = animal.name,
                style = MaterialTheme.typography.titleSmall,
                color = apple.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    animal.canHarvest -> "可收获"
                    animal.canProduce -> "可生产"
                    animal.grassBlocked -> "缺少牧草"
                    remainSec > 0L -> formatDurationSec(remainSec)
                    else -> animal.stageName.ifBlank { "成长中" }
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = when {
                    animal.canHarvest -> apple.green
                    animal.canProduce -> apple.blue
                    animal.grassBlocked -> apple.orange
                    else -> apple.secondaryText
                },
                maxLines = 1,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (animal.isGiant && animal.weightKg.isNotBlank()) {
                    Text(
                        text = animal.weightKg,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = SoilGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (animal.maxProduceCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 1..animal.maxProduceCount.coerceAtMost(8)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (i <= animal.produceCount) apple.orange else apple.track),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Spacer(modifier = Modifier.height(6.dp))

            val buttonModifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
            when {
                animal.canHarvest -> Button(
                    onClick = onHarvest,
                    enabled = !busy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = apple.green, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = buttonModifier,
                ) { Text("收获", style = MaterialTheme.typography.labelLarge) }

                animal.canProduce -> Button(
                    onClick = onProduce,
                    enabled = !busy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = apple.orange, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = buttonModifier,
                ) { Text("生产", style = MaterialTheme.typography.labelLarge) }

                animal.canFeed -> FilledTonalButton(
                    onClick = onFeed,
                    enabled = !busy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = apple.orange.copy(alpha = 0.14f),
                        contentColor = apple.orange,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = buttonModifier,
                ) { Text("喂养", style = MaterialTheme.typography.labelLarge) }

                else -> Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = apple.elevated,
                    modifier = buttonModifier,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("成长中", style = MaterialTheme.typography.labelMedium, color = apple.secondaryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun RanchFacilitiesTab(
    snapshot: RanchViewSnapshot?,
    clockTickMs: Long,
    busy: Boolean,
    onAddGrass: (RanchFacility) -> Unit,
    onUpgrade: (RanchFacility) -> Unit,
    onCompleteUpgrade: (RanchFacility) -> Unit,
    onActivateShed: (RanchFacility) -> Unit,
) {
    val apple = LocalAppleColors.current
    val payload = snapshot?.base
    if (payload == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                AppleEmptyState(icon = Icons.Rounded.Warehouse, title = "正在读取牧场设施...", subtitle = "下拉可刷新设施与升级进度")
            }
        }
        return
    }

    val elapsedSec = ((clockTickMs - payload.fetchedAtElapsedMs) / 1000L).coerceAtLeast(0L)
    val serverNowMs = payload.serverNowMs + elapsedSec * 1000L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppleIconBadge(icon = Icons.Rounded.Grass, tint = apple.green)
                        Text("牧草库存", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                    }
                    Text(
                        text = formatCompactCount(payload.grass),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = apple.primaryText,
                    )
                }
            }
        }

        items(payload.facilities, key = { it.id }) { fac ->
            val kindLabel = when {
                fac.hasFeed -> "饲料设施"
                fac.hasRanch -> "饲养栏舍"
                else -> "牧场设施"
            }
            val meterLabel: String
            val meterValue: String
            val fraction: Float
            when {
                fac.hasRanch -> {
                    meterLabel = "动物容量"
                    meterValue = "${fac.ranchCount} / ${fac.ranchCapacity}"
                    fraction = if (fac.ranchCapacity > 0) (fac.ranchCount.toFloat() / fac.ranchCapacity).coerceIn(0f, 1f) else 0f
                }
                fac.hasFeed -> {
                    meterLabel = "牧草槽容量"
                    meterValue = "${fac.feedCount} / ${fac.capacity}"
                    fraction = if (fac.capacity > 0) (fac.feedCount.toFloat() / fac.capacity).coerceIn(0f, 1f) else 0f
                }
                else -> {
                    meterLabel = "设施容量"
                    meterValue = fac.capacity.toString()
                    fraction = 0f
                }
            }
            val upgradeDone = fac.upgradeState == 2 || (fac.upgradeState != 0 && fac.upgradeEndTimeMs <= serverNowMs)

            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppleIconBadge(
                        icon = if (fac.hasFeed) Icons.Rounded.Grass else Icons.Rounded.Warehouse,
                        tint = if (fac.hasFeed) apple.green else apple.orange,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${fac.name}  Lv.${fac.level}", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                        Text(kindLabel, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                    }
                    if (fac.state == 2) {
                        AppleStatusPill("待建造", apple.secondaryText, showDot = false)
                    } else if (fac.upgradeState != 0) {
                        AppleStatusPill(if (upgradeDone) "可完成" else "升级中", apple.orange)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(meterLabel, style = MaterialTheme.typography.labelMedium, color = apple.secondaryText)
                    Text(
                        meterValue,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        color = apple.primaryText,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = apple.orange,
                    trackColor = apple.track,
                    strokeCap = StrokeCap.Round,
                )

                if (fac.hasFeed) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (fac.feedRemainingSec < 0L) "当前没有动物消耗牧草" else "预计可供 ${formatDurationSec(fac.feedRemainingSec)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = apple.elevated,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        when {
                            !fac.active && fac.moduleType == 7 -> {
                                Text(
                                    text = when {
                                        fac.canActivate -> "已满足开启条件，点击下方解锁"
                                        payload.level < 21 -> "牧场 21 级解锁"
                                        else -> "等待牧场开放"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (fac.canActivate) apple.orange else apple.secondaryText,
                                )
                            }
                            fac.active && fac.upgradeState != 0 -> {
                                val remSec = ((fac.upgradeEndTimeMs - serverNowMs) / 1000L).coerceAtLeast(0L)
                                Text(
                                    text = if (upgradeDone) "升级已完成，等待确认完成" else "升级剩余 ${formatDurationSec(remSec)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = apple.orange,
                                )
                            }
                            fac.active && fac.next != null -> {
                                val nx = fac.next
                                Text(
                                    text = "下一级 Lv.${nx.level} · 容量 ${nx.capacity} · 耗时 ${formatDurationSec(nx.durationSec)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.primaryText,
                                )
                                if (nx.requiredLevel > 0 && payload.level < nx.requiredLevel) {
                                    Text(
                                        text = "需要牧场 Lv.${nx.requiredLevel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = apple.orange,
                                    )
                                }
                                if (nx.costs.isNotEmpty()) {
                                    Text(
                                        text = "消耗：" + nx.costs.joinToString(" · ") { "${it.name} ×${formatCompactCount(it.count)}" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = apple.secondaryText,
                                    )
                                }
                            }
                            fac.active -> {
                                Text("已满级", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                            }
                        }
                    }
                }

                if (fac.canActivate || (fac.active && (fac.hasFeed || fac.upgradeState != 0 || fac.next != null))) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        if (fac.canActivate) {
                            Button(
                                onClick = { onActivateShed(fac) },
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("解锁") }
                        } else if (fac.active) {
                            if (fac.hasFeed) {
                                OutlinedButton(
                                    onClick = { onAddGrass(fac) },
                                    enabled = !busy && fac.feedCount < fac.capacity && payload.grass > 0L,
                                    shape = RoundedCornerShape(10.dp),
                                ) { Text("补充牧草") }
                            }
                            if (fac.upgradeState != 0) {
                                Button(
                                    onClick = { onCompleteUpgrade(fac) },
                                    enabled = !busy && upgradeDone,
                                    shape = RoundedCornerShape(10.dp),
                                ) { Text("完成升级") }
                            } else if (fac.next != null) {
                                Button(
                                    onClick = { onUpgrade(fac) },
                                    enabled = !busy && fac.next.available,
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("升级")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RanchBagTab(
    snapshot: RanchViewSnapshot?,
    busy: Boolean,
    onSellAllProducts: () -> Unit,
    onItemAction: (RanchBagItem, String) -> Unit,
) {
    val apple = LocalAppleColors.current
    if (snapshot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                AppleEmptyState(icon = Icons.Rounded.Inventory2, title = "正在读取牧场背包...", subtitle = "下拉可刷新库存")
            }
        }
        return
    }

    var filterIdx by remember { mutableIntStateOf(0) }
    val items = snapshot.bag

    fun catOf(item: RanchBagItem): Int = when {
        item.isProduct -> 1
        item.animalId > 0 && item.husbandryType != "棚" -> 2
        item.animalId > 0 && item.husbandryType == "棚" -> 3
        item.isCan -> 4
        else -> 5
    }

    val filtered = remember(items, filterIdx) {
        val sorted = items.sortedWith(
            compareBy<RanchBagItem> { catOf(it) }
                .thenByDescending { it.animalId }
                .thenBy { it.id }
                .thenBy { it.uid }
        )
        if (filterIdx == 0) sorted else sorted.filter { catOf(it) == filterIdx }
    }
    val canSellProducts = remember(items) {
        items.any { it.canSell && (it.type == 106 || it.type == 107) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cats = listOf("全部", "产物", "窝幼崽", "棚幼崽", "罐头", "其他")
                    cats.forEachIndexed { idx, label ->
                        item {
                            val count = if (idx == 0) items.size else items.count { catOf(it) == idx }
                            AppleFilterChip(
                                selected = filterIdx == idx,
                                onClick = { filterIdx = idx },
                                label = { Text("$label ($count)") },
                            )
                        }
                    }
                }
                if (canSellProducts) {
                    OutlinedButton(
                        onClick = onSellAllProducts,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("出售全部可售动物产物")
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Inventory2,
                        title = if (items.isEmpty()) "牧场背包是空的" else "该分类暂无物品",
                        subtitle = "在牧场商店购买幼崽或罐头后会显示在这里",
                    )
                }
            }
        } else {
            item {
                AppleGroupedCard {
                    filtered.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(46.dp)) {
                                RemoteAssetImage(
                                    url = item.image,
                                    contentDescription = item.name,
                                    fallbackIcon = Icons.Rounded.Inventory2,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(apple.elevated)
                                        .padding(4.dp),
                                )
                                if (item.locked) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(apple.orange),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Lock, contentDescription = "已锁定", tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = apple.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (item.giantCub) AppleStatusPill("巨大", apple.orange, showDot = false)
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = "×${formatCompactCount(item.count)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFeatureSettings = "tnum",
                                        ),
                                        color = apple.primaryText,
                                    )
                                    if (item.weightX10 > 0) {
                                        val kg = String.format(java.util.Locale.US, "%.2f", item.weightX10 / 100.0)
                                            .trimEnd('0').trimEnd('.') + "kg"
                                        Text(kg, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                                    }
                                    if (item.sellPrices.isNotEmpty()) {
                                        Text(
                                            text = "单价 ${formatCompactCount(item.sellPrices[0].count)}${item.sellPrices[0].name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = apple.secondaryText,
                                        )
                                    }
                                }
                            }
                            if (!item.locked) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    if (item.isCan) {
                                        Button(
                                            onClick = { onItemAction(item, "useCan") },
                                            enabled = !busy,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                        ) { Text("添加", style = MaterialTheme.typography.labelSmall) }
                                    } else if (item.animalId > 0) {
                                        Button(
                                            onClick = { onItemAction(item, "place") },
                                            enabled = !busy,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                        ) { Text("放养", style = MaterialTheme.typography.labelSmall) }
                                    }
                                    if (item.canSell) {
                                        OutlinedButton(
                                            onClick = { onItemAction(item, "sell") },
                                            enabled = !busy,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                        ) { Text("出售", style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                        }
                        if (idx < filtered.lastIndex) {
                            AppleHairlineDivider(startIndent = 72.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RanchTasksTab(
    snapshot: RanchViewSnapshot?,
    session: SessionUiState,
    busy: Boolean,
    onClaimTasks: () -> Unit,
) {
    val apple = LocalAppleColors.current
    var subMode by remember { mutableIntStateOf(0) }
    val tasks = snapshot?.tasks.orEmpty()
    val dailyTasks = remember(tasks) { tasks.filter { it.category == "daily" } }
    val growthTasks = remember(tasks) { tasks.filter { it.category != "daily" } }
    val claimableCount = remember(tasks) { tasks.count { it.canClaim } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppleSegmentedControl(
                options = listOf("每日礼包与任务", "成长任务 (${growthTasks.count { it.claimed }}/${growthTasks.size})"),
                selectedIndex = subMode,
                onSelect = { subMode = it },
            )
        }

        if (claimableCount > 0) {
            item {
                Button(
                    onClick = onClaimTasks,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("一键领取已完成任务奖励 ($claimableCount)")
                }
            }
        }

        if (subMode == 0) {
            val sharedGifts = session.dailyGifts?.gifts.orEmpty().filter {
                it.key in listOf("email_rewards", "vip_daily_gift", "month_card_gift")
            }
            val dailyClaimed = dailyTasks.count { it.claimed }
            item {
                AppleGroupedCard {
                    RanchDailyRow(
                        title = "牧场每日任务",
                        subtitle = if (dailyTasks.isNotEmpty()) "已完成 $dailyClaimed/${dailyTasks.size}" else "暂无数据",
                        done = dailyTasks.isNotEmpty() && dailyClaimed == dailyTasks.size,
                    )
                    AppleHairlineDivider(startIndent = 60.dp)
                    RanchDailyRow(
                        title = "牧场每日分享礼包",
                        subtitle = if (snapshot?.base?.dailyShareClaimed == true) "今日已领取" else "待检查或自动领取",
                        done = snapshot?.base?.dailyShareClaimed == true,
                    )
                    AppleHairlineDivider(startIndent = 60.dp)
                    RanchDailyRow(
                        title = "牧场每日福利",
                        subtitle = if (snapshot?.base?.dailyBenefitClaimed == true) "今日已领取" else "待检查或自动领取",
                        done = snapshot?.base?.dailyBenefitClaimed == true,
                    )
                    sharedGifts.forEach { gift ->
                        AppleHairlineDivider(startIndent = 60.dp)
                        RanchDailyRow(
                            title = gift.label.ifBlank { gift.key },
                            subtitle = if (gift.doneToday) "今日已完成" else if (gift.enabled) "等待执行" else "未开启",
                            done = gift.doneToday,
                        )
                    }
                }
            }
            if (dailyTasks.isNotEmpty()) {
                item { AppleSectionHeader(title = "每日任务明细") }
                item {
                    AppleGroupedCard {
                        dailyTasks.forEachIndexed { idx, task ->
                            RanchTaskDetailRow(task = task)
                            if (idx < dailyTasks.lastIndex) AppleHairlineDivider(startIndent = 16.dp)
                        }
                    }
                }
            }
        } else {
            if (growthTasks.isEmpty()) {
                item {
                    AppleGroupedCard {
                        AppleEmptyState(icon = Icons.Rounded.TaskAlt, title = "暂无成长任务", subtitle = "牧场成长任务会在达到对应条件后展示")
                    }
                }
            } else {
                item {
                    AppleGroupedCard {
                        growthTasks.forEachIndexed { idx, task ->
                            RanchTaskDetailRow(task = task)
                            if (idx < growthTasks.lastIndex) AppleHairlineDivider(startIndent = 16.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RanchDailyRow(title: String, subtitle: String, done: Boolean) {
    val apple = LocalAppleColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleIconBadge(icon = Icons.Rounded.CardGiftcard, tint = if (done) apple.green else apple.orange)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = apple.primaryText)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
        }
        AppleStatusPill(if (done) "已完成" else "进行中", if (done) apple.green else apple.blue, showDot = false)
    }
}

@Composable
private fun RanchTaskDetailRow(task: com.qqfarm.mobile.data.model.RanchTaskItem) {
    val apple = LocalAppleColors.current
    val (statusLabel, statusColor) = when {
        task.claimed -> "已领取" to apple.green
        task.canClaim -> "可领取" to apple.orange
        !task.unlocked -> "未解锁" to apple.secondaryText
        else -> "${task.progress}/${task.total}" to apple.blue
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = task.desc,
            style = MaterialTheme.typography.bodyMedium,
            color = apple.primaryText,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        AppleStatusPill(statusLabel, statusColor, showDot = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RanchBookTab(
    snapshot: RanchViewSnapshot?,
    busy: Boolean,
    onClaimIllustrated: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val book: RanchBookData? = snapshot?.book
    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                AppleEmptyState(icon = Icons.Rounded.MenuBook, title = "正在读取动物图鉴...", subtitle = "下拉可刷新图鉴收藏进度")
            }
        }
        return
    }

    var kindIdx by remember { mutableIntStateOf(0) }
    val isGiant = kindIdx == 1
    var filterIdx by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var showGiantLevels by remember { mutableStateOf(false) }

    val list: List<RanchBookAnimal> = remember(book, isGiant) {
        if (isGiant) book.animals.filter { it.hasGiant } else book.animals
    }
    val ownedFun: (RanchBookAnimal) -> Boolean = { a -> if (isGiant) a.giantUnlocked else a.unlocked }
    val ownedCount = list.count(ownedFun)
    val claimable = if (isGiant) book.giantLevels.any { it.canClaim } else book.normalCanClaim

    val filtered = remember(list, isGiant, filterIdx, search) {
        val kw = search.trim()
        list.filter { entry ->
            val name = if (isGiant) entry.giantName.ifBlank { entry.name } else entry.name
            val matchesKw = kw.isEmpty() || name.contains(kw, ignoreCase = true)
            val owned = ownedFun(entry)
            val matchesFilter = when (filterIdx) {
                1 -> owned
                2 -> !owned
                else -> true
            }
            matchesKw && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppleSegmentedControl(
                options = listOf("动物图鉴", "巨大图鉴"),
                selectedIndex = kindIdx,
                onSelect = {
                    kindIdx = it
                    filterIdx = 0
                },
            )
        }

        item {
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = if (isGiant) "巨大图鉴总分 ${formatCompactCount(book.giantScore)}" else "图鉴等级 Lv.${book.normalLevel}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            color = apple.primaryText,
                        )
                        Text(
                            text = "已收集 $ownedCount / ${list.size}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = apple.secondaryText,
                        )
                    }
                    if (isGiant) {
                        TextButton(onClick = { showGiantLevels = true }) {
                            Text("体重奖励")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val nextTarget = if (isGiant) {
                    book.giantLevels.firstOrNull { !it.claimed && !it.canClaim }?.score ?: book.giantScore
                } else {
                    book.normalNextProgress.toLong()
                }
                val currentProg = if (isGiant) book.giantScore else book.normalProgress.toLong()
                val fraction = if (nextTarget > 0L) (currentProg.toFloat() / nextTarget.toFloat()).coerceIn(0f, 1f) else 1f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (isGiant) apple.orange else apple.emerald,
                    trackColor = apple.track,
                    strokeCap = StrokeCap.Round,
                )

                if (claimable) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onClaimIllustrated(if (isGiant) "giant" else "normal") },
                        enabled = !busy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("领取图鉴奖励") }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("全部 (${list.size})", "已收集 ($ownedCount)", "未收集 (${list.size - ownedCount})").forEachIndexed { idx, label ->
                        AppleFilterChip(
                            selected = filterIdx == idx,
                            onClick = { filterIdx = idx },
                            label = { Text(label) },
                        )
                    }
                }
                AppleSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "搜索动物名称",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val rows = filtered.chunked(2)
        items(rows) { rowItems ->
            EqualHeightRow {
                rowItems.forEach { entry ->
                    val owned = ownedFun(entry)
                    val title = if (isGiant) entry.giantName.ifBlank { "巨大${entry.name}" } else entry.name
                    val img = if (isGiant) entry.giantImage.ifBlank { entry.image } else entry.image
                    val meta = if (!isGiant) {
                        if (entry.unlocked) "已收集" else "未收集"
                    } else if (!entry.giantUnlocked) {
                        if (entry.giantHarvestCount.isNotBlank() && entry.giantHarvestCount != "0") "已收获 ${entry.giantHarvestCount} 次" else "未收集"
                    } else {
                        listOf(entry.giantTierName, entry.giantWeightKg).filter { it.isNotBlank() }.joinToString(" · ")
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .alpha(if (owned) 1f else 0.58f),
                        shape = RoundedCornerShape(14.dp),
                        color = apple.card,
                        border = BorderStroke(
                            if (isGiant && entry.giantIsNew) 1.dp else 0.5.dp,
                            if (isGiant && entry.giantIsNew) apple.orange else apple.separator,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                RemoteAssetImage(
                                    url = img,
                                    contentDescription = title,
                                    fallbackIcon = Icons.Rounded.Pets,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .align(Alignment.Center),
                                )
                                if (isGiant && entry.giantIsNew) {
                                    AppleStatusPill("新", apple.orange, showDot = false, modifier = Modifier.align(Alignment.TopEnd))
                                }
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = apple.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGiant && owned) apple.orange else apple.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    if (showGiantLevels) {
        ModalBottomSheet(
            onDismissRequest = { showGiantLevels = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = apple.card,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("巨大图鉴体重奖励", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                book.giantLevels.forEach { lv ->
                    val statusText = when {
                        lv.claimed -> "已领取"
                        lv.canClaim -> "可领取"
                        else -> "未达成"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = apple.elevated,
                        border = BorderStroke(0.5.dp, apple.separator),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("总分 ${formatCompactCount(lv.score)}", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                Text(
                                    lv.rewards.joinToString(" · ") { "${it.name} ×${formatCompactCount(it.count)}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.secondaryText,
                                )
                            }
                            AppleStatusPill(
                                text = statusText,
                                color = if (lv.canClaim) apple.orange else if (lv.claimed) apple.green else apple.secondaryText,
                                showDot = false,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RanchPillBtn(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AppleActionPill(
        label = label,
        primary = primary,
        enabled = enabled,
        accentColor = LocalAppleColors.current.orange,
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RanchWorkspaceFeedSheet(
    targetAnimal: RanchAnimal?,
    onFeed: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bucketIndex by remember { mutableIntStateOf(0) }
    var produceAfterFeed by remember { mutableStateOf(true) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (targetAnimal != null) "喂养 ${targetAnimal.name}" else "一键喂养全部动物",
                style = MaterialTheme.typography.titleLarge,
                color = apple.primaryText,
            )
            AppleSegmentedControl(
                options = listOf("普通罐头", "有机罐头"),
                selectedIndex = bucketIndex,
                onSelect = { bucketIndex = it },
            )
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("喂养后自动开始生产", style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                    AppleSwitch(
                        checked = produceAfterFeed,
                        onCheckedChange = { produceAfterFeed = it },
                        checkedTrackColor = apple.green,
                    )
                }
            }
            Button(
                onClick = { onFeed(if (bucketIndex == 1) 1 else 2, produceAfterFeed) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("确认喂养")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RanchAmountPickerSheet(
    config: AmountPromptConfig,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var count by remember(config) { mutableIntStateOf(1) }
    var selectedPenId by remember(config) {
        mutableStateOf(
            config.pens.firstOrNull { it.ranchCount < it.ranchCapacity }?.id
                ?: config.pens.firstOrNull()?.id.orEmpty()
        )
    }
    val selectedPen = config.pens.firstOrNull { it.id == selectedPenId }
    val effectiveMax = if (config.pens.isNotEmpty() && selectedPen != null) {
        config.maxCount.coerceAtMost((selectedPen.ranchCapacity - selectedPen.ranchCount).coerceAtLeast(1)).coerceAtLeast(1)
    } else {
        config.maxCount.coerceAtLeast(1)
    }
    LaunchedEffect(effectiveMax) {
        if (count > effectiveMax) count = effectiveMax
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(config.title, style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RemoteAssetImage(
                    url = config.itemImage,
                    contentDescription = config.itemName,
                    fallbackIcon = Icons.Rounded.Inventory2,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(apple.elevated)
                        .padding(4.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(config.itemName, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                    Text(config.caption, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                }
            }

            if (config.pens.isNotEmpty()) {
                Text("放养设施", style = MaterialTheme.typography.labelLarge, color = apple.secondaryText)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(config.pens) { pen ->
                        val full = pen.ranchCount >= pen.ranchCapacity
                        AppleFilterChip(
                            selected = selectedPenId == pen.id,
                            onClick = { if (!full) selectedPenId = pen.id },
                            enabled = !full,
                            label = { Text("${pen.name} (${pen.ranchCount}/${pen.ranchCapacity})") },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("选择数量", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                AppleQuantityStepper(
                    value = count,
                    max = effectiveMax,
                    onChange = { count = it },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) { Text("取消") }
                Button(
                    onClick = { onSubmit(count, selectedPenId) },
                    enabled = config.pens.isEmpty() || selectedPenId.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) { Text(config.submitLabel) }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
