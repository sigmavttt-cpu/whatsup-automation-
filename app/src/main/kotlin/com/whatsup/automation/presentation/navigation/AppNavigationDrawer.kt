package com.whatsup.automation.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.whatsup.automation.data.engine.WhatsAppEngine
import com.whatsup.automation.data.security.AppLockManager
import com.whatsup.automation.data.security.SessionKeystore
import com.whatsup.automation.presentation.components.DevDiagnosticsSheet
import com.whatsup.automation.presentation.theme.*
import com.whatsup.automation.service.WhatsAppForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * عنصر قائمة درج التنقل الجانبي.
 */
data class DrawerMenuItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val route: String? = null,
    val badge: String? = null,
    val isAction: Boolean = false,
    val onClick: (() -> Unit)? = null
)

/**
 * القائمة الجانبية الفاخرة (Dark Glassmorphism Navigation Drawer).
 * تعيد هيكلة كافة أقسام التطبيق وتوفر وصولاً سريعاً للخزينة والأمان والنظام.
 */
@Composable
fun AppNavigationDrawer(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    whatsAppEngine: WhatsAppEngine,
    sessionKeystore: SessionKeystore,
    appLockManager: AppLockManager,
    onOpenLockSettings: () -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val connectionState by whatsAppEngine.connectionState.collectAsState()
    val isStealthMode = remember { mutableStateOf(sessionKeystore.isStealthModeEnabled()) }
    val isAppLocked = remember { mutableStateOf(appLockManager.isLockEnabled()) }
    var showDevDiagnostics by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth(0.85f)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF060D12),
        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 20.dp, bottomEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ترويسة القائمة الجانبية (Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(WhatsAppGreen, WhatsAppGreenLight))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "القائمة الرئيسية",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = Color(0x3300E676)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 1. المحاور الأساسية
            DrawerSectionTitle("المحاور الأساسية")

            DrawerNavRow(
                title = "لوحة التحكم",
                icon = Icons.Default.Dashboard,
                isSelected = currentRoute == Screen.Dashboard.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Dashboard.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "مركز الأتمتة",
                icon = Icons.Default.Bolt,
                isSelected = currentRoute == Screen.Automation.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Automation.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "الحالات والتفاعل",
                icon = Icons.Default.AutoStories,
                isSelected = currentRoute == Screen.Statuses.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Statuses.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "ربط الحساب",
                icon = Icons.Default.QrCode2,
                isSelected = currentRoute == Screen.Pairing.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Pairing.route) { launchSingleTop = true }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = Color(0x2600E676))

            // 2. المحادثات والمحذوفات
            DrawerSectionTitle("المحادثات والأرشيف")

            DrawerNavRow(
                title = "المحادثات والتسجيل",
                icon = Icons.Default.Forum,
                isSelected = currentRoute == Screen.ChatsHub.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.ChatsHub.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "الرسائل المحذوفة",
                icon = Icons.Default.DeleteSweep,
                badge = "المحذوفات",
                badgeColor = WhatsAppGreenLight,
                isSelected = currentRoute == Screen.Vault.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Vault.route) { launchSingleTop = true }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = Color(0x2600E676))

            // 3. النظام والأمان
            DrawerSectionTitle("النظام والأمان")

            DrawerNavRow(
                title = "قفل التطبيق بالـ PIN",
                icon = Icons.Default.Lock,
                badge = if (isAppLocked.value) "مفعّل" else "معطّل",
                badgeColor = if (isAppLocked.value) WhatsAppGreenLight else AmberWarning,
                isSelected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onOpenLockSettings()
                }
            )

            DrawerNavRow(
                title = "سجل النظام والتشغيل",
                icon = Icons.Default.Tune,
                isSelected = currentRoute == Screen.System.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.System.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "مركز الإشعارات",
                icon = Icons.Default.Notifications,
                isSelected = currentRoute == Screen.Notifications.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "الإعدادات العامة",
                icon = Icons.Default.Settings,
                isSelected = currentRoute == Screen.Settings.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "تشخيص المطور محلياً",
                icon = Icons.Default.BugReport,
                badge = "Dev",
                badgeColor = Color(0xFF6366F1),
                isSelected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    showDevDiagnostics = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // زر فحص التحديثات الفوري
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0F1A22))
                    .border(1.dp, Color(0x3300E676), RoundedCornerShape(12.dp))
                    .clickable {
                        scope.launch { drawerState.close() }
                        onCheckUpdates()
                    }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = WhatsAppGreenLight, modifier = Modifier.size(18.dp))
                        Text("التحقق من التحديثات", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = WhatsAppGreenLight, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDevDiagnostics) {
        DevDiagnosticsSheet(onDismiss = { showDevDiagnostics = false })
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = WhatsAppGreenLight,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerNavRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    badge: String? = null,
    badgeColor: Color = WhatsAppGreen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgModifier = if (isSelected) {
        Modifier
            .background(Color(0x2600E676), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x6600E676), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) WhatsAppGreenLight else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) WhatsAppGreenLight else Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeColor)
            }
        }
    }
}
