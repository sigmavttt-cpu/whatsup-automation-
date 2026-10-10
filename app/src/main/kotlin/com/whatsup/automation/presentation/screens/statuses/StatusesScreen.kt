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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.StatusMediaType
import com.whatsup.automation.domain.model.StatusStory
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.PulseReactor
import com.whatsup.automation.presentation.components.StoryAvatarRing
import com.whatsup.automation.presentation.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusesScreen(
    viewModel: StatusesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableEmojis = listOf("💚", "❤️", "⭐", "🔥", "🫀", "😂", "👍", "👏", "🎉", "🤲", "💯", "😍", "🥳")
    var showCustomEmojiDialog by remember { mutableStateOf(false) }
    var customEmojiInput by remember { mutableStateOf("") }
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

                        // زر جلب الحالات الفوري
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
            if (displayedList.isEmpty()) {
                item {
                    EmptyStatusesCard(subTab = uiState.selectedSubTab, hasSearch = uiState.searchQuery.isNotEmpty())
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
                        }
                    )
                }
            }
        }
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
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
 * بطاقة الحالة الفاخرة مع القلب الأخضر التفاعلي وزر الاستثناء والمفضلة.
 */
@Composable
private fun StatusStoryCard(
    story: StatusStory,
    isFavorite: Boolean,
    isMuted: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleMuted: () -> Unit,
    onSelectToggle: () -> Unit,
    onReactClick: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())
    }

    val borderBrush = when {
        isMuted -> Brush.linearGradient(listOf(RoseError.copy(alpha = 0.4f), Color(0x10FFFFFF)))
        isFavorite -> ObsidianGlowBorder.Amber
        story.isReacted -> ObsidianGlowBorder.Emerald
        else -> ObsidianGlowBorder.None
    }

    // حركة نبض القلب الأخضر التفاعلي
    val heartScale by animateFloatAsState(
        targetValue = if (story.isReacted) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heartScale"
    )

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = borderBrush,
        onClick = if (isMultiSelectMode) onSelectToggle else null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // سطر الرأس (صورة الشخص + الاسم + المفضلة + زر الاستثناء)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // اختيار متعدد
                    if (isMultiSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectToggle() },
                            colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                        )
                    }

                    // دائرة القصة
                    StoryAvatarRing(
                        initials = story.senderName.ifBlank { story.senderPhone }.take(2),
                        isViewed = story.isViewed || story.isReacted,
                        size = 46.dp
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = story.senderName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMuted) TextMuted else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isFavorite) {
                                Text("⭐", fontSize = 12.sp)
                            }
                            if (isMuted) {
                                Text("🚫", fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = "${story.senderPhone} • ${timeFormatter.format(story.timestamp)}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // أزرار التحكم السريع (نجمة المفضلة + زر الاستثناء السريع)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // زر المفضلة
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFavorite()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) AmberWarning else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // زر الاستثناء / التعطيل السريع (مشاهدة أحمد واستثناء سعيد)
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleMuted()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) RoseError.copy(alpha = 0.2f) else Color(0x10FFFFFF))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Outlined.VolumeUp,
                            contentDescription = if (isMuted) "إلغاء الاستثناء" else "استثناء من المشاهدة",
                            tint = if (isMuted) RoseError else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // محتوى الحالة (نص، صورة، فيديو)
            if (!story.textContent.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x15FFFFFF))
                        .padding(10.dp)
                ) {
                    Text(
                        text = story.textContent,
                        fontSize = 12.5.sp,
                        color = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // سطر الحالة والتفاعل مع القلب الأخضر التفاعلي المضيء 💚
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // نوع وسائط الحالة ووقت المشاهدة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val mediaIcon = when (story.mediaType) {
                        StatusMediaType.IMAGE -> Icons.Filled.Image
                        StatusMediaType.VIDEO -> Icons.Filled.Videocam
                        StatusMediaType.TEXT -> Icons.Filled.TextFields
                    }
                    val mediaLabel = when (story.mediaType) {
                        StatusMediaType.IMAGE -> "صورة"
                        StatusMediaType.VIDEO -> "فيديو"
                        StatusMediaType.TEXT -> "نص"
                    }

                    Icon(mediaIcon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(mediaLabel, fontSize = 11.sp, color = TextMuted)

                    if (story.isViewed) {
                        Text("•", fontSize = 11.sp, color = TextMuted)
                        Text("تمت المشاهدة ✓", fontSize = 11.sp, color = CyberCyan)
                    }
                }

                // 💚 القلب الأخضر التفاعلي الواقعي
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (story.isReacted) {
                        Text(
                            text = "تم التفاعل 💚",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhatsAppGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .scale(heartScale)
                            .clip(CircleShape)
                            .background(
                                if (story.isReacted) WhatsAppGreen.copy(alpha = 0.22f) else Color(0x10FFFFFF)
                            )
                            .border(
                                width = if (story.isReacted) 1.5.dp else 1.dp,
                                color = if (story.isReacted) WhatsAppGreen else Color(0x25FFFFFF),
                                shape = CircleShape
                            )
                            .clickable {
                                onReactClick("💚")
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = story.reactionEmoji?.ifBlank { "💚" } ?: "💚",
                            fontSize = if (story.isReacted) 18.sp else 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة الحالات الفارغة الإرشادية.
 */
@Composable
private fun EmptyStatusesCard(
    subTab: StatusSubTab,
    hasSearch: Boolean
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.None
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val (icon, title, desc) = when {
                hasSearch -> Triple(
                    Icons.Filled.SearchOff,
                    "لا توجد نتائج مطابقة للبحث",
                    "جرّب البحث باسم آخر أو رقم هاتف مختلف."
                )
                subTab == StatusSubTab.NEW -> Triple(
                    Icons.Filled.CheckCircleOutline,
                    "لا توجد حالات جديدة حالياً",
                    "تمت مشاهدة والتفاعل مع جميع الحالات المرصودة حتى الآن ⚡"
                )
                subTab == StatusSubTab.VIEWED -> Triple(
                    Icons.Filled.Visibility,
                    "لا توجد حالات تمت مشاهدتها بعد",
                    "ستظهر هنا الحالات فور مشاهدتها أو التفاعل معها."
                )
                subTab == StatusSubTab.FAVORITES -> Triple(
                    Icons.Filled.StarOutline,
                    "قائمة المفضلة فارغة",
                    "اضغط على رمز النجمة ⭐ بجانب أي جهة اتصال لإضافتها للمفضلة."
                )
                subTab == StatusSubTab.MUTED -> Triple(
                    Icons.Filled.VolumeOff,
                    "لا توجد جهات اتصال مستثناة",
                    "يمكنك استثناء أي شخص (مثل سعيد) من المشاهدة بالضغط على زر 🚫 بجانبه."
                )
                else -> Triple(Icons.Filled.Info, "لا توجد بيانات", "")
            }

            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(desc, fontSize = 12.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
