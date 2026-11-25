package com.qqfarm.mobile.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import okio.Path.Companion.toPath

actual object PasswordVault {
    actual fun seal(password: String, profileId: String): String =
        com.qqfarm.mobile.data.prefs.RememberedPassword.seal(password, profileId)

    actual fun open(sealed: String, profileId: String): String =
        com.qqfarm.mobile.data.prefs.RememberedPassword.open(sealed, profileId)
}

private fun appContext(): Context =
    PlatformGlobals.appContext as? Context
        ?: throw IllegalStateException("Application context not initialized")

actual object NotificationsGate {
    private val manager = com.qqfarm.mobile.data.network.ActivityNotificationManager

    actual fun areNotificationsEnabled(): Boolean =
        runCatching { manager.areNotificationsEnabled(appContext()) }.getOrDefault(false)

    actual fun hasPostPermission(): Boolean =
        runCatching { manager.hasPostPermission(appContext()) }.getOrDefault(false)

    actual fun openSystemSettings() {
        runCatching { manager.openSystemNotificationSettings(appContext()) }
    }

    actual fun ensureEnabled(onResult: (Boolean) -> Unit) {
        if (areNotificationsEnabled()) {
            onResult(true)
            return
        }
        openSystemSettings()
        onResult(false)
    }
}

actual fun openExternalUrl(url: String): Boolean = runCatching {
    val context = appContext()
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
    true
}.getOrDefault(false)

actual class AppHaptics(private val view: android.view.View) {
    actual fun tick() {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    actual fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.HapticFeedbackConstants.CONFIRM
        } else {
            android.view.HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(constant)
    }

    actual fun longPress() {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }
}

@Composable
actual fun rememberAppHaptics(): AppHaptics {
    val view = androidx.compose.ui.platform.LocalView.current
    return androidx.compose.runtime.remember(view) { AppHaptics(view) }
}

actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    val context = PlatformGlobals.appContext as? Context ?: return null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
actual fun SystemBarAppearanceEffect(darkTheme: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

actual fun createFarmImageLoader(): coil3.ImageLoader {
    val context = PlatformGlobals.appContext as? Context
        ?: throw IllegalStateException("Application context not initialized")
    return coil3.ImageLoader.Builder(context)
        .memoryCache {
            coil3.memory.MemoryCache.Builder()
                .maxSizePercent(context, 0.20)
                .build()
        }
        .diskCache {
            coil3.disk.DiskCache.Builder()
                .directory(java.io.File(context.cacheDir, "image_cache").absolutePath.toPath())
                .maxSizeBytes(64L * 1024 * 1024)
                .build()
        }
        .build()
}

actual fun supportsDynamicColor(): Boolean {
    val context = PlatformGlobals.appContext as? Context ?: return false
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

actual fun monotonicMillis(): Long = android.os.SystemClock.elapsedRealtime()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}
