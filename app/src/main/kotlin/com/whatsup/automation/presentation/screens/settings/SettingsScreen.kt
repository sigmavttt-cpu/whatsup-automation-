package com.whatsup.automation.presentation.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.theme.*

/**
 * شاشة الإعدادات بتصميم نمط Grouped Settings List (قائمة إعدادات مجمّعة / Sectioned List).
 * تعتمد النمط البصري الفاخر لـ Sigma AutoFlow 2.0: خلفية كربونية عميقة،
 * بطاقات زجاجية مجمعة (Grouped Glass Containers)، وأيقونات نيون زمردية متوهجة.
 */
@Composable
fun SettingsScreen(
    onNavigateToPairing: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onOpenLockSettings: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onNavigateToSplashAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // حالات النوافذ الحوارية التفاعلية
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("العربية (الافتراضية)") }
    var isPushEnabled by remember { mutableStateOf(true) }
    var isStealthActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070E13)) // كربوني ليلي فائق العمق مطابق للتصميم
    ) {
        // هالة خافتة زمردية في الزاوية العلوية (Ambient Emerald Nebula Glow)
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 60.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x2200E676),
                            Color(0x0C00C853),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // شريط العنوان العلوي (RTL Header with Emerald Accent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "الإعدادات",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    // الرمز الزخرفي الزمردي الأنيق المطابق للصورة
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // زر استعراض شاشة الترحيب والسبلاش السينمائية
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToSplashAuth()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F00E676))
                        .border(1.dp, Color(0x3300E676), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "شاشة الترحيب",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // =========================================================================
            // النمط الرئيسي: Grouped Settings List (قائمة إعدادات مجمّعة / Sectioned List)
            // مطابق 100% للصورة 3 مع كافة العناصر والأيقونات والتفاصيل
            // =========================================================================
            GroupedSettingsListContainer {
                // 1. الحساب (إعدادات الحساب والربط)
                GroupedSettingsRow(
                    title = "الحساب",
                    subtitle = "إعدادات الحساب والربط",
                    icon = Icons.Outlined.Person,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAccountDialog = true
                    }
                )

                SettingsRowDivider()

                // 2. الأمان والخصوصية (إعدادات الحماية والبيانات)
                GroupedSettingsRow(
                    title = "الأمان والخصوصية",
                    subtitle = "إعدادات الحماية والبيانات",
                    icon = Icons.Outlined.Lock,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenLockSettings()
                    }
                )

                SettingsRowDivider()

                // 3. التنبيهات (إدارة الإشعارات)
                GroupedSettingsRow(
                    title = "التنبيهات",
                    subtitle = "إدارة الإشعارات",
                    icon = Icons.Outlined.Notifications,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showNotificationsSheet = true
                    }
                )

                SettingsRowDivider()

                // 4. النسخ الاحتياطي (نسخ البيانات احتياطياً)
                GroupedSettingsRow(
                    title = "النسخ الاحتياطي",
                    subtitle = "نسخ البيانات احتياطياً",
                    icon = Icons.Outlined.CloudUpload,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToBackup()
                    }
                )

                SettingsRowDivider()

                // 5. اللغة (تغيير لغة التطبيق)
                GroupedSettingsRow(
                    title = "اللغة",
                    subtitle = "تغيير لغة التطبيق",
                    icon = Icons.Outlined.Language,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showLanguageDialog = true
                    }
                )

                SettingsRowDivider()

                // 6. عن التطبيق (الإصدار 2.0.0)
                GroupedSettingsRow(
                    title = "عن التطبيق",
                    subtitle = "الإصدار 2.0.0",
                    icon = Icons.Outlined.Info,
                    isLast = true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAboutDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // قسم إضافي اختياري: إجراءات سريعة للجلسة (Quick Session Controls)
            Text(
                text = "إجراءات الجلسة السريعة",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )

            GroupedSettingsListContainer {
                GroupedSettingsRow(
                    title = "ربط واتساب بالباركود",
                    subtitle = "مسح رمز QR أو إدخال كود الاقتران",
                    icon = Icons.Default.QrCode2,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToPairing()
                    }
                )

                SettingsRowDivider()

                GroupedSettingsRow(
                    title = "الخزينة السرية المشفرة",
                    subtitle = "حماية وسائط العرض مرة واحدة والملاحظات",
                    icon = Icons.Default.Shield,
                    isLast = true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToVault()
                    }
                )
            }

            Spacer(modifier = Modifier.height(100.dp)) // تباعد كافٍ لشريط التنقل السفلي
        }
    }

    // ==========================================
    // النوافذ الحوارية التفاعلية المرتبطة بالإعدادات
    // ==========================================

    // 1. نافذة معلومات الحساب
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            containerColor = Color(0xFF0F1A22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppGreenLight)
                    Text("إعدادات الحساب والربط", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "محرك واتساب يعمل محلياً بالكامل (On-Device 100%) بدون أي خوادم وسيطة.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF16252F))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("حالة الاتصال: نشط ومستقر 🟢", color = WhatsAppGreenLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("معرّف الجلسة مشفر ومحمي في Android Keystore", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccountDialog = false
                        onNavigateToPairing()
                    }
                ) {
                    Text("إدارة الربط والباركود", color = WhatsAppGreenLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("إغلاق", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // 2. نافذة اختيار اللغة
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = Color(0xFF0F1A22),
            title = {
                Text("تغيير لغة التطبيق", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("العربية (الافتراضية)", "English (US)").forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x1F00E676) else Color.Transparent)
                                .clickable { selectedLanguage = lang }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang, color = if (isSelected) WhatsAppGreenLight else Color.White, fontSize = 14.sp)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = WhatsAppGreenLight, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("حفظ", color = WhatsAppGreenLight, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 3. نافذة عن التطبيق
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Color(0xFF0F1A22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = WhatsAppGreenLight)
                    Text("عن التطبيق", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Sigma AutoFlow", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("الإصدار 2.0.0 (V8.3.0 Engine)", color = WhatsAppGreenLight, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "المنظومة الاحترافية لأتمتة واتساب والردود الذكية وإدارة الحالات محلياً 100%.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAboutDialog = false
                        onCheckUpdates()
                    }
                ) {
                    Text("فحص التحديثات ⚡", color = WhatsAppGreenLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("إغلاق", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // 4. ورقة خيارات التنبيهات والوضع المتخفي
    if (showNotificationsSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = Color(0xFF0F1A22),
            title = {
                Text("إدارة الإشعارات والتنبيهات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تنبيهات العمليات المكتملة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("إشعار عند إرسال رد آلي أو حفظ جهة اتصال", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Switch(
                            checked = isPushEnabled,
                            onCheckedChange = { isPushEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhatsAppGreen
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الوضع المتخفي الصامت 🔕", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("إخفاء الأيقونة والإشعار من شريط الحالة للنظام", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Switch(
                            checked = isStealthActive,
                            onCheckedChange = { isStealthActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhatsAppGreen
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsSheet = false }) {
                    Text("تم", color = WhatsAppGreenLight, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * الحاوية المجمعة لنمط Grouped Settings List (Sectioned Container).
 * تمثل كتلة زجاجية أنيقة ذات حواف دائرية وظلال ناعمة تضم مجموعة صفوف الإعدادات.
 */
@Composable
fun GroupedSettingsListContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0F1A22)) // لون بطاقة زجاجية داكنة فاخرة
            .border(
                width = 1.dp,
                color = Color(0x1F00E676), // حافة زمردية خافتة وراقية
                shape = RoundedCornerShape(22.dp)
            ),
        content = content
    )
}

/**
 * عنصر صف الإعدادات الفردي داخل القائمة المجمعة (Grouped Settings Row Item).
 * مطابق حرفياً للصورة 3: أيقونة خضراء دائرية، نص أساسي وفرعي، وسهم تنقل.
 */
@Composable
fun GroupedSettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // أيقونة الإعداد داخل حاوية دائرية بتوهج أخضر نيون ناعم
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x1400E676))
                .border(1.dp, Color(0x3300E676), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = WhatsAppGreenLight,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // النصوص: العنوان الرئيسي والفرعي
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF8696A0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // سهم التنقل الفرعي (RTL Chevron pointing leftwards)
        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * فاصل ناعم بين عناصر القائمة المجمعة لا يتجاوز مساحة الأيقونة ليعطي لمسة iOS/Modern Glass نقية.
 */
@Composable
fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.8.dp,
        color = Color(0x0F8696A0)
    )
}
