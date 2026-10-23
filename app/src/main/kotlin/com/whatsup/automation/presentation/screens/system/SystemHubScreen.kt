package com.whatsup.automation.presentation.screens.system

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.screens.logs.LogsScreen
import com.whatsup.automation.presentation.screens.logs.LogsViewModel
import com.whatsup.automation.presentation.screens.pairing.PairingScreen
import com.whatsup.automation.presentation.screens.pairing.PairingViewModel
import com.whatsup.automation.presentation.theme.*

enum class SystemTab(val title: String, val icon: ImageVector) {
    PAIRING("الربط", Icons.Filled.QrCode2),
    LOGS("السجلات", Icons.Filled.ListAlt)
}

@Composable
fun SystemHubScreen(
    pairingViewModel: PairingViewModel,
    logsViewModel: LogsViewModel,
    initialTab: SystemTab = SystemTab.PAIRING,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        // شريط الرأس القياسي المنظم مع محدد التبويب المدمج
        AppScreenHeader(
            title = if (selectedTab == SystemTab.PAIRING) "ربط الحساب والأجهزة" else "سجل العمليات والنشاط",
            subtitle = if (selectedTab == SystemTab.PAIRING) "إدارة جلسة واتساب وتوليد كود الاقتران" else "سجل الرسائل والأحداث اللحظية",
            onOpenDrawer = onOpenDrawer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            actions = {
                // شريط تبويب فرعي مدمج وأنيق
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, Color(0x3300E676), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SystemTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) WhatsAppGreen else Color.Transparent)
                                .clickable {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTab = tab
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        )

        // محتوى التبويب المختار
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "SystemHubContent",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                SystemTab.PAIRING -> PairingScreen(viewModel = pairingViewModel, showHeader = false)
                SystemTab.LOGS -> LogsScreen(viewModel = logsViewModel, showHeader = false)
            }
        }
    }
}
