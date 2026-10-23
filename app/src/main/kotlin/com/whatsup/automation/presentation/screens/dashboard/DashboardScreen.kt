package com.whatsup.automation.presentation.screens.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.ConnectionState
import com.whatsup.automation.presentation.components.*
import com.whatsup.automation.presentation.screens.dashboard.components.*
import com.whatsup.automation.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPairing: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToNotifications: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val healthState by viewModel.healthState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshDashboard()
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refreshDashboard()
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // شريط الرأس الموحد
            item {
                AppScreenHeader(
                    title = "Sigma AutoFlow",
                    subtitle = "محرك أتمتة واتساب المحلي الفائق",
                    onOpenDrawer = onOpenDrawer,
                    actions = {
                        NotificationBellButton(
                            unreadCount = unreadCount,
                            onClick = {
                                if (onNavigateToNotifications != null) {
                                    onNavigateToNotifications()
                                } else {
                                    showNotificationsSheet = true
                                    viewModel.markNotificationsAsRead()
                                }
                            }
                        )
                        ConnectionStatusBadge(connectionState = uiState.connectionState)
                    }
                )
            }

            // تنبيه فوري عند فقدان إذن الكتابة في جهات الاتصال
            if (!uiState.isWritePermissionGranted) {
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderBrush = Brush.linearGradient(listOf(RoseError.copy(alpha = 0.6f), AmberWarning.copy(alpha = 0.4f))),
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.WRITE_CONTACTS,
                                    Manifest.permission.READ_CONTACTS
                                )
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(RoseError.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = RoseError,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "إذن جهات الاتصال مفقود",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseError
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "مطلوب صلاحية WRITE_CONTACTS لتمكين حفظ الأسماء في دفتر الهاتف.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.WRITE_CONTACTS,
                                            Manifest.permission.READ_CONTACTS
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "منح الإذن",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // بطاقة التحكم الرئيسية بالمفاعل (Reactor Hero Bento)
            item {
                ReactorHeroCard(
                    isConnected = uiState.connectionState is ConnectionState.Connected,
                    isPaused = uiState.isAutomationPaused,
                    isAssistantActive = uiState.isAssistantActive,
                    isServiceRunning = healthState.isForegroundServiceRunning,
                    onToggleService = { viewModel.toggleService() },
                    onFullStopService = { viewModel.stopAllServices() },
                    onStartService = { viewModel.startAllServices() }
                )
            }

            // بطاقة حارس الخدمة ومراقب الأداء الحي
            item {
                ServiceHealthCard(snapshot = healthState)
            }

            // شبكة الإحصائيات الذكية
            item {
                Text(
                    text = "الإحصائيات المباشرة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            item {
                DashboardStatsGrid(stats = uiState.stats)
            }

            // قسم سجل الأحداث المباشر
            item {
                DashboardRecentLogsHeader(
                    isPreviewHidden = uiState.isPreviewHidden,
                    onTogglePrivacy = { viewModel.togglePreviewPrivacy() },
                    onViewAll = onNavigateToLogs
                )
            }

            if (uiState.recentLogs.isEmpty()) {
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderBrush = Brush.linearGradient(listOf(Color(0x2200A884), Color(0x1100A884)))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sensors,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "بانتظار أول رسالة أو حدث وارد...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "اسحب للأسفل للتحديث الفوري",
                                fontSize = 11.sp,
                                color = WhatsAppGreen
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.recentLogs,
                    key = { it.id }
                ) { log ->
                    DashboardRecentLogItem(
                        log = log,
                        isPreviewHidden = uiState.isPreviewHidden
                    )
                }
            }

            // سجل المحاكاة المباشر (Terminal Console)
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderBrush = ObsidianGlowBorder.Cyan
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleTerminalExpanded() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "تشخيص النظام وسجل الطرفية (Terminal)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = if (uiState.isTerminalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = uiState.isTerminalExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                TerminalConsole(
                                    lines = uiState.terminalLines,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    NotificationCenterSheet(
        isOpen = showNotificationsSheet,
        notifications = notifications,
        onDismiss = { showNotificationsSheet = false },
        onClearAll = { viewModel.clearNotifications() }
    )
}
