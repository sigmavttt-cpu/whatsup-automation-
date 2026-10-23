package com.whatsup.automation.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.ActivityLog
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

@Composable
fun DashboardRecentLogsHeader(
    isPreviewHidden: Boolean,
    onTogglePrivacy: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "سجل الأحداث والنشاط المباشر",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onTogglePrivacy,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPreviewHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (isPreviewHidden) "إظهار المعاينات" else "إخفاء المعاينات",
                    tint = if (isPreviewHidden) TextMuted else WhatsAppGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            TextButton(
                onClick = onViewAll,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("عرض الكل", fontSize = 12.5.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DashboardRecentLogItem(
    log: ActivityLog,
    isPreviewHidden: Boolean,
    modifier: Modifier = Modifier
) {
    val isContactSave = log.extractedName != null || log.actionExecuted.contains("حفظ", ignoreCase = true)
    val isReply = log.actionExecuted.contains("رد", ignoreCase = true) || log.actionExecuted.contains("Reply", ignoreCase = true)
    val isStatus = log.actionExecuted.contains("حالة", ignoreCase = true) || log.actionExecuted.contains("Status", ignoreCase = true)

    val actionColor = when {
        isContactSave -> WhatsAppGreen
        isReply -> CyberCyan
        isStatus -> NeonPurple
        else -> ElectricBlue
    }

    val actionIcon = when {
        isContactSave -> Icons.Filled.PersonAdd
        isReply -> Icons.Filled.Reply
        isStatus -> Icons.Filled.AutoStories
        else -> Icons.Filled.Bolt
    }

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        borderBrush = Brush.horizontalGradient(
            listOf(actionColor.copy(alpha = 0.35f), Color(0x1500A884))
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(actionColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.extractedName?.let { "تم حفظ: $it" } ?: log.actionExecuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val maskedPhone = if (isPreviewHidden) {
                        if (log.senderPhone.length > 5) log.senderPhone.take(4) + "•••" + log.senderPhone.takeLast(2) else "••••••"
                    } else {
                        log.senderPhone
                    }

                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                    ) {
                        Text(
                            text = maskedPhone,
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (log.messageText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val displayText = if (isPreviewHidden) "•••••••• (معاينة مخفية 🔒)" else "\"${log.messageText}\""
                    Text(
                        text = displayText,
                        fontSize = 12.sp,
                        color = if (isPreviewHidden) TextMuted else TextSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
