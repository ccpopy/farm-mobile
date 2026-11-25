@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.qqfarm.mobile.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qqfarm.mobile.data.model.CommerceItem
import com.qqfarm.mobile.data.model.SolarCalendarState
import kotlinx.datetime.toLocalDateTime
import com.qqfarm.mobile.data.model.SolarEvent
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleLoadingState
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.ScrollToTopOnTabReselect
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.components.swipeToSwitchTabs
import com.qqfarm.mobile.ui.state.SessionUiState
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.qqfarm.mobile.data.network.json.JSONObject
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SolarCalendarScreen(
    session: SessionUiState,
    apiClient: FarmApiClient,
    onMessage: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val scope = rememberCoroutineScope()
    val accountId = session.config.currentAccountId
    val isConnected = session.status?.connected == true && accountId.isNotBlank()

    var refreshing by remember(accountId, isConnected) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var state by remember(accountId, isConnected) { mutableStateOf<SolarCalendarState?>(null) }
    var errorMsg by remember(accountId, isConnected) { mutableStateOf("") }
    var selectedSeasonId by remember(accountId) { mutableIntStateOf(0) }
    var quizEvent by remember(accountId, isConnected) { mutableStateOf<SolarEvent?>(null) }

    var clockTickMs by remember { mutableLongStateOf(com.qqfarm.mobile.platform.monotonicMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            clockTickMs = com.qqfarm.mobile.platform.monotonicMillis()
        }
    }

    fun pickDefaultSeason(cal: SolarCalendarState, nowSec: Long): Int {
        if (cal.seasons.any { it.id == selectedSeasonId }) return selectedSeasonId
        val targetEventId = cal.events.firstOrNull { !it.directClaimed && it.endTimeSec >= nowSec }?.id
            ?: cal.defaultEventId
        return cal.seasons.firstOrNull { targetEventId in it.eventIds }?.id
            ?: cal.seasons.lastOrNull()?.id
            ?: 0
    }

    suspend fun loadCalendar() {
        if (!isConnected || refreshing) return
        refreshing = true
        errorMsg = ""
        try {
            val cal = apiClient.fetchSolarCalendar(accountId)
            state = cal
            selectedSeasonId = pickDefaultSeason(cal, cal.serverTimeSec)
        } catch (e: ApiException) {
            errorMsg = e.message
            if (state != null) onMessage(e.message)
        } finally {
            refreshing = false
        }
    }

    fun performAction(action: String, body: JSONObject) {
        if (busy || !isConnected) return
        busy = true
        scope.launch {
            runCatching { apiClient.operateSolarCalendar(accountId, action, body) }
                .onSuccess { res ->
                    quizEvent = null
                    val rewardMsg = res.rewards.joinToString("、") { "${it.name} ×${formatCompactCount(it.count)}" }
                    val msg = when {
                        rewardMsg.isNotBlank() -> rewardMsg
                        action == "answer" && res.correct == false -> "回答未中，明日再来"
                        else -> "领取成功"
                    }
                    onMessage(if (res.refreshRequired) "$msg（请刷新确认最新进度）" else msg)
                    if (res.calendar != null) {
                        state = res.calendar
                        selectedSeasonId = pickDefaultSeason(res.calendar, res.calendar.serverTimeSec)
                    } else {
                        loadCalendar()
                    }
                }
                .onFailure { onMessage(it.message ?: "节令操作失败") }
            busy = false
        }
    }

    LaunchedEffect(accountId, isConnected) {
        state = null
        selectedSeasonId = 0
        errorMsg = ""
        quizEvent = null
        if (isConnected) loadCalendar()
    }

    if (!isConnected) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppleGroupedCard(modifier = Modifier.padding(16.dp)) {
                AppleEmptyState(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "账号未在线",
                    subtitle = "请先启动当前账号后查看并领取节令日历奖励",
                )
            }
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = refreshing && state != null,
        onRefresh = { if (!busy) scope.launch { loadCalendar() } },
        modifier = Modifier.fillMaxSize(),
    ) {
        val current = state
        if (current == null) {
            if (errorMsg.isBlank()) {
                AppleLoadingState()
                return@PullToRefreshBox
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                item {
                    AppleGroupedCard {
                        AppleEmptyState(
                            icon = Icons.Rounded.CalendarMonth,
                            title = "节令日历读取失败",
                            subtitle = errorMsg,
                            actionLabel = "重新加载",
                            onAction = { scope.launch { loadCalendar() } },
                        )
                    }
                }
            }
            return@PullToRefreshBox
        }

        val elapsedSec = ((clockTickMs - current.receivedAtElapsedMs) / 1000L).coerceAtLeast(0L)
        val nowSec = current.serverTimeSec + elapsedSec
        val seasonIndex = current.seasons.indexOfFirst { it.id == selectedSeasonId }.coerceAtLeast(0)
        val season = current.seasons.getOrNull(seasonIndex)
        val filteredEvents = remember(current, season) {
            if (season == null) current.events else current.events.filter { it.id in season.eventIds }
        }
        val listState = rememberLazyListState()
        ScrollToTopOnTabReselect(2, listState)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .swipeToSwitchTabs(
                    selectedIndex = seasonIndex,
                    count = current.seasons.size,
                    enabled = !busy && current.seasons.size > 1,
                ) { nextIdx ->
                    current.seasons.getOrNull(nextIdx)?.let { selectedSeasonId = it.id }
                },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Season Filter Pills
            if (current.seasons.isNotEmpty()) {
                item {
                    val sealRed = Color(0xFFC0533F)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(current.seasons, key = { it.id }) { s ->
                            val selected = s.id == selectedSeasonId
                            AppleFilterChip(
                                selected = selected,
                                enabled = !busy,
                                accentColor = sealRed,
                                onClick = { selectedSeasonId = s.id },
                                label = {
                                    Text("${s.name} ${s.litCount}/${s.totalCount}")
                                },
                            )
                        }
                    }
                }
            }

            // Season Progress Summary Card
            if (season != null) {
                item {
                    val sealRed = Color(0xFFC0533F)
                    AppleGroupedCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${season.name}已点亮 ${season.litCount}/${season.totalCount}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                    color = apple.primaryText,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (i in 1..season.totalCount.coerceAtMost(12)) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (i <= season.litCount) sealRed else apple.track),
                                        )
                                    }
                                }
                            }
                            if (season.rewards.isNotEmpty()) {
                                Button(
                                    onClick = { performAction("season", JSONObject().put("id", season.id)) },
                                    enabled = !busy && season.canClaim,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        when {
                                            season.claimed -> "已领取"
                                            season.canClaim -> "领集齐礼"
                                            else -> "集齐可领"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                        if (season.rewards.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            SolarRewardChips(label = "集齐奖励", rewards = season.rewards)
                        }
                    }
                }
            }

            // Tear-Off Solar Term Event Cards
            items(filteredEvents, key = { it.id }) { event ->
                val done = event.directClaimed && (event.question.isBlank() || event.quizClaimed)
                val expired = !done && (nowSec > event.endTimeSec || event.status == 3)
                val upcoming = !done && !expired && nowSec < event.startTimeSec
                val phaseText = when {
                    done -> "已完成"
                    expired -> "已结束"
                    upcoming -> "${formatDistanceSec(event.startTimeSec - nowSec)}后开始"
                    else -> "进行中 · 剩 ${formatDistanceSec(event.endTimeSec - nowSec)}"
                }
                val phaseColor = when {
                    done -> apple.green
                    expired -> apple.secondaryText
                    upcoming -> apple.blue
                    else -> Color(0xFFC0533F)
                }

                AppleGroupedCard(
                    modifier = Modifier.alpha(if (expired) 0.68f else 1f),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        SolarCalendarLeaf(
                            startSec = event.startTimeSec,
                            endSec = event.endTimeSec,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = event.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = apple.primaryText,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                AppleStatusPill(text = phaseText, color = phaseColor, showDot = !done && !expired && !upcoming)
                            }
                            val lines = event.description.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                            val lunar = lines.firstOrNull()?.takeIf { it.startsWith("农历") }
                            val poems = if (lunar != null) lines.drop(1) else lines
                            if (!lunar.isNullOrBlank()) {
                                Text(lunar, style = MaterialTheme.typography.labelMedium, color = Color(0xFFC0533F))
                            }
                            if (poems.isNotEmpty()) {
                                Text(
                                    text = poems.joinToString("\n"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.secondaryText,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    AppleHairlineDivider(startIndent = 0.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct Claim Reward Row
                    val directAvailable = !busy && event.canClaim && nowSec in event.startTimeSec..event.endTimeSec
                    val directLabel = when {
                        event.directClaimed -> "已领取"
                        nowSec > event.endTimeSec || event.status == 3 -> "已过期"
                        nowSec < event.startTimeSec -> "${formatDistanceSec(event.startTimeSec - nowSec)}后可领"
                        else -> "领取"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SolarRewardChips(label = "节令奖励", rewards = event.rewards)
                        }
                        Button(
                            onClick = { performAction("claim", JSONObject().put("id", event.id)) },
                            enabled = directAvailable,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp),
                        ) { Text(directLabel, style = MaterialTheme.typography.labelMedium) }
                    }

                    // Quiz Reward Row
                    if (event.question.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val quizAvailable = !busy && event.canAnswer && nowSec in event.startTimeSec..event.endTimeSec
                        val quizLabel = when {
                            event.quizClaimed -> "已领取"
                            nowSec > event.endTimeSec || event.status == 3 -> "已过期"
                            nowSec < event.startTimeSec -> "${formatDistanceSec(event.startTimeSec - nowSec)}后开启"
                            event.quizWrongToday -> "明日再来"
                            else -> "答题"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                SolarRewardChips(label = "答题奖励", rewards = event.questionRewards)
                            }
                            OutlinedButton(
                                onClick = { quizEvent = event },
                                enabled = quizAvailable,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp),
                            ) { Text(quizLabel, style = MaterialTheme.typography.labelMedium) }
                        }
                    }

                    if (event.quizWrongToday && event.errorTips.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = apple.blue.copy(alpha = 0.1f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = apple.blue, modifier = Modifier.size(16.dp))
                                Text(event.errorTips, style = MaterialTheme.typography.bodySmall, color = apple.primaryText)
                            }
                        }
                    }
                }
            }
        }
    }

    quizEvent?.let { ev ->
        SolarQuizSheet(
            event = ev,
            busy = busy,
            onDismiss = { if (!busy) quizEvent = null },
            onSubmit = { optionIdx ->
                performAction(
                    "answer",
                    JSONObject()
                        .put("id", ev.id)
                        .put("optionIndex", optionIdx)
                        .put("expectedQuestion", ev.question),
                )
            },
        )
    }
}

@Composable
private fun SolarCalendarLeaf(
    startSec: Long,
    endSec: Long,
) {
    val apple = LocalAppleColors.current
    val (monthLabel, dayLabel, untilLabel) = remember(startSec, endSec) {
        val zone = kotlinx.datetime.TimeZone.of("Asia/Shanghai")
        val start = kotlinx.datetime.Instant.fromEpochSeconds(startSec).toLocalDateTime(zone)
        val end = kotlinx.datetime.Instant.fromEpochSeconds(endSec).toLocalDateTime(zone)
        val m1 = start.monthNumber
        val d1 = start.dayOfMonth
        val m2 = end.monthNumber
        val d2 = end.dayOfMonth
        Triple("${m1}月", d1.toString(), if (m2 == m1) "至 ${d2}日" else "至 ${m2}月${d2}日")
    }
    val sealRed = Color(0xFFC0533F)

    Surface(
        modifier = Modifier.width(66.dp),
        shape = RoundedCornerShape(14.dp),
        color = apple.elevated,
        border = BorderStroke(0.5.dp, apple.separator),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sealRed)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = apple.primaryText,
                )
                Text(
                    text = untilLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = apple.secondaryText,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SolarRewardChips(
    label: String,
    rewards: List<CommerceItem>,
) {
    val apple = LocalAppleColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            rewards.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = apple.elevated,
                    border = BorderStroke(0.5.dp, apple.separator),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        RemoteAssetImage(
                            url = item.image,
                            contentDescription = item.name,
                            fallbackIcon = Icons.Rounded.CardGiftcard,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${item.name} ×${formatCompactCount(item.count)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = apple.primaryText,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolarQuizSheet(
    event: SolarEvent,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIdx by remember(event.id) { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 540.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Quiz, contentDescription = null, tint = Color(0xFFC0533F))
                Text("${event.name} · 节令问答", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            }
            Text(event.question, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)

            event.options.forEachIndexed { idx, rawOpt ->
                val isWrong = idx in event.wrongOptions
                val isCorrect = event.answerIndex == idx
                val isSelected = selectedIdx == idx
                val letter = rawOpt.substringBefore('.').substringBefore('．').substringBefore('、')
                    .takeIf { it.length == 1 && it[0] in 'A'..'Z' }
                    ?: ('A' + idx).toString()
                val textBody = if (rawOpt.startsWith(letter) && rawOpt.length > 1 && rawOpt[1] in listOf('.', '．', '、')) {
                    rawOpt.drop(2).trim()
                } else rawOpt

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppleRadius.card))
                        .clickable(enabled = !busy && !isWrong) { selectedIdx = idx }
                        .alpha(if (isWrong) 0.5f else 1f),
                    shape = RoundedCornerShape(AppleRadius.card),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else apple.elevated,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 0.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else apple.separator,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else apple.track,
                            modifier = Modifier.size(26.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else apple.primaryText,
                                )
                            }
                        }
                        Text(
                            text = textBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = apple.primaryText,
                            modifier = Modifier.weight(1f),
                        )
                        if (isCorrect) {
                            AppleStatusPill("参考答案", apple.green, showDot = false)
                        } else if (isWrong) {
                            AppleStatusPill("已答错", apple.red, showDot = false)
                        }
                    }
                }
            }

            Button(
                onClick = { selectedIdx?.let(onSubmit) },
                enabled = !busy && selectedIdx != null && event.canAnswer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(if (busy) "提交中..." else "提交答案")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun formatDistanceSec(seconds: Long): String {
    val s = seconds.coerceAtLeast(1L)
    return when {
        s >= 86400L -> "${ceil(s / 86400.0).toLong()}天"
        s >= 3600L -> "${ceil(s / 3600.0).toLong()}小时"
        else -> "${ceil(s / 60.0).toLong().coerceAtLeast(1L)}分钟"
    }
}
