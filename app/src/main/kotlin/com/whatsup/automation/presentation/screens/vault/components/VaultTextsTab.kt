package com.whatsup.automation.presentation.screens.vault.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.presentation.components.AppEmptyState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeletedTextsBentoSection(
    items: List<VaultItem>,
    onCopyText: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenUserHistory: (String, String) -> Unit = { _, _ -> }
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = Brush.horizontalGradient(listOf(Color(0xFFFF4D4D), Color(0x33FF4D4D)))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4D4D).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Color(0xFFFF4D4D), modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "النصوص المحذوفة لدى الجميع",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF4D4D).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${items.size} رسالة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF4D4D),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Filled.DeleteSweep,
                    title = "لا توجد رسائل نصية محذوفة",
                    subtitle = "عند قيام أي شخص بحذف رسالة لدى الجميع، ستظهر هنا فورياً بنصها وتوقيتها الأصلي."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        DeletedTextItemRow(
                            item = item,
                            onCopyText = { onCopyText(item.originalText ?: "") },
                            onToggleFavorite = { onToggleFavorite(item.id) },
                            onDelete = { onDelete(item.id) },
                            onClick = { onOpenUserHistory(item.senderPhone, item.senderName.ifBlank { item.senderPhone }) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeletedTextItemRow(
    item: VaultItem,
    onCopyText: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit = {}
) {
    val dateFormatter = remember { SimpleDateFormat("hh:mm a • yyyy/MM/dd", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormatter.format(Date(item.timestamp)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C1318))
            .border(1.dp, Color(0x33FF4D4D), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.senderName.ifBlank { item.senderPhone },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                    ) {
                        Text(
                            text = item.senderPhone,
                            fontSize = 10.5.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopyText, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = WhatsAppGreenLight, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "تفضيل",
                            tint = if (item.isFavorite) Color(0xFFFFD700) else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x221E293B))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = item.originalText ?: "(رسالة بدون نص)",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "نص محذوف",
                    fontSize = 10.sp,
                    color = Color(0xFFFF4D4D),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EditedMessagesBentoSection(
    items: List<VaultItem>,
    onCopyText: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenUserHistory: (String, String) -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Amber
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AmberWarning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "كشف التعديلات السرية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberWarning.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${items.size} تعديل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Filled.Edit,
                    title = "لا توجد رسائل معدلة مسجلة",
                    subtitle = "عند تعديل أي رسالة في الدردشات، سنحتفظ بالنص الأصلي مع النص المعدل هنا."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        EditedMessageItemRow(
                            item = item,
                            onCopyText = { onCopyText(item.originalText ?: "") },
                            onDelete = { onDelete(item.id) },
                            onClick = { onOpenUserHistory(item.senderPhone, item.senderName.ifBlank { item.senderPhone }) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditedMessageItemRow(
    item: VaultItem,
    onCopyText: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("hh:mm a • yyyy/MM/dd", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormatter.format(Date(item.timestamp)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C1318))
            .border(1.dp, AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.senderName.ifBlank { item.senderPhone },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                    ) {
                        Text(
                            text = item.senderPhone,
                            fontSize = 10.5.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopyText, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = AmberWarning, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x221E293B))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = item.originalText ?: "",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "تم تعديلها",
                    fontSize = 10.sp,
                    color = AmberWarning,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
