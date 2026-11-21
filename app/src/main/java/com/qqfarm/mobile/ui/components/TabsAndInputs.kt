package com.qqfarm.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity

/** 全应用统一的圆角刻度：卡片 14、卡片内小块 12、控件 10。 */
object AppleRadius {
    val card = 14.dp
    val tile = 12.dp
    val control = 10.dp
}

/**
 * 页面内的二级标签：文字 + 下划线指示条（类似原生 App 的顶部标签）。
 * 比实心胶囊更轻，不会和页面里的按钮、筛选标签抢视觉。标签多时可横向滚动，选中项自动滚入可见区。
 */
@Composable
fun AppleTabStrip(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in options.indices) {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
            val fullyVisible = item != null && item.offset >= info.viewportStartOffset &&
                item.offset + item.size <= info.viewportEndOffset
            if (!fullyVisible) listState.animateScrollToItem(selectedIndex)
        }
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .selectableGroup(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(options) { index, label ->
            val selected = index == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (selected) apple.primaryText else apple.secondaryText,
                label = "tabText",
            )
            val indicatorWidth by animateDpAsState(
                targetValue = if (selected) 18.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "tabIndicator",
            )
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppleRadius.control))
                    .selectable(
                        selected = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                    ) {
                        if (!selected) {
                            haptics.tick()
                            onSelect(index)
                        }
                    }
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = textColor,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}

/** 筛选小标签：浅色底、无描边，选中为主色浅底 + 主色字。不受系统动态取色的描边影响。 */
@Composable
fun AppleFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = LocalAppleColors.current.emerald,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    val container by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.14f) else apple.track,
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) accentColor else apple.primaryText,
        label = "chipContent",
    )
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(AppleRadius.control))
            .background(container)
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox) {
                haptics.tick()
                onClick()
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            ProvideTextStyle(
                MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
            ) {
                label()
            }
        }
    }
}

/** 紧凑搜索框：40dp 高、浅灰底、无描边，带一键清空；键盘“搜索”键收起输入法。 */
@Composable
fun AppleSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = apple.primaryText),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(AppleRadius.control))
                    .background(apple.track)
                    .padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = apple.secondaryText,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = apple.secondaryText.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Cancel,
                        contentDescription = "清空",
                        tint = apple.secondaryText.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onValueChange("") }
                            .padding(7.dp),
                    )
                }
            }
        },
    )
}

/**
 * 整页左右滑动切换标签：手指拖动时内容带阻尼跟随，松手超过阈值切到相邻标签；
 * 无论点标签还是滑动切换，新内容都会从对应方向轻滑淡入。
 * - 子组件（横向列表、内层标签页）先处理手势；内层已到头时才交给外层。
 * - 纵向滚动先触发时本手势自动取消，不影响上下滑。
 */
fun Modifier.swipeToSwitchTabs(
    selectedIndex: Int,
    count: Int,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
): Modifier = composed {
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val selectTab by rememberUpdatedState(onSelect)
    val offset = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    var dragOffset by remember { mutableStateOf<Float?>(null) }
    var lastIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != lastIndex) {
            val direction = if (selectedIndex > lastIndex) 1f else -1f
            lastIndex = selectedIndex
            val enterOffset = direction * with(density) { 48.dp.toPx() }
            offset.snapTo(enterOffset)
            alpha.snapTo(0.45f)
            launch { alpha.animateTo(1f, tween(200)) }
            offset.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    this
        .pointerInput(selectedIndex, count, enabled) {
            if (!enabled || count <= 1) return@pointerInput
            val distanceThreshold = 56.dp.toPx()
            val velocityThreshold = 380.dp.toPx()
            val minFlickDistance = 24.dp.toPx()
            val touchSlop = viewConfiguration.touchSlop
            val velocityTracker = VelocityTracker()

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                velocityTracker.resetTracking()
                velocityTracker.addPosition(down.uptimeMillis, down.position)
                var total = 0f
                var dragging = false
                var switched = false

                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed || event.changes.any { it.id != down.id && it.pressed }) {
                            return@awaitEachGesture
                        }
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val deltaX = change.position.x - down.position.x
                        if (!change.pressed) {
                            total = deltaX
                            break
                        }

                        if (!dragging) {
                            val deltaY = change.position.y - down.position.y
                            if (abs(deltaY) > touchSlop && abs(deltaY) >= abs(deltaX)) {
                                return@awaitEachGesture
                            }
                            if (abs(deltaX) > touchSlop && abs(deltaX) > abs(deltaY)) {
                                val toNext = deltaX < 0f
                                val allowed = if (toNext) selectedIndex < count - 1 else selectedIndex > 0
                                if (!allowed) {
                                    // 边缘不吞事件，交给外层容器接管整页滑动
                                    return@awaitEachGesture
                                }
                                change.consume()
                                dragging = true
                                total = deltaX
                                dragOffset = total * 0.32f
                            }
                        } else {
                            total = deltaX
                            change.consume()
                            dragOffset = when {
                                total < 0f && selectedIndex >= count - 1 -> total * 0.08f
                                total > 0f && selectedIndex <= 0 -> total * 0.08f
                                else -> total * 0.32f
                            }
                        }
                    }

                    if (dragging) {
                        val vx = velocityTracker.calculateVelocity().x
                        val target = when {
                            (total <= -distanceThreshold || (vx <= -velocityThreshold && total <= -minFlickDistance)) &&
                                selectedIndex < count - 1 -> selectedIndex + 1
                            (total >= distanceThreshold || (vx >= velocityThreshold && total >= minFlickDistance)) &&
                                selectedIndex > 0 -> selectedIndex - 1
                            else -> null
                        }
                        if (target != null) {
                            switched = true
                            haptics.tick()
                            selectTab(target)
                        }
                    }
                } finally {
                    if (dragging) {
                        val previousOffset = dragOffset ?: 0f
                        dragOffset = null
                        if (!switched) {
                            scope.launch {
                                offset.snapTo(previousOffset)
                                offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    }
                }
            }
        }
        .graphicsLayer {
            translationX = dragOffset ?: offset.value
            this.alpha = alpha.value
        }
}
