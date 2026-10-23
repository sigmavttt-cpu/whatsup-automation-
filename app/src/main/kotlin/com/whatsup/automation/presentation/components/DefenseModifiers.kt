package com.whatsup.automation.presentation.components

import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.theme.DarkBgSecondary
import com.whatsup.automation.presentation.theme.DarkSurfaceElevated
import com.whatsup.automation.presentation.theme.DarkSurfaceHover
import com.whatsup.automation.presentation.theme.GlassBorderHighlight
import com.whatsup.automation.presentation.theme.GlassBorderSubtle
import com.whatsup.automation.presentation.theme.LiquidEmeraldGradient
import com.whatsup.automation.presentation.theme.TextMuted
import com.whatsup.automation.presentation.theme.TextPrimary

/**
 * أنماط الزر الآمن الحصين (Safe Action Button Style).
 */
enum class SafeButtonStyle {
    /** زمردي نيون فخم — مناسب لعمليات الحفظ والإرسال الأساسية */
    Primary,
    /** رمادي زجاجي شفاف — مناسب للعمليات الثانوية والإلغاء */
    Secondary,
    /** قرمزي نيون حذر — مناسب لعمليات الحذف والتصفير التدميرية */
    Destructive
}

/**
 * دالة امتداد لحماية عناصر الواجهة من النقر المزدوج أو التكرار السريع (Double-Click Prevention).
 * تضمن عدم تنفيذ النقر إلا بعد انقضاء المهلة الزمنية [debounceIntervalMs] (افتراضياً 600ms).
 *
 * @param debounceIntervalMs المهلة الزمنية بالمللي ثانية بين النقرات المتتالية لمنع التكرار العرضي.
 * @param enabled تحديد ما إذا كان العنصر قابلاً للنقر والتفاعل.
 * @param onClickLabel وصف إمكانية الوصول للنقر (Accessibility Label).
 * @param role دور العنصر الدلالي (مثل Role.Button).
 * @param onClick الدالة المطلوب تنفيذها بأمان عند النقر.
 */
fun Modifier.debouncedClickable(
    debounceIntervalMs: Long = 600L,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableStateOf(0L) }
    val currentOnClick by rememberUpdatedState(onClick)

    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= debounceIntervalMs) {
            lastClickTime = currentTime
            currentOnClick()
        }
    }
}

/**
 * تحميل إضافي لدالة [debouncedClickable] مع تخصيص [MutableInteractionSource] ومؤشر التفاعل [Indication].
 */
fun Modifier.debouncedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    debounceIntervalMs: Long = 600L,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableStateOf(0L) }
    val currentOnClick by rememberUpdatedState(onClick)

    this.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= debounceIntervalMs) {
            lastClickTime = currentTime
            currentOnClick()
        }
    }
}

/**
 * زر تفاعلي آمن وحصين (Safe Action Button) مدعوم بحصانة ضد النقر المزدوج ومؤشر تحميل مدمج.
 * متوافق كلياً مع فلسفة التصميم السيبرانية الفاخرة (Cyber Jade Dark Glassmorphism).
 *
 * @param text نص الزر الرئيسي.
 * @param onClick إجراء النقر المحمي ضد التكرار السريع.
 * @param modifier مخصص الواجهة للزر.
 * @param isLoading حالة المعالجة الحالية؛ عند تفعيلها يُعطل الزر ويظهر مؤشر تحميل مدمج.
 * @param enabled تمكين أو تعطيل الزر.
 * @param debounceIntervalMs المهلة الزمنية لمنع النقر المزدوج (افتراضياً 600ms).
 * @param icon أيقونة اختيارية بجانب النص.
 * @param style النمط البصري للزر (Primary, Secondary, Destructive).
 * @param shape شكل الحواف المنحنية للزر (افتراضياً 16.dp).
 * @param contentPadding حشوة المحتوى الداخلي للزر.
 * @param loadingText نص اختياري بديل يُعرض أثناء التحميل (افتراضياً يُعرض النص الأصلي بجانب المؤشر).
 */
@Composable
fun SafeActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    debounceIntervalMs: Long = 600L,
    icon: ImageVector? = null,
    style: SafeButtonStyle = SafeButtonStyle.Primary,
    shape: Shape = RoundedCornerShape(16.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
    loadingText: String? = null
) {
    val isInteractive = enabled && !isLoading
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "safeButtonScale"
    )

    // إعداد التدرج اللوني والحدود والظلال حسب النمط المختار
    val backgroundBrush = when {
        !enabled -> Brush.linearGradient(listOf(DarkSurfaceHover, DarkSurfaceHover))
        style == SafeButtonStyle.Primary -> LiquidEmeraldGradient
        style == SafeButtonStyle.Destructive -> Brush.linearGradient(
            listOf(Color(0xFF881337), Color(0xFFBE123C), Color(0xFFF43F5E))
        )
        style == SafeButtonStyle.Secondary -> Brush.linearGradient(
            listOf(DarkSurfaceElevated, DarkBgSecondary)
        )
        else -> LiquidEmeraldGradient
    }

    val borderBrush = when {
        !enabled -> Brush.horizontalGradient(listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF)))
        style == SafeButtonStyle.Primary -> Brush.horizontalGradient(
            listOf(Color(0x80FFFFFF), Color(0x4000E676), Color(0x1AFFFFFF))
        )
        style == SafeButtonStyle.Destructive -> Brush.horizontalGradient(
            listOf(Color(0x80FFFFFF), Color(0x4DF43F5E), Color(0x1AFFFFFF))
        )
        style == SafeButtonStyle.Secondary -> Brush.horizontalGradient(
            listOf(GlassBorderHighlight, GlassBorderSubtle)
        )
        else -> Brush.horizontalGradient(listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF)))
    }

    val shadowColor = when {
        !enabled || isLoading -> Color.Transparent
        style == SafeButtonStyle.Primary -> Color(0x3300C853)
        style == SafeButtonStyle.Destructive -> Color(0x33F43F5E)
        style == SafeButtonStyle.Secondary -> Color(0x1A000000)
        else -> Color.Transparent
    }

    val contentColor = when {
        !enabled -> TextMuted
        style == SafeButtonStyle.Secondary -> TextPrimary
        else -> Color.White
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isInteractive) 6.dp else 0.dp,
                shape = shape,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(width = 1.2.dp, brush = borderBrush, shape = shape)
            .debouncedClickable(
                interactionSource = interactionSource,
                indication = null,
                debounceIntervalMs = debounceIntervalMs,
                enabled = isInteractive,
                role = Role.Button
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = if (isLoading && loadingText != null) loadingText else text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * زر أيقونة آمن وحصين (Safe Icon Button) مدعوم بحصانة ضد النقر المزدوج ومؤشر تحميل مصغر.
 */
@Composable
fun SafeIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    debounceIntervalMs: Long = 600L,
    tint: Color = TextPrimary,
    backgroundColor: Color = Color(0x1F0F1A22),
    borderColor: Color = GlassBorderSubtle,
    size: Dp = 48.dp
) {
    val isInteractive = enabled && !isLoading
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "safeIconButtonScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) backgroundColor else DarkSurfaceHover)
            .border(1.dp, if (enabled) borderColor else Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .debouncedClickable(
                interactionSource = interactionSource,
                indication = null,
                debounceIntervalMs = debounceIntervalMs,
                enabled = isInteractive,
                role = Role.Button
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = tint,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
