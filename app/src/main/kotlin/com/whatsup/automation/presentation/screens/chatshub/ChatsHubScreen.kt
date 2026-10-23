package com.whatsup.automation.presentation.screens.chatshub

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppEmptyState
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.BentoSkeletonList
import com.whatsup.automation.presentation.screens.chatshub.components.*
import com.whatsup.automation.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsHubScreen(
    viewModel: ChatsHubViewModel,
    onNavigateToVault: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    var showDeletedDialog by remember { mutableStateOf(false) }
    var showMediaDialog by remember { mutableStateOf(false) }

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
            AppScreenHeader(
                title = "مركز المحادثات والتسجيل",
                subtitle = "رصد جهات الاتصال غير المسجلة ومتابعة التسجيلات الحية",
                onOpenDrawer = onOpenDrawer,
                modifier = Modifier.padding(bottom = 12.dp),
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.refresh()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, Color(0x3300E676), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

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
                        IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = WhatsAppGreen,
                    unfocusedBorderColor = Color(0x3300E676),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // بطاقة 1: الرسائل المحذوفة لدى الجميع
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToVault()
                    },
                borderBrush = ObsidianGlowBorder.Amber
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("الرسائل المحذوفة لدى الجميع", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = if (uiState.deletedCount > 0) "${uiState.deletedCount} رسالة محذوفة تم التقاطها" else "لا توجد رسائل محذوفة حالياً",
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { showDeletedDialog = true }) {
                            Text("استعراض", fontSize = 12.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // بطاقة 2: الخزينة ووسائط العرض لمرة واحدة
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToVault()
                    },
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("الخزينة ووسائط العرض لمرة واحدة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = if (uiState.vaultMediaCount > 0) "${uiState.vaultMediaCount} وسائط محفوظة ومحمية" else "لا توجد وسائط مسجلة",
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { showMediaDialog = true }) {
                            Text("استعراض", fontSize = 12.sp, color = WhatsAppGreenLight, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // شريط التبويبات الثلاثة
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    Triple(ChatsHubTab.UNREGISTERED, uiState.unregisteredCount, AmberWarning),
                    Triple(ChatsHubTab.REGISTERED_ALL, uiState.registeredAllCount, WhatsAppGreen),
                    Triple(ChatsHubTab.REGISTERED_TODAY, uiState.registeredTodayCount, CyberCyan)
                )

                items(tabs) { (tab, count, color) ->
                    val isSelected = uiState.selectedTab == tab
                    val bgTint by animateColorAsState(
                        targetValue = if (isSelected) color.copy(alpha = 0.16f) else DarkSurface,
                        animationSpec = tween(200),
                        label = "tabBg"
                    )
                    val borderBrush = if (isSelected) {
                        Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0x3300E676), Color(0x3300B0FF)))
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

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) color else Color(0x1A00E676))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // قائمة البيانات مع فحص حالة التحميل
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    BentoSkeletonList(itemCount = 4, itemHeight = 110.dp)
                }
            } else {
                val query = uiState.searchQuery.trim().lowercase()

                when (uiState.selectedTab) {
                    ChatsHubTab.UNREGISTERED -> {
                        val filtered = uiState.unregisteredList.filter {
                            query.isEmpty() || it.phone.contains(query) || it.lastMessage.lowercase().contains(query)
                        }

                        if (filtered.isEmpty()) {
                            if (query.isNotEmpty()) {
                                AppEmptyState(
                                    icon = Icons.Outlined.SearchOff,
                                    title = "لا توجد نتائج مطابقة للبحث",
                                    subtitle = "لم نعثر على أي محادثة غير مسجلة تطابق \"$query\".",
                                    actionLabel = "مسح البحث",
                                    onActionClick = { viewModel.updateSearchQuery("") }
                                )
                            } else {
                                AppEmptyState(
                                    icon = Icons.Outlined.CheckCircle,
                                    title = "لا توجد أرقام غير مسجلة حالياً",
                                    subtitle = "جميع جهات الاتصال التي تواصلت معك محفوظة في دفتر هاتفك.",
                                    actionLabel = "تحديث وفحص الرسائل",
                                    onActionClick = { viewModel.refresh() }
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
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
                            if (query.isNotEmpty()) {
                                AppEmptyState(
                                    icon = Icons.Outlined.SearchOff,
                                    title = "لا توجد جهات اتصال مطابقة",
                                    subtitle = "لم نعثر على أي جهة اتصال تطابق \"$query\".",
                                    actionLabel = "مسح البحث",
                                    onActionClick = { viewModel.updateSearchQuery("") }
                                )
                            } else {
                                AppEmptyState(
                                    icon = Icons.Outlined.PersonSearch,
                                    title = "لا توجد جهات اتصال مسجلة",
                                    subtitle = "تأكد من منح التطبيق صلاحية قراءة جهات الاتصال.",
                                    actionLabel = "مزامنة جهات الاتصال",
                                    onActionClick = { viewModel.refresh() }
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(filtered, key = { it.phone }) { item ->
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
                            if (query.isNotEmpty()) {
                                AppEmptyState(
                                    icon = Icons.Outlined.SearchOff,
                                    title = "لا توجد نتائج مطابقة",
                                    subtitle = "لم نعثر على أي جهة مسجلة اليوم تطابق \"$query\".",
                                    actionLabel = "مسح البحث",
                                    onActionClick = { viewModel.updateSearchQuery("") }
                                )
                            } else {
                                AppEmptyState(
                                    icon = Icons.Outlined.PersonAdd,
                                    title = "لم يتم حفظ أي جهات اتصال اليوم",
                                    subtitle = "الأرقام الجديدة التي يتم حفظها تلقائياً اليوم ستظهر هنا فور تسجيلها.",
                                    actionLabel = "تحديث القائمة",
                                    onActionClick = { viewModel.refresh() }
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(filtered, key = { it.phone }) { item ->
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
    }

    if (showDeletedDialog) {
        DeletedMessagesDialog(
            deletedMessages = uiState.deletedMessages,
            onDismiss = { showDeletedDialog = false }
        )
    }

    if (showMediaDialog) {
        ViewOnceVaultDialog(
            mediaVaultItems = uiState.vaultMediaItems,
            onDismiss = { showMediaDialog = false },
            onOpenMedia = {
                showMediaDialog = false
                onNavigateToVault()
            }
        )
    }
}
