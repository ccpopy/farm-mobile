package com.qqfarm.mobile.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.AccountSettingsModel
import com.qqfarm.mobile.data.model.RanchAnimalOption
import com.qqfarm.mobile.data.model.SchedulerSummary
import com.qqfarm.mobile.data.model.SeedOption
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleIconBadge
import com.qqfarm.mobile.ui.components.AppleSearchField
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSettingRow
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.components.AppleSwitchRow
import com.qqfarm.mobile.ui.components.EqualHeightRow
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

private enum class AutomationEditor {
    STRATEGY,
    SEED_PICKER,
    FARM,
    RANCH,
    FERTILIZER,
    FRIENDS,
    MYSTERY,
}

private val STRATEGY_OPTIONS = listOf(
    "max_exp" to "最大经验/时",
    "max_fert_exp" to "最大普通肥经验/时",
    "max_profit" to "最大净利润/时",
    "max_fert_profit" to "最大普通肥净利润/时",
    "level" to "最高等级作物",
    "preferred" to "指定优先种子",
    "bag_priority" to "背包种子优先",
)

private val FERTILIZER_OPTIONS = listOf(
    "both" to "普通 + 有机",
    "smart" to "普通 + 快成熟有机（智能）",
    "normal" to "仅普通化肥",
    "organic" to "仅有机化肥",
    "none" to "不自动施肥",
)

private val FERTILIZER_LAND_TYPES = listOf(
    "blue-crystal" to "蓝晶土地",
    "purple-gold" to "紫金土地",
    "gold" to "金土地",
    "black" to "黑土地",
    "red" to "红土地",
    "normal" to "普通土地",
)

private val RANCH_STRATEGY_OPTIONS = listOf(
    "level" to "按已解锁等级",
    "preferred" to "指定动物",
    "bag_priority" to "优先背包幼崽",
)

private val FARM_SWITCHES = listOf(
    "farm" to "自动种植收获",
    "farm_harvest_tiangong" to "自动收获天工作物",
    "farm_push" to "推送触发巡田",
    "skip_own_weed_bug" to "巡田时跳过一键务农",
    "land_upgrade" to "自动升级土地",
    "task" to "自动做任务",
    "sell" to "自动卖果实",
    "fertilizer_gift" to "自动填充化肥",
)

private val RANCH_SWITCHES = listOf(
    "ranch_harvest" to "自动收获",
    "ranch_produce" to "自动生产",
    "ranch_buy" to "自动补栏（金券）",
    "ranch_upgrade" to "自动升级建筑（金券）",
    "ranch_sell" to "自动出售产物",
    "ranch_clean" to "自动清理",
    "ranch_grass" to "自动补充牧草",
    "ranch_grass_buy" to "牧草不足时购买（金券）",
    "ranch_fill_cans" to "自动填充库存罐头",
    "ranch_feed_normal" to "普通罐头喂养",
    "ranch_feed_organic" to "有机罐头喂养",
    "ranch_task" to "自动领取任务奖励",
    "ranch_share" to "每日分享礼包",
    "ranch_daily_benefit" to "每日商城福利",
)

private val RANCH_FRIEND_SWITCHES = listOf(
    "ranch_friend_steal" to "自动偷取好友产物",
    "ranch_friend_clean" to "自动帮好友清理",
    "ranch_stroke" to "自动抚摸动物",
    "ranch_friend_mosquito" to "自动放蚊子",
)

private val FARM_FRIEND_SWITCHES = listOf(
    "friend" to "自动好友互动",
    "friend_steal" to "自动偷菜",
    "friend_help" to "自动帮忙",
    "friend_bad" to "自动捣乱",
    "friend_help_exp_limit" to "经验满不帮忙",
    "friend_help_protect_dog_ignore_exp_limit" to "护主犬经验满仍帮忙",
)

private val MYSTERY_CURRENCIES = listOf(
    "mystery_shop_allow_gold" to "金币",
    "mystery_shop_allow_coupon" to "点券",
    "mystery_shop_allow_gold_bean" to "金豆豆",
    "mystery_shop_allow_diamond" to "钻石",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()

    var settings by remember(accountId) { mutableStateOf<AccountSettingsModel?>(null) }
    var scheduler by remember(accountId) { mutableStateOf<SchedulerSummary?>(null) }
    var ranchOptions by remember { mutableStateOf<List<RanchAnimalOption>>(emptyList()) }
    var seeds by remember(accountId) { mutableStateOf<List<SeedOption>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var dirty by remember(accountId) { mutableStateOf(false) }
    var activeEditor by remember { mutableStateOf<AutomationEditor?>(null) }
    var animalPickerKind by remember { mutableStateOf<String?>(null) }

    suspend fun loadAll() {
        if (accountId.isBlank() || loading) return
        loading = true
        try {
            supervisorScope {
                val cfgJob = async { runCatching { apiClient.fetchAccountSettings(accountId) } }
                val schedJob = async { runCatching { apiClient.fetchSchedulerStatus(accountId) } }
                val ranchJob = async { runCatching { apiClient.fetchRanchAutomationOptions() } }
                val seedsJob = if (isConnected) {
                    async { runCatching { apiClient.fetchSeedCatalog(accountId) } }
                } else null

                cfgJob.await()
                    .onSuccess {
                        settings = it
                        dirty = false
                    }
                    .onFailure { onMessage(it.message ?: "读取自动化配置失败") }
                schedJob.await().onSuccess { scheduler = it }
                ranchJob.await().onSuccess { if (it.isNotEmpty()) ranchOptions = it }
                seedsJob?.await()?.onSuccess { seeds = it }
            }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(accountId, isConnected) { loadAll() }

    fun mutate(transform: (AccountSettingsModel) -> AccountSettingsModel) {
        val current = settings ?: return
        settings = transform(current)
        dirty = true
    }

    fun toggleBool(key: String, checked: Boolean) {
        mutate { model ->
            model.copy(automationBooleans = model.automationBooleans + (key to checked))
        }
    }

    fun boolOf(model: AccountSettingsModel, key: String, fallback: Boolean = false): Boolean =
        model.automationBooleans[key] ?: fallback

    fun saveSettingsNow(closeEditor: Boolean = false) {
        val current = settings ?: return
        if (accountId.isBlank() || saving) return
        saving = true
        scope.launch {
            try {
                val (ok, msg) = apiClient.saveAccountSettings(accountId, current)
                onMessage(msg)
                if (ok) {
                    if (settings == current) {
                        dirty = false
                        if (closeEditor) activeEditor = null
                    }
                    runCatching { apiClient.fetchSchedulerStatus(accountId) }
                        .onSuccess { scheduler = it }
                }
            } catch (e: ApiException) {
                onMessage(e.message)
            } finally {
                saving = false
            }
        }
    }

    if (accountId.isBlank()) {
        AppleEmptyState(
            icon = Icons.Rounded.Tune,
            title = "未选择账号",
            subtitle = "请先切换或登录账号，再管理对应的自动化设置",
        )
        return
    }

    val cfg = settings

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { scope.launch { loadAll() } },
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        if (cfg == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    AppleEmptyState(
                        icon = Icons.Rounded.Tune,
                        title = "未能读取自动化配置",
                        subtitle = "请检查服务器连接后下拉刷新",
                        actionLabel = "重新加载",
                        onAction = { scope.launch { loadAll() } },
                    )
                }
            }
            return@PullToRefreshBox
        }

        val farmEnabled = boolOf(cfg, "farm", false)
        val ranchEnabled = boolOf(cfg, "ranch_harvest", false) || boolOf(cfg, "ranch_produce", false)
        val fertEnabled = cfg.fertilizerMode != "none"
        val friendEnabled = boolOf(cfg, "friend", false)
        val mysteryEnabled = boolOf(cfg, "mystery_shop_auto_buy", false)
        val activeModuleCount = listOf(farmEnabled, ranchEnabled, fertEnabled, friendEnabled, mysteryEnabled).count { it }

        val strategyLabel = STRATEGY_OPTIONS.firstOrNull { it.first == cfg.plantingStrategy }?.second ?: cfg.plantingStrategy
        val ranchStrategyLabel = RANCH_STRATEGY_OPTIONS.firstOrNull { it.first == cfg.ranchStrategy }?.second ?: cfg.ranchStrategy
        val fertLabel = FERTILIZER_OPTIONS.firstOrNull { it.first == cfg.fertilizerMode }?.second ?: cfg.fertilizerMode
        val seedName = seeds.firstOrNull { it.seedId == cfg.preferredSeedId }?.name
            ?: if (cfg.preferredSeedId > 0L) "ID ${cfg.preferredSeedId}" else "未指定"

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppleIconBadge(icon = Icons.Rounded.Tune, tint = apple.indigo, size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.currentAccount?.let { it.name.ifBlank { it.displayTitle } } ?: accountId,
                                style = MaterialTheme.typography.titleMedium,
                                color = apple.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val schedText = when {
                                scheduler == null -> "已对齐上游官方自动化配置"
                                scheduler!!.workerRunning -> "运行中定时任务 ${scheduler!!.workerTaskCount} 项"
                                scheduler!!.workerError.isNotBlank() -> scheduler!!.workerError
                                else -> "账号未运行"
                            }
                            Text(
                                text = schedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (scheduler?.workerRunning == true) apple.emerald else apple.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (dirty) {
                            AppleStatusPill("有未保存修改", apple.orange, showDot = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    EqualHeightRow(spacing = 8.dp) {
                        AutoStatTile(
                            value = "$activeModuleCount / 5",
                            label = "模块已启用",
                            color = apple.emerald,
                            modifier = Modifier.weight(1f),
                        )
                        AutoStatTile(
                            value = strategyLabel,
                            label = "种植策略",
                            color = apple.green,
                            modifier = Modifier.weight(1f),
                        )
                        AutoStatTile(
                            value = ranchStrategyLabel,
                            label = "养殖策略",
                            color = apple.orange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                AppleSectionHeader(
                    title = "策略与巡查节奏",
                    actionLabel = if (dirty) "重置" else null,
                    onAction = if (dirty) ({ scope.launch { loadAll() } }) else null,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppleGroupedCard {
                    AppleSettingRow(
                        icon = Icons.Rounded.Eco,
                        iconTint = apple.green,
                        title = "种植策略与种子",
                        subtitle = if (cfg.plantingStrategy == "preferred") {
                            "$strategyLabel · $seedName"
                        } else {
                            "$strategyLabel · 农场 ${cfg.farmMinSec}~${cfg.farmMaxSec}s"
                        },
                        showChevron = true,
                        onClick = { activeEditor = AutomationEditor.STRATEGY },
                    )
                    AppleHairlineDivider()
                    AppleSettingRow(
                        icon = Icons.Rounded.Schedule,
                        iconTint = apple.blue,
                        title = "巡查间隔与种植节律",
                        subtitle = "农场 ${cfg.farmMinSec}~${cfg.farmMaxSec}s · 好友 ${cfg.friendMinSec}~${cfg.friendMaxSec}s · ${if (cfg.plantOrderRandom) "随机顺序" else "顺序种植"}",
                        showChevron = true,
                        onClick = { activeEditor = AutomationEditor.STRATEGY },
                    )
                }
            }

            item {
                AppleSectionHeader(title = "自动化模块")
                Spacer(modifier = Modifier.height(4.dp))
                AppleGroupedCard {
                    ModuleToggleRow(
                        icon = Icons.Rounded.Agriculture,
                        iconTint = apple.green,
                        title = "日常农场",
                        subtitle = buildString {
                            append(if (farmEnabled) "已开启巡田" else "已关闭自动巡田")
                            append(" · ")
                            append(if (boolOf(cfg, "sell")) "自动卖果" else "保留果实")
                            append(" · ")
                            append("领邮件 ${cfg.emailClaimHour ?: 10}:00")
                        },
                        checked = farmEnabled,
                        onCheckedChange = { toggleBool("farm", it) },
                        onClickDetail = { activeEditor = AutomationEditor.FARM },
                    )
                    AppleHairlineDivider()
                    ModuleToggleRow(
                        icon = Icons.Rounded.Pets,
                        iconTint = apple.orange,
                        title = "日常牧场与养殖",
                        subtitle = "$ranchStrategyLabel · 巡查 ${cfg.ranchCheckMinutes} 分钟 · 牧草阈值 ${cfg.ranchGrassThresholdPercent}%",
                        checked = ranchEnabled,
                        onCheckedChange = { on ->
                            mutate { model ->
                                model.copy(
                                    automationBooleans = model.automationBooleans +
                                        ("ranch_harvest" to on) +
                                        ("ranch_produce" to on),
                                )
                            }
                        },
                        onClickDetail = { activeEditor = AutomationEditor.RANCH },
                    )
                    AppleHairlineDivider()
                    AppleSettingRow(
                        icon = Icons.Rounded.Science,
                        iconTint = apple.emerald,
                        title = "施肥管理",
                        subtitle = "$fertLabel · 适用 ${cfg.fertilizerLandTypes.size} 类土地${if (boolOf(cfg, "fertilizer_buy_organic") || boolOf(cfg, "fertilizer_buy_normal")) " · 自动补肥中" else ""}",
                        showChevron = true,
                        onClick = { activeEditor = AutomationEditor.FERTILIZER },
                    )
                    AppleHairlineDivider()
                    ModuleToggleRow(
                        icon = Icons.Rounded.People,
                        iconTint = apple.blue,
                        title = "好友互动",
                        subtitle = buildString {
                            append(if (friendEnabled) "自动偷/帮" else "已暂停好友互动")
                            if (cfg.quietHoursEnabled) append(" · 静默 ${cfg.quietHoursStart}-${cfg.quietHoursEnd}")
                            if (boolOf(cfg, "friend_auto_accept")) append(" · 自动过申请")
                        },
                        checked = friendEnabled,
                        onCheckedChange = { toggleBool("friend", it) },
                        onClickDetail = { activeEditor = AutomationEditor.FRIENDS },
                    )
                    AppleHairlineDivider()
                    ModuleToggleRow(
                        icon = Icons.Rounded.Storefront,
                        iconTint = apple.indigo,
                        title = "神秘商人",
                        subtitle = buildString {
                            append(if (boolOf(cfg, "mystery_shop_auto_buy")) "自动购买开启" else "仅提醒或手动购买")
                            if (boolOf(cfg, "mystery_shop_arrival_notify")) append(" · 到货提醒")
                        },
                        checked = mysteryEnabled,
                        onCheckedChange = { toggleBool("mystery_shop_auto_buy", it) },
                        onClickDetail = { activeEditor = AutomationEditor.MYSTERY },
                    )
                }
            }

            if (dirty) {
                item {
                    Button(
                        onClick = { saveSettingsNow(closeEditor = false) },
                        enabled = !saving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("保存自动化设置", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }

    if (cfg != null) {
        when (activeEditor) {
            AutomationEditor.STRATEGY -> StrategyEditorSheet(
                model = cfg,
                seeds = seeds,
                saving = saving,
                onChange = { next -> settings = next; dirty = true },
                onPickSeed = {
                    activeEditor = AutomationEditor.SEED_PICKER
                },
                onSave = { saveSettingsNow(closeEditor = true) },
                onDismiss = { activeEditor = null },
            )
            AutomationEditor.SEED_PICKER -> SeedPickerSheet(
                seeds = seeds,
                selectedSeedId = cfg.preferredSeedId,
                onSelect = { id ->
                    mutate { it.copy(preferredSeedId = id) }
                    activeEditor = AutomationEditor.STRATEGY
                },
                onDismiss = { activeEditor = AutomationEditor.STRATEGY },
            )
            AutomationEditor.FARM -> FarmAutomationSheet(
                model = cfg,
                saving = saving,
                onToggleBool = ::toggleBool,
                onChange = { next -> settings = next; dirty = true },
                onSave = { saveSettingsNow(closeEditor = true) },
                onDismiss = { activeEditor = null },
            )
            AutomationEditor.RANCH -> RanchAutomationSheet(
                model = cfg,
                ranchOptions = ranchOptions,
                saving = saving,
                onToggleBool = ::toggleBool,
                onChange = { next -> settings = next; dirty = true },
                onPickAnimal = { kind -> animalPickerKind = kind },
                onSave = { saveSettingsNow(closeEditor = true) },
                onDismiss = { activeEditor = null },
            )
            AutomationEditor.FERTILIZER -> FertilizerAutomationSheet(
                model = cfg,
                saving = saving,
                onToggleBool = ::toggleBool,
                onChange = { next -> settings = next; dirty = true },
                onSave = { saveSettingsNow(closeEditor = true) },
                onDismiss = { activeEditor = null },
            )
            AutomationEditor.FRIENDS -> FriendsAutomationSheet(
                model = cfg,
                saving = saving,
                onToggleBool = ::toggleBool,
                onChange = { next -> settings = next; dirty = true },
                onSave = { saveSettingsNow(closeEditor = true) },
                onDismiss = { activeEditor = null },
            )
            AutomationEditor.MYSTERY -> MysteryAutomationSheet(
                model = cfg,
                saving = saving,
                onToggleBool = ::toggleBool,
                onSave = { saveSettingsNow(closeEditor = true) },
                onDismiss = { activeEditor = null },
            )
            null -> Unit
        }

        val kind = animalPickerKind
        if (kind != null) {
            RanchAnimalPickerSheet(
                kind = kind,
                options = ranchOptions.filter { it.kind == kind },
                selectedValue = if (kind == "窝") cfg.ranchCoopAnimal else cfg.ranchShedAnimal,
                onSelect = { chosen ->
                    mutate {
                        if (kind == "窝") it.copy(ranchCoopAnimal = chosen) else it.copy(ranchShedAnimal = chosen)
                    }
                    animalPickerKind = null
                },
                onDismiss = { animalPickerKind = null },
            )
        }
    }
}

@Composable
private fun AutoStatTile(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = apple.elevated,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

@Composable
private fun ModuleToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClickDetail: () -> Unit,
) {
    val apple = LocalAppleColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClickDetail)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleIconBadge(icon = icon, tint = iconTint, size = 34.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = apple.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = apple.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AppleSwitch(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "配置",
            tint = apple.secondaryText.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AutoStepperRow(
    title: String,
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
) {
    val apple = LocalAppleColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = apple.primaryText,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDecrease, enabled = canDecrease) {
            Icon(Icons.Rounded.Remove, contentDescription = "减少")
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            color = apple.primaryText,
        )
        IconButton(onClick = onIncrease, enabled = canIncrease) {
            Icon(Icons.Rounded.Add, contentDescription = "增加")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleSheetShell(
    title: String,
    subtitle: String,
    saving: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val apple = LocalAppleColors.current
    val isSaving by rememberUpdatedState(saving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !isSaving },
    )
    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            }
            AppleHairlineDivider(startIndent = 0.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
            AppleHairlineDivider(startIndent = 0.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !saving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Text("完成")
                }
                Button(
                    onClick = onSave,
                    enabled = !saving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Text(if (saving) "保存中..." else "保存并生效")
                }
            }
        }
    }
}

@Composable
private fun StrategyEditorSheet(
    model: AccountSettingsModel,
    seeds: List<SeedOption>,
    saving: Boolean,
    onChange: (AccountSettingsModel) -> Unit,
    onPickSeed: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    ModuleSheetShell(
        title = "种植与巡查策略",
        subtitle = "对齐服务端种植优选策略及农场/好友巡查间隔",
        saving = saving,
        onSave = onSave,
        onDismiss = onDismiss,
    ) {
        AppleSectionHeader(title = "种植策略")
        AppleGroupedCard {
            STRATEGY_OPTIONS.forEachIndexed { idx, (code, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(model.copy(plantingStrategy = code)) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                    if (model.plantingStrategy == code) {
                        Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (model.plantingStrategy == "preferred") {
            val matched = seeds.firstOrNull { it.seedId == model.preferredSeedId }
            AppleGroupedCard {
                AppleSettingRow(
                    icon = Icons.Rounded.Eco,
                    iconTint = apple.green,
                    title = "指定优先种子",
                    subtitle = matched?.name ?: if (model.preferredSeedId > 0L) "种子 ID ${model.preferredSeedId}" else "请选择优先种植的种子",
                    showChevron = true,
                    onClick = onPickSeed,
                )
            }
        }

        AppleSectionHeader(title = "巡查间隔与种植节奏")
        AppleGroupedCard {
            AutoStepperRow(
                title = "农场巡查最小 (秒)",
                valueText = "${model.farmMinSec}s",
                canDecrease = model.farmMinSec > 1,
                onDecrease = {
                    val nextMin = (model.farmMinSec - 1).coerceAtLeast(1)
                    onChange(model.copy(farmMinSec = nextMin, farmMaxSec = model.farmMaxSec.coerceAtLeast(nextMin)))
                },
                onIncrease = {
                    val nextMin = model.farmMinSec + 1
                    onChange(model.copy(farmMinSec = nextMin, farmMaxSec = model.farmMaxSec.coerceAtLeast(nextMin)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AutoStepperRow(
                title = "农场巡查最大 (秒)",
                valueText = "${model.farmMaxSec}s",
                canDecrease = model.farmMaxSec > model.farmMinSec,
                onDecrease = {
                    onChange(model.copy(farmMaxSec = (model.farmMaxSec - 1).coerceAtLeast(model.farmMinSec)))
                },
                onIncrease = {
                    onChange(model.copy(farmMaxSec = model.farmMaxSec + 1))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AutoStepperRow(
                title = "好友任务最小 (秒)",
                valueText = "${model.friendMinSec}s",
                canDecrease = model.friendMinSec > 1,
                onDecrease = {
                    val nextMin = (model.friendMinSec - 1).coerceAtLeast(1)
                    onChange(model.copy(friendMinSec = nextMin, friendMaxSec = model.friendMaxSec.coerceAtLeast(nextMin)))
                },
                onIncrease = {
                    val nextMin = model.friendMinSec + 1
                    onChange(model.copy(friendMinSec = nextMin, friendMaxSec = model.friendMaxSec.coerceAtLeast(nextMin)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AutoStepperRow(
                title = "好友任务最大 (秒)",
                valueText = "${model.friendMaxSec}s",
                canDecrease = model.friendMaxSec > model.friendMinSec,
                onDecrease = {
                    onChange(model.copy(friendMaxSec = (model.friendMaxSec - 1).coerceAtLeast(model.friendMinSec)))
                },
                onIncrease = {
                    onChange(model.copy(friendMaxSec = model.friendMaxSec + 1))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Eco,
                iconTint = apple.emerald,
                title = "随机打乱种植顺序",
                checked = model.plantOrderRandom,
                onCheckedChange = { onChange(model.copy(plantOrderRandom = it)) },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AutoStepperRow(
                title = "地块种植延迟 (秒)",
                valueText = "${model.plantDelaySeconds}s",
                canDecrease = model.plantDelaySeconds > 0,
                canIncrease = model.plantDelaySeconds < 60,
                onDecrease = { onChange(model.copy(plantDelaySeconds = (model.plantDelaySeconds - 1).coerceAtLeast(0))) },
                onIncrease = { onChange(model.copy(plantDelaySeconds = (model.plantDelaySeconds + 1).coerceAtMost(60))) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeedPickerSheet(
    seeds: List<SeedOption>,
    selectedSeedId: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    var manualSeedId by remember(selectedSeedId) {
        mutableStateOf(if (selectedSeedId > 0L) selectedSeedId.toString() else "")
    }

    val filtered = remember(seeds, search) {
        val kw = search.trim()
        if (kw.isEmpty()) seeds else seeds.filter {
            it.name.contains(kw, ignoreCase = true) || it.seedId.toString().contains(kw)
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
                .heightIn(max = 600.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("选择优先种子", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            if (seeds.isEmpty()) {
                OutlinedTextField(
                    value = manualSeedId,
                    onValueChange = { manualSeedId = it.filter(Char::isDigit) },
                    label = { Text("种子 ID") },
                    supportingText = { Text("账号上线后可拉取商店完整种子列表，也可直接输入种子 ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { manualSeedId.toLongOrNull()?.let(onSelect) },
                    enabled = manualSeedId.toLongOrNull()?.let { it > 0L } == true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) { Text("确定") }
            } else {
                AppleSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "搜索作物名或种子 ID",
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filtered, key = { it.seedId }) { seed ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(seed.seedId) },
                            shape = RoundedCornerShape(12.dp),
                            color = apple.card,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(seed.name, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                                    Text(
                                        text = "ID ${seed.seedId}" + if (seed.requiredLevel > 0) " · Lv.${seed.requiredLevel}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = apple.secondaryText,
                                    )
                                }
                                if (seed.seedId == selectedSeedId) {
                                    Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FarmAutomationSheet(
    model: AccountSettingsModel,
    saving: Boolean,
    onToggleBool: (String, Boolean) -> Unit,
    onChange: (AccountSettingsModel) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val hour = model.emailClaimHour ?: 10
    ModuleSheetShell(
        title = "日常农场设置",
        subtitle = "管理巡田、种植、任务、产物出售与邮箱领取",
        saving = saving,
        onSave = onSave,
        onDismiss = onDismiss,
    ) {
        AppleGroupedCard {
            FARM_SWITCHES.forEachIndexed { idx, (key, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.Agriculture,
                    iconTint = apple.green,
                    title = label,
                    checked = model.automationBooleans[key] ?: false,
                    onCheckedChange = { onToggleBool(key, it) },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            AutoStepperRow(
                title = "自动领取邮箱时间",
                valueText = "${hour} 时",
                onDecrease = {
                    val prev = if (hour <= 0) 23 else hour - 1
                    onChange(model.copy(emailClaimHour = prev))
                },
                onIncrease = {
                    val next = if (hour >= 23) 0 else hour + 1
                    onChange(model.copy(emailClaimHour = next))
                },
            )
        }
    }
}

@Composable
private fun RanchAutomationSheet(
    model: AccountSettingsModel,
    ranchOptions: List<RanchAnimalOption>,
    saving: Boolean,
    onToggleBool: (String, Boolean) -> Unit,
    onChange: (AccountSettingsModel) -> Unit,
    onPickAnimal: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val coopName = ranchOptions.firstOrNull { it.value == model.ranchCoopAnimal }?.label
        ?: if (model.ranchCoopAnimal == 0) "不购买" else "动物 #${model.ranchCoopAnimal}"
    val shedName = ranchOptions.firstOrNull { it.value == model.ranchShedAnimal }?.label
        ?: if (model.ranchShedAnimal == 0) "不购买" else "动物 #${model.ranchShedAnimal}"
    val needsPreferred = model.ranchStrategy == "preferred" ||
        (model.ranchStrategy == "bag_priority" && model.ranchBagFallback == "preferred")

    ModuleSheetShell(
        title = "日常牧场与养殖策略",
        subtitle = "对应牧场生产、补栏、牧草、罐头与牧场好友互动",
        saving = saving,
        onSave = onSave,
        onDismiss = onDismiss,
    ) {
        AppleSectionHeader(title = "养殖策略")
        AppleGroupedCard {
            RANCH_STRATEGY_OPTIONS.forEachIndexed { idx, (code, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(model.copy(ranchStrategy = code)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                    if (model.ranchStrategy == code) {
                        Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (model.ranchStrategy == "bag_priority") {
                AppleHairlineDivider(startIndent = 16.dp)
                val fallbackLabel = if (model.ranchBagFallback == "preferred") "指定动物" else "按已解锁等级"
                AppleSettingRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = "库存不足时回退",
                    trailingText = fallbackLabel,
                    showChevron = true,
                    onClick = {
                        val next = if (model.ranchBagFallback == "preferred") "level" else "preferred"
                        onChange(model.copy(ranchBagFallback = next))
                    },
                )
            }
            if (needsPreferred) {
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSettingRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = "窝 · 指定动物",
                    trailingText = coopName,
                    showChevron = true,
                    onClick = { onPickAnimal("窝") },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSettingRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = "棚 · 指定动物",
                    trailingText = shedName,
                    showChevron = true,
                    onClick = { onPickAnimal("棚") },
                )
            }
        }

        AppleSectionHeader(title = "日常牧场开关与阈值")
        AppleGroupedCard {
            val grassEnabled = model.automationBooleans["ranch_grass"] ?: true
            RANCH_SWITCHES.forEachIndexed { idx, (key, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = label,
                    checked = model.automationBooleans[key] ?: false,
                    enabled = if (key == "ranch_grass_buy") grassEnabled else true,
                    onCheckedChange = { onToggleBool(key, it) },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            AutoStepperRow(
                title = "牧草余量低于时补满",
                valueText = "${model.ranchGrassThresholdPercent}%",
                canDecrease = model.ranchGrassThresholdPercent > 5,
                canIncrease = model.ranchGrassThresholdPercent < 100,
                onDecrease = {
                    onChange(model.copy(ranchGrassThresholdPercent = (model.ranchGrassThresholdPercent - 5).coerceAtLeast(1)))
                },
                onIncrease = {
                    onChange(model.copy(ranchGrassThresholdPercent = (model.ranchGrassThresholdPercent + 5).coerceAtMost(100)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AutoStepperRow(
                title = "牧场巡查间隔",
                valueText = "${model.ranchCheckMinutes} 分钟",
                canDecrease = model.ranchCheckMinutes > 1,
                canIncrease = model.ranchCheckMinutes < 60,
                onDecrease = {
                    onChange(model.copy(ranchCheckMinutes = (model.ranchCheckMinutes - 1).coerceAtLeast(1)))
                },
                onIncrease = {
                    onChange(model.copy(ranchCheckMinutes = (model.ranchCheckMinutes + 1).coerceAtMost(60)))
                },
            )
        }

        AppleSectionHeader(title = "牧场好友互动")
        AppleGroupedCard {
            RANCH_FRIEND_SWITCHES.forEachIndexed { idx, (key, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.People,
                    iconTint = apple.blue,
                    title = label,
                    checked = model.automationBooleans[key] ?: false,
                    onCheckedChange = { onToggleBool(key, it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RanchAnimalPickerSheet(
    kind: String,
    options: List<RanchAnimalOption>,
    selectedValue: Int,
    onSelect: (Int) -> Unit,
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
                .heightIn(max = 580.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("$kind · 指定动物", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            val rows = listOf(RanchAnimalOption(0, "不购买", kind)) + options
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(rows, key = { it.value }) { opt ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(opt.value) },
                        shape = RoundedCornerShape(12.dp),
                        color = apple.card,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(opt.label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                            if (opt.value == selectedValue) {
                                Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
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
private fun FertilizerAutomationSheet(
    model: AccountSettingsModel,
    saving: Boolean,
    onToggleBool: (String, Boolean) -> Unit,
    onChange: (AccountSettingsModel) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val buyOrganic = model.automationBooleans["fertilizer_buy_organic"] ?: false
    val buyNormal = model.automationBooleans["fertilizer_buy_normal"] ?: false

    ModuleSheetShell(
        title = "施肥管理",
        subtitle = "配置施肥模式、范围土地与化肥余量自动购买",
        saving = saving,
        onSave = onSave,
        onDismiss = onDismiss,
    ) {
        AppleSectionHeader(title = "施肥策略")
        AppleGroupedCard {
            FERTILIZER_OPTIONS.forEachIndexed { idx, (code, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(model.copy(fertilizerMode = code)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                    if (model.fertilizerMode == code) {
                        Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (model.fertilizerMode == "smart") {
                AppleHairlineDivider(startIndent = 16.dp)
                AutoStepperRow(
                    title = "快成熟判定秒数",
                    valueText = "${model.fertilizerSmartSeconds}s",
                    canDecrease = model.fertilizerSmartSeconds > 30,
                    canIncrease = model.fertilizerSmartSeconds < 3600,
                    onDecrease = {
                        onChange(model.copy(fertilizerSmartSeconds = (model.fertilizerSmartSeconds - 30).coerceAtLeast(30)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerSmartSeconds = (model.fertilizerSmartSeconds + 30).coerceAtMost(3600)))
                    },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.emerald,
                title = "多季补肥",
                checked = model.automationBooleans["fertilizer_multi_season"] ?: false,
                onCheckedChange = { onToggleBool("fertilizer_multi_season", it) },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.blue,
                title = "显示手动施肥按钮",
                checked = model.automationBooleans["show_manual_fertilizer"] ?: true,
                onCheckedChange = { onToggleBool("show_manual_fertilizer", it) },
            )
        }

        AppleSectionHeader(title = "施肥范围（适用土地）")
        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FERTILIZER_LAND_TYPES.forEach { (key, label) ->
                    val selected = key in model.fertilizerLandTypes
                    AppleFilterChip(
                        selected = selected,
                        onClick = {
                            val next = if (selected) {
                                model.fertilizerLandTypes.filterNot { it == key }
                            } else {
                                model.fertilizerLandTypes + key
                            }
                            onChange(model.copy(fertilizerLandTypes = next))
                        },
                        label = { Text(label) },
                    )
                }
            }
        }

        AppleSectionHeader(title = "自动购买化肥")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.green,
                title = "自动购买有机化肥",
                checked = buyOrganic,
                onCheckedChange = { onToggleBool("fertilizer_buy_organic", it) },
            )
            if (buyOrganic) {
                AppleHairlineDivider(startIndent = 16.dp)
                AutoStepperRow(
                    title = "有机肥单次购买数量",
                    valueText = "${model.fertilizerBuyOrganicCount} 个",
                    canDecrease = model.fertilizerBuyOrganicCount > 1,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyOrganicCount = (model.fertilizerBuyOrganicCount - 1).coerceAtLeast(1)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyOrganicCount = (model.fertilizerBuyOrganicCount + 1).coerceAtMost(10000)))
                    },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AutoStepperRow(
                    title = "有机肥触发阈值 (小时)",
                    valueText = "${model.fertilizerBuyOrganicThresholdHours}h",
                    canDecrease = model.fertilizerBuyOrganicThresholdHours > 1,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyOrganicThresholdHours = (model.fertilizerBuyOrganicThresholdHours - 1).coerceAtLeast(1)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyOrganicThresholdHours = (model.fertilizerBuyOrganicThresholdHours + 1).coerceAtMost(990)))
                    },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.emerald,
                title = "自动购买普通化肥",
                checked = buyNormal,
                onCheckedChange = { onToggleBool("fertilizer_buy_normal", it) },
            )
            if (buyNormal) {
                AppleHairlineDivider(startIndent = 16.dp)
                AutoStepperRow(
                    title = "普通肥单次购买数量",
                    valueText = "${model.fertilizerBuyNormalCount} 个",
                    canDecrease = model.fertilizerBuyNormalCount > 1,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyNormalCount = (model.fertilizerBuyNormalCount - 1).coerceAtLeast(1)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyNormalCount = (model.fertilizerBuyNormalCount + 1).coerceAtMost(10000)))
                    },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AutoStepperRow(
                    title = "普通肥触发阈值 (小时)",
                    valueText = "${model.fertilizerBuyNormalThresholdHours}h",
                    canDecrease = model.fertilizerBuyNormalThresholdHours > 1,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyNormalThresholdHours = (model.fertilizerBuyNormalThresholdHours - 1).coerceAtLeast(1)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyNormalThresholdHours = (model.fertilizerBuyNormalThresholdHours + 1).coerceAtMost(990)))
                    },
                )
            }
            if (buyOrganic || buyNormal) {
                AppleHairlineDivider(startIndent = 16.dp)
                AutoStepperRow(
                    title = "检测间隔 (分钟)",
                    valueText = "${model.fertilizerBuyCheckIntervalMinutes} 分钟",
                    canDecrease = model.fertilizerBuyCheckIntervalMinutes > 5,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyCheckIntervalMinutes = (model.fertilizerBuyCheckIntervalMinutes - 5).coerceAtLeast(1)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyCheckIntervalMinutes = (model.fertilizerBuyCheckIntervalMinutes + 5).coerceAtMost(1440)))
                    },
                )
            }
        }
    }
}

@Composable
private fun FriendsAutomationSheet(
    model: AccountSettingsModel,
    saving: Boolean,
    onToggleBool: (String, Boolean) -> Unit,
    onChange: (AccountSettingsModel) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val autoAccept = model.automationBooleans["friend_auto_accept"] ?: false
    ModuleSheetShell(
        title = "好友互动与申请规则",
        subtitle = "控制好友偷帮捣乱、静默时段及自动通过好友申请条件",
        saving = saving,
        onSave = onSave,
        onDismiss = onDismiss,
    ) {
        AppleSectionHeader(title = "互动行为")
        AppleGroupedCard {
            FARM_FRIEND_SWITCHES.forEachIndexed { idx, (key, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.People,
                    iconTint = apple.blue,
                    title = label,
                    checked = model.automationBooleans[key] ?: false,
                    onCheckedChange = { onToggleBool(key, it) },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            AutoStepperRow(
                title = "蹲守偷菜延迟 (秒)",
                valueText = "${model.stealDelaySeconds}s",
                canDecrease = model.stealDelaySeconds > 0,
                canIncrease = model.stealDelaySeconds < 300,
                onDecrease = { onChange(model.copy(stealDelaySeconds = (model.stealDelaySeconds - 1).coerceAtLeast(0))) },
                onIncrease = { onChange(model.copy(stealDelaySeconds = (model.stealDelaySeconds + 1).coerceAtMost(300))) },
            )
        }

        AppleSectionHeader(title = "好友静默时段")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Schedule,
                iconTint = apple.indigo,
                title = "启用静默时段",
                checked = model.quietHoursEnabled,
                onCheckedChange = { onChange(model.copy(quietHoursEnabled = it)) },
            )
            if (model.quietHoursEnabled) {
                AppleHairlineDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = model.quietHoursStart,
                        onValueChange = { onChange(model.copy(quietHoursStart = it)) },
                        label = { Text("开始 HH:mm") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = model.quietHoursEnd,
                        onValueChange = { onChange(model.copy(quietHoursEnd = it)) },
                        label = { Text("结束 HH:mm") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.Eco,
                    iconTint = apple.green,
                    title = "静默时段继续照料自家农场",
                    checked = model.quietContinueFarm,
                    onCheckedChange = { onChange(model.copy(quietContinueFarm = it)) },
                )
            }
        }

        AppleSectionHeader(title = "自动通过好友申请")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.People,
                iconTint = apple.emerald,
                title = "自动通过好友申请",
                checked = autoAccept,
                onCheckedChange = { onToggleBool("friend_auto_accept", it) },
            )
            if (autoAccept) {
                AppleHairlineDivider(startIndent = 16.dp)
                AutoStepperRow(
                    title = "申请人最低等级 (0 为不限)",
                    valueText = "Lv.${model.autoAcceptFriendMinLevel}",
                    canDecrease = model.autoAcceptFriendMinLevel > 0,
                    onDecrease = { onChange(model.copy(autoAcceptFriendMinLevel = (model.autoAcceptFriendMinLevel - 1).coerceAtLeast(0))) },
                    onIncrease = { onChange(model.copy(autoAcceptFriendMinLevel = (model.autoAcceptFriendMinLevel + 1).coerceAtMost(200))) },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.People,
                    iconTint = apple.orange,
                    title = "不低于自身等级",
                    checked = model.autoAcceptRequireOwnLevel,
                    onCheckedChange = { onChange(model.copy(autoAcceptRequireOwnLevel = it)) },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.People,
                    iconTint = apple.red,
                    title = "按收偷比过滤 (${model.autoAcceptHarvestStealHarvest}:${model.autoAcceptHarvestStealSteal})",
                    subtitle = "不合格的申请会直接拒绝",
                    checked = model.autoAcceptHarvestStealEnabled,
                    onCheckedChange = { onChange(model.copy(autoAcceptHarvestStealEnabled = it)) },
                )
            }
        }
    }
}

@Composable
private fun MysteryAutomationSheet(
    model: AccountSettingsModel,
    saving: Boolean,
    onToggleBool: (String, Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val autoBuy = model.automationBooleans["mystery_shop_auto_buy"] ?: false
    ModuleSheetShell(
        title = "神秘商人自动化",
        subtitle = "配置自动购买开关、提醒与允许消费的货币范围",
        saving = saving,
        onSave = onSave,
        onDismiss = onDismiss,
    ) {
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Storefront,
                iconTint = apple.indigo,
                title = "自动购买神秘商人货品",
                checked = autoBuy,
                onCheckedChange = { onToggleBool("mystery_shop_auto_buy", it) },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Storefront,
                iconTint = apple.blue,
                title = "到货提醒",
                checked = model.automationBooleans["mystery_shop_arrival_notify"] ?: false,
                onCheckedChange = { onToggleBool("mystery_shop_arrival_notify", it) },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Storefront,
                iconTint = apple.emerald,
                title = "购买提醒",
                checked = model.automationBooleans["mystery_shop_purchase_notify"] ?: false,
                enabled = autoBuy,
                onCheckedChange = { onToggleBool("mystery_shop_purchase_notify", it) },
            )
        }

        if (autoBuy) {
            AppleSectionHeader(title = "允许自动使用的货币")
            AppleGroupedCard {
                MYSTERY_CURRENCIES.forEachIndexed { idx, (key, label) ->
                    if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                    AppleSwitchRow(
                        icon = Icons.Rounded.Storefront,
                        iconTint = apple.orange,
                        title = label,
                        checked = model.automationBooleans[key] ?: false,
                        onCheckedChange = { onToggleBool(key, it) },
                    )
                }
            }
        }
    }
}
