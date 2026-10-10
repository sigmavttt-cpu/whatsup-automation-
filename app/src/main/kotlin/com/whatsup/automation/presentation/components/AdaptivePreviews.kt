package com.whatsup.automation.presentation.components

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * معمل فحص الشاشات الآلي (Adaptive Device Matrix Previews).
 * يُمكّن المطور والوكيل من التحقق اللحظي من تجاوب المكونات
 * عبر مختلف المقاسات والأبعاد ونسب تكبير الخطوط والوضع الليلي.
 */

@Preview(
    name = "1. Small Phone (320dp)",
    widthDp = 320,
    heightDp = 640,
    showBackground = true,
    backgroundColor = 0xFF0D1117,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class SmallPhonePreview

@Preview(
    name = "2. Normal Phone (393dp)",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF0D1117,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class NormalPhonePreview

@Preview(
    name = "3. Large Phone / Foldable (480dp)",
    widthDp = 480,
    heightDp = 900,
    showBackground = true,
    backgroundColor = 0xFF0D1117,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class LargePhonePreview

@Preview(
    name = "4. High Accessibility Font Scale (1.5x)",
    widthDp = 360,
    heightDp = 740,
    fontScale = 1.5f,
    showBackground = true,
    backgroundColor = 0xFF0D1117,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class LargeFontScalePreview

/**
 * حزمة معاينة متكاملة تفحص الأبعاد الأربعة الحرجة دفعة واحدة.
 */
@SmallPhonePreview
@NormalPhonePreview
@LargePhonePreview
@LargeFontScalePreview
annotation class DeviceMatrixPreviews
