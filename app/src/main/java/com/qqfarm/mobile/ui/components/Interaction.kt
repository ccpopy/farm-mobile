package com.qqfarm.mobile.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.ui.theme.LocalAppleColors

/**
 * 统一的触感反馈。只用系统自带的轻量反馈常量，不申请振动权限，
 * 用户在系统里关闭“触摸反馈”时自动静默。
 */
@Stable
class AppHaptics(private val view: View) {
    /** 轻触：切换标签、开关、步进器。 */
    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** 确认：主操作按钮。 */
    fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(constant)
    }

    /** 长按：打开更多操作。 */
    fun longPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val view = LocalView.current
    return remember(view) { AppHaptics(view) }
}

/**
 * 手机端按压反馈：按下时轻微缩小并回弹，点击带触感。
 * 替代网页式的“只有水波纹”反馈，卡片、宫格、胶囊按钮统一使用。
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    haptic: Boolean = true,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    val haptics = rememberAppHaptics()
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = ripple(),
            enabled = enabled,
            role = role,
            onLongClick = onLongClick?.let { action ->
                {
                    if (haptic) haptics.longPress()
                    action()
                }
            },
            onClick = {
                if (haptic) haptics.tick()
                onClick()
            },
        )
}

/**
 * 同一行里的卡片统一高度：行高取最高的子项，子项需加 `Modifier.fillMaxHeight()`。
 * 用于两列/三列/四列宫格，避免某张卡多一行内容就把整行撑得高低不齐。
 */
@Composable
fun EqualHeightRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** 同一抽屉内保留已展开的高度，搜索、重试和空态只改变内容；键盘和分屏仍受窗口约束。 */
@Composable
internal fun Modifier.retainSheetHeight(): Modifier = then(remember(LocalDensity.current) {
    var width = -1
    var expandedHeight = 0
    Modifier.layout { measurable, constraints ->
        if (width != constraints.maxWidth) {
            width = constraints.maxWidth
            expandedHeight = 0
        }
        val minimum = expandedHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val child = measurable.measure(constraints.copy(minHeight = minimum))
        expandedHeight = maxOf(expandedHeight, child.height)
        layout(child.width, child.height) { child.placeRelative(0, 0) }
    }
})

/** 全应用统一的开关：iOS 式绿色轨道 + 切换触感。 */
@Composable
fun AppleSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = LocalAppleColors.current.green,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { change ->
            { next: Boolean ->
                haptics.tick()
                change(next)
            }
        },
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = checkedTrackColor,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = apple.secondaryText.copy(alpha = 0.28f),
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = checkedTrackColor.copy(alpha = 0.4f),
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.9f),
            disabledUncheckedTrackColor = apple.secondaryText.copy(alpha = 0.14f),
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.7f),
            disabledUncheckedBorderColor = Color.Transparent,
        ),
        thumbContent = null,
    )
}
