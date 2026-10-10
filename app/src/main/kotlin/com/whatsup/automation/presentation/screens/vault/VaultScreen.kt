package com.whatsup.automation.presentation.screens.vault

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.domain.model.VaultMediaType
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val selectedItem = uiState.selectedItem

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize().background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ترويسة الخزينة الفاخرة
            Spacer(modifier = Modifier.height(6.dp))
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = ObsidianGlowBorder.Emerald
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                            .border(1.5.dp, WhatsAppGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الخزينة السرية 🔒",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "وسائط العرض لمرة واحدة، الرسائل المحذوفة، والحالات الملتقطة محلياً",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // شريط البحث
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث بالاسم، الرقم، أو محتوى الرسالة...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = WhatsAppGreen) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WhatsAppGreen,
                    unfocusedBorderColor = Color(0x3300A884),
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // رقائق التبويبات الفرعية (Filter Chips)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    VaultFilterChip(
                        title = "العرض لمرة واحدة (${uiState.viewOnceCount})",
                        icon = Icons.Filled.Visibility,
                        isSelected = uiState.activeTab == VaultSubTab.VIEW_ONCE,
                        activeColor = NeonPurple,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setActiveTab(VaultSubTab.VIEW_ONCE)
                        }
                    )
                }

                item {
                    VaultFilterChip(
                        title = "الرسائل المحذوفة (${uiState.deletedCount})",
                        icon = Icons.Filled.DeleteSweep,
                        isSelected = uiState.activeTab == VaultSubTab.ANTI_DELETE,
                        activeColor = AmberWarning,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setActiveTab(VaultSubTab.ANTI_DELETE)
                        }
                    )
                }

                item {
                    VaultFilterChip(
                        title = "المفضلة ⭐",
                        icon = Icons.Filled.Star,
                        isSelected = uiState.activeTab == VaultSubTab.FAVORITES,
                        activeColor = WhatsAppGreen,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setActiveTab(VaultSubTab.FAVORITES)
                        }
                    )
                }
            }

            // قائمة العناصر
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (uiState.activeTab) {
                                VaultSubTab.VIEW_ONCE -> Icons.Filled.VisibilityOff
                                VaultSubTab.ANTI_DELETE -> Icons.Filled.DeleteOutline
                                VaultSubTab.FAVORITES -> Icons.Filled.StarBorder
                            },
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = when (uiState.activeTab) {
                                VaultSubTab.VIEW_ONCE -> "لم يتم التقاط أي وسائط عرض لمرة واحدة بعد"
                                VaultSubTab.ANTI_DELETE -> "لا توجد أي رسائل محذوفة مسجلة في الخزينة"
                                VaultSubTab.FAVORITES -> "لا توجد عناصر مضافة إلى المفضلة"
                            },
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        VaultItemCard(
                            item = item,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.selectItem(item)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                            onDelete = { viewModel.deleteItem(item.id) }
                        )
                    }
                }
            }
        }

        // نافذة المعاينة المنبثقة
        if (selectedItem != null) {
            VaultItemPreviewDialog(
                item = selectedItem,
                onDismiss = { viewModel.selectItem(null) },
                onToggleFavorite = { viewModel.toggleFavorite(selectedItem.id) },
                onDelete = { viewModel.deleteItem(selectedItem.id) }
            )
        }
    }
}

@Composable
private fun VaultFilterChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.15f) else Color(0xFFFFFFFF))
            .border(
                1.dp,
                if (isSelected) activeColor else Color(0x3300A884),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeColor else TextPrimary
            )
        }
    }
}

@Composable
private fun VaultItemCard(
    item: VaultItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormatter.format(Date(item.timestamp)) }

    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        borderBrush = if (item.isDeleted) ObsidianGlowBorder.Amber else if (item.isViewOnce) ObsidianGlowBorder.Purple else ObsidianGlowBorder.Emerald
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isDeleted) AmberWarning.copy(alpha = 0.2f)
                                else if (item.isViewOnce) NeonPurple.copy(alpha = 0.2f)
                                else WhatsAppGreen.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.mediaType) {
                                VaultMediaType.IMAGE -> Icons.Filled.Image
                                VaultMediaType.VIDEO -> Icons.Filled.Videocam
                                VaultMediaType.AUDIO -> Icons.Filled.Mic
                                VaultMediaType.TEXT -> Icons.Filled.Chat
                                VaultMediaType.DOCUMENT -> Icons.Filled.Description
                            },
                            contentDescription = null,
                            tint = if (item.isDeleted) AmberWarning else if (item.isViewOnce) NeonPurple else WhatsAppGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = item.senderName.ifBlank { item.senderPhone },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                            Text(
                                text = item.senderPhone,
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (item.isFavorite) Color(0xFFFFD700) else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // النص أو الوصف
            if (!item.originalText.isNullOrBlank()) {
                Text(
                    text = item.originalText,
                    fontSize = 12.5.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // وسوم الحالة والوقت
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.isViewOnce) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "عرض لمرة واحدة 👁️",
                                fontSize = 10.sp,
                                color = NeonPurple,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (item.isDeleted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberWarning.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "محذوفة من المرسل 🗑️",
                                fontSize = 10.sp,
                                color = AmberWarning,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (item.fileSizeBytes > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x15FFFFFF)
                        ) {
                            Text(
                                text = item.getFormattedSize(),
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 10.5.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun VaultItemPreviewDialog(
    item: VaultItem,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(item.localFilePath) {
        if (!item.localFilePath.isNullOrBlank()) {
            try {
                val f = File(item.localFilePath)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFFFFF))
                    .border(1.dp, Color(0x3300A884), RoundedCornerShape(20.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // شريط علوي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.senderName.ifBlank { item.senderPhone },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = TextMuted)
                    }
                }

                // عرض الصورة إذا كانت صورة
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else if (item.mediaType == VaultMediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x15FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(54.dp))
                            Text("فيديو محفوظ في الخزينة (${item.getFormattedSize()})", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }

                // عرض النص
                if (!item.originalText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x15FFFFFF))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = item.originalText,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }

                // أزرار الإجراءات
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (!item.localFilePath.isNullOrBlank()) {
                                try {
                                    val file = File(item.localFilePath)
                                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = item.mimeType ?: "image/*"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الوسائط"))
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        enabled = !item.localFilePath.isNullOrBlank()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة", fontSize = 12.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
