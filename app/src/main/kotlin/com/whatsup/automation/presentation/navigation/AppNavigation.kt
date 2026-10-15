package com.whatsup.automation.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.whatsup.automation.presentation.theme.*

/**
 * وجهات التطبيق الرئيسية بتصميم 4 محاور موحدة وأنيقة.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "الرئيسية",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )

    data object Automation : Screen(
        route = "automation",
        title = "الأتمتة",
        selectedIcon = Icons.Filled.Bolt,
        unselectedIcon = Icons.Outlined.Bolt
    )

    data object ChatsHub : Screen(
        route = "chats_hub",
        title = "المحادثات",
        selectedIcon = Icons.Filled.Forum,
        unselectedIcon = Icons.Outlined.Forum
    )

    data object Settings : Screen(
        route = "settings",
        title = "الإعدادات",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object System : Screen(
        route = "system",
        title = "النظام والربط",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    )

    data object SplashAuth : Screen(
        route = "splash_auth",
        title = "الترحيب والدخول",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    )

    // المسارات الفرعية للتوافق والتوجيه المباشر
    data object Vault : Screen("vault", "المسترجعات", Icons.Filled.DeleteSweep, Icons.Outlined.DeleteSweep)
    data object Groups : Screen("groups", "المجموعات", Icons.Filled.Groups, Icons.Outlined.Groups)
    data object Rules : Screen("rules", "القواعد", Icons.Filled.Bolt, Icons.Outlined.Bolt)
    data object Statuses : Screen("statuses", "الحالات", Icons.Filled.AutoStories, Icons.Outlined.AutoStories)
    data object Analytics : Screen("analytics", "الإحصائيات", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    data object Pairing : Screen("pairing", "الربط", Icons.Filled.QrCode2, Icons.Outlined.QrCode2)
    data object Logs : Screen("logs", "السجلات", Icons.Filled.Tune, Icons.Outlined.Tune)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Statuses,
    Screen.Automation,
    Screen.Pairing,
    Screen.Settings
)

/**
 * شريط تنقل سفلي عائم فخم (Luxury Floating Capsule Bar).
 * يعتمد 6 محاور كبرى بمساحات لمس مدروسة مع حواف زمردية متوهجة وتأثيرات ارتدادية فاخرة.
 */
@Composable
fun ModernBottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = Color(0x1F00C853),
                    spotColor = Color(0x3300C853)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF20F1A22),
                            Color(0xF2060D12)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0x3300E676),
                            Color(0x8000E676),
                            Color(0x3300E676)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "tabScale"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) WhatsAppGreenLight else Color(0xFF64748B),
                    animationSpec = tween(220),
                    label = "iconTint"
                )
                val bgPillBrush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0x2600E676),
                            Color(0x1000E676)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                }

                val tooltipText = when (screen) {
                    Screen.Dashboard -> "الرئيسية: لوحة التحكم وحالة المحرك الحية"
                    Screen.Statuses -> "الحالات: مشاهدة الحالات والإعجاب التلقائي وحفظها"
                    Screen.Automation -> "الأتمتة: محرك الردود الذكية والقواعد والحملات"
                    Screen.Pairing -> "الربط: مسح الباركود QR وربط حساب الواتساب"
                    Screen.Settings -> "الإعدادات: الأمان والـ PIN والنسخ الاحتياطي"
                    else -> screen.title
                }

                com.whatsup.automation.presentation.components.AppTooltipBox(
                    tooltipText = tooltipText,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgPillBrush)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.2.dp, Color(0x6600E676), RoundedCornerShape(16.dp))
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )

                            Text(
                                text = screen.title,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) WhatsAppGreenLight else TextSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            // مؤشر كبسولي نشط متوهج
                            Box(
                                modifier = Modifier
                                    .size(width = if (isSelected) 16.dp else 0.dp, height = if (isSelected) 3.dp else 0.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isSelected) WhatsAppGreenLight else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}
