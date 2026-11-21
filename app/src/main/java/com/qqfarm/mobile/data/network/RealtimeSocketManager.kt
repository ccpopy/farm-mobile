package com.qqfarm.mobile.data.network

import com.qqfarm.mobile.data.model.AccountLogEvent
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.StatusSnapshot
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.json.JSONObject

class RealtimeSocketManager(private val okHttpClient: OkHttpClient) {

    @Volatile
    private var socket: Socket? = null
    private var activeUrl: String = ""
    private var activeToken: String = ""

    @Volatile
    private var subscribedAccountId: String = ""

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // 切换服务器后，已经排队的旧连接事件也必须按来源过滤。
    private fun <T : Any> MutableSharedFlow<Pair<Socket, T>>.fromCurrentSocket(): Flow<T> =
        mapNotNull { (source, value) -> value.takeIf { source === socket } }

    private val _statusUpdates = MutableSharedFlow<Pair<Socket, Pair<String, StatusSnapshot>>>(extraBufferCapacity = 32)
    val statusUpdates: Flow<Pair<String, StatusSnapshot>> = _statusUpdates.fromCurrentSocket()

    private val _newLogs = MutableSharedFlow<Pair<Socket, RunLogEntry>>(extraBufferCapacity = 64)
    val newLogs: Flow<RunLogEntry> = _newLogs.fromCurrentSocket()

    private val _logSnapshots = MutableSharedFlow<Pair<Socket, Pair<String, List<RunLogEntry>>>>(extraBufferCapacity = 16)
    val logSnapshots: Flow<Pair<String, List<RunLogEntry>>> = _logSnapshots.fromCurrentSocket()

    private val _accountLogs = MutableSharedFlow<Pair<Socket, AccountLogEvent>>(extraBufferCapacity = 32)
    val accountLogs: Flow<AccountLogEvent> = _accountLogs.fromCurrentSocket()

    @Synchronized
    fun connect(serverUrl: String, token: String, accountId: String) {
        val cleanUrl = FarmApiClient.normalizeBaseUrl(serverUrl).orEmpty()
        val cleanToken = token.trim()
        val cleanAcc = accountId.trim()
        subscribedAccountId = cleanAcc

        if (cleanUrl.isEmpty() || cleanToken.isEmpty()) {
            disconnect()
            return
        }

        val currentSocket = socket
        if (currentSocket != null && activeUrl == cleanUrl && activeToken == cleanToken) {
            if (currentSocket.connected()) {
                currentSocket.emit(
                    "subscribe",
                    JSONObject().put("accountId", cleanAcc.ifBlank { "all" }),
                )
            } else {
                currentSocket.connect()
            }
            return
        }

        disconnect()
        activeUrl = cleanUrl
        activeToken = cleanToken

        val base = cleanUrl.toHttpUrl()
        val options = IO.Options().apply {
            path = base.encodedPath.trimEnd('/') + "/socket.io"
            forceNew = true
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionDelay = 2000L
            reconnectionDelayMax = 10000L
            auth = mapOf(
                "token" to cleanToken,
                "accountId" to cleanAcc.ifBlank { "all" },
            )
            callFactory = okHttpClient
            webSocketFactory = okHttpClient
        }

        val origin = base.newBuilder().encodedPath("/").build()
        val newSocket = IO.socket(origin.toUri(), options)
        newSocket.on(Socket.EVENT_CONNECT) {
            if (!setConnected(newSocket, true)) return@on
            val target = subscribedAccountId.ifBlank { "all" }
            newSocket.emit("subscribe", JSONObject().put("accountId", target))
        }
        newSocket.on(Socket.EVENT_DISCONNECT) {
            setConnected(newSocket, false)
        }
        newSocket.on(Socket.EVENT_CONNECT_ERROR) {
            setConnected(newSocket, false)
        }
        newSocket.on("status:update") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val accId = obj.optStringClean("accountId")
            val statusObj = obj.optJSONObject("status") ?: return@on
            if (accId.isBlank()) return@on
            val parsed = JsonParsers.parseStatusSnapshot(accId, statusObj, obj.optLongFlex("serverTime"))
            _statusUpdates.tryEmit(newSocket to (accId to parsed))
        }
        newSocket.on("log:new") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            _newLogs.tryEmit(newSocket to JsonParsers.parseRunLogEntry(obj))
        }
        newSocket.on("logs:snapshot") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val accId = obj.optStringClean("accountId")
            val logs = JsonParsers.parseRunLogs(obj.optJSONArray("logs"))
            _logSnapshots.tryEmit(newSocket to (accId to logs))
        }
        newSocket.on("account-log:new") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            _accountLogs.tryEmit(newSocket to JsonParsers.parseAccountLogEvent(obj))
        }

        socket = newSocket
        newSocket.connect()
    }

    @Synchronized
    private fun setConnected(source: Socket, connected: Boolean): Boolean {
        if (source !== socket) return false
        _isConnected.value = connected
        return true
    }

    fun subscribeAccount(accountId: String) {
        val cleanAcc = accountId.trim()
        subscribedAccountId = cleanAcc
        val currentSocket = socket ?: return
        if (currentSocket.connected()) {
            currentSocket.emit(
                "subscribe",
                JSONObject().put("accountId", cleanAcc.ifBlank { "all" }),
            )
        }
    }

    @Synchronized
    fun disconnect() {
        socket?.let { s ->
            runCatching {
                s.off()
                s.disconnect()
            }
        }
        socket = null
        activeUrl = ""
        activeToken = ""
        _isConnected.value = false
    }
}
