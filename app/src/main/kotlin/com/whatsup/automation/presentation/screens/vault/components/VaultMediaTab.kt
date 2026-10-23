package com.whatsup.automation.presentation.screens.vault.components

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.domain.model.VaultMediaType
import com.whatsup.automation.presentation.components.AppEmptyState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaVaultBentoSection(
    items: List<VaultItem>,
    onItemClick: (VaultItem) -> Unit,
    onSaveToGallery: (VaultItem) -> Unit,
    onShare: (VaultItem) -> Unit,
    onDelete: (String) -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Emerald
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "وسائط العرض لمرة واحدة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WhatsAppGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${items.size} وسائط",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Filled.PhotoLibrary,
                    title = "لا توجد صور أو مقاطع محفوظة",
                    subtitle = "عند استلام أي صورة أو فيديو بخاصية العرض لمرة واحدة أو حذفه، سيتم حفظه هنا تلقائياً وبدقته الكاملة."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item ->
                        MediaVaultItemRow(
                            item = item,
                            onClick = { onItemClick(item) },
                            onSave = { onSaveToGallery(item) },
                            onShare = { onShare(item) },
                            onDelete = { onDelete(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaVaultItemRow(
    item: VaultItem,
    onClick: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("hh:mm a • yyyy/MM/dd", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormatter.format(Date(item.timestamp)) }
    val bitmap = remember(item.localFilePath) {
        if (!item.localFilePath.isNullOrBlank()) {
            try {
                val f = File(item.localFilePath)
                if (f.exists()) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(f.absolutePath, opts)
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C1318))
            .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // صورة المعاينة المصغرة
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF16232D))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (item.mediaType == VaultMediaType.VIDEO) Icons.Filled.PlayCircle else Icons.Filled.Image,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // التفاصيل (المرسل، الشارة، التاريخ)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.senderName.ifBlank { item.senderPhone },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.isViewOnce) CyberCyan.copy(alpha = 0.18f) else Color(0xFFFF4D4D).copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            if (item.isViewOnce) CyberCyan.copy(alpha = 0.5f) else Color(0xFFFF4D4D).copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (item.isViewOnce) "عرض لمرة واحدة 🔒" else "محذوفة 🗑️",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isViewOnce) CyberCyan else Color(0xFFFF4D4D),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

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

                Text(
                    text = formattedDate,
                    fontSize = 9.5.sp,
                    color = TextSecondary
                )
            }

            // أزرار الإجراءات السريعة (حفظ، مشاركة، حذف)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "حفظ في المعرض",
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "مشاركة",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "حذف",
                        tint = Color(0xFFFF4D4D),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
