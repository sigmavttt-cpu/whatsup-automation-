package com.whatsup.automation.data.remote.update

/**
 * إعدادات وثوابت الاتصال بخادم Supabase لنظام التحديثات المؤسسي.
 */
object SupabaseUpdateConfig {
    // نقطة النهاية المعتمدة لخادم Supabase الخاص بالمشروع
    const val DEFAULT_SUPABASE_URL = "https://xalmbtjmxjgombbortlz.supabase.co"
    const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhhbG1idGpteGpnb21iYm9ydGx6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE1NTE3MDAsImV4cCI6MjEwNzEyNzcwMH0.RSRxfLXSv_31k8ZhUQJvp-tJZLURMqllbZAqMRdVB0E"
    
    // أسماء الجداول والمسارات
    const val TABLE_APP_UPDATES = "app_updates"
    const val REST_PATH = "/rest/v1"
    
    // مهل الاتصال (Timeouts)
    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 15_000
    const val DOWNLOAD_READ_TIMEOUT_MS = 30_000
}
