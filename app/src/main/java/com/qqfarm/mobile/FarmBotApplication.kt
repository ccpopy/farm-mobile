package com.qqfarm.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.qqfarm.mobile.data.network.ActivityNotificationManager
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.data.network.OfflineEventMonitor
import com.qqfarm.mobile.data.network.RealtimeSocketManager
import com.qqfarm.mobile.data.prefs.AppPreferences
import java.io.File

class AppContainer(application: Application) {
    val preferences = AppPreferences(application)
    val apiClient = FarmApiClient()
    val imageLoader: ImageLoader = ImageLoader.Builder(application)
        .okHttpClient(apiClient.imageHttpClient)
        .memoryCache {
            MemoryCache.Builder(application)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(application.cacheDir, "image_cache"))
                .maxSizeBytes(64L * 1024 * 1024)
                .build()
        }
        .allowRgb565(true)
        .crossfade(true)
        .build()
    val socketManager = RealtimeSocketManager(apiClient.okHttpClient)
    val offlineMonitor = OfflineEventMonitor(application, apiClient)
    val activityNotificationManager = ActivityNotificationManager(application, apiClient)
}

class FarmBotApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val reloginChannel = NotificationChannel(
            OfflineEventMonitor.CHANNEL_ID,
            getString(R.string.relogin_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.relogin_channel_desc)
        }
        val activityChannel = NotificationChannel(
            ActivityNotificationManager.CHANNEL_ID,
            getString(R.string.activity_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.activity_channel_desc)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannels(listOf(reloginChannel, activityChannel))
    }
}
