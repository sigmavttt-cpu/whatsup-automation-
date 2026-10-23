package com.whatsup.automation.presentation.screens.notifications.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.AppNotification
import com.whatsup.automation.domain.model.NotificationType
import com.whatsup.automation.presentation.components.debouncedClickable
import com.whatsup.automation.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationCard(
    item: AppNotification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    val (icon, tintColor, bgTint) = when (item.type) {
        NotificationType.ERROR -> Triple(Icons.Filled.ErrorOutline, RoseError, RoseError.copy(alpha = 0.15f))
        NotificationType.WARNING -> Triple(Icons.Filled.WarningAmber, AmberWarning, AmberWarning.copy(alpha = 0.15f))
        NotificationType.SUCCESS -> Triple(Icons.Filled.CheckCircleOutline, WhatsAppGreen, WhatsAppGreen.copy(alpha = 0.15f))
        NotificationType.INFO -> Triple(Icons.Filled.Info, CyberCyan, CyberCyan.copy(alpha = 0.15f))
    }

    val timeFormatted = remember(item.timestamp) {
        val now = System.currentTimeMillis()
        val itemTime = item.timestamp.toEpochMilli()
        val diffMs = now - itemTime
        when {
            diffMs < 60_000L -> "الآن"
            diffMs < 3_600_000L -> "منذ ${diffMs / 60_000L} د"
            diffMs < 86_400_000L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(itemTime))
            else -> SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(itemTime))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(
                width = 1.dp,
                color = if (!item.isRead) tintColor.copy(alpha = 0.45f) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .debouncedClickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isExpanded = !isExpanded
                if (!item.isRead) {
                    onMarkAsRead()
                }
            }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // أيقونة النوع مع نقطة الحالة
                Box(contentAlignment = Alignment.TopStart) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(bgTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(tintColor)
                                .border(1.5.dp, DarkSurface, CircleShape)
                        )
                    }
                }

                // تفاصيل الإشعار
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Text(
                            text = timeFormatted,
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = item.message,
                        color = if (!item.isRead) TextSecondary else TextMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // شريط العمليات السريعة عند التوسيع
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Notification", "${item.title}\n${item.message}"))
                            Toast.makeText(context, "تم نسخ محتوى التنبيه 📋", Toast.LENGTH_SHORT).show()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = RoseError,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
