package com.whatsup.automation.presentation.screens.logs.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.ActivityLog
import com.whatsup.automation.domain.model.LogStatus
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FilterChipItem(
    label: String,
    selected: Boolean,
    selectedColor: Color = WhatsAppGreen,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) selectedColor else DarkSurface)
            .border(
                1.dp,
                if (selected) selectedColor else Color(0x22FFFFFF),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) DarkBgPrimary else TextSecondary
        )
    }
}

@Composable
fun LogItemCard(log: ActivityLog, displayName: String? = null) {
    val (statusColor, statusLabel) = when (log.status) {
        LogStatus.SUCCESS -> WhatsAppGreen to "ناجح"
        LogStatus.NO_MATCH -> AmberWarning to "تجاهل"
        LogStatus.FAILURE -> RoseError to "فشل"
    }

    val cleanDigits = log.senderPhone.replace("[^0-9]".toRegex(), "")
    val titleText = displayName ?: if (cleanDigits.length >= 7) "+$cleanDigits" else "جهة غير مسجلة"

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        borderBrush = Brush.linearGradient(
            listOf(statusColor.copy(alpha = 0.3f), Color(0x0AFFFFFF))
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (displayName != null) {
                        Text(
                            text = displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    } else {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                        ) {
                            Text(
                                text = titleText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    if (displayName != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WhatsAppGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "مسجل 👤",
                                fontSize = 9.sp,
                                color = WhatsAppGreenLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "\"${log.messageText}\"",
                fontSize = 13.sp,
                color = TextSecondary
            )

            if (!log.matchedRule.isNullOrBlank()) {
                Text(
                    text = "القاعدة: ${log.matchedRule}",
                    fontSize = 11.sp,
                    color = WhatsAppGreenLight,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = Color(0x14FFFFFF), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الإجراء: ${log.actionExecuted}",
                    fontSize = 11.sp,
                    color = CyberCyan
                )

                log.extractedName?.let { name ->
                    Text(
                        text = "الاسم: $name",
                        fontSize = 11.sp,
                        color = WhatsAppGreenLight
                    )
                }
            }
        }
    }
}
