package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.AccountSettingsModel
import com.qqfarm.mobile.data.model.BagPriorityItem
import com.qqfarm.mobile.data.model.RanchBagItem
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.theme.LocalAppleColors

internal fun availableCubStacks(bag: List<RanchBagItem>): List<BagPriorityItem> =
    bag.filter { it.animalId > 0 && !it.locked && it.count > 0L }
        .sortedWith(compareByDescending<RanchBagItem> { it.animalId }.thenBy { it.id })
        .groupBy { it.id }.map { (id, stacks) ->
            val first = stacks.first()
            BagPriorityItem(id, first.name, stacks.sumOf { it.count }, first.image, first.husbandryType)
        }

// 缺货物品保留原有位置。新入库的物品接在已保存的顺序后。
internal fun mergeBagPriority(saved: List<Int>, reorderedStock: List<Int>): List<Int> {
    val visible = reorderedStock.toSet()
    val ordered = reorderedStock.iterator()
    return buildList {
        saved.distinct().forEach { add(if (it in visible) ordered.next() else it) }
        ordered.forEach { add(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BagPrioritySheet(
    accountId: String,
    connected: Boolean,
    ranch: Boolean,
    apiClient: FarmApiClient,
    model: AccountSettingsModel,
    onApply: (AccountSettingsModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    var group by remember { mutableIntStateOf(0) }
    var orders by remember(model, ranch) { mutableStateOf(
        if (ranch) listOf(model.ranchCoopCubPriority, model.ranchShedCubPriority) else listOf(model.bagSeedPriority)
    ) }
    var stock by remember(accountId, connected, ranch) { mutableStateOf<List<BagPriorityItem>?>(null) }
    var error by remember(accountId, connected, ranch) { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(accountId, connected, ranch, revision) {
        if (!connected) return@LaunchedEffect
        stock = null
        error = ""
        try {
            stock = if (ranch) {
                val view = apiClient.fetchRanchView(accountId, "bag")
                if (!view.base.unlocked) throw ApiException(view.base.reason.ifBlank { "牧场尚未解锁" })
                availableCubStacks(view.bag)
            } else apiClient.fetchBagSeeds(accountId)
        } catch (e: ApiException) {
            error = e.message
        }
    }
    val housing = if (group == 0) "窝" else "棚"
    val order = orders[group]
    val visible = remember(stock, order, housing) {
        val ranks = order.withIndex().associate { (index, id) -> id to index }
        stock.orEmpty().filter { !ranch || it.housing == housing }
            .sortedBy { ranks[it.id] ?: Int.MAX_VALUE }
    }
    fun move(from: Int, to: Int) {
        val ids = visible.map { it.id }.toMutableList()
        ids.add(to, ids.removeAt(from))
        orders = orders.toMutableList().also { it[group] = mergeBagPriority(order, ids) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (ranch) "背包幼崽优先顺序" else "背包种子优先顺序",
                        style = MaterialTheme.typography.titleLarge,
                        color = apple.primaryText,
                    )
                    Text(
                        text = "排在前面的会优先使用，点击箭头调整顺序",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
                IconButton(
                    onClick = { revision++ },
                    enabled = connected && (stock != null || error.isNotEmpty()),
                ) {
                    if (connected && stock == null && error.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                }
            }
            if (ranch) AppleSegmentedControl(listOf("窝", "棚"), group, onSelect = { group = it })
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    !connected -> item {
                        AppleGroupedCard {
                            AppleEmptyState(
                                icon = Icons.Rounded.Inventory2,
                                title = "账号未在线",
                                subtitle = "账号上线后才能读取背包并调整优先顺序",
                            )
                        }
                    }
                    error.isNotEmpty() -> item {
                        AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                            Text(error, style = MaterialTheme.typography.bodyMedium, color = apple.red)
                        }
                    }
                    stock == null -> item {
                        AppleLoadingState(Modifier.fillMaxWidth().height(180.dp))
                    }
                    visible.isEmpty() -> item {
                        AppleGroupedCard {
                            AppleEmptyState(
                                icon = Icons.Rounded.Inventory2,
                                title = if (ranch) "暂无可放养的${housing}幼崽" else "暂无可种植的背包种子",
                                subtitle = if (ranch) "在牧场商店购买未上锁幼崽后会显示在这里" else "背包中有可用种子后可在此调整优先顺序",
                            )
                        }
                    }
                }
                itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppleRadius.tile)),
                        shape = RoundedCornerShape(AppleRadius.tile),
                        color = apple.card,
                        border = BorderStroke(0.5.dp, apple.separator),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (index == 0) apple.emerald.copy(alpha = 0.14f) else apple.track),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = if (index == 0) apple.emerald else apple.secondaryText,
                                )
                            }
                            RemoteAssetImage(
                                url = item.image,
                                contentDescription = null,
                                fallbackIcon = Icons.Rounded.Inventory2,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(AppleRadius.control))
                                    .background(apple.elevated)
                                    .padding(4.dp),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = apple.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "库存 ×${formatCompactCount(item.count)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = apple.secondaryText,
                                )
                            }
                            IconButton(
                                enabled = index > 0,
                                onClick = { move(index, index - 1) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "上移${item.name}")
                            }
                            IconButton(
                                enabled = index < visible.lastIndex,
                                onClick = { move(index, index + 1) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "下移${item.name}")
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(AppleRadius.tile),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) { Text("取消") }
                Button(
                    onClick = {
                        val next = if (ranch) model.copy(ranchCoopCubPriority = orders[0], ranchShedCubPriority = orders[1])
                            else model.copy(bagSeedPriority = orders[0])
                        onApply(next)
                    },
                    enabled = stock != null,
                    shape = RoundedCornerShape(AppleRadius.tile),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) { Text("完成") }
            }
        }
    }
}
