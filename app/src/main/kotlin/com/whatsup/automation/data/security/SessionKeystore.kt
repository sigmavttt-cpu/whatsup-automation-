package com.whatsup.automation.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير التخزين الآمن لجلسات واتساب — يعتمد على Android Keystore و EncryptedSharedPreferences.
 * يضمن حفظ بيانات الجلسة والمفاتيح التشفيرية بشكل آمن على الجهاز دون تسريب.
 */
@Singleton
@Suppress("DEPRECATION")
class SessionKeystore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val securePrefs: SharedPreferences = try {
        createEncryptedPrefs(context)
    } catch (e: Throwable) {
        android.util.Log.e("SessionKeystore", "Failed to initialize EncryptedSharedPreferences: ${e.message}. Attempting recovery.", e)
        try {
            // محاولة التعافي الذاتي بحذف الملف التالف إذا أعيد توليد مفتاح التشفير
            val prefsFile = java.io.File(context.applicationInfo.dataDir, "shared_prefs/whatsup_secure_session.xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
            }
            createEncryptedPrefs(context)
        } catch (fatal: Throwable) {
            android.util.Log.e("SessionKeystore", "CRITICAL: Keystore recovery failed. Enforcing strict Fail-Closed policy: ${fatal.message}", fatal)
            // مسح أي ملفات fallback غير مشفرة سابقة لمنع تسريب الجلسات
            val fallbackFile = java.io.File(context.applicationInfo.dataDir, "shared_prefs/whatsup_secure_session_fallback.xml")
            if (fallbackFile.exists()) {
                fallbackFile.delete()
            }
            // تشغيل مخزن ذاكرة مؤقت محمي غير قابل للكتابة الصريحة على القرص
            InMemorySecurePreferences()
        }
    }

    companion object {
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_SESSION_PHONE = "session_phone"
        private const val KEY_SESSION_DATA = "session_data"
        private const val KEY_PAIRING_CODE = "pairing_code"
        private const val KEY_LAST_CONNECTED = "last_connected"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_HASH = "pin_hash"

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                "whatsup_secure_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun isStealthModeEnabled(): Boolean = securePrefs.getBoolean(KEY_STEALTH_MODE, false)

    fun setStealthModeEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_STEALTH_MODE, enabled).apply()
    }

    fun isAppLockEnabled(): Boolean = securePrefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = securePrefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getPinHash(): String? = securePrefs.getString(KEY_PIN_HASH, null)

    fun setPinHash(hash: String?) {
        securePrefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun isSessionActive(): Boolean = securePrefs.getBoolean(KEY_SESSION_ACTIVE, false)

    fun setSessionActive(active: Boolean) {
        securePrefs.edit().putBoolean(KEY_SESSION_ACTIVE, active).apply()
    }

    fun getSessionPhone(): String? = securePrefs.getString(KEY_SESSION_PHONE, null)

    fun setSessionPhone(phone: String?) {
        securePrefs.edit().putString(KEY_SESSION_PHONE, phone).apply()
    }

    fun getSessionData(): String? = securePrefs.getString(KEY_SESSION_DATA, null)

    fun saveSessionData(data: String) {
        securePrefs.edit().putString(KEY_SESSION_DATA, data).apply()
    }

    fun getPairingCode(): String? = securePrefs.getString(KEY_PAIRING_CODE, null)

    fun savePairingCode(code: String) {
        securePrefs.edit().putString(KEY_PAIRING_CODE, code).apply()
    }

    fun setLastConnectedTimestamp(timestamp: Long) {
        securePrefs.edit().putLong(KEY_LAST_CONNECTED, timestamp).apply()
    }

    fun getLastConnectedTimestamp(): Long = securePrefs.getLong(KEY_LAST_CONNECTED, 0L)

    fun clearSession() {
        securePrefs.edit().clear().apply()
    }
}

/**
 * مخزن ذاكرة مؤقت لتطبيق مبدأ Fail-Closed عند تعطل Keystore الجذري.
 * لا يكتب أي بيانات أو مفاتيح حساسة بصيغة نص واضح على قرص الجهاز.
 */
private class InMemorySecurePreferences : SharedPreferences {
    private val memoryMap = java.util.concurrent.ConcurrentHashMap<String, Any?>()

    override fun getAll(): MutableMap<String, *> = HashMap(memoryMap)
    override fun getString(key: String?, defValue: String?): String? = memoryMap[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (memoryMap[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = memoryMap[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = memoryMap[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = memoryMap[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = memoryMap[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = memoryMap.containsKey(key)
    override fun edit(): SharedPreferences.Editor = EditorImpl()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class EditorImpl : SharedPreferences.Editor {
        private val tempMap = HashMap<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { tempMap[key ?: ""] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { tempMap[key ?: ""] = values }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { tempMap[key ?: ""] = value }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { tempMap[key ?: ""] = value }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { tempMap[key ?: ""] = value }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { tempMap[key ?: ""] = value }
        override fun remove(key: String?): SharedPreferences.Editor = apply { tempMap[key ?: ""] = null }
        override fun clear(): SharedPreferences.Editor = apply { clear = true }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clear) memoryMap.clear()
            tempMap.forEach { (k, v) ->
                if (v == null) memoryMap.remove(k) else memoryMap[k] = v
            }
        }
    }
}

