package com.whatsup.automation.presentation.screens.logs.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.AntiBanSettings
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

@Composable
fun AntiBanCard(
    antiBan: AntiBanSettings,
    onUpdateAntiBan: (AntiBanSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Emerald
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "درع الحماية ومحاكي السلوك البشري",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "محاكاة القراءة والكتابة الطبيعية ومنع الحظر",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = antiBan.isEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateAntiBan(antiBan.copy(isEnabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WhatsAppGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBgSecondary
                    )
                )
            }

            if (antiBan.isEnabled) {
                HorizontalDivider(color = Color(0x15FFFFFF))

                // خيار محاكاة القراءة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("محاكاة قراءة الرسالة (Mark Read)", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text("تأكيد القراءة ثم الانتظار لحظات قبل البدء بالكتابة", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = antiBan.simulateReading,
                        onCheckedChange = {
                            onUpdateAntiBan(antiBan.copy(simulateReading = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBgSecondary
                        )
                    )
                }

                // خيار سرعة الكتابة المتناسبة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("كتابة ديناميكية متناسبة مع طول النص", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text("إظهار 'يكتب الآن...' لفترة تتطابق مع عدد حروف الرد", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = antiBan.dynamicTypingSpeed,
                        onCheckedChange = {
                            onUpdateAntiBan(antiBan.copy(dynamicTypingSpeed = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBgSecondary
                        )
                    )
                }

                // محدد أقصى عدد ردود لكل نافذة
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الحد الأقصى للردود المتتالية لنفس المحادثة", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            "${antiBan.maxRepliesPerWindow} ردود / ${antiBan.windowMinutes} د",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                    Slider(
                        value = antiBan.maxRepliesPerWindow.toFloat(),
                        onValueChange = {
                            onUpdateAntiBan(antiBan.copy(maxRepliesPerWindow = it.toInt()))
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPurple,
                            activeTrackColor = NeonPurple,
                            inactiveTrackColor = Color(0x22FFFFFF)
                        )
                    )
                }

                // شارة الحالة
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WhatsAppGreen.copy(alpha = 0.1f))
                        .border(1.dp, WhatsAppGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "الدرع مفعّل: محصن ضد الحظر والحلقات اللانهائية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhatsAppGreen
                        )
                    }
                }
            }
        }
    }
}
