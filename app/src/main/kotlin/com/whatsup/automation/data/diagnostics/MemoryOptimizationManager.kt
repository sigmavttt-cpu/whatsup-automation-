package com.whatsup.automation.data.diagnostics

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Process
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * مدير تحسين الذاكرة والوقاية الاستباقية من تسريب الذاكرة ونفادها (Memory Optimization & Leak Guard).
 * يستمع لإشارات ضغط الذاكرة من نظام أندرويد (onTrimMemory / onLowMemory) ويفرغ الموارد المؤقتة تلقائياً.
 */
class MemoryOptimizationManager private constructor(
    private val context: Context
) : ComponentCallbacks2 {

    data class MemorySnapshot(
        val totalMemoryMb: Long,
        val availableMemoryMb: Long,
        val usedHeapMb: Long,
        val maxHeapMb: Long,
        val percentUsed: Int,
        val isLowMemory: Boolean
    )

    companion object {
        private const val TAG = "MemoryOptimization"
        private const val MEMORY_LOGS_DIR = "dev_diagnostics/memory"

        @Volatile
        private var instance: MemoryOptimizationManager? = null

        private val cleanupCallbacks = CopyOnWriteArrayList<() -> Unit>()

        fun install(context: Context): MemoryOptimizationManager {
            return instance ?: synchronized(this) {
                instance ?: MemoryOptimizationManager(context.applicationContext).also {
                    context.applicationContext.registerComponentCallbacks(it)
                    instance = it
                    Log.i(TAG, "MemoryOptimizationManager installed successfully")
                }
            }
        }

        fun registerCleanupAction(action: () -> Unit) {
            cleanupCallbacks.add(action)
        }

        fun unregisterCleanupAction(action: () -> Unit) {
            cleanupCallbacks.remove(action)
        }

        fun getMemorySnapshot(context: Context): MemorySnapshot {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)

            val runtime = Runtime.getRuntime()
            val maxHeap = runtime.maxMemory() / (1024 * 1024)
            val totalHeap = runtime.totalMemory() / (1024 * 1024)
            val freeHeap = runtime.freeMemory() / (1024 * 1024)
            val usedHeap = totalHeap - freeHeap

            val percentUsed = if (maxHeap > 0) ((usedHeap.toDouble() / maxHeap) * 100).toInt() else 0

            return MemorySnapshot(
                totalMemoryMb = memoryInfo.totalMem / (1024 * 1024),
                availableMemoryMb = memoryInfo.availMem / (1024 * 1024),
                usedHeapMb = usedHeap,
                maxHeapMb = maxHeap,
                percentUsed = percentUsed,
                isLowMemory = memoryInfo.lowMemory
            )
        }
    }

    override fun onTrimMemory(level: Int) {
        val levelName = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE"
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            else -> "UNKNOWN($level)"
        }

        Log.w(TAG, "onTrimMemory received level: $levelName")

        // تنفيذ خطافات التنظيف المسجلة
        for (action in cleanupCallbacks) {
            try {
                action.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error executing cleanup action: ${e.message}")
            }
        }

        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // عند إخفاء الواجهة، طلب تنظيف مؤقت للذاكرة غير المستخدمة
                System.gc()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // حالة حرجة: توثيق تقرير بالذاكرة وطلب تفريغ قسري
                logMemoryIncident("TRIM_MEMORY_$levelName")
                System.gc()
            }
        }
    }

    override fun onLowMemory() {
        Log.e(TAG, "onLowMemory triggered by OS! Performing emergency memory cleanup.")
        for (action in cleanupCallbacks) {
            try {
                action.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Emergency cleanup failed: ${e.message}")
            }
        }
        logMemoryIncident("CRITICAL_ON_LOW_MEMORY")
        System.gc()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // لا حاجة لإجراء هنا
    }

    private fun logMemoryIncident(reason: String) {
        try {
            val dir = File(context.filesDir, MEMORY_LOGS_DIR)
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val file = File(dir, "memory_${timestamp}.log")

            val snapshot = getMemorySnapshot(context)
            val content = buildString {
                appendLine("=== Memory Pressure Incident Report ===")
                appendLine("Timestamp: $timestamp")
                appendLine("Trigger: $reason")
                appendLine("PID: ${Process.myPid()}")
                appendLine("Used Heap: ${snapshot.usedHeapMb} MB / ${snapshot.maxHeapMb} MB (${snapshot.percentUsed}%)")
                appendLine("Device Available RAM: ${snapshot.availableMemoryMb} MB / ${snapshot.totalMemoryMb} MB")
                appendLine("Device Low Memory Flag: ${snapshot.isLowMemory}")
            }

            file.writeText(content)
            Log.i(TAG, "Memory incident report saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log memory incident: ${e.message}")
        }
    }
}
