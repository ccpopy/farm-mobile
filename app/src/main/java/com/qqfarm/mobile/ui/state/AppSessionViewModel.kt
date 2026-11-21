package com.qqfarm.mobile.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qqfarm.mobile.FarmBotApplication
import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.BagSystemItem
import com.qqfarm.mobile.data.model.DailyGiftsSummary
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.RUN_LOG_LIMIT
import com.qqfarm.mobile.data.model.ServerVersionInfo
import com.qqfarm.mobile.data.model.StatusSnapshot
import com.qqfarm.mobile.data.model.isVisibleSince
import com.qqfarm.mobile.data.model.retainedRunLogs
import com.qqfarm.mobile.data.model.runLogCutoff
import com.qqfarm.mobile.data.network.ApiException
import com.qqfarm.mobile.data.network.FarmApiClient
import com.qqfarm.mobile.data.prefs.StoredConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

data class SessionUiState(
    val initializing: Boolean = true,
    val config: StoredConfig = StoredConfig(),
    val accounts: List<AccountItem> = emptyList(),
    val accountsLoading: Boolean = false,
    val status: StatusSnapshot? = null,
    val statusLoading: Boolean = false,
    val diamondBalance: Long = 0L,
    val systemItems: List<BagSystemItem> = emptyList(),
    val dailyGifts: DailyGiftsSummary? = null,
    val logs: List<RunLogEntry> = emptyList(),
    val realtimeConnected: Boolean = false,
    val connectionWarning: String = "",
    val showReconfigureServer: Boolean = false,
    /** true：连接页用于“添加服务器”，成功后新建条目并切换过去。 */
    val addingServer: Boolean = false,
) {
    val currentAccount: AccountItem?
        get() = accounts.firstOrNull { it.id == config.currentAccountId }
}

class AppSessionViewModel(application: Application) : AndroidViewModel(application) {

    val container = (application as FarmBotApplication).container
    private val prefs = container.preferences
    val apiClient = container.apiClient
    private val socketManager = container.socketManager
    private val offlineMonitor = container.offlineMonitor
    private val activityNotifier = container.activityNotificationManager

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _snackbars = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbars: SharedFlow<String> = _snackbars.asSharedFlow()

    private var bootstrapJob: Job? = null
    private var logsRefreshJob: Job? = null
    private val tokenUpdates = Mutex()

    init {
        apiClient.onUnauthorized = { serverUrl, token ->
            viewModelScope.launch {
                if (clearAuth(serverUrl, token)) {
                    emitMessage("登录凭证已失效，请重新授权")
                }
            }
        }

        viewModelScope.launch {
            prefs.configFlow.collect { latest ->
                applyConfig(latest)
            }
        }

        viewModelScope.launch {
            socketManager.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(realtimeConnected = connected)
                syncNotificationMonitors()
            }
        }

        viewModelScope.launch {
            socketManager.statusUpdates.collect { (accountId, snapshot) ->
                val activeId = _uiState.value.config.currentAccountId
                if (accountId == activeId) {
                    val prevConnected = _uiState.value.status?.connected == true
                    _uiState.value = _uiState.value.copy(
                        status = snapshot,
                        statusLoading = false,
                        connectionWarning = "",
                    )
                    if (!prevConnected && snapshot.connected) {
                        refreshSecondaryOverview(accountId)
                    }
                }
            }
        }

        viewModelScope.launch {
            socketManager.logSnapshots.collect { (accountId, logs) ->
                val activeId = _uiState.value.config.currentAccountId
                if (accountId == activeId || (accountId == "all" && activeId.isBlank())) {
                    _uiState.value = _uiState.value.copy(logs = logs.retainedRunLogs())
                    refreshRunLogs()
                }
            }
        }

        viewModelScope.launch {
            socketManager.newLogs.collect { entry ->
                activityNotifier.onRealtimeLog(entry)
                val activeId = _uiState.value.config.currentAccountId
                if ((activeId.isBlank() || entry.accountId.isBlank() || entry.accountId == activeId) &&
                    entry.isVisibleSince(runLogCutoff())
                ) {
                    val next = (_uiState.value.logs + entry).retainedRunLogs()
                    _uiState.value = _uiState.value.copy(logs = next)
                }
            }
        }

        viewModelScope.launch {
            socketManager.accountLogs.collect { ev ->
                offlineMonitor.onAccountLog(ev)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                val state = _uiState.value
                val logs = state.logs.retainedRunLogs()
                if (logs.size != state.logs.size) _uiState.value = state.copy(logs = logs)
            }
        }
    }

    private fun applyConfig(latest: StoredConfig) {
        val previous = _uiState.value
        val connectionChanged = previous.initializing || !isActiveConnection(latest)
        if (connectionChanged || previous.config.currentAccountId != latest.currentAccountId) {
            logsRefreshJob?.cancel()
        }
        if (connectionChanged) {
            bootstrapJob?.cancel()
            socketManager.disconnect()
            offlineMonitor.stop()
            activityNotifier.stop()
        }
        apiClient.updateSession(latest.serverUrl, latest.authToken, latest.currentAccountId)
        if (connectionChanged) {
            _uiState.value = SessionUiState(initializing = false, config = latest)
            if (latest.isAuthenticated) {
                bootstrapJob = viewModelScope.launch { bootstrapAuthenticatedSession(latest) }
            }
        } else if (previous.config.currentAccountId != latest.currentAccountId) {
            _uiState.value = previous.copy(
                config = latest,
                status = null,
                statusLoading = false,
                diamondBalance = 0L,
                systemItems = emptyList(),
                dailyGifts = null,
                logs = emptyList(),
            )
            socketManager.subscribeAccount(latest.currentAccountId)
            refreshOverview()
        } else {
            _uiState.value = previous.copy(config = latest)
        }
        syncNotificationMonitors()
    }

    private fun isActiveConnection(config: StoredConfig): Boolean {
        val current = _uiState.value.config
        return current.activeProfileId == config.activeProfileId && current.serverUrl == config.serverUrl && current.authToken == config.authToken
    }

    private suspend fun clearAuth(serverUrl: String, token: String): Boolean = tokenUpdates.withLock {
        prefs.clearAuth(serverUrl, token)
    }

    private fun syncNotificationMonitors() {
        val state = _uiState.value
        val cfg = state.config
        offlineMonitor.updateSubscription(
            enabled = cfg.offlineAlertsEnabled && cfg.isAuthenticated,
            accountIds = state.accounts.map { it.id },
        )
        activityNotifier.updateConfig(
            nextConfig = cfg,
            accounts = state.accounts,
            isSocketConnected = state.realtimeConnected,
        )
    }

    fun emitMessage(msg: String) {
        if (msg.isNotBlank()) {
            _snackbars.tryEmit(msg)
        }
    }

    fun openReconfigureServer() {
        _uiState.value = _uiState.value.copy(showReconfigureServer = true, addingServer = false)
    }

    fun openAddServer() {
        _uiState.value = _uiState.value.copy(showReconfigureServer = true, addingServer = true)
    }

    fun closeReconfigureServer() {
        _uiState.value = _uiState.value.copy(showReconfigureServer = false, addingServer = false)
    }

    /**
     * 是否作为新服务器保存：明确在“添加服务器”，或当前已退出授权且填了另一个地址
     * （避免退出后登录别的服务器时覆盖掉原来那台）。
     */
    private fun saveAsNewProfile(normalizedUrl: String): Boolean {
        val state = _uiState.value
        return state.addingServer || (!state.config.isAuthenticated && normalizedUrl != state.config.serverUrl)
    }

    fun switchServer(profileId: String) {
        val current = _uiState.value.config
        if (profileId == current.activeProfileId) return
        val target = current.serverProfiles.firstOrNull { it.id == profileId } ?: return
        viewModelScope.launch {
            if (prefs.switchServerProfile(profileId)) {
                emitMessage(
                    if (target.isAuthenticated) "已切换到「${target.displayName}」" else "「${target.displayName}」需要重新授权"
                )
            }
        }
    }

    fun renameServer(profileId: String, name: String) {
        viewModelScope.launch { prefs.renameServerProfile(profileId, name) }
    }

    fun removeServer(profileId: String) {
        val target = _uiState.value.config.serverProfiles.firstOrNull { it.id == profileId } ?: return
        viewModelScope.launch {
            if (prefs.removeServerProfile(profileId)) emitMessage("已移除「${target.displayName}」")
        }
    }

    suspend fun testServerConnection(rawUrl: String): Result<ServerVersionInfo> {
        val normalized = FarmApiClient.normalizeBaseUrl(rawUrl)
            ?: return Result.failure(IllegalArgumentException("请输入有效的服务器地址，例如 https://farm.example.com"))
        return runCatching { apiClient.fetchGameVersion(normalized) }
    }

    suspend fun connectWithApiToken(rawUrl: String, rawToken: String): Result<Unit> {
        val normalized = FarmApiClient.normalizeBaseUrl(rawUrl)
            ?: return Result.failure(IllegalArgumentException("请输入有效的服务器地址"))
        val token = rawToken.trim()
        if (token.isEmpty()) {
            return Result.failure(IllegalArgumentException("请输入 API Token"))
        }
        val from = _uiState.value.config
        val asNew = saveAsNewProfile(normalized)
        return try {
            val valid = apiClient.validateToken(normalized, token)
            if (!valid) {
                return Result.failure(IllegalArgumentException("Token 验证失败"))
            }
            val username = try { apiClient.fetchUserMe(normalized, token) } catch (_: ApiException) { "" }
            val saved = prefs.saveConnection(
                serverUrl = normalized,
                token = token,
                authMode = StoredConfig.AUTH_MODE_API_TOKEN,
                username = username,
                from = from,
                asNewProfile = asNew,
            )
            if (!saved) return Result.failure(IllegalStateException("当前连接已切换，请重新操作"))
            _uiState.value = _uiState.value.copy(showReconfigureServer = false, addingServer = false, connectionWarning = "")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun connectWithPassword(
        rawUrl: String,
        username: String,
        password: String,
    ): Result<String> {
        val normalized = FarmApiClient.normalizeBaseUrl(rawUrl)
            ?: return Result.failure(IllegalArgumentException("请输入有效的服务器地址"))
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入管理员用户名和密码"))
        }
        val from = _uiState.value.config
        val asNew = saveAsNewProfile(normalized)
        val loginRes = apiClient.login(normalized, username.trim(), password)
        if (!loginRes.ok || loginRes.token.isBlank()) {
            val detail = when (loginRes.errorType) {
                "locked" -> {
                    val mins = ((loginRes.remainingMs + 59999L) / 60000L).coerceAtLeast(1L)
                    "${loginRes.error}（约 ${mins} 分钟后解锁）"
                }
                "rate_limit" -> {
                    val secs = ((loginRes.remainingMs + 999L) / 1000L).coerceAtLeast(1L)
                    "${loginRes.error}（${secs} 秒后可重试）"
                }
                else -> loginRes.error.ifBlank { "登录失败" }
            }
            return Result.failure(IllegalArgumentException(detail))
        }

        val note = if (loginRes.mustChangePassword) {
            "登录成功，建议在网页端修改默认密码"
        } else ""

        val saved = prefs.saveConnection(
            serverUrl = normalized,
            token = loginRes.token,
            authMode = StoredConfig.AUTH_MODE_PASSWORD,
            username = loginRes.username.ifBlank { username.trim() },
            from = from,
            asNewProfile = asNew,
        )
        if (!saved) return Result.failure(IllegalStateException("当前连接已切换，请重新操作"))
        _uiState.value = _uiState.value.copy(showReconfigureServer = false, addingServer = false, connectionWarning = "")
        return Result.success(note)
    }

    private suspend fun bootstrapAuthenticatedSession(cfg: StoredConfig) {
        try {
            if (!apiClient.validateToken(cfg.serverUrl, cfg.authToken)) {
                throw ApiException("登录凭证无效", httpCode = 401)
            }
        } catch (e: ApiException) {
            if (e.httpCode == 401) {
                if (clearAuth(cfg.serverUrl, cfg.authToken)) {
                    emitMessage("登录凭证已过期，请重新授权")
                }
                return
            }
            _uiState.value = _uiState.value.copy(connectionWarning = e.message)
        }

        refreshAccounts()
        val activeAcc = _uiState.value.config.currentAccountId
        socketManager.connect(cfg.serverUrl, cfg.authToken, activeAcc)
        if (activeAcc.isNotBlank()) {
            refreshOverview()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            val config = _uiState.value.config
            if (config.authMode == StoredConfig.AUTH_MODE_PASSWORD) {
                apiClient.logout()
            }
            if (clearAuth(config.serverUrl, config.authToken)) {
                emitMessage("已退出当前服务器授权")
            }
        }
    }

    fun refreshAccounts() {
        viewModelScope.launch {
            val config = _uiState.value.config
            _uiState.value = _uiState.value.copy(accountsLoading = true)
            try {
                val list = apiClient.fetchAccounts()
                if (!isActiveConnection(config)) return@launch
                val currentId = _uiState.value.config.currentAccountId
                val resolvedId = when {
                    list.isEmpty() -> ""
                    list.any { it.id == currentId } -> currentId
                    else -> list.first().id
                }
                _uiState.value = _uiState.value.copy(
                    accounts = list,
                    accountsLoading = false,
                    connectionWarning = "",
                )
                syncNotificationMonitors()
                if (resolvedId != currentId) {
                    prefs.updateCurrentAccountId(resolvedId, config)
                }
            } catch (e: Exception) {
                if (isActiveConnection(config)) {
                    _uiState.value = _uiState.value.copy(
                        accountsLoading = false,
                        connectionWarning = e.message ?: "获取账号列表失败",
                    )
                }
            }
        }
    }

    fun selectAccount(accountId: String) {
        val connection = _uiState.value.config
        viewModelScope.launch {
            prefs.updateCurrentAccountId(accountId, connection)
        }
    }

    fun startAccount(accountId: String) {
        viewModelScope.launch {
            runCatching { apiClient.startAccount(accountId) }
                .onSuccess {
                    emitMessage("已发送启动指令")
                    refreshAccounts()
                    if (accountId == _uiState.value.config.currentAccountId) {
                        refreshOverview()
                    }
                }
                .onFailure { emitMessage(it.message ?: "启动账号失败") }
        }
    }

    fun stopAccount(accountId: String) {
        viewModelScope.launch {
            runCatching { apiClient.stopAccount(accountId) }
                .onSuccess {
                    emitMessage("已停止账号")
                    refreshAccounts()
                    if (accountId == _uiState.value.config.currentAccountId) {
                        refreshOverview()
                    }
                }
                .onFailure { emitMessage(it.message ?: "停止账号失败") }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            runCatching { apiClient.deleteAccount(accountId) }
                .onSuccess {
                    emitMessage("已删除账号")
                    refreshAccounts()
                }
                .onFailure { emitMessage(it.message ?: "删除账号失败") }
        }
    }

    fun refreshOverview() {
        val config = _uiState.value.config
        val accountId = config.currentAccountId
        if (accountId.isBlank()) return
        refreshRunLogs()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusLoading = true)
            runCatching { apiClient.fetchStatus(accountId) }
                .onSuccess { snap ->
                    if (isActiveConnection(config) && _uiState.value.config.currentAccountId == accountId) {
                        _uiState.value = _uiState.value.copy(
                            status = snap,
                            statusLoading = false,
                            connectionWarning = "",
                        )
                        if (snap.connected) {
                            refreshSecondaryOverview(accountId)
                        }
                    }
                }
                .onFailure { err ->
                    if (isActiveConnection(config) && _uiState.value.config.currentAccountId == accountId) {
                        _uiState.value = _uiState.value.copy(
                            statusLoading = false,
                            connectionWarning = err.message ?: "状态获取失败",
                        )
                    }
                }
        }
    }

    private fun refreshRunLogs() {
        val before = _uiState.value
        val config = before.config
        val accountId = config.currentAccountId
        if (accountId.isBlank()) return
        logsRefreshJob?.cancel()
        logsRefreshJob = viewModelScope.launch {
            val fetched = runCatching {
                apiClient.fetchLogs(
                    accountId = accountId,
                    limit = RUN_LOG_LIMIT,
                    timeFrom = Instant.ofEpochMilli(runLogCutoff()).toString(),
                )
            }.getOrNull() ?: return@launch
            val state = _uiState.value
            if (isActiveConnection(config) && state.config.currentAccountId == accountId) {
                val latestBeforeFetch = before.logs.lastOrNull()?.ts ?: 0L
                val receivedWhileFetching = state.logs.filter { it.ts > latestBeforeFetch }
                val combined = (fetched + receivedWhileFetching).distinct().sortedBy { it.ts }.retainedRunLogs()
                _uiState.value = state.copy(logs = combined)
            }
        }
    }

    private fun refreshSecondaryOverview(accountId: String) {
        val config = _uiState.value.config
        viewModelScope.launch {
            supervisorScope {
                val diamondJob = async { runCatching { apiClient.fetchDiamond(accountId) } }
                val bagJob = async { runCatching { apiClient.fetchBag(accountId) } }
                val giftJob = async { runCatching { apiClient.fetchDailyGifts(accountId) } }

                val d = diamondJob.await().getOrNull()
                val b = bagJob.await().getOrNull()
                val g = giftJob.await().getOrNull()

                if (isActiveConnection(config) && _uiState.value.config.currentAccountId == accountId) {
                    _uiState.value = _uiState.value.copy(
                        diamondBalance = d ?: _uiState.value.diamondBalance,
                        systemItems = b?.systemItems ?: _uiState.value.systemItems,
                        dailyGifts = g ?: _uiState.value.dailyGifts,
                    )
                }
            }
        }
    }

    suspend fun clearLogs(accountId: String) {
        val config = _uiState.value.config
        if (accountId.isBlank() || config.currentAccountId != accountId) {
            throw ApiException("请选择需要清理日志的当前账号")
        }
        apiClient.clearLogs(accountId)
        val state = _uiState.value
        if (isActiveConnection(config) && state.config.currentAccountId == accountId) {
            _uiState.value = state.copy(logs = emptyList())
            refreshRunLogs()
        }
        emitMessage("已清空当前账号的运行日志")
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { prefs.updateThemeMode(mode) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefs.updateDynamicColor(enabled) }
    }

    fun setOfflineAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.updateOfflineAlertsEnabled(enabled)
            if (enabled) emitMessage("已开启掉线重登提醒")
        }
    }

    fun setActivityPushEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.updateActivityPushEnabled(enabled) }
    }

    fun setNotifyCollapseEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.updateNotifyCollapseEnabled(enabled) }
    }

    fun setNotifySilentUpdate(enabled: Boolean) {
        viewModelScope.launch { prefs.updateNotifySilentUpdate(enabled) }
    }

    fun setNotifyAllAccounts(enabled: Boolean) {
        viewModelScope.launch { prefs.updateNotifyAllAccounts(enabled) }
    }

    fun toggleNotifyType(key: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.config.enabledNotifyTypes.toMutableSet()
            if (enabled) current.add(key) else current.remove(key)
            prefs.updateEnabledNotifyTypes(current)
        }
    }

    fun applyNotifyTypes(keys: Set<String>) {
        viewModelScope.launch { prefs.updateEnabledNotifyTypes(keys) }
    }

    fun sendTestActivityNotification(): Boolean = activityNotifier.sendTestNotification()

    override fun onCleared() {
        super.onCleared()
        apiClient.onUnauthorized = null
        socketManager.disconnect()
        offlineMonitor.stop()
        activityNotifier.stop()
    }
}
