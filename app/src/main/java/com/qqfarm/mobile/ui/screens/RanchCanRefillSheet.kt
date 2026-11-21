package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.RanchShopGood
import com.qqfarm.mobile.data.model.RanchViewSnapshot
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RanchCanRefillSheet(
    accountId: String,
    apiClient: FarmApiClient,
    onPurchased: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var slotType by remember(accountId) { mutableIntStateOf(6) }
    var catalog by remember(accountId, slotType) { mutableStateOf<RanchViewSnapshot?>(null) }
    var selected by remember(accountId, slotType) { mutableStateOf<RanchShopGood?>(null) }
    var loadError by remember(accountId, slotType) { mutableStateOf("") }
    var purchaseError by remember(accountId, slotType) { mutableStateOf("") }
    var loading by remember(accountId, slotType) { mutableStateOf(false) }
    var purchasing by remember(accountId) { mutableStateOf(false) }

    suspend fun loadOffers() {
        if (loading) return
        loading = true
        loadError = ""
        catalog = null
        try {
            catalog = apiClient.fetchRanchView(accountId, "cans", shopId = slotType)
        } catch (e: ApiException) {
            loadError = e.message
        } finally {
            loading = false
        }
    }

    LaunchedEffect(accountId, slotType) { loadOffers() }

    val current = catalog
    fun balanceOf(good: RanchShopGood): Long? =
        good.priceBalance ?: current?.base?.currencies?.firstOrNull { it.id == good.priceId }?.count

    val offer = selected
    if (offer != null) {
        val balance = balanceOf(offer)
        val verb = if (offer.free) "领取" else "购买"
        RanchMallAmountSheet(
            good = offer,
            verb = verb,
            balance = balance,
            maxCount = ranchPurchaseLimit(offer, balance),
            showAutoPlaceNote = false,
            pending = purchasing,
            onDismiss = { if (!purchasing) selected = null },
            onConfirm = { count ->
                if (purchasing) return@RanchMallAmountSheet
                purchaseError = ""
                purchasing = true
                scope.launch {
                    try {
                        val input = JSONObject()
                            .put("slotType", slotType)
                            .put("goodsId", offer.id)
                            .put("count", count)
                        val quote = apiClient.quoteRanchAction(accountId, "buyCan", input)
                        when {
                            quote.skipped -> purchaseError = quote.message.ifBlank { "当前商品暂不可购买" }
                            !ranchQuoteMatchesGoods(offer, count, quote) -> purchaseError = "商品价格或内容已变化，请重新选择"
                            else -> {
                                val result = apiClient.executeRanchAction(accountId, "buyCan", input, quote.signature)
                                if (result.status == "confirmed") {
                                    onMessage(result.notice.ifBlank { "已$verb ${offer.name} ×$count" })
                                    onDismiss()
                                    onPurchased()
                                    return@launch
                                }
                                purchaseError = result.notice.ifBlank { "当前没有可操作项目" }
                            }
                        }
                    } catch (e: ApiException) {
                        purchaseError = e.message
                    } finally {
                        purchasing = false
                    }
                    selected = null
                    loadOffers()
                }
            },
        )
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = apple.background) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("补充罐头", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            AppleSegmentedControl(
                options = listOf("常驻商品", "限时商品"),
                selectedIndex = slotType - 6,
                onSelect = { slotType = it + 6 },
            )
            if (purchaseError.isNotBlank()) Text(purchaseError, color = MaterialTheme.colorScheme.error)
            when {
                loadError.isNotBlank() -> {
                    Text(loadError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { scope.launch { loadOffers() } }) { Text("重新加载") }
                }
                current == null -> AppleLoadingState(Modifier.fillMaxWidth().height(84.dp))
                !current.base.unlocked -> Text(current.base.reason.ifBlank { "牧场暂未解锁" }, color = apple.secondaryText)
                current.canShopGoods.isEmpty() -> Text("当前没有罐头商品", color = apple.secondaryText)
                else -> AppleGroupedCard(contentPadding = PaddingValues(horizontal = 14.dp)) {
                    current.canShopGoods.forEachIndexed { index, good ->
                        val balance = balanceOf(good)
                        val maxCount = ranchPurchaseLimit(good, balance)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            RemoteAssetImage(
                                url = good.reward.image, contentDescription = null,
                                fallbackIcon = Icons.Rounded.Science, modifier = Modifier.size(40.dp),
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(good.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                Text(ranchGoodsContents(good), style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                                val price = if (good.free) "免费" else "单价 ${formatCompactCount(good.priceCount)} ${good.priceName}"
                                val quota = good.remaining?.let { " · 可购 $it 份" }.orEmpty()
                                Text("$price$quota", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                            }
                            TextButton(
                                onClick = { selected = good },
                                enabled = good.available && maxCount > 0,
                            ) {
                                Text(when {
                                    good.remaining == 0 -> "已达限购"
                                    !good.available -> "暂不可购"
                                    maxCount == 0 -> if (balance == null) "余额未知" else "余额不足"
                                    good.free -> "领取"
                                    else -> "购买"
                                })
                            }
                        }
                        if (index < current.canShopGoods.lastIndex) AppleHairlineDivider(startIndent = 50.dp)
                    }
                }
            }
        }
    }
}
