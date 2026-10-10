package com.whatsup.automation.presentation.screens.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.ActivityLog
import com.whatsup.automation.domain.model.LogStatus
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.ObsidianGlowBorder

import com.whatsup.automation.presentation.screens.logs.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()

    // نافذة تفاصيل المحادثة المنبثقة
    uiState.selectedConversation?.let { conversation ->
        ConversationDetailDialog(
            conversation = conversation,
            onDismiss = { viewModel.closeConversation() },
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refreshLogs()
        },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
            // شريط الرأس
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الإعدادات وسجل النشاط",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "درع الحماية، محاكي السلوك البشري، وسجلات العمليات الحية",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // زر نسخ التقرير التشخيصي
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val report = viewModel.generateDiagnosticReport()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SIGMACODEX Diagnostic Report", report)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ التقرير التشخيصي إلى الحافظة بنجاح 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Filled.Assessment, contentDescription = "نسخ التقرير التشخيصي", tint = CyberCyan)
                        }

                        // زر مسح السجل
                        if (uiState.logs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.clearAllLogs()
                                    Toast.makeText(context, "تم مسح كافة السجلات", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(RoseError.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = "مسح السجل", tint = RoseError)
                            }
                        }
                    }
                }
            }

            // بطاقة درع الحماية ومحاكي السلوك البشري (Anti-Ban Shield) في تبويب الإعدادات
            item {
                val antiBan = uiState.antiBanSettings

                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderBrush = ObsidianGlowBorder.Emerald
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppGreen.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "درع الحماية ومحاكي السلوك البشري",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "محاكاة القراءة والكتابة الطبيعية ومنع الحظر",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = antiBan.isEnabled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.updateAntiBanSettings(antiBan.copy(isEnabled = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = WhatsAppGreen,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBgSecondary
                                )
                            )
                        }

                        if (antiBan.isEnabled) {
                            HorizontalDivider(color = Color(0x15FFFFFF))

                            // خيار محاكاة القراءة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("محاكاة قراءة الرسالة (Mark Read)", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text("تأكيد القراءة ثم الانتظار لحظات قبل البدء بالكتابة", fontSize = 11.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = antiBan.simulateReading,
                                    onCheckedChange = {
                                        viewModel.updateAntiBanSettings(antiBan.copy(simulateReading = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CyberCyan,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkBgSecondary
                                    )
                                )
                            }

                            // خيار سرعة الكتابة المتناسبة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("كتابة ديناميكية متناسبة مع طول النص", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text("إظهار 'يكتب الآن...' لفترة تتطابق مع عدد حروف الرد", fontSize = 11.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = antiBan.dynamicTypingSpeed,
                                    onCheckedChange = {
                                        viewModel.updateAntiBanSettings(antiBan.copy(dynamicTypingSpeed = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CyberCyan,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkBgSecondary
                                    )
                                )
                            }

                            HorizontalDivider(color = Color(0x10FFFFFF))

                            // منزلق فترة التهدئة بين الردود
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("فترة التهدئة بين الردود لنفس الرقم:", fontSize = 12.sp, color = TextSecondary)
                                    Text("${antiBan.contactCooldownSeconds} ثانية", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                                }
                                Slider(
                                    value = antiBan.contactCooldownSeconds.toFloat(),
                                    onValueChange = {
                                        viewModel.updateAntiBanSettings(antiBan.copy(contactCooldownSeconds = it.toInt()))
                                    },
                                    valueRange = 5f..60f,
                                    steps = 10,
                                    colors = SliderDefaults.colors(
                                        thumbColor = WhatsAppGreen,
                                        activeTrackColor = WhatsAppGreen,
                                        inactiveTrackColor = Color(0x22FFFFFF)
                                    )
                                )
                            }

                            // منزلق سقف الردود لمنع الحلقات
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("أقصى عدد ردود لنفس الرقم خلال 10 دقائق:", fontSize = 12.sp, color = TextSecondary)
                                    Text("${antiBan.maxRepliesPerWindow} ردود", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                                }
                                Slider(
                                    value = antiBan.maxRepliesPerWindow.toFloat(),
                                    onValueChange = {
                                        viewModel.updateAntiBanSettings(antiBan.copy(maxRepliesPerWindow = it.toInt()))
                                    },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonPurple,
                                        activeTrackColor = NeonPurple,
                                        inactiveTrackColor = Color(0x22FFFFFF)
                                    )
                                )
                            }

                            // شارة الحالة
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WhatsAppGreen.copy(alpha = 0.1f))
                                    .border(1.dp, WhatsAppGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "الدرع مفعّل: محصن ضد الحظر والحلقات اللانهائية",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WhatsAppGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // شريط التبويبات الثلاثية
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. تبويب المحادثات
                    val isConv = uiState.selectedTab == LogsTab.CONVERSATIONS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isConv) WhatsAppGreen else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.selectTab(LogsTab.CONVERSATIONS)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "المحادثات 💬 (${uiState.conversations.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isConv) DarkBgPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. تبويب سجل العمليات
                    val isRaw = uiState.selectedTab == LogsTab.RAW_LOGS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isRaw) WhatsAppGreen else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.selectTab(LogsTab.RAW_LOGS)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "سجل النشاط 📋 (${uiState.logs.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isRaw) DarkBgPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3. تبويب تشخيص المحرك
                    val isDiag = uiState.selectedTab == LogsTab.SYSTEM_DIAGNOSTICS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDiag) CyberCyan else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.selectTab(LogsTab.SYSTEM_DIAGNOSTICS)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "التشخيص ⚙️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDiag) DarkBgPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // شريط البحث
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = if (uiState.selectedTab == LogsTab.CONVERSATIONS)
                                "بحث برقم المتصل، الاسم، الرسالة، أو الرد..."
                            else
                                "بحث في السجل بالرقم أو القاعدة...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "مسح", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = Color(0x26FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // محتوى التبويب المختار
            when (uiState.selectedTab) {
                LogsTab.CONVERSATIONS -> {
                    if (uiState.filteredConversations.isEmpty()) {
                        item {
                            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Forum,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "لا توجد محادثات مسجلة حتى الآن",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "ستظهر هنا كافة المحادثات الواردة وبطاقاتها فور استقبال الرسائل",
                                            fontSize = 11.sp,
                                            color = TextMuted.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(uiState.filteredConversations, key = { it.senderPhone }) { conversation ->
                            ConversationCard(
                                conversation = conversation,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.openConversation(conversation)
                                }
                            )
                        }
                    }
                }

                LogsTab.RAW_LOGS -> {
                    // فلاتر الحالة لسجل النشاط
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChipItem(
                                    label = "الكل (${uiState.logs.size})",
                                    selected = uiState.selectedFilter == LogStatusFilter.ALL,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFilter(LogStatusFilter.ALL)
                                    }
                                )
                            }
                            item {
                                FilterChipItem(
                                    label = "ناجح (${uiState.logs.count { it.status == LogStatus.SUCCESS }})",
                                    selected = uiState.selectedFilter == LogStatusFilter.SUCCESS,
                                    selectedColor = WhatsAppGreen,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFilter(LogStatusFilter.SUCCESS)
                                    }
                                )
                            }
                            item {
                                FilterChipItem(
                                    label = "أخطاء (${uiState.logs.count { it.status == LogStatus.FAILURE }})",
                                    selected = uiState.selectedFilter == LogStatusFilter.FAILURE,
                                    selectedColor = RoseError,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFilter(LogStatusFilter.FAILURE)
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.filteredLogs.isEmpty()) {
                        item {
                            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد سجلات مطابقة للبحث أو الفلتر",
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.filteredLogs, key = { it.id }) { log ->
                            LogItemCard(
                                log = log,
                                displayName = uiState.contactDisplayNames[log.senderPhone]
                            )
                        }
                    }
                }

                LogsTab.SYSTEM_DIAGNOSTICS -> {
                    item {
                        GlassmorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            borderBrush = Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.4f), Color(0x10FFFFFF)))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "سجل أحداث المحرك والجلسة (Live Engine)",
                                        fontSize = 13.sp,
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${uiState.engineDiagnostics.size} حدث",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                                if (uiState.engineDiagnostics.isEmpty()) {
                                    Text(
                                        "لا توجد أحداث مسجلة من المحرك حتى الآن.",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                } else {
                                    uiState.engineDiagnostics.takeLast(40).reversed().forEach { line ->
                                        val isError = line.contains("ERROR") || line.contains("Error")
                                        val isSuccess = line.contains("Connected") || line.contains("SUCCESS")
                                        Text(
                                            text = line,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = when {
                                                isError -> RoseError
                                                isSuccess -> WhatsAppGreenLight
                                                else -> TextSecondary
                                            },
                                            lineHeight = 16.sp
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

