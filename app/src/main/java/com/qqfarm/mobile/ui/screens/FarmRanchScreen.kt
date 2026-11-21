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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qqfarm.mobile.data.model.BagItem
import com.qqfarm.mobile.data.model.BagPayload
import com.qqfarm.mobile.data.model.LandItem
import com.qqfarm.mobile.data.model.LandsPayload
import com.qqfarm.mobile.data.model.PetSnapshot
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleQuantityStepper
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.components.formatNumber
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.ui.theme.SoilBlack
import com.qqfarm.mobile.ui.theme.SoilBlueCrystal
import com.qqfarm.mobile.ui.theme.SoilGold
import com.qqfarm.mobile.ui.theme.SoilNormal
import com.qqfarm.mobile.ui.theme.SoilPurpleGold
import com.qqfarm.mobile.ui.theme.SoilRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxHeight
import com.qqfarm.mobile.ui.components.AppleActionPill
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.EqualHeightRow
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.pressable
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmRanchScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()

    var subTab by remember { mutableIntStateOf(0) }
    var refreshing by remember(accountId, isConnected, subTab) { mutableStateOf(false) }
    var loadError by remember(accountId, isConnected, subTab) { mutableStateOf("") }
    var actionBusy by remember { mutableStateOf(false) }

    var landsPayload by remember(accountId, isConnected) { mutableStateOf<LandsPayload?>(null) }
    var bagPayload by remember(accountId, isConnected) { mutableStateOf<BagPayload?>(null) }
    var petSnapshot by remember(accountId, isConnected) { mutableStateOf<PetSnapshot?>(null) }

    var selectedLandId by remember(accountId, isConnected) { mutableStateOf<Int?>(null) }
    var selectedBagKey by remember(accountId, isConnected) { mutableStateOf<String?>(null) }
    var bagActionError by remember(selectedBagKey) { mutableStateOf<String?>(null) }
    var showUpgradeConfirm by remember(accountId, isConnected) { mutableStateOf(false) }

    var clockTickMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            clockTickMs = SystemClock.elapsedRealtime()
        }
    }

    suspend fun loadCurrentTab() {
        if (!isConnected || refreshing) return
        refreshing = true
        loadError = ""
        try {
            when (subTab) {
                0 -> landsPayload = apiClient.fetchLands(accountId)
                1 -> bagPayload = apiClient.fetchBag(accountId)
                2 -> petSnapshot = apiClient.fetchPets(accountId)
            }
        } catch (e: ApiException) {
            loadError = e.message
            onMessage(e.message)
        } finally {
            refreshing = false
        }
    }

    fun operateFarm(operation: String, landId: Int? = null) {
        if (actionBusy || refreshing || !isConnected) return
        actionBusy = true
        scope.launch {
            try {
                onMessage(apiClient.operateFarm(accountId, operation, landId))
                loadCurrentTab()
            } catch (e: ApiException) {
                onMessage(e.message)
            } finally {
                actionBusy = false
            }
        }
    }

    fun operateBag(onUpdated: (BagPayload) -> Unit = {}, action: suspend () -> Unit) {
        if (actionBusy || refreshing) return
        actionBusy = true
        bagActionError = null
        scope.launch {
            try {
                try {
                    action()
                } catch (e: ApiException) {
                    bagActionError = e.message
                }
                val updated = apiClient.fetchBag(accountId)
                bagPayload = updated
                if (bagActionError == null) onUpdated(updated)
                if (selectedBagKey != null && updated.items.none { it.key == selectedBagKey }) {
                    selectedBagKey = null
                    onMessage(bagActionError ?: "该物品已不在背包")
                }
            } catch (e: ApiException) {
                bagPayload = null
                selectedBagKey = null
                onMessage(bagActionError ?: e.message)
            } finally {
                actionBusy = false
            }
        }
    }

    LaunchedEffect(accountId, isConnected, subTab) {
        if (isConnected) {
            loadCurrentTab()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AppleSegmentedControl(
                options = listOf("土地", "背包", "宠物"),
                selectedIndex = subTab,
                onSelect = { if (!actionBusy) subTab = it },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .swipeToSwitchTabs(subTab, 3, !actionBusy) { idx -> if (!actionBusy) subTab = idx },
        ) {
            if (!isConnected) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                        AppleEmptyState(
                            icon = Icons.Rounded.Eco,
                            title = "账号未在线",
                            subtitle = "请先启动并登录当前农场账号后再管理田地、背包和宠物",
                        )
                    }
                }
            } else {
                val hasData = when (subTab) {
                    0 -> landsPayload != null
                    1 -> bagPayload != null
                    else -> petSnapshot != null
                }
                PullToRefreshBox(
                    isRefreshing = refreshing && hasData,
                    onRefresh = { if (!actionBusy) scope.launch { loadCurrentTab() } },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (!hasData) {
                        if (loadError.isBlank()) {
                            AppleLoadingState()
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                                    AppleEmptyState(
                                        icon = Icons.Rounded.Refresh,
                                        title = "读取失败",
                                        subtitle = loadError,
                                        actionLabel = "重新加载",
                                        onAction = { scope.launch { loadCurrentTab() } },
                                    )
                                }
                            }
                        }
                        return@PullToRefreshBox
                    }
                    when (subTab) {
                        0 -> FarmContent(
                            payload = landsPayload,
                            clockTickMs = clockTickMs,
                            busy = actionBusy || refreshing,
                            onOperate = { operateFarm(it) },
                            onFertilizeAll = { fertType ->
                                if (actionBusy) return@FarmContent
                                actionBusy = true
                                scope.launch {
                                    runCatching { apiClient.fertilizeAll(accountId, fertType) }
                                        .onSuccess { count ->
                                            onMessage("已为 $count 块土地施肥")
                                            loadCurrentTab()
                                        }
                                        .onFailure { onMessage(it.message ?: "施肥失败") }
                                    actionBusy = false
                                }
                            },
                            onRequestUpgrade = { showUpgradeConfirm = true },
                            onLandClick = { selectedLandId = it.id },
                        )

                        1 -> BagContent(
                            payload = bagPayload,
                            onItemClick = { if (!actionBusy && !refreshing) selectedBagKey = it.key },
                        )

                        2 -> PetsContent(
                            snapshot = petSnapshot,
                            busy = actionBusy || refreshing,
                            onClaimGifts = {
                                if (actionBusy) return@PetsContent
                                actionBusy = true
                                scope.launch {
                                    runCatching { apiClient.claimDogSkillGifts(accountId) }
                                        .onSuccess {
                                            onMessage("已领取全部宠物技能礼包")
                                            loadCurrentTab()
                                        }
                                        .onFailure { onMessage(it.message ?: "领取失败") }
                                    actionBusy = false
                                }
                            },
                            onUseFood = { foodId, count ->
                                if (actionBusy || refreshing) return@PetsContent
                                actionBusy = true
                                scope.launch {
                                    runCatching { apiClient.useDogFood(accountId, foodId, count) }
                                        .onSuccess { updated ->
                                            petSnapshot = updated
                                            onMessage("已喂食狗粮 ×$count")
                                        }
                                        .onFailure { onMessage(it.message ?: "喂食失败") }
                                    actionBusy = false
                                }
                            },
                            onDeployDog = { dogId ->
                                if (actionBusy) return@PetsContent
                                actionBusy = true
                                scope.launch {
                                    runCatching { apiClient.deployDog(accountId, dogId) }
                                        .onSuccess { updated ->
                                            petSnapshot = updated
                                            onMessage("宠物已上场守护")
                                        }
                                        .onFailure { onMessage(it.message ?: "上场失败") }
                                    actionBusy = false
                                }
                            },
                            onWithdrawDog = {
                                if (actionBusy) return@PetsContent
                                actionBusy = true
                                scope.launch {
                                    runCatching { apiClient.withdrawDog(accountId) }
                                        .onSuccess { updated ->
                                            petSnapshot = updated
                                            onMessage("已收回当前宠物")
                                        }
                                        .onFailure { onMessage(it.message ?: "收回失败") }
                                    actionBusy = false
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    landsPayload?.lands?.firstOrNull { it.id == selectedLandId }?.let { land ->
        val allLands = landsPayload?.lands.orEmpty()
        val hasFarmSocialCleanup = landsPayload?.cleanableSocialEventNames.orEmpty().isNotEmpty()
        val ripeCount = allLands.count {
            !it.occupiedByMaster && (it.status == "harvestable" || it.status == "stealable")
        }
        LandDetailSheet(
            land = land,
            allLands = allLands,
            ripeCount = ripeCount,
            hasFarmSocialCleanup = hasFarmSocialCleanup,
            clockTickMs = clockTickMs,
            busy = actionBusy || refreshing,
            onHarvestAll = {
                selectedLandId = null
                operateFarm("harvest")
            },
            onPlantAll = {
                selectedLandId = null
                operateFarm("plant")
            },
            onUpgradeLands = {
                selectedLandId = null
                showUpgradeConfirm = true
            },
            onCareSingle = {
                selectedLandId = null
                operateFarm("clear", land.id)
            },
            onFertilize = { fertType ->
                selectedLandId = null
                actionBusy = true
                scope.launch {
                    runCatching { apiClient.fertilizeLand(accountId, land.id, fertType) }
                        .onSuccess {
                            onMessage("土地 #${land.id} 施肥成功")
                            loadCurrentTab()
                        }
                        .onFailure { onMessage(it.message ?: "施肥失败") }
                    actionBusy = false
                }
            },
            onJumpToLand = { master -> selectedLandId = master.id },
            onDismiss = { selectedLandId = null },
        )
    }

    if (showUpgradeConfirm) {
        AlertDialog(
            onDismissRequest = { if (!actionBusy) showUpgradeConfirm = false },
            title = { Text("批量解锁与升级土地") },
            text = { Text("将自动解锁或升级农场中所有满足等级与前置条件的土地（会消耗对应金币或道具），是否继续？") },
            confirmButton = {
                Button(
                    onClick = {
                        showUpgradeConfirm = false
                        operateFarm("upgrade")
                    },
                    enabled = !actionBusy && !refreshing,
                ) { Text("确认升级") }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeConfirm = false }, enabled = !actionBusy) {
                    Text("取消")
                }
            },
        )
    }

    bagPayload?.let { bag ->
        bag.items.firstOrNull { it.key == selectedBagKey }?.let { item ->
            BagItemActionSheet(
                item = item,
                maxUseCount = bag.maxUseCount(item),
                error = bagActionError,
                busy = actionBusy || refreshing,
                onUse = { count ->
                    operateBag {
                        val message = apiClient.useBagItem(accountId, item.id, count, item.uid)
                        selectedBagKey = null
                        onMessage(message)
                    }
                },
                onSellAll = {
                    operateBag {
                        apiClient.sellBagItem(accountId, item.id, item.count, item.uid)
                        selectedBagKey = null
                        onMessage("已出售 ${item.name} ×${item.count}")
                    }
                },
                onSetLocked = { locked ->
                    operateBag(onUpdated = { updated ->
                        val current = updated.items.firstOrNull { it.key == item.key }
                        if (current != null && current.locked != locked) {
                            bagActionError = "锁定状态未变化"
                        }
                    }) {
                        apiClient.lockBagItems(accountId, listOf(item.uid), locked)
                    }
                },
                onDismiss = { selectedBagKey = null },
            )
        }
    }
}

// ---- Farm View ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FarmContent(
    payload: LandsPayload?,
    clockTickMs: Long,
    busy: Boolean,
    onOperate: (String) -> Unit,
    onFertilizeAll: (String) -> Unit,
    onRequestUpgrade: () -> Unit,
    onLandClick: (LandItem) -> Unit,
) {
    val apple = LocalAppleColors.current
    val lands = payload?.lands.orEmpty()
    val summary = payload?.summary
    val socialNames = payload?.cleanableSocialEventNames.orEmpty()
    val needsCare = lands.any {
        it.unlocked && !it.occupiedByMaster && (it.needWater || it.needWeed || it.needBug || it.needInteractionCleanup)
    } || socialNames.isNotEmpty()
    val hasCleanupTarget = lands.any {
        !it.occupiedByMaster && (it.status == "growing" || it.status == "harvestable" || it.status == "dead")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (summary != null) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (summary.harvestable > 0) AppleStatusPill("可收 ${summary.harvestable}", apple.green)
                    AppleStatusPill("生长 ${summary.growing}", apple.blue, showDot = false)
                    if (summary.empty > 0) AppleStatusPill("空地 ${summary.empty}", apple.secondaryText, showDot = false)
                    if (summary.dead > 0) AppleStatusPill("枯萎 ${summary.dead}", apple.red)
                    if (summary.needWater > 0) AppleStatusPill("缺水 ${summary.needWater}", apple.cyan)
                    if (summary.needWeed > 0) AppleStatusPill("杂草 ${summary.needWeed}", apple.emerald)
                    if (summary.needBug > 0) AppleStatusPill("虫害 ${summary.needBug}", apple.orange)
                }
            }
        }

        if (socialNames.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(AppleRadius.card),
                    color = apple.orange.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, apple.orange.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "农场中出现 ${socialNames.joinToString("、")}，需要清理",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.primaryText,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { onOperate("clear") },
                            enabled = !busy && hasCleanupTarget,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(if (hasCleanupTarget) "一键清理" else "种植后可清理")
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ActionPillButton("一键全自动", true, !busy) { onOperate("all") }
                }
                item {
                    ActionPillButton("收获", false, !busy && (summary?.harvestable ?: 0) > 0) { onOperate("harvest") }
                }
                item {
                    ActionPillButton("除草·除虫·浇水", false, !busy && needsCare) { onOperate("clear") }
                }
                item {
                    ActionPillButton("种植", false, !busy && summary != null && (summary.empty > 0 || summary.dead > 0)) { onOperate("plant") }
                }
                item {
                    ActionPillButton("施普通肥", false, !busy) { onFertilizeAll("normal") }
                }
                item {
                    ActionPillButton("施有机肥", false, !busy) { onFertilizeAll("organic") }
                }
                item {
                    ActionPillButton("解锁 / 升级", false, !busy && lands.any { it.couldUnlock || it.couldUpgrade }) {
                        onRequestUpgrade()
                    }
                }
            }
        }

        if (lands.isEmpty()) {
            item {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Eco,
                        title = "暂无土地记录",
                        subtitle = "下拉可刷新农场土地详情",
                    )
                }
            }
        } else {
            val rows = lands.chunked(3)
            items(rows) { rowLands ->
                EqualHeightRow(spacing = 10.dp) {
                    rowLands.forEach { land ->
                        AppleLandTile(
                            land = land,
                            clockTickMs = clockTickMs,
                            modifier = Modifier.weight(1f),
                            onClick = { onLandClick(land) },
                        )
                    }
                    repeat(3 - rowLands.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AppleLandTile(
    land: LandItem,
    clockTickMs: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val apple = LocalAppleColors.current
    val soilColor = soilColorForLevel(land.level)
    val soilLabel = soilLabelForLevel(land.level)
    val isLocked = !land.unlocked || land.status == "locked"
    val isEmpty = !isLocked && (land.status == "empty" || land.plantName.isBlank())

    val elapsedSec = if (land.fetchedAtElapsedMs > 0L) {
        ((clockTickMs - land.fetchedAtElapsedMs) / 1000L).coerceAtLeast(0L)
    } else 0L
    val remainSec = (land.matureInSec - elapsedSec).coerceAtLeast(0L)

    val growFraction = when {
        land.status == "harvestable" || land.status == "stealable" -> 1f
        land.totalGrowTime > 0L && remainSec > 0L ->
            (1f - remainSec.toFloat() / land.totalGrowTime.toFloat()).coerceIn(0.05f, 1f)
        land.totalGrowTime > 0L && remainSec == 0L && land.status == "growing" -> 1f
        else -> 0f
    }

    val (stateLabel, stateColor) = when {
        isLocked -> "未解锁" to apple.secondaryText
        isEmpty -> "空地" to apple.secondaryText
        land.occupiedByMaster -> "#${land.masterLandId}合种" to apple.secondaryText
        land.status == "harvestable" -> "可收获" to apple.green
        land.status == "stealable" -> "可偷取" to apple.orange
        land.status == "unstealable" -> "不可偷取" to apple.secondaryText
        land.status == "harvested" -> "已成熟" to apple.secondaryText
        land.status == "dead" -> "已枯萎" to apple.red
        remainSec > 0L -> formatDurationSec(remainSec) to apple.blue
        else -> (land.phaseName.ifBlank { "生长中" }) to apple.blue
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .alpha(if (isLocked || land.occupiedByMaster) 0.65f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = apple.card,
        border = BorderStroke(
            width = if (land.status == "harvestable" || land.status == "stealable") 1.2.dp else 0.5.dp,
            color = if (land.status == "harvestable") apple.green else if (land.status == "stealable") apple.orange else apple.separator,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "#${land.id}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = apple.secondaryText,
                )
                Surface(
                    shape = CircleShape,
                    color = soilColor.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = soilLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = soilColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLocked) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = apple.secondaryText, modifier = Modifier.size(22.dp))
                } else if (isEmpty) {
                    Icon(Icons.Rounded.Eco, contentDescription = null, tint = apple.secondaryText.copy(alpha = 0.45f), modifier = Modifier.size(22.dp))
                } else {
                    RemoteAssetImage(
                        url = land.seedImage,
                        contentDescription = land.plantName,
                        fallbackIcon = Icons.Rounded.Eco,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    isLocked -> "未开启"
                    isEmpty -> "空闲土地"
                    else -> land.plantName
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = apple.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                color = stateColor,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(5.dp))

            LinearProgressIndicator(
                progress = { growFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = stateColor,
                trackColor = apple.track,
                strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.height(14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (land.needWater) Icon(Icons.Rounded.WaterDrop, contentDescription = "缺水", tint = apple.cyan, modifier = Modifier.size(12.dp))
                if (land.needWeed) Icon(Icons.Rounded.Grass, contentDescription = "杂草", tint = apple.green, modifier = Modifier.size(12.dp))
                if (land.needBug) Icon(Icons.Rounded.BugReport, contentDescription = "虫害", tint = apple.orange, modifier = Modifier.size(12.dp))
                if (land.mutantNames.isNotEmpty()) Icon(Icons.Rounded.Star, contentDescription = "变异", tint = apple.indigo, modifier = Modifier.size(12.dp))
            }
        }
    }
}

private fun formatLandPercent(value: Double): String {
    val clean = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
    }
    return "$clean%"
}

private fun resolveSingleLandCareLabel(
    land: LandItem,
    hasFarmSocialCleanup: Boolean,
): String? {
    val issues = buildList {
        if (land.needWater) add("浇水")
        if (land.needWeed) add("除草")
        if (land.needBug) add("除虫")
        if (land.needInteractionCleanup) add("清理恶作剧")
        if (hasFarmSocialCleanup) add("清理农场异常")
    }
    return when {
        issues.size == 1 -> "立即${issues.first()}"
        issues.size == 2 -> "立即${issues[0]}·${issues[1]}"
        issues.size >= 3 -> "一键护理（${issues.joinToString("/")}）"
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LandDetailSheet(
    land: LandItem,
    allLands: List<LandItem>,
    ripeCount: Int,
    hasFarmSocialCleanup: Boolean,
    clockTickMs: Long,
    busy: Boolean,
    onHarvestAll: () -> Unit,
    onPlantAll: () -> Unit,
    onUpgradeLands: () -> Unit,
    onCareSingle: () -> Unit,
    onFertilize: (String) -> Unit,
    onJumpToLand: (LandItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val elapsedSec = if (land.fetchedAtElapsedMs > 0L) ((clockTickMs - land.fetchedAtElapsedMs) / 1000L).coerceAtLeast(0L) else 0L
    val remainSec = (land.matureInSec - elapsedSec).coerceAtLeast(0L)

    val isLocked = !land.unlocked || land.status == "locked"
    val isFollower = land.occupiedByMaster && land.masterLandId > 0 && land.masterLandId != land.id
    val masterLand = if (isFollower) allLands.firstOrNull { it.id == land.masterLandId } else null
    val isEmpty = land.status == "empty"
    val isDead = !isLocked && land.status == "dead"
    val isRipe = !isLocked && (land.status == "harvestable" || land.status == "stealable")
    val isGrowing = !isLocked && !isFollower && !isEmpty && !isDead && !isRipe
    val careLabel = resolveSingleLandCareLabel(land, hasFarmSocialCleanup)
    val organicExhausted = land.leftInorcFertTimes != null && land.leftInorcFertTimes <= 0
    val upgradeInfo = land.landInfo

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RemoteAssetImage(
                    url = land.seedImage,
                    contentDescription = land.plantName,
                    fallbackIcon = Icons.Rounded.Eco,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(apple.card)
                        .padding(6.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${land.id} · ${land.plantName.ifBlank { if (isLocked) "未解锁" else "空闲土地" }}",
                        style = MaterialTheme.typography.titleLarge,
                        color = apple.primaryText,
                    )
                    val phaseSummary = when {
                        isLocked -> "未解锁"
                        isFollower -> "与 #${land.masterLandId} 合种"
                        isEmpty -> "空地待种植"
                        isDead -> "作物已枯萎"
                        isRipe -> "已成熟待收获"
                        else -> land.phaseName.ifBlank { "生长中" }
                    }
                    val seasonSuffix = if (land.totalSeason > 1) {
                        " (${land.currentSeason}/${land.totalSeason}季)"
                    } else ""
                    Text(
                        text = "${soilLabelForLevel(land.level)} · $phaseSummary$seasonSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
            }

            if (isFollower) {
                AppleGroupedCard(
                    contentPadding = PaddingValues(14.dp),
                    containerColor = apple.orange.copy(alpha = 0.08f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "该地块正与 #${land.masterLandId} 联合种植同一株作物，护理与施肥请直接操作主地块。",
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.primaryText,
                        )
                        if (masterLand != null) {
                            Button(
                                onClick = { onJumpToLand(masterLand) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("前往 #${land.masterLandId} 主地块")
                            }
                        }
                    }
                }
            }

            if (!isLocked && !isFollower) {
                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("作物状态", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
                            Text(
                                text = when {
                                    isEmpty -> "当前为空地"
                                    isDead -> "已枯萎，等待铲除补种"
                                    isRipe -> if (land.status == "stealable") "可采摘 / 待收获" else "已成熟"
                                    remainSec > 0L -> "预计 ${formatDurationSec(remainSec)} 后成熟"
                                    else -> land.phaseName.ifBlank { "生长中" }
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = if (isRipe) apple.green else if (isDead) apple.red else apple.primaryText,
                            )
                        }
                        if (land.leftInorcFertTimes != null && !isEmpty && !isDead) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("有机肥可施次数", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
                                Text(
                                    text = if (organicExhausted) "本阶段已用完" else "剩余 ${land.leftInorcFertTimes} 次",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (organicExhausted) apple.secondaryText else apple.primaryText,
                                )
                            }
                        }
                        if (land.mutantNames.isNotEmpty()) {
                            Text(
                                text = "变异特性：${land.mutantNames.joinToString("、")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.indigo,
                            )
                        }
                        if (isGrowing && careLabel == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = apple.green,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "当前状态良好，无缺水、杂草或虫害",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.green,
                                )
                            }
                        }
                    }
                }
            }

            if (upgradeInfo != null) {
                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val bonus = upgradeInfo.current ?: upgradeInfo.next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = bonus?.name?.ifBlank { soilLabelForLevel(land.level) } ?: soilLabelForLevel(land.level),
                                style = MaterialTheme.typography.titleSmall,
                                color = apple.primaryText,
                            )
                            val stageLabel = when (upgradeInfo.mode) {
                                "max" -> "已满级"
                                "unlock" -> if (land.couldUnlock || upgradeInfo.serverAllowed) "可解锁" else "未解锁"
                                else -> if (land.couldUpgrade || upgradeInfo.serverAllowed) "可升级" else "加成详情"
                            }
                            AppleStatusPill(
                                text = stageLabel,
                                color = if (land.couldUpgrade || land.couldUnlock || upgradeInfo.serverAllowed) apple.orange else apple.secondaryText,
                            )
                        }
                        if (bonus != null) {
                            val parts = buildList {
                                if (bonus.yieldBonus > 0) add("产量 +${formatLandPercent(bonus.yieldBonus)}")
                                if (bonus.growthReduction > 0) add("提速 ${formatLandPercent(bonus.growthReduction)}")
                                if (bonus.expBonus > 0) add("经验 +${formatLandPercent(bonus.expBonus)}")
                                if (bonus.mutantMultiplier > 0) {
                                    val mult = if (bonus.mutantMultiplier % 1.0 == 0.0) {
                                        bonus.mutantMultiplier.toInt().toString()
                                    } else String.format(Locale.US, "%.1f", bonus.mutantMultiplier)
                                    add("变异 ×$mult")
                                }
                                if (bonus.resonanceBonus > 0) add("共鸣 +${formatLandPercent(bonus.resonanceBonus)}")
                            }
                            if (parts.isNotEmpty()) {
                                Text(
                                    text = parts.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                )
                            }
                        }
                        if (upgradeInfo.mode != "max" && upgradeInfo.next != null) {
                            AppleHairlineDivider(startIndent = 0.dp)
                            val conds = buildList {
                                upgradeInfo.needLevel?.let { add("需农场 Lv.$it") }
                                upgradeInfo.prerequisiteLandId?.let { preId ->
                                    add("需先解锁 #$preId${if (upgradeInfo.prerequisiteMet) "(已满足)" else "(未解锁)"}")
                                }
                                if (upgradeInfo.precedingRequired > 0 && upgradeInfo.precedingName.isNotBlank()) {
                                    val ownedTxt = upgradeInfo.precedingOwned?.toString() ?: "--"
                                    add("${upgradeInfo.precedingName}及以上需 ${upgradeInfo.precedingRequired} 块 ($ownedTxt/${upgradeInfo.precedingRequired})")
                                }
                            }
                            Text(
                                text = (if (upgradeInfo.mode == "unlock") "解锁条件：" else "升至 ${upgradeInfo.next.name}：") +
                                    conds.joinToString(" · ").ifBlank { "满足等级条件后可自动处理" },
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.secondaryText,
                            )
                            if (upgradeInfo.costs.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    upgradeInfo.costs.forEach { cost ->
                                        val countVal = cost.count.toLongOrNull()
                                        val countTxt = if (countVal != null) formatCompactCount(countVal) else cost.count
                                        AppleStatusPill(
                                            text = "${cost.name} ×$countTxt",
                                            color = apple.orange,
                                            showDot = false,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isFollower) {
                when {
                    isLocked -> if (land.couldUnlock || upgradeInfo?.serverAllowed == true) {
                        Button(
                            onClick = onUpgradeLands,
                            enabled = !busy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text("批量解锁与升级")
                        }
                    }

                    isEmpty -> {
                        Button(
                            onClick = onPlantAll,
                            enabled = !busy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text("批量种植空地和枯地")
                        }
                    }

                    isDead -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onPlantAll,
                                enabled = !busy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Text("批量种植空地和枯地")
                            }
                            if (hasFarmSocialCleanup) {
                                OutlinedButton(
                                    onClick = onCareSingle,
                                    enabled = !busy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("清理农场异常")
                                }
                            }
                        }
                    }

                    isRipe -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onHarvestAll,
                                enabled = !busy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Text(if (ripeCount > 1) "一键收获全部成熟作物 ($ripeCount 块)" else "收获成熟作物")
                            }
                            if (careLabel != null) {
                                OutlinedButton(
                                    onClick = onCareSingle,
                                    enabled = !busy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(careLabel)
                                }
                            }
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (careLabel != null) {
                                OutlinedButton(
                                    onClick = onCareSingle,
                                    enabled = !busy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                ) {
                                    Text(careLabel)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = { onFertilize("normal") },
                                    enabled = !busy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("施普通肥")
                                }
                                Button(
                                    onClick = { onFertilize("organic") },
                                    enabled = !busy && !organicExhausted,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(if (organicExhausted) "有机肥已满" else "施有机肥")
                                }
                            }
                        }
                    }
                }

                if (!isLocked && (land.couldUpgrade || (upgradeInfo?.mode == "upgrade" && upgradeInfo.serverAllowed))) {
                    OutlinedButton(
                        onClick = onUpgradeLands,
                        enabled = !busy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("批量解锁与升级")
                    }
                }
            }
        }
    }
}

// ---- Bag View ----

@Composable
private fun BagContent(
    payload: BagPayload?,
    onItemClick: (BagItem) -> Unit,
) {
    val apple = LocalAppleColors.current
    var category by remember { mutableStateOf("all") }

    val allItems = payload?.items.orEmpty()
    val filtered = remember(allItems, category) {
        when (category) {
            "all" -> allItems
            "fruit" -> allItems.filter { it.itemType == 6 }
            "mutant" -> allItems.filter { it.itemType == 17 }
            "seed" -> allItems.filter { it.itemType == 5 }
            else -> allItems.filter { it.itemType !in listOf(5, 6, 17) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val cats = listOf(
                "all" to "全部",
                "fruit" to "果实",
                "mutant" to "超变",
                "seed" to "种子",
                "tool" to "道具",
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { (key, label) ->
                    AppleFilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                AppleGroupedCard {
                    AppleEmptyState(
                        icon = Icons.Rounded.Inventory2,
                        title = "该分类暂无物品",
                        subtitle = "切换上方分类或下拉刷新背包库存",
                    )
                }
            }
        } else {
            item {
                AppleGroupedCard {
                    filtered.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RemoteAssetImage(
                                url = item.image,
                                contentDescription = item.name,
                                fallbackIcon = Icons.Rounded.Inventory2,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(apple.elevated)
                                    .padding(4.dp),
                            )
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
                                    if (item.locked) {
                                        Icon(
                                            Icons.Rounded.Lock,
                                            contentDescription = "已上锁",
                                            tint = apple.orange,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                                val sub = buildString {
                                    if (item.hoursText.isNotBlank()) append(item.hoursText)
                                    else if (item.price > 0L) append("单价 ${item.price} ${item.priceUnit}")
                                    else if (item.description.isNotBlank()) append(item.description)
                                }
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = apple.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "×${formatNumber(item.count)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (index < filtered.lastIndex) {
                            AppleHairlineDivider(startIndent = 70.dp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BagItemActionSheet(
    item: BagItem,
    maxUseCount: Int,
    error: String?,
    busy: Boolean,
    onUse: (Int) -> Unit,
    onSellAll: () -> Unit,
    onSetLocked: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var requestedCount by remember(item.key) { mutableIntStateOf(1) }
    val useCount = requestedCount.coerceAtMost(maxUseCount)

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
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
                    url = item.image,
                    contentDescription = item.name,
                    fallbackIcon = Icons.Rounded.Inventory2,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(apple.card)
                        .padding(6.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                    val status = when {
                        item.locked -> " · 已上锁"
                        item.canChangeLock -> " · 未上锁"
                        else -> ""
                    }
                    Text("库存 ×${formatNumber(item.count)}$status", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                }
                if (item.canChangeLock) {
                    AppleSwitch(
                        checked = item.locked,
                        onCheckedChange = onSetLocked,
                        enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = "物品上锁" },
                    )
                }
            }

            if (item.description.isNotBlank()) {
                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                    Text(item.description, style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
                }
            }

            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = apple.red)
            }

            if (maxUseCount > 0) {
                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("使用数量", style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                        AppleQuantityStepper(
                            value = useCount,
                            max = maxUseCount,
                            onChange = { requestedCount = it },
                            enabled = !busy,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onUse(useCount) },
                        enabled = !busy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("使用 ×$useCount")
                    }
                }
            } else if (item.isFertilizer && !item.locked && item.count > 0L) {
                Text("化肥容器剩余空间不足", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            }

            if (item.sellable && !item.locked) {
                Button(
                    onClick = onSellAll,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = apple.orange),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.Sell, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("全部出售 (${formatNumber(item.count * item.price)} ${item.priceUnit})")
                }
            }
        }
    }
}

// ---- Pets View ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PetsContent(
    snapshot: PetSnapshot?,
    busy: Boolean,
    onClaimGifts: () -> Unit,
    onUseFood: (Int, Int) -> Unit,
    onDeployDog: (Int) -> Unit,
    onWithdrawDog: () -> Unit,
) {
    val apple = LocalAppleColors.current
    if (snapshot == null) {
        AppleLoadingState()
        return
    }

    val activeDog = snapshot.dogs.firstOrNull { it.active }
    val activeTint = petRarityColor(activeDog?.rarity ?: 0)
    var selectedFoodId by remember { mutableStateOf<Int?>(null) }
    val protectFraction = if (snapshot.maxProtectDurationSec > 0L) {
        (snapshot.protectDurationSec.toFloat() / snapshot.maxProtectDurationSec.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RemoteAssetImage(
                        url = activeDog?.image.orEmpty(),
                        contentDescription = null,
                        fallbackIcon = Icons.Rounded.Pets,
                        fallbackTint = activeTint,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeTint.copy(alpha = 0.14f))
                            .padding(4.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = activeDog?.name ?: "未上场宠物",
                                style = MaterialTheme.typography.titleMedium,
                                color = apple.primaryText,
                            )
                            if (activeDog != null && activeDog.rarityLabel.isNotBlank()) {
                                AppleStatusPill(activeDog.rarityLabel, activeTint, showDot = false)
                            }
                        }
                        Text(
                            text = if (snapshot.protectDurationSec > 0L) {
                                "护主剩余 ${formatDurationSec(snapshot.protectDurationSec)}"
                            } else "当前无护主时长",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = apple.secondaryText,
                        )
                    }
                    if (snapshot.pendingGiftCount > 0) {
                        Button(
                            onClick = onClaimGifts,
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("礼包 (${snapshot.pendingGiftCount})")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { protectFraction },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = apple.blue,
                    trackColor = apple.track,
                    strokeCap = StrokeCap.Round,
                )
            }
        }

        if (snapshot.foods.isNotEmpty()) {
            item { AppleSectionHeader(title = "补充狗粮") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(snapshot.foods) { food ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = apple.card,
                            border = BorderStroke(0.5.dp, apple.separator),
                        ) {
                            Column(
                                modifier = Modifier.width(126.dp).padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                RemoteAssetImage(
                                    url = food.image,
                                    contentDescription = food.name,
                                    fallbackIcon = Icons.Rounded.Pets,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = apple.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "库存 ${food.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = apple.secondaryText,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { selectedFoodId = food.id },
                                    enabled = !busy && snapshot.maxFeedCount(food) > 0,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.fillMaxWidth().height(30.dp),
                                ) {
                                    Text("喂食", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { AppleSectionHeader(title = "护主犬") }
        item {
            AppleGroupedCard {
                snapshot.dogs.forEachIndexed { idx, dog ->
                    val rarityColor = petRarityColor(dog.rarity)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RemoteAssetImage(
                            url = dog.image,
                            contentDescription = dog.name,
                            fallbackIcon = Icons.Rounded.Pets,
                            fallbackTint = rarityColor,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(rarityColor.copy(alpha = 0.14f))
                                .padding(4.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(dog.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                if (dog.rarityLabel.isNotBlank()) {
                                    AppleStatusPill(dog.rarityLabel, rarityColor, showDot = false)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dog.skillDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.secondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        when {
                            dog.active -> OutlinedButton(
                                onClick = onWithdrawDog,
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) { Text("收回") }

                            dog.owned -> Button(
                                onClick = { onDeployDog(dog.id) },
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) { Text("上场") }

                            else -> Text("未拥有", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                        }
                    }
                    if (idx < snapshot.dogs.lastIndex) {
                        AppleHairlineDivider(startIndent = 72.dp)
                    }
                }
            }
        }
    }

    snapshot.foods.firstOrNull { it.id == selectedFoodId }?.let { food ->
        val limit = snapshot.maxFeedCount(food)
        var requestedCount by remember(food.id) { mutableIntStateOf(1) }
        val count = requestedCount.coerceAtMost(limit)
        AlertDialog(
            onDismissRequest = { selectedFoodId = null },
            title = { Text("使用 ${food.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (limit > 0) "当前最多可用 $limit 份" else "剩余护主时长已无法补充这款狗粮")
                    AppleQuantityStepper(value = count, max = limit, onChange = { requestedCount = it }, enabled = !busy)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedFoodId = null
                        onUseFood(food.id, count)
                    },
                    enabled = !busy && count > 0,
                ) { Text("确认喂食") }
            },
            dismissButton = { TextButton(onClick = { selectedFoodId = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun petRarityColor(rarity: Int): Color {
    val apple = LocalAppleColors.current
    return when (rarity) {
        2 -> apple.green
        3 -> apple.blue
        4 -> apple.orange
        else -> apple.secondaryText
    }
}

@Composable
private fun ActionPillButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AppleActionPill(
        label = label,
        primary = primary,
        enabled = enabled,
        accentColor = LocalAppleColors.current.emerald,
        onClick = onClick,
    )
}

private fun soilColorForLevel(level: Int): Color = when (level) {
    2 -> SoilRed
    3 -> SoilBlack
    4 -> SoilGold
    5 -> SoilPurpleGold
    6 -> SoilBlueCrystal
    else -> SoilNormal
}

private fun soilLabelForLevel(level: Int): String = when (level) {
    2 -> "红土"
    3 -> "黑土"
    4 -> "金土"
    5 -> "紫金"
    6 -> "蓝晶"
    else -> "普通"
}
