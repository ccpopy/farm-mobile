package com.qqfarm.mobile.data.network

import com.qqfarm.mobile.data.model.AccountLogEvent
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.StatusSnapshot
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.json.JSONObject

class RealtimeSocketManager(private val okHttpClient: OkHttpClient) {

    private var socket: Socket? = null
    private var activeUrl: String = ""
    private var activeToken: String = ""

    @Volatile
    private var subscribedAccountId: String = ""

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _statusUpdates = MutableSharedFlow<Pair<String, StatusSnapshot>>(extraBufferCapacity = 32)
    val statusUpdates: SharedFlow<Pair<String, StatusSnapshot>> = _statusUpdates.asSharedFlow()

    private val _newLogs = MutableSharedFlow<RunLogEntry>(extraBufferCapacity = 64)
    val newLogs: SharedFlow<RunLogEntry> = _newLogs.asSharedFlow()

    private val _logSnapshots = MutableSharedFlow<Pair<String, List<RunLogEntry>>>(extraBufferCapacity = 16)
    val logSnapshots: SharedFlow<Pair<String, List<RunLogEntry>>> = _logSnapshots.asSharedFlow()

    private val _accountLogs = MutableSharedFlow<AccountLogEvent>(extraBufferCapacity = 32)
    val accountLogs: SharedFlow<AccountLogEvent> = _accountLogs.asSharedFlow()

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
            _isConnected.value = true
            val target = subscribedAccountId.ifBlank { "all" }
            newSocket.emit("subscribe", JSONObject().put("accountId", target))
        }
        newSocket.on(Socket.EVENT_DISCONNECT) {
            _isConnected.value = false
        }
        newSocket.on(Socket.EVENT_CONNECT_ERROR) {
            _isConnected.value = false
        }
        newSocket.on("status:update") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val accId = obj.optStringClean("accountId")
            val statusObj = obj.optJSONObject("status") ?: return@on
            if (accId.isBlank()) return@on
            val parsed = JsonParsers.parseStatusSnapshot(accId, statusObj, obj.optLongFlex("serverTime"))
            _statusUpdates.tryEmit(accId to parsed)
        }
        newSocket.on("log:new") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            _newLogs.tryEmit(JsonParsers.parseRunLogEntry(obj))
        }
        newSocket.on("logs:snapshot") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val accId = obj.optStringClean("accountId")
            val logs = JsonParsers.parseRunLogs(obj.optJSONArray("logs"))
            _logSnapshots.tryEmit(accId to logs)
        }
        newSocket.on("account-log:new") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            _accountLogs.tryEmit(JsonParsers.parseAccountLogEvent(obj))
        }

        socket = newSocket
        newSocket.connect()
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
