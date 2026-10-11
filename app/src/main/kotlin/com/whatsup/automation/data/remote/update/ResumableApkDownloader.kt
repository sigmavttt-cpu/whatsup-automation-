package com.whatsup.automation.data.remote.update

import android.util.Log
import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.model.UpdateDownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * محرك تنزيل قطاعي مجزأ فائق السرعة يدعم الاستئناف التلقائي (Resumable Chunked Downloader)
 * وفحص مطابقة البصمة المشفرة SHA-256.
 */
@Singleton
class ResumableApkDownloader @Inject constructor() {

    companion object {
        private const val TAG = "ResumableApkDownloader"
        private const val BUFFER_SIZE = 128 * 1024 // 128 KB High-Performance Stream Buffer
        private const val SPEED_SAMPLE_INTERVAL_MS = 300L
    }

    /**
     * تنزيل حزمة APK مع دعم الاستئناف التلقائي وتدفق التقدم الحي.
     */
    fun download(
        updateInfo: AppUpdateInfo,
        destinationDir: File,
        useMirror: Boolean = false
    ): Flow<UpdateDownloadProgress> = flow {
        val targetUrlString = if (useMirror && !updateInfo.mirrorApkUrl.isNullOrBlank()) {
            updateInfo.mirrorApkUrl
        } else {
            updateInfo.apkUrl
        }

        val safeFileName = "whatsup_v${updateInfo.versionCode}_${updateInfo.versionName}.apk"
        val finalApkFile = File(destinationDir, safeFileName)
        val partFile = File(destinationDir, "$safeFileName.part")

        // 1. فحص ما إذا كان الملف النهائي مكتملاً ومطابقاً لبصمة SHA-256 مسبقاً
        if (finalApkFile.exists() && finalApkFile.length() > 0) {
            emit(UpdateDownloadProgress.VerifyingChecksum(finalApkFile, updateInfo.apkSha256))
            if (verifySha256(finalApkFile, updateInfo.apkSha256)) {
                Log.d(TAG, "Cached APK already verified: ${finalApkFile.absolutePath}")
                emit(UpdateDownloadProgress.Completed(finalApkFile, sha256Verified = true))
                return@flow
            } else {
                Log.w(TAG, "Cached APK checksum mismatch, re-downloading...")
                finalApkFile.delete()
            }
        }

        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        var connection: HttpURLConnection? = null
        var randomAccessFile: RandomAccessFile? = null

        try {
            var existingLength = if (partFile.exists()) partFile.length() else 0L

            // إذا كان حجم الملف المؤقت أكبر من الحجم المتوقع، يتم حذفه والبدء من جديد
            if (updateInfo.apkSizeBytes > 0 && existingLength > updateInfo.apkSizeBytes) {
                partFile.delete()
                existingLength = 0L
            }

            // دعم تتبع التوجيهات (HTTP 301/302/307/308 Redirects) حتى 5 مرات
            var currentUrl = targetUrlString
            var redirectCount = 0
            var finalResponseCode = 0

            while (redirectCount < 5) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = SupabaseUpdateConfig.CONNECT_TIMEOUT_MS
                    readTimeout = SupabaseUpdateConfig.DOWNLOAD_READ_TIMEOUT_MS
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "WhatsUp-Resumable-Downloader/8.1.0")
                    setRequestProperty("Accept-Encoding", "identity")

                    if (existingLength > 0) {
                        setRequestProperty("Range", "bytes=$existingLength-")
                        Log.d(TAG, "Resuming download from byte: $existingLength")
                    }
                }

                finalResponseCode = connection.responseCode
                Log.d(TAG, "Download HTTP Response Code ($currentUrl): $finalResponseCode")

                if (finalResponseCode in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308)) {
                    val newLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = if (newLocation.startsWith("http://") || newLocation.startsWith("https://")) {
                            newLocation
                        } else {
                            URL(URL(currentUrl), newLocation).toString()
                        }
                        redirectCount++
                        Log.d(TAG, "Following redirect #$redirectCount to: $currentUrl")
                        continue
                    }
                }
                break
            }

            val isPartial = finalResponseCode == HttpURLConnection.HTTP_PARTIAL // 206
            val isOk = finalResponseCode == HttpURLConnection.HTTP_OK // 200

            if (!isPartial && !isOk) {
                // إذا كان الخطأ 416 (Range Not Satisfiable)، احذف الملف المؤقت وحاول مجدداً من الصفر
                if (finalResponseCode == 416) {
                    partFile.delete()
                    emit(UpdateDownloadProgress.Error(
                        message = "نطاق الملف غير صالح، تم تصفير التنزيل المؤقت للبدء من جديد.",
                        canRetry = true,
                        canUseMirror = !useMirror && !updateInfo.mirrorApkUrl.isNullOrBlank()
                    ))
                    return@flow
                }
                emit(UpdateDownloadProgress.Error(
                    message = "فشل الاتصال بخادم التنزيل (HTTP $finalResponseCode)",
                    canRetry = true,
                    canUseMirror = !useMirror && !updateInfo.mirrorApkUrl.isNullOrBlank()
                ))
                return@flow
            }

            val totalBytesExpected = if (isPartial) {
                val contentLength = connection!!.contentLengthLong
                if (contentLength > 0) existingLength + contentLength else updateInfo.apkSizeBytes
            } else {
                existingLength = 0L // الخادم أرسل الملف كاملاً (200 OK) بدون تجزئة
                val contentLength = connection!!.contentLengthLong
                if (contentLength > 0) contentLength else updateInfo.apkSizeBytes
            }

            randomAccessFile = RandomAccessFile(partFile, "rw")
            if (isPartial) {
                randomAccessFile.seek(existingLength)
            } else {
                randomAccessFile.setLength(0)
                randomAccessFile.seek(0)
            }

            val rawStream = connection!!.inputStream
            val inputStream = java.io.BufferedInputStream(rawStream, BUFFER_SIZE)
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesDownloaded = existingLength
            var lastSampleTime = System.currentTimeMillis()
            var bytesDownloadedSinceSample = 0L
            var currentSpeed = 0L
            var isCancelledOrAborted = false

            while (coroutineContext.isActive) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                randomAccessFile.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                bytesDownloadedSinceSample += bytesRead

                val now = System.currentTimeMillis()
                val timeDiff = now - lastSampleTime
                if (timeDiff >= SPEED_SAMPLE_INTERVAL_MS) {
                    currentSpeed = (bytesDownloadedSinceSample * 1000L) / timeDiff
                    lastSampleTime = now
                    bytesDownloadedSinceSample = 0L

                    val progressPercent = if (totalBytesExpected > 0) {
                        (bytesDownloaded.toFloat() / totalBytesExpected.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    val remainingBytes = (totalBytesExpected - bytesDownloaded).coerceAtLeast(0L)
                    val etaSeconds = if (currentSpeed > 0) remainingBytes / currentSpeed else 0L

                    emit(UpdateDownloadProgress.Progress(
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytesExpected,
                        progressPercent = progressPercent,
                        speedBytesPerSec = currentSpeed,
                        etaSeconds = etaSeconds
                    ))
                }
            }

            randomAccessFile.close()
            randomAccessFile = null

            // إذا تم إلغاء الكوروتين في المنتصف، نتوقف دون مساس بالملف المؤقت
            if (!coroutineContext.isActive) {
                Log.d(TAG, "Download job cancelled by caller. Part file saved for resume at: $bytesDownloaded bytes.")
                return@flow
            }

            // فحص ما إذا كان التنزيل قد انقطع قبل اكتمال كامل البايتات المتوقعة
            if (totalBytesExpected > 0 && bytesDownloaded < totalBytesExpected) {
                Log.w(TAG, "Download interrupted prematurely ($bytesDownloaded of $totalBytesExpected bytes). Part file preserved.")
                emit(UpdateDownloadProgress.Error(
                    message = "انقطع اتصال التنزيل قبل اكتمال الحزمة. تم حفظ البيانات المجزأة لاستئناف التحميل لاحقاً.",
                    canRetry = true,
                    canUseMirror = !useMirror && !updateInfo.mirrorApkUrl.isNullOrBlank()
                ))
                return@flow
            }

            // إرسال تقرير 100% اكتمال التحميل
            emit(UpdateDownloadProgress.Progress(
                bytesDownloaded = bytesDownloaded,
                totalBytes = if (totalBytesExpected > 0) totalBytesExpected else bytesDownloaded,
                progressPercent = 1.0f,
                speedBytesPerSec = 0L,
                etaSeconds = 0L
            ))

            // 2. التحقق من سلامة البصمة المشفرة SHA-256
            emit(UpdateDownloadProgress.VerifyingChecksum(partFile, updateInfo.apkSha256))
            val isVerified = verifySha256(partFile, updateInfo.apkSha256)

            if (isVerified) {
                if (finalApkFile.exists()) finalApkFile.delete()
                val renamed = partFile.renameTo(finalApkFile)
                if (renamed) {
                    Log.d(TAG, "APK successfully downloaded and verified: ${finalApkFile.absolutePath}")
                    emit(UpdateDownloadProgress.Completed(finalApkFile, sha256Verified = true))
                } else {
                    emit(UpdateDownloadProgress.Error("فشل في تثبيت اسم ملف الحزمة النهائي.", canRetry = true))
                }
            } else {
                Log.e(TAG, "SHA-256 Checksum verification failed!")
                partFile.delete()
                emit(UpdateDownloadProgress.Error(
                    message = "فشل التحقق من أمان وبصمة الحزمة (SHA-256 Checksum Mismatch) ⚠️",
                    canRetry = true,
                    canUseMirror = !useMirror && !updateInfo.mirrorApkUrl.isNullOrBlank()
                ))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during APK download: ${e.message}", e)
            emit(UpdateDownloadProgress.Error(
                message = "حدث خطأ أثناء تنزيل التحديث: ${e.localizedMessage ?: "انقطع الاتصال"}",
                canRetry = true,
                canUseMirror = !useMirror && !updateInfo.mirrorApkUrl.isNullOrBlank()
            ))
        } finally {
            try { randomAccessFile?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * حساب بصمة SHA-256 لملف والتحقق من مطابقتها للبصمة المتوقعة.
     */
    fun verifySha256(file: File, expectedSha256: String): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        if (expectedSha256.isBlank()) {
            // إذا لم يتم تزويد بصمة من السيرفر (بيئة تطويرية أو تجريبية)
            return true
        }

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_SIZE)
            FileInputStream(file).use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hashBytes = digest.digest()
            val computedHex = hashBytes.joinToString("") { "%02x".format(it) }
            val match = computedHex.equals(expectedSha256.trim(), ignoreCase = true)
            Log.d(TAG, "Computed SHA-256: $computedHex, Expected: $expectedSha256, Match: $match")
            match
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating SHA-256: ${e.message}", e)
            false
        }
    }
}
