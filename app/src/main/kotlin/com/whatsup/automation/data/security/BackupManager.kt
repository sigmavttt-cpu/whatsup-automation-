package com.whatsup.automation.data.security

import android.content.Context
import com.whatsup.automation.data.local.dao.RuleDao
import com.whatsup.automation.data.local.entity.RuleEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data class ExportSuccess(val filePath: String, val rulesCount: Int) : BackupResult()
    data class ImportSuccess(val restoredRulesCount: Int) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

/**
 * مدير النسخ الاحتياطية المشفرة محلياً (Encrypted Local Backups).
 * يدعم التشفير الكامل بكلمة مرور باستخدام PBKDF2 + AES-256-GCM.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleDao: RuleDao
) {
    companion object {
        private const val MAGIC_HEADER = "WUBKP1" // 6 bytes
        private const val PBKDF2_ITERATIONS = 10000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private const val IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    /**
     * تصدير كافة القواعد في ملف مشفر بكلمة سر
     */
    suspend fun exportEncryptedBackup(password: String): BackupResult = withContext(Dispatchers.IO) {
        if (password.length < 4) {
            return@withContext BackupResult.Error("كلمة المرور يجب ألا تقل عن 4 أحرف/أرقام.")
        }
        try {
            val rules = ruleDao.getAllRulesList()
            val backupJson = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())
                put("app", "WhatsUp Automation")
                val rulesArray = JSONArray()
                rules.forEach { r ->
                    val ruleObj = JSONObject().apply {
                        put("name", r.name)
                        put("description", r.description)
                        put("patternType", r.patternType)
                        put("patternValue", r.patternValue)
                        put("actionsJson", r.actionsJson)
                        put("priority", r.priority)
                        put("isEnabled", r.isEnabled)
                        put("createdAt", r.createdAt)
                    }
                    rulesArray.put(ruleObj)
                }
                put("rules", rulesArray)
            }

            val plainBytes = backupJson.toString().toByteArray(Charsets.UTF_8)

            // 1. توليد Salt و IV عشوائي
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH_BYTES)
            random.nextBytes(salt)
            val iv = ByteArray(IV_LENGTH_BYTES)
            random.nextBytes(iv)

            // 2. اشتقاق المفتاح عبر PBKDF2
            val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // 3. التشفير عبر AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val cipherText = cipher.doFinal(plainBytes)

            // 4. حفظ الملف
            val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val fileName = "whatsup_backup_${System.currentTimeMillis()}.wubak"
            val targetFile = File(backupDir, fileName)

            FileOutputStream(targetFile).use { fos ->
                fos.write(MAGIC_HEADER.toByteArray(Charsets.US_ASCII))
                fos.write(salt)
                fos.write(iv)
                fos.write(cipherText)
            }

            BackupResult.ExportSuccess(targetFile.absolutePath, rules.size)
        } catch (e: Exception) {
            BackupResult.Error("فشل التصدير: ${e.message}")
        }
    }

    /**
     * استيراد واستعادة القواعد من ملف مشفر
     */
    suspend fun importEncryptedBackup(fileInputStream: InputStream, password: String, mergeExisting: Boolean = false): BackupResult = withContext(Dispatchers.IO) {
        if (password.length < 4) {
            return@withContext BackupResult.Error("كلمة المرور غير صالحة.")
        }
        try {
            val allBytes = fileInputStream.readBytes()
            val minLength = MAGIC_HEADER.length + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16
            if (allBytes.size < minLength) {
                return@withContext BackupResult.Error("الملف المحدد تالف أو غير صالح.")
            }

            // فحص الترويسة
            val header = String(allBytes, 0, MAGIC_HEADER.length, Charsets.US_ASCII)
            if (header != MAGIC_HEADER) {
                return@withContext BackupResult.Error("هذا الملف ليس ملف نسخة احتياطية صالح للتطبيق.")
            }

            var offset = MAGIC_HEADER.length
            val salt = ByteArray(SALT_LENGTH_BYTES)
            System.arraycopy(allBytes, offset, salt, 0, SALT_LENGTH_BYTES)
            offset += SALT_LENGTH_BYTES

            val iv = ByteArray(IV_LENGTH_BYTES)
            System.arraycopy(allBytes, offset, iv, 0, IV_LENGTH_BYTES)
            offset += IV_LENGTH_BYTES

            val cipherText = ByteArray(allBytes.size - offset)
            System.arraycopy(allBytes, offset, cipherText, 0, cipherText.size)

            // اشتقاق المفتاح
            val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // فك التشفير والتحقق من سلامة المصادقة
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plainBytes = try {
                cipher.doFinal(cipherText)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("كلمة المرور غير صحيحة أو تم تعديل الملف.")
            }

            val jsonString = String(plainBytes, Charsets.UTF_8)
            val backupJson = JSONObject(jsonString)
            val rulesArray = backupJson.optJSONArray("rules") ?: JSONArray()

            val importedRules = mutableListOf<RuleEntity>()
            for (i in 0 until rulesArray.length()) {
                val obj = rulesArray.getJSONObject(i)
                importedRules.add(
                    RuleEntity(
                        id = 0, // معرف جديد
                        name = obj.optString("name", "قاعدة مستعادة"),
                        description = obj.optString("description", ""),
                        patternType = obj.optString("patternType", "CONTAINS"),
                        patternValue = obj.optString("patternValue", ""),
                        actionsJson = obj.optString("actionsJson", "[]"),
                        priority = obj.optInt("priority", i + 1),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            if (!mergeExisting) {
                ruleDao.clearAllRules()
            }
            ruleDao.insertRules(importedRules)

            BackupResult.ImportSuccess(importedRules.size)
        } catch (e: Exception) {
            BackupResult.Error("فشل استعادة النسخة الاحتياطية: ${e.message}")
        }
    }
}
