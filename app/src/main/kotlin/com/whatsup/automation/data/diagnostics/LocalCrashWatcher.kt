package com.whatsup.automation.data.diagnostics

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.whatsup.automation.presentation.screens.diagnostics.CrashReportActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * حارس الأخطاء والانهيارات المحلي الشامل (Local Crash Watcher).
 * يعترض الاستثناءات غير المعالجة، يسجل التقرير محلياً 100% بدون سحابة،
 * ويفتح شاشة تشخيص احترافية للمطور بدلاً من إغلاق التطبيق فجأة.
 */
class LocalCrashWatcher private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "LocalCrashWatcher"
        private const val CRASH_DIR = "dev_diagnostics/crashes"

        fun install(application: Application) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (defaultHandler !is LocalCrashWatcher) {
                Thread.setDefaultUncaughtExceptionHandler(LocalCrashWatcher(application, defaultHandler))
                Log.d(TAG, "LocalCrashWatcher installed successfully.")
            }
        }

        fun getCrashDir(context: Context): File {
            val dir = File(context.filesDir, CRASH_DIR)
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

        fun getLatestCrashReport(context: Context): String? {
            val dir = getCrashDir(context)
            val files = dir.listFiles { _, name -> name.endsWith(".txt") }?.sortedByDescending { it.lastModified() }
            return files?.firstOrNull()?.readText()
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            val stackTrace = stringWriter.toString()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val nowStr = dateFormat.format(Date())

            val runtime = Runtime.getRuntime()
            val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
            val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
            val usedMemoryMb = totalMemoryMb - freeMemoryMb

            val report = buildString {
                appendLine("==========================================")
                appendLine("       WHATSUP LOCAL DEV CRASH REPORT      ")
                appendLine("==========================================")
                appendLine("الوقت: $nowStr")
                appendLine("الخيط: ${thread.name} (ID: ${thread.id})")
                appendLine("الجهاز: ${Build.MANUFACTURER} ${Build.MODEL} (SDK: ${Build.VERSION.SDK_INT})")
                appendLine("الذاكرة: مستخدم $usedMemoryMb MB / إجمالي $totalMemoryMb MB")
                appendLine("نوع الاستثناء: ${throwable.javaClass.name}")
                appendLine("الرسالة: ${throwable.message ?: "بدون رسالة"}")
                appendLine("------------------------------------------")
                appendLine("تتبع الأكواد (StackTrace):")
                appendLine(stackTrace)
                appendLine("==========================================")
            }

            // حفظ التقرير محلياً على الهاتف
            val dir = getCrashDir(context)
            val reportFile = File(dir, "crash_${System.currentTimeMillis()}.txt")
            reportFile.writeText(report)
            Log.e(TAG, "Uncaught Exception logged to: ${reportFile.absolutePath}")

            // فتح شاشة تشخيص المطور في مهمة جديدة
            val intent = Intent(context, CrashReportActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, report)
                putExtra(CrashReportActivity.EXTRA_EXCEPTION_CLASS, throwable.javaClass.simpleName)
                putExtra(CrashReportActivity.EXTRA_ERROR_MESSAGE, throwable.message ?: "خطأ غير متوقع")
            }
            context.startActivity(intent)

            // إنهاء العملية الحالية بأمان
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        } catch (e: Exception) {
            Log.e(TAG, "Failed in LocalCrashWatcher: ${e.message}", e)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
