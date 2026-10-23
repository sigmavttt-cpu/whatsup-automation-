package com.whatsup.automation.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.LiquidGradientButton
import com.whatsup.automation.presentation.components.PulseReactor
import com.whatsup.automation.presentation.theme.*

@Composable
fun ReactorHeroCard(
    isConnected: Boolean,
    isPaused: Boolean,
    isAssistantActive: Boolean,
    isServiceRunning: Boolean = true,
    onToggleService: () -> Unit,
    onFullStopService: () -> Unit = {},
    onStartService: () -> Unit = onToggleService,
    modifier: Modifier = Modifier
) {
    val isFullyActive = isServiceRunning && isConnected && !isPaused

    val borderBrush = when {
        !isServiceRunning -> Brush.linearGradient(listOf(RoseError.copy(alpha = 0.45f), Color(0x10FFFFFF)))
        isFullyActive -> ObsidianGlowBorder.Emerald
        isConnected && isPaused -> ObsidianGlowBorder.Cyan
        else -> Brush.linearGradient(listOf(AmberWarning.copy(alpha = 0.35f), Color(0x10FFFFFF)))
    }

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        borderBrush = borderBrush
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PulseReactor(
                            isActive = isServiceRunning && isConnected,
                            activeColor = if (isPaused) AmberWarning else WhatsAppGreen,
                            inactiveColor = if (!isServiceRunning) RoseError else TextMuted
                        )
                        Text(
                            text = when {
                                !isServiceRunning -> "التطبيق متوقف كلياً (سكون تام)"
                                isFullyActive -> "الأتمتة نشطة وتعمل 24/7"
                                isConnected && isPaused -> "الأتمتة متوقفة مؤقتاً"
                                else -> "المحرك الأساسي متوقف"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = when {
                            !isServiceRunning -> "تم إيقاف الخدمة والمحرك — استهلاك الموارد صفر"
                            isPaused && isConnected -> "الاتصال محفوظ بأمان — تم كتم الردود والحفظ مؤقتاً"
                            else -> "معالجة فورية محلية 100% بدون أي خوادم سحابية"
                        },
                        fontSize = 11.sp,
                        color = when {
                            !isServiceRunning -> RoseError
                            isPaused && isConnected -> AmberWarning
                            else -> TextMuted
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isServiceRunning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiquidGradientButton(
                            text = if (isPaused) "استئناف" else "تجميد",
                            onClick = onToggleService,
                            icon = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            gradient = if (isPaused) LiquidEmeraldGradient else Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309)))
                        )
                        LiquidGradientButton(
                            text = "إيقاف كلي",
                            onClick = onFullStopService,
                            icon = Icons.Filled.Close,
                            gradient = Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
                        )
                    }
                } else {
                    LiquidGradientButton(
                        text = "تشغيل المحرك",
                        onClick = onStartService,
                        icon = Icons.Filled.PlayArrow,
                        gradient = LiquidEmeraldGradient
                    )
                }
            }

            // شريط حالة المحركين (الأساسي + المساعد)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val engineStatusBg = when {
                    isFullyActive -> WhatsAppGreen.copy(alpha = 0.12f)
                    isConnected && isPaused -> AmberWarning.copy(alpha = 0.12f)
                    else -> DarkBgSecondary
                }
                val engineStatusBorder = when {
                    isFullyActive -> WhatsAppGreen.copy(alpha = 0.35f)
                    isConnected && isPaused -> AmberWarning.copy(alpha = 0.35f)
                    else -> Color(0x20FFFFFF)
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = engineStatusBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, engineStatusBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isFullyActive -> WhatsAppGreen
                                        isConnected && isPaused -> AmberWarning
                                        else -> TextMuted
                                    }
                                )
                        )
                        Text(
                            text = when {
                                !isServiceRunning -> "الأساسي: متوقف تماماً"
                                isFullyActive -> "الأساسي: نشط"
                                isConnected && isPaused -> "الأساسي: متصل"
                                else -> "الأساسي: بانتظار الربط"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                !isServiceRunning -> RoseError
                                isFullyActive -> WhatsAppGreenLight
                                isConnected && isPaused -> AmberWarning
                                else -> TextMuted
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // مؤشر المساعد
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAssistantActive) CyberCyan.copy(alpha = 0.12f) else DarkBgSecondary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAssistantActive) CyberCyan.copy(alpha = 0.35f) else Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAssistantActive) CyberCyan else TextMuted)
                        )
                        Text(
                            text = if (isAssistantActive) "المساعد: جاهز" else "المساعد: بانتظار التفعيل",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAssistantActive) CyberCyan else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
