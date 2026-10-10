package com.whatsup.automation.domain.model

/**
 * أنواع التحديثات المدعومة في نظام التحديث الذكي.
 */
enum class UpdateType {
    MANDATORY,          // تحديث إجباري حاسم
    OPTIONAL,           // تحديث اختياري عادي
    CRITICAL_SECURITY,  // تحديث أمني حرج وفوري
    SILENT_HOTFIX;      // إصلاح سريع

    companion object {
        fun fromString(value: String?): UpdateType = when (value?.trim()?.uppercase()) {
            "MANDATORY" -> MANDATORY
            "CRITICAL_SECURITY", "SECURITY" -> CRITICAL_SECURITY
            "SILENT_HOTFIX", "HOTFIX" -> SILENT_HOTFIX
            else -> OPTIONAL
        }
    }
}

/**
 * درجات إلزامية التحديث وفرض الدرع على المستخدم.
 */
enum class EnforcementLevel {
    STRICT_BLOCKING,    // إقفال كامل وشامل فوري (درع لا يمكن تجاوزه)
    GRACE_PERIOD,       // مهلة سماح قبل الإقفال الكامل
    DISMISSIBLE,        // قابل للإغلاق والتخطي
    RECOMMENDED;        // مستحسن وموصى به

    companion object {
        fun fromString(value: String?): EnforcementLevel = when (value?.trim()?.uppercase()) {
            "STRICT_BLOCKING", "BLOCKING" -> STRICT_BLOCKING
            "GRACE_PERIOD" -> GRACE_PERIOD
            "RECOMMENDED" -> RECOMMENDED
            else -> DISMISSIBLE
        }
    }
}

/**
 * قنوات النشر والتوزيع (Production / Stable / Beta / Alpha).
 */
enum class UpdateChannel(val channelKey: String) {
    STABLE("stable"),
    BETA("beta"),
    INTERNAL("internal");

    companion object {
        fun fromString(value: String?): UpdateChannel = when (value?.trim()?.lowercase()) {
            "beta" -> BETA
            "internal" -> INTERNAL
            else -> STABLE
        }
    }
}
