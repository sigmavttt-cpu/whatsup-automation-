#!/usr/bin/env node

/**
 * أداة توليد مفاتيح واشتراكات WhatsUp Automation للأدمن.
 * الاستخدام:
 *   node scripts/generate_license.cjs [المدة_بالأيام] [ملاحظة/اسم_المشتري]
 * 
 * مثال:
 *   node scripts/generate_license.cjs 30 "أحمد - تيليجرام"
 */

const https = require('https');

const SUPABASE_URL = "https://xalmbtjmxjgombbortlz.supabase.co";
const ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhhbG1idGpteGpnb21iYm9ydGx6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE1NTE3MDAsImV4cCI6MjEwNzEyNzcwMH0.RSRxfLXSv_31k8ZhUQJvp-tJZLURMqllbZAqMRdVB0E";

const days = parseInt(process.argv[2] || "30", 10);
const notes = process.argv[3] || "كود تم توليده بواسطة الأدمن";

const postData = JSON.stringify({
    p_days: days,
    p_notes: notes
});

const url = new URL(`${SUPABASE_URL}/rest/v1/rpc/admin_generate_license`);

const req = https.request(url, {
    method: 'POST',
    headers: {
        'apikey': ANON_KEY,
        'Authorization': `Bearer ${ANON_KEY}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
    }
}, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        try {
            const json = JSON.parse(data);
            if (json.success && json.license_key) {
                console.log("\n=======================================================");
                console.log("       🎉 تم توليد كود اشتراك جديد بنجاح!            ");
                console.log("=======================================================");
                console.log(`🔑 كود التفعيل: ${json.license_key}`);
                console.log(`⏱️ مدة الاشتراك: ${json.duration_days} يوماً`);
                console.log(`📝 الملاحظات:   ${json.notes || 'لا يوجد'}`);
                console.log(`📅 تاريخ الإنشاء: ${json.created_at}`);
                console.log("=======================================================");
                console.log(`👉 انسخ الكود وأرسله للعميل مباشرة: ${json.license_key}\n`);
            } else {
                console.error("❌ فشل توليد الكود:", data);
            }
        } catch (e) {
            console.error("❌ خطأ في معالجة الرد:", data);
        }
    });
});

req.on('error', (e) => {
    console.error("❌ خطأ في الاتصال بالسحابة:", e.message);
});

req.write(postData);
req.end();
