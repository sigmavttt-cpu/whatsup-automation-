package com.whatsup.automation

import com.whatsup.automation.data.remote.update.ResumableApkDownloader
import com.whatsup.automation.data.remote.update.SupabaseUpdateApi
import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.model.EnforcementLevel
import com.whatsup.automation.domain.model.UpdateType
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class EnterpriseUpdateEngineTest {

    private val supabaseUpdateApi = SupabaseUpdateApi()
    private val downloader = ResumableApkDownloader()

    @Test
    fun testSupabaseJsonParsing() {
        val sampleJson = """
            {
                "version_code": 12,
                "version_name": "8.2.0",
                "min_supported_version": 10,
                "update_type": "MANDATORY",
                "enforcement_level": "STRICT_BLOCKING",
                "grace_period_days": 2,
                "apk_url": "https://example.com/whatsup_v8.2.0.apk",
                "mirror_apk_url": "https://mirror.example.com/whatsup_v8.2.0.apk",
                "apk_size_bytes": 28456789,
                "apk_sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "target_abi": "arm64-v8a",
                "channel": "stable",
                "rollout_percentage": 100,
                "title_ar": "تحديث أمني واستقرار حاسم",
                "title_en": "Critical Stability & Security Update",
                "changelog_ar": "• دعم التحديثات التلقائية\n• تحسين محرك الأتمتة ومكافحة الحظر",
                "changelog_en": "• Auto updates support\n• Anti-ban engine improvements",
                "release_notes": "Hotfix release",
                "is_active": true,
                "published_at": "2026-10-09T12:00:00Z"
            }
        """.trimIndent()

        val jsonObject = JSONObject(sampleJson)
        val updateInfo = supabaseUpdateApi.parseSingleUpdate(jsonObject)

        assertNotNull(updateInfo)
        assertEquals(12, updateInfo?.versionCode)
        assertEquals("8.2.0", updateInfo?.versionName)
        assertEquals(10, updateInfo?.minSupportedVersion)
        assertEquals(UpdateType.MANDATORY, updateInfo?.updateType)
        assertEquals(EnforcementLevel.STRICT_BLOCKING, updateInfo?.enforcementLevel)
        assertEquals("https://example.com/whatsup_v8.2.0.apk", updateInfo?.apkUrl)
        assertEquals("https://mirror.example.com/whatsup_v8.2.0.apk", updateInfo?.mirrorApkUrl)
        assertEquals(28456789L, updateInfo?.apkSizeBytes)
        assertEquals("arm64-v8a", updateInfo?.targetAbi)
        assertEquals(true, updateInfo?.isActive)
        assertTrue(updateInfo?.changelogAr?.contains("مكافحة الحظر") == true)
    }

    @Test
    fun testMandatoryUpdateLogic() {
        val mandatoryUpdate = AppUpdateInfo(
            versionCode = 15,
            versionName = "8.3.0",
            minSupportedVersion = 12,
            updateType = UpdateType.MANDATORY,
            enforcementLevel = EnforcementLevel.STRICT_BLOCKING,
            gracePeriodDays = 0,
            apkUrl = "https://example.com/apk",
            apkSizeBytes = 1000L,
            apkSha256 = "abc",
            titleAr = "تحديث إجباري",
            titleEn = "Mandatory Update",
            changelogAr = "تفاصيل",
            changelogEn = "Details"
        )

        // إذا كان الإصدار الحالي 10 والحد الأدنى 12 -> إجباري
        assertTrue(mandatoryUpdate.isMandatoryUpdate(currentVersionCode = 10))
        // إذا كان الإصدار الحالي 11 -> إجباري
        assertTrue(mandatoryUpdate.isMandatoryUpdate(currentVersionCode = 11))
        // إذا كان الإصدار الحالي 14 لكن نوع التحديث MANDATORY -> إجباري
        assertTrue(mandatoryUpdate.isMandatoryUpdate(currentVersionCode = 14))

        val optionalUpdate = mandatoryUpdate.copy(
            minSupportedVersion = 5,
            updateType = UpdateType.OPTIONAL,
            enforcementLevel = EnforcementLevel.DISMISSIBLE
        )

        // إذا كان الإصدار الحالي 10 والحد الأدنى 5 ونوع التحديث اختياري -> ليس إجبارياً
        assertFalse(optionalUpdate.isMandatoryUpdate(currentVersionCode = 10))
    }

    @Test
    fun testAbiCompatibilityMatching() {
        val arm64Update = AppUpdateInfo(
            versionCode = 12,
            versionName = "8.2.0",
            minSupportedVersion = 1,
            updateType = UpdateType.OPTIONAL,
            enforcementLevel = EnforcementLevel.DISMISSIBLE,
            gracePeriodDays = 0,
            apkUrl = "https://example.com/apk",
            apkSizeBytes = 1000L,
            apkSha256 = "abc",
            targetAbi = "arm64-v8a",
            titleAr = "تحديث",
            titleEn = "Update",
            changelogAr = "",
            changelogEn = ""
        )

        assertTrue(arm64Update.matchesAbi(listOf("arm64-v8a", "armeabi-v7a")))
        assertFalse(arm64Update.matchesAbi(listOf("x86_64", "x86")))

        val universalUpdate = arm64Update.copy(targetAbi = "all")
        assertTrue(universalUpdate.matchesAbi(listOf("x86_64")))
    }

    @Test
    fun testSha256ChecksumVerification() {
        val tempFile = File.createTempFile("test_apk", ".bin")
        try {
            val content = "WhatsUp Automation Enterprise APK Binary Content Payload 2026".toByteArray(Charsets.UTF_8)
            tempFile.writeBytes(content)

            val digest = MessageDigest.getInstance("SHA-256")
            val expectedHash = digest.digest(content).joinToString("") { "%02x".format(it) }

            // 1. فحص المطابقة الصحيحة
            val isMatch = downloader.verifySha256(tempFile, expectedHash)
            assertTrue("Expected SHA-256 match", isMatch)

            // 2. فحص كشف عدم المطابقة عند التلاعب
            val wrongHash = "11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff"
            val isWrongMatch = downloader.verifySha256(tempFile, wrongHash)
            assertFalse("Expected SHA-256 mismatch for wrong hash", isWrongMatch)

            // 3. فحص البصمة الفارغة (تعتبر مقبولة في بيئة الاختبارات أو عند عدم تزويدها)
            assertTrue(downloader.verifySha256(tempFile, ""))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testEnumParsingAndDefaults() {
        assertEquals(UpdateType.MANDATORY, UpdateType.fromString("MANDATORY"))
        assertEquals(UpdateType.CRITICAL_SECURITY, UpdateType.fromString("CRITICAL_SECURITY"))
        assertEquals(UpdateType.CRITICAL_SECURITY, UpdateType.fromString("SECURITY"))
        assertEquals(UpdateType.SILENT_HOTFIX, UpdateType.fromString("HOTFIX"))
        assertEquals(UpdateType.OPTIONAL, UpdateType.fromString("OPTIONAL"))
        assertEquals(UpdateType.OPTIONAL, UpdateType.fromString("UNKNOWN_VALUE"))
        assertEquals(UpdateType.OPTIONAL, UpdateType.fromString(null))

        assertEquals(EnforcementLevel.STRICT_BLOCKING, EnforcementLevel.fromString("STRICT_BLOCKING"))
        assertEquals(EnforcementLevel.STRICT_BLOCKING, EnforcementLevel.fromString("BLOCKING"))
        assertEquals(EnforcementLevel.GRACE_PERIOD, EnforcementLevel.fromString("GRACE_PERIOD"))
        assertEquals(EnforcementLevel.RECOMMENDED, EnforcementLevel.fromString("RECOMMENDED"))
        assertEquals(EnforcementLevel.DISMISSIBLE, EnforcementLevel.fromString("DISMISSIBLE"))
        assertEquals(EnforcementLevel.DISMISSIBLE, EnforcementLevel.fromString("UNKNOWN"))
    }

    @Test
    fun testFormattedSizeAndSpeedCalculations() {
        val updateInfoMb = AppUpdateInfo(
            versionCode = 20,
            versionName = "8.5.0",
            minSupportedVersion = 1,
            updateType = UpdateType.OPTIONAL,
            enforcementLevel = EnforcementLevel.DISMISSIBLE,
            gracePeriodDays = 0,
            apkUrl = "https://example.com/apk",
            apkSizeBytes = 28 * 1024 * 1024L + (512 * 1024L), // ~28.5 MB
            apkSha256 = "",
            titleAr = "",
            titleEn = "",
            changelogAr = "",
            changelogEn = ""
        )
        assertEquals("28.5 MB", updateInfoMb.getFormattedSize())

        val updateInfoKb = updateInfoMb.copy(apkSizeBytes = 512 * 1024L)
        assertEquals("512 KB", updateInfoKb.getFormattedSize())

        val progress = com.whatsup.automation.domain.model.UpdateDownloadProgress.Progress(
            bytesDownloaded = 14 * 1024 * 1024L,
            totalBytes = 28 * 1024 * 1024L,
            progressPercent = 0.5f,
            speedBytesPerSec = 2 * 1024 * 1024L,
            etaSeconds = 7L
        )
        assertEquals("2.00 MB/s", progress.getFormattedSpeed())
        assertEquals("14.0 / 28.0 MB", progress.getFormattedProgress())
    }

    @Test
    fun testInvalidJsonItemHandling() {
        // حزمة بدون versionCode أو apkUrl تعتبر غير صالحة وتُستبعد بأمان
        val invalidJson = JSONObject("""{"version_name": "1.0.0"}""")
        assertNull(supabaseUpdateApi.parseSingleUpdate(invalidJson))

        val invalidUrlJson = JSONObject("""{"version_code": 5, "apk_url": ""}""")
        assertNull(supabaseUpdateApi.parseSingleUpdate(invalidUrlJson))
    }
}
