package com.whatsup.automation.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * مساعد التغذية اللمسية الاهتزازية الموحد (Unified App Haptic Feedback).
 * يمنح المستخدم إحساساً فيزيائياً فخماً ويقيناً لحظياً عند الضغط أو التبديل أو النجاح.
 */
class AppHapticFeedback(private val haptic: HapticFeedback) {

    /** نقرة عادية خفيفة (أزرار، بطاقات) */
    fun click() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** ضغطة تبديل المفاتيح (Switches & Toggles) */
    fun toggle() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** ضغطة مطولة (Long Press) */
    fun longPress() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** تأكيد نجاح العملية (Success Feedback) */
    fun success() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** تنبيه أو تحذير (Warning / Error) */
    fun warning() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * دالة ملائمة لاسترجاع المساعد اللمسي داخل أي Composable
 */
@Composable
fun rememberAppHaptic(): AppHapticFeedback {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { AppHapticFeedback(haptic) }
}
