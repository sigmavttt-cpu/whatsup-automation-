package com.whatsup.automation.presentation.screens.chatshub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.PulseReactor
import com.whatsup.automation.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsHubScreen(
    viewModel: ChatsHubViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    var showDeletedDialog by remember { mutableStateOf(false) }
    var showMediaDialog by remember { mutableStateOf(false) }
    var selectedMediaItem by remember { mutableStateOf<com.whatsup.automation.domain.model.VaultItem?>(null) }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refresh()
        },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
        // رأس الشاشة (Header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مركز المحادثات والتسجيل",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    PulseReactor(isActive = true, activeColor = CyberCyan)
                }
                Text(
                    text = "رصد جهات الاتصال غير المسجلة ومتابعة التسجيلات الحية",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.refresh()
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GlassFill)
                    .border(1.dp, GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "تحديث",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // حقل البحث الذكي
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            placeholder = { Text("بحث بالاسم، الرقم، أو نص الرسالة...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFFFFF),
                unfocusedContainerColor = Color(0xFFFFFFFF),
                focusedBorderColor = WhatsAppGreen,
                unfocusedBorderColor = Color(0x3300A884),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // بطاقة 1: الرسائل المحذوفة لدى الجميع (Anti-Delete)
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            borderBrush = ObsidianGlowBorder.Cyan
        ) {
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "الرسائل المحذوفة لدى الجميع 🗑️",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.deletedCount} رسائل مسترجعة تلقائياً",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                if (uiState.deletedCount > 0) {
                    TextButton(onClick = { showDeletedDialog = true }) {
                        Text("عرض الكل", fontSize = 11.5.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // بطاقة 2: الخزينة ووسائط العرض لمرة واحدة (Media Vault)
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            borderBrush = ObsidianGlowBorder.Emerald
        ) {
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "الخزينة ووسائط العرض لمرة واحدة 🔒",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.vaultMediaCount} وسائط محفوظة ومحمية",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                if (uiState.vaultMediaCount > 0) {
                    TextButton(onClick = { showMediaDialog = true }) {
                        Text("استعراض", fontSize = 11.5.sp, color = WhatsAppGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // شريط التبويبات الفرعية الثلاثة مع العدادات الحية
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Triple(ChatsHubTab.UNREGISTERED, uiState.unregisteredCount, AmberWarning),
                Triple(ChatsHubTab.REGISTERED_ALL, uiState.registeredAllCount, WhatsAppGreen),
                Triple(ChatsHubTab.REGISTERED_TODAY, uiState.registeredTodayCount, NeonPurple)
            )

            items(tabs) { (tab, count, color) ->
                val isSelected = uiState.selectedTab == tab
                val bgTint by animateColorAsState(
                    targetValue = if (isSelected) color.copy(alpha = 0.14f) else Color(0xFFFFFFFF),
                    animationSpec = tween(200),
                    label = "tabBg"
                )
                val borderBrush = if (isSelected) {
                    Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0x2200A884), Color(0x2200A884)))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgTint)
                        .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(tab)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tab.title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) color else TextSecondary
                        )

                        // Badge عداد التبويب
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color else Color(0x1A00A884))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // محتوى التبويب المحدد
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(36.dp))
            }
        } else {
            val query = uiState.searchQuery.trim().lowercase()

            when (uiState.selectedTab) {
                ChatsHubTab.UNREGISTERED -> {
                    val filtered = uiState.unregisteredList.filter {
                        query.isEmpty() || it.phone.contains(query) || it.lastMessage.lowercase().contains(query)
                    }

                    if (filtered.isEmpty()) {
                        EmptyHubState(
                            icon = Icons.Outlined.CheckCircle,
                            title = "لا توجد رسائل من غير مسجلين",
                            subtitle = "جميع من راسلوك خلال آخر 24 ساعة مسجلين مسبقاً أو لا توجد رسائل جديدة."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.phone }) { item ->
                                UnregisteredCard(
                                    item = item,
                                    onOpenChat = { viewModel.openWhatsAppChat(item.phone) }
                                )
                            }
                        }
                    }
                }

                ChatsHubTab.REGISTERED_ALL -> {
                    val filtered = uiState.registeredAllList.filter {
                        query.isEmpty() || it.name.lowercase().contains(query) || it.phone.contains(query)
                    }

                    if (filtered.isEmpty()) {
                        EmptyHubState(
                            icon = Icons.Outlined.Contacts,
                            title = "لا توجد جهات اتصال مطابقة",
                            subtitle = "لم يتم العثور على جهات اتصال مسجلة في الهاتف تطابق البحث."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.phone + it.name }) { item ->
                                RegisteredAllCard(
                                    item = item,
                                    onOpenChat = { viewModel.openWhatsAppChat(item.phone) }
                                )
                            }
                        }
                    }
                }

                ChatsHubTab.REGISTERED_TODAY -> {
                    val filtered = uiState.registeredTodayList.filter {
                        query.isEmpty() || it.name.lowercase().contains(query) || it.phone.contains(query)
                    }

                    if (filtered.isEmpty()) {
                        EmptyHubState(
                            icon = Icons.Outlined.HourglassEmpty,
                            title = "لا توجد تسجيلات خلال آخر 24 ساعة",
                            subtitle = "أي جهة اتصال جديدة يتم تسجيلها آلياً ستظهر هنا لمدة 24 ساعة."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.phone + it.savedAt }) { item ->
                                RegisteredTodayCard(
                                    item = item,
                                    onOpenChat = { viewModel.openWhatsAppChat(item.phone) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة استعراض كافة الرسائل المحذوفة
    if (showDeletedDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDeletedDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    borderBrush = ObsidianGlowBorder.Cyan
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الرسائل المحذوفة لدى الجميع 🗑️",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            IconButton(onClick = { showDeletedDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.deletedMessages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد رسائل محذوفة حالياً 🟢", color = TextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.deletedMessages) { item ->
                                    BentoCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        borderBrush = ObsidianGlowBorder.Amber
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = item.senderName.ifBlank { item.senderPhone },
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp)),
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            Text(
                                                text = item.originalText ?: "رسالة محذوفة",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE11D48)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة استعراض وسائط الخزينة والعرض لمرة واحدة
    if (showMediaDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showMediaDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    borderBrush = ObsidianGlowBorder.Emerald
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "وسائط الخزينة والعرض لمرة واحدة 🔒",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            IconButton(onClick = { showMediaDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.vaultMediaItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد وسائط محفوظة حالياً 🔒", color = TextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.vaultMediaItems) { item ->
                                    BentoCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        borderBrush = ObsidianGlowBorder.Emerald
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(WhatsAppGreen.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when (item.mediaType) {
                                                            com.whatsup.automation.domain.model.VaultMediaType.IMAGE -> Icons.Default.Image
                                                            com.whatsup.automation.domain.model.VaultMediaType.VIDEO -> Icons.Default.Videocam
                                                            com.whatsup.automation.domain.model.VaultMediaType.AUDIO -> Icons.Default.Mic
                                                            else -> Icons.Default.InsertDriveFile
                                                        },
                                                        contentDescription = null,
                                                        tint = WhatsAppGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = item.senderName.ifBlank { item.senderPhone },
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "عرض لمرة واحدة • ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))}",
                                                        fontSize = 10.5.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            if (!item.localFilePath.isNullOrBlank()) {
                                                Button(
                                                    onClick = {
                                                        val file = java.io.File(item.localFilePath)
                                                        if (file.exists()) {
                                                            try {
                                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                                        context,
                                                                        "${context.packageName}.fileprovider",
                                                                        file
                                                                    )
                                                                    setDataAndType(uri, item.mimeType ?: "image/*")
                                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (_: Exception) {}
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text("فتح 👁️", fontSize = 11.sp, color = DarkBgPrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * بطاقة جهة اتصال غير مسجلة (مع زر الدخول للمحادثة المباشر).
 */
@Composable
private fun UnregisteredCard(
    item: UnregisteredContactItem,
    onOpenChat: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Amber
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.phone,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                        )
                        Text(
                            text = "غير مسجل في الهاتف",
                            fontSize = 11.sp,
                            color = AmberWarning
                        )
                    }
                }

                // وقت وصول الرسالة (مؤقت 24 ساعة)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassFill)
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.timeAgoFormatted,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // نص الرسالة الواردة
            if (item.lastMessage.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(10.dp)
                ) {
                    Text(
                        text = item.lastMessage,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 3
                    )
                }
            }

            // زر الدخول الفوري للمحادثة في واتساب
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenChat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "الدخول للمحادثة في واتساب",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * بطاقة جهة اتصال مسجلة فعلياً.
 */
@Composable
private fun RegisteredAllCard(
    item: RegisteredContactItem,
    onOpenChat: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Emerald
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.phone,
                        fontSize = 12.sp,
                        color = TextMuted,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenChat()
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WhatsAppGreen.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "مراسلة",
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * بطاقة جهة اتصال تم تسجيلها اليوم (مع مؤقت 24 ساعة).
 */
@Composable
private fun RegisteredTodayCard(
    item: RegisteredTodayItem,
    onOpenChat: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Purple
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = item.phone,
                            fontSize = 12.sp,
                            color = TextMuted,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                        )
                    }
                }

                // شارة مؤقت الـ 24 ساعة
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonPurple.copy(alpha = 0.15f))
                        .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "متبقي ${item.hoursRemaining}h",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سُجل: ${item.timeAgoFormatted}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenChat()
                    },
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.25f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "مراسلة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * شاشة الحالة الفارغة.
 */
@Composable
private fun EmptyHubState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GlassFill)
                    .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
