package com.qqfarm.mobile.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual object PasswordVault {
    // v1：NSUserDefaults + 与 profileId 绑定的异或混淆。TODO: 升级为 iOS Keychain
    private fun key(profileId: String) = "farm.sealed.pw.$profileId"

    private fun xorBytes(data: String, keySeed: String): String {
        val ks = keySeed.toCharArray()
        val sb = StringBuilder(data.length)
        for (i in data.indices) {
            sb.append(((data[i].code) xor (ks[i % ks.size].code)).toChar())
        }
        return sb.toString()
    }

    actual fun seal(password: String, profileId: String): String {
        val raw = xorBytes(password, profileId + "farm.salt")
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setObject(raw, key(profileId))
        return raw
    }

    actual fun open(sealed: String, profileId: String): String {
        if (sealed.isEmpty()) return ""
        val defaults = NSUserDefaults.standardUserDefaults
        val stored = defaults.stringForKey(key(profileId)) ?: ""
        if (stored.isEmpty()) return ""
        return runCatching { xorBytes(stored, profileId + "farm.salt") }.getOrDefault("")
    }
}

actual object NotificationsGate {
    actual fun areNotificationsEnabled(): Boolean = true
    actual fun hasPostPermission(): Boolean = true
    actual fun openSystemSettings() {}
    actual fun ensureEnabled(onResult: (Boolean) -> Unit) = onResult(true)
}

@OptIn(ExperimentalForeignApi::class)
actual fun openExternalUrl(url: String): Boolean = runCatching {
    val app = platform.UIKit.UIApplication.sharedApplication
    val nsUrl = platform.Foundation.NSURL.URLWithString(url) ?: return false
    app.openURL(nsUrl)
    true
}.getOrDefault(false)

actual class AppHaptics(private val generator: UIImpactFeedbackGenerator) {
    actual fun tick() {
        generator.impactOccurred()
    }

    actual fun confirm() {
        generator.impactOccurred()
    }

    actual fun longPress() {
        generator.impactOccurred()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberAppHaptics(): AppHaptics = remember {
    AppHaptics(UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight))
}

actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

@Composable
actual fun SystemBarAppearanceEffect(darkTheme: Boolean) {
    // iOS 状态栏外观由 SwiftUI 宿主控制
}

actual fun createFarmImageLoader(): coil3.ImageLoader =
    coil3.ImageLoader.Builder(coil3.PlatformContext.INSTANCE)
        .memoryCache {
            coil3.memory.MemoryCache.Builder()
                .maxSizePercent(coil3.PlatformContext.INSTANCE, 0.20)
                .build()
        }
        .components {
            add(coil3.network.ktor3.KtorNetworkFetcherFactory())
        }
        .build()

actual fun supportsDynamicColor(): Boolean = false

actual fun monotonicMillis(): Long = wallClockMillis()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS 无系统返回键；宿主可自行映射手势
}
