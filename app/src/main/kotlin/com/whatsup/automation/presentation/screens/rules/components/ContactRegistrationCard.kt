package com.whatsup.automation.presentation.screens.rules.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.ContactFormattingSettings
import com.whatsup.automation.domain.util.SpintaxEngine
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

@Composable
fun ContactRegistrationCard(
    settings: ContactFormattingSettings,
    onUpdateSettings: (ContactFormattingSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Purple
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Badge,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "قاعدة تسجيل جهات الاتصال",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonPurple.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ثابتة 📌", color = NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "التقاط اسم العميل وحفظه في دفتر الهاتف والرد عليه فوراً",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUpdateSettings(settings.copy(isEnabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WhatsAppGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBgSecondary
                    )
                )
            }

            if (settings.isEnabled) {
                HorizontalDivider(color = Color(0x15FFFFFF))

                // حقول تخصيص الاسم: قبل الاسم وبعد الاسم
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = settings.customPrefix,
                        onValueChange = { onUpdateSettings(settings.copy(customPrefix = it)) },
                        label = { Text("قبل الاسم (بادئة/رمز)") },
                        placeholder = { Text("مثال: عميل / ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color(0x22FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = settings.customTag,
                        onValueChange = { onUpdateSettings(settings.copy(customTag = it)) },
                        label = { Text("بعد الاسم (وسم/رمز)") },
                        placeholder = { Text("مثال: [واتساب]") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0x22FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // معاينة مباشرة للاسم المحفوظ في الهاتف
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x15FFFFFF))
                        .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    val previewName = settings.formatForPhonebook("(الاسم)")
                    Text(
                        text = "معاينة الحفظ في الهاتف: $previewName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // حقل الكلمات المفتاحية للتسجيل
                OutlinedTextField(
                    value = settings.triggerKeywords,
                    onValueChange = { onUpdateSettings(settings.copy(triggerKeywords = it)) },
                    label = { Text("الكلمات المفتاحية للتسجيل والحفظ *") },
                    placeholder = { Text("سجلني، احفظني، سجل اسمي، احفظ رقمي، اسمي") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = Color(0x22FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // حقل نص الرد التلقائي عند الحفظ
                OutlinedTextField(
                    value = settings.replyMessage,
                    onValueChange = { onUpdateSettings(settings.copy(replyMessage = it)) },
                    label = { Text("نص الرد التلقائي عند اكتمال الحفظ * (يدعم Spintax)") },
                    placeholder = { Text("تم حفظك باسم {name} بنجاح ✅") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0x22FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // شريط إدراج متغيرات الرد
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val replyVars = listOf(
                        "{name}" to "الاسم كامل",
                        "{first_name}" to "الاسم الأول",
                        "{time_greeting}" to "التحية بالوقت",
                        "{{أهلاً|مرحباً|حياك الله}}" to "Spintax ترحيب",
                        "{{تم حفظك|سجلتك عندي}}" to "Spintax تأكيد"
                    )
                    replyVars.forEach { (tag, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1800E5FF))
                                .clickable {
                                    onUpdateSettings(settings.copy(replyMessage = settings.replyMessage + " $tag"))
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "+ $label",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // معاينة عينة من رد التأكيد المولد
                if (settings.replyMessage.isNotBlank()) {
                    val previewReply = remember(settings.replyMessage) {
                        SpintaxEngine.process(
                            template = settings.replyMessage,
                            recipientName = "محمد أحمد",
                            recipientPhone = "+967770000000"
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1025D366))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "🎲 عينة حية لرسالة التأكيد التي ستصل للشخص:",
                                fontSize = 10.sp,
                                color = WhatsAppGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = previewReply,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
