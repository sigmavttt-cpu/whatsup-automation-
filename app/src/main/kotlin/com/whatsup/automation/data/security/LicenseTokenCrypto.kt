package com.whatsup.automation.data.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * حمولة بيانات الترخيص المشفرة.
 */
data class LicensePayload(
    val deviceId: String,
    val expiresAtMillis: Long,
    val planType: String = "VIP",
    val issuedAtMillis: Long = System.currentTimeMillis()
)

/**
 * نتيجة فحص الهاش المشفر.
 */
sealed class LicenseValidationResult {
    data class Valid(val payload: LicensePayload) : LicenseValidationResult()
    data class Expired(val payload: LicensePayload) : LicenseValidationResult()
    data class DeviceMismatch(val actualDevice: String, val expectedDevice: String) : LicenseValidationResult()
    object InvalidSignature : LicenseValidationResult()
    object NotAValidToken : LicenseValidationResult()
}

/**
 * محرك التشفير والتوقيع الرقمي لتراخيص التطبيق (Cryptographic License Hash Engine).
 * يتيح التحقق والتشغيل الأوفلاين 100% بربط معرّف الجهاز وتاريخ الصلاحية بتوقيع HMAC رقمي غير قابل للتزوير.
 */
@Singleton
class LicenseTokenCrypto @Inject constructor() {

    companion object {
        const val PREFIX = "SIGMA-"
        // مفتاح التوقيع السري الأساسي المشفر لمنظومة WhatsUp AutoFlow
        const val MASTER_SALT = "WUP_SIGMA_AUTOFLOW_2026_MASTER_SECRET_KEY_XY987"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    /**
     * توليد رمز هاش ترخيص مشفر وموقع رقمياً.
     */
    fun generateToken(
        deviceId: String,
        durationDays: Int,
        planType: String = "VIP",
        issuedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val cleanDevice = deviceId.trim().uppercase()
        val expiresAtMillis = if (durationDays > 0) {
            issuedAtMillis + (durationDays.toLong() * 24L * 60L * 60L * 1000L)
        } else {
            0L // 0 يعني اشتراك دائم غير منتهٍ
        }

        val rawPayload = "$cleanDevice|$expiresAtMillis|${planType.trim().uppercase()}|$issuedAtMillis"
        val payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(rawPayload.toByteArray(StandardCharsets.UTF_8))
        val signatureHex = computeHmacSha256(payloadBase64, MASTER_SALT)

        return "$PREFIX$payloadBase64.$signatureHex"
    }

    /**
     * فحص والتحقق من صحة رمز الهاش المشفر محلياً دون الحاجة للاتصال بالإنترنت.
     */
    fun validateToken(
        token: String,
        currentDeviceFingerprint: String
    ): LicenseValidationResult {
        val trimmed = token.trim()
        val cleanToken = if (trimmed.startsWith(PREFIX, ignoreCase = true)) {
            trimmed.substring(PREFIX.length)
        } else {
            trimmed
        }

        val parts = cleanToken.split(".")
        if (parts.size != 2) {
            return LicenseValidationResult.NotAValidToken
        }

        val payloadBase64 = parts[0]
        val providedSignature = parts[1]

        // 1. التحقق من سلامة التوقيع الرقمي (Constant-time comparison)
        val expectedSignature = computeHmacSha256(payloadBase64, MASTER_SALT)
        if (!MessageDigest.isEqual(
                expectedSignature.toByteArray(StandardCharsets.UTF_8),
                providedSignature.uppercase().toByteArray(StandardCharsets.UTF_8)
            )
        ) {
            return LicenseValidationResult.InvalidSignature
        }

        // 2. فك ترميز محتوى الهاش
        val payload = try {
            val decodedBytes = Base64.getUrlDecoder().decode(payloadBase64)
            val decodedString = String(decodedBytes, StandardCharsets.UTF_8)
            val payloadParts = decodedString.split("|")
            if (payloadParts.size < 4) {
                return LicenseValidationResult.NotAValidToken
            }
            LicensePayload(
                deviceId = payloadParts[0],
                expiresAtMillis = payloadParts[1].toLongOrNull() ?: 0L,
                planType = payloadParts[2],
                issuedAtMillis = payloadParts[3].toLongOrNull() ?: 0L
            )
        } catch (_: Throwable) {
            return LicenseValidationResult.NotAValidToken
        }

        // 3. التحقق من مطابقة بصمة العتاد
        val cleanCurrentDevice = currentDeviceFingerprint.trim().uppercase()
        val isDeviceMatch = payload.deviceId.equals("ALL", ignoreCase = true) ||
                payload.deviceId.equals("*", ignoreCase = true) ||
                payload.deviceId.equals(cleanCurrentDevice, ignoreCase = true)

        if (!isDeviceMatch) {
            return LicenseValidationResult.DeviceMismatch(
                actualDevice = cleanCurrentDevice,
                expectedDevice = payload.deviceId
            )
        }

        // 4. التحقق من الصلاحية الزمنية
        val now = System.currentTimeMillis()
        if (payload.expiresAtMillis > 0L && now >= payload.expiresAtMillis) {
            return LicenseValidationResult.Expired(payload)
        }

        return LicenseValidationResult.Valid(payload)
    }

    private fun computeHmacSha256(data: String, key: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM)
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return hmacBytes.joinToString("") { "%02X".format(it) }
    }
}
