package com.qqfarm.mobile.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qqfarm.mobile.data.model.ActivityNotifyType
import com.qqfarm.mobile.data.network.DEFAULT_UPDATE_ACCELERATOR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qq_farm_mobile_prefs")

enum class UpdateDownloadMode(val label: String) {
    DIRECT("直连"),
    ACCELERATOR("下载加速"),
    PROXY("自定义代理"),
}

data class UpdateDownloadSettings(
    val mode: UpdateDownloadMode = UpdateDownloadMode.ACCELERATOR,
    val acceleratorUrl: String = DEFAULT_UPDATE_ACCELERATOR,
    val proxyUrl: String = "",
) {
    val activeProxyUrl: String? get() = proxyUrl.takeIf { mode == UpdateDownloadMode.PROXY }

    fun forRelease(canAccelerate: Boolean): UpdateDownloadSettings =
        if (mode == UpdateDownloadMode.ACCELERATOR && !canAccelerate) copy(mode = UpdateDownloadMode.DIRECT) else this
}

data class StoredConfig(
    val serverUrl: String = "",
    val authToken: String = "",
    val authMode: String = AUTH_MODE_PASSWORD,
    val username: String = "",
    val currentAccountId: String = "",
    val themeMode: String = THEME_SYSTEM,
    val dynamicColor: Boolean = false,
    val offlineAlertsEnabled: Boolean = false,
    val activityPushEnabled: Boolean = false,
    val notifyCollapseEnabled: Boolean = true,
    val notifySilentUpdate: Boolean = true,
    val notifyAllAccounts: Boolean = true,
    val enabledNotifyTypes: Set<String> = ActivityNotifyType.defaultKeys,
    val updateDownload: UpdateDownloadSettings = UpdateDownloadSettings(),
) {
    val isAuthenticated: Boolean
        get() = serverUrl.isNotBlank() && authToken.isNotBlank()

    companion object {
        const val AUTH_MODE_API_TOKEN = "api_token"
        const val AUTH_MODE_PASSWORD = "password"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
}

class AppPreferences(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val AUTH_MODE = stringPreferencesKey("auth_mode")
        val USERNAME = stringPreferencesKey("username")
        val CURRENT_ACCOUNT_ID = stringPreferencesKey("current_account_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val OFFLINE_ALERTS_ENABLED = booleanPreferencesKey("offline_alerts_enabled")
        val ACTIVITY_PUSH_ENABLED = booleanPreferencesKey("activity_push_enabled")
        val NOTIFY_COLLAPSE_ENABLED = booleanPreferencesKey("notify_collapse_enabled")
        val NOTIFY_SILENT_UPDATE = booleanPreferencesKey("notify_silent_update")
        val NOTIFY_ALL_ACCOUNTS = booleanPreferencesKey("notify_all_accounts")
        val ENABLED_NOTIFY_TYPES = stringSetPreferencesKey("enabled_notify_types")
        val USE_UPDATE_ACCELERATION = booleanPreferencesKey("use_update_acceleration")
        val UPDATE_DOWNLOAD_MODE = stringPreferencesKey("update_download_mode")
        val UPDATE_ACCELERATOR_URL = stringPreferencesKey("update_accelerator_url")
        val UPDATE_PROXY_URL = stringPreferencesKey("update_proxy_url")
    }

    val configFlow: Flow<StoredConfig> = context.dataStore.data.map { prefs ->
        val downloadMode = UpdateDownloadMode.entries.firstOrNull { it.name == prefs[Keys.UPDATE_DOWNLOAD_MODE] }
            ?: if (prefs[Keys.USE_UPDATE_ACCELERATION] == false) UpdateDownloadMode.DIRECT else UpdateDownloadMode.ACCELERATOR
        StoredConfig(
            serverUrl = prefs[Keys.SERVER_URL].orEmpty(),
            authToken = prefs[Keys.AUTH_TOKEN].orEmpty(),
            authMode = prefs[Keys.AUTH_MODE] ?: StoredConfig.AUTH_MODE_PASSWORD,
            username = prefs[Keys.USERNAME].orEmpty(),
            currentAccountId = prefs[Keys.CURRENT_ACCOUNT_ID].orEmpty(),
            themeMode = prefs[Keys.THEME_MODE] ?: StoredConfig.THEME_SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            offlineAlertsEnabled = prefs[Keys.OFFLINE_ALERTS_ENABLED] ?: false,
            activityPushEnabled = prefs[Keys.ACTIVITY_PUSH_ENABLED] ?: false,
            notifyCollapseEnabled = prefs[Keys.NOTIFY_COLLAPSE_ENABLED] ?: true,
            notifySilentUpdate = prefs[Keys.NOTIFY_SILENT_UPDATE] ?: true,
            notifyAllAccounts = prefs[Keys.NOTIFY_ALL_ACCOUNTS] ?: true,
            enabledNotifyTypes = prefs[Keys.ENABLED_NOTIFY_TYPES] ?: ActivityNotifyType.defaultKeys,
            updateDownload = UpdateDownloadSettings(
                mode = downloadMode,
                acceleratorUrl = prefs[Keys.UPDATE_ACCELERATOR_URL] ?: DEFAULT_UPDATE_ACCELERATOR,
                proxyUrl = prefs[Keys.UPDATE_PROXY_URL].orEmpty(),
            ),
        )
    }

    suspend fun saveConnection(
        serverUrl: String,
        token: String,
        authMode: String,
        username: String,
    ) {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.SERVER_URL] != serverUrl) {
                prefs.remove(Keys.CURRENT_ACCOUNT_ID)
            }
            prefs[Keys.SERVER_URL] = serverUrl
            prefs[Keys.AUTH_TOKEN] = token
            prefs[Keys.AUTH_MODE] = authMode
            prefs[Keys.USERNAME] = username
        }
    }

    suspend fun updateServerUrl(serverUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = serverUrl
        }
    }

    suspend fun updateCurrentAccountId(accountId: String, serverUrl: String) {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.SERVER_URL] == serverUrl) {
                prefs[Keys.CURRENT_ACCOUNT_ID] = accountId
            }
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode
        }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun updateDownloadSettings(settings: UpdateDownloadSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UPDATE_DOWNLOAD_MODE] = settings.mode.name
            prefs[Keys.UPDATE_ACCELERATOR_URL] = settings.acceleratorUrl
            prefs[Keys.UPDATE_PROXY_URL] = settings.proxyUrl
        }
    }

    suspend fun updateOfflineAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OFFLINE_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun updateActivityPushEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVITY_PUSH_ENABLED] = enabled
        }
    }

    suspend fun updateNotifyCollapseEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_COLLAPSE_ENABLED] = enabled
        }
    }

    suspend fun updateNotifySilentUpdate(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_SILENT_UPDATE] = enabled
        }
    }

    suspend fun updateNotifyAllAccounts(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_ALL_ACCOUNTS] = enabled
        }
    }

    suspend fun updateEnabledNotifyTypes(types: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLED_NOTIFY_TYPES] = types
        }
    }

    suspend fun clearAuth(serverUrl: String, token: String): Boolean {
        var cleared = false
        context.dataStore.edit { prefs ->
            if (prefs[Keys.SERVER_URL] == serverUrl && prefs[Keys.AUTH_TOKEN] == token) {
                prefs.remove(Keys.AUTH_TOKEN)
                prefs.remove(Keys.USERNAME)
                prefs.remove(Keys.CURRENT_ACCOUNT_ID)
                cleared = true
            }
        }
        return cleared
    }
}
