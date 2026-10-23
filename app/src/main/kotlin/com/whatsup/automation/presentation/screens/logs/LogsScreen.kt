package com.whatsup.automation.presentation.screens.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.LogStatus
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.components.AppTooltipBox
import com.whatsup.automation.presentation.components.BentoSkeletonList
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.screens.logs.components.*
import com.whatsup.automation.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    showHeader: Boolean = true,
    onOpenDrawer: (() -> Unit)? = null,
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
            onDismiss = { viewModel.closeConversation() }
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
            // شريط الرأس القياسي
            item {
                if (showHeader) {
                    AppScreenHeader(
                        title = "سجل العمليات والنشاط",
                        subtitle = "درع الحماية وسجلات العمليات والأحداث الحية",
                        onOpenDrawer = onOpenDrawer,
                        actions = {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val report = viewModel.generateDiagnosticReport()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("SIGMACODEX Diagnostic Report", report)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ التقرير التشخيصي بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ التقرير", tint = CyberCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                }
            }

            // شريط إحصائيات السجل ومسح العمليات
            item {
                LogsStatsBar(
                    totalLogsCount = uiState.logs.size,
                    onGenerateReport = { viewModel.generateDiagnosticReport() },
                    onClearAllLogs = { viewModel.clearAllLogs() }
                )
            }

            // بطاقة درع الحماية ومحاكي السلوك البشري (Anti-Ban Shield)
            item {
                AntiBanCard(
                    antiBan = uiState.antiBanSettings,
                    onUpdateAntiBan = { viewModel.updateAntiBanSettings(it) }
                )
            }

            // شريط التبويبات الثلاثية النظيفة بدون إيموجي
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
                    AppTooltipBox(
                        tooltipText = "سجل المحادثات المجمعة والرسائل المتبادلة (${uiState.conversations.size})",
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isConv) WhatsAppGreen else Color.Transparent)
                                .then(if (isConv) Modifier.border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp)) else Modifier)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectTab(LogsTab.CONVERSATIONS)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "المحادثات (${uiState.conversations.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isConv) Color.White else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 2. تبويب سجل العمليات
                    val isRaw = uiState.selectedTab == LogsTab.RAW_LOGS
                    AppTooltipBox(
                        tooltipText = "سجل النشاط التفصيلي لكافة العمليات (${uiState.logs.size})",
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isRaw) WhatsAppGreen else Color.Transparent)
                                .then(if (isRaw) Modifier.border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp)) else Modifier)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectTab(LogsTab.RAW_LOGS)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "سجل النشاط (${uiState.logs.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isRaw) Color.White else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 3. تبويب تشخيص المحرك
                    val isDiag = uiState.selectedTab == LogsTab.SYSTEM_DIAGNOSTICS
                    AppTooltipBox(
                        tooltipText = "شاشة تشخيص المحرك ومؤشرات الذاكرة والشبكة",
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDiag) CyberCyan else Color.Transparent)
                                .then(if (isDiag) Modifier.border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp)) else Modifier)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectTab(LogsTab.SYSTEM_DIAGNOSTICS)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "التشخيص",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isDiag) Color.White else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                    if (uiState.isRefreshing && uiState.filteredConversations.isEmpty()) {
                        item {
                            BentoSkeletonList(itemCount = 4, itemHeight = 82.dp)
                        }
                    } else if (uiState.filteredConversations.isEmpty()) {
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
                                            imageVector = Icons.Filled.Forum,
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

                    if (uiState.isRefreshing && uiState.filteredLogs.isEmpty()) {
                        item {
                            BentoSkeletonList(itemCount = 5, itemHeight = 72.dp)
                        }
                    } else if (uiState.filteredLogs.isEmpty()) {
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
                        LogsDiagnosticsCard(diagnostics = uiState.engineDiagnostics)
                    }
                }
            }
        }
    }
}
