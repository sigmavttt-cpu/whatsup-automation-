package com.whatsup.automation.data.security

import android.util.Log
import com.whatsup.automation.data.remote.license.LicenseApi
import com.whatsup.automation.data.remote.license.LicenseClaimResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * حالات ترخيص واشتراك التطبيق.
 */
sealed class LicenseState {
    object Idle : LicenseState()
    object NotActivated : LicenseState()
    data class Active(
        val licenseKey: String,
        val remainingDays: Int,
        val expiresAtMillis: Long
    ) : LicenseState()
    data class Expired(
        val licenseKey: String?,
        val expiresAtMillis: Long
    ) : LicenseState()
    data class Blocked(
        val message: String
    ) : LicenseState()
    object Verifying : LicenseState()
}

/**
 * مدير التراخيص والاشتراكات المؤسسي (Enterprise License Manager).
 * يراقب حالة الاشتراك، يتحقق من السحابة، ويحمي المحرك من التشغيل غير المصرح به.
 */
@Singleton
class LicenseManager @Inject constructor(
    private val sessionKeystore: SessionKeystore,
    private val deviceSecurityManager: DeviceSecurityManager,
    private val licenseApi: LicenseApi
) {
    companion object {
        private const val TAG = "LicenseManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _licenseState = MutableStateFlow<LicenseState>(LicenseState.Idle)
    val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

    init {
        checkLocalStateAndRefresh()
    }

    /**
     * فحص الحالة المحلية أولاً ثم التحقق الصامت من السحابة في الخلفية.
     */
    fun checkLocalStateAndRefresh() {
        val key = sessionKeystore.getLicenseKey()
        val status = sessionKeystore.getLicenseStatus()
        val expiresAt = sessionKeystore.getLicenseExpiresAt()

        if (key.isNullOrBlank() || status == "NOT_ACTIVATED") {
            _licenseState.value = LicenseState.NotActivated
            return
        }

        if (status == "BLOCKED") {
            _licenseState.value = LicenseState.Blocked("تم حظر هذا الاشتراك من قبل الإدارة")
            return
        }

        val now = System.currentTimeMillis()
        if (expiresAt > 0 && now >= expiresAt) {
            sessionKeystore.setLicenseStatus("EXPIRED")
            _licenseState.value = LicenseState.Expired(key, expiresAt)
            return
        }

        // إذا كان نشطاً محلياً، نضعه كنشط فوراً حتى لا ينتظر العميل
        val remainingMillis = if (expiresAt > now) expiresAt - now else 0L
        val remainingDays = (remainingMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        _licenseState.value = LicenseState.Active(key, remainingDays, expiresAt)

        // التحقق في الخلفية من السحابة لتحديث الحالة إذا انتهى في السحابة
        scope.launch {
            verifyWithServerSilently(key)
        }
    }

    /**
     * تفعيل اشتراك جديد باستخدام الكود المدخل من العميل.
     */
    suspend fun activateLicense(inputKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = inputKey.trim().uppercase()
        if (cleanKey.length < 5) {
            return@withContext Result.failure(Exception("يرجى إدخال كود تفعيل صحيح"))
        }

        _licenseState.value = LicenseState.Verifying
        val fingerprint = deviceSecurityManager.getOrCreateDeviceFingerprint()

        val result = licenseApi.claimLicense(cleanKey, fingerprint)
        result.fold(
            onSuccess = { claimRes ->
                if (claimRes.success && claimRes.status == "ACTIVE") {
                    sessionKeystore.setLicenseKey(cleanKey)
                    sessionKeystore.setLicenseStatus("ACTIVE")
                    sessionKeystore.setLicenseExpiresAt(claimRes.expiresAtMillis)
                    sessionKeystore.setLastLicenseVerifiedAt(System.currentTimeMillis())

                    val now = System.currentTimeMillis()
                    val remainingMillis = if (claimRes.expiresAtMillis > now) claimRes.expiresAtMillis - now else 0L
                    val remainingDays = (remainingMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)

                    _licenseState.value = LicenseState.Active(cleanKey, remainingDays, claimRes.expiresAtMillis)
                    Result.success(claimRes.message.ifBlank { "تم تفعيل الاشتراك بنجاح" })
                } else {
                    // فشل التفعيل
                    when (claimRes.code) {
                        "DEVICE_MISMATCH" -> {
                            _licenseState.value = LicenseState.NotActivated
                            Result.failure(Exception("هذا الكود مستخدم بالفعل على جهاز آخر ولا يمكن مشاركته"))
                        }
                        "EXPIRED" -> {
                            sessionKeystore.setLicenseStatus("EXPIRED")
                            _licenseState.value = LicenseState.Expired(cleanKey, claimRes.expiresAtMillis)
                            Result.failure(Exception("هذا الاشتراك منتهٍ، يرجى التواصل مع الإدارة للتجديد"))
                        }
                        "BLOCKED" -> {
                            sessionKeystore.setLicenseStatus("BLOCKED")
                            _licenseState.value = LicenseState.Blocked("تم حظر هذا الكود من قبل الإدارة")
                            Result.failure(Exception("تم حظر هذا الكود من قبل الإدارة"))
                        }
                        else -> {
                            _licenseState.value = LicenseState.NotActivated
                            Result.failure(Exception(claimRes.message.ifBlank { "كود التفعيل غير صالح" }))
                        }
                    }
                }
            },
            onFailure = { error ->
                _licenseState.value = LicenseState.NotActivated
                Result.failure(Exception("تعذر الاتصال بخادم التفعيل: ${error.message ?: "تحقق من اتصال الإنترنت"}"))
            }
        )
    }

    /**
     * التحقق الصامت من السحابة بدون تعطيل تجربة المستخدم.
     */
    private suspend fun verifyWithServerSilently(key: String) {
        val fingerprint = deviceSecurityManager.getOrCreateDeviceFingerprint()
        val result = licenseApi.verifyLicense(key, fingerprint)
        result.onSuccess { verifyRes ->
            if (!verifyRes.valid) {
                when (verifyRes.code) {
                    "EXPIRED" -> {
                        sessionKeystore.setLicenseStatus("EXPIRED")
                        _licenseState.value = LicenseState.Expired(key, verifyRes.expiresAtMillis)
                    }
                    "BLOCKED" -> {
                        sessionKeystore.setLicenseStatus("BLOCKED")
                        _licenseState.value = LicenseState.Blocked("تم حظر هذا الاشتراك")
                    }
                    "DEVICE_MISMATCH" -> {
                        sessionKeystore.clearLicense()
                        _licenseState.value = LicenseState.NotActivated
                    }
                }
            } else {
                // تحديث موعد الانتهاء من السيرفر
                if (verifyRes.expiresAtMillis > 0) {
                    sessionKeystore.setLicenseExpiresAt(verifyRes.expiresAtMillis)
                    sessionKeystore.setLastLicenseVerifiedAt(System.currentTimeMillis())
                    val remainingDays = (verifyRes.remainingSeconds / (60 * 60 * 24)).toInt().coerceAtLeast(1)
                    _licenseState.value = LicenseState.Active(key, remainingDays, verifyRes.expiresAtMillis)
                }
            }
        }
    }

    /**
     * فحص حاسم يُستخدم من قبل محرك واتساب والخدمة الأمامية لمعرفة هل مسموح بالعمل.
     */
    fun isSubscriptionActive(): Boolean {
        val status = sessionKeystore.getLicenseStatus()
        val expiresAt = sessionKeystore.getLicenseExpiresAt()
        val now = System.currentTimeMillis()

        if (status != "ACTIVE") return false
        if (expiresAt > 0 && now >= expiresAt) return false
        return true
    }

    fun getRemainingDays(): Int {
        val expiresAt = sessionKeystore.getLicenseExpiresAt()
        val now = System.currentTimeMillis()
        if (expiresAt <= now) return 0
        return ((expiresAt - now) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    }

    fun getActiveKey(): String? = sessionKeystore.getLicenseKey()
}
