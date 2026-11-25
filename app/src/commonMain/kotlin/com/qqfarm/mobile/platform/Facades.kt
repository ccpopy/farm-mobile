package com.qqfarm.mobile.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** 平台全局上下文：androidMain 在 AppContainer 创建时注入 Application；iOS 为空 */
object PlatformGlobals {
    var appContext: Any? = null
}

/** 密码保险箱（Android: AndroidKeyStore；iOS: NSUserDefaults 混淆存储） */
expect object PasswordVault {
    fun seal(password: String, profileId: String): String
    fun open(sealed: String, profileId: String): String
}

/** 系统通知能力门面 */
expect object NotificationsGate {
    fun areNotificationsEnabled(): Boolean
    fun hasPostPermission(): Boolean
    fun openSystemSettings()
    /** 请求通知权限；返回当前是否已启用 */
    fun ensureEnabled(onResult: (enabled: Boolean) -> Unit)
}

/** 系统浏览器打开外部链接，返回是否成功 */
expect fun openExternalUrl(url: String): Boolean

/** 文件大小人类可读格式化（对齐 android Formatter.formatShortFileSize） */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.size - 1) {
        value /= 1024.0
        unit += 1
    }
    return if (value >= 100 || unit == 0) "${value.toInt()}${units[unit]}" else fmtDec(value, 1) + units[unit]
}

/** Material You 动态取色（Android 12+；iOS 返回 null 走默认配色） */
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** 是否支持动态取色（Android 12+；iOS false） */
expect fun supportsDynamicColor(): Boolean

/** 系统状态栏外观跟随主题（Android: WindowInsetsController；iOS: 空实现） */
@Composable
expect fun SystemBarAppearanceEffect(darkTheme: Boolean)

/** 平台图片加载器（coil3：Android 磁盘缓存 + 内存缓存；iOS 内存缓存） */
expect fun createFarmImageLoader(): coil3.ImageLoader

/** 触感反馈门面 */
expect class AppHaptics {
    fun tick()
    fun confirm()
    fun longPress()
}

/** 触感工厂：Android 走 View.performHapticFeedback；iOS 走 UIImpactFeedbackGenerator */
@Composable
expect fun rememberAppHaptics(): AppHaptics

/** 单调时钟毫秒（Android: SystemClock.elapsedRealtime；iOS: 单调钟） */
expect fun monotonicMillis(): Long

/** 系统返回键处理（Android: BackHandler；iOS: 空实现） */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** HSV 颜色转换（替代 android.graphics.Color.colorToHSV/HSVToColor） */
fun hsvOf(argb: Int): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }.let { if (it < 0) it + 360f else it }
    val s = if (max == 0f) 0f else delta / max
    return floatArrayOf(h, s, max)
}

fun colorFromHsv(hue: Float, sat: Float, value: Float): Int {
    val h = ((hue % 360f) + 360f) % 360f
    val c = value * sat
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2f - 1))
    val m = value - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun toByte(v: Float) = ((v + m) * 255f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (toByte(r) shl 16) or (toByte(g) shl 8) or toByte(b)
}
