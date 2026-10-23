package com.whatsup.automation.presentation.screens.automation

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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.screens.groups.GroupsScreen
import com.whatsup.automation.presentation.screens.groups.GroupsViewModel
import com.whatsup.automation.presentation.screens.rules.RulesScreen
import com.whatsup.automation.presentation.screens.rules.RulesViewModel
import com.whatsup.automation.presentation.theme.*

enum class AutomationTab(val title: String, val icon: ImageVector) {
    RULES("قواعد الرد", Icons.Filled.Bolt),
    GROUPS("مجموعات البث", Icons.Filled.Groups)
}

@Composable
fun AutomationHubScreen(
    rulesViewModel: RulesViewModel,
    groupsViewModel: GroupsViewModel,
    initialTab: AutomationTab = AutomationTab.RULES,
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
            title = if (selectedTab == AutomationTab.RULES) "محرك القواعد والردود" else "مجموعات البث والنشر",
            subtitle = if (selectedTab == AutomationTab.RULES) "الردود التلقائية وحفظ جهات الاتصال الذكي" else "إدارة مجموعات البث والإرسال الجماعي",
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
                    AutomationTab.values().forEach { tab ->
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
                                text = if (tab == AutomationTab.RULES) "القواعد" else "المجموعات",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        )

        // محتوى التبويب المختار مع أنيميشن انتقال ناعم
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "AutomationHubContent",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                AutomationTab.RULES -> RulesScreen(viewModel = rulesViewModel, showHeader = false)
                AutomationTab.GROUPS -> GroupsScreen(viewModel = groupsViewModel)
            }
        }
    }
}
