package com.whatsup.automation.domain.repository

import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.model.UpdateDownloadProgress
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * مستودع التحديثات الشامل (Update Repository Interface).
 */
interface UpdateRepository {
    /**
     * فحص وجود تحديث جديد للتطبيق من خادم Supabase.
     */
    suspend fun checkForUpdate(
        channel: String = "stable",
        currentVersionCode: Int,
        currentAbi: String = "arm64-v8a"
    ): Result<AppUpdateInfo?>

    /**
     * تنزيل حزمة التحديث عبر محرك التحميل القطاعي المجزأ القابل للاستئناف.
     */
    fun downloadUpdate(
        updateInfo: AppUpdateInfo,
        useMirror: Boolean = false
    ): Flow<UpdateDownloadProgress>

    /**
     * التحقق من سلامة وصحة حزمة التحديث ومطابقة بصمة التشفير SHA-256.
     */
    suspend fun verifyApkChecksum(file: File, expectedSha256: String): Boolean

    /**
     * تنفيذ بروتوكول التهدئة الآمنة (Graceful Teardown) للخدمات وقواعد البيانات قبل التثبيت.
     */
    suspend fun performGracefulTeardown(): Result<Unit>

    /**
     * فحص ما إذا كان للمستخدم صلاحية تثبيت التطبيقات المجهولة canRequestPackageInstalls.
     */
    fun canInstallPackages(): Boolean

    /**
     * إطلاق شاشة إعدادات النظام لمنح إذن تثبيت التطبيقات.
     */
    fun launchInstallPermissionSettings()

    /**
     * إطلاق معالج تثبيت حزمة APK عبر FileProvider.
     */
    suspend fun launchApkInstallation(apkFile: File): Result<Unit>

    /**
     * التحقق مما إذا كانت حزمة APK منزلة ومتحققاً منها مسبقاً في الذاكرة المحلية.
     */
    fun getCachedApkIfValid(updateInfo: AppUpdateInfo): File?

    /**
     * تنظيف ملفات APK القديمة لتوفير مساحة التخزين على الجهاز.
     */
    fun cleanupOldUpdateFiles()

    /**
     * فحص سلامة وتطابق الحزمة والشهادة الرقمية (Package Name & Signing Integrity).
     */
    fun verifyApkPackageIntegrity(apkFile: File): Result<Unit>
}
