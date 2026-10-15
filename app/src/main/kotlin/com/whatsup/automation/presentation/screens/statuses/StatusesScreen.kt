package com.whatsup.automation.presentation.screens.statuses

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.StatusMediaType
import com.whatsup.automation.domain.model.StatusStory
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.BentoSkeletonList
import com.whatsup.automation.presentation.components.PulseReactor
import com.whatsup.automation.presentation.components.StoryAvatarRing
import com.whatsup.automation.presentation.screens.statuses.components.StatusPreviewDialog
import com.whatsup.automation.presentation.screens.statuses.components.StatusStoryCard
import com.whatsup.automation.presentation.theme.*
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusesScreen(
    viewModel: StatusesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    val availableEmojis = listOf("💚", "❤️", "⭐", "🔥", "🫀", "😂", "👍", "👏", "🎉", "🤲", "💯", "😍", "🥳")
    var showCustomEmojiDialog by remember { mutableStateOf(false) }
    var customEmojiInput by remember { mutableStateOf("") }
    var selectedStatusForPreview by remember { mutableStateOf<StatusStory?>(null) }
    val haptic = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()



    // نافذة إدخال إيموجي مخصص
    if (showCustomEmojiDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showCustomEmojiDialog = false
                customEmojiInput = ""
            }
        ) {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                borderBrush = ObsidianGlowBorder.Cyan
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تحديد إيموجي التفاعل المخصص",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "اكتب أو الصق أي إيموجي تفضله من لوحة المفاتيح",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    OutlinedTextField(
                        value = customEmojiInput,
                        onValueChange = { customEmojiInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثال: ⭐ أو 🫀 أو 💯", color = TextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showCustomEmojiDialog = false
                                customEmojiInput = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إلغاء", color = TextMuted)
                        }

                        Button(
                            onClick = {
                                val trimmed = customEmojiInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    viewModel.setDefaultEmoji(trimmed)
                                    showCustomEmojiDialog = false
                                    customEmojiInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تطبيق", color = DarkBgPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
            // 1. شريط الرأس والأزرار السريعة
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "أتمتة الحالات والتفاعلات",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "فلترة ذكية، استثناء جهات اتصال، وتفاعل لحظي 💚",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // زر وضع التحديد المتعدد
                        com.whatsup.automation.presentation.components.AppTooltipBox(
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

                        // زر جلب الحالات الفوري
                        com.whatsup.automation.presentation.components.AppTooltipBox(tooltipText = "تحديث وجلب أحدث الحالات من واتساب الآن") {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.fetchLatestStatuses()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = 0.15f))
                                    .border(1.dp, WhatsAppGreen.copy(alpha = 0.35f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "جلب الحالات",
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. بطاقة المؤشرات السريعة (Quick Status Metrics)
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
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
                                PulseReactor(isActive = true, activeColor = WhatsAppGreen)
                            }
                            Column {
                                Text(
                                    text = "محرك الحالات الذكي",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "تفاعل تلقائي: ${if (uiState.settings.autoReactEnabled) "مفعل 🟢" else "معطل ⚪"}",
                                    fontSize = 11.sp,
                                    color = WhatsAppGreenLight
                                )
                            }
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.viewAllStatusesAndReact()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = DarkBgPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تفاعل الكل 💚", fontSize = 11.sp, color = DarkBgPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. شريط البحث الذكي وترتيب الحالات
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // حقل البحث
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

                    // أزرار الترتيب السريع (Sort Chips)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الترتيب:", fontSize = 11.sp, color = TextMuted)

                        StatusSortChip(
                            label = "الأحدث ⏱️",
                            isSelected = uiState.sortBy == StatusSortBy.DATE_DESC,
                            onClick = { viewModel.setSortBy(StatusSortBy.DATE_DESC) }
                        )

                        StatusSortChip(
                            label = "الاسم 🔤",
                            isSelected = uiState.sortBy == StatusSortBy.NAME_ASC,
                            onClick = { viewModel.setSortBy(StatusSortBy.NAME_ASC) }
                        )

                        StatusSortChip(
                            label = "المفضلة ⭐",
                            isSelected = uiState.sortBy == StatusSortBy.FAVORITES,
                            onClick = { viewModel.setSortBy(StatusSortBy.FAVORITES) }
                        )
                    }
                }
            }

            // 4. شريط التبويبات مع العدادات الحية
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
                            modifier = Modifier.width(90.dp),
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
                            modifier = Modifier.width(90.dp),
                            tooltipText = "الحالات التي تمت مشاهدتها والتفاعل معها (${uiState.viewedCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.VIEWED) }
                        )
                    }

                    item {
                        StatusTabButton(
                            title = "المحذوفة ⏳",
                            count = uiState.deletedTempCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.DELETED_TEMP,
                            accentColor = Color(0xFFE11D48),
                            modifier = Modifier.width(110.dp),
                            tooltipText = "الحالات المحفوظة قبل حذفها من قِبل أصحابها (${uiState.deletedTempCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.DELETED_TEMP) }
                        )
                    }

                    item {
                        StatusTabButton(
                            title = "المستثناة 🚫",
                            count = uiState.mutedCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.MUTED,
                            accentColor = RoseError,
                            modifier = Modifier.width(105.dp),
                            tooltipText = "جهات الاتصال المستثناة من المشاهدة التلقائية (${uiState.mutedCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.MUTED) }
                        )
                    }

                    item {
                        StatusTabButton(
                            title = "المفضلة ⭐",
                            count = uiState.favoritesCount,
                            isSelected = uiState.selectedSubTab == StatusSubTab.FAVORITES,
                            accentColor = AmberWarning,
                            modifier = Modifier.width(95.dp),
                            tooltipText = "الحالات وجهات الاتصال المميزة بنجمة (${uiState.favoritesCount})",
                            onClick = { viewModel.selectSubTab(StatusSubTab.FAVORITES) }
                        )
                    }
                }
            }

            // 5. شريط إجراءات التحديد المتعدد (يظهر فقط عند تفعيل التحديد المتعدد)
            if (uiState.isMultiSelectMode) {
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderBrush = ObsidianGlowBorder.Cyan
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "محدد (${uiState.selectedIds.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { viewModel.selectAllDisplayed() }) {
                                    Text("تحديد الكل", fontSize = 11.sp, color = TextPrimary)
                                }

                                if (uiState.selectedIds.isNotEmpty()) {
                                    // زر استثناء المحددين (كتم سعيد وغيره)
                                    Button(
                                        onClick = { viewModel.muteSelected() },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("استثناء 🚫", fontSize = 10.sp, color = Color.White)
                                    }

                                    // زر تفعيل المحددين
                                    Button(
                                        onClick = { viewModel.unmuteSelected() },
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("تفعيل 🟢", fontSize = 10.sp, color = DarkBgPrimary)
                                    }

                                    // زر تفضيل المحددين
                                    Button(
                                        onClick = { viewModel.favoriteSelected() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("تفضيل ⭐", fontSize = 10.sp, color = DarkBgPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. قائمة الحالات الفعلية (Status Cards)
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
                            selectedStatusForPreview = story
                        }
                    )
                }
            }
        }

        // 6. نافذة معاينة الحالة المنبثقة الواقعية بمنتصف الشاشة مع زر (X) للإغلاق
        selectedStatusForPreview?.let { previewStory ->
            StatusPreviewDialog(
                story = previewStory,
                onDismiss = { selectedStatusForPreview = null },
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

/**
 * زر التبويب الفرعي مع شارة العدد.
 */
@Composable
private fun StatusTabButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    tooltipText: String = "",
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    com.whatsup.automation.presentation.components.AppTooltipBox(tooltipText = tooltipText) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) accentColor.copy(alpha = 0.18f) else DarkSurface.copy(alpha = 0.5f))
                .border(1.dp, if (isSelected) accentColor else Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accentColor else TextSecondary
                )
                Text(
                    text = "$count",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) TextPrimary else TextMuted
                )
            }
        }
    }
}

/**
 * زر فلتر الترتيب الصغير.
 */
@Composable
private fun StatusSortChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) WhatsAppGreen.copy(alpha = 0.2f) else Color(0x10FFFFFF))
            .border(1.dp, if (isSelected) WhatsAppGreen else Color(0x20FFFFFF), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = if (isSelected) WhatsAppGreen else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}



/**
 * بيانات بطاقة الحالة الفارغة.
 */
private data class EmptyStatusData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val desc: String,
    val ctaText: String,
    val ctaAction: () -> Unit
)

/**
 * بطاقة الحالات الفارغة الإرشادية مع أزرار الإجراء المباشر (Empty State with Actionable CTA).
 */
@Composable
private fun EmptyStatusesCard(
    subTab: StatusSubTab,
    hasSearch: Boolean,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    onSwitchToNew: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.None
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val config = when {
                hasSearch -> EmptyStatusData(
                    icon = Icons.Filled.SearchOff,
                    title = "لا توجد نتائج مطابقة للبحث",
                    desc = "جرّب البحث باسم آخر أو رقم هاتف مختلف.",
                    ctaText = "مسح البحث 🔍",
                    ctaAction = onClearSearch
                )
                subTab == StatusSubTab.NEW -> EmptyStatusData(
                    icon = Icons.Filled.CheckCircleOutline,
                    title = "لا توجد حالات جديدة حالياً",
                    desc = "تمت مشاهدة والتفاعل مع جميع الحالات المرصودة حتى الآن ⚡",
                    ctaText = "تحديث الحالات الآن 🔄",
                    ctaAction = onRefresh
                )
                subTab == StatusSubTab.VIEWED -> EmptyStatusData(
                    icon = Icons.Filled.Visibility,
                    title = "لا توجد حالات تمت مشاهدتها بعد",
                    desc = "ستظهر هنا الحالات فور مشاهدتها أو التفاعل معها.",
                    ctaText = "العودة للحالات الجديدة ✨",
                    ctaAction = onSwitchToNew
                )
                subTab == StatusSubTab.DELETED_TEMP -> EmptyStatusData(
                    icon = Icons.Filled.HistoryToggleOff,
                    title = "لا توجد حالات محذوفة مؤقتاً",
                    desc = "الحالات التي يحذفها أصحابها خلال 24 ساعة تُحفظ هنا تلقائياً لخصوصيتك ⏳",
                    ctaText = "تحديث الحالات 🔄",
                    ctaAction = onRefresh
                )
                subTab == StatusSubTab.FAVORITES -> EmptyStatusData(
                    icon = Icons.Filled.StarOutline,
                    title = "قائمة المفضلة فارغة",
                    desc = "اضغط على رمز النجمة ⭐ بجانب أي جهة اتصال لتفضيل حالاتها دائماً.",
                    ctaText = "استعراض الحالات الجديدة ✨",
                    ctaAction = onSwitchToNew
                )
                subTab == StatusSubTab.MUTED -> EmptyStatusData(
                    icon = Icons.Filled.VolumeOff,
                    title = "لا توجد جهات اتصال مستثناة",
                    desc = "يمكنك استثناء أي شخص من المشاهدة بالضغط على زر 🚫 بجانبه.",
                    ctaText = "العودة للحالات الجديدة ✨",
                    ctaAction = onSwitchToNew
                )
                else -> EmptyStatusData(
                    icon = Icons.Filled.Info,
                    title = "لا توجد حالات متوفرة",
                    desc = "ستظهر الحالات التلقائية بمجرد استلامها عبر واتساب.",
                    ctaText = "تحديث 🔄",
                    ctaAction = onRefresh
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GlassFill)
                    .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = config.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                text = config.desc,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    config.ctaAction()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = config.ctaText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
