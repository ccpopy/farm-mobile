package com.qqfarm.mobile.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.qqfarm.mobile.data.model.ActivityNotifyType
import com.qqfarm.mobile.data.network.DEFAULT_UPDATE_ACCELERATOR
import com.qqfarm.mobile.ui.theme.CustomAppearanceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import com.qqfarm.mobile.data.network.json.JSONArray
import com.qqfarm.mobile.data.network.json.JSONObject

private const val LEGACY_PROFILE_ID = "default"

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

/** 已保存的远程服务器。当前连接仍以 StoredConfig 顶层字段为准，切换时整体替换。 */
data class ServerProfile(
    val id: String,
    val name: String = "",
    val serverUrl: String,
    val authToken: String = "",
    val authMode: String = StoredConfig.AUTH_MODE_PASSWORD,
    val username: String = "",
    val sealedPassword: String = "",
    val currentAccountId: String = "",
) {
    val isAuthenticated: Boolean get() = serverUrl.isNotBlank() && authToken.isNotBlank()

    /** 未命名时显示主机名（含端口），便于区分多台服务器。 */
    val displayName: String
        get() = name.ifBlank {
            serverUrl.substringAfter("://").substringBefore('/').ifBlank { serverUrl }
        }
}

data class StoredConfig(
    val serverUrl: String = "",
    val authToken: String = "",
    val authMode: String = AUTH_MODE_PASSWORD,
    val username: String = "",
    val savedPassword: String = "",
    val currentAccountId: String = "",
    val themeMode: String = THEME_SYSTEM,
    val dynamicColor: Boolean = false,
    val customAppearance: CustomAppearanceConfig = CustomAppearanceConfig(),
    val offlineAlertsEnabled: Boolean = false,
    val activityPushEnabled: Boolean = false,
    val notifyCollapseEnabled: Boolean = true,
    val notifySilentUpdate: Boolean = true,
    val notifyAllAccounts: Boolean = true,
    val enabledNotifyTypes: Set<String> = ActivityNotifyType.defaultKeys,
    val updateDownload: UpdateDownloadSettings = UpdateDownloadSettings(),
    val serverProfiles: List<ServerProfile> = emptyList(),
    val activeProfileId: String = "",
    val githubToken: String = "",
) {
    val activeProfile: ServerProfile?
        get() = serverProfiles.firstOrNull { it.id == activeProfileId }

    val isAuthenticated: Boolean
        get() = serverUrl.isNotBlank() && authToken.isNotBlank()

    companion object {
        const val AUTH_MODE_API_TOKEN = "api_token"
        const val AUTH_MODE_PASSWORD = "password"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_CUSTOM = "custom"
    }
}

class AppPreferences(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val AUTH_MODE = stringPreferencesKey("auth_mode")
        val USERNAME = stringPreferencesKey("username")
        val CURRENT_ACCOUNT_ID = stringPreferencesKey("current_account_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val CUSTOM_APPEARANCE_JSON = stringPreferencesKey("custom_appearance_json")
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
        val SERVER_PROFILES = stringPreferencesKey("server_profiles")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val GITHUB_TOKEN = stringPreferencesKey("update_github_token")
    }

    private fun readProfiles(prefs: Preferences): List<ServerProfile> {
        val raw = prefs[Keys.SERVER_PROFILES].orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val url = o.optString("serverUrl")
                    val id = o.optString("id")
                    if (url.isBlank() || id.isBlank()) continue
                    add(
                        ServerProfile(
                            id = id,
                            name = o.optString("name"),
                            serverUrl = url,
                            authToken = o.optString("authToken"),
                            authMode = o.optString("authMode").ifBlank { StoredConfig.AUTH_MODE_PASSWORD },
                            username = o.optString("username"),
                            sealedPassword = o.optString("sealedPassword"),
                            currentAccountId = o.optString("currentAccountId"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeProfiles(prefs: MutablePreferences, profiles: List<ServerProfile>) {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("serverUrl", p.serverUrl)
                    .put("authToken", p.authToken)
                    .put("authMode", p.authMode)
                    .put("username", p.username)
                    .put("sealedPassword", p.sealedPassword)
                    .put("currentAccountId", p.currentAccountId)
            )
        }
        prefs[Keys.SERVER_PROFILES] = arr.toString()
    }

    /** 读取列表；老版本只有单个连接时，把它当作第一台服务器（首次写入时落盘）。 */
    private fun profilesWithLegacy(prefs: Preferences): Pair<List<ServerProfile>, String> {
        val stored = readProfiles(prefs)
        val activeId = prefs[Keys.ACTIVE_PROFILE_ID] ?: LEGACY_PROFILE_ID
        val url = prefs[Keys.SERVER_URL].orEmpty()
        if (stored.any { it.id == activeId }) return stored to activeId
        if (url.isBlank()) return stored to ""
        val legacy = ServerProfile(
            id = activeId,
            serverUrl = url,
            authToken = prefs[Keys.AUTH_TOKEN].orEmpty(),
            authMode = prefs[Keys.AUTH_MODE] ?: StoredConfig.AUTH_MODE_PASSWORD,
            username = prefs[Keys.USERNAME].orEmpty(),
            currentAccountId = prefs[Keys.CURRENT_ACCOUNT_ID].orEmpty(),
        )
        return (stored + legacy) to legacy.id
    }

    private fun applyProfile(prefs: MutablePreferences, profile: ServerProfile) {
        prefs[Keys.SERVER_URL] = profile.serverUrl
        prefs[Keys.AUTH_MODE] = profile.authMode
        if (profile.authToken.isBlank()) prefs.remove(Keys.AUTH_TOKEN) else prefs[Keys.AUTH_TOKEN] = profile.authToken
        if (profile.username.isBlank()) prefs.remove(Keys.USERNAME) else prefs[Keys.USERNAME] = profile.username
        if (!profile.isAuthenticated || profile.currentAccountId.isBlank()) {
            prefs.remove(Keys.CURRENT_ACCOUNT_ID)
        } else {
            prefs[Keys.CURRENT_ACCOUNT_ID] = profile.currentAccountId
        }
        prefs[Keys.ACTIVE_PROFILE_ID] = profile.id
    }

    val configFlow: Flow<StoredConfig> = dataStore.data.map { prefs ->
        val downloadMode = UpdateDownloadMode.entries.firstOrNull { it.name == prefs[Keys.UPDATE_DOWNLOAD_MODE] }
            ?: if (prefs[Keys.USE_UPDATE_ACCELERATION] == false) UpdateDownloadMode.DIRECT else UpdateDownloadMode.ACCELERATOR
        val (profiles, activeProfileId) = profilesWithLegacy(prefs)
        val active = profiles.firstOrNull { it.id == activeProfileId }
        StoredConfig(
            serverUrl = prefs[Keys.SERVER_URL].orEmpty(),
            authToken = prefs[Keys.AUTH_TOKEN].orEmpty(),
            authMode = prefs[Keys.AUTH_MODE] ?: StoredConfig.AUTH_MODE_PASSWORD,
            username = prefs[Keys.USERNAME].orEmpty(),
            savedPassword = active?.let { com.qqfarm.mobile.platform.PasswordVault.open(it.sealedPassword, it.id) }.orEmpty(),
            currentAccountId = prefs[Keys.CURRENT_ACCOUNT_ID].orEmpty(),
            themeMode = prefs[Keys.THEME_MODE] ?: StoredConfig.THEME_SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            customAppearance = CustomAppearanceConfig.fromJsonString(prefs[Keys.CUSTOM_APPEARANCE_JSON].orEmpty()),
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
            serverProfiles = profiles,
            activeProfileId = activeProfileId,
            githubToken = prefs[Keys.GITHUB_TOKEN].orEmpty(),
        )
    }.flowOn(Dispatchers.Default)

    /**
     * 保存连接。
     * - asNewProfile = false：修改当前服务器条目（地址变了会清空已选账号）。
     * - asNewProfile = true：添加服务器；同地址同用户已存在时复用该条目，否则新建，并切换过去。
     */
    suspend fun saveConnection(
        serverUrl: String,
        token: String,
        authMode: String,
        username: String,
        from: StoredConfig,
        asNewProfile: Boolean = false,
        passwordToRemember: String? = null,
    ): Boolean {
        var saved = false
        dataStore.edit { prefs ->
            val (profiles, activeId) = profilesWithLegacy(prefs)
            if (activeId != from.activeProfileId || prefs[Keys.AUTH_TOKEN].orEmpty() != from.authToken ||
                prefs[Keys.SERVER_URL].orEmpty() != from.serverUrl) return@edit
            val existing = if (asNewProfile || activeId.isBlank()) {
                profiles.firstOrNull {
                    it.serverUrl == serverUrl && (it.authToken == token || (username.isNotBlank() && it.username == username))
                }
            } else {
                profiles.firstOrNull { it.id == activeId }
            }
            val base = existing ?: ServerProfile(id = com.qqfarm.mobile.platform.randomUuid(), serverUrl = serverUrl)
            val sameToken = base.serverUrl == serverUrl && base.authToken == token
            val sameUser = base.serverUrl == serverUrl && (sameToken || (username.isNotBlank() && base.username == username))
            val sealedPassword = when {
                passwordToRemember == null -> if (sameUser) base.sealedPassword else ""
                passwordToRemember.isEmpty() -> ""
                else -> withContext(Dispatchers.Default) { com.qqfarm.mobile.platform.PasswordVault.seal(passwordToRemember, base.id) }
            }
            val updated = base.copy(
                serverUrl = serverUrl,
                authToken = token,
                authMode = authMode,
                username = if (sameToken && username.isBlank()) base.username else username,
                sealedPassword = sealedPassword,
                currentAccountId = if (sameUser) base.currentAccountId else "",
            )
            val next = if (profiles.any { it.id == updated.id }) {
                profiles.map { if (it.id == updated.id) updated else it }
            } else {
                profiles + updated
            }
            writeProfiles(prefs, next)
            applyProfile(prefs, updated)
            saved = true
        }
        return saved
    }

    suspend fun forgetPassword(profileId: String) {
        dataStore.edit { prefs ->
            val (profiles, activeId) = profilesWithLegacy(prefs)
            if (profileId == activeId && profiles.any { it.id == profileId && it.sealedPassword.isNotEmpty() }) {
                writeProfiles(prefs, profiles.map { if (it.id == profileId) it.copy(sealedPassword = "") else it })
            }
        }
    }

    /** 切换到已保存的服务器；会恢复该服务器上次选中的农场账号。 */
    suspend fun switchServerProfile(profileId: String): Boolean {
        var switched = false
        dataStore.edit { prefs ->
            val (profiles, _) = profilesWithLegacy(prefs)
            val target = profiles.firstOrNull { it.id == profileId }
            if (target != null) {
                writeProfiles(prefs, profiles)
                applyProfile(prefs, target)
                switched = true
            }
        }
        return switched
    }

    suspend fun renameServerProfile(profileId: String, name: String) {
        dataStore.edit { prefs ->
            val (profiles, _) = profilesWithLegacy(prefs)
            writeProfiles(prefs, profiles.map { if (it.id == profileId) it.copy(name = name.trim()) else it })
        }
    }

    /** 删除非当前的服务器条目（当前连接不允许删除，需先切换）。 */
    suspend fun removeServerProfile(profileId: String): Boolean {
        var removed = false
        dataStore.edit { prefs ->
            val (profiles, activeId) = profilesWithLegacy(prefs)
            if (profileId != activeId) {
                val next = profiles.filterNot { it.id == profileId }
                removed = next.size != profiles.size
                writeProfiles(prefs, next)
            }
        }
        return removed
    }

    suspend fun updateGithubToken(token: String) {
        dataStore.edit { prefs ->
            if (token.isBlank()) prefs.remove(Keys.GITHUB_TOKEN) else prefs[Keys.GITHUB_TOKEN] = token
        }
    }

    suspend fun updateCurrentAccountId(accountId: String, connection: StoredConfig) {
        dataStore.edit { prefs ->
            val (profiles, activeId) = profilesWithLegacy(prefs)
            if (activeId == connection.activeProfileId && prefs[Keys.SERVER_URL] == connection.serverUrl &&
                prefs[Keys.AUTH_TOKEN] == connection.authToken) {
                prefs[Keys.CURRENT_ACCOUNT_ID] = accountId
                writeProfiles(prefs, profiles.map { if (it.id == activeId) it.copy(currentAccountId = accountId) else it })
            }
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode
        }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun updateCustomAppearance(appearance: CustomAppearanceConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_APPEARANCE_JSON] = appearance.toJsonString()
        }
    }

    suspend fun updateDownloadSettings(settings: UpdateDownloadSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.UPDATE_DOWNLOAD_MODE] = settings.mode.name
            prefs[Keys.UPDATE_ACCELERATOR_URL] = settings.acceleratorUrl
            prefs[Keys.UPDATE_PROXY_URL] = settings.proxyUrl
        }
    }

    suspend fun updateOfflineAlertsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.OFFLINE_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun updateActivityPushEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ACTIVITY_PUSH_ENABLED] = enabled
        }
    }

    suspend fun updateNotifyCollapseEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_COLLAPSE_ENABLED] = enabled
        }
    }

    suspend fun updateNotifySilentUpdate(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_SILENT_UPDATE] = enabled
        }
    }

    suspend fun updateNotifyAllAccounts(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_ALL_ACCOUNTS] = enabled
        }
    }

    suspend fun updateEnabledNotifyTypes(types: Set<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.ENABLED_NOTIFY_TYPES] = types
        }
    }

    suspend fun clearAuth(serverUrl: String, token: String): Boolean {
        var cleared = false
        dataStore.edit { prefs ->
            if (prefs[Keys.SERVER_URL] == serverUrl && prefs[Keys.AUTH_TOKEN] == token) {
                val (profiles, activeId) = profilesWithLegacy(prefs)
                val signedOut = profiles.first { it.id == activeId }.copy(authToken = "")
                writeProfiles(prefs, profiles.map { if (it.id == activeId) signedOut else it })
                applyProfile(prefs, signedOut)
                cleared = true
            }
        }
        return cleared
    }
}
