package com.whatsup.automation.data.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.whatsup.automation.data.engine.WhatsAppEngine
import com.whatsup.automation.data.local.AppDatabase
import com.whatsup.automation.service.WhatsAppForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير بروتوكول التهدئة والإغلاق الآمن (Graceful Teardown Protocol).
 * يضمن إيقاف الخدمات، وحفظ ومزامنة قواعد البيانات (WAL Flush)،
 * وإنهاء العمليات الخلفية بسلاسة قبل إطلاق معالج تثبيت تحديث الـ APK.
 */
@Singleton
class GracefulTeardownManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whatsAppEngine: WhatsAppEngine,
    private val appDatabase: AppDatabase
) {

    companion object {
        private const val TAG = "GracefulTeardownManager"
    }

    /**
     * تنفيذ دورة التهدئة الكاملة والآمنة قبل ترقية وتثبيت التطبيق.
     */
    suspend fun executeTeardown(onProgress: (stage: String) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "=== Starting Graceful Teardown for App Update ===")
            // تفعيل راية تجميد الخدمة لمنع onTaskRemoved من إحياء الخدمة أثناء التثبيت
            WhatsAppForegroundService.isUpdateInstallationInProgress = true

            // الخطوة 1: تجميد الأتمتة وإيقاف محرك واتساب والـ Node Runner
            onProgress("إيقاف محرك الأتمتة وجسر الاتصال...")
            try {
                whatsAppEngine.setAutomationPaused(true)
                whatsAppEngine.stopEngine()
                Log.d(TAG, "Step 1: WhatsApp engine stopped.")
            } catch (e: Exception) {
                Log.w(TAG, "Step 1 warning: ${e.message}")
            }

            delay(250L)

            // الخطوة 2: إيقاف الخدمة الأمامية الدائمة
            onProgress("إيقاف الخدمات الأمامية وخوادم الخلفية...")
            try {
                WhatsAppForegroundService.stop(context)
                Log.d(TAG, "Step 2: Foreground service stopped.")
            } catch (e: Exception) {
                Log.w(TAG, "Step 2 warning: ${e.message}")
            }

            delay(250L)

            // الخطوة 3: تفريغ ومزامنة سجلات قاعدة بيانات Room (WAL Checkpoint)
            onProgress("حفظ ومزامنة كافة السجلات وقواعد البيانات...")
            try {
                val db = appDatabase.openHelper.writableDatabase
                // تشغيل WAL Checkpoint لدمج كافة البيانات المكتوبة في ملف قاعدة البيانات الرئيسي
                db.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL);"))?.close()
                Log.d(TAG, "Step 3: Database WAL flushed and checkpointed.")
            } catch (e: Exception) {
                Log.w(TAG, "Step 3 warning: ${e.message}")
            }

            delay(150L)

            // الخطوة 4: إلغاء أي منبهات أو مجدولات نشطة مؤقتاً لتجنب إيقاظ الخدمة أثناء التثبيت
            onProgress("تعليق المجدولات الزمنية المؤقتة...")
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val restartIntent = Intent(context, WhatsAppForegroundService::class.java).apply {
                    action = WhatsAppForegroundService.ACTION_START
                }
                val restartPendingIntent = PendingIntent.getService(
                    context,
                    101,
                    restartIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (restartPendingIntent != null && alarmManager != null) {
                    alarmManager.cancel(restartPendingIntent)
                    restartPendingIntent.cancel()
                    Log.d(TAG, "Step 4: Pending restart alarms cancelled.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Step 4 warning: ${e.message}")
            }

            delay(150L)

            onProgress("جاهز للتثبيت الفوري 🚀")
            Log.i(TAG, "=== Graceful Teardown completed successfully ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Graceful Teardown encountered error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
