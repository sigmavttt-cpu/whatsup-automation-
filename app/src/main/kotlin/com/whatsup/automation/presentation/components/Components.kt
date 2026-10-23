package com.whatsup.automation.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.ConnectionState
import com.whatsup.automation.presentation.theme.*

/**
 * بطاقة بنتو زجاجية عصرية ثلاثية الأبعاد (Bento Glass Card) بهيكل منحني فاخر وحواف متوهجة.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderBrush: Brush = ObsidianGlowBorder.Emerald,
    backgroundColor: Color = DarkSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bentoScale"
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor, backgroundColor.copy(alpha = 0.95f))
                )
            )
            .border(1.5.dp, borderBrush, RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                    )
                } else Modifier
            )
            .padding(20.dp)
    ) {
        Column(content = content)
    }
}

/**
 * بطاقة زجاجية فاخرة (Glassmorphic Card) بهيكل جديد عالي الأناقة وحواف سيان متوهجة.
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderBrush: Brush = ObsidianGlowBorder.Cyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(DarkSurface)
            .border(1.5.dp, borderBrush, RoundedCornerShape(cornerRadius))
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}

/**
 * شارة علامة الصح الفاخرة ذات التوهج النيوني (Luxury Neon Checkmark Badge).
 */
@Composable
fun LuxuryCheckmarkBadge(
    modifier: Modifier = Modifier,
    text: String? = null,
    isVerified: Boolean = true,
    accentColor: Color = CheckmarkGreen
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVerified) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        if (!text.isNullOrBlank()) {
            Text(
                text = text,
                color = accentColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * زر متدفق بتدرج زمردي مائي (Liquid Gradient Button) مع لمعان مائي ناعم وحركة ارتدادية عند الضغط.
 */
@Composable
fun LiquidGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    gradient: Brush = LiquidEmeraldGradient
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "buttonScale"
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x3300C853)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) gradient else Brush.linearGradient(listOf(DarkSurfaceHover, DarkSurfaceHover))
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x80FFFFFF),
                        Color(0x3300E676),
                        Color(0x1AFFFFFF)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                color = if (enabled) Color.White else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * زر محدد بإطار فاخر نيون ومظهر زجاجي شفاف (Luxury Outlined Button).
 */
@Composable
fun LuxuryOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    borderBrush: Brush = ObsidianGlowBorder.Emerald,
    textColor: Color = WhatsAppGreenDark
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnOutlinedScale"
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) textColor else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                color = if (enabled) textColor else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * زر متدرج أنيق بنمط Cyber Emerald (للتوافق العام).
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    LiquidGradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        gradient = EmeraldGradient
    )
}

/**
 * مفتاح كبسولي منزلق فائق السلاسة (Segmented Pill Selector) للتنقل بين الخيارات.
 */
@Composable
fun <T> SegmentedPillSelector(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = WhatsAppGreen
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, Color(0x3300A884))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (option, label) ->
                val isSelected = option == selectedOption
                val bgTint by animateColorAsState(
                    targetValue = if (isSelected) activeColor else Color.Transparent,
                    animationSpec = tween(250),
                    label = "pillBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextPrimary,
                    animationSpec = tween(250),
                    label = "pillText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgTint)
                        .clickable {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOptionSelected(option)
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * المفاعل المركزي النابض (Pulse Reactor) لحالة الاتصال والمحرك الحي.
 */
@Composable
fun PulseReactor(
    isActive: Boolean,
    activeColor: Color = WhatsAppGreen,
    inactiveColor: Color = AmberWarning,
    modifier: Modifier = Modifier
) {
    val currentColor = if (isActive) activeColor else inactiveColor
    val infiniteTransition = rememberInfiniteTransition(label = "reactorPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.35f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactorScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isActive) 0.8f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactorAlpha"
    )

    Box(
        modifier = modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // هالة متوهجة خارجية — تُنفّذ في مرحلة الرسم عبر graphicsLayer لمنع إعادة تركيب الكومبوز واستهلاك المعالج
        Box(
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha * 0.4f
                }
                .clip(CircleShape)
                .background(currentColor)
        )
        // قلب المفاعل الداخلي
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(currentColor)
        )
    }
}

/**
 * شارة حالة الاتصال مع نقطة نبضية متحركة.
 */
@Composable
fun ConnectionStatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (connectionState) {
        is ConnectionState.Connected -> "متصل ونشط" to WhatsAppGreen
        is ConnectionState.AwaitingPairing -> "في انتظار الاقتران" to AmberWarning
        is ConnectionState.Reconnecting -> "جارٍ إعادة الاتصال" to CyberCyan
        is ConnectionState.Disconnected -> "غير متصل" to TextMuted
        is ConnectionState.Error -> "خطأ بالاتصال" to RoseError
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PulseReactor(
            isActive = connectionState is ConnectionState.Connected,
            activeColor = statusColor,
            inactiveColor = statusColor
        )
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * حلقة الحالة الدائرية المتوهجة (Story Avatar Ring) لجهات الاتصال والحالات المرصودة.
 */
@Composable
fun StoryAvatarRing(
    initials: String,
    isViewed: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null
) {
    val ringBrush = if (isViewed) {
        Brush.linearGradient(listOf(WhatsAppGreen, CyberCyan))
    } else {
        Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF)))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, ringBrush, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(DarkSurfaceElevated)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = if (isViewed) WhatsAppGreenLight else TextSecondary,
            fontSize = (size.value * 0.35).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * بطاقة إحصائية لعرض العدادات الحية في لوحة التحكم (مع تأثير Bento وتفاعل لمسي).
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    BentoCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        borderBrush = Brush.linearGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), Color(0x0EFFFFFF))
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = value,
                fontSize = if (value.length > 5) 18.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 11.5.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/**
 * طرفية محاكاة Terminal لعرض السجلات والأوامر البرمجية الحية.
 */
@Composable
fun TerminalConsole(
    lines: List<String>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF04070E))
            .border(1.dp, Color(0x2800F2FE), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyberCyan))
                    Text(
                        text = "سجل الأتمتة المباشر (Terminal)",
                        fontSize = 12.sp,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RoseError))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AmberWarning))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WhatsAppGreen))
                }
            }

            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                lines.takeLast(6).forEach { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (line.contains("✅") || line.contains("تم") || line.contains("CONNECTED")) WhatsAppGreenLight else TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * بطاقة حارس الخدمة ومراقب الأداء الحي (Live Service Health & Watchdog Card).
 */
@Composable
fun ServiceHealthCard(
    snapshot: com.whatsup.automation.service.SystemHealthSnapshot,
    modifier: Modifier = Modifier,
    onRestartService: (() -> Unit)? = null
) {
    val isHealthy = snapshot.isForegroundServiceRunning && snapshot.connectionState is ConnectionState.Connected
    val glowBrush = if (isHealthy) ObsidianGlowBorder.Emerald else ObsidianGlowBorder.Amber

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        borderBrush = glowBrush
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    val statusColor = if (snapshot.isForegroundServiceRunning) WhatsAppGreen else RoseError
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer {
                                this.alpha = alpha
                            }
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = "حارس الخدمة ومراقبة الأداء",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (snapshot.isForegroundServiceRunning) "نشط 24/7" else "متوقف",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (snapshot.isForegroundServiceRunning) WhatsAppGreenLight else RoseError,
                    maxLines = 1
                )
            }

            HorizontalDivider(color = Color(0x14FFFFFF), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // وقت التشغيل
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "مدة التشغيل",
                        fontSize = 10.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = snapshot.uptimeFormatted,
                        fontSize = 13.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        maxLines = 1
                    )
                }

                // استهلاك الذاكرة
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الذاكرة (RAM)",
                        fontSize = 10.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${snapshot.usedMemoryMb}MB (${snapshot.memoryUsagePercent}%)",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (snapshot.memoryUsagePercent > 80) AmberWarning else WhatsAppGreenLight,
                        maxLines = 1
                    )
                }

                // حالة الأتمتة
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "الأتمتة",
                        fontSize = 10.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (snapshot.isAutomationActive) "مفعّلة ⚡" else "مؤقتة ⏸️",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (snapshot.isAutomationActive) WhatsAppGreenLight else AmberWarning,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * مكون التلميحات الفاخر (Glassmorphic Luxury Tooltip).
 * يوفر تلميحاً إرشادياً ذكياً يظهر عند الضغط المطول أو النقر على الأيقونات والأزرار.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTooltipBox(
    tooltipText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (tooltipText.isBlank()) {
        content()
        return
    }

    val tooltipState = rememberTooltipState(isPersistent = false)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(spacingBetweenTooltipAndAnchor = 6.dp),
        tooltip = {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkBgSecondary.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(WhatsAppGreen.copy(alpha = 0.6f), CyberCyan.copy(alpha = 0.6f)))),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = tooltipText,
                    color = TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        },
        state = tooltipState,
        modifier = modifier
    ) {
        content()
    }
}

/**
 * شريط الرأس القياسي الموحد لجميع شاشات التطبيق (Unified App Screen Header).
 * يوفر تصميماً أندرويد حقيقياً ونظيفاً: زر القائمة الجانبية (≡) + العنوان والوصف + أزرار الإجراءات والشارات.
 */
@Composable
fun AppScreenHeader(
    title: String,
    subtitle: String? = null,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (onOpenDrawer != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenDrawer()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xD90F1A22))
                        .border(1.2.dp, Color(0x6600E676), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "القائمة الجانبية",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}

/**
 * مكوّن الحالة الفارغة الموحد عالي الجودة لكافة شاشات التطبيق (Unified Actionable Empty State).
 * يستبدل النصوص البدائية بأيقونة متجهة وشرح واضح وزر استعادة/إجراء فوري بنقرة واحدة.
 */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A00E676))
                    .border(1.dp, Color(0x3300E676), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WhatsAppGreenLight,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LiquidGradientButton(
                    text = actionLabel,
                    onClick = onActionClick,
                    modifier = Modifier.height(42.dp)
                )
            }
        }
    }
}
