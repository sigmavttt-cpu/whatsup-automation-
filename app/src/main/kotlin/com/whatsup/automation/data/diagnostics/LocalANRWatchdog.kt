package com.whatsup.automation.data.diagnostics

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * حارس عدم الاستجابة المحلي وتجمد الشاشة (Local ANR Watchdog).
 * يعمل بخيط خلفي دائم بنبضات قلب (2000ms). إذا تجمد الـ Main Thread لأكثر من ثانيتين،
 * يلتقط مسار كافة الخيوط ويحفظ تقريراً تفصيلياً محلياً للمطور لمعرفة سبب التجميد.
 */
class LocalANRWatchdog private constructor(
    private val context: Context,
    private val timeoutMs: Long = 2500L
) : Thread("ANR-Watchdog-Thread") {

    companion object {
        private const val TAG = "LocalANRWatchdog"
        private const val ANR_DIR = "dev_diagnostics/anr"

        @Volatile
        private var instance: LocalANRWatchdog? = null

        fun start(context: Context, timeoutMs: Long = 2500L) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val watchdog = LocalANRWatchdog(context.applicationContext, timeoutMs)
                        watchdog.isDaemon = true
                        watchdog.start()
                        instance = watchdog
                        Log.d(TAG, "LocalANRWatchdog started with timeout: ${timeoutMs}ms")
                    }
                }
            }
        }

        fun getAnrDir(context: Context): File {
            val dir = File(context.filesDir, ANR_DIR)
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

        fun getLatestAnrReport(context: Context): String? {
            val dir = getAnrDir(context)
            val files = dir.listFiles { _, name -> name.endsWith(".txt") }?.sortedByDescending { it.lastModified() }
            return files?.firstOrNull()?.readText()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var tick = 0L
    @Volatile
    private var reported = false

    private val ticker = Runnable {
        tick = (tick + 1) % Long.MAX_VALUE
        reported = false
    }

    override fun run() {
        while (!isInterrupted) {
            val lastTick = tick
            mainHandler.post(ticker)

            try {
                sleep(timeoutMs)
            } catch (e: InterruptedException) {
                break
            }

            // إذا لم يتغير الـ tick، فهذا يعني أن الـ Main Thread لم ينفذ الدالة وتجمد
            if (tick == lastTick) {
                if (!reported) {
                    reported = true
                    handleAnrDetected()
                }
            }
        }
    }

    private fun handleAnrDetected() {
        try {
            val mainThread = Looper.getMainLooper().thread
            val mainStackTrace = mainThread.stackTrace

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val nowStr = dateFormat.format(Date())

            val report = buildString {
                appendLine("==========================================")
                appendLine("       WHATSUP LOCAL DEV ANR REPORT       ")
                appendLine("==========================================")
                appendLine("الوقت: $nowStr")
                appendLine("السبب: تجمد المسار الرئيسي (Main Thread Frozen > ${timeoutMs}ms)")
                appendLine("------------------------------------------")
                appendLine("تتبع المسار الرئيسي (Main Thread StackTrace):")
                mainStackTrace.forEach { element ->
                    appendLine("    at $element")
                }
                appendLine("==========================================")
            }

            val dir = getAnrDir(context)
            val reportFile = File(dir, "anr_${System.currentTimeMillis()}.txt")
            reportFile.writeText(report)
            Log.w(TAG, "ANR Detected! Report saved to: ${reportFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling ANR detection: ${e.message}", e)
        }
    }
}
