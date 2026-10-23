#!/usr/bin/env node

/**
 * أداة توليد تراخيص الهاش المشفرة الرسمية لتطبيق WhatsUp AutoFlow.
 * 
 * الاستخدام:
 *   node tools/generate_license.js <DEVICE_FINGERPRINT> <DAYS> [PLAN_TYPE]
 * 
 * أمثلة:
 *   node tools/generate_license.js 4B7E6F8A1C2D3E4F... 30 VIP
 *   node tools/generate_license.js ALL 365 ENTERPRISE
 *   node tools/generate_license.js ALL 0 LIFETIME
 */

const crypto = require('crypto');

const MASTER_SALT = "WUP_SIGMA_AUTOFLOW_2026_MASTER_SECRET_KEY_XY987";
const PREFIX = "SIGMA-";

function generateLicenseToken(deviceId, durationDays, planType = "VIP") {
    if (!deviceId) {
        throw new Error("معرّف الجهاز (Device Fingerprint) مطلوب!");
    }

    const cleanDevice = deviceId.trim().toUpperCase();
    const cleanPlan = (planType || "VIP").trim().toUpperCase();
    const days = parseInt(durationDays, 10);

    const now = Date.now();
    const expiresAt = (!isNaN(days) && days > 0) ? (now + (days * 24 * 60 * 60 * 1000)) : 0;

    const rawPayload = `${cleanDevice}|${expiresAt}|${cleanPlan}|${now}`;
    const payloadBase64 = Buffer.from(rawPayload, 'utf8').toString('base64url');

    const hmac = crypto.createHmac('sha256', MASTER_SALT);
    hmac.update(payloadBase64);
    const signatureHex = hmac.digest('hex').toUpperCase();

    const fullToken = `${PREFIX}${payloadBase64}.${signatureHex}`;

    return {
        token: fullToken,
        deviceId: cleanDevice,
        days: days > 0 ? days : "دائم (Lifetime)",
        planType: cleanPlan,
        expiresDate: expiresAt > 0 ? new Date(expiresAt).toISOString().split('T')[0] : "غير محدد (مدى الحياة)",
        length: fullToken.length
    };
}

// تشغيل الأداة عبر سطر الأوامر (CLI)
const args = process.argv.slice(2);

if (args.length < 2) {
    console.log(`
============================================================
   🔑 أداة توليد تراخيص الهاش المشفرة - WhatsUp AutoFlow
============================================================
طريقة الاستخدام:
  node tools/generate_license.js <بصمة_الجهاز> <عدد_الأيام> [نوع_الباقة]

أمثلة:
  node tools/generate_license.js A1B2C3D4E5... 30 VIP
  node tools/generate_license.js ALL 365 ENTERPRISE
  node tools/generate_license.js ALL 0 LIFETIME
============================================================
`);
    process.exit(1);
}

const targetDevice = args[0];
const targetDays = args[1];
const targetPlan = args[2] || "VIP";

try {
    const result = generateLicenseToken(targetDevice, targetDays, targetPlan);
    console.log(`
============================================================
   ✅ تم إنشاء هاش الترخيص المشفر بنجاح 🚀
============================================================
بصمة الجهاز المربوط : ${result.deviceId}
نوع الباقة         : ${result.planType}
المدة بالـأيام       : ${result.days}
تاريخ الانتهاء      : ${result.expiresDate}
طول نص الهاش       : ${result.length} حرفاً
------------------------------------------------------------
النص المشفر للترخيص (قم بنسخه وإرساله للعميل):

${result.token}

============================================================
`);
} catch (err) {
    console.error("❌ خطأ أثناء توليد الترخيص:", err.message);
    process.exit(1);
}
