package com.whatsup.automation.presentation.screens.update

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.domain.model.*
import com.whatsup.automation.domain.repository.UpdateRepository
import com.whatsup.automation.domain.usecase.CheckForUpdateUseCase
import com.whatsup.automation.domain.usecase.DownloadUpdateUseCase
import com.whatsup.automation.domain.usecase.GracefulTeardownUseCase
import com.whatsup.automation.domain.usecase.InstallUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * مدير حالة التحديثات المؤسسي (Update ViewModel).
 * يدير تدفق فحص وتنزيل وتثبيت التحديثات مع الحصانة الكاملة والتعافي من الأخطاء.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    application: Application,
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val downloadUpdateUseCase: DownloadUpdateUseCase,
    private val gracefulTeardownUseCase: GracefulTeardownUseCase,
    private val installUpdateUseCase: InstallUpdateUseCase,
    private val updateRepository: UpdateRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null
    private var currentActiveUpdateInfo: AppUpdateInfo? = null

    val currentVersionCode: Int
        get() = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            10 // Default fallback versionCode
        }

    val currentVersionName: String
        get() = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
            pInfo.versionName ?: "8.1.0"
        } catch (_: Exception) {
            "8.1.0"
        }

    init {
        // تنظيف الملفات القديمة في الخلفية عند بدء التشغيل
        viewModelScope.launch {
            updateRepository.cleanupOldUpdateFiles()
        }
    }

    /**
     * فحص وجود تحديث جديد للتطبيق.
     */
    fun checkForUpdates(isManual: Boolean = false) {
        if (_uiState.value is UpdateUiState.Checking || _uiState.value is UpdateUiState.Downloading) {
            return
        }

        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            val currentCode = currentVersionCode
            val currentName = currentVersionName
            val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

            val result = checkForUpdateUseCase.execute(
                channel = "stable",
                currentVersionCode = currentCode,
                currentAbi = primaryAbi
            )

            if (result.isSuccess) {
                val candidate = result.getOrNull()
                if (candidate != null && candidate.versionCode > currentCode) {
                    currentActiveUpdateInfo = candidate
                    val isMandatory = candidate.isMandatoryUpdate(currentCode)
                    val cachedApk = updateRepository.getCachedApkIfValid(candidate)

                    if (cachedApk != null) {
                        _uiState.value = UpdateUiState.ReadyToInstall(
                            updateInfo = candidate,
                            apkFile = cachedApk,
                            isMandatory = isMandatory
                        )
                    } else {
                        _uiState.value = UpdateUiState.UpdateAvailable(
                            updateInfo = candidate,
                            isMandatory = isMandatory,
                            currentVersionCode = currentCode,
                            currentVersionName = currentName,
                            cachedApkReady = false
                        )
                    }
                } else {
                    _uiState.value = UpdateUiState.UpToDate(
                        currentVersionCode = currentCode,
                        currentVersionName = currentName
                    )
                }
            } else {
                val exc = result.exceptionOrNull()
                val error = exc?.localizedMessage ?: "فشل الاتصال بخادم التحديثات"
                if (isManual || exc is AbiIncompatibleException) {
                    _uiState.value = UpdateUiState.Failed(
                        errorMessage = error,
                        canRetry = exc !is AbiIncompatibleException,
                        updateInfo = null,
                        isMandatory = false
                    )
                } else {
                    _uiState.value = UpdateUiState.Idle
                }
            }
        }
    }

    /**
     * بدء أو استئناف تنزيل حزمة التحديث.
     */
    fun startDownload(useMirror: Boolean = false) {
        val updateInfo = currentActiveUpdateInfo
            ?: (_uiState.value as? UpdateUiState.UpdateAvailable)?.updateInfo
            ?: (_uiState.value as? UpdateUiState.Downloading)?.updateInfo
            ?: (_uiState.value as? UpdateUiState.ReadyToInstall)?.updateInfo
            ?: (_uiState.value as? UpdateUiState.Failed)?.updateInfo
            ?: return

        currentActiveUpdateInfo = updateInfo
        val isMandatory = updateInfo.isMandatoryUpdate(currentVersionCode)

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.value = UpdateUiState.Downloading(
                updateInfo = updateInfo,
                bytesDownloaded = 0L,
                totalBytes = updateInfo.apkSizeBytes,
                progressPercent = 0f,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
                isPaused = false,
                isMandatory = isMandatory
            )

            downloadUpdateUseCase.execute(updateInfo, useMirror = useMirror).collect { progress ->
                when (progress) {
                    is UpdateDownloadProgress.Progress -> {
                        _uiState.value = UpdateUiState.Downloading(
                            updateInfo = updateInfo,
                            bytesDownloaded = progress.bytesDownloaded,
                            totalBytes = progress.totalBytes,
                            progressPercent = progress.progressPercent,
                            speedBytesPerSec = progress.speedBytesPerSec,
                            etaSeconds = progress.etaSeconds,
                            isPaused = false,
                            isMandatory = isMandatory
                        )
                    }
                    is UpdateDownloadProgress.VerifyingChecksum -> {
                        _uiState.value = UpdateUiState.VerifyingChecksum(
                            updateInfo = updateInfo,
                            isMandatory = isMandatory
                        )
                    }
                    is UpdateDownloadProgress.Completed -> {
                        _uiState.value = UpdateUiState.ReadyToInstall(
                            updateInfo = updateInfo,
                            apkFile = progress.apkFile,
                            isMandatory = isMandatory
                        )
                    }
                    is UpdateDownloadProgress.Error -> {
                        _uiState.value = UpdateUiState.Failed(
                            errorMessage = progress.message,
                            canRetry = progress.canRetry,
                            canUseMirror = progress.canUseMirror,
                            updateInfo = updateInfo,
                            isMandatory = isMandatory
                        )
                    }
                }
            }
        }
    }

    /**
     * تنفيذ بروتوكول التهدئة الآمنة (Graceful Teardown) ثم إطلاق تثبيت الـ APK.
     */
    fun proceedWithTeardownAndInstall(apkFile: File) {
        val updateInfo = currentActiveUpdateInfo
            ?: (_uiState.value as? UpdateUiState.ReadyToInstall)?.updateInfo
            ?: (_uiState.value as? UpdateUiState.PreparingTeardown)?.updateInfo
            ?: (_uiState.value as? UpdateUiState.Installing)?.updateInfo
            ?: return

        currentActiveUpdateInfo = updateInfo
        val isMandatory = updateInfo.isMandatoryUpdate(currentVersionCode)

        // التحقق أولاً من منح إذن تثبيت التطبيقات المجهولة قبل إيقاف الخدمات
        if (!canInstallPackages()) {
            Log.w(TAG, "Install permission not granted, redirecting to system settings first.")
            openInstallPermissionSettings()
            return
        }

        viewModelScope.launch {
            _uiState.value = UpdateUiState.PreparingTeardown(
                updateInfo = updateInfo,
                stage = "جارٍ حفظ البيانات وإيقاف الخدمات بأمان...",
                apkFile = apkFile,
                isMandatory = isMandatory
            )

            val teardownResult = gracefulTeardownUseCase.execute()
            if (teardownResult.isSuccess) {
                _uiState.value = UpdateUiState.Installing(
                    updateInfo = updateInfo,
                    apkFile = apkFile,
                    isMandatory = isMandatory
                )
                val installResult = installUpdateUseCase.execute(apkFile)
                if (installResult.isFailure) {
                    _uiState.value = UpdateUiState.Failed(
                        errorMessage = "تعذر بدء تثبيت الحزمة تلقائياً: ${installResult.exceptionOrNull()?.localizedMessage}",
                        canRetry = true,
                        canUseMirror = false,
                        updateInfo = updateInfo,
                        isMandatory = isMandatory
                    )
                }
            } else {
                Log.w(TAG, "Teardown had warnings, proceeding to install anyway...")
                _uiState.value = UpdateUiState.Installing(
                    updateInfo = updateInfo,
                    apkFile = apkFile,
                    isMandatory = isMandatory
                )
                val installResult = installUpdateUseCase.execute(apkFile)
                if (installResult.isFailure) {
                    _uiState.value = UpdateUiState.Failed(
                        errorMessage = "تعذر بدء تثبيت الحزمة تلقائياً: ${installResult.exceptionOrNull()?.localizedMessage}",
                        canRetry = true,
                        canUseMirror = false,
                        updateInfo = updateInfo,
                        isMandatory = isMandatory
                    )
                }
            }
        }
    }

    /**
     * التحقق من صلاحية canRequestPackageInstalls.
     */
    fun canInstallPackages(): Boolean = installUpdateUseCase.canInstallPackages()

    /**
     * فتح صفحة إعدادات النظام لتمكين تثبيت التطبيقات.
     */
    fun openInstallPermissionSettings() {
        installUpdateUseCase.launchInstallPermissionSettings()
    }

    /**
     * إغلاق نافذة التحديث الاختياري.
     */
    fun dismissOptionalUpdate() {
        if (_uiState.value is UpdateUiState.UpdateAvailable) {
            val state = _uiState.value as UpdateUiState.UpdateAvailable
            if (!state.isMandatory) {
                _uiState.value = UpdateUiState.Idle
            }
        } else if (_uiState.value is UpdateUiState.UpToDate || _uiState.value is UpdateUiState.Failed) {
            _uiState.value = UpdateUiState.Idle
        }
    }
}
