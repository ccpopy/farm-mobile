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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Authorization(val message: String) : UpdateUiState
    data class Notice(val title: String, val message: String) : UpdateUiState
    data class Available(
        val release: AndroidRelease,
        val accelerator: String?,
        val useAcceleration: Boolean,
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
        if (enteredToken != null) {
            val token = enteredToken.trim()
            if (token.isEmpty() || token.any { it !in '!'..'~' }) {
                _state.value = UpdateUiState.Authorization("请粘贴完整的 GitHub Token")
                return
            }
            githubToken = token
        }
        _state.value = UpdateUiState.Checking
        task = viewModelScope.launch {
            try {
                val release = updates.check(BuildConfig.VERSION_NAME, githubToken)
                _state.value = if (release == null) {
                    UpdateUiState.Notice("暂无可用更新", "当前版本 ${BuildConfig.VERSION_NAME}")
                } else {
                    val accelerator = config.updateAcceleratorUrl.takeIf { release.canAccelerate }
                    UpdateUiState.Available(release, accelerator, config.useUpdateAcceleration && accelerator != null)
                }
            } catch (e: UpdateException) {
                if (e.authorizationRequired) {
                    githubToken = null
                    _state.value = UpdateUiState.Authorization(e.message.orEmpty())
                } else {
                    _state.value = UpdateUiState.Notice("检查更新失败", e.message.orEmpty())
                }
            } catch (_: IOException) {
                _state.value = UpdateUiState.Notice("检查更新失败", "连接 GitHub 失败，请检查网络后重试")
            }
        }
    }

    fun download() {
        val update = _state.value as? UpdateUiState.Available ?: return
        val accelerated = update.accelerator.takeIf { update.useAcceleration }
        _state.value = UpdateUiState.Downloading(update)
        task = viewModelScope.launch {
            try {
                val apk = updates.download(update.release, accelerated, githubToken, app.cacheDir) { percent ->
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
                    githubToken = null
                    _state.value = UpdateUiState.Authorization(e.message.orEmpty())
                } else {
                    _state.value = update.copy(error = if (accelerated != null) "${e.message}，可关闭本次加速后重试" else e.message)
                }
            } catch (_: IOException) {
                _state.value = update.copy(error = if (accelerated != null) "加速下载失败，可关闭本次加速后重试" else "下载失败，请检查网络与可用空间")
            }
        }
    }

    fun setDownloadAcceleration(enabled: Boolean) {
        val update = _state.value as? UpdateUiState.Available ?: return
        _state.value = update.copy(useAcceleration = enabled && update.accelerator != null, error = null)
    }

    fun setPreferredAcceleration(enabled: Boolean) {
        viewModelScope.launch { preferences.updateDownloadAcceleration(enabled) }
    }

    fun setAcceleratorUrl(url: String) {
        viewModelScope.launch { preferences.updateAcceleratorUrl(url) }
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
