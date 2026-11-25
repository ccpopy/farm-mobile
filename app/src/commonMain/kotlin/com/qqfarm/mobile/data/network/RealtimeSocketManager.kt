package com.qqfarm.mobile.data.network

import kotlin.concurrent.Volatile
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.StatusSnapshot
import com.qqfarm.mobile.data.network.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull

class RealtimeSocketManager {

    /** 连接标识：事件按来源过滤，切换服务器后旧连接事件作废 */
    class SocketHandle internal constructor(internal val socket: KmpSocketIO)

    @Volatile
    private var handle: SocketHandle? = null
    private var activeUrl: String = ""
    private var activeToken: String = ""

    @Volatile
    private var subscribedAccountId: String = ""

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // 切换服务器后，已经排队的旧连接事件也必须按来源过滤。
    private fun <T : Any> MutableSharedFlow<Pair<SocketHandle, T>>.fromCurrentSocket(): Flow<T> =
        mapNotNull { (source, value) -> value.takeIf { source === handle } }

    private val _statusUpdates = MutableSharedFlow<Pair<SocketHandle, Pair<String, StatusSnapshot>>>(extraBufferCapacity = 32)
    val statusUpdates: Flow<Pair<String, StatusSnapshot>> = _statusUpdates.fromCurrentSocket()

    private val _newLogs = MutableSharedFlow<Pair<SocketHandle, RunLogEntry>>(extraBufferCapacity = 64)
    val newLogs: Flow<RunLogEntry> = _newLogs.fromCurrentSocket()

    private val _logSnapshots = MutableSharedFlow<Pair<SocketHandle, Pair<String, List<RunLogEntry>>>>(extraBufferCapacity = 16)
    val logSnapshots: Flow<Pair<String, List<RunLogEntry>>> = _logSnapshots.fromCurrentSocket()

        fun connect(serverUrl: String, token: String, accountId: String) {
        val cleanUrl = FarmApiClient.normalizeBaseUrl(serverUrl).orEmpty()
        val cleanToken = token.trim()
        val cleanAcc = accountId.trim()
        subscribedAccountId = cleanAcc

        if (cleanUrl.isEmpty() || cleanToken.isEmpty()) {
            disconnect()
            return
        }

        val current = handle
        if (current != null && activeUrl == cleanUrl && activeToken == cleanToken) {
            if (current.socket.connected()) {
                current.socket.emit(
                    "subscribe",
                    JSONObject().put("accountId", cleanAcc.ifBlank { "all" }),
                )
            } else {
                current.socket.connect()
            }
            return
        }

        disconnect()
        activeUrl = cleanUrl
        activeToken = cleanToken

        // ws://host/<path>/socket.io/?EIO=4&transport=websocket
        val scheme = cleanUrl.substringBefore("://")
        val rest = cleanUrl.substringAfter("://")
        val authority = rest.substringBefore('/')
        val pathPrefix = rest.substringAfter('/', missingDelimiterValue = "")
        val wsUrl = when {
            pathPrefix.isEmpty() -> "$scheme://$authority/socket.io/?EIO=4&transport=websocket"
            else -> "$scheme://$authority/$pathPrefix/socket.io/?EIO=4&transport=websocket"
        }

        val newSocket = KmpSocketIO(
            wsUrl = wsUrl,
            auth = mapOf(
                "token" to cleanToken,
                "accountId" to cleanAcc.ifBlank { "all" },
            ),
            onEvent = { name, args ->
                val newHandle = handle ?: return@KmpSocketIO
                handleEvent(newHandle, name, args)
            },
            onConnected = {
                val newHandle = handle ?: return@KmpSocketIO
                if (setConnected(newHandle, true)) {
                    val target = subscribedAccountId.ifBlank { "all" }
                    newHandle.socket.emit("subscribe", JSONObject().put("accountId", target))
                }
            },
            onDisconnected = {
                handle?.let { setConnected(it, false) }
            },
        )
        val newHandle = SocketHandle(newSocket)
        handle = newHandle
        newSocket.connect()
    }

    private fun handleEvent(source: SocketHandle, name: String, args: List<Any?>) {
        val obj = args.firstOrNull() as? JSONObject ?: return
        when (name) {
            "status:update" -> {
                val accId = obj.optStringClean("accountId")
                val statusObj = obj.optJSONObject("status") ?: return
                if (accId.isBlank()) return
                val parsed = JsonParsers.parseStatusSnapshot(accId, statusObj, obj.optLongFlex("serverTime"))
                _statusUpdates.tryEmit(source to (accId to parsed))
            }
            "log:new" -> {
                _newLogs.tryEmit(source to JsonParsers.parseRunLogEntry(obj))
            }
            "logs:snapshot" -> {
                val accId = obj.optStringClean("accountId")
                val logs = JsonParsers.parseRunLogs(obj.optJSONArray("logs"))
                _logSnapshots.tryEmit(source to (accId to logs))
            }
        }
    }

        private fun setConnected(source: SocketHandle, connected: Boolean): Boolean {
        if (source !== handle) return false
        _isConnected.value = connected
        return true
    }

    fun subscribeAccount(accountId: String) {
        val cleanAcc = accountId.trim()
        subscribedAccountId = cleanAcc
        val current = handle ?: return
        if (current.socket.connected()) {
            current.socket.emit(
                "subscribe",
                JSONObject().put("accountId", cleanAcc.ifBlank { "all" }),
            )
        }
    }

        fun disconnect() {
        handle?.let { h ->
            runCatching { h.socket.disconnect() }
        }
        handle = null
        activeUrl = ""
        activeToken = ""
        _isConnected.value = false
    }
}
