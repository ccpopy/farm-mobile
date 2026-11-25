package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.ui.components.AppleAlertDialog
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleProgressBar
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.AppleSwitchRow
import com.qqfarm.mobile.ui.components.EqualHeightRow
import com.qqfarm.mobile.ui.theme.ACCENT_PRESETS
import com.qqfarm.mobile.ui.theme.APPEARANCE_QUICK_PRESETS
import com.qqfarm.mobile.ui.theme.AppleEmerald
import com.qqfarm.mobile.ui.theme.AppleOrange
import com.qqfarm.mobile.ui.theme.AppearanceQuickPreset
import com.qqfarm.mobile.ui.theme.BackgroundTone
import com.qqfarm.mobile.ui.theme.CardSurfaceStyle
import com.qqfarm.mobile.ui.theme.CornerStyle
import com.qqfarm.mobile.ui.theme.CustomAppearanceConfig
import com.qqfarm.mobile.ui.theme.EffectiveAppearance
import com.qqfarm.mobile.ui.theme.LAND_TIER_SPECS
import com.qqfarm.mobile.ui.theme.LandColorPreset
import com.qqfarm.mobile.ui.theme.LocalAppAppearance
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.ui.theme.ProgressBarStyle
import com.qqfarm.mobile.ui.theme.RANCH_ACCENT_PRESETS
import com.qqfarm.mobile.ui.theme.RANCH_COLOR_SPECS
import com.qqfarm.mobile.ui.theme.RanchAnimalColorPreset
import com.qqfarm.mobile.ui.theme.SoilGold
import com.qqfarm.mobile.ui.theme.ranchDefaultLightBackground
import com.qqfarm.mobile.ui.theme.onAccent
import com.qqfarm.mobile.ui.theme.resolveLandCardColors
import com.qqfarm.mobile.ui.theme.resolveRanchAnimalCardColors
import com.qqfarm.mobile.ui.components.retainSheetHeight
import com.qqfarm.mobile.ui.theme.readableOn
import com.qqfarm.mobile.ui.theme.webLandLightBackground
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val ACCENT_WHEEL_SWATCHES = listOf(
    "农牧绿" to AppleEmerald,
    "晴空蓝" to Color(0xFF007AFF),
    "薰衣草" to Color(0xFF6E56CF),
    "樱花粉" to Color(0xFFE5487B),
    "琥珀金" to Color(0xFFD97706),
    "青松青" to Color(0xFF0D9488),
    "珊瑚红" to Color(0xFFEF4444),
    "墨岩灰" to Color(0xFF475569),
)

private val RANCH_ACCENT_WHEEL_SWATCHES = listOf(
    "经典橙" to AppleOrange,
    "暖琥珀" to Color(0xFFD97706),
    "珊瑚橘" to Color(0xFFF97316),
    "焦糖棕" to Color(0xFFB45309),
    "玫瑰粉" to Color(0xFFE11D48),
    "青松绿" to Color(0xFF0D9488),
    "晴空蓝" to Color(0xFF007AFF),
    "薰衣草" to Color(0xFF6E56CF),
)

private val PREVIEW_LAND_SAMPLES = listOf(
    Triple(5, "紫金", 0.64f),
    Triple(6, "蓝晶", 0.90f),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomAppearanceSheet(
    themeMode: String,
    onUpdateThemeMode: (String) -> Unit,
    config: CustomAppearanceConfig,
    onUpdate: (CustomAppearanceConfig) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isCustom = themeMode == StoredConfig.THEME_CUSTOM
    val scrollBoundary = remember {
        // 滚到内容顶部后，吸收剩余下滑距离，避免它继续拖动抽屉。
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                Offset(0f, available.y.coerceAtLeast(0f))

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(0f, available.y.coerceAtLeast(0f))
        }
    }

    var editingAccentWheel by remember { mutableStateOf(false) }
    var editingRanchAccentWheel by remember { mutableStateOf(false) }
    var editingLandTier by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var editingRanchKey by remember { mutableStateOf<Pair<String, String>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 680.dp)
                .retainSheetHeight(),
        ) {
            // Fixed single-line header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isCustom) "自定义主题外观" else "主题外观",
                    style = MaterialTheme.typography.titleLarge,
                    color = apple.primaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isCustom && config != CustomAppearanceConfig()) {
                    TextButton(onClick = onReset) {
                        Text("恢复默认")
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = apple.secondaryText,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            ) {
                val modeIdx = when (themeMode) {
                    StoredConfig.THEME_LIGHT -> 1
                    StoredConfig.THEME_DARK -> 2
                    StoredConfig.THEME_CUSTOM -> 3
                    else -> 0
                }
                AppleSegmentedControl(
                    options = listOf("跟随系统", "浅色", "深色", "自定义"),
                    selectedIndex = modeIdx,
                    onSelect = { idx ->
                        val nextMode = when (idx) {
                            1 -> StoredConfig.THEME_LIGHT
                            2 -> StoredConfig.THEME_DARK
                            3 -> StoredConfig.THEME_CUSTOM
                            else -> StoredConfig.THEME_SYSTEM
                        }
                        onUpdateThemeMode(nextMode)
                    },
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .nestedScroll(scrollBoundary)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) appearanceContent@{
                AppearanceLivePreviewCard(themeMode = themeMode)
                if (!isCustom) return@appearanceContent
                val appearance = LocalAppAppearance.current
                AppleHairlineDivider()

                // 1. One-tap quick presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "一键快捷方案")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(APPEARANCE_QUICK_PRESETS, key = { it.id }) { preset ->
                            QuickPresetCard(
                                preset = preset,
                                selected = config == preset.config,
                                onClick = { onUpdate(preset.config) },
                            )
                        }
                    }
                }

                // 2. Base theme mode & background tone
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "明暗底色与背景质感")
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                        Text(
                            text = "明暗底色",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val baseIdx = when (config.baseThemeMode) {
                            StoredConfig.THEME_LIGHT -> 1
                            StoredConfig.THEME_DARK -> 2
                            else -> 0
                        }
                        AppleSegmentedControl(
                            options = listOf("跟随系统", "浅色", "深色"),
                            selectedIndex = baseIdx,
                            onSelect = { idx ->
                                val mode = when (idx) {
                                    1 -> StoredConfig.THEME_LIGHT
                                    2 -> StoredConfig.THEME_DARK
                                    else -> StoredConfig.THEME_SYSTEM
                                }
                                onUpdate(config.copy(baseThemeMode = mode))
                            },
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        AppleHairlineDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "背景底色质感",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val tones = BackgroundTone.entries
                        AppleSegmentedControl(
                            options = tones.map { it.label },
                            selectedIndex = tones.indexOf(config.backgroundTone).coerceAtLeast(0),
                            onSelect = { idx ->
                                onUpdate(config.copy(backgroundTone = tones[idx]))
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (apple.isDark) {
                                "深色效果：${config.backgroundTone.darkDesc}"
                            } else {
                                "浅色效果：${config.backgroundTone.lightDesc}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                        )
                    }
                }

                // 3. Farm/global & Ranch accents
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "空间强调色")
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                        Text(
                            text = "全局与农场主色",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val activeCustomAccent = config.accentColorArgb
                        val isPresetMatch = ACCENT_PRESETS.any { (_, presetColor) ->
                            presetColor?.toArgb()?.toLong() == activeCustomAccent
                        }
                        val customColor = if (!isPresetMatch && activeCustomAccent != null) {
                            Color(activeCustomAccent.toInt())
                        } else null
                        val globalCells = buildList {
                            ACCENT_PRESETS.forEach { (label, presetColor) ->
                                val presetArgb = presetColor?.toArgb()?.toLong()
                                add(
                                    AccentGridCell.Swatch(
                                        label = label,
                                        color = presetColor ?: AppleEmerald,
                                        selected = activeCustomAccent == presetArgb,
                                        onClick = { onUpdate(config.copy(accentColorArgb = presetArgb)) },
                                    )
                                )
                            }
                            add(
                                AccentGridCell.Wheel(
                                    selected = customColor != null,
                                    activeColor = customColor,
                                    onClick = { editingAccentWheel = true },
                                )
                            )
                        }
                        FourColumnAccentGrid(cells = globalCells)

                        Spacer(modifier = Modifier.height(14.dp))
                        AppleHairlineDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "牧场空间主色",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val activeRanchAccent = config.ranchAccentColorArgb
                        val isRanchPresetMatch = RANCH_ACCENT_PRESETS.any { (_, presetColor) ->
                            presetColor?.toArgb()?.toLong() == activeRanchAccent
                        }
                        val customRanchColor = if (
                            !config.ranchAccentFollowPrimary &&
                            !isRanchPresetMatch &&
                            activeRanchAccent != null
                        ) {
                            Color(activeRanchAccent.toInt())
                        } else null
                        val ranchCells = buildList {
                            add(
                                AccentGridCell.Swatch(
                                    label = "跟随主色",
                                    color = activeCustomAccent?.let { Color(it.toInt()) } ?: AppleEmerald,
                                    selected = config.ranchAccentFollowPrimary,
                                    onClick = { onUpdate(config.copy(ranchAccentFollowPrimary = true)) },
                                )
                            )
                            RANCH_ACCENT_PRESETS.forEach { (label, presetColor) ->
                                val presetArgb = presetColor?.toArgb()?.toLong()
                                add(
                                    AccentGridCell.Swatch(
                                        label = label,
                                        color = presetColor ?: AppleOrange,
                                        selected = !config.ranchAccentFollowPrimary && activeRanchAccent == presetArgb,
                                        onClick = {
                                            onUpdate(
                                                config.copy(
                                                    ranchAccentFollowPrimary = false,
                                                    ranchAccentColorArgb = presetArgb,
                                                )
                                            )
                                        },
                                    )
                                )
                            }
                            add(
                                AccentGridCell.Wheel(
                                    selected = customRanchColor != null,
                                    activeColor = customRanchColor,
                                    onClick = { editingRanchAccentWheel = true },
                                )
                            )
                        }
                        FourColumnAccentGrid(cells = ranchCells)
                    }
                }

                // 4. Land card color preset & per-tier wheel
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "土地卡片配色")
                    AppleGroupedCard {
                        LandColorPreset.entries.forEachIndexed { idx, preset ->
                            if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                            val selected = config.landPreset == preset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdate(config.copy(landPreset = preset)) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            ),
                                            color = apple.primaryText,
                                        )
                                        if (preset == LandColorPreset.WEB) {
                                            TagBadge(text = "默认", tint = apple.blue)
                                        } else if (preset == LandColorPreset.PURE) {
                                            TagBadge(text = "原版纯白", tint = apple.secondaryText)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = apple.secondaryText,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        val rowAppearance = appearance.copy(landPreset = preset)
                                        LAND_TIER_SPECS.forEach { (level, _) ->
                                            val colors = resolveLandCardColors(level, apple.isDark, apple, rowAppearance)
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.background)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (preset == LandColorPreset.PURE) colors.badgeText else colors.border,
                                                        shape = CircleShape,
                                                    ),
                                            )
                                        }
                                    }
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "已选中",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        if (config.landPreset == LandColorPreset.CUSTOM) {
                            AppleHairlineDivider(startIndent = 16.dp)
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "分级自选（点击用颜色环调色）",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = apple.primaryText,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CompactPresetActionChip(
                                        label = "套用网页色",
                                        onClick = { onUpdate(config.copy(customLandColors = emptyMap())) },
                                    )
                                    CompactPresetActionChip(
                                        label = "全部纯白",
                                        onClick = {
                                            val whiteArgb = Color.White.toArgb().toLong()
                                            val allWhite = LAND_TIER_SPECS.associate { (lvl, _) -> lvl to whiteArgb }
                                            onUpdate(config.copy(customLandColors = allWhite))
                                        },
                                    )
                                }
                                LAND_TIER_SPECS.chunked(3).forEach { rowSpecs ->
                                    EqualHeightRow(spacing = 8.dp) {
                                        rowSpecs.forEach { (level, label) ->
                                            val resolved = resolveLandCardColors(level, apple.isDark, apple, appearance)
                                            val customArgb = config.customLandColors[level]
                                            val colorDesc = when {
                                                customArgb == null -> "网页"
                                                Color(customArgb.toInt()).toArgb() == 0xFFFFFFFF.toInt() -> "纯白"
                                                else -> colorToHex(Color(customArgb.toInt()))
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { editingLandTier = level to label },
                                                shape = RoundedCornerShape(10.dp),
                                                color = resolved.background,
                                                border = BorderStroke(0.9.dp, resolved.border),
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = resolved.badgeBackground,
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = resolved.badgeText,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        )
                                                    }
                                                    Text(
                                                        text = colorDesc,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                        color = apple.secondaryText,
                                                        maxLines = 1,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AppleHairlineDivider(startIndent = 16.dp)
                        AppleSwitchRow(
                            icon = Icons.Rounded.Palette,
                            iconTint = apple.emerald,
                            title = "土地详情头部随土壤着彩",
                            subtitle = "点开单块土地时顶部卡片同步使用对应土壤色调；简约纯白保持纯白",
                            checked = config.landDetailTinted,
                            enabled = config.landPreset != LandColorPreset.PURE,
                            onCheckedChange = { onUpdate(config.copy(landDetailTinted = it)) },
                        )
                    }
                }

                // 5. Ranch animal card color preset & per-type custom wheel
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "牧场动物卡片配色")
                    AppleGroupedCard {
                        RanchAnimalColorPreset.entries.forEachIndexed { idx, preset ->
                            if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                            val selected = config.ranchAnimalPreset == preset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdate(config.copy(ranchAnimalPreset = preset)) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            ),
                                            color = apple.primaryText,
                                        )
                                        if (preset == RanchAnimalColorPreset.STATUS) {
                                            TagBadge(text = "默认", tint = apple.blue)
                                        } else if (preset == RanchAnimalColorPreset.PURE) {
                                            TagBadge(text = "原版纯白", tint = apple.secondaryText)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = apple.secondaryText,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        val rowApp = appearance.copy(ranchAnimalPreset = preset)
                                        val samples = when (preset) {
                                            RanchAnimalColorPreset.STATUS -> listOf(
                                                resolveRanchAnimalCardColors(false, false, true, false, false, apple.isDark, apple, rowApp),
                                                resolveRanchAnimalCardColors(false, false, false, true, false, apple.isDark, apple, rowApp),
                                                resolveRanchAnimalCardColors(false, true, false, false, false, apple.isDark, apple, rowApp),
                                                resolveRanchAnimalCardColors(false, false, false, false, true, apple.isDark, apple, rowApp),
                                            )
                                            else -> listOf(
                                                resolveRanchAnimalCardColors(false, false, false, false, false, apple.isDark, apple, rowApp),
                                                resolveRanchAnimalCardColors(true, false, false, false, false, apple.isDark, apple, rowApp),
                                                resolveRanchAnimalCardColors(false, true, false, false, false, apple.isDark, apple, rowApp),
                                            )
                                        }
                                        samples.forEach { c ->
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(c.background)
                                                    .border(1.dp, c.border, CircleShape),
                                            )
                                        }
                                    }
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "已选中",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        if (config.ranchAnimalPreset == RanchAnimalColorPreset.CUSTOM) {
                            AppleHairlineDivider(startIndent = 16.dp)
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "分类自选（点击用颜色环调色）",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = apple.primaryText,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CompactPresetActionChip(
                                        label = "套用默认彩",
                                        onClick = { onUpdate(config.copy(customRanchColors = emptyMap())) },
                                    )
                                    CompactPresetActionChip(
                                        label = "全部纯白",
                                        onClick = {
                                            val whiteArgb = Color.White.toArgb().toLong()
                                            val allWhite = RANCH_COLOR_SPECS.associate { (k, _) -> k to whiteArgb }
                                            onUpdate(config.copy(customRanchColors = allWhite))
                                        },
                                    )
                                }
                                EqualHeightRow(spacing = 8.dp) {
                                    RANCH_COLOR_SPECS.forEach { (key, label) ->
                                        val resolved = resolveRanchAnimalCardColors(
                                            isShed = key == "shed",
                                            isGiant = key == "giant",
                                            canHarvest = false,
                                            canProduce = false,
                                            grassBlocked = false,
                                            isDark = apple.isDark,
                                            apple = apple,
                                            appearance = appearance,
                                        )
                                        val customArgb = config.customRanchColors[key]
                                        val colorDesc = when {
                                            customArgb == null -> "默认"
                                            Color(customArgb.toInt()).toArgb() == 0xFFFFFFFF.toInt() -> "纯白"
                                            else -> colorToHex(Color(customArgb.toInt()))
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { editingRanchKey = key to label },
                                            shape = RoundedCornerShape(10.dp),
                                            color = resolved.background,
                                            border = BorderStroke(0.9.dp, resolved.border),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = apple.primaryText,
                                                    maxLines = 1,
                                                )
                                                Text(
                                                    text = colorDesc,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                    color = apple.secondaryText,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Grid columns density
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "宫格列数（信息密度）")
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                        Text(
                            text = "农场土地每行列数",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppleSegmentedControl(
                            options = listOf("3 列（标准）", "4 列（紧凑）"),
                            selectedIndex = if (config.landGridColumns == 4) 1 else 0,
                            onSelect = { idx ->
                                onUpdate(config.copy(landGridColumns = if (idx == 1) 4 else 3))
                            },
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        AppleHairlineDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "牧场动物每行列数",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppleSegmentedControl(
                            options = listOf("2 列（标准）", "3 列（紧凑）"),
                            selectedIndex = if (config.ranchAnimalColumns == 3) 1 else 0,
                            onSelect = { idx ->
                                onUpdate(config.copy(ranchAnimalColumns = if (idx == 1) 3 else 2))
                            },
                        )
                    }
                }

                // 7. Progress bar style
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "进度条样式")
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                        val barStyles = ProgressBarStyle.entries
                        val selectedIdx = barStyles.indexOf(config.progressBarStyle).coerceAtLeast(0)
                        AppleSegmentedControl(
                            options = barStyles.map { it.label },
                            selectedIndex = selectedIdx,
                            onSelect = { idx ->
                                onUpdate(config.copy(progressBarStyle = barStyles[idx]))
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (config.progressBarStyle) {
                                ProgressBarStyle.SMOOTH -> "完整一体圆角填充，去掉 M3 末端的分段断点与圆点。"
                                ProgressBarStyle.SEGMENTED -> "Material 3 原生指示条（带尾部间隙与终点圆点）。"
                                ProgressBarStyle.GRADIENT -> "完整平滑圆轨叠加柔和亮光渐变填充。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = apple.secondaryText,
                        )
                    }
                }

                // 8. Corner & card surface style
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "圆角与卡片层次")
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                        Text(
                            text = "界面圆角",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val corners = CornerStyle.entries
                        AppleSegmentedControl(
                            options = corners.map { it.label },
                            selectedIndex = corners.indexOf(config.cornerStyle).coerceAtLeast(0),
                            onSelect = { idx ->
                                onUpdate(config.copy(cornerStyle = corners[idx]))
                            },
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        AppleHairlineDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "卡片层级",
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val surfaces = CardSurfaceStyle.entries
                        AppleSegmentedControl(
                            options = surfaces.map { it.label },
                            selectedIndex = surfaces.indexOf(config.cardSurfaceStyle).coerceAtLeast(0),
                            onSelect = { idx ->
                                onUpdate(config.copy(cardSurfaceStyle = surfaces[idx]))
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (editingAccentWheel) {
        val currentAccent = config.accentColorArgb?.let { Color(it.toInt()) } ?: AppleEmerald
        AppleColorWheelDialog(
            title = "自定义全局强调色",
            initialColor = currentAccent,
            quickSwatches = ACCENT_WHEEL_SWATCHES,
            onConfirm = { picked ->
                onUpdate(config.copy(accentColorArgb = picked.toArgb().toLong()))
                editingAccentWheel = false
            },
            onDismiss = { editingAccentWheel = false },
        )
    }

    if (editingRanchAccentWheel) {
        val currentRanch = config.ranchAccentColorArgb?.let { Color(it.toInt()) } ?: AppleOrange
        AppleColorWheelDialog(
            title = "自定义牧场空间主色",
            initialColor = currentRanch,
            quickSwatches = RANCH_ACCENT_WHEEL_SWATCHES,
            onConfirm = { picked ->
                onUpdate(
                    config.copy(
                        ranchAccentFollowPrimary = false,
                        ranchAccentColorArgb = picked.toArgb().toLong(),
                    )
                )
                editingRanchAccentWheel = false
            },
            onDismiss = { editingRanchAccentWheel = false },
        )
    }

    editingLandTier?.let { (level, label) ->
        val currentArgb = config.customLandColors[level]
        val currentColor = currentArgb?.let { Color(it.toInt()) } ?: webLandLightBackground(level)
        AppleColorWheelDialog(
            title = "选择「$label」卡片底色",
            initialColor = currentColor,
            quickSwatches = landTierQuickSwatches(level),
            onConfirm = { picked ->
                val nextMap = config.customLandColors + (level to picked.toArgb().toLong())
                onUpdate(config.copy(customLandColors = nextMap))
                editingLandTier = null
            },
            onDismiss = { editingLandTier = null },
        )
    }

    editingRanchKey?.let { (key, label) ->
        val currentArgb = config.customRanchColors[key]
        val currentColor = currentArgb?.let { Color(it.toInt()) } ?: ranchDefaultLightBackground(key)
        AppleColorWheelDialog(
            title = "选择「$label」卡片底色",
            initialColor = currentColor,
            quickSwatches = ranchKeyQuickSwatches(key),
            onConfirm = { picked ->
                val nextMap = config.customRanchColors + (key to picked.toArgb().toLong())
                onUpdate(config.copy(customRanchColors = nextMap))
                editingRanchKey = null
            },
            onDismiss = { editingRanchKey = null },
        )
    }
}

@Composable
private fun AppearanceLivePreviewCard(themeMode: String) {
    val apple = LocalAppleColors.current
    val appearance = LocalAppAppearance.current
    val farmAccent = MaterialTheme.colorScheme.primary
    val ranchAccent = appearance.ranchAccent ?: apple.orange
    val modeSummary = when (themeMode) {
        StoredConfig.THEME_LIGHT -> "浅色"
        StoredConfig.THEME_DARK -> "深色"
        StoredConfig.THEME_CUSTOM -> "${appearance.landGridColumns}×${appearance.ranchAnimalColumns}"
        else -> "跟随系统"
    }

    AppleGroupedCard(
        contentPadding = PaddingValues(12.dp),
        containerColor = apple.card,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "实时预览 · $modeSummary",
                style = MaterialTheme.typography.labelLarge,
                color = apple.primaryText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Surface(shape = CircleShape, color = farmAccent) {
                    Text(
                        text = "农场",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = farmAccent.onAccent(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Surface(shape = CircleShape, color = ranchAccent) {
                    Text(
                        text = "牧场",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = ranchAccent.onAccent(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        EqualHeightRow(spacing = 8.dp) {
            PREVIEW_LAND_SAMPLES.forEach { (level, label, progress) ->
                PreviewMiniLandTile(
                    level = level,
                    label = label,
                    progress = progress,
                    accent = if (level == 6) apple.green else farmAccent,
                    appearance = appearance,
                    modifier = Modifier.weight(1f),
                )
            }
            PreviewMiniRanchTile(
                ranchAccent = ranchAccent,
                appearance = appearance,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = appearance.progressBarStyle.label,
                style = MaterialTheme.typography.labelSmall,
                color = apple.secondaryText,
            )
            AppleProgressBar(
                progress = { 0.68f },
                modifier = Modifier.weight(1f).height(6.dp),
                color = farmAccent,
                trackColor = apple.track,
            )
        }
    }
}

@Composable
private fun PreviewMiniLandTile(
    level: Int,
    label: String,
    progress: Float,
    accent: Color,
    appearance: EffectiveAppearance,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    val colors = resolveLandCardColors(level, apple.isDark, apple, appearance)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appearance.tileRadius),
        color = colors.background,
        border = BorderStroke(
            width = if (appearance.landPreset == LandColorPreset.PURE) 0.5.dp else 0.8.dp,
            color = colors.border,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "#$level",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = apple.secondaryText,
                )
                Surface(shape = CircleShape, color = colors.badgeBackground) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = colors.badgeText,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            AppleProgressBar(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = accent,
                trackColor = if (appearance.landPreset == LandColorPreset.PURE) {
                    apple.track
                } else {
                    colors.badgeText.copy(alpha = 0.14f)
                },
            )
        }
    }
}

@Composable
private fun PreviewMiniRanchTile(
    ranchAccent: Color,
    appearance: EffectiveAppearance,
    modifier: Modifier = Modifier,
) {
    val apple = LocalAppleColors.current
    val ranchColors = resolveRanchAnimalCardColors(
        isShed = false,
        isGiant = true,
        canHarvest = false,
        canProduce = true,
        grassBlocked = false,
        isDark = apple.isDark,
        apple = apple,
        appearance = appearance,
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appearance.tileRadius),
        color = ranchColors.background,
        border = BorderStroke(if (ranchColors.highlightBorder) 1.dp else 0.7.dp, ranchColors.border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "天竺鼠",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = apple.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Surface(shape = CircleShape, color = SoilGold) {
                    Text(
                        text = "巨",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = SoilGold.onAccent(),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(appearance.controlRadius),
                color = ranchAccent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "生产",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ranchAccent.onAccent(),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPresetCard(
    preset: AppearanceQuickPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    Surface(
        modifier = Modifier
            .width(138.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = apple.card,
        border = BorderStroke(
            width = if (selected) 1.4.dp else 0.6.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else apple.separator,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    preset.previewColors.forEach { dot ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dot)
                                .border(0.6.dp, apple.separator, CircleShape),
                        )
                    }
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "已选中",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleSmall,
                color = apple.primaryText,
                maxLines = 1,
            )
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = apple.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private sealed interface AccentGridCell {
    data class Swatch(
        val label: String,
        val color: Color,
        val selected: Boolean,
        val onClick: () -> Unit,
    ) : AccentGridCell

    data class Wheel(
        val selected: Boolean,
        val activeColor: Color?,
        val onClick: () -> Unit,
    ) : AccentGridCell
}

@Composable
private fun FourColumnAccentGrid(cells: List<AccentGridCell>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(4).forEach { rowCells ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rowCells.forEach { cell ->
                    when (cell) {
                        is AccentGridCell.Swatch -> AccentSwatchButton(
                            label = cell.label,
                            color = cell.color,
                            selected = cell.selected,
                            modifier = Modifier.weight(1f),
                            onClick = cell.onClick,
                        )
                        is AccentGridCell.Wheel -> AccentWheelButton(
                            selected = cell.selected,
                            activeColor = cell.activeColor,
                            modifier = Modifier.weight(1f),
                            onClick = cell.onClick,
                        )
                    }
                }
                repeat(4 - rowCells.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccentSwatchButton(
    label: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val selectionTint = remember(color) { color.onAccent(3f) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (selected) Modifier.border(2.dp, color.readableOn(apple.card, 3f), CircleShape) else Modifier
                )
                .padding(3.5.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "已选中",
                    tint = selectionTint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) apple.primaryText else apple.secondaryText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AccentWheelButton(
    selected: Boolean,
    activeColor: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val selectionTint = remember(activeColor) { activeColor?.onAccent(3f) ?: Color.White }
    val wheelColors = remember {
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (selected && activeColor != null) Modifier.border(2.dp, activeColor.readableOn(apple.card, 3f), CircleShape) else Modifier
                )
                .padding(3.5.dp)
                .clip(CircleShape)
                .then(
                    if (activeColor != null) {
                        Modifier.background(activeColor)
                    } else {
                        Modifier.background(Brush.sweepGradient(wheelColors))
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.Check else Icons.Rounded.ColorLens,
                contentDescription = if (selected) "已选中" else "颜色环选色",
                tint = selectionTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = if (activeColor != null) colorToHex(activeColor) else "颜色环",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontFeatureSettings = "tnum",
            ),
            color = if (selected) apple.primaryText else apple.secondaryText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TagBadge(text: String, tint: Color) {
    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = tint,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun CompactPresetActionChip(label: String, onClick: () -> Unit) {
    val apple = LocalAppleColors.current
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = apple.track,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = apple.primaryText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppleColorWheelDialog(
    title: String,
    initialColor: Color,
    quickSwatches: List<Pair<String, Color>>,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { com.qqfarm.mobile.platform.hsvOf(initialColor.toArgb()).copyInto(it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv[2].coerceIn(0.15f, 1f)) }
    val currentColor = Color(com.qqfarm.mobile.platform.colorFromHsv(hue, sat, value))
    var hexInput by remember(initialColor) { mutableStateOf(colorToHex(currentColor)) }

    val applyPickedColor: (Color) -> Unit = { color ->
        val hsv = FloatArray(3)
        com.qqfarm.mobile.platform.hsvOf(color.toArgb()).copyInto(hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2].coerceIn(0.15f, 1f)
        hexInput = colorToHex(Color(com.qqfarm.mobile.platform.colorFromHsv(hue, sat, value)))
    }

    val onHsvTouch by rememberUpdatedState { nextHue: Float, nextSat: Float ->
        hue = nextHue
        sat = nextSat
        hexInput = colorToHex(Color(com.qqfarm.mobile.platform.colorFromHsv(nextHue, nextSat, value)))
    }

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(188.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(188.dp)
                            .clip(CircleShape)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val radius = size.width / 2f
                                    val cx = radius
                                    val cy = radius
                                    fun emitOffset(pos: Offset) {
                                        val dx = pos.x - cx
                                        val dy = pos.y - cy
                                        val angle = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                                        val normSat = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                                        onHsvTouch(angle, normSat)
                                    }
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    emitOffset(down.position)
                                    drag(down.id) { change ->
                                        change.consume()
                                        emitOffset(change.position)
                                    }
                                }
                            },
                    ) {
                        val radius = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red,
                                ),
                                center = center,
                            ),
                            radius = radius,
                            center = center,
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                        if (value < 0.999f) {
                            drawCircle(
                                color = Color.Black.copy(alpha = 1f - value),
                                radius = radius,
                                center = center,
                            )
                        }
                        val rad = (hue * PI / 180.0)
                        val knobX = center.x + (sat * radius * cos(rad)).toFloat()
                        val knobY = center.y + (sat * radius * sin(rad)).toFloat()
                        val knobCenter = Offset(knobX, knobY)
                        drawCircle(
                            color = currentColor,
                            radius = 9.dp.toPx(),
                            center = knobCenter,
                        )
                        drawCircle(
                            color = currentColor.onAccent(3f),
                            radius = 9.dp.toPx(),
                            center = knobCenter,
                            style = Stroke(width = 2.5.dp.toPx()),
                        )
                        drawCircle(
                            color = currentColor,
                            radius = 10.5.dp.toPx(),
                            center = knobCenter,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "明度",
                            style = MaterialTheme.typography.labelMedium,
                            color = apple.secondaryText,
                        )
                        Text(
                            text = "${(value * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = apple.primaryText,
                        )
                    }
                    Slider(
                        value = value,
                        onValueChange = { nextV ->
                            value = nextV.coerceIn(0.15f, 1f)
                            hexInput = colorToHex(Color(com.qqfarm.mobile.platform.colorFromHsv(hue, sat, value)))
                        },
                        valueRange = 0.15f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(currentColor)
                            .border(1.dp, apple.separator, RoundedCornerShape(10.dp)),
                    )
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { typed ->
                            val clean = typed.uppercase().take(7)
                            hexInput = clean
                            parseHexColorOrNull(clean)?.let { parsed ->
                                val hsv = FloatArray(3)
                                com.qqfarm.mobile.platform.hsvOf(parsed.toArgb()).copyInto(hsv)
                                hue = hsv[0]
                                sat = hsv[1]
                                value = hsv[2].coerceIn(0.15f, 1f)
                            }
                        },
                        label = { Text("色值 (#RRGGBB)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (quickSwatches.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "常用候选",
                            style = MaterialTheme.typography.labelMedium,
                            color = apple.secondaryText,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            quickSwatches.forEach { (label, swatchColor) ->
                                val selected = swatchColor.toArgb() == currentColor.toArgb()
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { applyPickedColor(swatchColor) },
                                    shape = CircleShape,
                                    color = if (selected) apple.elevated else apple.track,
                                    border = if (selected) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else null,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(0.8.dp, apple.separator, CircleShape),
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = apple.primaryText,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parseHexColorOrNull(hexInput) ?: currentColor) }) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun landTierQuickSwatches(level: Int): List<Pair<String, Color>> = listOf(
    "纯白" to Color.White,
    "网页默认" to webLandLightBackground(level),
    "淡蓝晶" to Color(0xFFEEF8FD),
    "淡紫金" to Color(0xFFF8F3FD),
    "暖金" to Color(0xFFFFFAE8),
    "浅红" to Color(0xFFFDF4F1),
    "柔青" to Color(0xFFEEFBF3),
    "素灰" to Color(0xFFF1F5F9),
)

private fun ranchKeyQuickSwatches(key: String): List<Pair<String, Color>> = listOf(
    "纯白" to Color.White,
    "默认色" to ranchDefaultLightBackground(key),
    "暖米" to Color(0xFFFDF7EE),
    "淡青" to Color(0xFFEFF8FD),
    "暖金" to Color(0xFFFFF8E1),
    "淡绿" to Color(0xFFEEFBF2),
    "淡紫" to Color(0xFFF8F3FD),
    "淡橙" to Color(0xFFFFF4EB),
)

private fun colorToHex(color: Color): String =
    run {
        val argb = 0xFFFFFF and color.toArgb()
        "#" + argb.toString(16).uppercase().padStart(6, '0')
    }

private fun parseHexColorOrNull(input: String): Color? {
    val clean = input.trim().removePrefix("#")
    if (clean.length != 6 || !clean.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }) {
        return null
    }
    val rgb = clean.toLong(16)
    return Color((0xFF000000L or rgb).toInt())
}
