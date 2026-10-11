package com.whatsup.automation

import android.app.Application
import com.whatsup.automation.data.diagnostics.LocalANRWatchdog
import com.whatsup.automation.data.diagnostics.LocalCrashWatcher
import dagger.hilt.android.HiltAndroidApp

/**
 * نقطة الدخول الرئيسية للتطبيق — مزودة بـ Hilt وحراس التشخيص والأمان المحلي.
 */
@HiltAndroidApp
class WhatsUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // تثبيت حارس الأخطاء والانهيارات المحلي للمطور
        LocalCrashWatcher.install(this)
        // تشغيل حارس عدم الاستجابة وتجمد الشاشة (ANR Watchdog)
        LocalANRWatchdog.start(this)
    }
}
