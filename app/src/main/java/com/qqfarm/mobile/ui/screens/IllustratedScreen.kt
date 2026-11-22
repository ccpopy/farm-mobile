package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qqfarm.mobile.data.model.CollectionItemEntry
import com.qqfarm.mobile.data.model.CollectionMilestone
import com.qqfarm.mobile.data.model.CollectionMutationRule
import com.qqfarm.mobile.data.model.CollectionSeries
import com.qqfarm.mobile.data.model.CollectionSnapshot
import com.qqfarm.mobile.data.model.CommerceItem
import com.qqfarm.mobile.data.model.IllustratedBook
import com.qqfarm.mobile.data.model.IllustratedBookItem
import com.qqfarm.mobile.data.model.IllustratedLevel
import com.qqfarm.mobile.data.model.IllustratedSnapshot
import com.qqfarm.mobile.data.model.MutantCatalogEffect
import com.qqfarm.mobile.data.model.MutationCatalogCollectionSeries
import com.qqfarm.mobile.data.model.MutationCatalogSnapshot
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleProgressBar
import com.qqfarm.mobile.ui.components.AppleQuantityStepper
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.searchHeader
import com.qqfarm.mobile.ui.components.AppleSearchField
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.AppleTabStrip
import com.qqfarm.mobile.ui.components.RemoteThumbImage
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.formatNumber
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs
import com.qqfarm.mobile.ui.components.AppleAlertDialog
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.ui.theme.onAccent
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val MUTANT_GROUPS = listOf(
    "gold" to "黄金果实",
    "decoration" to "装扮果实",
    "activity" to "活动果实",
)

private val RARITY_LABELS = listOf("", "普通", "稀有", "珍品", "天工")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IllustratedScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    onRefreshOverview: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var illustrated by remember(accountId, isConnected) { mutableStateOf<IllustratedSnapshot?>(null) }
    var illustratedLoading by remember(accountId, isConnected) { mutableStateOf(false) }
    var illustratedError by remember(accountId, isConnected) { mutableStateOf("") }

    var collections by remember(accountId, isConnected) { mutableStateOf<CollectionSnapshot?>(null) }
    var collectionsLoading by remember(accountId, isConnected) { mutableStateOf(false) }
    var collectionsError by remember(accountId, isConnected) { mutableStateOf("") }

    var mutationCatalog by remember { mutableStateOf<MutationCatalogSnapshot?>(null) }
    var catalogLoading by remember { mutableStateOf(false) }
    var catalogError by remember { mutableStateOf("") }

    var actionBusy by remember { mutableStateOf(false) }
    var selectedCropItem by remember { mutableStateOf<IllustratedBookItem?>(null) }
    var rewardsBookType by remember { mutableStateOf<Int?>(null) }
    var selectedCollectionItemId by remember { mutableStateOf<Int?>(null) }
    var showCollectionMilestones by remember { mutableStateOf(false) }
    var upgradeConfirmItem by remember { mutableStateOf<CollectionItemEntry?>(null) }
    var rulesDialogSeries by remember { mutableStateOf<Pair<String, List<CollectionMutationRule>>?>(null) }

    suspend fun loadIllustrated() {
        if (!isConnected || illustratedLoading) return
        illustratedLoading = true
        illustratedError = ""
        try {
            illustrated = apiClient.fetchIllustrated(accountId)
        } catch (e: ApiException) {
            illustrated = null
            selectedCropItem = null
            rewardsBookType = null
            illustratedError = e.message
        } finally {
            illustratedLoading = false
        }
    }

    suspend fun loadCollections() {
        if (!isConnected || collectionsLoading) return
        collectionsLoading = true
        collectionsError = ""
        try {
            collections = apiClient.fetchCollections(accountId)
        } catch (e: ApiException) {
            collections = null
            selectedCollectionItemId = null
            showCollectionMilestones = false
            upgradeConfirmItem = null
            collectionsError = e.message
        } finally {
            collectionsLoading = false
        }
    }

    suspend fun loadMutationCatalog() {
        if (catalogLoading) return
        catalogLoading = true
        catalogError = ""
        try {
            mutationCatalog = apiClient.fetchMutationCatalog()
        } catch (e: ApiException) {
            catalogError = e.message
        } finally {
            catalogLoading = false
        }
    }

    LaunchedEffect(isConnected) {
        selectedCropItem = null
        rewardsBookType = null
        selectedCollectionItemId = null
        showCollectionMilestones = false
        upgradeConfirmItem = null
        if (isConnected) {
            launch { loadIllustrated() }
            launch { loadCollections() }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3 && mutationCatalog == null && catalogError.isEmpty()) {
            loadMutationCatalog()
        }
    }

    fun runBookAction(refresh: suspend () -> Unit, action: suspend () -> String) {
        if (!isConnected || actionBusy) return
        actionBusy = true
        scope.launch {
            try {
                val message = action()
                onRefreshOverview()
                onMessage(message)
            } catch (e: ApiException) {
                selectedCropItem = null
                rewardsBookType = null
                selectedCollectionItemId = null
                showCollectionMilestones = false
                upgradeConfirmItem = null
                onMessage(e.message)
                refresh()
            } finally {
                actionBusy = false
            }
        }
    }

    fun claimBookRewards(type: Int) {
        if (illustratedLoading) return
        runBookAction(::loadIllustrated) {
            val result = apiClient.claimIllustrated(accountId, type)
            if (result.snapshot != null) {
                illustrated = result.snapshot
            } else {
                loadIllustrated()
            }
            result.message
        }
    }

    fun purchaseCropSeed(item: IllustratedBookItem, count: Int) {
        val seed = item.shopSeed ?: return
        if (illustratedLoading) return
        runBookAction(::loadIllustrated) {
            val received = apiClient.purchaseIllustratedSeed(accountId, seed.goodsId, seed.seedId, count)
            selectedCropItem = null
            loadIllustrated()
            "已购入 ${item.name} ×${formatNumber(received)} 颗"
        }
    }

    fun operateCollection(action: String, body: JSONObject, successLabel: String) {
        if (collectionsLoading) return
        runBookAction(::loadCollections) {
            val result = apiClient.operateCollections(accountId, action, body, successLabel)
            collections = result.snapshot
            upgradeConfirmItem = null
            result.message
        }
    }

    val cropClaimable = illustrated?.crop?.claimable == true
    val mutantClaimable = illustrated?.mutant?.claimable == true
    val colClaimableCount = remember(collections) {
        collections?.milestones?.count { it.reached && !it.claimed } ?: 0
    }
    val hasOpenSheet = selectedCropItem != null || rewardsBookType != null ||
        selectedCollectionItemId != null || showCollectionMilestones ||
        upgradeConfirmItem != null || rulesDialogSeries != null

    val tabOptions = listOf(
        if (cropClaimable) "作物图鉴 ●" else "作物图鉴",
        if (mutantClaimable) "超变图鉴 ●" else "超变图鉴",
        if (colClaimableCount > 0) "蓝晶藏品 ●" else "蓝晶藏品",
        "变异宝典",
        "牧场图鉴",
    )

    val refreshing = when (selectedTab) {
        2 -> collectionsLoading && collections != null
        3 -> catalogLoading && mutationCatalog != null
        else -> illustratedLoading && illustrated != null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        Surface(
            color = apple.background,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppleTabStrip(
                options = tabOptions,
                selectedIndex = selectedTab,
                onSelect = { if (!actionBusy) selectedTab = it },
                accentColor = when (selectedTab) {
                    1 -> apple.orange
                    2 -> apple.blue
                    3 -> apple.indigo
                    4 -> apple.orange
                    else -> apple.emerald
                },
            )
        }

        val tabContent = Modifier
            .fillMaxWidth()
            .weight(1f)
            .swipeToSwitchTabs(
                selectedIndex = selectedTab,
                count = tabOptions.size,
                enabled = !actionBusy && !hasOpenSheet,
            ) { selectedTab = it }
        if (selectedTab == 4) {
            Box(tabContent) {
                RanchBookPage(
                    accountId = accountId,
                    isConnected = isConnected,
                    apiClient = apiClient,
                    onBusyChange = { actionBusy = it },
                    onMessage = onMessage,
                    onRewardsClaimed = onRefreshOverview,
                )
            }
        } else PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                if (!actionBusy) scope.launch {
                    when (selectedTab) {
                        2 -> loadCollections()
                        3 -> loadMutationCatalog()
                        else -> loadIllustrated()
                    }
                }
            },
            modifier = tabContent,
        ) {
            when (selectedTab) {
                0, 1 -> {
                    val isMutant = selectedTab == 1
                    val otherClaimTarget: Pair<Int, String>? = when {
                        selectedTab == 0 && mutantClaimable -> 1 to "超变图鉴有可领取的收藏奖励，点此切换领取"
                        selectedTab == 0 && colClaimableCount > 0 -> 2 to "蓝晶藏品有 $colClaimableCount 档可领取的评分奖励，点此切换领取"
                        selectedTab == 1 && cropClaimable -> 0 to "作物图鉴有可领取的收藏奖励，点此切换领取"
                        selectedTab == 1 && colClaimableCount > 0 -> 2 to "蓝晶藏品有 $colClaimableCount 档可领取的评分奖励，点此切换领取"
                        else -> null
                    }
                    when {
                        accountId.isBlank() -> BookCenteredCard {
                            AppleEmptyState(icon = Icons.Rounded.MenuBook, title = "请先选择账号", subtitle = "在右上角选择运行中的农场账号后查看图鉴")
                        }
                        !isConnected -> BookCenteredCard {
                            AppleEmptyState(icon = Icons.Rounded.MenuBook, title = "账号未运行", subtitle = "启动当前账号后即可查看图鉴进度与领取奖励")
                        }
                        illustrated == null && illustratedError.isBlank() -> AppleLoadingState()
                        illustratedError.isNotBlank() && illustrated == null -> BookCenteredCard {
                            AppleEmptyState(
                                icon = Icons.Rounded.MenuBook,
                                title = "图鉴读取失败",
                                subtitle = illustratedError,
                                actionLabel = "重新读取",
                                onAction = { scope.launch { loadIllustrated() } },
                            )
                        }
                        else -> {
                            val book = if (isMutant) illustrated?.mutant else illustrated?.crop
                            if (book != null) {
                                CropOrMutantBookTab(
                                    book = book,
                                    isMutant = isMutant,
                                    isConnected = isConnected,
                                    busy = actionBusy || illustratedLoading,
                                    crossTabNotice = otherClaimTarget,
                                    onJumpTab = { selectedTab = it },
                                    onClaimRewards = { claimBookRewards(book.type) },
                                    onOpenRewardsSheet = { rewardsBookType = book.type },
                                    onSelectItem = { item ->
                                        if (!isMutant) selectedCropItem = item
                                    },
                                )
                            }
                        }
                    }
                }

                2 -> {
                    val otherNotice: Pair<Int, String>? = when {
                        cropClaimable -> 0 to "作物图鉴有可领取的收藏奖励，点此切换领取"
                        mutantClaimable -> 1 to "超变图鉴有可领取的收藏奖励，点此切换领取"
                        else -> null
                    }
                    when {
                        accountId.isBlank() -> BookCenteredCard {
                            AppleEmptyState(icon = Icons.Rounded.Stars, title = "请先选择账号", subtitle = "选择账号后查看蓝晶藏品与评分奖励")
                        }
                        !isConnected -> BookCenteredCard {
                            AppleEmptyState(icon = Icons.Rounded.Stars, title = "账号未运行", subtitle = "启动当前账号后即可管理蓝晶藏品与领奖")
                        }
                        collections == null && collectionsError.isBlank() -> AppleLoadingState()
                        collectionsError.isNotBlank() && collections == null -> BookCenteredCard {
                            AppleEmptyState(
                                icon = Icons.Rounded.Stars,
                                title = "蓝晶藏品读取失败",
                                subtitle = collectionsError,
                                actionLabel = "重新读取",
                                onAction = { scope.launch { loadCollections() } },
                            )
                        }
                        collections != null -> {
                            CollectionsBookTab(
                                snapshot = collections!!,
                                busy = actionBusy || collectionsLoading,
                                crossTabNotice = otherNotice,
                                onJumpTab = { selectedTab = it },
                                onClaimMilestones = {
                                    operateCollection(
                                        action = "claim",
                                        body = JSONObject(),
                                        successLabel = "已领取全部达标评分奖励",
                                    )
                                },
                                onOpenMilestonesSheet = { showCollectionMilestones = true },
                                onShowRules = { title, rules -> rulesDialogSeries = title to rules },
                                onSelectItem = { entry -> selectedCollectionItemId = entry.id },
                            )
                        }
                    }
                }

                else -> {
                    when {
                        mutationCatalog == null && catalogError.isBlank() -> AppleLoadingState()
                        catalogError.isNotBlank() && mutationCatalog == null -> BookCenteredCard {
                            AppleEmptyState(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "变异宝典读取失败",
                                subtitle = catalogError,
                                actionLabel = "重新读取",
                                onAction = { scope.launch { loadMutationCatalog() } },
                            )
                        }
                        mutationCatalog != null -> {
                            MutationCatalogTab(
                                catalog = mutationCatalog!!,
                                onJumpToCollections = { selectedTab = 2 },
                                onShowRules = { title, rules -> rulesDialogSeries = title to rules },
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Sheets & Dialogs ----

    val activeBookForRewards = when (rewardsBookType) {
        1 -> illustrated?.crop
        2 -> illustrated?.mutant
        else -> null
    }
    if (activeBookForRewards != null) {
        IllustratedRewardsSheet(
            book = activeBookForRewards,
            busy = actionBusy || illustratedLoading,
            onClaim = { claimBookRewards(activeBookForRewards.type) },
            onDismiss = { rewardsBookType = null },
        )
    }

    selectedCropItem?.let { currentSelected ->
        val freshItem = illustrated?.crop?.items?.firstOrNull { it.seedId == currentSelected.seedId } ?: currentSelected
        CropItemDetailSheet(
            item = freshItem,
            goldBalance = session.status?.gold ?: 0L,
            isConnected = isConnected,
            busy = actionBusy || illustratedLoading,
            onPurchase = { count -> purchaseCropSeed(freshItem, count) },
            onDismiss = { selectedCropItem = null },
        )
    }

    val currentCollectionSnap = collections
    if (showCollectionMilestones && currentCollectionSnap != null) {
        CollectionMilestonesSheet(
            snapshot = currentCollectionSnap,
            busy = actionBusy || collectionsLoading,
            onClaimAll = {
                operateCollection(
                    action = "claim",
                    body = JSONObject(),
                    successLabel = "已领取全部达标评分奖励",
                )
            },
            onDismiss = { showCollectionMilestones = false },
        )
    }

    val selectedColId = selectedCollectionItemId
    if (selectedColId != null && currentCollectionSnap != null) {
        val pair = currentCollectionSnap.series.firstNotNullOfOrNull { s ->
            s.items.firstOrNull { it.id == selectedColId }?.let { s to it }
        }
        if (pair != null) {
            val (series, colItem) = pair
            CollectionItemDetailSheet(
                series = series,
                item = colItem,
                busy = actionBusy || collectionsLoading,
                onRequestUpgrade = { upgradeConfirmItem = colItem },
                onToggleDisplay = {
                    val body = JSONObject()
                        .put("collectionId", colItem.id)
                        .put("displayed", !colItem.displayed)
                        .put("expectedDisplayItemId", currentCollectionSnap.displayItemId)
                    operateCollection(
                        action = "display",
                        body = body,
                        successLabel = if (colItem.displayed) "已取消展示" else "正在展示 ${colItem.name}",
                    )
                },
                onDismiss = { selectedCollectionItemId = null },
            )
        }
    }

    upgradeConfirmItem?.let { item ->
        val costText = item.nextStarCosts.joinToString("、") { "${it.name} ×${formatNumber(it.count)}" }
        AppleAlertDialog(
            onDismissRequest = { if (!actionBusy) upgradeConfirmItem = null },
            title = { Text(if (item.activated) "确认升星" else "确认激活") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${item.name} → ${item.starLevel + 1} 星")
                    if (costText.isNotBlank()) {
                        Text("消耗：$costText", color = apple.secondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !actionBusy && !collectionsLoading,
                    onClick = {
                        val costsArr = JSONArray().apply {
                            item.nextStarCosts.forEach { cost ->
                                put(JSONObject().put("itemId", cost.id).put("count", cost.count))
                            }
                        }
                        val body = JSONObject()
                            .put("collectionId", item.id)
                            .put("expectedStarLevel", item.starLevel)
                            .put("expectedCosts", costsArr)
                        operateCollection(
                            action = "upgrade",
                            body = body,
                            successLabel = if (item.activated) "${item.name} 已升至 ${item.starLevel + 1} 星" else "${item.name} 已激活",
                        )
                    },
                ) {
                    Text(if (actionBusy) "提交中..." else "确认")
                }
            },
            dismissButton = {
                TextButton(enabled = !actionBusy, onClick = { upgradeConfirmItem = null }) {
                    Text("取消")
                }
            },
        )
    }

    rulesDialogSeries?.let { (seriesTitle, rules) ->
        AppleAlertDialog(
            onDismissRequest = { rulesDialogSeries = null },
            title = { Text("$seriesTitle · 变异概率") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (rules.isEmpty()) {
                        Text("暂无概率规则说明", color = apple.secondaryText)
                    } else {
                        rules.forEach { rule ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(rule.name, style = MaterialTheme.typography.bodyMedium, color = apple.primaryText)
                                val probText = if (rule.guarantee > 0) {
                                    "${formatPercent(rule.probability)}% · ${rule.guarantee}次保底"
                                } else {
                                    "${formatPercent(rule.probability)}%"
                                }
                                Text(probText, style = MaterialTheme.typography.labelMedium, color = apple.blue)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { rulesDialogSeries = null }) { Text("知道了") }
            },
        )
    }
}

@Composable
private fun BookCenteredCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun CrossTabRewardNotice(
    message: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppleRadius.tile))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(AppleRadius.tile),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, accent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
    }
}

// ---- Crop & Mutant Books Tab ----

@Composable
private fun SearchableBookGrid(
    minTileWidth: Dp,
    vararg filters: Any,
    toolbar: @Composable ColumnScope.() -> Unit,
    content: LazyGridScope.() -> Unit,
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(*filters) { gridState.scrollToItem(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = toolbar,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minTileWidth),
            state = gridState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CropOrMutantBookTab(
    book: IllustratedBook,
    isMutant: Boolean,
    isConnected: Boolean,
    busy: Boolean,
    crossTabNotice: Pair<Int, String>?,
    onJumpTab: (Int) -> Unit,
    onClaimRewards: () -> Unit,
    onOpenRewardsSheet: () -> Unit,
    onSelectItem: (IllustratedBookItem) -> Unit,
) {
    val apple = LocalAppleColors.current
    val accent = if (isMutant) apple.orange else apple.emerald
    var filterKey by remember(isMutant) { mutableStateOf("all") }
    var groupKey by remember(isMutant) { mutableStateOf("all") }
    var search by remember(isMutant) { mutableStateOf("") }

    val items = book.items
    val unlockedCount = remember(items) { items.count { it.unlocked } }
    val buyableCount = remember(items, isConnected, isMutant) {
        if (isMutant) 0 else items.count { canPurchaseIllustratedSeed(it, isConnected) }
    }
    val claimableLevelsCount = remember(book.levels) {
        book.levels.count { it.canClaim && !it.claimed }
    }

    val filteredItems = remember(items, isMutant, isConnected, filterKey, groupKey, search) {
        val kw = search.trim()
        items.filter { item ->
            if (isMutant && groupKey != "all" && item.group != groupKey) return@filter false
            if (kw.isNotEmpty() && !item.name.contains(kw, ignoreCase = true)) return@filter false
            when (filterKey) {
                "owned" -> item.unlocked
                "missing" -> !item.unlocked
                "buyable" -> canPurchaseIllustratedSeed(item, isConnected)
                else -> true
            }
        }
    }

    val groupedMutantSections = remember(filteredItems, isMutant) {
        if (!isMutant) emptyList()
        else MUTANT_GROUPS.mapNotNull { (key, label) ->
            val groupAll = items.filter { it.group == key }
            val visible = filteredItems.filter { it.group == key }
            if (visible.isEmpty()) null else Triple(label, groupAll.count { it.unlocked } to groupAll.size, visible)
        }
    }

    SearchableBookGrid(
        84.dp, book.type, filterKey, groupKey, search,
        toolbar = {
            AppleSearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = if (isMutant) "搜索超变果实名称" else "搜索作物名称",
            )
            if (isMutant) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AppleFilterChip(
                            selected = groupKey == "all",
                            onClick = { groupKey = "all" },
                            accentColor = accent,
                            label = { Text("全部分类") },
                        )
                    }
                    MUTANT_GROUPS.forEach { (key, label) ->
                        item {
                            AppleFilterChip(
                                selected = groupKey == key,
                                onClick = { groupKey = key },
                                accentColor = accent,
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val stateChips = buildList {
                    add("all" to "全部 (${items.size})")
                    add("owned" to "已收集 ($unlockedCount)")
                    add("missing" to "未收集 (${items.size - unlockedCount})")
                    if (!isMutant) add("buyable" to "可购买 ($buyableCount)")
                }
                items(stateChips, key = { it.first }) { (key, label) ->
                    AppleFilterChip(
                        selected = filterKey == key,
                        onClick = { filterKey = key },
                        accentColor = accent,
                        label = { Text(label) },
                    )
                }
            }
        },
    ) {
        if (crossTabNotice != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "cross_notice_${book.type}") {
                CrossTabRewardNotice(
                    message = crossTabNotice.second,
                    accent = if (crossTabNotice.first == 2) apple.blue else apple.orange,
                    onClick = { onJumpTab(crossTabNotice.first) },
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "overview_${book.type}") {
            IllustratedOverviewCard(
                book = book,
                isMutant = isMutant,
                unlockedCount = unlockedCount,
                claimableCount = claimableLevelsCount,
                accent = accent,
                busy = busy,
                onClaim = onClaimRewards,
                onOpenDetails = onOpenRewardsSheet,
            )
        }

        if (filteredItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty_${book.type}") {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.MenuBook,
                        title = if (items.isEmpty()) "暂无图鉴记录" else "没有符合条件的条目",
                        subtitle = if (items.isEmpty()) "当前账号还没有收集记录" else "换个筛选条件或搜索词试试",
                    )
                }
            }
        } else if (isMutant) {
            groupedMutantSections.forEach { (sectionTitle, counts, sectionItems) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "sec_${sectionTitle}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(sectionTitle, style = MaterialTheme.typography.titleSmall, color = apple.secondaryText)
                        Text(
                            "${counts.first} / ${counts.second}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = apple.secondaryText,
                        )
                    }
                }
                items(
                    items = sectionItems,
                    key = { "mutant_${it.seedId}" },
                    contentType = { "book_tile" },
                ) { entry ->
                    IllustratedGridTile(
                        item = entry,
                        isMutant = true,
                        isConnected = isConnected,
                        accent = accent,
                        onClick = { onSelectItem(entry) },
                    )
                }
            }
        } else {
            items(
                items = filteredItems,
                key = { "crop_${it.seedId}" },
                contentType = { "book_tile" },
            ) { entry ->
                IllustratedGridTile(
                    item = entry,
                    isMutant = false,
                    isConnected = isConnected,
                    accent = accent,
                    onClick = { onSelectItem(entry) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IllustratedOverviewCard(
    book: IllustratedBook,
    isMutant: Boolean,
    unlockedCount: Int,
    claimableCount: Int,
    accent: Color,
    busy: Boolean,
    onClaim: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val nextProgress = book.nextLevelProgress
    val fraction = if (nextProgress > 0) (book.progress.toFloat() / nextProgress.toFloat()).coerceIn(0f, 1f) else 1f
    val remain = (nextProgress - book.progress).coerceAtLeast(0)
    val hintText = when {
        book.claimable -> if (claimableCount > 0) "有 $claimableCount 级图鉴奖励可领取" else "有可领取的收藏奖励"
        nextProgress > 0 && remain > 0 -> "距下一级还差 $remain 进度"
        else -> "已达到当前最高等级"
    }

    AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(AppleRadius.tile),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "${book.level}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = accent,
                    )
                    Text("等级", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = accent)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isMutant) "超变图鉴" else "作物图鉴",
                    style = MaterialTheme.typography.titleMedium,
                    color = apple.primaryText,
                )
                Text(
                    text = "已收集 $unlockedCount / ${book.items.size}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = apple.secondaryText,
                )
            }
            if (book.claimable) {
                AppleStatusPill("可领奖", accent, showDot = true)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        AppleProgressBar(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = accent,
            trackColor = apple.track,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(hintText, style = MaterialTheme.typography.labelMedium, color = if (book.claimable) accent else apple.secondaryText)
            Text(
                text = if (nextProgress > 0) "${book.progress} / $nextProgress" else "${book.progress}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = apple.primaryText,
            )
        }

        if (isMutant) {
            Spacer(modifier = Modifier.height(10.dp))
            if (book.currentBuffs.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    book.currentBuffs.forEach { buff ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accent.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = "${buff.name} · ${buff.formattedEffect}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            } else {
                Text("当前暂无属性加成，提升图鉴等级即可解锁", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (book.claimable) {
                Button(
                    onClick = onClaim,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = accent.onAccent()),
                    shape = RoundedCornerShape(AppleRadius.control),
                    modifier = Modifier.weight(1.35f),
                ) {
                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (claimableCount > 0) "一键领奖 ($claimableCount)" else "一键领取奖励")
                }
            }
            OutlinedButton(
                onClick = onOpenDetails,
                shape = RoundedCornerShape(AppleRadius.control),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isMutant) "加成与奖励" else "奖励详情")
            }
        }
    }
}

@Composable
internal fun SeedShopGridContent(
    items: List<IllustratedBookItem>,
    goldBalance: Long,
    isConnected: Boolean,
    onSelectItem: (IllustratedBookItem) -> Unit,
) {
    val apple = LocalAppleColors.current
    val accent = apple.emerald
    var filterKey by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }

    val buyableCount = remember(items, isConnected) {
        items.count { canPurchaseIllustratedSeed(it, isConnected) }
    }
    val filteredItems = remember(items, isConnected, filterKey, search) {
        val kw = search.trim()
        items.filter { item ->
            if (kw.isNotEmpty() && !item.name.contains(kw, ignoreCase = true)) return@filter false
            when (filterKey) {
                "buyable" -> canPurchaseIllustratedSeed(item, isConnected)
                else -> true
            }
        }
    }

    SearchableBookGrid(
        84.dp, filterKey, search,
        toolbar = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppleSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "搜索种子名称",
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = apple.card,
                    border = BorderStroke(0.5.dp, apple.separator),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Paid,
                            contentDescription = null,
                            tint = apple.orange,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "当前金币 ${formatCompactCount(goldBalance)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = apple.primaryText,
                        )
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val stateChips = listOf(
                    "all" to "全部 (${items.size})",
                    "buyable" to "可购买 ($buyableCount)",
                )
                items(stateChips, key = { it.first }) { (key, label) ->
                    AppleFilterChip(
                        selected = filterKey == key,
                        onClick = { filterKey = key },
                        accentColor = accent,
                        label = { Text(label) },
                    )
                }
            }
        },
    ) {
        if (filteredItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "seed_shop_empty") {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Eco,
                        title = if (items.isEmpty()) "暂无商店种子" else "没有符合条件的种子",
                        subtitle = if (items.isEmpty()) "当前商店没有可展示的种子" else "换个筛选条件或搜索词试试",
                    )
                }
            }
        } else {
            items(
                items = filteredItems,
                key = { "shop_seed_${it.shopSeed?.goodsId ?: it.seedId}" },
                contentType = { "book_tile" },
            ) { entry ->
                IllustratedGridTile(
                    item = entry,
                    isMutant = false,
                    isConnected = isConnected,
                    accent = accent,
                    seedShopMode = true,
                    onClick = { onSelectItem(entry) },
                )
            }
        }
    }
}

@Composable
internal fun IllustratedGridTile(
    item: IllustratedBookItem,
    isMutant: Boolean,
    isConnected: Boolean,
    accent: Color,
    seedShopMode: Boolean = false,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val buyable = !isMutant && canPurchaseIllustratedSeed(item, isConnected)
    val highlighted = if (seedShopMode) buyable else item.unlocked
    val metaText = when {
        seedShopMode -> seedShopMetaText(item, isConnected)
        isMutant -> if (item.unlocked) "已收集" else "未收集"
        else -> cropMetaText(item, isConnected)
    }
    val metaColor = when {
        seedShopMode && buyable -> apple.orange
        seedShopMode -> apple.secondaryText.copy(alpha = 0.78f)
        item.unlocked -> apple.secondaryText
        else -> apple.secondaryText.copy(alpha = 0.78f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppleRadius.tile))
            .clickable(enabled = !isMutant, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(AppleRadius.tile),
        color = if (highlighted) apple.card else apple.card.copy(alpha = 0.72f),
        border = BorderStroke(
            width = if (!seedShopMode && item.isNew) 1.dp else 0.5.dp,
            color = if (!seedShopMode && item.isNew) apple.orange else apple.separator,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(apple.elevated),
                contentAlignment = Alignment.Center,
            ) {
                RemoteThumbImage(
                    url = item.image,
                    contentDescription = item.name,
                    fallbackIcon = if (isMutant) Icons.Rounded.AutoAwesome else Icons.Rounded.Eco,
                    sizeDp = 42.dp,
                    fallbackTint = accent,
                    modifier = Modifier
                        .size(42.dp)
                        .alpha(if (highlighted) 1f else 0.45f),
                )
                if (!seedShopMode && item.unlocked) {
                    Surface(
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(14.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = accent.onAccent(3f), modifier = Modifier.size(10.dp))
                        }
                    }
                } else if (!seedShopMode && item.isNew) {
                    Surface(
                        shape = CircleShape,
                        color = apple.orange,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp),
                    ) {
                        Text(
                            "新",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = apple.orange.onAccent(),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = if (highlighted) apple.primaryText else apple.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = metaText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontFeatureSettings = if (seedShopMode && buyable) "tnum" else null,
                ),
                color = metaColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun canPurchaseIllustratedSeed(item: IllustratedBookItem, isConnected: Boolean): Boolean {
    val seed = item.shopSeed ?: return false
    return isConnected && seed.goodsId > 0 && !seed.locked && !seed.soldOut &&
        !seed.unknownMeta && !seed.requiresUnlockCard && (seed.remainingPurchaseCount ?: 1) > 0 && seed.price > 0L
}

private fun seedShopMetaText(item: IllustratedBookItem, isConnected: Boolean): String {
    val seed = item.shopSeed
    if (canPurchaseIllustratedSeed(item, isConnected) && seed != null) {
        return "${formatCompactCount(seed.price)} 金币"
    }
    return when {
        seed?.unknownMeta == true -> "其他途径"
        seed?.requiresUnlockCard == true -> "需解锁卡"
        seed?.locked == true -> if (seed.requiredLevel > 0) "${seed.requiredLevel}级解锁" else "未解锁"
        seed?.soldOut == true || seed?.remainingPurchaseCount == 0 -> "已达限购"
        else -> "暂不可购"
    }
}

private fun cropMetaText(item: IllustratedBookItem, isConnected: Boolean): String {
    if (canPurchaseIllustratedSeed(item, isConnected)) {
        return if (item.unlocked) "已收集" else "未收集"
    }
    val seed = item.shopSeed
    return when {
        seed?.unknownMeta == true -> "其他途径"
        seed?.requiresUnlockCard == true -> "需解锁卡"
        seed?.locked == true -> if (seed.requiredLevel > 0) "${seed.requiredLevel}级解锁" else "未解锁"
        seed?.soldOut == true || seed?.remainingPurchaseCount == 0 -> "已达限购"
        item.unlocked -> "已收集"
        else -> "未收集"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CropItemDetailSheet(
    item: IllustratedBookItem,
    goldBalance: Long,
    isConnected: Boolean,
    busy: Boolean,
    seedShopMode: Boolean = false,
    onPurchase: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val seed = item.shopSeed
    val buyable = canPurchaseIllustratedSeed(item, isConnected)
    val unitPrice = seed?.price ?: 0L
    val affordMax = if (buyable && unitPrice > 0L) (goldBalance / unitPrice).coerceAtMost(9999L).toInt() else 0
    val limitMax = seed?.remainingPurchaseCount?.coerceIn(0, 9999) ?: 9999
    val maxBuy = minOf(affordMax, limitMax)
    var requestedCount by remember(item.seedId) { mutableIntStateOf(1) }
    val count = if (maxBuy > 0) requestedCount.coerceIn(1, maxBuy) else 0
    val subtitleText = if (seedShopMode) {
        when {
            seed == null -> "商店种子"
            seed.requiresUnlockCard && seed.requiredLevel > 0 -> "Lv.${seed.requiredLevel} 解锁 · 需解锁卡"
            seed.requiresUnlockCard -> "需使用种子解锁卡"
            seed.requiredLevel > 0 -> "Lv.${seed.requiredLevel} 解锁"
            else -> "无等级限制"
        }
    } else {
        if (item.unlocked) "已收集 · 进度 ${item.progress}" else "未收集"
    }
    val subtitleColor = if (seedShopMode) {
        when {
            seed == null -> apple.secondaryText
            seed.locked || seed.requiresUnlockCard -> apple.orange
            else -> apple.emerald
        }
    } else {
        if (item.unlocked) apple.emerald else apple.secondaryText
    }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(AppleRadius.tile),
                    color = apple.elevated,
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        RemoteThumbImage(
                            url = item.image,
                            contentDescription = item.name,
                            fallbackIcon = Icons.Rounded.Eco,
                            sizeDp = 42.dp,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                    )
                }
            }

            if (seed != null) {
                Surface(
                    shape = RoundedCornerShape(AppleRadius.tile),
                    color = apple.elevated,
                    border = BorderStroke(0.5.dp, apple.separator),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = when {
                                buyable -> "每批 ${seed.itemCount} 颗 · 每批 ${formatNumber(unitPrice)} 金币"
                                seed.unknownMeta -> "该种子需通过活动或其他途径获取"
                                seed.requiresUnlockCard -> "需使用对应的种子解锁卡后才可购买"
                                seed.locked -> if (seed.requiredLevel > 0) "需农场达到 ${seed.requiredLevel} 级后解锁购买" else "当前尚未满足商店解锁条件"
                                seed.soldOut || seed.remainingPurchaseCount == 0 -> "当前商店已达到限购上限"
                                else -> "商店暂不可直接购买"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = apple.primaryText,
                        )
                        if (buyable) {
                            Text(
                                text = "当前金币 ${formatNumber(goldBalance)}" +
                                    (seed.remainingPurchaseCount?.let { " · 剩余限购 $it 批" } ?: ""),
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                            )
                        }
                    }
                }
            }

            if (buyable) {
                if (maxBuy > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("购买批次", style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                        AppleQuantityStepper(
                            value = count,
                            min = 1,
                            max = maxBuy,
                            onChange = { requestedCount = it },
                            enabled = !busy,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("合计花费", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
                        Text(
                            "${formatNumber(unitPrice * count)} 金币",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                            color = apple.orange,
                        )
                    }
                    Button(
                        onClick = { onPurchase(count) },
                        enabled = !busy && count > 0,
                        shape = RoundedCornerShape(AppleRadius.card),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(if (busy) "购买中..." else "确认购买 $count 批")
                    }
                } else {
                    Text("金币不足，无法购买该种子", style = MaterialTheme.typography.bodySmall, color = apple.red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun IllustratedRewardsSheet(
    book: IllustratedBook,
    busy: Boolean,
    onClaim: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isMutant = book.type == 2
    val listState = rememberLazyListState()
    val buffByLevel = remember(book.buffs) { book.buffs.groupBy { it.level } }

    LaunchedEffect(book.levels) {
        val targetIdx = book.levels.indexOfFirst { it.canClaim && !it.claimed }
            .takeIf { it >= 0 }
            ?: book.levels.indexOfFirst { !it.claimed }
        if (targetIdx > 0) {
            listState.scrollToItem(targetIdx)
        }
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isMutant) "超变图鉴属性加成与奖励" else "作物图鉴收藏奖励",
                style = MaterialTheme.typography.titleLarge,
                color = apple.primaryText,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (book.levels.isEmpty() && isMutant && book.buffs.isNotEmpty()) {
                    items(book.buffs, key = { "buff_${it.id}_${it.level}" }) { buff ->
                        val reached = buff.level <= book.level
                        Surface(
                            shape = RoundedCornerShape(AppleRadius.tile),
                            color = apple.elevated,
                            border = BorderStroke(0.5.dp, if (reached) apple.orange.copy(alpha = 0.4f) else apple.separator),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("Lv.${buff.level} ${buff.name}", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                    Text(buff.formattedEffect, style = MaterialTheme.typography.bodySmall, color = apple.orange)
                                }
                                AppleStatusPill(
                                    text = if (reached) "已生效" else "未达成",
                                    color = if (reached) apple.green else apple.secondaryText,
                                    showDot = false,
                                )
                            }
                        }
                    }
                } else {
                    items(book.levels, key = { "lv_${it.level}" }) { lv ->
                        IllustratedLevelRow(
                            level = lv,
                            buffs = buffByLevel[lv.level].orEmpty(),
                        )
                    }
                }
            }

            if (book.claimable) {
                Button(
                    onClick = onClaim,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMutant) apple.orange else apple.emerald,
                        contentColor = (if (isMutant) apple.orange else apple.emerald).onAccent(),
                    ),
                    shape = RoundedCornerShape(AppleRadius.card),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (busy) "领取中..." else "一键领取全部可领奖励")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IllustratedLevelRow(
    level: IllustratedLevel,
    buffs: List<com.qqfarm.mobile.data.model.IllustratedBuff>,
) {
    val apple = LocalAppleColors.current
    val (statusLabel, statusColor) = when {
        level.claimed -> "已领取" to apple.green
        level.canClaim -> "可领取" to apple.orange
        else -> "未达成" to apple.secondaryText
    }
    Surface(
        shape = RoundedCornerShape(AppleRadius.tile),
        color = if (level.canClaim) apple.orange.copy(alpha = 0.08f) else apple.elevated,
        border = BorderStroke(
            width = if (level.canClaim) 1.dp else 0.5.dp,
            color = if (level.canClaim) apple.orange.copy(alpha = 0.45f) else apple.separator,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.width(56.dp)) {
                Text("${level.level} 级", style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                Text("${level.progress} 进度", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (buffs.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        buffs.forEach { buff ->
                            Text(
                                text = "${buff.name} · ${buff.formattedEffect}",
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.orange,
                            )
                        }
                    }
                }
                if (level.rewards.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        level.rewards.forEach { reward ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                RemoteThumbImage(
                                    url = reward.image,
                                    contentDescription = reward.name,
                                    fallbackIcon = Icons.Rounded.CardGiftcard,
                                    sizeDp = 20.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "${reward.name} ×${formatCompactCount(reward.count)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.primaryText,
                                )
                            }
                        }
                    }
                }
            }
            AppleStatusPill(statusLabel, statusColor, showDot = level.canClaim)
        }
    }
}

// ---- Collections Tab ----

@Composable
private fun CollectionsBookTab(
    snapshot: CollectionSnapshot,
    busy: Boolean,
    crossTabNotice: Pair<Int, String>?,
    onJumpTab: (Int) -> Unit,
    onClaimMilestones: () -> Unit,
    onOpenMilestonesSheet: () -> Unit,
    onShowRules: (String, List<CollectionMutationRule>) -> Unit,
    onSelectItem: (CollectionItemEntry) -> Unit,
) {
    val apple = LocalAppleColors.current
    var statusFilter by remember { mutableStateOf("all") }
    var seriesIdFilter by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }

    val allItems = remember(snapshot.series) { snapshot.series.flatMap { it.items } }
    val activeCount = remember(allItems) { allItems.count { it.activated } }
    val upgradeableCount = remember(allItems) { allItems.count { it.canUpgrade } }
    val missingCount = remember(allItems) { allItems.count { !it.activated && it.ownCount <= 0L } }
    val claimableCount = remember(snapshot.milestones) { snapshot.milestones.count { it.reached && !it.claimed } }

    val visibleSeries = remember(snapshot.series, seriesIdFilter, statusFilter, search) {
        val kw = search.trim()
        snapshot.series.filter { seriesIdFilter == 0 || it.id == seriesIdFilter }.mapNotNull { series ->
            val items = series.items.filter { item ->
                val matchesKw = kw.isEmpty() || item.name.contains(kw, ignoreCase = true) || series.name.contains(kw, ignoreCase = true)
                val matchesStatus = when (statusFilter) {
                    "active" -> item.activated
                    "upgrade" -> item.canUpgrade
                    "missing" -> !item.activated && item.ownCount <= 0L
                    else -> true
                }
                matchesKw && matchesStatus
            }
            if (items.isEmpty()) null else series to items
        }
    }

    SearchableBookGrid(
        104.dp, seriesIdFilter, statusFilter, search,
        toolbar = {
            AppleSearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "搜索藏品或系列名称",
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statusChips = listOf(
                    "all" to "全部 (${allItems.size})",
                    "upgrade" to "待激活/可升星 ($upgradeableCount)",
                    "active" to "已激活 ($activeCount)",
                    "missing" to "未获得 ($missingCount)",
                )
                items(statusChips, key = { it.first }) { (key, label) ->
                    AppleFilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        accentColor = apple.blue,
                        label = { Text(label) },
                    )
                }
            }
            if (snapshot.series.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AppleFilterChip(
                            selected = seriesIdFilter == 0,
                            onClick = { seriesIdFilter = 0 },
                            accentColor = apple.blue,
                            label = { Text("全部系列") },
                        )
                    }
                    items(snapshot.series, key = { "filter_series_${it.id}" }) { s ->
                        AppleFilterChip(
                            selected = seriesIdFilter == s.id,
                            onClick = { seriesIdFilter = s.id },
                            accentColor = collectionRarityColor(s.rarity),
                            label = { Text(s.name) },
                        )
                    }
                }
            }
        },
    ) {
        if (crossTabNotice != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "col_cross_notice") {
                CrossTabRewardNotice(
                    message = crossTabNotice.second,
                    accent = apple.emerald,
                    onClick = { onJumpTab(crossTabNotice.first) },
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "col_overview") {
            CollectionOverviewCard(
                snapshot = snapshot,
                claimableCount = claimableCount,
                busy = busy,
                onClaim = onClaimMilestones,
                onOpenMilestones = onOpenMilestonesSheet,
            )
        }

        if (visibleSeries.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "col_empty") {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Stars,
                        title = "没有符合条件的藏品",
                        subtitle = "换个系列、状态或搜索词试试",
                    )
                }
            }
        } else {
            visibleSeries.forEach { (series, items) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "series_hdr_${series.id}") {
                    val rarityColor = collectionRarityColor(series.rarity)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(series.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                            if (series.rarityName.isNotBlank()) {
                                AppleStatusPill(series.rarityName, rarityColor, showDot = false)
                            }
                            if (series.mutationRules.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "变异概率",
                                    tint = apple.secondaryText,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable { onShowRules(series.name, series.mutationRules) },
                                )
                            }
                        }
                        Text(
                            text = "${series.activatedCount} / ${series.totalCount}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = apple.secondaryText,
                        )
                    }
                }
                items(
                    items = items,
                    key = { "col_${it.id}" },
                    contentType = { "collection_tile" },
                ) { entry ->
                    CollectionSeriesTile(
                        item = entry,
                        rarityColor = collectionRarityColor(series.rarity),
                        onClick = { onSelectItem(entry) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionOverviewCard(
    snapshot: CollectionSnapshot,
    claimableCount: Int,
    busy: Boolean,
    onClaim: () -> Unit,
    onOpenMilestones: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val nextScore = snapshot.nextRewardScore
    val fraction = if (nextScore != null && nextScore > 0L) {
        (snapshot.score.toFloat() / nextScore.toFloat()).coerceIn(0f, 1f)
    } else 1f

    AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(AppleRadius.tile),
                color = apple.blue.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "${snapshot.activatedCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        color = apple.blue,
                    )
                    Text("已激活", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = apple.blue)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("蓝晶藏品", style = MaterialTheme.typography.titleMedium, color = apple.primaryText)
                Text(
                    text = "共 ${snapshot.totalCount} 款 · 总评分 ${formatNumber(snapshot.score)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = apple.secondaryText,
                )
            }
            if (claimableCount > 0) {
                AppleStatusPill("可领 $claimableCount 档", apple.orange, showDot = true)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        AppleProgressBar(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = apple.blue,
            trackColor = apple.track,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (nextScore == null) "评分奖励已全部达成" else "距下一档评分奖励",
                style = MaterialTheme.typography.labelMedium,
                color = apple.secondaryText,
            )
            if (nextScore != null) {
                Text(
                    text = "${formatNumber(snapshot.score)} / ${formatNumber(nextScore)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = apple.primaryText,
                )
            }
        }

        if (snapshot.currentBuffs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                snapshot.currentBuffs.forEach { buff ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = apple.blue.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = buff.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = apple.blue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (claimableCount > 0) {
                Button(
                    onClick = onClaim,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = apple.orange, contentColor = apple.orange.onAccent()),
                    shape = RoundedCornerShape(AppleRadius.control),
                    modifier = Modifier.weight(1.35f),
                ) {
                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("领取评分奖励 ($claimableCount)")
                }
            }
            OutlinedButton(
                onClick = onOpenMilestones,
                shape = RoundedCornerShape(AppleRadius.control),
                modifier = Modifier.weight(1f),
            ) {
                Text("评分奖励")
            }
        }
    }
}

@Composable
private fun CollectionSeriesTile(
    item: CollectionItemEntry,
    rarityColor: Color,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val highlightUpgrade = item.canUpgrade
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppleRadius.tile))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(AppleRadius.tile),
        color = if (item.activated || item.ownCount > 0L) apple.card else apple.card.copy(alpha = 0.72f),
        border = BorderStroke(
            width = if (highlightUpgrade || item.displayed) 1.dp else 0.5.dp,
            color = when {
                highlightUpgrade -> apple.orange
                item.displayed -> rarityColor
                else -> apple.separator
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(apple.elevated),
                contentAlignment = Alignment.Center,
            ) {
                RemoteThumbImage(
                    url = item.image,
                    contentDescription = item.name,
                    fallbackIcon = Icons.Rounded.Stars,
                    sizeDp = 46.dp,
                    fallbackTint = rarityColor,
                    modifier = Modifier
                        .size(46.dp)
                        .alpha(if (item.activated || item.ownCount > 0L) 1f else 0.45f),
                )
                if (item.displayed) {
                    Surface(
                        shape = CircleShape,
                        color = rarityColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp),
                    ) {
                        Text(
                            "展示",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = rarityColor.onAccent(),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                } else if (highlightUpgrade) {
                    Surface(
                        shape = CircleShape,
                        color = apple.orange,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp),
                    ) {
                        Text(
                            if (item.activated) "升星" else "可激活",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = apple.orange.onAccent(),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = if (item.activated || item.ownCount > 0L) apple.primaryText else apple.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            StarBar(starLevel = item.starLevel, maxStarLevel = item.maxStarLevel, iconSize = 11.dp)
            Text(
                text = when {
                    item.activated -> "${formatNumber(item.score)} 分"
                    item.ownCount > 0L -> "待激活 · 持有 ${item.ownCount}"
                    else -> "未获得"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = when {
                    highlightUpgrade -> apple.orange
                    item.activated -> apple.secondaryText
                    else -> apple.secondaryText.copy(alpha = 0.78f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StarBar(
    starLevel: Int,
    maxStarLevel: Int,
    iconSize: androidx.compose.ui.unit.Dp,
) {
    val apple = LocalAppleColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..maxStarLevel) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = if (i <= starLevel) apple.orange else apple.separator,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionItemDetailSheet(
    series: CollectionSeries,
    item: CollectionItemEntry,
    busy: Boolean,
    onRequestUpgrade: () -> Unit,
    onToggleDisplay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rarityColor = collectionRarityColor(series.rarity)

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(AppleRadius.tile),
                    color = apple.elevated,
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        RemoteThumbImage(
                            url = item.image,
                            contentDescription = item.name,
                            fallbackIcon = Icons.Rounded.Stars,
                            sizeDp = 46.dp,
                            fallbackTint = rarityColor,
                            modifier = Modifier.size(46.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                        if (series.rarityName.isNotBlank()) {
                            AppleStatusPill(series.rarityName, rarityColor, showDot = false)
                        }
                    }
                    StarBar(starLevel = item.starLevel, maxStarLevel = item.maxStarLevel, iconSize = 14.dp)
                    Text(series.name, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                }
            }

            if (item.description.isNotBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            }

            Surface(
                shape = RoundedCornerShape(AppleRadius.tile),
                color = apple.elevated,
                border = BorderStroke(0.5.dp, apple.separator),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CollectionStatCell("状态", if (item.activated) "已激活" else if (item.ownCount > 0L) "待激活" else "未获得")
                    CollectionStatCell("持有数量", formatNumber(item.ownCount))
                    CollectionStatCell("提供评分", formatNumber(item.score))
                }
            }

            Surface(
                shape = RoundedCornerShape(AppleRadius.tile),
                color = apple.elevated,
                border = BorderStroke(0.5.dp, apple.separator),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = when {
                            item.starLevel >= item.maxStarLevel -> "已达最高星级"
                            item.activated -> "升至 ${item.starLevel + 1} 星需要"
                            else -> "激活需要"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = apple.primaryText,
                    )
                    item.nextStarCosts.forEach { cost ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RemoteThumbImage(
                                url = cost.image,
                                contentDescription = cost.name,
                                fallbackIcon = Icons.Rounded.ShoppingBag,
                                sizeDp = 26.dp,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(apple.card),
                            )
                            Text(cost.name, style = MaterialTheme.typography.bodySmall, color = apple.primaryText, modifier = Modifier.weight(1f))
                            Text(
                                "×${formatNumber(cost.count)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                                color = apple.orange,
                            )
                        }
                    }
                    if (!item.unlocked && item.requirements.isNotEmpty()) {
                        AppleHairlineDivider()
                        item.requirements.forEach { req ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = apple.secondaryText, modifier = Modifier.size(14.dp))
                                Text(req, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (item.activated) {
                    OutlinedButton(
                        onClick = onToggleDisplay,
                        enabled = !busy,
                        shape = RoundedCornerShape(AppleRadius.card),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (item.displayed) "取消展示" else "设为展示")
                    }
                }
                if (item.starLevel < item.maxStarLevel) {
                    Button(
                        onClick = onRequestUpgrade,
                        enabled = !busy && item.canUpgrade,
                        colors = ButtonDefaults.buttonColors(containerColor = rarityColor, contentColor = rarityColor.onAccent()),
                        shape = RoundedCornerShape(AppleRadius.card),
                        modifier = Modifier.weight(1.25f),
                    ) {
                        Text(
                            when {
                                !item.unlocked -> "尚未解锁"
                                !item.canUpgrade -> "材料不足"
                                item.activated -> "升星 (${item.starLevel + 1}星)"
                                else -> "立即激活"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionStatCell(label: String, value: String) {
    val apple = LocalAppleColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            color = apple.primaryText,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CollectionMilestonesSheet(
    snapshot: CollectionSnapshot,
    busy: Boolean,
    onClaimAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val hasClaimable = snapshot.milestones.any { it.reached && !it.claimed }

    LaunchedEffect(snapshot.milestones) {
        val targetIdx = snapshot.milestones.indexOfFirst { it.reached && !it.claimed }
            .takeIf { it >= 0 }
            ?: snapshot.milestones.indexOfFirst { !it.claimed }
        if (targetIdx > 0) {
            listState.scrollToItem(targetIdx)
        }
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("藏品评分奖励", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(snapshot.milestones, key = { "ms_${it.level}_${it.score}" }) { milestone ->
                    CollectionMilestoneRow(milestone)
                }
            }

            if (hasClaimable) {
                Button(
                    onClick = onClaimAll,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = apple.orange, contentColor = apple.orange.onAccent()),
                    shape = RoundedCornerShape(AppleRadius.card),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (busy) "领取中..." else "一键领取全部达标评分奖励")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionMilestoneRow(milestone: CollectionMilestone) {
    val apple = LocalAppleColors.current
    val canClaim = milestone.reached && !milestone.claimed
    val (statusLabel, statusColor) = when {
        milestone.claimed -> "已领取" to apple.green
        canClaim -> "可领取" to apple.orange
        else -> "未达成" to apple.secondaryText
    }
    Surface(
        shape = RoundedCornerShape(AppleRadius.tile),
        color = if (canClaim) apple.orange.copy(alpha = 0.08f) else apple.elevated,
        border = BorderStroke(
            width = if (canClaim) 1.dp else 0.5.dp,
            color = if (canClaim) apple.orange.copy(alpha = 0.45f) else apple.separator,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.width(64.dp)) {
                Text(
                    text = formatCompactCount(milestone.score),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = apple.primaryText,
                )
                Text("评分", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                milestone.buffs.forEach { buff ->
                    Text(buff.description, style = MaterialTheme.typography.labelSmall, color = apple.blue)
                }
                if (milestone.rewards.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        milestone.rewards.forEach { reward ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                RemoteThumbImage(
                                    url = reward.image,
                                    contentDescription = reward.name,
                                    fallbackIcon = Icons.Rounded.CardGiftcard,
                                    sizeDp = 20.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "${reward.name} ×${formatCompactCount(reward.count)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.primaryText,
                                )
                            }
                        }
                    }
                }
            }
            AppleStatusPill(statusLabel, statusColor, showDot = canClaim)
        }
    }
}

// ---- Mutation Catalog Tab ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MutationCatalogTab(
    catalog: MutationCatalogSnapshot,
    onJumpToCollections: () -> Unit,
    onShowRules: (String, List<CollectionMutationRule>) -> Unit,
) {
    val apple = LocalAppleColors.current
    var category by remember { mutableStateOf("normal") }
    var search by remember { mutableStateOf("") }
    val kw = search.trim()

    val filteredEffects = remember(catalog, category, kw) {
        val list = if (category == "decoration") catalog.decoration else catalog.normal
        list.filter { kw.isEmpty() || "${it.name}${it.effect}${it.description}".contains(kw, ignoreCase = true) }
    }
    val filteredSeries = remember(catalog.collections, kw) {
        catalog.collections.mapNotNull { entry ->
            val items = entry.items.filter { kw.isEmpty() || it.name.contains(kw, ignoreCase = true) || entry.name.contains(kw, ignoreCase = true) }
            if (items.isEmpty()) null else entry to items
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        searchHeader {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppleSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = if (category == "collections") "搜索藏品或系列" else "搜索变异名称或效果",
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cats = listOf(
                        "normal" to "常规变异 (${catalog.normal.size})",
                        "decoration" to "装扮变异 (${catalog.decoration.size})",
                        "collections" to "藏品变异 (${catalog.collections.size})",
                    )
                    items(cats, key = { it.first }) { (key, label) ->
                        AppleFilterChip(
                            selected = category == key,
                            onClick = { category = key },
                            accentColor = apple.indigo,
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        if (category != "collections") {
            if (filteredEffects.isEmpty()) {
                item(key = "cat_empty_eff") {
                    AppleGroupedCard {
                        AppleEmptyState(icon = Icons.Rounded.AutoAwesome, title = "没有符合条件的变异", subtitle = "换个搜索词试试")
                    }
                }
            } else {
                items(filteredEffects, key = { "eff_${it.id}" }, contentType = { "mutation_effect" }) { effect ->
                    MutationEffectCard(effect)
                }
            }
        } else {
            item(key = "cat_col_intro") {
                CrossTabRewardNotice(
                    message = "蓝晶土地作物有概率变异为藏品，点此返回我的藏品管理激活与领奖",
                    accent = apple.blue,
                    onClick = onJumpToCollections,
                )
            }
            if (filteredSeries.isEmpty()) {
                item(key = "cat_empty_col") {
                    AppleGroupedCard {
                        AppleEmptyState(icon = Icons.Rounded.Stars, title = "没有符合条件的藏品", subtitle = "换个搜索词试试")
                    }
                }
            } else {
                items(filteredSeries, key = { "cat_ser_${it.first.id}" }) { (series, items) ->
                    MutationSeriesCatalogCard(
                        series = series,
                        items = items,
                        onShowRules = { onShowRules(series.name, series.mutationRules) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MutationEffectCard(effect: MutantCatalogEffect) {
    val apple = LocalAppleColors.current
    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(AppleRadius.control),
                color = apple.elevated,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RemoteThumbImage(
                        url = effect.image,
                        contentDescription = effect.name,
                        fallbackIcon = Icons.Rounded.AutoAwesome,
                        sizeDp = 38.dp,
                        fallbackTint = apple.indigo,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(effect.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                    if (effect.effect.isNotBlank()) {
                        AppleStatusPill(effect.effect, apple.indigo, showDot = false)
                    }
                }
                if (effect.description.isNotBlank()) {
                    Text(effect.description, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                }
                if (effect.probabilities.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        effect.probabilities.forEach { prob ->
                            val label = RARITY_LABELS.getOrNull(prob.rarity)?.takeIf { it.isNotBlank() } ?: "触发"
                            Surface(shape = RoundedCornerShape(6.dp), color = apple.track) {
                                Text(
                                    text = "$label ${formatPercent(prob.percent)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
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
private fun MutationSeriesCatalogCard(
    series: MutationCatalogCollectionSeries,
    items: List<CommerceItem>,
    onShowRules: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val rarityIdx = RARITY_LABELS.indexOf(series.rarityName).coerceAtLeast(1)
    val rarityColor = collectionRarityColor(rarityIdx)
    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(series.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                if (series.rarityName.isNotBlank()) {
                    AppleStatusPill(series.rarityName, rarityColor, showDot = false)
                }
            }
            if (series.mutationRules.isNotEmpty()) {
                TextButton(onClick = onShowRules, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("概率/保底")
                }
            }
        }
        if (series.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(series.description, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
        }
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(AppleRadius.control),
                    color = apple.elevated,
                    border = BorderStroke(0.5.dp, apple.separator),
                    modifier = Modifier.width(74.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        RemoteThumbImage(
                            url = item.image,
                            contentDescription = item.name,
                            fallbackIcon = Icons.Rounded.Stars,
                            sizeDp = 36.dp,
                            fallbackTint = rarityColor,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = apple.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun collectionRarityColor(rarity: Int): Color {
    val apple = LocalAppleColors.current
    return when (rarity) {
        2 -> apple.blue
        3 -> apple.indigo
        4 -> apple.orange
        else -> apple.emerald
    }
}

private fun formatPercent(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
