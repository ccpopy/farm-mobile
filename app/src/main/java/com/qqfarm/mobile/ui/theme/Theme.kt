package com.qqfarm.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.ImageLoader
import com.qqfarm.mobile.data.prefs.StoredConfig
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

@Immutable
data class AppleExtendedColors(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val elevated: Color,
    val track: Color,
    val separator: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val emerald: Color,
    val blue: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val indigo: Color,
    val cyan: Color,
)

private val LightExtendedColors = AppleExtendedColors(
    isDark = false,
    background = AppleLightBackground,
    card = AppleLightCard,
    elevated = AppleLightElevated,
    track = AppleLightTrack,
    separator = AppleLightSeparator,
    primaryText = AppleLightLabel,
    secondaryText = AppleLightSecondaryLabel,
    emerald = AppleEmerald,
    blue = AppleBlue,
    green = AppleGreen,
    orange = AppleOrange,
    red = AppleRed,
    indigo = AppleIndigo,
    cyan = AppleCyan,
)

private val DarkExtendedColors = AppleExtendedColors(
    isDark = true,
    background = AppleDarkBackground,
    card = AppleDarkCard,
    elevated = AppleDarkElevated,
    track = AppleDarkTrack,
    separator = AppleDarkSeparator,
    primaryText = AppleDarkLabel,
    secondaryText = AppleDarkSecondaryLabel,
    emerald = AppleEmeraldDark,
    blue = AppleBlueDark,
    green = AppleGreenDark,
    orange = AppleOrangeDark,
    red = AppleRedDark,
    indigo = AppleIndigoDark,
    cyan = AppleCyanDark,
)

val LocalAppleColors = staticCompositionLocalOf { LightExtendedColors }
val LocalFarmImageLoader = staticCompositionLocalOf<ImageLoader?> { null }
val LocalAssetResolver = staticCompositionLocalOf<(String) -> String> { { it } }

private val LightColorScheme = lightColorScheme(
    primary = AppleEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF4EA),
    onPrimaryContainer = Color(0xFF0E4D31),
    secondary = AppleBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F0FF),
    onSecondaryContainer = Color(0xFF003D80),
    tertiary = AppleIndigo,
    background = AppleLightBackground,
    onBackground = AppleLightLabel,
    surface = AppleLightCard,
    onSurface = AppleLightLabel,
    surfaceVariant = AppleLightTrack,
    onSurfaceVariant = AppleLightSecondaryLabel,
    outline = AppleLightSeparator,
    outlineVariant = AppleLightSeparator,
    error = AppleRed,
    // 补齐 M3 容器色：未定义时会落回基线色板的淡粉紫（#ECE6F0），弹窗、菜单、抽屉都会偏暖。
    surfaceTint = Color.Transparent,
    surfaceBright = AppleLightCard,
    surfaceDim = AppleLightBackground,
    surfaceContainerLowest = AppleLightCard,
    surfaceContainerLow = AppleLightCard,
    surfaceContainer = AppleLightCard,
    surfaceContainerHigh = AppleLightCard,
    surfaceContainerHighest = AppleLightTrack,
    inverseSurface = AppleDarkElevated,
    inverseOnSurface = AppleDarkLabel,
    scrim = Color.Black,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = AppleEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF123D24),
    onPrimaryContainer = Color(0xFFA7F3C1),
    secondary = AppleBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0E2A4A),
    onSecondaryContainer = Color(0xFFCCE3FF),
    tertiary = AppleIndigoDark,
    background = AppleDarkBackground,
    onBackground = AppleDarkLabel,
    surface = AppleDarkCard,
    onSurface = AppleDarkLabel,
    surfaceVariant = AppleDarkElevated,
    onSurfaceVariant = AppleDarkSecondaryLabel,
    outline = AppleDarkSeparator,
    outlineVariant = AppleDarkSeparator,
    error = AppleRedDark,
    surfaceTint = Color.Transparent,
    surfaceBright = AppleDarkElevated,
    surfaceDim = AppleDarkBackground,
    surfaceContainerLowest = AppleDarkBackground,
    surfaceContainerLow = AppleDarkCard,
    surfaceContainer = AppleDarkCard,
    surfaceContainerHigh = AppleDarkCard,
    surfaceContainerHighest = AppleDarkElevated,
    inverseSurface = AppleLightCard,
    inverseOnSurface = AppleLightLabel,
    scrim = Color.Black,
    onError = Color.White,
)

/** 统一圆角：小控件 8/10，卡片 14，弹窗与底部抽屉 20。 */
private val AppleShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

private val AppleTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    ),
)

@Composable
fun FarmBotTheme(
    themeMode: String = StoredConfig.THEME_SYSTEM,
    dynamicColor: Boolean = false,
    customAppearance: CustomAppearanceConfig = CustomAppearanceConfig(),
    imageLoader: ImageLoader? = null,
    assetResolver: (String) -> String = { it },
    content: @Composable () -> Unit,
) {
    val effectiveThemeMode = if (themeMode == StoredConfig.THEME_CUSTOM) {
        customAppearance.baseThemeMode
    } else {
        themeMode
    }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (effectiveThemeMode) {
        StoredConfig.THEME_LIGHT -> false
        StoredConfig.THEME_DARK -> true
        else -> systemDark
    }
    val appearance = resolveEffectiveAppearance(themeMode, customAppearance)

    val context = LocalContext.current
    val customAccent = appearance.customAccent
    val baseScheme = when {
        customAccent == null && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val baseExtended = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val tonedExtended = when (appearance.backgroundTone) {
        BackgroundTone.STANDARD -> baseExtended
        BackgroundTone.WARM_SOFT -> if (darkTheme) {
            baseExtended.copy(
                background = Color(0xFF131417),
                card = Color(0xFF1E2025),
                elevated = Color(0xFF292C33),
                track = Color(0xFF292C33),
                separator = Color(0xFF363A42),
            )
        } else {
            baseExtended.copy(
                background = Color(0xFFF6F4EC),
                card = Color(0xFFFFFDF8),
                elevated = Color(0xFFF1EEE4),
                track = Color(0xFFEAE6DA),
                separator = Color(0xFFE1DCCE),
            )
        }
        BackgroundTone.PURE_AMOLED -> if (darkTheme) {
            baseExtended.copy(
                background = Color(0xFF000000),
                card = Color(0xFF101114),
                elevated = Color(0xFF1A1C21),
                track = Color(0xFF1C1E23),
                separator = Color(0xFF262930),
            )
        } else {
            baseExtended.copy(
                background = Color(0xFFFFFFFF),
                card = Color(0xFFF7F7FA),
                elevated = Color(0xFFEFEFF4),
                track = Color(0xFFE6E6EC),
                separator = Color(0xFFE4E4EA),
            )
        }
    }
    val tonedScheme = if (appearance.backgroundTone != BackgroundTone.STANDARD) {
        baseScheme.copy(
            background = tonedExtended.background,
            surface = tonedExtended.card,
            surfaceVariant = tonedExtended.elevated,
            outlineVariant = tonedExtended.separator,
            surfaceContainerLowest = tonedExtended.card,
            surfaceContainerLow = tonedExtended.card,
            surfaceContainer = tonedExtended.elevated,
            surfaceContainerHigh = tonedExtended.elevated,
            surfaceContainerHighest = tonedExtended.elevated,
        )
    } else {
        baseScheme
    }
    val colorScheme = if (customAccent != null) {
        val primary = customAccent.readableOn(tonedExtended.card)
        val onPrimary = primary.onAccent()
        val container = if (darkTheme) lerp(tonedExtended.card, primary, 0.24f) else lerp(Color.White, primary, 0.14f)
        val onContainer = primary.readableOn(container)
        tonedScheme.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
        )
    } else {
        tonedScheme
    }

    val extended = if (themeMode == StoredConfig.THEME_CUSTOM) {
        tonedExtended.copy(emerald = colorScheme.primary)
    } else {
        tonedExtended
    }
    val resolvedRanchAccent = if (themeMode == StoredConfig.THEME_CUSTOM) {
        customAppearance.resolveRanchAccent(extended.emerald, darkTheme)?.readableOn(extended.card)
    } else null
    val finalAppearance = appearance.copy(ranchAccent = resolvedRanchAccent)
    val shapes = when (appearance.cornerStyle) {
        CornerStyle.ROUNDED -> Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(22.dp),
        )
        CornerStyle.COMPACT -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(6.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(14.dp),
        )
        CornerStyle.STANDARD -> AppleShapes
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAppleColors provides extended,
        LocalAppAppearance provides finalAppearance,
        LocalFarmImageLoader provides imageLoader,
        LocalAssetResolver provides assetResolver,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppleTypography,
            shapes = shapes,
            content = content,
        )
    }
}
