package com.qqfarm.mobile.data.network

import kotlin.concurrent.Volatile
import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.AccountSettingsModel
import com.qqfarm.mobile.data.model.ActivityDirectoryResponse
import com.qqfarm.mobile.data.model.AutumnWishState
import com.qqfarm.mobile.data.model.BagPriorityItem
import com.qqfarm.mobile.data.model.BagPayload
import com.qqfarm.mobile.data.model.CharityState
import com.qqfarm.mobile.data.model.CollectionOperateResult
import com.qqfarm.mobile.data.model.CollectionSnapshot
import com.qqfarm.mobile.data.model.DailyGiftsSummary
import com.qqfarm.mobile.data.model.FriendBlacklistItem
import com.qqfarm.mobile.data.model.FriendLandsPayload
import com.qqfarm.mobile.data.model.FriendOperationResult
import com.qqfarm.mobile.data.model.FriendsPayload
import com.qqfarm.mobile.data.model.IllustratedBookItem
import com.qqfarm.mobile.data.model.IllustratedClaimResult
import com.qqfarm.mobile.data.model.IllustratedSnapshot
import com.qqfarm.mobile.data.model.InteractRecord
import com.qqfarm.mobile.data.model.LandsPayload
import com.qqfarm.mobile.data.model.LoginConfigModel
import com.qqfarm.mobile.data.model.LoginResponse
import com.qqfarm.mobile.data.model.MallCatalog
import com.qqfarm.mobile.data.model.MallGoods
import com.qqfarm.mobile.data.model.MallPurchaseResult
import com.qqfarm.mobile.data.model.MapleMarketClaimCommissionResult
import com.qqfarm.mobile.data.model.MapleMarketClaimSeedResult
import com.qqfarm.mobile.data.model.MapleMarketComparison
import com.qqfarm.mobile.data.model.MapleMarketExchangeResult
import com.qqfarm.mobile.data.model.MapleMarketPurchaseRecord
import com.qqfarm.mobile.data.model.MapleMarketSellResult
import com.qqfarm.mobile.data.model.MapleMarketState
import com.qqfarm.mobile.data.model.MilkTeaState
import com.qqfarm.mobile.data.model.MobileOfflineEvent
import com.qqfarm.mobile.data.model.MugeState
import com.qqfarm.mobile.data.model.MutationCatalogSnapshot
import com.qqfarm.mobile.data.model.MysteryPurchaseResult
import com.qqfarm.mobile.data.model.MysteryShopState
import com.qqfarm.mobile.data.model.OfflineReminderConfig
import com.qqfarm.mobile.data.model.PetSnapshot
import com.qqfarm.mobile.data.model.PushChannelEntry
import com.qqfarm.mobile.data.model.PluginAccountDetail
import com.qqfarm.mobile.data.model.PluginDefinition
import com.qqfarm.mobile.data.model.PluginListPayload
import com.qqfarm.mobile.data.model.PluginLogEntry
import com.qqfarm.mobile.data.model.PluginQuietWindow
import com.qqfarm.mobile.data.model.PluginSaveResult
import com.qqfarm.mobile.data.model.PluginSpendingPolicy
import com.qqfarm.mobile.data.model.PluginSummary
import com.qqfarm.mobile.data.model.SeedOption
import com.qqfarm.mobile.data.model.QingMeiState
import com.qqfarm.mobile.data.model.QixiState
import com.qqfarm.mobile.data.model.QqQrTask
import com.qqfarm.mobile.data.model.RanchOperateResult
import com.qqfarm.mobile.data.model.RanchQuote
import com.qqfarm.mobile.data.model.RanchViewSnapshot
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.ServerVersionInfo
import com.qqfarm.mobile.data.model.ShareRewardState
import com.qqfarm.mobile.data.model.SolarActionResult
import com.qqfarm.mobile.data.model.SolarCalendarState
import com.qqfarm.mobile.data.model.StatusSnapshot
import com.qqfarm.mobile.data.model.StellarSnapshot
import com.qqfarm.mobile.data.model.SystemReleaseCleanupResult
import com.qqfarm.mobile.data.model.SystemUpdatePullResult
import com.qqfarm.mobile.data.model.SystemUpdateStatus
import com.qqfarm.mobile.data.model.WeatherState
import com.qqfarm.mobile.data.model.WxQrTask
import com.qqfarm.mobile.data.network.json.JSONArray
import com.qqfarm.mobile.data.network.json.JSONException
import com.qqfarm.mobile.data.network.json.JSONObject
import com.qqfarm.mobile.platform.PlatformIOException
import com.qqfarm.mobile.platform.createPlatformHttpClient
import com.qqfarm.mobile.platform.percentEncode
import com.qqfarm.mobile.platform.wallClockMillis
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.http.contentType

class ApiException(
    override val message: String,
    val httpCode: Int = 0,
    val errorCode: String = "",
    val errorType: String = "",
    val remainingMs: Long = 0L,
) : Exception(message)

/** 请求超时档位（对齐原 OkHttp 多客户端分级） */
private enum class TimeoutTier(val requestTimeoutMs: Long) {
    NORMAL(25_000L),
    LONG_POLL(35_000L),
    RANCH(45_000L),
    SYSTEM_UPDATE(90_000L),
}

class FarmApiClient {

    private data class Session(val baseUrl: String = "", val token: String = "", val accountId: String = "")

    @Volatile
    private var session = Session()

    @Volatile
    var onUnauthorized: ((String, String) -> Unit)? = null

    private val client: HttpClient = createPlatformHttpClient {
        // 全局默认超时交给每档请求的 timeout{} 覆盖
    }

    fun updateSession(serverUrl: String, token: String, accountId: String) {
        session = Session(normalizeBaseUrl(serverUrl).orEmpty(), token.trim(), accountId.trim())
    }

    fun resolveAssetUrl(rawPath: String): String {
        val trimmed = rawPath.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("data:image/", ignoreCase = true)) return trimmed
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        val base = session.baseUrl
        if (base.isEmpty()) return trimmed
        return if (trimmed.startsWith("/")) "$base$trimmed" else "$base/$trimmed"
    }

    companion object {
        fun normalizeBaseUrl(raw: String): String? {
            var cleaned = raw.trim()
            if (cleaned.isEmpty()) return null
            if (!cleaned.contains("://")) {
                cleaned = "http://$cleaned"
            }
            // 轻量 URL 校验（对齐原 okhttp HttpUrl 拒绝规则）
            val schemeEnd = cleaned.indexOf("://")
            if (schemeEnd <= 0) return null
            val scheme = cleaned.substring(0, schemeEnd).lowercase()
            if (scheme != "http" && scheme != "https") return null
            val rest = cleaned.substring(schemeEnd + 3)
            if (rest.contains('?') || rest.contains('#') || rest.contains('@')) return null
            val authority = rest.substringBefore('/')
            if (authority.isEmpty()) return null
            return cleaned.trimEnd('/')
        }

        private fun normalizeMessage(raw: String): String {
            val text = raw.trim()
            if (text.isEmpty()) return ""
            val regex = Regex("""^[\w-]+\.[\w.-]+.*\bcode=\d+\b""")
            val match = regex.find(text)
            if (match != null) {
                val rest = text.substring(match.range.last + 1).trim()
                if (rest.isNotEmpty()) return rest
            }
            return text
        }

        private fun encodeQuery(value: String): String =
            percentEncode(value)
    }

    private suspend fun executeJson(
        method: String,
        path: String,
        body: JSONObject? = null,
        accountIdOverride: String? = null,
        overrideBaseUrl: String? = null,
        overrideToken: String? = null,
        tier: TimeoutTier = TimeoutTier.NORMAL,
        requestSession: Session = session,
        acceptUnconfirmedSave: Boolean = false,
    ): JSONObject {
        val targetBase = if (overrideBaseUrl == null) {
            requestSession.baseUrl
        } else {
            normalizeBaseUrl(overrideBaseUrl) ?: throw ApiException("请输入有效的服务器地址")
        }
        if (targetBase.isBlank()) {
            throw ApiException("请先配置远程服务器地址")
        }
        val fullUrl = if (path.startsWith("/")) "$targetBase$path" else "$targetBase/$path"

        val token = overrideToken?.trim() ?: if (overrideBaseUrl == null) requestSession.token else ""
        val accountId = accountIdOverride ?: if (overrideBaseUrl == null) requestSession.accountId else ""
        val httpMethod = method.uppercase()

        return try {
            val response = client.request(fullUrl) {
                this.method = when (httpMethod) {
                    "GET" -> HttpMethod.Get
                    "POST" -> HttpMethod.Post
                    "PUT" -> HttpMethod.Put
                    "DELETE" -> HttpMethod.Delete
                    else -> HttpMethod(httpMethod)
                }
                if (token.isNotBlank()) header("x-admin-token", token)
                if (accountId.isNotBlank()) header("x-account-id", accountId)
                when {
                    body != null -> {
                        contentType(ContentType.Application.Json)
                        setBody(body.toString())
                    }
                    httpMethod == "POST" || httpMethod == "PUT" -> {
                        contentType(ContentType.Application.Json)
                        setBody("{}")
                    }
                    httpMethod == "DELETE" -> setBody(ByteArray(0))
                    else -> {}
                }
                timeout {
                    requestTimeoutMillis = tier.requestTimeoutMs
                    connectTimeoutMillis = 12_000L
                }
            }

            if (response.status.value in 300..399) {
                throw ApiException("服务器地址发生重定向，请填写最终访问地址", httpCode = response.status.value)
            }
            val rawText = response.bodyAsText()
            val json = try {
                if (rawText.trimStart().startsWith("[")) {
                    JSONObject().put("_array", JSONArray(rawText)).put("ok", response.status.isSuccess())
                } else if (rawText.isNotBlank()) {
                    JSONObject(rawText)
                } else {
                    JSONObject().put("ok", response.status.isSuccess())
                }
            } catch (_: Exception) {
                if (!response.status.isSuccess()) {
                    throw ApiException("服务器返回异常 (${response.status.value})", httpCode = response.status.value)
                }
                throw ApiException("服务器响应格式无效")
            }

            if (response.status.value == 401 && overrideBaseUrl == null && overrideToken == null && token.isNotBlank()) {
                onUnauthorized?.invoke(targetBase, token)
            }

            val savedPendingConfirmation = acceptUnconfirmedSave && response.status.value == 202 &&
                json.optBoolean("saved") && json.optBoolean("unconfirmed")
            if (!response.status.isSuccess() || (!savedPendingConfirmation && json.has("ok") && !json.optBoolFlex("ok", true))) {
                val errMsg = normalizeMessage(
                    json.optStringClean("errorMessage")
                        .ifBlank { json.optStringClean("error") }
                        .ifBlank { json.optStringClean("message") }
                        .ifBlank { "请求失败 (${response.status.value})" }
                )
                throw ApiException(
                    message = errMsg,
                    httpCode = response.status.value,
                    errorCode = json.optStringClean("errorCode").ifBlank { json.optStringClean("code") },
                    errorType = json.optStringClean("errorType"),
                    remainingMs = json.optLongFlex("remainingMs"),
                )
            }
            json
        } catch (e: ApiException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: PlatformIOException) {
            throw ApiException("无法连接到服务器，请检查地址与网络 (${e.message ?: "网络错误"})")
        } catch (e: Exception) {
            throw ApiException("无法连接到服务器，请检查地址与网络 (${e.message ?: "网络错误"})")
        }
    }

    // ---- Server & Authentication ----

    suspend fun fetchGameVersion(overrideBaseUrl: String? = null): ServerVersionInfo {
        val json = executeJson(
            method = "GET",
            path = "/api/game-version",
            overrideBaseUrl = overrideBaseUrl,
        )
        if (json.optStringClean("botVersion").isBlank()) {
            throw ApiException("该地址未返回 QQ 农场助手版本信息")
        }
        return JsonParsers.parseServerVersion(json)
    }

    suspend fun login(overrideBaseUrl: String, username: String, password: String): LoginResponse {
        return try {
            val payload = JSONObject()
                .put("username", username.trim())
                .put("password", password)
            val json = executeJson(
                method = "POST",
                path = "/api/login",
                body = payload,
                overrideBaseUrl = overrideBaseUrl,
                overrideToken = "",
            )
            val data = json.optJSONObject("data") ?: JSONObject()
            val userObj = data.optJSONObject("user") ?: JSONObject()
            LoginResponse(
                ok = true,
                token = data.optStringClean("token"),
                username = userObj.optStringClean("username", username),
                mustChangePassword = data.optBoolFlex("mustChangePassword"),
            )
        } catch (e: ApiException) {
            LoginResponse(
                ok = false,
                error = e.message,
                errorType = e.errorType,
                remainingMs = e.remainingMs,
            )
        }
    }

    suspend fun validateToken(overrideBaseUrl: String? = null, tokenOverride: String? = null): Boolean {
        val json = executeJson(
            method = "GET",
            path = "/api/auth/validate",
            overrideBaseUrl = overrideBaseUrl,
            overrideToken = tokenOverride,
        )
        val data = json.optJSONObject("data")
        return data?.optBoolFlex("valid") == true
    }

    suspend fun fetchUserMe(overrideBaseUrl: String? = null, tokenOverride: String? = null): String {
        val json = executeJson(
            method = "GET",
            path = "/api/user/me",
            overrideBaseUrl = overrideBaseUrl,
            overrideToken = tokenOverride,
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        return data.optStringClean("username")
    }

    suspend fun fetchApiToken(overrideBaseUrl: String? = null, tokenOverride: String? = null): String {
        val json = executeJson(
            method = "GET",
            path = "/api/user/api-token",
            overrideBaseUrl = overrideBaseUrl,
            overrideToken = tokenOverride,
        )
        return json.optJSONObject("data")?.optStringClean("token").orEmpty()
    }

    suspend fun resetApiToken(): String {
        val json = executeJson("POST", "/api/user/api-token/reset")
        return json.optJSONObject("data")?.optStringClean("token").orEmpty()
    }

    suspend fun logout() {
        runCatching { executeJson("POST", "/api/logout") }
    }

    // ---- Accounts & QR Login ----

    suspend fun fetchAccounts(): List<AccountItem> = fetchAccounts(session)

    private suspend fun fetchAccounts(requestSession: Session): List<AccountItem> {
        val json = executeJson("GET", "/api/accounts", requestSession = requestSession)
        val data = json.optJSONObject("data")
        if (data?.optJSONArray("accounts") == null) throw ApiException("服务端未返回账号列表")
        return JsonParsers.parseAccounts(data)
    }

    suspend fun fetchOnlineAccountCount(): Int {
        val requestSession = session
        return fetchAccounts(requestSession).count { account ->
            account.running && run {
                val reply = fetchStatusResponse(account.id, requestSession)
                reply.optJSONObject("data")?.optJSONObject("connection")?.opt("connected") as? Boolean
                    ?: throw ApiException("服务端未返回账号连接状态")
            }
        }
    }

    suspend fun startAccount(accountId: String) = startAccount(accountId, session)

    private suspend fun startAccount(accountId: String, requestSession: Session) {
        executeJson(
            "POST", "/api/accounts/${encodeQuery(accountId)}/start",
            accountIdOverride = accountId,
            requestSession = requestSession,
        )
    }

    suspend fun stopAccount(accountId: String) {
        executeJson("POST", "/api/accounts/${encodeQuery(accountId)}/stop")
    }

    suspend fun deleteAccount(accountId: String) {
        executeJson("DELETE", "/api/accounts/${encodeQuery(accountId)}")
    }

    /** Returns the startup error if the new code was saved but starting the account failed. */
    suspend fun updateAccount(accountId: String, name: String, code: String): String? {
        val body = JSONObject().put("id", accountId)
        if (code.isEmpty()) {
            executeJson(
                "POST", "/api/account/remark", body = body.put("remark", name),
                accountIdOverride = accountId,
            )
            return null
        }

        val requestSession = session
        val response = executeJson(
            "POST", "/api/accounts", body = body.put("name", name).put("code", code),
            accountIdOverride = accountId,
            requestSession = requestSession,
        )
        val data = response.optJSONObject("data") ?: throw ApiException("服务器未确认账号更新")
        if (JsonParsers.parseAccounts(data).none { it.id == accountId }) {
            throw ApiException("账号已不存在，请刷新账号列表", httpCode = 404)
        }

        // Saving restarts running accounts. Starting is idempotent and also resumes stopped accounts.
        return try {
            startAccount(accountId, requestSession)
            null
        } catch (e: ApiException) {
            e.message
        }
    }

    suspend fun saveAccount(
        name: String,
        platform: String,
        code: String,
        loginType: String = "manual",
        id: String? = null,
        wxLoginTaskId: String? = null,
        wxSessionJson: JSONObject? = null,
    ) {
        val body = JSONObject()
            .put("name", name.trim())
            .put("platform", platform)
            .put("loginType", loginType)
        if (!id.isNullOrBlank()) body.put("id", id)
        if (code.isNotBlank()) body.put("code", code.trim())
        if (!wxLoginTaskId.isNullOrBlank()) body.put("wxLoginTaskId", wxLoginTaskId)
        if (wxSessionJson != null) body.put("wxSession", wxSessionJson)
        executeJson("POST", "/api/accounts", body = body)
    }

    suspend fun fetchLoginConfig(): LoginConfigModel {
        val json = executeJson("GET", "/api/settings/login-config")
        return JsonParsers.parseLoginConfig(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun createWxQrTask(): WxQrTask {
        val json = executeJson("POST", "/api/wx-login/tasks")
        return JsonParsers.parseWxQrTask(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun pollWxQrStatus(taskId: String): WxQrTask {
        val json = executeJson("GET", "/api/wx-login/tasks/${encodeQuery(taskId)}/status", tier = TimeoutTier.LONG_POLL)
        return JsonParsers.parseWxQrTask(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun confirmWxQr(taskId: String) {
        executeJson("POST", "/api/wx-login/tasks/${encodeQuery(taskId)}/confirm")
    }

    suspend fun claimWxQrCode(taskId: String): String {
        val json = executeJson("POST", "/api/wx-login/tasks/${encodeQuery(taskId)}/code")
        return json.optJSONObject("data")?.optStringClean("code").orEmpty()
    }

    suspend fun cancelWxQrTask(taskId: String) {
        runCatching { executeJson("DELETE", "/api/wx-login/tasks/${encodeQuery(taskId)}") }
    }

    suspend fun createQqQrTask(): QqQrTask {
        val json = executeJson("POST", "/api/qq-login/tasks")
        return JsonParsers.parseQqQrTask(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun pollQqQrStatus(taskId: String): QqQrTask {
        val json = executeJson("POST", "/api/qq-login/tasks/${encodeQuery(taskId)}/status", tier = TimeoutTier.LONG_POLL)
        return JsonParsers.parseQqQrTask(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun claimQqQrCode(taskId: String): String {
        val json = executeJson("POST", "/api/qq-login/tasks/${encodeQuery(taskId)}/code", tier = TimeoutTier.LONG_POLL)
        return json.optJSONObject("data")?.optStringClean("code").orEmpty()
    }

    suspend fun cancelQqQrTask(taskId: String) {
        runCatching { executeJson("POST", "/api/qq-login/tasks/${encodeQuery(taskId)}/cancel") }
    }

    // ---- Overview, Status, Logs, Gifts ----

    private suspend fun fetchStatusResponse(accountId: String, requestSession: Session): JSONObject =
        executeJson("GET", "/api/status", accountIdOverride = accountId, requestSession = requestSession)

    suspend fun fetchStatus(accountId: String): StatusSnapshot {
        val json = fetchStatusResponse(accountId, session)
        val data = json.optJSONObject("data") ?: JSONObject()
        return JsonParsers.parseStatusSnapshot(accountId, data, json.optLongFlex("serverTime"))
    }

    suspend fun fetchDiamond(accountId: String): Long {
        val json = executeJson("GET", "/api/diamond", accountIdOverride = accountId)
        return json.optJSONObject("data")?.optLongFlex("diamond") ?: 0L
    }

    suspend fun fetchDailyGifts(accountId: String): DailyGiftsSummary {
        val json = executeJson("GET", "/api/daily-gifts", accountIdOverride = accountId)
        return JsonParsers.parseDailyGifts(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun fetchLogs(
        accountId: String,
        module: String = "",
        keyword: String = "",
        isWarn: Boolean? = null,
        limit: Int = 120,
        timeFrom: String = "",
    ): List<RunLogEntry> {
        val params = mutableListOf("limit=$limit")
        if (accountId.isBlank() || accountId == "all") {
            params.add("accountId=all")
        }
        if (module.isNotBlank()) params.add("module=${encodeQuery(module)}")
        if (keyword.isNotBlank()) params.add("keyword=${encodeQuery(keyword)}")
        if (isWarn != null) params.add("isWarn=${if (isWarn) "true" else "false"}")
        if (timeFrom.isNotBlank()) params.add("timeFrom=${encodeQuery(timeFrom)}")
        val query = params.joinToString("&")
        val json = executeJson(
            method = "GET",
            path = "/api/logs?$query",
            accountIdOverride = if (accountId.isNotBlank() && accountId != "all") accountId else null,
        )
        return JsonParsers.parseRunLogs(json.optJSONArray("data"))
    }

    suspend fun clearLogs(accountId: String) {
        executeJson("DELETE", "/api/logs", accountIdOverride = accountId)
    }

    // ---- Farm, Bag, Ranch, Pets ----

    suspend fun fetchLands(accountId: String): LandsPayload {
        val json = executeJson("GET", "/api/lands", accountIdOverride = accountId)
        return JsonParsers.parseLandsPayload(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun operateFarm(accountId: String, opType: String, landId: Int? = null): String {
        val body = JSONObject().put("opType", opType)
        if (landId != null) body.put("landId", landId)
        val json = executeJson("POST", "/api/farm/operate", body = body, accountIdOverride = accountId)
        val data = json.optJSONObject("data") ?: JSONObject()
        val hadWork = data.optBoolFlex("hadWork")
        if (opType == "clear" && landId != null) return if (hadWork) "护理完成" else "当前无需操作"
        val actions = data.optJSONArray("actions")
        if (actions != null && actions.length() > 0) {
            return buildList {
                for (i in 0 until actions.length()) {
                    val s = actions.optString(i).trim()
                    if (s.isNotEmpty()) add(s)
                }
            }.joinToString(" · ")
        }
        return if (hadWork) "操作完成" else "当前无需操作"
    }

    suspend fun fertilizeLand(accountId: String, landId: Int, fertilizerType: String) {
        val body = JSONObject().put("landId", landId).put("fertilizerType", fertilizerType)
        executeJson("POST", "/api/farm/fertilize", body = body, accountIdOverride = accountId)
    }

    suspend fun fertilizeAll(accountId: String, fertilizerType: String): Int {
        val body = JSONObject().put("fertilizerType", fertilizerType)
        val json = executeJson("POST", "/api/farm/fertilize-all", body = body, accountIdOverride = accountId)
        return json.optJSONObject("data")?.optIntFlex("successCount") ?: 0
    }

    suspend fun fetchBag(accountId: String): BagPayload {
        val json = executeJson("GET", "/api/bag", accountIdOverride = accountId)
        return JsonParsers.parseBagPayload(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun useBagItem(accountId: String, itemId: Int, count: Int, uid: Long): String {
        val body = JSONObject().put("itemId", itemId).put("count", count).put("uid", uid)
        val json = executeJson("POST", "/api/bag/use", body = body, accountIdOverride = accountId)
        val rewards = json.optJSONObject("data")?.optJSONArray("rewards")
        if (rewards != null && rewards.length() > 0) {
            return buildList {
                for (i in 0 until rewards.length()) {
                    val r = rewards.optJSONObject(i) ?: continue
                    add("${r.optStringClean("name")} ×${r.optLongFlex("count")}")
                }
            }.joinToString("、")
        }
        return "使用成功"
    }

    suspend fun sellBagItem(accountId: String, itemId: Int, count: Long, uid: Long) {
        val entry = JSONObject().put("id", itemId).put("count", count).put("uid", uid)
        val body = JSONObject().put("items", JSONArray().put(entry))
        executeJson("POST", "/api/bag/sell", body = body, accountIdOverride = accountId)
    }

    suspend fun lockBagItems(accountId: String, uids: List<Long>, locked: Boolean) {
        val arr = JSONArray()
        uids.forEach { arr.put(it) }
        val body = JSONObject().put("itemUids", arr).put("locked", locked)
        executeJson("POST", "/api/bag/lock", body = body, accountIdOverride = accountId)
    }

    suspend fun operateRanchDirect(
        accountId: String,
        action: String,
        params: Map<String, Any> = emptyMap(),
    ): String {
        val body = JSONObject().put("direct", true)
        for ((k, v) in params) {
            body.put(k, v)
        }
        val json = executeJson("POST", "/api/ranch/${encodeQuery(action)}", body = body, accountIdOverride = accountId, tier = TimeoutTier.LONG_POLL)
        val result = FeatureJsonParsers.parseRanchOperateResult(json.optJSONObject("data") ?: JSONObject())
        return result.notice.ifBlank { if (result.status == "skipped") "当前没有可操作项目" else "操作完成" }
    }

    suspend fun fetchPets(accountId: String): PetSnapshot {
        val json = executeJson("GET", "/api/pets", accountIdOverride = accountId)
        return JsonParsers.parsePetSnapshot(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun deployDog(accountId: String, dogId: Int): PetSnapshot {
        val json = executeJson("POST", "/api/pets/deploy", body = JSONObject().put("dogId", dogId), accountIdOverride = accountId)
        return JsonParsers.parsePetSnapshot(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun withdrawDog(accountId: String): PetSnapshot {
        val json = executeJson("POST", "/api/pets/withdraw", body = JSONObject(), accountIdOverride = accountId)
        return JsonParsers.parsePetSnapshot(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun useDogFood(accountId: String, itemId: Int, count: Int = 1): PetSnapshot {
        val body = JSONObject().put("itemId", itemId).put("count", count).put("uid", 0)
        val json = executeJson("POST", "/api/pets/food/use", body = body, accountIdOverride = accountId)
        return JsonParsers.parsePetSnapshot(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun claimDogSkillGifts(accountId: String): Int {
        val json = executeJson("POST", "/api/dog/skill-gifts/claim", body = JSONObject(), accountIdOverride = accountId)
        return json.optJSONObject("data")?.optIntFlex("pending") ?: 0
    }

    // ---- Friends ----

    suspend fun fetchFriends(accountId: String, forceSync: Boolean = false): FriendsPayload {
        val path = if (forceSync) "/api/friends?forceSync=true" else "/api/friends"
        val json = executeJson("GET", path, accountIdOverride = accountId, tier = TimeoutTier.LONG_POLL)
        return JsonParsers.parseFriendsPayload(json)
    }

    suspend fun fetchFriendLands(accountId: String, friendGid: Long): FriendLandsPayload {
        val json = executeJson("GET", "/api/friend/$friendGid/lands", accountIdOverride = accountId, tier = TimeoutTier.LONG_POLL)
        val data = json.optJSONObject("data") ?: throw ApiException("服务器未返回好友农田")
        if (data.optJSONArray("lands") == null) throw ApiException("服务器未返回好友地块")
        return JsonParsers.parseFriendLandsPayload(data)
    }

    suspend fun operateFriend(accountId: String, friendGid: Long, opType: String): FriendOperationResult {
        return try {
            val json = executeJson(
                "POST",
                "/api/friend/$friendGid/op",
                body = JSONObject().put("opType", opType),
                accountIdOverride = accountId,
                tier = TimeoutTier.LONG_POLL,
            )
            JsonParsers.parseFriendOperation(json)
        } catch (e: ApiException) {
            val fruitAlreadyPicked = e.errorCode == "1001040" || e.message.contains("果实被人摘走") ||
                Regex("""\bcode=1001040\b""").containsMatchIn(e.message)
            if (opType != "steal" || !fruitAlreadyPicked) throw e
            FriendOperationResult("果实已被摘走", count = null, fruitAlreadyPicked = true)
        }
    }

    suspend fun fetchInteractRecords(accountId: String): List<InteractRecord> {
        val json = executeJson("GET", "/api/interact-records", accountIdOverride = accountId)
        return JsonParsers.parseInteractRecords(json.optJSONArray("data"))
    }

    suspend fun fetchFriendBlacklist(accountId: String): List<FriendBlacklistItem> {
        val json = executeJson("GET", "/api/friend-blacklist", accountIdOverride = accountId)
        return JsonParsers.parseBlacklist(json.optJSONArray("data"))
    }

    suspend fun toggleFriendBlacklist(accountId: String, gid: Long): List<FriendBlacklistItem> {
        val json = executeJson(
            "POST",
            "/api/friend-blacklist/toggle",
            body = JSONObject().put("gid", gid),
            accountIdOverride = accountId,
        )
        return JsonParsers.parseBlacklist(json.optJSONArray("data"))
    }

    // ---- Plugins ----

    suspend fun fetchPlugins(): PluginListPayload {
        val json = executeJson("GET", "/api/plugins")
        return JsonParsers.parsePluginList(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun fetchPluginDefinition(pluginId: String): PluginDefinition {
        val json = executeJson("GET", "/api/plugins/${encodeQuery(pluginId)}")
        return JsonParsers.parsePluginDefinition(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun fetchBagSeeds(accountId: String): List<BagPriorityItem> {
        val json = executeJson("GET", "/api/bag/seeds", accountIdOverride = accountId)
        val seeds = json.optJSONArray("data") ?: throw ApiException("服务器未返回背包种子列表")
        return try {
            buildList {
                for (index in 0 until seeds.length()) {
                    val seed = seeds.getJSONObject(index)
                    add(BagPriorityItem(seed.getInt("seedId"), seed.getString("name"), seed.getLong("count"), seed.optStringClean("image")))
                }
            }
        } catch (e: JSONException) {
            throw ApiException("背包种子数据格式有误：${e.message}")
        }
    }

    suspend fun fetchSeedCatalog(accountId: String): List<SeedOption> {
        val json = executeJson("GET", "/api/seeds", accountIdOverride = accountId)
        val arr = json.optJSONArray("data") ?: json.optJSONObject("data")?.optJSONArray("seeds")
        return JsonParsers.parseSeedCatalog(arr)
    }

    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean): PluginSummary {
        val json = executeJson(
            "POST",
            "/api/plugins/${encodeQuery(pluginId)}/enabled",
            body = JSONObject().put("enabled", enabled),
        )
        val data = json.optJSONObject("data")
        if (data == null || data.opt("enabled") !is Boolean || data.optStringClean("id") != pluginId) {
            throw ApiException("服务器未确认插件状态，请刷新核对")
        }
        return JsonParsers.parsePluginSummary(data)
    }

    suspend fun fetchPluginAccountDetail(
        pluginId: String,
        accountId: String,
        isBuiltin: Boolean,
    ): PluginAccountDetail {
        val viewJson = executeJson(
            "GET",
            "/api/plugins/${encodeQuery(pluginId)}/accounts/${encodeQuery(accountId)}",
        )
        val base = JsonParsers.parsePluginAccountDetail(viewJson.optJSONObject("data") ?: JSONObject(), isBuiltin)
        val logs = if (isBuiltin) {
            emptyList()
        } else {
            try {
                fetchPluginLogs(pluginId, accountId)
            } catch (_: ApiException) {
                emptyList()
            }
        }
        return base.copy(logs = logs)
    }

    /** 只读当前账号下官方模块是否暂停，用于列表外层开关；不拉日志，不触发游戏操作。 */
    suspend fun fetchBuiltinPluginPaused(pluginId: String, accountId: String): Boolean {
        val json = executeJson(
            "GET",
            "/api/plugins/${encodeQuery(pluginId)}/accounts/${encodeQuery(accountId)}",
        )
        return JsonParsers.parseBuiltinPluginPaused(json.optJSONObject("data"))
    }

    suspend fun setBuiltinPluginPaused(pluginId: String, accountId: String, paused: Boolean): Boolean {
        val json = executeJson(
            "POST",
            "/api/plugins/${encodeQuery(pluginId)}/accounts/${encodeQuery(accountId)}/pause",
            body = JSONObject().put("paused", paused),
        )
        return json.optJSONObject("data")?.opt("paused") as? Boolean
            ?: throw ApiException("服务器未确认模块状态，请刷新核对")
    }

    suspend fun runPluginJob(pluginId: String, jobId: String, accountId: String?) {
        val body = JSONObject()
        if (!accountId.isNullOrBlank()) body.put("accountId", accountId)
        executeJson(
            "POST",
            "/api/plugins/${encodeQuery(pluginId)}/jobs/${encodeQuery(jobId)}/run",
            body = body,
        )
    }

    suspend fun fetchPluginLogs(pluginId: String, accountId: String?): List<PluginLogEntry> {
        val query = if (!accountId.isNullOrBlank()) "?accountId=${encodeQuery(accountId)}" else ""
        val json = executeJson("GET", "/api/plugins/${encodeQuery(pluginId)}/logs$query")
        return JsonParsers.parsePluginLogs(json.optJSONArray("data"))
    }

    suspend fun resolvePluginOperation(
        pluginId: String,
        operationId: String,
        resolution: String,
        isNative: Boolean,
        accountId: String,
    ) {
        val path = if (isNative) {
            "/api/plugins/accounts/${encodeQuery(accountId)}/native-operations/${encodeQuery(operationId)}/resolve"
        } else {
            "/api/plugins/${encodeQuery(pluginId)}/operations/${encodeQuery(operationId)}/resolve"
        }
        executeJson("POST", path, body = JSONObject().put("resolution", resolution))
    }

    suspend fun saveBuiltinPluginSettings(
        pluginId: String,
        accountId: String,
        model: AccountSettingsModel,
        baseline: AccountSettingsModel,
        settingsRevision: String,
    ): PluginSaveResult {
        val settings = JSONObject()
        val automation = JSONObject()
        val intervals = JSONObject()
        fun JSONObject.changed(key: String, before: Any?, after: Any?) {
            if (before != after) put(key, if (after is List<*>) JSONArray(after) else after)
        }

        // 服务端合并局部设置。只提交用户编辑的字段，避免重写未展示的策略和旧配置。
        for ((key, enabled) in model.automationBooleans) {
            automation.changed(key, baseline.automationBooleans[key], enabled)
        }
        when (pluginId) {
            "official.daily-farm" -> {
                intervals.changed("farmMin", baseline.farmMinSec, model.farmMinSec)
                intervals.changed("farmMax", baseline.farmMaxSec, model.farmMaxSec)
                settings.changed("plantingStrategy", baseline.plantingStrategy, model.plantingStrategy)
                settings.changed("preferredSeedId", baseline.preferredSeedId, model.preferredSeedId)
                settings.changed("bagSeedMultiLandReservationEnabled", baseline.bagSeedMultiLandReservationEnabled, model.bagSeedMultiLandReservationEnabled)
                settings.changed("bagSeedPriority", baseline.bagSeedPriority, model.bagSeedPriority)
                settings.changed("bagSeedFallbackStrategy", baseline.bagSeedFallbackStrategy, model.bagSeedFallbackStrategy)
                settings.changed("plantOrderRandom", baseline.plantOrderRandom, model.plantOrderRandom)
                settings.changed("plantDelaySeconds", baseline.plantDelaySeconds, model.plantDelaySeconds)
                settings.changed("emailClaimHour", baseline.emailClaimHour, model.emailClaimHour)
            }
            "official.daily-ranch" -> {
                automation.changed("ranch_strategy", baseline.ranchStrategy, model.ranchStrategy)
                automation.changed("ranch_bag_fallback", baseline.ranchBagFallback, model.ranchBagFallback)
                automation.changed("ranch_coop_cub_priority", baseline.ranchCoopCubPriority, model.ranchCoopCubPriority)
                automation.changed("ranch_shed_cub_priority", baseline.ranchShedCubPriority, model.ranchShedCubPriority)
                automation.changed("ranch_coop_animal", baseline.ranchCoopAnimal, model.ranchCoopAnimal)
                automation.changed("ranch_shed_animal", baseline.ranchShedAnimal, model.ranchShedAnimal)
                automation.changed("ranch_grass_threshold_percent", baseline.ranchGrassThresholdPercent, model.ranchGrassThresholdPercent)
                automation.changed("ranch_check_minutes", baseline.ranchCheckMinutes, model.ranchCheckMinutes)
                automation.changed("ranch_feed_normal_stage", baseline.ranchFeedNormalStage, model.ranchFeedNormalStage)
                automation.changed("ranch_feed_organic_stage", baseline.ranchFeedOrganicStage, model.ranchFeedOrganicStage)
            }
            "official.fertilizer" -> {
                automation.changed("fertilizer", baseline.fertilizerMode, model.fertilizerMode)
                automation.changed("fertilizer_smart_seconds", baseline.fertilizerSmartSeconds, model.fertilizerSmartSeconds)
                automation.changed("fertilizer_land_types", baseline.fertilizerLandTypes, model.fertilizerLandTypes)
                settings.changed("fertilizerBuyOrganicCount", baseline.fertilizerBuyOrganicCount, model.fertilizerBuyOrganicCount)
                settings.changed("fertilizerBuyOrganicThresholdHours", baseline.fertilizerBuyOrganicThresholdHours, model.fertilizerBuyOrganicThresholdHours)
                settings.changed("fertilizerBuyNormalCount", baseline.fertilizerBuyNormalCount, model.fertilizerBuyNormalCount)
                settings.changed("fertilizerBuyNormalThresholdHours", baseline.fertilizerBuyNormalThresholdHours, model.fertilizerBuyNormalThresholdHours)
                settings.changed("fertilizerBuyCheckIntervalMinutes", baseline.fertilizerBuyCheckIntervalMinutes, model.fertilizerBuyCheckIntervalMinutes)
            }
            "official.friends" -> {
                intervals.changed("friendMin", baseline.friendMinSec, model.friendMinSec)
                intervals.changed("friendMax", baseline.friendMaxSec, model.friendMaxSec)
                settings.changed("stealDelaySeconds", baseline.stealDelaySeconds, model.stealDelaySeconds)
                settings.changed("autoAcceptFriendMinLevel", baseline.autoAcceptFriendMinLevel, model.autoAcceptFriendMinLevel)
                settings.changed("autoAcceptRequireOwnLevel", baseline.autoAcceptRequireOwnLevel, model.autoAcceptRequireOwnLevel)
                settings.changed("autoAcceptHarvestStealEnabled", baseline.autoAcceptHarvestStealEnabled, model.autoAcceptHarvestStealEnabled)
                settings.changed("autoAcceptHarvestStealHarvest", baseline.autoAcceptHarvestStealHarvest, model.autoAcceptHarvestStealHarvest)
                settings.changed("autoAcceptHarvestStealSteal", baseline.autoAcceptHarvestStealSteal, model.autoAcceptHarvestStealSteal)
                if (baseline.quietHoursEnabled != model.quietHoursEnabled || baseline.quietHoursStart != model.quietHoursStart ||
                    baseline.quietHoursEnd != model.quietHoursEnd || baseline.quietContinueFarm != model.quietContinueFarm) {
                    settings.put("friendQuietHours", JSONObject()
                        .put("enabled", model.quietHoursEnabled).put("start", model.quietHoursStart)
                        .put("end", model.quietHoursEnd).put("continueFarm", model.quietContinueFarm))
                }
            }
        }
        if (intervals.length() > 0) settings.put("intervals", intervals)
        if (automation.length() > 0) settings.put("automation", automation)
        val res = executeJson(
            "PUT", "/api/plugins/${encodeQuery(pluginId)}/accounts/${encodeQuery(accountId)}",
            body = JSONObject().put("settings", settings).put("settingsRevision", settingsRevision),
            acceptUnconfirmedSave = true,
        )
        val view = res.optJSONObject("data")
        if (!res.optBoolean("saved") || view == null) throw ApiException("服务器未确认保存，请重新加载核对")
        return PluginSaveResult(
            detail = JsonParsers.parsePluginAccountDetail(view, isBuiltin = true),
            unconfirmed = res.optBoolean("unconfirmed"),
            message = res.optStringClean("error"),
        )
    }

    suspend fun saveExtensionPluginBinding(
        pluginId: String,
        accountId: String,
        revision: Int,
        enabled: Boolean,
        settingsJson: String,
        quietWindows: List<PluginQuietWindow>,
        spending: PluginSpendingPolicy,
        accountQuietWindows: List<PluginQuietWindow>?,
    ): PluginAccountDetail {
        fun windowsToJson(list: List<PluginQuietWindow>): JSONArray = JSONArray().apply {
            list.forEach { w ->
                put(JSONObject().put("start", w.start.trim()).put("end", w.end.trim()))
            }
        }
        val spendingObj = JSONObject()
            .put("autoSpend", spending.autoSpend)
            .put("rules", JSONArray().apply {
                spending.rules.forEach { r ->
                    put(
                        JSONObject()
                            .put("resourceKey", r.resourceKey.trim())
                            .put("perOperationMax", r.perOperationMax.ifBlank { "-1" })
                            .put("dailyMax", r.dailyMax.ifBlank { "-1" })
                            .put("minReserve", r.minReserve.ifBlank { "0" })
                    )
                }
            })
        val payload = JSONObject()
            .put("revision", revision)
            .put("enabled", enabled)
            .put("settings", JSONObject(settingsJson))
            .put("quietWindows", windowsToJson(quietWindows))
            .put("spending", spendingObj)
        if (accountQuietWindows != null) payload.put("accountQuietWindows", windowsToJson(accountQuietWindows))

        val res = executeJson(
            "PUT",
            "/api/plugins/${encodeQuery(pluginId)}/accounts/${encodeQuery(accountId)}",
            body = payload,
        )
        val parsed = JsonParsers.parsePluginAccountDetail(res.optJSONObject("data") ?: JSONObject(), isBuiltin = false)
        val logs = try {
            fetchPluginLogs(pluginId, accountId)
        } catch (_: ApiException) {
            emptyList()
        }
        return parsed.copy(logs = logs)
    }

    // ---- Account Settings ----

    suspend fun fetchAccountSettings(accountId: String): AccountSettingsModel {
        val json = executeJson("GET", "/api/settings", accountIdOverride = accountId)
        return JsonParsers.parseAccountSettings(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun saveAccountSettings(accountId: String, model: AccountSettingsModel): Pair<Boolean, String> {
        val farmMin = model.farmMinSec.coerceAtLeast(1)
        val farmMax = model.farmMaxSec.coerceAtLeast(farmMin)
        val friendMin = model.friendMinSec.coerceAtLeast(1)
        val friendMax = model.friendMaxSec.coerceAtLeast(friendMin)

        val intervals = runCatching { JSONObject(model.rawIntervalsJson) }.getOrDefault(JSONObject())
        intervals.put("farm", farmMin)
        intervals.put("farmMin", farmMin)
        intervals.put("farmMax", farmMax)
        intervals.put("friendMin", friendMin)
        intervals.put("friendMax", friendMax)

        val automation = runCatching { JSONObject(model.rawAutomationJson) }.getOrDefault(JSONObject())
        automation.put("fertilizer", model.fertilizerMode)
        for ((k, v) in model.automationBooleans) {
            automation.put(k, v)
        }

        val quiet = JSONObject()
            .put("enabled", model.quietHoursEnabled)
            .put("start", model.quietHoursStart)
            .put("end", model.quietHoursEnd)
            .put("continueFarm", model.quietContinueFarm)

        val payload = JSONObject()
            .put("plantingStrategy", model.plantingStrategy)
            .put("preferredSeedId", model.preferredSeedId)
            .put("intervals", intervals)
            .put("friendQuietHours", quiet)
            .put("automation", automation)

        try {
            executeJson("POST", "/api/settings/save", body = payload, accountIdOverride = accountId)
        } catch (e: ApiException) {
            if (e.httpCode == 202) return false to e.message
            throw e
        }
        return true to "账号设置已保存"
    }

    // ---- Mobile Offline Events ----

    suspend fun pollOfflineEvents(
        afterCursor: String,
        accountIds: List<String>,
        waitSeconds: Int = 25,
    ): Triple<String, Boolean, List<MobileOfflineEvent>> {
        val accountsParam = encodeQuery(accountIds.joinToString(","))
        val afterParam = encodeQuery(afterCursor)
        val json = executeJson(
            method = "GET",
            path = "/api/mobile/offline-events?after=$afterParam&accounts=$accountsParam&wait=$waitSeconds",
            tier = TimeoutTier.LONG_POLL,
        )
        return JsonParsers.parseOfflineEvents(json.optJSONObject("data") ?: JSONObject())
    }

    // ---- Solar Calendar ----

    suspend fun fetchSolarCalendar(accountId: String): SolarCalendarState {
        val json = executeJson("GET", "/api/solar-calendar", accountIdOverride = accountId)
        return FeatureJsonParsers.parseSolarCalendar(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun operateSolarCalendar(
        accountId: String,
        action: String,
        body: JSONObject = JSONObject(),
    ): SolarActionResult {
        val json = executeJson(
            "POST",
            "/api/solar-calendar/${encodeQuery(action)}",
            body = body,
            accountIdOverride = accountId,
        )
        return FeatureJsonParsers.parseSolarActionResult(json.optJSONObject("data") ?: JSONObject())
    }

    // ---- Game Mall / Props Shop / Mystery Shop ----

    private suspend fun mergeDiamondIfPresent(accountId: String, catalog: MallCatalog): MallCatalog {
        if (catalog.currencies.none { it.id == 1004 }) return catalog
        val diamond = runCatching { fetchDiamond(accountId) }.getOrNull() ?: return catalog
        if (diamond < 0L) return catalog
        return catalog.copy(
            currencies = catalog.currencies.map { cur ->
                if (cur.id == 1004) cur.copy(count = diamond, balanceKnown = true) else cur
            },
            goods = catalog.goods.map { g ->
                if (g.priceId == 1004) g.copy(priceBalance = diamond) else g
            },
        )
    }

    suspend fun fetchMallCatalog(
        accountId: String,
        slotType: Int,
        propsShop: Boolean = false,
        subSlotType: Int = 1,
    ): MallCatalog {
        val path = if (propsShop) {
            "/api/props-shop"
        } else {
            "/api/game-mall?slotType=$slotType&subSlotType=$subSlotType"
        }
        val json = executeJson("GET", path, accountIdOverride = accountId)
        val parsed = FeatureJsonParsers.parseMallCatalog(json.optJSONObject("data") ?: JSONObject())
        return mergeDiamondIfPresent(accountId, parsed)
    }

    suspend fun purchaseMallGoods(
        accountId: String,
        goods: MallGoods,
        slotType: Int,
        propsShop: Boolean,
        count: Int,
    ): MallPurchaseResult {
        val rewardsArray = JSONArray()
        for (r in goods.rewards) {
            rewardsArray.put(JSONObject().put("id", r.id).put("count", r.count))
        }
        val expectedPrice = JSONObject()
            .put("id", goods.priceId)
            .put("count", goods.priceCount)
            .put("rewards", rewardsArray)
        val payload = JSONObject()
            .put("goodsId", goods.id)
            .put("expectedPrice", expectedPrice)
            .put("slotType", slotType)
            .put("count", count)
        if (propsShop) {
            val expectedOffer = JSONObject()
                .put("price", JSONObject().put("id", goods.priceId).put("count", goods.priceCount))
                .put("rewards", rewardsArray)
            payload.put("expectedOffer", expectedOffer)
        }
        val path = if (propsShop) "/api/props-shop/purchase" else "/api/game-mall/purchase"
        val json = executeJson("POST", path, body = payload, accountIdOverride = accountId)
        val res = FeatureJsonParsers.parseMallPurchaseResult(json.optJSONObject("data") ?: JSONObject())
        return if (res.catalog != null) {
            res.copy(catalog = mergeDiamondIfPresent(accountId, res.catalog))
        } else res
    }

    suspend fun fetchMysteryShop(accountId: String): MysteryShopState {
        val json = executeJson("GET", "/api/mystery-shop", accountIdOverride = accountId)
        return FeatureJsonParsers.parseMysteryShop(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun purchaseMysteryOffer(
        accountId: String,
        npcId: Int,
        expectedOfferJson: String,
    ): MysteryPurchaseResult {
        val payload = JSONObject()
            .put("npcId", npcId)
            .put("expectedOffer", JSONObject(expectedOfferJson))
        val json = executeJson("POST", "/api/mystery-shop/purchase", body = payload, accountIdOverride = accountId)
        return FeatureJsonParsers.parseMysteryPurchaseResult(json.optJSONObject("data") ?: JSONObject())
    }

    // ---- Extended Ranch Operations ----

    suspend fun fetchRanchView(
        accountId: String,
        view: String,
        shopId: Int? = null,
        hostGid: String? = null,
    ): RanchViewSnapshot {
        val params = mutableListOf("view=${encodeQuery(view)}")
        if (shopId != null) params.add("shopId=$shopId")
        if (!hostGid.isNullOrBlank()) params.add("hostGid=${encodeQuery(hostGid)}")
        val json = try {
            executeJson("GET", "/api/ranch?${params.joinToString("&")}", accountIdOverride = accountId, tier = TimeoutTier.RANCH)
        } catch (e: ApiException) {
            if (e.httpCode != 200 || e.message != "未知牧场页面") throw e
            val legacyView = when (view) {
                "mall" -> "cans"
                "overview" -> "animals"
                else -> throw e
            }
            return fetchRanchView(accountId, legacyView, shopId, hostGid)
        }
        val data = json.optJSONObject("data") ?: throw ApiException("服务器未返回牧场数据")
        return FeatureJsonParsers.parseRanchViewSnapshot(data)
    }

    suspend fun quoteRanchAction(
        accountId: String,
        action: String,
        input: JSONObject = JSONObject(),
    ): RanchQuote = quoteRanchAction(accountId, action, input, session)

    private suspend fun quoteRanchAction(
        accountId: String,
        action: String,
        input: JSONObject,
        requestSession: Session,
    ): RanchQuote {
        val json = executeJson(
            "POST",
            "/api/ranch/${encodeQuery(action)}/quote",
            body = input,
            accountIdOverride = accountId,
            tier = TimeoutTier.RANCH,
            requestSession = requestSession,
        )
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回牧场报价")
        if (data.optBoolNullable("sceneRestored") == false) {
            throw ApiException(data.optStringClean("notice").ifBlank { "牧场场景恢复未确认，请重新连接账号后再试" })
        }
        val quote = FeatureJsonParsers.parseRanchQuote(data)
        if (!quote.skipped && quote.signature.isBlank()) {
            throw ApiException("牧场报价缺少确认凭据，请刷新后重试")
        }
        return quote
    }

    suspend fun executeRanchAction(
        accountId: String,
        action: String,
        input: JSONObject = JSONObject(),
        expectedSignature: String,
    ): RanchOperateResult = executeRanchAction(accountId, action, input, expectedSignature, session)

    private suspend fun executeRanchAction(
        accountId: String,
        action: String,
        input: JSONObject,
        expectedSignature: String,
        requestSession: Session,
    ): RanchOperateResult {
        val payload = JSONObject(input.toString())
            .put("expected", JSONObject().put("signature", expectedSignature))
        val json = executeJson(
            "POST",
            "/api/ranch/${encodeQuery(action)}",
            body = payload,
            accountIdOverride = accountId,
            tier = TimeoutTier.RANCH,
            requestSession = requestSession,
        )
        return FeatureJsonParsers.parseRanchOperateResult(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun executeQuotedRanchAction(accountId: String, action: String, input: JSONObject): RanchOperateResult {
        val requestSession = session
        val quote = quoteRanchAction(accountId, action, input, requestSession)
        if (quote.skipped) return RanchOperateResult(status = "skipped", notice = quote.message)
        if (session != requestSession) throw ApiException("服务器或账号已切换，请重新发起操作")
        return executeRanchAction(accountId, action, input, quote.signature, requestSession)
    }

    suspend fun clearRanchIllustratedNew(accountId: String): String {
        val json = executeJson(
            "POST",
            "/api/ranch/illustrated/clear-new",
            body = JSONObject(),
            accountIdOverride = accountId,
            tier = TimeoutTier.RANCH,
        )
        return json.optJSONObject("data")?.optStringClean("notice", "已全部标记为已读") ?: "已全部标记为已读"
    }

    // ---- Activity Center ----

    suspend fun fetchActivityDirectory(accountId: String, isRanch: Boolean): ActivityDirectoryResponse {
        val path = if (isRanch) {
            "/api/activity-center/ranch-activities"
        } else {
            "/api/activity-center/activities"
        }
        val json = executeJson("GET", path, accountIdOverride = accountId)
        return FeatureJsonParsers.parseActivityDirectory(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun fetchMuge(accountId: String): MugeState {
        val json = executeJson("GET", "/api/activities/muge", accountIdOverride = accountId)
        return FeatureJsonParsers.parseMugeState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun fetchMapleMarket(accountId: String): MapleMarketState {
        val json = executeJson("GET", "/api/activities/maple-market", accountIdOverride = accountId)
        return FeatureJsonParsers.parseMapleMarketState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun fetchMapleMarketPurchaseLog(accountId: String): List<MapleMarketPurchaseRecord> {
        val json = executeJson("GET", "/api/activities/maple-market/purchase-log", accountIdOverride = accountId)
        return FeatureJsonParsers.parseMapleMarketPurchaseRecords(json.optJSONObject("data")?.optJSONArray("records"))
    }

    suspend fun fetchMapleMarketComparison(accountId: String): MapleMarketComparison {
        val json = executeJson("GET", "/api/activities/maple-market/friend-quotes", accountIdOverride = accountId)
        return FeatureJsonParsers.parseMapleMarketComparison(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun startMapleMarketComparison(accountId: String): MapleMarketComparison {
        val json = executeJson(
            "POST",
            "/api/activities/maple-market/friend-quotes",
            body = JSONObject(),
            accountIdOverride = accountId,
        )
        return FeatureJsonParsers.parseMapleMarketComparison(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun claimMapleMarketSeed(accountId: String): MapleMarketClaimSeedResult {
        val json = executeJson(
            "POST",
            "/api/activities/maple-market/claim-seed",
            body = JSONObject(),
            accountIdOverride = accountId,
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        val seedObj = data.optJSONObject("data")?.optJSONObject("seed")
        return MapleMarketClaimSeedResult(
            status = data.optStringClean("status"),
            count = seedObj?.optIntFlex("count") ?: 0,
            message = data.optStringClean("message"),
        )
    }

    suspend fun claimMapleMarketCommission(accountId: String): MapleMarketClaimCommissionResult {
        val json = executeJson(
            "POST",
            "/api/activities/maple-market/claim-commission",
            body = JSONObject(),
            accountIdOverride = accountId,
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        val payload = data.optJSONObject("data") ?: JSONObject()
        return MapleMarketClaimCommissionResult(
            status = data.optStringClean("status"),
            claimedCoin = payload.optLongFlex("claimedCoin"),
            claimedCount = payload.optIntFlex("claimedCount"),
            coinBalance = payload.optLongFlex("coinBalance"),
            message = data.optStringClean("message"),
        )
    }

    suspend fun sellMapleMarketCrop(
        accountId: String,
        count: Int,
        expectedUnitPrice: Int,
    ): MapleMarketSellResult {
        val body = JSONObject()
            .put("count", count)
            .put("expectedUnitPrice", expectedUnitPrice)
        val json = executeJson("POST", "/api/activities/maple-market/sell", body = body, accountIdOverride = accountId)
        val data = json.optJSONObject("data") ?: JSONObject()
        return MapleMarketSellResult(
            sold = data.optIntFlex("sold"),
            unitPrice = data.optIntFlex("unitPrice"),
            income = data.optLongFlex("income"),
            coinBalance = data.optLongFlex("coinBalance"),
            remainingQuota = if (data.has("remainingQuota") && !data.isNull("remainingQuota")) data.optIntFlex("remainingQuota") else null,
        )
    }

    suspend fun sellMapleMarketToFriend(
        accountId: String,
        gid: Long,
        count: Int,
        expectedUnitPrice: Int,
    ): MapleMarketSellResult {
        val body = JSONObject()
            .put("gid", gid)
            .put("count", count)
            .put("expectedUnitPrice", expectedUnitPrice)
        val json = executeJson(
            "POST",
            "/api/activities/maple-market/friend-quotes/sell",
            body = body,
            accountIdOverride = accountId,
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        return MapleMarketSellResult(
            sold = data.optIntFlex("sold"),
            unitPrice = data.optIntFlex("unitPrice"),
            income = data.optLongFlex("income"),
            coinBalance = data.optLongFlex("coinBalance"),
            remainingQuota = if (data.has("remainingQuota") && !data.isNull("remainingQuota")) data.optIntFlex("remainingQuota") else null,
        )
    }

    suspend fun exchangeMapleMarketGoods(
        accountId: String,
        goodsId: Int,
        count: Int,
        expectedUnitPrice: Int,
    ): MapleMarketExchangeResult {
        val body = JSONObject()
            .put("goodsId", goodsId)
            .put("count", count)
            .put("expectedUnitPrice", expectedUnitPrice)
        val json = executeJson("POST", "/api/activities/maple-market/exchange", body = body, accountIdOverride = accountId)
        val data = json.optJSONObject("data") ?: JSONObject()
        return MapleMarketExchangeResult(
            goodsId = data.optIntFlex("goodsId"),
            count = data.optIntFlex("count"),
            cost = data.optLongFlex("cost"),
            coinBalance = data.optLongFlex("coinBalance"),
        )
    }

    suspend fun operateMuge(
        accountId: String,
        action: String,
        body: JSONObject = JSONObject(),
    ): Pair<MugeState?, String> {
        val json = executeJson(
            "POST",
            "/api/activities/muge/${encodeQuery(action)}",
            body = body,
            accountIdOverride = accountId,
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        val actObj = data.optJSONObject("activity")
        val rewards = FeatureJsonParsers.parseCommerceItems(data.optJSONArray("rewards"))
        val rewardsText = rewards.joinToString("、") { "${it.name} ×${it.count}" }
        val msg = rewardsText.ifBlank { if (action == "settle") "牧场人格已生成" else "操作已完成" }
        val suffix = if (data.optBoolFlex("refreshRequired")) "（请刷新确认最新进度）" else ""
        return (if (actObj != null) FeatureJsonParsers.parseMugeState(actObj) else null) to "$msg$suffix"
    }

    suspend fun fetchMilkTea(
        accountId: String,
        page: String = "shop",
        extraParams: Map<String, String> = emptyMap(),
    ): MilkTeaState {
        val params = mutableListOf("page=${encodeQuery(page)}")
        for ((k, v) in extraParams) {
            if (v.isNotBlank()) params.add("${encodeQuery(k)}=${encodeQuery(v)}")
        }
        val json = executeJson(
            "GET",
            "/api/activities/milk-tea?${params.joinToString("&")}",
            accountIdOverride = accountId,
        )
        return FeatureJsonParsers.parseMilkTeaState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun operateMilkTea(
        accountId: String,
        action: String,
        body: JSONObject = JSONObject(),
    ): Pair<MilkTeaState?, String> {
        val json = executeJson(
            "POST",
            "/api/activities/milk-tea/${encodeQuery(action)}",
            body = body,
            accountIdOverride = accountId,
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        val actObj = data.optJSONObject("activity")
        val rewards = FeatureJsonParsers.parseCommerceItems(data.optJSONArray("rewards"))
        val msg = rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "操作成功" }
        val suffix = if (data.optBoolFlex("refreshRequired")) "（请刷新确认最新进度）" else ""
        return (if (actObj != null) FeatureJsonParsers.parseMilkTeaState(actObj) else null) to "$msg$suffix"
    }

    suspend fun fetchShareReward(accountId: String, activityId: String): ShareRewardState {
        val json = executeJson(
            "GET",
            "/api/activities/share-reward/${encodeQuery(activityId)}",
            accountIdOverride = accountId,
        )
        return FeatureJsonParsers.parseShareRewardState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun operateShareReward(
        accountId: String,
        activityId: String,
        action: String,
        body: JSONObject = JSONObject(),
    ): Pair<ShareRewardState?, String> {
        val json = executeJson(
            "POST",
            "/api/activities/share-reward/${encodeQuery(activityId)}/${encodeQuery(action)}",
            body = body,
            accountIdOverride = accountId,
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        val actObj = data.optJSONObject("activity")
        val rewards = FeatureJsonParsers.parseCommerceItems(data.optJSONArray("rewards"))
        val grantedScore = data.optJSONObject("result")?.optLongFlex("granted_score") ?: 0L
        val msg = rewards.joinToString("、") { "${it.name} ×${it.count}" }
            .ifBlank { if (grantedScore > 0L) "获得 $grantedScore 快乐值" else "领取成功" }
        val suffix = if (data.optBoolFlex("refreshRequired")) "（请刷新确认最新进度）" else ""
        return (if (actObj != null) FeatureJsonParsers.parseShareRewardState(actObj) else null) to "$msg$suffix"
    }

    suspend fun fetchAutumnWish(accountId: String): AutumnWishState {
        val json = executeJson("GET", "/api/activities/autumn/wish", accountIdOverride = accountId)
        return FeatureJsonParsers.parseAutumnWishState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun operateAutumnWish(
        accountId: String,
        action: String,
        body: JSONObject = JSONObject(),
    ): Pair<AutumnWishState?, String> {
        val json = executeJson(
            "POST",
            "/api/activities/autumn/wish/${encodeQuery(action)}",
            body = body,
            accountIdOverride = accountId,
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        val actObj = data.optJSONObject("activity")
        val rewards = FeatureJsonParsers.parseCommerceItems(data.optJSONArray("rewards"))
        val msg = rewards.joinToString("、") { "${it.name} ×${it.count}" }
            .ifBlank { if (action == "draw") "签文已揭晓，请领取好运奖励" else "操作已完成" }
        val suffix = if (data.optBoolFlex("refreshRequired")) "（请刷新确认最新进度）" else ""
        return (if (actObj != null) FeatureJsonParsers.parseAutumnWishState(actObj) else null) to "$msg$suffix"
    }

    suspend fun fetchStellarActivity(accountId: String): StellarSnapshot {
        val json = executeJson("GET", "/api/activity-center/stellar", accountIdOverride = accountId)
        return FeatureJsonParsers.parseStellarSnapshot(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun claimStellarPass(accountId: String): String {
        val json = executeJson("POST", "/api/activity-center/pass/claim", body = JSONObject(), accountIdOverride = accountId)
        val rewards = FeatureJsonParsers.parseCommerceItems(json.optJSONObject("data")?.optJSONArray("rewards"))
        return rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "游记奖励已领取" }
    }

    suspend fun lightStellarConstellation(accountId: String): String {
        val json = executeJson("POST", "/api/activity-center/constellation/light", body = JSONObject(), accountIdOverride = accountId)
        val rewards = FeatureJsonParsers.parseCommerceItems(json.optJSONObject("data")?.optJSONArray("rewards"))
        return rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "星宿点亮成功" }
    }

    suspend fun exchangeStarSand(accountId: String, goodsId: String, count: Int): String {
        val body = JSONObject().put("goodsId", goodsId).put("count", count)
        val json = executeJson("POST", "/api/activity-center/shop/exchange", body = body, accountIdOverride = accountId)
        val rewards = FeatureJsonParsers.parseCommerceItems(json.optJSONObject("data")?.optJSONArray("rewards"))
        return rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "兑换成功" }
    }

    suspend fun claimStellarSolarTerm(accountId: String, termId: String): String {
        val json = executeJson("POST", "/api/activity-center/solar-terms/${encodeQuery(termId)}/claim", body = JSONObject(), accountIdOverride = accountId)
        val rewards = FeatureJsonParsers.parseCommerceItems(json.optJSONObject("data")?.optJSONArray("rewards"))
        return rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "节令奖励已领取" }
    }

    suspend fun fetchQixi(accountId: String): QixiState {
        val json = executeJson("GET", "/api/activity-center/qixi", accountIdOverride = accountId)
        return FeatureJsonParsers.parseQixiState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun claimQixiBridge(accountId: String): String {
        val json = executeJson("POST", "/api/activity-center/qixi/bridge/claim", body = JSONObject(), accountIdOverride = accountId)
        val rewards = FeatureJsonParsers.parseCommerceItems(json.optJSONObject("data")?.optJSONArray("rewards"))
        return rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "鹊桥奖励已领取" }
    }

    suspend fun giftQixiSachet(accountId: String, friendGid: String, messageTextId: Int = 15): String {
        val body = JSONObject().put("friendGid", friendGid).put("messageTextId", messageTextId)
        executeJson("POST", "/api/activity-center/qixi/gift", body = body, accountIdOverride = accountId)
        return "鹊羽香囊赠送成功"
    }

    suspend fun fetchQingMei(accountId: String): QingMeiState {
        val json = executeJson("GET", "/api/activity-center/qingmei", accountIdOverride = accountId)
        return FeatureJsonParsers.parseQingMeiState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun operateQingMei(accountId: String, actionPath: String, body: JSONObject = JSONObject()): String {
        val json = executeJson(
            "POST",
            "/api/activity-center/qingmei/$actionPath",
            body = body,
            accountIdOverride = accountId,
        )
        val rewards = FeatureJsonParsers.parseCommerceItems(json.optJSONObject("data")?.optJSONArray("rewards"))
        return rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "操作完成" }
    }

    suspend fun fetchCharity(accountId: String): CharityState {
        val json = executeJson("GET", "/api/activity-center/charity-red-flower", accountIdOverride = accountId)
        return FeatureJsonParsers.parseCharityState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun operateCharity(accountId: String, actionPath: String): String {
        val json = executeJson(
            "POST",
            "/api/activity-center/charity-red-flower/$actionPath",
            body = JSONObject(),
            accountIdOverride = accountId,
        )
        val rewards = FeatureJsonParsers.parseCommerceItems(json.optJSONObject("data")?.optJSONArray("rewards"))
        return rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "操作完成" }
    }

    suspend fun fetchWeather(accountId: String): WeatherState {
        val json = executeJson("GET", "/api/activity-center/weather", accountIdOverride = accountId)
        return FeatureJsonParsers.parseWeatherState(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun operateWeather(accountId: String, actionPath: String, body: JSONObject = JSONObject()): String {
        val json = executeJson(
            "POST",
            "/api/activity-center/weather/$actionPath",
            body = body,
            accountIdOverride = accountId,
        )
        val rewards = FeatureJsonParsers.parseCommerceItems(json.optJSONObject("data")?.optJSONArray("rewards"))
        return rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "操作完成" }
    }

    // ---- Illustrated Book & Collections ----

    suspend fun fetchIllustrated(accountId: String): IllustratedSnapshot {
        val json = executeJson("GET", "/api/illustrated", accountIdOverride = accountId, tier = TimeoutTier.LONG_POLL)
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回图鉴数据")
        return FeatureJsonParsers.parseIllustratedSnapshot(data)
    }

    suspend fun claimIllustrated(accountId: String, type: Int): IllustratedClaimResult {
        val body = JSONObject().put("type", type)
        val json = executeJson("POST", "/api/illustrated/claim", body = body, accountIdOverride = accountId, tier = TimeoutTier.LONG_POLL)
        return FeatureJsonParsers.parseIllustratedClaimResult(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun purchaseIllustratedSeed(accountId: String, goodsId: Int, seedId: Int, count: Int): Long {
        val body = JSONObject().put("goodsId", goodsId).put("count", count)
        val json = executeJson("POST", "/api/seeds/purchase", body = body, accountIdOverride = accountId)
        return FeatureJsonParsers.parseIllustratedSeedReceipt(json.optJSONObject("data"), seedId)
    }

    suspend fun fetchSeedShopItems(accountId: String): List<IllustratedBookItem> {
        val json = executeJson("GET", "/api/seeds", accountIdOverride = accountId)
        val seeds = json.optJSONArray("data") ?: json.optJSONObject("data")?.optJSONArray("seeds")
            ?: throw ApiException("服务端未返回种子商店数据")
        val items = FeatureJsonParsers.parseSeedShopItems(seeds)
        if (seeds.length() > 0 && items.isEmpty()) {
            throw ApiException("种子商店暂不可用，请刷新重试")
        }
        return items
    }

    suspend fun fetchCollections(accountId: String): CollectionSnapshot {
        val json = executeJson("GET", "/api/collections", accountIdOverride = accountId)
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回蓝晶藏品数据")
        return FeatureJsonParsers.parseCollectionSnapshot(data)
    }

    suspend fun operateCollections(
        accountId: String,
        action: String,
        body: JSONObject = JSONObject(),
        actionLabel: String = "操作完成",
    ): CollectionOperateResult {
        val json = executeJson(
            method = "POST",
            path = "/api/collections/${encodeQuery(action)}",
            body = body,
            accountIdOverride = accountId,
        )
        return FeatureJsonParsers.parseCollectionOperateResult(json.optJSONObject("data") ?: JSONObject(), actionLabel)
    }

    suspend fun fetchMutationCatalog(): MutationCatalogSnapshot {
        val json = executeJson("GET", "/api/illustrated/mutations")
        return FeatureJsonParsers.parseMutationCatalog(json.optJSONObject("data") ?: JSONObject())
    }

    // ---- Server Version Update ----

    suspend fun fetchSystemUpdateStatus(): SystemUpdateStatus {
        val json = executeJson("GET", "/api/system/update/status")
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回更新状态")
        return FeatureJsonParsers.parseSystemUpdateStatus(data)
    }

    suspend fun checkSystemUpdates(): SystemUpdateStatus {
        val json = executeJson("POST", "/api/system/update/check", tier = TimeoutTier.SYSTEM_UPDATE)
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回检查结果")
        return FeatureJsonParsers.parseSystemUpdateStatus(data)
    }

    suspend fun pullSystemUpdates(): SystemUpdatePullResult {
        val json = executeJson("POST", "/api/system/update/pull", tier = TimeoutTier.SYSTEM_UPDATE)
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回拉取结果")
        return FeatureJsonParsers.parseSystemUpdatePullResult(data)
    }

    suspend fun applySystemUpdate(pullFirst: Boolean, serverUrl: String = session.baseUrl): SystemUpdateStatus =
        submitSystemDeployment(
            "/api/system/update/apply",
            JSONObject().put("pullFirst", pullFirst),
            sessionForServer(serverUrl),
            allowNoChange = true,
        )

    suspend fun rollbackSystemRelease(serverUrl: String, releaseId: String): SystemUpdateStatus =
        submitSystemDeployment("/api/system/update/rollback", JSONObject().put("releaseId", releaseId), sessionForServer(serverUrl))

    suspend fun deleteSystemReleases(
        serverUrl: String,
        releaseIds: List<String>,
        cleanOrphans: Boolean,
    ): SystemReleaseCleanupResult {
        val body = JSONObject().put("releaseIds", JSONArray(releaseIds)).put("cleanOrphans", cleanOrphans)
        val json = executeJson(
            "POST",
            "/api/system/update/releases/delete",
            body = body,
            tier = TimeoutTier.SYSTEM_UPDATE,
            requestSession = sessionForServer(serverUrl),
        )
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未确认清理结果，请刷新备份列表核对")
        return FeatureJsonParsers.parseSystemReleaseCleanupResult(data)
    }

    suspend fun setServerBackupEnabled(serverUrl: String, keepBackups: Boolean): Boolean {
        val body = JSONObject().put("keepBackups", keepBackups)
        val json = executeJson(
            method = "PUT",
            path = "/api/system/update/backup-setting",
            body = body,
            tier = TimeoutTier.SYSTEM_UPDATE,
            requestSession = sessionForServer(serverUrl),
        )
        val saved = json.optJSONObject("data")?.opt("keepBackups") as? Boolean
            ?: throw ApiException("服务端未确认备份设置")
        if (saved != keepBackups) {
            throw ApiException("服务端备份设置未生效，请刷新后核对")
        }
        return saved
    }

    private suspend fun submitSystemDeployment(
        path: String,
        body: JSONObject,
        target: Session,
        allowNoChange: Boolean = false,
    ): SystemUpdateStatus {
        val json = executeJson("POST", path, body = body, tier = TimeoutTier.SYSTEM_UPDATE, requestSession = target)
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回更新任务状态")
        val statusObj = data.optJSONObject("status") ?: throw ApiException("服务端未返回更新状态")
        val status = FeatureJsonParsers.parseSystemUpdateStatus(statusObj)
        val noJob = data.has("job") && data.isNull("job")
        if (allowNoChange && noJob && status.gitSupported && !status.needsApply && !status.deployBusy) {
            return status.copy(job = null)
        }
        if (noJob || status.job?.id.isNullOrBlank()) {
            throw ApiException("服务端未确认版本切换任务，请刷新进度核对")
        }
        return status
    }

    private fun sessionForServer(serverUrl: String): Session {
        val target = session
        if (target.baseUrl != serverUrl) throw ApiException("当前服务器已切换，请重新打开服务端更新")
        return target
    }

    suspend fun saveServerGithubToken(serverUrl: String, token: String) {
        val body = JSONObject().put("token", token.trim())
        val json = executeJson("PUT", "/api/system/update/github-token", body = body, requestSession = sessionForServer(serverUrl))
        if (json.optJSONObject("data")?.opt("githubTokenConfigured") != true) {
            throw ApiException("服务端未确认保存 GitHub 授权")
        }
    }

    suspend fun clearServerGithubToken(serverUrl: String) {
        val json = executeJson("DELETE", "/api/system/update/github-token", requestSession = sessionForServer(serverUrl))
        if (json.optJSONObject("data")?.opt("githubTokenConfigured") != false) {
            throw ApiException("服务端未确认移除 GitHub 授权")
        }
    }

    fun pushSettings(serverUrl: String, authToken: String): PushSettings =
        PushSettings(normalizeBaseUrl(serverUrl).orEmpty(), authToken.trim())

    inner class PushSettings internal constructor(
        private val serverUrl: String,
        private val authToken: String,
    ) {
        private val saveUnconfirmed = "服务端未确认保存推送配置，请刷新后核对"

        suspend fun fetch(): OfflineReminderConfig {
            val response = request("GET", "/api/settings")
            return readConfig(
                response.optJSONObject("data")?.optJSONObject("offlineReminder"),
                "当前服务端未提供推送渠道配置",
            )
        }

        suspend fun setNotificationType(type: String, enabled: Boolean): OfflineReminderConfig =
            save(JSONObject().put("notificationTypes", JSONObject().put(type, enabled))) { data ->
                data.optJSONObject("notificationTypes")?.opt(type) == enabled
            }

        suspend fun saveGeneral(title: String, message: String, deleteSeconds: Long): OfflineReminderConfig {
            val body = messageBody(title, message).put("offlineDeleteSec", deleteSeconds)
            return save(body) { data -> data.optLongFlex("offlineDeleteSec", -1) == deleteSeconds }
        }

        suspend fun setChannelEnabled(channel: String, enabled: Boolean): OfflineReminderConfig {
            val config = save(JSONObject().put("channel", channel).put("enabled", enabled))
            if (!config.supportsMultiChannel || config.entryFor(channel).isEffectivelyEnabled != enabled) {
                throw ApiException(saveUnconfirmed)
            }
            return config
        }

        suspend fun saveChannel(entry: PushChannelEntry, multiChannel: Boolean): OfflineReminderConfig {
            val body = channelBody(entry)
            if (multiChannel) body.put("enabled", entry.enabled)
            val saved = save(body)
            if (saved.supportsMultiChannel != multiChannel ||
                saved.entryFor(entry.channel) != entry ||
                (!multiChannel && !entry.hasCredentials && saved.channels.isNotEmpty())
            ) {
                throw ApiException(saveUnconfirmed)
            }
            return saved
        }

        suspend fun clearChannel(entry: PushChannelEntry, multiChannel: Boolean): OfflineReminderConfig {
            if (multiChannel) return saveChannel(PushChannelEntry(entry.channel), multiChannel = true)

            // The legacy endpoint clears the only channel. Check it still belongs to the open editor.
            val latest = fetch()
            if (latest.supportsMultiChannel || latest.entryFor(entry.channel) != entry) {
                throw ApiException("推送配置已变化，请刷新后再清空")
            }
            return saveChannel(PushChannelEntry("webhook"), multiChannel = false)
        }

        suspend fun testChannel(entry: PushChannelEntry, title: String, message: String): String {
            val body = channelBody(entry)
            val text = messageBody(title, message)
            body.put("title", text.getString("title")).put("msg", text.getString("msg"))
            val response = request("POST", "/api/settings/offline-reminder/test", body)
            val receipt = response.optJSONObject("data")
            if (receipt?.opt("ok") != true ||
                receipt.optStringClean("channel").ifBlank { entry.channel } != entry.channel
            ) {
                throw ApiException("服务端未确认测试通知已发送")
            }
            return response.optStringClean("message").ifBlank { "测试通知已发送" }
        }

        private fun channelBody(entry: PushChannelEntry): JSONObject =
            JSONObject()
                .put("channel", entry.channel)
                .put("endpoint", entry.endpoint)
                .put("token", entry.token)
                .put("secret", entry.secret)

        private fun messageBody(title: String, message: String): JSONObject =
            JSONObject()
                .put("title", title.trim().ifBlank { "账号下线提醒" })
                .put("msg", message.trim().ifBlank { "账号下线" })

        private suspend fun save(
            body: JSONObject,
            confirmed: (JSONObject) -> Boolean = { true },
        ): OfflineReminderConfig {
            val data = request("POST", "/api/settings/offline-reminder", body).optJSONObject("data")
            if (data == null || !confirmed(data)) throw ApiException(saveUnconfirmed)
            return readConfig(data, saveUnconfirmed)
        }

        private fun readConfig(data: JSONObject?, error: String): OfflineReminderConfig {
            if (data == null || (data.optJSONArray("channels") == null &&
                    data.opt("channel") !is String && data.opt("endpoint") !is String)
            ) {
                throw ApiException(error)
            }
            return FeatureJsonParsers.parseOfflineReminder(data)
        }

        private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
            val current = session
            if (current.baseUrl != serverUrl || current.token != authToken || authToken.isBlank()) {
                throw ApiException("服务器或登录状态已切换，请重新打开推送渠道")
            }
            return executeJson(method, path, body = body, accountIdOverride = "", requestSession = current)
        }
    }
}
