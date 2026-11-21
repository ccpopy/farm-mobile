package com.qqfarm.mobile.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.qqfarm.mobile.ui.theme.LocalAppleColors

/**
 * 全应用统一的居中弹窗：中性白/深灰底（不随 M3 基线色偏暖）、20dp 圆角、
 * 标题比 M3 默认小一号，正文用次要文字色。参数与 M3 AlertDialog 保持一致，可直接替换。
 */
@Composable
fun AppleAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    properties: DialogProperties = DialogProperties(),
) {
    val apple = LocalAppleColors.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title?.let { content ->
            {
                ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) {
                    content()
                }
            }
        },
        text = text?.let { content ->
            {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) { content() }
            }
        },
        shape = shape,
        containerColor = apple.card,
        iconContentColor = MaterialTheme.colorScheme.primary,
        titleContentColor = apple.primaryText,
        textContentColor = apple.secondaryText,
        tonalElevation = 0.dp,
        properties = properties,
    )
}
