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
            .widthIn(min = 260.dp, max = 340.dp)
            .fillMaxWidth(0.84f)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFFFFFFFF),
        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ترويسة القائمة الجانبية (Header)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFF0FDF4),
                                Color(0xFFECFDF5)
                            )
                        )
                    )
                    .border(1.dp, Color(0x3300A884), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(WhatsAppGreen, WhatsAppGreenLight)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "واتس آب أوتوميشن",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Sigma AutoFlow v2.0",
                                fontSize = 11.sp,
                                color = WhatsAppGreenDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // شارة حالة الاتصال الحية
                    val isConnected = connectionState == com.whatsup.automation.domain.model.ConnectionState.Connected
                    val isPairing = connectionState == com.whatsup.automation.domain.model.ConnectionState.AwaitingPairing
                    val badgeBg = if (isConnected) Color(0x1F00A884) else if (isPairing) Color(0x1FF59E0B) else Color(0x1FEF4444)
                    val badgeColor = if (isConnected) WhatsAppGreenDark else if (isPairing) Color(0xFFD97706) else Color(0xFFDC2626)
                    val statusText = if (isConnected) "المحرك متصل ومستقر 🟢" else if (isPairing) "بانتظار مسح الباركود 🟡" else "المحرك غير متصل 🔴"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = statusText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(badgeColor))
                    }

                    // شارة حالة الاشتراك والترخيص
                    val licenseStatus = sessionKeystore.getLicenseStatus()
                    val licenseExpiresAt = sessionKeystore.getLicenseExpiresAt()
                    val now = System.currentTimeMillis()
                    val remainingDays = if (licenseExpiresAt > now) {
                        ((licenseExpiresAt - now) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                    } else 0

                    if (licenseStatus == "ACTIVE") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0FDF4))
                                .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الاشتراك نشط (متبقي $remainingDays يوماً) 🛡️",
                                color = Color(0xFF15803D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. قسم المحاور الرئيسية
            DrawerSectionTitle("المحاور الأساسية للتطبيق")

            DrawerNavRow(
                title = "لوحة التحكم الرئيسية",
                subtitle = "نظرة عامة والخدمة والأداء",
                icon = Icons.Default.Dashboard,
                isSelected = currentRoute == Screen.Dashboard.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Dashboard.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "مركز المحادثات والتسجيل",
                subtitle = "فلترة الأرقام وحفظ جهات الاتصال",
                icon = Icons.Default.Forum,
                isSelected = currentRoute == Screen.ChatsHub.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.ChatsHub.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "مركز الأتمتة الشامل",
                subtitle = "القواعد والردود، الحالات، والمجموعات",
                icon = Icons.Default.Bolt,
                isSelected = currentRoute == Screen.Automation.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Automation.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "الخزينة السرية 🛡️",
                subtitle = "عرض مرة واحدة، الحالات، والوسائط المحمية",
                icon = Icons.Default.Security,
                badge = "محمي",
                badgeColor = WhatsAppGreen,
                isSelected = currentRoute == Screen.Vault.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Vault.route) { launchSingleTop = true }
                }
            )

            DrawerNavRow(
                title = "النظام وإدارة الجلسة ⚙️",
                subtitle = "ربط واتساب وسجل العمليات اللحظي",
                icon = Icons.Default.Tune,
                isSelected = currentRoute == Screen.System.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.System.route) { launchSingleTop = true }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = Color(0x1F00A884))

            // 2. قسم الخصوصية والأمان
            DrawerSectionTitle("أدوات الأمان والخصوصية")

            // مفتاح وضع التخفي الصامت في الخلفية
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0x2200A884), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = if (isStealthMode.value) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (isStealthMode.value) WhatsAppGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text("الوضع المتخفي الصامت 🔕", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("إخفاء الأيقونة والإشعار من شريط الحالة", color = TextSecondary, fontSize = 10.sp)
                    }
                }
                Switch(
                    checked = isStealthMode.value,
                    onCheckedChange = { checked ->
                        isStealthMode.value = checked
                        sessionKeystore.setStealthModeEnabled(checked)
                        WhatsAppForegroundService.updateStealthMode(context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WhatsAppGreen
                    )
                )
            }

            // إعدادات قفل التطبيق بالـ PIN
            DrawerNavRow(
                title = "قفل التطبيق (PIN والبصمة)",
                subtitle = if (isAppLocked.value) "مفعّل ومحمي 🔒" else "معطّل — اضغط للضبط",
                icon = Icons.Default.Lock,
                badge = if (isAppLocked.value) "محمي" else null,
                badgeColor = WhatsAppGreen,
                isSelected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onOpenLockSettings()
                }
            )

            // لوحة تشخيص المطور المحلية وفاحص الواجهات الذكي
            DrawerNavRow(
                title = "تشخيص المطور والواجهات 🛠️",
                subtitle = "فاحص الواجهات الذكي وسجلات الانهيار والـ ANR محلياً",
                icon = Icons.Default.BugReport,
                badge = "Dev",
                badgeColor = Color(0xFF6366F1),
                isSelected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    showDevDiagnostics = true
                }
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))

            // تذييل القائمة: زر فحص التحديثات الفوري
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF0FDF4))
                    .border(1.dp, Color(0x3300A884), RoundedCornerShape(14.dp))
                    .clickable {
                        scope.launch { drawerState.close() }
                        onCheckUpdates()
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                        Text("التحقق من وجود تحديثات 🚀", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = WhatsAppGreenDark, modifier = Modifier.size(18.dp))
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
        color = WhatsAppGreenDark,
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
            .background(Color(0x1F00A884), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x6600A884), RoundedCornerShape(12.dp))
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
                tint = if (isSelected) WhatsAppGreen else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) WhatsAppGreenDark else TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeColor)
            }
        }
    }
}
