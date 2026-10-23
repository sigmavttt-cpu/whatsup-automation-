package com.whatsup.automation.presentation.screens.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.whatsup.automation.domain.model.PatternType
import com.whatsup.automation.domain.model.Rule
import com.whatsup.automation.presentation.components.*
import com.whatsup.automation.presentation.screens.rules.components.ContactRegistrationCard
import com.whatsup.automation.presentation.screens.rules.components.RuleCard
import com.whatsup.automation.presentation.screens.rules.components.RuleFormDialog
import com.whatsup.automation.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    viewModel: RulesViewModel,
    showHeader: Boolean = true,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<Rule?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Rule?>(null) }
    val haptic = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()

    // تأكيد الحذف
    showDeleteConfirmDialog?.let { targetRule ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("حذف القاعدة", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("هل أنت متأكد من رغبتك في حذف قاعدة \"${targetRule.name}\"؟", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteRule(targetRule.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("حذف نهائي", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = DarkBgSecondary
        )
    }

    // نافذة محاكاة القواعد
    if (uiState.showSimulationDialog) {
        RuleSimulationDialog(
            simulationResult = uiState.simulationResult,
            isSimulating = uiState.isSimulating,
            onDismiss = { viewModel.setSimulationDialogVisible(false) },
            onRunSimulation = { message, phone -> viewModel.runSimulation(message, phone) }
        )
    }

    // نافذة إضافة أو تعديل قاعدة
    if (showAddDialog || ruleToEdit != null) {
        RuleFormDialog(
            initialRule = ruleToEdit,
            onDismiss = {
                showAddDialog = false
                ruleToEdit = null
            },
            onSave = { name, patternVal, reply, priority ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val target = ruleToEdit
                if (target != null) {
                    viewModel.editRule(target.id, name, "", PatternType.CONTAINS, patternVal, reply, false, priority)
                } else {
                    viewModel.addRule(name, "", PatternType.CONTAINS, patternVal, reply, false, priority)
                }
                showAddDialog = false
                ruleToEdit = null
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBgPrimary,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    ruleToEdit = null
                    showAddDialog = true
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
                    Icon(Icons.Filled.Add, contentDescription = "إضافة قاعدة")
                    Text("إضافة رد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.refreshRules()
            },
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
                // 1. شريط الرأس الرئيسي
                item {
                    if (showHeader) {
                        AppScreenHeader(
                            title = "محرك القواعد والردود",
                            subtitle = "الردود التلقائية وقواعد حفظ جهات الاتصال المخصصة",
                            onOpenDrawer = onOpenDrawer,
                            actions = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(WhatsAppGreen.copy(alpha = 0.15f))
                                        .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${uiState.rules.count { it.isEnabled }} نشطة",
                                        color = WhatsAppGreenLight,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setSimulationDialogVisible(true)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyberCyan.copy(alpha = 0.2f))
                                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                ) {
                                    Icon(
                                        Icons.Filled.Science,
                                        contentDescription = "محاكي القواعد",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = WhatsAppGreen.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "${uiState.rules.count { it.isEnabled }} قواعد نشطة",
                                    color = WhatsAppGreenLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setSimulationDialogVisible(true)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberCyan.copy(alpha = 0.2f))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    Icons.Filled.Science,
                                    contentDescription = "محاكي القواعد",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 2. القاعدة الثابتة الأولى: تسجيل وحفظ جهات الاتصال في دفتر الهاتف
                item {
                    ContactRegistrationCard(
                        settings = uiState.formattingSettings,
                        onUpdateSettings = { viewModel.updateFormattingSettings(it) }
                    )
                }

                // فاصل وعنوان قسم الردود التلقائية
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الردود التلقائية المخصصة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "قواعد الرد على الكلمات والرسائل الخاصة",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 3. قائمة القواعد المسجلة
                if (uiState.isLoading) {
                    item {
                        BentoSkeletonList(itemCount = 3, itemHeight = 110.dp)
                    }
                } else if (uiState.rules.isEmpty()) {
                    item {
                        AppEmptyState(
                            icon = Icons.AutoMirrored.Filled.List,
                            title = "لا توجد قواعد مضافة حالياً",
                            subtitle = "اضغط على زر (إضافة رد) لإنشاء ردود تلقائية مخصصة للكلمات والعبارات",
                            actionLabel = "إضافة قاعدة جديدة",
                            onActionClick = {
                                ruleToEdit = null
                                showAddDialog = true
                            }
                        )
                    }
                } else {
                    items(uiState.rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onToggle = { isEnabled ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleRule(rule.id, isEnabled)
                            },
                            onEdit = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                ruleToEdit = rule
                            },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteConfirmDialog = rule
                            }
                        )
                    }
                }
            }
        }
    }
}
