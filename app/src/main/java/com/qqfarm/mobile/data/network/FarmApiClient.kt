package com.qqfarm.mobile.data.network

import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.AccountLogEvent
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
import com.qqfarm.mobile.data.model.MilkTeaState
import com.qqfarm.mobile.data.model.MugeState
import com.qqfarm.mobile.data.model.MutationCatalogSnapshot
import com.qqfarm.mobile.data.model.MysteryPurchaseResult
import com.qqfarm.mobile.data.model.MysteryShopState
import com.qqfarm.mobile.data.model.PetSnapshot
import com.qqfarm.mobile.data.model.QingMeiState
import com.qqfarm.mobile.data.model.QixiState
import com.qqfarm.mobile.data.model.QqQrTask
import com.qqfarm.mobile.data.model.RanchAnimalOption
import com.qqfarm.mobile.data.model.RanchOperateResult
import com.qqfarm.mobile.data.model.RanchQuote
import com.qqfarm.mobile.data.model.RanchViewSnapshot
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.SchedulerSummary
import com.qqfarm.mobile.data.model.SeedOption
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(
    override val message: String,
    val httpCode: Int = 0,
    val errorCode: String = "",
    val errorType: String = "",
    val remainingMs: Long = 0L,
) : Exception(message)

private class MutationRequestBody(private val body: RequestBody) : RequestBody() {
    override fun contentType() = body.contentType()
    override fun contentLength() = body.contentLength()
    override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
    override fun isOneShot() = true
}

class FarmApiClient {

    private data class Session(val baseUrl: String = "", val token: String = "", val accountId: String = "")

    @Volatile
    private var session = Session()

    @Volatile
    var onUnauthorized: ((String, String) -> Unit)? = null

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    val imageHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .followRedirects(true)
        .addNetworkInterceptor { chain ->
            val current = session
            val request = chain.request()
            val builder = request.newBuilder().removeHeader("x-admin-token")
            if (current.baseUrl.isNotBlank() && current.token.isNotBlank() &&
                request.url.toString().startsWith("${current.baseUrl}/api/wx-login/tasks/") &&
                request.url.encodedPath.endsWith("/qr")
            ) {
                builder.header("x-admin-token", current.token)
            }
            chain.proceed(builder.build())
        }
        .build()

    private val longPollClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    private val ranchHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val systemUpdateHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

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
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun normalizeBaseUrl(raw: String): String? {
            var cleaned = raw.trim()
            if (cleaned.isEmpty()) return null
            if (!cleaned.contains("://")) {
                cleaned = "http://$cleaned"
            }
            val parsed: HttpUrl = cleaned.toHttpUrlOrNull() ?: return null
            if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty() ||
                parsed.query != null || parsed.fragment != null
            ) return null
            return parsed.toString().trimEnd('/')
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
            URLEncoder.encode(value, "UTF-8")
    }

    private suspend fun executeJson(
        method: String,
        path: String,
        body: JSONObject? = null,
        accountIdOverride: String? = null,
        overrideBaseUrl: String? = null,
        overrideToken: String? = null,
        client: OkHttpClient = okHttpClient,
        requestSession: Session = session,
    ): JSONObject = withContext(Dispatchers.IO) {
        val targetBase = if (overrideBaseUrl == null) {
            requestSession.baseUrl
        } else {
            normalizeBaseUrl(overrideBaseUrl) ?: throw ApiException("请输入有效的服务器地址")
        }
        if (targetBase.isBlank()) {
            throw ApiException("请先配置远程服务器地址")
        }
        val fullUrl = if (path.startsWith("/")) "$targetBase$path" else "$targetBase/$path"

        val reqBuilder = Request.Builder().url(fullUrl)
        val token = overrideToken?.trim() ?: if (overrideBaseUrl == null) requestSession.token else ""
        if (token.isNotBlank()) {
            reqBuilder.header("x-admin-token", token)
        }
        val accountId = accountIdOverride ?: if (overrideBaseUrl == null) requestSession.accountId else ""
        if (accountId.isNotBlank()) {
            reqBuilder.header("x-account-id", accountId)
        }

        val httpMethod = method.uppercase()
        val reqBody = when {
            body != null -> body.toString().toRequestBody(JSON_MEDIA)
            httpMethod == "POST" || httpMethod == "PUT" -> "{}".toRequestBody(JSON_MEDIA)
            httpMethod == "DELETE" -> ByteArray(0).toRequestBody()
            else -> null
        }
        val request = reqBuilder.method(httpMethod, reqBody?.let(::MutationRequestBody)).build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isRedirect) {
                    throw ApiException("服务器地址发生重定向，请填写最终访问地址", httpCode = response.code)
                }
                val rawText = response.body?.string().orEmpty()
                val json = try {
                    if (rawText.trimStart().startsWith("[")) {
                        JSONObject().put("_array", JSONArray(rawText)).put("ok", response.isSuccessful)
                    } else if (rawText.isNotBlank()) {
                        JSONObject(rawText)
                    } else {
                        JSONObject().put("ok", response.isSuccessful)
                    }
                } catch (_: Exception) {
                    if (!response.isSuccessful) {
                        throw ApiException("服务器返回异常 (${response.code})", httpCode = response.code)
                    }
                    throw ApiException("服务器响应格式无效")
                }

                if (response.code == 401 && overrideBaseUrl == null && overrideToken == null && token.isNotBlank()) {
                    onUnauthorized?.invoke(targetBase, token)
                }

                if (!response.isSuccessful || (json.has("ok") && !json.optBoolFlex("ok", true))) {
                    val errMsg = normalizeMessage(
                        json.optStringClean("errorMessage")
                            .ifBlank { json.optStringClean("error") }
                            .ifBlank { json.optStringClean("message") }
                            .ifBlank { "请求失败 (${response.code})" }
                    )
                    throw ApiException(
                        message = errMsg,
                        httpCode = response.code,
                        errorCode = json.optStringClean("errorCode").ifBlank { json.optStringClean("code") },
                        errorType = json.optStringClean("errorType"),
                        remainingMs = json.optLongFlex("remainingMs"),
                    )
                }
                json
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw ApiException("无法连接到服务器，请检查地址与网络 (${e.localizedMessage ?: "网络错误"})")
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
        val json = executeJson("GET", "/api/wx-login/tasks/${encodeQuery(taskId)}/status", client = longPollClient)
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
        val json = executeJson("POST", "/api/qq-login/tasks/${encodeQuery(taskId)}/status", client = longPollClient)
        return JsonParsers.parseQqQrTask(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun claimQqQrCode(taskId: String): String {
        val json = executeJson("POST", "/api/qq-login/tasks/${encodeQuery(taskId)}/code", client = longPollClient)
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
        val json = executeJson("POST", "/api/ranch/${encodeQuery(action)}", body = body, accountIdOverride = accountId, client = longPollClient)
        val data = json.optJSONObject("data") ?: JSONObject()
        val notice = data.optStringClean("notice")
        if (notice.isNotBlank()) return notice
        return if (data.optStringClean("status") == "skipped") "当前没有可操作项目" else "操作完成"
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
        val json = executeJson("GET", path, accountIdOverride = accountId, client = longPollClient)
        return JsonParsers.parseFriendsPayload(json)
    }

    suspend fun fetchFriendLands(accountId: String, friendGid: Long): FriendLandsPayload {
        val json = executeJson("GET", "/api/friend/$friendGid/lands", accountIdOverride = accountId, client = longPollClient)
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
                client = longPollClient,
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

    // ---- Automation, Strategy & Account Logs ----

    suspend fun fetchSchedulerStatus(accountId: String): SchedulerSummary {
        val json = executeJson("GET", "/api/scheduler", accountIdOverride = accountId)
        return JsonParsers.parseSchedulerSummary(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun fetchRanchAutomationOptions(): List<RanchAnimalOption> {
        val json = executeJson("GET", "/api/ranch/automation-options")
        return JsonParsers.parseRanchAutomationOptions(json.optJSONArray("data"))
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
        } catch (e: org.json.JSONException) {
            throw ApiException("背包种子数据格式有误：${e.message}")
        }
    }

    suspend fun fetchSeedCatalog(accountId: String): List<SeedOption> {
        val json = executeJson("GET", "/api/seeds", accountIdOverride = accountId)
        return JsonParsers.parseSeedCatalog(json.optJSONArray("data"))
    }

    suspend fun fetchAccountSettings(accountId: String): AccountSettingsModel {
        val json = executeJson("GET", "/api/settings", accountIdOverride = accountId)
        return JsonParsers.parseAccountSettings(json.optJSONObject("data") ?: JSONObject())
    }

    suspend fun saveAccountSettings(accountId: String, model: AccountSettingsModel, baseline: AccountSettingsModel): Pair<Boolean, String> {
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
        automation.put("fertilizer_smart_seconds", model.fertilizerSmartSeconds.coerceIn(30, 3600))
        automation.put("fertilizer_land_types", JSONArray().apply {
            model.fertilizerLandTypes.forEach { put(it) }
        })
        automation.put("ranch_strategy", model.ranchStrategy)
        automation.put("ranch_bag_fallback", model.ranchBagFallback)
        fun putPriorityChange(key: String, before: List<Int>, after: List<Int>) {
            if (before == after) automation.remove(key) else automation.put(key, JSONArray(after))
        }
        putPriorityChange("ranch_coop_cub_priority", baseline.ranchCoopCubPriority, model.ranchCoopCubPriority)
        putPriorityChange("ranch_shed_cub_priority", baseline.ranchShedCubPriority, model.ranchShedCubPriority)
        automation.put("ranch_coop_animal", model.ranchCoopAnimal)
        automation.put("ranch_shed_animal", model.ranchShedAnimal)
        automation.put("ranch_grass_threshold_percent", model.ranchGrassThresholdPercent.coerceIn(1, 100))
        automation.put("ranch_check_minutes", model.ranchCheckMinutes.coerceIn(1, 60))
        for ((k, v) in model.automationBooleans) {
            automation.put(k, v)
        }
        fun putCanStageChange(key: String, before: String?, after: String?) {
            if (after == null || before == after) automation.remove(key) else automation.put(key, after)
        }
        putCanStageChange("ranch_feed_normal_stage", baseline.ranchFeedNormalStage, model.ranchFeedNormalStage)
        putCanStageChange("ranch_feed_organic_stage", baseline.ranchFeedOrganicStage, model.ranchFeedOrganicStage)

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
            .put("stealDelaySeconds", model.stealDelaySeconds.coerceIn(0, 300))
            .put("plantOrderRandom", model.plantOrderRandom)
            .put("plantDelaySeconds", model.plantDelaySeconds.coerceIn(0, 60))
            .put("fertilizerBuyOrganicCount", model.fertilizerBuyOrganicCount.coerceIn(0, 10000))
            .put("fertilizerBuyOrganicThresholdHours", model.fertilizerBuyOrganicThresholdHours.coerceIn(0, 990))
            .put("fertilizerBuyNormalCount", model.fertilizerBuyNormalCount.coerceIn(0, 10000))
            .put("fertilizerBuyNormalThresholdHours", model.fertilizerBuyNormalThresholdHours.coerceIn(0, 990))
            .put("fertilizerBuyCheckIntervalMinutes", model.fertilizerBuyCheckIntervalMinutes.coerceIn(1, 1440))
            .put("autoAcceptFriendMinLevel", model.autoAcceptFriendMinLevel.coerceIn(0, 200))
            .put("autoAcceptRequireOwnLevel", model.autoAcceptRequireOwnLevel)
            .put("autoAcceptHarvestStealEnabled", model.autoAcceptHarvestStealEnabled)
            .put("autoAcceptHarvestStealHarvest", model.autoAcceptHarvestStealHarvest.coerceIn(0, 9999))
            .put("autoAcceptHarvestStealSteal", model.autoAcceptHarvestStealSteal.coerceIn(1, 9999))
            .put("automation", automation)
        if (model.bagSeedPriority != baseline.bagSeedPriority) payload.put("bagSeedPriority", JSONArray(model.bagSeedPriority))
        model.emailClaimHour?.let { payload.put("emailClaimHour", it.coerceIn(0, 23)) }

        try {
            executeJson("POST", "/api/settings/save", body = payload, accountIdOverride = accountId)
        } catch (e: ApiException) {
            if (e.httpCode == 202) return false to e.message
            throw e
        }
        return true to "自动化设置已保存"
    }

    suspend fun fetchAccountLogs(limit: Int = 50): List<AccountLogEvent> {
        val json = executeJson("GET", "/api/account-logs?limit=$limit")
        val arr = json.optJSONArray("_array") ?: json.optJSONArray("data")
        return JsonParsers.parseAccountLogs(arr)
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
            executeJson("GET", "/api/ranch?${params.joinToString("&")}", accountIdOverride = accountId, client = ranchHttpClient)
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
            client = ranchHttpClient,
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
            client = ranchHttpClient,
            requestSession = requestSession,
        )
        val result = FeatureJsonParsers.parseRanchOperateResult(json.optJSONObject("data") ?: JSONObject())
        if (result.status != "confirmed" && result.status != "skipped") {
            throw ApiException("牧场操作结果未确认，请刷新核对", errorCode = "RANCH_RESULT_UNKNOWN")
        }
        return result
    }

    suspend fun executeQuotedRanchAction(accountId: String, action: String, input: JSONObject): RanchOperateResult {
        val requestSession = session
        val quote = quoteRanchAction(accountId, action, input, requestSession)
        if (quote.skipped) return RanchOperateResult(status = "skipped", notice = quote.message)
        if (session != requestSession) throw ApiException("服务器或账号已切换，请重新发起操作")
        return executeRanchAction(accountId, action, input, quote.signature, requestSession)
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

    suspend fun operateCharity(accountId: String, actionPath: String, body: JSONObject = JSONObject()): String {
        val json = executeJson(
            "POST",
            "/api/activity-center/charity-red-flower/$actionPath",
            body = body,
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
        val json = executeJson("GET", "/api/illustrated", accountIdOverride = accountId, client = longPollClient)
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回图鉴数据")
        return FeatureJsonParsers.parseIllustratedSnapshot(data)
    }

    suspend fun claimIllustrated(accountId: String, type: Int): IllustratedClaimResult {
        val body = JSONObject().put("type", type)
        val json = executeJson("POST", "/api/illustrated/claim", body = body, accountIdOverride = accountId, client = longPollClient)
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
        val json = executeJson("POST", "/api/system/update/check", client = systemUpdateHttpClient)
        val data = json.optJSONObject("data") ?: throw ApiException("服务端未返回检查结果")
        return FeatureJsonParsers.parseSystemUpdateStatus(data)
    }

    suspend fun pullSystemUpdates(): SystemUpdatePullResult {
        val json = executeJson("POST", "/api/system/update/pull", client = systemUpdateHttpClient)
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
            client = systemUpdateHttpClient,
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
            client = systemUpdateHttpClient,
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
        val json = executeJson("POST", path, body = body, client = systemUpdateHttpClient, requestSession = target)
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
}
