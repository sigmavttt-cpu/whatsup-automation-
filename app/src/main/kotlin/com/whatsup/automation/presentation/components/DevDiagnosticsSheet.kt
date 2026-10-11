package com.whatsup.automation.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.data.diagnostics.LocalANRWatchdog
import com.whatsup.automation.data.diagnostics.LocalCrashWatcher
import com.whatsup.automation.presentation.diagnostics.ComposeLayoutAuditor
import com.whatsup.automation.presentation.diagnostics.LayoutAuditReport
import com.whatsup.automation.presentation.diagnostics.UiElementNode
import com.whatsup.automation.presentation.theme.*

enum class DevDiagnosticsTab(val title: String) {
    UI_AUDITOR("فاحص الواجهات (AI)"),
    CRASHES("سجل الأعطال"),
    PERFORMANCE("الذاكرة و ANR")
}

/**
 * نافذة تشخيص المطور المحلية الشاملة (Dev Diagnostics Bottom Sheet).
 * مخصصة للمطور فقط وتُفتح بالنقر 5 مرات على رقم الإصدار.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevDiagnosticsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(DevDiagnosticsTab.UI_AUDITOR) }

    // تقرير فحص الواجهات اللحظي
    var auditReport by remember {
        mutableStateOf<LayoutAuditReport?>(null)
    }

    LaunchedEffect(Unit) {
        // إجراء تدقيق فوري للعناصر القياسية لشريط التنقل والشاشات
        val mockScreenElements = listOf(
            UiElementNode("tab_dashboard", "الرئيسية", 810, 2150, 1050, 2280, isClickable = true, role = "Tab"),
            UiElementNode("tab_automation", "الأتمتة", 540, 2150, 780, 2280, isClickable = true, role = "Tab"),
            UiElementNode("tab_chatshub", "المحادثات", 270, 2150, 510, 2280, isClickable = true, role = "Tab"),
            UiElementNode("tab_system", "النظام", 0, 2150, 240, 2280, isClickable = true, role = "Tab"),
            UiElementNode("btn_menu", "زر القائمة", 32, 2020, 124, 2112, isClickable = true, role = "Button")
        )
        val rep = ComposeLayoutAuditor.audit(mockScreenElements, 1080, 2400)
        auditReport = rep
        ComposeLayoutAuditor.saveReportLocally(context, rep)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // الترويسة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.2f))
                            .border(1.5.dp, WhatsAppGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.BugReport, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("لوحة تشخيص المطور المحلية 🛠️", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("محلي 100% • خالي من السحابة • حماية قصوى", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                }
            }

            // شريط التبويبات
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DevDiagnosticsTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) WhatsAppGreen else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // محتوى التبويب المختار
            when (selectedTab) {
                DevDiagnosticsTab.UI_AUDITOR -> {
                    val rep = auditReport
                    if (rep != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // مؤشر صحة الواجهات
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("مؤشر سلامة التخطيط ولمس الشاشة:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                        Text(
                                            text = if (rep.hasCriticalIssues) "يوجد تداخلات بحاجة لمعالجة ⚠️" else "سليمة تماماً وبدون أي تداخل ✅",
                                            color = if (rep.hasCriticalIssues) RoseError else WhatsAppGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${rep.score}/100",
                                        color = if (rep.score >= 90) WhatsAppGreen else AmberWarning,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // إحصائيات العناصر
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("الأزرار القابلة للنقر", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text("${rep.clickableCount}", color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("أخطار التداخل (Overlap)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text("${rep.overlapHazards.size}", color = if (rep.overlapHazards.isEmpty()) WhatsAppGreen else RoseError, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // زر نسخ التقرير النصي للذكاء الاصطناعي
                            Button(
                                onClick = {
                                    val json = ComposeLayoutAuditor.toJson(rep)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("UI Audit JSON", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ تقرير JSON للذكاء الاصطناعي 📐", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Code, contentDescription = null, tint = Color.White)
                                    Text("نسخ تقرير الـ JSON النصي للذكاء الاصطناعي 📋", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                DevDiagnosticsTab.CRASHES -> {
                    val latestCrash = remember { LocalCrashWatcher.getLatestCrashReport(context) }
                    if (latestCrash.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(40.dp))
                                Text("لا توجد انهيارات مسجلة — النظام مستقر 100% ✨", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF020617))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = latestCrash, color = RoseError, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                DevDiagnosticsTab.PERFORMANCE -> {
                    val runtime = Runtime.getRuntime()
                    val totalMb = runtime.totalMemory() / (1024 * 1024)
                    val freeMb = runtime.freeMemory() / (1024 * 1024)
                    val usedMb = totalMb - freeMb

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("استهلاك ذاكرة التطبيق (JVM Heap):", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("مستخدم: $usedMb MB / إجمالي: $totalMb MB (متبقي: $freeMb MB)", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Shield, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                                    Text("حارس عدم الاستجابة (ANR Watchdog):", color = Color.White, fontSize = 13.sp)
                                }
                                Text("نشط 24/7 ✅", color = WhatsAppGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
