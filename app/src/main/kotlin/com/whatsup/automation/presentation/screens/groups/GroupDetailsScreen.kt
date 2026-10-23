package com.whatsup.automation.presentation.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.Group
import com.whatsup.automation.domain.util.SpintaxEngine
import com.whatsup.automation.presentation.components.AppTooltipBox
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.components.GradientButton
import com.whatsup.automation.presentation.screens.groups.components.LogsListTab
import com.whatsup.automation.presentation.screens.groups.components.MembersListTab
import com.whatsup.automation.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    group: Group,
    uiState: GroupsUiState,
    viewModel: GroupsViewModel,
    onBackClick: () -> Unit,
    onTabSelect: (Int) -> Unit,
    onBroadcastTextChange: (String) -> Unit,
    onSendBroadcast: () -> Unit,
    onPauseBroadcast: () -> Unit,
    onResumeBroadcast: () -> Unit,
    onCancelBroadcast: () -> Unit,
    onRemoveMember: (Long) -> Unit,
    onOpenAddMember: () -> Unit,
    modifier: Modifier = Modifier
) {
    val broadcastText = uiState.broadcastTexts[group.id] ?: ""
    val isBroadcasting = uiState.isBroadcastingMap[group.id] == true
    val isPaused = uiState.isPausedMap[group.id] == true
    val progress = uiState.broadcastingProgress[group.id]
    val estimatedTime = viewModel.calculateEstimatedTime(uiState.selectedGroupMembers.size)

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshGroups() },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // شريط علوي مع زر الرجوع
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.selectedGroupMembers.size} عضو • الوقت المقدر: $estimatedTime",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onOpenAddMember,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x3325D366))
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "إضافة عضو", tint = WhatsAppGreen)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // حقل إدخال الرسالة الممتد داخل التفاصيل
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إرسال رسالة بث لأعضاء البطاقة:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "يدعم Spintax والتنويع 🎲",
                            fontSize = 10.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // شريط إدراج المتغيرات والـ Spintax السريعة
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val variables = listOf(
                            "{first_name}" to "الاسم الأول",
                            "{name}" to "الاسم كامل",
                            "{time_greeting}" to "التحية بالوقت",
                            "{day_name}" to "اليوم",
                            "{{أهلاً|مرحباً|حياك الله}}" to "Spintax ترحيب",
                            "---" to "تبديل القالب"
                        )
                        variables.forEach { (tag, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1825D366))
                                    .clickable {
                                        onBroadcastTextChange(broadcastText + if (broadcastText.isBlank()) tag else " $tag")
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+ $label",
                                    color = WhatsAppGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = broadcastText,
                            onValueChange = onBroadcastTextChange,
                            placeholder = { Text("اكتب الرسالة (استخدم {{خيار1|خيار2}} و {name} للتنويع)...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            minLines = 2,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhatsAppGreen,
                                unfocusedBorderColor = Color(0x26FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        GradientButton(
                            text = if (isBroadcasting) (if (isPaused) "موقوف" else "جاري...") else "إرسال",
                            onClick = onSendBroadcast,
                            enabled = broadcastText.isNotBlank() && !isBroadcasting,
                            icon = Icons.Filled.Send
                        )
                    }

                    // معاينة حية لعينة من النص المولد عشوائياً
                    if (broadcastText.isNotBlank()) {
                        val previewSpun = remember(broadcastText) {
                            SpintaxEngine.process(
                                template = SpintaxEngine.getTemplateForIndex(broadcastText, 0, 10),
                                recipientName = "محمد أحمد",
                                recipientPhone = "+967770000000"
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1000E5FF))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "🎲 عينة حية لنص الرسالة كما ستصل للعميل:",
                                    fontSize = 10.sp,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = previewSpun,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (isBroadcasting && progress != null) {
                        val (sent, total) = progress
                        val fraction = if (total > 0) sent.toFloat() / total.toFloat() else 0f
                        val percent = (fraction * 100).toInt()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPaused) "الحملة متوقفة مؤقتاً" else "جارٍ البث الآمن...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaused) AmberWarning else CyberCyan
                                )
                                Text(
                                    text = "$sent من $total ($percent%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaused) AmberWarning else WhatsAppGreenLight,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isPaused) AmberWarning else WhatsAppGreen,
                                trackColor = Color(0x33FFFFFF)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isPaused) {
                                    Button(
                                        onClick = onResumeBroadcast,
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 44.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DarkBgPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("استئناف الإرسال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBgPrimary)
                                    }
                                } else {
                                    Button(
                                        onClick = onPauseBroadcast,
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning.copy(alpha = 0.2f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 44.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Pause, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إيقاف مؤقت", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                    }
                                }

                                OutlinedButton(
                                    onClick = onCancelBroadcast,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null, tint = CoralError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إلغاء", fontSize = 11.sp, color = CoralError)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // شريط التبويبات الثلاثة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    title = "الأعضاء",
                    count = uiState.selectedGroupMembers.size,
                    isSelected = uiState.activeDetailsTab == 0,
                    onClick = { onTabSelect(0) },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    title = "المستلمين منهم",
                    count = uiState.selectedGroupCompletedLogs.size,
                    isSelected = uiState.activeDetailsTab == 1,
                    onClick = { onTabSelect(1) },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    title = "في الانتظار",
                    count = uiState.selectedGroupInsiteLogs.size,
                    isSelected = uiState.activeDetailsTab == 2,
                    onClick = { onTabSelect(2) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // محتوى التبويب المختار
            when (uiState.activeDetailsTab) {
                0 -> {
                    MembersListTab(
                        members = uiState.selectedGroupMembers,
                        onRemoveMember = onRemoveMember,
                        onOpenAddMember = onOpenAddMember
                    )
                }
                1 -> {
                    LogsListTab(
                        logs = uiState.selectedGroupCompletedLogs,
                        emptyMessage = "لا يوجد سجل رسائل مستلمة بعد"
                    )
                }
                2 -> {
                    LogsListTab(
                        logs = uiState.selectedGroupInsiteLogs,
                        emptyMessage = "لا توجد رسائل قيد الإرسال أو في الانتظار حالياً"
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tooltipText: String = ""
) {
    AppTooltipBox(tooltipText = tooltipText) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) WhatsAppGreen.copy(alpha = 0.22f) else Color.Transparent)
                .border(
                    1.dp,
                    if (isSelected) WhatsAppGreen else Color.Transparent,
                    RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) WhatsAppGreenLight else TextSecondary
                )

                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) WhatsAppGreen else TextMuted)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$count",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBgPrimary
                        )
                    }
                }
            }
        }
    }
}
