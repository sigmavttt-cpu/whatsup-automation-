package com.whatsup.automation.presentation.screens.groups

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.Group
import com.whatsup.automation.domain.model.GroupLogStatus
import com.whatsup.automation.domain.model.GroupMember
import com.whatsup.automation.domain.model.GroupMessageLog
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.components.GradientButton
import com.whatsup.automation.presentation.components.LiquidGradientButton
import com.whatsup.automation.presentation.components.SegmentedPillSelector
import com.whatsup.automation.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    com.whatsup.automation.presentation.components.NotificationCenterSheet(
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshGroups() },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
        // شريط العنوان العلوي + زر إنشاء حملة / جرس الإشعارات
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الحملات والعمل الجماعي",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "تقسيم ذكي لجهات الاتصال وبث آمن بدون حظر",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.whatsup.automation.presentation.components.NotificationBellButton(
                        unreadCount = unreadCount,
                        onClick = onOpenNotifications
                    )

                    IconButton(
                        onClick = { viewModel.openCreateModal() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(WhatsAppGreen, CyberCyan))
                            )
                            .size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "إنشاء حملة جديدة",
                            tint = DarkBgPrimary
                        )
                    }
                }
            }
        }


        // حقل البحث عن مجموعة أو بطاقة
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("ابحث في البطاقات والحملات...") },
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
        if (filteredGroups.isEmpty()) {
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "لا توجد بطاقات أو حملات حالياً",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "اضغط على زر (+) بالأعلى لاختيار جهات الاتصال وتقسيمها إلى بطاقات متساوية بأسلوب مرن وفاخر",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        LiquidGradientButton(
                            text = "إنشاء حملة أو تقسيم جهات الاتصال الآن",
                            onClick = { viewModel.openCreateModal() },
                            modifier = Modifier.padding(top = 8.dp),
                            icon = Icons.Filled.Add
                        )
                    }
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

@Composable
fun GroupCardItem(
    group: Group,
    estimatedTime: String,
    broadcastText: String,
    isBroadcasting: Boolean,
    isPaused: Boolean,
    progress: Pair<Int, Int>?,
    onBroadcastTextChange: (String) -> Unit,
    onSendBroadcast: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpenDetails: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = if (isBroadcasting) {
            if (isPaused) ObsidianGlowBorder.Amber else ObsidianGlowBorder.Cyan
        } else ObsidianGlowBorder.Emerald
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // رأس البطاقة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                            .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = group.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WhatsAppGreen.copy(alpha = 0.15f))
                                    .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${group.memberCount} جهة اتصال",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhatsAppGreen
                                )
                            }

                            Text(
                                text = "• الوقت المقدر: $estimatedTime",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onOpenDetails) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "فتح التفاصيل", tint = CyberCyan)
                    }
                    IconButton(onClick = onDeleteGroup) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف المجموعة", tint = CoralError)
                    }
                }
            }

            if (group.description.isNotBlank()) {
                Text(
                    text = group.description,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = Color(0x15FFFFFF))

            // حقل إدخال الرسالة الممتد ذاتياً (Auto-Expanding Multi-line)
            Text(
                text = "نص الرسالة الموجهة لأعضاء البطاقة:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = broadcastText,
                    onValueChange = onBroadcastTextChange,
                    placeholder = { Text("اكتب نص الرسالة هنا (يتمدد الحقل تلقائياً)...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    minLines = 2,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = Color(0x26FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = onSendBroadcast,
                    enabled = broadcastText.isNotBlank() && !isBroadcasting,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (broadcastText.isNotBlank() && !isBroadcasting) WhatsAppGreen else DarkSurface
                        )
                        .size(50.dp)
                ) {
                    if (isBroadcasting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBgPrimary)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "إرسال للبث",
                            tint = if (broadcastText.isNotBlank()) DarkBgPrimary else TextMuted
                        )
                    }
                }
            }

            // شريط التقدم عند البث المباشر
            if (isBroadcasting && progress != null) {
                val (sent, total) = progress
                val fraction = if (total > 0) sent.toFloat() / total.toFloat() else 0f
                val percent = (fraction * 100).toInt()
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPaused) AmberWarning.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Filled.PauseCircle else Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (isPaused) AmberWarning else CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isPaused) "الحملة متوقفة مؤقتاً ⏸️" else "درع منع الحظر نشط 🛡️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaused) AmberWarning else CyberCyan
                                )
                            }
                            Text(
                                text = "$sent من $total ($percent%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) AmberWarning else WhatsAppGreenLight
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isPaused) AmberWarning else WhatsAppGreen,
                            trackColor = Color(0x33FFFFFF)
                        )

                        // أزرار التحكم: إيقاف مؤقت / استئناف / إلغاء
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPaused) {
                                Button(
                                    onClick = onResume,
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DarkBgPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("استئناف الإرسال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBgPrimary)
                                }
                            } else {
                                Button(
                                    onClick = onPause,
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning.copy(alpha = 0.2f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Pause, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إيقاف مؤقت", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                }
                            }

                            OutlinedButton(
                                onClick = onCancel,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, tint = CoralError, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إلغاء", fontSize = 11.sp, color = CoralError)
                            }
                        }
                    }
                }
            }

            // زر فتح التفاصيل وإدارة الأعضاء
            TextButton(
                onClick = onOpenDetails,
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إدارة الأعضاء (${group.memberCount}) وسجل الإرسال",
                        fontSize = 13.sp,
                        color = CyberCyan
                    )
                    Icon(
                        Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    group: Group,
    uiState: GroupsUiState,
    viewModel: GroupsViewModel,
    onBackClick: () -> Unit,
    onTabSelect: (Int) -> Unit,
    onBroadcastTextChange: (String) -> Unit,
    onSendBroadcast: () -> Unit,
    onPauseBroadcast: () -> Unit,
    onResumeBroadcast: () -> Unit,
    onCancelBroadcast: () -> Unit,
    onRemoveMember: (Long) -> Unit,
    onOpenAddMember: () -> Unit
) {
    val broadcastText = uiState.broadcastTexts[group.id] ?: ""
    val isBroadcasting = uiState.isBroadcastingMap[group.id] == true
    val isPaused = uiState.isPausedMap[group.id] == true
    val progress = uiState.broadcastingProgress[group.id]
    val estimatedTime = viewModel.calculateEstimatedTime(uiState.selectedGroupMembers.size)

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshGroups() },
        state = pullToRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

        // شريط علوي مع زر الرجوع
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .size(42.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${uiState.selectedGroupMembers.size} عضو • الوقت المقدر: $estimatedTime",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onOpenAddMember,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x3325D366))
                    .size(42.dp)
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "إضافة عضو", tint = WhatsAppGreen)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // حقل إدخال الرسالة الممتد داخل التفاصيل
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إرسال رسالة بث لأعضاء البطاقة:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "يدعم Spintax والتنويع 🎲",
                        fontSize = 10.sp,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                // شريط إدراج المتغيرات والـ Spintax السريعة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val variables = listOf(
                        "{first_name}" to "الاسم الأول",
                        "{name}" to "الاسم كامل",
                        "{time_greeting}" to "التحية بالوقت",
                        "{day_name}" to "اليوم",
                        "{{أهلاً|مرحباً|حياك الله}}" to "Spintax ترحيب",
                        "---" to "تبديل القالب"
                    )
                    variables.forEach { (tag, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1825D366))
                                .clickable {
                                    onBroadcastTextChange(broadcastText + if (broadcastText.isBlank()) tag else " $tag")
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ $label",
                                color = WhatsAppGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = broadcastText,
                        onValueChange = onBroadcastTextChange,
                        placeholder = { Text("اكتب الرسالة (استخدم {{خيار1|خيار2}} و {name} للتنويع)...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        minLines = 2,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    GradientButton(
                        text = if (isBroadcasting) (if (isPaused) "موقوف" else "جاري...") else "إرسال",
                        onClick = onSendBroadcast,
                        enabled = broadcastText.isNotBlank() && !isBroadcasting,
                        icon = Icons.Filled.Send
                    )
                }

                // معاينة حية لعينة من النص المولد عشوائياً
                if (broadcastText.isNotBlank()) {
                    val previewSpun = remember(broadcastText) {
                        com.whatsup.automation.domain.util.SpintaxEngine.process(
                            template = com.whatsup.automation.domain.util.SpintaxEngine.getTemplateForIndex(broadcastText, 0, 10),
                            recipientName = "محمد أحمد",
                            recipientPhone = "+967770000000"
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1000E5FF))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "🎲 عينة حية لنص الرسالة كما ستصل للعميل:",
                                fontSize = 10.sp,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = previewSpun,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isBroadcasting && progress != null) {
                    val (sent, total) = progress
                    val fraction = if (total > 0) sent.toFloat() / total.toFloat() else 0f
                    val percent = (fraction * 100).toInt()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPaused) "⏸️ الحملة متوقفة مؤقتاً" else "⚡ جارٍ البث الآمن...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) AmberWarning else CyberCyan
                            )
                            Text(
                                text = "$sent من $total ($percent%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) AmberWarning else WhatsAppGreenLight
                            )
                        }

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isPaused) AmberWarning else WhatsAppGreen,
                            trackColor = Color(0x33FFFFFF)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPaused) {
                                Button(
                                    onClick = onResumeBroadcast,
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DarkBgPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("استئناف الإرسال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBgPrimary)
                                }
                            } else {
                                Button(
                                    onClick = onPauseBroadcast,
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning.copy(alpha = 0.2f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Pause, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إيقاف مؤقت", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                }
                            }

                            OutlinedButton(
                                onClick = onCancelBroadcast,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, tint = CoralError, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إلغاء", fontSize = 11.sp, color = CoralError)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // شريط التبويبات الثلاثة (الأعضاء / المستلمين منهم / في الانتظار)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton(
                title = "الأعضاء",
                count = uiState.selectedGroupMembers.size,
                isSelected = uiState.activeDetailsTab == 0,
                onClick = { onTabSelect(0) },
                modifier = Modifier.weight(1f)
            )

            TabButton(
                title = "المستلمين منهم",
                count = uiState.selectedGroupCompletedLogs.size,
                isSelected = uiState.activeDetailsTab == 1,
                onClick = { onTabSelect(1) },
                modifier = Modifier.weight(1f)
            )

            TabButton(
                title = "في الانتظار",
                count = uiState.selectedGroupInsiteLogs.size,
                isSelected = uiState.activeDetailsTab == 2,
                onClick = { onTabSelect(2) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // محتوى التبويب المختار
        when (uiState.activeDetailsTab) {
            0 -> {
                // تبويب الأعضاء مع زر الحذف الأحمر وزر الإضافة
                MembersListTab(
                    members = uiState.selectedGroupMembers,
                    onRemoveMember = onRemoveMember,
                    onOpenAddMember = onOpenAddMember
                )
            }
            1 -> {
                // تبويب سجل المستلمين (الناجح)
                LogsListTab(
                    logs = uiState.selectedGroupCompletedLogs,
                    emptyMessage = "لا يوجد سجل رسائل مستلمة بعد"
                )
            }
            2 -> {
                // تبويب سجل في الانتظار (الجاري)
                LogsListTab(
                    logs = uiState.selectedGroupInsiteLogs,
                    emptyMessage = "لا توجد رسائل قيد الإرسال أو في الانتظار حالياً"
                )
            }
        }
    }
    }
}

@Composable
fun MembersListTab(
    members: List<GroupMember>,
    onRemoveMember: (Long) -> Unit,
    onOpenAddMember: () -> Unit
) {
    if (members.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.PersonOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Text("لا يوجد أعضاء في هذه المجموعة حالياً", fontSize = 14.sp, color = TextMuted)
                GradientButton(
                    text = "إضافة أعضاء الآن",
                    onClick = onOpenAddMember,
                    icon = Icons.Filled.PersonAdd
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة جهات الاتصال المسجلة بالبطاقة:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    TextButton(onClick = onOpenAddMember) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة جهة اتصال", fontSize = 12.sp, color = WhatsAppGreen)
                    }
                }
            }

            items(members, key = { it.id }) { member ->
                MemberRowItem(
                    member = member,
                    onRemove = { onRemoveMember(member.id) }
                )
            }
        }
    }
}

@Composable
fun MemberRowItem(
    member: GroupMember,
    onRemove: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x2606B6D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.contactName.take(1).ifBlank { "؟" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }

                Column {
                    Text(
                        text = member.contactName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = member.phone,
                        fontSize = 12.sp,
                        color = TextMuted,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr)
                    )
                }
            }

            // زر الإزالة الأحمر الدائري البارز (-)
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x33F43F5E))
                    .border(1.dp, CoralError, CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "إزالة العضو",
                    tint = CoralError,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun LogsListTab(
    logs: List<GroupMessageLog>,
    emptyMessage: String
) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                Text(text = emptyMessage, fontSize = 14.sp, color = TextMuted)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(logs, key = { it.id }) { log ->
                GroupLogCardItem(log = log)
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0x3325D366) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) WhatsAppGreen else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) WhatsAppGreen else TextMuted
            )

            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) WhatsAppGreen else TextMuted)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBgPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun GroupLogCardItem(log: GroupMessageLog) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }
    val formattedTime = log.sentAt?.let { dateFormat.format(Date(it.toEpochMilli())) } ?: "الآن"

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${log.recipientName} (${log.recipientPhone})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Text(
                    text = when (log.status) {
                        GroupLogStatus.IN_PROGRESS -> "جاري الإرسال ⏳"
                        GroupLogStatus.COMPLETED -> "تم الإرسال بنجاح ✅"
                        GroupLogStatus.FAILED -> "فشل الإرسال ❌"
                    },
                    fontSize = 11.sp,
                    color = when (log.status) {
                        GroupLogStatus.IN_PROGRESS -> AmberWarning
                        GroupLogStatus.COMPLETED -> WhatsAppGreen
                        GroupLogStatus.FAILED -> CoralError
                    }
                )
            }

            Text(
                text = "\"${log.messageText}\"",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formattedTime, fontSize = 11.sp, color = TextMuted)
                if (!log.errorMessage.isNullOrBlank()) {
                    Text(text = log.errorMessage, fontSize = 11.sp, color = CoralError)
                }
            }
        }
    }
}

