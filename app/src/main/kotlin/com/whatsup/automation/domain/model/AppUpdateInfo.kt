package com.whatsup.automation.domain.model

/**
 * نموذج بيانات التحديث الشامل المطابق لجدول Supabase: `app_updates`.
 */
data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val minSupportedVersion: Int,
    val updateType: UpdateType,
    val enforcementLevel: EnforcementLevel,
    val gracePeriodDays: Int,
    val apkUrl: String,
    val mirrorApkUrl: String? = null,
    val apkSizeBytes: Long,
    val apkSha256: String,
    val targetAbi: String? = "arm64-v8a",
    val channel: String = "stable",
    val rolloutPercentage: Int = 100,
    val titleAr: String,
    val titleEn: String,
    val changelogAr: String,
    val changelogEn: String,
    val releaseNotes: String? = null,
    val isActive: Boolean = true,
    val publishedAt: String? = null
) {
    /**
     * هل التحديث إجباري يستلزم إظهار الدرع الواقي الكامل وإيقاف عمل التطبيق القديم؟
     */
    fun isMandatoryUpdate(currentVersionCode: Int): Boolean {
        // 1. إذا كان الإصدار الحالي أقل من الحد الأدنى المدعوم
        if (currentVersionCode < minSupportedVersion) return true
        
        // 2. إذا كان نوع التحديث إجباري أو أمني حرج
        if (updateType == UpdateType.MANDATORY || updateType == UpdateType.CRITICAL_SECURITY) return true
        
        // 3. إذا كان مستوى الإلزام إقفال صارم
        if (enforcementLevel == EnforcementLevel.STRICT_BLOCKING) return true
        
        return false
    }

    /**
     * حساب حجم الملف المقروء بصيغة ميجابايت منسقة (مثال: "28.5 MB").
     */
    fun getFormattedSize(): String {
        val mb = apkSizeBytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        } else {
            val kb = apkSizeBytes / 1024.0
            String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }

    /**
     * هل يتطابق المعمار المستهدف (ABI) مع الجهاز الحالي؟
     */
    fun matchesAbi(deviceAbis: List<String>): Boolean {
        if (targetAbi.isNullOrBlank() || targetAbi.equals("all", ignoreCase = true) || targetAbi.equals("universal", ignoreCase = true)) {
            return true
        }
        return deviceAbis.any { it.equals(targetAbi, ignoreCase = true) }
    }
}

/**
 * استثناء يفيد بوجود إصدار أحدث ولكن المعمارية المستهدفة لا تطابق جهاز المستخدم.
 */
class AbiIncompatibleException(
    val versionName: String,
    val versionCode: Int,
    val targetAbi: String,
    val deviceAbis: List<String>
) : Exception("الإصدار v$versionName متاح لكنه مخصص لمعمارية ($targetAbi) بينما جهازك يدعم (${deviceAbis.firstOrNull() ?: "غير معروف"}).")

