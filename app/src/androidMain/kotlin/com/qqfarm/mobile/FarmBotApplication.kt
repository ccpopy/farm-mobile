package com.qqfarm.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.qqfarm.mobile.data.network.ActivityNotificationManager
import com.qqfarm.mobile.data.network.OfflineEventMonitor
import com.qqfarm.mobile.platform.AppContainer
import com.qqfarm.mobile.platform.PlatformGlobals

class FarmBotApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PlatformGlobals.appContext = this
        container = AppContainer()
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
