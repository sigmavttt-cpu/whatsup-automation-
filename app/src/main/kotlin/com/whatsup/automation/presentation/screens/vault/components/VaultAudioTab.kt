package com.whatsup.automation.presentation.screens.vault.components

import android.content.Intent
import android.media.MediaPlayer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.presentation.components.AppEmptyState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AudioVaultBentoSection(
    items: List<VaultItem>,
    onDelete: (String) -> Unit,
    onShare: (VaultItem) -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Cyan
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
                            .background(CyberCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "التسجيلات والملاحظات الصوتية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${items.size} تسجيل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Filled.Mic,
                    title = "لا توجد تسجيلات صوتية",
                    subtitle = "التسجيلات والملاحظات الصوتية المحذوفة ستظهر هنا مع إمكانية الاستماع والمشاركة."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        AudioVaultItemRow(
                            item = item,
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
fun AudioVaultItemRow(
    item: VaultItem,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("hh:mm a • yyyy/MM/dd", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormatter.format(Date(item.timestamp)) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070E14))
            .border(1.dp, Color(0x3300B0FF), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.2f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                if (isPlaying) {
                                    try {
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                    } catch (_: Exception) {}
                                } else {
                                    if (!item.localFilePath.isNullOrBlank()) {
                                        try {
                                            if (mediaPlayer == null) {
                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(item.localFilePath)
                                                    prepare()
                                                    setOnCompletionListener {
                                                        isPlaying = false
                                                    }
                                                }
                                            }
                                            mediaPlayer?.start()
                                            isPlaying = true
                                        } catch (_: Exception) {
                                            isPlaying = false
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.senderName.ifBlank { item.senderPhone },
                            fontSize = 13.sp,
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
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = CyberCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            val displaySize = remember(item.fileSizeBytes, item.localFilePath) {
                if (item.fileSizeBytes > 0L) {
                    item.getFormattedSize()
                } else if (!item.localFilePath.isNullOrBlank()) {
                    try {
                        val f = File(item.localFilePath)
                        if (f.exists() && f.length() > 0L) {
                            val kb = f.length() / 1024.0
                            if (kb >= 1024.0) String.format(Locale.US, "%.1f MB", kb / 1024.0)
                            else String.format(Locale.US, "%.0f KB", kb)
                        } else "تسجيل صوتي"
                    } catch (_: Exception) {
                        "تسجيل صوتي"
                    }
                } else {
                    "تسجيل صوتي"
                }
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
                    text = displaySize,
                    fontSize = 10.sp,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun OtherFilesBentoSection(
    items: List<VaultItem>,
    onOpen: (VaultItem) -> Unit,
    onShare: (VaultItem) -> Unit,
    onDelete: (String) -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Emerald
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
                            .background(WhatsAppGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.FolderZip, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "المستندات والملفات الأخرى",
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
                        text = "${items.size} ملف",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Filled.InsertDriveFile,
                    title = "لا توجد ملفات أو مستندات",
                    subtitle = "المستندات والملفات المحفوظة ستظهر هنا مرتبة حسب تاريخ وصولها."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        OtherFileItemRow(
                            item = item,
                            onOpen = { onOpen(item) },
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
fun OtherFileItemRow(
    item: VaultItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("hh:mm a • yyyy/MM/dd", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormatter.format(Date(item.timestamp)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070E14))
            .border(1.dp, Color(0x3300E676), RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WhatsAppGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.InsertDriveFile, contentDescription = null, tint = WhatsAppGreenLight, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.senderName.ifBlank { item.senderPhone },
                    fontSize = 13.sp,
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
                Text(
                    text = "$formattedDate • ${item.getFormattedSize()}",
                    fontSize = 9.5.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = WhatsAppGreenLight, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
