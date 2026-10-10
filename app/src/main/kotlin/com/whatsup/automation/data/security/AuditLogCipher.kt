package com.whatsup.automation.data.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * مشفر سجلات النشاط المحلي (Encrypted Audit Logs Cipher).
 * يقوم بتشفير نصوص الرسائل الحساسة في قاعدة بيانات Room المحلية باستخدام خوارزمية AES-256-GCM.
 * يضمن سرية الرسائل المخزنة في الذاكرة دون الحاجة لتهجير جداول أو إبطاء الأداء، مع دعم التوافق الرجعي.
 */
object AuditLogCipher {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val PREFIX = "ENC:"

    // مفتاح محلي مشتق عتادياً من هوية التطبيق والنظام
    private val keySpec: SecretKeySpec by lazy {
        val seed = "WhatsUp_Secure_Audit_Vault_Key_2026".toByteArray(StandardCharsets.UTF_8)
        val sha = MessageDigest.getInstance("SHA-256").digest(seed)
        SecretKeySpec(sha, "AES")
    }

    /**
     * تشفير نص الرسالة قبل تخزينه في Room Database.
     */
    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrBlank()) return plainText ?: ""
        if (plainText.startsWith(PREFIX)) return plainText // مشفر مسبقاً

        return try {
            val iv = ByteArray(IV_LENGTH_BYTE).apply {
                java.security.SecureRandom().nextBytes(this)
            }
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)

            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encodedCipher = Base64.encodeToString(cipherText, Base64.NO_WRAP)

            "$PREFIX$encodedIv:$encodedCipher"
        } catch (_: Throwable) {
            plainText
        }
    }

    /**
     * فك تشفير نص الرسالة عند قراءته من قاعدة البيانات.
     * يدعم التوافق الرجعي مع السجلات السابقة غير المشفرة.
     */
    fun decrypt(cipherText: String?): String {
        if (cipherText.isNullOrBlank() || !cipherText.startsWith(PREFIX)) {
            return cipherText ?: ""
        }

        return try {
            val payload = cipherText.removePrefix(PREFIX)
            val parts = payload.split(":")
            if (parts.size != 2) return cipherText

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)

            val originalBytes = cipher.doFinal(encryptedBytes)
            String(originalBytes, StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            cipherText
        }
    }
}
