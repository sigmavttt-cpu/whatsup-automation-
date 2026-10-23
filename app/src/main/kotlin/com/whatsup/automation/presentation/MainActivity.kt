package com.whatsup.automation.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.whatsup.automation.data.security.SessionKeystore
import com.whatsup.automation.presentation.navigation.ModernBottomNavBar
import com.whatsup.automation.presentation.navigation.Screen
import com.whatsup.automation.presentation.screens.dashboard.DashboardScreen
import com.whatsup.automation.presentation.screens.dashboard.DashboardViewModel
import com.whatsup.automation.presentation.screens.logs.LogsScreen
import com.whatsup.automation.presentation.screens.logs.LogsViewModel
import com.whatsup.automation.presentation.screens.pairing.PairingScreen
import com.whatsup.automation.presentation.screens.pairing.PairingViewModel
import com.whatsup.automation.presentation.screens.rules.RulesScreen
import com.whatsup.automation.presentation.screens.rules.RulesViewModel
import com.whatsup.automation.presentation.screens.groups.GroupsScreen
import com.whatsup.automation.presentation.screens.groups.GroupsViewModel
import com.whatsup.automation.presentation.screens.chatshub.ChatsHubScreen
import com.whatsup.automation.presentation.screens.chatshub.ChatsHubViewModel
import com.whatsup.automation.presentation.screens.statuses.StatusesScreen
import com.whatsup.automation.presentation.screens.statuses.StatusesViewModel
import com.whatsup.automation.presentation.screens.automation.AutomationHubScreen
import com.whatsup.automation.presentation.screens.automation.AutomationTab
import com.whatsup.automation.presentation.screens.system.SystemHubScreen
import com.whatsup.automation.presentation.screens.system.SystemTab
import com.whatsup.automation.presentation.theme.DarkBgPrimary
import com.whatsup.automation.presentation.theme.WhatsUpTheme
import com.whatsup.automation.data.engine.WhatsAppEngine
import com.whatsup.automation.service.ServicePermissionManager
import com.whatsup.automation.service.WhatsAppForegroundService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.whatsup.automation.data.local.contacts.ContactsSyncManager
import com.whatsup.automation.data.security.AppLockManager
import com.whatsup.automation.presentation.components.AppLockOverlay
import com.whatsup.automation.presentation.components.AppLockSettingsDialog
import com.whatsup.automation.presentation.navigation.AppNavigationDrawer
import com.whatsup.automation.presentation.screens.vault.VaultScreen
import com.whatsup.automation.presentation.screens.vault.VaultViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * النشاط الرئيسي للتطبيق — مهيأ بـ Hilt و Jetpack Compose مع دعم كامل للاتجاه من اليمين لليسار (RTL).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionKeystore: SessionKeystore
    @Inject
    lateinit var whatsAppEngine: WhatsAppEngine

    @Inject
    lateinit var contactsSyncManager: ContactsSyncManager

    @Inject
    lateinit var appLockManager: AppLockManager

    override fun onStop() {
        super.onStop()
        appLockManager.lock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // تشغيل الخدمة الأمامية وتحديث حالة الاتصال الحقيقية
        WhatsAppForegroundService.start(this)
        whatsAppEngine.refreshRealConnectionState()
        
        lifecycleScope.launch {
            contactsSyncManager.syncContacts()
        }

        setContent {
            WhatsUpTheme {
                // فرض الاتجاه من اليمين إلى اليسار (RTL) للغة العربية
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val context = LocalContext.current

                    // إعداد طلب الأذونات الحيوية (جهات الاتصال والإشعارات) تلقائياً عند التشغيل
                    val permissionsToRequest = remember {
                        buildList {
                            add(Manifest.permission.READ_CONTACTS)
                            add(Manifest.permission.WRITE_CONTACTS)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }.toTypedArray()
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { _ -> }

                    val updateViewModel: com.whatsup.automation.presentation.screens.update.UpdateViewModel = hiltViewModel()
                    val activationViewModel: com.whatsup.automation.presentation.screens.activation.ActivationViewModel = hiltViewModel()
                    val licenseState by activationViewModel.licenseState.collectAsState()

                    LaunchedEffect(Unit) {
                        updateViewModel.checkForUpdates(isManual = false)
                        activationViewModel.refreshStatus()
                    }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isSplashAuth = currentRoute == Screen.SplashAuth.route

                    LaunchedEffect(currentRoute) {
                        if (currentRoute == Screen.Dashboard.route) {
                            val hasAll = permissionsToRequest.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (!hasAll) {
                                permissionLauncher.launch(permissionsToRequest)
                            }

                            // فحص وطلب استثناء تحسين استهلاك البطارية لضمان الحصانة الدائمة في الخلفية
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                                if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == false) {
                                    try {
                                        val batteryIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(batteryIntent)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }

                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var showLockSettingsDialog by remember { mutableStateOf(false) }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = true,
                        drawerContent = {
                            AppNavigationDrawer(
                                navController = navController,
                                drawerState = drawerState,
                                scope = scope,
                                whatsAppEngine = whatsAppEngine,
                                sessionKeystore = sessionKeystore,
                                appLockManager = appLockManager,
                                onOpenLockSettings = { showLockSettingsDialog = true },
                                onCheckUpdates = { updateViewModel.checkForUpdates(isManual = true) }
                            )
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = DarkBgPrimary,
                                bottomBar = {
                                    if (!isSplashAuth) {
                                        ModernBottomNavBar(navController = navController)
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (isSplashAuth) Modifier else Modifier.padding(innerPadding))
                                ) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = Screen.SplashAuth.route,
                                        enterTransition = { fadeIn(animationSpec = tween(220)) },
                                        exitTransition = { fadeOut(animationSpec = tween(180)) },
                                        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                                        popExitTransition = { fadeOut(animationSpec = tween(180)) }
                                    ) {
                                        composable(Screen.SplashAuth.route) {
                                            com.whatsup.automation.presentation.screens.auth.SplashAuthScreen(
                                                onLoginSuccess = {
                                                    navController.navigate(Screen.Dashboard.route) {
                                                        popUpTo(Screen.SplashAuth.route) { inclusive = true }
                                                    }
                                                },
                                                onLinkNewAccount = {
                                                    navController.navigate(Screen.Pairing.route) {
                                                        popUpTo(Screen.SplashAuth.route) { inclusive = true }
                                                    }
                                                },
                                                activationViewModel = activationViewModel
                                            )
                                        }

                                        composable(Screen.Settings.route) {
                                            com.whatsup.automation.presentation.screens.settings.SettingsScreen(
                                                onNavigateToPairing = { navController.navigate(Screen.Pairing.route) },
                                                onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                                onOpenLockSettings = { showLockSettingsDialog = true },
                                                onCheckUpdates = { updateViewModel.checkForUpdates(isManual = true) },
                                                onNavigateToSplashAuth = { navController.navigate(Screen.SplashAuth.route) }
                                            )
                                        }

                                        composable(Screen.Dashboard.route) {
                                            val viewModel: DashboardViewModel = hiltViewModel()
                                            DashboardScreen(
                                                viewModel = viewModel,
                                                onNavigateToPairing = { navController.navigate(Screen.System.route) },
                                                onNavigateToRules = { navController.navigate(Screen.Automation.route) },
                                                onNavigateToLogs = { navController.navigate(Screen.System.route) },
                                                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                                                onOpenDrawer = { scope.launch { drawerState.open() } }
                                            )
                                        }

                                        composable(Screen.Vault.route) {
                                            val vaultViewModel: VaultViewModel = hiltViewModel()
                                            VaultScreen(viewModel = vaultViewModel)
                                        }

                                        composable(Screen.Automation.route) {
                                            val rulesViewModel: RulesViewModel = hiltViewModel()
                                            val groupsViewModel: GroupsViewModel = hiltViewModel()
                                            AutomationHubScreen(
                                                rulesViewModel = rulesViewModel,
                                                groupsViewModel = groupsViewModel
                                            )
                                        }

                                        composable(Screen.ChatsHub.route) {
                                            val viewModel: ChatsHubViewModel = hiltViewModel()
                                            ChatsHubScreen(viewModel = viewModel)
                                        }

                                        composable(Screen.System.route) {
                                            val pairingViewModel: PairingViewModel = hiltViewModel()
                                            val logsViewModel: LogsViewModel = hiltViewModel()
                                            SystemHubScreen(
                                                pairingViewModel = pairingViewModel,
                                                logsViewModel = logsViewModel
                                            )
                                        }

                                        // المسارات الفرعية للتوجيه المباشر
                                        composable(Screen.Rules.route) {
                                            val rulesViewModel: RulesViewModel = hiltViewModel()
                                            val groupsViewModel: GroupsViewModel = hiltViewModel()
                                            AutomationHubScreen(
                                                rulesViewModel = rulesViewModel,
                                                groupsViewModel = groupsViewModel,
                                                initialTab = AutomationTab.RULES
                                            )
                                        }

                                        composable(Screen.Statuses.route) {
                                            val statusesViewModel: StatusesViewModel = hiltViewModel()
                                            StatusesScreen(viewModel = statusesViewModel)
                                        }

                                        composable(Screen.Groups.route) {
                                            val rulesViewModel: RulesViewModel = hiltViewModel()
                                            val groupsViewModel: GroupsViewModel = hiltViewModel()
                                            AutomationHubScreen(
                                                rulesViewModel = rulesViewModel,
                                                groupsViewModel = groupsViewModel,
                                                initialTab = AutomationTab.GROUPS
                                            )
                                        }

                                        composable(Screen.Pairing.route) {
                                            val pairingViewModel: PairingViewModel = hiltViewModel()
                                            val logsViewModel: LogsViewModel = hiltViewModel()
                                            SystemHubScreen(
                                                pairingViewModel = pairingViewModel,
                                                logsViewModel = logsViewModel,
                                                initialTab = SystemTab.PAIRING
                                            )
                                        }

                                        composable(Screen.Logs.route) {
                                            val pairingViewModel: PairingViewModel = hiltViewModel()
                                            val logsViewModel: LogsViewModel = hiltViewModel()
                                            SystemHubScreen(
                                                pairingViewModel = pairingViewModel,
                                                logsViewModel = logsViewModel,
                                                initialTab = SystemTab.LOGS
                                            )
                                        }

                                        composable(Screen.Notifications.route) {
                                            val notificationsViewModel: com.whatsup.automation.presentation.screens.notifications.NotificationsViewModel = hiltViewModel()
                                            com.whatsup.automation.presentation.screens.notifications.NotificationsScreen(
                                                viewModel = notificationsViewModel,
                                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                                onNavigateBack = { navController.popBackStack() }
                                            )
                                        }
                                    }

                                    // زر القائمة الجانبية في شريط الرأس العلوي (Top Header Drawer Trigger) خارج شاشة الدخول والشاشة الرئيسية
                                    if (!isSplashAuth && currentRoute != Screen.Dashboard.route) {
                                        com.whatsup.automation.presentation.components.AppTooltipBox(
                                            tooltipText = "فتح القائمة الجانبية للتطبيق",
                                            modifier = Modifier
                                                .statusBarsPadding()
                                                .padding(top = 10.dp, start = 16.dp)
                                                .align(Alignment.TopStart)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Color(0xD90F1A22))
                                                    .border(1.2.dp, Color(0x6600E676), CircleShape)
                                                    .size(42.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { scope.launch { drawerState.open() } },
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Menu,
                                                        contentDescription = "القائمة الجانبية",
                                                        tint = com.whatsup.automation.presentation.theme.WhatsAppGreenLight,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // درع ونافذة التحديثات الشاملة (Mandatory/Optional Update Engine Shield)
                    com.whatsup.automation.presentation.screens.update.FullScreenUpdateShield(
                        viewModel = updateViewModel
                    )

                    // درع قفل التطبيق بالـ PIN والبصمة
                    AppLockOverlay(appLockManager = appLockManager)

                    // درع تفعيل الاشتراك والترخيص المقاوم للكراك (خارج شاشة البداية والدخول)
                    val isSplashAuthActive = currentRoute == Screen.SplashAuth.route
                    if (!isSplashAuthActive && (
                        licenseState is com.whatsup.automation.data.security.LicenseState.NotActivated ||
                        licenseState is com.whatsup.automation.data.security.LicenseState.Expired ||
                        licenseState is com.whatsup.automation.data.security.LicenseState.Blocked
                    )) {
                        com.whatsup.automation.presentation.screens.activation.ActivationScreen(
                            viewModel = activationViewModel
                        )
                    }

                    // نافذة ضبط إعدادات القفل
                    if (showLockSettingsDialog) {
                        AppLockSettingsDialog(
                            appLockManager = appLockManager,
                            onDismiss = { showLockSettingsDialog = false }
                        )
                    }
                }
            }
        }
    }
}
