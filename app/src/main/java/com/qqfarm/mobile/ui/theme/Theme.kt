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
    primary = AppleEmeraldDark,
    onPrimary = Color(0xFF00391A),
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
    imageLoader: ImageLoader? = null,
    assetResolver: (String) -> String = { it },
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        StoredConfig.THEME_LIGHT -> false
        StoredConfig.THEME_DARK -> true
        else -> systemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors
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
        LocalFarmImageLoader provides imageLoader,
        LocalAssetResolver provides assetResolver,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppleTypography,
            shapes = AppleShapes,
            content = content,
        )
    }
}
