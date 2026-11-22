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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.FriendBlacklistItem
import com.qqfarm.mobile.data.model.FriendItem
import com.qqfarm.mobile.data.model.GameSpace
import com.qqfarm.mobile.data.model.InteractRecord
import com.qqfarm.mobile.data.model.FriendLandsPayload
import com.qqfarm.mobile.data.model.RanchAnimal
import com.qqfarm.mobile.data.model.RanchFriendSummary
import com.qqfarm.mobile.data.model.RanchQuote
import com.qqfarm.mobile.data.model.RanchViewSnapshot
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.RanchQuoteConfirmSheet
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.formatDurationSec
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppAppearance
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.ui.components.spaceAccent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.qqfarm.mobile.ui.components.EqualHeightRow
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleSearchField
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FriendsScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    gameSpace: GameSpace,
    onMessage: (String) -> Unit,
) {
    if (gameSpace == GameSpace.RANCH) {
        RanchFriendsContent(
            session = session,
            apiClient = apiClient,
            onMessage = onMessage,
        )
        return
    }
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()

    var subTab by remember { mutableIntStateOf(0) }
    var refreshing by remember(accountId, isConnected, subTab) { mutableStateOf(false) }
    var loadError by remember(accountId, isConnected, subTab) { mutableStateOf("") }
    val loadedTabs = remember(accountId, isConnected) { mutableStateMapOf<Int, Boolean>() }
    var busyOp by remember { mutableStateOf(false) }
    var friendListReady by remember(accountId, isConnected) { mutableStateOf(false) }
    var friendActionStates by remember(accountId, isConnected) { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }

    var friends by remember(accountId, isConnected) { mutableStateOf<List<FriendItem>>(emptyList()) }
    var records by remember(accountId, isConnected) { mutableStateOf<List<InteractRecord>>(emptyList()) }
    var blacklist by remember(accountId, isConnected) { mutableStateOf<List<FriendBlacklistItem>>(emptyList()) }

    var searchKeyword by remember { mutableStateOf("") }
    var onlyActionable by remember { mutableStateOf(false) }
    var inspectedFriend by remember(accountId, isConnected) { mutableStateOf<FriendItem?>(null) }

    suspend fun loadTab(forceSync: Boolean = false) {
        if (!isConnected || refreshing || busyOp) return
        val tab = subTab
        refreshing = true
        loadError = ""
        try {
            when (tab) {
                0 -> {
                    friendListReady = false
                    friendActionStates = emptyMap()
                    val res = apiClient.fetchFriends(accountId, forceSync = forceSync)
                    friends = res.friends
                    friendListReady = res.state == "ready"
                    loadedTabs[0] = true
                    try {
                        blacklist = apiClient.fetchFriendBlacklist(accountId)
                        loadedTabs[2] = true
                    } catch (e: ApiException) {
                        onMessage(e.message)
                    }
                }
                1 -> records = apiClient.fetchInteractRecords(accountId)
                2 -> blacklist = apiClient.fetchFriendBlacklist(accountId)
            }
            loadedTabs[tab] = true
        } catch (e: ApiException) {
            loadError = e.message
            if (loadedTabs[tab] == true) onMessage(e.message)
        } finally {
            refreshing = false
        }
    }

    fun updateFriendActions(gid: Long, snapshot: FriendLandsPayload?) {
        friendActionStates = friendActionStates + (gid to (snapshot != null))
        if (snapshot == null) return
        friends = friends.map { friend ->
            if (friend.gid != gid) friend else friend.copy(
                stealNum = snapshot.stealableCount,
                dryNum = snapshot.dryCount,
                weedNum = snapshot.weedCount,
                insectNum = snapshot.insectCount,
            )
        }
    }

    fun operateFromList(friend: FriendItem, opType: String) {
        if (busyOp || refreshing) return
        busyOp = true
        scope.launch {
            try {
                val message = try {
                    apiClient.operateFriend(accountId, friend.gid, opType)
                } catch (e: ApiException) {
                    e.message
                }
                onMessage(message)
                try {
                    updateFriendActions(friend.gid, apiClient.fetchFriendLands(accountId, friend.gid))
                } catch (e: ApiException) {
                    updateFriendActions(friend.gid, null)
                    onMessage("好友状态刷新失败：${e.message}")
                }
            } finally {
                busyOp = false
            }
        }
    }

    LaunchedEffect(accountId, isConnected, subTab) {
        if (isConnected) loadTab(forceSync = subTab == 0)
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
                options = listOf("好友", "访客", "黑名单"),
                selectedIndex = subTab,
                onSelect = { if (!busyOp) subTab = it },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .swipeToSwitchTabs(subTab, 3, !busyOp) { idx -> if (!busyOp) subTab = idx },
        ) {

            if (!isConnected) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                        AppleEmptyState(
                            icon = Icons.Rounded.Group,
                            title = "账号未在线",
                            subtitle = "请先启动并登录当前账号后再查看好友农场与访客记录",
                        )
                    }
                }
            } else {
                val hasData = loadedTabs[subTab] == true
                PullToRefreshBox(
                isRefreshing = refreshing && hasData,
                onRefresh = { scope.launch { loadTab(subTab == 0) } },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (!hasData) {
                    if (loadError.isBlank()) {
                        AppleLoadingState()
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                                AppleEmptyState(
                                    icon = Icons.Rounded.Group,
                                    title = "好友数据读取失败",
                                    subtitle = loadError,
                                    actionLabel = "重新加载",
                                    onAction = { scope.launch { loadTab(subTab == 0) } },
                                )
                            }
                        }
                    }
                    return@PullToRefreshBox
                }
                when (subTab) {
                    0 -> {
                        val blockedGids = remember(blacklist) { blacklist.map { it.gid }.toSet() }
                        val filteredFriends = remember(friends, searchKeyword, onlyActionable, friendListReady, friendActionStates) {
                            val kw = searchKeyword.trim().lowercase()
                            friends.filter { f ->
                                val matchesKw = kw.isEmpty() || f.name.lowercase().contains(kw) || f.gid.toString().contains(kw)
                                val statusKnown = friendActionStates[f.gid] ?: friendListReady
                                val matchesAction = !onlyActionable || (statusKnown && (f.stealNum > 0 || f.needsCareCount > 0))
                                matchesKw && matchesAction
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AppleSearchField(
                                        value = searchKeyword,
                                        onValueChange = { searchKeyword = it },
                                        placeholder = "搜索好友昵称或 GID",
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { scope.launch { loadTab(forceSync = true) } },
                                        enabled = !refreshing && !busyOp,
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = "强制同步好友")
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AppleFilterChip(
                                        selected = onlyActionable,
                                        onClick = { onlyActionable = !onlyActionable },
                                        label = { Text("仅看可偷/可帮") },
                                    )
                                    Text(
                                        text = "共 ${filteredFriends.size} 位",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = apple.secondaryText,
                                    )
                                }
                            }

                            if (filteredFriends.isEmpty()) {
                                item {
                                    AppleGroupedCard {
                                        AppleEmptyState(
                                            icon = Icons.Rounded.Group,
                                            title = if (friends.isEmpty()) "暂无好友数据" else "没有匹配的好友",
                                            subtitle = "点击右上角刷新图标可向服务器强制同步好友名单",
                                        )
                                    }
                                }
                            } else {
                                itemsIndexed(filteredFriends, key = { _, f -> f.gid }) { _, friend ->
                                    val isBlocked = blockedGids.contains(friend.gid)
                                    val statusKnown = !refreshing && (friendActionStates[friend.gid] ?: friendListReady)
                                    val canSteal = statusKnown && friend.stealNum > 0
                                    val canHelp = statusKnown && friend.needsCareCount > 0
                                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !busyOp && !refreshing) { inspectedFriend = friend },
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            RemoteAssetImage(
                                                url = friend.avatarUrl,
                                                contentDescription = friend.name,
                                                fallbackIcon = Icons.Rounded.Person,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(apple.track),
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Text(
                                                        text = friend.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = apple.primaryText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false),
                                                    )
                                                    if (friend.level > 0) {
                                                        Text(
                                                            text = "Lv.${friend.level}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = apple.secondaryText,
                                                        )
                                                    }
                                                    if (isBlocked) {
                                                        AppleStatusPill("已屏蔽", apple.red, showDot = false)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    if (canSteal) AppleStatusPill("可偷 ${friend.stealNum}", apple.orange, showDot = false)
                                                    if (statusKnown && friend.dryNum > 0) AppleStatusPill("缺水 ${friend.dryNum}", apple.cyan, showDot = false)
                                                    if (statusKnown && friend.weedNum > 0) AppleStatusPill("杂草 ${friend.weedNum}", apple.green, showDot = false)
                                                    if (statusKnown && friend.insectNum > 0) AppleStatusPill("虫害 ${friend.insectNum}", apple.red, showDot = false)
                                                    if (!canSteal && !canHelp) {
                                                        Text(
                                                            text = "GID ${friend.gid}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = apple.secondaryText,
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            FriendActionChip("偷菜", canSteal, !busyOp && canSteal, Modifier.weight(1f)) {
                                                operateFromList(friend, "steal")
                                            }
                                            FriendActionChip("帮务农", canHelp, !busyOp && canHelp, Modifier.weight(1f)) {
                                                operateFromList(friend, "farming")
                                            }
                                            FriendActionChip("查看农田", false, !busyOp && !refreshing, Modifier.weight(1f)) {
                                                inspectedFriend = friend
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        ) {
                            if (records.isEmpty()) {
                                item {
                                    AppleGroupedCard {
                                        AppleEmptyState(
                                            icon = Icons.Rounded.History,
                                            title = "暂无访客记录",
                                            subtitle = "好友来访偷菜、帮忙或捣乱的记录会显示在这里",
                                        )
                                    }
                                }
                            } else {
                                item {
                                    AppleGroupedCard {
                                        records.forEachIndexed { idx, rec ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                RemoteAssetImage(
                                                    url = rec.friendAvatar,
                                                    contentDescription = rec.friendName,
                                                    fallbackIcon = Icons.Rounded.Person,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(apple.track),
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(rec.friendName, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                                    Text(rec.actionText, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                                                }
                                                if (rec.timeText.isNotBlank()) {
                                                    Text(
                                                        text = rec.timeText,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                        color = apple.secondaryText,
                                                    )
                                                }
                                            }
                                            if (idx < records.lastIndex) {
                                                AppleHairlineDivider(startIndent = 66.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        ) {
                            if (blacklist.isEmpty()) {
                                item {
                                    AppleGroupedCard {
                                        AppleEmptyState(
                                            icon = Icons.Rounded.Block,
                                            title = "黑名单为空",
                                            subtitle = "加入黑名单的好友将被自动巡查跳过",
                                        )
                                    }
                                }
                            } else {
                                item {
                                    AppleGroupedCard {
                                        blacklist.forEachIndexed { idx, item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                RemoteAssetImage(
                                                    url = item.avatarUrl,
                                                    contentDescription = item.name,
                                                    fallbackIcon = Icons.Rounded.Person,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(apple.track),
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                                    Text("GID ${item.gid}", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        scope.launch {
                                                            runCatching { apiClient.toggleFriendBlacklist(accountId, item.gid) }
                                                                .onSuccess { next -> blacklist = next; onMessage("已移出黑名单") }
                                                                .onFailure { onMessage(it.message ?: "操作失败") }
                                                        }
                                                    },
                                                ) { Text("移除") }
                                            }
                                            if (idx < blacklist.lastIndex) {
                                                AppleHairlineDivider(startIndent = 66.dp)
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
    }

    inspectedFriend?.let { friend ->
        FriendLandsSheet(
            friend = friend,
            accountId = accountId,
            isBlocked = blacklist.any { it.gid == friend.gid },
            apiClient = apiClient,
            onMessage = onMessage,
            onLandsLoaded = { updateFriendActions(friend.gid, it) },
            onBlacklistUpdated = { blacklist = it },
            onDismiss = { inspectedFriend = null },
        )
    }
}

@Composable
private fun FriendActionChip(
    label: String,
    highlighted: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val accent = enabled && highlighted
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else apple.elevated,
        border = BorderStroke(
            0.5.dp,
            if (accent) MaterialTheme.colorScheme.primary else apple.separator,
        ),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (accent) MaterialTheme.colorScheme.primary else if (enabled) apple.primaryText else apple.secondaryText,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendLandsSheet(
    friend: FriendItem,
    accountId: String,
    isBlocked: Boolean,
    apiClient: FarmApiClient,
    onMessage: (String) -> Unit,
    onLandsLoaded: (FriendLandsPayload?) -> Unit,
    onBlacklistUpdated: (List<FriendBlacklistItem>) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var payload by remember { mutableStateOf<FriendLandsPayload?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var clockTickMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            clockTickMs = SystemClock.elapsedRealtime()
        }
    }

    suspend fun loadLands() {
        loading = true
        errorMsg = ""
        try {
            val snapshot = apiClient.fetchFriendLands(accountId, friend.gid)
            payload = snapshot
            onLandsLoaded(snapshot)
        } catch (e: ApiException) {
            payload = null
            onLandsLoaded(null)
            errorMsg = e.message
        } finally {
            loading = false
        }
    }

    fun runAction(action: suspend () -> Unit) {
        if (loading || busy) return
        busy = true
        scope.launch {
            try {
                action()
            } catch (e: ApiException) {
                onMessage(e.message)
            } finally {
                busy = false
            }
        }
    }

    fun operate(opType: String) = runAction {
        val message = try {
            apiClient.operateFriend(accountId, friend.gid, opType)
        } catch (e: ApiException) {
            e.message
        }
        onMessage(message)
        loadLands()
    }

    LaunchedEffect(accountId, friend.gid) { loadLands() }
    val actionsEnabled = !loading && !busy && errorMsg.isBlank()

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RemoteAssetImage(
                    url = friend.avatarUrl,
                    contentDescription = friend.name,
                    fallbackIcon = Icons.Rounded.Person,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(apple.track),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.name, style = MaterialTheme.typography.titleLarge, color = apple.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("GID ${friend.gid} · Lv.${friend.level}", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                }
                TextButton(
                    onClick = {
                        runAction {
                            onBlacklistUpdated(apiClient.toggleFriendBlacklist(accountId, friend.gid))
                            onMessage(if (isBlocked) "已移出黑名单" else "已加入黑名单")
                        }
                    },
                    enabled = !loading && !busy,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) { Text(if (isBlocked) "解除屏蔽" else "屏蔽") }
                IconButton(onClick = { scope.launch { loadLands() } }, enabled = !loading && !busy) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { operate("steal") },
                    enabled = actionsEnabled && (payload?.stealableCount ?: 0) > 0,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) { Text("一键偷菜") }

                OutlinedButton(
                    onClick = { operate("farming") },
                    enabled = actionsEnabled && payload?.needsCare == true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) { Text("帮务农") }

                OutlinedButton(
                    onClick = { operate("bad") },
                    enabled = actionsEnabled && payload?.canDisrupt == true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) { Text("捣乱") }
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AppleLoadingState()
                }
            } else if (errorMsg.isNotBlank()) {
                AppleGroupedCard(contentPadding = PaddingValues(20.dp)) {
                    Text(errorMsg, style = MaterialTheme.typography.bodyMedium, color = apple.red)
                }
            } else {
                val lands = payload?.lands.orEmpty()
                val landColumns = LocalAppAppearance.current.landGridColumns
                val rows = lands.chunked(landColumns)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(rows) { _, row ->
                        EqualHeightRow(spacing = if (landColumns >= 4) 6.dp else 8.dp) {
                            row.forEach { land ->
                                AppleLandTile(
                                    land = land,
                                    clockTickMs = clockTickMs,
                                    compact = landColumns >= 4,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(landColumns - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PendingRanchFriendOp(
    val action: String,
    val input: JSONObject,
    val quote: RanchQuote,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RanchFriendsContent(
    session: SessionUiState,
    apiClient: FarmApiClient,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()

    var refreshing by remember(accountId, isConnected) { mutableStateOf(false) }
    var snapshot by remember(accountId, isConnected) { mutableStateOf<RanchViewSnapshot?>(null) }
    var loadError by remember(accountId, isConnected) { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var selectedFriend by remember(accountId, isConnected) { mutableStateOf<RanchFriendSummary?>(null) }
    var reportedEntry by remember(accountId) { mutableStateOf(false) }

    suspend fun loadFriendsList() {
        if (!isConnected || refreshing) return
        refreshing = true
        loadError = ""
        try {
            if (!reportedEntry) {
                reportedEntry = true
                val input = JSONObject().put("entry", "friends")
                try {
                    val quote = apiClient.quoteRanchAction(accountId, "reportEntry", input)
                    if (!quote.skipped) apiClient.executeRanchAction(accountId, "reportEntry", input, quote.signature)
                } catch (e: ApiException) {
                    onMessage(e.message)
                }
            }
            snapshot = apiClient.fetchRanchView(accountId, "friends")
        } catch (e: ApiException) {
            loadError = e.message
            if (snapshot != null) onMessage(e.message)
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(accountId, isConnected) {
        if (isConnected) loadFriendsList()
    }

    if (!isConnected) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(apple.background),
            contentAlignment = Alignment.Center,
        ) {
            AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                AppleEmptyState(
                    icon = Icons.Rounded.Pets,
                    title = "账号未在线",
                    subtitle = "请先启动当前账号后查看并拜访好友牧场",
                )
            }
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = refreshing && snapshot != null,
        onRefresh = { scope.launch { loadFriendsList() } },
        modifier = Modifier
            .fillMaxSize()
            .background(apple.background),
    ) {
        val current = snapshot
        if (current == null) {
            if (loadError.isBlank()) {
                AppleLoadingState()
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                        AppleEmptyState(
                            icon = Icons.Rounded.Group,
                            title = "好友牧场读取失败",
                            subtitle = loadError,
                            actionLabel = "重新加载",
                            onAction = { scope.launch { loadFriendsList() } },
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
                            subtitle = current.base.reason.ifBlank { "达到牧场解锁条件后即可拜访好友牧场" },
                        )
                    }
                }
            }
        } else {
            val friends = current.friends
            val filtered = remember(friends, keyword) {
                val kw = keyword.trim().lowercase()
                if (kw.isEmpty()) friends else friends.filter {
                    it.name.lowercase().contains(kw) || it.gid.lowercase().contains(kw)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppleSearchField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            placeholder = "搜索牧场好友或 GID",
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { scope.launch { loadFriendsList() } },
                            enabled = !refreshing,
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "刷新牧场好友")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "共 ${filtered.size} 位好友",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }

                if (filtered.isEmpty() && !refreshing) {
                    item {
                        AppleGroupedCard {
                            AppleEmptyState(
                                icon = Icons.Rounded.Group,
                                title = if (friends.isEmpty()) "暂无牧场好友" else "没有匹配的好友",
                                subtitle = "添加游戏好友后可以互相拜访牧场、偷取货架或抚摸巨大动物",
                            )
                        }
                    }
                } else {
                    itemsIndexed(filtered, key = { _, f -> f.gid }) { _, friend ->
                        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFriend = friend },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RemoteAssetImage(
                                    url = friend.avatarUrl,
                                    contentDescription = friend.name,
                                    fallbackIcon = Icons.Rounded.Person,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(apple.track),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = friend.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = apple.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (!refreshing && loadError.isBlank() && friend.canHelp) {
                                            AppleStatusPill(
                                                text = if (friend.helpCount > 0) "可清理 ${friend.helpCount} 处" else "可清理",
                                                color = apple.green,
                                                showDot = false,
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "GID ${friend.gid} · 点击拜访牧场",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = apple.secondaryText,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = apple.secondaryText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedFriend?.let { friend ->
        RanchFriendDetailSheet(
            accountId = accountId,
            friend = friend,
            apiClient = apiClient,
            onMessage = onMessage,
            onDismiss = {
                selectedFriend = null
                scope.launch { loadFriendsList() }
            },
        )
    }
}

private val ranchFriendTimeFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm")

@Composable
private fun RanchFriendAnimalTimes(animal: RanchAnimal, nowMs: Long) {
    if (animal.canHarvest) return
    val deadlines = remember(animal) {
        buildList {
            if (animal.deliveringUntilMs > 0L) add("产出" to animal.deliveringUntilMs)
            if (animal.nextAtMs > 0L && animal.nextAtMs != animal.deliveringUntilMs) {
                add((if (animal.stage == 4) "预计可收获" else "下一阶段") to animal.nextAtMs)
            }
        }
    }
    val color = LocalAppleColors.current.secondaryText
    deadlines.forEach { (label, deadline) ->
        val time = remember(deadline) { ranchFriendTimeFormat.format(Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault())) }
        val remaining = deadline - nowMs
        val status = if (remaining > 0L) "余 ${formatDurationSec((remaining + 999L) / 1000L)}" else "待刷新"
        Text("$label $time · $status", style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RanchFriendDetailSheet(
    accountId: String,
    friend: RanchFriendSummary,
    apiClient: FarmApiClient,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var loading by remember(accountId, friend.gid) { mutableStateOf(true) }
    var busy by remember(accountId, friend.gid) { mutableStateOf(false) }
    var snapshot by remember(accountId, friend.gid) { mutableStateOf<RanchViewSnapshot?>(null) }
    var errorMsg by remember(accountId, friend.gid) { mutableStateOf("") }
    var pendingOp by remember(accountId, friend.gid) { mutableStateOf<PendingRanchFriendOp?>(null) }

    suspend fun loadDetail() {
        loading = true
        errorMsg = ""
        try {
            val loaded = apiClient.fetchRanchView(accountId, "friends", hostGid = friend.gid)
            if (loaded.friendRanch == null) throw ApiException("未返回好友牧场数据")
            snapshot = loaded
        } catch (e: ApiException) {
            snapshot = null
            errorMsg = e.message
        } finally {
            loading = false
        }
    }

    fun requestQuote(action: String, input: JSONObject) {
        if (busy || loading || pendingOp != null || snapshot?.friendRanch?.sceneRestored != true) return
        busy = true
        scope.launch {
            try {
                val quote = apiClient.quoteRanchAction(accountId, action, input)
                if (quote.skipped) {
                    onMessage(quote.message.ifBlank { "当前没有可操作项目" })
                    loadDetail()
                } else {
                    pendingOp = PendingRanchFriendOp(action, input, quote)
                }
            } catch (e: ApiException) {
                onMessage(e.message)
                loadDetail()
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(accountId, friend.gid) {
        loadDetail()
    }

    val current = snapshot
    val ranch = current?.friendRanch
    var clockTickMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(ranch) {
        if (ranch != null) {
            while (true) {
                clockTickMs = SystemClock.elapsedRealtime()
                delay(1000L)
            }
        }
    }
    val actionsEnabled = !busy && !loading && pendingOp == null && ranch?.sceneRestored == true
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 580.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RemoteAssetImage(
                    url = friend.avatarUrl,
                    contentDescription = friend.name,
                    fallbackIcon = Icons.Rounded.Person,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(apple.track),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.name, style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
                    ranch?.let { d ->
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AppleStatusPill("牧场 Lv.${d.level}", spaceAccent(GameSpace.RANCH), showDot = false)
                            AppleStatusPill("可抚摸 ${d.strokeRemaining} 次", apple.blue, showDot = false)
                            AppleStatusPill("可放蚊子 ${d.mosquitoRemaining} 次", apple.indigo, showDot = false)
                        }
                    }
                }
                IconButton(onClick = { scope.launch { loadDetail() } }, enabled = !loading && !busy) {
                    if (loading && ranch != null) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                }
            }

            if (loading && ranch == null) {
                AppleLoadingState(Modifier.fillMaxWidth().height(180.dp))
            } else if (errorMsg.isNotBlank()) {
                AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                    Text(errorMsg, style = MaterialTheme.typography.bodyMedium, color = apple.red)
                }
            } else if (current != null && ranch != null) {
                val d = ranch
                val nowMs = current.base.serverNowMs + (clockTickMs - current.base.fetchedAtElapsedMs).coerceAtLeast(0L)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            requestQuote("cleanFriend", JSONObject().put("hostGid", friend.gid))
                        },
                        enabled = actionsEnabled && d.cleanableHazards > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("清理 (${d.cleanableHazards})")
                    }
                    Button(
                        onClick = {
                            requestQuote("steal", JSONObject().put("hostGid", friend.gid))
                        },
                        enabled = actionsEnabled && d.canSteal,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (d.canSteal) "偷取 (${d.stealableShelves})" else "暂无可偷")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (d.notice.isNotBlank()) {
                        item {
                            Surface(shape = RoundedCornerShape(12.dp), color = apple.orange.copy(alpha = 0.14f)) {
                                Text(d.notice, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = apple.orange)
                            }
                        }
                    }
                    d.career?.let { career -> item { RanchCareerStatistics(career, embedded = true) } }
                    items(d.ranches, key = { it.id }) { pen ->
                        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pen.name, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        AppleStatusPill(
                                            "蚊子 ${pen.mosquitoes}/${pen.mosquitoLimit}",
                                            if (pen.mosquitoes > 0) apple.indigo else apple.secondaryText,
                                            showDot = false,
                                        )
                                        val stealable = pen.shelves.count { it.canSteal }
                                        if (stealable > 0) AppleStatusPill("可偷取 $stealable", apple.green, showDot = false)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        requestQuote(
                                            "putMosquito",
                                            JSONObject().put("hostGid", friend.gid).put("ranchId", pen.id),
                                        )
                                    },
                                    enabled = actionsEnabled && pen.mosquitoRoom > 0 && d.mosquitoRemaining > 0,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("放蚊子", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            if (pen.animals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                pen.animals.forEachIndexed { idx, animal ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        RemoteAssetImage(
                                            url = animal.image,
                                            contentDescription = animal.name,
                                            fallbackIcon = Icons.Rounded.Pets,
                                            modifier = Modifier.size(34.dp),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(animal.name, style = MaterialTheme.typography.bodyMedium, color = apple.primaryText)
                                            Text(
                                                text = listOf(animal.stageName, animal.weightKg).filter { it.isNotBlank() }.joinToString(" · "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = apple.secondaryText,
                                            )
                                            RanchFriendAnimalTimes(animal, nowMs)
                                        }
                                        if (animal.canStroke && !animal.stroked && d.strokeRemaining > 0) {
                                            Button(
                                                onClick = {
                                                    requestQuote(
                                                        "stroke",
                                                        JSONObject()
                                                            .put("hostGid", friend.gid)
                                                            .put("ranchId", pen.id)
                                                            .put("animalId", animal.id),
                                                    )
                                                },
                                                enabled = actionsEnabled,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp),
                                            ) { Text("抚摸", style = MaterialTheme.typography.labelSmall) }
                                        } else if (animal.stroked) {
                                            AppleStatusPill("已抚摸", apple.green, showDot = false)
                                        }
                                    }
                                    if (idx < pen.animals.lastIndex) AppleHairlineDivider(startIndent = 44.dp)
                                }
                            }

                            if (pen.shelves.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                AppleHairlineDivider(startIndent = 0.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("货架产物", style = MaterialTheme.typography.labelMedium, color = apple.secondaryText)
                                Spacer(modifier = Modifier.height(6.dp))
                                pen.shelves.forEach { shelf ->
                                    val prodSummary = shelf.products.joinToString(" · ") { "${it.name}×${formatCompactCount(it.count)}" }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "货架 #${shelf.slot}: ${prodSummary.ifBlank { "空" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = apple.primaryText,
                                            modifier = Modifier.weight(1f),
                                        )
                                        AppleStatusPill(
                                            text = if (shelf.canSteal) "可偷取" else "不可偷",
                                            color = if (shelf.canSteal) apple.orange else apple.secondaryText,
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
    }

    pendingOp?.let { pending ->
        RanchQuoteConfirmSheet(
            quote = pending.quote,
            busy = busy,
            onDismiss = { if (!busy) pendingOp = null },
            onConfirm = {
                if (busy) return@RanchQuoteConfirmSheet
                busy = true
                scope.launch {
                    runCatching {
                        apiClient.executeRanchAction(
                            accountId = accountId,
                            action = pending.action,
                            input = pending.input,
                            expectedSignature = pending.quote.signature,
                        )
                    }.onSuccess { res ->
                        pendingOp = null
                        onMessage(res.notice.ifBlank { if (res.status == "skipped") "当前没有可操作项目" else "操作成功" })
                        loadDetail()
                    }.onFailure {
                        if (it is CancellationException) throw it
                        pendingOp = null
                        onMessage(it.message ?: "好友牧场操作失败")
                        loadDetail()
                    }
                    busy = false
                }
            },
        )
    }
}
