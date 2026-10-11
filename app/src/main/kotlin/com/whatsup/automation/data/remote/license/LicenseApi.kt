package com.whatsup.automation.data.remote.license

import android.util.Log
import com.whatsup.automation.data.remote.update.SupabaseUpdateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * نتيجة محاولة تفعيل الترخيص (License Claim Result).
 */
data class LicenseClaimResult(
    val success: Boolean,
    val code: String,
    val message: String,
    val status: String? = null,
    val expiresAtMillis: Long = 0L,
    val serverTimeMillis: Long = 0L,
    val durationDays: Int = 0
)

/**
 * نتيجة التحقق الدوري من الترخيص (License Verification Result).
 */
data class LicenseVerifyResult(
    val valid: Boolean,
    val code: String,
    val message: String = "",
    val status: String? = null,
    val expiresAtMillis: Long = 0L,
    val serverTimeMillis: Long = 0L,
    val remainingSeconds: Long = 0L
)

/**
 * عميل سحابي خفيف بدون أي مكاتب خارجية ثقيلة للتواصل مع دوال RPC في Supabase.
 */
@Singleton
class LicenseApi @Inject constructor() {

    companion object {
        private const val TAG = "LicenseApi"
        private const val CLAIM_RPC_PATH = "/rest/v1/rpc/claim_license"
        private const val VERIFY_RPC_PATH = "/rest/v1/rpc/verify_license"
    }

    /**
     * تفعيل وربط كود الترخيص ببصمة الجهاز عبر السحابة.
     */
    suspend fun claimLicense(
        licenseKey: String,
        deviceFingerprint: String
    ): Result<LicenseClaimResult> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "${SupabaseUpdateConfig.DEFAULT_SUPABASE_URL.trimEnd('/')}$CLAIM_RPC_PATH"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = SupabaseUpdateConfig.CONNECT_TIMEOUT_MS
                readTimeout = SupabaseUpdateConfig.READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("apikey", SupabaseUpdateConfig.DEFAULT_SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseUpdateConfig.DEFAULT_SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val requestBody = JSONObject().apply {
                put("p_license_key", licenseKey.trim().uppercase())
                put("p_device_fingerprint", deviceFingerprint.trim())
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (responseCode !in 200..299) {
                Log.e(TAG, "claimLicense error HTTP $responseCode: $responseText")
                return@withContext Result.failure(Exception("خطأ في الاتصال بالخادم: $responseCode"))
            }

            val json = JSONObject(responseText)
            val success = json.optBoolean("success", false)
            val code = json.optString("code", "UNKNOWN")
            val message = json.optString("message", "")
            val status = json.optString("status", null)
            val expiresAtIso = json.optString("expires_at", "")
            val serverTimeIso = json.optString("server_time", "")
            val durationDays = json.optInt("duration_days", 0)

            val expiresAtMillis = parseIsoTimestamp(expiresAtIso)
            val serverTimeMillis = parseIsoTimestamp(serverTimeIso)

            Result.success(
                LicenseClaimResult(
                    success = success,
                    code = code,
                    message = message,
                    status = status,
                    expiresAtMillis = expiresAtMillis,
                    serverTimeMillis = serverTimeMillis,
                    durationDays = durationDays
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "claimLicense network failure: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * التحقق من صلاحية الترخيص المسجل.
     */
    suspend fun verifyLicense(
        licenseKey: String,
        deviceFingerprint: String
    ): Result<LicenseVerifyResult> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "${SupabaseUpdateConfig.DEFAULT_SUPABASE_URL.trimEnd('/')}$VERIFY_RPC_PATH"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = SupabaseUpdateConfig.CONNECT_TIMEOUT_MS
                readTimeout = SupabaseUpdateConfig.READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("apikey", SupabaseUpdateConfig.DEFAULT_SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseUpdateConfig.DEFAULT_SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val requestBody = JSONObject().apply {
                put("p_license_key", licenseKey.trim().uppercase())
                put("p_device_fingerprint", deviceFingerprint.trim())
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (responseCode !in 200..299) {
                Log.e(TAG, "verifyLicense error HTTP $responseCode: $responseText")
                return@withContext Result.failure(Exception("خطأ في الاتصال بالخادم: $responseCode"))
            }

            val json = JSONObject(responseText)
            val valid = json.optBoolean("valid", false)
            val code = json.optString("code", "UNKNOWN")
            val message = json.optString("message", "")
            val status = json.optString("status", null)
            val expiresAtIso = json.optString("expires_at", "")
            val serverTimeIso = json.optString("server_time", "")
            val remainingSeconds = json.optLong("remaining_seconds", 0L)

            val expiresAtMillis = parseIsoTimestamp(expiresAtIso)
            val serverTimeMillis = parseIsoTimestamp(serverTimeIso)

            Result.success(
                LicenseVerifyResult(
                    valid = valid,
                    code = code,
                    message = message,
                    status = status,
                    expiresAtMillis = expiresAtMillis,
                    serverTimeMillis = serverTimeMillis,
                    remainingSeconds = remainingSeconds
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "verifyLicense network failure: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseIsoTimestamp(isoString: String): Long {
        if (isoString.isBlank()) return 0L
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleanDate = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
            format.parse(cleanDate)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
