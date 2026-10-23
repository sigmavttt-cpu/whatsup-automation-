package com.whatsup.automation.presentation.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppEmptyState
import com.whatsup.automation.presentation.components.BentoSkeletonList
import com.whatsup.automation.presentation.components.NotificationBellButton
import com.whatsup.automation.presentation.components.NotificationCenterSheet
import com.whatsup.automation.presentation.screens.groups.components.GroupCardItem
import com.whatsup.automation.presentation.theme.*

@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    var showNotificationsSheet by remember { mutableStateOf(false) }

    when {
        uiState.isCreateModalOpen -> {
            CreateCampaignScreen(
                uiState = uiState,
                onBackClick = { viewModel.closeCreateModal() },
                onGroupNameChange = { viewModel.onNewGroupNameChange(it) },
                onGroupDescChange = { viewModel.onNewGroupDescriptionChange(it) },
                onPartitionModeChange = { viewModel.onPartitionModeChange(it) },
                onPartitionSizeChange = { viewModel.onPartitionSizeChange(it) },
                onContactSearchChange = { viewModel.onContactSearchQueryChange(it) },
                onToggleContact = { viewModel.toggleContactSelection(it) },
                onSelectAll = { viewModel.selectAllContacts() },
                onClearAll = { viewModel.clearContactSelection() },
                onCreateConfirm = { viewModel.createGroup() }
            )
        }
        uiState.selectedGroup != null -> {
            GroupDetailsScreen(
                group = uiState.selectedGroup!!,
                uiState = uiState,
                viewModel = viewModel,
                onBackClick = { viewModel.closeGroupDetails() },
                onTabSelect = { viewModel.setDetailsTab(it) },
                onBroadcastTextChange = { viewModel.onBroadcastTextChange(uiState.selectedGroup!!.id, it) },
                onSendBroadcast = { viewModel.sendBroadcast(uiState.selectedGroup!!.id) },
                onPauseBroadcast = { viewModel.pauseBroadcast(uiState.selectedGroup!!.id) },
                onResumeBroadcast = { viewModel.resumeBroadcast(uiState.selectedGroup!!.id) },
                onCancelBroadcast = { viewModel.cancelBroadcast(uiState.selectedGroup!!.id) },
                onRemoveMember = { viewModel.removeMemberFromGroup(it) },
                onOpenAddMember = { viewModel.openAddMemberModal() }
            )
        }
        else -> {
            GroupsListScreen(
                uiState = uiState,
                viewModel = viewModel,
                unreadCount = unreadCount,
                onOpenNotifications = {
                    showNotificationsSheet = true
                    viewModel.markNotificationsAsRead()
                },
                modifier = modifier
            )
        }
    }

    if (uiState.isAddMemberModalOpen) {
        AddMemberDialog(
            uiState = uiState,
            onDismiss = { viewModel.closeAddMemberModal() },
            onSearchChange = { viewModel.onAddMemberSearchQueryChange(it) },
            onToggleContact = { viewModel.toggleMemberToAddSelection(it) },
            onCustomNameChange = { viewModel.onCustomAddNameChange(it) },
            onCustomPhoneChange = { viewModel.onCustomAddPhoneChange(it) },
            onConfirmFromContacts = { viewModel.addSelectedContactsToGroup() },
            onConfirmCustom = { viewModel.addCustomContactToGroup() }
        )
    }

    NotificationCenterSheet(
        isOpen = showNotificationsSheet,
        notifications = notifications,
        onDismiss = { showNotificationsSheet = false },
        onClearAll = { viewModel.clearNotifications() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsListScreen(
    uiState: GroupsUiState,
    viewModel: GroupsViewModel,
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val filteredGroups = remember(uiState.groups, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.groups
        } else {
            uiState.groups.filter {
                it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                it.description.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBgPrimary,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.openCreateModal()
                },
                containerColor = WhatsAppGreen,
                contentColor = DarkBgPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "إنشاء حملة")
                    Text("حملة جديدة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshGroups() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                // شريط العنوان العلوي + جرس الإشعارات
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الحملات والعمل الجماعي",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "تقسيم ذكي لجهات الاتصال وبث آمن بدون حظر",
                                fontSize = 12.5.sp,
                                color = TextSecondary
                            )
                        }

                        NotificationBellButton(
                            unreadCount = unreadCount,
                            onClick = onOpenNotifications
                        )
                    }
                }

                // حقل البحث عن مجموعة أو بطاقة
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("ابحث في البطاقات والحملات...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // قائمة بطاقات المجموعات
                if (uiState.isLoading) {
                    item {
                        BentoSkeletonList(itemCount = 3, itemHeight = 160.dp)
                    }
                } else if (filteredGroups.isEmpty()) {
                    item {
                        if (uiState.searchQuery.isNotBlank()) {
                            AppEmptyState(
                                icon = Icons.Filled.SearchOff,
                                title = "لا توجد بطاقات مطابقة لبحثك",
                                subtitle = "لم نعثر على أي بطاقة أو حملة تحتوي على \"${uiState.searchQuery}\".",
                                actionLabel = "مسح البحث",
                                onActionClick = { viewModel.onSearchQueryChange("") }
                            )
                        } else {
                            AppEmptyState(
                                icon = Icons.Outlined.Groups,
                                title = "لا توجد بطاقات أو حملات حالياً",
                                subtitle = "اضغط على زر (حملة جديدة) بالأسفل لاختيار جهات الاتصال وتقسيمها إلى بطاقات منظمة",
                                actionLabel = "إنشاء حملة جديدة",
                                onActionClick = { viewModel.openCreateModal() }
                            )
                        }
                    }
                } else {
                    items(filteredGroups, key = { it.id }) { group ->
                        val isBroadcasting = uiState.isBroadcastingMap[group.id] == true
                        val isPaused = uiState.isPausedMap[group.id] == true
                        val progress = uiState.broadcastingProgress[group.id]
                        val estimatedTime = viewModel.calculateEstimatedTime(group.memberCount)

                        GroupCardItem(
                            group = group,
                            estimatedTime = estimatedTime,
                            broadcastText = uiState.broadcastTexts[group.id] ?: "",
                            isBroadcasting = isBroadcasting,
                            isPaused = isPaused,
                            progress = progress,
                            onBroadcastTextChange = { viewModel.onBroadcastTextChange(group.id, it) },
                            onSendBroadcast = { viewModel.sendBroadcast(group.id) },
                            onPause = { viewModel.pauseBroadcast(group.id) },
                            onResume = { viewModel.resumeBroadcast(group.id) },
                            onCancel = { viewModel.cancelBroadcast(group.id) },
                            onOpenDetails = { viewModel.selectGroupForDetails(group) },
                            onDeleteGroup = { viewModel.deleteGroup(group.id) }
                        )
                    }
                }
            }
        }
    }
}
