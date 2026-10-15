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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.screens.logs.LogsScreen
import com.whatsup.automation.presentation.screens.logs.LogsViewModel
import com.whatsup.automation.presentation.screens.pairing.PairingScreen
import com.whatsup.automation.presentation.screens.pairing.PairingViewModel
import com.whatsup.automation.presentation.theme.*

enum class SystemTab(val title: String, val icon: ImageVector) {
    PAIRING("ربط الجلسة", Icons.Filled.QrCode2),
    LOGS("سجل العمليات اللحظي", Icons.Filled.ListAlt)
}

@Composable
fun SystemHubScreen(
    pairingViewModel: PairingViewModel,
    logsViewModel: LogsViewModel,
    analyticsViewModel: com.whatsup.automation.presentation.screens.analytics.AnalyticsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    updateViewModel: com.whatsup.automation.presentation.screens.update.UpdateViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    initialTab: SystemTab = SystemTab.PAIRING,
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
        // شريط التبويبات الفرعية العلوي بتصميم Cyber Jade Glassmorphic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .border(1.dp, Brush.horizontalGradient(listOf(Color(0x3300E676), Color(0x3300B0FF))), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SystemTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val bgBrush = if (isSelected) {
                        Brush.horizontalGradient(listOf(WhatsAppGreen, WhatsAppGreenLight))
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }

                    com.whatsup.automation.presentation.components.AppTooltipBox(
                        tooltipText = if (tab == SystemTab.PAIRING) "ربط وإدارة حساب واتساب وتوليد الرمز" else "سجل العمليات والنشاط والرسائل المباشرة",
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgBrush)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(16.dp))
                                    } else Modifier
                                )
                                .clickable {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTab = tab
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) Color.White else WhatsAppGreenLight,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

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
                SystemTab.PAIRING -> PairingScreen(viewModel = pairingViewModel)
                SystemTab.LOGS -> LogsScreen(viewModel = logsViewModel)
            }
        }
    }
}

/**
 * شاشة إعدادات درع الحساب ومحاكي السلوك البشري لمنع الحظر.
 */
@Composable
fun AntiBanShieldScreen(
    viewModel: LogsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val antiBan = uiState.antiBanSettings
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // بطاقة الرأس والتحكم العام
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = if (antiBan.isEnabled) ObsidianGlowBorder.Emerald else ObsidianGlowBorder.Amber
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "درع الحساب ومحاكي السلوك البشري",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (antiBan.isEnabled) "الدرع نشط ومحصن ضد خوارزميات الكشف 🛡️" else "الدرع معطل مؤقتاً ⚠️",
                                    fontSize = 11.5.sp,
                                    color = if (antiBan.isEnabled) WhatsAppGreen else AmberWarning,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Switch(
                            checked = antiBan.isEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateAntiBanSettings(antiBan.copy(isEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhatsAppGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBgSecondary
                            )
                        )
                    }

                    Text(
                        text = "يقوم درع الحساب بحساب التوزيع الجاوسي (Gaussian Jitter) للفواصل الزمنية، ومحاكاة الكتابة والقراءة الطبيعية بدقة تامة، لمنع حظر الحساب تماماً حتى مع أثقل أحجام الرسائل.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (antiBan.isEnabled) {
            // بطاقة خيارات المحاكاة الذكية
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderBrush = ObsidianGlowBorder.Cyan
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "آليات المحاكاة المتقدمة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        // خيار محاكاة القراءة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("محاكاة قراءة الرسالة (Mark Read)", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("تأكيد القراءة بفاصل زمني طبيعي قبل البدء بالكتابة", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = antiBan.simulateReading,
                                onCheckedChange = {
                                    viewModel.updateAntiBanSettings(antiBan.copy(simulateReading = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CyberCyan
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0x15FFFFFF))

                        // خيار سرعة الكتابة الديناميكية
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("سرعة كتابة متناسبة مع طول النص", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("إظهار 'يكتب الآن...' مع وقفات تفكير بشرية كل 60 حرفاً", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = antiBan.dynamicTypingSpeed,
                                onCheckedChange = {
                                    viewModel.updateAntiBanSettings(antiBan.copy(dynamicTypingSpeed = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CyberCyan
                                )
                            )
                        }
                    }
                }
            }

            // بطاقة التهدئة ومكافحة الحلقات
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderBrush = ObsidianGlowBorder.Emerald
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "فترات التهدئة ومكافحة الحلقات مع البوتات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        // منزلق فترة التهدئة
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الفاصل الأدنى بين الردود لنفس الرقم:", fontSize = 12.sp, color = TextSecondary)
                                Text("${antiBan.contactCooldownSeconds} ثانية", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                            }
                            Slider(
                                value = antiBan.contactCooldownSeconds.toFloat(),
                                onValueChange = {
                                    viewModel.updateAntiBanSettings(antiBan.copy(contactCooldownSeconds = it.toInt()))
                                },
                                valueRange = 5f..60f,
                                steps = 10,
                                colors = SliderDefaults.colors(
                                    thumbColor = WhatsAppGreen,
                                    activeTrackColor = WhatsAppGreen,
                                    inactiveTrackColor = Color(0x22FFFFFF)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0x15FFFFFF))

                        // منزلق سقف الردود
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("أقصى عدد ردود لنفس الرقم خلال 10 دقائق:", fontSize = 12.sp, color = TextSecondary)
                                Text("${antiBan.maxRepliesPerWindow} ردود", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                            }
                            Slider(
                                value = antiBan.maxRepliesPerWindow.toFloat(),
                                onValueChange = {
                                    viewModel.updateAntiBanSettings(antiBan.copy(maxRepliesPerWindow = it.toInt()))
                                },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonPurple,
                                    activeTrackColor = NeonPurple,
                                    inactiveTrackColor = Color(0x22FFFFFF)
                                )
                            )
                        }

                        // شارة الحماية
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WhatsAppGreen.copy(alpha = 0.12f))
                                .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "الحماية القصوى نشطة: منع الحلقات التكرارية + نمط التوقيت الليلي التلقائي",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhatsAppGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
