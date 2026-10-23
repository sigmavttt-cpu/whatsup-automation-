package com.whatsup.automation.presentation.screens.statuses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.components.AppTooltipBox
import com.whatsup.automation.presentation.components.BentoSkeletonList
import com.whatsup.automation.presentation.screens.statuses.components.*
import com.whatsup.automation.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusesScreen(
    viewModel: StatusesViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    var showCustomEmojiDialog by remember { mutableStateOf(false) }
    var selectedStatusIdForPreview by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()

    // نافذة إدخال إيموجي مخصص
    if (showCustomEmojiDialog) {
        StatusCustomEmojiDialog(
            onDismiss = { showCustomEmojiDialog = false },
            onEmojiConfirmed = { viewModel.setDefaultEmoji(it) }
        )
    }

    // نافذة تأكيد حذف الحالات المحددة
    if (showDeleteConfirmDialog) {
        StatusDeleteConfirmDialog(
            selectedCount = uiState.selectedIds.size,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirmDelete = {
                viewModel.deleteSelectedStatuses()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("تم حذف الحالات المحددة بنجاح")
                }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refreshStatuses()
        },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            // 1. شريط الرأس القياسي الموحد
            item {
                AppScreenHeader(
                    title = "أتمتة وتفاعل الحالات",
                    subtitle = "فلترة ذكية، استثناء جهات اتصال، وتفاعل لحظي",
                    onOpenDrawer = onOpenDrawer,
                    actions = {
                        if (uiState.statuses.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = WhatsAppGreen.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "${uiState.statuses.size} حالة",
                                    color = WhatsAppGreenLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                )
            }

            // 2. شريط البحث الذكي وترتيب الحالات مع أزرار التحديد والحذف
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("بحث باسم الشخص أو رقم الهاتف أو محتوى الحالة...", fontSize = 12.sp, color = TextMuted)
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "مسح", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = Color(0x22FFFFFF),
                            focusedContainerColor = DarkSurface.copy(alpha = 0.6f),
                            unfocusedContainerColor = DarkSurface.copy(alpha = 0.6f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // أزرار الترتيب السريع بدون إيموجيات عشوائية
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الترتيب:", fontSize = 11.sp, color = TextMuted)

                        StatusSortChip(
                            label = "الأحدث",
                            isSelected = uiState.sortBy == StatusSortBy.DATE_DESC,
                            onClick = { viewModel.setSortBy(StatusSortBy.DATE_DESC) }
                        )

                        StatusSortChip(
                            label = "الاسم",
                            isSelected = uiState.sortBy == StatusSortBy.NAME_ASC,
                            onClick = { viewModel.setSortBy(StatusSortBy.NAME_ASC) }
                        )

                        StatusSortChip(
                            label = "المفضلة",
                            isSelected = uiState.sortBy == StatusSortBy.FAVORITES,
                            onClick = { viewModel.setSortBy(StatusSortBy.FAVORITES) }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        AppTooltipBox(
                            tooltipText = if (uiState.isMultiSelectMode) "إلغاء وضع التحديد المتعدد" else "تفعيل وضع التحديد المتعدد للحالات"
                        ) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setMultiSelectMode(!uiState.isMultiSelectMode)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isMultiSelectMode) CyberCyan.copy(alpha = 0.2f) else Color(0x15FFFFFF))
                                    .border(1.dp, if (uiState.isMultiSelectMode) CyberCyan else Color(0x25FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isMultiSelectMode) Icons.Filled.Checklist else Icons.Outlined.Checklist,
                                    contentDescription = "تحديد متعدد",
                                    tint = if (uiState.isMultiSelectMode) CyberCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        AppTooltipBox(
                            tooltipText = if (uiState.selectedIds.isNotEmpty()) "حذف الحالات المحددة (${uiState.selectedIds.size})" else "حذف الحالات المحددة"
                        ) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (uiState.selectedIds.isNotEmpty()) {
                                        showDeleteConfirmDialog = true
                                    } else {
                                        viewModel.setMultiSelectMode(true)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("حدد الحالات التي ترغب بحذفها أولاً")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.selectedIds.isNotEmpty()) RoseError.copy(alpha = 0.2f) else Color(0x15FFFFFF))
                                    .border(1.dp, if (uiState.selectedIds.isNotEmpty()) RoseError else Color(0x25FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "حذف",
                                    tint = if (uiState.selectedIds.isNotEmpty()) RoseError else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. شريط التبويبات مع العدادات الحية بدون إيموجيات
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        StatusTabButton(
                            title = "الجديدة",
                            count = uiState.newCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.NEW,
                            accentColor = WhatsAppGreen,
                            modifier = Modifier.width(85.dp),
                            tooltipText = "الحالات الجديدة غير المشاهدة (${uiState.newCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.NEW) }
                        )
                    }

                    item {
                        StatusTabButton(
                            title = "المشاهدة",
                            count = uiState.viewedCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.VIEWED,
                            accentColor = CyberCyan,
                            modifier = Modifier.width(85.dp),
                            tooltipText = "الحالات التي تمت مشاهدتها والتفاعل معها (${uiState.viewedCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.VIEWED) }
                        )
                    }

                    item {
                        StatusTabButton(
                            title = "المحفوظات",
                            count = uiState.savedCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.SAVED,
                            accentColor = ElectricBlue,
                            modifier = Modifier.width(95.dp),
                            tooltipText = "الحالات المحفوظة في جهازك (${uiState.savedCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.SAVED) }
                        )
                    }

                    item {
                        StatusTabButton(
                            title = "المحذوفة",
                            count = uiState.deletedTempCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.DELETED_TEMP,
                            accentColor = Color(0xFFE11D48),
                            modifier = Modifier.width(90.dp),
                            tooltipText = "الحالات المحفوظة قبل حذفها (${uiState.deletedTempCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.DELETED_TEMP) }
                        )
                    }

                    item {
                        StatusTabButton(
                            title = "المستثناة",
                            count = uiState.mutedCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.MUTED,
                            accentColor = RoseError,
                            modifier = Modifier.width(90.dp),
                            tooltipText = "جهات الاتصال المستثناة (${uiState.mutedCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.MUTED) }
                        )
                    }

                    item {
                        StatusTabButton(
                            title = "المفضلة",
                            count = uiState.favoritesCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.FAVORITES,
                            accentColor = AmberWarning,
                            modifier = Modifier.width(85.dp),
                            tooltipText = "الحالات وجهات الاتصال المميزة بنجمة (${uiState.favoritesCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.FAVORITES) }
                        )
                    }
                }
            }

            // 4. شريط إجراءات التحديد المتعدد
            if (uiState.isMultiSelectMode) {
                item {
                    StatusMultiSelectBar(
                        selectedCount = uiState.selectedIds.size,
                        onSelectAll = { viewModel.selectAllDisplayed() },
                        onMuteSelected = { viewModel.muteSelected() },
                        onUnmuteSelected = { viewModel.unmuteSelected() },
                        onFavoriteSelected = { viewModel.favoriteSelected() },
                        onRequestDelete = { showDeleteConfirmDialog = true }
                    )
                }
            }

            // 5. قائمة الحالات الفعلية (Status Cards)
            val displayedList = uiState.displayedStatuses
            if (uiState.isLoading) {
                item {
                    BentoSkeletonList(itemCount = 4, itemHeight = 110.dp)
                }
            } else if (displayedList.isEmpty()) {
                item {
                    EmptyStatusesCard(
                        subTab = uiState.selectedSubTab,
                        hasSearch = uiState.searchQuery.isNotEmpty(),
                        onClearSearch = { viewModel.setSearchQuery("") },
                        onRefresh = { viewModel.refreshStatuses() },
                        onSwitchToNew = { viewModel.selectSubTab(StatusSubTab.NEW) }
                    )
                }
            } else {
                items(displayedList, key = { it.id }) { story ->
                    val isFavorite = story.isFavorite || uiState.settings.favoriteSenders.contains(story.senderPhone)
                    val isMuted = story.isMuted || uiState.settings.mutedSenders.contains(story.senderPhone)
                    val isSelected = uiState.selectedIds.contains(story.id)

                    StatusStoryCard(
                        story = story,
                        isFavorite = isFavorite,
                        isMuted = isMuted,
                        isMultiSelectMode = uiState.isMultiSelectMode,
                        isSelected = isSelected,
                        onToggleFavorite = { viewModel.toggleFavorite(story) },
                        onToggleMuted = { viewModel.toggleMuted(story) },
                        onSelectToggle = { viewModel.toggleSelection(story.id) },
                        onReactClick = { emoji ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.viewAndReact(story.id, emoji)
                        },
                        onDownloadClick = {
                            viewModel.downloadStatus(context, story) { _, msg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        onCardClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedStatusIdForPreview = story.id
                        }
                    )
                }
            }
        }

        // نافذة معاينة الحالة المنبثقة
        val currentPreviewStory = uiState.statuses.find { it.id == selectedStatusIdForPreview }
        currentPreviewStory?.let { previewStory ->
            StatusPreviewDialog(
                story = previewStory,
                onDismiss = { selectedStatusIdForPreview = null },
                onDownloadOrCopy = {
                    viewModel.downloadStatus(context, previewStory) { _, msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                },
                onReactClick = { emoji ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.viewAndReact(previewStory.id, emoji)
                },
                onRequestDownload = { id ->
                    viewModel.requestStatusMediaDownload(id)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}
