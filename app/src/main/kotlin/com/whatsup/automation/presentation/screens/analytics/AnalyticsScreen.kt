package com.whatsup.automation.presentation.screens.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refreshAnalytics()
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            // 1. بطاقة الرأس والتعريف
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderBrush = ObsidianGlowBorder.Emerald
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "مركز الإحصائيات وتحليل الأداء",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "تحليل محلي فائق لرسائل الأتمتة وتفاعلات الحالات 100% أوفلاين",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 2. شبكة المؤشرات الحيوية السريعة (KPI Bento Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // السطر الأول: الوقت الموفر وإجمالي الردود
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoCard(modifier = Modifier.weight(1f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.HourglassBottom, contentDescription = null, tint = WhatsAppNeon, modifier = Modifier.size(16.dp))
                                    Text("الوقت الموفّر", fontSize = 11.sp, color = TextSecondary)
                                }
                                Text(
                                    text = uiState.timeSavedFormatted,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppNeon
                                )
                                Text("جهد الردود والحالات", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        BentoCard(modifier = Modifier.weight(1f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Text("الردود الآلية", fontSize = 11.sp, color = TextSecondary)
                                }
                                Text(
                                    text = "${uiState.totalReplies}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text("رد آلي مرسل بنجاح", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }

                    // السطر الثاني: الحالات المشاهدة وجهات الاتصال المحفوظة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoCard(modifier = Modifier.weight(1f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.AutoStories, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                                    Text("الحالات المشاهدة", fontSize = 11.sp, color = TextSecondary)
                                }
                                Text(
                                    text = "${uiState.statusesViewed}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppGreen
                                )
                                Text("تفاعل بالقلب: ${uiState.statusesReacted} 💚", fontSize = 10.sp, color = WhatsAppGreenLight)
                            }
                        }

                        BentoCard(modifier = Modifier.weight(1f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                                    Text("جهات الاتصال", fontSize = 11.sp, color = TextSecondary)
                                }
                                Text(
                                    text = "${uiState.totalContactsSaved}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPurple
                                )
                                Text("محفوظة تلقائياً بالهاتف", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            // 3. الرسم البياني لساعات الذروة (24-Hour Native Canvas Bar Chart)
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "توزيع النشاط اليومي (24 ساعة)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "حجم النشاط ومعالجة الرسائل بالساعات",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WhatsAppGreen.copy(alpha = 0.12f))
                                    .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الذروة: ${uiState.peakHourLabel}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppGreen
                                )
                            }
                        }

                        // أعمدة الرسم البياني بنعومة فائقة
                        val maxCount = (uiState.hourlyCounts.maxOrNull() ?: 1).coerceAtLeast(1)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkBgSecondary)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                uiState.hourlyCounts.forEachIndexed { _, count ->
                                    val barFraction = (count.toFloat() / maxCount.toFloat()).coerceIn(0.06f, 1f)
                                    val isPeak = count == maxCount && count > 0

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.65f)
                                                .fillMaxHeight(barFraction)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(
                                                    if (isPeak) Brush.verticalGradient(listOf(WhatsAppNeon, WhatsAppGreen))
                                                    else if (count > 0) Brush.verticalGradient(listOf(CyberCyan, ElectricBlue))
                                                    else Brush.verticalGradient(listOf(Color(0x2564748B), Color(0x1064748B)))
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // تسميات التوقيت
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("12 ص", fontSize = 9.sp, color = TextMuted)
                            Text("06 ص", fontSize = 9.sp, color = TextMuted)
                            Text("12 م", fontSize = 9.sp, color = TextMuted)
                            Text("06 م", fontSize = 9.sp, color = TextMuted)
                            Text("11 م", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }
            }

            // 4. أكثر جهات الاتصال نشاطاً في الحالات
            if (uiState.topContacts.isNotEmpty()) {
                item {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "أنشط جهات الاتصال في الحالات ⭐",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text("الأكثر نشراً وتفاعلاً", fontSize = 11.sp, color = TextMuted)
                            }

                            uiState.topContacts.forEachIndexed { index, contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x10FFFFFF))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (index == 0) WhatsAppGreen.copy(alpha = 0.2f) else Color(0x15FFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index == 0) WhatsAppGreen else TextSecondary
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = contact.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = contact.phone,
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${contact.count} حالة 💚",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WhatsAppGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. القواعد الأكثر استخداماً
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "القواعد الأكثر نشاطاً ومطابقة 🏆",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Icon(Icons.Filled.Stars, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                        }

                        if (uiState.topRules.isEmpty()) {
                            Text(
                                text = "لم يتم تنفيذ أي قواعد بعد. بمجرد وصول رسائل ومطابقتها ستظهر هنا تلقائياً.",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        } else {
                            val maxRuleCount = uiState.topRules.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                            uiState.topRules.forEachIndexed { index, ruleStat ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${ruleStat.matchedRule}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${ruleStat.count} مرة",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WhatsAppGreen
                                        )
                                    }

                                    val progressFraction by animateFloatAsState(
                                        targetValue = (ruleStat.count.toFloat() / maxRuleCount.toFloat()).coerceIn(0.05f, 1f),
                                        animationSpec = tween(500),
                                        label = "ruleProgress"
                                    )

                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (index == 0) WhatsAppGreen else CyberCyan,
                                        trackColor = DarkBgSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. سجل الأحداث والعمليات اللحظي (Live Activity Timeline)
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "سجل الأنشطة والعمليات اللحظي 📋",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text("آخر 10 أحداث", fontSize = 11.sp, color = TextMuted)
                        }

                        if (uiState.recentLogs.isEmpty()) {
                            Text(
                                text = "لا توجد سجلات حالياً، ستظهر العمليات هنا فور وقوعها في الخلفية.",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        } else {
                            uiState.recentLogs.forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x0CFFFFFF))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.actionExecuted.ifBlank { "تنفيذ قاعدة" },
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${log.senderPhone} • ${log.matchedRule ?: "أتمتة"}",
                                            fontSize = 10.5.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (log.status == "SUCCESS") WhatsAppGreen.copy(alpha = 0.15f)
                                                    else RoseError.copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (log.status == "SUCCESS") "ناجح ✓" else log.status,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (log.status == "SUCCESS") WhatsAppGreen else RoseError
                                            )
                                        }

                                        Text(
                                            text = timeFormatter.format(Instant.ofEpochMilli(log.timestamp)),
                                            fontSize = 9.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
