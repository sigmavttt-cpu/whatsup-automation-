package com.whatsup.automation.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.whatsup.automation.data.remote.update.ResumableApkDownloader
import com.whatsup.automation.data.remote.update.SupabaseUpdateApi
import com.whatsup.automation.data.service.GracefulTeardownManager
import com.whatsup.automation.domain.model.AbiIncompatibleException
import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.model.UpdateDownloadProgress
import com.whatsup.automation.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * التنفيذ الفعلي لمستودع التحديثات المؤسسي الشامل (Update Repository Implementation).
 */
@Singleton
class UpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseUpdateApi: SupabaseUpdateApi,
    private val resumableApkDownloader: ResumableApkDownloader,
    private val gracefulTeardownManager: GracefulTeardownManager
) : UpdateRepository {

    companion object {
        private const val TAG = "UpdateRepositoryImpl"
        private const val UPDATES_DIR_NAME = "updates"
    }

    private fun getUpdatesDir(): File {
        // نفضل التخزين الخارجي الخاص بالتطبيق لضمان بقاء الصلاحية متاحة لنظام التثبيت حتى عند إغلاق التطبيق
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = if (externalDir != null && (externalDir.exists() || externalDir.mkdirs())) {
            File(externalDir, UPDATES_DIR_NAME)
        } else {
            File(context.filesDir, UPDATES_DIR_NAME)
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return targetDir
    }

    override suspend fun checkForUpdate(
        channel: String,
        currentVersionCode: Int,
        currentAbi: String
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseUpdateApi.fetchActiveUpdates(channel = channel)
            if (result.isSuccess) {
                val updates = result.getOrNull().orEmpty()
                val supportedAbis = Build.SUPPORTED_ABIS.toList()

                // التحديثات النشطة ذات الإصدار الأعلى من الإصدار الحالي
                val newerUpdates = updates
                    .filter { it.isActive }
                    .filter { it.versionCode > currentVersionCode }

                // البحث عن أحدث إصدار متوافق مع معمارية الجهاز
                val latestMatchingCandidate = newerUpdates
                    .filter { it.matchesAbi(supportedAbis) }
                    .maxByOrNull { it.versionCode }

                if (latestMatchingCandidate != null) {
                    Log.d(TAG, "Update check result: Compatible candidate found = ${latestMatchingCandidate.versionName} (v${latestMatchingCandidate.versionCode})")
                    Result.success(latestMatchingCandidate)
                } else if (newerUpdates.isNotEmpty()) {
                    // يوجد تحديث جديد ولكن معمارية الجهاز غير متوافقة!
                    val incompatibleCandidate = newerUpdates.maxByOrNull { it.versionCode }!!
                    val targetAbi = incompatibleCandidate.targetAbi ?: "غير معروف"
                    Log.w(TAG, "Update available (v${incompatibleCandidate.versionName}) but ABI incompatible. Target=$targetAbi, Device=$supportedAbis")
                    Result.failure(AbiIncompatibleException(
                        versionName = incompatibleCandidate.versionName,
                        versionCode = incompatibleCandidate.versionCode,
                        targetAbi = targetAbi,
                        deviceAbis = supportedAbis
                    ))
                } else {
                    Log.d(TAG, "Update check result: Application is up to date.")
                    Result.success(null)
                }
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown update check failure"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking for update: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun downloadUpdate(
        updateInfo: AppUpdateInfo,
        useMirror: Boolean
    ): Flow<UpdateDownloadProgress> {
        val destinationDir = getUpdatesDir()
        return resumableApkDownloader.download(
            updateInfo = updateInfo,
            destinationDir = destinationDir,
            useMirror = useMirror
        )
    }

    override suspend fun verifyApkChecksum(file: File, expectedSha256: String): Boolean = withContext(Dispatchers.IO) {
        resumableApkDownloader.verifySha256(file, expectedSha256)
    }

    override suspend fun performGracefulTeardown(): Result<Unit> {
        return gracefulTeardownManager.executeTeardown()
    }

    override fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    override fun launchInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch unknown sources settings: ${e.message}")
            }
        }
    }

    override suspend fun launchApkInstallation(apkFile: File): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return@withContext Result.failure(Exception("ملف الحزمة غير موجود أو تالف."))
            }

            // فحص سلامة وتطابق الحزمة والشهادة قبل استدعاء مثبت النظام
            val integrityResult = verifyApkPackageIntegrity(apkFile)
            if (integrityResult.isFailure) {
                return@withContext Result.failure(integrityResult.exceptionOrNull() ?: Exception("فشل فحص سلامة الحزمة"))
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // منح صلاحيات القراءة الصريحة لكافة معالجي التثبيت على مستوى النظام لتجنب حظر مثبت الحزم
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    installIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo?.packageName
                if (!pkgName.isNullOrBlank()) {
                    context.grantUriPermission(pkgName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            Log.i(TAG, "Launching APK installer for: ${apkFile.absolutePath} via URI: $apkUri")
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun verifyApkPackageIntegrity(apkFile: File): Result<Unit> {
        try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                ?: return Result.failure(Exception("تعذر تحليل ملف التثبيت (Parse Error)؛ قد يكون الملف غير مكتمل أو تالفاً."))

            // 1. فحص اسم الحزمة (Package Name)
            if (archiveInfo.packageName != context.packageName) {
                return Result.failure(Exception("اسم حزمة التحديث (${archiveInfo.packageName}) لا يطابق التطبيق الحالي (${context.packageName})."))
            }

            // 2. فحص البصمة والتوقيع (Signature Matching) لتفادي خطأ INSTALL_FAILED_UPDATE_INCOMPATIBLE
            val currentPkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, flags)
            }

            val signaturesMatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val archiveSigning = archiveInfo.signingInfo
                val currentSigning = currentPkgInfo.signingInfo
                if (archiveSigning != null && currentSigning != null) {
                    val archiveSignatures = if (archiveSigning.hasMultipleSigners()) {
                        archiveSigning.apkContentsSigners.map { it.toCharsString() }.toSet()
                    } else {
                        archiveSigning.signingCertificateHistory.map { it.toCharsString() }.toSet()
                    }
                    val currentSignatures = if (currentSigning.hasMultipleSigners()) {
                        currentSigning.apkContentsSigners.map { it.toCharsString() }.toSet()
                    } else {
                        currentSigning.signingCertificateHistory.map { it.toCharsString() }.toSet()
                    }
                    archiveSignatures.intersect(currentSignatures).isNotEmpty()
                } else {
                    true
                }
            } else {
                @Suppress("DEPRECATION")
                val archiveSignatures = archiveInfo.signatures?.map { it.toCharsString() }?.toSet().orEmpty()
                @Suppress("DEPRECATION")
                val currentSignatures = currentPkgInfo.signatures?.map { it.toCharsString() }?.toSet().orEmpty()
                if (archiveSignatures.isNotEmpty() && currentSignatures.isNotEmpty()) {
                    archiveSignatures.intersect(currentSignatures).isNotEmpty()
                } else {
                    true
                }
            }

            if (!signaturesMatch) {
                return Result.failure(Exception("شهادة التوقيع للنسخة الجديدة تختلف عن النسخة المثبتة حالياً. تم حظر التثبيت لحماية بياناتك من التلف."))
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Signature integrity warning: ${e.message}")
            return Result.success(Unit)
        }
    }

    override fun getCachedApkIfValid(updateInfo: AppUpdateInfo): File? {
        val destinationDir = getUpdatesDir()
        val safeFileName = "whatsup_v${updateInfo.versionCode}_${updateInfo.versionName}.apk"
        val finalApkFile = File(destinationDir, safeFileName)

        return if (finalApkFile.exists() && finalApkFile.length() > 0) {
            if (resumableApkDownloader.verifySha256(finalApkFile, updateInfo.apkSha256)) {
                finalApkFile
            } else {
                null
            }
        } else {
            null
        }
    }

    override fun cleanupOldUpdateFiles() {
        try {
            val now = System.currentTimeMillis()
            val threeDaysAgo = 3 * 24 * 60 * 60 * 1000L

            // تنظيف المجلد الأساسي
            val dirsToClean = listOfNotNull(
                getUpdatesDir(),
                File(context.filesDir, UPDATES_DIR_NAME),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, UPDATES_DIR_NAME) }
            ).distinctBy { it.absolutePath }

            for (dir in dirsToClean) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (now - file.lastModified() > threeDaysAgo) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up old update files: ${e.message}")
        }
    }
}
