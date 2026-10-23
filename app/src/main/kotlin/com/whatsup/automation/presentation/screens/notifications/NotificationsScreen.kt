package com.whatsup.automation.presentation.screens.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.components.SafeActionButton
import com.whatsup.automation.presentation.components.SafeButtonStyle
import com.whatsup.automation.presentation.screens.notifications.components.NotificationCard
import com.whatsup.automation.presentation.screens.notifications.components.NotificationFilterChips
import com.whatsup.automation.presentation.screens.notifications.components.NotificationsBentoGrid
import com.whatsup.automation.presentation.theme.*

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. ترويسة الشاشة
        AppScreenHeader(
            title = "مركز التنبيهات والأحداث",
            subtitle = if (uiState.unreadCount > 0) "${uiState.unreadCount} تنبيهات غير مقروءة" else "جميع العمليات تسير بسلاسة",
            onOpenDrawer = onOpenDrawer,
            actions = {
                if (uiState.unreadCount > 0) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.markAllAsRead()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x1F00E676))
                            .border(1.dp, Color(0x4000E676), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "تحديد الكل كمقروء",
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (uiState.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showClearConfirmDialog = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x1FF43F5E))
                            .border(1.dp, Color(0x40F43F5E), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "مسح كل التنبيهات",
                            tint = RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )

        // 2. بطاقات Bento الإحصائية الحية
        NotificationsBentoGrid(
            stats = uiState.stats,
            selectedFilter = uiState.selectedFilter,
            onFilterSelect = { viewModel.setFilter(it) }
        )

        // 3. شريط البحث السيبراني
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = {
                Text(
                    text = "بحث في التنبيهات أو أرقام الهواتف...",
                    color = TextHint,
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "مسح البحث",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = WhatsAppGreen,
                unfocusedBorderColor = Color(0x22FFFFFF),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        // 4. أزرار تصفية التصنيفات
        NotificationFilterChips(
            selectedFilter = uiState.selectedFilter,
            stats = uiState.stats,
            onFilterSelect = { viewModel.setFilter(it) }
        )

        // 5. قائمة التنبيهات أو الواجهة الخالية
        if (uiState.filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x1400E676)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.items.isEmpty()) Icons.Default.CheckCircle else Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = if (uiState.items.isEmpty()) WhatsAppGreenLight else AmberWarning,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = if (uiState.items.isEmpty()) "لا توجد تنبيهات حالياً" else "لا توجد نتائج مطابقة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = if (uiState.items.isEmpty())
                            "جميع العمليات وحفظ جهات الاتصال والأتمتة تسير بسلاسة ✅"
                        else
                            "جرب تغيير فلتر التصفية أو تعديل عبارة البحث",
                        fontSize = 12.sp,
                        color = TextSecondary
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
                items(uiState.filteredItems, key = { it.id }) { notification ->
                    NotificationCard(
                        item = notification,
                        onMarkAsRead = { viewModel.markAsRead(notification.id) },
                        onDelete = { viewModel.deleteNotification(notification.id) }
                    )
                }
            }
        }
    }

    // حوار تأكيد مسح الكل
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "مسح جميع التنبيهات؟",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "سيتم تفريغ كافة الإشعارات والعمليات السابقة نهائياً من الذاكرة.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                SafeActionButton(
                    text = "مسح الكل",
                    onClick = {
                        viewModel.clearAll()
                        showClearConfirmDialog = false
                    },
                    style = SafeButtonStyle.Destructive
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}
