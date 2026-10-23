package com.whatsup.automation.presentation.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.screens.settings.components.*
import com.whatsup.automation.presentation.theme.*
import kotlinx.coroutines.launch

/**
 * شاشة الإعدادات العامة والأمان — بتصميم شبكة بينتو السيبرانية (Cyber Bento Grid)
 * مبنية بالكامل وفق هوية Cyber Jade Dark Glassmorphism مع كروت تفاعلية ومفاتيح سريعة مباشرة.
 */
@Composable
fun SettingsScreen(
    onNavigateToPairing: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onOpenLockSettings: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onNavigateToSplashAuth: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // التخزين المحلي السريع للإعدادات التفاعلية المباشرة
    val prefs = remember { context.getSharedPreferences("whatsup_settings_prefs", Context.MODE_PRIVATE) }
    var isStealthActive by remember { mutableStateOf(prefs.getBoolean("stealth_mode", true)) }
    var isHapticEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_enabled", true)) }
    var isNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var isAntiBanEnabled by remember { mutableStateOf(prefs.getBoolean("antiban_enabled", true)) }

    // حالات النوافذ الحوارية
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("العربية (الافتراضية)") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // شريط العنوان العلوي الموحد
            AppScreenHeader(
                title = "الإعدادات والتحكم",
                subtitle = "إدارة المحرك، دروع الحماية، والتخصيص المتقدم",
                onOpenDrawer = onOpenDrawer,
                modifier = Modifier.padding(bottom = 4.dp, top = 2.dp)
            )

            // =========================================================================
            // 1. بطاقة المحرك والحساب الكبرى (Hero Engine & Profile Card)
            // =========================================================================
            SettingsEngineCard(onNavigateToPairing = onNavigateToPairing)

            // =========================================================================
            // 2. بطاقة الأمان والخزينة السرية (Security & Vault Bento)
            // =========================================================================
            BentoSectionContainer(
                title = "الأمان والخزينة السرية",
                icon = Icons.Filled.Shield,
                accentColor = CyberCyan,
                borderBrush = ObsidianGlowBorder.Cyan
            ) {
                BentoActionRow(
                    title = "قفل التطبيق البيومتري ورمز PIN",
                    subtitle = "تأمين الدخول بالبصمة ورمز سري مخصص",
                    icon = Icons.Outlined.Lock,
                    iconTint = CyberCyan,
                    trailingText = "ضبط الحماية",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenLockSettings()
                    }
                )

                BentoDivider()

                BentoActionRow(
                    title = "الخزينة السرية المشفرة",
                    subtitle = "استعراض الرسائل المحذوفة ووسائط العرض لمرة واحدة",
                    icon = Icons.Outlined.FolderSpecial,
                    iconTint = CyberCyan,
                    trailingText = "فتح الخزينة",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToVault()
                    }
                )

                BentoDivider()

                BentoSwitchRow(
                    title = "وضع التخفي والشبح (Ghost Mode)",
                    subtitle = "مشاهدة الحالات والرسائل دون إرسال إشعارات قراءة",
                    icon = Icons.Outlined.VisibilityOff,
                    iconTint = CyberCyan,
                    checked = isStealthActive,
                    onCheckedChange = { checked ->
                        isStealthActive = checked
                        prefs.edit().putBoolean("stealth_mode", checked).apply()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (checked) "تم تفعيل وضع التخفي بنجاح" else "تم إيقاف وضع التخفي"
                            )
                        }
                    }
                )
            }

            // =========================================================================
            // 3. بطاقة النواة والأتمتة الذكية (Automation Core Bento)
            // =========================================================================
            BentoSectionContainer(
                title = "النواة والأتمتة الذكية",
                icon = Icons.Filled.Bolt,
                accentColor = WhatsAppGreen,
                borderBrush = ObsidianGlowBorder.Emerald
            ) {
                BentoSwitchRow(
                    title = "درع مكافحة الحظر والسلوك البشري",
                    subtitle = "فواصل زمنية عشوائية ومحاكاة الكتابة الحية",
                    icon = Icons.Outlined.Security,
                    iconTint = WhatsAppGreen,
                    checked = isAntiBanEnabled,
                    onCheckedChange = { checked ->
                        isAntiBanEnabled = checked
                        prefs.edit().putBoolean("antiban_enabled", checked).apply()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (checked) "درع مكافحة الحظر نشط لحماية حسابك" else "تم تعطيل محاكي السلوك البشري"
                            )
                        }
                    }
                )

                BentoDivider()

                BentoSwitchRow(
                    title = "التنبيهات والإشعارات اللحظية",
                    subtitle = "إشعار عند تشغيل القواعد والتقاط الرسائل المحذوفة",
                    icon = Icons.Outlined.Notifications,
                    iconTint = WhatsAppGreen,
                    checked = isNotificationsEnabled,
                    onCheckedChange = { checked ->
                        isNotificationsEnabled = checked
                        prefs.edit().putBoolean("notifications_enabled", checked).apply()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }

            // =========================================================================
            // 4. بطاقة التخصيص والمظهر (UX & Preferences Bento)
            // =========================================================================
            BentoSectionContainer(
                title = "التخصيص وتجربة الاستخدام",
                icon = Icons.Filled.Palette,
                accentColor = Color(0xFFA855F7),
                borderBrush = ObsidianGlowBorder.None
            ) {
                BentoActionRow(
                    title = "لغة التطبيق",
                    subtitle = selectedLanguage,
                    icon = Icons.Outlined.Language,
                    iconTint = Color(0xFFA855F7),
                    trailingText = "تغيير",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showLanguageDialog = true
                    }
                )

                BentoDivider()

                BentoSwitchRow(
                    title = "الاهتزاز اللمسي التفاعلي (Haptic Feedback)",
                    subtitle = "اهتزاز دقيق للأزرار والتمرير وتأكيد الأوامر",
                    icon = Icons.Outlined.Vibration,
                    iconTint = Color(0xFFA855F7),
                    checked = isHapticEnabled,
                    onCheckedChange = { checked ->
                        isHapticEnabled = checked
                        prefs.edit().putBoolean("haptic_enabled", checked).apply()
                        if (checked) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }

            // =========================================================================
            // 5. بطاقة سلامة البيانات والتخزين المحلي (Storage & Database Bento)
            // =========================================================================
            BentoSectionContainer(
                title = "قاعدة البيانات والتخزين المحلي",
                icon = Icons.Outlined.Storage,
                accentColor = WhatsAppGreenLight,
                borderBrush = ObsidianGlowBorder.Emerald
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = WhatsAppGreenLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "قاعدة بيانات Room المحلية",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "تخزين مشفر ومحمي محلياً داخل سياق التطبيق",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(
                        text = "محلي 100%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenLight
                    )
                }
            }

            // =========================================================================
            // 6. بطاقة النظام والتحديثات والمعلومات (System & About Bento)
            // =========================================================================
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = ObsidianGlowBorder.None
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WhatsAppGreen.copy(alpha = 0.15f))
                                .border(1.dp, WhatsAppGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Σ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Sigma AutoFlow",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "الإصدار 8.3.0 (Build 12) • مستقر",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCheckUpdates()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فحص التحديثات",
                            fontSize = 11.5.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
        )
    }

    if (showLanguageDialog) {
        SettingsLanguageDialog(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { selectedLanguage = it },
            onDismiss = { showLanguageDialog = false }
        )
    }
}
