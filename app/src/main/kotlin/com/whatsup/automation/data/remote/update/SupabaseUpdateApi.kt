package com.whatsup.automation.data.remote.update

import android.util.Log
import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.model.EnforcementLevel
import com.whatsup.automation.domain.model.UpdateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * عميل REST خفيف وسريع لجلب بيانات التحديثات من Supabase بدون حزم خارجية ثقيلة.
 */
@Singleton
class SupabaseUpdateApi @Inject constructor() {

    companion object {
        private const val TAG = "SupabaseUpdateApi"
    }

    /**
     * جلب أحدث سجلات التحديثات النشطة من جدول `app_updates` في Supabase.
     */
    suspend fun fetchActiveUpdates(
        supabaseUrl: String = SupabaseUpdateConfig.DEFAULT_SUPABASE_URL,
        anonKey: String = SupabaseUpdateConfig.DEFAULT_SUPABASE_ANON_KEY,
        channel: String = "stable"
    ): Result<List<AppUpdateInfo>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // صياغة رابط استعلام PostgREST لجلب التحديثات النشطة بترتيب الإصدار تنازلياً
            val queryUrl = buildString {
                append(supabaseUrl.trimEnd('/'))
                append(SupabaseUpdateConfig.REST_PATH)
                append("/")
                append(SupabaseUpdateConfig.TABLE_APP_UPDATES)
                append("?is_active=eq.true")
                if (channel.isNotBlank() && channel != "all") {
                    append("&channel=in.(${channel},all)")
                }
                append("&order=version_code.desc")
                append("&limit=10")
            }

            Log.d(TAG, "Fetching updates from: $queryUrl")
            val url = URL(queryUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = SupabaseUpdateConfig.CONNECT_TIMEOUT_MS
                readTimeout = SupabaseUpdateConfig.READ_TIMEOUT_MS
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "WhatsUp-Enterprise-Updater/8.1.0")
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Supabase response code: $responseCode")

            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                val jsonString = reader.use { it.readText() }
                val updates = parseUpdatesJson(jsonString)
                Result.success(updates)
            } else {
                val errorStream = connection.errorStream
                val errorMsg = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Log.e(TAG, "Supabase query error: $errorMsg")
                Result.failure(Exception("Supabase HTTP $responseCode: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Supabase update server: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * تحويل مصفوفة JSON المستلمة إلى قائمة كائنات `AppUpdateInfo`.
     */
    private fun parseUpdatesJson(jsonString: String): List<AppUpdateInfo> {
        val list = mutableListOf<AppUpdateInfo>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val updateInfo = parseSingleUpdate(obj)
                if (updateInfo != null) {
                    list.add(updateInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse updates JSON: ${e.message}", e)
        }
        return list
    }

    /**
     * تحليل كائن JSON فردي واستخراج كافة الحقول بدقة وأمان.
     */
    fun parseSingleUpdate(obj: JSONObject): AppUpdateInfo? {
        return try {
            val versionCode = obj.optInt("version_code", 0)
            if (versionCode <= 0) return null

            val versionName = obj.optString("version_name", "1.0.0")
            val minSupportedVersion = obj.optInt("min_supported_version", 1)
            val updateTypeStr = obj.optString("update_type", "OPTIONAL")
            val enforcementLevelStr = obj.optString("enforcement_level", "DISMISSIBLE")
            val gracePeriodDays = obj.optInt("grace_period_days", 0)
            val apkUrl = obj.optString("apk_url", "")
            if (apkUrl.isBlank()) return null

            val mirrorApkUrl = obj.optString("mirror_apk_url").takeIf { it.isNotBlank() && it != "null" }
            val apkSizeBytes = obj.optLong("apk_size_bytes", 0L)
            val apkSha256 = obj.optString("apk_sha256", "").trim()
            val targetAbi = obj.optString("target_abi", "arm64-v8a").takeIf { it.isNotBlank() && it != "null" }
            val channel = obj.optString("channel", "stable")
            val rolloutPercentage = obj.optInt("rollout_percentage", 100)
            val titleAr = obj.optString("title_ar", "تحديث جديد متاح")
            val titleEn = obj.optString("title_en", "New Update Available")
            val changelogAr = obj.optString("changelog_ar", "تحسينات شاملة على استقرار الأداء والأمان.")
            val changelogEn = obj.optString("changelog_en", "Performance and stability improvements.")
            val releaseNotes = obj.optString("release_notes").takeIf { it.isNotBlank() && it != "null" }
            val isActive = obj.optBoolean("is_active", true)
            val publishedAt = obj.optString("published_at").takeIf { it.isNotBlank() && it != "null" }

            AppUpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                minSupportedVersion = minSupportedVersion,
                updateType = UpdateType.fromString(updateTypeStr),
                enforcementLevel = EnforcementLevel.fromString(enforcementLevelStr),
                gracePeriodDays = gracePeriodDays,
                apkUrl = apkUrl,
                mirrorApkUrl = mirrorApkUrl,
                apkSizeBytes = apkSizeBytes,
                apkSha256 = apkSha256,
                targetAbi = targetAbi,
                channel = channel,
                rolloutPercentage = rolloutPercentage,
                titleAr = titleAr,
                titleEn = titleEn,
                changelogAr = changelogAr,
                changelogEn = changelogEn,
                releaseNotes = releaseNotes,
                isActive = isActive,
                publishedAt = publishedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing single update JSON item: ${e.message}", e)
            null
        }
    }
}
