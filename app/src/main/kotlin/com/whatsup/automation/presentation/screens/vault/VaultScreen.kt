package com.whatsup.automation.presentation.screens.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.components.BentoSkeletonList
import com.whatsup.automation.presentation.screens.vault.components.*
import com.whatsup.automation.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedItem = uiState.selectedItem
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    fun showFeedback(message: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = uiState.viewMode == VaultViewMode.SUB_PAGE) {
        viewModel.navigateBackToHub()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkBgPrimary
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBgPrimary)
        ) {
            AnimatedContent(
                targetState = uiState.viewMode,
                transitionSpec = {
                    if (targetState == VaultViewMode.SUB_PAGE) {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    } else {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    }
                },
                label = "VaultViewModeAnim"
            ) { mode ->
                if (mode == VaultViewMode.HUB) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
                    ) {
                        item {
                            AppScreenHeader(
                                title = "الرسائل المحذوفة",
                                subtitle = "كاشف النصوص المحذوفة والرسائل المعدلة سراً",
                                onOpenDrawer = onOpenDrawer
                            )
                        }

                        item {
                            VaultHeaderBanner(
                                deletedTextCount = uiState.deletedTextCount,
                                editedCount = uiState.editedCount,
                                trackingMode = uiState.trackingMode,
                                trackedPhonesCount = uiState.trackedPhones.size,
                                onOpenTrackingConfig = { viewModel.showTrackingConfigDialog() }
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("بحث في كافة الرسائل والوسائط...", fontSize = 12.sp, color = TextMuted)
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Search, contentDescription = null, tint = WhatsAppGreenLight)
                                },
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
                                    focusedBorderColor = WhatsAppGreenLight,
                                    unfocusedBorderColor = Color(0x3300E676),
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        if (uiState.isLoading) {
                            item {
                                BentoSkeletonList(itemCount = 4, itemHeight = 90.dp)
                            }
                        } else {
                            item {
                                Text(
                                    text = "اختر القسم لفتح صفحته الكاملة:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            item {
                                VaultHubCategoryCard(
                                    title = "النصوص المحذوفة",
                                    subtitle = "الرسائل النصية التي تم حذفها لدى الجميع من الطرفين",
                                    count = uiState.deletedTextCount,
                                    countLabel = "رسالة",
                                    color = Color(0xFFFF4D4D),
                                    icon = Icons.Default.DeleteSweep,
                                    latestInfo = "حفظ تلقائي لكافة المحادثات قبل الحذف",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.navigateToSubPage(VaultSubTab.TEXTS)
                                    }
                                )
                            }

                            item {
                                VaultHubCategoryCard(
                                    title = "رسائل تم تعديلها",
                                    subtitle = "كشف النص الأصلي السابق ومقارنته بالمعدل قبل التعديل",
                                    count = uiState.editedCount,
                                    countLabel = "تعديلات",
                                    color = AmberWarning,
                                    icon = Icons.Default.Edit,
                                    latestInfo = "كشف التعديل وحفظ النص السابق فور وقوعه",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.navigateToSubPage(VaultSubTab.EDITED)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
                    ) {
                        item {
                            VaultSubPageHeader(
                                activeTab = uiState.activeTab,
                                onBack = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.navigateBackToHub()
                                }
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("ابحث بالاسم، الرقم، أو النص...", fontSize = 12.sp, color = TextMuted)
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Search, contentDescription = null, tint = WhatsAppGreenLight)
                                },
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
                                    focusedBorderColor = WhatsAppGreenLight,
                                    unfocusedBorderColor = Color(0x3300E676),
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    VaultCategoryTabButton(
                                        title = "نصوص محذوفة",
                                        count = uiState.deletedTextCount,
                                        icon = Icons.Default.DeleteSweep,
                                        isSelected = uiState.activeTab == VaultSubTab.TEXTS,
                                        activeColor = Color(0xFFFF4D4D),
                                        onClick = { viewModel.setActiveTab(VaultSubTab.TEXTS) }
                                    )
                                }
                                item {
                                    VaultCategoryTabButton(
                                        title = "رسائل معدلة",
                                        count = uiState.editedCount,
                                        icon = Icons.Default.Edit,
                                        isSelected = uiState.activeTab == VaultSubTab.EDITED,
                                        activeColor = AmberWarning,
                                        onClick = { viewModel.setActiveTab(VaultSubTab.EDITED) }
                                    )
                                }
                            }
                        }

                        if (uiState.isLoading) {
                            item {
                                BentoSkeletonList(itemCount = 5, itemHeight = 90.dp)
                            }
                        } else {
                            item {
                                when (uiState.activeTab) {
                                    VaultSubTab.TEXTS -> {
                                        DeletedTextsBentoSection(
                                            items = uiState.deletedTexts,
                                            onCopyText = { text ->
                                                viewModel.copyTextToClipboard(text)
                                                showFeedback("تم نسخ النص المحذوف بنجاح")
                                            },
                                            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                            onDelete = { id ->
                                                viewModel.deleteItem(id)
                                                showFeedback("تم حذف السجل من الخزينة")
                                            },
                                            onOpenUserHistory = { phone, name ->
                                                viewModel.openUserHistory(phone, name)
                                            }
                                        )
                                    }

                                    VaultSubTab.EDITED -> {
                                        EditedMessagesBentoSection(
                                            items = uiState.editedMessages,
                                            onCopyText = { text ->
                                                viewModel.copyTextToClipboard(text)
                                                showFeedback("تم نسخ النص بنجاح")
                                            },
                                            onDelete = { id ->
                                                viewModel.deleteItem(id)
                                                showFeedback("تم حذف السجل من الخزينة")
                                            },
                                            onOpenUserHistory = { phone, name ->
                                                viewModel.openUserHistory(phone, name)
                                            }
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

    if (selectedItem != null) {
        VaultItemPreviewDialog(
            item = selectedItem,
            onDismiss = { viewModel.selectItem(null) },
            onSaveToGallery = { item ->
                viewModel.saveMediaToGallery(item) { success, msg ->
                    showFeedback(if (success) "تم الحفظ في ألبوم الهاتف" else "تعذر الحفظ: $msg")
                }
            },
            onToggleFavorite = { viewModel.toggleFavorite(selectedItem.id) },
            onDelete = {
                viewModel.deleteItem(selectedItem.id)
                showFeedback("تم حذف الوسائط")
            }
        )
    }

    if (uiState.inspectedUserPhone != null) {
        UserHistoryDialog(
            userName = uiState.inspectedUserName ?: uiState.inspectedUserPhone!!,
            userPhone = uiState.inspectedUserPhone!!,
            logs = uiState.inspectedUserHistory,
            onDismiss = { viewModel.closeUserHistory() },
            onCopy = { text ->
                viewModel.copyTextToClipboard(text)
                showFeedback("تم نسخ النص")
            }
        )
    }

    if (uiState.isTrackingDialogVisible) {
        VaultTrackingConfigDialog(
            currentMode = uiState.trackingMode,
            trackedPhones = uiState.trackedPhones,
            onDismiss = { viewModel.hideTrackingConfigDialog() },
            onSaveMode = { mode -> viewModel.setTrackingMode(mode) },
            onAddPhone = { phone -> viewModel.addTrackedPhone(phone) },
            onRemovePhone = { phone -> viewModel.removeTrackedPhone(phone) }
        )
    }
}
