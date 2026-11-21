package com.qqfarm.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.qqfarm.mobile.data.model.CommerceItem
import com.qqfarm.mobile.data.model.GameSpace
import com.qqfarm.mobile.data.model.RanchQuote
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.ui.theme.LocalAssetResolver
import com.qqfarm.mobile.ui.theme.LocalFarmImageLoader
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.IntOffset

@Composable
fun AppleGroupedCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    containerColor: Color = LocalAppleColors.current.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    val apple = LocalAppleColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppleRadius.card),
        color = containerColor,
        border = BorderStroke(0.5.dp, apple.separator),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun AppleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val apple = LocalAppleColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .padding(start = 4.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = apple.secondaryText,
        )
        if (actionLabel != null && onAction != null) {
            // 紧凑文字按钮：不用 TextButton，避免其 40dp 最小高度把分组标题撑高。
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pressable(pressedScale = 0.94f, onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun AppleSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    val count = options.size.coerceAtLeast(1)
    val safeIndex = selectedIndex.coerceIn(0, count - 1)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = apple.track,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
        ) {
            val segmentWidth = maxWidth / count
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * safeIndex,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "segmentIndicator",
            )
            // 滑块：选中项下方的白色卡片随选择平滑移动，而不是瞬间跳变。
            Surface(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(segmentWidth)
                    .height(34.dp),
                shape = RoundedCornerShape(9.dp),
                color = if (apple.isDark) apple.elevated else apple.card,
                shadowElevation = if (apple.isDark) 0.dp else 1.5.dp,
            ) {}
            Row(modifier = Modifier.fillMaxWidth().height(34.dp)) {
                options.forEachIndexed { index, label ->
                    val selected = index == safeIndex
                    val textColor by animateColorAsState(
                        targetValue = if (selected) apple.primaryText else apple.secondaryText,
                        label = "segmentText",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                            ) {
                                if (index != safeIndex) {
                                    haptics.tick()
                                    onSelect(index)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图标徽章。默认是 iOS 设置页式的实色渐变圆角方块 + 白色图标；
 * `soft = true`（或显式传入 containerColor）时为浅色底 + 彩色图标，用于空状态等大面积展示。
 */
@Composable
fun AppleIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    containerColor: Color? = null,
    soft: Boolean = containerColor != null,
) {
    val shape = RoundedCornerShape(size * 0.3f)
    val background = if (soft) {
        Modifier.background(containerColor ?: tint.copy(alpha = 0.14f), shape)
    } else {
        Modifier.background(
            Brush.verticalGradient(listOf(lerp(tint, Color.White, 0.22f), tint)),
            shape,
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (soft) tint else Color.White,
            modifier = Modifier.size(size * if (soft) 0.56f else 0.58f),
        )
    }
}

@Composable
fun AppleHairlineDivider(startIndent: Dp = 58.dp) {
    val apple = LocalAppleColors.current
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent),
        thickness = 0.5.dp,
        color = apple.separator,
    )
}

@Composable
fun AppleSettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingColor: Color = LocalAppleColors.current.secondaryText,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptics.tick()
                        onClick()
                    }
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleIconBadge(icon = icon, tint = iconTint, size = 30.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = apple.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!trailingText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = trailingColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = apple.secondaryText.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun AppleSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(enabled = enabled, role = Role.Switch) {
                haptics.tick()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleIconBadge(icon = icon, tint = if (enabled) iconTint else apple.secondaryText, size = 30.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) apple.primaryText else apple.secondaryText,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        // 整行可点，开关本身只做展示，避免一次点击触发两次切换。
        AppleSwitch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
fun AppleStatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AppleMetricTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    deltaText: String? = null,
    deltaPositive: Boolean = true,
) {
    val apple = LocalAppleColors.current
    Surface(
        // fillMaxHeight：放进 EqualHeightRow 时与同行卡片等高；普通 Row 中不生效。
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = apple.card,
        border = BorderStroke(0.5.dp, apple.separator),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                AppleIconBadge(icon = icon, tint = iconTint, size = 22.dp)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = apple.secondaryText,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = apple.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!deltaText.isNullOrBlank()) {
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = if (deltaPositive) apple.green else apple.red,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun AppleEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val apple = LocalAppleColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppleIconBadge(
            icon = icon,
            tint = MaterialTheme.colorScheme.primary,
            size = 56.dp,
            soft = true,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = apple.primaryText,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = apple.secondaryText,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .pressable(onClick = onAction),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
fun RemoteAssetImage(
    url: String,
    contentDescription: String?,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier,
    fallbackTint: Color = MaterialTheme.colorScheme.primary,
    contentScale: ContentScale = ContentScale.Fit,
) {
    RemoteImageContent(
        url = url,
        contentDescription = contentDescription,
        fallbackIcon = fallbackIcon,
        modifier = modifier,
        fallbackTint = fallbackTint,
        contentScale = contentScale,
        targetSizeDp = null,
    )
}

/** 网格/列表缩略图版本：按显示尺寸降采样解码并关闭 crossfade，降低长列表快速滑动时的内存占用与卡顿。 */
@Composable
fun RemoteThumbImage(
    url: String,
    contentDescription: String?,
    fallbackIcon: ImageVector,
    sizeDp: Dp,
    modifier: Modifier = Modifier,
    fallbackTint: Color = MaterialTheme.colorScheme.primary,
    contentScale: ContentScale = ContentScale.Fit,
) {
    RemoteImageContent(
        url = url,
        contentDescription = contentDescription,
        fallbackIcon = fallbackIcon,
        modifier = modifier,
        fallbackTint = fallbackTint,
        contentScale = contentScale,
        targetSizeDp = sizeDp,
    )
}

@Composable
private fun RemoteImageContent(
    url: String,
    contentDescription: String?,
    fallbackIcon: ImageVector,
    modifier: Modifier,
    fallbackTint: Color,
    contentScale: ContentScale,
    targetSizeDp: Dp?,
) {
    val resolver = LocalAssetResolver.current
    val imageLoader = LocalFarmImageLoader.current
    val resolvedUrl = remember(url, resolver) { resolver(url) }

    if (resolvedUrl.startsWith("data:image/", ignoreCase = true)) {
        val bitmap = remember(resolvedUrl) {
            runCatching {
                val base64Part = resolvedUrl.substringAfter("base64,", "")
                if (base64Part.isBlank()) return@runCatching null
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier,
            )
            return
        }
    }

    var failed by remember(resolvedUrl) { mutableStateOf(false) }
    if (resolvedUrl.isBlank() || failed || imageLoader == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = fallbackTint,
                modifier = Modifier.fillMaxSize(0.58f),
            )
        }
    } else {
        val model: Any = if (targetSizeDp != null) {
            val context = LocalContext.current
            val targetPx = with(LocalDensity.current) { targetSizeDp.roundToPx().coerceAtLeast(36) }
            remember(resolvedUrl, targetPx, context) {
                ImageRequest.Builder(context)
                    .data(resolvedUrl)
                    .size(targetPx)
                    .crossfade(false)
                    .memoryCacheKey("$resolvedUrl@$targetPx")
                    .diskCacheKey(resolvedUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
        } else {
            resolvedUrl
        }
        AsyncImage(
            model = model,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            onError = { failed = true },
        )
    }
}

// ---- Formatting Helpers ----

fun formatNumber(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.CHINA).format(value)

fun formatDurationSec(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    if (s == 0L) return "00:00"
    val d = s / 86400L
    val h = (s % 86400L) / 3600L
    val m = (s % 3600L) / 60L
    val rem = s % 60L
    return if (d > 0L) {
        String.format(Locale.CHINA, "%d天 %02d:%02d:%02d", d, h, m, rem)
    } else if (h > 0L) {
        String.format(Locale.CHINA, "%02d:%02d:%02d", h, m, rem)
    } else {
        String.format(Locale.CHINA, "%02d:%02d", m, rem)
    }
}

fun formatBucketHours(seconds: Long, preformatted: String = ""): String {
    if (preformatted.isNotBlank()) return preformatted.replace("小时", "h")
    val hours = (seconds.coerceAtLeast(0L) / 3600.0)
    return String.format(Locale.US, "%.1fh", hours)
}

fun formatTimestamp(ms: Long): String {
    if (ms <= 0L) return ""
    return SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(Date(ms))
}

fun formatCompactCount(value: Long): String {
    val abs = kotlin.math.abs(value)
    return when {
        abs >= 100_000_000L -> {
            val formatted = String.format(Locale.US, "%.1f", value / 100_000_000.0).trimEnd('0').trimEnd('.')
            "${formatted}亿"
        }
        abs >= 10_000L -> {
            val formatted = if (abs >= 100_000L) {
                String.format(Locale.US, "%.0f", value / 10_000.0)
            } else {
                String.format(Locale.US, "%.1f", value / 10_000.0).trimEnd('0').trimEnd('.')
            }
            "${formatted}万"
        }
        else -> formatNumber(value)
    }
}

// ---- Space Switcher, SubPage Header, Quick Entries, Steppers & Ranch Quote Sheet ----

@Composable
fun AppleSpaceSwitcher(
    space: GameSpace,
    onSelect: (GameSpace) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = apple.track,
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameSpace.entries.forEach { entry ->
                val selected = entry == space
                val accent = spaceAccent(entry)
                val container by animateColorAsState(
                    targetValue = if (selected) accent else Color.Transparent,
                    label = "spaceContainer",
                )
                val content by animateColorAsState(
                    targetValue = if (selected) Color.White else apple.secondaryText,
                    label = "spaceContent",
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(container)
                        .clickable(role = Role.Tab) {
                            if (!selected) {
                                haptics.tick()
                                onSelect(entry)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = spaceIcon(entry, filled = selected),
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = content,
                    )
                }
            }
        }
    }
}

/** 空间主色：农场绿、牧场橙，导航、切换器、主按钮统一取这里。 */
@Composable
fun spaceAccent(space: GameSpace): Color {
    val apple = LocalAppleColors.current
    return if (space == GameSpace.RANCH) apple.orange else apple.emerald
}

/** 空间图标：农场用拖拉机、牧场用爪印；选中实心、未选中线框。 */
fun spaceIcon(space: GameSpace, filled: Boolean): ImageVector = when (space) {
    GameSpace.RANCH -> if (filled) Icons.Rounded.Pets else Icons.Outlined.Pets
    else -> if (filled) Icons.Rounded.Agriculture else Icons.Outlined.Agriculture
}

/** 页面二级标签，统一改为下划线式 [AppleTabStrip]；保留旧名以兼容各页面调用。 */
@Composable
fun AppleScrollablePills(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    AppleTabStrip(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
        accentColor = accentColor,
    )
}

/**
 * 横向操作胶囊（一键收获、一键生产等）。农场和牧场共用，主操作用空间主色实心。
 */
@Composable
fun AppleActionPill(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val container = when {
        primary && enabled -> accentColor
        primary -> accentColor.copy(alpha = 0.35f)
        else -> apple.card
    }
    val content = when {
        primary -> Color.White
        enabled -> apple.primaryText
        else -> apple.secondaryText.copy(alpha = 0.6f)
    }
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .pressable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = container,
        border = if (primary) null else BorderStroke(0.5.dp, apple.separator),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun AppleSubPageHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val apple = LocalAppleColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(apple.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = apple.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = apple.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = apple.separator)
    }
}

data class QuickEntryItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val badge: String = "",
    val onClick: () -> Unit,
)

@Composable
fun AppleQuickEntryRow(
    entries: List<QuickEntryItem>,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    // 手机桌面式的快捷入口：一张卡片里四个“小 App 图标”，比四张独立卡片更轻、更好点。
    AppleGroupedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            entries.forEach { entry ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .pressable(pressedScale = 0.92f, onClick = entry.onClick)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box {
                        AppleIconBadge(
                            icon = entry.icon,
                            tint = entry.tint,
                            size = 44.dp,
                        )
                        if (entry.badge.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp),
                                shape = CircleShape,
                                color = apple.red,
                                border = BorderStroke(1.5.dp, apple.card),
                            ) {
                                Text(
                                    text = entry.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = apple.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun AppleQuantityStepper(
    value: Int,
    min: Int = 1,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    val count = if (max >= min) value.coerceIn(min, max) else 0
    val canDecrease = enabled && count > min
    val canIncrease = enabled && count < max
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        QuantityStepButton(Icons.Rounded.Remove, "减少数量", canDecrease) {
            if (canDecrease) onChange(count - 1)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            color = apple.primaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 40.dp),
        )
        QuantityStepButton(Icons.Rounded.Add, "增加数量", canIncrease) {
            if (canIncrease) onChange(count + 1)
        }
        if (max > min) {
            val tint = if (canIncrease) MaterialTheme.colorScheme.primary else apple.secondaryText
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = canIncrease, role = Role.Button) {
                        haptics.tick()
                        onChange(max)
                    },
                shape = RoundedCornerShape(10.dp),
                color = if (canIncrease) tint.copy(alpha = 0.14f) else apple.track,
            ) {
                Text(
                    text = "最大",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tint,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuantityStepButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onStep: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val step by rememberUpdatedState(onStep)
    var repeating by remember { mutableStateOf(false) }

    LaunchedEffect(pressed, enabled, repeating) {
        if (!pressed || !enabled) {
            repeating = false
        } else if (repeating) {
            while (true) {
                delay(85L)
                step()
            }
        }
    }

    Surface(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = "连续$label",
                onLongClick = { step(); repeating = true },
                onClick = { haptics.tick(); step() },
            ),
        shape = RoundedCornerShape(10.dp),
        color = apple.track,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, Modifier.size(18.dp), tint = if (enabled) apple.primaryText else apple.secondaryText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RanchQuoteConfirmSheet(
    quote: RanchQuote,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
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
            Text(
                text = quote.label.ifBlank { "确认牧场操作" },
                style = MaterialTheme.typography.titleLarge,
                color = apple.primaryText,
            )
            if (quote.count > 1) {
                Text(
                    text = "本次共涉及 ${quote.count} 项",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = apple.secondaryText,
                )
            }
            if (quote.costItems.isNotEmpty()) {
                Text("预计消耗", style = MaterialTheme.typography.labelLarge, color = apple.secondaryText)
                quote.costItems.forEach { cost ->
                    val displayName = when (cost.id) {
                        7003 -> "普通罐头"
                        7004 -> "有机罐头"
                        else -> cost.name
                    }
                    val displayAmount = if (cost.id == 7003 || cost.id == 7004) {
                        "最多 ${formatDurationSec(cost.count)}"
                    } else "×${formatCompactCount(cost.count)}"
                    QuoteItemRow(item = cost, displayName = displayName, amountText = displayAmount, amountColor = apple.orange)
                }
            }
            if (quote.rewards.isNotEmpty()) {
                Text("预计获得", style = MaterialTheme.typography.labelLarge, color = apple.secondaryText)
                quote.rewards.forEach { rew ->
                    QuoteItemRow(
                        item = rew,
                        displayName = rew.name,
                        amountText = "×${formatCompactCount(rew.count)}",
                        amountColor = apple.emerald,
                    )
                }
            }
            if (quote.message.isNotBlank() || (quote.costItems.isEmpty() && quote.rewards.isEmpty())) {
                Text(
                    text = quote.message.ifBlank { "确认执行本次操作？" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = apple.secondaryText,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("取消")
                }
                val isDestructive = quote.action == "release"
                Button(
                    onClick = onConfirm,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    colors = if (isDestructive) {
                        ButtonDefaults.buttonColors(containerColor = apple.red, contentColor = Color.White)
                    } else ButtonDefaults.buttonColors(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            busy -> "执行中..."
                            isDestructive -> "确认放生"
                            else -> "确认执行"
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuoteItemRow(
    item: CommerceItem,
    displayName: String,
    amountText: String,
    amountColor: Color,
) {
    val apple = LocalAppleColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = apple.elevated,
        border = BorderStroke(0.5.dp, apple.separator),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RemoteAssetImage(
                url = item.image,
                contentDescription = displayName,
                fallbackIcon = if (amountColor == LocalAppleColors.current.orange) Icons.Rounded.ShoppingBag else Icons.Rounded.CardGiftcard,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(apple.track),
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = apple.primaryText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = amountColor,
            )
        }
    }
}
