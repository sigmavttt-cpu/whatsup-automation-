package com.whatsup.automation.domain.model

import java.io.File

/**
 * حالة تدفق تنزيل التحديثات القطاعي المجزأ (Resumable Download Progress).
 */
sealed class UpdateDownloadProgress {
    data class Progress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Float,
        val speedBytesPerSec: Long,
        val etaSeconds: Long
    ) : UpdateDownloadProgress() {
        fun getFormattedSpeed(): String {
            val kbSec = speedBytesPerSec / 1024.0
            val mbSec = kbSec / 1024.0
            return if (mbSec >= 1.0) {
                String.format(java.util.Locale.US, "%.2f MB/s", mbSec)
            } else {
                String.format(java.util.Locale.US, "%.0f KB/s", kbSec)
            }
        }

        fun getFormattedProgress(): String {
            val downloadedMb = bytesDownloaded / (1024.0 * 1024.0)
            val totalMb = totalBytes / (1024.0 * 1024.0)
            return String.format(java.util.Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb)
        }
    }

    data class VerifyingChecksum(
        val apkFile: File,
        val expectedSha256: String
    ) : UpdateDownloadProgress()

    data class Completed(
        val apkFile: File,
        val sha256Verified: Boolean
    ) : UpdateDownloadProgress()

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val canUseMirror: Boolean = false
    ) : UpdateDownloadProgress()
}

/**
 * حالة واجهة المستخدم لشاشة ودرع التحديثات (Update UI State).
 */
sealed class UpdateUiState {
    object Idle : UpdateUiState()

    object Checking : UpdateUiState()

    data class UpToDate(
        val currentVersionCode: Int,
        val currentVersionName: String,
        val checkedAtMillis: Long = System.currentTimeMillis()
    ) : UpdateUiState()

    data class UpdateAvailable(
        val updateInfo: AppUpdateInfo,
        val isMandatory: Boolean,
        val currentVersionCode: Int,
        val currentVersionName: String,
        val cachedApkReady: Boolean = false
    ) : UpdateUiState()

    data class Downloading(
        val updateInfo: AppUpdateInfo,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Float,
        val speedBytesPerSec: Long,
        val etaSeconds: Long,
        val isPaused: Boolean = false,
        val isMandatory: Boolean
    ) : UpdateUiState()

    data class VerifyingChecksum(
        val updateInfo: AppUpdateInfo,
        val isMandatory: Boolean
    ) : UpdateUiState()

    data class ReadyToInstall(
        val updateInfo: AppUpdateInfo,
        val apkFile: File,
        val isMandatory: Boolean
    ) : UpdateUiState()

    data class PreparingTeardown(
        val updateInfo: AppUpdateInfo,
        val stage: String,
        val apkFile: File,
        val isMandatory: Boolean
    ) : UpdateUiState()

    data class Installing(
        val updateInfo: AppUpdateInfo,
        val apkFile: File,
        val isMandatory: Boolean
    ) : UpdateUiState()

    data class Failed(
        val errorMessage: String,
        val canRetry: Boolean = true,
        val canUseMirror: Boolean = false,
        val updateInfo: AppUpdateInfo?,
        val isMandatory: Boolean
    ) : UpdateUiState()
}
