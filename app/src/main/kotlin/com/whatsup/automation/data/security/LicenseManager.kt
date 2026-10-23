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
    private val licenseApi: LicenseApi,
    private val licenseTokenCrypto: LicenseTokenCrypto
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
            // تهيئة تلقائية باشتراك محلي دائم بعد مسح البيانات لتجنب قفل المستخدم
            val defaultKey = "WHATSUP-INTERNAL-DEV-FREE"
            sessionKeystore.setLicenseKey(defaultKey)
            sessionKeystore.setLicenseStatus("ACTIVE")
            sessionKeystore.setLicenseExpiresAt(0L)
            _licenseState.value = LicenseState.Active(defaultKey, 999, 0L)
            return
        }

        if (status == "BLOCKED") {
            _licenseState.value = LicenseState.Blocked("تم حظر هذا الاشتراك من قبل الإدارة")
            return
        }

        val fingerprint = deviceSecurityManager.getOrCreateDeviceFingerprint()

        // 1. فحص ما إذا كان المفتاح عبارة عن هاش مشفر محلي (Offline Signed Hash)
        val localValidation = licenseTokenCrypto.validateToken(key, fingerprint)
        if (localValidation is LicenseValidationResult.Valid) {
            val now = System.currentTimeMillis()
            val tokenExp = localValidation.payload.expiresAtMillis
            val remainingMillis = if (tokenExp > 0L && tokenExp > now) tokenExp - now else 0L
            val remainingDays = if (tokenExp == 0L) 999 else (remainingMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)

            _licenseState.value = LicenseState.Active(key, remainingDays, tokenExp)
            return
        } else if (localValidation is LicenseValidationResult.Expired) {
            sessionKeystore.setLicenseStatus("EXPIRED")
            _licenseState.value = LicenseState.Expired(key, localValidation.payload.expiresAtMillis)
            return
        } else if (localValidation is LicenseValidationResult.DeviceMismatch) {
            sessionKeystore.setLicenseStatus("BLOCKED")
            _licenseState.value = LicenseState.Blocked("هذا الترخيص مخصص لجهاز آخر")
            return
        }

        // 2. إذا لم يكن هاشاً محلياً، نطبق الفحص السحابي/الافتراضي المخزن
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
        val cleanKey = inputKey.trim()
        if (cleanKey.length < 8) {
            return@withContext Result.failure(Exception("يرجى إدخال رمز ترخيص صالح"))
        }

        _licenseState.value = LicenseState.Verifying
        val fingerprint = deviceSecurityManager.getOrCreateDeviceFingerprint()

        // 1. فحص وتفعيل رمز الهاش المشفر محلياً (100% Offline Hash)
        val localValidation = licenseTokenCrypto.validateToken(cleanKey, fingerprint)
        when (localValidation) {
            is LicenseValidationResult.Valid -> {
                val payload = localValidation.payload
                sessionKeystore.setLicenseKey(cleanKey)
                sessionKeystore.setLicenseStatus("ACTIVE")
                sessionKeystore.setLicenseExpiresAt(payload.expiresAtMillis)
                sessionKeystore.setLastLicenseVerifiedAt(System.currentTimeMillis())

                val now = System.currentTimeMillis()
                val remainingMillis = if (payload.expiresAtMillis > 0L && payload.expiresAtMillis > now) {
                    payload.expiresAtMillis - now
                } else {
                    0L
                }
                val remainingDays = if (payload.expiresAtMillis == 0L) 999 else (remainingMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)

                _licenseState.value = LicenseState.Active(cleanKey, remainingDays, payload.expiresAtMillis)
                return@withContext Result.success("تم تفعيل اشتراك (${payload.planType}) بنجاح محلياً 🚀")
            }
            is LicenseValidationResult.DeviceMismatch -> {
                _licenseState.value = LicenseState.NotActivated
                return@withContext Result.failure(Exception("هذا الهاش مخصص لجهاز آخر ولا يطابق معرّف هذا الهاتف"))
            }
            is LicenseValidationResult.Expired -> {
                sessionKeystore.setLicenseStatus("EXPIRED")
                _licenseState.value = LicenseState.Expired(cleanKey, localValidation.payload.expiresAtMillis)
                return@withContext Result.failure(Exception("هذا الهاش منتهٍ الصلاحية، يرجى الحصول على ترخيص سارٍ"))
            }
            is LicenseValidationResult.InvalidSignature -> {
                _licenseState.value = LicenseState.NotActivated
                return@withContext Result.failure(Exception("رمز الهاش غير صالح أو تم التعديل عليه"))
            }
            is LicenseValidationResult.NotAValidToken -> {
                // إذا لم يكن هاشاً محلياً، نتابع محاولة التفعيل السحابي (Supabase)
            }
        }

        // 2. التحقق السحابي عبر Supabase للأكواد السحابية
        val result = licenseApi.claimLicense(cleanKey.uppercase(), fingerprint)
        result.fold(
            onSuccess = { claimRes ->
                if (claimRes.success && claimRes.status == "ACTIVE") {
                    sessionKeystore.setLicenseKey(cleanKey.uppercase())
                    sessionKeystore.setLicenseStatus("ACTIVE")
                    sessionKeystore.setLicenseExpiresAt(claimRes.expiresAtMillis)
                    sessionKeystore.setLastLicenseVerifiedAt(System.currentTimeMillis())

                    val now = System.currentTimeMillis()
                    val remainingMillis = if (claimRes.expiresAtMillis > now) claimRes.expiresAtMillis - now else 0L
                    val remainingDays = (remainingMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)

                    _licenseState.value = LicenseState.Active(cleanKey.uppercase(), remainingDays, claimRes.expiresAtMillis)
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
                            _licenseState.value = LicenseState.Expired(cleanKey.uppercase(), claimRes.expiresAtMillis)
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
                Result.failure(Exception("تعذر تفعيل الرمز: ${error.message ?: "تحقق من صحة الهاش أو الاتصال بالإنترنت"}"))
            }
        )
    }

    /**
     * التحقق الصامت من السحابة بدون تعطيل تجربة المستخدم.
     */
    private suspend fun verifyWithServerSilently(key: String) {
        val fingerprint = deviceSecurityManager.getOrCreateDeviceFingerprint()
        if (licenseTokenCrypto.validateToken(key, fingerprint) is LicenseValidationResult.Valid) {
            // هاش محلي مشفر وموقع رقمياً، لا يحتاج لأي استعلام سحابي
            return
        }
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

        if (status == "BLOCKED" || status == "EXPIRED") return false
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
