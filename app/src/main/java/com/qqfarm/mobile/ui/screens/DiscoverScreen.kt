package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.GameSpace
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs

@Composable
fun DiscoverScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    gameSpace: GameSpace,
    selectedSubTab: Int,
    onSelectSubTab: (Int) -> Unit,
    mallTab: Int,
    onSelectMallTab: (Int) -> Unit,
    onFullScreenOverlayChange: (Boolean) -> Unit,
    onRefreshOverview: () -> Unit = {},
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    var activityDetailOpen by remember { mutableStateOf(false) }
    val mallBusy = remember(gameSpace) { mutableStateOf(false) }

    LaunchedEffect(selectedSubTab, activityDetailOpen) {
        onFullScreenOverlayChange(selectedSubTab == 0 && activityDetailOpen)
    }

    DisposableEffect(Unit) {
        onDispose { onFullScreenOverlayChange(false) }
    }

    val options = if (gameSpace == GameSpace.RANCH) {
        listOf("牧场活动", "牧场商城", "节令日历")
    } else {
        listOf("活动", "商城", "节令日历")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        if (!(selectedSubTab == 0 && activityDetailOpen)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
            ) {
                AppleSegmentedControl(
                    options = options,
                    selectedIndex = selectedSubTab.coerceIn(0, 2),
                    onSelect = { if (!mallBusy.value) onSelectSubTab(it) },
                )
            }
        }

        // 整页左右滑动切换 活动 / 商城 / 节令日历；进入活动详情后由详情页自己的标签接管。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .swipeToSwitchTabs(
                    selectedIndex = selectedSubTab.coerceIn(0, 2),
                    count = 3,
                    enabled = !mallBusy.value && !(selectedSubTab == 0 && activityDetailOpen),
                ) { idx -> if (!mallBusy.value) onSelectSubTab(idx) },
        ) {
            when (selectedSubTab.coerceIn(0, 2)) {
                0 -> ActivityCenterScreen(
                    session = session,
                    apiClient = apiClient,
                    gameSpace = gameSpace,
                    onDetailOpenChange = { activityDetailOpen = it },
                    onMessage = onMessage,
                )
                1 -> MallScreen(
                    session = session,
                    apiClient = apiClient,
                    gameSpace = gameSpace,
                    selectedShopTab = mallTab,
                    onSelectShopTab = onSelectMallTab,
                    busy = mallBusy,
                    onBusyChange = { mallBusy.value = it },
                    onRefreshOverview = onRefreshOverview,
                    onMessage = onMessage,
                )
                2 -> SolarCalendarScreen(
                    session = session,
                    apiClient = apiClient,
                    onMessage = onMessage,
                )
            }
        }
    }
}
