package com.qqfarm.mobile.data.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val UPDATE_REPOSITORY = "ccpopy/qq-farm-bot"
private const val GITHUB_API = "https://api.github.com/repos/$UPDATE_REPOSITORY"
private const val GITHUB_RELEASES = "https://github.com/$UPDATE_REPOSITORY/releases"
private const val TAG_PREFIX = "android-v"

data class AndroidRelease(
    val version: String,
    val notes: String,
    val assetId: Long,
    val size: Long,
    val sha256: String,
    val requiresAuthorization: Boolean,
) {
    val apkUrl: String get() = "$GITHUB_RELEASES/download/$TAG_PREFIX$version/qq-farm-mobile-$version-release.apk"
    val canAccelerate: Boolean get() = !requiresAuthorization && sha256.isNotEmpty()
}

internal class UpdateException(message: String, val authorizationRequired: Boolean = false) : IOException(message)

private data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
}

private val releaseVersionPattern = Regex("[0-9]+\\.[0-9]+\\.[0-9]+")
private val sha256Pattern = Regex("sha256:[0-9a-fA-F]{64}")

private fun parseReleaseVersion(value: String): ReleaseVersion? {
    if (!releaseVersionPattern.matches(value)) return null
    val parts = value.split('.').map { it.toIntOrNull() ?: return null }
    return ReleaseVersion(parts[0], parts[1], parts[2])
}

internal fun selectAndroidUpdate(
    releases: JSONArray,
    installedVersion: String,
    requiresAuthorization: Boolean,
): AndroidRelease? {
    var newestVersion = parseReleaseVersion(installedVersion) ?: throw UpdateException("当前应用版本号无效")
    var newestRelease: AndroidRelease? = null
    for (index in 0 until releases.length()) {
        val release = releases.optJSONObject(index) ?: continue
        if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
        val tag = release.optString("tag_name")
        if (!tag.startsWith(TAG_PREFIX)) continue
        val version = tag.removePrefix(TAG_PREFIX)
        val parsedVersion = parseReleaseVersion(version) ?: continue
        if (parsedVersion <= newestVersion) continue
        val assets = release.optJSONArray("assets") ?: continue
        val asset = (0 until assets.length()).asSequence().mapNotNull(assets::optJSONObject).firstOrNull {
            it.optString("name") == "qq-farm-mobile-$version-release.apk" && it.optString("state") == "uploaded"
        } ?: continue

        val rawDigest = if (asset.isNull("digest")) "" else asset.optString("digest").trim()
        newestVersion = parsedVersion
        newestRelease = AndroidRelease(
            version = version,
            notes = if (release.isNull("body")) "" else release.optString("body"),
            assetId = asset.optLong("id"),
            size = asset.optLong("size"),
            sha256 = rawDigest,
            requiresAuthorization = requiresAuthorization,
        )
    }
    if (newestRelease != null && (
            newestRelease.assetId <= 0 ||
            newestRelease.size <= 0 ||
            (newestRelease.sha256.isNotEmpty() && !sha256Pattern.matches(newestRelease.sha256))
        )
    ) {
        throw UpdateException("GitHub Release 缺少有效的安装包校验信息")
    }
    return newestRelease
}

internal class AppUpdateClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun check(installedVersion: String, token: String?, proxyUrl: String? = null): AndroidRelease? {
        val request = githubRequest("releases?per_page=100", "application/vnd.github+json", token)
        return read(request, proxyUrl) { response ->
            if (!response.isSuccessful) throw githubFailure(response)
            val raw = response.body?.string()
            val releases = try {
                JSONArray(raw ?: "")
            } catch (_: JSONException) {
                throw UpdateException("GitHub 返回的发布信息无效")
            }
            selectAndroidUpdate(releases, installedVersion, requiresAuthorization = token != null)
        }
    }

    suspend fun download(
        release: AndroidRelease,
        accelerator: String?,
        token: String?,
        cacheDir: File,
        proxyUrl: String? = null,
        onProgress: (Int) -> Unit,
    ): File {
        val request = when {
            release.requiresAuthorization -> githubRequest("releases/assets/${release.assetId}", "application/octet-stream", token)
            proxyUrl == null && accelerator != null && release.canAccelerate -> Request.Builder()
                .url("$accelerator/${release.apkUrl}")
                .header("User-Agent", "FarmAndroid")
                .build()
            else -> Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", "FarmAndroid")
                .build()
        }
        return read(request, proxyUrl) { response ->
            if (!response.isSuccessful) {
                if (release.requiresAuthorization) throw githubFailure(response)
                throw UpdateException("下载失败（HTTP ${response.code}）")
            }
            val body = response.body ?: throw UpdateException("安装包为空")
            val directory = File(cacheDir, "app-updates").apply { mkdirs() }
            val part = File.createTempFile("download-", ".part", directory)
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var received = 0L
                var progress = 0
                part.outputStream().buffered().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var count = input.read(buffer)
                        while (count != -1) {
                            received += count
                            if (received > release.size) throw UpdateException("下载内容与 GitHub 发布的安装包不一致")
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            val percent = (received * 100 / release.size).toInt()
                            if (percent != progress) {
                                progress = percent
                                onProgress(percent)
                            }
                            count = input.read(buffer)
                        }
                    }
                }
                val sha256 = digest.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
                if (received != release.size || (release.sha256.isNotEmpty() && !release.sha256.equals("sha256:$sha256", ignoreCase = true))) {
                    throw UpdateException("安装包校验失败，请重新下载")
                }
                val apk = File(directory, "farm-android-update.apk")
                Files.move(part.toPath(), apk.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                apk
            } finally {
                part.delete()
            }
        }
    }

    private fun githubRequest(path: String, accept: String, token: String?): Request =
        Request.Builder()
            .url("$GITHUB_API/$path")
            .header("Accept", accept)
            .header("X-GitHub-Api-Version", "2026-03-10")
            .header("User-Agent", "FarmAndroid")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

    private fun githubFailure(response: Response): UpdateException = when {
        response.code == 401 || response.code == 404 ->
            UpdateException("需要有权限读取当前仓库的 GitHub Token", authorizationRequired = true)
        response.code == 403 && response.header("X-RateLimit-Remaining") == "0" ->
            UpdateException("GitHub 检查次数达到限制，请稍后重试")
        else -> UpdateException("GitHub 请求失败（HTTP ${response.code}）")
    }

    private suspend fun <T> read(request: Request, proxyUrl: String?, consume: (Response) -> T): T {
        val routeClient = if (proxyUrl != null) {
            val address = normalizeUpdateProxy(proxyUrl) ?: throw UpdateException("代理地址无效，请检查协议、主机和端口")
            val uri = URI(address)
            val type = if (uri.scheme == "socks5") Proxy.Type.SOCKS else Proxy.Type.HTTP
            val proxy = Proxy(type, InetSocketAddress.createUnresolved(uri.host.removeSurrounding("[", "]"), uri.port))
            client.newBuilder().proxy(proxy).build()
        } else {
            client
        }
        return suspendCancellableCoroutine { continuation ->
            val call = routeClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resumeWith(runCatching { response.use(consume) })
                }
            })
        }
    }
}
