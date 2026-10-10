package com.whatsup.automation.presentation.screens.logs.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.ActivityLog
import com.whatsup.automation.domain.model.LogStatus
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.screens.logs.ConversationSummary
import com.whatsup.automation.presentation.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * بطاقة عرض ملخص المحادثة لكل شخص
 */
@Composable
fun ConversationCard(
    conversation: ConversationSummary,
    onClick: () -> Unit
) {
    val timeFormatted = remember(conversation.latestTimestamp) {
        val zone = ZoneId.systemDefault()
        val dt = conversation.latestTimestamp.atZone(zone)
        dt.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
        borderBrush = Brush.linearGradient(
            listOf(WhatsAppGreen.copy(alpha = 0.35f), Color(0x12FFFFFF))
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // الصف العلوي: بيانات المرسل + التوقيت
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        val hasName = !conversation.contactName.isNullOrBlank() && conversation.contactName != conversation.senderPhone
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (hasName) {
                                Text(
                                    text = conversation.contactName!!,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(WhatsAppGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("مسجل 👤", fontSize = 9.sp, color = WhatsAppGreenLight, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val cleanDigits = conversation.senderPhone.replace("[^0-9]".toRegex(), "")
                                val displayPhone = if (cleanDigits.length >= 7) "+$cleanDigits" else "جهة اتصال غير مسجلة"
                                androidx.compose.runtime.CompositionLocalProvider(
                                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                                ) {
                                    Text(
                                        text = displayPhone,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        if (hasName && conversation.contactName != conversation.senderPhone) {
                            val cleanDigits = conversation.senderPhone.replace("[^0-9]".toRegex(), "")
                            val displaySubPhone = if (cleanDigits.length >= 7) "+$cleanDigits" else "رقم مسجل"
                            androidx.compose.runtime.CompositionLocalProvider(
                                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                            ) {
                                Text(
                                    text = displaySubPhone,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Text(
                                text = "رقم غير مسجل",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${conversation.totalMessages} رسائل",
                            fontSize = 10.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x12FFFFFF), thickness = 1.dp)

            // نص الرسالة الواردة
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "📩 الوارد:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text(
                    text = conversation.latestMessageText,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // نص الرد التلقائي والإجراء
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🤖 الرد:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppGreen
                )
                Text(
                    text = conversation.latestActionExecuted,
                    fontSize = 12.sp,
                    color = WhatsAppGreenLight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // زر فتح تفاصيل المحادثة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhatsAppGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "عرض المحادثة كاملة",
                        fontSize = 11.sp,
                        color = WhatsAppGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Filled.ArrowBack, // RTL arrow back is forward in Arabic
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = WhatsAppGreen
                    )
                }
            }
        }
    }
}

/**
 * نافذة حوارية لعرض تفاصيل المحادثة كاملة وفقاعات الرسائل
 */
@Composable
fun ConversationDetailDialog(
    conversation: ConversationSummary,
    onDismiss: () -> Unit
) {
    var customNameInput by remember { mutableStateOf(conversation.contactName ?: "") }
    var showNameEdit by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBgPrimary.copy(alpha = 0.95f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
                cornerRadius = 20.dp,
                borderBrush = Brush.linearGradient(
                    listOf(WhatsAppGreen.copy(alpha = 0.5f), CyberCyan.copy(alpha = 0.3f))
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // رأس المحادثة
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column {
                                    val hasDetailName = !conversation.contactName.isNullOrBlank()
                                    val cleanDigits = conversation.senderPhone.replace("[^0-9]".toRegex(), "")
                                    val displayPhone = if (cleanDigits.length >= 7) "+$cleanDigits" else "جهة اتصال غير مسجلة"
                                    if (hasDetailName) {
                                        Text(
                                            text = conversation.contactName!!,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        androidx.compose.runtime.CompositionLocalProvider(
                                            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                                        ) {
                                            Text(
                                                text = displayPhone,
                                                fontSize = 12.sp,
                                                color = TextMuted,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    } else {
                                        androidx.compose.runtime.CompositionLocalProvider(
                                            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                                        ) {
                                            Text(
                                                text = displayPhone,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = "رقم غير مسجل في جهات الاتصال",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val context = LocalContext.current
                                val haptic = LocalHapticFeedback.current
                                
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val sb = java.lang.StringBuilder()
                                        sb.append("=== سجل المحادثة مع: ${conversation.contactName ?: conversation.senderPhone} ===\n")
                                        conversation.allLogs.reversed().forEach { log ->
                                            sb.append("[${log.timestamp}] وارد: ${log.messageText}\n")
                                            if (log.actionExecuted.isNotBlank()) {
                                                sb.append(" └─ صادر: ${log.actionExecuted}\n")
                                            }
                                        }
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("WhatsApp Chat Log", sb.toString()))
                                        Toast.makeText(context, "تم نسخ سجل المحادثة كاملاً 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(CyberCyan.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ المحادثة", tint = CyberCyan, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(DarkSurface)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = TextPrimary)
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                    }

                    // قائمة فقاعات المحادثة (Chat History)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(conversation.allLogs, key = { it.id }) { log ->
                            ChatBubbleEntry(log = log)
                        }
                    }

                    // أسفل النافذة
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "تمت معالجة كافة الرسائل والردود محلياً على الجهاز 100% 🔒",
                            fontSize = 10.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * فقاعة رسالة داخل سجل المحادثة (الرسالة الواردة + الرد التلقائي الصادر)
 */
@Composable
fun ChatBubbleEntry(log: ActivityLog) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val timeFormatted = remember(log.timestamp) {
        val zone = ZoneId.systemDefault()
        val dt = log.timestamp.atZone(zone)
        dt.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. فقاعة الرسالة الواردة من الطرف الآخر
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Incoming Message", log.messageText))
                        Toast.makeText(context, "تم نسخ الرسالة الواردة", Toast.LENGTH_SHORT).show()
                    }
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "طرف المحادثة (وارد)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Text(
                            text = timeFormatted,
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }

                    Text(
                        text = log.messageText,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        // 2. فقاعة الرد التلقائي الصادر من التطبيق (إن وجد)
        if (log.actionExecuted.isNotBlank() && log.status == LogStatus.SUCCESS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp))
                        .background(Color(0xFF064E3B).copy(alpha = 0.85f))
                        .border(1.dp, WhatsAppGreen.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Auto Reply", log.actionExecuted))
                        Toast.makeText(context, "تم نسخ الرد التلقائي", Toast.LENGTH_SHORT).show()
                    }
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.SmartToy,
                                contentDescription = null,
                                tint = WhatsAppGreenLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "الرد التلقائي (${log.matchedRule ?: "أتمتة"})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreenLight
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = timeFormatted,
                                fontSize = 9.sp,
                                color = WhatsAppGreenLight.copy(alpha = 0.8f)
                            )
                            Icon(
                                Icons.Filled.DoneAll,
                                contentDescription = "تم الإرسال",
                                tint = CyberCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Text(
                        text = log.actionExecuted,
                        fontSize = 13.sp,
                        color = Color.White
                    )

                    log.extractedName?.let { name ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WhatsAppGreen.copy(alpha = 0.3f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "الاسم المستخرج: $name",
                                fontSize = 10.sp,
                                color = WhatsAppGreenLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
}

