package com.whatsup.automation.data.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير أمان العتاد وبصمة الجهاز (Device Hardware Security Manager).
 * يقوم بتوليد وحفظ بصمة عتادية فريدة وغير قابلة للتزوير مستخرجة من معالج وشريحة الهاتف.
 */
@Singleton
class DeviceSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionKeystore: SessionKeystore
) {

    /**
     * استرجاع بصمة الجهاز الحالية أو توليدها وحفظها مشفرة إذا لم تكن موجودة.
     */
    fun getOrCreateDeviceFingerprint(): String {
        val existing = sessionKeystore.getDeviceFingerprint()
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val rawHardwareString = buildString {
            // معرّف أندرويد الآمن
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ANDROID_ID"
            append("ID:").append(androidId).append(";")

            // خصائص العتاد والمعالج الثابتة
            append("BOARD:").append(Build.BOARD).append(";")
            append("BRAND:").append(Build.BRAND).append(";")
            append("DEVICE:").append(Build.DEVICE).append(";")
            append("HARDWARE:").append(Build.HARDWARE).append(";")
            append("MANUFACTURER:").append(Build.MANUFACTURER).append(";")
            append("MODEL:").append(Build.MODEL).append(";")
            append("PRODUCT:").append(Build.PRODUCT).append(";")
            
            // بصمة النظام
            append("FINGERPRINT:").append(Build.FINGERPRINT)
        }

        val sha256Hash = sha256(rawHardwareString)
        sessionKeystore.setDeviceFingerprint(sha256Hash)
        return sha256Hash
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02X".format(it) }
    }
}
