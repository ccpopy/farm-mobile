package com.qqfarm.mobile.ui.state

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qqfarm.mobile.BuildConfig
import com.qqfarm.mobile.FarmBotApplication
import com.qqfarm.mobile.data.network.AndroidRelease
import com.qqfarm.mobile.data.network.AppUpdateClient
import com.qqfarm.mobile.data.network.UpdateException
import com.qqfarm.mobile.data.prefs.StoredConfig
import com.qqfarm.mobile.data.prefs.UpdateDownloadMode
import com.qqfarm.mobile.data.prefs.UpdateDownloadSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal fun isValidGithubToken(token: String): Boolean = token.isNotEmpty() && token.all { it in '!'..'~' }

internal sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Authorization(val message: String, val prefill: String = "") : UpdateUiState
    data class Notice(val title: String, val message: String) : UpdateUiState
    data class Available(
        val release: AndroidRelease,
        val downloadSettings: UpdateDownloadSettings,
        val error: String? = null,
    ) : UpdateUiState
    data class Downloading(val update: Available, val percent: Int = 0) : UpdateUiState
    data class Ready(val apk: File, val autoInstall: Boolean = true) : UpdateUiState
}

internal class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val preferences = (app as FarmBotApplication).container.preferences
    private val updates = AppUpdateClient()
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state = _state.asStateFlow()

    private var task: Job? = null
    private var githubToken: String? = null

    fun check(config: StoredConfig, enteredToken: String? = null) {
        if (_state.value == UpdateUiState.Checking || _state.value is UpdateUiState.Downloading) return
        val token = (enteredToken ?: config.githubToken).trim()
        if ((enteredToken != null || token.isNotEmpty()) && !isValidGithubToken(token)) {
            _state.value = UpdateUiState.Authorization("请粘贴完整的 GitHub Token", token)
            return
        }
        githubToken = token.ifEmpty { null }
        _state.value = UpdateUiState.Checking
        task = viewModelScope.launch {
            try {
                val release = updates.check(BuildConfig.VERSION_NAME, token.ifEmpty { null }, config.updateDownload.activeProxyUrl)
                if (enteredToken != null && token != config.githubToken) {
                    preferences.updateGithubToken(token)
                }
                _state.value = if (release == null) {
                    UpdateUiState.Notice("暂无可用更新", "当前版本 ${BuildConfig.VERSION_NAME}")
                } else {
                    UpdateUiState.Available(release, config.updateDownload.forRelease(release.canAccelerate))
                }
            } catch (e: UpdateException) {
                if (e.authorizationRequired) {
                    val failed = githubToken
                    githubToken = null
                    val message = if (failed != null && failed == config.githubToken) {
                        "已记住的 GitHub Token 无效或权限不足，请更新后重试"
                    } else {
                        e.message.orEmpty()
                    }
                    _state.value = UpdateUiState.Authorization(message, failed.orEmpty())
                } else {
                    _state.value = UpdateUiState.Notice("检查更新失败", e.message.orEmpty())
                }
            } catch (_: IOException) {
                val message = if (config.updateDownload.mode == UpdateDownloadMode.PROXY) {
                    "无法通过代理连接 GitHub，请核对代理地址"
                } else {
                    "连接 GitHub 失败，可在下载方式中配置代理后重试"
                }
                _state.value = UpdateUiState.Notice("检查更新失败", message)
            }
        }
    }

    fun download() {
        val update = _state.value as? UpdateUiState.Available ?: return
        val settings = update.downloadSettings
        val accelerator = settings.acceleratorUrl.takeIf { settings.mode == UpdateDownloadMode.ACCELERATOR }
        _state.value = UpdateUiState.Downloading(update)
        task = viewModelScope.launch {
            try {
                val apk = updates.download(update.release, accelerator, githubToken, app.cacheDir, settings.activeProxyUrl) { percent ->
                    _state.update { state ->
                        if (state is UpdateUiState.Downloading && state.update === update) {
                            state.copy(percent = percent)
                        } else {
                            state
                        }
                    }
                }
                withContext(Dispatchers.IO) {
                    val archive = app.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
                    if (archive == null ||
                        archive.packageName != app.packageName ||
                        archive.versionName != update.release.version ||
                        PackageInfoCompat.getLongVersionCode(archive) <= BuildConfig.VERSION_CODE
                    ) {
                        apk.delete()
                        throw UpdateException("安装包的应用标识或版本不匹配")
                    }
                }
                _state.value = UpdateUiState.Ready(apk)
            } catch (e: UpdateException) {
                if (e.authorizationRequired) {
                    _state.value = UpdateUiState.Authorization(e.message.orEmpty(), githubToken.orEmpty())
                    githubToken = null
                } else {
                    _state.value = update.copy(error = e.message)
                }
            } catch (_: IOException) {
                val message = when (settings.mode) {
                    UpdateDownloadMode.ACCELERATOR -> "加速下载失败，可切换下载方式后重试"
                    UpdateDownloadMode.PROXY -> "代理下载失败，请核对代理地址后重试"
                    UpdateDownloadMode.DIRECT -> "下载失败，请检查网络与可用空间"
                }
                _state.value = update.copy(error = message)
            }
        }
    }

    fun setDownloadSettings(settings: UpdateDownloadSettings) {
        val update = _state.value as? UpdateUiState.Available ?: return
        _state.value = update.copy(downloadSettings = settings.forRelease(update.release.canAccelerate), error = null)
    }

    /** 在「我的」里修改或清除记住的 GitHub Token；空字符串表示清除。 */
    fun saveGithubToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isNotEmpty() && !isValidGithubToken(trimmed)) return
        githubToken = trimmed.ifEmpty { null }
        viewModelScope.launch { preferences.updateGithubToken(trimmed) }
    }

    fun setPreferredDownloadSettings(settings: UpdateDownloadSettings) {
        viewModelScope.launch { preferences.updateDownloadSettings(settings) }
    }

    fun takeAutomaticInstall(): File? {
        val ready = _state.value as? UpdateUiState.Ready ?: return null
        if (!ready.autoInstall) return null
        _state.value = ready.copy(autoInstall = false)
        return ready.apk
    }

    fun dismiss() {
        task?.cancel()
        _state.value = UpdateUiState.Idle
    }
}
