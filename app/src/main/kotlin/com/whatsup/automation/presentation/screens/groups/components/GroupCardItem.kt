package com.whatsup.automation.presentation.screens.groups.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.Group
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

@Composable
fun GroupCardItem(
    group: Group,
    estimatedTime: String,
    broadcastText: String,
    isBroadcasting: Boolean,
    isPaused: Boolean,
    progress: Pair<Int, Int>?,
    onBroadcastTextChange: (String) -> Unit,
    onSendBroadcast: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpenDetails: () -> Unit,
    onDeleteGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        borderBrush = if (isBroadcasting) {
            if (isPaused) ObsidianGlowBorder.Amber else ObsidianGlowBorder.Cyan
        } else ObsidianGlowBorder.Emerald
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // رأس البطاقة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                            .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = group.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WhatsAppGreen.copy(alpha = 0.15f))
                                    .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${group.memberCount} جهة اتصال",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhatsAppGreen
                                )
                            }

                            Text(
                                text = "• الوقت المقدر: $estimatedTime",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDetails,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "فتح التفاصيل", tint = CyberCyan)
                    }
                    IconButton(
                        onClick = onDeleteGroup,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف المجموعة", tint = CoralError)
                    }
                }
            }

            if (group.description.isNotBlank()) {
                Text(
                    text = group.description,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = Color(0x15FFFFFF))

            // حقل إدخال الرسالة الممتد ذاتياً
            Text(
                text = "نص الرسالة الموجهة لأعضاء البطاقة:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = broadcastText,
                    onValueChange = onBroadcastTextChange,
                    placeholder = { Text("اكتب نص الرسالة هنا (يتمدد الحقل تلقائياً)...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    minLines = 2,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = Color(0x26FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = onSendBroadcast,
                    enabled = broadcastText.isNotBlank() && !isBroadcasting,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (broadcastText.isNotBlank() && !isBroadcasting) WhatsAppGreen else DarkSurface
                        )
                        .size(50.dp)
                ) {
                    if (isBroadcasting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBgPrimary)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "إرسال للبث",
                            tint = if (broadcastText.isNotBlank()) DarkBgPrimary else TextMuted
                        )
                    }
                }
            }

            // شريط التقدم عند البث المباشر
            if (isBroadcasting && progress != null) {
                val (sent, total) = progress
                val fraction = if (total > 0) sent.toFloat() / total.toFloat() else 0f
                val percent = (fraction * 100).toInt()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPaused) AmberWarning.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    imageVector = if (isPaused) Icons.Filled.PauseCircle else Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (isPaused) AmberWarning else CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isPaused) "الحملة متوقفة مؤقتاً ⏸️" else "درع منع الحظر نشط 🛡️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaused) AmberWarning else CyberCyan
                                )
                            }
                            Text(
                                text = "$sent من $total ($percent%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) AmberWarning else WhatsAppGreenLight
                            )
                        }

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isPaused) AmberWarning else WhatsAppGreen,
                            trackColor = Color(0x33FFFFFF)
                        )

                        // أزرار التحكم: إيقاف مؤقت / استئناف / إلغاء
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPaused) {
                                Button(
                                    onClick = onResume,
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = DarkBgPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "استئناف الإرسال",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkBgPrimary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onPause,
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning.copy(alpha = 0.2f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.Pause,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "إيقاف مؤقت",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onCancel,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Stop,
                                    contentDescription = null,
                                    tint = CoralError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إلغاء", fontSize = 11.sp, color = CoralError)
                            }
                        }
                    }
                }
            }

            // زر فتح التفاصيل وإدارة الأعضاء
            TextButton(
                onClick = onOpenDetails,
                modifier = Modifier
                    .align(Alignment.End)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إدارة الأعضاء (${group.memberCount}) وسجل الإرسال",
                        fontSize = 13.sp,
                        color = CyberCyan
                    )
                    Icon(
                        Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
