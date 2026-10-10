package com.whatsup.automation.data.security

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير قفل التطبيق والخزينة — يدير رمز الـ PIN والبصمة البيومترية بتشفير SHA-256.
 */
@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionKeystore: SessionKeystore
) {
    private val _isUnlocked = MutableStateFlow(!isLockEnabled())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun isLockEnabled(): Boolean {
        return sessionKeystore.isAppLockEnabled() && !sessionKeystore.getPinHash().isNullOrBlank()
    }

    fun isBiometricEnabled(): Boolean {
        return sessionKeystore.isBiometricEnabled()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sessionKeystore.setBiometricEnabled(enabled)
    }

    fun isBiometricAvailable(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? android.hardware.fingerprint.FingerprintManager
                fingerprintManager?.isHardwareDetected == true && fingerprintManager.hasEnrolledFingerprints()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val storedHash = sessionKeystore.getPinHash() ?: return false
        val enteredHash = hashPin(enteredPin)
        val success = storedHash == enteredHash
        if (success) {
            _isUnlocked.value = true
        }
        return success
    }

    fun unlockByBiometric() {
        _isUnlocked.value = true
    }

    fun lock() {
        if (isLockEnabled()) {
            _isUnlocked.value = false
        }
    }

    fun setPin(newPin: String) {
        val hash = hashPin(newPin)
        sessionKeystore.setPinHash(hash)
        sessionKeystore.setAppLockEnabled(true)
        _isUnlocked.value = true
    }

    fun disableLock() {
        sessionKeystore.setAppLockEnabled(false)
        sessionKeystore.setPinHash(null)
        _isUnlocked.value = true
    }

    /**
     * تشغيل فحص البصمة المدمج في نظام أندرويد عبر BiometricPrompt.
     */
    fun promptBiometricAuth(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricAvailable() || !isBiometricEnabled()) {
            onError("البصمة غير مفعلة أو غير متوفرة")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle("التحقق من الهوية 🛡️")
                    .setSubtitle("استخدم البصمة لإلغاء قفل التطبيق والخزينة")
                    .setDescription("بيانات واتس آب أوتوميشن مشفرة ومحمية محلياً")
                    .setNegativeButton("إلغاء", activity.mainExecutor) { _, _ ->
                        onError("تم إلغاء المصادقة")
                    }
                    .build()

                val cancellationSignal = CancellationSignal()
                prompt.authenticate(
                    cancellationSignal,
                    activity.mainExecutor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            unlockByBiometric()
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            onError(errString?.toString() ?: "فشلت المصادقة البيومترية")
                        }

                        override fun onAuthenticationFailed() {
                            onError("لم يتم التعرف على البصمة")
                        }
                    }
                )
            } catch (e: Exception) {
                onError("خطأ في بدء المصادقة: ${e.message}")
            }
        } else {
            onError("المصادقة البيومترية تتطلب أندرويد 9 فما فوق")
        }
    }

    private fun hashPin(pin: String): String {
        val salt = "whatsup_vault_salt_2026_"
        val bytes = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
