package com.qqfarm.mobile.data.network

import kotlin.concurrent.Volatile
import com.qqfarm.mobile.data.network.json.JSONArray
import com.qqfarm.mobile.data.network.json.JSONException
import com.qqfarm.mobile.data.network.json.JSONObject
import com.qqfarm.mobile.data.network.json.toJsonElement
import com.qqfarm.mobile.platform.createPlatformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * 极简 Socket.IO v5 / Engine.IO v4 客户端（WebSocket-only，跨平台）。
 * 协议：0 open → 40 connect(auth) → 40{sid} ack；2 ping/3 pong；42[event,args]；41 disconnect。
 * 语义对齐 io.socket:socket.io-client 的使用面（connect/disconnect/emit/on + 自动重连）。
 */
internal class KmpSocketIO(
    private val wsUrl: String,
    private val auth: Map<String, String>,
    private val onEvent: (name: String, args: List<Any?>) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client: HttpClient = createPlatformHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession? = null

    private var readerJob: Job? = null
    private var pingWatchdogJob: Job? = null
    private var reconnectJob: Job? = null

    @Volatile
    private var closed = false

    @Volatile
    private var pingDeadlineMs: Long = 0L

    @Volatile
    private var lastMessageMs: Long = 0L

    @Volatile
    private var reconnectionDelayMs: Long = 2_000L

    private val EIO_TEXT = "0123456789"

    /** 建立连接（自动重连，指数退避 2s→10s） */
    fun connect() {
        if (closed) return
        scope.launch {
            runWebSocketAttempt()
        }
    }

    private suspend fun runWebSocketAttempt() {
        if (closed) return
        try {
            val ws = client.webSocketSession(wsUrl) {
                timeout { requestTimeoutMillis = 120_000L }
            }
            session = ws
            lastMessageMs = nowMs()
            pingDeadlineMs = nowMs() + HANDSHAKE_DEADLINE_MS
            readerJob = scope.launch { readLoop(ws) }
            send(ws, "40" + json.encodeToString(JsonObject.serializer(), JsonObject(auth.mapValues { JsonPrimitive(it.value) })))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            scheduleReconnect()
        }
    }

    private suspend fun readLoop(ws: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession) {
        try {
            for (frame in ws.incoming) {
                if (frame !is Frame.Text) continue
                lastMessageMs = nowMs()
                handleFrame(frame.readText(), ws)
            }
            // 正常关闭
            onDisconnectedSafe()
            scheduleReconnect()
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 会话被取消（disconnect()）
        } catch (e: Exception) {
            onDisconnectedSafe()
            scheduleReconnect()
        }
    }

    private fun handleFrame(text: String, ws: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession) {
        if (text.isEmpty()) return
        val packetType = text[0]
        when (packetType) {
            '0' -> {
                // open：读取 pingInterval/pingTimeout，重置看门狗
                pingDeadlineMs = nowMs() + HANDSHAKE_DEADLINE_MS
                reconnectionDelayMs = 2_000L
            }
            '4' -> {
                val sub = text.getOrNull(1)
                when {
                    sub == '0' -> {
                        // socket.io connect ack → 触发 connected
                        pingDeadlineMs = nowMs() + HANDSHAKE_DEADLINE_MS
                        onConnectedSafe()
                    }
                    sub == '2' -> {
                        // 事件：42 或 42<ackId>[...]
                        val payload = text.substring(2)
                        dispatchEvent(payload)
                    }
                    else -> {
                        // 未知 socket.io 子包，忽略
                    }
                }
            }
            '2' -> {
                // Engine.IO ping → 回 pong "3"
                pingDeadlineMs = nowMs() + HANDSHAKE_DEADLINE_MS
                scope.launch { runCatching { ws.send(Frame.Text("3")) } }
            }
            '3' -> {
                pingDeadlineMs = nowMs() + HANDSHAKE_DEADLINE_MS
            }
            // '1' close / '4' binary attach 等忽略
        }
    }

    private fun dispatchEvent(payload: String) {
        val trimmed = payload.trim()
        if (!trimmed.startsWith("[")) return
        try {
            val array = json.parseToJsonElement(trimmed).jsonArray
            val name = (array.firstOrNull() as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return
            val args = array.drop(1).map { elementToArg(it) }
            onEvent(name, args)
        } catch (_: Exception) {
            // 忽略无法解析的事件帧
        }
    }

    private fun elementToArg(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonObject -> JSONObject(element.toString())
        is JsonArray -> JSONArray(element.toString())
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.booleanOrNull
            else -> element.content.toLongOrNull() ?: element.content.toDoubleOrNull() ?: element.content
        }
    }

    fun emit(name: String, arg: JSONObject) {
        val payload = json.encodeToString(
            JsonArray.serializer(),
            JsonArray(listOf(JsonPrimitive(name), toJsonElement(arg))),
        )
        send("42$payload")
    }

    private fun send(text: String) {
        val ws = session ?: return
        scope.launch {
            runCatching { ws.send(Frame.Text(text)) }
        }
    }

    private suspend fun send(ws: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession, text: String) {
        runCatching { ws.send(Frame.Text(text)) }
    }

    private fun onConnectedSafe() {
        try { onConnected() } catch (_: Exception) {}
    }

    private fun onDisconnectedSafe() {
        try { onDisconnected() } catch (_: Exception) {}
    }

    private fun scheduleReconnect() {
        if (closed) return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(reconnectionDelayMs)
            reconnectionDelayMs = (reconnectionDelayMs * 2).coerceAtMost(10_000L)
            runWebSocketAttempt()
        }
    }

    fun connected(): Boolean = session?.incoming != null && !closed

    fun disconnect() {
        closed = true
        readerJob?.cancel()
        reconnectJob?.cancel()
        pingWatchdogJob?.cancel()
        scope.launch { runCatching { session?.close() } }
        session = null
        scope.cancel()
        onDisconnectedSafe()
    }

    private fun nowMs(): Long = com.qqfarm.mobile.platform.wallClockMillis()

    private companion object {
        const val HANDSHAKE_DEADLINE_MS = 65_000L
    }
}
