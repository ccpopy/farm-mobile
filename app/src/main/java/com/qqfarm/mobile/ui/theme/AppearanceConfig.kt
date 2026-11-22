package com.qqfarm.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.prefs.StoredConfig
import org.json.JSONObject

enum class LandColorPreset(val key: String, val label: String, val desc: String) {
    WEB("web", "网页淡彩", "复刻网页端 6 级土地淡彩"),
    PURE("pure", "简约纯白", "原版纯白卡片，仅土壤徽章着彩"),
    SOFT("soft", "清透柔色", "轻薄柔和着底与细描边"),
    VIVID("vivid", "鲜明彩卡", "更高饱和度，土地等级一眼区分"),
    CUSTOM("custom", "自定义", "按土地等级用颜色环自由选色");

    companion object {
        fun fromKey(key: String?): LandColorPreset =
            entries.firstOrNull { it.key == key } ?: WEB
    }
}

enum class BackgroundTone(
    val key: String,
    val label: String,
    val lightDesc: String,
    val darkDesc: String,
) {
    STANDARD("standard", "默认", "冷灰底 + 纯白卡", "经典黑底 + 深灰卡"),
    WARM_SOFT("warm_soft", "暖纸 / 柔灰", "护眼暖米纸底（网页风）", "柔和炭灰底（降对比）"),
    PURE_AMOLED("pure_amoled", "亮白 / AMOLED", "清透纯白底 + 微灰卡", "AMOLED 纯黑 + 深邃黑卡");

    companion object {
        fun fromKey(key: String?): BackgroundTone =
            entries.firstOrNull { it.key == key } ?: STANDARD
    }
}

enum class RanchAnimalColorPreset(val key: String, val label: String, val desc: String) {
    PURE("pure", "简约纯白", "原版纯白卡片，巨大与可收获仅边框提示"),
    STATUS("status", "状态着彩", "可收获淡绿、可生产淡蓝、巨大暖金、缺草淡橙"),
    PEN("pen", "窝棚区分", "窝暖米、棚淡青，巨大动物自动金底高亮"),
    CUSTOM("custom", "自定义", "按窝 / 棚 / 巨大动物用颜色环自由选色");

    companion object {
        fun fromKey(key: String?): RanchAnimalColorPreset =
            entries.firstOrNull { it.key == key } ?: STATUS
    }
}

enum class ProgressBarStyle(val key: String, val label: String) {
    SMOOTH("smooth", "平滑圆轨"),
    SEGMENTED("segmented", "分段指示"),
    GRADIENT("gradient", "渐变流光");

    companion object {
        fun fromKey(key: String?): ProgressBarStyle =
            entries.firstOrNull { it.key == key } ?: SMOOTH
    }
}

enum class CornerStyle(val key: String, val label: String) {
    ROUNDED("rounded", "柔和圆润"),
    STANDARD("standard", "标准"),
    COMPACT("compact", "利落方正");

    companion object {
        fun fromKey(key: String?): CornerStyle =
            entries.firstOrNull { it.key == key } ?: STANDARD
    }
}

enum class CardSurfaceStyle(val key: String, val label: String) {
    BORDERED("bordered", "细腻描边"),
    FLAT("flat", "纯平无框"),
    ELEVATED("elevated", "轻盈阴影");

    companion object {
        fun fromKey(key: String?): CardSurfaceStyle =
            entries.firstOrNull { it.key == key } ?: BORDERED
    }
}

@Immutable
data class CustomAppearanceConfig(
    val baseThemeMode: String = StoredConfig.THEME_SYSTEM,
    val backgroundTone: BackgroundTone = BackgroundTone.STANDARD,
    val accentColorArgb: Long? = null,
    val ranchAccentFollowPrimary: Boolean = false,
    val ranchAccentColorArgb: Long? = null,
    val landPreset: LandColorPreset = LandColorPreset.WEB,
    val customLandColors: Map<Int, Long> = emptyMap(),
    val landDetailTinted: Boolean = true,
    val ranchAnimalPreset: RanchAnimalColorPreset = RanchAnimalColorPreset.STATUS,
    val customRanchColors: Map<String, Long> = emptyMap(),
    val landGridColumns: Int = 3,
    val ranchAnimalColumns: Int = 2,
    val progressBarStyle: ProgressBarStyle = ProgressBarStyle.SMOOTH,
    val cornerStyle: CornerStyle = CornerStyle.STANDARD,
    val cardSurfaceStyle: CardSurfaceStyle = CardSurfaceStyle.BORDERED,
) {
    fun toJsonString(): String {
        val obj = JSONObject()
            .put("baseThemeMode", baseThemeMode)
            .put("backgroundTone", backgroundTone.key)
            .put("ranchAccentFollowPrimary", ranchAccentFollowPrimary)
            .put("landPreset", landPreset.key)
            .put("landDetailTinted", landDetailTinted)
            .put("ranchAnimalPreset", ranchAnimalPreset.key)
            .put("landGridColumns", landGridColumns)
            .put("ranchAnimalColumns", ranchAnimalColumns)
            .put("progressBarStyle", progressBarStyle.key)
            .put("cornerStyle", cornerStyle.key)
            .put("cardSurfaceStyle", cardSurfaceStyle.key)
        if (accentColorArgb != null) {
            obj.put("accentColorArgb", accentColorArgb)
        }
        if (ranchAccentColorArgb != null) {
            obj.put("ranchAccentColorArgb", ranchAccentColorArgb)
        }
        if (customLandColors.isNotEmpty()) {
            val landObj = JSONObject()
            customLandColors.forEach { (level, argb) ->
                landObj.put(level.toString(), argb)
            }
            obj.put("customLandColors", landObj)
        }
        if (customRanchColors.isNotEmpty()) {
            val ranchObj = JSONObject()
            customRanchColors.forEach { (key, argb) ->
                ranchObj.put(key, argb)
            }
            obj.put("customRanchColors", ranchObj)
        }
        return obj.toString()
    }

    companion object {
        private val VALID_RANCH_KEYS = listOf("coop", "shed", "giant")

        fun fromJsonString(raw: String): CustomAppearanceConfig {
            if (raw.isBlank()) return CustomAppearanceConfig()
            return runCatching {
                val obj = JSONObject(raw)
                val baseMode = when (obj.optString("baseThemeMode")) {
                    StoredConfig.THEME_LIGHT -> StoredConfig.THEME_LIGHT
                    StoredConfig.THEME_DARK -> StoredConfig.THEME_DARK
                    else -> StoredConfig.THEME_SYSTEM
                }
                val accent = if (obj.has("accentColorArgb") && !obj.isNull("accentColorArgb")) {
                    obj.optLong("accentColorArgb").toInt().toLong()
                } else null
                val ranchAccent = if (obj.has("ranchAccentColorArgb") && !obj.isNull("ranchAccentColorArgb")) {
                    obj.optLong("ranchAccentColorArgb").toInt().toLong()
                } else null
                val landObj = obj.optJSONObject("customLandColors")
                val landMap = if (landObj != null) {
                    buildMap {
                        for (level in 1..6) {
                            val key = level.toString()
                            if (landObj.has(key) && !landObj.isNull(key)) {
                                put(level, landObj.optLong(key).toInt().toLong())
                            }
                        }
                    }
                } else emptyMap()
                val ranchObj = obj.optJSONObject("customRanchColors")
                val ranchMap = if (ranchObj != null) {
                    buildMap {
                        VALID_RANCH_KEYS.forEach { key ->
                            if (ranchObj.has(key) && !ranchObj.isNull(key)) {
                                put(key, ranchObj.optLong(key).toInt().toLong())
                            }
                        }
                    }
                } else emptyMap()
                val landCols = if (obj.optInt("landGridColumns", 3) == 4) 4 else 3
                val animalCols = if (obj.optInt("ranchAnimalColumns", 2) == 3) 3 else 2
                CustomAppearanceConfig(
                    baseThemeMode = baseMode,
                    backgroundTone = BackgroundTone.fromKey(obj.optString("backgroundTone")),
                    accentColorArgb = accent,
                    ranchAccentFollowPrimary = obj.optBoolean("ranchAccentFollowPrimary", false),
                    ranchAccentColorArgb = ranchAccent,
                    landPreset = LandColorPreset.fromKey(obj.optString("landPreset")),
                    customLandColors = landMap,
                    landDetailTinted = obj.optBoolean("landDetailTinted", true),
                    ranchAnimalPreset = RanchAnimalColorPreset.fromKey(obj.optString("ranchAnimalPreset")),
                    customRanchColors = ranchMap,
                    landGridColumns = landCols,
                    ranchAnimalColumns = animalCols,
                    progressBarStyle = ProgressBarStyle.fromKey(obj.optString("progressBarStyle")),
                    cornerStyle = CornerStyle.fromKey(obj.optString("cornerStyle")),
                    cardSurfaceStyle = CardSurfaceStyle.fromKey(obj.optString("cardSurfaceStyle")),
                )
            }.getOrDefault(CustomAppearanceConfig())
        }
    }
}

@Immutable
data class EffectiveAppearance(
    val backgroundTone: BackgroundTone = BackgroundTone.STANDARD,
    val landPreset: LandColorPreset = LandColorPreset.PURE,
    val customLandColors: Map<Int, Long> = emptyMap(),
    val landDetailTinted: Boolean = false,
    val ranchAnimalPreset: RanchAnimalColorPreset = RanchAnimalColorPreset.PURE,
    val customRanchColors: Map<String, Long> = emptyMap(),
    val landGridColumns: Int = 3,
    val ranchAnimalColumns: Int = 2,
    val progressBarStyle: ProgressBarStyle = ProgressBarStyle.SEGMENTED,
    val cornerStyle: CornerStyle = CornerStyle.STANDARD,
    val cardRadius: Dp = 14.dp,
    val tileRadius: Dp = 12.dp,
    val controlRadius: Dp = 10.dp,
    val cardSurfaceStyle: CardSurfaceStyle = CardSurfaceStyle.BORDERED,
    val customAccent: Color? = null,
    val ranchAccent: Color? = null,
)

val LocalAppAppearance = staticCompositionLocalOf { EffectiveAppearance() }

fun resolveEffectiveAppearance(
    themeMode: String,
    custom: CustomAppearanceConfig,
): EffectiveAppearance {
    if (themeMode != StoredConfig.THEME_CUSTOM) return EffectiveAppearance()
    val (cardR, tileR, ctrlR) = when (custom.cornerStyle) {
        CornerStyle.ROUNDED -> Triple(18.dp, 14.dp, 12.dp)
        CornerStyle.STANDARD -> Triple(14.dp, 12.dp, 10.dp)
        CornerStyle.COMPACT -> Triple(8.dp, 6.dp, 6.dp)
    }
    return EffectiveAppearance(
        backgroundTone = custom.backgroundTone,
        landPreset = custom.landPreset,
        customLandColors = custom.customLandColors,
        landDetailTinted = custom.landDetailTinted,
        ranchAnimalPreset = custom.ranchAnimalPreset,
        customRanchColors = custom.customRanchColors,
        landGridColumns = if (custom.landGridColumns == 4) 4 else 3,
        ranchAnimalColumns = if (custom.ranchAnimalColumns == 3) 3 else 2,
        progressBarStyle = custom.progressBarStyle,
        cornerStyle = custom.cornerStyle,
        cardRadius = cardR,
        tileRadius = tileR,
        controlRadius = ctrlR,
        cardSurfaceStyle = custom.cardSurfaceStyle,
        customAccent = custom.accentColorArgb?.let { Color(it.toInt()) },
    )
}

// 沿当前色相增减明度，取在实际背景上达到目标对比度的最近色阶。
internal fun Color.readableOn(surface: Color, minContrast: Float = 4.5f): Color {
    val surfaceLight = surface.luminance()
    val lightLimit = (surfaceLight + 0.05f) * minContrast - 0.05f
    val darkLimit = (surfaceLight + 0.05f) / minContrast - 0.05f
    fun meetsContrast(color: Color) = color.luminance().let { it >= lightLimit || it <= darkLimit }
    if (meetsContrast(this)) return this

    val end = if (surfaceLight > 0.179f) 0f else 1f
    var low = 0f
    var high = 1f
    var result = if (end == 0f) Color.Black else Color.White
    repeat(9) {
        val mix = (low + high) / 2f
        val tone = copy(
            red = red + (end - red) * mix,
            green = green + (end - green) * mix,
            blue = blue + (end - blue) * mix,
        )
        if (meetsContrast(tone)) {
            result = tone
            high = mix
        } else {
            low = mix
        }
    }
    return result
}

internal fun Color.onAccent(minContrast: Float = 4.5f): Color =
    if ((luminance() + 0.05f) * minContrast <= 1.05f) Color.White else readableOn(this, minContrast)

internal fun CustomAppearanceConfig.resolveRanchAccent(primary: Color, dark: Boolean): Color? {
    if (ranchAccentFollowPrimary) return primary
    val custom = ranchAccentColorArgb?.let { Color(it.toInt()) } ?: return null
    return if (dark) lerp(custom, Color.White, 0.16f) else custom
}

val LAND_TIER_SPECS = listOf(
    1 to "普通",
    2 to "红土",
    3 to "黑土",
    4 to "金土",
    5 to "紫金",
    6 to "蓝晶",
)

val ACCENT_PRESETS: List<Pair<String, Color?>> = listOf(
    "农牧默认" to null,
    "晴空蓝" to Color(0xFF007AFF),
    "薰衣草" to Color(0xFF6E56CF),
    "樱花粉" to Color(0xFFE5487B),
    "琥珀金" to Color(0xFFD97706),
    "青松青" to Color(0xFF0D9488),
    "珊瑚红" to Color(0xFFEF4444),
)

val RANCH_ACCENT_PRESETS: List<Pair<String, Color?>> = listOf(
    "经典橙" to null,
    "暖琥珀" to Color(0xFFD97706),
    "珊瑚橘" to Color(0xFFF97316),
    "焦糖棕" to Color(0xFFB45309),
    "玫瑰粉" to Color(0xFFE11D48),
    "青松绿" to Color(0xFF0D9488),
)

val RANCH_COLOR_SPECS = listOf(
    "coop" to "窝 · 动物",
    "shed" to "棚 · 动物",
    "giant" to "巨大动物",
)

@Immutable
data class AppearanceQuickPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val previewColors: List<Color>,
    val config: CustomAppearanceConfig,
)

val APPEARANCE_QUICK_PRESETS: List<AppearanceQuickPreset> = listOf(
    AppearanceQuickPreset(
        id = "web_classic",
        name = "网页复刻",
        subtitle = "暖纸护眼 · 网页淡彩",
        previewColors = listOf(Color(0xFFF6F4EC), Color(0xFFEEF8FD), AppleOrange),
        config = CustomAppearanceConfig(
            baseThemeMode = StoredConfig.THEME_LIGHT,
            backgroundTone = BackgroundTone.WARM_SOFT,
            accentColorArgb = null,
            ranchAccentFollowPrimary = false,
            ranchAccentColorArgb = null,
            landPreset = LandColorPreset.WEB,
            ranchAnimalPreset = RanchAnimalColorPreset.PEN,
            landGridColumns = 3,
            ranchAnimalColumns = 2,
            progressBarStyle = ProgressBarStyle.SMOOTH,
            cornerStyle = CornerStyle.STANDARD,
            cardSurfaceStyle = CardSurfaceStyle.BORDERED,
        ),
    ),
    AppearanceQuickPreset(
        id = "pure_white",
        name = "原版纯白",
        subtitle = "极简纯白 · 平滑圆轨",
        previewColors = listOf(Color.White, AppleEmerald, AppleOrange),
        config = CustomAppearanceConfig(
            baseThemeMode = StoredConfig.THEME_LIGHT,
            backgroundTone = BackgroundTone.STANDARD,
            accentColorArgb = null,
            ranchAccentFollowPrimary = false,
            ranchAccentColorArgb = null,
            landPreset = LandColorPreset.PURE,
            landDetailTinted = false,
            ranchAnimalPreset = RanchAnimalColorPreset.PURE,
            landGridColumns = 3,
            ranchAnimalColumns = 2,
            progressBarStyle = ProgressBarStyle.SMOOTH,
            cornerStyle = CornerStyle.STANDARD,
            cardSurfaceStyle = CardSurfaceStyle.BORDERED,
        ),
    ),
    AppearanceQuickPreset(
        id = "compact_pro",
        name = "高密效率",
        subtitle = "4列农田 · 3列牧场",
        previewColors = listOf(Color(0xFF007AFF), Color(0xFFF97316), Color(0xFFEEF8FD)),
        config = CustomAppearanceConfig(
            baseThemeMode = StoredConfig.THEME_SYSTEM,
            backgroundTone = BackgroundTone.STANDARD,
            accentColorArgb = Color(0xFF007AFF).toArgb().toLong(),
            ranchAccentFollowPrimary = false,
            ranchAccentColorArgb = Color(0xFFF97316).toArgb().toLong(),
            landPreset = LandColorPreset.SOFT,
            ranchAnimalPreset = RanchAnimalColorPreset.STATUS,
            landGridColumns = 4,
            ranchAnimalColumns = 3,
            progressBarStyle = ProgressBarStyle.SMOOTH,
            cornerStyle = CornerStyle.COMPACT,
            cardSurfaceStyle = CardSurfaceStyle.BORDERED,
        ),
    ),
    AppearanceQuickPreset(
        id = "amoled_glow",
        name = "暗夜流光",
        subtitle = "AMOLED 纯黑 · 渐变",
        previewColors = listOf(Color.Black, Color(0xFF6E56CF), Color(0xFFD97706)),
        config = CustomAppearanceConfig(
            baseThemeMode = StoredConfig.THEME_DARK,
            backgroundTone = BackgroundTone.PURE_AMOLED,
            accentColorArgb = Color(0xFF6E56CF).toArgb().toLong(),
            ranchAccentFollowPrimary = false,
            ranchAccentColorArgb = Color(0xFFD97706).toArgb().toLong(),
            landPreset = LandColorPreset.VIVID,
            ranchAnimalPreset = RanchAnimalColorPreset.STATUS,
            landGridColumns = 3,
            ranchAnimalColumns = 2,
            progressBarStyle = ProgressBarStyle.GRADIENT,
            cornerStyle = CornerStyle.ROUNDED,
            cardSurfaceStyle = CardSurfaceStyle.BORDERED,
        ),
    ),
)

@Immutable
data class LandCardColors(
    val background: Color,
    val border: Color,
    val badgeBackground: Color,
    val badgeText: Color,
)

@Immutable
data class RanchAnimalCardColors(
    val background: Color,
    val border: Color,
    val highlightBorder: Boolean,
)

fun ranchDefaultLightBackground(key: String): Color = when (key) {
    "shed" -> Color(0xFFEFF8FD)
    "giant" -> Color(0xFFFFF8E1)
    else -> Color(0xFFFDF7EE)
}

fun resolveRanchAnimalCardColors(
    isShed: Boolean,
    isGiant: Boolean,
    canHarvest: Boolean,
    canProduce: Boolean,
    grassBlocked: Boolean,
    isDark: Boolean,
    apple: AppleExtendedColors,
    appearance: EffectiveAppearance,
): RanchAnimalCardColors {
    val defaultBorder = when {
        isGiant -> SoilGold
        canHarvest -> apple.green
        else -> apple.separator
    }
    val defaultHighlight = isGiant || canHarvest

    return when (appearance.ranchAnimalPreset) {
        RanchAnimalColorPreset.PURE -> RanchAnimalCardColors(
            background = apple.card,
            border = defaultBorder,
            highlightBorder = defaultHighlight,
        )
        RanchAnimalColorPreset.STATUS -> when {
            isGiant -> RanchAnimalCardColors(
                background = if (isDark) lerp(apple.card, SoilGold, 0.14f) else Color(0xFFFFF9E6),
                border = if (isDark) SoilGold.copy(alpha = 0.72f) else SoilGold,
                highlightBorder = true,
            )
            canHarvest -> RanchAnimalCardColors(
                background = if (isDark) lerp(apple.card, apple.green, 0.13f) else Color(0xFFEEFBF2),
                border = apple.green,
                highlightBorder = true,
            )
            canProduce -> RanchAnimalCardColors(
                background = if (isDark) lerp(apple.card, apple.blue, 0.12f) else Color(0xFFEEF7FE),
                border = apple.blue.copy(alpha = if (isDark) 0.45f else 0.55f),
                highlightBorder = false,
            )
            grassBlocked -> RanchAnimalCardColors(
                background = if (isDark) lerp(apple.card, apple.orange, 0.13f) else Color(0xFFFFF4EB),
                border = apple.orange.copy(alpha = if (isDark) 0.50f else 0.70f),
                highlightBorder = false,
            )
            else -> RanchAnimalCardColors(
                background = apple.card,
                border = apple.separator,
                highlightBorder = false,
            )
        }
        RanchAnimalColorPreset.PEN -> when {
            isGiant -> RanchAnimalCardColors(
                background = if (isDark) lerp(apple.card, SoilGold, 0.14f) else Color(0xFFFFF8E1),
                border = SoilGold,
                highlightBorder = true,
            )
            isShed -> {
                val darkTint = Color(0xFF60B5DF)
                RanchAnimalCardColors(
                    background = if (isDark) lerp(apple.card, darkTint, 0.11f) else Color(0xFFEFF8FD),
                    border = when {
                        canHarvest -> apple.green
                        isDark -> darkTint.copy(alpha = 0.32f)
                        else -> Color(0xFFB8DAEC)
                    },
                    highlightBorder = canHarvest,
                )
            }
            else -> {
                val darkTint = Color(0xFFE0A865)
                RanchAnimalCardColors(
                    background = if (isDark) lerp(apple.card, darkTint, 0.11f) else Color(0xFFFDF7EE),
                    border = when {
                        canHarvest -> apple.green
                        isDark -> darkTint.copy(alpha = 0.32f)
                        else -> Color(0xFFE9D4B5)
                    },
                    highlightBorder = canHarvest,
                )
            }
        }
        RanchAnimalColorPreset.CUSTOM -> {
            val key = when {
                isGiant -> "giant"
                isShed -> "shed"
                else -> "coop"
            }
            val picked = appearance.customRanchColors[key]?.let { Color(it.toInt()) }
                ?: ranchDefaultLightBackground(key)
            val pureWhite = picked.toArgb() == android.graphics.Color.WHITE
            if (pureWhite) {
                RanchAnimalCardColors(
                    background = apple.card,
                    border = defaultBorder,
                    highlightBorder = defaultHighlight,
                )
            } else if (picked.luminance() >= 0.72f) {
                val bg = if (isDark) lerp(apple.card, picked, 0.13f) else picked
                val border = when {
                    isGiant -> SoilGold
                    canHarvest -> apple.green
                    isDark -> picked.copy(alpha = 0.34f)
                    else -> lerp(picked, Color.Black, 0.14f)
                }
                RanchAnimalCardColors(
                    background = bg,
                    border = border,
                    highlightBorder = defaultHighlight,
                )
            } else {
                val bg = if (isDark) lerp(apple.card, picked, 0.18f) else lerp(Color.White, picked, 0.14f)
                val border = when {
                    isGiant -> SoilGold
                    canHarvest -> apple.green
                    isDark -> picked.copy(alpha = 0.42f)
                    else -> picked.copy(alpha = 0.36f)
                }
                RanchAnimalCardColors(
                    background = bg,
                    border = border,
                    highlightBorder = defaultHighlight,
                )
            }
        }
    }
}

private data class WebSoilPalette(
    val bg: Color,
    val border: Color,
    val chipBg: Color,
    val ink: Color,
    val darkTint: Color,
    val baseBadgeColor: Color,
)

fun webLandLightBackground(level: Int): Color = webPaletteForLevel(level).bg

private fun webPaletteForLevel(level: Int): WebSoilPalette = when (level) {
    2 -> WebSoilPalette(
        bg = Color(0xFFFDF4F1),
        border = Color(0xFFEECBC1),
        chipBg = Color(0xFFF8DDD5),
        ink = Color(0xFFB0482F),
        darkTint = Color(0xFFE08A6E),
        baseBadgeColor = SoilRed,
    )
    3 -> WebSoilPalette(
        bg = Color(0xFFF4F3F1),
        border = Color(0xFFD3CFC8),
        chipBg = Color(0xFFE3DFD9),
        ink = Color(0xFF4B4540),
        darkTint = Color(0xFF8D939B),
        baseBadgeColor = SoilBlack,
    )
    4 -> WebSoilPalette(
        bg = Color(0xFFFFFAE8),
        border = Color(0xFFEAD57F),
        chipBg = Color(0xFFF9E9A8),
        ink = Color(0xFF946600),
        darkTint = Color(0xFFE6C35A),
        baseBadgeColor = SoilGold,
    )
    5 -> WebSoilPalette(
        bg = Color(0xFFF8F3FD),
        border = Color(0xFFD7C6EF),
        chipBg = Color(0xFFEADFFA),
        ink = Color(0xFF7140B5),
        darkTint = Color(0xFFB892F0),
        baseBadgeColor = SoilPurpleGold,
    )
    6 -> WebSoilPalette(
        bg = Color(0xFFEEF8FD),
        border = Color(0xFFB5D9EB),
        chipBg = Color(0xFFD7EDF8),
        ink = Color(0xFF367CAA),
        darkTint = Color(0xFF82BDDF),
        baseBadgeColor = SoilBlueCrystal,
    )
    else -> WebSoilPalette(
        bg = Color(0xFFFBF8F1),
        border = Color(0xFFE6DBC2),
        chipBg = Color(0xFFF1E7D2),
        ink = Color(0xFF86642E),
        darkTint = Color(0xFFC9A867),
        baseBadgeColor = SoilNormal,
    )
}

fun resolveLandCardColors(
    level: Int,
    isDark: Boolean,
    apple: AppleExtendedColors,
    appearance: EffectiveAppearance,
): LandCardColors {
    val web = webPaletteForLevel(level)
    return when (appearance.landPreset) {
        LandColorPreset.PURE -> LandCardColors(
            background = apple.card,
            border = apple.separator,
            badgeBackground = web.baseBadgeColor.copy(alpha = 0.16f),
            badgeText = web.baseBadgeColor,
        )
        LandColorPreset.WEB -> if (isDark) {
            LandCardColors(
                background = lerp(apple.card, web.darkTint, 0.11f),
                border = web.darkTint.copy(alpha = 0.30f),
                badgeBackground = web.darkTint.copy(alpha = 0.16f),
                badgeText = web.darkTint,
            )
        } else {
            LandCardColors(
                background = web.bg,
                border = web.border,
                badgeBackground = web.chipBg,
                badgeText = web.ink,
            )
        }
        LandColorPreset.SOFT -> if (isDark) {
            LandCardColors(
                background = lerp(apple.card, web.darkTint, 0.06f),
                border = lerp(apple.separator, web.darkTint, 0.22f),
                badgeBackground = web.darkTint.copy(alpha = 0.14f),
                badgeText = web.darkTint,
            )
        } else {
            LandCardColors(
                background = lerp(Color.White, web.bg, 0.55f),
                border = lerp(apple.separator, web.border, 0.55f),
                badgeBackground = web.chipBg.copy(alpha = 0.72f),
                badgeText = web.ink,
            )
        }
        LandColorPreset.VIVID -> if (isDark) {
            LandCardColors(
                background = lerp(apple.card, web.darkTint, 0.20f),
                border = web.darkTint.copy(alpha = 0.45f),
                badgeBackground = web.darkTint.copy(alpha = 0.24f),
                badgeText = lerp(web.darkTint, Color.White, 0.18f),
            )
        } else {
            LandCardColors(
                background = lerp(web.bg, web.baseBadgeColor, 0.14f),
                border = lerp(web.border, web.baseBadgeColor, 0.35f),
                badgeBackground = lerp(web.chipBg, web.baseBadgeColor, 0.18f),
                badgeText = lerp(web.ink, Color.Black, 0.12f),
            )
        }
        LandColorPreset.CUSTOM -> {
            val customArgb = appearance.customLandColors[level]
                ?: return resolveLandCardColors(
                    level = level,
                    isDark = isDark,
                    apple = apple,
                    appearance = appearance.copy(landPreset = LandColorPreset.WEB),
                )
            val picked = Color(customArgb.toInt())
            val pureWhite = picked.toArgb() == android.graphics.Color.WHITE
            if (pureWhite) {
                LandCardColors(
                    background = apple.card,
                    border = apple.separator,
                    badgeBackground = web.baseBadgeColor.copy(alpha = 0.16f),
                    badgeText = web.baseBadgeColor,
                )
            } else if (picked.luminance() >= 0.72f) {
                if (isDark) {
                    LandCardColors(
                        background = lerp(apple.card, picked, 0.12f),
                        border = picked.copy(alpha = 0.30f),
                        badgeBackground = picked.copy(alpha = 0.18f),
                        badgeText = lerp(picked, Color.White, 0.18f),
                    )
                } else {
                    val accent = lerp(picked, Color(0xFF2B2B2E), 0.52f)
                    LandCardColors(
                        background = picked,
                        border = lerp(picked, Color.Black, 0.14f),
                        badgeBackground = lerp(picked, Color.Black, 0.08f),
                        badgeText = accent,
                    )
                }
            } else {
                if (isDark) {
                    val softText = lerp(picked, Color.White, 0.36f)
                    LandCardColors(
                        background = lerp(apple.card, picked, 0.18f),
                        border = picked.copy(alpha = 0.42f),
                        badgeBackground = picked.copy(alpha = 0.24f),
                        badgeText = softText,
                    )
                } else {
                    val darkText = if (picked.luminance() > 0.35f) lerp(picked, Color.Black, 0.32f) else picked
                    LandCardColors(
                        background = lerp(Color.White, picked, 0.14f),
                        border = picked.copy(alpha = 0.36f),
                        badgeBackground = picked.copy(alpha = 0.18f),
                        badgeText = darkText,
                    )
                }
            }
        }
    }
}
